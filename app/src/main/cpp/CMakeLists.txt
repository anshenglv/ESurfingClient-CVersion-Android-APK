cmake_minimum_required(VERSION 3.16)
project(ESurfingClient C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(PROGRAM_VERSION_MAJOR 2)
set(PROGRAM_VERSION_MINOR 0)
set(PROGRAM_VERSION_PATCH 4)
set(PROGRAM_VERSION_RELEASE 6)

set(PROGRAM_VERSION "${PROGRAM_VERSION_MAJOR}.${PROGRAM_VERSION_MINOR}.${PROGRAM_VERSION_PATCH}")
set(PROGRAM_FULL_VERSION "${PROGRAM_VERSION}-r${PROGRAM_VERSION_RELEASE}")

add_definitions(-DPROGRAM_VERSION="${PROGRAM_VERSION}")
add_definitions(-DPROGRAM_FULL_VERSION="${PROGRAM_FULL_VERSION}")

message(STATUS "Building version: ${PROGRAM_FULL_VERSION}")

if (NOT DEFINED __OPENWRT__)
    set(__OPENWRT__ 0)
endif ()

if (__OPENWRT__)
    add_definitions(-D__OPENWRT__=1)
    message(STATUS "OpenWRT - yes")
else ()
    message(STATUS "OpenWRT - no")
endif ()

set(WEB_ROOT_DIR "${CMAKE_SOURCE_DIR}/web_root")
add_definitions(-DWEB_ROOT_PATH="${WEB_ROOT_DIR}")

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif ()

if (WIN32)
    add_definitions(-DCURL_STATICLIB)
    add_definitions(-DOPENSSL_NO_DYNAMIC_ENGINE)

    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static -static-libgcc -static-libstdc++")
    set(CMAKE_FIND_LIBRARY_SUFFIXES .a .lib ${CMAKE_FIND_LIBRARY_SUFFIXES})
else ()
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -pthread")

    set(BUILD_SHARED_LIBS OFF)
    set(OPENSSL_USE_STATIC_LIBS TRUE)
    set(CURL_USE_STATIC_LIBS TRUE)

    set(CMAKE_FIND_LIBRARY_SUFFIXES .a ${CMAKE_FIND_LIBRARY_SUFFIXES})
endif ()

if (ANDROID)
    set(ANDROID_ARM64_ROOT "D:/android-arm64")

    set(CURL_INCLUDE_DIR "${ANDROID_ARM64_ROOT}/include")
    set(CURL_LIBRARY "${ANDROID_ARM64_ROOT}/lib/libcurl.a")

    set(OPENSSL_INCLUDE_DIR "${ANDROID_ARM64_ROOT}/include")
    set(OPENSSL_CRYPTO_LIBRARY "${ANDROID_ARM64_ROOT}/lib/libcrypto.a")
    set(OPENSSL_SSL_LIBRARY "${ANDROID_ARM64_ROOT}/lib/libssl.a")

    # Define targets for these static libraries to mimic find_package results
    add_library(CURL::libcurl STATIC IMPORTED)
    set_target_properties(CURL::libcurl PROPERTIES
            IMPORTED_LOCATION "${CURL_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES "${CURL_INCLUDE_DIR}"
            INTERFACE_COMPILE_DEFINITIONS "CURL_STATICLIB"
    )

    add_library(OpenSSL::Crypto STATIC IMPORTED)
    set_target_properties(OpenSSL::Crypto PROPERTIES
            IMPORTED_LOCATION "${OPENSSL_CRYPTO_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES "${OPENSSL_INCLUDE_DIR}"
    )

    add_library(OpenSSL::SSL STATIC IMPORTED)
    set_target_properties(OpenSSL::SSL PROPERTIES
            IMPORTED_LOCATION "${OPENSSL_SSL_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES "${OPENSSL_INCLUDE_DIR}"
            INTERFACE_LINK_LIBRARIES OpenSSL::Crypto
    )

    message(STATUS "Using manual Android ARM64 libraries from ${ANDROID_ARM64_ROOT}")
else()
    find_package(CURL REQUIRED)
    find_package(OpenSSL REQUIRED)
endif()

add_library(ans SHARED
        src/cipher/impl/mod_xtea_cbc_triple_pc.c
        src/cipher/impl/des_ecb_six_pc.c
        src/cipher/impl/desede_cbc_pc.c
        src/cipher/impl/mod_xtea_pc.c
        src/cipher/impl/mod_xtea_iv.c
        src/cipher/impl/aes_ecb_pc.c
        src/cipher/impl/aes_cbc_pc.c
        src/cipher/impl/desede_ecb.c
        src/cipher/impl/desede_cbc.c
        src/cipher/impl/mod_xtea.c
        src/cipher/impl/sm4_ecb.c
        src/cipher/impl/sm4_cbc.c
        src/cipher/impl/aes_ecb.c
        src/cipher/impl/aes_cbc.c
        src/cipher/impl/zuc.c
        src/cipher/CipherFactory.c
        src/cipher/CipherUtils.c
        src/cipher/KeyData.c
        src/utils/PlatformUtils.c
        src/utils/SimThread.c
        src/utils/Shutdown.c
        src/utils/Logger.c
        src/utils/cJSON.c
        src/DialerClient.c
        src/NetClient.c
        src/States.c
        native-lib.cpp
)

target_include_directories(ans PRIVATE
        ${CMAKE_SOURCE_DIR}/inc
)

if (WIN32)
    target_link_libraries(ans PRIVATE
            CURL::libcurl
            OpenSSL::Crypto
            ws2_32
            wldap32
            winmm
            crypt32
            normaliz
            secur32
            bcrypt
            iphlpapi
    )
else ()
    if(ANDROID)
        target_link_libraries(ans PRIVATE
                CURL::libcurl
                OpenSSL::SSL
                OpenSSL::Crypto
                z
                log
                dl
        )
    else()
        target_link_libraries(ans PRIVATE
                CURL::libcurl
                OpenSSL::Crypto
                pthread
                dl
        )
    endif()

    if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
        target_link_libraries(ans PRIVATE
                atomic
                m
        )
        if (__OPENWRT__)
            target_link_libraries(ans PRIVATE
                    OpenSSL::SSL
            )
        endif ()
    endif ()
endif ()

target_compile_options(ans PRIVATE -Os -fvisibility=hidden)

target_compile_options(ans PRIVATE -Wall -Wextra -Wno-unused-parameter)

if (__OPENWRT__ OR ANDROID)
    set_target_properties(ans PROPERTIES
        LINK_FLAGS "-Wl,--strip-all -Wl,--exclude-libs,ALL"
    )
else ()
    add_custom_command(TARGET ans POST_BUILD
        COMMAND ${CMAKE_STRIP} "$<TARGET_FILE:ans>"
)
endif ()

# install(TARGETS ESurfingClient RUNTIME DESTINATION bin)
