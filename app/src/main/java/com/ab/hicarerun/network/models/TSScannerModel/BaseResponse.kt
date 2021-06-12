package com.ab.hicarerun.network.models.TSScannerModel

import com.google.gson.annotations.SerializedName

data class BaseResponse (
    @SerializedName("IsSuccess") val isSuccess : Boolean?,
    @SerializedName("Data") val data : String?,
    @SerializedName("ErrorMessage") val errorMessage : String?,
    @SerializedName("Param1") val param1 : Boolean?,
    @SerializedName("ResponseMessage") val responseMessage : String?
)