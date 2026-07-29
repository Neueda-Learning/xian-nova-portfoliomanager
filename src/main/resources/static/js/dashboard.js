let allocationChart = null;
let performanceChart = null;
let snapshotChart = null;
let editingItemId = null;
let pendingRemovalItem = null;
let currentSnapshot = null;
let selectedSnapshotWindow = 30;
let portfolioItems = [];
let portfolioSummary = null;
let snapshotRequestController = null;
let navigationIntentTimer = null;

const SUPPORTED_TICKERS = ['C', 'AMZN', 'TSLA', 'FB', 'AAPL'];
const ASSET_NAMES = {
    C: 'Citigroup',
    AMZN: 'Amazon',
    TSLA: 'Tesla',
    FB: 'Meta Platforms',
    AAPL: 'Apple'
};
const CHART_COLORS = ['#2b5a43', '#b9db73', '#e8a45c', '#829c8c', '#c7b88d', '#79a7a0', '#a18bad'];

const currencyFormatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

const compactCurrencyFormatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    notation: 'compact',
    maximumFractionDigits: 1
});

const numberFormatter = new Intl.NumberFormat('en-US', {
    maximumFractionDigits: 4
});

function money(value) {
    return currencyFormatter.format(Number(value) || 0);
}

function compactMoney(value) {
    return compactCurrencyFormatter.format(Number(value) || 0);
}

function percent(value, includeSign = false) {
    const number = Number(value) || 0;
    const sign = includeSign && number > 0 ? '+' : '';
    return `${sign}${number.toFixed(2)}%`;
}

function formatQuantity(value) {
    return numberFormatter.format(Number(value) || 0);
}

function formatDate(value) {
    if (!value) {
        return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return String(value);
    }
    return new Intl.DateTimeFormat('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
    }).format(date);
}

