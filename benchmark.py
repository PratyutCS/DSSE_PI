import subprocess
import re
import json
import os

# Configuration: Define the ranges to test
# Configuration: Define the ranges to test
TEST_RANGES = [10000, 25000, 50000, 75000, 100000, 250000, 500000, 750000, 1000000]

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

    # 3. Run multiple iterations and average
    NUM_RUNS = 10
    metrics_sum = {
        "setup_time_s": 0.0,
        "random_input_time_s": 0.0,
        "db_conversion_time_s": 0.0,
        "update_client_time_s": 0.0,
        "update_server_time_s": 0.0,
        "search_1_client_s": 0.0,
        "search_1_server_s": 0.0,
        "search_2_client_s": 0.0,
        "search_2_server_s": 0.0,
        "post_processing_time_s": 0.0,
        "c1": 0.0,
        "c2": 0.0
    }
    
    # Store results from the last run (assuming they are consistent)
    last_results = {}

    for i in range(NUM_RUNS):
        # Clean up before each run
        subprocess.run(["rm", "-rf", "Sigma_map1", "Server_map2"], check=True)
        
        # Execute
        result = subprocess.run(["./queen_bench"], capture_output=True)
        output = result.stdout.decode('utf-8', errors='replace')
        
        # Parse output for this run
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
                metrics_sum[key] += float(match.group(1))

        # Search params
        s1_c_match = re.search(r"Search 1 Client took: ([\d.eE+-]+) seconds", output)
        s1_s_match = re.search(r"Search 1 Server took: ([\d.eE+-]+) seconds", output)
        s2_c_match = re.search(r"Search 2 Client took: ([\d.eE+-]+) seconds", output)
        s2_s_match = re.search(r"Search 2 Server took: ([\d.eE+-]+) seconds", output)
        
        if s1_c_match: metrics_sum["search_1_client_s"] += float(s1_c_match.group(1))
        if s1_s_match: metrics_sum["search_1_server_s"] += float(s1_s_match.group(1))
        if s2_c_match: metrics_sum["search_2_client_s"] += float(s2_c_match.group(1))
        if s2_s_match: metrics_sum["search_2_server_s"] += float(s2_s_match.group(1))
        
        # Parse debug C values (Indices: 0=warmup, 1=Search1, 2=Search2)
        c_matches = re.findall(r"DEBUG_C: (\d+)", output)
        if len(c_matches) >= 3:
            metrics_sum["c1"] += float(c_matches[1])
            metrics_sum["c2"] += float(c_matches[2])
        elif len(c_matches) == 2: # fallback for safety
            metrics_sum["c1"] += float(c_matches[0])
            metrics_sum["c2"] += float(c_matches[1])

        # Parse results (only need to keep one valid set)
        res_patterns = re.findall(r"(\d+) - (\d+) : (-?\d+)", output)
        if len(res_patterns) >= 4:
            last_results["client0"] = {"param1": int(res_patterns[0][2]), "param2": int(res_patterns[2][2])}
            last_results["client1"] = {"param1": int(res_patterns[1][2]), "param2": int(res_patterns[3][2])}

    # 4. Average and Construct Data Object
    data = {
        "input_index_range": range_val,
        "search": {}, # Structure kept for compatibility
        "results": last_results
    }
    
    # Calculate averages
    for key, total_val in metrics_sum.items():
        data[key] = total_val / NUM_RUNS

    # populate nested search object for compatibility if needed
    data["search"]["param1"] = {
        "value": 0, 
        "client_s": data["search_1_client_s"],
        "server_s": data["search_1_server_s"],
        "time_s": (data["search_1_client_s"] + data["search_1_server_s"]) # total time for back-compat view
    }
    data["search"]["param2"] = {
        "value": range_val - 1, 
        "client_s": data["search_2_client_s"],
        "server_s": data["search_2_server_s"],
        "time_s": (data["search_2_client_s"] + data["search_2_server_s"])
    }

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
