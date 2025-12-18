import { BrowserRouter } from 'react-router';
import { Navbar } from './components/layout/Navbar';
import { AppRoutes } from './routes';
import { Toaster } from 'react-hot-toast';
import { useEffect } from 'react';
import { useAuthStore } from './store/authStore';

function App() {
  useEffect(() => {
    useAuthStore.getState().loadFromStorage();
  }, []);

  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <AppRoutes />
        <Toaster position="top-right" />
      </div>
    </BrowserRouter>
  );
}

export default App;