function toDateInputValue(value) {
    return value ? String(value).substring(0, 10) : '';
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function getDirectionClass(value) {
    const number = Number(value) || 0;
    return number > 0 ? 'profit' : number < 0 ? 'loss' : 'neutral';
}

function getDirectionIcon(value) {
    const number = Number(value) || 0;
    return number > 0 ? 'bi-arrow-up-right' : number < 0 ? 'bi-arrow-down-right' : 'bi-dash';
}

function removeSkeletons() {
    document.querySelectorAll('.skeleton-text').forEach(node => node.classList.remove('skeleton-text'));
}

function fitHeroMetricValues() {
    ['totalCost', 'totalPnl', 'positionCount'].forEach(id => {
        const node = document.getElementById(id);
        node.style.fontSize = '';
        node.title = node.textContent;

        let fontSize = Number.parseFloat(window.getComputedStyle(node).fontSize);
        while (node.scrollWidth > node.clientWidth && fontSize > 12) {
            fontSize -= 1;
            node.style.fontSize = `${fontSize}px`;
        }
    });
}

function showToast(message, type = 'success', timeout = 3200) {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `app-toast ${type}`;
    toast.setAttribute('role', type === 'error' ? 'alert' : 'status');

    const icon = document.createElement('span');
    icon.className = 'toast-icon';
    icon.innerHTML = `<i class="bi ${type === 'error' ? 'bi-exclamation-lg' : 'bi-check-lg'}" aria-hidden="true"></i>`;

    const text = document.createElement('span');
    text.textContent = message;

    const close = document.createElement('button');
    close.type = 'button';
    close.className = 'toast-close';
    close.setAttribute('aria-label', 'Dismiss notification');
    close.innerHTML = '<i class="bi bi-x-lg" aria-hidden="true"></i>';
    close.addEventListener('click', () => toast.remove());

    toast.append(icon, text, close);
    container.appendChild(toast);
    window.setTimeout(() => toast.remove(), timeout);
}

function setPageContext(username) {
    const now = new Date();
    const hour = now.getHours();
    const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
    const cleanUsername = username || 'Investor';
    const initials = cleanUsername
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map(part => part.charAt(0).toUpperCase())
        .join('') || 'PM';

    document.getElementById('greeting').textContent = greeting;
    document.getElementById('currentUser').textContent = cleanUsername;
    document.getElementById('sidebarUsername').textContent = cleanUsername;
    document.getElementById('userAvatar').textContent = initials;
    document.getElementById('currentDate').textContent = new Intl.DateTimeFormat('en-US', {
        weekday: 'long',
        month: 'long',
        day: 'numeric'
    }).format(now);
}

function markUpdated() {
    const node = document.getElementById('lastUpdated');
    node.innerHTML = '<i class="bi bi-check2-circle" aria-hidden="true"></i> Updated just now';
}

function initTickerSelect(selectId, defaultValue) {
    const select = document.getElementById(selectId);
    select.innerHTML = '';
    SUPPORTED_TICKERS.forEach(ticker => {
        const option = document.createElement('option');
        option.value = ticker;
        option.textContent = ticker;
        select.appendChild(option);
    });
    select.value = defaultValue;
}

function ensureTickerOption(selectId, ticker) {
    const select = document.getElementById(selectId);
    const exists = Array.from(select.options).some(option => option.value === ticker);
    if (!exists) {
        const option = document.createElement('option');
        option.value = ticker;
        option.textContent = ticker;
        select.appendChild(option);
    }
}

function aggregateItemsByTicker(items) {
    const result = new Map();
    items.forEach(item => {
        const ticker = String(item.ticker);
        const existing = result.get(ticker) || { ticker, cost: 0, value: 0 };
        existing.cost += Number(item.buyPrice) * Number(item.quantity);
        existing.value += Number(item.currentValue);
        result.set(ticker, existing);
    });
    return Array.from(result.values()).sort((a, b) => b.value - a.value);
}

function renderSummary(summary, items) {
    const cost = Number(summary.totalCost) || 0;
    const value = Number(summary.totalMarketValue) || 0;
    const pnl = Number(summary.totalProfitLoss) || 0;
    const returnPct = cost > 0 ? (pnl / cost) * 100 : 0;

    document.getElementById('totalValue').textContent = money(value);
    document.getElementById('totalCost').textContent = money(cost);
    document.getElementById('totalPnl').textContent = money(pnl);
    document.getElementById('totalPnl').className = getDirectionClass(pnl);
    document.getElementById('positionCount').textContent = String(items.length);
    document.getElementById('totalReturn').textContent = percent(returnPct, true);
    document.getElementById('holdingsTotal').textContent = money(value);

    const returnBadge = document.getElementById('returnBadge');
    returnBadge.className = `return-badge ${getDirectionClass(returnPct)}`;
    returnBadge.querySelector('i').className = `bi ${getDirectionIcon(returnPct)}`;

    const performanceSummary = document.getElementById('performanceSummary');
    if (items.length === 0) {
        performanceSummary.textContent = 'Add a position to compare invested capital with current value.';
    } else if (pnl === 0) {
        performanceSummary.textContent = `Current value is level with cost across ${items.length} ${items.length === 1 ? 'position' : 'positions'}.`;
    } else {
        performanceSummary.textContent = `Your portfolio is ${money(Math.abs(pnl))} ${pnl > 0 ? 'above' : 'below'} its invested cost.`;
    }

    removeSkeletons();
    window.requestAnimationFrame(fitHeroMetricValues);
}

function renderPerformanceChart(items) {
    const empty = document.getElementById('performanceChartEmpty');
    const ctx = document.getElementById('performanceChart');
    const aggregated = aggregateItemsByTicker(items).slice(0, 8);

    if (performanceChart) {
        performanceChart.destroy();
        performanceChart = null;
    }

    if (aggregated.length === 0 || typeof Chart === 'undefined') {
        ctx.hidden = true;
        empty.hidden = false;
        return;
    }

    ctx.hidden = false;
    empty.hidden = true;
    performanceChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: aggregated.map(item => item.ticker),
            datasets: [
                {
                    label: 'Invested cost',
                    data: aggregated.map(item => item.cost),
                    backgroundColor: '#dfe5e0',
                    borderColor: '#dfe5e0',
                    borderWidth: 1,
                    borderRadius: 6,
                    borderSkipped: false,
                    maxBarThickness: 34
                },
                {
                    label: 'Market value',
                    data: aggregated.map(item => item.value),
                    backgroundColor: '#2b5a43',
                    borderColor: '#2b5a43',
                    borderWidth: 1,
                    borderRadius: 6,
                    borderSkipped: false,
                    maxBarThickness: 34
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false
            },
            plugins: {
                legend: {
                    position: 'top',
                    align: 'end',
                    labels: {
                        color: '#66716b',
                        usePointStyle: true,
                        pointStyle: 'circle',
                        boxWidth: 7,
                        boxHeight: 7,
                        padding: 18,
                        font: { family: 'DM Sans', size: 10 }
                    }
                },
                tooltip: {
                    backgroundColor: '#14261d',
                    titleFont: { family: 'DM Sans', size: 11 },
                    bodyFont: { family: 'DM Mono', size: 10 },
                    padding: 11,
                    cornerRadius: 9,
                    callbacks: {
                        label: context => `${context.dataset.label}: ${money(context.parsed.y)}`
                    }
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    border: { display: false },
                    ticks: {
                        color: '#66716b',
                        font: { family: 'DM Mono', size: 10 }
                    }
                },
                y: {
                    beginAtZero: true,
                    border: { display: false },
                    grid: { color: 'rgba(226, 231, 226, 0.75)' },
                    ticks: {
                        color: '#919a95',
                        padding: 8,
                        font: { family: 'DM Sans', size: 9 },
                        callback: value => compactMoney(value)
                    }
                }
            }
        }
    });
}

