#include <iostream>
#include <vector>
#include <string>
#include <tuple>
#include <algorithm>

using namespace std;

vector<tuple<int, int>> esGen(vector<tuple<string, int>> inp)
{
    vector<tuple<int, int>> res(100, make_tuple(0, -1));

    if (inp.empty()) {
        return res;
    }

    sort(inp.begin(), inp.end(),
    [](const tuple<string,int>& a,
        const tuple<string,int>& b) {
        return get<1>(a) < get<1>(b);
    });


    for(int i=0 ; i<inp.size() ; i++)
    {
        cout<<i<<" - "<<get<0>(inp[i])<<" : "<<get<1>(inp[i])<<endl;
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
    for(int i=1 ; i<100 ; i++){
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

int main() {
    vector<tuple<string, int>> inp;
    inp.push_back(make_tuple("ID1", 22));
    inp.push_back(make_tuple("ID2", 20));
    inp.push_back(make_tuple("ID3", 18));
    inp.push_back(make_tuple("ID4", 25));
    inp.push_back(make_tuple("ID5", 30));
    inp.push_back(make_tuple("ID6", 22));
    inp.push_back(make_tuple("ID7", 18));
    vector<tuple<int, int>> res = esGen(inp);
    for(int i=0 ; i<res.size() ; i++){
        // if(i == 18 || i == 20 || i == 22 || i == 25 || i == 30 )
            // cout<<i<<" - "<<get<0>(res[i])<<" : "<<get<1>(res[i])<<endl;
        cout<<i<<" - "<<get<0>(res[i])<<" : "<<get<1>(res[i])<<endl;
    }
    return 0;
}