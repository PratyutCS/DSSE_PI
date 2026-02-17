#include <jni.h>
#include <string>
#include <vector>
#include <tuple>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "PI_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Forward declarations from queen.cpp
std::vector<std::tuple<std::string, std::string>> queen_process(std::vector<std::tuple<std::string, int>> &inp);
std::tuple<std::string, std::string, int> queen_search_client(std::string keyword);
std::vector<int> queen_post_process(int index_range, std::string res1_0, std::string res1_1, std::string res2_0, std::string res2_1);

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_pi_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_pi_UpdateActivity_generateTokens(
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
        
        env->ReleaseStringUTFChars(pathStr, pathChars);
        env->DeleteLocalRef(pathStr);
    }
    
    env->ReleaseIntArrayElements(keywords, keywordsPtr, JNI_ABORT);
    
    // Call the redirection logic in queen.cpp to get (u, e) tokens
    auto result = queen_process(input);
    
    LOGI("Token generation complete. Result size: %zu", result.size());
    
    // Return pairs as flattened array [u1, e1, u2, e2, ...]
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray tokenArray = env->NewObjectArray(result.size() * 2, stringClass, nullptr);
    
    for (size_t i = 0; i < result.size(); i++) {
        env->SetObjectArrayElement(tokenArray, i * 2, env->NewStringUTF(std::get<0>(result[i]).c_str()));
        env->SetObjectArrayElement(tokenArray, i * 2 + 1, env->NewStringUTF(std::get<1>(result[i]).c_str()));
    }
    
    return tokenArray;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_pi_SearchActivity_getSearchToken(
        JNIEnv* env,
        jobject /* this */,
        jstring keyword) {
    
    const char* keywordChars = env->GetStringUTFChars(keyword, nullptr);
    auto result = queen_search_client(std::string(keywordChars));
    env->ReleaseStringUTFChars(keyword, keywordChars);
    
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray tokenArray = env->NewObjectArray(3, stringClass, nullptr);
    
    env->SetObjectArrayElement(tokenArray, 0, env->NewStringUTF(std::get<0>(result).c_str()));
    env->SetObjectArrayElement(tokenArray, 1, env->NewStringUTF(std::get<1>(result).c_str()));
    env->SetObjectArrayElement(tokenArray, 2, env->NewStringUTF(std::to_string(std::get<2>(result)).c_str()));
    
    return tokenArray;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_example_pi_SearchActivity_performPostProcessing(
        JNIEnv* env,
        jobject /* this */,
        jint indexRange,
        jstring res1_0, jstring res1_1,
        jstring res2_0, jstring res2_1) {
    
    const char* r10 = env->GetStringUTFChars(res1_0, nullptr);
    const char* r11 = env->GetStringUTFChars(res1_1, nullptr);
    const char* r20 = env->GetStringUTFChars(res2_0, nullptr);
    const char* r21 = env->GetStringUTFChars(res2_1, nullptr);
    
    auto result = queen_post_process(indexRange, std::string(r10), std::string(r11), std::string(r20), std::string(r21));
    
    env->ReleaseStringUTFChars(res1_0, r10);
    env->ReleaseStringUTFChars(res1_1, r11);
    env->ReleaseStringUTFChars(res2_0, r20);
    env->ReleaseStringUTFChars(res2_1, r21);
    
    jintArray resultArray = env->NewIntArray(result.size());
    if (result.size() > 0) {
        env->SetIntArrayRegion(resultArray, 0, result.size(), (jint*)result.data());
    }
    
    return resultArray;
}