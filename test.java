/**
 * Copyright (c) 2018-2019
 * All Rights Reserved by Thunder Software Technology Co., Ltd and its affiliates.
 * You may not use, copy, distribute, modify, transmit in any form this file
 * except in compliance with THUNDERSOFT in writing by applicable law.
 */
package com.ts.app.gallery.activity;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.ts.app.gallery.R;
import com.ts.app.gallery.common.utilities.AntiShakeUtils;
import com.ts.app.gallery.common.utilities.CommonConstant;
import com.ts.app.gallery.common.utilities.DataMiningUtils;
import com.ts.app.gallery.common.utilities.DateTimeUtils;
import com.ts.app.gallery.common.utilities.HmiLogUtil;
import com.ts.app.gallery.common.utilities.MediaDataQueryWorker;
import com.ts.app.gallery.common.utilities.ToastUtils;
import com.ts.app.gallery.common.utilities.ValueParseUtils;
import com.ts.app.gallery.common.utilities.VideoFrameWorker;
import com.ts.app.gallery.widget.ComfirDialog;
import com.ts.app.gallery.widget.TouchSurfaceView;
import com.ts.lib.common.Constants;
import com.ts.lib.gallery.data.LocalBroadcastConstDef;
import com.ts.lib.gallery.data.MediaConstDef;
import com.ts.lib.gallery.data.MediaItem;
import com.ts.lib.gallery.listener.IVideoPlayHmiListener;
import com.ts.lib.gallery.manager.VideoPlayer;
import com.ts.lib.mining.DataMiningManager;
import com.ts.lib.settings.TsAudioManager;
import com.ts.lib.settings.interfaces.OnManagerConnChangedListener;
import com.ts.lib.settings.interfaces.OnMuteChangedListener;
import com.ts.lib.settings.interfaces.OnVolumeChangedListener;
import java.lang.ref.WeakReference;
/**
 * Created by zhangfan on 18-12-12.
 */
