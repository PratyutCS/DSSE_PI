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
        "DB size",
        "DBConv",
        "Setup",
        "Avg Client search time (s)",
        "Avg Server search time (s)",
        "Post processing"
    ]

    with open(OUTPUT_FILE, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(headers)

        for entry in data:
            avg_client = (entry.get("search_1_client_s", 0) + entry.get("search_2_client_s", 0)) / 2
            avg_server = (entry.get("search_1_server_s", 0) + entry.get("search_2_server_s", 0)) / 2
            
            row = [
                entry.get("input_index_range", 0),
                entry.get("db_conversion_time_s", 0),
                entry.get("setup_time_s", 0),
                avg_client,
                avg_server,
                entry.get("post_processing_time_s", 0)
            ]
            writer.writerow(row)

    print(f"Data successfully exported to {OUTPUT_FILE}")

if __name__ == "__main__":
    export_json_to_csv()
