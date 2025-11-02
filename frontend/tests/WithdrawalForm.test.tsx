import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import WithdrawalForm from '../src/components/WithdrawalForm'
import { server } from './setupTests'
import { http, HttpResponse } from 'msw'

test('submits withdrawal and shows transaction result', async () => {
    render(<WithdrawalForm />)

    await userEvent.type(screen.getByPlaceholderText('Account ID'), 'acc456')
    await userEvent.type(screen.getByPlaceholderText('Amount'), '150.00')
    await userEvent.click(screen.getByText('Withdraw'))

    await waitFor(() => {
        const result = screen.getByTestId('transaction-result')
        expect(within(result).getByText('wd1')).toBeInTheDocument()
        expect(within(result).getByText(/PENDING/)).toBeInTheDocument()
    })
})

test('button is disabled until both fields are valid', async () => {
    render(<WithdrawalForm />)

    const button = screen.getByRole('button', { name: /withdraw/i })
    expect(button).toBeDisabled()

    await userEvent.type(screen.getByPlaceholderText('Account ID'), 'acc456')
    expect(button).toBeDisabled()

    await userEvent.type(screen.getByPlaceholderText('Amount'), '50')
    expect(button).toBeEnabled()
})

test('displays error on failed withdrawal', async () => {
    server.use(
        http.post('http://localhost:8080/api/v1/banking/withdrawals', () => {
            return HttpResponse.json(
                { message: 'Insufficient balance' },
                { status: 400 }
            )
        })
    )

    render(<WithdrawalForm />)

    await userEvent.type(screen.getByPlaceholderText('Account ID'), 'acc456')
    await userEvent.type(screen.getByPlaceholderText('Amount'), '99999')
    await userEvent.click(screen.getByText('Withdraw'))

    await waitFor(() => {
        expect(screen.getByText('Insufficient balance')).toBeInTheDocument()
    })
})