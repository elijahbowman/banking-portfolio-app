import { configureStore } from '@reduxjs/toolkit';
import depositReducer from './slices/depositSlice';
import transferReducer from './slices/transferSlice';
import cardReducer from './slices/cardSlice';

export const store = configureStore({
  reducer: {
    deposits: depositReducer,
    transfers: transferReducer,
    card: cardReducer
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;