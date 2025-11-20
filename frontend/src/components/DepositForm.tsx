import { useState } from 'react'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { makeDeposit } from '../store/slices/depositSlice'

export default function DepositForm() {
  const [form, setForm] = useState({ accountId: '', amount: '' })
  const dispatch = useAppDispatch()
  const { data: result, status, error } = useAppSelector(state => state.deposits)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    dispatch(makeDeposit({
      accountId: form.accountId,
      amount: form.amount
    }))
  }

    return (
        <div className="max-w-md mx-auto p-6 bg-white rounded-lg shadow-md">
            <h2 className="text-2xl font-bold mb-4 text-gray-800">Deposit Funds</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
                <input
                    type="text"
                    placeholder="Account ID"
                    value={form.accountId}
                    onChange={(e) => setForm({ ...form, accountId: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 text-black rounded-md focus:ring-2 focus:ring-green-500"
                />
                <input
                    type="number"
                    step="0.01"
                    placeholder="Amount"
                    value={form.amount}
                    onChange={(e) => setForm({ ...form, amount: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 text-black rounded-md focus:ring-2 focus:ring-green-500"
                />
                <button
                    type="submit"
                    disabled={status == 'loading' || !form.accountId || !form.amount}
                    className="w-full py-2 px-4 bg-green-600 text-white font-medium rounded-md hover:bg-green-700 disabled:bg-gray-400"
                >
                    {status == 'loading' ? 'Processing...' : 'Deposit'}
                </button>
            </form>

            {result && (
                <div data-testid="transaction-result" className="mt-6 p-4 bg-green-50 border border-green-200 rounded-md">
                    <p className="text-sm text-green-600">Transaction ID: <strong>{result.transactionId}</strong></p>
                    <p className="text-sm font-medium text-green-800">Status: {result.status}</p>
                </div>
            )}

            {error && <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-md text-red-700">{error}</div>}
        </div>
    )
}