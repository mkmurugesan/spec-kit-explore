/* ── Constants ────────────────────────────────────────────── */
const API_BASE = '/v1/api';

const ENDPOINTS = {
  signup:        API_BASE + '/auth/signup',
  signin:        API_BASE + '/auth/signin',
  resetRequest:  API_BASE + '/auth/password-reset/request',
  resetConfirm:  API_BASE + '/auth/password-reset/confirm',
};

const STORAGE_KEYS = {
  access:  'um_access_token',
  refresh: 'um_refresh_token',
};

/* ── Shared utilities ─────────────────────────────────────── */

function showView(id) {
  document.querySelectorAll('main > section').forEach(s => s.classList.add('hidden'));
  const target = document.getElementById(id);
  if (!target) return;
  target.classList.remove('hidden');
  const form = target.querySelector('form');
  if (form) clearErrors(form);
}

function clearErrors(formEl) {
  const banner = formEl.closest('section').querySelector('.error-banner');
  if (banner) {
    banner.classList.remove('visible', 'success');
    banner.textContent = '';
  }
  formEl.querySelectorAll('.field-error').forEach(el => { el.textContent = ''; });
}

function renderErrors(formEl, apiError) {
  const section = formEl.closest('section');
  const banner = section && section.querySelector('.error-banner');

  if (apiError.fieldErrors) {
    for (const [field, message] of Object.entries(apiError.fieldErrors)) {
      const span = formEl.querySelector(`[data-field="${field}"]`);
      if (span) span.textContent = message;
    }
  }

  if (banner) {
    if (apiError.globalMessage) {
      banner.classList.add('visible');
      banner.classList.remove('success');
      banner.textContent = apiError.globalMessage;
    }
  }
}

function setLoading(formEl, isLoading) {
  const btn = formEl.querySelector('button[type="submit"]');
  if (!btn) return;
  if (isLoading) {
    if (!btn.dataset.label) btn.dataset.label = btn.textContent;
    btn.textContent = 'Loading\u2026';
    btn.disabled = true;
  } else {
    btn.textContent = btn.dataset.label || btn.textContent;
    btn.disabled = false;
  }
}

function parseApiError(body) {
  if (!body) {
    return { fieldErrors: null, globalMessage: 'Something went wrong. Please try again later.' };
  }
  const fieldErrors = (body.details && Object.keys(body.details).length) ? body.details : null;
  const globalMessage = body.message ?? body.error ?? 'Something went wrong. Please try again later.';
  return { fieldErrors, globalMessage };
}

async function apiPost(endpoint, payload) {
  let response;
  let body;
  try {
    response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
  } catch (_networkErr) {
    return {
      ok: false,
      networkError: true,
      apiError: {
        fieldErrors: null,
        globalMessage: 'Could not reach the server. Please check your connection.',
      },
    };
  }

  try {
    body = await response.json();
  } catch (_parseErr) {
    body = null;
  }

  if (response.ok) {
    return { ok: true, data: body };
  }
  return { ok: false, status: response.status, apiError: parseApiError(body) };
}

/* ── User Story 1: Sign-Up ────────────────────────────────── */

async function handleSignup(e) {
  e.preventDefault();
  const form = e.currentTarget;
  clearErrors(form);

  const firstName  = form.elements['firstName'].value.trim();
  const lastName   = form.elements['lastName'].value.trim();
  const email      = form.elements['email'].value.trim();
  const password   = form.elements['password'].value;

  const missing = {};
  if (!firstName) missing.firstName = 'Required';
  if (!lastName)  missing.lastName  = 'Required';
  if (!email)     missing.email     = 'Required';
  if (!password)  missing.password  = 'Required';

  if (Object.keys(missing).length) {
    renderErrors(form, { fieldErrors: missing, globalMessage: 'Please fill in all required fields.' });
    return;
  }

  setLoading(form, true);
  const result = await apiPost(ENDPOINTS.signup, { firstName, lastName, email, password });
  setLoading(form, false);

  if (result.ok) {
    const section = form.closest('section');
    const banner = section && section.querySelector('.error-banner');
    if (banner) {
      banner.classList.add('visible', 'success');
      banner.textContent = 'Account created successfully!';
    }
    setTimeout(() => showView('view-signin'), 1500);
  } else {
    renderErrors(form, result.apiError);
  }
}

