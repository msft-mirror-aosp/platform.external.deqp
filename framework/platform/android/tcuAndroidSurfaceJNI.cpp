/*-------------------------------------------------------------------------
 * drawElements Quality Program Tester Core
 * ----------------------------------------
 *
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *//*!
 * \file
 * \brief Android Surface JNI.
 *//*--------------------------------------------------------------------*/

#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <android/native_activity.h>
#include <android/asset_manager_jni.h>
#include <stdexcept>
#include <string>
#include <memory>
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
#include "tcuAndroidAssets.hpp"
#include "tcuTestSessionListener.hpp"

// JNI listener implementation to dispatch C++ events to the Java service
class JniTestSessionListener : public tcu::TestSessionListener
{
public:
    JniTestSessionListener(JavaVM *vm, jobject listenerObj)
        : m_vm(vm)
        , m_listenerObj(nullptr)
        , m_beginSessionID(nullptr)
        , m_endSessionID(nullptr)
        , m_sessionInfoID(nullptr)
        , m_beginTestCaseID(nullptr)
        , m_endTestCaseID(nullptr)
        , m_testCaseResultID(nullptr)
        , m_terminateTestCaseID(nullptr)
    {
        TCU_CHECK_INTERNAL(m_vm != nullptr);
        TCU_CHECK_INTERNAL(listenerObj != nullptr);

        tcu::Android::ScopedJNIEnv env(m_vm);
        JNIEnv *jniEnv = env.getEnv();

        jclass cls = jniEnv->GetObjectClass(listenerObj);
        TCU_CHECK_INTERNAL(cls != nullptr);

        m_beginSessionID      = getMethodID(jniEnv, cls, "beginSession", "()V");
        m_endSessionID        = getMethodID(jniEnv, cls, "endSession", "()V");
        m_sessionInfoID       = getMethodID(jniEnv, cls, "sessionInfo", "(Ljava/lang/String;Ljava/lang/String;)V");
        m_beginTestCaseID     = getMethodID(jniEnv, cls, "beginTestCase", "(Ljava/lang/String;)V");
        m_endTestCaseID       = getMethodID(jniEnv, cls, "endTestCase", "()V");
        m_testCaseResultID    = getMethodID(jniEnv, cls, "testCaseResult", "(Ljava/lang/String;Ljava/lang/String;)V");
        m_terminateTestCaseID = getMethodID(jniEnv, cls, "terminateTestCase", "(Ljava/lang/String;)V");

        jniEnv->DeleteLocalRef(cls);

        m_listenerObj = jniEnv->NewGlobalRef(listenerObj);
        TCU_CHECK_INTERNAL(m_listenerObj != nullptr);
    }

    virtual ~JniTestSessionListener()
    {
        if (m_listenerObj && m_vm)
        {
            try
            {
                tcu::Android::ScopedJNIEnv env(m_vm);
                env.getEnv()->DeleteGlobalRef(m_listenerObj);
            }
            catch (const std::exception &)
            {
                // Destructors should not throw.
            }
        }
    }

    JniTestSessionListener(const JniTestSessionListener &)            = delete;
    JniTestSessionListener &operator=(const JniTestSessionListener &) = delete;

    // QPA Log-Equivalent Overrides
    virtual void beginSession(void) override
    {
        tcu::Android::ScopedJNIEnv env(m_vm);
        JNIEnv *jniEnv = env.getEnv();
        jniEnv->CallVoidMethod(m_listenerObj, m_beginSessionID);
        checkAndClearException(jniEnv);
    }

    virtual void endSession(void) override
    {
        tcu::Android::ScopedJNIEnv env(m_vm);
        JNIEnv *jniEnv = env.getEnv();
        jniEnv->CallVoidMethod(m_listenerObj, m_endSessionID);
        checkAndClearException(jniEnv);
    }

    virtual void sessionInfo(const char *name, const char *value) override
    {
        TCU_CHECK_INTERNAL(name != nullptr);
        TCU_CHECK_INTERNAL(value != nullptr);
        tcu::Android::ScopedJNIEnv env(m_vm);
        JNIEnv *jniEnv = env.getEnv();
        jstring jName  = jniEnv->NewStringUTF(name);
        checkAndClearException(jniEnv);
        jstring jValue = jniEnv->NewStringUTF(value);
        if (!jValue)
        {
            jniEnv->DeleteLocalRef(jName);
            checkAndClearException(jniEnv);
        }
        jniEnv->CallVoidMethod(m_listenerObj, m_sessionInfoID, jName, jValue);
        jniEnv->DeleteLocalRef(jName);
        jniEnv->DeleteLocalRef(jValue);
        checkAndClearException(jniEnv);
    }

    virtual void beginTestCase(const char *testCasePath) override
    {
        TCU_CHECK_INTERNAL(testCasePath != nullptr);
        tcu::Android::ScopedJNIEnv env(m_vm);
        JNIEnv *jniEnv = env.getEnv();
        jstring jPath  = jniEnv->NewStringUTF(testCasePath);
        checkAndClearException(jniEnv);
        jniEnv->CallVoidMethod(m_listenerObj, m_beginTestCaseID, jPath);
        jniEnv->DeleteLocalRef(jPath);
        checkAndClearException(jniEnv);
    }

