/*
 * hev-task-system.h
 * Lightweight cooperative coroutine scheduler and epoll I/O multiplexer.
 * Part of AegisVPN Android Censorship Resistance Engine.
 */

#ifndef HEV_TASK_SYSTEM_H
#define HEV_TASK_SYSTEM_H

#include <stdint.h>
#include <stddef.h>
#include <sys/epoll.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct _HevTask HevTask;
typedef void (*HevTaskEntry)(void *data);

/**
 * Initializes the global task system and epoll reactor.
 * Returns 0 on success, negative error code on failure.
 */
int hev_task_system_init(void);

/**
 * Destroys the task system and frees all epoll descriptors.
 */
void hev_task_system_fini(void);

/**
 * Runs the cooperative scheduler loop until quit flag is set.
 */
void hev_task_system_run(void);

/**
 * Signals the scheduler to exit its loop.
 */
void hev_task_system_stop(void);

/**
 * Spawns a new cooperative task with specified stack size.
 */
HevTask *hev_task_new(size_t stack_size);

/**
 * Starts execution of a task with entry function and argument.
 */
int hev_task_run(HevTask *self, HevTaskEntry entry, void *data);

/**
 * Yields CPU execution to other tasks.
 */
void hev_task_yield(void);

/**
 * Suspends current task until I/O is ready on fd or timeout expires.
 * events: EPOLLIN, EPOLLOUT, etc.
 * timeout_ms: timeout in milliseconds (-1 for infinite).
 * Returns positive ready events or 0 on timeout, negative on error.
 */
int hev_task_io_wait(int fd, uint32_t events, int timeout_ms);

/**
 * Destroys and cleans up a task.
 */
void hev_task_destroy(HevTask *self);

#ifdef __cplusplus
}
#endif

#endif /* HEV_TASK_SYSTEM_H */
