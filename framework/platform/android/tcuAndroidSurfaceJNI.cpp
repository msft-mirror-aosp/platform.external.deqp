//
// Created by Gopinath Chennakeswaran on 20/05/26.
//

#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <android/native_activity.h>
#include <android/asset_manager_jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <cstdlib>

#include "tcuApp.hpp"
#include "tcuCommandLine.hpp"
#include "tcuTestLog.hpp"
#include "tcuResource.hpp"

#include "tcuAndroidPlatform.hpp"
#include "tcuAndroidWindow.hpp"
#include "tcuAndroidUtil.hpp"
#include "tcuAndroidAssets.hpp" // <-- ADDED: Needed for AssetArchive

//Java_com_drawelements_deqp_DeqpWorkerService_nativeStartDeqp
extern "C" JNIEXPORT void JNICALL Java_com_drawelements_deqp_parallelrunner_DeqpWorkerService_nativeStartDeqp(
        JNIEnv* env, jobject thiz, jobject jSurface, jstring jArgs, jobject jAssetManager) {

    __android_log_print(ANDROID_LOG_ERROR, "dEQP", "nativeStartDeqp called for service:%p, surface:%p", thiz, jSurface);

    ANativeWindow* window = ANativeWindow_fromSurface(env, jSurface);
    if (!window) return;

    const char* argsCStr = env->GetStringUTFChars(jArgs, nullptr);
    std::string argsStr(argsCStr);
    env->ReleaseStringUTFChars(jArgs, argsCStr);

    std::vector<char*> argv;
    argv.push_back(strdup("deqp")); // dummy argv[0]

    size_t pos = 0;
    while ((pos = argsStr.find(' ')) != std::string::npos) {
        if (pos > 0) argv.push_back(strdup(argsStr.substr(0, pos).c_str()));
        argsStr.erase(0, pos + 1);
    }
    if (!argsStr.empty()) argv.push_back(strdup(argsStr.c_str()));
    int argc = (int)argv.size();

    AAssetManager* assetManager = nullptr;
    if (jAssetManager != nullptr) {
        assetManager = AAssetManager_fromJava(env, jAssetManager);
    }

    try {
        JavaVM* vm = nullptr;
        env->GetJavaVM(&vm);

        tcu::Android::Platform platform(vm, thiz, window);

        tcu::CommandLine cmdLine(argc, &argv[0]);

        tcu::Android::AssetArchive archive(assetManager);

        tcu::TestLog log(cmdLine.getLogFileName(), cmdLine.getLogFlags());

        tcu::App app(platform, archive, log, cmdLine);

        for (;;) {
            if (!app.iterate()) break;
        }

    } catch (...) {
// Driver crashed. Let it fail gracefully so the process can be killed.
    }

// Cleanup
    for (char* arg : argv) free(arg);
    ANativeWindow_release(window);
}
