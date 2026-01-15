#include <jni.h>
#include <string>
#include <vector>
#include <tuple>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "PI_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Forward declarations from queen.cpp
std::vector<std::tuple<int, int>> queen_process(std::vector<std::tuple<std::string, int>> &inp);

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_pi_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_pi_UpdateActivity_processFiles(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray filePaths,
        jintArray keywords) {
    
    int len = env->GetArrayLength(filePaths);
    jint* keywordsPtr = env->GetIntArrayElements(keywords, nullptr);
    
    std::vector<std::tuple<std::string, int>> input;
    for (int i = 0; i < len; i++) {
        jstring pathStr = (jstring)env->GetObjectArrayElement(filePaths, i);
        const char* pathChars = env->GetStringUTFChars(pathStr, nullptr);
        
        input.emplace_back(std::string(pathChars), (int)keywordsPtr[i]);
        
        LOGI("Processing file: %s with keyword: %d", pathChars, keywordsPtr[i]);
        
        env->ReleaseStringUTFChars(pathStr, pathChars);
        env->DeleteLocalRef(pathStr);
    }
    
    env->ReleaseIntArrayElements(keywords, keywordsPtr, JNI_ABORT);
    
    // Call the redirection logic in queen.cpp
    auto result = queen_process(input);
    
    LOGI("Processing complete. Result size: %zu", result.size());
    
    std::string resultMsg = "Processed " + std::to_string(len) + " files. Internal index size: " + std::to_string(result.size());
    return env->NewStringUTF(resultMsg.c_str());
}