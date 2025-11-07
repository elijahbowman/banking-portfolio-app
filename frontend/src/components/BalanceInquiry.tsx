import { useState } from 'react'
import { getBalance } from '../services/api'
import type { BalanceResponse } from '../types'

export default function BalanceInquiry() {
    const [accountId, setAccountId] = useState('')
    const [balance, setBalance] = useState<BalanceResponse | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!accountId.trim()) return

        setLoading(true)
        setError('')
        try {
            const data = await getBalance(accountId)
            setBalance(data)
        } catch (err: any) {
            setError(err.response?.data?.message || 'Failed to fetch balance')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="w-full max-w-md mx-auto p-6 bg-white rounded-lg shadow-md">
            <h2 className="text-2xl font-bold mb-4 text-gray-800">Check Balance</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
                <input
                    type="text"
                    placeholder="Account ID"
                    value={accountId}
                    onChange={(e) => setAccountId(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 text-black rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <button
                    type="submit"
                    disabled={loading || !accountId.trim()}
                    className="w-full py-2 px-4 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 disabled:bg-gray-400 transition"
                >
                    {loading ? 'Loading...' : 'Get Balance'}
                </button>
            </form>

            {balance && (
                <div data-testid="balance-result" className="mt-6 p-4 bg-green-50 border border-green-200 rounded-md">
                    <p className="text-sm text-green-600">Account: <strong>{balance.accountId}</strong></p>
                    <p className="text-lg font-bold text-green-800">Balance: ${balance.balance}</p>
                </div>
            )}

            {error && (
                <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-md text-red-700">
                    {error}
                </div>
            )}
        </div>
    )
}