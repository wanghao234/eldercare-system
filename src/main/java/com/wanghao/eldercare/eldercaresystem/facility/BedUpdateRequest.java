package com.wanghao.eldercare.eldercaresystem.facility;

import com.fasterxml.jackson.annotation.JsonAlias;

public class BedUpdateRequest {

    @JsonAlias({"bedNo", "bed_no"})
    private String bedCode;

    private String status;

    public String getBedCode() {
        return bedCode;
    }

    public void setBedCode(String bedCode) {
        this.bedCode = bedCode;
    }

    public String resolveBedCode() {
        return bedCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
