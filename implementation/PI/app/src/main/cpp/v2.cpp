#include <iostream>
#include <vector>
#include <string>
#include <tuple>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "PI_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace std;

static const int RANGE = 100;

vector<tuple<int, int>> esGen(vector<tuple<string, int>> &inp)
{
    vector<tuple<int, int>> res(RANGE, make_tuple(0, -1));

    if (inp.empty()) {
        return res;
    }

    sort(inp.begin(), inp.end(),
    [](const tuple<string,int>& a,
        const tuple<string,int>& b) {
        return get<1>(a) < get<1>(b);
    });

    // Logging the sorted array for verification
    LOGI("Sorted array contents:");
    for(int i=0 ; i<inp.size() ; i++)
    {
        LOGI("%d - %s : %d", i, get<0>(inp[i]).c_str(), get<1>(inp[i]));
    }

    int match = get<1>(inp[0]);
    get<0>(res[match]) = 0;
    int i = 1;
    for(; i<inp.size() ; i++){
        if(match != get<1>(inp[i])){
            get<1>(res[match]) = i-1;
            match = get<1>(inp[i]);
            get<0>(res[match]) = i;
        }
    }
    get<1>(res[match]) = i-1;

    bool flag = true;
    for(int i=1 ; i<RANGE ; i++){
        if(flag){
            get<0>(res[i]) = get<1>(res[i-1])+1;
            flag = false;
        }
        else{
            if(get<0>(res[i]) == 0 && get<1>(res[i]) == -1){
                get<0>(res[i]) = get<0>(res[i-1]);
            }
            else{
                flag = true;
            }
        }
    }
    return res;
}

vector<tuple<int, int>> DBConversion(vector<tuple<string, int>> &inp){
    vector<tuple<int, int>> res;
    res.reserve(RANGE*2);

    auto res1 = esGen(inp);

    for(int i=0 ; i<RANGE ; i++){
        res.emplace_back(get<0>(res1[i]), i);
        res.emplace_back(get<1>(res1[i]), i);
    }

    return res;
}
