package com.vie.mit.data.model.mapper

import com.vie.mit.data.model.domain.auth.SignUpResult
import com.vie.mit.data.model.domain.auth.UserAuthToken
import com.vie.mit.data.model.remote.response.auth.LoginResponse
import com.vie.mit.data.model.remote.response.auth.SignUpResponse

fun LoginResponse.toDomain(): UserAuthToken = UserAuthToken(token = token)

fun SignUpResponse.toDomain(): SignUpResult = SignUpResult(isActive = isActive)
