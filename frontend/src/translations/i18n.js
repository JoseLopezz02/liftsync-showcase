// This file is responsible for setting up internationalization (i18n) in the application using the @lingui/core library.
// It defines functions to load default translations and specific locale translations, and it initializes the i18n instance with the default locale(EN).
// The getLocale function retrieves the user's preferred locale from local storage, and if it's not set, it defaults to 'en-EN'.
// The loadDefaultTranslations function loads the translations for the default locale, while the loadTranslations function can be used to load translations for any specified locale.
import { i18n } from '@lingui/core';
import { getLocale } from '../utils/localStorage';

const defaultLocale = getLocale() ?? 'en-EN';

i18n.activate(defaultLocale);
export async function loadDefaultTranslations() {
  const { messages } = await import(`./locales/${defaultLocale}/messages.mjs`);
  i18n.load(defaultLocale, messages);
  i18n.activate(defaultLocale);
}

export async function loadTranslations(locale) {
  try {
    const { messages } = await import(`./locales/${locale}/messages.mjs`);
    i18n.load(locale, messages);
    i18n.activate(locale);
  } catch (error) {
    console.error(`Error loading translations for ${locale}:`, error);
  }
}

export default i18n;