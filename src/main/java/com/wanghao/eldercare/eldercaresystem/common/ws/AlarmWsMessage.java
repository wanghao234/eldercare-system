package com.wanghao.eldercare.eldercaresystem.common.ws;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;

public class AlarmWsMessage {
    private String event;
    private AlarmWsData data;

    public AlarmWsMessage() {
    }

    public AlarmWsMessage(String event, AlarmWsData data) {
        this.event = event;
        this.data = data;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public AlarmWsData getData() {
        return data;
    }

    public void setData(AlarmWsData data) {
        this.data = data;
    }
}
