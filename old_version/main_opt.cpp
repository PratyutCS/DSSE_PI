#include <iostream>
#include <vector>
#include <string>
#include <tuple>
#include <algorithm>

using namespace std;

vector<tuple<int, int>> esGen(vector<tuple<string, int>> inp){
    vector<tuple<int, int>> res(100, make_tuple(0, -1));
    vector<tuple<int, int>> db;
    db.reserve(200);

    if (inp.empty()) {
        return res;
    }

    sort(inp.begin(), inp.end(),
    [](const tuple<string,int>& a,
        const tuple<string,int>& b) {
        return get<1>(a) < get<1>(b);
    });


    for(int i=0 ; i<inp.size() ; i++){
        cout<<i<<" - "<<get<0>(inp[i])<<" : "<<get<1>(inp[i])<<endl;
    }
    
    int ctrl = 0;
    db.push_back(make_tuple(0, 0));
    bool flag = false;
    bool flag1 = false;
    int ci = 1;

    for(int i=0 ; (i< 100) ;){
        if(ctrl >= inp.size()){
            if(flag1){
                db.push_back(make_tuple(ctrl-1, i));
                ci++;
                flag1 = false;
                flag = true;
                i++;
            }
            else{
                db.push_back(make_tuple(get<0>(db[ci-2]), i));
                ci++;
                db.push_back(make_tuple(-1, i));
                ci++;
                i++;
            }
        }
        else{
            if(i == get<1>(inp[ctrl])){
                if(flag){
                    db.push_back(make_tuple(ctrl, i));
                    ci++;
                    ctrl++;
                    flag = false;
                }
                else{
                    ctrl++;
                }
                flag1 = true;
            }
            else{
                if(flag){
                    db.push_back(make_tuple(get<0>(db[ci-2]), i));
                    ci++;
                    flag = false;
                }
                else{
                    if(flag1){
                        db.push_back(make_tuple(ctrl-1, i));
                        flag1 = false;
                    }
                    else{
                        db.push_back(make_tuple(-1, i));
                    }
                    ci++;
                    i++;
                    flag = true;
                }
            }
        }
    }
    return db;
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
    inp.push_back(make_tuple("ID8", 37));
    inp.push_back(make_tuple("ID9", 18));
    inp.push_back(make_tuple("ID10", 97));
    vector<tuple<int, int>> res = esGen(inp);
    for(int i=0 ; i<res.size() ; i++){
        cout<<i<<" - "<<get<0>(res[i])<<" : "<<get<1>(res[i])<<endl;
    }
    return 0;
}