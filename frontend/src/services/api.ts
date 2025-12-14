import axios from 'axios'
import type { BalanceResponse, TransactionRequest, TransactionResponse } from '../types'

interface RuntimeConfig {
    BANKING_SERVICE_URL: string;
    ACCOUNT_SERVICE_URL: string;
    CARD_SERVICE_URL: string;
}

const prependHttpPrefixIfMissing = (url: string): string => {
    return url.startsWith('http')
            ? url
            : `http://${url}`;
}

const loadConfig = async (): Promise<RuntimeConfig> => {
    if (import.meta.env.DEV) {
        const config = {
            BANKING_SERVICE_URL: import.meta.env.VITE_BANKING_SERVICE_URL,
            ACCOUNT_SERVICE_URL: import.meta.env.VITE_ACCOUNT_SERVICE_URL,
            CARD_SERVICE_URL: import.meta.env.VITE_CARD_SERVICE_URL,
        };
        console.log('Dev config:', config);
        if (!config.BANKING_SERVICE_URL) {
            throw new Error('BANKING_SERVICE_URL is not set');
        }
        if (!config.CARD_SERVICE_URL) {
            throw new Error('CARD_SERVICE_URL is not set');
        }
        return config;
    }
    
    try {
        const response = await fetch('/config.json');
        if (!response.ok) {
            throw new Error(`Failed to fetch config.json: ${response.statusText}`);
        }
        const config = await response.json();
        console.log('Fetched config:', config);
        if (!config.BANKING_SERVICE_URL) {
            throw new Error('BANKING_SERVICE_URL is not set');
        }
        if (!config.CARD_SERVICE_URL) {
            throw new Error('CARD_SERVICE_URL is not set');
        }
        config.BANKING_SERVICE_URL = prependHttpPrefixIfMissing(config.BANKING_SERVICE_URL);
        config.CARD_SERVICE_URL = prependHttpPrefixIfMissing(config.CARD_SERVICE_URL);
        return config;
    } catch (error) {
        console.error('loadConfig error:', error);
        throw error;
    }
};

let bankingApiInstance: ReturnType<typeof axios.create> | null = null;

export const getBankingApi = async () => {
    if (!bankingApiInstance) {
        try {
            const config = await loadConfig();
            console.log('Config loaded:', config);
            bankingApiInstance = axios.create({
                baseURL: `${config.BANKING_SERVICE_URL}/api/v1/banking`,
            });
            // console.log('Banking API baseURL:', bankingApiInstance.getUri());
        } catch (error) {
            console.error('getBankingApi error:', error);
            throw error;
        }
    }
    return bankingApiInstance;
};

let cardApiInstance: ReturnType<typeof axios.create> | null = null;

export const getCardApi = async () => {
    if (!cardApiInstance) {
        try {
            const config = await loadConfig();
            console.log('Config loaded:', config);
            cardApiInstance = axios.create({
                baseURL: `${config.CARD_SERVICE_URL}/`,
            });
            // console.log('Card API baseURL:', cardApiInstance.getUri());
        } catch (error) {
            console.error('getCardApi error:', error);
            throw error;
        }
    }
    return cardApiInstance;
};

export const getBalance = async (accountId: string): Promise<BalanceResponse> => {
    const api = await getBankingApi();
    return api.get(`/accounts/${accountId}/balance`).then(r => r.data)
}

export const deposit = async (data: TransactionRequest): Promise<TransactionResponse> => {
    const api = await getBankingApi();
    return api.post('/deposits', data).then(r => r.data)
}

export const withdraw = async (data: TransactionRequest): Promise<TransactionResponse> => {
    const api = await getBankingApi();
    return api.post('/withdrawals', data).then(r => r.data)
}

export const transfer = async (data: TransactionRequest): Promise<TransactionResponse> => {
    const api = await getBankingApi();
    return api.post('/transfers', data).then(r => r.data)
}

export const issueVCN = async (realCardId: string, limit: number) => {
    const api = await getCardApi();
  const response = await api.post('/cards/vcn', null, {
    params: { realCardId, limit },
  });
  return response.data;
};

export const getRealCard = async (accountId: string) => {
    const api = await getCardApi();
  const response = await api.get('/cards/real', {
    params: { accountId },
  });
  return response.data;
};

export const getVCNs = async (realCardId: string) => {
    const api = await getCardApi();
  const response = await api.get('/cards/vcns', {
    params: { realCardId },
  });
  return response.data;
};