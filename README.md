# PI using DSSE

This project implements a PI protocol built on top of **Dynamic Searchable Symmetric Encryption (DSSE)**. It allows for privacy-preserving range queries and set operations using bit sequences.

## 1. DSSE (Dynamic Searchable Symmetric Encryption)
The core encryption layer uses the **FAST** scheme. DSSE allows a client to encrypt their data such that it remains searchable by a server without revealing the actual content or keywords to the server (unless a search token is provided).

- **Setup**: Initializes the encryption keys and the server-side database (RocksDB).
- **Update**: Encrypts and sends data to the server. This project pads data to a fixed length (15 bytes) to ensure consistency.
- **Search**: Generates tokens for specific keywords, allowing the server to return encrypted results which are then decrypted by the client.

## 2. Code Flow
The main logic is contained in `queen.cpp`, which follows this lifecycle:

### A. Initialization & Setup
- The DSSE system is initialized.
- A local database (`inp`) is converted and uploaded to the DSSE server using `Update_client` and `Update_server`.

### B. Interactive Search Loop
The program enters a loop asking the user for two search parameters (`param1` and `param2`). For each search:

1.  **DSSE Search**: Search tokens are generated and sent to the server to retrieve raw search results.
2.  **Bit Sequence Generation**:
    - **`less_than(n, k)`**: Returns a `BitSequence` where the first `k` bits are set to 1. This represents the "Less Than" property of the search result.
    - **`equal_bits(a, b, n)`**: Returns a `BitSequence` with bits in the range `[a, b]` set to 1.
3.  **Bitwise Operations**:
    - **Complement**: Using `ones_complement()`, we turn a "Less Than" sequence into a "Greater Than or Equal" (GE) sequence.
    - **OR**: Using `bitwise_or()`, we combine "Less Than" and "Equal" to form a "Less Than or Equal" (LE) sequence.
    - **AND**: Using `bitwise_and()`, we intersect the GE and LE sequences to find the final range intersection (`result_bitmap`).

### C. Final Output
The `result_bitmap` represents the indices where both conditions are met, effectively performing a private range intersection.

## 3. Storage & Utilities
- **`BitSequence` Class**: A high-performance class in `BitSequence.cpp` that stores large numbers of bits in `uint64_t` blocks. It supports efficient bitwise NOT, OR, and AND.
- **`run_pi.sh`**: A shell script to clean up databases, compile with `g++`, and run the `queen` binary.

## 4. How to Run (CLI / Standalone)
```bash
bash run_pi.sh
```
Follow the prompts to enter search parameters. Enter `0` when asked if you want to continue to exit the loop.
## 5. Server Implementation (Node.js + RocksDB)

This document provides a comprehensive guide to the **PI Server**, a secure Node.js application integrated with a high-performance C++ RocksDB backend for encrypted search (DSSE).

### 5.1 Architecture Overview

The system uses a 3-tier architecture:
1.  **API Layer (Node.js/Express)**: Handles RESTful requests, JWT authentication, and file streaming.
2.  **Data Layer (MongoDB)**: Stores user credentials (hashed) and space metadata.
3.  **Storage Engine (RocksDB + C++)**:
    -   **Files**: Stored physically on disk under `storage/<user_id>/<db_name>`.
    -   **Indexes**: Managed by a C++ binary (`dsse_server`) using RocksDB. It handles cryptographic operations (AES-128, SHA-256).

### 5.2 Prerequisites

Before running the server, ensure the following are installed:
*   **Node.js** (v18+) & **npm**
*   **MongoDB** (running on default port `27017` or configured via `.env`)
*   **System Libraries** (for C++ compilation):
    *   `librocksdb-dev` (RocksDB development headers/libs)
    *   `libcryptopp-dev` (Crypto++ development headers/libs)
    *   `build-essential` (g++, make, etc.)

### 5.3 Compilation (C++ Binaries)

The Node.js server relies on the compiled C++ binary to talk to RocksDB. You **must** compile this before starting the server.

**Command:**
```bash
npm run compile
```

**What this does:**
It invokes `g++` to compile `cpp/dsse_server.cpp`, linking against `librocksdb` and `libcryptopp`, generating the executable at `cpp/dsse_server`.

### 5.4 CLI Management Tool (`manage_server.sh`)

We provide a comprehensive bash utility to manage the server lifecycle.

**Usage:**
```bash
./manage_server.sh
```

**Features:**
-   **Install Dependencies**: `npm install`.
-   **Recompile C++**: Rebuilds the DSSE binary.
-   **Clean Data**: Wipes all user data and logs (dev utility).
-   **Run Tests**: Executes the verification suite.
-   **Start Server**: Runs the server in the background with process control (Restart/Stop/View Logs).

### 5.5 Manual Start & Deployment

#### Development Mode
To run the server locally without the CLI tool:
```bash
npm start
```
*   Server listens on `PORT` defined in `.env` (default: 3000).
*   Connects to MongoDB at `MONGO_URI`.

#### Production Deployment
For production environments, ensure you:
1.  Set `NODE_ENV=production` in your `.env` file.
2.  Use a process manager like **PM2** to ensure uptime and auto-restart.
    ```bash
    npm install -g pm2
    pm2 start src/index.js --name "pi-server"
    ```
3.  Ensure MongoDB is secured (auth enabled) and update `MONGO_URI` accordingly.
4.  Configure `STORAGE_ROOT` in `.env` to point to a persistent, backed-up volume.

### 5.6 API Endpoints

All endpoints (except auth) require a valid JWT token in the `Authorization` header: `Bearer <token>`.

#### Authentication
-   `POST /api/register` - Register a new user.
-   `POST /api/login` - Login and receive JWT.
-   `POST /api/logout` - Invalidate current session.

#### Spaces (Databases)
-   `POST /api/new` - Create a new isolated database space.
    -   Body: `{ "dbName": "my_db" }`
-   `DELETE /api/delete` - Delete a space and all its contents.
    -   Body: `{ "dbName": "my_db" }`

#### Index Operations (RocksDB)
-   `POST /api/save-index_value` - Save/Update a key-value pair in the encrypted index.
    -   Body: `{ "dbName": "...", "key": "...", "value": "..." }`
-   `GET /api/get-index_value` - Search/Retrieve values. (Requires client-side token generation).

#### File Operations
-   `POST /api/upload_files` - Upload files to a space.
    -   Query: `?dbName=...`
    -   Body: `multipart/form-data` (field: `files`)
-   `GET /api/get-files` - List filenames in a space.
    -   Query: `?dbName=...`
    -   Returns: `{ "files": ["file1.txt", ...] }`
-   `GET /api/download-file` - **Secure Download**.
    -   Query: `?dbName=...&fileName=...`
    -   Streams the file content to the authenticated user.

### 5.7 Testing & Verification

Run the end-to-end test suite to verify system integrity.

**Command:**
```bash
node test/verify_suite.js
```

**Scope:**
1.  **Authentication**: Logs in as a static test user.
2.  **Multi-Database Isolation**: Creates multiple databases (`alpha`, `beta`) and ensures data saved in one does not leak to the other.
3.  **RocksDB Integration**: Performs cryptographic updates and searches using the C++ backend.
4.  **File Operations**: Uploads files, lists them, and visualizes the folder hierarchy using a pause (to allow manual inspection).
5.  **Secure Download**: Verifies file integrity by downloading and comparing hashes.
6.  **Clean Deletion**: Verifies that `DELETE` operations physically remove the folders from the disk.
7.  **Logout Security**: Confirms that tokens are immediately invalidated after logout.
