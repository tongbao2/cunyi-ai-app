cmake_minimum_required(VERSION 3.28.0)
project("cunyi_ai")

# LiteRT-LM source (cloned as git submodule or downloaded)
# This CMake build pulls in LiteRT-LM and compiles the JNI interface

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Download & configure LiteRT-LM if not present
set(LITERT_LM_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../shared-assets/litert-lm"
    CACHE PATH "Path to LiteRT-LM source")

if(NOT EXISTS "${LITERT_LM_DIR}/CMakeLists.txt")
    message(STATUS "Cloning LiteRT-LM...")
    execute_process(
        COMMAND git clone --depth=1 --branch v0.10.1
                https://github.com/google-ai-edge/LiteRT-LM.git
                "${LITERT_LM_DIR}"
        RESULT_VARIABLE CLONE_RESULT
    )
endif()

# Use FetchContent to bring in LiteRT-LM dependencies
include(FetchContent)

FetchContent_Declare(
    fetch_litert_lm
    SOURCE_DIR "${LITERT_LM_DIR}"
)

FetchContent_MakeAvailable(fetch_litert_lm)

# JNI wrapper that bridges Kotlin <-> LiteRT-LM C++ runtime
add_library(cunyi_litert_jni SHARED
    litert_jni_wrapper.cpp
)

target_include_directories(cunyi_litert_jni PRIVATE
    "${LITERT_LM_DIR}"
    "${CMAKE_CURRENT_SOURCE_DIR}/jni"
)

target_link_libraries(cunyi_litert_jni PRIVATE
    litert_lm_model
    litert_lm_session
    litert_lm_jni_support
    android
    log
)

# Expose symbols for Kotlin to load
target_compile_definitions(cunyi_litert_jni PRIVATE
    JNIEXPORT __attribute__((visibility("default")))
)