    virtual void endTestCase(void) override
    {
        tcu::Android::ScopedJNIEnv env(m_vm);
        JNIEnv *jniEnv = env.getEnv();
        jniEnv->CallVoidMethod(m_listenerObj, m_endTestCaseID);
        checkAndClearException(jniEnv);
    }

    virtual void testCaseResult(const char *statusCode, const char *details) override
    {
        TCU_CHECK_INTERNAL(statusCode != nullptr);
        TCU_CHECK_INTERNAL(details != nullptr);
        tcu::Android::ScopedJNIEnv env(m_vm);
        JNIEnv *jniEnv      = env.getEnv();
        jstring jStatusCode = jniEnv->NewStringUTF(statusCode);
        checkAndClearException(jniEnv);
        jstring jDetails = jniEnv->NewStringUTF(details);
        if (!jDetails)
        {
            jniEnv->DeleteLocalRef(jStatusCode);
            checkAndClearException(jniEnv);
        }
        jniEnv->CallVoidMethod(m_listenerObj, m_testCaseResultID, jStatusCode, jDetails);
        jniEnv->DeleteLocalRef(jStatusCode);
        jniEnv->DeleteLocalRef(jDetails);
        checkAndClearException(jniEnv);
    }

    virtual void terminateTestCase(const char *reason) override
    {
        TCU_CHECK_INTERNAL(reason != nullptr);
        tcu::Android::ScopedJNIEnv env(m_vm);
        JNIEnv *jniEnv  = env.getEnv();
        jstring jReason = jniEnv->NewStringUTF(reason);
        checkAndClearException(jniEnv);
        jniEnv->CallVoidMethod(m_listenerObj, m_terminateTestCaseID, jReason);
        jniEnv->DeleteLocalRef(jReason);
        checkAndClearException(jniEnv);
    }

private:
    static void checkAndClearException(JNIEnv *env)
    {
        if (env->ExceptionCheck())
        {
            env->ExceptionDescribe();
            env->ExceptionClear();
            throw std::runtime_error("Got JNI exception");
        }
    }

    static jmethodID getMethodID(JNIEnv *env, jclass cls, const char *name, const char *sig)
    {
        jmethodID id = env->GetMethodID(cls, name, sig);
        checkAndClearException(env);
        TCU_CHECK_INTERNAL(id != nullptr);
        return id;
    }

    JavaVM *m_vm;
    jobject m_listenerObj;
    jmethodID m_beginSessionID;
    jmethodID m_endSessionID;
    jmethodID m_sessionInfoID;
    jmethodID m_beginTestCaseID;
    jmethodID m_endTestCaseID;
    jmethodID m_testCaseResultID;
    jmethodID m_terminateTestCaseID;
};

//Java_com_drawelements_deqp_parallelrunner_WorkerService_nativeStartDeqp
extern "C" JNIEXPORT void JNICALL Java_com_drawelements_deqp_parallelrunner_WorkerService_nativeStartDeqp(
    JNIEnv *env, jobject thiz, jobject jSurface, jstring jArgs, jobject jAssetManager, jobject jListener)
{

    __android_log_print(ANDROID_LOG_ERROR, "dEQP", "nativeStartDeqp called for service:%p, surface:%p", thiz, jSurface);

    ANativeWindow *window = ANativeWindow_fromSurface(env, jSurface);
    if (!window)
        return;

    const char *argsCStr = env->GetStringUTFChars(jArgs, nullptr);
    std::string argsStr(argsCStr);
    env->ReleaseStringUTFChars(jArgs, argsCStr);

    std::vector<char *> argv;
    argv.push_back(strdup("deqp")); // dummy argv[0]

    size_t pos = 0;
    while ((pos = argsStr.find(' ')) != std::string::npos)
    {
        if (pos > 0)
            argv.push_back(strdup(argsStr.substr(0, pos).c_str()));
        argsStr.erase(0, pos + 1);
    }
    if (!argsStr.empty())
        argv.push_back(strdup(argsStr.c_str()));
    int argc = (int)argv.size();

    AAssetManager *assetManager = nullptr;
    if (jAssetManager != nullptr)
    {
        assetManager = AAssetManager_fromJava(env, jAssetManager);
    }

    try
    {
        JavaVM *vm = nullptr;
        env->GetJavaVM(&vm);

        tcu::Android::Platform platform(vm, thiz, window);

        tcu::CommandLine cmdLine(argc, &argv[0]);

        tcu::Android::AssetArchive archive(assetManager);

        tcu::TestLog log(cmdLine.getLogFileName(), cmdLine.getLogFlags());

        std::unique_ptr<JniTestSessionListener> jniListener;
        if (jListener)
        {
            jniListener.reset(new JniTestSessionListener(vm, jListener));
        }

        tcu::App app(platform, archive, log, cmdLine, jniListener.get());

        for (;;)
        {
            if (!app.iterate())
                break;
        }
    }
    catch (const std::exception &e)
    {
        __android_log_print(ANDROID_LOG_ERROR, "dEQP", "nativeStartDeqp caught std::exception: %s", e.what());
    }
    catch (...)
    {
        __android_log_print(ANDROID_LOG_ERROR, "dEQP", "nativeStartDeqp caught unknown exception!");
    }

    // Cleanup
    for (char *arg : argv)
        free(arg);
    ANativeWindow_release(window);
}
