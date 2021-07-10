package com.ab.hicarerun.network.models.ModelQRCode;

import com.ab.hicarerun.network.models.NPSModel.NPSData;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Arjun Bhatt on 4/1/2020.
 */
public class QRCodeResponse {
    @SerializedName("IsSuccess")
    @Expose
    private Boolean isSuccess;
    @SerializedName("com.ab.hicarerun.network.models.TSScannerModel.Data")
    @Expose
    private QRCode data;
    @SerializedName("ErrorMessage")
    @Expose
    private String errorMessage;
    @SerializedName("Param1")
    @Expose
    private Boolean param1;
    @SerializedName("ResponseMessage")
    @Expose
    private String responseMessage;

    public Boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public QRCode getData() {
        return data;
    }

    public void setData(QRCode data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Boolean getParam1() {
        return param1;
    }

    public void setParam1(Boolean param1) {
        this.param1 = param1;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }
}
