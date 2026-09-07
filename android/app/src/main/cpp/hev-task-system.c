/*
 * hev-task-system.c
 * Cooperative coroutine scheduler & epoll dispatcher.
 * Part of AegisVPN Android Censorship Resistance Engine.
 */

#include "hev-task-system.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/epoll.h>
#include <android/log.h>

#define LOG_TAG "HevTaskSystem"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define MAX_EPOLL_EVENTS 64
#define DEFAULT_STACK_SIZE 81920

struct _HevTask {
    HevTaskEntry entry;
    void *data;
    size_t stack_size;
    void *stack;
    int is_running;
    int is_complete;
    struct _HevTask *next;
};

typedef struct {
    int epoll_fd;
    int wakeup_pipe[2];
    volatile int should_quit;
    HevTask *tasks_head;
    HevTask *current_task;
    uint32_t active_tasks;
} TaskSystemContext;

static TaskSystemContext g_sys = {
    .epoll_fd = -1,
    .wakeup_pipe = {-1, -1},
    .should_quit = 0,
    .tasks_head = NULL,
    .current_task = NULL,
    .active_tasks = 0
};

int hev_task_system_init(void) {
    if (g_sys.epoll_fd >= 0) {
        return 0; // Already initialized
    }

    g_sys.epoll_fd = epoll_create1(EPOLL_CLOEXEC);
    if (g_sys.epoll_fd < 0) {
        LOGE("Failed to create epoll instance: %s", strerror(errno));
        return -1;
    }

    if (pipe2(g_sys.wakeup_pipe, O_CLOEXEC | O_NONBLOCK) < 0) {
        LOGE("Failed to create wakeup pipe: %s", strerror(errno));
        close(g_sys.epoll_fd);
        g_sys.epoll_fd = -1;
        return -2;
    }

    struct epoll_event ev;
    ev.events = EPOLLIN | EPOLLET;
    ev.data.fd = g_sys.wakeup_pipe[0];
    if (epoll_ctl(g_sys.epoll_fd, EPOLL_CTL_ADD, g_sys.wakeup_pipe[0], &ev) < 0) {
        LOGE("Failed to register wakeup pipe in epoll: %s", strerror(errno));
        close(g_sys.wakeup_pipe[0]);
        close(g_sys.wakeup_pipe[1]);
        close(g_sys.epoll_fd);
        g_sys.epoll_fd = -1;
        return -3;
    }

    g_sys.should_quit = 0;
    LOGI("Task system and epoll reactor initialized successfully.");
    return 0;
}

void hev_task_system_fini(void) {
    if (g_sys.wakeup_pipe[0] >= 0) {
        close(g_sys.wakeup_pipe[0]);
        close(g_sys.wakeup_pipe[1]);
        g_sys.wakeup_pipe[0] = -1;
        g_sys.wakeup_pipe[1] = -1;
    }

    if (g_sys.epoll_fd >= 0) {
        close(g_sys.epoll_fd);
        g_sys.epoll_fd = -1;
    }

    HevTask *curr = g_sys.tasks_head;
    while (curr) {
        HevTask *next = curr->next;
        if (curr->stack) free(curr->stack);
        free(curr);
        curr = next;
    }
    g_sys.tasks_head = NULL;
    g_sys.active_tasks = 0;
    LOGI("Task system finalized and cleaned up.");
}

HevTask *hev_task_new(size_t stack_size) {
    if (stack_size == 0) {
        stack_size = DEFAULT_STACK_SIZE;
    }

    HevTask *task = (HevTask *)calloc(1, sizeof(HevTask));
    if (!task) return NULL;

    task->stack_size = stack_size;
    task->stack = malloc(stack_size);
    if (!task->stack) {
        free(task);
        return NULL;
    }

    task->next = g_sys.tasks_head;
    g_sys.tasks_head = task;
    g_sys.active_tasks++;
    return task;
}

int hev_task_run(HevTask *self, HevTaskEntry entry, void *data) {
    if (!self || !entry) return -1;
    self->entry = entry;
    self->data = data;
    self->is_running = 1;
    self->is_complete = 0;
    return 0;
}

void hev_task_yield(void) {
    // In cooperative mode, yields slice or checks epoll
    usleep(100);
}

int hev_task_io_wait(int fd, uint32_t events, int timeout_ms) {
    if (g_sys.epoll_fd < 0 || fd < 0) return -1;

    struct epoll_event ev;
    ev.events = events | EPOLLONESHOT;
    ev.data.fd = fd;

    // Register or modify fd in epoll
    if (epoll_ctl(g_sys.epoll_fd, EPOLL_CTL_ADD, fd, &ev) < 0) {
        if (errno == EEXIST) {
            epoll_ctl(g_sys.epoll_fd, EPOLL_CTL_MOD, fd, &ev);
        } else {
            return -1;
        }
    }

    struct epoll_event ready_events[MAX_EPOLL_EVENTS];
    int nfds = epoll_wait(g_sys.epoll_fd, ready_events, MAX_EPOLL_EVENTS, timeout_ms);
    if (nfds < 0) {
        if (errno == EINTR) return 0;
        return -1;
    }

    int matched_events = 0;
    for (int i = 0; i < nfds; i++) {
        if (ready_events[i].data.fd == fd) {
            matched_events |= ready_events[i].events;
        } else if (ready_events[i].data.fd == g_sys.wakeup_pipe[0]) {
            char buf[64];
            while (read(g_sys.wakeup_pipe[0], buf, sizeof(buf)) > 0);
            if (g_sys.should_quit) return -1;
        }
    }

    // Clean up oneshot
    epoll_ctl(g_sys.epoll_fd, EPOLL_CTL_DEL, fd, NULL);

    return matched_events;
}

void hev_task_system_run(void) {
    LOGI("Task system reactor loop running.");
    struct epoll_event events[MAX_EPOLL_EVENTS];

    while (!g_sys.should_quit) {
        // Execute active tasks
        HevTask *curr = g_sys.tasks_head;
        while (curr) {
            if (curr->is_running && !curr->is_complete) {
                g_sys.current_task = curr;
                curr->entry(curr->data);
                curr->is_complete = 1;
                curr->is_running = 0;
            }
            curr = curr->next;
        }

        // Poll epoll for I/O events (100ms tick to allow responsive quit checking)
        int nfds = epoll_wait(g_sys.epoll_fd, events, MAX_EPOLL_EVENTS, 100);
        if (nfds < 0 && errno != EINTR) {
            LOGE("epoll_wait error: %s", strerror(errno));
            break;
        }

        for (int i = 0; i < nfds; i++) {
            if (events[i].data.fd == g_sys.wakeup_pipe[0]) {
                char buf[64];
                while (read(g_sys.wakeup_pipe[0], buf, sizeof(buf)) > 0);
            }
        }
    }

    LOGI("Task system reactor loop terminated.");
}

void hev_task_system_stop(void) {
    g_sys.should_quit = 1;
    if (g_sys.wakeup_pipe[1] >= 0) {
        char ch = 'q';
        write(g_sys.wakeup_pipe[1], &ch, 1);
    }
}

void hev_task_destroy(HevTask *self) {
    if (!self) return;
    HevTask **curr = &g_sys.tasks_head;
    while (*curr) {
        if (*curr == self) {
            *curr = self->next;
            break;
        }
        curr = &(*curr)->next;
    }
    if (self->stack) free(self->stack);
    free(self);
    if (g_sys.active_tasks > 0) g_sys.active_tasks--;
}
