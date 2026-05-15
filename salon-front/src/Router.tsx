import { Routes, Route } from 'react-router-dom';
import { DefaultLayout } from './layouts/DefaultLayout';
import { AdminLayout } from './layouts/AdminLayout';
import { CustomerLayout } from './layouts/CustomerLayout';
import { Login } from './pages/auth/Login';
import { Register } from './pages/auth/Register';
import { AdminServices } from './pages/admin/AdminServices';
import { Products } from './pages/admin/Products';
import { Users } from './pages/admin/Users';
import { Employees } from './pages/admin/Employees';

export const Router = () => {
  return (
    <Routes>
      <Route element={<DefaultLayout />}>
        <Route path="/" element={<div>Home (Pública)</div>} />
        <Route path="/services" element={<div>Serviços (Pública)</div>} />
        <Route path="/appointment" element={<div>Agendamento (Pública)</div>} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
      </Route>

      <Route element={<AdminLayout />}>
        <Route path="/admin/dashboard" element={<div>Dashboard Admin</div>} />
        <Route path="/admin/users" element={<Users />} />
        <Route path="/admin/employees" element={<Employees />} />
        <Route path="/admin/services" element={<AdminServices />} />
        <Route path="/admin/products" element={<Products />} />
        <Route path="/admin/appointments" element={<div>Agendamentos Admin</div>} />
        <Route path="/admin/cashflow" element={<div>Fluxo de Caixa Admin</div>} />
        <Route path="/admin/reports" element={<div>Relatórios Admin</div>} />
      </Route>

      <Route element={<CustomerLayout />}>
        <Route path="/my-appointments" element={<div>Meus Agendamentos</div>} />
        <Route path="/profile" element={<div>Meu Perfil</div>} />
      </Route>
    </Routes>
  );
};
