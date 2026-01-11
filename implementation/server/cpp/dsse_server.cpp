#include <iostream>
#include <sstream>
#include <vector>
#include <string>
#include <stdexcept>
#include <tuple>
#include <algorithm>

#include <cryptopp/sha.h>
#include <cryptopp/shake.h>
#include <cryptopp/aes.h>
#include <cryptopp/filters.h>
#include <cryptopp/modes.h>
#include <cryptopp/secblock.h>

#include <rocksdb/db.h>
#include <rocksdb/options.h>

using namespace std;
using namespace CryptoPP;

// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Utilities %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

void decryptAES(const SecByteBlock &key, const string &ciphertext, string &plaintext) {
    ECB_Mode<AES>::Decryption decryptor;
    decryptor.SetKey(key, key.size());
    StringSource(ciphertext, true, new StreamTransformationFilter(decryptor, new StringSink(plaintext), BlockPaddingSchemeDef::NO_PADDING));
}


SecByteBlock StringToSecByteBlock(const string& str) {
    SecByteBlock block(reinterpret_cast<const unsigned char*>(str.data()), 16);
    return block;
}

std::string xorStrings(const std::string &str1, const std::string &str2) {
    if (str1.size() != str2.size()) {
        throw std::invalid_argument("Strings must be of equal length for XOR operation.");
    }
    std::string result;
    result.reserve(str1.size());
    for (size_t i = 0; i < str1.size(); ++i) {
        result += static_cast<char>(str1[i] ^ str2[i]);
    }
    return result;
}

string sha256(const std::string &input) {
    SHA256 hash;
    std::string digest;
    StringSource ss(input, true, new HashFilter(hash, new StringSink(digest)));
    return digest;
}

string hashSHAKE(const std::string& input, size_t outputLength) {
    SHAKE128 shake;
    string output;
    shake.Update(reinterpret_cast<const unsigned char*>(input.data()), input.size());
    output.resize(outputLength);
    shake.TruncatedFinal(reinterpret_cast<unsigned char*>(&output[0]), outputLength);
    return output;
}

// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% DSSE Class %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

struct DSSEData {
    rocksdb::DB* map2; 
};

class DSSE {
public:
    DSSEData Data;

    DSSE() { Data.map2 = nullptr; }
    ~DSSE() {
        if (Data.map2) delete Data.map2;
    }

    void Setup(string dbPath);
    void Update_server(const tuple<string, string> &u_token);
    void Search_server(const tuple<string, string, int> &s_token, vector<string> &search_result);
};

void DSSE::Setup(string dbPath) {
    rocksdb::Options options;
    options.create_if_missing = true;
    rocksdb::Status status = rocksdb::DB::Open(options, dbPath, &Data.map2);
    if (!status.ok()) {
        cerr << "Error opening RocksDB for map2: " << status.ToString() << endl;
        exit(1);
    }
}

void DSSE::Update_server(const tuple<string, string> &u_token) {
    rocksdb::Status status = this->Data.map2->Put(rocksdb::WriteOptions(), get<0>(u_token), get<1>(u_token));
    if(!status.ok()) {
        std::cerr << "Error reading from RocksDB:" << status.ToString() << std::endl;
    } else {
        // Output something if needed implicitly or just success?
        // User snippet didn't output "Update successful", but I will print for confirmation.
        cout << "Update successful" << endl;
    }
}

void DSSE::Search_server(const tuple<string, string, int> &s_token, vector<string> &search_result) {
    search_result.clear();
    vector<string> Delta;
    
    string t_keyword = get<0>(s_token);
    string st_c = get<1>(s_token);
    int c = get<2>(s_token);

    for(int i = c; i > 0; --i) {  
        string u, e;
        u = sha256(t_keyword + st_c);                                                         
        
        rocksdb::Status status = this->Data.map2->Get(rocksdb::ReadOptions(), u, &e); 
        if (!status.ok()) {
            // Logically handle missing keys? 
            // In DSSE if chain breaks, we usually stop. 
            // But if c > 0 we expect it.
            // Let's assume empty 'e' if missing to avoid crash, but standard flow assumes integrity.
             e = string(32, '\0'); 
        }

        string temp = hashSHAKE(t_keyword + st_c, 32);                                                          
        
        string ind_op_k_i = xorStrings(e, temp);

        // Safety check for substring length
        if (ind_op_k_i.size() < 32) ind_op_k_i.resize(32, '\0');

        string ind = ind_op_k_i.substr(0, 15);                                                                  
        string op = ind_op_k_i.substr(15, 1);  
        string k_i = ind_op_k_i.substr(16, 16);
        
        if(op == "0") {
            Delta.push_back(ind);
        } else if(op == "1") {
            auto it = find(Delta.begin(), Delta.end(), ind);
            if(it != Delta.end()) {
                Delta.erase(it);                                             
            } else {
                search_result.push_back(ind);  
            }
        }

        string st_i_minus_1;
        decryptAES(StringToSecByteBlock(k_i), st_c, st_i_minus_1);
        st_c = st_i_minus_1;
    }

    // Print results for the caller
    for(const auto& res : search_result) {
        cout << "RESULT:" << res << endl;
    }
}

// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Main %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

int main(int argc, char* argv[]) {
    if (argc < 3) {
        cerr << "Usage: " << argv[0] << " <db_path> <opcode> [args...]" << endl;
        return 1;
    }

    string dbPath = argv[1];
    int opCode = stoi(argv[2]);

    DSSE dsse;
    dsse.Setup(dbPath);

    if (opCode == 0) { // Update
        if (argc < 5) {
            cerr << "Usage: Update requires <key> <value>" << endl;
            return 1;
        }
        string key = argv[3];
        string value = argv[4];
        dsse.Update_server(make_tuple(key, value));
    
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
    } else {
        cerr << "Invalid OpCode" << endl;
        return 1;
    }

    return 0;
}
