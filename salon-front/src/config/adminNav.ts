import type { LucideIcon } from 'lucide-react';
import {
  Users,
  UserCheck,
  UserCog,
  Scissors,
  Package,
  Calendar,
  DollarSign,
  FileBarChart,
  Lightbulb,
} from 'lucide-react';

export interface AdminNavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  allowedRoles: string[];
}

/**
 * Fonte única para os itens da área administrativa: usada pela sidebar (AdminLayout)
 * e pelo redirecionamento padrão de "/admin" (Router). allowedRoles deve espelhar os
 * `allowedRoles` das rotas equivalentes em Router.tsx.
 */
export const ADMIN_NAV_ITEMS: AdminNavItem[] = [
  { to: '/admin/reports', label: 'Relatórios', icon: FileBarChart, allowedRoles: ['ADMIN', 'GERENTE_DE_ATENDIMENTO'] },
  { to: '/admin/clients', label: 'Clientes', icon: Users, allowedRoles: ['ADMIN', 'GERENTE_DE_ATENDIMENTO'] },
  { to: '/admin/users', label: 'Equipe', icon: UserCog, allowedRoles: ['ADMIN', 'GERENTE_DE_ATENDIMENTO'] },
  { to: '/admin/employees', label: 'Funcionários(as)', icon: UserCheck, allowedRoles: ['ADMIN', 'GERENTE_DE_ATENDIMENTO'] },
  { to: '/admin/services', label: 'Serviços', icon: Scissors, allowedRoles: ['ADMIN', 'GERENTE_DE_ATENDIMENTO'] },
  { to: '/admin/products', label: 'Produtos', icon: Package, allowedRoles: ['ADMIN'] },
  { to: '/admin/appointments', label: 'Agendamentos', icon: Calendar, allowedRoles: ['ADMIN', 'GERENTE_DE_ATENDIMENTO', 'FUNCIONARIA'] },
  { to: '/admin/cashflow', label: 'Fluxo de Caixa', icon: DollarSign, allowedRoles: ['ADMIN', 'GERENTE_DE_ATENDIMENTO'] },
  { to: '/admin/recommendations', label: 'Recomendações de IA', icon: Lightbulb, allowedRoles: ['ADMIN', 'GERENTE_DE_ATENDIMENTO'] },
];

/** SYSADMIN tem bypass total no ProtectedRoute/AdminLayout, então enxerga tudo. */
export const canAccessAdminNavItem = (item: AdminNavItem, role?: string | null): boolean =>
  role === 'SYSADMIN' || (!!role && item.allowedRoles.includes(role));

export const getVisibleAdminNavItems = (role?: string | null): AdminNavItem[] =>
  ADMIN_NAV_ITEMS.filter((item) => canAccessAdminNavItem(item, role));

/** Rota padrão ao entrar em "/admin": primeiro item que o cargo do usuário pode acessar. */
export const getDefaultAdminPath = (role?: string | null): string => {
  if (role === 'SYSADMIN') return '/admin/reports';
  const first = getVisibleAdminNavItems(role)[0];
  return first ? first.to : '/';
};
