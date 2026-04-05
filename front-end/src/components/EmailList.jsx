import { useState } from 'react';

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const now = new Date();
  const isToday = d.toDateString() === now.toDateString();
  if (isToday) return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  const isThisYear = d.getFullYear() === now.getFullYear();
  if (isThisYear) return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
  return d.toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' });
}

function getSenderName(sender) {
  if (!sender) return 'Unknown';
  const match = sender.match(/^(.+?)\s*<.*>$/);
  return match ? match[1].trim() : sender.split('@')[0];
}

function getSenderInitial(sender) {
  const name = getSenderName(sender);
  return name.charAt(0).toUpperCase();
}

function getPreview(body) {
  if (!body) return '';
  return body.replace(/\s+/g, ' ').substring(0, 120);
}

export default function EmailList({ emails, onSelectEmail }) {
  if (emails.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">📭</div>
        <p>No emails yet</p>
        <p style={{ color: 'var(--text-light)', fontSize: '0.84rem', marginTop: 4 }}>
          Your inbox is empty. Fetch emails from your connected account.
        </p>
      </div>
    );
  }

  return (
    <div className="email-list">
      {emails.map((email) => (
        <div
          key={email.id}
          className={`email-row ${!email.isProcessed ? 'unread' : ''}`}
          onClick={() => onSelectEmail(email)}
        >
          <span className="email-row-star">☆</span>
          <span className="email-row-sender">
            {getSenderName(email.sender)}
          </span>
          <div className="email-row-content">
            <span className="email-row-subject">{email.subject}</span>
            {email.taskCount > 0 && (
              <span className="email-badge">{email.taskCount} tasks</span>
            )}
            <span className="email-row-preview"> — {getPreview(email.body)}</span>
          </div>
          <span className="email-row-date">{formatDate(email.receivedAt)}</span>
        </div>
      ))}
    </div>
  );
}

export function EmailDetail({ email, onBack }) {
  return (
    <div className="email-detail">
      <div className="email-detail-header">
        <button className="back-btn" onClick={onBack}>
          ← Back to Inbox
        </button>
        {email.isProcessed && (
          <span className="email-badge" style={{ marginLeft: 'auto' }}>
            ✅ Processed · {email.taskCount || 0} tasks
          </span>
        )}
      </div>

      <div className="email-detail-body">
        <h2>{email.subject || '(No Subject)'}</h2>

        <div className="email-detail-meta">
          <div
            className="email-detail-avatar"
            style={{ background: `hsl(${(email.sender || '').length * 37 % 360}, 60%, 50%)` }}
          >
            {getSenderInitial(email.sender)}
          </div>
          <div className="email-detail-sender-info">
            <div className="email-detail-sender-name">{getSenderName(email.sender)}</div>
            <div className="email-detail-sender-email">{email.sender}</div>
          </div>
          <div className="email-detail-date">
            {email.receivedAt ? new Date(email.receivedAt).toLocaleString() : ''}
          </div>
        </div>

        <div className="email-detail-content">
          {email.body}
        </div>

        {email.tasks && email.tasks.length > 0 && (
          <div className="email-detail-tasks">
            <h3>📋 Extracted Tasks ({email.tasks.length})</h3>
            <ul>
              {email.tasks.map((task, i) => (
                <li key={i}>
                  <span style={{ color: task.priority === 'HIGH' ? 'var(--high)' : task.priority === 'MEDIUM' ? 'var(--medium)' : 'var(--low)' }}>
                    {task.priority === 'HIGH' ? '🔴' : task.priority === 'MEDIUM' ? '🟡' : '🟢'}
                  </span>
                  {task.taskName}
                  {task.deadline && <span style={{ color: 'var(--text-muted)', marginLeft: 8, fontSize: '0.8rem' }}>📅 {task.deadline}</span>}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
