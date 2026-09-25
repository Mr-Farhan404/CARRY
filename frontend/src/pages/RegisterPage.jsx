import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Input, Card } from '../components/common';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    studentId: '',
    email: '',
    phone: '',
    department: '',
    password: '',
    confirmPassword: '',
  });

  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (globalError) setGlobalError('');
  };

  const validate = () => {
    const newErrors = {};

    if (!formData.fullName.trim()) {
      newErrors.fullName = 'Full name is required';
    }

    if (!formData.studentId.trim()) {
      newErrors.studentId = 'Student ID is required';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'University email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      newErrors.email = 'Please enter a valid email address';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters long';
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError('');
    setErrors({});

    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        fullName: formData.fullName.trim(),
        studentId: formData.studentId.trim(),
        email: formData.email.trim().toLowerCase(),
        phone: formData.phone.trim() || null,
        department: formData.department.trim() || null,
        password: formData.password,
      };

      await register(payload);

      // Redirect to login page with success notification
      navigate('/login', {
        state: {
          message: 'Account created successfully! Please sign in with your email and password.',
        },
      });
    } catch (err) {
      if (err.status === 409) {
        setGlobalError(err.message || 'An account with this email or student ID already exists.');
      } else if (err.fields) {
        setErrors(err.fields);
      } else {
        setGlobalError(err.message || 'Registration failed. Please check your information.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <Card
        title="Create Student Account"
        subtitle="Join the KUET campus carpooling and delivery community"
        className="auth-card auth-card--wide"
      >
        {globalError && (
          <div className="ui-alert ui-alert--error" role="alert">
            {globalError}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <div className="form-grid">
            <Input
              label="Full Name"
              id="reg-fullname"
              name="fullName"
              type="text"
              value={formData.fullName}
              onChange={handleChange}
              placeholder="e.g. Farhan Israq"
              error={errors.fullName}
              required
              autoFocus
            />

            <Input
              label="Student ID"
              id="reg-studentid"
              name="studentId"
              type="text"
              value={formData.studentId}
              onChange={handleChange}
              placeholder="e.g. 1907001"
              error={errors.studentId}
              required
            />
          </div>

          <div className="form-grid">
            <Input
              label="University Email"
              id="reg-email"
              name="email"
              type="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="student@stud.kuet.ac.bd"
              error={errors.email}
              required
              autoComplete="email"
            />

            <Input
              label="Phone Number"
              id="reg-phone"
              name="phone"
              type="tel"
              value={formData.phone}
              onChange={handleChange}
              placeholder="017XXXXXXXX (optional)"
              error={errors.phone}
              autoComplete="tel"
            />
          </div>

          <Input
            label="Department"
            id="reg-department"
            name="department"
            type="text"
            value={formData.department}
            onChange={handleChange}
            placeholder="e.g. CSE, EEE, ME (optional)"
            error={errors.department}
          />

          <div className="form-grid">
            <Input
              label="Password"
              id="reg-password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="At least 8 characters"
              error={errors.password}
              helperText="Minimum 8 characters"
              required
              autoComplete="new-password"
            />

            <Input
              label="Confirm Password"
              id="reg-confirmpassword"
              name="confirmPassword"
              type="password"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Re-enter your password"
              error={errors.confirmPassword}
              required
              autoComplete="new-password"
            />
          </div>

          <Button
            type="submit"
            variant="primary"
            size="lg"
            isLoading={isSubmitting}
            className="auth-submit-btn"
          >
            Create Account
          </Button>
        </form>

        <div className="auth-footer">
          <p>
            Already have an account?{' '}
            <Link to="/login" className="auth-link">
              Sign in here
            </Link>
          </p>
        </div>
      </Card>
    </div>
  );
}
