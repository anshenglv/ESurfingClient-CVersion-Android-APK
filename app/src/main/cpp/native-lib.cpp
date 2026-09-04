#include <jni.h>
#include <string>
#include <pthread.h>
#include <android/log.h>

extern "C" {
#include "utils/PlatformUtils.h"
#include "utils/Logger.h"
#include "States.h"

// Defined in DialerClient.c
extern void work();
extern bool g_thread_keep_alive;
extern bool g_need_exit;
}

#define LOG_TAG "ESurfingNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static pthread_t native_thread;

// Defined in States.c
extern uint64_t g_start_run_tm;

void* start_work_thread(void* arg) {
    LOGI("Native thread started");
    g_start_run_tm = get_cur_tm_ms();
    work();
    LOGI("Native thread finished");
    return NULL;
}

extern "C" JNIEXPORT void JNICALL
Java_com_esurfingclient_ans_ESurfingService_startNative(JNIEnv* env, jobject thiz, jstring base_dir) {
    if (g_thread_keep_alive) {
        LOGI("Native client is already running");
        return;
    }

    const char* native_base_dir = env->GetStringUTFChars(base_dir, 0);
    set_base_dir(native_base_dir);
    set_log_base_dir(native_base_dir);
    LOGI("Base directory set to: %s", native_base_dir);
    env->ReleaseStringUTFChars(base_dir, native_base_dir);

    g_need_exit = false;
    pthread_create(&native_thread, NULL, start_work_thread, NULL);
}

extern "C" JNIEXPORT void JNICALL
Java_com_esurfingclient_ans_ESurfingService_stopNative(JNIEnv* env, jobject thiz) {
    if (!g_thread_keep_alive) {
        LOGI("Native client is not running");
        return;
    }
    LOGI("Stopping native client...");
    g_thread_keep_alive = false;
    g_need_exit = true;

    // Do NOT join on the UI thread to avoid ANR.
    // The native thread will clean itself up and finish.
    // pthread_join(native_thread, NULL);

    LOGI("Native client stop requested (async)");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_esurfingclient_ans_MainActivity_isNativeRunning(JNIEnv* env, jobject thiz) {
    return (jboolean)g_thread_keep_alive;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_esurfingclient_ans_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "ESurfing Client Native Ready";
    return env->NewStringUTF(hello.c_str());
}