function renderAllocationChart(summary) {
    const labels = Object.keys(summary.allocationPercentages || {});
    const values = Object.values(summary.allocationPercentages || {}).map(Number);
    const ctx = document.getElementById('allocationChart');
    const legend = document.getElementById('allocationLegend');

    document.getElementById('allocationCount').textContent = String(labels.length);
    legend.innerHTML = '';

    labels.forEach((label, index) => {
        const item = document.createElement('div');
        item.className = 'legend-item';
        item.innerHTML = `
            <span class="legend-dot" style="background:${CHART_COLORS[index % CHART_COLORS.length]}"></span>
            <span>${escapeHtml(label)}</span>
            <strong>${percent(values[index])}</strong>
        `;
        legend.appendChild(item);
    });

    if (labels.length === 0) {
        legend.innerHTML = '<span class="legend-item"><span class="legend-dot" style="background:#dfe5e0"></span><span>No holdings</span><strong>0%</strong></span>';
    }

    if (allocationChart) {
        allocationChart.destroy();
        allocationChart = null;
    }

    if (typeof Chart === 'undefined') {
        ctx.hidden = true;
        return;
    }

    ctx.hidden = false;
    allocationChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels.length ? labels : ['No holdings'],
            datasets: [{
                data: values.length ? values : [1],
                backgroundColor: values.length
                    ? labels.map((_, index) => CHART_COLORS[index % CHART_COLORS.length])
                    : ['#edf1ed'],
                borderColor: '#ffffff',
                borderWidth: 4,
                hoverOffset: values.length ? 4 : 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '72%',
            plugins: {
                legend: { display: false },
                tooltip: {
                    enabled: values.length > 0,
                    backgroundColor: '#14261d',
                    bodyFont: { family: 'DM Sans', size: 10 },
                    padding: 10,
                    cornerRadius: 9,
                    callbacks: {
                        label: context => `${context.label}: ${percent(context.parsed)}`
                    }
                }
            }
        }
    });
}

function getFilteredItems() {
    const query = document.getElementById('holdingSearch').value.trim().toLowerCase();
    const assetType = document.getElementById('assetFilter').value;
    return portfolioItems.filter(item => {
        const matchesQuery = String(item.ticker).toLowerCase().includes(query)
            || (ASSET_NAMES[item.ticker] || '').toLowerCase().includes(query);
        const matchesType = assetType === 'ALL' || item.assetType === assetType;
        return matchesQuery && matchesType;
    });
}

