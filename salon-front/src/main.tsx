import { StrictMode } from 'react';
import 'virtual:pwa-register';
import { createRoot } from 'react-dom/client';
import './index.css';
import './dev/autoLogin';
import App from './App.tsx';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
