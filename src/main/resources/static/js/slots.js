const SYMBOLS = {CHERRY: '🍒', LEMON: '🍋', BELL: '🔔', CLOVER: '🍀', DIAMOND: '💎', SEVEN: '7️⃣'};
const reels = document.querySelectorAll('.reel');
const spinBtn = document.getElementById('spin');

document.querySelectorAll('.sym').forEach(el => el.textContent = SYMBOLS[el.dataset.symbol] || el.textContent);

spinBtn.addEventListener('click', async () => {
    const amount = Number(document.getElementById('amount').value);
    spinBtn.disabled = true;
    Casino.message('Крутимо...');
    document.querySelector('.reels').classList.add('spinning');
    const keys = Object.keys(SYMBOLS);
    const shuffle = setInterval(() => reels.forEach(r => r.textContent = SYMBOLS[keys[Math.floor(Math.random() * keys.length)]]), 80);

    try {
        const [result] = await Promise.all([
            Casino.api('/api/play/slots/' + Casino.gameId(), {amount}),
            new Promise(r => setTimeout(r, 900))
        ]);
        clearInterval(shuffle);
        result.details.reels.forEach((s, i) => reels[i].textContent = SYMBOLS[s]);
        Casino.setBalance(result.balance);
        if (Number(result.payout) > 0) {
            Casino.message('Виграш ' + result.payout + ' (x' + result.details.multiplier + ')', 'win');
        } else {
            Casino.message('Не пощастило, спробуйте ще', 'loss');
        }
    } catch (e) {
        clearInterval(shuffle);
        Casino.message(e.message, 'loss');
    } finally {
        document.querySelector('.reels').classList.remove('spinning');
        spinBtn.disabled = false;
    }
});
