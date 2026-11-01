/**
 * Cliente HTTP mínimo para la API REST de PTrinity.
 */
const Api = (() => {
    class ApiError extends Error {
        constructor(status, body) {
            super(body?.message || `Error ${status}`);
            this.status = status;
            this.fieldErrors = body?.fieldErrors || [];
        }
    }

    async function request(method, path, body) {
        const response = await fetch(`api${path}`, {
            method,
            headers: body ? {'Content-Type': 'application/json'} : {},
            body: body ? JSON.stringify(body) : undefined,
        });
        if (response.status === 204) return null;
        const data = await response.json().catch(() => null);
        if (!response.ok) throw new ApiError(response.status, data);
        return data;
    }

    return {
        ApiError,
        customers: {
            list: () => request('GET', '/customers'),
            create: (data) => request('POST', '/customers', data),
            update: (id, data) => request('PUT', `/customers/${id}`, data),
            remove: (id) => request('DELETE', `/customers/${id}`),
        },
        accounts: {
            list: (customerId) => request('GET', `/accounts${customerId ? `?customerId=${customerId}` : ''}`),
            open: (data) => request('POST', '/accounts', data),
            changeStatus: (id, status) => request('PATCH', `/accounts/${id}/status`, {status}),
            statement: (id) => request('GET', `/accounts/${id}/statement`),
        },
        transactions: {
            list: (accountId) => request('GET', `/transactions${accountId ? `?accountId=${accountId}` : ''}`),
            deposit: (data) => request('POST', '/transactions/deposits', data),
            withdraw: (data) => request('POST', '/transactions/withdrawals', data),
            transfer: (data) => request('POST', '/transactions/transfers', data),
        },
    };
})();
