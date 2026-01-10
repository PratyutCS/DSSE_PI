// using global fetch available in Node 18+
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const BASE_URL = 'http://localhost:3000/api';

// Helper for pause
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function runTest() {
    try {
        console.log('--- Starting Merged Verification Suite ---');

        const BaseRegUser = 'testuser';
        const BaseRegPass = 'password123';

        // -------------------------------------------------------------------------
        // 1. Auth: Login
        // -------------------------------------------------------------------------
        console.log('\n[1] Logging in...');
        let loginRes = await fetch(`${BASE_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: BaseRegUser, password: BaseRegPass })
        });

        let loginData = await loginRes.json();

        // Handle registration if needed (resiliency)
        if (loginRes.status !== 200 || !loginData.token) {
            console.log('Login failed, attempting registration...');
            await fetch(`${BASE_URL}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: BaseRegUser, password: BaseRegPass })
            });
            // Retry login
            loginRes = await fetch(`${BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: BaseRegUser, password: BaseRegPass })
            });
            loginData = await loginRes.json();
        }

        if (!loginData.token) throw new Error('Authentication Failed');
        const token = loginData.token;
        const userId = loginData._id;
        console.log(`Login Success. User ID: ${userId}`);


        // -------------------------------------------------------------------------
        // 2. Multi-Database Scenario (RocksDB Operations)
        // -------------------------------------------------------------------------
        console.log('\n[2] Starting Multi-Database Scenario (RocksDB Operations)...');
        const dbNames_Rocks = ['test_db_alpha', 'test_db_beta'];

        for (const dbName of dbNames_Rocks) {
            console.log(`\n  > [${dbName}] Operations:`);

            // A. Create Space
            await fetch(`${BASE_URL}/delete`, { // Cleanup first
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ dbName })
            });

            const spaceRes = await fetch(`${BASE_URL}/new`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ dbName })
            });
            console.log(`    Create Status: ${spaceRes.status}`);

            // B. Save Index Value (RocksDB Update)
            const uniqueVal = `val_${dbName}`;
            const kvRes = await fetch(`${BASE_URL}/save-index_value`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    dbName: dbName,
                    key: 'hello',
                    value: uniqueVal
                })
            });
            console.log(`    Save (RocksDB) Status: ${kvRes.status}`);

            // C. Get Index Value (RocksDB Search)
            const queryParams = new URLSearchParams({
                dbName: dbName,
                keyword_token: 'dummy_keyword_value',
                state_token: '1234567890123456',
                count: '1'
            });

            const getKvRes = await fetch(`${BASE_URL}/get-index_value?${queryParams}`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const getKvData = await getKvRes.json();
            const resCount = Array.isArray(getKvData.results) ? getKvData.results.length : 0;
            console.log(`    Search Result Count: ${resCount}`);
        }

        // -------------------------------------------------------------------------
        // 3. File Upload Scenario
        // -------------------------------------------------------------------------
        const dbName_Files = 'test_db_files';
        console.log(`\n[3] Starting File Upload Scenario (${dbName_Files})...`);

        // Setup Space
        await fetch(`${BASE_URL}/delete`, { // Cleanup
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ dbName: dbName_Files })
        });
        await fetch(`${BASE_URL}/new`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ dbName: dbName_Files })
        });
        console.log(`  Space ${dbName_Files} created.`);

        // Upload Files
        console.log('  Uploading Files...');
        const file1Path = path.join(__dirname, 'data/file1.txt');
        const file2Path = path.join(__dirname, 'data/file2.txt');

        // Ensure dummy files exist
        if (!fs.existsSync(file1Path)) fs.writeFileSync(file1Path, 'Dummy Content 1');
        if (!fs.existsSync(file2Path)) fs.writeFileSync(file2Path, 'Dummy Content 2');

        const file1Content = fs.readFileSync(file1Path);
        const file2Content = fs.readFileSync(file2Path);

        const formData = new FormData();
        const blob1 = new Blob([file1Content]);
        const blob2 = new Blob([file2Content]);

        formData.append('files', blob1, 'file1.txt');
        formData.append('files', blob2, 'file2.txt');

        const uploadRes = await fetch(`${BASE_URL}/upload_files?dbName=${dbName_Files}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });
        const uploadData = await uploadRes.json();
        console.log(`  Upload Status: ${uploadRes.status} (${uploadData.message})`);

        // Retrieve List
        const listRes = await fetch(`${BASE_URL}/get-files?dbName=${dbName_Files}`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const listData = await listRes.json();
        console.log(`  Files Retrieved: ${JSON.stringify(listData.files)}`);


        // -------------------------------------------------------------------------
        // 4. Visualization & Deletion
        // -------------------------------------------------------------------------
        console.log('\n[4] Hierarchy Visualization & Deletion...');

        console.log('\n--- STORAGE HIERARCHY (Before Deletion) ---');
        try {
            const output = execSync('ls -R storage', { encoding: 'utf-8' });
            console.log(output);
        } catch (e) {
            console.error('Visualization Error:', e.message);
        }
        console.log('-------------------------------------------\n');

        console.log('Pausing for 3 seconds before deletion...');
        await sleep(3000);

        // Delete All Spaces Created
        const allTestDBs = [...dbNames_Rocks, dbName_Files];

        for (const db of allTestDBs) {
            console.log(`  Deleting Space: ${db}...`);
            const delRes = await fetch(`${BASE_URL}/delete`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ dbName: db })
            });
            console.log(`    API Status: ${delRes.status}`);

            // Verify FS
            const checkPath = path.join('./storage', userId, db);
            if (fs.existsSync(checkPath)) {
                console.log(`    FAILURE: Folder ${checkPath} STILL EXISTS.`);
            } else {
                console.log(`    SUCCESS: Folder ${checkPath} was deleted.`);
            }
        }

        console.log('\n--- STORAGE HIERARCHY (After Deletion) ---');
        try {
            const outputAfter = execSync(`ls -R storage/${userId} || echo "User folder empty or gone"`, { encoding: 'utf-8' });
            console.log(outputAfter);
        } catch (e) {
            console.log('(User folder likely empty/gone)');
        }
        console.log('------------------------------------------\n');


        // -------------------------------------------------------------------------
        // 5. Logout Verification
        // -------------------------------------------------------------------------
        console.log('[5] Logout Verification...');

        // Pre-check
        const preCheck = await fetch(`${BASE_URL}/get-files?dbName=test_db_files`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        // 404/200 means authorized. 401 means unauthorized.
        if (preCheck.status === 401) console.error('  FAILURE: Token invalid before logout.');
        else console.log('  Token Valid (Pre-Logout).');

        // Logout
        await fetch(`${BASE_URL}/logout`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        console.log('  Logged Out.');

        // Post-check
        const postCheck = await fetch(`${BASE_URL}/get-files?dbName=test_db_files`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (postCheck.status === 401) {
            console.log('  SUCCESS: Access denied after logout (401).');
        } else {
            console.log(`  FAILURE: Still able to access (Status: ${postCheck.status}).`);
        }

        console.log('\n--- Full Merged Verification Suite Complete ---');

    } catch (error) {
        console.error('Test Suite Failed:', error);
    }
}

runTest();
