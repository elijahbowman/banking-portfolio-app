// tests/msw/handlers.ts
import { http, HttpResponse } from 'msw'

export const handlers = [
    // Balance Inquiry
    http.get('/api/v1/accounts/balance', ({ request }) => {
        const url = new URL(request.url)
        const accountId = url.searchParams.get('accountId')
        if (accountId === 'account1') {
            return HttpResponse.json({ accountId, balance: '1000.00' })
        }
        return new HttpResponse(null, { status: 400 })
    }),

    // Deposit
    http.post('/api/v1/banking/deposits', () => {
        return HttpResponse.json({ transactionId: 'dep1', status: 'PENDING' })
    }),

    // Withdrawal
    http.post('/api/v1/banking/withdrawals', () => {
        return HttpResponse.json({ transactionId: 'wd1', status: 'PENDING' })
    }),

    // Transfer
    http.post('/api/v1/banking/transfers', () => {
        return HttpResponse.json({ transactionId: 'tr1', status: 'PENDING' })
    }),
]