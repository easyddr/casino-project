const canvas = document.getElementById('chart');
const ctx = canvas.getContext('2d');
const buyBtn = document.getElementById('buy');
const sellBtn = document.getElementById('sell');
const POLL_MS = 300;

let betId = null;
let prices = [];
let timer = null;
let crashed = false;

function draw() {
    const w = canvas.width, h = canvas.height, pad = 30;
    ctx.clearRect(0, 0, w, h);
    if (prices.length === 0) return;

    const max = Math.max(1.2, ...prices) * 1.05;
    const min = Math.min(0.8, ...prices) * 0.95;
    const n = Math.max(prices.length, 60);
    const x = i => pad + (w - 2 * pad) * i / (n - 1);
    const y = p => h - pad - (h - 2 * pad) * (p - min) / (max - min);

    ctx.strokeStyle = '#353949';
    ctx.setLineDash([4, 4]);
    ctx.beginPath(); ctx.moveTo(pad, y(1)); ctx.lineTo(w - pad, y(1)); ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = '#8b8ea0';
    ctx.fillText('1.00', 2, y(1) + 4);

    const last = prices[prices.length - 1];
    ctx.strokeStyle = crashed ? '#e5484d' : (last >= 1 ? '#6ee7a0' : '#ff8589');
    ctx.lineWidth = 2;
    ctx.beginPath();
    prices.forEach((p, i) => i === 0 ? ctx.moveTo(x(i), y(p)) : ctx.lineTo(x(i), y(p)));
    if (crashed) ctx.lineTo(x(prices.length), h - pad);
    ctx.stroke();
}

function show(state) {
    if (state.prices && state.prices.length) {
        prices = prices.slice(0, state.from).concat(state.prices);
        document.getElementById('price').textContent = state.currentPrice.toFixed(4);
        document.getElementById('value').textContent = Number(state.currentValue).toFixed(2);
    }
    if (state.finished) finish(state);
    draw();
}

function finish(state) {
    clearInterval(timer);
    timer = null;
    betId = null;
    crashed = state.crashed;
    buyBtn.disabled = false;
    sellBtn.disabled = true;
    Casino.setBalance(state.balance);
    if (state.crashed) {
        Casino.message('Обвал! Монета знецінилась, ставка згоріла', 'loss');
        document.getElementById('value').textContent = '0.00';
    } else if (state.status === 'WON') {
        Casino.message('Продано за ' + state.payout + '. Прибуток!', 'win');
    } else {
        Casino.message('Продано за ' + state.payout + '. Збиток', 'loss');
    }
}

async function poll() {
    if (!betId) return;
    try {
        show(await Casino.api('/api/play/crypto/rounds/' + betId + '?from=' + prices.length));
    } catch (e) {
        Casino.message(e.message, 'loss');
    }
}

buyBtn.addEventListener('click', async () => {
    const amount = Number(document.getElementById('amount').value);
    buyBtn.disabled = true;
    try {
        const state = await Casino.api('/api/play/crypto/' + Casino.gameId() + '/buy', {amount});
        betId = state.betId;
        prices = [];
        crashed = false;
        Casino.setBalance(state.balance);
        Casino.message('Позицію відкрито. Не проґавте момент!');
        sellBtn.disabled = false;
        show(state);
        timer = setInterval(poll, POLL_MS);
    } catch (e) {
        buyBtn.disabled = false;
        Casino.message(e.message, 'loss');
    }
});

sellBtn.addEventListener('click', async () => {
    if (!betId) return;
    sellBtn.disabled = true;
    try {
        show(await Casino.api('/api/play/crypto/rounds/' + betId + '/sell', {}));
    } catch (e) {
        Casino.message(e.message, 'loss');
    }
});

draw();
