import React, { useEffect, useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { fetchRealCard, fetchVCNs } from '../store/slices/cardSlice';
import { type RootState } from '../store/store';
import IssueVCNForm from './IssueVCNForm';

export default function CardDashboard() {
    const [accountId, setAccountId] = useState('');
  const [submittedId, setSubmittedId] = useState('');
  const dispatch = useAppDispatch();
  const { realCard, vcns, realCardStatus, vcnsStatus, error } = useAppSelector((state: RootState) => state.card);
  const [loading, setLoading] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true)
    if (accountId.trim()) {
      setSubmittedId(accountId.trim());
    }
    setLoading(false)
  };

  useEffect(() => {
    if (submittedId) {
      dispatch(fetchRealCard(submittedId));
    }
  }, [dispatch, submittedId]);

  useEffect(() => {
    if (realCard) {
      dispatch(fetchVCNs(realCard.cardId));
    }
  }, [dispatch, realCard]);

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-2xl font-bold mb-4 text-center text-gray-800">Card Dashboard</h2>

      {/* Account ID Form */}
      <form onSubmit={handleSubmit} className="mb-6">
        <div className="flex gap-2 max-[480px]:flex-col">
          <input
            type="text"
            value={accountId}
            onChange={(e) => setAccountId(e.target.value)}
            placeholder="Enter Account ID (e.g., acc123)"
            className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-indigo-500"
            required
          />
          <button
            type="submit"
            disabled={loading || !accountId}
            className="px-6 py-2 bg-indigo-600 text-white font-medium rounded-md hover:bg-indigo-700 transition disabled:bg-gray-400"
          >
            {loading ? 'Processing...' : 'Load Card'}
          </button>
        </div>
      </form>

      {/* Results */}
      {submittedId && (
        <>
          {realCardStatus === 'loading' && (
            <p className="text-center text-gray-600">Loading card details...</p>
          )}

          {error && (
            <p className="text-red-700 text-center bg-red-50 p-3 rounded-md border border-red-200">{error}</p>
          )}

          {realCard && (
            <>
              {/* Real Card */}
              <div className="bg-linear-to-r from-indigo-50 to-blue-50 p-6 rounded-lg border border-indigo-200 mb-6">
                <h3 className="text-lg font-semibold mb-4">Primary Card</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-gray-600">Card Number</p>
                    <p className="font-mono text-lg">**** **** **** {realCard.cardNumber.slice(-4)}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Cardholder</p>
                    <p className="font-semibold">{realCard.cardHolderName}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Expiry</p>
                    <p className="font-semibold">{realCard.expiryDate}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Status</p>
                    <p className="text-green-600 font-semibold">{realCard.status}</p>
                  </div>
                </div>
              </div>

              {/* Virtual Cards */}
              <h3 className="text-xl font-bold mb-4">Virtual Cards</h3>
              {vcnsStatus == 'loading' ? <p className="text-center text-gray-600">Loading virtual cards...</p> :
                vcns.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {vcns.map((vcn) => (
                    <div
                      key={vcn.tokenId}
                      className="bg-gradient-to-br from-purple-50 to-pink-50 p-4 rounded-lg border border-purple-200"
                    >
                      <p className="font-mono text-lg mb-2">{vcn.vcn}</p>
                      <div className="text-sm space-y-1">
                        <p><strong>Limit:</strong> ${vcn.spendLimit.toFixed(2)}</p>
                        <p><strong>Expires:</strong> {new Date(vcn.expiresAt).toLocaleDateString()}</p>
                        <p><strong>Status:</strong> <span className="text-green-600">{vcn.status}</span></p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-center text-gray-600">No virtual cards issued yet.</p>
              )}

              <IssueVCNForm realCardId={realCard.cardId} />
            </>
          )}
        </>
      )}
    </div>
  );
}