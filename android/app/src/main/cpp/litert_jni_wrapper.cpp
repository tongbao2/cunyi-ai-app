// LiteRT-LM JNI wrapper for 村医AI
// Bridges Kotlin/Java calls to the native LiteRT-LM C++ runtime

#include <jni.h>
#include <string>
#include <memory>
#include <vector>
#include <android/log.h>
#include <litert.h>
#include <litert_model.h>
#include <litert_session.h>

#define LOG_TAG "CunyiLiteRT"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
    std::unique_ptr<litert::Model> g_model;
    std::string g_model_path;
    bool g_model_loaded = false;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_cunyi_ai_model_LiteRTEngine_initModel(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) return JNI_FALSE;

    LOGI("Initializing LiteRT model from: %s", path);

    try {
        // Detect device hardware (GPU/NPU/CPU)
        litert::EngineOptions options;
        options.accelerator_type = litert::AcceleratorType::GPU;
        options.preferred_num_tokens = 512;

        auto model_result = litert::Model::LoadFromFile(path, options);
        if (!model_result.has_value()) {
            LOGE("Failed to load model: %s", model_result.error().message().c_str());
            env->ReleaseStringUTFChars(model_path, path);
            return JNI_FALSE;
        }

        g_model = std::move(model_result.value());
        g_model_path = path;
        g_model_loaded = true;

        LOGI("Model loaded successfully!");
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_TRUE;

    } catch (const std::exception &e) {
        LOGE("Exception loading model: %s", e.what());
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_cunyi_ai_model_LiteRTEngine_isModelLoaded(JNIEnv *env, jobject thiz) {
    return g_model_loaded ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_cunyi_ai_model_LiteRTEngine_generate(
    JNIEnv *env, jobject thiz, jstring prompt, jint max_tokens, jfloat temperature) {

    if (!g_model_loaded || !g_model) {
        return env->NewStringUTF("Error: Model not loaded");
    }

    const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);
    if (!prompt_str) return env->NewStringUTF("Error: Invalid prompt");

    try {
        litert::SessionOptions sess_opts;
        sess_opts.max_tokens = static_cast<size_t>(max_tokens);
        sess_opts.temperature = temperature;
        sess_opts.do_sample = temperature > 0.0f;

        auto session_result = g_model->CreateSession(sess_opts);
        if (!session_result.has_value()) {
            LOGE("Failed to create session");
            env->ReleaseStringUTFChars(prompt, prompt_str);
            return env->NewStringUTF("Error: Cannot create session");
        }

        auto &session = session_result.value();
        auto output = session->Run(prompt_str);

        env->ReleaseStringUTFChars(prompt, prompt_str);

        if (output.has_value()) {
            return env->NewStringUTF(output.value().c_str());
        } else {
            return env->NewStringUTF("Error: Generation failed");
        }

    } catch (const std::exception &e) {
        LOGE("Generation error: %s", e.what());
        env->ReleaseStringUTFChars(prompt, prompt_str);
        return env->NewStringUTF("Error: Exception during generation");
    }
}

JNIEXPORT jfloatArray JNICALL
Java_com_cunyi_ai_model_LiteRTEngine_getTokenUsage(JNIEnv *env, jobject thiz) {
    // Return [prompt_tokens, generated_tokens, total_tokens]
    jfloat arr[3] = {0, 0, 0};

    jfloatArray result = env->NewFloatArray(3);
    env->SetFloatArrayRegion(result, 0, 3, arr);
    return result;
}

JNIEXPORT void JNICALL
Java_com_cunyi_ai_model_LiteRTEngine_unloadModel(JNIEnv *env, jobject thiz) {
    g_model.reset();
    g_model_loaded = false;
    LOGI("Model unloaded");
}

}