function renderTable() {
    const body = document.getElementById('portfolioTableBody');
    const tableScroller = document.querySelector('.table-scroller');
    const empty = document.getElementById('holdingsEmpty');
    const emptyTitle = document.getElementById('emptyStateTitle');
    const emptyCopy = document.getElementById('emptyStateCopy');
    const emptyAddButton = document.getElementById('emptyAddButton');
    const filteredItems = getFilteredItems();

    body.innerHTML = '';
    document.getElementById('holdingsCount').textContent =
        `${filteredItems.length}${filteredItems.length !== portfolioItems.length ? ` of ${portfolioItems.length}` : ''} ${filteredItems.length === 1 ? 'holding' : 'holdings'}`;

    if (filteredItems.length === 0) {
        tableScroller.hidden = true;
        empty.hidden = false;
        if (portfolioItems.length === 0) {
            emptyTitle.textContent = 'Build your first portfolio';
            emptyCopy.textContent = 'Add a position and Portfolio Manager will track its value and performance.';
            emptyAddButton.hidden = false;
        } else {
            emptyTitle.textContent = 'No matching holdings';
            emptyCopy.textContent = 'Try a different ticker or asset-type filter.';
            emptyAddButton.hidden = true;
        }
        return;
    }

    tableScroller.hidden = false;
    empty.hidden = true;

    filteredItems.forEach(item => {
        const cost = Number(item.buyPrice) * Number(item.quantity);
        const pnl = Number(item.profitLoss);
        const pnlPct = cost > 0 ? (pnl / cost) * 100 : 0;
        const directionClass = getDirectionClass(pnlPct);
        const ticker = escapeHtml(item.ticker);
        const type = escapeHtml(String(item.assetType || '').toLowerCase());
        const company = escapeHtml(ASSET_NAMES[item.ticker] || `${item.assetType || 'Portfolio'} asset`);

        const row = document.createElement('tr');
        row.innerHTML = `
            <td>
                <div class="asset-cell">
                    <span class="asset-monogram">${ticker.substring(0, 2)}</span>
                    <span class="asset-copy">
                        <strong>${ticker}</strong>
                        <small>${company} · ${type}</small>
                    </span>
                </div>
            </td>
            <td>${formatQuantity(item.quantity)}</td>
            <td>${money(item.buyPrice)}</td>
            <td>${money(item.currentPrice)}</td>
            <td>${money(item.currentValue)}</td>
            <td>
                <span class="table-return ${directionClass}">
                    <i class="bi ${getDirectionIcon(pnlPct)}" aria-hidden="true"></i>
                    ${percent(pnlPct, true)}
                </span>
            </td>
            <td>
                <span class="row-actions">
                    <button class="row-action" type="button" data-action="edit" aria-label="Edit ${ticker}" title="Edit">
                        <i class="bi bi-pencil" aria-hidden="true"></i>
                    </button>
                    <button class="row-action remove" type="button" data-action="remove" aria-label="Remove ${ticker}" title="Remove">
                        <i class="bi bi-trash3" aria-hidden="true"></i>
                    </button>
                </span>
            </td>
        `;

        row.querySelector('[data-action="edit"]').addEventListener('click', () => openEditDialog(item));
        row.querySelector('[data-action="remove"]').addEventListener('click', () => openRemoveDialog(item));
        body.appendChild(row);
    });
}

function renderDashboard(items, summary) {
    portfolioItems = items;
    portfolioSummary = summary;
    renderSummary(summary, items);
    renderPerformanceChart(items);
    renderAllocationChart(summary);
    renderTable();
    markUpdated();
}

function resetPositionForm() {
    editingItemId = null;
    const form = document.getElementById('addItemForm');
    form.reset();
    document.getElementById('positionDialogTitle').textContent = 'Add a new position';
    document.getElementById('savePositionLabel').textContent = 'Add position';
    document.getElementById('formMessage').textContent = '';
    document.getElementById('formMessage').className = 'form-message';
    document.getElementById('purchaseDate').value = new Date().toISOString().substring(0, 10);
    document.getElementById('ticker').value = 'TSLA';
    document.getElementById('assetType').value = 'STOCK';
}

function openAddDialog() {
    resetPositionForm();
    document.getElementById('positionDialog').showModal();
    window.setTimeout(() => document.getElementById('ticker').focus(), 0);
}

function openEditDialog(item) {
    resetPositionForm();
    editingItemId = item.id;
    ensureTickerOption('ticker', item.ticker);
    document.getElementById('ticker').value = item.ticker;
    document.getElementById('assetType').value = item.assetType;
    document.getElementById('quantity').value = item.quantity;
    document.getElementById('buyPrice').value = item.buyPrice;
    document.getElementById('purchaseDate').value = toDateInputValue(item.purchaseDate);
    document.getElementById('positionDialogTitle').textContent = `Edit ${item.ticker} position`;
    document.getElementById('savePositionLabel').textContent = 'Save changes';
    document.getElementById('positionDialog').showModal();
    window.setTimeout(() => document.getElementById('quantity').focus(), 0);
}

