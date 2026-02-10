document.addEventListener('DOMContentLoaded', () => {
    fetchData();
});

async function fetchData() {
    try {
        const response = await fetch('data.json');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        renderTable(data);
    } catch (error) {
        console.error('Error fetching data:', error);
        document.querySelector('.table-container').innerHTML = `
            <div style="text-align: center; color: #ef4444; padding: 2rem;">
                <h3>Error loading data</h3>
                <p>Please ensure you are running this via a local server (e.g., using 'python3 -m http.server').</p>
                <p>Details: ${error.message}</p>
            </div>
        `;
    }
}

function renderTable(data) {
    const tableBody = document.querySelector('#performance-table tbody');
    tableBody.innerHTML = '';

    data.forEach((row, index) => {
        const tr = document.createElement('tr');
        tr.style.animation = `fadeInUp 0.5s ease-out ${index * 0.1}s backwards`;

        tr.innerHTML = `
            <td>#${row.run}</td>
            <td>${formatNumber(row.input_index_range)}</td>
            <td>${formatTime(row.setup_time_s)}</td>
            <td>${formatTime(row.random_input_time_s)}</td>
            <td>${formatTime(row.db_conversion_time_s)}</td>
            <td>${formatTime(row.update_client_time_s)}</td>
            <td>${formatTime(row.update_server_time_s)}</td>
            <td>${formatTime(row.post_processing_time_s)}</td>
            <td>${formatSearch(row.search)}</td>
            <td>${formatResults(row.results)}</td>
        `;

        tableBody.appendChild(tr);
    });

    renderCharts(data);
}

function renderCharts(data) {
    const container = document.getElementById('charts-container');
    container.innerHTML = '';

    // Metrics to plot
    const metrics = [
        { label: 'Setup Time (s)', key: 'setup_time_s', color: '#38bdf8' },
        { label: 'DB Conversion (s)', key: 'db_conversion_time_s', color: '#a855f7' },
        { label: 'Update Client (s)', key: 'update_client_time_s', color: '#ec4899' },
        { label: 'Update Server (s)', key: 'update_server_time_s', color: '#f43f5e' },
        { label: 'Post Processing (s)', key: 'post_processing_time_s', color: '#10b981' }
    ];

    // Sort data by run number for the charts
    const sortedData = [...data].sort((a, b) => a.run - b.run);
    const labels = sortedData.map(d => `Run #${d.run} (${formatNumber(d.input_index_range)})`);

    metrics.forEach(metric => {
        const card = document.createElement('div');
        card.className = 'chart-card';
        card.innerHTML = `
            <h3>${metric.label}</h3>
            <div class="chart-inner-container">
                <canvas id="chart-${metric.key}"></canvas>
            </div>
        `;
        container.appendChild(card);

        const ctx = document.getElementById(`chart-${metric.key}`).getContext('2d');
        new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: metric.label,
                    data: sortedData.map(d => d[metric.key]),
                    borderColor: metric.color,
                    backgroundColor: metric.color + '22',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 5,
                    pointHoverRadius: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { color: 'rgba(255, 255, 255, 0.1)' },
                        ticks: { color: '#94a3b8' }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { color: '#94a3b8' }
                    }
                }
            }
        });
    });
}

function formatNumber(num) {
    return new Intl.NumberFormat().format(num);
}

function formatTime(seconds) {
    return `<span style="font-feature-settings: 'tnum';">${seconds.toFixed(4)}s</span>`;
}

function formatSearch(searchData) {
    if (!searchData) return '-';
    let html = '<div class="nested-data">';
    for (const [key, val] of Object.entries(searchData)) {
        html += `<div><span class="nested-label">${key}:</span> ${val.value} (${val.time_s.toFixed(5)}s)</div>`;
    }
    html += '</div>';
    return html;
}

function formatResults(resultsData) {
    if (!resultsData) return '-';
    let html = '<div class="nested-data">';
    for (const [client, val] of Object.entries(resultsData)) {
        html += `<div><span class="nested-label">${client}:</span> P1=${val.param1}, P2=${val.param2}</div>`;
    }
    html += '</div>';
    return html;
}
