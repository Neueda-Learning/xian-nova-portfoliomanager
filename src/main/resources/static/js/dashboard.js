let allocationChart;
let editingItemId = null;
const SUPPORTED_TICKERS = ['C', 'AMZN', 'TSLA', 'FB', 'AAPL'];

let snapshotChart;
let currentSnapshot = null;
let selectedSnapshotWindow = 30;

function money(value) {
    return `$${Number(value).toFixed(2)}`;
}

function toDateInputValue(value) {
    if (!value) {
        return '';
    }
    return String(value).substring(0, 10);
}

function resetFormState() {
    editingItemId = null;
    const form = document.getElementById('addItemForm');
    const submitButton = form.querySelector('button[type="submit"]');
    submitButton.textContent = 'Add';
}

function percent(value) {
    return `${Number(value).toFixed(2)}%`;
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
        option.textContent = `${ticker} (existing)`;
        select.appendChild(option);
    }
}

function renderTable(items) {
    const body = document.getElementById('portfolioTableBody');
    body.innerHTML = '';

    items.forEach(item => {
        const tr = document.createElement('tr');
        const pnlClass = Number(item.profitLoss) >= 0 ? 'profit' : 'loss';
        tr.innerHTML = `
            <td>${item.ticker}</td>
            <td>${item.assetType}</td>
            <td>${item.quantity}</td>
            <td>${money(item.buyPrice)}</td>
            <td>${item.purchaseDate}</td>
            <td>${money(item.currentPrice)}</td>
            <td>${money(item.currentValue)}</td>
            <td class="${pnlClass}">${money(item.profitLoss)}</td>
            <td class="d-flex gap-2">
                <button class="btn btn-sm btn-outline-secondary" data-action="edit">Edit</button>
                <button class="btn btn-sm btn-outline-danger" data-action="remove">Remove</button>
            </td>
        `;
        tr.querySelector('[data-action="remove"]').addEventListener('click', async () => {
            const confirmed = window.confirm(`Are you sure you want to remove ${item.ticker}?`);
            if (!confirmed) {
                return;
            }
            await portfolioApi.deleteItem(item.id);
            if (editingItemId === item.id) {
                document.getElementById('addItemForm').reset();
                resetFormState();
            }
            await loadDashboard();
        });
        tr.querySelector('[data-action="edit"]').addEventListener('click', () => {
            editingItemId = item.id;
            ensureTickerOption('ticker', item.ticker);
            document.getElementById('ticker').value = item.ticker;
            document.getElementById('assetType').value = item.assetType;
            document.getElementById('quantity').value = item.quantity;
            document.getElementById('buyPrice').value = item.buyPrice;
            document.getElementById('purchaseDate').value = toDateInputValue(item.purchaseDate);
            const submitButton = document.querySelector('#addItemForm button[type="submit"]');
            submitButton.textContent = 'Update';
        });
        body.appendChild(tr);
    });
}

function renderSummary(summary) {
    document.getElementById('totalCost').textContent = money(summary.totalCost);
    document.getElementById('totalValue').textContent = money(summary.totalMarketValue);
    const pnlNode = document.getElementById('totalPnl');
    pnlNode.textContent = money(summary.totalProfitLoss);
    pnlNode.className = Number(summary.totalProfitLoss) >= 0 ? 'profit' : 'loss';
}

