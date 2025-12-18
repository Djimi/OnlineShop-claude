import { Link, useNavigate } from 'react-router';
import { useAuthStore } from '../../store/authStore';
import { Button } from '../common/Button';
import { LogOut, ShoppingBag } from 'lucide-react';

export function Navbar() {
  const navigate = useNavigate();
  const { isAuthenticated, username, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="sticky top-0 z-50 bg-white shadow-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo/Brand */}
          <Link to="/" className="flex items-center gap-2 group">
            <ShoppingBag className="w-6 h-6 text-blue-600 group-hover:text-blue-700 transition-colors" />
            <span className="text-xl font-bold text-gray-900 group-hover:text-blue-600 transition-colors">
              OnlineShop
            </span>
          </Link>

          {/* Navigation Links */}
          <div className="hidden md:flex items-center gap-8">
            {isAuthenticated && (
              <>
                <Link
                  to="/items"
                  className="text-gray-700 hover:text-blue-600 font-medium transition-colors"
                >
                  Catalog
                </Link>
                <span className="text-gray-400">•</span>
                <span className="text-gray-700 font-medium">
                  Welcome, <span className="text-blue-600">{username}</span>
                </span>
              </>
            )}
          </div>

          {/* Auth Buttons */}
          <div className="flex items-center gap-3">
            {!isAuthenticated ? (
              <>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => navigate('/login')}
                >
                  Login
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => navigate('/register')}
                >
                  Register
                </Button>
              </>
            ) : (
              <Button
                variant="danger"
                size="sm"
                onClick={handleLogout}
                className="flex items-center gap-2"
              >
                <LogOut className="w-4 h-4" />
                <span className="hidden sm:inline">Logout</span>
              </Button>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
