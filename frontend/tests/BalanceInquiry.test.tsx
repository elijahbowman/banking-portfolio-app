import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import BalanceInquiry from '../src/components/BalanceInquiry'
import { server } from './setupTests'
import { http, HttpResponse } from 'msw'

test('fetches and displays balance', async () => {
    render(<BalanceInquiry />)

    await userEvent.type(screen.getByPlaceholderText('Account ID'), 'account1')
    await userEvent.click(screen.getByText('Get Balance'))

    await waitFor(() => {
        const result = screen.getByTestId('balance-result')
        expect(within(result).getByText('account1')).toBeInTheDocument()
        expect(within(result).getByText(/\$1000.00/)).toBeInTheDocument()
    })
})

test('displays error on failed balance fetch', async () => {
    server.use(
        http.get('http://localhost:8080/api/v1/banking/accounts/:accountId/balance', () => {
            return new HttpResponse(null, { status: 400 })
        })
    )

    render(<BalanceInquiry />)

    await userEvent.type(screen.getByPlaceholderText('Account ID'), 'invalid')
    await userEvent.click(screen.getByText('Get Balance'))

    await waitFor(() => {
        expect(screen.getByText(/Failed to fetch balance/i)).toBeInTheDocument()
    })
})