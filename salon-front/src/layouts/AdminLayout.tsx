import { useState } from 'react';
import { Outlet, Navigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { 
  Menu, 
  X, 
  LayoutDashboard, 
  Users, 
  UserCheck, 
  Scissors, 
  Package, 
  Calendar, 
  DollarSign, 
  FileBarChart, 
  LogOut,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';

export const AdminLayout = () => {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const [showSidebar, setShowSidebar] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(() => {
    return localStorage.getItem('salon_sidebar_collapsed') === 'true';
  });
  const location = useLocation();

  if (isLoading) return (
    <div className="flex justify-center items-center h-screen bg-[#fcf9f9]">
      <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-[#be8a83]"></div>
    </div>
  );

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.role !== 'ADMIN' && user?.role !== 'GERENTE_DE_ATENDIMENTO') {
    return <Navigate to="/" replace />;
  }

  const toggleSidebar = () => setShowSidebar(!showSidebar);
  const closeSidebar = () => setShowSidebar(false);

  const toggleCollapse = () => {
    setIsCollapsed(prev => {
      const next = !prev;
      localStorage.setItem('salon_sidebar_collapsed', String(next));
      return next;
    });
  };

  const menuItems = [
    { to: '/admin/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/admin/users', label: 'Clientes', icon: Users },
    { to: '/admin/employees', label: 'Funcionárias', icon: UserCheck },
    { to: '/admin/services', label: 'Serviços', icon: Scissors },
    { to: '/admin/products', label: 'Produtos', icon: Package },
    { to: '/admin/appointments', label: 'Agendamentos', icon: Calendar },
    { to: '/admin/cashflow', label: 'Fluxo de Caixa', icon: DollarSign },
    { to: '/admin/reports', label: 'Relatórios', icon: FileBarChart },
  ];

  const userName = user?.email ? user.email.split('@')[0] : 'Admin';

  return (
    <div className="min-h-screen bg-[#fcf9f9] flex flex-col md:flex-row">
      {/* Mobile Top Navbar */}
      <div className="md:hidden bg-[#3b3036] text-white flex justify-between items-center p-4 shadow-md z-40 sticky top-0">
        <h5 className="m-0 font-heading font-semibold text-lg tracking-wide text-[#e5a49c]">Admin Salão</h5>
        <button 
          onClick={toggleSidebar} 
          className="p-1 hover:text-[#be8a83] focus:outline-none transition-colors"
          aria-label="Toggle navigation"
        >
          {showSidebar ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {/* Overlay when sidebar is open on mobile */}
      {showSidebar && (
        <div 
          className="fixed inset-0 bg-black/40 backdrop-blur-xs z-40 md:hidden transition-opacity" 
          onClick={closeSidebar}
        />
      )}

      {/* Sidebar */}
      <aside 
        className={`bg-gradient-to-b from-[#3b3036] to-[#261f23] text-white flex flex-col fixed md:sticky top-0 h-screen z-50 transition-all duration-300 md:translate-x-0 ${
          showSidebar ? 'translate-x-0' : '-translate-x-full'
        } ${isCollapsed ? 'md:w-20' : 'md:w-[260px]'}`}
      >
        <div className="flex justify-between items-center px-6 py-5 border-b border-white/10 h-[73px]">
          {!isCollapsed ? (
            <h4 className="font-heading font-bold text-xl tracking-wide text-white transition-all duration-300">
              Admin <span className="text-[#be8a83]">Salão</span>
            </h4>
          ) : (
            <div className="mx-auto text-xl font-bold text-[#be8a83] transition-all duration-300">AS</div>
          )}
          <button 
            className="md:hidden text-white/80 hover:text-white focus:outline-none" 
            onClick={closeSidebar}
            aria-label="Close sidebar"
          >
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
          {menuItems.map((item) => {
            const isActive = location.pathname === item.to;
            const Icon = item.icon;
            return (
              <Link
                key={item.to}
                to={item.to}
                onClick={closeSidebar}
                className={`flex items-center rounded-xl transition-all duration-200 group text-sm font-medium relative ${
                  isCollapsed ? 'justify-center p-3' : 'gap-3 px-4 py-3'
                } ${
                  isActive
                    ? 'bg-[#be8a83] text-white shadow-md shadow-[#be8a83]/10'
                    : 'text-white/70 hover:text-white hover:bg-white/5'
                }`}
              >
                <Icon size={18} className={isActive ? 'text-white' : 'text-white/60 group-hover:text-white transition-colors'} />
                {!isCollapsed && <span>{item.label}</span>}
                
                {isCollapsed && (
                  <div className="absolute left-full ml-4 px-3 py-1.5 bg-[#261f23] text-white text-xs font-semibold rounded-lg shadow-lg border border-white/10 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50 whitespace-nowrap">
                    {item.label}
                  </div>
                )}
              </Link>
            );
          })}

          <button
            onClick={() => { logout(); closeSidebar(); }}
            className={`w-full flex items-center rounded-xl text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-all duration-200 text-sm font-semibold mt-8 text-left relative ${
              isCollapsed ? 'justify-center p-3' : 'gap-3 px-4 py-3'
            }`}
          >
            <LogOut size={18} />
            {!isCollapsed && <span>Sair</span>}
            
            {isCollapsed && (
              <div className="absolute left-full ml-4 px-3 py-1.5 bg-[#261f23] text-red-400 text-xs font-semibold rounded-lg shadow-lg border border-red-500/10 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50 whitespace-nowrap">
                Sair
              </div>
            )}
          </button>
        </nav>

        {/* Collapse button for desktop */}
        <button
          onClick={toggleCollapse}
          className="hidden md:flex items-center justify-center p-2 mx-4 my-3 rounded-lg hover:bg-white/5 text-white/60 hover:text-white transition-all duration-200"
          aria-label="Collapse sidebar"
        >
          {isCollapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
      </aside>

      {/* Main Content Area with Desktop Header */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Desktop Header */}
        <header className="hidden md:flex justify-between items-center px-8 py-4 bg-white/70 backdrop-blur-md border-b border-[#eae1e1] z-30 h-[73px]">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold bg-[#be8a83]/10 text-[#be8a83] px-2.5 py-1 rounded-full uppercase tracking-wider">
              {user?.role === 'ADMIN' ? 'Administrador' : 'Gerente'}
            </span>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <div className="text-sm font-semibold text-[#2a2528] capitalize">{userName}</div>
              <div className="text-xs text-[#7a7074]">{user?.email}</div>
            </div>
            <div className="h-10 w-10 rounded-full bg-gradient-to-tr from-[#be8a83] to-[#e5a49c] flex items-center justify-center text-white font-bold shadow-sm uppercase">
              {userName.charAt(0)}
            </div>
          </div>
        </header>

        <main className="flex-1 p-6 md:p-8 overflow-y-auto max-w-full">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
