#include <iostream>
#include <vector>
#include <string>
#include <tuple>
#include <algorithm>

using namespace std;

vector<tuple<int, int>> esGen(vector<tuple<string, int>>& inp)
{
    constexpr int RANGE = 100;

    vector<tuple<int, int>> res(RANGE, {0, -1});
    vector<tuple<int, int>> db;
    db.reserve(2 * RANGE);

    if (inp.empty())
        return res;

    // Sort by second element
    sort(inp.begin(), inp.end(),
         [](const auto& a, const auto& b) {
             return get<1>(a) < get<1>(b);
         });

    // Debug print (unchanged behavior)
    for (size_t i = 0; i < inp.size(); ++i) {
        cout << i << " - " << get<0>(inp[i]) << " : " << get<1>(inp[i]) << endl;
    }

    // Build ranges
    int match = get<1>(inp[0]);
    get<0>(res[match]) = 0;

    size_t i = 1;
    for (; i < inp.size(); ++i) {
        int curr = get<1>(inp[i]);
        if (curr != match) {
            get<1>(res[match]) = static_cast<int>(i - 1);
            match = curr;
            get<0>(res[match]) = static_cast<int>(i);
        }
    }
    get<1>(res[match]) = static_cast<int>(i - 1);

    // Emit DB entries
    db.emplace_back(get<0>(res[0]), 0);
    db.emplace_back(get<1>(res[0]), 0);

    for (int idx = 1; idx < RANGE; ++idx) {
        if (get<0>(res[idx]) == 0 && get<1>(res[idx]) == -1) {
            get<0>(res[idx]) = get<0>(res[idx - 1]);
        }
        db.emplace_back(get<0>(res[idx]), idx);
        db.emplace_back(get<1>(res[idx]), idx);
    }

    return db;
}

int main()
{
    vector<tuple<string, int>> inp = {
        {"ID1", 22},
        {"ID2", 20},
        {"ID3", 18},
        {"ID4", 25},
        {"ID5", 30},
        {"ID6", 22},
        {"ID7", 18}
    };

    auto res = esGen(inp);

    for (size_t i = 0; i < res.size(); ++i) {
        cout << i << " - "
             << get<0>(res[i]) << " : "
             << get<1>(res[i]) << endl;
    }

    return 0;
}
