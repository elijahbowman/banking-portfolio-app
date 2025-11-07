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
        config.BANKING_SERVICE_URL = prependHttpPrefixIfMissing(config.BANKING_SERVICE_URL);
        return config;
    } catch (error) {
        console.error('loadConfig error:', error);
        throw error;
    }
};

let apiInstance: ReturnType<typeof axios.create> | null = null;

export const getApi = async () => {
    if (!apiInstance) {
        try {
            const config = await loadConfig();
            console.log('Config loaded:', config);
            apiInstance = axios.create({
                baseURL: `${config.BANKING_SERVICE_URL}/api/v1/banking`,
            });
            console.log('API baseURL:', apiInstance.getUri());
        } catch (error) {
            console.error('getApi error:', error);
            throw error;
        }
    }
    return apiInstance;
};

export const getBalance = async (accountId: string): Promise<BalanceResponse> => {
    const api = await getApi();
    return api.get(`/accounts/${accountId}/balance`).then(r => r.data)
}

export const deposit = async (data: TransactionRequest): Promise<TransactionResponse> => {
    const api = await getApi();
    return api.post('/deposits', data).then(r => r.data)
}

export const withdraw = async (data: TransactionRequest): Promise<TransactionResponse> => {
    const api = await getApi();
    return api.post('/withdrawals', data).then(r => r.data)
}

export const transfer = async (data: TransactionRequest): Promise<TransactionResponse> => {
    const api = await getApi();
    return api.post('/transfers', data).then(r => r.data)
}
