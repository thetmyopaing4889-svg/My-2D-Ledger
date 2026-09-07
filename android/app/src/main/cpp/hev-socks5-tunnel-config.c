/*
 * hev-socks5-tunnel-config.c
 * Robust parser for hev-socks5-tunnel YAML configuration files.
 * Part of AegisVPN Android Censorship Resistance Engine.
 */

#include "hev-socks5-tunnel-config.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <android/log.h>

#define LOG_TAG "HevTunnelConfig"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static char *trim_whitespace(char *str) {
    if (!str) return NULL;
    while (isspace((unsigned char)*str)) str++;
    if (*str == 0) return str;

    char *end = str + strlen(str) - 1;
    while (end > str && isspace((unsigned char)*end)) end--;
    end[1] = '\0';
    return str;
}

static void unquote_string(char *str) {
    if (!str) return;
    size_t len = strlen(str);
    if (len >= 2 && ((str[0] == '\'' && str[len - 1] == '\'') ||
                     (str[0] == '"' && str[len - 1] == '"'))) {
        memmove(str, str + 1, len - 2);
        str[len - 2] = '\0';
    }
}

void hev_tunnel_config_set_defaults(HevTunnelConfig *config) {
    if (!config) return;
    memset(config, 0, sizeof(HevTunnelConfig));

    strncpy(config->tunnel_name, "tun0", sizeof(config->tunnel_name) - 1);
    config->tunnel_mtu = 1500;
    strncpy(config->tunnel_ipv4, "172.19.0.1", sizeof(config->tunnel_ipv4) - 1);
    strncpy(config->tunnel_ipv6, "fdfe:dcba:9876::1", sizeof(config->tunnel_ipv6) - 1);

    strncpy(config->socks5_address, "127.0.0.1", sizeof(config->socks5_address) - 1);
    config->socks5_port = 10808;
    strncpy(config->socks5_udp_mode, "udp", sizeof(config->socks5_udp_mode) - 1);
    config->socks5_mark = 0;

    config->task_stack_size = 81920;   // 80KB coroutine stack
    config->connect_timeout_ms = 5000; // 5 seconds
    config->rw_timeout_ms = 60000;     // 60 seconds
    config->limit_nofile = 65535;      // High file descriptor limit
    config->log_level = HEV_LOG_WARN;
}

int hev_tunnel_config_validate(const HevTunnelConfig *config) {
    if (!config) return -1;

    if (config->tunnel_mtu < 576 || config->tunnel_mtu > 9000) {
        LOGE("Invalid tunnel MTU: %d (must be between 576 and 9000)", config->tunnel_mtu);
        return -2;
    }

    if (config->socks5_port <= 0 || config->socks5_port > 65535) {
        LOGE("Invalid SOCKS5 port: %d (must be 1-65535)", config->socks5_port);
        return -3;
    }

    if (strlen(config->socks5_address) == 0) {
        LOGE("Missing SOCKS5 server address.");
        return -4;
    }

    return 0;
}

