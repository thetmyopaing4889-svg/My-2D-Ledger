/*
 * hev-socks5-tunnel-bridge.c
 * Production JNI bridge connecting Android VpnService TUN file descriptor
 * directly to the local SOCKS5 core daemon via hev-socks5-tunnel.
 * Part of AegisVPN Android Censorship Resistance Engine.
 */

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <android/log.h>

#include "hev-socks5-tunnel.h"

#define LOG_TAG "HevTunnelBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define JNI_EXPORT __attribute__((visibility("default"))) JNIEXPORT

typedef struct {
    char *config_path;
    int tun_fd;
    pthread_t worker_thread;
    volatile int is_running;
} TunnelBridgeContext;

static TunnelBridgeContext g_ctx = {
    .config_path = NULL,
    .tun_fd = -1,
    .is_running = 0
};

static pthread_mutex_t g_mutex = PTHREAD_MUTEX_INITIALIZER;

static void *tunnel_worker_routine(void *arg) {
    (void)arg;
    LOGI("Tunnel worker thread launched with TUN fd=%d, config=%s", g_ctx.tun_fd, g_ctx.config_path);

    // Call the core hev-socks5-tunnel packet processing event loop
    int rc = hev_socks5_tunnel_main(g_ctx.config_path, g_ctx.tun_fd);
    LOGI("hev_socks5_tunnel_main loop exited with return code: %d", rc);

    pthread_mutex_lock(&g_mutex);
    g_ctx.is_running = 0;
    pthread_mutex_unlock(&g_mutex);

    return NULL;
}

JNI_EXPORT jint JNICALL
Java_org_anticensor_vpn_core_tunnel_HevTunnel_nativeInit(
    JNIEnv *env,
    jobject thiz,
    jstring jconfig_path,
    jint tun_fd
) {
    (void)thiz;
    pthread_mutex_lock(&g_mutex);

    if (g_ctx.is_running) {
        LOGE("Tunnel is already running! Must stop prior to re-init.");
        pthread_mutex_unlock(&g_mutex);
        return -1;
    }

    if (g_ctx.config_path) {
        free(g_ctx.config_path);
        g_ctx.config_path = NULL;
    }

    const char *c_path = (*env)->GetStringUTFChars(env, jconfig_path, NULL);
    if (c_path) {
        g_ctx.config_path = strdup(c_path);
        (*env)->ReleaseStringUTFChars(env, jconfig_path, c_path);
    }

    g_ctx.tun_fd = tun_fd;
    LOGI("HevTunnel initialized with config: %s and TUN FD: %d", g_ctx.config_path, g_ctx.tun_fd);

    pthread_mutex_unlock(&g_mutex);
    return 0;
}

JNI_EXPORT jint JNICALL
Java_org_anticensor_vpn_core_tunnel_HevTunnel_nativeStart(
    JNIEnv *env,
    jobject thiz
) {
    (void)env;
    (void)thiz;
    pthread_mutex_lock(&g_mutex);

    if (g_ctx.is_running) {
        LOGD("Tunnel is already marked active.");
        pthread_mutex_unlock(&g_mutex);
        return 0;
    }

    if (g_ctx.tun_fd < 0 || g_ctx.config_path == NULL) {
        LOGE("Cannot start tunnel: invalid TUN FD (%d) or missing config (%p)",
             g_ctx.tun_fd, (void*)g_ctx.config_path);
        pthread_mutex_unlock(&g_mutex);
        return -2;
    }

    g_ctx.is_running = 1;
    int rc = pthread_create(&g_ctx.worker_thread, NULL, tunnel_worker_routine, NULL);
    if (rc != 0) {
        LOGE("Failed to spawn tunnel worker thread: error %d", rc);
        g_ctx.is_running = 0;
        pthread_mutex_unlock(&g_mutex);
        return -3;
    }

    pthread_mutex_unlock(&g_mutex);
    LOGI("HevTunnel native worker pthread successfully spawned.");
    return 0;
}

JNI_EXPORT jint JNICALL
Java_org_anticensor_vpn_core_tunnel_HevTunnel_nativeStop(
    JNIEnv *env,
    jobject thiz
) {
    (void)env;
    (void)thiz;
    pthread_mutex_lock(&g_mutex);

    if (!g_ctx.is_running) {
        LOGD("Tunnel is already stopped.");
        pthread_mutex_unlock(&g_mutex);
        return 0;
    }

    LOGI("Halting HevTunnel worker...");
    g_ctx.is_running = 0;

    hev_socks5_tunnel_quit();

    pthread_join(g_ctx.worker_thread, NULL);

    if (g_ctx.config_path) {
        free(g_ctx.config_path);
        g_ctx.config_path = NULL;
    }
    g_ctx.tun_fd = -1;

    LOGI("HevTunnel worker joined and resources cleared.");
    pthread_mutex_unlock(&g_mutex);
    return 0;
}

JNI_EXPORT void JNICALL
Java_org_anticensor_vpn_core_tunnel_HevTunnel_nativeGetStats(
    JNIEnv *env,
    jobject thiz,
    jlongArray jstats
) {
    (void)thiz;
    jsize len = (*env)->GetArrayLength(env, jstats);
    if (len < 2) return;

    unsigned long long tx = 0, rx = 0;
    hev_socks5_tunnel_stats(&tx, &rx);

    jlong values[2];
    values[0] = (jlong)tx;
    values[1] = (jlong)rx;
    (*env)->SetLongArrayRegion(env, jstats, 0, 2, values);
}

JNI_EXPORT jstring JNICALL
Java_org_anticensor_vpn_core_tunnel_HevTunnel_nativeGetVersion(
    JNIEnv *env,
    jobject thiz
) {
    (void)thiz;
    const char *ver = hev_socks5_tunnel_version();
    return (*env)->NewStringUTF(env, ver);
}
