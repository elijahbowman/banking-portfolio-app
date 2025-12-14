import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getRealCard, getVCNs, issueVCN } from '../../services/api';

interface CardState {
  realCard: RealCard | null;
  vcns: VirtualCardToken[];
  realCardStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  vcnsStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  createVCNStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

interface RealCard {
  cardId: string;
  accountId: string;
  cardNumber: string;
  cardHolderName: string;
  expiryDate: string;
  status: string;
}

interface VirtualCardToken {
  tokenId: string;
  realCardId: string;
  vcn: string;
  spendLimit: number;
  expiresAt: string;
  createdAt: string;
  status: string;
}

const initialState: CardState = {
  realCard: null,
  vcns: [],
  realCardStatus: 'idle',
  vcnsStatus: 'idle',
  createVCNStatus: 'idle',
  error: null,
};

export const fetchRealCard = createAsyncThunk(
  'card/fetchRealCard',
  async (accountId: string, { rejectWithValue }) => {
    try {
      return await getRealCard(accountId);
    } catch (err: any) {
      return rejectWithValue(err.response?.data?.message || 'Failed to fetch real card');
    }
  }
);

export const fetchVCNs = createAsyncThunk(
  'card/fetchVCNs',
  async (realCardId: string, { rejectWithValue }) => {
    try {
      return await getVCNs(realCardId);
    } catch (err: any) {
      return rejectWithValue(err.response?.data?.message || 'Failed to fetch VCNs');
    }
  }
);

export const createVCN = createAsyncThunk(
  'card/createVCN',
  async ({ realCardId, limit }: { realCardId: string; limit: number }, { rejectWithValue }) => {
    try {
      return await issueVCN(realCardId, limit);
    } catch (err: any) {
      return rejectWithValue(err.response?.data?.message || 'Failed to issue VCN');
    }
  }
);

const cardSlice = createSlice({
  name: 'card',
  initialState,
  reducers: {
    resetCard: (state) => {
      state.realCard = null;
      state.vcns = [];
      state.realCardStatus = 'idle';
      state.vcnsStatus = 'idle',
      state.createVCNStatus = 'idle',
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchRealCard.pending, (state) => {
        state.error = null;
        state.vcns = [];
        state.realCard = null;
        state.vcnsStatus = 'idle',
        state.createVCNStatus = 'idle',
        state.realCardStatus = 'loading';
      })
      .addCase(fetchRealCard.fulfilled, (state, action) => {
        state.realCardStatus = 'succeeded';
        state.error = null;
        state.realCard = action.payload;
      })
      .addCase(fetchRealCard.rejected, (state, action) => {
        state.realCardStatus = 'failed';
        state.error = action.payload as string;
      })
      .addCase(fetchVCNs.pending, (state) => {
        state.error = null;
        state.vcnsStatus = 'loading';
      })
      .addCase(fetchVCNs.fulfilled, (state, action) => {
        state.error = null;
        state.vcnsStatus = 'succeeded';
        state.vcns = action.payload;
      })
      .addCase(fetchVCNs.rejected, (state, action) => {
        state.vcnsStatus = 'failed';
        state.error = action.payload as string;
      })
      .addCase(createVCN.pending, (state) => {
        state.error = null;
        state.createVCNStatus = 'loading';
      })
      .addCase(createVCN.fulfilled, (state, action) => {
        state.error = null;
        state.createVCNStatus = 'succeeded';
        state.vcns = [...state.vcns, action.payload];
      })
      .addCase(createVCN.rejected, (state, action) => {
        state.createVCNStatus = 'failed';
        state.error = action.payload as string;
      });
  },
});

export const { resetCard } = cardSlice.actions;
export default cardSlice.reducer;