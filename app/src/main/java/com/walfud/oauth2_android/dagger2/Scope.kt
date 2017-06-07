package com.walfud.oauth2_android.dagger2

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import javax.inject.Scope

/**
 * Created by walfud on 06/06/2017.
 */
@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class Application

@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class Activity

@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class Fragment

