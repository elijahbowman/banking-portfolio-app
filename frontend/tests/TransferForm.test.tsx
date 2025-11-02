import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import TransferForm from '../src/components/TransferForm'
import { server } from './setupTests'
import { http, HttpResponse } from 'msw'

test('submits transfer with all fields filled', async () => {
    render(<TransferForm />)

    await userEvent.type(screen.getByPlaceholderText('From Account ID'), 'acc789')
    await userEvent.type(screen.getByPlaceholderText('To Account ID'), 'acc101')
    await userEvent.type(screen.getByPlaceholderText('Amount'), '75.50')
    await userEvent.click(screen.getByText('Transfer'))

    await waitFor(() => {
        const result = screen.getByTestId('transaction-result')
        expect(within(result).getByText('tr1')).toBeInTheDocument()
        expect(within(result).getByText(/PENDING/)).toBeInTheDocument()
    })
})

test('button disabled if any field is missing', async () => {
    render(<TransferForm />)

    const button = screen.getByRole('button', { name: /transfer/i })
    expect(button).toBeDisabled()

    await userEvent.type(screen.getByPlaceholderText('From Account ID'), 'acc789')
    expect(button).toBeDisabled()

    await userEvent.type(screen.getByPlaceholderText('To Account ID'), 'acc101')
    expect(button).toBeDisabled()

    await userEvent.type(screen.getByPlaceholderText('Amount'), '100')
    expect(button).toBeEnabled()
})

test('shows error when transfer fails', async () => {
    server.use(
        http.post('http://localhost:8080/api/v1/banking/transfers', () => {
            return HttpResponse.json(
                { message: 'Invalid destination account' },
                { status: 400 }
            )
        })
    )

    render(<TransferForm />)

    await userEvent.type(screen.getByPlaceholderText('From Account ID'), 'acc789')
    await userEvent.type(screen.getByPlaceholderText('To Account ID'), 'invalid')
    await userEvent.type(screen.getByPlaceholderText('Amount'), '50')
    await userEvent.click(screen.getByText('Transfer'))

    await waitFor(() => {
        expect(screen.getByText('Invalid destination account')).toBeInTheDocument()
    })
})