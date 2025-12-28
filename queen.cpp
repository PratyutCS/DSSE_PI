#include<iostream>
#include "./FAST/FAST.h"
#include "./v2.cpp"
#include "BitSequence.cpp"

using namespace std;

int main(){
    cout<<"works"<<endl;

    DSSE FAST_;

    cout << "============================= PSI: Setup =============================" << endl; 
    FAST_.Setup();

    vector<tuple<string, int>> inp = {
        {"ID1", 22},
        {"ID2", 20},
        {"ID3", 18},
        {"ID4", 25},
        {"ID5", 30},
        {"ID6", 22},
        {"ID7", 18}
    };
    auto res = DBConversion(inp);

    //debug print
    for(int i=0 ; i<res.size() ; i++){
        cout<<i<<" - "<<get<0>(res[i])<<" : "<<get<1>(res[i])<<endl;
    }

    vector<tuple<string, string>> u_List;

    for(int i=0 ; i<res.size() ; i++){
        tuple<string, string> u_token;
        string ind = to_string(get<0>(res[i]));
        string keyword = to_string(get<1>(res[i]));
        bool op = true;
        FAST_.Update_client(ind, keyword, op, u_token);
        u_List.push_back(u_token);
    }

    for(auto utk : u_List)
    {
        FAST_.Update_server(utk);
    }

    int choice = 1;
    while (choice != 0) {
        vector<string> search_result1;
        cout<<"enter search param1"<<endl;
        string param1;
        cin>>param1;
        tuple<string, string, int> s_token;
        FAST_.Search_client(param1, s_token);
        FAST_.Search_server(s_token, search_result1);
        
        vector<string> search_result2;
        cout<<"enter search param2"<<endl;
        string param2;
        cin>>param2;
        tuple<string, string, int> s_token2;
        FAST_.Search_client(param2, s_token2);
        FAST_.Search_server(s_token2, search_result2);

        cout<<"============================= PSI: Search ============================="<<endl;

        //debug print
        for(int i=0 ; i<search_result1.size() ; i++){
            cout<<i<<" - "<<param1<<" : "<<search_result1[i]<<endl;
        }
        
        for(int i=0 ; i<search_result2.size() ; i++){
            cout<<i<<" - "<<param2<<" : "<<search_result2[i]<<endl;
        }

        BitSequence<uint64_t> param1_L_bitmap = less_than(inp.size(), stoull(search_result1[1]));
        BitSequence<uint64_t> param2_L_bitmap = less_than(inp.size(), stoull(search_result2[1]));

        // Final outputs
        cout << "lessthan 1: "; param1_L_bitmap.print_range(0, inp.size());
        cout << "lessthan 2: "; param2_L_bitmap.print_range(0, inp.size());

        // 1's Complement Demonstration
        BitSequence<uint64_t> param1_GE_bitmap = param1_L_bitmap.ones_complement();
        cout << "param1 complement: "; param1_GE_bitmap.print_range(0, inp.size());

        // Initialize with all zeros (n, 0) to ensure it exists even if 'equal' is not applicable
        BitSequence<uint64_t> param2_E_bitmap(inp.size(), 0); 
        
        if(stoull(search_result2[0]) != -1)
        {
            cout<<"equal to applicable for param2"<<endl;
            param2_E_bitmap = equal_bits(stoull(search_result2[1]), stoull(search_result2[0]), inp.size());
            cout << "param2 equal bitmap is : "; param2_E_bitmap.print_range(0, inp.size());
        }
        
        // OR Operation: Combine LessThan and Equal to get LessThanEqual
        BitSequence<uint64_t> param2_LE_bitmap = param2_L_bitmap.bitwise_or(param2_E_bitmap);
        cout << "param2 LE (L | E) bitmap is : "; param2_LE_bitmap.print_range(0, inp.size());

        // AND Operation: Intersect param1 GE and param2 LE
        BitSequence<uint64_t> result_bitmap = param1_GE_bitmap.bitwise_and(param2_LE_bitmap);
        cout << "result bitmap (GE & LE) is : "; result_bitmap.print_range(0, inp.size());

        cout << "\nEnter 0 to exit or 1 to continue search: ";
        cin >> choice;
    }

    return 0;
}