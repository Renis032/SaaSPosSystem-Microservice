import { useCallback, useEffect, useMemo, useState } from 'react'
import { listCustomersByStore, searchCustomers } from '@/api/customers'
import { listInventoriesByStore } from '@/api/inventory'
import { createOrder, getOrderReceipt } from '@/api/orders'
import { listProductsByStore } from '@/api/products'
import { endShift, getCurrentShift, isNoOpenShiftError, startShiftOrGetCurrent } from '@/api/shifts'
import { useActiveBranch } from '@/lib/branch-store'
import { downloadReceiptPdf, printReceipt } from '@/lib/receipt-pdf'
import { useStoreId } from '@/hooks/useStoreId'
import type { Customer, Inventory, Product, Receipt, ShiftReport } from '@/types/models'

type CartLine = {
  product: Product
  quantity: number
}

type PaymentType = 'CASH' | 'CARD' | 'UPI'

function money(value?: number) {
  return `$${(value ?? 0).toFixed(2)}`
}

function estimateTotals(
  cart: CartLine[],
  taxRatePercent: number,
  orderDiscountPercent: number,
) {
  let subtotal = 0
  let lineDiscount = 0
  for (const line of cart) {
    const unit = line.product.sellingPrice
    const discPct = line.product.discountPercentage ?? 0
    const lineSub = unit * line.quantity
    const lineDisc = (unit * discPct) / 100 * line.quantity
    subtotal += lineSub
    lineDiscount += lineDisc
  }
  const afterLine = subtotal - lineDiscount
  const orderDiscount = (afterLine * orderDiscountPercent) / 100
  const taxable = afterLine - orderDiscount
  const taxAmount = (taxable * taxRatePercent) / 100
  const total = taxable + taxAmount
  return {
    subtotal,
    lineDiscount,
    orderDiscount,
    totalDiscount: lineDiscount + orderDiscount,
    taxAmount,
    total,
  }
}

