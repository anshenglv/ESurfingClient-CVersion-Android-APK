cmake_minimum_required(VERSION 3.16)
project(ESurfingClient C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(PROGRAM_VERSION_MAJOR 2)
set(PROGRAM_VERSION_MINOR 1)
set(PROGRAM_VERSION_PATCH 3)
set(PROGRAM_VERSION_RELEASE 5)

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

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif ()

if (WIN32)
    add_definitions(-DCURL_STATICLIB)

    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static -static-libgcc -static-libstdc++")
    set(CMAKE_FIND_LIBRARY_SUFFIXES .a .lib ${CMAKE_FIND_LIBRARY_SUFFIXES})
elseif (ANDROID)
    add_definitions(-DCURL_STATICLIB)

    #set(BUILD_SHARED_LIBS OFF)
    set(CURL_USE_STATIC_LIBS TRUE)

    set(CMAKE_FIND_LIBRARY_SUFFIXES .a ${CMAKE_FIND_LIBRARY_SUFFIXES})
else ()
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -pthread")

    set(BUILD_SHARED_LIBS OFF)
    set(CURL_USE_STATIC_LIBS TRUE)

    set(CMAKE_FIND_LIBRARY_SUFFIXES .a ${CMAKE_FIND_LIBRARY_SUFFIXES})
endif ()

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
        third_party/7z/LzmaDec.c
        third_party/cJSON/cJSON.c
        src/cipher/algo/android/aes_double_cbc_android.c
        src/cipher/algo/android/aes_double_ecb_android.c
        src/cipher/algo/android/des_ecb_six_android.c
        src/cipher/algo/android/desede_double_cbc_android.c
        src/cipher/algo/android/sm4_variant_cbc_android.c
        src/cipher/algo/android/sm4_variant_ecb_android.c
        src/cipher/algo/android/snow3g_variant_android.c
        src/cipher/algo/android/tea_triple_cbc_android.c
        src/cipher/algo/android/tea_triple_ecb_android.c
        src/cipher/algo/ios/ios_zsm.c
        src/cipher/algo/linux/aes_cbc_linux.c
        src/cipher/algo/linux/aes_ecb_linux.c
        src/cipher/algo/linux/des_ecb_six_linux.c
        src/cipher/algo/linux/desede_cbc_linux.c
        src/cipher/algo/linux/mod_xtea_cbc_triple_linux.c
        src/cipher/algo/linux/mod_xtea_linux.c
        src/cipher/CipherFactory.c
        src/cipher/CipherUtils.c
        src/cipher/KeyData.c
        src/clients/dialer/DialerLocation.c
        src/clients/dialer/DialerSession.c
        src/clients/dialer/DialerClient.c
        src/clients/dialer/DialerTicket.c
        src/clients/dialer/DialerAuth.c
        src/clients/dialer/DialerNet.c
        src/clients/net/NetClientCurl.c
        src/clients/net/CheckNetwork.c
        src/clients/net/NetClient.c
        src/config/ConfigTimeWindow.c
        src/config/ConfigAccounts.c
        src/config/ConfigDefaults.c
        src/config/Config.c
        src/states/Refresh.c
        src/states/States.c
        src/utils/sim/SimEvpCipher.c
        src/utils/sim/SimEvpDes3.c
        src/utils/sim/SimEvpAes.c
        src/utils/sim/SimEvpMd5.c
        src/utils/sim/SimThread.c
        src/utils/LogoutState.c
        src/utils/NetAdapters.c
        src/utils/ProcessInfo.c
        src/utils/TimeControl.c
        src/utils/LogControl.c
        src/utils/BytesUtil.c
        src/utils/LoggerDir.c
        src/utils/ExecPath.c
        src/utils/Shutdown.c
        src/utils/TimeUtil.c
        src/utils/Watchdog.c
        src/utils/LogCore.c
        src/utils/LogFile.c
        src/utils/LogMeta.c
        src/utils/StrUtil.c
        src/utils/LogDir.c
        src/utils/Xml.c
        native-lib.cpp
)

if (NOT __OPENWRT__)
    target_sources(ans PRIVATE
            third_party/mongoose/mongoose.c
            src/supervisor/SupervisorSignal.c
            src/supervisor/SupervisorChild.c
            src/supervisor/SupervisorState.c
            src/supervisor/SupervisorLoop.c
            src/webserver/WebLogFiles.c
            src/webserver/WebApiPost.c
            src/webserver/WebApiGet.c
            src/webserver/WebServer.c
            src/webserver/WebServe.c
            src/control/ControlClient.c
            src/control/ControlServer.c
            src/utils/sim/SimProcess.c
            src/utils/Service.c
    )

    add_custom_target(CopyPortal ALL
            COMMAND ${CMAKE_COMMAND} -E remove_directory "${CMAKE_CURRENT_BINARY_DIR}/portal"
            COMMAND ${CMAKE_COMMAND} -E copy_directory
                "${CMAKE_CURRENT_SOURCE_DIR}/portal"
                "${CMAKE_CURRENT_BINARY_DIR}/portal"
            COMMENT "Copy 'portal' to build dir"
    )

    add_dependencies(ans CopyPortal)
endif ()

target_include_directories(ans PRIVATE
        ${CMAKE_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/src
        ${CMAKE_SOURCE_DIR}/third_party
)

if (WIN32)
    target_link_libraries(ans PRIVATE
            CURL::libcurl
            ws2_32
            wldap32
            winmm
            crypt32
            normaliz
            secur32
            bcrypt
            iphlpapi
    )
elseif (ANDROID)
    target_link_libraries(ans PRIVATE
            CURL::libcurl
            log
    )
else ()
    target_link_libraries(ans PRIVATE
            CURL::libcurl
            pthread
            dl
    )
    if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
        target_link_libraries(ESurfingClient PRIVATE
                atomic
                m
        )
    endif ()
endif ()

target_compile_options(ans PRIVATE -Os)

target_compile_options(ans PRIVATE -Wall -Wextra -Wno-unused-parameter)

    set_target_properties(ans PROPERTIES
            LINK_FLAGS "-Wl,--strip-all -Wl,--exclude-libs,ALL"
    )
