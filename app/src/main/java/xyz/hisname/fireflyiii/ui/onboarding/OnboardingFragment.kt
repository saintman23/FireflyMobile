package xyz.hisname.fireflyiii.ui.onboarding

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_onboarding.*
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.data.remote.firefly.FireflyClient
import xyz.hisname.fireflyiii.ui.HomeActivity
import xyz.hisname.fireflyiii.util.extension.*

class OnboardingFragment: Fragment() {

    private val authActivityViewModel by lazy { getViewModel(AuthActivityViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.create(R.layout.fragment_onboarding, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onboarding_text.text = "Hang on..."
        hideKeyboard()
        ObjectAnimator.ofInt(onboarding_progress,"progress", 10).start()
        FireflyClient.destroyInstance()
        ObjectAnimator.ofInt(onboarding_progress,"progress", 30).start()
        getUser()
    }


    private fun getUser(){
        onboarding_text.text = "Retrieving your data..."
        ObjectAnimator.ofInt(onboarding_progress,"progress", 45).start()
        ObjectAnimator.ofInt(onboarding_progress,"progress", 60).start()
        zipLiveData(authActivityViewModel.getUser(),authActivityViewModel.userSystem()).observe(viewLifecycleOwner) { multipleLiveData ->
            ObjectAnimator.ofInt(onboarding_progress,"progress", 90).start()
            onboarding_text.text = "Almost there!"
            if(multipleLiveData.first && multipleLiveData.second){
                startActivity(Intent(requireActivity(), HomeActivity::class.java))
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                requireActivity().finish()
            }
        }
    }

}