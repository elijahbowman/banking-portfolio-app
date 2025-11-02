import axios from 'axios'
import type { BalanceResponse, TransactionRequest, TransactionResponse } from '../types'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

const api = axios.create({
    baseURL: API_BASE,
})

// export const api = axios.create({
//     baseURL: API_BASE,
//     headers: {
//         'Content-Type': 'application/json',
//     },
// })

export const getBalance = (accountId: string): Promise<BalanceResponse> =>
    api.get(`/api/v1/accounts/balance`, { params: { accountId } }).then(r => r.data)

export const deposit = (data: TransactionRequest): Promise<TransactionResponse> =>
    api.post('/api/v1/banking/deposits', data).then(r => r.data)

export const withdraw = (data: TransactionRequest): Promise<TransactionResponse> =>
    api.post('/api/v1/banking/withdrawals', data).then(r => r.data)

export const transfer = (data: TransactionRequest): Promise<TransactionResponse> =>
    api.post('/api/v1/banking/transfers', data).then(r => r.data)