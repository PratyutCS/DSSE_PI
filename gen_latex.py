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
    \begin{tabular}{| S[table-format=8.0] | S[table-format=1.4] | S[table-format=1.4] | S[table-format=2.4] | S[table-format=1.2e-1] | S[table-format=1.2e-1] | S[table-format=1.2e-1] | S[table-format=1.2e-1] | S[table-format=1.2e-1] |}
        \hline
        \rowcolor{lightgray!50}
        {\textbf{Range}} & {\textbf{Setup (ms)}} & {\textbf{RndInp (ms)}} & {\textbf{DBConv (ms)}} & {\textbf{S1-Cli (ms)}} & {\textbf{S1-Svr (ms)}} & {\textbf{S2-Cli (ms)}} & {\textbf{S2-Svr (ms)}} & {\textbf{Post (ms)}} \\
        \hline
"""
    for row in rows:
        range_val = row[0]
        # Format values to ms with consistent precision
        def clean_val(x):
            try:
                v = float(x)
                return "{:.3f}".format(v * 1000)
            except: return x

        setup = clean_val(row[1])
        rnd_inp = clean_val(row[2])
        db = clean_val(row[3])
        s1c = clean_val(row[6])
        s1s = clean_val(row[7])
        s2c = clean_val(row[8])
        s2s = clean_val(row[9])
        post = clean_val(row[12])
        
        latex_content += fr"        {range_val} & {setup} & {rnd_inp} & {db} & {s1c} & {s1s} & {s2c} & {s2s} & {post} \\ \hline"
        latex_content += "\n"

    latex_content += r"""    \end{tabular}
\end{table}

\end{document}
"""

    with open(tex_file, 'w') as f:
        f.write(latex_content)

if __name__ == "__main__":
    generate_latex()
