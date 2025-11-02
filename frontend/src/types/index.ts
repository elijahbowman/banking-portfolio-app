export interface BalanceResponse {
    accountId: string
    balance: string
}

export interface TransactionRequest {
    accountId?: string
    fromAccountId?: string
    toAccountId?: string
    amount: string
}

export interface TransactionResponse {
    transactionId: string
    status: 'PENDING' | 'COMPLETED' | 'FAILED'
}