function openRemoveDialog(item) {
    pendingRemovalItem = item;
    document.getElementById('removeTicker').textContent = item.ticker;
    document.getElementById('removeDialog').showModal();
}

function closeDialog(dialogId) {
    const dialog = document.getElementById(dialogId);
    if (dialog.open) {
        dialog.close();
    }
    if (dialogId === 'removeDialog') {
        pendingRemovalItem = null;
    }
}

async function loadDashboard() {
    const [user, items, summary] = await Promise.all([
        portfolioApi.fetchCurrentUser(),
        portfolioApi.fetchItems(),
        portfolioApi.fetchSummary()
    ]);
    setPageContext(user.username);
    renderDashboard(items, summary);
}

function bindPositionForm() {
    const form = document.getElementById('addItemForm');
    const message = document.getElementById('formMessage');
    const submitButton = document.getElementById('savePositionButton');

    form.addEventListener('submit', async event => {
        event.preventDefault();
        const payload = {
            ticker: document.getElementById('ticker').value,
            assetType: document.getElementById('assetType').value,
            quantity: Number(document.getElementById('quantity').value),
            buyPrice: Number(document.getElementById('buyPrice').value),
            purchaseDate: document.getElementById('purchaseDate').value
        };
        const isEditing = editingItemId !== null;

        submitButton.disabled = true;
        submitButton.querySelector('i').className = 'bi bi-arrow-repeat is-spinning';
        document.getElementById('savePositionLabel').textContent = isEditing ? 'Saving…' : 'Adding…';
        message.textContent = '';

        try {
            if (isEditing) {
                await portfolioApi.updateItem(editingItemId, payload);
            } else {
                await portfolioApi.addItem(payload);
            }
            closeDialog('positionDialog');
            await loadDashboard();
            showToast(isEditing ? `${payload.ticker} was updated.` : `${payload.ticker} was added to your portfolio.`);
            resetPositionForm();
        } catch (error) {
            message.className = 'form-message error';
            message.textContent = error.message || 'The position could not be saved.';
        } finally {
            submitButton.disabled = false;
            submitButton.querySelector('i').className = 'bi bi-arrow-right';
            document.getElementById('savePositionLabel').textContent = isEditing ? 'Save changes' : 'Add position';
        }
    });
}

function bindRemoveDialog() {
    const confirmButton = document.getElementById('confirmRemoveButton');
    confirmButton.addEventListener('click', async () => {
        if (!pendingRemovalItem) {
            return;
        }
        const item = pendingRemovalItem;
        confirmButton.disabled = true;
        confirmButton.textContent = 'Removing…';

        try {
            await portfolioApi.deleteItem(item.id);
            closeDialog('removeDialog');
            await loadDashboard();
            showToast(`${item.ticker} was removed from your portfolio.`);
        } catch (error) {
            showToast(error.message || 'The position could not be removed.', 'error');
        } finally {
            confirmButton.disabled = false;
            confirmButton.textContent = 'Remove position';
        }
    });
}

function extractPriceSeries(rawPayload) {
    if (!rawPayload) {
        return null;
    }

    try {
        const payload = JSON.parse(rawPayload);
        const priceData = payload.price_data || {};
        const readSeries = key => Array.isArray(priceData[key])
            ? priceData[key].map(Number).filter(Number.isFinite)
            : [];
        const close = readSeries('close');
        if (close.length === 0) {
            return null;
        }
        return {
            close,
            volume: readSeries('volume')
        };
    } catch (error) {
        return null;
    }
}

function standardDeviation(values) {
    if (!values || values.length < 2) {
        return 0;
    }
    const mean = values.reduce((sum, value) => sum + value, 0) / values.length;
    const variance = values.reduce((sum, value) => sum + ((value - mean) ** 2), 0) / (values.length - 1);
    return Math.sqrt(variance);
}

function setSnapshotMetric(id, text, cssClass = '') {
    const node = document.getElementById(id);
    node.textContent = text;
    node.classList.remove('profit', 'loss', 'neutral');
    if (cssClass) {
        node.classList.add(cssClass);
    }
}

