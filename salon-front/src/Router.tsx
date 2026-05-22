import { Routes, Route } from 'react-router-dom';
import { DefaultLayout } from './layouts/DefaultLayout';
import { AdminLayout } from './layouts/AdminLayout';
import { CustomerLayout } from './layouts/CustomerLayout';
import { SysadminLayout } from './layouts/SysadminLayout';
import { FeatureFlags } from './pages/sysadmin/FeatureFlags';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Login } from './pages/auth/Login';
import { Register } from './pages/auth/Register';
import { AdminServices } from './pages/admin/services/AdminServices';
import { Products } from './pages/admin/products/Products';
import { Users } from './pages/admin/users/Users';
import { Employees } from './pages/admin/employees/Employees';
import { PublicServices } from './pages/services/PublicServices';
import { PublicHome } from './pages/home/PublicHome';
import { PublicAppointment } from './pages/appointments/PublicAppointment';
import { MyAppointments } from './pages/appointments/MyAppointments';
import { AdminAppointments } from './pages/admin/appointments/AdminAppointments';
import { CashFlow } from './pages/admin/cashflow/CashFlow';
import { Reports } from './pages/admin/reports/Reports';
import { AuditLog } from './pages/admin/audit/AuditLog';
import { NotFound } from './pages/error/NotFound';
import { Profile } from './pages/profile/Profile';

export const Router = () => {
  return (
    <Routes>
      <Route element={<DefaultLayout />}>
        <Route path="/" element={<PublicHome />} />
        <Route path="/services" element={<PublicServices />} />
        <Route path="/appointment" element={<PublicAppointment />} />
      </Route>

      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<AdminLayout />}>
        <Route
          path="/admin/dashboard"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <Reports />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <Users />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/employees"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <Employees />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/services"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <AdminServices />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/products"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <Products />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/appointments"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <AdminAppointments />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/cashflow"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <CashFlow />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/reports"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <Reports />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route element={<SysadminLayout />}>
        <Route
          path="/sysadmin/feature-flags"
          element={
            <ProtectedRoute requiredRole="SYSADMIN">
              <FeatureFlags />
            </ProtectedRoute>
          }
        />
        <Route
          path="/sysadmin/audit"
          element={
            <ProtectedRoute requiredRole="SYSADMIN">
              <AuditLog />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route element={<CustomerLayout />}>
        <Route
          path="/my-appointments"
          element={
            <ProtectedRoute>
              <MyAppointments />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <Profile />
            </ProtectedRoute>
          }
        />
      </Route>

      {/* Catch-all para 404 */}
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};
