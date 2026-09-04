#include <jni.h>
#include <string>
#include <pthread.h>
#include <android/log.h>
#include "utils/Shutdown.h"

extern "C" {
#include "utils/PlatformUtils.h"
#include "utils/Logger.h"
#include "States.h"

// Entry point defined in DialerClient.c
extern void work();
}

#define LOG_TAG "ESurfingNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static pthread_t native_thread;
static bool s_is_stopping = false;

// 启动线程函数
void* start_work_thread(void* arg) {
    LOGI("Native thread started");
    g_start_run_tm = get_cur_tm_ms();
    work();
    LOGI("Native thread finished");
    return NULL;
}

// 异步停止线程函数
void* stop_async_thread(void* arg) {
    LOGI("Async shutdown started");
    shut(0);
    s_is_stopping = false;
    LOGI("Async shutdown finished");
    return NULL;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_esurfingclient_ans_ESurfingService_getNativeStatus(JNIEnv* env, jobject thiz) {
    if (s_is_stopping) return 2; // STOPPING
    if (g_thread_keep_alive) return 1; // RUNNING
    return 0; // STOPPED
}

extern "C" JNIEXPORT void JNICALL
Java_com_esurfingclient_ans_ESurfingService_startNative(JNIEnv* env, jobject thiz, jstring base_dir) {
    // 如果正在运行或正在停止，则禁止重复启动
    if (g_thread_keep_alive || s_is_stopping) {
        LOGI("Native client is already running or stopping");
        return;
    }

    const char* native_base_dir = env->GetStringUTFChars(base_dir, 0);
    set_base_dir(native_base_dir);
    set_log_base_dir(native_base_dir);
    LOGI("Base directory set to: %s", native_base_dir);
    env->ReleaseStringUTFChars(base_dir, native_base_dir);

    g_need_exit = false;
    pthread_create(&native_thread, NULL, start_work_thread, NULL);
    pthread_detach(native_thread); // 允许线程独立运行
}

extern "C" JNIEXPORT void JNICALL
Java_com_esurfingclient_ans_ESurfingService_stopNative(JNIEnv* env, jobject thiz) {
    if (!g_thread_keep_alive || s_is_stopping) {
        LOGI("Native client is not running or already stopping");
        return;
    }

    s_is_stopping = true;
    LOGI("Starting async stop sequence...");

    pthread_t stop_thread;
    if (pthread_create(&stop_thread, NULL, stop_async_thread, NULL) == 0) {
        pthread_detach(stop_thread);
    } else {
        // 如果创建线程失败（极少见），回退到同步模式以确保资源释放
        shut(0);
        s_is_stopping = false;
    }

    LOGI("stopNative returned to UI thread");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_esurfingclient_ans_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "ESurfing Client Native Ready";
    return env->NewStringUTF(hello.c_str());
}
