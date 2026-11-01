/**
 * Interfaz web de PTrinity: administración de clientes, cuentas y movimientos.
 */
(() => {
    const state = {customers: [], accounts: [], transactions: [], txMode: 'deposit'};

    const LABELS = {
        accountType: {SAVINGS: 'Ahorros', CHECKING: 'Corriente'},
        status: {ACTIVE: 'Activa', INACTIVE: 'Inactiva', CANCELLED: 'Cancelada'},
        txType: {DEPOSIT: 'Consignación', WITHDRAWAL: 'Retiro', TRANSFER: 'Transferencia'},
        idType: {CC: 'Cédula de ciudadanía', CE: 'Cédula de extranjería', PA: 'Pasaporte', PPT: 'Permiso por protección temporal'},
    };

    const VIEWS = {
        dashboard: {title: 'Resumen', subtitle: 'Estado general de la entidad'},
        customers: {title: 'Clientes', subtitle: 'Registro, actualización y eliminación de clientes', action: ['Nuevo cliente', () => openCustomerForm()]},
        accounts: {title: 'Cuentas', subtitle: 'Productos financieros: cuentas de ahorros y corrientes', action: ['Abrir cuenta', () => openAccountForm()]},
        transactions: {title: 'Movimientos', subtitle: 'Consignaciones, retiros y transferencias entre cuentas'},
    };

    // ---------- Utilidades ----------
    const $ = (selector, root = document) => root.querySelector(selector);
    const money = new Intl.NumberFormat('es-CO', {style: 'currency', currency: 'COP', minimumFractionDigits: 2});
    const fmtMoney = (value) => money.format(Number(value || 0));
    const fmtDate = (value) => value ? new Date(value).toLocaleDateString('es-CO', {year: 'numeric', month: 'short', day: '2-digit'}) : '—';
    const fmtDateTime = (value) => value ? new Date(value).toLocaleString('es-CO', {dateStyle: 'medium', timeStyle: 'short'}) : '—';
    const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
    const customerName = (id) => {
        const c = state.customers.find((x) => x.id === id);
        return c ? c.fullName : `Cliente #${id}`;
    };
    const emptyRow = (cols, text) => `<tr><td colspan="${cols}" class="empty">${text}</td></tr>`;

    function toast(message, type = 'success') {
        const el = document.createElement('div');
        el.className = `toast ${type}`;
        el.textContent = message;
        $('#toasts').appendChild(el);
        setTimeout(() => el.remove(), 4500);
    }

    function handleError(error, form) {
        if (form) showFieldErrors(form, error.fieldErrors || []);
        toast(error.message || 'Ocurrió un error inesperado', 'error');
    }

    function showFieldErrors(form, errors) {
        form.querySelectorAll('.field-error').forEach((e) => e.remove());
        form.querySelectorAll('.invalid').forEach((e) => e.classList.remove('invalid'));
        errors.forEach(({field, message}) => {
            const input = form.elements[field];
            if (!input) return;
            input.classList.add('invalid');
            const msg = document.createElement('span');
            msg.className = 'field-error';
            msg.textContent = message;
            input.closest('label').appendChild(msg);
        });
    }

    // ---------- Modal ----------
    function openModal(title, html, wide = false) {
        $('#modal-title').textContent = title;
        $('#modal-body').innerHTML = html;
        $('.modal-dialog').classList.toggle('wide', wide);
        $('#modal').hidden = false;
        const first = $('#modal-body input, #modal-body select');
        if (first) first.focus();
    }

    function closeModal() {
        $('#modal').hidden = true;
        $('#modal-body').innerHTML = '';
    }

    function confirmDialog(title, message, confirmLabel = 'Confirmar') {
        return new Promise((resolve) => {
            openModal(title, `
                <div class="form">
                    <p style="margin:0">${message}</p>
                    <div class="form-actions">
                        <button class="btn" data-answer="no">Cancelar</button>
                        <button class="btn primary" data-answer="yes">${confirmLabel}</button>
                    </div>
                </div>`);
            $('#modal-body').addEventListener('click', (e) => {
                const answer = e.target.dataset.answer;
                if (!answer) return;
                closeModal();
                resolve(answer === 'yes');
            });
        });
    }

    // ---------- Carga de datos ----------
    async function loadAll() {
        try {
            const [customers, accounts, transactions] = await Promise.all([
                Api.customers.list(), Api.accounts.list(), Api.transactions.list(),
            ]);
            Object.assign(state, {customers, accounts, transactions});
            renderAll();
        } catch (error) {
            handleError(error);
        }
    }

    function renderAll() {
        renderDashboard();
        renderCustomers();
        renderAccountFilters();
        renderAccounts();
        renderTxForm();
        renderTransactions();
    }

    // ---------- Resumen ----------
    function renderDashboard() {
        const active = state.accounts.filter((a) => a.status === 'ACTIVE');
        const total = state.accounts.reduce((sum, a) => sum + Number(a.balance), 0);
        $('#kpi-customers').textContent = state.customers.length;
        $('#kpi-accounts').textContent = active.length;
        $('#kpi-balance').textContent = fmtMoney(total);
        $('#kpi-transactions').textContent = state.transactions.length;

        const recent = state.transactions.slice(0, 8);
        $('#dashboard-transactions').innerHTML = recent.length ? recent.map((t) => `
            <tr>
                <td>${fmtDateTime(t.createdAt)}</td>
                <td class="mono">${esc(t.accountNumber)}</td>
                <td>${LABELS.txType[t.type]}</td>
                <td class="num ${t.movement === 'CREDIT' ? 'amount-credit' : 'amount-debit'}">
                    ${t.movement === 'CREDIT' ? '+' : '−'}${fmtMoney(t.amount)}</td>
            </tr>`).join('') : emptyRow(4, 'Aún no hay movimientos');

        const byType = ['SAVINGS', 'CHECKING'].map((type) => {
            const accounts = state.accounts.filter((a) => a.accountType === type);
            return {type, count: accounts.length, total: accounts.reduce((s, a) => s + Math.max(0, Number(a.balance)), 0)};
        });
        const max = Math.max(1, ...byType.map((b) => b.total));
        $('#dashboard-split').innerHTML = byType.map((b) => `
            <div class="split-row">
                <header><span>${LABELS.accountType[b.type]} · ${b.count} cuenta(s)</span><strong>${fmtMoney(b.total)}</strong></header>
                <div class="bar ${b.type.toLowerCase()}"><span style="width:${(b.total / max) * 100}%"></span></div>
            </div>`).join('');
    }

    // ---------- Clientes ----------
    function renderCustomers() {
        const term = $('#customer-search').value.trim().toLowerCase();
        const rows = state.customers.filter((c) =>
            !term || [c.fullName, c.identificationNumber, c.email].some((v) => v.toLowerCase().includes(term)));
        $('#customers-body').innerHTML = rows.length ? rows.map((c) => {
            const products = state.accounts.filter((a) => a.customerId === c.id).length;
            return `
            <tr>
                <td><span class="cell-main">${esc(c.fullName)}</span><span class="cell-sub">${products} producto(s)</span></td>
                <td><span class="badge neutral">${c.identificationType}</span> ${esc(c.identificationNumber)}</td>
                <td>${esc(c.email)}</td>
                <td>${fmtDate(c.birthDate)}</td>
                <td>${fmtDateTime(c.createdAt)}</td>
                <td>${fmtDateTime(c.updatedAt)}</td>
                <td><div class="row-actions">
                    <button class="btn small" data-action="accounts" data-id="${c.id}">Cuentas</button>
                    <button class="btn small" data-action="edit" data-id="${c.id}">Editar</button>
                    <button class="btn small danger" data-action="delete" data-id="${c.id}"
                        ${products ? 'disabled title="Tiene productos vinculados"' : ''}>Eliminar</button>
                </div></td>
            </tr>`;
        }).join('') : emptyRow(7, term ? 'Sin resultados para la búsqueda' : 'No hay clientes registrados');
    }

    function maxBirthDate() {
        const d = new Date();
        d.setFullYear(d.getFullYear() - 18);
        return d.toISOString().slice(0, 10);
    }

    function openCustomerForm(customer) {
        const c = customer || {};
        const options = Object.entries(LABELS.idType).map(([value, label]) =>
            `<option value="${value}" ${c.identificationType === value ? 'selected' : ''}>${value} · ${label}</option>`).join('');
        openModal(customer ? 'Editar cliente' : 'Nuevo cliente', `
            <form class="form" id="customer-form" novalidate>
                <div class="row">
                    <label>Tipo de identificación<select name="identificationType" required>${options}</select></label>
                    <label>Número de identificación<input name="identificationNumber" required maxlength="20" value="${esc(c.identificationNumber)}"></label>
                </div>
                <div class="row">
                    <label>Nombres<input name="firstName" required minlength="2" maxlength="100" value="${esc(c.firstName)}"></label>
                    <label>Apellidos<input name="lastName" required minlength="2" maxlength="100" value="${esc(c.lastName)}"></label>
                </div>
                <label>Correo electrónico<input name="email" type="email" required maxlength="150" value="${esc(c.email)}"></label>
                <label>Fecha de nacimiento<input name="birthDate" type="date" required max="${maxBirthDate()}" value="${esc(c.birthDate)}"></label>
                <p class="hint">El cliente debe ser mayor de edad. Las fechas de creación y modificación se calculan automáticamente.</p>
                <div class="form-actions">
                    <button type="button" class="btn" data-close>Cancelar</button>
                    <button class="btn primary" type="submit">${customer ? 'Guardar cambios' : 'Registrar cliente'}</button>
                </div>
            </form>`);
        $('#customer-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const form = e.target;
            const data = Object.fromEntries(new FormData(form));
            const errors = validateCustomer(data);
            if (errors.length) return showFieldErrors(form, errors);
            try {
                if (customer) await Api.customers.update(customer.id, data);
                else await Api.customers.create(data);
                closeModal();
                toast(customer ? 'Cliente actualizado' : 'Cliente registrado');
                loadAll();
            } catch (error) {
                handleError(error, form);
            }
        });
    }

    function validateCustomer(data) {
        const errors = [];
        if (!data.identificationNumber.trim()) errors.push({field: 'identificationNumber', message: 'Obligatorio'});
        if (data.firstName.trim().length < 2) errors.push({field: 'firstName', message: 'Mínimo 2 caracteres'});
        if (data.lastName.trim().length < 2) errors.push({field: 'lastName', message: 'Mínimo 2 caracteres'});
        if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(data.email.trim())) {
            errors.push({field: 'email', message: 'Formato esperado: usuario@dominio.com'});
        }
        if (!data.birthDate) errors.push({field: 'birthDate', message: 'Obligatoria'});
        else if (data.birthDate > maxBirthDate()) errors.push({field: 'birthDate', message: 'El cliente debe ser mayor de edad'});
        return errors;
    }

    async function deleteCustomer(id) {
        const customer = state.customers.find((c) => c.id === id);
        const ok = await confirmDialog('Eliminar cliente',
            `¿Deseas eliminar a <strong>${esc(customer.fullName)}</strong>? Esta acción no se puede deshacer.`, 'Eliminar');
        if (!ok) return;
        try {
            await Api.customers.remove(id);
            toast('Cliente eliminado');
            loadAll();
        } catch (error) {
            handleError(error);
        }
    }

    // ---------- Cuentas ----------
    function renderAccountFilters() {
        const select = $('#account-customer-filter');
        const current = select.value;
        select.innerHTML = '<option value="">Todos los clientes</option>' + state.customers
            .map((c) => `<option value="${c.id}">${esc(c.fullName)} · ${esc(c.identificationNumber)}</option>`).join('');
        select.value = state.customers.some((c) => String(c.id) === current) ? current : '';
    }

    function renderAccounts() {
        const customerId = Number($('#account-customer-filter').value) || null;
        const rows = state.accounts.filter((a) => !customerId || a.customerId === customerId);
        $('#accounts-body').innerHTML = rows.length ? rows.map((a) => `
            <tr>
                <td class="mono cell-main">${a.accountNumber}<span class="cell-sub">Creada ${fmtDate(a.createdAt)}</span></td>
                <td>${LABELS.accountType[a.accountType]}</td>
                <td>${esc(customerName(a.customerId))}</td>
                <td><span class="badge ${a.status.toLowerCase()}">${LABELS.status[a.status]}</span></td>
                <td class="num">${fmtMoney(a.balance)}</td>
                <td class="num">${fmtMoney(a.availableBalance)}</td>
                <td>${a.gmfExempt ? 'Exenta' : '4 × 1.000'}</td>
                <td><div class="row-actions">
                    <button class="btn small" data-action="statement" data-id="${a.id}">Extracto</button>
                    ${a.status === 'ACTIVE' ? `<button class="btn small" data-action="status" data-status="INACTIVE" data-id="${a.id}">Inactivar</button>` : ''}
                    ${a.status === 'INACTIVE' ? `<button class="btn small" data-action="status" data-status="ACTIVE" data-id="${a.id}">Activar</button>` : ''}
                    ${a.status !== 'CANCELLED' ? `<button class="btn small danger" data-action="status" data-status="CANCELLED" data-id="${a.id}"
                        ${Number(a.balance) !== 0 ? 'disabled title="Solo se cancelan cuentas con saldo $0"' : ''}>Cancelar</button>` : ''}
                </div></td>
            </tr>`).join('') : emptyRow(8, 'No hay cuentas para mostrar');
    }

    function openAccountForm(preselectedCustomerId) {
        if (!state.customers.length) {
            toast('Primero registra un cliente', 'error');
            return;
        }
        const options = state.customers.map((c) => `<option value="${c.id}" ${c.id === preselectedCustomerId ? 'selected' : ''}>
            ${esc(c.fullName)} · ${c.identificationType} ${esc(c.identificationNumber)}</option>`).join('');
        openModal('Abrir cuenta', `
            <form class="form" id="account-form" novalidate>
                <label>Titular<select name="customerId" required>${options}</select></label>
                <label>Tipo de cuenta
                    <select name="accountType" required>
                        <option value="SAVINGS">Cuenta de ahorros (inicia en 53)</option>
                        <option value="CHECKING">Cuenta corriente (inicia en 33)</option>
                    </select>
                </label>
                <label class="check"><input type="checkbox" name="gmfExempt"> Exenta del GMF (4 × 1.000)</label>
                <p class="hint">El número de cuenta de 10 dígitos se genera automáticamente y la cuenta queda activa.</p>
                <div class="form-actions">
                    <button type="button" class="btn" data-close>Cancelar</button>
                    <button class="btn primary" type="submit">Abrir cuenta</button>
                </div>
            </form>`);
        $('#account-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const form = e.target;
            try {
                const account = await Api.accounts.open({
                    customerId: Number(form.customerId.value),
                    accountType: form.accountType.value,
                    gmfExempt: form.gmfExempt.checked,
                });
                closeModal();
                toast(`Cuenta ${account.accountNumber} creada`);
                loadAll();
            } catch (error) {
                handleError(error, form);
            }
        });
    }

    async function changeAccountStatus(id, status) {
        const account = state.accounts.find((a) => a.id === id);
        if (status === 'CANCELLED') {
            const ok = await confirmDialog('Cancelar cuenta',
                `La cuenta <strong class="mono">${account.accountNumber}</strong> quedará cancelada de forma definitiva.`, 'Cancelar cuenta');
            if (!ok) return;
        }
        try {
            await Api.accounts.changeStatus(id, status);
            toast(`Cuenta ${account.accountNumber}: ${LABELS.status[status].toLowerCase()}`);
            loadAll();
        } catch (error) {
            handleError(error);
        }
    }

    async function openStatement(id) {
        try {
            const s = await Api.accounts.statement(id);
            const rows = s.transactions.length ? s.transactions.map((t) => `
                <tr>
                    <td>${fmtDateTime(t.createdAt)}</td>
                    <td>${LABELS.txType[t.type]}${t.counterpartAccountNumber ? `<span class="cell-sub mono">${t.movement === 'DEBIT' ? 'a' : 'desde'} ${t.counterpartAccountNumber}</span>` : ''}</td>
                    <td>${esc(t.description)}</td>
                    <td class="num amount-credit">${t.movement === 'CREDIT' ? fmtMoney(t.amount) : ''}</td>
                    <td class="num amount-debit">${t.movement === 'DEBIT' ? fmtMoney(t.amount) : ''}</td>
                    <td class="num">${Number(t.gmfAmount) ? fmtMoney(t.gmfAmount) : ''}</td>
                    <td class="num">${fmtMoney(t.balanceAfter)}</td>
                </tr>`).join('') : emptyRow(7, 'La cuenta no tiene movimientos');
            openModal(`Estado de cuenta · ${s.account.accountNumber}`, `
                <div class="statement-head">
                    <div><span>Titular</span><strong>${esc(s.customer.fullName)}</strong><span>${s.customer.identificationType} ${esc(s.customer.identificationNumber)}</span></div>
                    <div><span>Producto</span><strong>${LABELS.accountType[s.account.accountType]}</strong><span>${LABELS.status[s.account.status]}</span></div>
                    <div><span>Saldo actual</span><strong>${fmtMoney(s.account.balance)}</strong><span>Disponible ${fmtMoney(s.account.availableBalance)}</span></div>
                    <div><span>Total créditos</span><strong class="amount-credit">${fmtMoney(s.totalCredits)}</strong></div>
                    <div><span>Total débitos</span><strong class="amount-debit">${fmtMoney(s.totalDebits)}</strong></div>
                    <div><span>GMF cobrado</span><strong>${fmtMoney(s.totalGmf)}</strong></div>
                </div>
                <div class="table-wrap"><table>
                    <thead><tr><th>Fecha</th><th>Tipo</th><th>Descripción</th><th class="num">Crédito</th><th class="num">Débito</th><th class="num">GMF</th><th class="num">Saldo</th></tr></thead>
                    <tbody>${rows}</tbody>
                </table></div>
                <div class="form-actions no-print" style="padding:16px">
                    <span class="hint" style="margin-right:auto">Generado ${fmtDateTime(s.generatedAt)}</span>
                    <button class="btn" onclick="window.print()">Imprimir</button>
                </div>`, true);
        } catch (error) {
            handleError(error);
        }
    }

    // ---------- Movimientos ----------
    function renderTxForm() {
        const form = $('#tx-form');
        const active = state.accounts.filter((a) => a.status === 'ACTIVE');
        const current = form.source.value;
        form.source.innerHTML = active.length
            ? active.map((a) => `<option value="${a.accountNumber}">${a.accountNumber} · ${LABELS.accountType[a.accountType]} · ${esc(customerName(a.customerId))}</option>`).join('')
            : '<option value="">No hay cuentas activas</option>';
        if (active.some((a) => a.accountNumber === current)) form.source.value = current;
        $('#tx-account-list').innerHTML = active.map((a) =>
            `<option value="${a.accountNumber}">${esc(customerName(a.customerId))}</option>`).join('');

        const isTransfer = state.txMode === 'transfer';
        $('#tx-destination').hidden = !isTransfer;
        $('#tx-source-label').firstChild.textContent = isTransfer ? 'Cuenta origen' : 'Cuenta';
        updateTxHint();

        const filter = $('#tx-account-filter');
        const selected = filter.value;
        filter.innerHTML = '<option value="">Todas las cuentas</option>' + state.accounts
            .map((a) => `<option value="${a.id}">${a.accountNumber} · ${esc(customerName(a.customerId))}</option>`).join('');
        filter.value = state.accounts.some((a) => String(a.id) === selected) ? selected : '';
    }

    function updateTxHint() {
        const form = $('#tx-form');
        const account = state.accounts.find((a) => a.accountNumber === form.source.value);
        if (!account) {
            $('#tx-hint').textContent = '';
            return;
        }
        const gmf = state.txMode !== 'deposit' && !account.gmfExempt ? ' · Se cobra GMF (4 × 1.000)' : '';
        $('#tx-hint').textContent = `Saldo ${fmtMoney(account.balance)} · Disponible ${fmtMoney(account.availableBalance)}${gmf}`;
    }

    function renderTransactions() {
        const accountId = Number($('#tx-account-filter').value) || null;
        const rows = state.transactions.filter((t) => !accountId || t.accountId === accountId);
        $('#transactions-body').innerHTML = rows.length ? rows.map((t) => `
            <tr>
                <td>${fmtDateTime(t.createdAt)}</td>
                <td class="mono">${t.accountNumber}</td>
                <td>${LABELS.txType[t.type]}<span class="cell-sub">${esc(t.description)}</span></td>
                <td class="num ${t.movement === 'CREDIT' ? 'amount-credit' : 'amount-debit'}">${t.movement === 'CREDIT' ? '+' : '−'}${fmtMoney(t.amount)}</td>
                <td class="num">${Number(t.gmfAmount) ? fmtMoney(t.gmfAmount) : '—'}</td>
                <td class="num">${fmtMoney(t.balanceAfter)}</td>
            </tr>`).join('') : emptyRow(6, 'No hay movimientos');
    }

    async function submitTransaction(e) {
        e.preventDefault();
        const form = e.target;
        const amount = Number(form.amount.value);
        const description = form.description.value.trim() || null;
        const errors = [];
        if (!form.source.value) errors.push({field: 'source', message: 'Selecciona una cuenta activa'});
        if (!(amount > 0)) errors.push({field: 'amount', message: 'El monto debe ser mayor a cero'});
        if (state.txMode === 'transfer' && !/^\d{10}$/.test(form.destination.value.trim())) {
            errors.push({field: 'destination', message: 'Ingresa un número de cuenta de 10 dígitos'});
        }
        showFieldErrors(form, errors);
        if (errors.length) return;

        try {
            if (state.txMode === 'deposit') {
                await Api.transactions.deposit({accountNumber: form.source.value, amount, description});
                toast('Consignación registrada');
            } else if (state.txMode === 'withdrawal') {
                await Api.transactions.withdraw({accountNumber: form.source.value, amount, description});
                toast('Retiro registrado');
            } else {
                await Api.transactions.transfer({
                    sourceAccountNumber: form.source.value,
                    destinationAccountNumber: form.destination.value.trim(),
                    amount, description,
                });
                toast('Transferencia realizada');
            }
            form.amount.value = '';
            form.description.value = '';
            form.destination.value = '';
            loadAll();
        } catch (error) {
            handleError(error);
        }
    }

    // ---------- Navegación y eventos ----------
    function showView(name) {
        document.querySelectorAll('.nav-item').forEach((b) => b.classList.toggle('active', b.dataset.view === name));
        document.querySelectorAll('.view').forEach((v) => v.classList.toggle('active', v.id === `view-${name}`));
        const view = VIEWS[name];
        $('#view-title').textContent = view.title;
        $('#view-subtitle').textContent = view.subtitle;
        const actions = $('#view-actions');
        actions.innerHTML = '';
        if (view.action) {
            const button = document.createElement('button');
            button.className = 'btn primary';
            button.textContent = view.action[0];
            button.addEventListener('click', () => view.action[1]());
            actions.appendChild(button);
        }
        try { localStorage.setItem('ptrinity.view', name); } catch (_) { /* almacenamiento no disponible */ }
    }

    document.querySelectorAll('.nav-item').forEach((b) => b.addEventListener('click', () => showView(b.dataset.view)));
    $('#modal').addEventListener('click', (e) => { if (e.target.closest('[data-close]')) closeModal(); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && !$('#modal').hidden) closeModal(); });

    $('#customer-search').addEventListener('input', renderCustomers);
    $('#customers-body').addEventListener('click', (e) => {
        const button = e.target.closest('button[data-action]');
        if (!button) return;
        const id = Number(button.dataset.id);
        if (button.dataset.action === 'edit') openCustomerForm(state.customers.find((c) => c.id === id));
        if (button.dataset.action === 'delete') deleteCustomer(id);
        if (button.dataset.action === 'accounts') {
            $('#account-customer-filter').value = String(id);
            renderAccounts();
            showView('accounts');
        }
    });

    $('#account-customer-filter').addEventListener('change', renderAccounts);
    $('#accounts-body').addEventListener('click', (e) => {
        const button = e.target.closest('button[data-action]');
        if (!button) return;
        const id = Number(button.dataset.id);
        if (button.dataset.action === 'statement') openStatement(id);
        if (button.dataset.action === 'status') changeAccountStatus(id, button.dataset.status);
    });

    document.querySelectorAll('.tab').forEach((tab) => tab.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t === tab));
        state.txMode = tab.dataset.tx;
        showFieldErrors($('#tx-form'), []);
        renderTxForm();
    }));
    $('#tx-form').addEventListener('submit', submitTransaction);
    $('#tx-form').source.addEventListener('change', updateTxHint);
    $('#tx-account-filter').addEventListener('change', renderTransactions);

    let initial = 'dashboard';
    try { initial = localStorage.getItem('ptrinity.view') || initial; } catch (_) { /* almacenamiento no disponible */ }
    showView(VIEWS[initial] ? initial : 'dashboard');
    loadAll();
})();
