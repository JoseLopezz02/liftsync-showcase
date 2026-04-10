//App, UserContextProvider, UIContextProvider, InfoNotificationProvider and other elements,
// are part of the business logic of the application and are being censured on this show case. 
// The main focus of this code snippet is to demonstrate the initialization of the React application, 
// including the setup of internationalization (i18n) and context providers for user and UI state management. 
// The authWatcher function is also called to monitor authentication status changes. 
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/core_liftsync/globalStyles.css';
import { authWatcher } from './hooks/authWatcher';
import { I18nProvider } from '@lingui/react';
import i18n, { loadDefaultTranslations } from './translations/i18n';
import UserContextProvider from './context/user/UserProvider';
import UIContextProvider from './context/ui/UIProvider';
import { InfoNotificationProvider } from './context/notification/InfoNotificationProvider';

authWatcher();

async function initialize() {
  await loadDefaultTranslations();

  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <I18nProvider i18n={i18n}>
        <UIContextProvider>
          <UserContextProvider>
            <InfoNotificationProvider>
              <App />
            </InfoNotificationProvider>
          </UserContextProvider>
        </UIContextProvider>
      </I18nProvider>
    </React.StrictMode>
  );
}

initialize();