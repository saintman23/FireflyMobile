package xyz.hisname.fireflyiii.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.GlobalViewModel
import xyz.hisname.fireflyiii.util.animation.CircularReveal
import xyz.hisname.fireflyiii.util.extension.bindView
import xyz.hisname.fireflyiii.util.extension.getViewModel
import kotlin.coroutines.CoroutineContext

abstract class BaseFragment: Fragment() {

    protected val progressLayout by bindView<View>(R.id.progress_overlay)
    protected val extendedFab by bindView<ExtendedFloatingActionButton>(R.id.fab_action)
    protected val fragmentContainer by bindView<FrameLayout>(R.id.fragment_container)
    protected val revealX by lazy { arguments?.getInt("revealX") ?: 0 }
    protected val revealY by lazy { arguments?.getInt("revealY") ?: 0 }
    protected  val globalViewModel by lazy { getViewModel(GlobalViewModel::class.java) }
    private var parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Main
    protected  val scope = CoroutineScope(coroutineContext)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        requireActivity().findViewById<AppBarLayout>(R.id.activity_appbar)?.setExpanded(true,true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    protected fun showReveal(rootLayout: View) = CircularReveal(rootLayout).showReveal(revealX, revealY)

    protected fun isDarkMode() = globalViewModel.isDark
}