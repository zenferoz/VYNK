import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { WelcomePage } from '../pages/WelcomePage';
import * as AuthContextModule from '../context/AuthContext';

// Mock useAuth
vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

describe('VYNK 2.0 Landing Page (WelcomePage)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Content and Section Structure', () => {
    beforeEach(() => {
      AuthContextModule.useAuth.mockReturnValue({
        isAuthenticated: false,
        isOnboarded: false,
      });
    });

    it('renders the editorial hero headline and core messaging', () => {
      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      // Hero Headline
      expect(screen.getByRole('heading', { level: 1 })).toBeDefined();
      expect(screen.getByText(/FIND YOUR PEOPLE\./i)).toBeDefined();
      expect(screen.getByText(/MAKE YOUR MOVE\./i)).toBeDefined();

      // Campus badge pill
      expect(screen.getByText(/Campus Verified Network • Institutional Access/i)).toBeDefined();

      // Subhead
      expect(
        screen.getByText(/VYNK is an academic and professional networking platform designed for college-student networking through institutional email verification/i)
      ).toBeDefined();
    });

    it('integrates both main messaging and the connection preview inside the split hero', () => {
      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      const heroSection = document.querySelector('.landing-hero');
      expect(heroSection).not.toBeNull();

      const heroGrid = heroSection.querySelector('.landing-hero-grid');
      expect(heroGrid).not.toBeNull();

      const heroContent = heroSection.querySelector('.landing-hero-content');
      expect(heroContent).not.toBeNull();
      expect(heroContent.textContent).toContain('FIND YOUR PEOPLE.');
      expect(heroContent.textContent).toContain('MAKE YOUR MOVE.');

      const previewWrapper = heroSection.querySelector('.landing-preview-wrapper');
      expect(previewWrapper).not.toBeNull();
      expect(previewWrapper.textContent).toContain('Priya Sharma');
      expect(previewWrapper.textContent).toContain('Illustrative preview of a purpose-matched student connection card.');
    });

    it('renders the illustrative connection preview card with disclaimer', () => {
      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      // Card details
      expect(screen.getByText('Priya Sharma')).toBeDefined();
      expect(screen.getByText(/Computer Science & Engineering • Year 3/i)).toBeDefined();
      expect(screen.getByText('Shared Purpose Match')).toBeDefined();
      expect(screen.getByText('Hackathon Team')).toBeDefined();
      expect(screen.getByText('PyTorch')).toBeDefined();
      expect(screen.getByText('React')).toBeDefined();
      expect(screen.getByText('System Design')).toBeDefined();

      // Clear non-misleading disclaimer
      expect(
        screen.getByText(/Illustrative preview of a purpose-matched student connection card\./i)
      ).toBeDefined();
    });

    it('renders the differentiator strip items', () => {
      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      expect(screen.getByText('Institutional Email Verification')).toBeDefined();
      expect(screen.getByText('Campus-Restricted Discovery')).toBeDefined();
      expect(screen.getByText('Purpose-Focused Networking')).toBeDefined();
    });

    it('renders the three core architectural pillars with capability-based phrasing', () => {
      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      expect(screen.getByText('Why VYNK Hits Different')).toBeDefined();
      expect(screen.getByText('Purpose-Driven Matching')).toBeDefined();
      expect(screen.getByText('Campus-Focused Privacy')).toBeDefined();
      expect(screen.getByText('Student Safety Controls')).toBeDefined();
    });

    it('avoids unsupported absolute guarantees and provides accurate capability language', () => {
      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      const bodyText = document.body.textContent;
      expect(bodyText).not.toContain('data leakage');
      expect(bodyText).not.toContain('zero data');
      expect(bodyText).not.toContain('spam prevention');
      expect(bodyText).not.toContain('100%');

      expect(
        screen.getByText(/Privacy-conscious networking with clear connection boundaries/i)
      ).toBeDefined();
      expect(
        screen.getByText(/Equipped with blocking, reporting tools, and connection request limits/i)
      ).toBeDefined();
    });

    it('renders the three-step workflow', () => {
      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      expect(screen.getByText('How It Works')).toBeDefined();
      expect(screen.getByText('01')).toBeDefined();
      expect(screen.getByText('Verify Your Campus Email')).toBeDefined();
      expect(screen.getByText('02')).toBeDefined();
      expect(screen.getByText('Declare Goals & Skills')).toBeDefined();
      expect(screen.getByText('03')).toBeDefined();
      expect(screen.getByText('Connect With Purpose')).toBeDefined();
    });

    it('renders the student scenarios section', () => {
      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      expect(screen.getByText('Built For Real Student Needs')).toBeDefined();
      expect(screen.getByText('Hackathon Squad Assembly')).toBeDefined();
      expect(screen.getByText('Placement Preparation')).toBeDefined();
      expect(screen.getByText('Research & Study Partnerships')).toBeDefined();
    });

    it('renders the final high-impact CTA banner with dedicated secondary button styling hook', () => {
      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      expect(screen.getByText('READY TO ELEVATE YOUR CAMPUS CIRCLE?')).toBeDefined();
      expect(
        screen.getByText(/Join verified peers within your college community/i)
      ).toBeDefined();

      const banner = document.querySelector('.landing-cta-banner');
      expect(banner).not.toBeNull();

      const secondaryBtn = banner.querySelector('.landing-banner-btn-secondary');
      expect(secondaryBtn).not.toBeNull();
      expect(secondaryBtn.getAttribute('href')).toBe('/login');
      expect(secondaryBtn.textContent.trim()).toBe('Sign In');
    });
  });

  describe('Authentication-Aware CTA Routing', () => {
    it('renders links to /register and /login for unauthenticated visitors in both hero and final banner', () => {
      AuthContextModule.useAuth.mockReturnValue({
        isAuthenticated: false,
        isOnboarded: false,
      });

      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      const registerLinks = screen.getAllByRole('link', { name: /Join With College Email/i });
      expect(registerLinks.length).toBeGreaterThanOrEqual(2);
      registerLinks.forEach((link) => {
        expect(link.getAttribute('href')).toBe('/register');
      });

      const loginLinks = screen.getAllByRole('link', { name: /Sign In/i });
      expect(loginLinks.length).toBeGreaterThanOrEqual(2);
      loginLinks.forEach((link) => {
        expect(link.getAttribute('href')).toBe('/login');
      });
    });

    it('renders link to /dashboard for authenticated and onboarded students', () => {
      AuthContextModule.useAuth.mockReturnValue({
        isAuthenticated: true,
        isOnboarded: true,
      });

      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      const dashboardLinks = screen.getAllByRole('link', { name: /Go to Your Dashboard/i });
      expect(dashboardLinks.length).toBeGreaterThanOrEqual(2);
      dashboardLinks.forEach((link) => {
        expect(link.getAttribute('href')).toBe('/dashboard');
      });

      expect(screen.queryByRole('link', { name: /Join With College Email/i })).toBeNull();
      expect(screen.queryByRole('link', { name: /Sign In/i })).toBeNull();
    });

    it('renders link to /onboarding for authenticated users with incomplete onboarding', () => {
      AuthContextModule.useAuth.mockReturnValue({
        isAuthenticated: true,
        isOnboarded: false,
      });

      render(
        <MemoryRouter>
          <WelcomePage />
        </MemoryRouter>
      );

      const onboardingLinks = screen.getAllByRole('link', { name: /Complete Onboarding/i });
      expect(onboardingLinks.length).toBeGreaterThanOrEqual(2);
      onboardingLinks.forEach((link) => {
        expect(link.getAttribute('href')).toBe('/onboarding');
      });

      expect(screen.queryByRole('link', { name: /Join With College Email/i })).toBeNull();
      expect(screen.queryByRole('link', { name: /Sign In/i })).toBeNull();
    });
  });
});
