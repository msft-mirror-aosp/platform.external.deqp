/*-------------------------------------------------------------------------
 * drawElements Quality Program Tester Core
 * ----------------------------------------
 *
 * Copyright 2014 The Android Open Source Project
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
 * \brief Android Native Activity.
 *//*--------------------------------------------------------------------*/

#include "tcuAndroidNativeActivity.hpp"
#include "deMemory.h"
#if defined(ENABLE_MULTI_WINDOW_PARALLEL)
#include <android/log.h>
#endif

DE_BEGIN_EXTERN_C

static void onStartCallback(ANativeActivity *activity)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onStart();
}

static void onResumeCallback(ANativeActivity *activity)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onResume();
}

static void *onSaveInstanceStateCallback(ANativeActivity *activity, size_t *outSize)
{
    return static_cast<tcu::Android::NativeActivity *>(activity->instance)->onSaveInstanceState(outSize);
}

static void onPauseCallback(ANativeActivity *activity)
{
    return static_cast<tcu::Android::NativeActivity *>(activity->instance)->onPause();
}

static void onStopCallback(ANativeActivity *activity)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onStop();
}

static void onDestroyCallback(ANativeActivity *activity)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onDestroy();
}

static void onWindowFocusChangedCallback(ANativeActivity *activity, int hasFocus)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onWindowFocusChanged(hasFocus);
}

static void onNativeWindowCreatedCallback(ANativeActivity *activity, ANativeWindow *window)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onNativeWindowCreated(window);
}

static void onNativeWindowResizedCallback(ANativeActivity *activity, ANativeWindow *window)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onNativeWindowResized(window);
}

static void onNativeWindowRedrawNeededCallback(ANativeActivity *activity, ANativeWindow *window)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onNativeWindowRedrawNeeded(window);
}

static void onNativeWindowDestroyedCallback(ANativeActivity *activity, ANativeWindow *window)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onNativeWindowDestroyed(window);
}

static void onInputQueueCreatedCallback(ANativeActivity *activity, AInputQueue *queue)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onInputQueueCreated(queue);
}

static void onInputQueueDestroyedCallback(ANativeActivity *activity, AInputQueue *queue)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onInputQueueDestroyed(queue);
}

static void onContentRectChangedCallback(ANativeActivity *activity, const ARect *rect)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onContentRectChanged(rect);
}

static void onConfigurationChangedCallback(ANativeActivity *activity)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onConfigurationChanged();
}

static void onLowMemoryCallback(ANativeActivity *activity)
{
    static_cast<tcu::Android::NativeActivity *>(activity->instance)->onLowMemory();
}

DE_END_EXTERN_C

