package com.ab.hicarerun.network.models.TSScannerModel

import com.google.gson.annotations.SerializedName

data class BarcodeType(
    @SerializedName("Text") val text : String?,
    @SerializedName("Value") val value : String?
)