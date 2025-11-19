// tests/setupTests.ts
import { beforeAll, afterEach, afterAll } from 'vitest'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import '@testing-library/jest-dom/vitest';


const handlers = [
    // CORS preflight for all API routes
    http.options('http://localhost:8080/api/v1/:path*', () => {
        return new HttpResponse(null, {
            status: 204,
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
                'Access-Control-Allow-Headers': 'Content-Type',
            },
        })
    }),

    // Balance Inquiry
    http.get('http://localhost:8080/api/v1/banking/accounts/:accountId/balance', ({ params }) => {
        const { accountId } = params
        if (accountId === 'account1') {
            return HttpResponse.json({ accountId, balance: '1000.00' })
        }
        return new HttpResponse(null, { status: 400 })
    }),

    // Deposit
    http.post('http://localhost:8080/api/v1/banking/deposits', async ({ request }) => {
        const body = await request.json()
        if (body && (body as any).amount > 10000) {
            return HttpResponse.json({ message: 'Insufficient funds' }, { status: 400 })
        }
        return HttpResponse.json({ transactionId: 'dep1', status: 'PENDING' })
    }),

    // Withdrawal
    http.post('http://localhost:8080/api/v1/banking/withdrawals', async ({ request }) => {
        const body = await request.json()
        if (body && (body as any).amount > 5000) {
            return HttpResponse.json({ message: 'Insufficient balance' }, { status: 400 })
        }
        return HttpResponse.json({ transactionId: 'wd1', status: 'PENDING' })
    }),

    // Transfer
    http.post('http://localhost:8080/api/v1/banking/transfers', async ({ request }) => {
        const body = await request.json()
        if (body && (body as any).toAccountId === 'invalid') {
            return HttpResponse.json({ message: 'Invalid destination account' }, { status: 400 })
        }
        return HttpResponse.json({ transactionId: 'tr1', status: 'PENDING' })
    }),

    // Catch-all for debugging
    http.all('http://localhost:8080/api/v1/:path*', ({ request }) => {
        console.warn('Unhandled API request:', request.method, request.url)
        return new HttpResponse(null, { status: 404 })
    }),
]

export const server = setupServer(...handlers)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())