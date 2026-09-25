import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

export default function Layout() {
  return (
    <div className="app-layout">
      <Navbar />
      <main className="app-main">
        <div className="app-container">
          <Outlet />
        </div>
      </main>
      <footer className="app-footer">
        <p>© {new Date().getFullYear()} CARRY — KUET Campus Delivery & Carpooling</p>
      </footer>
    </div>
  );
}
