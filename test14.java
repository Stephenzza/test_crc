/**
 * Copyright (c) 2018-2019
 * All Rights Reserved by Thunder Software Technology Co., Ltd and its affiliates.
 * You may not use, copy, distribute, modify, transmit in any form this file
 * except in compliance with THUNDERSOFT in writing by applicable law.
 *
 */
package com.ts.appservice.vradapterservice.manager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFocusInfo;
import android.media.AudioAttributes;
import android.os.Binder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.VisibleForTesting;
import com.iflytek.autofly.voicecore.aidl.ISrStatusListener;
import com.iflytek.autofly.voicecore.sr.SrStatusInitListener;
import com.iflytek.autofly.voicecore.sr.SrStatusManager;
import com.tencent.wecarspeech.sdk.TASAsrManager;
import com.tencent.wecarspeech.sdk.TASAudioEventManager;
import com.tencent.wecarspeech.sdk.TASLifecycleManager;
import com.ts.appservice.vradapterservice.common.constants.VrConstants;
import com.ts.appservice.vradapterservice.common.utilities.ConfigurationUtil;
import com.ts.lib.common.Logger;
import com.ts.lib.vradapter.IVrCardAdapterCallback;
import com.ts.lib.vradapter.IVrCardService;
import com.ts.lib.vradapter.common.constant.SdkConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import ts.car.audio.AudioExtManager;
import ts.car.audio.AudioUsage;
import ts.car.transition.SceneStateListener;
import ts.car.transition.SceneTransitionManager;
import static com.iflytek.autofly.voicecore.SrStatusValue.SR_STATUS_SLEEP;
public class AudioEventManager {
    private static final String TAG = VrConstants.TAG + "-TsAudioEventManager";
    private static final String TENCENT_VR_PACKAGENAME = "com.ts.smartprojection";
    private static final String IFLYTEK_PACKAGE_NAME = "com.iflytek.autofly.voicecoreservice";
    private static final String IFLYTEK_CLASS_NAME
            = "com.iflytek.autofly.voicecoreservice.SpeechClientService";
    private static final String IFLYTEK_KEY_STOP_VR = "stopvr";
    private static final String IFLYTEK_KEY_RESUME_VR = "resumevr";
    private static final String IFLYTEK_VALUE = "navi";
    private static final int AUDIOFOCUS_LOSS = 0;
    private static final int AUDIOFOCUS_GAIN = 1;
    private static final int RANDOM_TIME = 600000;
    private static AudioEventManager sManager;
    private Context mContext;
    private TASAudioEventManager mTasAudioEventManager;
    private TASLifecycleManager mTasLifecycleManager;
    private TASAsrManager mTasAsrManager;
    private AudioExtManager mAudioExtManager;
    private boolean mVrState;
    private int mVrAudioEvent;
    private AudioAttributes mAttributes;
    private List<String> mRandomSemantic;
    private List<String> mSemanticList;
    private VrCardBinder mVrCardBinder;
    private SceneTransitionManager mSceneManager;
    private String mVersion;
    private boolean mIsMicOnVr = true;
    private boolean mVrEnable = true;
    /**
     * getInstance.
     */
    public static synchronized AudioEventManager getInstance(Context context) {
        if (sManager == null) {
            sManager = new AudioEventManager(context);
        }
        return sManager;
    }
    private AudioEventManager(Context context) {
        mContext = context;
        init();
    }
    @VisibleForTesting
    public AudioEventManager(VrCardBinder binder,
                             TASAudioEventManager.IAudioEventListener listener) {
        mVrCardBinder = binder;
        mIAudioEventListener = listener;
    }
    private void init() {
        Logger.info(TAG,"init");
        mTasAudioEventManager = TASAudioEventManager.getInstance();
        mTasLifecycleManager = TASLifecycleManager.getInstance();
        mTasAudioEventManager.addIAudioEventListener(mIAudioEventListener);
        mTasAsrManager = TASAsrManager.getInstance();
        mVrCardBinder = new VrCardBinder();
        try {
            mAudioExtManager = new AudioExtManager(mContext);
        } catch (Exception exception) {
            Logger.info(TAG,"AudioExtManager init failed");
        }
        if (mAudioExtManager != null) {
            mAttributes = mAudioExtManager.getAudioAttributesForUsage(AudioUsage.AUDIO_USAGE_VR);
            mAudioExtManager.registerAudioSourceChangedListener(mAudioSourceChangeListener);
        }
        initSemanticList();
        randomSemantic();
        initSceneTransition();
        initSystemVersion();
        initIflyVoiceClient();
    }
    /**
     * destroy.
     */
    public void destroy() {
        Logger.info(TAG,"destroy");
        if (sManager != null) {
            mTasAudioEventManager.removeIAudioEventListener(mIAudioEventListener);
            sManager = null;
        }
    }
    private void initSceneTransition() {
        Logger.info(TAG,"initSceneTransition");
        mSceneManager = SceneTransitionManager.getInstance();
        mSceneManager.registerSceneStateListener(new SceneStateListener() {
            @Override
            public void onSceneStateChanged(int sceneType, int newState) {
                if (sceneType == SceneTransitionManager.SCENE_BT_HMI_BG
                        || sceneType == SceneTransitionManager.SCENE_TYPE_COMMUNICATION_CARD) {
                    Logger.info(TAG, "Hardkey,into BT call BackGround");
                    if (newState == SceneTransitionManager.SCENE_STATE_SHOWN) {
                        if (mVrState) {
                            mTasAsrManager.stopASR();
                        }
                    }
                }
            }
        });
    }
    private void initSystemVersion() {
        mVersion = ConfigurationUtil.getVersion();
        Logger.info(TAG, "version:" + mVersion);
    }
    private void initIflyVoiceClient() {
        SrStatusManager.getInstance().initClient(mContext, new SrStatusInitListener() {
            @Override
            public void onInitCallback(boolean status, int i) {
                Logger.info(TAG, "onInitCallback Service connect:" + status);
                if (status) {
                    registerListener();
                }
            }
        });
    }
    private  void registerListener() {
        SrStatusManager.getInstance().regisetListener(mContext, iSrStatusListener);
    }
    private ISrStatusListener iSrStatusListener = new ISrStatusListener.Stub() {
        @Override
        public void onSrStatus(long status) throws RemoteException {
            Logger.info(TAG, "onSrStatus" + status);
            if (status == SR_STATUS_SLEEP) {
                mVrAudioEvent = SdkConstants.AudioEvent.TTS_END;
                mVrState = false;
                NativeVrManager.getInstance(mContext)
                        .setNativeVrState(SdkConstants.NativeVrState.NATIVEVR_STATE_STOP);
            } else {
                mVrAudioEvent = SdkConstants.AudioEvent.ASR_START_TIP_BEGIN;
                mVrState = true;
                NativeVrManager.getInstance(mContext)
                        .setNativeVrState(SdkConstants.NativeVrState.NATIVEVR_STATE_START);
            }
            VrHardKeyManager.getInstance(mContext).setVrState(mVrState);
            callbackOnAudioEventChange(mVrAudioEvent);
        }
    } ;
    private AudioExtManager.OnAudioExtFocusChangeListener mAudioExtListener =
            new AudioExtManager.OnAudioExtFocusChangeListener() {
        @Override
        public void onAudioExtFocusChange(int focusChange) {
            if (focusChange == AudioExtManager.AUDIOFOCUS_LOSS
                    || focusChange == AudioExtManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                mTasAsrManager.stopASR();
            }
        }
    };
    private AudioExtManager.OnAudioSourceChangeListener mAudioSourceChangeListener =
            new AudioExtManager.OnAudioSourceChangeListener() {
        @Override
        public void onAudioSourceChange(int sourceid, int action) {
            if (sourceid == AudioUsage.AUDIO_USAGE_CARLIFE_VR
                    || sourceid == AudioUsage.AUDIO_USAGE_CARPLAY_VR
                    || sourceid == AudioUsage.AUDIO_USAGE_REVERSE
                    || sourceid == AudioUsage.AUDIO_USAGE_REVERSE_MIX
                    || sourceid == AudioUsage.AUDIO_USAGE_ICALL
                    || sourceid == AudioUsage.AUDIO_USAGE_WECHAT_CALL
                    || sourceid == AudioUsage.AUDIO_USAGE_ANDROID_AUTO_VR
                    || sourceid == AudioUsage.AUDIO_USAGE_CARPLAY_TEL) {
                Logger.info(TAG,"onAudioSourceChange sourceid:" + sourceid + " action:" + action);
                if (mVrEnable) {
                    if (action == AUDIOFOCUS_GAIN) {
                        doReleaseSpeechRecord();
                    } else {
                        doReallocateSpeechRecord();
                    }
                } else {
                    Logger.info(TAG,"Native Vr disable");
                }
            }
        }
    };
    /**
     * release mic.
     */
    public void doReleaseSpeechRecord() {
        Logger.info(TAG, mVersion + " doReleaseSpeechRecord, mIsMicOnVr:" + mIsMicOnVr);
        if (mIsMicOnVr) {
            if (ConfigurationUtil.SYSTEM_MARIO.equals(mVersion)) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(IFLYTEK_PACKAGE_NAME, IFLYTEK_CLASS_NAME));
                intent.putExtra(IFLYTEK_KEY_STOP_VR, IFLYTEK_VALUE);
                mContext.startService(intent);
            } else {
                mTasLifecycleManager.releaseSpeechRecord(TENCENT_VR_PACKAGENAME);
                setVrEnable(false);
            }
            mIsMicOnVr = false;
        }
    }
    /**
     * gain mic.
     */
    public void doReallocateSpeechRecord() {
        Logger.info(TAG, mVersion + " doReallocateSpeechRecord, mIsMicOnVr:" + mIsMicOnVr);
        if (!mIsMicOnVr) {
            if (ConfigurationUtil.SYSTEM_MARIO.equals(mVersion)) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(IFLYTEK_PACKAGE_NAME, IFLYTEK_CLASS_NAME));
                intent.putExtra(IFLYTEK_KEY_RESUME_VR, IFLYTEK_VALUE);
                mContext.startService(intent);
            } else {
                setVrEnable(true);
                mTasLifecycleManager.reallocateSpeechRecord();
            }
            mIsMicOnVr = true;
        }
    }
    public void setNativeVrEnable(boolean enable) {
        mVrEnable = enable;
    }
    private void setVrEnable(boolean state) {
        mTasAsrManager.enableAsr(state);
        mTasAsrManager.enableWakeup(state);
    }
    private boolean isAudiofocusOnVr() {
        int type = -1;
        if (mAudioExtManager == null) {
            return false;
        }
        mAudioExtManager.getAudioFocusInfos();
        AudioFocusInfo[] infos = mAudioExtManager.getAudioFocusInfos();
        type = mAudioExtManager.getUsageFromAudioAttributes(infos[0].getAttributes());
        if (type == AudioUsage.AUDIO_USAGE_VR) {
            return true;
        }
        return false;
    }
    private TASAudioEventManager.IAudioEventListener mIAudioEventListener =
            new TASAudioEventManager.IAudioEventListener() {
        @Override
        public void onAsrStartTipBegin() {
            Logger.info(TAG,"onAsrStartTipBegin");
            mVrState = true;
            mVrAudioEvent = SdkConstants.AudioEvent.ASR_START_TIP_BEGIN;
            callbackOnAudioEventChange(mVrAudioEvent);
        }
        @Override
        public void onAsrStartTipEnd() {
            Logger.info(TAG,"onAsrStartTipEnd");
            mVrAudioEvent = SdkConstants.AudioEvent.ASR_START_TIP_END;
            callbackOnAudioEventChange(mVrAudioEvent);
        }
        @Override
        public void onAsrBegin() {
            Logger.info(TAG,"onAsrBegin");
            mVrState = true;
            VrHardKeyManager.getInstance(mContext).setVrState(mVrState);
            mVrAudioEvent = SdkConstants.AudioEvent.ASR_BEGIN;
            callbackOnAudioEventChange(mVrAudioEvent);
            NativeVrManager.getInstance(mContext)
                    .setNativeVrState(SdkConstants.NativeVrState.NATIVEVR_STATE_START);
        }
        @Override
        public void onAsrEnd() {
            Logger.info(TAG,"onAsrEnd");
            mVrState = false;
            VrHardKeyManager.getInstance(mContext).setVrState(mVrState);
            mVrAudioEvent = SdkConstants.AudioEvent.ASR_END;
            callbackOnAudioEventChange(mVrAudioEvent);
            NativeVrManager.getInstance(mContext)
                    .setNativeVrState(SdkConstants.NativeVrState.NATIVEVR_STATE_STOP);
        }
        @Override
            public void onTtsBegin() {
            Logger.info(TAG,"onTtsBegin");
            mVrAudioEvent = SdkConstants.AudioEvent.TTS_BEGIN;
            callbackOnAudioEventChange(mVrAudioEvent);
        }
        @Override
        public void onTtsEnd() {
            Logger.info(TAG,"onTtsEnd");
            mVrAudioEvent = SdkConstants.AudioEvent.TTS_END;
            callbackOnAudioEventChange(mVrAudioEvent);
        }
        @Override
        public void onMusicPlay() {
        }
        @Override
        public void onMusicPause() {
        }
        @Override
        public void onCallBegin() {
        }
        @Override
        public void onCallEnd() {
        }
        @Override
        public int onRequestAudioFocus() {
            Logger.info(TAG,"onRequestAudioFocus");
            if (mAudioExtManager == null) {
                return 0;
            }
            int requestResult = mAudioExtManager.requestAudioFocus(mAttributes, mAudioExtListener);
            return requestResult;
        }
        @Override
        public int onAbandonAudioFocus(boolean b) {
            Logger.info(TAG,"onAbandonAudioFocus");
            if (mAudioExtManager == null) {
                return 0;
            }
            int abandonResult = mAudioExtManager.abandonAudioFocus(mAudioExtListener);
            return abandonResult;
        }
    };
    public Binder getVrCardBinder() {
        return mVrCardBinder;
    }
    private RemoteCallbackList<IVrCardAdapterCallback> mCallbacks =
            new RemoteCallbackList<IVrCardAdapterCallback>();
    public class VrCardBinder extends IVrCardService.Stub {
        @Override
        public void registerVrCardCallback(IVrCardAdapterCallback callback) throws RemoteException {
            Logger.info(TAG,"registerVrCardCallback");
            if (callback != null) {
                mCallbacks.register(callback);
            }
        }
        @Override
        public void unregisterVrCardCallback(IVrCardAdapterCallback callback)
                throws RemoteException {
            Logger.info(TAG,"unregisterVrCardCallback");
            if (callback != null) {
                mCallbacks.unregister(callback);
            }
        }
        @Override
        public int getAudioEvent() throws RemoteException {
            Logger.info(TAG,"getAudioEvent");
            return mVrAudioEvent;
        }
        @Override
        public void startVr() throws RemoteException {
            Logger.info(TAG,"startVr");
            mTasAsrManager.startASR();
        }
        @Override
        public List<String> getRandomSemantic() throws RemoteException {
            Logger.info(TAG,"mRandomSemantic:" + mRandomSemantic);
            return mRandomSemantic;
        }
    }
    /**
     * callbackSetPlaySource.
     */
    public synchronized void callbackOnAudioEventChange(int audioEvent) {
        Logger.info(TAG,"callbackOnAudioEventChange");
        if (mCallbacks == null || mCallbacks.getRegisteredCallbackCount() <= 0) {
            Logger.info(TAG,"Callback = null");
            return;
                    randomSemantic2 = mSemanticList.get(random.nextInt(mSemanticList.size()));
                }
                mRandomSemantic.add(randomSemantic1);
                mRandomSemantic.add(randomSemantic2);
                Logger.info(TAG,"mRandomSemantic:" + mRandomSemantic);
                callbackOnRandomSemanticChange(mRandomSemantic);
            }
        }, 0, RANDOM_TIME);
    }
    /**
     * callbackOnRadioAction.
     */
    public synchronized void callbackOnRandomSemanticChange(List<String> randomSemantic) {
        Logger.info(TAG,"callbackOnRandomSemanticChange");
        if (mCallbacks == null || mCallbacks.getRegisteredCallbackCount() <= 0) {
            Logger.info(TAG,"Callback = null");
            return;
        }
        synchronized (mCallbacks) {
            int count = mCallbacks.beginBroadcast();
            for (int i = 0; i < count; i++) {
                try {
                    mCallbacks.getBroadcastItem(i).onRandomSemanticChange(randomSemantic);
                } catch (RemoteException exception) {
                    exception.printStackTrace();
                }
            }
            mCallbacks.finishBroadcast();
        }
    }
}
