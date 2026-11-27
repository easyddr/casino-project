const chance = document.getElementById('chance');
const rollBtn = document.getElementById('rollBtn');
const rollEl = document.getElementById('roll');

// Та сама формула, що і на сервері (DiceGame.multiplierFor): 96 / шанс
function updateHint() {
    const c = Number(chance.value);
    document.getElementById('chanceLabel').textContent = c;
    document.getElementById('multiplier').textContent = 'x' + (Math.floor(96 / c * 10000) / 10000);
    document.getElementById('target').textContent = c.toFixed(2);
}
chance.addEventListener('input', updateHint);
updateHint();

rollBtn.addEventListener('click', async () => {
    const amount = Number(document.getElementById('amount').value);
    rollBtn.disabled = true;
    const anim = setInterval(() => rollEl.textContent = (Math.random() * 100).toFixed(2), 50);
    try {
        const [result] = await Promise.all([
            Casino.api('/api/play/dice/' + Casino.gameId(), {amount, chance: Number(chance.value)}),
            new Promise(r => setTimeout(r, 600))
        ]);
        clearInterval(anim);
        rollEl.textContent = Number(result.details.roll).toFixed(2);
        Casino.setBalance(result.balance);
        if (result.details.win) {
            rollEl.className = 'big-number win';
            Casino.message('Виграш ' + result.payout, 'win');
        } else {
            rollEl.className = 'big-number loss';
            Casino.message('Програш', 'loss');
        }
    } catch (e) {
        clearInterval(anim);
        Casino.message(e.message, 'loss');
    } finally {
        rollBtn.disabled = false;
    }
});
