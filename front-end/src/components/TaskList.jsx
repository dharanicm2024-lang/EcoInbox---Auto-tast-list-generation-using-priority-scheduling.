import { useState } from 'react';
import { updateTaskStatus, updateTaskPriority, deleteTask } from '../api';

const PRIORITY_EMOJI = { HIGH: '🔴', MEDIUM: '🟡', LOW: '🟢' };

export default function TaskList({ tasks, onRefresh, showToast }) {
  const [filter, setFilter] = useState('ALL');

  const filteredTasks = tasks.filter((t) => {
    if (filter === 'ALL') return true;
    if (filter === 'PENDING') return t.status === 'PENDING';
    if (filter === 'DONE') return t.status === 'DONE';
    if (filter === 'HIGH' || filter === 'MEDIUM' || filter === 'LOW') return t.priority === filter;
    return true;
  });

  const handleToggleDone = async (task) => {
    try {
      const newStatus = task.status === 'DONE' ? 'PENDING' : 'DONE';
      await updateTaskStatus(task.id, newStatus);
      showToast(newStatus === 'DONE' ? 'Task completed!' : 'Task reopened');
      onRefresh();
    } catch {
      showToast('Failed to update task', 'error');
    }
  };

  const handleCyclePriority = async (task) => {
    const cycle = { HIGH: 'MEDIUM', MEDIUM: 'LOW', LOW: 'HIGH' };
    try {
      await updateTaskPriority(task.id, cycle[task.priority]);
      showToast('Priority updated');
      onRefresh();
    } catch {
      showToast('Failed to update priority', 'error');
    }
  };

  const handleDelete = async (task) => {
    try {
      await deleteTask(task.id);
      showToast('Task deleted');
      onRefresh();
    } catch {
      showToast('Failed to delete task', 'error');
    }
  };

  return (
    <div>
      <div className="filters">
        {['ALL', 'PENDING', 'DONE', 'HIGH', 'MEDIUM', 'LOW'].map((f) => (
          <button
            key={f}
            className={`filter-btn ${filter === f ? 'active' : ''}`}
            onClick={() => setFilter(f)}
          >
            {f}
          </button>
        ))}
      </div>

      {filteredTasks.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📭</div>
          <p>No tasks found. Submit an email to extract tasks!</p>
        </div>
      ) : (
        <div className="task-list">
          {filteredTasks.map((task) => (
            <div
              key={task.id}
              className={`task-card priority-${task.priority} ${task.status === 'DONE' ? 'done' : ''}`}
            >
              <button
                className={`task-checkbox ${task.status === 'DONE' ? 'checked' : ''}`}
                onClick={() => handleToggleDone(task)}
                title={task.status === 'DONE' ? 'Mark as pending' : 'Mark as done'}
              >
                {task.status === 'DONE' ? '✓' : ''}
              </button>

              <div className="task-content">
                <div className="task-name">{task.taskName}</div>
                <div className="task-meta">
                  <span
                    className={`priority-badge ${task.priority}`}
                    onClick={() => handleCyclePriority(task)}
                    style={{ cursor: 'pointer' }}
                    title="Click to change priority"
                  >
                    {PRIORITY_EMOJI[task.priority]} {task.priority}
                  </span>
                  {task.deadline && <span>📅 {task.deadline}</span>}
                  {task.category && task.category !== 'Task' && (
                    <span className="category-badge">🏷️ {task.category}</span>
                  )}
                  {task.emailSender && <span>👤 {task.emailSender}</span>}
                  {task.emailSubject && <span>📧 {task.emailSubject}</span>}
                </div>
              </div>

              <div className="task-actions">
                <button
                  className="btn-icon danger"
                  onClick={() => handleDelete(task)}
                  title="Delete task"
                >
                  🗑
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
