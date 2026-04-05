import { useState, useEffect } from 'react';
import {
  getAllAccounts,
  addAccount,
  testAccountConnection,
  fetchEmailsNow,
  toggleAccount,
  deleteAccount,
} from '../api';

const PROVIDER_HINTS = {
  'gmail.com': { host: 'imap.gmail.com', port: 993, note: 'Use a Google App Password (not your regular password). Go to myaccount.google.com → Security → 2-Step Verification → App passwords.' },
  'outlook.com': { host: 'outlook.office365.com', port: 993, note: 'Use your regular password or an App password if 2FA is enabled.' },
  'hotmail.com': { host: 'outlook.office365.com', port: 993, note: 'Use your regular password or an App password if 2FA is enabled.' },
  'yahoo.com': { host: 'imap.mail.yahoo.com', port: 993, note: 'Generate an App password at login.yahoo.com → Account Security → App password.' },
  'icloud.com': { host: 'imap.mail.me.com', port: 993, note: 'Generate an App-specific password at appleid.apple.com.' },
};

const GOOGLE_WORKSPACE_SUFFIXES = ['edu.in', 'edu', 'ac.in', 'org', 'org.in'];

function getProviderHint(email) {
  if (!email || !email.includes('@')) return null;
  const domain = email.split('@')[1]?.toLowerCase();
  if (!domain) return null;

  const exact = PROVIDER_HINTS[domain];
  if (exact) return exact;

  // Check for Google Workspace domains (educational / org)
  for (const suffix of GOOGLE_WORKSPACE_SUFFIXES) {
    if (domain.endsWith('.' + suffix)) {
      return {
        host: 'imap.gmail.com',
        port: 993,
        note: 'This looks like a Google Workspace account. Use a Google App Password (not your regular password). Go to myaccount.google.com → Security → 2-Step Verification → App passwords. If your admin hasn\'t enabled it, contact your IT admin.'
      };
    }
  }
  return null;
}

