#!/bin/bash

# Build native libraries for Ubuntu on Android

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building Ubuntu on Android native libraries${NC}"

# Detect OS
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    HOST_OS="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    HOST_OS="darwin"
else
    echo -e "${RED}Unsupported OS: $OSTYPE${NC}"
    exit 1
fi

# Find NDK
if [ -z "$ANDROID_NDK" ]; then
    # Try to find NDK in common locations
    NDK_PATHS=(
        "$HOME/Android/Sdk/ndk"
        "/opt/android-ndk"
        "$HOME/Android/Sdk/ndk-bundle"
    )
    
    for path in "${NDK_PATHS[@]}"; do
        if [ -d "$path" ]; then
            NDK_VERSION=$(ls "$path" | grep -E '^[0-9]' | sort -V | tail -1)
            ANDROID_NDK="$path/$NDK_VERSION"
            break
        fi
    done
    
    if [ -z "$ANDROID_NDK" ]; then
        echo -e "${RED}Android NDK not found. Please set ANDROID_NDK environment variable${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}Using NDK: $ANDROID_NDK${NC}"

# ABIs to build
ABIS=("arm64-v8a" "armeabi-v7a" "x86_64")

# Create build directories
BUILD_DIR="$(pwd)/build"
mkdir -p "$BUILD_DIR"

# Build PRoot for each ABI
for ABI in "${ABIS[@]}"; do
    echo -e "${YELLOW}Building PRoot for $ABI...${NC}"
    
    BUILD_ABI_DIR="$BUILD_DIR/$ABI"
    mkdir -p "$BUILD_ABI_DIR"
    
    cd "$BUILD_ABI_DIR"
    
    cmake \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=$ABI \
        -DANDROID_PLATFORM=android-24 \
        -DCMAKE_BUILD_TYPE=Release \
        ../..
    
    cmake --build . --target proot -- -j$(nproc)
    
    echo -e "${GREEN}✓ PRoot for $ABI built${NC}"
done

# Build VNC bridge for each ABI
for ABI in "${ABIS[@]}"; do
    echo -e "${YELLOW}Building VNC bridge for $ABI...${NC}"
    
    BUILD_ABI_DIR="$BUILD_DIR/$ABI"
    cd "$BUILD_ABI_DIR"
    
    cmake --build . --target vnc-bridge -- -j$(nproc)
    
    echo -e "${GREEN}✓ VNC bridge for $ABI built${NC}"
done

# Build JNI library for each ABI
for ABI in "${ABIS[@]}"; do
    echo -e "${YELLOW}Building JNI bridge for $ABI...${NC}"
    
    BUILD_ABI_DIR="$BUILD_DIR/$ABI"
    cd "$BUILD_ABI_DIR"
    
    cmake --build . --target udroid-native -- -j$(nproc)
    
    echo -e "${GREEN}✓ JNI bridge for $ABI built${NC}"
done

# Copy libraries to app jniLibs
echo -e "${YELLOW}Copying libraries to app...${NC}"
APP_JNI_DIR="$(pwd)/../app/src/main/jniLibs"
mkdir -p "$APP_JNI_DIR"

for ABI in "${ABIS[@]}"; do
    ABI_LIB_DIR="$APP_JNI_DIR/$ABI"
    mkdir -p "$ABI_LIB_DIR"
    
    cp "$BUILD_DIR/$ABI/libudroid-native.so" "$ABI_LIB_DIR/"
    
    echo -e "${GREEN}✓ Copied libraries for $ABI${NC}"
done

echo -e "${GREEN}✓ Native build complete!${NC}"
