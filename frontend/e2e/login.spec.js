import { test, expect } from '@playwright/test';

const DEMO_ACCOUNTS = [
  { role: 'Admin', email: 'admin@medisphere.com', expectedRoute: '/dashboard' },
  { role: 'Doctor', email: 'doctor@medisphere.com', expectedRoute: '/doctor' },
  { role: 'Receptionist', email: 'receptionist@medisphere.com', expectedRoute: '/frontdesk' },
  { role: 'Nurse', email: 'nurse@medisphere.com', expectedRoute: '/ipd' },
  { role: 'Pharmacist', email: 'pharmacist@medisphere.com', expectedRoute: '/pharmacy' },
  { role: 'Lab Tech', email: 'labtech@medisphere.com', expectedRoute: '/lab' },
  { role: 'Patient', email: 'patient@medisphere.com', expectedRoute: '/patient' },
];

test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('text=Welcome back')).toBeVisible();
  });

  for (const account of DEMO_ACCOUNTS) {
    test(`${account.role} can login and reach correct dashboard`, async ({ page }) => {
      await page.fill('input[type="email"]', account.email);
      await page.fill('input[type="password"]', 'password123');
      await page.click('button[type="submit"]');

      await page.waitForURL(`**${account.expectedRoute}**`, { timeout: 10000 });
      expect(page.url()).toContain(account.expectedRoute);
    });
  }

  test('shows error on invalid credentials', async ({ page }) => {
    await page.fill('input[type="email"]', 'wrong@email.com');
    await page.fill('input[type="password"]', 'badpassword');
    await page.click('button[type="submit"]');

    await expect(page.locator('text=Invalid credentials')).toBeVisible({ timeout: 5000 });
  });

  test('demo account quick-fill works', async ({ page }) => {
    await page.click('text=Hospital Admin');
    const emailInput = page.locator('input[type="email"]');
    await expect(emailInput).toHaveValue('admin@medisphere.com');
  });
});

test.describe('Navigation Guards', () => {
  test('redirects unauthenticated user to login', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/);
  });
});
