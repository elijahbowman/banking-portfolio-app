/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_BANKING_SERVICE_URL: string;
  readonly VITE_ACCOUNT_SERVICE_URL: string;
  readonly VITE_CARD_SERVICE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}