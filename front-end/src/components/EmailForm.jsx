import { useState } from 'react';
import { submitEmail } from '../api';

export default function EmailForm({ onSuccess, showToast }) {
  const [form, setForm] = useState({ subject: '', sender: '', body: '' });
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.subject.trim() || !form.sender.trim() || !form.body.trim()) {
      showToast('Please fill all fields', 'error');
      return;
    }

    try {
      setSubmitting(true);
      await submitEmail(form);
      showToast('Email processed! Tasks extracted.');
      setForm({ subject: '', sender: '', body: '' });
      onSuccess();
    } catch (err) {
      showToast('Failed to process email', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const fillSample = () => {
    setForm({
      subject: 'Urgent: Q1 Report & Meeting Setup',
      sender: 'boss@company.com',
      body: `Hi Team,

Please submit the Q1 financial report by tomorrow EOD. This is urgent and needs to be reviewed before the board meeting.

Also, could you schedule a team meeting for next Monday to discuss the project roadmap?

Don't forget to complete the client proposal for Acme Corp - the deadline is Friday.

FYI - I've attached the newsletter from the industry conference. No action needed on that.

Thanks,
Manager`,
    });
  };

  return (
    <div className="email-form">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h2>✉️ Submit an Email for Task Extraction</h2>
        <button
          onClick={fillSample}
          className="filter-btn"
          style={{ fontSize: '0.8rem' }}
        >
          📝 Fill Sample
        </button>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Subject</label>
          <input
            type="text"
            name="subject"
            value={form.subject}
            onChange={handleChange}
            placeholder="e.g., Urgent: Submit Q1 Report"
          />
        </div>

        <div className="form-group">
          <label>Sender</label>
          <input
            type="text"
            name="sender"
            value={form.sender}
            onChange={handleChange}
            placeholder="e.g., boss@company.com"
          />
        </div>

        <div className="form-group">
          <label>Email Body</label>
          <textarea
            name="body"
            value={form.body}
            onChange={handleChange}
            placeholder="Paste the email content here..."
          />
        </div>

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Processing...' : '🚀 Extract Tasks from Email'}
        </button>
      </form>
    </div>
  );
}
