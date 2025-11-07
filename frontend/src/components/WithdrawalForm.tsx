import { useState } from 'react'
import { withdraw } from '../services/api'
import type { TransactionRequest, TransactionResponse } from '../types'

export default function WithdrawalForm() {
    const [form, setForm] = useState<TransactionRequest>({ accountId: '', amount: '' })
    const [result, setResult] = useState<TransactionResponse | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setLoading(true)
        setError('')
        try {
            const data = await withdraw(form)
            setResult(data)
        } catch (err: any) {
            setError(err.response?.data?.message || 'Withdrawal failed')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="max-w-md mx-auto p-6 bg-white rounded-lg shadow-md">
            <h2 className="text-2xl font-bold mb-4 text-gray-800">Withdraw Funds</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
                <input
                    type="text"
                    placeholder="Account ID"
                    value={form.accountId}
                    onChange={(e) => setForm({ ...form, accountId: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 text-black rounded-md focus:ring-2 focus:ring-red-500"
                />
                <input
                    type="number"
                    step="0.01"
                    placeholder="Amount"
                    value={form.amount}
                    onChange={(e) => setForm({ ...form, amount: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 text-black rounded-md focus:ring-2 focus:ring-red-500"
                />
                <button
                    type="submit"
                    disabled={loading || !form.accountId || !form.amount}
                    className="w-full py-2 px-4 bg-red-600 text-white font-medium rounded-md hover:bg-red-700 disabled:bg-gray-400"
                >
                    {loading ? 'Processing...' : 'Withdraw'}
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