namespace tcu
{
namespace Android
{

#if defined(ENABLE_MULTI_WINDOW_PARALLEL)
namespace {
/**
 * Checks for pending Java exceptions, logs them, and clears the state.
 */
bool checkAndClearException(JNIEnv* env, const char* context) {
    if (env->ExceptionCheck()) {
        __android_log_print(ANDROID_LOG_ERROR, "dEQP", "Java exception during %s", context);
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

/**
 * Centralized Layout Parameter Application.
 * Uses static caching to ensure O(1) performance after the first call.
 */
void applyLayoutParams(JNIEnv* env, jobject layoutParamsObj, int x, int y, int w, int h) {
    static jfieldID fidX = nullptr, fidY = nullptr, fidW = nullptr, fidH = nullptr, fidG = nullptr;
    static bool idsCached = false;

    if (!idsCached) {
        jclass lpCls = env->GetObjectClass(layoutParamsObj);
        fidX = env->GetFieldID(lpCls, "x", "I");
        fidY = env->GetFieldID(lpCls, "y", "I");
        fidW = env->GetFieldID(lpCls, "width", "I");
        fidH = env->GetFieldID(lpCls, "height", "I");
        fidG = env->GetFieldID(lpCls, "gravity", "I");
        idsCached = true;
        env->DeleteLocalRef(lpCls);
    }

    if (fidX) env->SetIntField(layoutParamsObj, fidX, x);
    if (fidY) env->SetIntField(layoutParamsObj, fidY, y);
    if (fidW) env->SetIntField(layoutParamsObj, fidW, w);
    if (fidH) env->SetIntField(layoutParamsObj, fidH, h);
    if (fidG) env->SetIntField(layoutParamsObj, fidG, 0x33); // TOP | LEFT
}

/**
 * Helper to get the Window Object.
 */
jobject getWindowObject(JNIEnv* env, jobject activityObj, jclass activityClass) {
    jmethodID getWindowMethod = env->GetMethodID(activityClass, "getWindow", "()Landroid/view/Window;");
    return env->CallObjectMethod(activityObj, getWindowMethod);
}

/**
 * Helper to extract Intent extras.
 */
int getIntentIntExtra(JNIEnv* env, jobject intentObj, jclass intentClass, const char* extraKey) {
    jstring keyString = env->NewStringUTF(extraKey);
    jmethodID getIntExtraMethod = env->GetMethodID(intentClass, "getIntExtra", "(Ljava/lang/String;I)I");
    int value = (getIntExtraMethod) ? env->CallIntMethod(intentObj, getIntExtraMethod, keyString, 0) : 0;
    env->DeleteLocalRef(keyString);
    return value;
}

} // anonymous namespace

bool tcu::Android::NativeActivity::setWindowParams(void)
{
    JNIEnv* env = m_activity->env;
    jobject activityObj = m_activity->clazz;
    jclass activityClass = env->GetObjectClass(activityObj);

    // 1. Get Intent and Extract Extras
    jmethodID getIntentMethod = env->GetMethodID(activityClass, "getIntent", "()Landroid/content/Intent;");
    jobject intentObj = env->CallObjectMethod(activityObj, getIntentMethod);
    if (!intentObj) {
        env->DeleteLocalRef(activityClass);
        return false;
    }

    jclass intentClass = env->GetObjectClass(intentObj);
    int x = getIntentIntExtra(env, intentObj, intentClass, "windowX");
    int y = getIntentIntExtra(env, intentObj, intentClass, "windowY");
    int w = getIntentIntExtra(env, intentObj, intentClass, "windowWidth");
    int h = getIntentIntExtra(env, intentObj, intentClass, "windowHeight");

    env->DeleteLocalRef(intentClass);
    env->DeleteLocalRef(intentObj);

    // 2. Validate - if no dimensions are provided, this is Case 1: Standard Run.
    if (w <= 0 || h <= 0) {
        env->DeleteLocalRef(activityClass);
        return false;
    }
    x = std::max(0, x);
    y = std::max(0, y);

    // 3. Access Window and LayoutParams
    jobject windowObj = getWindowObject(env, activityObj, activityClass);
    if (windowObj) {
        jclass windowClass = env->GetObjectClass(windowObj);
        jmethodID getAttrMethod = env->GetMethodID(windowClass, "getAttributes", "()Landroid/view/WindowManager$LayoutParams;");
        jobject lpObj = env->CallObjectMethod(windowObj, getAttrMethod);

        if (lpObj) {
            // 4. Apply parameters via subroutine
            applyLayoutParams(env, lpObj, x, y, w, h);

            jmethodID setAttrMethod = env->GetMethodID(windowClass, "setAttributes", "(Landroid/view/WindowManager$LayoutParams;)V");
            env->CallVoidMethod(windowObj, setAttrMethod, lpObj);
            env->DeleteLocalRef(lpObj);
        }
        env->DeleteLocalRef(windowClass);
        env->DeleteLocalRef(windowObj);
    }

    env->DeleteLocalRef(activityClass);

    // Final check to ensure no JNI errors are left pending.
    return !checkAndClearException(env, "setWindowParams");
}
#endif

NativeActivity::NativeActivity(ANativeActivity *activity)
    : m_activity(activity)
#if defined(ENABLE_MULTI_WINDOW_PARALLEL)
    , m_multiParallelWindow(false)
#endif
{
    if (activity)
    {
        activity->instance                              = (void *)this;
        activity->callbacks->onStart                    = onStartCallback;
        activity->callbacks->onResume                   = onResumeCallback;
        activity->callbacks->onSaveInstanceState        = onSaveInstanceStateCallback;
        activity->callbacks->onPause                    = onPauseCallback;
        activity->callbacks->onStop                     = onStopCallback;
        activity->callbacks->onDestroy                  = onDestroyCallback;
        activity->callbacks->onWindowFocusChanged       = onWindowFocusChangedCallback;
        activity->callbacks->onNativeWindowCreated      = onNativeWindowCreatedCallback;
        activity->callbacks->onNativeWindowResized      = onNativeWindowResizedCallback;
        activity->callbacks->onNativeWindowRedrawNeeded = onNativeWindowRedrawNeededCallback;
        activity->callbacks->onNativeWindowDestroyed    = onNativeWindowDestroyedCallback;
        activity->callbacks->onInputQueueCreated        = onInputQueueCreatedCallback;
        activity->callbacks->onInputQueueDestroyed      = onInputQueueDestroyedCallback;
        activity->callbacks->onContentRectChanged       = onContentRectChangedCallback;
        activity->callbacks->onConfigurationChanged     = onConfigurationChangedCallback;
        activity->callbacks->onLowMemory                = onLowMemoryCallback;
#if defined(ENABLE_MULTI_WINDOW_PARALLEL)
        m_multiParallelWindow = setWindowParams();
#endif
    }
}

NativeActivity::~NativeActivity(void)
{
}

void NativeActivity::onStart(void)
{
}

void NativeActivity::onResume(void)
{
}

void *NativeActivity::onSaveInstanceState(size_t *outSize)
{
    *outSize = 0;
    return nullptr;
}

void NativeActivity::onPause(void)
{
}

void NativeActivity::onStop(void)
{
}

void NativeActivity::onDestroy(void)
{
}

void NativeActivity::onWindowFocusChanged(int hasFocus)
{
    DE_UNREF(hasFocus);
}

void NativeActivity::onNativeWindowCreated(ANativeWindow *window)
{
    DE_UNREF(window);
}

void NativeActivity::onNativeWindowResized(ANativeWindow *window)
{
    DE_UNREF(window);
}

void NativeActivity::onNativeWindowRedrawNeeded(ANativeWindow *window)
{
    DE_UNREF(window);
}

void NativeActivity::onNativeWindowDestroyed(ANativeWindow *window)
{
    DE_UNREF(window);
}

void NativeActivity::onInputQueueCreated(AInputQueue *queue)
{
    DE_UNREF(queue);
}

void NativeActivity::onInputQueueDestroyed(AInputQueue *queue)
{
    DE_UNREF(queue);
}

void NativeActivity::onContentRectChanged(const ARect *rect)
{
    DE_UNREF(rect);
}

void NativeActivity::onConfigurationChanged(void)
{
}

void NativeActivity::onLowMemory(void)
{
}

void NativeActivity::finish(void)
{
    ANativeActivity_finish(m_activity);
}

} // namespace Android
} // namespace tcu
