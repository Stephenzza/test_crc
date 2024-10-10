/**
 * Copyright (c) 2020-
 * All Rights Reserved by Thunder Software Technology Co., Ltd and its affiliates.
 * You may not use, copy, distribute, modify, transmit in any form this file
 * except in compliance with THUNDERSOFT in writing by applicable law.
 */
package com.ts.app.gallery.common.utilities;
import android.os.HandlerThread;
import android.content.Context;
import android.os.Handler;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.ts.app.gallery.data.StickyGridItem;
import android.os.Message;
import com.ts.lib.gallery.data.MediaItem;
import java.util.ListIterator;
/**
 * update sticky grid process thread, it is a handler thread.
 *
 * @author liudi
 * @version 1.0
 */
public class StickyGridThread extends HandlerThread {
    private static final String TAG = StickyGridThread.class.getSimpleName();
    private Handler mHandler;
    private Context mContext;
    private Map<String, Integer> mTimeGroupMap = new HashMap<String, Integer>();
    private int mGridColumn;
    private static final int SET_TIME_GROUP = 1;
    private static final int BUID_GRID_LIST = 2;
    /**
     * update sticky process thread, sill update picture or video list.
     *
     * @param context  application context
     */
    public StickyGridThread(Context context, int gridColumn) {
        super(TAG);
        mContext = context.getApplicationContext();
        mGridColumn = gridColumn;
        start();
        initHandler();
    }
    private void initHandler() {
        mHandler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SET_TIME_GROUP:
                        HmiLogUtil.debug(TAG, "start()");
                        List<StickyGridItem> list = (List<StickyGridItem>)msg.obj;
                        mTimeGroupMap.clear();
                        int groupNumber = 0;
                        int position = 0;
                        for (ListIterator<StickyGridItem> it = list.listIterator(); it.hasNext(); ) {
                            StickyGridItem gridItem = it.next();
                            String timeLabel = gridItem.getDateString();
                            if (!mTimeGroupMap.containsKey(timeLabel)) {
                                gridItem.setSection(groupNumber);
                                mTimeGroupMap.put(timeLabel, groupNumber);
                                groupNumber++;
                                position = ((int) Math.ceil((float) (position) / mGridColumn)) * mGridColumn;
                            } else {
                                gridItem.setSection(mTimeGroupMap.get(timeLabel));
                            }
                            gridItem.setPosition(position);
                            position++;
                        }
                        HmiLogUtil.debug(TAG, "end()");
                        break;
                    default:
                        break;
                }
            }
        };
    }
    /**
     * send set time message.
     * @param list list.
     */
    public void setTimeGroup(List<StickyGridItem> list) {
        HmiLogUtil.debug(TAG, "setTimeGroup()");
        Message message = new Message();
        message.obj = list;
        message.what = SET_TIME_GROUP;
        mHandler.sendMessage(message);
    }
    /**
     * quit thread.
     */
    public void quitThread() {
        quit();
    }
}
