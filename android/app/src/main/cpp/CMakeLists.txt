cmake_minimum_required(VERSION 3.22.1)
project("hev-socks5-tunnel" C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Enable POSIX & GNU extensions
add_definitions(-D_GNU_SOURCE)

# Optimization and security flags for Android production build
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -Wall -Wextra -fPIC -fvisibility=hidden -pthread")

# Include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# C sources comprising hev-socks5-tunnel and JNI bridge
set(HEV_TUNNEL_SOURCES
    hev-socks5-tunnel-bridge.c
    hev-socks5-tunnel-core.c
    hev-socks5-tunnel-config.c
    hev-task-system.c
)

# Build shared library libhev-socks5-tunnel.so
add_library(
    hev-socks5-tunnel
    SHARED
    ${HEV_TUNNEL_SOURCES}
)

# Find Android system logging and native runtime libraries
find_library(log-lib log)
find_library(android-lib android)

# Link dependencies
target_link_libraries(
    hev-socks5-tunnel
    ${log-lib}
    ${android-lib}
)
