package xyz.hisname.fireflyiii.repository.category

import android.app.Application
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.data.local.dao.AppDatabase
import xyz.hisname.fireflyiii.data.local.pref.AppPref
import xyz.hisname.fireflyiii.data.remote.RetrofitBuilder
import xyz.hisname.fireflyiii.data.remote.api.CategoryService
import xyz.hisname.fireflyiii.repository.BaseViewModel
import xyz.hisname.fireflyiii.repository.models.category.CategoryData
import xyz.hisname.fireflyiii.util.retrofitCallback

class CategoryViewModel(application: Application): BaseViewModel(application) {

    val repository: CategoryRepository

    init {
        val categoryDataDao = AppDatabase.getInstance(application).categoryDataDao()
        repository = CategoryRepository(categoryDataDao)
    }

    fun getAllCategory(): LiveData<MutableList<CategoryData>> {
        isLoading.value = true
        val categoryService = RetrofitBuilder.getClient(AppPref(getApplication()).baseUrl,
                AppPref(getApplication()).accessToken)?.create(CategoryService::class.java)
        categoryService?.getCategory()?.enqueue(retrofitCallback({ response ->
            if (response.isSuccessful) {
                val networkData = response.body()?.data
                networkData?.forEachIndexed { _, element ->
                    scope.launch(Dispatchers.IO) { repository.insertCategory(element) }
                }
            } else {
                val responseError = response.errorBody()
                if (responseError != null) {
                    val errorBody = String(responseError.bytes())
                    apiResponse.postValue(errorBody)
                }
            }
        })
        { throwable ->  apiResponse.postValue(throwable.localizedMessage)})
        isLoading.value = false
        return repository.allCategory
    }

}