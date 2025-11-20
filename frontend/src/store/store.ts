import { configureStore } from '@reduxjs/toolkit';
import depositReducer from './slices/depositSlice';
import transferReducer from './slices/transferSlice';

export const store = configureStore({
  reducer: {
    deposits: depositReducer,
    transfers: transferReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;