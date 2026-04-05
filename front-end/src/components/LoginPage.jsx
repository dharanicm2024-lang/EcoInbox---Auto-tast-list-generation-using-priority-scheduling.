import { useState } from 'react';
import { testAccountConnection, addAccount } from '../api';
import { Leaf, Mail, Lock, Eye, EyeOff, ArrowRight } from 'lucide-react';

const GOOGLE_WORKSPACE_SUFFIXES = ['edu.in', 'edu', 'ac.in', 'org', 'org.in'];

function isGoogleDomain(domain) {
  if (domain === 'gmail.com' || domain === 'googlemail.com') return true;
  for (const suffix of GOOGLE_WORKSPACE_SUFFIXES) {
    if (domain.endsWith('.' + suffix)) return true;
  }
  return false;
}

function getProviderInfo(email) {
  if (!email || !email.includes('@')) return null;
  const domain = email.split('@')[1]?.toLowerCase();
  if (!domain) return null;

  if (isGoogleDomain(domain)) {
    return {
      name: 'Google',
      hint: 'Use a Google App Password (not your regular password).\nMake sure IMAP is enabled in your Gmail settings (Settings → Forwarding and POP/IMAP).\nThen go to myaccount.google.com → Security → 2-Step Verification → App passwords.',
      imapHost: 'imap.gmail.com',
    };
  }
  if (['outlook.com', 'hotmail.com', 'live.com'].includes(domain)) {
    return { name: 'Microsoft', hint: 'Use your regular password or an App password if 2FA is enabled.', imapHost: 'outlook.office365.com' };
  }
  if (domain === 'yahoo.com') {
    return { name: 'Yahoo', hint: 'Generate an App password at login.yahoo.com → Account Security.', imapHost: 'imap.mail.yahoo.com' };
  }
  return null;
}

export default function LoginPage({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingMsg, setLoadingMsg] = useState('Connecting...');
  const [error, setError] = useState('');
  const [provider, setProvider] = useState(null);
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);

  const handleEmailChange = (e) => {
    const val = e.target.value;
    setEmail(val);
    setError('');
    setProvider(getProviderInfo(val));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please enter both email and app password.');
      return;
    }

    setLoading(true);
    setError('');

    // Demo bypass for UI testing
    if (email === 'demo@ecoinbox.com' && password === 'demo123') {
      setTimeout(() => {
        setLoadingMsg('Syncing your inbox...');
        onLogin({ email });
      }, 1500);
      return;
    }

    try {
      // Test connection first
      const testRes = await testAccountConnection({ email, appPassword: password });
      if (!testRes.data.success) {
        setError(testRes.data.message);
        setLoading(false);
        return;
      }

      // Connection works — add the account
      try {
        await addAccount({ email, appPassword: password });
      } catch (err) {
        // Account may already exist — that's fine
        if (!err.response?.data?.error?.includes('already exists')) {
          throw err;
        }
      }

      // Success — pass account info to parent and wait for it to finish loading
      setLoadingMsg('Syncing your inbox...');
      await onLogin({ email });
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Connection failed. Please check your credentials.';
      setError(msg);
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-split-container">
        
        {/* Left Branding Pane */}
        <div className="login-left-pane">
          <div className="login-branding">
            <h1><Leaf size={48} color="white" /> EcoInbox</h1>
            <p>Organize Smarter, Live Greener</p>
          </div>
          
          <div className="login-eco-tip">
            <h3><Leaf size={16} /> Eco Tip of the Day</h3>
            <p>Did you know? Deleting 10 old emails can save up to 39MB of storage space, reducing the energy needed by data centers.</p>
          </div>
        </div>

        {/* Right Form Pane */}
        <div className="login-right-pane">
          <div className="login-glass-card">
            <h2>Welcome Back</h2>
            <p className="subtitle">Sign in to manage tasks. Try <strong>demo@ecoinbox.com</strong> / <strong>demo123</strong> for a tour!</p>

            {provider && (
              <div className="login-hint" role="alert">
                💡 <strong>{provider.name} detected:</strong> {provider.hint}
              </div>
            )}

            {error && (
              <div className="login-error" role="alert">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit}>
              <div className="form-group-icon">
                <label htmlFor="email">Email Address</label>
                <div className="input-icon-wrapper">
                  <Mail className="icon-left" size={20} />
                  <input
                    id="email"
                    type="email"
                    value={email}
                    onChange={handleEmailChange}
                    placeholder="you@gmail.com"
                    autoFocus
                    required
                    aria-label="Email Address"
                  />
                </div>
              </div>

              <div className="form-group-icon">
                <label htmlFor="password">App Password</label>
                <div className="input-icon-wrapper">
                  <Lock className="icon-left" size={20} />
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => { setPassword(e.target.value); setError(''); }}
                    placeholder="Your app-specific password"
                    required
                    aria-label="App Password"
                  />
                  <button 
                    type="button" 
                    className="icon-right" 
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? "Hide password" : "Show password"}
                  >
                    {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                  </button>
                </div>
              </div>

              <div className="login-options">
                <label>
                  <input 
                    type="checkbox" 
                    checked={rememberMe} 
                    onChange={(e) => setRememberMe(e.target.checked)} 
                  />
                  Remember Me
                </label>
                <a href="#" className="forgot-link" onClick={(e) => e.preventDefault()}>
                  Forgot Password?
                </a>
              </div>

              <button type="submit" className="btn-eco" disabled={loading}>
                {loading ? (
                  <>
                    <Leaf className="spinner-eco" size={20} />
                    {loadingMsg}
                  </>
                ) : (
                  <>
                    Login <ArrowRight size={20} />
                  </>
                )}
              </button>
            </form>

            <div className="login-footer-links">
              <p>New to EcoInbox? <a href="#" onClick={(e) => e.preventDefault()}>Create Account</a></p>
            </div>
            
          </div>
        </div>
        
      </div>
    </div>
  );
}
