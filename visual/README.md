# PI Performance Dashboard

A modern, responsive dashboard to visualize performance metrics from the PI system.

## How to Run

Because this application fetches a local JSON file, you need to run it through a local web server to avoid CORS issues.

### using Python (Recommended)

Run the following command in this directory:

```bash
python3 -m http.server 8000
```

Then open your browser and go to:
[http://localhost:8000](http://localhost:8000)

### using Node.js

If you have `http-server` installed:

```bash
npx http-server .
```

## Features

- **Dark Mode Design**: Easy on the eyes with a modern aesthetic.
- **Glassmorphism**: Translucent panels for a futuristic look.
- **Responsive Table**: Displays complex nested data cleanly.
- **Dynamic Loading**: Fetches data directly from `data.json`.
