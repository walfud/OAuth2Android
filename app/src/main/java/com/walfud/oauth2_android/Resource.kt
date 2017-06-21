package com.walfud.oauth2_android

/**
 * Created by walfud on 2017/6/15.
 */

class Resource<out T>(val status: Int = STATUS_DEFAULT, val data: T? = null, val err: String? = null, val loading: String? = null) {
    companion object {
        val STATUS_DEFAULT = 0
        val STATUS_SUCCESS = 1
        val STATUS_ERROR = 2
        val STATUS_LOADING = 3

        fun <S> success(data: S?): Resource<S> {
            return Resource(STATUS_SUCCESS, data, null, null)
        }

        fun <S> fail(error: String?): Resource<S> {
            return Resource(STATUS_ERROR, null, error, null)
        }

        fun <S> loading(loadingMessage: String? = null): Resource<S> {
            return Resource(STATUS_LOADING, null, null, loadingMessage)
        }
    }
}