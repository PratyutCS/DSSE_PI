#include <iostream>
#include <vector>
#include <string>
#include <tuple>
#include <android/log.h>
#include "v2.cpp"

#define LOG_TAG "PI_QUEEN"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace std;

vector<tuple<int, int>> queen_process(vector<tuple<string, int>> &inp) {
    LOGI("Queen redirecting to DBConversion. Input size: %zu", inp.size());
    
    auto result = DBConversion(inp);
    
    LOGI("DBConversion complete. Result size: %zu", result.size());
    
    // Log the results returned by DBConversion
    LOGI("DBConversion output logs:");
    for (size_t i = 0; i < result.size(); ++i) {
        LOGI("Index %zu: [%d, %d]", i, get<0>(result[i]), get<1>(result[i]));
    }

    vector<tuple<string, string>> u_List;

    for(size_t i=0 ; i<result.size() ; i++){
        tuple<string, string> u_token;
        string ind = to_string(get<0>(result[i]));
        string keyword = to_string(get<1>(result[i]));
        bool op = true;
        // FAST_.Update_client(ind, keyword, op, u_token);
        // u_List.push_back(u_token);
    }
    
    return result;
}
