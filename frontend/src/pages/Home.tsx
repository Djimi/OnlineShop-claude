import { useNavigate } from 'react-router';
import { Button } from '../components/common/Button';
import { ShoppingBag, Zap, Shield, Truck } from 'lucide-react';

export default function Home() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      {/* Hero Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="grid md:grid-cols-2 gap-12 items-center">
          {/* Left Column */}
          <div>
            <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6 leading-tight">
              Welcome to <span className="text-blue-600">OnlineShop</span>
            </h1>
            <p className="text-xl text-gray-700 mb-8 leading-relaxed">
              Discover a world of quality products at unbeatable prices. Shop your favorite items with confidence and ease.
            </p>

            {/* CTA Buttons */}
            <div className="flex flex-col sm:flex-row gap-4">
              <Button
                variant="primary"
                size="lg"
                onClick={() => navigate('/register')}
                className="text-lg"
              >
                Get Started
              </Button>
              <Button
                variant="secondary"
                size="lg"
                onClick={() => navigate('/login')}
                className="text-lg"
              >
                Sign In
              </Button>
            </div>
          </div>

          {/* Right Column - Feature Icon */}
          <div className="hidden md:flex items-center justify-center">
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-r from-blue-400 to-blue-600 rounded-full blur-3xl opacity-20"></div>
              <ShoppingBag className="w-64 h-64 text-blue-600 relative" />
            </div>
          </div>
        </div>
      </div>

      {/* Features Section */}
      <div className="bg-white py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-4xl font-bold text-center text-gray-900 mb-16">
            Why Choose OnlineShop?
          </h2>

          <div className="grid md:grid-cols-3 gap-8">
            {/* Feature 1 */}
            <div className="text-center">
              <div className="flex justify-center mb-4">
                <div className="p-3 bg-blue-100 rounded-lg">
                  <Zap className="w-8 h-8 text-blue-600" />
                </div>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                Lightning Fast
              </h3>
              <p className="text-gray-600">
                Browse and shop with blazing-fast performance and instant checkout.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="text-center">
              <div className="flex justify-center mb-4">
                <div className="p-3 bg-green-100 rounded-lg">
                  <Shield className="w-8 h-8 text-green-600" />
                </div>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                Secure & Safe
              </h3>
              <p className="text-gray-600">
                Your data is protected with industry-leading security measures.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="text-center">
              <div className="flex justify-center mb-4">
                <div className="p-3 bg-orange-100 rounded-lg">
                  <Truck className="w-8 h-8 text-orange-600" />
                </div>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                Fast Shipping
              </h3>
              <p className="text-gray-600">
                Get your orders delivered quickly to your doorstep.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* CTA Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="bg-gradient-to-r from-blue-600 to-blue-700 rounded-lg shadow-xl p-12 text-center">
          <h2 className="text-4xl font-bold text-white mb-4">
            Ready to Shop?
          </h2>
          <p className="text-xl text-blue-100 mb-8">
            Sign up now or log in to your account to start browsing our products.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Button
              variant="secondary"
              size="lg"
              onClick={() => navigate('/register')}
            >
              Create Account
            </Button>
            <Button
              variant="primary"
              size="lg"
              onClick={() => navigate('/login')}
              className="bg-white text-blue-600 hover:bg-gray-100"
            >
              Log In
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
