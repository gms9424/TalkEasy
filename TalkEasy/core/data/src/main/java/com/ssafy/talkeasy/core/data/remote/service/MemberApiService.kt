package com.ssafy.talkeasy.core.data.remote.service

import com.ssafy.talkeasy.core.data.remote.datasource.common.DefaultResponse
import com.ssafy.talkeasy.core.domain.entity.response.MemberInfo
import retrofit2.http.GET

interface MemberApiService {

    @GET("/api/members")
    suspend fun requestMemberInfo(): DefaultResponse<MemberInfo>
}