import json
import csv
import os

DATA_FILE = "visual/data.json"
OUTPUT_FILE = "performance_data.csv"

def export_json_to_csv():
    if not os.path.exists(DATA_FILE):
        print(f"Error: {DATA_FILE} not found.")
        return

    with open(DATA_FILE, 'r') as f:
        data = json.load(f)

    headers = [
        "Input Range",
        "Setup (s)",
        "DB Conversion (s)",
        "Post Processing (s)",
        "Search Time (s)"
    ]

    with open(OUTPUT_FILE, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(headers)

        for entry in data:
            # Calculate average search time
            search = entry.get("search", {})
            p1 = search.get("param1", {}).get("time_s", 0)
            p2 = search.get("param2", {}).get("time_s", 0)
            search_time_avg = (p1 + p2) / 2 if (p1 > 0 and p2 > 0) else 0
            
            # If avg_search_time_s was pre-calculated in benchmark.py, use it as fallback/check
            if search_time_avg == 0 and "avg_search_time_s" in entry:
                search_time_avg = entry["avg_search_time_s"]

            row = [
                entry.get("input_index_range", 0),
                entry.get("setup_time_s", 0),
                entry.get("db_conversion_time_s", 0),
                entry.get("post_processing_time_s", 0),
                search_time_avg
            ]
            writer.writerow(row)

    print(f"Data successfully exported to {OUTPUT_FILE}")

if __name__ == "__main__":
    export_json_to_csv()
