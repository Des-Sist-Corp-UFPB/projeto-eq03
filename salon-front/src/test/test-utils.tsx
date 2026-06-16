import type { ReactNode, ReactElement } from 'react';
import { render } from '@testing-library/react';
import type { RenderOptions } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import type { UserContextData } from '../types/auth';

interface CustomRenderOptions extends Omit<RenderOptions, 'queries'> {
  route?: string;
  user?: UserContextData | null;
  isAuthenticated?: boolean;
  login?: (accessToken: string, refreshToken: string, redirect?: boolean) => void;
  logout?: () => void;
  isLoading?: boolean;
}

const customRender = (
  ui: ReactElement,
  {
    route = '/',
    user = null,
    isAuthenticated = false,
    login = () => {},
    logout = () => {},
    isLoading = false,
    ...renderOptions
  }: CustomRenderOptions = {}
) => {
  window.history.pushState({}, 'Test page', route);

  const Wrapper = ({ children }: { children: ReactNode }) => {
    return (
      <AuthContext.Provider value={{ user, isAuthenticated, login, logout, isLoading }}>
        <BrowserRouter>{children}</BrowserRouter>
      </AuthContext.Provider>
    );
  };

  return render(ui, { wrapper: Wrapper, ...renderOptions });
};

export * from '@testing-library/react';
export { customRender };
