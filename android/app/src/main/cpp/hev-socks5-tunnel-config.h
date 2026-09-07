/*
 * hev-socks5-tunnel-config.h
 * Configuration loader & parser for hev-socks5-tunnel YAML format.
 * Part of AegisVPN Android Censorship Resistance Engine.
 */

#ifndef HEV_SOCKS5_TUNNEL_CONFIG_H
#define HEV_SOCKS5_TUNNEL_CONFIG_H

#include "hev-socks5-tunnel.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initializes a HevTunnelConfig struct with safe production default values.
 */
void hev_tunnel_config_set_defaults(HevTunnelConfig *config);

/**
 * Parses a YAML configuration file from disk.
 * Returns 0 on success, negative error code on syntax/file failure.
 */
int hev_tunnel_config_parse_file(const char *path, HevTunnelConfig *config);

/**
 * Parses raw YAML text string into HevTunnelConfig.
 */
int hev_tunnel_config_parse_string(const char *yaml_content, HevTunnelConfig *config);

/**
 * Validates the loaded configuration values.
 * Returns 0 if valid, negative if invalid.
 */
int hev_tunnel_config_validate(const HevTunnelConfig *config);

#ifdef __cplusplus
}
#endif

#endif /* HEV_SOCKS5_TUNNEL_CONFIG_H */