int hev_tunnel_config_parse_string(const char *yaml_content, HevTunnelConfig *config) {
    if (!yaml_content || !config) return -1;

    hev_tunnel_config_set_defaults(config);

    char *content_copy = strdup(yaml_content);
    if (!content_copy) return -2;

    char current_section[32] = {0};
    char *line = strtok(content_copy, "\r\n");

    while (line) {
        char *trimmed = trim_whitespace(line);

        // Skip comments and empty lines
        if (*trimmed == '\0' || *trimmed == '#') {
            line = strtok(NULL, "\r\n");
            continue;
        }

        // Detect top-level section: e.g. "tunnel:", "socks5:", "misc:"
        char *colon = strchr(trimmed, ':');
        if (colon) {
            *colon = '\0';
            char *key = trim_whitespace(trimmed);
            char *val = trim_whitespace(colon + 1);

            // Strip trailing comments on value e.g. "1500 # standard mtu"
            char *hash = strchr(val, '#');
            if (hash) {
                *hash = '\0';
                val = trim_whitespace(val);
            }
            unquote_string(val);

            if (*val == '\0') {
                // Section header
                strncpy(current_section, key, sizeof(current_section) - 1);
                current_section[sizeof(current_section) - 1] = '\0';
            } else {
                // Key-value pair
                if (strcmp(current_section, "tunnel") == 0) {
                    if (strcmp(key, "name") == 0) {
                        strncpy(config->tunnel_name, val, sizeof(config->tunnel_name) - 1);
                    } else if (strcmp(key, "mtu") == 0) {
                        config->tunnel_mtu = atoi(val);
                    } else if (strcmp(key, "ipv4") == 0) {
                        strncpy(config->tunnel_ipv4, val, sizeof(config->tunnel_ipv4) - 1);
                    } else if (strcmp(key, "ipv6") == 0) {
                        strncpy(config->tunnel_ipv6, val, sizeof(config->tunnel_ipv6) - 1);
                    }
                } else if (strcmp(current_section, "socks5") == 0) {
                    if (strcmp(key, "port") == 0) {
                        config->socks5_port = atoi(val);
                    } else if (strcmp(key, "address") == 0) {
                        strncpy(config->socks5_address, val, sizeof(config->socks5_address) - 1);
                    } else if (strcmp(key, "udp") == 0) {
                        strncpy(config->socks5_udp_mode, val, sizeof(config->socks5_udp_mode) - 1);
                    } else if (strcmp(key, "username") == 0) {
                        strncpy(config->socks5_username, val, sizeof(config->socks5_username) - 1);
                    } else if (strcmp(key, "password") == 0) {
                        strncpy(config->socks5_password, val, sizeof(config->socks5_password) - 1);
                    } else if (strcmp(key, "mark") == 0) {
                        config->socks5_mark = atoi(val);
                    }
                } else if (strcmp(current_section, "misc") == 0) {
                    if (strcmp(key, "task-stack-size") == 0) {
                        config->task_stack_size = atoi(val);
                    } else if (strcmp(key, "connect-timeout") == 0) {
                        config->connect_timeout_ms = atoi(val);
                    } else if (strcmp(key, "read-write-timeout") == 0) {
                        config->rw_timeout_ms = atoi(val);
                    } else if (strcmp(key, "limit-nofile") == 0) {
                        config->limit_nofile = atoi(val);
                    } else if (strcmp(key, "log-level") == 0) {
                        if (strcasecmp(val, "debug") == 0) config->log_level = HEV_LOG_DEBUG;
                        else if (strcasecmp(val, "info") == 0) config->log_level = HEV_LOG_INFO;
                        else if (strcasecmp(val, "warn") == 0) config->log_level = HEV_LOG_WARN;
                        else if (strcasecmp(val, "error") == 0) config->log_level = HEV_LOG_ERROR;
                    }
                }
            }
        }

        line = strtok(NULL, "\r\n");
    }

    free(content_copy);
    return hev_tunnel_config_validate(config);
}

int hev_tunnel_config_parse_file(const char *path, HevTunnelConfig *config) {
    if (!path || !config) return -1;

    FILE *fp = fopen(path, "rb");
    if (!fp) {
        LOGE("Failed to open YAML config file: %s", path);
        return -2;
    }

    fseek(fp, 0, SEEK_END);
    long size = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    if (size <= 0 || size > 1048576) { // Max 1MB
        fclose(fp);
        LOGE("Invalid YAML config file size: %ld", size);
        return -3;
    }

    char *buffer = (char *)malloc(size + 1);
    if (!buffer) {
        fclose(fp);
        return -4;
    }

    size_t read_bytes = fread(buffer, 1, size, fp);
    fclose(fp);
    buffer[read_bytes] = '\0';

    int rc = hev_tunnel_config_parse_string(buffer, config);
    free(buffer);

    if (rc == 0) {
        LOGI("Parsed hev-socks5-tunnel config: SOCKS5 %s:%d, MTU %d, UDP mode: %s",
             config->socks5_address, config->socks5_port, config->tunnel_mtu, config->socks5_udp_mode);
    } else {
        LOGE("Config parse error code: %d", rc);
    }

    return rc;
}

int hev_socks5_tunnel_config_load(const char *path, HevTunnelConfig *config) {
    return hev_tunnel_config_parse_file(path, config);
}
