package com.wanghao.eldercare.eldercaresystem.ws;

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
