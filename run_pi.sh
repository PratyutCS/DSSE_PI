# Check for benchmark flag
if [ "$1" == "--benchmark" ]; then
    echo "Starting automated benchmark suite..."
    python3 benchmark.py
    
    echo "Exporting results to CSV..."
    python3 export_to_csv.py
    
    echo "Generating LaTeX table..."
    mkdir -p latex_table
    python3 gen_latex.py
    
    if command -v pdflatex &> /dev/null; then
        echo "Compiling LaTeX to PDF..."
        cd latex_table
        pdflatex -interaction=nonstopmode table.tex > /dev/null
        cd ..
        echo "PDF generated at latex_table/table.pdf"
    else
        echo "pdflatex not found. Skipping PDF generation."
    fi
    
    rm -f queen_bench
    echo "Done! Check performance_data.csv and latex_table/table.pdf"
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
