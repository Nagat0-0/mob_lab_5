package com.example.cs2skinswipe.network

import retrofit2.http.GET

interface CsgoApiService {
    @GET("skins.json")
    suspend fun getSkins(): List<ApiSkin>
}