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
    db.reserve(2 * RANGE);

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
    db.emplace_back(0, 0);

    bool flag = false;
    bool flag1 = false;
    int ci = 1;

    for (int i = 0; i < RANGE;) {
        if (ctrl >= inpSize) {
            if (flag1) {
                db.emplace_back(ctrl - 1, i);
                ++ci;
                flag1 = false;
                flag = true;
                ++i;
            } else {
                db.emplace_back(get<0>(db[ci - 2]), i);
                ++ci;
                db.emplace_back(-1, i);
                ++ci;
                ++i;
            }
        } else {
            const int inpVal = get<1>(inp[ctrl]);

            if (i == inpVal) {
                if (flag) {
                    db.emplace_back(ctrl, i);
                    ++ci;
                    ++ctrl;
                    flag = false;
                } else {
                    ++ctrl;
                }
                flag1 = true;
            } else {
                if (flag) {
                    db.emplace_back(get<0>(db[ci - 2]), i);
                    ++ci;
                    flag = false;
                } else {
                    if (flag1) {
                        db.emplace_back(ctrl - 1, i);
                        flag1 = false;
                    } else {
                        db.emplace_back(-1, i);
                    }
                    ++ci;
                    ++i;
                    flag = true;
                }
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
