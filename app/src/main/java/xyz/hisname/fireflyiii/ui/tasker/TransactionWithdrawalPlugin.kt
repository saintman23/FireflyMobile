package xyz.hisname.fireflyiii.ui.tasker

import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.ui.transaction.addtransaction.AddTransactionFragment

class TransactionWithdrawalPlugin: BaseTransactionPlugin() {

    override fun navigateFragment() {
        supportFragmentManager.commit {
            replace(R.id.addTransactionFrame, AddTransactionFragment().apply {
                arguments = bundleOf("transactionType" to "Withdrawal", "isTasker" to true)
            })
        }
    }

}