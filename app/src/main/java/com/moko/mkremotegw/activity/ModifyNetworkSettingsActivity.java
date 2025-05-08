package com.moko.mkremotegw.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkremotegw.AppConstants;
import com.moko.mkremotegw.R;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityNetworkSettingsBinding;
import com.moko.mkremotegw.entity.MQTTConfig;
import com.moko.mkremotegw.entity.MokoDevice;
import com.moko.mkremotegw.utils.SPUtiles;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.support.remotegw.MQTTConstants;
import com.moko.lib.mqtt.MQTTSupport;
import com.moko.lib.mqtt.entity.MsgConfigResult;
import com.moko.lib.mqtt.entity.MsgReadResult;
import com.moko.lib.mqtt.event.DeviceOnlineEvent;
import com.moko.lib.mqtt.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class ModifyNetworkSettingsActivity extends BaseActivity<ActivityNetworkSettingsBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;
    public Handler mHandler;

    @Override
    protected void onCreate() {
        mBind.cbDhcp.setOnCheckedChangeListener((buttonView, isChecked) -> mBind.clIp.setVisibility(isChecked ? View.GONE : View.VISIBLE));
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDevice.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getNetworkSettings();
    }

    @Override
    protected ActivityNetworkSettingsBinding getViewBinding() {
        return ActivityNetworkSettingsBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String topic = event.getTopic();
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message))
            return;
        int msg_id;
        try {
            JsonObject object = new Gson().fromJson(message, JsonObject.class);
            JsonElement element = object.get("msg_id");
            msg_id = element.getAsInt();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_NETWORK_SETTINGS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            int enable = result.data.get("dhcp_en").getAsInt();
            mBind.cbDhcp.setChecked(enable == 1);
            mBind.clIp.setVisibility(enable == 1 ? View.GONE : View.VISIBLE);

            mBind.etIp.setText(result.data.get("ip").getAsString());
            mBind.etMask.setText(result.data.get("netmask").getAsString());
            mBind.etGateway.setText(result.data.get("gw").getAsString());
            mBind.etDns.setText(result.data.get("dns").getAsString());
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_NETWORK_SETTINGS) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.result_code == 0) {
                ToastUtils.showToast(this, "Set up succeed");
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        super.offline(event, mMokoDevice.mac);
    }

    public void onBack(View view) {
        finish();
    }

    private void setNetworkSettings() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_NETWORK_SETTINGS;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("dhcp_en", mBind.cbDhcp.isChecked() ? 1 : 0);
        jsonObject.addProperty("ip", mBind.etIp.getText().toString());
        jsonObject.addProperty("netmask", mBind.etMask.getText().toString());
        jsonObject.addProperty("gw", mBind.etGateway.getText().toString());
        jsonObject.addProperty("dns", mBind.etDns.getText().toString());
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getNetworkSettings() {
        int msgId = MQTTConstants.READ_MSG_ID_NETWORK_SETTINGS;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (!isParaError()) {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setNetworkSettings();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isParaError() {
        if (!mBind.cbDhcp.isChecked()) {
            if (TextUtils.isEmpty(mBind.etIp.getText()) || TextUtils.isEmpty(mBind.etMask.getText()) ||
                    TextUtils.isEmpty(mBind.etGateway.getText()) || TextUtils.isEmpty(mBind.etDns.getText())) {
                return true;
            }
            String ip = mBind.etIp.getText().toString();
            String mask = mBind.etMask.getText().toString();
            String gateway = mBind.etGateway.getText().toString();
            String dns = mBind.etDns.getText().toString();
            int[] ipArray = getIp(ip);
            int[] maskArray = getIp(mask);
            int[] gatewayArray = getIp(gateway);
            int[] dnsArray = getIp(dns);
            if (null == ipArray || null == gatewayArray || null == maskArray || null == dnsArray)
                return true;
            if (isIpError(ipArray)) return true;
            if (isIpError(maskArray)) return true;
            if (isIpError(gatewayArray)) return true;
            if (isIpError(dnsArray)) return true;
        }
        return false;
    }

    private boolean isIpError(@NonNull int[] array) {
        for (int arr : array) {
            if (arr > 255) return true;
        }
        return false;
    }

    private int[] getIp(String ipInfo) {
        if (TextUtils.isEmpty(ipInfo)) return null;
        String[] split = ipInfo.split("\\.");
        if (split.length != 4) return null;
        for (String str : split) {
            if (TextUtils.isEmpty(str)) return null;
        }
        return new int[]{Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3])};
    }
}
