// src/store/slices/__tests__/depositSlice.test.ts
import { describe, it, expect, beforeEach, vi, type Mocked } from 'vitest'
import { configureStore } from '@reduxjs/toolkit'
import axios from 'axios'
import depositSlice, { makeDeposit } from '../../../src/store/slices/depositSlice'

vi.mock('axios', () => {
  const mockAxiosInstance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    defaults: { headers: { common: {} } }, // Mock defaults if needed
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  };

  // Mock axios.create to return the mock instance
  return {
    default: {
      create: vi.fn(() => mockAxiosInstance),
      get: vi.fn(),
      post: vi.fn(),
    },
    mockAxiosInstance,
  };
});
const mockedApi = axios.create() as Mocked<typeof axios>

describe('depositSlice', () => {
  const createTestStore = () =>
    configureStore({
      reducer: { deposits: depositSlice },
      preloadedState: {
        deposits: {
          status: 'idle' as const,
          data: null,
          error: null,
        },
      },
    })

  let store: ReturnType<typeof createTestStore>

  beforeEach(() => {
    vi.clearAllMocks()
    store = createTestStore()
  })

  it('handles successful deposit', async () => {
    mockedApi.post.mockResolvedValueOnce({
      data: { transactionId: 'dep-001', newBalance: '1250.00' },
    })

    await store.dispatch(makeDeposit({ accountId: 'acc-001', amount: '250' }))

    const state = store.getState().deposits
    expect(state.status).toBe('succeeded')
    expect(state.data).toEqual({ transactionId: 'dep-001', newBalance: '1250.00' })
  })

  it('handles deposit failure', async () => {
    mockedApi.post.mockRejectedValueOnce({
      response: { data: { message: 'Account not found' }, status: 404 },
    })

    await store.dispatch(makeDeposit({ accountId: 'invalid', amount: '100' }))

    const state = store.getState().deposits
    expect(state.status).toBe('failed')
    expect(state.error).toEqual('Account not found')
  })
})