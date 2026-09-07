/*
 * hev-socks5-tunnel.h
 * High-performance coroutine-based tun2socks proxy core header.
 * Part of AegisVPN Android Censorship Resistance Engine.
 */

#ifndef HEV_SOCKS5_TUNNEL_H
#define HEV_SOCKS5_TUNNEL_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define HEV_TUNNEL_VERSION "2.7.3-aegis"

typedef enum {
    HEV_LOG_ERROR = 0,
    HEV_LOG_WARN  = 1,
    HEV_LOG_INFO  = 2,
    HEV_LOG_DEBUG = 3
} HevLogLevel;

typedef struct {
    // Tunnel configuration
    char tunnel_name[64];
    int tunnel_mtu;
    char tunnel_ipv4[64];
    char tunnel_ipv6[128];

    // SOCKS5 destination
    char socks5_address[128];
    int socks5_port;
    char socks5_udp_mode[16]; // "udp" or "tcp"
    char socks5_username[64];
    char socks5_password[64];
    int socks5_mark;          // fwmark for SO_MARK routing bypass

    // Miscellaneous parameters
    int task_stack_size;      // Coroutine fiber stack in bytes (e.g. 81920)
    int connect_timeout_ms;   // SOCKS5 connection timeout
    int rw_timeout_ms;        // Read/write socket timeout
    int limit_nofile;         // System rlimit for max open fds
    HevLogLevel log_level;
} HevTunnelConfig;

typedef struct {
    uint64_t tx_bytes;
    uint64_t rx_bytes;
    uint64_t tx_packets;
    uint64_t rx_packets;
    uint32_t active_tcp_sessions;
    uint32_t active_udp_sessions;
} HevTunnelStats;

/**
 * Parses and loads configuration from YAML file at path.
 * Returns 0 on success, negative error code on failure.
 */
int hev_socks5_tunnel_config_load(const char *path, HevTunnelConfig *config);

/**
 * Initializes the tunnel subsystems with loaded configuration and TUN file descriptor.
 * Returns 0 on success, negative error code on failure.
 */
int hev_socks5_tunnel_init(const char *config_path, int tun_fd);

/**
 * Runs the coroutine event loop (blocking until hev_socks5_tunnel_quit is invoked).
 * Returns 0 on clean exit.
 */
int hev_socks5_tunnel_main(const char *config_path, int tun_fd);

/**
 * Triggers graceful termination of the event loop.
 */
void hev_socks5_tunnel_quit(void);

/**
 * Reads cumulative throughput and packet counters.
 */
void hev_socks5_tunnel_stats(unsigned long long *tx, unsigned long long *rx);

/**
 * Reads complete snapshot of tunnel statistics.
 */
void hev_socks5_tunnel_get_stats_snapshot(HevTunnelStats *out_stats);

/**
 * Returns library version string.
 */
const char *hev_socks5_tunnel_version(void);

#ifdef __cplusplus
}
#endif

#endif /* HEV_SOCKS5_TUNNEL_H */
