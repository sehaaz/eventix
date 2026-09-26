import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();

  return (
    <header className="border-b border-stone-200 bg-white">
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
        <Link to="/" className="text-lg font-semibold text-teal-700">
          EventTix
        </Link>
        <div className="flex items-center gap-4 text-sm">
          {user ? (
            <>
              {user.role === 'ADMIN' && (
                <Link to="/admin/events" className="text-stone-600 hover:text-stone-900">
                  Yönetim
                </Link>
              )}
              <Link to="/tickets" className="text-stone-600 hover:text-stone-900">
                Biletlerim
              </Link>
              <span className="text-stone-500">{user.email}</span>
              <button onClick={logout} className="text-stone-600 hover:text-stone-900">
                Çıkış
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-stone-600 hover:text-stone-900">
                Giriş
              </Link>
              <Link
                to="/register"
                className="rounded-md bg-teal-600 px-3 py-1.5 text-white hover:bg-teal-700"
              >
                Kayıt ol
              </Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
