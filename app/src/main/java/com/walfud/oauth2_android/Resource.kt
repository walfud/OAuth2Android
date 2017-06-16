package com.walfud.oauth2_android

/**
 * Created by walfud on 2017/6/15.
 */

class Resource<out T>(val status: Int, val data: T?, val err: String?) {
    companion object {
        val STATUS_SUCCESS = 1
        val STATUS_ERROR = 2
        val STATUS_LOADING = 3

        fun <S> success(data: S?): Resource<S> {
            return Resource(STATUS_SUCCESS, data, null)
        }
        fun <S> fail(error: String?): Resource<S> {
            return Resource(STATUS_ERROR, null, error)
        }
        fun <S> loading(): Resource<S> {
            return Resource(STATUS_LOADING, null, null)
        }
    }
}