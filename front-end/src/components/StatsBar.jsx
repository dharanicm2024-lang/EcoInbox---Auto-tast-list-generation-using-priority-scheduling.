export default function StatsBar({ stats }) {
  return (
    <div className="stats-grid">
      <div className="stat-card stat-total">
        <div className="stat-value">{stats.totalTasks}</div>
        <div className="stat-label">Total Tasks</div>
      </div>
      <div className="stat-card stat-high">
        <div className="stat-value">{stats.highPriorityTasks}</div>
        <div className="stat-label">High Priority</div>
      </div>
      <div className="stat-card stat-medium">
        <div className="stat-value">{stats.mediumPriorityTasks}</div>
        <div className="stat-label">Medium</div>
      </div>
      <div className="stat-card stat-low">
        <div className="stat-value">{stats.lowPriorityTasks}</div>
        <div className="stat-label">Low</div>
      </div>
      <div className="stat-card stat-done">
        <div className="stat-value">{stats.completedTasks}</div>
        <div className="stat-label">Completed</div>
      </div>
      <div className="stat-card">
        <div className="stat-value" style={{ color: 'var(--accent)' }}>{stats.totalEmails}</div>
        <div className="stat-label">Emails</div>
      </div>
    </div>
  );
}
