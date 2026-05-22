import { useState } from 'react';
import { Outlet, NavLink, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Menu, X, Sparkles } from 'lucide-react';

export const DefaultLayout = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const toggleMobileMenu = () => setMobileMenuOpen(!mobileMenuOpen);
  const closeMobileMenu = () => setMobileMenuOpen(false);

  const userName = user?.email ? user.email.split('@')[0] : 'Cliente';

  return (
    <div className="min-h-screen flex flex-col bg-[#fcf9f9]">
      {/* Top Navbar */}
      <nav className="fixed top-0 left-0 right-0 glass-panel border-x-0 border-t-0 border-b border-[#eae1e1]/50 shadow-xs z-50 transition-all duration-300">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            {/* Logo */}
            <Link to="/" onClick={closeMobileMenu} className="flex items-center gap-2 group">
              <Sparkles className="text-[#be8a83] h-5 w-5 transition-transform group-hover:rotate-12" />
              <span className="font-heading font-semibold text-lg tracking-wide text-[#3b3036] group-hover:text-[#be8a83] transition-colors">
                Salão Cristiane
              </span>
            </Link>

            {/* Desktop Navigation */}
            <div className="hidden md:flex items-center gap-1.5">
              <NavLink 
                to="/services" 
                className={({ isActive }) => 
                  `px-4 py-2 rounded-xl text-sm font-semibold transition-all duration-200 ${
                    isActive 
                      ? 'bg-[#be8a83]/10 text-[#be8a83]' 
                      : 'text-[#3b3036]/70 hover:text-[#3b3036] hover:bg-[#be8a83]/5'
                  }`
                }
              >
                Nossos Serviços
              </NavLink>
              <NavLink 
                to="/appointment" 
                className={({ isActive }) => 
                  `px-4 py-2 rounded-xl text-sm font-semibold transition-all duration-200 ${
                    isActive 
                      ? 'bg-[#be8a83]/10 text-[#be8a83]' 
                      : 'text-[#3b3036]/70 hover:text-[#3b3036] hover:bg-[#be8a83]/5'
                  }`
                }
              >
                Agendamento
              </NavLink>
            </div>

            {/* Desktop Actions */}
            <div className="hidden md:flex items-center gap-3">
              {isAuthenticated ? (
                <div className="flex items-center gap-4">
                  <NavLink 
                    to="/my-appointments" 
                    className={({ isActive }) => 
                      `px-4 py-2 rounded-xl text-sm font-semibold transition-all duration-200 ${
                        isActive 
                          ? 'bg-[#be8a83]/10 text-[#be8a83]' 
                          : 'text-[#3b3036]/70 hover:text-[#3b3036] hover:bg-[#be8a83]/5'
                      }`
                    }
                  >
                    Meus Horários
                  </NavLink>
                  <div className="flex items-center gap-3 border-l border-r border-[#eae1e1] px-4 py-1">
                    <div className="text-right">
                      <div className="text-sm font-semibold text-[#2a2528] capitalize">{userName}</div>
                      <div className="text-xs text-[#7a7074]">{user?.email}</div>
                    </div>
                    <div className="h-9 w-9 rounded-full bg-gradient-to-tr from-[#be8a83] to-[#e5a49c] flex items-center justify-center text-white font-bold shadow-xs uppercase">
                      {userName.charAt(0)}
                    </div>
                  </div>
                  <button 
                    onClick={logout} 
                    className="px-4 py-2 border border-gray-200 rounded-xl text-sm font-semibold text-[#3b3036] hover:bg-gray-50 transition-all duration-200"
                  >
                    Sair
                  </button>
                </div>
              ) : (
                <>
                  <Link 
                    to="/login" 
                    className="text-sm font-semibold text-[#3b3036]/70 hover:text-[#3b3036] transition-colors pr-2"
                  >
                    Entrar
                  </Link>
                  <Link 
                    to="/register" 
                    className="px-5 py-2.5 bg-[#be8a83] text-white rounded-full text-sm font-semibold hover:bg-[#a6726b] hover:shadow-lg hover:shadow-[#be8a83]/20 transition-all duration-200 hover:-translate-y-0.5"
                  >
                    Criar Conta
                  </Link>
                </>
              )}
            </div>

            {/* Mobile Hamburger Button */}
            <div className="md:hidden flex items-center">
              <button
                onClick={toggleMobileMenu}
                className="p-2 text-[#3b3036] hover:text-[#be8a83] transition-colors focus:outline-none"
                aria-label="Toggle menu"
              >
                {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile Dropdown Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden border-t border-gray-100 bg-white/98 shadow-lg py-4 px-4 space-y-3 animate-fadeIn">
            {/* User Profile Info on Mobile */}
            {isAuthenticated && (
              <div className="flex items-center gap-3 px-2 py-1 border-b border-gray-100 pb-3">
                <div className="h-9 w-9 rounded-full bg-gradient-to-tr from-[#be8a83] to-[#e5a49c] flex items-center justify-center text-white font-bold shadow-xs uppercase">
                  {userName.charAt(0)}
                </div>
                <div className="min-w-0">
                  <div className="text-sm font-semibold text-[#2a2528] capitalize truncate">{userName}</div>
                  <div className="text-xs text-[#7a7074] truncate">{user?.email}</div>
                </div>
              </div>
            )}

            <div className="space-y-1">
              <NavLink 
                to="/services" 
                onClick={closeMobileMenu}
                className={({ isActive }) => 
                  `block px-4 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 ${
                    isActive 
                      ? 'bg-[#be8a83]/10 text-[#be8a83]' 
                      : 'text-[#3b3036]/70 hover:text-[#3b3036] hover:bg-gray-50'
                  }`
                }
              >
                Nossos Serviços
              </NavLink>
              <NavLink 
                to="/appointment" 
                onClick={closeMobileMenu}
                className={({ isActive }) => 
                  `block px-4 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 ${
                    isActive 
                      ? 'bg-[#be8a83]/10 text-[#be8a83]' 
                      : 'text-[#3b3036]/70 hover:text-[#3b3036] hover:bg-gray-50'
                  }`
                }
              >
                Agendamento
              </NavLink>
            </div>

            <div className="border-t border-gray-100 pt-3 flex flex-col gap-2">
              {isAuthenticated ? (
                <>
                  <Link 
                    to="/my-appointments" 
                    onClick={closeMobileMenu}
                    className="block px-4 py-2.5 text-sm font-semibold text-[#3b3036]/70 hover:text-[#3b3036] transition-colors"
                  >
                    Meus Horários
                  </Link>
                  <button 
                    onClick={() => { logout(); closeMobileMenu(); }} 
                    className="w-full text-left px-4 py-2.5 border border-gray-200 rounded-xl text-sm font-semibold text-[#3b3036] hover:bg-gray-50 transition-all duration-200"
                  >
                    Sair
                  </button>
                </>
              ) : (
                <>
                  <Link 
                    to="/login" 
                    onClick={closeMobileMenu}
                    className="block px-4 py-2.5 text-sm font-semibold text-[#3b3036]/70 hover:text-[#3b3036] transition-colors"
                  >
                    Entrar
                  </Link>
                  <Link 
                    to="/register" 
                    onClick={closeMobileMenu}
                    className="block text-center px-4 py-2.5 bg-[#be8a83] text-white rounded-xl text-sm font-semibold hover:bg-[#a6726b] transition-all duration-200"
                  >
                    Criar Conta
                  </Link>
                </>
              )}
            </div>
          </div>
        )}
      </nav>

      {/* Main Content */}
      <main className="flex-1 pt-16 animate-fadeIn">
        <Outlet />
      </main>
    </div>
  );
};
