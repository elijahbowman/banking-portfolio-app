import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { type TransactionRequest } from '../../types';
import { getApi } from '../../services/api';

interface DepositState {
    status: 'idle' | 'loading' | 'succeeded' | 'failed';
    data: any | null;
    error: any | null;
}

const initialState: DepositState = {
    status: 'idle',
    data: null,
    error: null,
};

const api = await getApi();

export const makeDeposit = createAsyncThunk(
    'deposits/makeDeposit',
    async ({ accountId, amount }: TransactionRequest, { rejectWithValue }) => {
        try {
            const response = await api.post('/deposits', { accountId, amount });
            return response.data;
        } catch (error: any) {
            console.log('deposit slice error: ')
            console.log(error?.response?.data);
            return rejectWithValue(error.response?.data?.message || 'Deposit failed');
        }
    }
);

const depositSlice = createSlice({
    name: 'deposits',
    initialState,
    reducers: {
        resetDepositState: (state) => {
            state.status = 'idle';
            state.data = null;
            state.error = null;
        },
    },
    extraReducers: (builder) => {
        builder
            .addCase(makeDeposit.pending, (state) => {
                state.status = 'loading';
            })
            .addCase(makeDeposit.fulfilled, (state, action) => {
                state.status = 'succeeded';
                state.data = action.payload;
            })
            .addCase(makeDeposit.rejected, (state, action) => {
                state.status = 'failed';
                state.error = action.payload ?? action.error;
            });
    },
});

export default depositSlice.reducer;