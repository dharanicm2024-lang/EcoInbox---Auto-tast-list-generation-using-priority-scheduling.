import { useState, useEffect } from 'react';
import { getAllAccounts, sendEmail } from '../api';

export default function ComposeEmail({ showToast }) {
  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState({ accountId: '', to: '', subject: '', body: '' });
  const [sending, setSending] = useState(false);

  useEffect(() => {
    getAllAccounts().then(res => {
      const active = res.data.filter(a => a.isActive);
      setAccounts(active);
      if (active.length > 0) {
        setForm(f => ({ ...f, accountId: active[0].id }));
      }
    }).catch(() => {});
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(f => ({ ...f, [name]: name === 'accountId' ? Number(value) : value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.accountId || !form.to || !form.subject || !form.body) {
      showToast('All fields are required', 'error');
      return;
    }
    setSending(true);
    try {
      const res = await sendEmail(form);
      showToast(res.data.message);
      setForm(f => ({ ...f, to: '', subject: '', body: '' }));
    } catch (err) {
      const msg = err.response?.data?.error || err.response?.data?.message || 'Failed to send email';
      showToast(msg, 'error');
    } finally {
      setSending(false);
    }
  };

  if (accounts.length === 0) {
    return (
      <div className="email-form">
        <h2>New Message</h2>
        <div className="empty-state" style={{ padding: '30px 20px' }}>
          <div className="empty-icon">📬</div>
          <p>No email account connected.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="email-form">
      <h2>New Message</h2>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>From</label>
          <select name="accountId" value={form.accountId} onChange={handleChange}>
            {accounts.map(acc => (
              <option key={acc.id} value={acc.id}>
                {acc.displayName || acc.email}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>To</label>
          <input type="email" name="to" value={form.to} onChange={handleChange} placeholder="recipient@example.com" />
        </div>

        <div className="form-group">
          <label>Subject</label>
          <input type="text" name="subject" value={form.subject} onChange={handleChange} placeholder="Subject" />
        </div>

        <div className="form-group">
          <label>Body</label>
          <textarea name="body" value={form.body} onChange={handleChange} placeholder="Write your message..." rows={10} />
        </div>

        <button type="submit" className="btn-primary" disabled={sending} style={{ width: 'auto' }}>
          {sending ? 'Sending...' : 'Send'}
        </button>
      </form>
    </div>
  );
}
