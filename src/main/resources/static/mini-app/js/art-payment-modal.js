/*
 * Reusable ART payment modal for Telegram Mini App pages.
 *
 * Requirements on the host page:
 * - Telegram auth headers are provided by getTelegramInitData() or options.getTelegramInitData.
 * - TON Connect UI is available as window.tonConnectUI for TON payments.
 */
(function () {
    const DEFAULTS = {
        packagesUrl: '/api/stars/packages',
        starsInvoiceUrl: '/api/stars/create-invoice',
        tonCreateUrl: '/api/ton-payments/create',
        tonStatusUrl: '/api/ton-payments',
        pollIntervalMs: 1500,
        maxPollAttempts: 80
    };

    class ArtPaymentModal {
        constructor(options) {
            this.options = Object.assign({}, DEFAULTS, options || {});
            this.packages = [];
            this.selectedPackage = null;
            this.method = 'stars';
            this.root = null;
            this.stateEl = null;
        }

        async open() {
            await this.ensureRoot();
            this.root.classList.remove('hidden');
            await this.loadPackages();
            this.render();
        }

        close() {
            if (this.root) {
                this.root.classList.add('hidden');
            }
        }

        async ensureRoot() {
            if (this.root) return;
            this.root = document.createElement('div');
            this.root.className = 'hidden fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4';
            this.root.innerHTML = `
                <div class="w-full max-w-lg rounded-2xl bg-white p-5 shadow-xl dark:bg-slate-900 dark:text-slate-100">
                    <div class="mb-4 flex items-center justify-between">
                        <div>
                            <h2 class="text-lg font-semibold">Пополнить ART</h2>
                            <p class="text-sm text-slate-500 dark:text-slate-400">Выберите пакет и способ оплаты</p>
                        </div>
                        <button type="button" data-close class="text-2xl leading-none text-slate-400 hover:text-slate-700">&times;</button>
                    </div>
                    <div class="mb-4 grid grid-cols-2 gap-2 rounded-xl bg-slate-100 p-1 dark:bg-slate-800">
                        <button type="button" data-method="stars" class="rounded-lg px-3 py-2 text-sm font-medium">Stars</button>
                        <button type="button" data-method="ton" class="rounded-lg px-3 py-2 text-sm font-medium">TON</button>
                    </div>
                    <div data-packages class="space-y-2"></div>
                    <div data-state class="mt-4 text-sm text-slate-500 dark:text-slate-400"></div>
                    <button type="button" data-pay class="mt-4 w-full rounded-xl bg-blue-600 px-4 py-3 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60">
                        Оплатить
                    </button>
                </div>
            `;
            document.body.appendChild(this.root);
            this.stateEl = this.root.querySelector('[data-state]');
            this.root.querySelector('[data-close]').addEventListener('click', () => this.close());
            this.root.querySelectorAll('[data-method]').forEach((button) => {
                button.addEventListener('click', () => {
                    this.method = button.dataset.method;
                    this.render();
                });
            });
            this.root.querySelector('[data-pay]').addEventListener('click', () => this.pay());
        }

        async loadPackages() {
            const response = await fetch(this.options.packagesUrl);
            if (!response.ok) throw new Error('Не удалось загрузить пакеты ART');
            this.packages = await response.json();
            this.selectedPackage = this.packages[0] || null;
        }

        render() {
            this.root.querySelectorAll('[data-method]').forEach((button) => {
                const active = button.dataset.method === this.method;
                button.className = active
                    ? 'rounded-lg bg-white px-3 py-2 text-sm font-medium shadow dark:bg-slate-700'
                    : 'rounded-lg px-3 py-2 text-sm font-medium text-slate-500';
            });

            const packagesEl = this.root.querySelector('[data-packages]');
            packagesEl.innerHTML = this.packages.map((pkg) => {
                const selected = this.selectedPackage && this.selectedPackage.code === pkg.code;
                const tonDisabled = this.method === 'ton' && !pkg.tonPriceNano;
                return `
                    <button type="button" data-package="${escapeHtml(pkg.code)}"
                        ${tonDisabled ? 'disabled' : ''}
                        class="w-full rounded-xl border p-3 text-left ${selected ? 'border-blue-500 bg-blue-50 dark:bg-blue-950/40' : 'border-slate-200 dark:border-slate-700'} ${tonDisabled ? 'opacity-50' : ''}">
                        <div class="flex items-center justify-between gap-3">
                            <div>
                                <div class="font-semibold">${escapeHtml(pkg.name)}</div>
                                <div class="text-sm text-slate-500">${pkg.artAmount} ART</div>
                            </div>
                            <div class="text-sm font-semibold">${this.priceLabel(pkg)}</div>
                        </div>
                    </button>
                `;
            }).join('');

            packagesEl.querySelectorAll('[data-package]').forEach((button) => {
                button.addEventListener('click', () => {
                    this.selectedPackage = this.packages.find((pkg) => pkg.code === button.dataset.package);
                    this.render();
                });
            });

            this.root.querySelector('[data-pay]').disabled = !this.selectedPackage
                || (this.method === 'ton' && !this.selectedPackage.tonPriceNano);
        }

        priceLabel(pkg) {
            if (this.method === 'stars') {
                return `${pkg.starsPrice} ⭐`;
            }
            return pkg.tonPriceNano ? `${formatTon(pkg.tonPriceNano)} TON` : 'TON недоступен';
        }

        async pay() {
            if (!this.selectedPackage) return;
            if (this.method === 'stars') {
                return this.payStars();
            }
            return this.payTon();
        }

        async payStars() {
            this.setState('Создаём invoice Telegram Stars...');
            const response = await fetch(this.options.starsInvoiceUrl, {
                method: 'POST',
                headers: this.authHeaders(),
                body: JSON.stringify({ packageCode: this.selectedPackage.code })
            });
            if (!response.ok) throw new Error('Не удалось создать Stars invoice');
            const data = await response.json();
            window.location.href = data.invoiceUrl;
        }

        async payTon() {
            if (!window.tonConnectUI || !window.tonConnectUI.wallet) {
                throw new Error('Подключите TON Connect перед оплатой');
            }
            const senderAddress = window.tonConnectUI.wallet.account.address;
            this.setState('Готовим TON Pay транзакцию...');
            const response = await fetch(this.options.tonCreateUrl, {
                method: 'POST',
                headers: this.authHeaders(),
                body: JSON.stringify({ packageCode: this.selectedPackage.code, senderAddress })
            });
            if (!response.ok) throw new Error('Не удалось создать TON Pay платеж');
            const payment = await response.json();

            this.setState('Подтвердите транзакцию в кошельке...');
            await window.tonConnectUI.sendTransaction({
                messages: [payment.message],
                validUntil: Math.floor(Date.now() / 1000) + 300,
                from: senderAddress
            });

            this.setState('Ждём подтверждение в блокчейне...');
            const status = await this.waitForTonStatus(payment.intentId);
            if (status.status !== 'COMPLETED') {
                throw new Error(status.failureReason || 'TON платеж не завершён');
            }
            this.setState(`Готово: начислено ${status.artAmount} ART`);
            this.root.dispatchEvent(new CustomEvent('art-payment-completed', { detail: status }));
        }

        async waitForTonStatus(intentId) {
            for (let attempt = 0; attempt < this.options.maxPollAttempts; attempt += 1) {
                const response = await fetch(`${this.options.tonStatusUrl}/${intentId}`, {
                    headers: this.authHeaders(false)
                });
                if (response.ok) {
                    const status = await response.json();
                    if (status.status === 'COMPLETED' || status.status === 'FAILED' || status.status === 'EXPIRED') {
                        return status;
                    }
                }
                await new Promise((resolve) => setTimeout(resolve, this.options.pollIntervalMs));
            }
            throw new Error('Истекло время ожидания TON подтверждения');
        }

        authHeaders(withJson) {
            const headers = {};
            if (withJson !== false) {
                headers['Content-Type'] = 'application/json';
            }
            const initData = this.options.getTelegramInitData
                ? this.options.getTelegramInitData()
                : (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.initData);
            if (initData) {
                headers['X-Telegram-Init-Data'] = initData;
            }
            return headers;
        }

        setState(text) {
            if (this.stateEl) {
                this.stateEl.textContent = text || '';
            }
        }
    }

    function formatTon(nano) {
        const whole = Math.floor(nano / 1000000000);
        const fraction = String(nano % 1000000000).padStart(9, '0').replace(/0+$/, '');
        return fraction ? `${whole}.${fraction}` : String(whole);
    }

    function escapeHtml(value) {
        return String(value || '').replace(/[&<>"']/g, (ch) => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        }[ch]));
    }

    window.ArtPaymentModal = ArtPaymentModal;
})();
