(function () {
  const root = document.getElementById('pos');
  if (!root) return;

  const vat = parseFloat(root.dataset.vat || '0');
  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
  const $ = id => document.getElementById(id);
  const money = n => '৳' + Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const HELD_KEY = 'niramoy.held.sale.v2';

  // cart line: {id,name,batchNo,boxNo,price,stripPrice,stock,stripSize,strips,pieces}
  let cart = [];
  let results = [];
  let timer = null;
  let paidTouched = false;   // ব্যবহারকারী নিজে পরিশোধের অঙ্ক লিখলে true

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text !== undefined) e.textContent = text;
    return e;
  }
  function toast(msg) {
    const t = el('div', 'toast', msg);
    t.setAttribute('role', 'alert');
    document.body.appendChild(t);
    setTimeout(() => t.remove(), 4500);
  }
  const needPieces = c => c.strips * c.stripSize + c.pieces;
  const lineTotal = c => c.strips * c.stripPrice + c.pieces * c.price;
  function stockText(stock, size) {
    if (size <= 1) return stock + ' পিস';
    const s = Math.floor(stock / size), l = stock % size;
    if (s === 0) return l + ' পিস';
    return l === 0 ? s + ' পাতা' : s + ' পাতা ' + l + ' পিস';
  }

  // ---------- search ----------
  async function search(q) {
    try {
      const res = await fetch('/pos/api/search?q=' + encodeURIComponent(q), { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
      if (!res.ok) throw new Error((await res.json()).message || 'অনুসন্ধান ব্যর্থ হয়েছে');
      results = await res.json();
      renderResults();
    } catch (e) { toast(e.message); }
  }

  function renderResults() {
    const box = $('posResults');
    box.replaceChildren();
    if (!results.length) { box.appendChild(el('p', 'muted center', 'কোনো ওষুধ পাওয়া যায়নি।')); return; }
    results.forEach(m => {
      const row = el('div', 'result-row');
      const left = el('div');
      const title = el('strong', null, m.name + ' ');
      if (m.bestSeller) {
        const star = el('span', 'box-tag', '⭐ বেশি বিক্রিত');
        star.style.marginRight = '6px';
        title.appendChild(star);
      }
      if (m.boxNo) title.appendChild(el('span', 'box-tag', 'বক্স ' + m.boxNo));
      left.appendChild(title);
      const sm = el('small', 'muted', (m.genericName ? m.genericName + ' · ' : '') + (m.companyName ? m.companyName + ' · ' : '') + m.batchNo + ' · মেয়াদ ' + m.expiryDate + ' · স্টক ' + stockText(m.stock, m.stripSize));
      sm.style.display = 'block';
      left.appendChild(sm);

      const actions = el('div', 'result-actions');
      const pcs = el('button', 'btn sm', '+ পিস ' + money(m.price));
      pcs.type = 'button';
      pcs.addEventListener('click', () => addToCart(m, 'pieces'));
      actions.appendChild(pcs);
      if (m.stripSize > 1) {
        const strip = el('button', 'btn sm primary', '+ পাতা ' + money(m.stripPrice));
        strip.type = 'button';
        strip.title = '১ পাতা = ' + m.stripSize + ' পিস';
        strip.addEventListener('click', () => addToCart(m, 'strips'));
        actions.appendChild(strip);
      }
      row.append(left, actions);
      box.appendChild(row);
    });
  }

  $('posSearch').addEventListener('input', e => { clearTimeout(timer); timer = setTimeout(() => search(e.target.value), 250); });
  $('posSearch').addEventListener('keydown', e => {
    if (e.key !== 'Enter') return;
    e.preventDefault();
    const q = e.target.value.trim();
    const exact = results.filter(m => m.barcode === q);
    const pick = exact.length ? exact[0] : (results.length === 1 ? results[0] : null);
    if (pick) { addToCart(pick, 'pieces'); e.target.value = ''; search(''); }
  });

  // ---------- cart ----------
  function addToCart(m, mode) {
    let line = cart.find(c => c.id === m.id);
    if (!line) {
      line = { id: m.id, name: m.name, batchNo: m.batchNo, boxNo: m.boxNo, price: m.price, stripPrice: m.stripPrice,
               stock: m.stock, stripSize: m.stripSize || 1, strips: 0, pieces: 0 };
      cart.push(line);
    }
    line[mode] += 1;
    if (needPieces(line) > line.stock) {
      line[mode] -= 1;
      if (needPieces(line) === 0) cart = cart.filter(c => c !== line);
      toast('পর্যাপ্ত স্টক নেই। স্টক: ' + stockText(line.stock, line.stripSize));
    }
    renderCart();
  }

  /** পাতা/পিস বক্সে হাতে লিখে পরিমাণ বদলানো */
  function setLine(id, strips, pieces) {
    const line = cart.find(c => c.id === id);
    if (!line) return;
    strips = Math.max(0, Math.floor(strips) || 0);
    pieces = Math.max(0, Math.floor(pieces) || 0);
    if (strips * line.stripSize + pieces > line.stock) {
      toast('পর্যাপ্ত স্টক নেই। স্টক: ' + stockText(line.stock, line.stripSize));
      return renderCart();
    }
    line.strips = strips; line.pieces = pieces;
    if (strips === 0 && pieces === 0) cart = cart.filter(c => c.id !== id);
    renderCart();
  }

  const r2 = x => Math.round(x * 100) / 100;

  function totals() {
    const subtotal = cart.reduce((n, c) => n + lineTotal(c), 0);
    let discount = parseFloat($('discount').value) || 0;
    discount = Math.min(Math.max(discount, 0), subtotal);
    const net = subtotal - discount;
    const vatAmount = net * vat / 100;
    const total = r2(net + vatAmount);

    let paid = total;                                   // ডিফল্ট: পূর্ণ পরিশোধ
    if ($('payment').value === 'CREDIT') paid = 0;      // বকেয়া বিক্রয়: কিছুই দেয়নি
    else if (paidTouched) {
      const raw = parseFloat($('paid').value);
      paid = Number.isFinite(raw) ? r2(Math.min(Math.max(raw, 0), total)) : total;
    }
    return { subtotal, discount, vatAmount, total, paid, due: r2(Math.max(total - paid, 0)) };
  }

  function numInput(value) {
    const i = document.createElement('input');
    i.type = 'number'; i.min = '0'; i.step = '1'; i.inputMode = 'numeric'; i.value = String(value);
    i.addEventListener('focus', () => i.select());
    return i;
  }
  function field(label, input) { const f = el('label', 'qty-f'); f.append(el('small', 'muted', label), input); return f; }

  function renderCart() {
    const box = $('cartItems');
    box.replaceChildren();
    if (!cart.length) {
      const p = el('p', 'muted center', 'কার্ট খালি। পাশ থেকে পিস বা পাতা যোগ করুন।');
      p.style.padding = '28px';
      box.appendChild(p);
    }
    cart.forEach(c => {
      const row = el('div', 'cart-line');
      const info = el('div');
      const title = el('strong', null, c.name + ' ');
      if (c.boxNo) title.appendChild(el('span', 'box-tag', 'বক্স ' + c.boxNo));
      info.appendChild(title);
      const sm = el('small', 'muted', c.batchNo + ' · পিস ' + money(c.price)
        + (c.stripSize > 1 ? ' · পাতা ' + money(c.stripPrice) + ' (১ পাতা = ' + c.stripSize + ' পিস)' : ''));
      sm.style.display = 'block';
      info.appendChild(sm);
      const lt = el('small', 'bold', 'মোট ' + money(lineTotal(c)) + ' · স্টক থেকে যাবে ' + needPieces(c) + ' পিস');
      lt.style.display = 'block';
      info.appendChild(lt);

      const qty = el('div', 'qty');
      const hasStrip = c.stripSize > 1;
      const stripIn = hasStrip ? numInput(c.strips) : null;
      const pcsIn = numInput(c.pieces);
      const commit = () => setLine(c.id, hasStrip ? parseInt(stripIn.value, 10) : 0, parseInt(pcsIn.value, 10));
      [stripIn, pcsIn].forEach(i => {
        if (!i) return;
        i.addEventListener('change', commit);                  // টাইপ করার সময় ফোকাস হারায় না
        i.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); commit(); } });
      });
      if (hasStrip) qty.append(field('পাতা', stripIn));
      qty.append(field('পিস', pcsIn));
      const rm = el('button', 'btn sm rm', '✕'); rm.type = 'button'; rm.setAttribute('aria-label', 'লাইন মুছুন');
      rm.addEventListener('click', () => { cart = cart.filter(x => x.id !== c.id); renderCart(); });
      qty.append(rm);

      row.append(info, qty);
      box.appendChild(row);
    });

    const t = totals();
    $('cartTotal').innerHTML = '';
    [['উপমোট', t.subtotal], ['ছাড়', -t.discount], ['VAT (' + vat + '%)', t.vatAmount]].forEach(([k, v]) => {
      const d = el('div'); d.append(el('span', null, k), el('b', null, money(v))); $('cartTotal').appendChild(d);
    });
    const g = el('div', 'grand'); g.append(el('span', null, 'মোট'), el('span', null, money(t.total)));
    $('cartTotal').appendChild(g);
    if (t.due > 0.004) {
      [['পরিশোধ', t.paid], ['বকেয়া', t.due]].forEach(([k, v]) => {
        const d = el('div'); d.append(el('span', null, k), el('b', null, money(v))); $('cartTotal').appendChild(d);
      });
    }

    // পরিশোধ ইনপুট ও গ্রাহকের তথ্য ঘর
    const credit = $('payment').value === 'CREDIT';
    const paidInput = $('paid');
    paidInput.disabled = credit;
    if (credit) paidInput.value = '0';
    else if (!paidTouched && document.activeElement !== paidInput) paidInput.value = t.total > 0 ? t.total.toFixed(2) : '';
  }

  $('discount').addEventListener('input', renderCart);
  $('payment').addEventListener('change', () => { paidTouched = false; $('paid').value = ''; renderCart(); });
  $('paid').addEventListener('input', () => { paidTouched = $('paid').value.trim() !== ''; renderCart(); });
  $('payFull').addEventListener('click', () => { paidTouched = false; $('paid').value = ''; renderCart(); });

  // ---------- hold / restore ----------
  $('holdBtn').addEventListener('click', () => {
    if (!cart.length) return toast('কার্ট খালি।');
    try { localStorage.setItem(HELD_KEY, JSON.stringify(cart)); } catch (e) { return toast('ধরে রাখা সম্ভব হয়নি।'); }
    cart = []; renderCart(); toast('বিক্রয় ধরে রাখা হয়েছে (এই ব্রাউজারে)।');
  });
  $('restoreHeld').addEventListener('click', () => {
    try {
      const held = JSON.parse(localStorage.getItem(HELD_KEY) || '[]');
      if (!held.length) return toast('ধরে রাখা বিক্রয় নেই।');
      cart = held; localStorage.removeItem(HELD_KEY); renderCart();
    } catch (e) { toast('ধরে রাখা বিক্রয় পড়া যায়নি।'); }
  });

  // ---------- preview ----------
  $('previewBtn').addEventListener('click', () => {
    if (!cart.length) return toast('কার্টে ওষুধ যোগ করুন।');
    const body = $('previewBody'); body.replaceChildren();
    cart.forEach(c => {
      const qtyText = [c.strips ? c.strips + ' পাতা' : '', c.pieces ? c.pieces + ' পিস' : ''].filter(Boolean).join(' + ');
      const r = el('div', 'row between'); r.style.padding = '6px 0';
      r.append(el('span', null, c.name + ' × ' + qtyText), el('span', null, money(lineTotal(c))));
      body.appendChild(r);
    });
    const t = totals();
    const g = el('p', 'right bold mt', 'মোট ' + money(t.total)); g.style.fontSize = '20px';
    body.appendChild(g);
    $('previewDialog').showModal();
  });
  $('closePreview').addEventListener('click', () => $('previewDialog').close());

  // ---------- checkout ----------
  $('checkoutBtn').addEventListener('click', async () => {
    if (!cart.length) return toast('কার্টে ওষুধ যোগ করুন।');
    const method = $('payment').value;
    const t = totals();
    const custName = $('custName').value.trim();
    const custPhone = $('custPhone').value.replace(/[০-৯]/g, d => '০১২৩৪৫৬৭৮৯'.indexOf(d)).replace(/[\s\-()]/g, '');
    if (!custName) { $('custName').focus(); return toast('গ্রাহকের নাম আবশ্যক।'); }
    if (!/^(\+?88)?01[3-9][0-9]{8}$/.test(custPhone)) {
      $('custPhone').focus();
      return toast('সঠিক মোবাইল নম্বর দিন (যেমন: 01712345678)।');
    }
    const msg = t.due > 0.004
      ? 'মোট ' + money(t.total) + ' · পরিশোধ ' + money(t.paid) + ' · বকেয়া ' + money(t.due) + ' — বিক্রয় সম্পন্ন করবেন?'
      : 'মোট ' + money(t.total) + ' — বিক্রয় সম্পন্ন করবেন?';
    if (!confirm(msg)) return;

    const btn = $('checkoutBtn'); btn.disabled = true;
    try {
      const res = await fetch('/pos/api/checkout', {
        method: 'POST', credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json', [csrfHeader]: csrfToken },
        body: JSON.stringify({
          items: cart.map(c => ({ medicineId: c.id, strips: c.strips, pieces: c.pieces })),
          discount: totals().discount,
          paymentMethod: method,
          paidAmount: (paidTouched && method !== 'CREDIT') ? t.paid : null,   // null = পূর্ণ পরিশোধ
          customerName: custName,
          customerPhone: custPhone
        })
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || 'বিক্রয় সম্পন্ন করা যায়নি।');
      window.location.href = '/sales/' + data.saleId;
    } catch (e) { toast(e.message); btn.disabled = false; }
  });

  renderCart();
  search('');
})();
