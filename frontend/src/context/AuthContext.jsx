import { createContext, useContext, useState } from 'react';
import client from '../api/client';

const AuthContext = createContext(null);

// Login yanıtı sadece token döndürür; kullanıcı bilgisi JWT claim'lerinden okunur.
function decodeUser(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const claims = JSON.parse(atob(payload));
    if (claims.exp * 1000 < Date.now()) return null;
    return { id: Number(claims.sub), email: claims.email, role: claims.role };
  } catch {
    return null;
  }
}

function initialToken() {
  const token = localStorage.getItem('token');
  if (token && decodeUser(token)) return token;
  localStorage.removeItem('token');
  return null;
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(initialToken);
  const user = token ? decodeUser(token) : null;

  async function login(email, password) {
    const { data } = await client.post('/api/auth/login', { email, password });
    localStorage.setItem('token', data.token);
    setToken(data.token);
  }

  function logout() {
    localStorage.removeItem('token');
    setToken(null);
  }

  return (
    <AuthContext.Provider value={{ token, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
