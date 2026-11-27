// Спільні хелпери для сторінок ігор: запит до API з CSRF-заголовком і оновлення балансу в шапці.
const Casino = {
    async api(url, body, method) {
        const headers = {'Content-Type': 'application/json', 'Accept': 'application/json'};
        const token = document.querySelector('meta[name="_csrf"]');
        const header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) headers[header.content] = token.content;

        const res = await fetch(url, {
            method: method || (body === undefined ? 'GET' : 'POST'),
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
            credentials: 'same-origin'
        });
        if (res.status === 401) {
            window.location.href = '/login';
            throw new Error('Потрібно увійти');
        }
        const data = await res.json().catch(() => ({}));
        if (!res.ok) throw new Error(data.detail || 'Помилка ' + res.status);
        return data;
    },

    setBalance(value) {
        const el = document.getElementById('balance');
        if (el && value !== null && value !== undefined) el.textContent = Number(value).toFixed(2);
    },

    message(text, cls) {
        const el = document.getElementById('message');
        el.textContent = text;
        el.className = 'message ' + (cls || '');
    },

    gameId() {
        return document.getElementById('game').dataset.gameId;
    }
};
