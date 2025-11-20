import { useState } from 'react'
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { makeTransfer } from '../store/slices/transferSlice'

export default function TransferForm() {
  const [form, setForm] = useState({
    fromAccountId: '',
    toAccountId: '',
    amount: '',
  })
  const dispatch = useAppDispatch()
  const { data: result, status, error } = useAppSelector(state => state.transfers)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    dispatch(makeTransfer({
      fromAccountId: form.fromAccountId,
      toAccountId: form.toAccountId,
      amount: form.amount,
    }))
  }

    return (
        <div className="max-w-md mx-auto p-6 bg-white rounded-lg shadow-md">
            <h2 className="text-2xl font-bold mb-4 text-gray-800">Transfer Funds</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
                <input
                    type="text"
                    placeholder="From Account ID"
                    value={form.fromAccountId}
                    onChange={(e) => setForm({ ...form, fromAccountId: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 text-black rounded-md focus:ring-2 focus:ring-purple-500"
                />
                <input
                    type="text"
                    placeholder="To Account ID"
                    value={form.toAccountId}
                    onChange={(e) => setForm({ ...form, toAccountId: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 text-black rounded-md focus:ring-2 focus:ring-purple-500"
                />
                <input
                    type="number"
                    step="0.01"
                    placeholder="Amount"
                    value={form.amount}
                    onChange={(e) => setForm({ ...form, amount: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 text-black rounded-md focus:ring-2 focus:ring-purple-500"
                />
                <button
                    type="submit"
                    disabled={status == 'loading' || !form.fromAccountId || !form.toAccountId || !form.amount}
                    className="w-full py-2 px-4 bg-purple-600 text-white font-medium rounded-md hover:bg-purple-700 disabled:bg-gray-400"
                >
                    {status == 'loading' ? 'Processing...' : 'Transfer'}
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