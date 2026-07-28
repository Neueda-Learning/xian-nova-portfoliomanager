let allocationChart;
let editingItemId = null;
const SUPPORTED_TICKERS = ['C', 'AMZN', 'TSLA', 'FB', 'AAPL'];

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

initSupportedTickerDropdowns();
bindForm();
loadDashboard().catch(error => {
    const message = document.getElementById('formMessage');
    message.className = 'error';
    message.textContent = `Failed to load dashboard: ${error.message}`;
});