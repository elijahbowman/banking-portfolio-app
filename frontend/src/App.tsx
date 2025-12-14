import BalanceInquiry from './components/BalanceInquiry'
import DepositForm from './components/DepositForm'
import WithdrawalForm from './components/WithdrawalForm'
import TransferForm from './components/TransferForm'
import CardDashboard from './components/CardDashboard';

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto space-y-8">
        <h1 className="text-3xl font-bold text-center mb-8">Banking Portfolio</h1>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <BalanceInquiry />
          <DepositForm />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <WithdrawalForm />
          <TransferForm />
        </div>

        <div className="mt-8">
          <CardDashboard />
        </div>
      </div>
    </div>
  );
}