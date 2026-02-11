import subprocess
import re
import json
import os

# Configuration: Define the ranges to test
# TEST_RANGES = [10000, 50000, 100000, 500000, 1000000, 5000000, 10000000]
TEST_RANGES = [10, 25, 50, 75, 100, 250, 500, 750, 1000, 2500, 5000, 7500, 10000, 25000, 50000, 75000, 100000, 250000, 500000, 750000, 1000000, 2500000, 5000000, 7500000, 10000000 ]
DATA_FILE = "visual/data.json"

def run_benchmark(range_val):
    print(f"\n>>> Running benchmark for RANGE={range_val}...")
    
    # Compilation command with definitions
    compile_cmd = [
        "g++", "queen.cpp", "./FAST/Search.cpp", "./FAST/Update.cpp", 
        "./FAST/Setup.cpp", "./FAST/Utilities.cpp", 
        "-lcryptopp", "-lrocksdb", 
        f"-DRANGE={range_val}", f"-Dindex_range={range_val}", "-DAUTOMATED_SEARCH",
        "-o", "queen_bench"
    ]
    
    # 1. Clean up
    subprocess.run(["rm", "-rf", "Sigma_map1", "Server_map2"], check=True)
    
    # 2. Compile
    result = subprocess.run(compile_cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Compilation failed for range {range_val}")
        print(result.stderr)
        return None

    # 3. Run
    result = subprocess.run(["./queen_bench"], capture_output=True, text=True)
    output = result.stdout
    
    # 4. Parse output
    data = {
        "input_index_range": range_val,
        "search": {},
        "results": {}
    }
    
    # Regex patterns (supporting scientific notation like 2.3386e-05)
    patterns = {
        "setup_time_s": r"Setup took: ([\d.eE+-]+) seconds",
        "random_input_time_s": r"Generate Random Input took: ([\d.eE+-]+) seconds",
        "db_conversion_time_s": r"DB Conversion took: ([\d.eE+-]+) seconds",
        "update_client_time_s": r"Update Client takes: ([\d.eE+-]+) seconds",
        "update_server_time_s": r"Update Server takes: ([\d.eE+-]+) seconds",
        "post_processing_time_s": r"Post Processing took: ([\d.eE+-]+) seconds",
    }

    for key, pattern in patterns.items():
        match = re.search(pattern, output)
        if match:
            data[key] = float(match.group(1))

    # Search params (with scientific notation support)
    s1_match = re.search(r"Search 1 took: ([\d.eE+-]+) seconds", output)
    s2_match = re.search(r"Search 2 took: ([\d.eE+-]+) seconds", output)
    if s1_match and s2_match:
        data["avg_search_time_s"] = (float(s1_match.group(1)) + float(s2_match.group(1))) / 2
    if s1_match: data["search"]["param1"] = {"value": 0, "time_s": float(s1_match.group(1))}
    if s2_match: data["search"]["param2"] = {"value": range_val - 1, "time_s": float(s2_match.group(1))}

    # Parse search results snippets
    # Example: 0 - 0 : -1
    res_patterns = re.findall(r"(\d+) - (\d+) : (-?\d+)", output)
    # result1 (param1=0): first two matches
    # result2 (param2=range-1): next two matches
    if len(res_patterns) >= 4:
        data["results"]["client0"] = {"param1": int(res_patterns[0][2]), "param2": int(res_patterns[2][2])}
        data["results"]["client1"] = {"param1": int(res_patterns[1][2]), "param2": int(res_patterns[3][2])}

    return data

def main():
    all_results = []
    
    for range_val in TEST_RANGES:
        res = run_benchmark(range_val)
        if res:
            res["run"] = len(all_results) + 1
            all_results.append(res)
            
    # Save to file
    os.makedirs(os.path.dirname(DATA_FILE), exist_ok=True)
    with open(DATA_FILE, 'w') as f:
        json.dump(all_results, f, indent=4)
    
    print(f"\nDone! Results saved to {DATA_FILE}")

if __name__ == "__main__":
    main()
