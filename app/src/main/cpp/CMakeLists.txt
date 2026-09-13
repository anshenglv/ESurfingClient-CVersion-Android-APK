cmake_minimum_required(VERSION 3.16)
project(ESurfingClient C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if (NOT DEFINED __OPENWRT__)
    set(__OPENWRT__ 0)
endif ()

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif ()

    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -pthread")

    set(BUILD_SHARED_LIBS OFF)

    set(CURL_USE_STATIC_LIBS TRUE)

    set(CMAKE_FIND_LIBRARY_SUFFIXES .a ${CMAKE_FIND_LIBRARY_SUFFIXES})

    set(ANDROID_ARM64_ROOT "D:/curl-android-arm64")

    set(CURL_INCLUDE_DIR "${ANDROID_ARM64_ROOT}/include")
    set(CURL_LIBRARY "${ANDROID_ARM64_ROOT}/lib/libcurl.a")

    # Define targets for these static libraries to mimic find_package results
    add_library(CURL::libcurl STATIC IMPORTED)
    set_target_properties(CURL::libcurl PROPERTIES
            IMPORTED_LOCATION "${CURL_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES "${CURL_INCLUDE_DIR}"
            INTERFACE_COMPILE_DEFINITIONS "CURL_STATICLIB"
    )

    message(STATUS "Using manual Android ARM64 libraries from ${ANDROID_ARM64_ROOT}")

add_library(ans SHARED
        src/cipher/algo/android/aes_double_cbc_android.c
        src/cipher/algo/android/aes_double_ecb_android.c
        src/cipher/algo/android/des_ecb_six_android.c
        src/cipher/algo/android/desede_double_cbc_android.c
        src/cipher/algo/android/sm4_variant_cbc_android.c
        src/cipher/algo/android/sm4_variant_ecb_android.c
        src/cipher/algo/android/snow3g_variant_android.c
        src/cipher/algo/android/tea_triple_cbc_android.c
        src/cipher/algo/android/tea_triple_ecb_android.c
        src/cipher/algo/android/aes_double_cbc_android.c
        src/cipher/algo/android/aes_double_ecb_android.c
        src/cipher/algo/android/des_ecb_six_android.c
        src/cipher/algo/ios/lzma/LzmaDec.c
        src/cipher/algo/ios/ios_zsm.c
        src/cipher/algo/linux/mod_xtea_cbc_triple_linux.c
        src/cipher/algo/linux/des_ecb_six_linux.c
        src/cipher/algo/linux/desede_cbc_linux.c
        src/cipher/algo/linux/mod_xtea_linux.c
        src/cipher/algo/linux/aes_ecb_linux.c
        src/cipher/algo/linux/aes_cbc_linux.c
        src/cipher/CipherFactory.c
        src/cipher/CipherUtils.c
        src/cipher/KeyData.c
        src/utils/PlatformUtils.c
        src/utils/SimThread.c
        src/utils/Shutdown.c
        src/utils/Logger.c
        src/utils/cJSON.c
        src/utils/simssl/evp.c
        src/DialerClient.c
        src/utils/TimeControl.c
        src/NetClient.c
        src/States.c
        native-lib.cpp
)

target_include_directories(ans PRIVATE
        ${CMAKE_SOURCE_DIR}/inc
        ${CMAKE_SOURCE_DIR}/src/cipher/algo/ios/lzma
)
        target_link_libraries(ans PRIVATE
                CURL::libcurl
                z
                log
                dl
        )

target_compile_options(ans PRIVATE -Os)

target_compile_options(ans PRIVATE -Wall -Wextra -Wno-unused-parameter)

    set_target_properties(ans PROPERTIES
            LINK_FLAGS "-Wl,--strip-all -Wl,--exclude-libs,ALL"
    )
