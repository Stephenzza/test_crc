/**
 * Copyright (c) 2018-2019
 * All Rights Reserved by Thunder Software Technology Co., Ltd and its affiliates.
 * You may not use, copy, distribute, modify, transmit in any form this file
 * except in compliance with THUNDERSOFT in writing by applicable law.
 */
package com.ts.app.diagnostic;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ts.app.diagnostic.common.CarManager;
import com.ts.app.diagnostic.common.ConstantFactory;
import com.ts.app.diagnostic.common.ListConfig;
import com.ts.app.diagnostic.base.BaseActivity;
import com.ts.lib.common.Logger;
import com.ts.lib.engmodesdk.EngConstant;
import com.ts.lib.engmodesdk.EngineeringModeManager;
import com.ts.lib.engmodesdk.EngineeringModeManager.DiagSignalListener;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import static com.ts.app.diagnostic.common.ListConfig.EngineerItem.getJson;
/**
 * The engineer main activity.
 *
 * @version 1.0
 */
public class GaeiEngineerModeActivity extends BaseActivity implements
        AdapterView.OnItemClickListener {
    private static final String TAG = "GaeiEngineerModeActivity";
    private static final String INTENT_EXTRA = "position";
    private ArrayList<ListConfig.EngineerItem> mList;
    private DiagSignalListener mDiagListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.debug(TAG, "onCreate");
        setContentView(R.layout.list_commont);
        ListView listView = findViewById(R.id.lv_main_list);
        if (ConstantFactory.GOS_TYPE.equals(getResources().getString(R.string.project_name))) {
            mList = getList(ConstantFactory.ENGINEER_GOS_JSON);
        } else {
            mList = getList(ConstantFactory.ENGINEER_JSON);
        }
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        for (int i = 0; i < mList.size(); i++) {
            Map<String, String> keyValuePair = new HashMap<String, String>();
            keyValuePair.put("name", mList.get(i).getTitle());
            list.add(keyValuePair);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, list,
                android.R.layout.simple_list_item_2,
                new String[]{"name"},
                new int[]{android.R.id.text1});
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        CarManager.getInstance(getApplicationContext()).connect();
        mDiagListener = new DiagSignalListener() {
            @Override
            public void onDiagCallback(int msgId, int msgType, String msgData) {
                Logger.debug(TAG, "onDiagCallback msgId = " + msgId);
                switch (msgId) {
                    case EngConstant.DIAG_MSG_ID_ENGINEERING_START:
                        EngineeringModeManager.getInstance().sendMsgToDiag(EngConstant
                                .DIAG_MSG_ID_ENGINEERING_START_SUCCESS, EngConstant.DIAG_MSG_READ_REPLY, null);
                        break;
                    case EngConstant.DIAG_MSG_ID_ENGINEERING_STATUS:
                        EngineeringModeManager.getInstance().sendMsgToDiag(EngConstant
                                .DIAG_MSG_ID_ENGINEERING_STATUS, EngConstant.DIAG_MSG_READ_REPLY, null);
                        break;
                    case EngConstant.DIAG_MSG_ID_ENGINEERING_STOP:
                        DiagApplication.getInstance().exit();
                        break;
                    default:
                        break;
                }
            }
        };
        EngineeringModeManager.getInstance().registerDiagSignalListener(mDiagListener);
        //Add the started activity to the list.
        DiagApplication.getInstance().addActivity(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        //Notify that diag engineering mode start success.
        EngineeringModeManager.getInstance().sendMsgToDiag(EngConstant
                .DIAG_MSG_ID_ENGINEERING_START_SUCCESS, EngConstant.DIAG_MSG_READ_REPLY, null);
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position <= mList.size()) {
            if (mList.get(position).getType()
                    .equals(ListConfig.EngineerItem.TYPE_ACTIVITY)) {
                startActivity(position);
            } else if (mList.get(position).getType()
                    .equals(ListConfig.EngineerItem.TYPE_FRAGMENT)) {
                startFragmentActivity(position);
            } else if (mList.get(position).getType()
                    .equals(ListConfig.EngineerItem.TYPE_UPDATE)) {
                Intent tboxIntent = new Intent();
                ComponentName componentName = new ComponentName(mList.get(position).getPck(),
                        mList.get(position).getCls());
                tboxIntent.setComponent(componentName);
                startActivity(tboxIntent);
            }
        } else {
            Logger.debug(TAG, "onItemClick: not found class"
                    + mList.get(position).getCls());
        }
    }
    /**
     * get the list.
     *
     * @param fileName file name
     * @return list the list.
     */
    private ArrayList<ListConfig.EngineerItem> getList(String fileName) {
        String str = getJson(fileName, this);
        Logger.debug(TAG, "getList: " + str);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(str);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        JSONArray jsonArray = null;
        try {
            if (jsonObject != null) {
                jsonArray = jsonObject.getJSONArray("engineer");
            }
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        Type listType = new TypeToken<List<ListConfig.EngineerItem>>() {
        }.getType();
        if (jsonArray != null) {
            String json = jsonArray.toString();
            ArrayList<ListConfig.EngineerItem> list = new Gson().fromJson(json, listType);
            return list;
        } else {
            return new ArrayList<ListConfig.EngineerItem>();
        }
    }
    protected void startActivity(int position) {
        try {
            Intent intent = new Intent();
            intent.setClassName(mList.get(position).getPck(), mList.get(position).getCls());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ConstantFactory.TITLE_EXTRA, mList.get(position).getTitle());
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            exception.printStackTrace();
            Toast.makeText(this, "not found this activity "
                    + mList.get(position).getCls(), Toast.LENGTH_LONG).show();
        }
    }
    protected void startFragmentActivity(int position) {
        try {
            Intent intent = new Intent(this, SecondActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(INTENT_EXTRA, position);
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            exception.printStackTrace();
            Toast.makeText(this, "not found this fragment ", Toast.LENGTH_LONG).show();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        //Notify that diag engineering mode exit success.
        EngineeringModeManager.getInstance().sendMsgToDiag(EngConstant
                .DIAG_MSG_ID_ENGINEERING_EXIT, EngConstant.DIAG_MSG_READ_REPLY, null);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.debug(TAG, "onDestroy");
        try {
            CarManager.getInstance(this).disConnect();
            EngineeringModeManager.getInstance().unRegisterDiagSignalListener(mDiagListener);
        } catch (Exception exception) {
            Logger.debug(TAG, "occurred exception");
        }
    }
}
