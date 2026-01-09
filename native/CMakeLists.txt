cmake_minimum_required(VERSION 3.22.1)

project(udroid-native C CXX ASM)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

# PRoot library
add_library(proot STATIC
    src/proot/proot.c
    src/proot/cli.c
)

target_include_directories(proot PRIVATE
    ${CMAKE_SOURCE_DIR}/include
)

# VNC Bridge library
add_library(vnc-bridge SHARED
    src/vnc/bridge.cpp
    src/vnc/rfb.c
)

target_include_directories(vnc-bridge PRIVATE
    ${CMAKE_SOURCE_DIR}/include
    ${ANDROID_NDK}/sources/android/cpufeatures
)

# Link libraries
target_link_libraries(vnc-bridge
    log
    android
)

# JNI Bridge
add_library(udroid-native SHARED
    src/jni/udroid_jni.cpp
)

target_include_directories(udroid-native PRIVATE
    ${CMAKE_SOURCE_DIR}/include
)

target_link_libraries(udroid-native
    log
    android
    proot
    vnc-bridge
)