function clearSnapshotMetrics(dayChangeText = 'Price history is unavailable') {
    [
        'snapshotPrevClose',
        'snapshotPeriodLow',
        'snapshotPeriodHigh',
        'snapshotAvgClose',
        'snapshotPeriodReturn',
        'snapshotVolatility',
        'snapshotAvgVolume',
        'snapshotLatestVolume'
    ].forEach(id => setSnapshotMetric(id, '—'));
    setSnapshotMetric('snapshotDayChange', dayChangeText, 'neutral');
}

function prepareSnapshotForTicker(ticker) {
    currentSnapshot = null;
    document.getElementById('snapshotTickerValue').textContent = `${ticker} · ${ASSET_NAMES[ticker] || 'Market asset'}`;
    document.getElementById('snapshotTickerMark').textContent = ticker.substring(0, 2);
    document.getElementById('snapshotPriceValue').textContent = '—';
    document.getElementById('snapshotFetchedAtValue').textContent = '—';
    clearSnapshotMetrics(`Loading ${ticker} quote…`);

    if (snapshotChart) {
        snapshotChart.destroy();
        snapshotChart = null;
    }
    document.getElementById('snapshotChart').hidden = true;
    const placeholder = document.getElementById('snapshotPlaceholder');
    placeholder.hidden = false;
    placeholder.querySelector('span').textContent = `Loading ${ticker} market data…`;
}

function renderSnapshotChart(series, ticker) {
    const placeholder = document.getElementById('snapshotPlaceholder');
    const ctx = document.getElementById('snapshotChart');
    const close = series.close.slice(-selectedSnapshotWindow);

    if (snapshotChart) {
        snapshotChart.destroy();
        snapshotChart = null;
    }

    if (!close.length || typeof Chart === 'undefined') {
        ctx.hidden = true;
        placeholder.hidden = false;
        return;
    }

    ctx.hidden = false;
    placeholder.hidden = true;
    const labels = close.map((_, index) => {
        const daysAgo = close.length - index - 1;
        return daysAgo === 0 ? 'Today' : `${daysAgo}d`;
    });

    const directionUp = close[close.length - 1] >= close[0];
    const lineColor = directionUp ? '#2b5a43' : '#c4534f';
    const fillColor = directionUp ? 'rgba(43, 90, 67, 0.08)' : 'rgba(196, 83, 79, 0.07)';

    snapshotChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [{
                label: `${ticker} close`,
                data: close,
                borderColor: lineColor,
                backgroundColor: fillColor,
                borderWidth: 2,
                fill: true,
                tension: 0.35,
                pointRadius: 0,
                pointHoverRadius: 4,
                pointHoverBackgroundColor: lineColor
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#14261d',
                    titleFont: { family: 'DM Sans', size: 10 },
                    bodyFont: { family: 'DM Mono', size: 10 },
                    padding: 10,
                    cornerRadius: 9,
                    callbacks: {
                        label: context => money(context.parsed.y)
                    }
                }
            },
            scales: {
                x: {
                    border: { display: false },
                    grid: { display: false },
                    ticks: {
                        maxTicksLimit: 5,
                        color: '#919a95',
                        font: { family: 'DM Sans', size: 9 }
                    }
                },
                y: {
                    position: 'right',
                    border: { display: false },
                    grid: { color: 'rgba(226, 231, 226, 0.72)' },
                    ticks: {
                        color: '#919a95',
                        font: { family: 'DM Sans', size: 9 },
                        callback: value => compactMoney(value)
                    }
                }
            }
        }
    });
}

