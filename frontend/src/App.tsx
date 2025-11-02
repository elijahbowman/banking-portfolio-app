import BalanceInquiry from './components/BalanceInquiry'
import DepositForm from './components/DepositForm'
import WithdrawalForm from './components/WithdrawalForm'
import TransferForm from './components/TransferForm'

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <h1 className="text-4xl font-bold text-center mb-12 text-gray-800">Banking Portal</h1>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <BalanceInquiry />
          <DepositForm />
          <WithdrawalForm />
          <TransferForm />
        </div>
      </div>
    </div>
  )
}