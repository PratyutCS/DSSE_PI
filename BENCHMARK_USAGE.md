# Benchmark Suite Usage Guide

This guide explains how to use the automated benchmarking system for the Private Information Retrieval (PI) project.

## Prerequisites

Ensure the following dependencies are installed:
*   **C++ Compiler**: `g++` (supporting C++11 or later)
*   **Libraries**: `libcryptopp-dev`, `librocksdb-dev`
*   **Python 3**: `python3`

## Running the Automated Benchmark

To run the full benchmark suite, execute the main shell script with the `--benchmark` flag:

```bash
./run_pi.sh --benchmark
```

### What this command does:
1.  **Compiles** `queen.cpp` with the `AUTOMATED_SEARCH` flag enabled.
2.  **Iterates** through the defined test ranges (e.g., 10, 100, ..., 10,000,000) specified in `benchmark.py`.
3.  **Executes** the binary for each range, measuring:
    *   Setup Time
    *   Random Input Generation Time
    *   DB Conversion Time
    *   Update Client Time
    *   Update Server Time
    *   Search 1 Time (Client & Server separate)
    *   Search 2 Time (Client & Server separate)
    *   Post Processing Time
4.  **Parses** the output and saves the raw data to `visual/data.json`.
5.  **Exports** the data to a CSV file: `performance_data.csv`.
6.  **Generates** a LaTeX table for the results: `latex_table/table.tex`.
7.  **Cleans up** the temporary benchmark binary (`queen_bench`).

## Output Files

After the script completes, you will find the following files:

*   **`performance_data.csv`**: A CSV spreadsheet containing all the timing metrics for each input range.
*   **`latex_table/table.tex`**: A LaTeX-formatted table ready to be included in a research paper or report.
*   **`visual/data.json`**: The raw JSON data used for intermediate storage.

## Manual Execution

To run the program manually (interactive mode) without the automated benchmarking suite:

```bash
./run_pi.sh
```

This will:
1.  Cleanup existing RocksDB directories (`Sigma_map1`, `Server_map2`).
2.  Compile the standard `queen` binary.
3.  Run `./queen` interactively.

## Customizing the Benchmark

To change the input ranges being tested, edit the `TEST_RANGES` list in `benchmark.py`:

```python
# benchmark.py
TEST_RANGES = [10, 100, 1000, 10000] 
```