/* ── User Story 2: Sign-In ────────────────────────────────── */

async function handleSignin(e) {
  e.preventDefault();
  const form = e.currentTarget;
  clearErrors(form);

  const email    = form.elements['email'].value.trim();
  const password = form.elements['password'].value;

  const missing = {};
  if (!email)    missing.email    = 'Required';
  if (!password) missing.password = 'Required';

  if (Object.keys(missing).length) {
    renderErrors(form, { fieldErrors: missing, globalMessage: 'Please fill in all required fields.' });
    return;
  }

  setLoading(form, true);
  const result = await apiPost(ENDPOINTS.signin, { email, password });
  setLoading(form, false);

  if (result.ok) {
    sessionStorage.setItem(STORAGE_KEYS.access,  result.data.accessToken);
    sessionStorage.setItem(STORAGE_KEYS.refresh, result.data.refreshToken);

    const section = form.closest('section');
    const banner = section && section.querySelector('.error-banner');
    if (banner) {
      banner.classList.add('visible', 'success');
      banner.textContent = `Welcome! You are signed in as ${email}.`;
    }
  } else {
    renderErrors(form, result.apiError);
  }
}

/* ── User Story 3: Forgot Password ───────────────────────── */

async function handleForgotStep1(e) {
  e.preventDefault();
  const form = e.currentTarget;
  clearErrors(form);

  const email = form.elements['email'].value.trim();

  if (!email) {
    renderErrors(form, { fieldErrors: { email: 'Required' }, globalMessage: 'Please enter your email address.' });
    return;
  }

  setLoading(form, true);
  const result = await apiPost(ENDPOINTS.resetRequest, { email });
  setLoading(form, false);

  // Network failure: stay on step 1 and show connection error
  if (result.networkError) {
    renderErrors(form, result.apiError);
    return;
  }

  // Validation error (e.g. 400 with field errors): render inline and stay on step 1
  if (!result.ok && result.apiError.fieldErrors) {
    renderErrors(form, result.apiError);
    return;
  }

  // For any other HTTP response (ok or non-validation error), show generic confirmation
  const section = form.closest('section');
  const banner = section && section.querySelector('.error-banner');
  if (banner) {
    banner.classList.add('visible', 'success');
    banner.textContent = 'If that email is registered, you will receive reset instructions.';
  }

  setTimeout(() => showView('view-forgot-step2'), 2000);
}

async function handleForgotStep2(e) {
  e.preventDefault();
  const form = e.currentTarget;
  clearErrors(form);

  const token       = form.elements['token'].value.trim();
  const newPassword = form.elements['newPassword'].value;

  const missing = {};
  if (!token)       missing.token       = 'Required';
  if (!newPassword) missing.newPassword = 'Required';

  if (Object.keys(missing).length) {
    renderErrors(form, { fieldErrors: missing, globalMessage: 'Please fill in all required fields.' });
    return;
  }

  setLoading(form, true);
  const result = await apiPost(ENDPOINTS.resetConfirm, { token, newPassword });
  setLoading(form, false);

  if (result.ok) {
    const section = form.closest('section');
    const banner = section && section.querySelector('.error-banner');
    if (banner) {
      banner.classList.add('visible', 'success');
      banner.textContent = 'Password reset successfully.';
    }
    setTimeout(() => showView('view-signin'), 1500);
  } else {
    renderErrors(form, result.apiError);
  }
}

/* ── Bootstrap ────────────────────────────────────────────── */

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('form-signin').addEventListener('submit', handleSignin);
  document.getElementById('form-signup').addEventListener('submit', handleSignup);
  document.getElementById('form-forgot-step1').addEventListener('submit', handleForgotStep1);
  document.getElementById('form-forgot-step2').addEventListener('submit', handleForgotStep2);

  // Delegated navigation handler for all [data-view] links
  document.addEventListener('click', e => {
    if (e.target.dataset.view) showView(e.target.dataset.view);
  });
});



