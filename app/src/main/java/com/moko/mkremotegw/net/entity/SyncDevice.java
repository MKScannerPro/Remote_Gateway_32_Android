package com.moko.mkremotegw.net.entity;


import java.io.Serializable;

public class SyncDevice implements Serializable {

    public String macName;
    public String mac;
    public String lastWill;
    public String publishTopic;
    public String subscribeTopic;
    public String model;
}
