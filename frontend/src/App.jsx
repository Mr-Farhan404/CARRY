import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import Layout from './components/layout/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import HomePage from './pages/HomePage';
import ShopPage from './pages/ShopPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import MyRequestsPage from './pages/MyRequestsPage';
import NewRequestPage from './pages/NewRequestPage';
import RequestDetailPage from './pages/RequestDetailPage';
import PostTripPage from './pages/PostTripPage';
import MyTripsPage from './pages/MyTripsPage';
import AvailableRequestsPage from './pages/AvailableRequestsPage';
import MyDeliveriesPage from './pages/MyDeliveriesPage';
import AdminPage from './pages/AdminPage';
import AdminRoute from './components/AdminRoute';

function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <Routes>
          <Route path="/" element={<Layout />}>
            {/* Public Routes */}
            <Route index element={<HomePage />} />
            <Route path="shop" element={<ShopPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />

          {/* Protected Customer Routes */}
          <Route
            path="requests"
            element={
              <ProtectedRoute>
                <MyRequestsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="requests/new"
            element={
              <ProtectedRoute>
                <NewRequestPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="requests/:id"
            element={
              <ProtectedRoute>
                <RequestDetailPage />
              </ProtectedRoute>
            }
          />

          {/* Protected Delivery Partner Routes */}
          <Route
            path="trips"
            element={
              <ProtectedRoute>
                <MyTripsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="trips/new"
            element={
              <ProtectedRoute>
                <PostTripPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="requests/available"
            element={
              <ProtectedRoute>
                <AvailableRequestsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="deliveries"
            element={
              <ProtectedRoute>
                <MyDeliveriesPage />
              </ProtectedRoute>
            }
          />

          {/* Protected Admin Route (Requires Authentication + isAdmin === true) */}
          <Route
            path="admin"
            element={
              <AdminRoute>
                <AdminPage />
              </AdminRoute>
            }
          />

          {/* Catch-all redirect */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
        </Routes>
      </CartProvider>
    </AuthProvider>
  );
}

export default App;