public class PlayVideoActivity extends GalleryBaseActivity {
    private static final String TAG = "PlayVideoActivity";
    public static final String USB_PORT_TAG = "usb_port_tag";
    private static final String VIDEO_NEXT_CLICK = "gaeiVideo_click_MainActivity_next";
    private static final String VIDEO_PLAY_CLICK = "gaeiVideo_click_MainActivity_play";
    private static final String VIDEO_DRAG_CLICK = "gaeiVideo_click_MainListActivity_fastdrag";
    private static final String VIDEO_PLAY_LONG_KEY = "gaeiVideo_time_PlayVideoActivity";
    private static final String CLICK_VALUE = "video";
    private static final int PRE_REPLAY_EDGE = 3;
    private static final int WHAT_INFO_PLAY = 3;
    private static final long DELETE_TOAST_SHOW_DELAY = 1000L;
    private static final int MILLION_SECOND = 1000;
    private static final long PROGRSS_UPDATE_DELAY = 1000L;
    private static final int PROGRSS_UPDATE_MSG = 100;
    private static final int VIDEO_PLAY_FINISHED_MSG = 200;
    private static final int VIDEO_SEEK_FINISHED_MSG = 201;
    private static final int EXCEPTION_AUTOPLAY_NEXT_MSG = 202;
    private static final int PANEL_HIDEN_MSG = 300;
    //private static final int NEXT_BTN_RESET_MSG = 301;
    private static final int AUDIO_FOCUS_LOST_MSG = 400;
    private static final int VOLUME_CHANGE_MSG = 401;
    private static final int VIDEO_PLAY_EXCEPTION = 500;
    private static final int UPDATE_SURFACE_VIEW = 510;
    private static final int SHOW_CAR_RUNNING = 600;
    private static final int HIDEN_CAR_RUNNING = 601;
    private static final long PANEL_HIDEN_DELAY = 5000L;
    private static final long EXCEPTION_AUTOPLAY_DELAY = 3000L;
    private static final long NEXT_BTN_RESET_DELAY = 500L;
    private static final long PROCESS_SAVE_PERIOD = 5;
    private static final int MIN_CHANGE_PERIOD = 5;
    private static final int PRECENT_NUMBER = 100;
    private static final int TEMP_DEFAULT_MAX_FLOAT = 39;
    private static final int HALF_DIVIDE_VALUE = 2;
    private static final int VOLUMNE_CHANGE_FLAG = 1;
    /**
     * frame touch threshold, only progress bar touch bigger than this, will show frame window.
     */
    private static final long FRAME_TOUCH_THRESHOLD = 200L;
    /**
     * Media data query work, can get paging video data.
     */
    private MediaDataQueryWorker mMediaDataQueryWorker;
    /**
     * Ts audio setting mananger.
     */
    private TsAudioManager mAudioManager;
    private ImageView mBtnNext;
    private ImageView mBtnPlay;
    private TextView mPlayTimeTv;
    private TextView mRestTimeTv;
    private TouchSurfaceView mPlaySurfaceView;
    private SeekBar mProgressBar;
    private SeekBar mVolumeBar;
    private TextView mVolumeTv;
    private RelativeLayout mCtrlLayout;
    private ViewGroup mRootView;
    private FrameLayout mVideoFrameLayout;
    private ImageView mVideoFrameIm;
    private ImageView mChangeTimeIm;
    private ViewGroup mExceptionView;
    private TextView mChangeTimeTv;
    private TextView mTitleTv;
    private ImageView mVoiceIm;
    private ImageView mProgessThumnIm;
    private ComfirDialog mComfirDialog;
    private ViewGroup mMaskLayout;
    /**
     * Is playing progress is tracking.
     */
    private boolean mIsTracking = false;
    /**
     * Is lost audio focus.
     */
    private boolean mIsAudioFocusLost = false;
    /**
     * Is showing play exception.
     */
    private boolean mIsException = false;
    /**
     * Playing view width.
     */
    private int mWidth;
    /**
     * Playing view height.
     */
    private int mHeight;
    /**
     * Play handler, will used to update progrss and hidle control panel.
     */
    private PlayHandler mPlayHandler;
    /**
     * Current playing video start position.
     */
    private int mCurrentStartPosition = 0;
    /**
     * Surface view has first inited.
     */
    private boolean mHasFirstInit = false;
    /**
     * Surface view has first created.
     */
    private boolean mIsSurfaceCreate = false;
    /**
     * Current video last playing time.
     */
    private int mLastPlayTime = 0;
    /**
     * Surface view has inited.
     */
    private boolean mSurfaceInit = false;
    /**
     * Service has binded.
     */
    private boolean mServiceBind = false;
    /**
     * Is surface view recreate.
     */
    private boolean mIsSurfaceRecreate = false;
    /**
     * Video frame window position of y axes.
     */
    private int mVideoFramePosY = 0;
    /**
     * Video frame window half width.
     */
    private int mHalfFrameWidth = 0;
    /**
     * Progess bar left margin.
     */
    private int mProgressLeft = 0;
    /**
     * progress thumb imageview position of y axes.
     */
    private int mThumbImPositionY = 0;
    /**
     * Last capture video frame time.
     */
    private int mLastFrameTime = 0;
    /**
     * Video frame window height.
     */
    private int mFrameWindowHeight = 0;
    /**
     * Video frame window width.
     */
    private int mFrameWindowWidth = 0;
    /**
     * Video progress bar width.
     */
    private int mVideoProgressBarWidth = 0;
    /**
     * Current usb port, will be initialized in oncreate method.
     */
    private int mUsbPort = MediaConstDef.UsbPort.UNKNOW;
    /**
     * Video frame capture worker.
     */
    private VideoFrameWorker mVideoFrameWorker;
    /**
     * Max volume value.
     */
    private int mVolumemax;
    /**
     * Video player service.
     */
    private VideoPlayer mVideoPlayer;
    /**
     * Video player notification, will be used to initialize the mVideoPlayer.
     */
    private GalleryVideoNotification mGalleryVideoNotification;
    /**
     * current showing mediaItem.
     */
    private MediaItem mCurrentMediaItem;
    /**
     * Progress bar touch start time.
     */
    private long mProgressTouchStart;
    /**
     * Input video id from Intent.
     */
    private String mInputVid;
    /**
     * Video play hmi can receive exception flag.
     */
    private boolean mCanReceiveException = true;
    /**
     * Video play has received exception when pause.
     */
    private boolean mHasPauseException = false;
    /**
     * Is volume changed by self.
     */
    private boolean mVolumeChangeBySelf = false;
    /**
     * Volume change listener.
     */
    private OnVolumeChangedListener mVolumeChangeListener = new OnVolumeChangedListener() {
        @Override
        public void onMediaVolumeChanged(int value) {
            HmiLogUtil.verbose(TAG, "-OnVolumeChangedListener value:" + value
                    + " changed by self:" + mVolumeChangeBySelf);
            if (!mVolumeChangeBySelf) {
                mPlayHandler.removeMessages(VOLUME_CHANGE_MSG);
                Message message = mPlayHandler.obtainMessage(VOLUME_CHANGE_MSG);
                message.arg1 = value;
                mPlayHandler.sendMessage(message);
            }
        }
        @Override
        public void onPhoneVolumeChanged(int i) {
        }
        @Override
        public void onNaviVolumeChanged(int i) {
        }
    };
    private OnManagerConnChangedListener mAudioListener = new OnManagerConnChangedListener() {
        @Override
        public void onBinderStateChanged(boolean binderState) {
            HmiLogUtil.verbose(TAG, "-onBinderStateChanged Audio+" + binderState);
            if (binderState) {
                try {
                    mAudioManager.registerMuteCallback(mOnMuteChangedListener);
                    mAudioManager.registerVolumeChangedListener(mVolumeChangeListener,
                            VOLUMNE_CHANGE_FLAG);
                    int value = mAudioManager.getGroupVolume(TsAudioManager.VOLUME_GROUP_MEDIA);
                    HmiLogUtil.verbose(TAG, "get group volume value:" + value);
                    showVolumeValue(value);
                } catch (Exception exceptions) {
                    HmiLogUtil.warning(TAG, "onBinderStateChanged Exception:", exceptions);
                }
            }
        }
    };
    /**
     * Mute listener.
     */
    private OnMuteChangedListener mOnMuteChangedListener = new OnMuteChangedListener() {
        @Override
        public void onPhoneMuteStateChanged(boolean mute, int flag) {
            HmiLogUtil.verbose(TAG, "-onPhoneMuteStateChanged()");
        }
        @Override
        public void onMediaMuteStateChanged(boolean mute, int flag) {
            HmiLogUtil.verbose(TAG, "-onMediaMuteStateChanged() mute-->" + mute + "flag-->" + flag);
            int volume = mAudioManager.getGroupVolume(TsAudioManager.VOLUME_GROUP_MEDIA);
            HmiLogUtil.verbose(TAG, "volume" + volume);
            if (mute) {
                setVoiceIcon(0);
                showVolumeValue(0);
            } else {
                setVoiceIcon(volume);
                showVolumeValue(volume);
            }
        }
        @Override
        public void onNaviMuteStateChanged(boolean mute, int flag) {
            HmiLogUtil.verbose(TAG, "-onNaviMuteStateChanged()");
        }
    };
    private SeekBar.OnSeekBarChangeListener mSeekBarChangerListener
            = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        HmiLogUtil.debug(TAG, "onProgressChanged from user:" + progress);
                        changeThumbPosition(progress);
                    }
                    if (!mIsException) {
                        //update played time
                        String playedTimeStr = DateTimeUtils.getTimeDescString(progress);
                        mPlayTimeTv.setText(playedTimeStr);
                        int restTime = mProgressBar.getMax() - mProgressBar.getProgress();
                        String restTimeStr = DateTimeUtils.getTimeDescString(restTime);
                        //String restTimeDesc = getString(R.string.reset_time_format, restTimeStr);
                        mRestTimeTv.setText(restTimeStr);
                        long touchPeriodTime = SystemClock.elapsedRealtime() - mProgressTouchStart;
                        HmiLogUtil.debug(TAG, "onProgressChanged touchPeriodTime:"
                                + touchPeriodTime);
                        if (fromUser && !mIsException && touchPeriodTime > FRAME_TOUCH_THRESHOLD) {
                            HmiLogUtil.debug(TAG, "onProgressChanged from user:" + progress);
                            showVideoFrameToTarget(progress);
                        }
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    HmiLogUtil.debug(TAG, "onStartTrackingTouch");
                    mProgressTouchStart = SystemClock.elapsedRealtime();
                    showProgressThumb(true);
                    //mProgressBar.setThumb(getDrawable(R.mipmap.pv_icon_press));
                    if (!mIsException) {
                        showCtrlPanel();
                        mIsTracking = true;
                        mPlayHandler.removeMessages(PANEL_HIDEN_MSG);
                    }
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    HmiLogUtil.debug(TAG, "onStopTrackingTouch");
                    showProgressThumb(false);
                    if (!mIsException) {
                        removeProgressMsg();
                        trackPlayPosition(seekBar.getProgress());
                        hidenVideoFrame();
                        hidenCtrlPanelDelay();
                        DataMiningManager.getInstance(PlayVideoActivity.this.getApplication())
                                .trackCustomEvent(VIDEO_DRAG_CLICK, CLICK_VALUE);
                    }
                }
            };
    private void showProgressThumb(boolean isShowing) {
        HmiLogUtil.debug(TAG, "Thumb showThumb isShowing:" + isShowing);
        if (isShowing) {
            mProgessThumnIm.setVisibility(View.VISIBLE);
            changeThumbPosition(mProgressBar.getProgress());
        } else {
            mProgessThumnIm.setVisibility(View.INVISIBLE);
        }
    }
    private void changeThumbPosition(int progress) {
        if (0 == mThumbImPositionY) {
            int[] position = new int[2];
            int[] parentposi = new int[2];
            mProgressBar.getLocationInWindow(position);
            mRootView.getLocationInWindow(parentposi);
            mProgressLeft = position[0] - parentposi[0]
                    + (int) getResources().getDimension(R.dimen.seek_bar_offset);
            mThumbImPositionY = position[1]
                    + (int) getResources().getDimension(R.dimen.thumb_offset_y)
                    - mProgessThumnIm.getHeight() / 2;
        }
        int maxValue = mProgressBar.getMax();
        int valueOffset = (int) (mVideoProgressBarWidth * ((float) progress / maxValue));
        int floatPos = valueOffset + mProgressLeft;
        int result = floatPos - mProgessThumnIm.getWidth() / 2;
        HmiLogUtil.debug(TAG, "Thumb postion X:" + result + " Y:" + mThumbImPositionY);
        FrameLayout.LayoutParams flp =
                (FrameLayout.LayoutParams) mProgessThumnIm.getLayoutParams();
        flp.topMargin = mThumbImPositionY;
        flp.leftMargin = result;
        mProgessThumnIm.setLayoutParams(flp);
    }
    private SeekBar.OnSeekBarChangeListener mVoiceChangerListener
            = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        HmiLogUtil.debug(TAG, "onProgressChanged from user:" + progress);
                        changeVoiceToTarget(progress);
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    HmiLogUtil.verbose(TAG, "User change volume value start");
                    mVolumeChangeBySelf = true;
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    HmiLogUtil.verbose(TAG, "User change volume value finished");
                    mVolumeChangeBySelf = false;
                }
            };
    private void changeVoiceToTarget(int progress) {
        int targetValue = progress * PRECENT_NUMBER / TEMP_DEFAULT_MAX_FLOAT;
        targetValue = targetValue > PRECENT_NUMBER ? PRECENT_NUMBER : targetValue;
        HmiLogUtil.info(TAG, "changeVoiceToTarget progress:" + progress + " Volume bar value:"
                + targetValue);
        mVolumeTv.setText(getString(R.string.volume_precent_format, targetValue));
        setVoiceIcon(targetValue);
        mAudioManager.setGroupVolume(
                TsAudioManager.VOLUME_GROUP_MEDIA, progress);
    }
    private View.OnClickListener mCtrlPanelClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Avoid frequent clicks
            if (!AntiShakeUtils.canResponseClick()) {
                return;
            } else {
                AntiShakeUtils.updateLastClickTime();
            }
            switch (v.getId()) {
                case R.id.image_play_btn_play:
                    HmiLogUtil.debug(TAG, "on click: play");
                    if (mVideoPlayer != null && !mIsException) {
                        if (mVideoPlayer.isPlayingStatus()) {
                            pausePlay();
                        } else {
                            mCanReceiveException = true;
                            HmiLogUtil.debug(TAG, "click play btn to play!");
                            if (mIsSurfaceRecreate) {
                                mVideoPlayer.initAutoPlay(mCurrentMediaItem.getId(),
                                        mCurrentMediaItem.getPath(), mLastPlayTime);
                                mIsSurfaceRecreate = false;
                            } else {
                                mVideoPlayer.play();
                            }
                            showPlayingStatu(true);
                            mHasPauseException = false;
                            sendProgressMsg();
                            mVideoPlayer.setAutoPlayAfterSeek(true);
                        }
                        DataMiningManager.getInstance(PlayVideoActivity.this.getApplication())
                                .trackCustomEvent(VIDEO_PLAY_CLICK, CLICK_VALUE);
                    }
                    break;
                case R.id.image_play_btn_next:
                    HmiLogUtil.debug(TAG, "on click: play next!");
                    playNextPreChange(true);
                    DataMiningManager.getInstance(PlayVideoActivity.this.getApplication())
                            .trackCustomEvent(VIDEO_NEXT_CLICK, CLICK_VALUE);
                    break;
                case R.id.root_view:
                    showCtrlPanel();
                    break;
                default:
                    break;
            }
            //panel delay hiden.
            hidenCtrlPanelDelay();
        }
    };
    private void playNextPreChange(boolean isNext) {
        mIsSurfaceRecreate = false;
        mPlayHandler.removeMessages(EXCEPTION_AUTOPLAY_NEXT_MSG);
        removeProgressMsg();
        updateLastPlayPosition();
        mVideoPlayer.setAutoPlayAfterSeek(true);
        playNextVideo(isNext);
    }
    private TouchSurfaceView.TouchControlListener mSurfaceViewToucher =
            new TouchSurfaceView.TouchControlListener() {
            @Override
            public void dragProgressStart() {
                HmiLogUtil.debug(TAG, "dragProgressStart ");
                mProgressTouchStart = SystemClock.elapsedRealtime();
                if (!mIsException) {
                    mIsTracking = true;
                    showCtrlPanel();
                    showProgressThumb(true);
                    mPlayHandler.removeMessages(PANEL_HIDEN_MSG);
                }
            }
            @Override
            public void dragProgressEnd() {
                HmiLogUtil.debug(TAG, "dragProgressEnd ");
                if (!mIsException) {
                    removeProgressMsg();
                    //mIsSurfaceRecreate = false;
                    trackPlayPosition(mProgressBar.getProgress());
                    hidenVideoFrame();
                    showProgressThumb(false);
                    DataMiningManager.getInstance(PlayVideoActivity.this.getApplication())
                            .trackCustomEvent(VIDEO_DRAG_CLICK, CLICK_VALUE);
                }
            }
            @Override
            public void changeProgress(int changePrecent) {
                if (!mIsException) {
                    HmiLogUtil.debug(TAG, "changeProgress:" + changePrecent);
                    int changeProcess = mProgressBar.getMax() * changePrecent
                            / TouchSurfaceView.HUNDRED_PRECENT;
                    int targetProcess = mProgressBar.getProgress() + changeProcess;
                    targetProcess = targetProcess < 0 ? 0 : targetProcess;
                    targetProcess = targetProcess > mProgressBar.getMax()
                            ? mProgressBar.getMax() : targetProcess;
                    HmiLogUtil.debug(TAG, "changeProgress targetProcess:" + targetProcess);
                    mProgressBar.setProgress(targetProcess);
                    long touchPeriodTime = SystemClock.elapsedRealtime() - mProgressTouchStart;
                    HmiLogUtil.debug(TAG, "changeProgress touchPeriodTime:" + touchPeriodTime);
                    if (touchPeriodTime > FRAME_TOUCH_THRESHOLD) {
                        showVideoFrameToTarget(targetProcess);
                    }
                    changeThumbPosition(targetProcess);
                }
            }
            @Override
            public void volumeChange(int changePrecent) {
                //QA result,don't need touch control for volume
                //HmiLogUtil.debug(TAG, "volumeChange:" + changePrecent);
                //int volumne = mVolumeBar.getProgress();
                //int targetVol = volumne + changePrecent;
                //targetVol = targetVol > mVolumemax ? mVolumemax : targetVol;
                //targetVol = targetVol < 0 ? 0 : targetVol;
                //mVolumeBar.setProgress(targetVol);
                //changeVoiceToTarget(targetVol);
            }
            @Override
            public void brightChange(int changePrecent) {
                HmiLogUtil.debug(TAG, "brightChange:" + changePrecent);
            }
            @Override
            public void onTouchStart() {
                showCtrlPanel();
            }
            @Override
            public void onTouchEnd() {
                hidenCtrlPanelDelay();
            }
        };
    private void setNavigationBarStatu(boolean needShowing) {
        Intent actionIntent = new Intent(
                CommonConstant.NavigatorBarChange.NAVIGATOR_BAR_CHANGE_ACTION);
        actionIntent.putExtra(CommonConstant.NavigatorBarChange.CHANGE_TYPE, needShowing);
        sendBroadcast(actionIntent);
        HmiLogUtil.debug(TAG, "setNavigationBarStatu needShowing:" + needShowing);
        View decorView = getWindow().getDecorView();
        int uiOptions;
        if (needShowing) {
            uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        } else {
            uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        uiOptions = uiOptions | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }
    private void showVideoFrameToTarget(int progress) {
        if (0 == mVideoFramePosY) {
            initVideoFramePosY();
        }
        if (null == mVideoFrameWorker) {
            mVideoFrameWorker = new VideoFrameWorker();
        }
        int videoFramePosX = calVideoFramePosX(progress);
        showVideoFrameToPositionX(videoFramePosX);
        int currentProgress = mProgressBar.getProgress();
        int preoidProgress = Math.abs(currentProgress - mLastFrameTime);
        if (preoidProgress >= MIN_CHANGE_PERIOD
                && preoidProgress >= (mProgressBar.getMax() / PRECENT_NUMBER)) {
            mLastFrameTime = mProgressBar.getProgress();
            mVideoFrameWorker.addTask(mCurrentMediaItem.getPath(),
                    mLastFrameTime * MILLION_SECOND * MILLION_SECOND, mVideoFrameIm);
        }
        HmiLogUtil.debug(TAG, "showVideoFrameToTarget start to get currentPosition");
        int currentPos = mVideoPlayer.getCurMoviePosition();
        HmiLogUtil.debug(TAG, "showVideoFrameToTarget end to get currentPosition");
        int changeTime = progress - Math.round((float) currentPos / MILLION_SECOND);
        if (changeTime >= 0) {
            String restTimeStr = DateTimeUtils.getTimeDescString(changeTime);
            mChangeTimeTv.setText(restTimeStr);
            mChangeTimeIm.setImageResource(R.mipmap.pv_icon_pre);
        } else if (changeTime < 0) {
            String restTimeStr = DateTimeUtils.getTimeDescString(-changeTime);
            String restTimeDesc = getString(R.string.reset_time_format, restTimeStr);
            mChangeTimeTv.setText(restTimeDesc);
            mChangeTimeIm.setImageResource(R.mipmap.pv_icon_back);
        }
    }
    private int calVideoFramePosX(int progress) {
        int maxValue = mProgressBar.getMax();
        int valueOffset = (int) (mVideoProgressBarWidth * ((float) progress / maxValue));
        int floatPos = valueOffset + mProgressLeft;
        int result = floatPos - mHalfFrameWidth;
        if (result < 0) {
            return 0;
        }
        if ((floatPos + mHalfFrameWidth) > mWidth) {
            return mWidth - mHalfFrameWidth * 2;
        }
        HmiLogUtil.debug(TAG, "calVideoFramePosX valueOffset:" + valueOffset
                + " result:" + result + " floatPos" + floatPos);
        return result;
    }
    private void trackPlayPosition(int position) {
        //mIsTracking = true;
        HmiLogUtil.debug(TAG, "track position:" + position + " total time:"
                + mProgressBar.getMax());
        mVideoPlayer.pause();
        mVideoPlayer.setPlayPosition(position * MILLION_SECOND);
        mLastPlayTime = position * MILLION_SECOND;
    }
    private void hidenVideoFrame() {
        mVideoFrameLayout.setVisibility(View.GONE);
    }
    private void initVideoFramePosY() {
        int[] position = new int[2];
        int[] parentposi = new int[2];
        mProgressBar.getLocationInWindow(position);
        mRootView.getLocationInWindow(parentposi);
        mProgressLeft = position[0] - parentposi[0]
                + (int) getResources().getDimension(R.dimen.seek_bar_offset);
        mVideoFramePosY = position[1]
                - (int) getResources().getDimension(R.dimen.video_frame_margin)
                - (int) getResources().getDimension(R.dimen.video_mini_frame_height);
    }
    private void showVideoFrameToPositionX(int videoFramePosX) {
        HmiLogUtil.debug(TAG, "showVideoFrameToPositionX videoFramePosX:" + videoFramePosX);
        mVideoFrameLayout.setVisibility(View.VISIBLE);
        //mVideoFrameLayout.bringToFront();
        FrameLayout.LayoutParams flp =
                (FrameLayout.LayoutParams) mVideoFrameLayout.getLayoutParams();
        flp.topMargin = mVideoFramePosY;
        flp.leftMargin = videoFramePosX;
        mVideoFrameLayout.setLayoutParams(flp);
        mVideoFrameLayout.layout(videoFramePosX, mVideoFramePosY,
                videoFramePosX + mFrameWindowWidth, mVideoFramePosY + mFrameWindowHeight);
    }
    @Override
    protected void onPause() {
        HmiLogUtil.debug(TAG, "onPause");
        pausePlay();
        DataMiningManager.getInstance(this.getApplicationContext())
                .trackEndPage(VIDEO_PLAY_LONG_KEY);
        super.onPause();
    }
    private void pausePlay() {
        if (mVideoPlayer != null && mVideoPlayer.isPlayingStatus()) {
            updateLastPlayPosition();
            mVideoPlayer.pause();
            setPauseStatu();
        }
    }
    private void setPauseStatu() {
        mCanReceiveException = false;
        mVideoPlayer.setAutoPlayAfterSeek(false);
        showPlayingStatu(false);
        removeProgressMsg();
        mLastPlayTime = mVideoPlayer.getCurMoviePosition();
    }
    private void showVideoPlayException() {
        HmiLogUtil.debug(TAG, "showVideoPlayException mIsResuming:" + mIsResuming);
        if (mIsResuming) {
            mIsException = true;
            mExceptionView.setVisibility(View.VISIBLE);
            mPlaySurfaceView.setVisibility(View.GONE);
            mRestTimeTv.setText(R.string.default_time_fromat_str);
            mPlayTimeTv.setText(R.string.default_time_fromat_str);
            mVideoPlayer.releaseMediaPlayer();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        HmiLogUtil.debug(TAG, "onResume");
        DataMiningManager.getInstance(this.getApplicationContext())
                .trackBeginPage(VIDEO_PLAY_LONG_KEY);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        HmiLogUtil.debug(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        setContentView(R.layout.activity_video_play);
        mGalleryVideoNotification = new GalleryVideoNotification();
        mVideoPlayer = new VideoPlayer(this, mGalleryVideoNotification);
        mPlayHandler = new PlayHandler(this);
        mHasFirstInit = false;
        DataMiningUtils.setVideoAppInfo(getApplicationContext());
        initView();
        initIntentData();
        initLocalBroadCast();
        initSurfaceView();
        HmiLogUtil.debug(TAG, "onCreate: end");
        setNavigationBarStatu(false);
    }
    @Override
    public int getSceneType() {
        return Constants.SceneType.TS_SCENE_TYPE_GALLERY_VIDEO_PLAYER;
    }
    private void initAudio() {
        mAudioManager = TsAudioManager.getInstance(this);
        mAudioManager.connect();
        mAudioManager.setOnManagerConnChangedListener(mAudioListener);
        HmiLogUtil.verbose(TAG, "init Audio +" + mAudioManager.isServiceBound());
        if (mAudioManager.isServiceBound()) {
            int volume = mAudioManager.getGroupVolume(TsAudioManager.VOLUME_GROUP_MEDIA);
            HmiLogUtil.debug(TAG, "initAudio get media volume:" + volume);
            showVolumeValue(volume);
        }
    }
    private void showVolumeValue(int volume) {
        HmiLogUtil.debug(TAG, "get media volume:" + volume);
        if (volume < 0) {
            return;
        }
        //temp use this default value, will get real value from setting in furture.
        int targetValue = volume * PRECENT_NUMBER / TEMP_DEFAULT_MAX_FLOAT;
        targetValue = targetValue > PRECENT_NUMBER ? PRECENT_NUMBER : targetValue;
        mVolumeTv.setText(getString(R.string.volume_precent_format, targetValue));
        mVolumeBar.setProgress(volume);
        setVoiceIcon(targetValue);
        HmiLogUtil.info(TAG, "current set progress:" + targetValue + " Volumne bar value:"
                + mVolumeBar.getProgress());
    }
    private void setVoiceIcon(int targetValue) {
        if (targetValue > 0) {
            mVoiceIm.setImageResource(R.mipmap.pv_icon_voice);
        } else {
            mVoiceIm.setImageResource(R.mipmap.pv_icon_voice_no);
        }
    }
    private void initSurfaceView() {
        HmiLogUtil.debug(TAG, "initSurfaceView");
        SurfaceHolder surfaceHolder = mPlaySurfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolderCallBack());
    }
    private void updateSurfaceView() {
        HmiLogUtil.debug(TAG, "updateSurfaceView");
        ViewGroup.LayoutParams layoutParams = mPlaySurfaceView.getLayoutParams();
        mPlaySurfaceView.setLayoutParams(calculateLayoutParams(layoutParams));
    }
    private void initLocalBroadCast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalBroadcastConstDef.USB_DEVICE_UNMOUNT_ACTION);
        intentFilter.addAction(LocalBroadcastConstDef.GALLERY_EXTERNAL_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalBroadcastReceiver,
                intentFilter);
    }
    private void hidenCtrlPanelDelay() {
        mPlayHandler.removeMessages(PANEL_HIDEN_MSG);
        mPlayHandler.sendEmptyMessageDelayed(PANEL_HIDEN_MSG, PANEL_HIDEN_DELAY);
    }
    private void hidenCtrlPanel() {
        setNavigationBarStatu(false);
        mCtrlLayout.setVisibility(View.GONE);
        mTitleTv.setVisibility(View.GONE);
        mProgessThumnIm.setVisibility(View.GONE);
        mMaskLayout.setVisibility(View.GONE);
    }
    private void showCtrlPanel() {
        setNavigationBarStatu(true);
        mPlayHandler.removeMessages(PANEL_HIDEN_MSG);
        mCtrlLayout.setVisibility(View.VISIBLE);
        mTitleTv.setVisibility(View.VISIBLE);
        mMaskLayout.setVisibility(View.VISIBLE);
    }
    /**
     * init activity default view .
     */
    private void initView() {
        HmiLogUtil.debug(TAG, "initView: ");
        mBtnNext = (ImageView) findViewById(R.id.image_play_btn_next);
        mBtnPlay = (ImageView) findViewById(R.id.image_play_btn_play);
        mRestTimeTv = (TextView) findViewById(R.id.rest_time_tv);
        mPlayTimeTv = (TextView) findViewById(R.id.played_time_tv);
        mPlaySurfaceView = (TouchSurfaceView) findViewById(R.id.fitxy_surface_view);
        mProgressBar = (SeekBar) findViewById(R.id.video_progressbar);
        mVolumeBar = (SeekBar) findViewById(R.id.voice_progressbar);
        mCtrlLayout = (RelativeLayout) findViewById(R.id.ctrl_panel_layout);
        mRootView = (ViewGroup) findViewById(R.id.root_view);
        mVideoFrameLayout = (FrameLayout) findViewById(R.id.video_mini_frame);
        mVideoFrameIm = (ImageView) findViewById(R.id.video_frame_show_im);
        mVolumeTv = (TextView) findViewById(R.id.voice_progress_tv);
        mChangeTimeIm = (ImageView) findViewById(R.id.change_time_im);
        mChangeTimeTv = (TextView) findViewById(R.id.change_time_tv);
        mExceptionView = (ViewGroup) findViewById(R.id.exception_layout);
        mTitleTv = (TextView) findViewById(R.id.play_title_tv);
        mVoiceIm = (ImageView) findViewById(R.id.image_play_btn_voice);
        mProgessThumnIm = findViewById(R.id.progress_thunmb_im);
        mMaskLayout = findViewById(R.id.mask_layout);
        //set padding in xml is not effective,
        int padding = (int) getResources().getDimension(R.dimen.seek_bar_offset);
        mProgressBar.setPadding(padding, 0, padding, 0);
    }
    @Override
    protected void initData() {
        HmiLogUtil.debug(TAG, "initData selectId:" + mInputVid);
        if (null != mMediaManager && mMediaManager.isBinded()) {
            initAfterServiceConnected();
        } else {
            finish();
        }
    }
    /**
     * init activity default data .
     */
    private void initIntentData() {
        HmiLogUtil.debug(TAG, "initData: ");
        Intent intent = getIntent();
        mInputVid = intent.getStringExtra(MediaBaseListActivity.MEDIA_ITEM_ID_TAG);
        mUsbPort = intent.getIntExtra(USB_PORT_TAG, MediaConstDef.UsbPort.ALL);
        //winManager.getDefaultDisplay  get screen height is a error value, temporary
        // acquisition by getResources().getDimension
        //mWidth = ScreentUtils.getScreenWidth(this);
        //mHeight = ScreentUtils.getScreenHight(this);
        mWidth = (int) getResources().getDimension(R.dimen.screen_width);
        mHeight = (int) getResources().getDimension(R.dimen.screen_height);
        mFrameWindowHeight = (int) getResources().getDimension(R.dimen.video_mini_frame_height);
        mFrameWindowWidth = (int) getResources().getDimension(R.dimen.video_mini_frame_width);
        mVideoProgressBarWidth = (int) getResources().getDimension(R.dimen.progress_bar_width)
                - 2 * (int) getResources().getDimension(R.dimen.seek_bar_offset);
        mVolumemax = getResources().getInteger(R.integer.volume_max);
        mHalfFrameWidth = (int) getResources().getDimension(R.dimen.video_mini_frame_width)
                / HALF_DIVIDE_VALUE;
    }
    private void initAfterServiceConnected() {
        HmiLogUtil.debug(TAG, "initAfterServiceConnected");
        mMediaDataQueryWorker = new MediaDataQueryWorker(mMediaManager,
                MediaConstDef.MediaType.VIDEO, mUsbPort);
        mCurrentMediaItem = mMediaDataQueryWorker.getTargetMediaItem(mInputVid);
        if (null != mCurrentMediaItem) {
            initEvent();
            updateSurfaceView();
            //initSurfaceView();
            if (mServiceBind && mSurfaceInit) {
                initFirstPlay();
            }
            initAudio();
            initCtrStatu();
        }
    }
    private void initCtrStatu() {
        if (mMediaDataQueryWorker.isOnlyOneMediaItem()) {
            mBtnNext.setEnabled(false);
        } else {
            mBtnNext.setEnabled(true);
        }
    }
    private void initEvent() {
        HmiLogUtil.debug(TAG, "initEvent: ");
        mBtnPlay.setOnClickListener(mCtrlPanelClickListener);
        mBtnNext.setOnClickListener(mCtrlPanelClickListener);
        mRootView.setOnClickListener(mCtrlPanelClickListener);
        mProgressBar.setOnSeekBarChangeListener(mSeekBarChangerListener);
        mVolumeBar.setOnSeekBarChangeListener(mVoiceChangerListener);
        mPlaySurfaceView.setTouchControlListener(mSurfaceViewToucher);
    }
    private void replayCurrentVideo() {
        mVideoPlayer.resetPlayer();
        mVideoPlayer.initAutoPlay(mCurrentMediaItem.getId(), mCurrentMediaItem.getPath());
        mCurrentStartPosition = 0;
        //mVideoPlayer.setPlayPosition(0);
        if (!mVideoPlayer.isPlayingStatus()) {
            HmiLogUtil.debug(TAG, "replayCurrentVideo to play!");
            mBtnPlay.setImageResource(R.mipmap.pv_icon_time_out);
            mVideoPlayer.setAutoPlayAfterSeek(true);
            //mVideoPlayer.play();
        }
    }
    private void updateLastPlayPosition() {
        updateLastPlayPosition(mProgressBar.getProgress());
    }
    private void updateLastPlayPosition(int progress) {
        //store current video play position
        String mediaId = mCurrentMediaItem.getId();
        if (progress >= PROCESS_SAVE_PERIOD) {
            mVideoPlayer.setLastPlayPosition(mediaId, progress);
        } else {
            mVideoPlayer.setLastPlayPosition(mediaId, 0);
        }
    }
    private void playNextVideo(boolean isNext) {
        if (isNext) {
            mCurrentMediaItem = mMediaDataQueryWorker.getNextMediaItemLoop();
        } else {
            mCurrentMediaItem = mMediaDataQueryWorker.getPreMediaItemLoop();
        }
        updatePlay();
    }
    private ViewGroup.LayoutParams calculateLayoutParams(ViewGroup.LayoutParams vlp) {
        int mediaHeight = ValueParseUtils.parseStringToInt(mCurrentMediaItem.getHeight());
        int mediaWidth = ValueParseUtils.parseStringToInt(mCurrentMediaItem.getWidth());
        HmiLogUtil.debug(TAG, "video width:" + mediaHeight + " height:" + mediaWidth);
        if (-1 == mediaHeight || -1 == mediaWidth) {
            vlp.width = mWidth;
            vlp.height = mHeight;
            HmiLogUtil.debug(TAG, "Video size exception, use screen size:");
        } else if (mediaWidth > mWidth || mediaHeight > mHeight) {
            if (mWidth > 0 && mHeight > 0) {
                float widthScale = (float) mediaWidth / mWidth;
                float heightScale = (float) mediaHeight / mHeight;
                float maxScale = Math.max(widthScale, heightScale);
                vlp.width = (int) (mediaWidth / maxScale);
                vlp.height = (int) (mediaHeight / maxScale);
                HmiLogUtil.debug(TAG, "reduce video width:" + vlp.width + " height:"
                        + vlp.height);
            } else {
                vlp.width = mediaWidth;
                vlp.height = mediaHeight;
            }
        } else {
            float widthScale = (float) mWidth / mediaWidth;
            float heightScale = (float) mHeight / mediaHeight;
            float minScale = Math.min(widthScale, heightScale);
            vlp.width = (int) (mediaWidth * minScale);
            vlp.height = (int) (mediaHeight * minScale);
            HmiLogUtil.debug(TAG, "enlarge video width:" + vlp.width + " height:"
                    + vlp.height);
        }
        return vlp;
    }
    private void updatePlay() {
        mCanReceiveException = true;
        //updateSurfaceView();
        mCurrentStartPosition = mVideoPlayer.getLastPlayPosition(mCurrentMediaItem.getId());
        mVideoPlayer.resetPlayer();
        String mediaPath = mCurrentMediaItem.getPath();
        mIsException = false;
        mExceptionView.setVisibility(View.GONE);
        mPlaySurfaceView.setVisibility(View.VISIBLE);
        mTitleTv.setText(mCurrentMediaItem.getTitle());
        boolean isPlaySuccess;
        if (mCurrentStartPosition > 0) {
            isPlaySuccess = mVideoPlayer.initAutoPlay(mCurrentMediaItem.getId(), mediaPath,
                    mCurrentStartPosition * MILLION_SECOND);
        } else {
            isPlaySuccess = mVideoPlayer.initAutoPlay(mCurrentMediaItem.getId(),
                    mCurrentMediaItem.getPath());
        }
        showPlayingStatu(isPlaySuccess, mCurrentMediaItem.getMediaDuration());
    }
    private void showPlayingStatu(boolean isPlaySuccess, String mediaDuration) {
        if (isPlaySuccess) {
            long durationMil = ValueParseUtils.parseStringToLong(mediaDuration);
            if (durationMil > 0) {
                int durationSecond = (int) (durationMil / MILLION_SECOND);
                mProgressBar.setMin(0);
                mProgressBar.setMax(durationSecond);
                mProgressBar.setProgress(mCurrentStartPosition);
                sendProgressMsg();
            }
        } else {
            mProgressBar.setProgress(0);
            removeProgressMsg();
        }
        showPlayingStatu(isPlaySuccess);
        mHasPauseException = false;
    }
    private void showPlayingStatu(boolean isPlaying) {
        if (isPlaying) {
            mBtnPlay.setImageResource(R.mipmap.pv_icon_time_out);
        } else {
            mBtnPlay.setImageResource(R.mipmap.pv_icon_play);
        }
    }
    private void initFirstPlay() {
        HmiLogUtil.debug(TAG, "initFirstPlay() called");
        mCanReceiveException = true;
        int durationMil = ValueParseUtils.parseStringToInt(mCurrentMediaItem.getMediaDuration());
        if (durationMil > 0) {
            mProgressBar.setMin(0);
            mProgressBar.setProgress(0);
            mProgressBar.setMax(durationMil / MILLION_SECOND);
        }
        mCurrentStartPosition = mVideoPlayer.getLastPlayPosition(mCurrentMediaItem.getId());
        mVideoPlayer.setSurfaceViewHolder(mPlaySurfaceView);
        HmiLogUtil.debug(TAG, "setSurfaceViewHolder finished!");
        mVideoPlayer.setAutoPlayAfterSeek(true);
        String mediaPath = mCurrentMediaItem.getPath();
        String vid = mCurrentMediaItem.getId();
        boolean isPlaySuccess;
        if (mCurrentStartPosition > 0) {
            HmiLogUtil.debug(TAG, "trackingplay current startPosition:" + mCurrentStartPosition);
            isPlaySuccess = mVideoPlayer.initAutoPlay(vid, mediaPath,
                    mCurrentStartPosition * MILLION_SECOND);
        } else {
            isPlaySuccess = mVideoPlayer.initAutoPlay(vid, mediaPath);
        }
        showPlayingStatu(isPlaySuccess, mCurrentMediaItem.getMediaDuration());
        mTitleTv.setText(mCurrentMediaItem.getTitle());
        hidenCtrlPanel();
    }
    private void sendProgressMsg() {
        mPlayHandler.removeMessages(PROGRSS_UPDATE_MSG);
        mPlayHandler.sendEmptyMessageDelayed(PROGRSS_UPDATE_MSG, PROGRSS_UPDATE_DELAY);
    }
    private void removeProgressMsg() {
        mPlayHandler.removeMessages(PROGRSS_UPDATE_MSG);
    }
    /**
     * update progress when seekbar is not tracking.
     */
    public void updateProgress() {
        if (!mIsTracking) {
            int currentPosition = mVideoPlayer.getCurMoviePosition();
            int progress = Math.round((float) currentPosition / MILLION_SECOND);
            mProgressBar.setProgress(progress);
            HmiLogUtil.debug(TAG, "updateProgress progress:" + progress + " total progress:"
                    + mProgressBar.getMax());
            sendProgressMsg();
            // Reset avoid frequent clicks
            //mBtnNext.setEnabled(true);
        }
    }
    private void showCarRunningRemind() {
        HmiLogUtil.debug(TAG, "showCarRunningRemind");
        setPauseStatu();
        if (null != mComfirDialog) {
            mComfirDialog.dismiss();
        }
        mComfirDialog = new ComfirDialog(this, R.string.car_running_remind,
                ComfirDialog.DIALOG_COMFIR,
                new ComfirDialog.OnClickListener() {
                    @Override
                    public void positiveClick() {
                    }
                    @Override
                    public void negativeClick() {
                    }
                });
        mComfirDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mComfirDialog = null;
            }
        });
        mComfirDialog.show();
    }
    private class GalleryVideoNotification implements IVideoPlayHmiListener {
        @Override
        public void onVideoPlayAction(int type, int data) {
            HmiLogUtil.debug(TAG, "onVideoPlayAction eventCode:" + type);
            if (null != mPlayHandler) {
                switch (type) {
                    case MediaConstDef.VideoPlayEvent.PLAY_FINISHED:
                        removeProgressMsg();
                        mPlayHandler.removeMessages(VIDEO_PLAY_FINISHED_MSG);
                        if (mIsSurfaceCreate  && mIsResuming && mCanReceiveException) {
                            mPlayHandler.sendEmptyMessage(VIDEO_PLAY_FINISHED_MSG);
                        }
                        break;
                    case MediaConstDef.VideoPlayEvent.SEEK_FINISHED:
                        HmiLogUtil.debug(TAG, "onGalleryVideoEvent seek finished:");
                        mPlayHandler.removeMessages(VIDEO_SEEK_FINISHED_MSG);
                        if (mIsSurfaceCreate) {
                            mPlayHandler.sendEmptyMessage(VIDEO_SEEK_FINISHED_MSG);
                        }
                        break;
                    case MediaConstDef.VideoPlayEvent.AUDIO_FOCUS_LOST:
                        HmiLogUtil.debug(TAG, "onGalleryVideoEvent audio focus lost");
                        mIsAudioFocusLost = true;
                        mPlayHandler.sendEmptyMessage(AUDIO_FOCUS_LOST_MSG);
                        break;
                    case MediaConstDef.VideoPlayEvent.AUDIO_FOCUS_GAIN:
                        HmiLogUtil.debug(TAG, "onGalleryVideoEvent audio focus get again");
                        mIsAudioFocusLost = false;
                        break;
                    case MediaConstDef.VideoPlayEvent.CAR_RUNNING:
                        HmiLogUtil.debug(TAG, "onGalleryVideoEvent car is running");
                        mPlayHandler.removeMessages(SHOW_CAR_RUNNING);
                        mPlayHandler.sendEmptyMessage(SHOW_CAR_RUNNING);
                        break;
                    case MediaConstDef.VideoPlayEvent.CAR_PARKING:
                        HmiLogUtil.debug(TAG, "onGalleryVideoEvent car is parking");
                        mPlayHandler.removeMessages(HIDEN_CAR_RUNNING);
                        mPlayHandler.sendEmptyMessage(HIDEN_CAR_RUNNING);
                        break;
                    case MediaConstDef.VideoPlayEvent.REQUEST_AUDIO_FOCUS_FAULT:
                        HmiLogUtil.debug(TAG, "request audio focus fault.");
                        mPlayHandler.removeMessages(AUDIO_FOCUS_LOST_MSG);
                        mPlayHandler.sendEmptyMessage(AUDIO_FOCUS_LOST_MSG);
                        break;
                    default:
                        break;
                }
            }
        }
        @Override
        public void onVideoPlayBind(boolean success) {
            HmiLogUtil.debug(TAG, "VideoPlayBind result:" + success + " mSurfaceInit:"
                    + mSurfaceInit);
            if (success && mIsResuming) {
                mServiceBind = true;
                if (mSurfaceInit && null != mCurrentMediaItem) {
                    initFirstPlay();
                }
            }
        }
        @Override
        public void onVideoPlayError(int type, int data) {
            HmiLogUtil.debug(TAG, "onVideoPlayError type:" + type + " data:" + data);
            if (!mIsResuming) {
                mHasPauseException = true;
            }
            if (null != mPlayHandler && mIsResuming && mCanReceiveException) {
                mPlayHandler.removeMessages(VIDEO_PLAY_EXCEPTION);
                mPlayHandler.sendEmptyMessage(VIDEO_PLAY_EXCEPTION);
                mCanReceiveException = false;
            }
        }
        @Override
        public void onVideoPlayInfo(int type, int data) {
            HmiLogUtil.debug(TAG, "onVideoPlayInfo type:" + type + " data:" + data);
            if (null != mPlayHandler && type == WHAT_INFO_PLAY) {
                mPlayHandler.sendEmptyMessage(UPDATE_SURFACE_VIEW);
            }
        }
    }
    private class SurfaceHolderCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            HmiLogUtil.debug(TAG, "SurfaceHolderCallBack surface created  mSurfaceInit:"
                    + mSurfaceInit);
            if (!mHasFirstInit) {
                mSurfaceInit = true;
                if (mServiceBind &&  null != mCurrentMediaItem) {
                    initFirstPlay();
                }
                mHasFirstInit = true;
            } else {
                if (!mIsSurfaceCreate) {
                    mVideoPlayer.setSurfaceViewHolder(mPlaySurfaceView);
                    //set pause statue, must play then pause, or surface view will black
                    HmiLogUtil.debug(TAG, "surfaceCreated onresume play ");
                    //mVideoPlayer.initAutoPlay(mCurrentMediaItem.getId(),
                    //        mCurrentMediaItem.getPath(), mLastPlayTime);
                    //mVideoPlayer.pauseSeek(mLastPlayTime);
                    //mVideoPlayer.setAutoPlayAfterSeek(false);
                    if (mHasPauseException) {
                        mVideoPlayer.setAutoPlayAfterSeek(false);
                        mVideoPlayer.initAutoPlay(mCurrentMediaItem.getId(),
                                mCurrentMediaItem.getPath(), mLastPlayTime);
                    } else {
                        mVideoPlayer.setPlayPosition(mLastPlayTime);
                    }
                    mIsSurfaceRecreate = true;
                }
            }
            mIsSurfaceCreate = true;
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            HmiLogUtil.debug(TAG, "SurfaceHolderCallBack surfaceChanged width:"
                    + width + " height:" + height);
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            HmiLogUtil.debug(TAG, "SurfaceHolderCallBack surface surfaceDestroyed");
            mIsSurfaceCreate = false;
        }
    }
    @Override
    public void finish() {
        if (null != mCurrentMediaItem) {
            Intent intent = new Intent();
            intent.putExtra(MediaBaseListActivity.MEDIA_ITEM_ID_TAG, mCurrentMediaItem.getId());
            setResult(Activity.RESULT_OK, intent);
        }
        super.finish();
    }
    private BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String actionString = intent.getAction();
            HmiLogUtil.debug(TAG, "receiver local action:" + actionString);
            if (LocalBroadcastConstDef.USB_DEVICE_UNMOUNT_ACTION.equals(actionString)) {
                //receiver a usb unmounted local broad cast, if is cureent activity's usb port,
                //will finished current activity.
                int usbPort = intent.getIntExtra(LocalBroadcastConstDef.MOUNT_UNMOUNT_PORT_TAG,
                        MediaConstDef.UsbPort.ALL);
                if (mUsbPort == MediaConstDef.UsbPort.ALL || mUsbPort == usbPort) {
                    finish();
                    if (mIsResuming) {
                        ToastUtils.showUsbRejectToast(PlayVideoActivity.this, usbPort);
                    }
                }
            } else if (LocalBroadcastConstDef.GALLERY_EXTERNAL_ACTION.equals(actionString)) {
                int actionKind = intent.getIntExtra(LocalBroadcastConstDef.EXTERNAL_ACTION_KIND_TAG,
                        0);
                int subKind = intent.getIntExtra(
                        LocalBroadcastConstDef.EXTERNAL_ACTION_SUB_KIND_TAG, 0);
                if (MediaConstDef.ExternalEventKind.HVAC_CONTROL == actionKind) {
                    if (MediaConstDef.HvacCtrlKind.CONTROL_START == subKind) {
                        pausePlay();
                    }
                } else if (MediaConstDef.ExternalEventKind.HARDKEY_EVENT == actionKind) {
                    boolean isAudioFocus = mVideoPlayer.isHaveAudioFocus();
                    HmiLogUtil.debug(TAG, "Received play hardkey:" + subKind
                            + " isAudioFocus:" + isAudioFocus);
                    if (isAudioFocus && mIsResuming) {
                        if (MediaConstDef.HardKeyKind.EVENT_NEXT == subKind) {
                            playNextPreChange(true);
                        } else if (MediaConstDef.HardKeyKind.EVENT_PRE == subKind) {
                            playNextPreChange(false);
                        }
                    }
                }
            }
        }
    };
    @Override
    protected void onDestroy() {
        HmiLogUtil.debug(TAG, "onDestroy() called");
        mPlayHandler.removeMessages(PROGRSS_UPDATE_MSG);
        mPlayHandler.removeMessages(VIDEO_PLAY_FINISHED_MSG);
        mPlayHandler.removeMessages(VIDEO_SEEK_FINISHED_MSG);
        mPlayHandler.removeMessages(PANEL_HIDEN_MSG);
        mPlayHandler.removeMessages(AUDIO_FOCUS_LOST_MSG);
        mPlayHandler.removeMessages(VIDEO_PLAY_EXCEPTION);
        mPlayHandler.removeMessages(SHOW_CAR_RUNNING);
        mPlayHandler.removeMessages(HIDEN_CAR_RUNNING);
        mPlayHandler.removeMessages(UPDATE_SURFACE_VIEW);
        mPlayHandler.removeMessages(EXCEPTION_AUTOPLAY_NEXT_MSG);
        mPlayHandler.removeMessages(VOLUME_CHANGE_MSG);
        mPlayHandler = null;
        mVideoPlayer.releaseMediaPlayer();
        try {
            mVideoPlayer.release();
        } catch (IllegalArgumentException exception) {
            HmiLogUtil.err(TAG, "release play service failed!", exception);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver);
        if (null != mAudioManager) {
            mAudioManager.destory();
        }
        if (null != mMediaDataQueryWorker) {
            mMediaDataQueryWorker.dispose();
        }
        if (null != mComfirDialog) {
            mComfirDialog.dismiss();
            mComfirDialog = null;
        }
        super.onDestroy();
    }
    @Override
    protected void applicationRelease() {
        super.applicationRelease();
        Context context = getApplicationContext();
        DataMiningManager.getInstance(context).unBindService(context);
    }
    private class PlayHandler extends Handler {
        WeakReference<PlayVideoActivity> mSoftReference;
        public PlayHandler(PlayVideoActivity activity) {
            mSoftReference = new WeakReference<PlayVideoActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            PlayVideoActivity playVideoActivity = mSoftReference.get();
            if (null != playVideoActivity && !playVideoActivity.isDestroyed()) {
                switch (msg.what) {
                    case PROGRSS_UPDATE_MSG:
                        updateProgress();
                        break;
                    case VIDEO_PLAY_FINISHED_MSG:
                        updateLastPlayPosition(0);
                        if (!isFinishing()) {
                            if (mMediaDataQueryWorker.hasNextMediaItem()) {
                                Toast.makeText(PlayVideoActivity.this,
                                        getString(R.string.auto_play_next_video),
                                        Toast.LENGTH_LONG).show();
                                playNextVideo(true);
                            } else {
                                showPlayingStatu(false);
                                mVideoPlayer.setAutoPlayAfterSeek(false);
                            }
                        }
                        break;
                    case VIDEO_SEEK_FINISHED_MSG:
                        mIsTracking = false;
                        HmiLogUtil.debug(TAG, "receive seek finished ");
                        if (mVideoPlayer.isPlayingStatus() || mVideoPlayer.isAutoPlayAfterSeek()) {
                            updateProgress();
                        }
                        break;
                    case PANEL_HIDEN_MSG:
                        hidenCtrlPanel();
                        break;
                    case AUDIO_FOCUS_LOST_MSG:
                        setPauseStatu();
                        break;
                    case VIDEO_PLAY_EXCEPTION:
                        removeProgressMsg();
                        showVideoPlayException();
                        mPlayHandler.removeMessages(EXCEPTION_AUTOPLAY_NEXT_MSG);
                        mPlayHandler.sendEmptyMessageDelayed(EXCEPTION_AUTOPLAY_NEXT_MSG,
                                EXCEPTION_AUTOPLAY_DELAY);
                        break;
                    case SHOW_CAR_RUNNING:
                        showCarRunningRemind();
                        break;
                    case HIDEN_CAR_RUNNING:
                        hidenCarRunningRemind();
                        break;
                    case UPDATE_SURFACE_VIEW:
                        updateSurfaceView();
                        break;
                    case EXCEPTION_AUTOPLAY_NEXT_MSG:
                        if (mMediaDataQueryWorker.hasNextMediaItem()) {
                            //Toast.makeText(PlayVideoActivity.this,
                            //        getString(R.string.auto_play_next_video),
                            //        Toast.LENGTH_LONG).show();
                            mVideoPlayer.setAutoPlayAfterSeek(true);
                            playNextVideo(true);
                        }
                        break;
                    case VOLUME_CHANGE_MSG:
                        showVolumeValue(msg.arg1);
                        break;
                    default:
                        break;
                }
            }
            super.handleMessage(msg);
        }
    }
    private void hidenCarRunningRemind() {
        if (null != mComfirDialog) {
            mComfirDialog.dismiss();
            mComfirDialog = null;
        }
    }
}
