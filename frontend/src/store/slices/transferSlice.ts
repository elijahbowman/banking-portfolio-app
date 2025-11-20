// frontend/src/store/slices/transferSlice.ts
import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import { type TransactionRequest } from '../../types';
import { getApi } from '../../services/api';

export interface TransferState {
    status: 'idle' | 'loading' | 'succeeded' | 'failed';
    data: any | null;
    error: any | null;
}

const initialState: TransferState = {
    status: 'idle',
    data: null,
    error: null,
};

const api = await getApi();

export const makeTransfer = createAsyncThunk(
  'transfers/makeTransfer',
  async (request: TransactionRequest, { rejectWithValue }) => {
    try {
      const response = await api.post('/transfers', request);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Deposit failed');
    }
  }
);

const transferSlice = createSlice({
  name: 'transfers',
  initialState,
  reducers: {
    resetTransferState: (state) => {
      state.status = 'idle';
      state.data = null;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(makeTransfer.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(makeTransfer.fulfilled, (state, action: PayloadAction<any>) => {
        state.status = 'succeeded';
        state.data = action.payload;
      })
      .addCase(makeTransfer.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? action.error;
      });
  },
});

export const { resetTransferState } = transferSlice.actions;
export default transferSlice.reducer;