export default function EmailAccounts({ showToast, onRefresh }) {
  const [accounts, setAccounts] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ email: '', appPassword: '', imapHost: '', imapPort: 993, displayName: '' });
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [fetching, setFetching] = useState(false);
  const [providerHint, setProviderHint] = useState(null);

  const loadAccounts = async () => {
    try {
      const res = await getAllAccounts();
      setAccounts(res.data);
    } catch {
      showToast('Failed to load accounts', 'error');
    }
  };

  useEffect(() => { loadAccounts(); }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    const updated = { ...form, [name]: name === 'imapPort' ? parseInt(value) || 993 : value };

    // Auto-detect provider
    if (name === 'email' && value.includes('@')) {
      const hint = getProviderHint(value);
      if (hint) {
        setProviderHint(hint);
        if (!updated.imapHost) updated.imapHost = hint.host;
        if (!updated.imapPort || updated.imapPort === 993) updated.imapPort = hint.port;
      } else {
        setProviderHint(null);
      }
    }

    setForm(updated);
    setTestResult(null);
  };

  const handleTest = async () => {
    if (!form.email || !form.appPassword) {
      showToast('Email and password are required', 'error');
      return;
    }
    setTesting(true);
    setTestResult(null);
    try {
      const res = await testAccountConnection(form);
      setTestResult(res.data);
      showToast(res.data.message, res.data.success ? 'success' : 'error');
    } catch {
      setTestResult({ success: false, message: 'Connection test failed' });
      showToast('Connection test failed', 'error');
    } finally {
      setTesting(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.email || !form.appPassword) {
      showToast('Email and password are required', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await addAccount(form);
      showToast('Email account connected!');
      setForm({ email: '', appPassword: '', imapHost: '', imapPort: 993, displayName: '' });
      setShowForm(false);
      setTestResult(null);
      setProviderHint(null);
      loadAccounts();
    } catch (err) {
      showToast(err.response?.data?.error || 'Failed to add account', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleFetchNow = async () => {
    setFetching(true);
    try {
      const res = await fetchEmailsNow();
      showToast(res.data.message);
      loadAccounts();
      onRefresh();
    } catch {
      showToast('Failed to fetch emails', 'error');
    } finally {
      setFetching(false);
    }
  };

  const handleToggle = async (id) => {
    try {
      await toggleAccount(id);
      loadAccounts();
    } catch {
      showToast('Failed to update account', 'error');
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteAccount(id);
      showToast('Account removed');
      loadAccounts();
    } catch {
      showToast('Failed to delete account', 'error');
    }
  };

  return (
    <div>
      {/* Connected Accounts */}
      <div className="email-form" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h2>🔗 Connected Email Accounts</h2>
          <div style={{ display: 'flex', gap: 8 }}>
            {accounts.length > 0 && (
              <button
                className="filter-btn active"
                onClick={handleFetchNow}
                disabled={fetching}
                style={{ fontSize: '0.82rem' }}
              >
                {fetching ? '⏳ Fetching...' : '📥 Fetch Emails Now'}
              </button>
            )}
            <button
              className="filter-btn"
              onClick={() => setShowForm(!showForm)}
              style={{ fontSize: '0.82rem' }}
            >
              {showForm ? '✕ Cancel' : '+ Add Account'}
            </button>
          </div>
        </div>

        {accounts.length === 0 && !showForm ? (
          <div className="empty-state" style={{ padding: '30px 20px' }}>
            <div className="empty-icon">📬</div>
            <p>No email accounts connected yet.</p>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: 8 }}>
              Add your email to start fetching real emails automatically.
            </p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {accounts.map((acc) => (
              <div key={acc.id} className="email-card" style={{ cursor: 'default' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div className="email-subject">
                      {acc.isActive ? '🟢' : '⚪'} {acc.displayName || acc.email}
                      <span className="email-badge" style={
                        acc.isActive
                          ? {}
                          : { background: 'rgba(139,143,163,0.12)', color: 'var(--text-muted)' }
                      }>
                        {acc.isActive ? '✅ Active' : '⏸ Paused'}
                      </span>
                    </div>
                    <div className="email-sender">
                      {acc.email} · {acc.imapHost}:{acc.imapPort}
                      {acc.lastFetchedAt && (
                        <span style={{ marginLeft: 12 }}>
                          Last fetched: {new Date(acc.lastFetchedAt).toLocaleString()}
                        </span>
                      )}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: 6 }}>
                    <button
                      className="btn-icon"
                      onClick={() => handleToggle(acc.id)}
                      title={acc.isActive ? 'Pause' : 'Activate'}
                    >
                      {acc.isActive ? '⏸' : '▶'}
                    </button>
                    <button
                      className="btn-icon danger"
                      onClick={() => handleDelete(acc.id)}
                      title="Remove account"
                    >
                      🗑
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add Account Form */}
      {showForm && (
        <div className="email-form">
          <h2>✉️ Connect Email Account</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: 20 }}>
            Connect your email via IMAP to automatically fetch and process emails into tasks.
          </p>

          {providerHint && (
            <div style={{
              background: 'rgba(108,99,255,0.08)',
              border: '1px solid rgba(108,99,255,0.2)',
              borderRadius: 'var(--radius-sm)',
              padding: '12px 16px',
              marginBottom: 16,
              fontSize: '0.85rem',
              color: 'var(--text-muted)'
            }}>
              💡 <strong style={{ color: 'var(--text)' }}>Tip:</strong> {providerHint.note}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Email Address</label>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="you@gmail.com"
              />
            </div>

            <div className="form-group">
              <label>App Password</label>
              <input
                type="password"
                name="appPassword"
                value={form.appPassword}
                onChange={handleChange}
                placeholder="Your app-specific password"
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 120px', gap: 12 }}>
              <div className="form-group">
                <label>IMAP Host (auto-detected)</label>
                <input
                  type="text"
                  name="imapHost"
                  value={form.imapHost}
                  onChange={handleChange}
                  placeholder="imap.gmail.com"
                />
              </div>
              <div className="form-group">
                <label>Port</label>
                <input
                  type="number"
                  name="imapPort"
                  value={form.imapPort}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="form-group">
              <label>Display Name (optional)</label>
              <input
                type="text"
                name="displayName"
                value={form.displayName}
                onChange={handleChange}
                placeholder="e.g., Work Email"
              />
            </div>

            {testResult && (
              <div style={{
                padding: '10px 16px',
                borderRadius: 'var(--radius-sm)',
                marginBottom: 16,
                fontSize: '0.85rem',
                background: testResult.success ? 'rgba(77,255,145,0.08)' : 'rgba(255,77,106,0.08)',
                color: testResult.success ? 'var(--success)' : 'var(--danger)',
                border: `1px solid ${testResult.success ? 'rgba(77,255,145,0.2)' : 'rgba(255,77,106,0.2)'}`,
              }}>
                {testResult.success ? '✅' : '❌'} {testResult.message}
              </div>
            )}

            <div style={{ display: 'flex', gap: 12 }}>
              <button
                type="button"
                className="btn-primary"
                onClick={handleTest}
                disabled={testing}
                style={{ background: 'var(--bg-hover)', flex: 1 }}
              >
                {testing ? '⏳ Testing...' : '🔌 Test Connection'}
              </button>
              <button
                type="submit"
                className="btn-primary"
                disabled={submitting}
                style={{ flex: 1 }}
              >
                {submitting ? 'Connecting...' : '🚀 Connect Account'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
