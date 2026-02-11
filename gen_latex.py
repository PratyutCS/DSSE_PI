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
    \begin{tabular}{| S[table-format=8.0] | S[table-format=1.4] | S[table-format=2.4] | S[table-format=1.2e-1] | S[table-format=1.2e-1] |}
        \hline
        \rowcolor{lightgray!50}
        {\textbf{Input Range}} & {\textbf{Setup (sec)}} & {\textbf{DB Conv. (sec)}} & {\textbf{Search (sec)}} & {\textbf{Post-Proc. (sec)}} \\
        \hline
"""
    for row in rows:
        range_val = row[0]
        # Round or format values to prevent overflow
        def clean_val(x):
            try:
                v = float(x)
                if v < 0.001 and v > 0:
                    return "{:.2e}".format(v)
                return "{:.4f}".format(v)
            except: return x

        setup = clean_val(row[1])
        db = clean_val(row[2])
        post = clean_val(row[3])
        search = clean_val(row[4])
        
        # Swapping search and post in the table output
        latex_content += f"        {range_val} & {setup} & {db} & {search} & {post} \\\\ \n        \hline\n"

    latex_content += r"""    \end{tabular}
\end{table}

\end{document}
"""

    with open(tex_file, 'w') as f:
        f.write(latex_content)

if __name__ == "__main__":
    generate_latex()