export function PosPage() {
  const { storeId, loading: storeLoading, error: storeError, refresh: refreshStore } = useStoreId()
  const activeBranch = useActiveBranch()
  const [products, setProducts] = useState<Product[]>([])
  const [inventoryByProduct, setInventoryByProduct] = useState<Record<number, Inventory>>({})
  const [cart, setCart] = useState<CartLine[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [checkingOut, setCheckingOut] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [receipt, setReceipt] = useState<Receipt | null>(null)
  const [shift, setShift] = useState<ShiftReport | null>(null)
  const [shiftBusy, setShiftBusy] = useState(false)
  const [tendered, setTendered] = useState('')
  const [paymentType, setPaymentType] = useState<PaymentType>('CASH')
  const [cardName, setCardName] = useState('Demo Cardholder')
  const [cardNumber, setCardNumber] = useState('4242 4242 4242 4242')
  const [taxRate, setTaxRate] = useState('0')
  const [orderDiscountPercent, setOrderDiscountPercent] = useState('0')
  const [customers, setCustomers] = useState<Customer[]>([])
  const [customerQuery, setCustomerQuery] = useState('')
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null)
  const [customerBusy, setCustomerBusy] = useState(false)
  const [lastPayment, setLastPayment] = useState<{
    tendered?: number
    change?: number
    paymentType: PaymentType
  } | null>(null)

  const taxRateNum = Number(taxRate) || 0
  const discountNum = Number(orderDiscountPercent) || 0

  const loadCatalog = useCallback(async (id: number) => {
    setLoading(true)
    setError(null)
    try {
      const [productList, inventories] = await Promise.all([
        listProductsByStore(id),
        listInventoriesByStore(id),
      ])
      setProducts(Array.isArray(productList) ? productList : [])
      const map: Record<number, Inventory> = {}
      for (const row of Array.isArray(inventories) ? inventories : []) {
        map[row.productId] = row
      }
      setInventoryByProduct(map)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load products')
    } finally {
      setLoading(false)
    }
  }, [])

  const loadCustomers = useCallback(async (id: number) => {
    try {
      const list = await listCustomersByStore(id)
      setCustomers(Array.isArray(list) ? list : [])
    } catch {
      setCustomers([])
    }
  }, [])

  const loadShift = useCallback(async () => {
    try {
      const current = await getCurrentShift()
      setShift(current)
    } catch (err) {
      if (isNoOpenShiftError(err)) {
        setShift(null)
      }
    }
  }, [])

  useEffect(() => {
    if (storeId) {
      void loadCatalog(storeId)
      void loadCustomers(storeId)
      void loadShift()
    }
  }, [storeId, loadCatalog, loadCustomers, loadShift])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return products
    return products.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        (p.sku ?? '').toLowerCase().includes(q) ||
        (p.brand ?? '').toLowerCase().includes(q),
    )
  }, [products, search])

  const totals = useMemo(
    () => estimateTotals(cart, taxRateNum, discountNum),
    [cart, taxRateNum, discountNum],
  )

  const tenderedAmount = Number(tendered)
  const cashReady =
    paymentType !== 'CASH' ||
    (tendered !== '' && !Number.isNaN(tenderedAmount) && tenderedAmount >= totals.total)
  const cardReady =
    paymentType !== 'CARD' ||
    (cardName.trim().length > 1 && cardNumber.replace(/\s/g, '').length >= 12)
  const changeDue =
    paymentType === 'CASH' && cashReady ? tenderedAmount - totals.total : 0
  const canCheckout = cart.length > 0 && cashReady && cardReady && !checkingOut

  const customerMatches = useMemo(() => {
    const q = customerQuery.trim().toLowerCase()
    if (!q) return customers.slice(0, 8)
    return customers
      .filter(
        (c) =>
          c.fullName.toLowerCase().includes(q) ||
          (c.email ?? '').toLowerCase().includes(q) ||
          (c.phone ?? '').toLowerCase().includes(q),
      )
      .slice(0, 8)
  }, [customers, customerQuery])

  function stockFor(productId: number): number {
    return inventoryByProduct[productId]?.quantity ?? 0
  }

  function addToCart(product: Product) {
    setReceipt(null)
    setLastPayment(null)
    setError(null)
    const available = stockFor(product.id)
    if (available <= 0) {
      setError(`${product.name} is out of stock.`)
      return
    }
    const existing = cart.find((line) => line.product.id === product.id)
    const nextQty = (existing?.quantity ?? 0) + 1
    if (nextQty > available) {
      setError(`Only ${available} in stock for ${product.name}.`)
      return
    }
    if (existing) {
      setCart((prev) =>
        prev.map((line) =>
          line.product.id === product.id ? { ...line, quantity: nextQty } : line,
        ),
      )
    } else {
      setCart((prev) => [...prev, { product, quantity: 1 }])
    }
  }

  function updateQty(productId: number, quantity: number) {
    setError(null)
    if (quantity <= 0) {
      setCart((prev) => prev.filter((line) => line.product.id !== productId))
      return
    }
    const available = stockFor(productId)
    if (quantity > available) {
      const name = cart.find((line) => line.product.id === productId)?.product.name ?? 'Item'
      setError(`Only ${available} in stock for ${name}.`)
      quantity = available
      if (quantity <= 0) {
        setCart((prev) => prev.filter((line) => line.product.id !== productId))
        return
      }
    }
    setCart((prev) =>
      prev.map((line) => (line.product.id === productId ? { ...line, quantity } : line)),
    )
  }

  function clearCart() {
    setCart([])
    setTendered('')
  }

  function validateStock(): string | null {
    for (const line of cart) {
      const available = stockFor(line.product.id)
      if (available <= 0) {
        return `${line.product.name} is out of stock.`
      }
      if (line.quantity > available) {
        return `Only ${available} in stock for ${line.product.name} (cart has ${line.quantity}).`
      }
    }
    return null
  }

  async function handleCustomerSearch() {
    const q = customerQuery.trim()
    if (!q) {
      if (storeId) void loadCustomers(storeId)
      return
    }
    setCustomerBusy(true)
    try {
      const remote = await searchCustomers(q)
      if (Array.isArray(remote) && remote.length > 0) {
        setCustomers(remote)
      }
    } catch {
      // Local filter still works from loaded list
    } finally {
      setCustomerBusy(false)
    }
  }

  async function handleCheckout() {
    if (!storeId || cart.length === 0) return
    setError(null)

    if (!shift || shift.shiftEnd) {
      setError('Start a shift before checkout.')
      return
    }
    if (paymentType === 'CASH' && !cashReady) {
      setError('Amount tendered must be at least the cart total.')
      return
    }
    if (paymentType === 'CARD' && !cardReady) {
      setError('Enter a demo cardholder name and card number.')
      return
    }

    const stockError = validateStock()
    if (stockError) {
      setError(stockError)
      return
    }

    setCheckingOut(true)
    try {
      const inventories = await listInventoriesByStore(storeId)
      const map: Record<number, Inventory> = {}
      for (const row of Array.isArray(inventories) ? inventories : []) {
        map[row.productId] = row
      }
      setInventoryByProduct(map)

      for (const line of cart) {
        const available = map[line.product.id]?.quantity ?? 0
        if (available <= 0) {
          throw new Error(`${line.product.name} is out of stock.`)
        }
        if (line.quantity > available) {
          throw new Error(
            `Only ${available} in stock for ${line.product.name} (cart has ${line.quantity}).`,
          )
        }
      }

      const last4 = cardNumber.replace(/\D/g, '').slice(-4) || '4242'
      const order = await createOrder({
        storeId,
        customerId: selectedCustomer?.id,
        branchId: activeBranch?.id,
        taxRate: taxRateNum,
        orderDiscountPercent: discountNum,
        paymentType,
        stripePaymentIntentId:
          paymentType === 'CARD' ? `demo_pi_${Date.now()}_${last4}` : undefined,
        items: cart.map((line) => ({
          productId: line.product.id,
          quantity: line.quantity,
        })),
      })
      const receiptData = await getOrderReceipt(order.id)
      setLastPayment({
        paymentType,
        tendered: paymentType === 'CASH' ? tenderedAmount : totals.total,
        change: paymentType === 'CASH' ? tenderedAmount - totals.total : 0,
      })
      setReceipt(receiptData)
      setCart([])
      setTendered('')
      void loadCatalog(storeId)
      void loadShift()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Checkout failed')
    } finally {
      setCheckingOut(false)
    }
  }

  async function handleStartShift() {
    setShiftBusy(true)
    setError(null)
    try {
      const started = await startShiftOrGetCurrent(
        activeBranch?.id != null ? { branchId: activeBranch.id } : undefined,
      )
      setShift(started)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not start shift')
    } finally {
      setShiftBusy(false)
    }
  }

  async function handleEndShift() {
    setShiftBusy(true)
    setError(null)
    try {
      const ended = await endShift()
      setShift(ended)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not end shift')
    } finally {
      setShiftBusy(false)
    }
  }

  if (storeLoading) {
    return <div className="page-state">Resolving store…</div>
  }

  if (!storeId) {
    return (
      <div className="page-state">
        <h2>No store available</h2>
        <p>{storeError ?? 'Link or create a store before using POS.'}</p>
        <button type="button" className="btn" onClick={() => void refreshStore()}>
          Retry
        </button>
      </div>
    )
  }

  const shiftOpen = Boolean(shift && !shift.shiftEnd)
  const receiptOpts = {
    branch: activeBranch,
    paymentType: lastPayment?.paymentType ?? receipt?.paymentType,
    tendered: lastPayment?.tendered,
    change: lastPayment?.change,
  }

  return (
    <div className="pos-page">
      <div className="pos-toolbar">
        <div>
          <h1>Sell</h1>
          <p>
            Store #{storeId}
            {activeBranch ? ` · ${activeBranch.name}` : ''}
            {shiftOpen
              ? ` · Shift open since ${shift?.shiftStart ? new Date(shift.shiftStart).toLocaleString() : '—'}`
              : ' · No active shift — start before checkout'}
          </p>
        </div>
        <div className="pos-toolbar-actions">
          <button
            type="button"
            className="btn btn-secondary"
            disabled={shiftBusy || shiftOpen}
            onClick={() => void handleStartShift()}
          >
            Start shift
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            disabled={shiftBusy || !shiftOpen}
            onClick={() => void handleEndShift()}
          >
            End shift
          </button>
          <button type="button" className="btn btn-secondary" onClick={() => void loadCatalog(storeId)}>
            Refresh
          </button>
        </div>
      </div>

      {error ? <div className="app-alert error">{error}</div> : null}
      {!shiftOpen ? (
        <div className="app-alert">Start a shift to enable checkout (safeguard).</div>
      ) : null}

      <div className="pos-layout">
        <section className="pos-products panel">
          <div className="panel-header pos-products-header">
            <h2>Products</h2>
            <input
              className="pos-search"
              placeholder="Search name, SKU, brand…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="panel-body">
            {loading ? <p className="muted loading-msg">Loading catalog…</p> : null}
            {!loading && products.length === 0 ? (
              <div className="empty-state">
                <p className="muted">No products yet</p>
                <button type="button" className="btn btn-secondary btn-sm" onClick={() => void loadCatalog(storeId)}>
                  Refresh
                </button>
              </div>
            ) : null}
            {!loading && products.length > 0 && filtered.length === 0 ? (
              <p className="muted">No products match your search.</p>
            ) : null}
            <div className="product-grid">
              {filtered.map((product) => {
                const stock = stockFor(product.id)
                const outOfStock = stock <= 0
                return (
                  <button
                    key={product.id}
                    type="button"
                    className={`product-card${outOfStock ? ' out-of-stock' : ''}`}
                    onClick={() => addToCart(product)}
                    disabled={outOfStock}
                    title={outOfStock ? 'Out of stock' : `${stock} in stock`}
                  >
                    <strong>{product.name}</strong>
                    <span className="product-meta">{product.sku || product.brand || `ID ${product.id}`}</span>
                    <span className={`product-stock${outOfStock ? ' stock-zero' : ''}`}>
                      {outOfStock ? 'Out of stock' : `${stock} in stock`}
                    </span>
                    <span className="product-price">{money(product.sellingPrice)}</span>
                  </button>
                )
              })}
            </div>
          </div>
        </section>

        <aside className="pos-cart panel">
          <div className="panel-header">
            <h2>Cart</h2>
            <p>
              {cart.length} line{cart.length === 1 ? '' : 's'}
            </p>
          </div>
          <div className="panel-body cart-body">
            <div className="customer-picker">
              <span className="field-label">Customer</span>
              {selectedCustomer ? (
                <div className="customer-selected">
                  <div>
                    <strong>{selectedCustomer.fullName}</strong>
                    <span className="muted">
                      {[selectedCustomer.phone, selectedCustomer.email].filter(Boolean).join(' · ') ||
                        `ID ${selectedCustomer.id}`}
                    </span>
                  </div>
                  <button
                    type="button"
                    className="btn btn-sm btn-secondary"
                    onClick={() => setSelectedCustomer(null)}
                  >
                    Clear
                  </button>
                </div>
              ) : (
                <>
                  <div className="customer-search-row">
                    <input
                      className="pos-search"
                      placeholder="Search customer…"
                      value={customerQuery}
                      onChange={(e) => setCustomerQuery(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault()
                          void handleCustomerSearch()
                        }
                      }}
                    />
                    <button
                      type="button"
                      className="btn btn-sm btn-secondary"
                      disabled={customerBusy}
                      onClick={() => void handleCustomerSearch()}
                    >
                      {customerBusy ? '…' : 'Find'}
                    </button>
                  </div>
                  {customerMatches.length > 0 ? (
                    <ul className="customer-results">
                      {customerMatches.map((customer) => (
                        <li key={customer.id}>
                          <button
                            type="button"
                            onClick={() => {
                              setSelectedCustomer(customer)
                              setCustomerQuery('')
                            }}
                          >
                            <strong>{customer.fullName}</strong>
                            <span>
                              {[customer.phone, customer.email].filter(Boolean).join(' · ') ||
                                `#${customer.id}`}
                            </span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="muted">No customer selected — guest checkout.</p>
                  )}
                </>
              )}
            </div>

            {cart.length === 0 ? <p className="muted">Tap products to add them.</p> : null}
            <ul className="cart-list">
              {cart.map((line) => {
                const available = stockFor(line.product.id)
                return (
                  <li key={line.product.id} className="cart-line">
                    <div>
                      <strong>{line.product.name}</strong>
                      <span>
                        {money(line.product.sellingPrice)} each · {available} available
                      </span>
                    </div>
                    <div className="qty-controls">
                      <button type="button" onClick={() => updateQty(line.product.id, line.quantity - 1)}>
                        −
                      </button>
                      <input
                        type="number"
                        min={1}
                        max={available}
                        value={line.quantity}
                        onChange={(e) =>
                          updateQty(line.product.id, Math.max(1, Number(e.target.value) || 1))
                        }
                      />
                      <button
                        type="button"
                        disabled={line.quantity >= available}
                        onClick={() => updateQty(line.product.id, line.quantity + 1)}
                      >
                        +
                      </button>
                    </div>
                    <strong>{money(line.product.sellingPrice * line.quantity)}</strong>
                  </li>
                )
              })}
            </ul>

            <div className="cart-footer">
              <div className="order-adjust-fields">
                <label className="tender-field">
                  <span className="field-label">Tax rate (%)</span>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    step="0.01"
                    value={taxRate}
                    onChange={(e) => setTaxRate(e.target.value)}
                  />
                </label>
                <label className="tender-field">
                  <span className="field-label">Order discount (%)</span>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    step="0.01"
                    value={orderDiscountPercent}
                    onChange={(e) => setOrderDiscountPercent(e.target.value)}
                  />
                </label>
              </div>

              <div className="cart-totals">
                <div>
                  <span>Subtotal</span>
                  <strong>{money(totals.subtotal)}</strong>
                </div>
                {totals.totalDiscount > 0 ? (
                  <div>
                    <span>Discount</span>
                    <strong>−{money(totals.totalDiscount)}</strong>
                  </div>
                ) : null}
                {taxRateNum > 0 ? (
                  <div>
                    <span>Tax ({taxRateNum}%)</span>
                    <strong>{money(totals.taxAmount)}</strong>
                  </div>
                ) : null}
                <div className="cart-total">
                  <span>Total</span>
                  <strong>{money(totals.total)}</strong>
                </div>
              </div>

              <div className="pay-type-row" role="group" aria-label="Payment type">
                {(['CASH', 'CARD', 'UPI'] as const).map((type) => (
                  <button
                    key={type}
                    type="button"
                    className={`pay-type-btn${paymentType === type ? ' active' : ''}`}
                    onClick={() => setPaymentType(type)}
                  >
                    {type === 'CARD' ? 'Card (demo)' : type}
                  </button>
                ))}
              </div>

              {paymentType === 'CASH' ? (
                <>
                  <label className="tender-field">
                    <span className="field-label">Amount tendered</span>
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="0.00"
                      value={tendered}
                      onChange={(e) => setTendered(e.target.value)}
                      disabled={cart.length === 0}
                    />
                  </label>
                  <div className="tender-change">
                    <span>Change due</span>
                    <strong>{cart.length > 0 && cashReady ? money(changeDue) : '—'}</strong>
                  </div>
                </>
              ) : null}

              {paymentType === 'CARD' ? (
                <div className="demo-card-fields">
                  <label className="tender-field">
                    <span className="field-label">Name on card</span>
                    <input value={cardName} onChange={(e) => setCardName(e.target.value)} />
                  </label>
                  <label className="tender-field">
                    <span className="field-label">Card number (demo)</span>
                    <input
                      value={cardNumber}
                      onChange={(e) => setCardNumber(e.target.value)}
                      inputMode="numeric"
                      autoComplete="off"
                    />
                  </label>
                  <p className="muted demo-card-note">Simulated charge — no Stripe call.</p>
                </div>
              ) : null}

              {paymentType === 'UPI' ? (
                <p className="muted demo-card-note">UPI marked paid locally for this demo.</p>
              ) : null}

              <button type="button" className="btn btn-secondary" disabled={cart.length === 0} onClick={clearCart}>
                Clear
              </button>
              <button
                type="button"
                className="btn btn-checkout"
                disabled={!canCheckout || !shiftOpen}
                onClick={() => void handleCheckout()}
              >
                {checkingOut ? 'Processing…' : `Checkout (${paymentType === 'CARD' ? 'Card' : paymentType})`}
              </button>
            </div>

            {receipt ? (
              <div className="receipt-box receipt-print">
                <h3>Receipt {receipt.receiptNumber ?? `#${receipt.orderId}`}</h3>
                <p>
                  {receipt.storeName ?? 'Store'}
                  {receipt.branchName || activeBranch
                    ? ` · ${receipt.branchName ?? activeBranch?.name}`
                    : ''}{' '}
                  · {receipt.orderDate ? new Date(receipt.orderDate).toLocaleString() : ''}
                </p>
                <p>
                  Cashier: {receipt.cashierName ?? '—'} ·{' '}
                  {lastPayment?.paymentType ?? receipt.paymentType ?? 'CASH'}
                </p>
                {(receipt.customerName || selectedCustomer) && (
                  <p>Customer: {receipt.customerName ?? selectedCustomer?.fullName}</p>
                )}
                <ul>
                  {(receipt.items ?? []).map((item, index) => (
                    <li key={`${item.name}-${index}`}>
                      <span>
                        {item.quantity} × {item.name}
                      </span>
                      <span>{money(item.lineTotal ?? item.finalPrice)}</span>
                    </li>
                  ))}
                </ul>
                <div className="receipt-totals">
                  {receipt.subtotal != null ? (
                    <div>
                      <span>Subtotal</span>
                      <strong>{money(receipt.subtotal)}</strong>
                    </div>
                  ) : null}
                  {receipt.totalDiscount != null && receipt.totalDiscount > 0 ? (
                    <div>
                      <span>Discount</span>
                      <strong>−{money(receipt.totalDiscount)}</strong>
                    </div>
                  ) : null}
                  {receipt.taxRate != null && receipt.taxRate > 0 ? (
                    <div>
                      <span>Tax ({receipt.taxRate}%)</span>
                      <strong>{money(receipt.taxAmount)}</strong>
                    </div>
                  ) : null}
                  <div>
                    <span>Total</span>
                    <strong>
                      {money(
                        receipt.totalAmount ??
                          (receipt.items ?? []).reduce(
                            (sum, item) => sum + (item.lineTotal ?? item.finalPrice ?? 0),
                            0,
                          ),
                      )}
                    </strong>
                  </div>
                </div>
                {lastPayment?.paymentType === 'CASH' ? (
                  <div className="receipt-tender">
                    <div>
                      <span>Tendered</span>
                      <strong>{money(lastPayment.tendered)}</strong>
                    </div>
                    <div>
                      <span>Change</span>
                      <strong>{money(lastPayment.change)}</strong>
                    </div>
                  </div>
                ) : null}
                <div className="receipt-actions">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => downloadReceiptPdf(receipt, receiptOpts)}
                  >
                    Download PDF
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => printReceipt(receipt, receiptOpts)}
                  >
                    Print
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </aside>
      </div>
    </div>
  )
}
