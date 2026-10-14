import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute.jsx';
import DashboardLayout from '../components/layout/DashboardLayout.jsx';
import LoginPage from '../pages/LoginPage.jsx';
import PatientDashboard from '../pages/PatientDashboard.jsx';
import IamDashboard from '../pages/IamDashboard.jsx';
import AuditLogs from '../pages/AuditLogs.jsx';
import SsoCallback from '../pages/SsoCallback.jsx';
import SsoOnboarding from '../pages/SsoOnboarding.jsx';

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/sso-callback" element={<SsoCallback />} />
      <Route path="/sso-onboarding" element={<SsoOnboarding />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path="/dashboard" element={<PatientDashboard />} />
          <Route element={<ProtectedRoute allowedRoles={['DOCTOR', 'TECHNICIAN']} />}>
            <Route path="/iam-dashboard" element={<IamDashboard />} />
          </Route>
          <Route element={<ProtectedRoute allowedRoles={['DOCTOR']} />}>
            <Route path="/audit-logs" element={<AuditLogs />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
