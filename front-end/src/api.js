import axios from 'axios';

const RAW_URL = import.meta.env.VITE_API_BASE_URL;
const API_BASE = RAW_URL ? `${RAW_URL.replace(/\/$/, '')}/api` : '/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── Tasks ──
export const getAllTasks = () => api.get('/tasks');
export const getTasksByStatus = (status) => api.get(`/tasks?status=${status}`);
export const getTasksByPriority = (priority) => api.get(`/tasks?priority=${priority}`);
export const getTaskById = (id) => api.get(`/tasks/${id}`);
export const updateTaskStatus = (id, status) => api.patch(`/tasks/${id}/status`, { status });
export const updateTaskPriority = (id, priority) => api.patch(`/tasks/${id}/priority`, { priority });
export const deleteTask = (id) => api.delete(`/tasks/${id}`);
export const getDashboardStats = () => api.get('/tasks/stats');

// ── Emails ──
export const getAllEmails = () => api.get('/emails');
export const getEmailById = (id) => api.get(`/emails/${id}`);
export const submitEmail = (emailData) => api.post('/emails', emailData);
export const processEmails = () => api.post('/emails/process');

// ── Email Accounts ──
export const getAllAccounts = () => api.get('/accounts');
export const addAccount = (data) => api.post('/accounts', data);
export const testAccountConnection = (data) => api.post('/accounts/test', data);
export const fetchEmailsNow = () => api.post('/accounts/fetch');
export const toggleAccount = (id) => api.patch(`/accounts/${id}/toggle`);
export const deleteAccount = (id) => api.delete(`/accounts/${id}`);

// ── Send Email ──
export const sendEmail = (data) => api.post('/emails/send', data);

export default api;
