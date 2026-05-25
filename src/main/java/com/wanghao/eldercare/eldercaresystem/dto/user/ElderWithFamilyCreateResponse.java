package com.wanghao.eldercare.eldercaresystem.dto.user;

public class ElderWithFamilyCreateResponse {

    private UserAdminDTO elder;
    private UserAdminDTO family;

    public UserAdminDTO getElder() {
        return elder;
    }

    public void setElder(UserAdminDTO elder) {
        this.elder = elder;
    }

    public UserAdminDTO getFamily() {
        return family;
    }

    public void setFamily(UserAdminDTO family) {
        this.family = family;
    }
}
