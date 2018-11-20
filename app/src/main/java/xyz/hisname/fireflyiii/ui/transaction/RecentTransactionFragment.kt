package xyz.hisname.fireflyiii.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_dashboard_recent_transaction.*
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.models.transaction.TransactionData
import xyz.hisname.fireflyiii.repository.transaction.TransactionsViewModel
import xyz.hisname.fireflyiii.ui.base.BaseFragment
import xyz.hisname.fireflyiii.util.extension.create
import xyz.hisname.fireflyiii.util.extension.getViewModel
import java.util.*

class RecentTransactionFragment: BaseFragment() {

    private val transactionViewModel by lazy { getViewModel(TransactionsViewModel::class.java) }
    private var dataAdapter = ArrayList<TransactionData>()
    private lateinit var rtAdapter: TransactionRecyclerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.create(R.layout.fragment_dashboard_recent_transaction, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTransaction()
    }

    private fun loadTransaction() {
        dataAdapter.clear()
        recentTransactionList.layoutManager = LinearLayoutManager(requireContext())
        recentTransactionList.addItemDecoration(DividerItemDecoration(recentTransactionList.context,
                DividerItemDecoration.VERTICAL))
        transactionViewModel.getRecentTransaction(5).observe(this, Observer {
            dataAdapter = ArrayList(it)
            if (dataAdapter.size == 0) {
                recentTransactionList.isGone = true
                noTransactionText.isVisible = true
            } else {
                recentTransactionList.isVisible = true
                noTransactionText.isGone = true
                rtAdapter = TransactionRecyclerAdapter(dataAdapter, "recent")
                recentTransactionList.adapter = rtAdapter
                rtAdapter.apply { recentTransactionList.adapter as TransactionRecyclerAdapter }
                rtAdapter.notifyDataSetChanged()
            }
        })
        transactionViewModel.isLoading.observe(this, Observer {
            if(it == true){
                transactionLoader.bringToFront()
                transactionLoader.show()
            } else {
                transactionLoader.hide()
            }
        })
    }

}