function renderChart(summary) {
    const labels = Object.keys(summary.allocationPercentages);
    const values = Object.values(summary.allocationPercentages).map(Number);
    const ctx = document.getElementById('allocationChart');

    if (allocationChart) {
        allocationChart.destroy();
    }

    allocationChart = new Chart(ctx, {
        type: 'pie',
        data: {
            labels,
            datasets: [{
                data: values,
                radius: '72%'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            layout: {
                padding: 8
            },
            plugins: {
                legend: {
                    position: 'bottom'
                },
                tooltip: {
                    callbacks: {
                        label: context => `${context.label}: ${context.parsed.toFixed(2)}%`
                    }
                }
            }
        }
    });
}

function bindForm() {
    const form = document.getElementById('addItemForm');
    const message = document.getElementById('formMessage');

    form.addEventListener('submit', async event => {
        event.preventDefault();

        const payload = {
            ticker: document.getElementById('ticker').value,
            assetType: document.getElementById('assetType').value,
            quantity: Number(document.getElementById('quantity').value),
            buyPrice: Number(document.getElementById('buyPrice').value),
            purchaseDate: document.getElementById('purchaseDate').value
        };

        const actionName = editingItemId == null ? 'add' : 'update';
        const confirmed = window.confirm(`Are you sure you want to ${actionName} this portfolio item?`);
        if (!confirmed) {
            message.className = 'text-muted';
            message.textContent = 'Action cancelled.';
            return;
        }

        try {
            if (editingItemId == null) {
                await portfolioApi.addItem(payload);
                message.textContent = 'Item added.';
            } else {
                await portfolioApi.updateItem(editingItemId, payload);
                message.textContent = 'Item updated.';
            }
            message.className = 'success';
            form.reset();
            resetFormState();
            await loadDashboard();
        } catch (error) {
            message.className = 'error';
            message.textContent = error.message;
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
        const close = Array.isArray(priceData.close) ? priceData.close.map(Number).filter(Number.isFinite) : [];
        const volume = Array.isArray(priceData.volume) ? priceData.volume.map(Number).filter(Number.isFinite) : [];
        const open = Array.isArray(priceData.open) ? priceData.open.map(Number).filter(Number.isFinite) : [];
        const high = Array.isArray(priceData.high) ? priceData.high.map(Number).filter(Number.isFinite) : [];
        const low = Array.isArray(priceData.low) ? priceData.low.map(Number).filter(Number.isFinite) : [];
        if (close.length === 0) {
            return null;
        }
        return { close, volume, open, high, low };
    } catch (error) {
        return null;
    }
}

function getStandardDeviation(values) {
    if (!values || values.length < 2) {
        return 0;
    }
    const mean = values.reduce((sum, value) => sum + value, 0) / values.length;
    const variance = values.reduce((sum, value) => sum + Math.pow(value - mean, 2), 0) / (values.length - 1);
    return Math.sqrt(variance);
}

function setSnapshotMetric(id, text, cssClass) {
    const node = document.getElementById(id);
    node.textContent = text;
    node.classList.remove('profit', 'loss');
    if (cssClass) {
        node.classList.add(cssClass);
    }
}

function renderSnapshotChart(series) {
    const ctx = document.getElementById('snapshotChart');
    if (snapshotChart) {
        snapshotChart.destroy();
    }

    const windowedClose = series.close.slice(-selectedSnapshotWindow);
    const labels = windowedClose.map((_, index) => `T-${windowedClose.length - index}`);
    snapshotChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [{
                label: 'Close Price',
                data: windowedClose,
                borderColor: '#0d6efd',
                backgroundColor: 'rgba(13, 110, 253, 0.15)',
                tension: 0.25,
                pointRadius: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: true
                }
            },
            scales: {
                x: {
                    ticks: {
                        maxTicksLimit: 8
                    }
                }
            }
        }
    });
}

function renderSnapshot(snapshot) {
    currentSnapshot = snapshot;
    document.getElementById('snapshotTickerValue').textContent = snapshot.ticker;
    document.getElementById('snapshotPriceValue').textContent = money(snapshot.latestPrice);
    document.getElementById('snapshotFetchedAtValue').textContent = snapshot.fetchedAt || '-';

    const series = extractPriceSeries(snapshot.rawPayload);
    if (!series) {
        setSnapshotMetric('snapshotPrevClose', '-');
        setSnapshotMetric('snapshotDayChange', '-');
        setSnapshotMetric('snapshotPeriodHigh', '-');
        setSnapshotMetric('snapshotPeriodLow', '-');
        setSnapshotMetric('snapshotAvgClose', '-');
        setSnapshotMetric('snapshotPeriodReturn', '-');
        setSnapshotMetric('snapshotVolatility', '-');
        setSnapshotMetric('snapshotAvgVolume', '-');
        setSnapshotMetric('snapshotLatestVolume', '-');
        if (snapshotChart) {
            snapshotChart.destroy();
            snapshotChart = null;
        }
        return;
    }

    const close = series.close.slice(-selectedSnapshotWindow);
    if (close.length === 0) {
        return;
    }
    const latest = close[close.length - 1];
    const prev = close.length > 1 ? close[close.length - 2] : latest;
    const first = close[0];
    const dayChange = latest - prev;
    const dayChangePct = prev !== 0 ? (dayChange / prev) * 100 : 0;
    const periodReturn = first !== 0 ? ((latest - first) / first) * 100 : 0;
    const high = Math.max(...close);
    const low = Math.min(...close);
    const avgClose = close.reduce((sum, value) => sum + value, 0) / close.length;

    const returns = [];
    for (let i = 1; i < close.length; i++) {
        if (close[i - 1] !== 0) {
            returns.push((close[i] - close[i - 1]) / close[i - 1]);
        }
    }
    const volatilityPct = getStandardDeviation(returns) * 100;
    const volume = series.volume.slice(-selectedSnapshotWindow);
    const avgVolume = volume.length > 0 ? volume.reduce((sum, value) => sum + value, 0) / volume.length : 0;
    const latestVolume = volume.length > 0 ? volume[volume.length - 1] : 0;

    setSnapshotMetric('snapshotPrevClose', money(prev));
    setSnapshotMetric(
        'snapshotDayChange',
        `${money(dayChange)} (${percent(dayChangePct)})`,
        dayChange >= 0 ? 'profit' : 'loss'
    );
    setSnapshotMetric('snapshotPeriodHigh', money(high));
    setSnapshotMetric('snapshotPeriodLow', money(low));
    setSnapshotMetric('snapshotAvgClose', money(avgClose));
    setSnapshotMetric('snapshotPeriodReturn', percent(periodReturn), periodReturn >= 0 ? 'profit' : 'loss');
    setSnapshotMetric('snapshotVolatility', percent(volatilityPct));
    setSnapshotMetric('snapshotAvgVolume', avgVolume > 0 ? avgVolume.toFixed(0) : '-');
    setSnapshotMetric('snapshotLatestVolume', latestVolume > 0 ? latestVolume.toFixed(0) : '-');

    renderSnapshotChart(series);
}

function bindSnapshotWindowSelector() {
    const buttons = document.querySelectorAll('[data-window]');
    buttons.forEach(button => {
        button.addEventListener('click', () => {
            selectedSnapshotWindow = Number(button.dataset.window);
            buttons.forEach(node => node.classList.remove('active'));
            button.classList.add('active');
            if (currentSnapshot) {
                renderSnapshot(currentSnapshot);
            }
        });
    });
}

function bindSnapshotForm() {
    const form = document.getElementById('snapshotForm');
    const message = document.getElementById('snapshotMessage');

    form.addEventListener('submit', async event => {
        event.preventDefault();
        const ticker = document.getElementById('snapshotTicker').value.trim();
        if (!ticker) {
            message.className = 'error';
            message.textContent = 'Ticker is required.';
            return;
        }

        try {
            const snapshot = await portfolioApi.fetchLiveSnapshot(ticker);
            renderSnapshot(snapshot);
            message.className = 'success';
            message.textContent = `Live snapshot refreshed for ${snapshot.ticker}.`;
        } catch (error) {
            message.className = 'error';
            message.textContent = error.message;
        }
    });
}


async function loadDashboard() {
    const [user, items, summary] = await Promise.all([
        portfolioApi.fetchCurrentUser(),
        portfolioApi.fetchItems(),
        portfolioApi.fetchSummary()
    ]);

    document.getElementById('currentUser').textContent = user.username;
    renderTable(items);
    renderSummary(summary);
    renderChart(summary);
}

function initSupportedTickerDropdowns() {
    initTickerSelect('ticker', 'TSLA');
    initTickerSelect('snapshotTicker', 'TSLA');
}


initSupportedTickerDropdowns();
bindForm();
bindSnapshotForm();
bindSnapshotWindowSelector();
loadDashboard().catch(error => {
    const message = document.getElementById('formMessage');
    message.className = 'error';
    message.textContent = `Failed to load dashboard: ${error.message}`;
});

document.getElementById('snapshotForm').dispatchEvent(new Event('submit'));