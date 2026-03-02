/**
 * Страница аналитики (дашборд): KPI, графики по бакетам, детализации.
 */

checkAuth();

let chartUsers, chartContent, chartArt, chartGeneration, chartReferrals;

function getDefaultRange() {
    const to = new Date();
    const from = new Date(to);
    from.setDate(from.getDate() - 30);
    return {
        from: from.toISOString().slice(0, 19).replace('T', 'T'),
        to: to.toISOString().slice(0, 19).replace('T', 'T')
    };
}

function setDefaultFilters() {
    const range = getDefaultRange();
    const fromEl = document.getElementById('filter-from');
    const toEl = document.getElementById('filter-to');
    if (fromEl) fromEl.value = range.from;
    if (toEl) toEl.value = range.to;
}

function getFilters() {
    const fromEl = document.getElementById('filter-from');
    const toEl = document.getElementById('filter-to');
    const gran = document.getElementById('filter-granularity');
    const from = fromEl && fromEl.value ? new Date(fromEl.value) : new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
    const to = toEl && toEl.value ? new Date(toEl.value) : new Date();
    return {
        from: from.toISOString(),
        to: to.toISOString(),
        granularity: (gran && gran.value) || 'day',
        tz: 'UTC'
    };
}

function showLoading(show) {
    document.getElementById('loading-state').classList.toggle('hidden', !show);
    document.getElementById('error-state').classList.add('hidden');
    document.getElementById('dashboard-content').classList.toggle('hidden', show);
}

function showError(show) {
    document.getElementById('loading-state').classList.add('hidden');
    document.getElementById('error-state').classList.toggle('hidden', !show);
    document.getElementById('dashboard-content').classList.toggle('hidden', show);
}

function renderKpi(data) {
    const kpi = data.kpiCards;
    if (!kpi) return;
    const cards = [
        { label: 'Всего пользователей', value: kpi.totalUsers },
        { label: 'Новых за период', value: kpi.newUsers },
        { label: 'Активных за период', value: kpi.activeUsers },
        { label: 'Стикерсетов создано', value: kpi.createdStickerSets },
        { label: 'Лайков', value: kpi.likes },
        { label: 'Дизлайков', value: kpi.dislikes },
        { label: 'Свайпов', value: kpi.swipes },
        { label: 'ART заработано', value: kpi.artEarned },
        { label: 'ART потрачено', value: kpi.artSpent },
        { label: 'Запусков генерации', value: kpi.generationRuns },
        { label: 'Успешность генерации %', value: (kpi.generationSuccessRate != null ? Number(kpi.generationSuccessRate).toFixed(1) : '0') + '%' },
        { label: 'Реферальных событий', value: kpi.referralEventsTotal }
    ];
    const html = cards.map(c => `
        <div class="bg-white rounded-lg shadow p-3 border border-gray-100">
            <p class="text-xs text-gray-500 truncate">${escapeHtml(c.label)}</p>
            <p class="text-lg font-semibold text-gray-800 mt-0.5">${formatNumber(c.value)}</p>
        </div>
    `).join('');
    document.getElementById('kpi-cards').innerHTML = html;
}

function seriesToChartData(series, label) {
    if (!series || !series.length) return { labels: [], datasets: [{ label, data: [] }] };
    const labels = series.map(p => {
        const s = (p.bucketStart || '').replace('Z', '').slice(0, 16);
        return s.replace('T', ' ');
    });
    const data = series.map(p => (p.value != null ? p.value : 0));
    return {
        labels,
        datasets: [{ label, data, borderColor: 'rgb(59, 130, 246)', backgroundColor: 'rgba(59, 130, 246, 0.1)', fill: true, tension: 0.2 }]
    };
}

function destroyChart(chart) {
    if (chart) {
        chart.destroy();
    }
}

function createLineChart(canvasId, series, label, options = {}) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return null;
    const config = {
        type: 'line',
        data: seriesToChartData(series, label),
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { ticks: { maxRotation: 45, font: { size: 10 } } },
                y: { beginAtZero: true }
            },
            ...options
        }
    };
    return new Chart(ctx, config);
}

