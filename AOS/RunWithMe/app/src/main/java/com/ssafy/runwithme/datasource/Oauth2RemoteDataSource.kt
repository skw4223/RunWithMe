package com.ssafy.runwithme.datasource

import com.ssafy.runwithme.api.Oauth2Api
import com.ssafy.runwithme.model.response.OauthResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Oauth2RemoteDataSource @Inject constructor(
    private val oauth2Api: Oauth2Api
){
    fun googleLogin(code: String): Flow<OauthResponse> = flow {
        emit(oauth2Api.googleLogin(code))
    }

    fun naverLogin(code: String, email: String): Flow<OauthResponse> = flow {
        emit(oauth2Api.naverLogin(code, email))
    }

    fun kakaoLogin(code: String): Flow<OauthResponse> = flow {
        emit(oauth2Api.kakaoLogin(code))
    }
}