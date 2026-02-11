# Check for benchmark flag
if [ "$1" == "--benchmark" ]; then
    echo "Starting automated benchmark suite..."
    python3 benchmark.py
    rm -f queen_bench
    exit 0
fi

# 1. Clean up existing RocksDB directories
echo "Cleaning up database directories..."
rm -rf Sigma_map1 Server_map2

# 2. Compile the project
echo "Compiling queen.cpp..."
g++ queen.cpp ./FAST/Search.cpp ./FAST/Update.cpp ./FAST/Setup.cpp ./FAST/Utilities.cpp -lcryptopp -lrocksdb -o queen

# 3. Check for compilation success
if [ $? -eq 0 ]; then
    echo "Compilation successful. Executing ./queen..."
    # 4. Execute the binary
    ./queen
else
    echo "Compilation failed."
    exit 1
fi
