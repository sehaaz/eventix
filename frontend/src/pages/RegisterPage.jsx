import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import client, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '' });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  function update(field) {
    return (e) => setForm({ ...form, [field]: e.target.value });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await client.post('/api/auth/register', form);
      await login(form.email, form.password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-sm rounded-xl border border-stone-200 bg-white p-6">
      <h1 className="mb-6 text-xl font-semibold text-stone-900">Kayıt ol</h1>
      <form onSubmit={handleSubmit} className="space-y-4">
        <input
          required
          placeholder="Ad soyad"
          value={form.fullName}
          onChange={update('fullName')}
          className="w-full rounded-md border border-stone-300 px-3 py-2 focus:border-teal-500 focus:outline-none"
        />
        <input
          type="email"
          required
          placeholder="E-posta"
          value={form.email}
          onChange={update('email')}
          className="w-full rounded-md border border-stone-300 px-3 py-2 focus:border-teal-500 focus:outline-none"
        />
        <input
          type="password"
          required
          minLength={8}
          maxLength={72}
          placeholder="Şifre (en az 8 karakter)"
          value={form.password}
          onChange={update('password')}
          className="w-full rounded-md border border-stone-300 px-3 py-2 focus:border-teal-500 focus:outline-none"
        />
        {error && <p className="text-sm text-rose-600">{error}</p>}
        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-teal-600 py-2 text-white hover:bg-teal-700 disabled:opacity-50"
        >
          Kayıt ol
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-stone-500">
        Zaten hesabın var mı?{' '}
        <Link to="/login" className="text-teal-700 hover:underline">
          Giriş yap
        </Link>
      </p>
    </div>
  );
}
