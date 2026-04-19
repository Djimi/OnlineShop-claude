import { Link, useNavigate } from 'react-router';
import { useAuthStore } from '../../store/authStore';

export function Navbar() {
  const navigate = useNavigate();
  const { isAuthenticated, username, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="grid grid-cols-[1fr_auto_1fr] items-center px-6 md:px-16 py-8 border-b border-[#dcd5c7]">
      <Link to="/" className="justify-self-start">
        <span className="font-display text-2xl tracking-[0.01em]">
          Online<em className="italic text-[#7a3b2c]">shop</em>
        </span>
      </Link>

      <div className="hidden md:flex gap-10 justify-self-center">
        {isAuthenticated ? (
          <>
            <Link to="/items" className="nav-link">Shop</Link>
            <span className="nav-link">Journal</span>
            <span className="nav-link">About</span>
          </>
        ) : (
          <>
            <span className="nav-link">Shop</span>
            <span className="nav-link">Journal</span>
            <span className="nav-link">About</span>
          </>
        )}
      </div>

      <div className="flex gap-6 items-center justify-self-end">
        {isAuthenticated ? (
          <>
            <span className="hidden sm:inline font-display italic text-[#5b524a] text-base">
              Welcome,&nbsp;<span className="text-[#1f1a14]">{username}</span>
            </span>
            <button onClick={handleLogout} className="nav-link hover:text-[#7a3b2c]">
              Sign&nbsp;out
            </button>
          </>
        ) : (
          <>
            <button onClick={() => navigate('/login')} className="btn btn-primary px-5 py-3">
              Sign&nbsp;in
            </button>
            <button onClick={() => navigate('/register')} className="btn btn-primary px-5 py-3">
              Create&nbsp;account
            </button>
          </>
        )}
      </div>
    </nav>
  );
}