function renderSnapshot(snapshot) {
    currentSnapshot = snapshot;
    const ticker = snapshot.ticker || '—';
    document.getElementById('snapshotTickerValue').textContent = `${ticker} · ${ASSET_NAMES[ticker] || 'Market asset'}`;
    document.getElementById('snapshotTickerMark').textContent = ticker.substring(0, 2);
    document.getElementById('snapshotPriceValue').textContent = money(snapshot.latestPrice);
    document.getElementById('snapshotFetchedAtValue').textContent = formatDate(snapshot.fetchedAt);

    const series = extractPriceSeries(snapshot.rawPayload);
    if (!series) {
        clearSnapshotMetrics();
        if (snapshotChart) {
            snapshotChart.destroy();
            snapshotChart = null;
        }
        document.getElementById('snapshotChart').hidden = true;
        document.getElementById('snapshotPlaceholder').hidden = false;
        return;
    }

    const close = series.close.slice(-selectedSnapshotWindow);
    if (!close.length) {
        clearSnapshotMetrics();
        return;
    }

    const latest = close[close.length - 1];
    const previous = close.length > 1 ? close[close.length - 2] : latest;
    const first = close[0];
    const dayChange = latest - previous;
    const dayChangePct = previous !== 0 ? (dayChange / previous) * 100 : 0;
    const periodReturn = first !== 0 ? ((latest - first) / first) * 100 : 0;
    const averageClose = close.reduce((sum, value) => sum + value, 0) / close.length;
    const returns = [];

    for (let index = 1; index < close.length; index += 1) {
        if (close[index - 1] !== 0) {
            returns.push((close[index] - close[index - 1]) / close[index - 1]);
        }
    }

    const volume = series.volume.slice(-selectedSnapshotWindow);
    const averageVolume = volume.length
        ? volume.reduce((sum, value) => sum + value, 0) / volume.length
        : 0;
    const latestVolume = volume.length ? volume[volume.length - 1] : 0;
    const directionClass = getDirectionClass(dayChange);

    setSnapshotMetric('snapshotPrevClose', money(previous));
    setSnapshotMetric(
        'snapshotDayChange',
        `${money(dayChange)} · ${percent(dayChangePct, true)} today`,
        directionClass
    );
    setSnapshotMetric('snapshotPeriodHigh', money(Math.max(...close)));
    setSnapshotMetric('snapshotPeriodLow', money(Math.min(...close)));
    setSnapshotMetric('snapshotAvgClose', money(averageClose));
    setSnapshotMetric('snapshotPeriodReturn', percent(periodReturn, true), getDirectionClass(periodReturn));
    setSnapshotMetric('snapshotVolatility', percent(standardDeviation(returns) * 100));
    setSnapshotMetric('snapshotAvgVolume', averageVolume ? Math.round(averageVolume).toLocaleString('en-US') : '—');
    setSnapshotMetric('snapshotLatestVolume', latestVolume ? Math.round(latestVolume).toLocaleString('en-US') : '—');
    renderSnapshotChart(series, ticker);
}

async function loadSnapshot(ticker, { silent = false, resetView = false } = {}) {
    const message = document.getElementById('snapshotMessage');
    const button = document.getElementById('refreshSnapshotButton');
    const icon = button.querySelector('i');
    const requestController = new AbortController();

    if (snapshotRequestController) {
        snapshotRequestController.abort();
    }
    snapshotRequestController = requestController;

    if (resetView) {
        prepareSnapshotForTicker(ticker);
    }

    button.disabled = true;
    icon.classList.add('is-spinning');
    message.className = '';
    message.textContent = `Fetching the latest ${ticker} market data…`;

    try {
        const snapshot = await portfolioApi.fetchLiveSnapshot(ticker, requestController.signal);
        renderSnapshot(snapshot);
        message.className = 'success';
        message.textContent = `${ticker} is current with the latest available quote.`;
        if (!silent) {
            showToast(`${ticker} market data was refreshed.`);
        }
    } catch (error) {
        if (error.name === 'AbortError') {
            return;
        }
        message.className = 'error';
        message.textContent = error.message || `Live data for ${ticker} is unavailable.`;
        if (!silent) {
            showToast(message.textContent, 'error');
        }
    } finally {
        if (snapshotRequestController === requestController) {
            button.disabled = false;
            icon.classList.remove('is-spinning');
            snapshotRequestController = null;
        }
    }
}

function bindSnapshotControls() {
    document.getElementById('snapshotForm').addEventListener('submit', event => {
        event.preventDefault();
        loadSnapshot(document.getElementById('snapshotTicker').value);
    });

    document.getElementById('snapshotTicker').addEventListener('change', event => {
        loadSnapshot(event.target.value, { silent: true, resetView: true });
    });

    document.querySelectorAll('[data-window]').forEach(button => {
        button.addEventListener('click', () => {
            selectedSnapshotWindow = Number(button.dataset.window);
            document.querySelectorAll('[data-window]').forEach(node => {
                const active = node === button;
                node.classList.toggle('active', active);
                node.setAttribute('aria-pressed', String(active));
            });
            if (currentSnapshot) {
                renderSnapshot(currentSnapshot);
            }
        });
    });
}

