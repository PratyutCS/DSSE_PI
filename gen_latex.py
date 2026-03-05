import csv

def generate_latex():
    csv_file = 'performance_data.csv'
    tex_file = 'latex_table/table.tex'
    
    rows = []
    with open(csv_file, 'r') as f:
        reader = csv.reader(f)
        headers = next(reader)
        for row in reader:
            rows.append(row)

    latex_content = r"""\documentclass{article}
\usepackage[utf8]{inputenc}
\usepackage{geometry}
\usepackage{siunitx}
\usepackage{array}
\usepackage[table]{xcolor}

\geometry{a4paper, margin=0.5in}

% Define custom colors
\definecolor{lightgrey}{gray}{0.95}

\begin{document}

\begin{table}[h!]
    \centering
    \scriptsize
    \rowcolors{2}{white}{lightgrey}
    \begin{tabular}{| S[table-format=8.0] | S[table-format=1.4] | S[table-format=1.4] | S[table-format=1.4] | S[table-format=1.4] | S[table-format=1.4] |}
        \hline
        \rowcolor{lightgray!50}
        {\textbf{DB size}} & {\textbf{DBConv (ms)}} & {\textbf{Setup (ms)}} & {\textbf{Avg Cli (ms)}} & {\textbf{Avg Svr (ms)}} & {\textbf{Post (ms)}} \\
        \hline
"""
    for row in rows:
        db_size = row[0]
        # Format values to ms with consistent precision
        def clean_val(x):
            try:
                v = float(x)
                return "{:.3f}".format(v * 1000)
            except: return x

        db_conv = clean_val(row[1])
        setup = clean_val(row[2])
        avg_cli = clean_val(row[3])
        avg_svr = clean_val(row[4])
        post = clean_val(row[5])
        
        latex_content += fr"        {db_size} & {db_conv} & {setup} & {avg_cli} & {avg_svr} & {post} \\ \hline"
        latex_content += "\n"

    latex_content += r"""    \end{tabular}
\end{table}

\end{document}
"""

    with open(tex_file, 'w') as f:
        f.write(latex_content)

if __name__ == "__main__":
    generate_latex()
