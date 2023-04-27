package com.moko.mkremotegw.net;

import okhttp3.MediaType;

public class Urls {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 测试环境
     */
    private static final String HOST_URL = "http://192.168.0.115/prod-api/";
    /**
     * 正式环境
     */
//    private static final String HOST_URL = "http://erp.mokotechnology.com/api/production/";

    /**
     * 生产测试
     */
    // 用户登录
    public static final String URL_LOGIN = HOST_URL + "auth/login";
    // req:
    // {"username":"lwz","password":"123456"}
    // resp:
    // {
    //    "code": 200,
    //    "msg": null,
    //    "data": {
    //        "access_token": "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2lkIjoxMDYsInVzZXJfa2V5IjoiNGZkMWQzZWEtOWUyMy00YTMxLWE0YmYtNzAyZDY0MjU3MjY3IiwidXNlcm5hbWUiOiJsd3oifQ.Vd8oWDpJpnX__SaEpq65VKkyQdHCHVdWY-RurdZowroBHmean7pDBqgFOFeuW4eA84Asw8f-cBcKXs7RS382Lg",
    //        "expires_in": 720
    //    }
    //}

    // 同步网关
    public static final String URL_SYNC_GATEWAY = HOST_URL + "mqtt/mqttgateway/batchAdd ";
    // req:
    // [{"macName":"","mac":"","model":"","publishTopic":"","subscribeTopic":"","lastWill":""}]
    // resp:
    // {
    //    "code": 200,
    //    "msg": null,
    //    "data": {
    //
    //    }
    //}
}
