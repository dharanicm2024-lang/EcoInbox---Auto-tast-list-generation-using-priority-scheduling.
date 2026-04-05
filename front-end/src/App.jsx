import { useState, useEffect, useCallback, useRef } from 'react';
import LoginPage from './components/LoginPage';
import TaskList from './components/TaskList';
import EmailForm from './components/EmailForm';
import EmailList, { EmailDetail } from './components/EmailList';
import ComposeEmail from './components/ComposeEmail';
import StatsBar from './components/StatsBar';
import Toast from './components/Toast';
import { getAllTasks, getDashboardStats, getAllEmails, getEmailById, fetchEmailsNow } from './api';

export default function App() {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('emailFilterUser');
    return saved ? JSON.parse(saved) : null;
  });

  const [activeView, setActiveView] = useState('inbox');
  const [tasks, setTasks] = useState([]);
  const [emails, setEmails] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);
  const [selectedEmail, setSelectedEmail] = useState(null);
  const loginInProgress = useRef(false);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const refresh = useCallback(async () => {
    try {
      setLoading(true);
      const [tasksRes, statsRes, emailsRes] = await Promise.all([
        getAllTasks(),
        getDashboardStats(),
        getAllEmails(),
      ]);
      setTasks(tasksRes.data);
      setStats(statsRes.data);
      setEmails(emailsRes.data);
    } catch {
      showToast('Failed to load data', 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  const handleLogin = async (account) => {
    loginInProgress.current = true;
    localStorage.setItem('emailFilterUser', JSON.stringify(account));
    setUser(account);
    setLoading(true);
    // Fetch emails first, then load data
    try {
      await fetchEmailsNow();
    } catch {
      // non-blocking — account is added, emails may come on next scheduled fetch
    }
    // Now load all data (emails should be populated)
    await refresh();
    loginInProgress.current = false;
  };

  const handleLogout = () => {
    localStorage.removeItem('emailFilterUser');
    setUser(null);
    setActiveView('inbox');
    setSelectedEmail(null);
  };

  const handleSelectEmail = async (email) => {
    try {
      const res = await getEmailById(email.id);
      setSelectedEmail(res.data);
    } catch {
      setSelectedEmail(email);
    }
  };

  useEffect(() => {
    if (user && !loginInProgress.current) refresh();
  }, [user, refresh]);

  if (!user) {
    return (
      <>
        <LoginPage onLogin={handleLogin} />
        {toast && <Toast message={toast.message} type={toast.type} />}
      </>
    );
  }

  const navItems = [
    { id: 'inbox', icon: '📥', label: 'Inbox', count: emails.length },
    { id: 'tasks', icon: '📋', label: 'Tasks', count: tasks.length },
    { id: 'dashboard', icon: '📊', label: 'Dashboard' },
    { id: 'compose', icon: '✏️', label: 'Compose' },
    { id: 'submit', icon: '🧪', label: 'Test' },
  ];

  return (
    <div className="app-shell">
      {/* Top Bar */}
      <div className="app-topbar">
        <div className="topbar-logo">
          <span>📧</span>
          <h1>Mail Tasks</h1>
        </div>
        <div className="topbar-search">
          <input type="text" placeholder="Search emails and tasks..." />
        </div>
        <div className="topbar-user">
          <span className="user-email">{user.email}</span>
          <button className="logout-btn" onClick={handleLogout}>Sign out</button>
        </div>
      </div>

      <div className="app-body">
        {/* Sidebar */}
        <aside className="sidebar">
          <button
            className="sidebar-compose-btn"
            onClick={() => { setActiveView('compose'); setSelectedEmail(null); }}
          >
            <span className="compose-icon">✏️</span>
            Compose
          </button>

          <ul className="sidebar-nav">
            {navItems.filter(i => i.id !== 'compose').map((item) => (
              <li key={item.id}>
                <button
                  className={activeView === item.id ? 'active' : ''}
                  onClick={() => { setActiveView(item.id); setSelectedEmail(null); }}
                >
                  <span className="nav-icon">{item.icon}</span>
                  {item.label}
                  {item.count != null && <span className="nav-count">{item.count}</span>}
                </button>
              </li>
            ))}
          </ul>
        </aside>

        {/* Main Content */}
        <main className="main-content">
          {loading && !emails.length ? (
            <div className="loading">
              <div className="spinner"></div>
              <p>Loading your emails...</p>
            </div>
          ) : (
            <>
              {/* Inbox */}
              {activeView === 'inbox' && !selectedEmail && (
                <>
                  <div className="content-header">
                    <span style={{ fontSize: '0.88rem', fontWeight: 500, color: 'var(--text-muted)' }}>
                      Inbox
                    </span>
                    <button
                      className="filter-btn active"
                      onClick={async () => {
                        try {
                          const res = await fetchEmailsNow();
                          showToast(res.data.message);
                          refresh();
                        } catch { showToast('Failed to fetch', 'error'); }
                      }}
                      style={{ fontSize: '0.78rem' }}
                    >
                      🔄 Refresh
                    </button>
                  </div>
                  <div className="content-body">
                    <EmailList emails={emails} onSelectEmail={handleSelectEmail} />
                  </div>
                </>
              )}

              {/* Email Detail */}
              {activeView === 'inbox' && selectedEmail && (
                <div className="content-body">
                  <EmailDetail email={selectedEmail} onBack={() => setSelectedEmail(null)} />
                </div>
              )}

              {/* Tasks */}
              {activeView === 'tasks' && (
                <div className="content-body" style={{ padding: 0 }}>
                  <TaskList tasks={tasks} onRefresh={refresh} showToast={showToast} />
                </div>
              )}

              {/* Dashboard */}
              {activeView === 'dashboard' && stats && (
                <div className="content-body">
                  <StatsBar stats={stats} />
                </div>
              )}

              {/* Compose */}
              {activeView === 'compose' && (
                <div className="content-body">
                  <ComposeEmail showToast={showToast} />
                </div>
              )}

              {/* Submit Test Email */}
              {activeView === 'submit' && (
                <div className="content-body">
                  <EmailForm onSuccess={() => { refresh(); setActiveView('tasks'); }} showToast={showToast} />
                </div>
              )}
            </>
          )}
        </main>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} />}

      {/* Mobile Bottom Nav */}
      <div className="mobile-bottom-nav">
        <div className="mobile-bottom-nav-inner">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={`mobile-nav-btn ${activeView === item.id ? 'active' : ''}`}
              onClick={() => { setActiveView(item.id); setSelectedEmail(null); }}
            >
              <span className="mobile-nav-icon">{item.icon}</span>
              {item.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
