const fs = require('fs');
const path = require('path');

// Move logic
function move(src, dest) {
  if (fs.existsSync(src)) {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.renameSync(src, dest);
  }
}

// 1. Move files
move('src/pages/admin/Users.tsx', 'src/pages/admin/users/Users.tsx');
move('src/services/users.ts', 'src/pages/admin/users/services/users.ts');

move('src/pages/admin/Employees.tsx', 'src/pages/admin/employees/Employees.tsx');
move('src/services/employees.ts', 'src/pages/admin/employees/services/employees.ts');

move('src/pages/admin/AdminServices.tsx', 'src/pages/admin/services/AdminServices.tsx');
move('src/services/services.ts', 'src/pages/services/services/services.ts');
move('src/pages/public/PublicServices.tsx', 'src/pages/services/PublicServices.tsx');

move('src/pages/admin/Products.tsx', 'src/pages/admin/products/Products.tsx');
move('src/services/products.ts', 'src/pages/admin/products/services/products.ts');

move('src/pages/admin/AdminAppointments.tsx', 'src/pages/admin/appointments/AdminAppointments.tsx');
move('src/pages/public/PublicAppointment.tsx', 'src/pages/appointments/PublicAppointment.tsx');
move('src/pages/customer/MyAppointments.tsx', 'src/pages/appointments/MyAppointments.tsx');
move('src/services/appointments.ts', 'src/pages/appointments/services/appointments.ts');

// Delete unused dirs
fs.rmSync('src/features', { recursive: true, force: true });
fs.rmSync('src/pages/customer', { recursive: true, force: true });
fs.rmSync('src/pages/public', { recursive: true, force: true });

// Also there might be users folder in src/pages/users that I incorrectly created via write_to_file
fs.rmSync('src/pages/users', { recursive: true, force: true });
fs.rmSync('src/pages/employees', { recursive: true, force: true });

// Clean services dir except api.ts
if (fs.existsSync('src/services')) {
    fs.readdirSync('src/services').forEach(f => {
      if (f !== 'api.ts') fs.rmSync(path.join('src/services', f), { recursive: true, force: true });
    });
}

// 2. Fix imports in all files
function replaceInFile(file, replacements) {
  if (!fs.existsSync(file)) return;
  let content = fs.readFileSync(file, 'utf8');
  for (const [search, replace] of replacements) {
    content = content.split(search).join(replace);
  }
  fs.writeFileSync(file, content);
}

// Router.tsx
replaceInFile('src/Router.tsx', [
  ["import { AdminServices } from './pages/admin/AdminServices';", "import { AdminServices } from './pages/admin/services/AdminServices';"],
  ["import { Products } from './pages/admin/Products';", "import { Products } from './pages/admin/products/Products';"],
  ["import { Users } from './pages/admin/Users';", "import { Users } from './pages/admin/users/Users';"],
  ["import { Employees } from './pages/admin/Employees';", "import { Employees } from './pages/admin/employees/Employees';"],
  ["import { PublicServices } from './pages/public/PublicServices';", "import { PublicServices } from './pages/services/PublicServices';"],
  ["import { PublicAppointment } from './pages/public/PublicAppointment';", "import { PublicAppointment } from './pages/appointments/PublicAppointment';"],
  ["import { MyAppointments } from './pages/customer/MyAppointments';", "import { MyAppointments } from './pages/appointments/MyAppointments';"],
  ["import { AdminAppointments } from './pages/admin/AdminAppointments';", "import { AdminAppointments } from './pages/admin/appointments/AdminAppointments';"]
]);

// Users.tsx
replaceInFile('src/pages/admin/users/Users.tsx', [
  ["import { Table } from '../../components/table/Table';", "import { Table } from '../../../components/table/Table';"],
  ["import { ModalForm } from '../../components/modal/ModalForm';", "import { ModalForm } from '../../../components/modal/ModalForm';"],
  ["import { ConfirmDialog } from '../../components/modal/ConfirmDialog';", "import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';"],
  ["import { PermissionGate } from '../../components/permissions/PermissionGate';", "import { PermissionGate } from '../../../components/permissions/PermissionGate';"],
  ["import { usersApi, UserData, UserUpdateRequest } from '../../services/users';", "import { usersApi, UserData, UserUpdateRequest } from './services/users';"]
]);

