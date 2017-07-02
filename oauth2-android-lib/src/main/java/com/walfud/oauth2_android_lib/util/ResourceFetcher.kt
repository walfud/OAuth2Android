package com.walfud.oauth2_android_lib.util

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData

/**
 * Created by walfud on 2017/6/15.
 */

abstract class ResourceFetcher<T>(val loadingMessage: String? = null) {
    val result = MediatorLiveData<Resource<T>>()

    val memoryData = MutableLiveData<T>()
    open fun memory(): LiveData<T> {
        memoryData.postValue(null)
        return memoryData
    }

    val diskData = MutableLiveData<T>()
    open fun disk(): LiveData<T> {
        diskData.postValue(null)
        return diskData
    }

    open fun network(): LiveData<MyResponse<T>> {
        val networkData = MutableLiveData<MyResponse<T>>()
        networkData.postValue(MyResponse.valueOf(RuntimeException()))
        return networkData
    }

    open fun valid(t: T?) = t != null

    fun fetch(): ResourceFetcher<T> {
        result.postValue(Resource.loading(loadingMessage))

        // Try memory
        val memoryData = memory()
        result.addSource(memoryData, {
            result.removeSource(memoryData)

            if (valid(it)) {
                val value = Resource.success(it)
                result.postValue(value)
            } else {
                // Try disk
                val diskData = disk()
                result.addSource(diskData, {
                    result.removeSource(diskData)

                    if (valid(it)) {
                        val value = Resource.success(it)
                        result.postValue(value)
                    } else {
                        // Try network
                        val networkData = network()
                        result.addSource(networkData, { myResponse ->
                            result.removeSource(networkData)

                            myResponse!!
                            if (myResponse.isSuccess()) {
                                val value = Resource.success(myResponse.body)
                                result.postValue(value)
                            } else {
                                result.postValue(Resource.fail(myResponse.err))
                            }
                        })
                    }
                })
            }
        })

        return this
    }

    fun asLiveData() = result
}