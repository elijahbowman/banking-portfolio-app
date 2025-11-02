// tests/msw/mockError.ts
import { http, HttpResponse } from 'msw'

export const mockDepositError = http.post(
    '/api/v1/banking/deposits',
    () => HttpResponse.json({ message: 'Insufficient funds' }, { status: 400 })
)

export const mockWithdrawalError = http.post(
    '/api/v1/banking/withdrawals',
    () => HttpResponse.json({ message: 'Insufficient balance' }, { status: 400 })
)

export const mockTransferError = http.post(
    '/api/v1/banking/transfers',
    () => HttpResponse.json({ message: 'Invalid destination account' }, { status: 400 })
)

export const mockBalanceError = http.get(
    '/api/v1/accounts/balance',
    () => new HttpResponse(null, { status: 400 })
)