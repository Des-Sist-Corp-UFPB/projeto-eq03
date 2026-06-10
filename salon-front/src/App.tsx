import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { AlertProvider } from './context/AlertContext';
import { ThemeProvider } from './context/ThemeContext';
import { Router } from './Router';

function App() {
  return (
    <BrowserRouter>
      <AlertProvider>
        <ThemeProvider>
          <AuthProvider>
            <Router />
          </AuthProvider>
        </ThemeProvider>
      </AlertProvider>
    </BrowserRouter>
  );
}

export default App;
