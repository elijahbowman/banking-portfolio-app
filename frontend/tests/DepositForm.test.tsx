import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import DepositForm from '../src/components/DepositForm'
import { server } from './setupTests'
import { http, HttpResponse } from 'msw'

test('submits deposit and displays success', async () => {
    render(<DepositForm />)

    await userEvent.type(screen.getByPlaceholderText('Account ID'), 'acc123')
    await userEvent.type(screen.getByPlaceholderText('Amount'), '250.00')
    await userEvent.click(screen.getByText('Deposit'))

    await waitFor(() => {
        expect(screen.getByText('dep1')).toBeInTheDocument()
        expect(screen.getByText("Status: PENDING")).toBeInTheDocument()
        const result = screen.getByTestId('transaction-result')
        expect(within(result).getByText('dep1')).toBeInTheDocument()
        expect(within(result).getByText(/PENDING/)).toBeInTheDocument()
    })
})

test('handles API error gracefully', async () => {
    server.use(
        http.post('http://localhost:8080/api/v1/banking/deposits', () => {
            return HttpResponse.json({ message: 'Insufficient funds' }, { status: 400 })
        })
    )

    render(<DepositForm />)

    await userEvent.type(screen.getByPlaceholderText('Account ID'), 'acc123')
    await userEvent.type(screen.getByPlaceholderText('Amount'), '999999')
    await userEvent.click(screen.getByText('Deposit'))

    await waitFor(() => {
        expect(screen.getByText('Insufficient funds')).toBeInTheDocument()
    })
})