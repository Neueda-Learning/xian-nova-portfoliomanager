const portfolioApi = (() => {
    async function request(path, options = {}) {
        const headers = {
            Accept: 'application/json',
            ...(options.body ? { 'Content-Type': 'application/json' } : {}),
            ...(options.headers || {})
        };

        const response = await fetch(path, { ...options, headers });

        if (response.redirected && response.url.includes('/login')) {
            window.location.assign('/login.html');
            throw new Error('Your session has expired. Please sign in again.');
        }

        if (!response.ok) {
            const contentType = response.headers.get('content-type') || '';
            const errorBody = contentType.includes('application/json')
                ? await response.json().catch(() => ({}))
                : {};
            throw new Error(errorBody.message || `Request failed (${response.status})`);
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            throw new Error('The server returned an unexpected response.');
        }

        return response.json();
    }

    return {
        fetchCurrentUser() {
            return request('/api/auth/me');
        },

        fetchItems() {
            return request('/api/portfolio/items');
        },

        fetchSummary() {
            return request('/api/portfolio/summary');
        },

        addItem(payload) {
            return request('/api/portfolio/items', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
        },

        updateItem(id, payload) {
            return request(`/api/portfolio/items/${encodeURIComponent(id)}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
        },

        deleteItem(id) {
            return request(`/api/portfolio/items/${encodeURIComponent(id)}`, {
                method: 'DELETE'
            });
        },

        fetchLiveSnapshot(ticker, signal) {
            return request(`/api/price-snapshots/${encodeURIComponent(ticker)}/live`, { signal });
        },

        fetchForecast(ticker, window = 30, model = 'WMA', signal) {
            const params = new URLSearchParams({
                model,
                window: String(window)
            });
            return request(`/api/price-snapshots/${encodeURIComponent(ticker)}/forecast?${params.toString()}`, { signal });
        }
    };
})();
