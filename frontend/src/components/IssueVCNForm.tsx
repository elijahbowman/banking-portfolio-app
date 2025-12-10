import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks'
import { createVCN } from '../store/slices/cardSlice';
import { type RootState } from '../store/store';

export default function IssueVCNForm({ realCardId }: { realCardId: string }) {
  const [limit, setLimit] = useState('');
  const dispatch = useAppDispatch();
  const { status, error } = useAppSelector((state: RootState) => state.card);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    dispatch(createVCN({ realCardId, limit: Number(limit) }));
  };

  return (
    <div className="p-6 bg-white rounded-lg shadow">
      <h3 className="text-xl font-bold mb-4">Issue Virtual Card Number</h3>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium">Spending Limit ($)</label>
          <input
            type="number"
            value={limit}
            onChange={(e) => setLimit(e.target.value)}
            placeholder="500.00"
            min="0.01"
            step="0.01"
            className="mt-1 block w-full rounded border-gray-300"
            required
          />
        </div>
        <button
          type="submit"
          disabled={status === 'loading'}
          className="w-full bg-indigo-600 text-white py-2 rounded hover:bg-indigo-700 disabled:opacity-50"
        >
          {status === 'loading' ? 'Issuing...' : 'Issue VCN'}
        </button>
      </form>

      {error && <p className="mt-4 text-red-700 border border-red-200">{error}</p>}

      {status === 'succeeded' && <p className="mt-4 text-green-600">VCN issued successfully!</p>}
    </div>
  );
}