// src/store/slices/__tests__/transferSlice.test.ts
import { describe, it, expect, beforeEach, vi, type Mocked } from 'vitest'
import { configureStore } from '@reduxjs/toolkit'
import axios from 'axios'
import transferSlice, {
  makeTransfer,
  resetTransferState,
  type TransferState,
} from '../../../src/store/slices/transferSlice'

vi.mock('axios', () => {
  const mockAxiosInstance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    defaults: { headers: { common: {} } },
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  };

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

describe('transferSlice', () => {
  // Fully typed store
  const createTestStore = () =>
    configureStore({
      reducer: { transfers: transferSlice },
      preloadedState: {
        transfers: {
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

  it('handles successful transfer', async () => {
    const responsePayload = {
      transactionId: 'tr-123',
      status: 'COMPLETED',
      newFromBalance: '850.00',
      newToBalance: '1150.00',
    }

    mockedApi.post.mockResolvedValueOnce({ data: responsePayload })

    await store.dispatch(
      makeTransfer({
        fromAccountId: 'acc-001',
        toAccountId: 'acc-002',
        amount: '150',
      })
    )

    const state = store.getState().transfers as TransferState
    expect(state.status).toBe('succeeded')
    expect(state.data).toEqual(responsePayload)
    expect(state.error).toBeNull()
  })

  it('handles failed transfer (API error)', async () => {
    mockedApi.post.mockRejectedValueOnce({
      response: { data: { message: 'Invalid destination account' }, status: 400 },
    })

    await store.dispatch(
      makeTransfer({
        fromAccountId: 'acc-001',
        toAccountId: 'invalid',
        amount: '100',
      })
    )

    const state = store.getState().transfers as TransferState
    expect(state.status).toBe('failed')
    expect(state.error).toEqual('Invalid destination account')
  })

  it('resets transfer state', () => {
    // Start with succeeded state
    store = configureStore({
      reducer: { transfers: transferSlice },
      preloadedState: {
        transfers: {
          status: 'succeeded' as const,
          data: { transactionId: 'tr-999' },
          error: null,
        },
      },
    })

    store.dispatch(resetTransferState())

    const state = store.getState().transfers as TransferState
    expect(state.status).toBe('idle')
    expect(state.data).toBeNull()
    expect(state.error).toBeNull()
  })
})