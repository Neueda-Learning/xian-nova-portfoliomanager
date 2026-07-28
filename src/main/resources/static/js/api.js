const portfolioApi = {
    async fetchCurrentUser() {
        const response = await fetch('/api/auth/me');
        if (!response.ok) {
            throw new Error('Failed to fetch user');
        }
        return response.json();
    },

    async fetchItems() {
        const response = await fetch('/api/portfolio/items');
        if (!response.ok) {
            throw new Error('Failed to fetch portfolio items');
        }
        return response.json();
    },

    async fetchSummary() {
        const response = await fetch('/api/portfolio/summary');
        if (!response.ok) {
            throw new Error('Failed to fetch portfolio summary');
        }
        return response.json();
    },

    async addItem(payload) {
        const response = await fetch('/api/portfolio/items', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const errorBody = await response.json().catch(() => ({}));
            throw new Error(errorBody.message || 'Failed to add item');
        }
        return response.json();
    },

    async updateItem(id, payload) {
        const response = await fetch(`/api/portfolio/items/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const errorBody = await response.json().catch(() => ({}));
            throw new Error(errorBody.message || 'Failed to update item');
        }
        return response.json();
    },

    async deleteItem(id) {
        const response = await fetch(`/api/portfolio/items/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            throw new Error('Failed to delete item');
        }
    },

    async fetchLiveSnapshot(ticker) {
        const response = await fetch(`/api/price-snapshots/${encodeURIComponent(ticker)}/live`);
        if (!response.ok) {
            const errorBody = await response.json().catch(() => ({}));
            throw new Error(errorBody.message || 'Failed to fetch live snapshot');
        }
        return response.json();
    }

};

