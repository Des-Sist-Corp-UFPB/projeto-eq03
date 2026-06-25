import type { ReactNode, ReactElement } from 'react';
import { render } from '@testing-library/react';
import type { RenderOptions } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import type { UserContextData } from '../types/auth';
import { vi } from 'vitest';

interface CustomRenderOptions extends Omit<RenderOptions, 'queries'> {
  route?: string;
  user?: UserContextData | null;
  isAuthenticated?: boolean;
  login?: (accessToken: string, refreshToken: string, redirect?: boolean) => Promise<void>;
  logout?: () => void;
  updateUserCpf?: (cpf: string) => void;
  isLoading?: boolean;
}

const customRender = (
  ui: ReactElement,
  {
    route = '/',
    user = null,
    isAuthenticated = false,
    login = async () => {},
    logout = () => {},
    updateUserCpf = vi.fn(),
    isLoading = false,
    ...renderOptions
  }: CustomRenderOptions = {}
) => {
  window.history.pushState({}, 'Test page', route);

  const Wrapper = ({ children }: { children: ReactNode }) => {
    return (
      <AuthContext.Provider
        value={{ user, isAuthenticated, login, logout, updateUserCpf, isLoading }}
      >
        <BrowserRouter>{children}</BrowserRouter>
      </AuthContext.Provider>
    );
  };

  return render(ui, { wrapper: Wrapper, ...renderOptions });
};

export * from '@testing-library/react';
export { customRender };