function renderCharts(data) {
    const ts = data.timeseries || {};
    destroyChart(chartUsers);
    destroyChart(chartContent);
    destroyChart(chartArt);
    destroyChart(chartGeneration);
    destroyChart(chartReferrals);

    chartUsers = createLineChart('chart-users', ts.newUsers, 'Новые пользователи');
    if (ts.activeUsers && ts.activeUsers.length) {
        const labels = ts.activeUsers.map(p => (p.bucketStart || '').replace('Z', '').slice(0, 16).replace('T', ' '));
        const dataActive = ts.activeUsers.map(p => p.value != null ? p.value : 0);
        const ctx = document.getElementById('chart-users');
        if (ctx && chartUsers) {
            chartUsers.data.datasets.push({
                label: 'Активные',
                data: dataActive,
                borderColor: 'rgb(34, 197, 94)',
                backgroundColor: 'rgba(34, 197, 94, 0.1)',
                fill: true,
                tension: 0.2
            });
            chartUsers.update();
        }
    }

    const seriesList = ts.createdStickerSets || ts.likes || ts.swipes || [];
    const contentLabels = seriesList.length ? seriesList.map(p => (p.bucketStart || '').replace('Z', '').slice(0, 16).replace('T', ' ')) : [];
    const contentData = {
        labels: contentLabels,
        datasets: [
            { label: 'Стикерсеты', data: (ts.createdStickerSets || []).map(p => p.value != null ? p.value : 0), borderColor: 'rgb(59, 130, 246)', backgroundColor: 'rgba(59, 130, 246, 0.1)', fill: true, tension: 0.2 },
            { label: 'Лайки', data: (ts.likes || []).map(p => p.value != null ? p.value : 0), borderColor: 'rgb(34, 197, 94)', backgroundColor: 'rgba(34, 197, 94, 0.1)', fill: true, tension: 0.2 },
            { label: 'Свайпы', data: (ts.swipes || []).map(p => p.value != null ? p.value : 0), borderColor: 'rgb(234, 179, 8)', backgroundColor: 'rgba(234, 179, 8, 0.1)', fill: true, tension: 0.2 }
        ]
    };
    const ctxContent = document.getElementById('chart-content');
    if (ctxContent) {
        chartContent = new Chart(ctxContent, {
            type: 'line',
            data: contentData,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { x: { ticks: { maxRotation: 45, font: { size: 10 } } }, y: { beginAtZero: true } }
            }
        });
    }

    const artEarnedSeries = ts.artEarned || [];
    const artSpentSeries = ts.artSpent || [];
    const artAllKeys = Array.from(new Set([
        ...artEarnedSeries.map(p => p.bucketStart),
        ...artSpentSeries.map(p => p.bucketStart)
    ])).sort();
    const artEarnedMap = Object.fromEntries(artEarnedSeries.map(p => [p.bucketStart, p.value != null ? p.value : 0]));
    const artSpentMap = Object.fromEntries(artSpentSeries.map(p => [p.bucketStart, p.value != null ? p.value : 0]));
    const artLabels = artAllKeys.map(k => k.replace('Z', '').slice(0, 16).replace('T', ' '));
    const ctxArt = document.getElementById('chart-art');
    if (ctxArt) {
        chartArt = new Chart(ctxArt, {
            type: 'line',
            data: {
                labels: artLabels,
                datasets: [
                    {
                        label: 'ART заработано',
                        data: artAllKeys.map(k => artEarnedMap[k] ?? 0),
                        borderColor: 'rgb(59, 130, 246)',
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        fill: true,
                        tension: 0.2
                    },
                    {
                        label: 'ART потрачено',
                        data: artAllKeys.map(k => artSpentMap[k] ?? 0),
                        borderColor: 'rgb(239, 68, 68)',
                        backgroundColor: 'rgba(239, 68, 68, 0.1)',
                        fill: true,
                        tension: 0.2
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { ticks: { maxRotation: 45, font: { size: 10 } } },
                    y: { beginAtZero: true }
                }
            }
        });
    }

    chartGeneration = createLineChart('chart-generation', ts.generationRuns, 'Запуски');
    if (ts.generationSuccess && ts.generationSuccess.length && chartGeneration) {
        chartGeneration.data.datasets.push({
            label: 'Успешные',
            data: ts.generationSuccess.map(p => p.value != null ? p.value : 0),
            borderColor: 'rgb(34, 197, 94)',
            backgroundColor: 'rgba(34, 197, 94, 0.1)',
            fill: true,
            tension: 0.2
        });
        chartGeneration.update();
    }

    chartReferrals = createLineChart('chart-referrals', ts.referralEvents, 'Реферальные события');
}

function renderBreakdowns(data) {
    const b = data.breakdowns || {};
    const refEl = document.getElementById('breakdown-referral');
    const genEl = document.getElementById('breakdown-generation');
    if (refEl) {
        const refByType = b.referralByType || {};
        refEl.innerHTML = Object.keys(refByType).length
            ? Object.entries(refByType).map(([k, v]) => `<p class="py-0.5"><span class="font-medium">${escapeHtml(k)}</span>: ${formatNumber(v)}</p>`).join('')
            : '<p class="text-gray-500">Нет данных за период</p>';
    }
    if (genEl) {
        const genByStage = b.generationByStageStatus || {};
        genEl.innerHTML = Object.keys(genByStage).length
            ? Object.entries(genByStage).map(([k, v]) => `<p class="py-0.5"><span class="font-medium">${escapeHtml(k)}</span>: ${formatNumber(v)}</p>`).join('')
            : '<p class="text-gray-500">Нет данных за период</p>';
    }
}

async function loadDashboard() {
    const { from, to, granularity, tz } = getFilters();
    showLoading(true);
    try {
        const response = await api.getAnalyticsDashboard(from, to, granularity, tz);
        showLoading(false);
        renderKpi(response);
        renderCharts(response);
        renderBreakdowns(response);
    } catch (err) {
        showLoading(false);
        showError(true);
        console.error('Analytics load failed:', err);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    setDefaultFilters();
    document.getElementById('btn-apply').addEventListener('click', loadDashboard);
    const retryBtn = document.getElementById('error-retry-btn');
    if (retryBtn) retryBtn.addEventListener('click', loadDashboard);
    loadDashboard();
});
