/**
 * Copyright (c) 2018-2020
 * All Rights Reserved by Thunder Software Technology Co., Ltd and its affiliates.
 * You may not use, copy, distribute, modify, transmit in any form this file
 * except in compliance with THUNDERSOFT in writing by applicable law.
 */
package com.ts.app.gallery.common.utilities;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Message;
import com.ts.app.gallery.widget.listener.ThumbCallBack;
/**
 * Get video frame handler thread.
 *
 * @author zhouyu.
 * @version 1.0
 */
public class VideoFrameThread extends HandlerThread {
    private static final String TAG = "VideoFrameThread";
    private long mFrameMiscSec;
    private String mFilePath;
    private static final int GET_THUMP_MSG = 1;
    private Handler mHandler;
    private ThumbCallBack mThumbCallBack;
    private boolean mIsWork;
    /**
     * create a thumbnail handlerThread.
     * @param thumbCallBack ThumbCallBack.
     */
    public VideoFrameThread(ThumbCallBack thumbCallBack) {
        super(TAG);
        mThumbCallBack = thumbCallBack;
        start();
        initHandler();
    }
    private void initHandler() {
        mHandler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case GET_THUMP_MSG:
                        String path = mFilePath;
                        long timeSec = mFrameMiscSec;
                        mFrameMiscSec = 0;
                        Bitmap bitmap = null;
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        try {
                            retriever.setDataSource(mFilePath);
                            bitmap = retriever.getFrameAtTime(timeSec,
                                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                            retriever.release();
                            HmiLogUtil.info(TAG, "Get frame release time " + mFrameMiscSec + " is:" + bitmap);
                        } catch (IllegalArgumentException ex) {
                            // Assume this is a corrupt video file
                        } catch (RuntimeException ex) {
                            // Assume this is a corrupt video file.
                        } finally {
                            try {
                                retriever.release();
                            } catch (RuntimeException ex) {
                                // Ignore failures while cleaning up.
                            }
                        }
                        mThumbCallBack.getBitmapFinished(bitmap);
                        if (mFrameMiscSec != 0) {
                            mHandler.sendEmptyMessage(GET_THUMP_MSG);
                        } else {
                            mIsWork = false;
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }
    /**
     * get video thumb.
     */
    public void sendThumbMessage(String path, long misc) {
        this.mFilePath = path;
        this.mFrameMiscSec = misc;
        if (!mIsWork) {
            mHandler.sendEmptyMessage(GET_THUMP_MSG);
            mIsWork = true;
        }
    }
    /**
     * quit thread.
     */
    public void quitThread() {
        quitSafely();
    }
}