// Employees.tsx
replaceInFile('src/pages/admin/employees/Employees.tsx', [
  ["import { Table } from '../../components/table/Table';", "import { Table } from '../../../components/table/Table';"],
  ["import { ModalForm } from '../../components/modal/ModalForm';", "import { ModalForm } from '../../../components/modal/ModalForm';"],
  ["import { ConfirmDialog } from '../../components/modal/ConfirmDialog';", "import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';"],
  ["import { PermissionGate } from '../../components/permissions/PermissionGate';", "import { PermissionGate } from '../../../components/permissions/PermissionGate';"],
  ["import { employeesApi, EmployeeData } from '../../services/employees';", "import { employeesApi, EmployeeData } from './services/employees';"]
]);

// AdminServices.tsx
replaceInFile('src/pages/admin/services/AdminServices.tsx', [
  ["import { Table } from '../../components/table/Table';", "import { Table } from '../../../components/table/Table';"],
  ["import { ModalForm } from '../../components/modal/ModalForm';", "import { ModalForm } from '../../../components/modal/ModalForm';"],
  ["import { ConfirmDialog } from '../../components/modal/ConfirmDialog';", "import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';"],
  ["import { PermissionGate } from '../../components/permissions/PermissionGate';", "import { PermissionGate } from '../../../components/permissions/PermissionGate';"],
  ["import { servicesApi, ServiceData } from '../../services/services';", "import { servicesApi, ServiceData } from '../../services/services/services';"]
]);

// Products.tsx
replaceInFile('src/pages/admin/products/Products.tsx', [
  ["import { Table } from '../../components/table/Table';", "import { Table } from '../../../components/table/Table';"],
  ["import { ModalForm } from '../../components/modal/ModalForm';", "import { ModalForm } from '../../../components/modal/ModalForm';"],
  ["import { ConfirmDialog } from '../../components/modal/ConfirmDialog';", "import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';"],
  ["import { PermissionGate } from '../../components/permissions/PermissionGate';", "import { PermissionGate } from '../../../components/permissions/PermissionGate';"],
  ["import { productsApi, ProductData } from '../../services/products';", "import { productsApi, ProductData } from './services/products';"]
]);

// AdminAppointments.tsx
replaceInFile('src/pages/admin/appointments/AdminAppointments.tsx', [
  ["import { Table } from '../../components/table/Table';", "import { Table } from '../../../components/table/Table';"],
  ["import { ConfirmDialog } from '../../components/modal/ConfirmDialog';", "import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';"],
  ["import { PermissionGate } from '../../components/permissions/PermissionGate';", "import { PermissionGate } from '../../../components/permissions/PermissionGate';"],
  ["import { appointmentsApi, AppointmentResponse } from '../../services/appointments';", "import { appointmentsApi, AppointmentResponse } from '../../appointments/services/appointments';"]
]);

// PublicServices.tsx
replaceInFile('src/pages/services/PublicServices.tsx', [
  ["import { servicesApi, ServiceData } from '../../services/services';", "import { servicesApi, ServiceData } from './services/services';"]
]);

// PublicAppointment.tsx
replaceInFile('src/pages/appointments/PublicAppointment.tsx', [
  ["import { servicesApi, ServiceData } from '../../services/services';", "import { servicesApi, ServiceData } from '../services/services/services';"],
  ["import { employeesApi, EmployeeData } from '../../services/employees';", "import { employeesApi, EmployeeData } from '../admin/employees/services/employees';"],
  ["import { appointmentsApi, TimeSlotResponse } from '../../services/appointments';", "import { appointmentsApi, TimeSlotResponse } from './services/appointments';"]
]);

// MyAppointments.tsx
replaceInFile('src/pages/appointments/MyAppointments.tsx', [
  ["import { appointmentsApi, AppointmentResponse } from '../../services/appointments';", "import { appointmentsApi, AppointmentResponse } from './services/appointments';"]
]);

// API files
replaceInFile('src/pages/admin/users/services/users.ts', [
  ["import api from './api';", "import api from '../../../../services/api';"],
  ["import api from '../../services/api';", "import api from '../../../../services/api';"]
]);
replaceInFile('src/pages/admin/employees/services/employees.ts', [
  ["import api from './api';", "import api from '../../../../services/api';"],
  ["import api from '../../services/api';", "import api from '../../../../services/api';"]
]);
replaceInFile('src/pages/admin/products/services/products.ts', [
  ["import api from './api';", "import api from '../../../../services/api';"],
  ["import api from '../../services/api';", "import api from '../../../../services/api';"]
]);
replaceInFile('src/pages/services/services/services.ts', [
  ["import api from './api';", "import api from '../../../services/api';"],
  ["import api from '../../services/api';", "import api from '../../../services/api';"]
]);
replaceInFile('src/pages/appointments/services/appointments.ts', [
  ["import api from './api';", "import api from '../../../services/api';"],
  ["import api from '../../services/api';", "import api from '../../../services/api';"]
]);

console.log("Refactoring complete");
