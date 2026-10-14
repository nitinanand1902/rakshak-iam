import { useCallback, useMemo, useState } from 'react';
import { AuthContext } from './authContextObject.js';
import { loginRequest, logoutRequest, completeSsoProvisioning } from '../services/backendApi.js';

const TOKEN_KEY         = 'healthcare_iam_token';
const REFRESH_TOKEN_KEY = 'healthcare_iam_refresh_token';
const USER_KEY          = 'healthcare_iam_user';
// Stored only during the SSO onboarding window (5-minute lifetime)
const PROV_TOKEN_KEY    = 'healthcare_iam_provisioning_token';

function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user,  setUser]  = useState(getStoredUser);
  const [token, setToken] = useState(localStorage.getItem(TOKEN_KEY));
  // provisioningToken is set when the SSO callback indicates a new user
  const [provisioningToken, setProvisioningToken] = useState(
    localStorage.getItem(PROV_TOKEN_KEY)
  );

  // ------------------------------------------------------------------
  // Username/password login (existing flow — unchanged)
  // ------------------------------------------------------------------
  const login = useCallback(async (credentials) => {
    const response = await loginRequest(credentials);
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
    setToken(response.accessToken);
    setUser(response.user);
    return response.user;
  }, []);

  // ------------------------------------------------------------------
  // Called by SsoCallback.jsx when the backend redirect contains a
  // full JWT pair (returning SSO user — no onboarding needed).
  // ------------------------------------------------------------------
  const loginWithSsoTokens = useCallback(({ accessToken, refreshToken, username, role }) => {
    const userObj = {
      username,
      name: username,
      role,
      permissions: [],
    };
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(userObj));
    setToken(accessToken);
    setUser(userObj);
    return userObj;
  }, []);

  // ------------------------------------------------------------------
  // Called by SsoCallback.jsx when the backend redirect contains a
  // PROVISIONING token (new user needs to pick a role).
  // ------------------------------------------------------------------
  const startSsoProvisioning = useCallback((token) => {
    localStorage.setItem(PROV_TOKEN_KEY, token);
    setProvisioningToken(token);
  }, []);

  // ------------------------------------------------------------------
  // Called by SsoOnboarding.jsx after the user picks their role.
  // ------------------------------------------------------------------
  const completeSsoOnboarding = useCallback(async (role) => {
    const storedToken = localStorage.getItem(PROV_TOKEN_KEY);
    if (!storedToken) throw new Error('No provisioning token found');

    const response = await completeSsoProvisioning(storedToken, role);
    localStorage.removeItem(PROV_TOKEN_KEY);
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
    setProvisioningToken(null);
    setToken(response.accessToken);
    setUser(response.user);
    return response.user;
  }, []);

  // ------------------------------------------------------------------
  // Logout (unchanged)
  // ------------------------------------------------------------------
  const logout = useCallback(() => {
    logoutRequest();
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(PROV_TOKEN_KEY);
    setToken(null);
    setUser(null);
    setProvisioningToken(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      token,
      provisioningToken,
      isAuthenticated:  Boolean(token && user),
      isProvisioning:   Boolean(provisioningToken && !user),
      login,
      loginWithSsoTokens,
      startSsoProvisioning,
      completeSsoOnboarding,
      logout,
    }),
    [login, loginWithSsoTokens, startSsoProvisioning, completeSsoOnboarding, logout, token, user, provisioningToken],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