function setActiveNavigation(sectionId) {
    document.querySelectorAll('.side-nav-link[href^="#"]').forEach(link => {
        const active = link.getAttribute('href') === `#${sectionId}`;
        link.classList.toggle('active', active);
        if (active) {
            link.setAttribute('aria-current', 'location');
        } else {
            link.removeAttribute('aria-current');
        }
    });
}

function updateNavigationFromScroll() {
    const activationLine = Math.min(220, window.innerHeight * 0.28);
    let activeSection = 'overview';

    ['overview', 'holdings', 'market'].forEach(sectionId => {
        const section = document.getElementById(sectionId);
        if (section && section.getBoundingClientRect().top <= activationLine) {
            activeSection = sectionId;
        }
    });

    setActiveNavigation(activeSection);
}

function bindSectionNavigation() {
    const links = document.querySelectorAll('.side-nav-link[href^="#"]');
    let scrollFrameRequested = false;

    links.forEach(link => {
        link.addEventListener('click', () => {
            const sectionId = link.getAttribute('href').substring(1);
            setActiveNavigation(sectionId);
            window.clearTimeout(navigationIntentTimer);
            navigationIntentTimer = window.setTimeout(() => {
                navigationIntentTimer = null;
                updateNavigationFromScroll();
            }, 700);
        });
    });

    window.addEventListener('scroll', () => {
        if (navigationIntentTimer || scrollFrameRequested) {
            return;
        }
        scrollFrameRequested = true;
        window.requestAnimationFrame(() => {
            updateNavigationFromScroll();
            scrollFrameRequested = false;
        });
    }, { passive: true });

    window.addEventListener('hashchange', () => {
        const sectionId = window.location.hash.substring(1);
        if (['overview', 'holdings', 'market'].includes(sectionId)) {
            setActiveNavigation(sectionId);
        }
    });

    const initialSection = window.location.hash.substring(1);
    if (['overview', 'holdings', 'market'].includes(initialSection)) {
        setActiveNavigation(initialSection);
    } else {
        updateNavigationFromScroll();
    }
}

function bindGeneralInteractions() {
    document.getElementById('openAddDialog').addEventListener('click', openAddDialog);
    document.getElementById('emptyAddButton').addEventListener('click', openAddDialog);
    document.getElementById('holdingSearch').addEventListener('input', renderTable);
    document.getElementById('assetFilter').addEventListener('change', renderTable);

    document.querySelectorAll('[data-close-dialog]').forEach(button => {
        button.addEventListener('click', () => closeDialog(button.dataset.closeDialog));
    });

    document.querySelectorAll('.app-dialog').forEach(dialog => {
        dialog.addEventListener('click', event => {
            const rect = dialog.getBoundingClientRect();
            const clickedBackdrop = event.clientX < rect.left
                || event.clientX > rect.right
                || event.clientY < rect.top
                || event.clientY > rect.bottom;
            if (clickedBackdrop) {
                closeDialog(dialog.id);
            }
        });
    });
}

async function initialize() {
    initTickerSelect('ticker', 'TSLA');
    initTickerSelect('snapshotTicker', 'TSLA');
    setPageContext('Investor');
    resetPositionForm();
    bindGeneralInteractions();
    bindPositionForm();
    bindRemoveDialog();
    bindSnapshotControls();
    bindSectionNavigation();

    if ('ResizeObserver' in window) {
        const metricObserver = new ResizeObserver(() => fitHeroMetricValues());
        metricObserver.observe(document.querySelector('.hero-metrics'));
    }
    if (document.fonts && document.fonts.ready) {
        document.fonts.ready.then(fitHeroMetricValues);
    }

    try {
        await loadDashboard();
    } catch (error) {
        removeSkeletons();
        document.getElementById('portfolioTableBody').innerHTML = `
            <tr class="loading-row">
                <td colspan="7"><span class="table-loading">We could not load your portfolio.</span></td>
            </tr>
        `;
        document.getElementById('lastUpdated').innerHTML = '<i class="bi bi-exclamation-circle" aria-hidden="true"></i> Update failed';
        showToast(error.message || 'The portfolio could not be loaded.', 'error', 5000);
    }

    loadSnapshot('TSLA', { silent: true, resetView: true });
}

initialize();
