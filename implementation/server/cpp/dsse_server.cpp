#include <iostream>
#include <sstream>
#include <vector>
#include <string>
#include <stdexcept>
#include <tuple>
#include <algorithm>

#include "FAST.h"

using namespace std;

class ServerNode : public DSSE {
public:
    void Setup(string dbPath) {
        rocksdb::Options options;
        options.create_if_missing = true;
        
        // In this integration, we only care about map2 on the server.
        // map1 is client-side.
        rocksdb::Status status = rocksdb::DB::Open(options, dbPath, &Data.map2);
        if (!status.ok()) {
            cerr << "Error opening RocksDB for map2: " << status.ToString() << endl;
            exit(1);
        }
    }
};

int main(int argc, char* argv[]) {
    if (argc < 3) {
        cerr << "Usage: " << argv[0] << " <db_path> <opcode> [args...]" << endl;
        return 1;
    }

    string dbPath = argv[1];
    int opCode = stoi(argv[2]);

    ServerNode dsse;
    dsse.Setup(dbPath);

    if (opCode == 0) { // Update
        if (argc < 5) {
            cerr << "Usage: Update requires <key> <value>" << endl;
            return 1;
        }
        string key = argv[3];
        string value = argv[4];
        dsse.Update_server(make_tuple(key, value));
        cout << "Update successful" << endl;
    
    } else if (opCode == 1) { // Search
        if (argc < 6) {
            cerr << "Usage: Search requires <keyword_token> <state_token> <count>" << endl;
            return 1;
        }
        string t_keyword = argv[3];
        string st_c = argv[4];
        int c = stoi(argv[5]);

        vector<string> results;
        dsse.Search_server(make_tuple(t_keyword, st_c, c), results);
        
        for(const auto& res : results) {
            cout << "RESULT:" << res << endl;
        }
    } else {
        cerr << "Invalid OpCode" << endl;
        return 1;
    }

    return 0;
}
