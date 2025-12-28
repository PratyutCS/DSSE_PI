#include <iostream>
#include <vector>
#include <string>
#include <tuple>
#include <algorithm>

using namespace std;

// ✅ Global static range
static const int RANGE = 100;

vector<tuple<int, int>> esGen(vector<tuple<string, int>> inp) {
    vector<tuple<int, int>> db;
    db.reserve(RANGE);

    sort(inp.begin(), inp.end(),
        [](const tuple<string, int>& a,
           const tuple<string, int>& b) {
            return get<1>(a) < get<1>(b);
        });

    const int inpSize = static_cast<int>(inp.size());

    for (int i = 0; i < inpSize; ++i) {
        cout << i << " - " << get<0>(inp[i]) << " : " << get<1>(inp[i]) << endl;
    }

    int ctrl = 0;
    bool flag = true;

    for (int i = 0; i < RANGE;) {
        if (ctrl >= inpSize) {
            if (!flag) {
                get<1>(db[i]) = ctrl -1;
                flag = true;
            } else {
                db.emplace_back(get<0>(db[i-1]), -1);
            }
            ++i;
        } else {
            const int inpVal = get<1>(inp[ctrl]);

            if (i == inpVal) {
                if (flag) {
                    db.emplace_back(ctrl, i);
                    flag = false;
                }
                ++ctrl;
            } 
            else {
                if (flag) {
                    if(i != 0){
                        db.emplace_back(get<0>(db[i - 1]), -1);
                    }
                    else{
                        db.emplace_back(0, -1);
                    }
                } 
                else {
                    get<1>(db[i]) = ctrl -1 ;
                    flag = true;
                }
                ++i;
            }
        }
    }

    return db;
}

int main() {
    vector<tuple<string, int>> inp = {
        {"ID1", 22}, {"ID2", 20}, {"ID3", 18}, {"ID4", 25},
        {"ID5", 30}, {"ID6", 22}, {"ID7", 18}, {"ID8", 37},
        {"ID9", 18}, {"ID10", 97}
    };

    vector<tuple<int, int>> res = esGen(inp);

    for (int i = 0; i < res.size(); ++i) {
        cout << i << " - " << get<0>(res[i]) << " : " << get<1>(res[i]) << endl;
    }

    return 0;
}
