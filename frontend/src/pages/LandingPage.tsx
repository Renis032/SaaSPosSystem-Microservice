import { useState, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { resetDemo } from '@/api/demo'
import { clearAuthSession, useAuth } from '@/stores/auth-store'
import { canAccessPos, homePathForRole } from '@/lib/roles'

const DEMO_ACCOUNTS = [
  {
    id: 'owner',
    role: 'OWNER',
    email: 'owner@renko.demo',
    path: 'Admin console',
  },
  {
    id: 'manager',
    role: 'MANAGER',
    email: 'manager@renko.demo',
    path: 'Back office',
  },
  {
    id: 'cashier',
    role: 'CASHIER',
    email: 'cashier@renko.demo',
    path: 'POS sell screen',
  },
] as const

const FEATURES = [
  {
    title: 'Counter POS',
    body: 'SKU search, stock guards, cash tender, demo card, and PDF receipts in one sell flow.',
  },
  {
    title: 'Back office',
    body: 'Products, inventory, staff, customers, orders, reports, and billing for store owners.',
  },
  {
    title: 'Roles that hold',
    body: 'Owner, cashier, and manager routes — with store tenancy checks behind the API.',
  },
  {
    title: 'Demo-ready',
    body: 'Fixed demo accounts and a one-click reset so every walkthrough starts from a known state.',
  },
] as const

function FeatureIcon({ children }: { children: ReactNode }) {
  return <span className="mkt-feature-icon">{children}</span>
}

function roleButtonLabel(role?: string | null) {
  if (!role) return 'APP'
  if (role === 'STORE_MANAGER' || role === 'BRANCH_MANAGER') return 'MANAGER'
  return role.replace(/_/g, ' ')
}

export function LandingPage() {
  const { token, user } = useAuth()
  const navigate = useNavigate()
  const [resetting, setResetting] = useState(false)
  const [resetMessage, setResetMessage] = useState<string | null>(null)
  const [resetError, setResetError] = useState<string | null>(null)

  const signedIn = Boolean(token)
  const appHome = homePathForRole(user?.role)
  const showPosShortcut = signedIn && canAccessPos(user?.role)
  const roleLabel = roleButtonLabel(user?.role)

  async function handleResetDemo() {
    if (
      !window.confirm(
        'Reset the demo database? This wipes all data and recreates the fixed Renko demo store.',
      )
    ) {
      return
    }
    setResetting(true)
    setResetError(null)
    setResetMessage(null)
    try {
      const result = await resetDemo()
      clearAuthSession()
      setResetMessage(
        result.message ??
          'Demo ready. Sign in with a demo role — password Demo1234!',
      )
    } catch (err) {
      const name = err instanceof Error ? err.name : ''
      if (name === 'TimeoutError' || name === 'AbortError') {
        setResetError(
          'Reset timed out. Start MySQL (scripts/ensure-mysql.sh), all six services, and the gateway on port 5000.',
        )
      } else {
        const msg = err instanceof Error ? err.message : 'Could not reset demo'
        setResetError(
          msg.includes('EntityManager') || msg.includes('Database unavailable')
            ? `${msg} — run scripts/ensure-mysql.sh, restart services, then try again.`
            : msg,
        )
      }
    } finally {
      setResetting(false)
    }
  }

  return (
    <div className="mkt">
      <header className="mkt-nav">
        <Link to="/" className="mkt-brand">
          <span className="mkt-mark">R</span>
          Renko
        </Link>
        <nav className="mkt-nav-links">
          <a href="#demo">Demo</a>
          <a href="#product">Product</a>
          {signedIn ? (
            <>
              {showPosShortcut ? (
                <Link to="/pos" className="mkt-btn mkt-btn-ghost">
                  POS
                </Link>
              ) : null}
              <button
                type="button"
                className="mkt-btn mkt-btn-solid"
                onClick={() => navigate(appHome)}
              >
                {roleLabel}
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="mkt-btn mkt-btn-ghost">
                Sign in
              </Link>
              <Link to="/signup" className="mkt-btn mkt-btn-solid">
                Get started
              </Link>
            </>
          )}
        </nav>
      </header>

      <section className="mkt-hero" id="demo">
        <div className="mkt-hero-copy">
          <p className="mkt-eyebrow">Demo</p>
          <h1>
            <span className="mkt-hero-brand">Renko</span>
            <span className="mkt-hero-line">
              {signedIn ? (
                <>
                  Welcome back. Open your <em>floor</em>.
                </>
              ) : (
                <>
                  One reset. Three roles. Ready to <em>sell</em>.
                </>
              )}
            </span>
          </h1>
          <p className="mkt-lede">
            {signedIn ? (
              <>
                Signed in as <strong>{user?.fullName ?? user?.email}</strong>
                {user?.role ? ` · ${user.role}` : ''}. Use your role button to continue
                {showPosShortcut ? ', or open POS' : ''}.
              </>
            ) : (
              <>Reset the demo, then open a role to walk the POS and admin flows.</>
            )}
          </p>
          <div className="mkt-hero-actions">
            {signedIn ? (
              <>
                <button
                  type="button"
                  className="mkt-btn mkt-btn-solid"
                  onClick={() => navigate(appHome)}
                >
                  {roleLabel}
                </button>
                {showPosShortcut ? (
                  <Link to="/pos" className="mkt-btn mkt-btn-ghost">
                    Open POS
                  </Link>
                ) : null}
              </>
            ) : (
              <>
                <Link to="/login" className="mkt-btn mkt-btn-solid">
                  Choose a role
                </Link>
                <button
                  type="button"
                  className="mkt-btn mkt-btn-ghost"
                  disabled={resetting}
                  onClick={() => void handleResetDemo()}
                >
                  {resetting ? 'Resetting…' : 'Reset demo'}
                </button>
              </>
            )}
          </div>
          {resetMessage ? <p className="mkt-ok mkt-hero-status">{resetMessage}</p> : null}
          {resetError ? <p className="mkt-err mkt-hero-status">{resetError}</p> : null}
        </div>

        <div className="mkt-hero-demo">
          <p className="mkt-hero-demo-label">Demo accounts</p>
          <ul className="mkt-account-stack">
            {DEMO_ACCOUNTS.map((account) => (
              <li key={account.email} className={account.id === 'cashier' ? 'mkt-account-featured' : undefined}>
                <Link to="/login" state={{ demoRole: account.id }} className="mkt-account-role-btn">
                  {account.role}
                  {account.id === 'cashier' ? <span className="mkt-account-badge">POS</span> : null}
                </Link>
                <span>{account.email}</span>
                <em>{account.path}</em>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="mkt-band mkt-band-light" id="product">
        <div className="mkt-band-inner">
          <h2>
            Everything the floor and the <em>office</em> need
          </h2>
          <p className="mkt-section-lede">
            From first sale to end-of-day reports — without bolting five tools together.
          </p>
          <div className="mkt-feature-grid">
            {FEATURES.map((feature) => (
              <article
                key={feature.title}
                className={`mkt-feature${feature.title === 'Counter POS' ? ' mkt-feature-featured' : ''}`}
              >
                <FeatureIcon>
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.6">
                    <rect x="4" y="4" width="16" height="16" rx="3" />
                    <path d="M8 12h8M8 16h5" />
                  </svg>
                </FeatureIcon>
                <h3>{feature.title}</h3>
                <p>{feature.body}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="mkt-split mkt-band-dark">
        <div className="mkt-split-inner">
          <div className="mkt-split-copy">
            <p className="mkt-eyebrow">Operations</p>
            <h2>
              Stock that moves with the <em>sale</em>
            </h2>
            <p>
              Pessimistic inventory locks, low-stock signals, and audit trails so concurrent checkouts
              don’t invent stock that isn’t there.
            </p>
            <ul className="mkt-bullets">
              <li>Locked inventory on order create</li>
              <li>Adjust stock with reasons</li>
              <li>Store report + audit log</li>
            </ul>
          </div>
          <div className="mkt-panel-card" aria-hidden="true">
            <div className="mkt-panel-row">
              <span>House Espresso</span>
              <strong>79</strong>
            </div>
            <div className="mkt-panel-row">
              <span>Cold Brew</span>
              <strong>60</strong>
            </div>
            <div className="mkt-panel-row warn">
              <span>Branded Tote</span>
              <strong>14</strong>
            </div>
            <div className="mkt-panel-foot">Low stock · threshold 15</div>
          </div>
        </div>
      </section>

      <section className="mkt-cta-band">
        <h2>{signedIn ? 'Back to the floor.' : 'Ready when you are.'}</h2>
        <p>
          {signedIn
            ? 'Open POS or continue with your role button.'
            : 'Reset the demo above, then tap OWNER, MANAGER, or CASHIER.'}
        </p>
        <div className="mkt-hero-actions">
          {signedIn ? (
            <>
              {showPosShortcut ? (
                <Link to="/pos" className="mkt-btn mkt-btn-solid">
                  Open POS
                </Link>
              ) : null}
              <button type="button" className="mkt-btn mkt-btn-ghost" onClick={() => navigate(appHome)}>
                {roleLabel}
              </button>
            </>
          ) : (
            <>
              <a href="#demo" className="mkt-btn mkt-btn-solid">
                Back to demo
              </a>
              <Link to="/login" className="mkt-btn mkt-btn-ghost">
                Sign in to POS
              </Link>
            </>
          )}
        </div>
      </section>

      <footer className="mkt-footer">
        <div className="mkt-footer-top">
          <Link to="/" className="mkt-brand">
            <span className="mkt-mark">R</span>
            Renko
          </Link>
          <div className="mkt-footer-cols">
            <div>
              <strong>Product</strong>
              <a href="#demo">Demo</a>
              <a href="#product">Features</a>
              <Link to="/playground">Playground</Link>
            </div>
            <div>
              <strong>Access</strong>
              {signedIn ? (
                <>
                  {showPosShortcut ? <Link to="/pos">POS</Link> : null}
                  <button type="button" className="mkt-footer-btn" onClick={() => navigate(appHome)}>
                    {roleLabel}
                  </button>
                </>
              ) : (
                <>
                  <Link to="/login">Sign in</Link>
                  <Link to="/signup">Sign up</Link>
                </>
              )}
            </div>
          </div>
        </div>
        <div className="mkt-footer-bottom">
          <span>© {new Date().getFullYear()} Renko · local demo</span>
        </div>
      </footer>
    </div>
  )
}
