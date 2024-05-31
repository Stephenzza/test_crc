/**
 * Copyright (c) 2018-2019
 * All Rights Reserved by Thunder Software Technology Co., Ltd and its affiliates.
 * You may not use, copy, distribute, modify, transmit in any form this file
 * except in compliance with THUNDERSOFT in writing by applicable law.
 */
package com.ts.app.btcall.presentation.view.impl;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.ts.app.btcall.R;
import com.ts.app.btcall.common.constant.BtActionDefine;
import com.ts.app.btcall.common.constant.BtConstant;
import com.ts.app.btcall.common.utilities.LogUtils;
import com.ts.app.btcall.common.utilities.PopupManager;
import com.ts.app.btcall.common.utilities.bean.PhonebookItem;
import com.ts.app.btcall.common.utilities.observer.SimpleSubjecter;
import com.ts.app.btcall.presentation.view.IBtCallListener;
import com.ts.app.btcall.presentation.view.IBtCallMainView;
import com.ts.app.btcall.presentation.view.impl.fragment.KeyboardFragment;
import com.ts.app.btcall.presentation.view.impl.fragment.PhonebookAllFragment;
import com.ts.app.btcall.presentation.view.impl.fragment.SearchActivity;
import com.ts.app.btcall.presentation.view.impl.fragment.SideBar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/**
 * bt call main view.
 *
 * @author XuYang
 * @version 1.0
 */
public class BtCallMainView extends SimpleSubjecter implements IBtCallMainView,
        OnClickListener, IBtCallListener, SideBar.OnTouchingLetterChangedListener {
    /**
     * this class log tag.
     */
    private static final String TAG = "BTCallMainView";
    private static final int MSG_REFRESH_ALL_DATA = 0;
    private static final int MSG_REFRESH_INIT = 1;
    private static final int MSG_REFRESH_LETTER_CHANGE = 2;
    private static final int MSG_REFRESH_SYNC_CONTACTS = 3;
    private static final int MSG_REFRESH_SYNC_RECORDS = 4;
    private static final int MSG_REFRESH_DOWNLOAD_WARNING = 5;
    private static final int MSG_REFRESH_LOAD_DATA_START = 6;
    private static final int MSG_REFRESH_LOAD_DATA_END = 7;
    private static final int MSG_REFRESH_ALL_DATA_BY_DIAL = 9;
    private static final int MSG_REFRESH_SYNC_END = 10;
    private static final int MSG_REFRESH_SHOW_DATA = 11;
    private static final int MSG_REFRESH_TAB_MENU = 12;
    private static final int MSG_REFRESH_RECORD_DATA = 13;
    private static final int MSG_REFRESH_TAB_MENU_UI = 14;
    private static final int MSG_REFRESH_CALL_END = 15;
    private static final int MSG_REFRESH_CALL_END_LOCATION = 16;
    private static final int MSG_REFRESH_FRAGMENT = 17;
    /**
     * message about refresh pinyin data.
     */
    private static final int MSG_REFRESH_LETTER_DATA = 18;
    /**
     * message about refresh edit number.
     */
    private static final int MSG_REFRESH_CLEAN_EDIT_NUMBER = 19;
    /**
     * clean number delay.
     */
    private static final int HANDLER_CLEAN_NUMBER_DELAY = 500;
    /**
     * not sync records and contacts.
     */
    private static final int STATUS_SYNC = 1;
    /**
     * sync records and contacts.
     */
    private static final int STATUS_SYNC_NOT = 0;
    /**
     * context.
     */
    private Context mContext = null;
    /**
     * activity.
     */
    private FragmentActivity mActivity;
    /**
     * keyboard layout button.
     */
    private LinearLayout mLayoutBtnKeyboard;
    /**
     * search layout button.
     */
    private LinearLayout mLayouBtnSearch;
    /**
     * contacts layout button.
     */
    private LinearLayout mLayouBtnContacts;
    /**
     * recent button.
     */
    private Button mBtnRecent;
    /**
     * use button.
     */
    private Button mBtnUse;
    /**
     * letter view.
     */
    private SideBar mSidebarLetter;
    /**
     * letter tip.
     */
    private TextView mTxtTip;
    /**
     * download button.
     */
    private Button mBtnDownload;
    /**
     * textView download item.
     */
    private TextView mTvDownloadCount;
    /**
     * progressBar display download progress.
     */
    private ProgressBar mPbDownload;
    /**
     * tip about no records or no result.
     */
    private TextView mTextTip;
    /**
     * tip about no contacts or no result.
     */
    private TextView mTextTipContacts;
    /**
     * Popup control.
     */
    private PopupManager mPopupManager;
    /**
     * message.
     */
    private TextView mTextMsg;
    /**
     * main switch.
     */
    private Switch mSwitch;
    /**
     * wheel view.
     */
    private ImageView mSyncWheelView;
    /**
     * download wheel animator.
     */
    private AnimationDrawable mRotateAnimator;
    /**
     * keyboard fragment.
     */
    private KeyboardFragment mKeyboardFragment;
    /**
     * phone book all fragment.
     */
    private PhonebookAllFragment mPhonebookAllFragment;
    /**
     * letter layout.
     */
    private RelativeLayout mLayoutBelow;
    /**
     * keyboard show flag.
     */
    private boolean mIsKeyboardShow = false;
    /**
     * keyboard show flag.
     */
    private boolean mUpdateRecords = false;
    /**
     * btcall Loading Imageview.
     */
    private ImageView mBtcallLoadingIm;
    private Animation mRotateAnimation;
    private LinearLayout mLoadingLayout;
    /**
     * main layout.
     */
    private RelativeLayout mLayoutMain;
    /**
     * Fragment manager.
     */
    private FragmentManager mFragmentManager;
    /**
     * contacts list to separate data from records.
     */
    private List<PhonebookItem> mPhoneBookData = new ArrayList<PhonebookItem>();
    /**
     * records list only to separate data from contacts.
     */
    private List<PhonebookItem> mRecordData = new ArrayList<PhonebookItem>();
    /**
     * pinyin letter map data.
     */
    private HashMap<String, Integer> mLetterMap = new HashMap<String, Integer>();
    /**
     * download state flag.
     */
    private boolean mLoadState = false;
    /**
     * request search flag.
     */
    private boolean mFlagReq = false;
    /**
     * request search key flag.
     */
    private String mReqKey = "";
    /**
     * BtCallMainView construction.
     *
     * @param context context
     */
    public BtCallMainView(Context context) {
        // TODO Auto-generated constructor stub
        mContext = context;
        mActivity = (FragmentActivity) mContext;
        initView();
    }
    /**
     * init view.
     */
    private void initView() {
        mLayoutBtnKeyboard = (LinearLayout) mActivity.findViewById(R.id.ll_keyboard);
        mLayouBtnContacts = (LinearLayout) mActivity.findViewById(R.id.ll_contacts);
        mLayouBtnSearch = (LinearLayout) mActivity.findViewById(R.id.ll_search);
        mSwitch = (Switch) mActivity.findViewById(R.id.main_switch);
        mBtnRecent = (Button) mActivity.findViewById(R.id.btn_recent);
        mBtnDownload = (Button) mActivity.findViewById(R.id.btn_download);
        mTvDownloadCount = (TextView) mActivity.findViewById(R.id.text_msg_supply);
        mPbDownload = (ProgressBar) mActivity.findViewById(R.id.progressbar_download);
        mLoadingLayout = (LinearLayout) mActivity.findViewById(R.id.loading_layout);
        mBtcallLoadingIm = (ImageView) mActivity.findViewById(R.id.btcall_progress);
        mRotateAnimation = AnimationUtils.loadAnimation(mContext, R.anim.loading_animator);
        mTextMsg = (TextView) mActivity.findViewById(R.id.text_msg);
        mTextTip = (TextView) mActivity.findViewById(R.id.text_tips);
        mTextTipContacts = (TextView) mActivity.findViewById(R.id.text_tips_contacts);
        mBtnDownload.setOnClickListener(this);
        mSyncWheelView = (ImageView) mActivity.findViewById(R.id.img_sync_roller);
        mSyncWheelView.setBackgroundResource(R.drawable.sync_roller_000);
        mSyncWheelView.setVisibility(View.GONE);
        mBtnUse = (Button) mActivity.findViewById(R.id.btn_use);
        mSidebarLetter = mActivity.findViewById(R.id.sidebar_letter);
        mTxtTip = mActivity.findViewById(R.id.sidebar_letter_tips);
        mLayoutBelow = mActivity.findViewById(R.id.layout_below);
        mLayoutBelow.setVisibility(View.GONE);
        mSidebarLetter.setOnTouchingLetterChangedListener(this);
        mSidebarLetter.setTextDialog(mTxtTip);
        mLayoutBtnKeyboard.setOnClickListener(this);
        mLayouBtnContacts.setOnClickListener(this);
        mLayouBtnSearch.setOnClickListener(this);
        mBtnUse.setOnClickListener(this);
        mBtnRecent.setOnClickListener(this);
        mLayoutMain = (RelativeLayout) mActivity.findViewById(R.id.layout_main);
        mKeyboardFragment = new KeyboardFragment();
        mKeyboardFragment.setListener(this);
        mPhonebookAllFragment = new PhonebookAllFragment();
        mPhonebookAllFragment.setListener(this);
        mFragmentManager = mActivity.getSupportFragmentManager();
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.frame_layout_list, mKeyboardFragment);
        transaction.add(R.id.frame_layout_list, mPhonebookAllFragment);
        transaction.show(mKeyboardFragment).hide(mPhonebookAllFragment);
        transaction.commitAllowingStateLoss();
        setSelectBackground(R.id.ll_keyboard);
        setLetterLayoutShow(false);
        LogUtils.log(TAG, "initView() setLetterLayoutShow(false)");
    }
    @Override
    public void onClick(View v) {
        LogUtils.log(TAG, "onClick" + v.getId());
        LogUtils.log(TAG, "onClick" + v.getId());
        switch (v.getId()) {
            case R.id.ll_keyboard:
                if (isShownTabKeyboard()) {
                    return;
                }
                Message msgRecords = Message.obtain();
                msgRecords.what = BtActionDefine.ACTION_TAB_MENU;
                msgRecords.arg1 = BtConstant.TYPE_KEYBORD;
                msgRecords.arg2 = BtConstant.FLAG_CLEAR_EDIT;
                BtCallMainView.this.notify(msgRecords, FLAG_RUN_SYNC);
                LogUtils.log(TAG, "switch keyboard :" + BtConstant.TYPE_KEYBORD);
                notifyDataMiningClickEvent(BtActionDefine.DATAMINING_ACTION_KEYBOARD_BUTTON_CLICK);
                mSwitch.setChecked(false);
                break;
            case R.id.ll_contacts:
                if (!isShownTabKeyboard()) {
                    return;
                }
                Message msgContacts = Message.obtain();
                msgContacts.what = BtActionDefine.ACTION_TAB_MENU;
                msgContacts.arg1 = BtConstant.TYPE_CONTACTS;
                BtCallMainView.this.notify(msgContacts, FLAG_RUN_SYNC);
                LogUtils.log(TAG, "switch contacts :" + BtConstant.TYPE_CONTACTS);
                notifyDataMiningClickEvent(BtActionDefine.DATAMINING_ACTION_CONTACTS_BUTTON_CLICK);
                mSwitch.setChecked(true);
                break;
            case R.id.ll_search:
                Message msgSearch = Message.obtain();
                msgSearch.what = BtActionDefine.ACTION_TAB_MENU;
                msgSearch.arg1 = BtConstant.TYPE_SEARCH;
                BtCallMainView.this.notify(msgSearch, FLAG_RUN_SYNC);
                LogUtils.log(TAG, "switch search :" + BtConstant.TYPE_SEARCH);
                break;
            case R.id.btn_recent:
                Message msgRecent = Message.obtain();
                msgRecent.what = BtActionDefine.ACTION_VIEW_NOTIFY_RECENT_USE;
                msgRecent.arg1 = BtConstant.TYPE_KEYBORD;
                BtCallMainView.this.notify(msgRecent, FLAG_RUN_SYNC);
                break;
            case R.id.btn_use:
                Message msgUse = Message.obtain();
                msgUse.what = BtActionDefine.ACTION_VIEW_NOTIFY_RECENT_USE;
                msgUse.arg1 = BtConstant.TYPE_CONTACTS;
                BtCallMainView.this.notify(msgUse, FLAG_RUN_SYNC);
                break;
            case R.id.btn_download:
                onClickDownload();
                mBtnDownload.setEnabled(false);
                break;
            default:
                break;
        }
    }
    /**
     * judge keyboard is shown or not.
     *
     * @return boolean
     */
    @Override
    public boolean isShownTabKeyboard() {
        if (mKeyboardFragment != null && mKeyboardFragment.isKeyboardShown()) {
            return true;
        } else {
            return false;
        }
    }
    @Override
    public void setUpdateRecordsFlag(boolean isUpdate) {
        this.mUpdateRecords = isUpdate;
    }
    @Override
    public boolean isNotEmptyKeyboardInput() {
        return false;
    }
    @Override
    public void callEndExit(String number) {
    }
    @Override
    public void locateListPosition(List<PhonebookItem> list, int index) {
        LogUtils.log(TAG, "locateListPosition index2:" + index);
        mPhoneBookData.clear();
        mPhoneBookData.addAll(list);
        Message msg = new Message();
        msg.what = MSG_REFRESH_CALL_END_LOCATION;
        msg.obj = index;
        mRefreshViewHandler.sendMessage(msg);
        LogUtils.log(TAG, "locateListPosition msg.obj: " + msg.obj);
    }
    @Override
    public void locatePhonebookListPosition(int index) {
        Message msg = new Message();
        msg.what = MSG_REFRESH_CALL_END;
        msg.arg1 = index;
        mRefreshViewHandler.sendMessage(msg);
        LogUtils.log(TAG, "locatePhonebookListPosition" + index);
    }
    /**
     * notify DataMining click event.
     *
     * @param action action
     */
    private void notifyDataMiningClickEvent(int action) {
        Message msg = Message.obtain();
        msg.what = action;
        BtCallMainView.this.notify(msg, FLAG_RUN_SYNC);
    }
    /**
     * notify get contact/call record data.
     *
     * @param index tab index.
     */
    private void notifyGetData(int index) {
        LogUtils.log(TAG, "notifyGetData dataType" + index);
        Message msgUse = Message.obtain();
        msgUse.what = BtActionDefine.ACTION_VIEW_NOTIFY_CONTACTS_OR_CALLRECORD;
        if (R.id.ll_contacts == index) {
            LogUtils.log(TAG, "notifyGetData index is contacts: " + index);
            msgUse.arg1 = BtConstant.TYPE_CONTACTS;
            boolean isChangeContact =
                    mPhonebookAllFragment != null && mPhonebookAllFragment.isListEmpty();
            if (isChangeContact) {
                //only no data app will still get data.
                BtCallMainView.this.notify(msgUse, FLAG_RUN_SYNC);
                LogUtils.log(TAG, "notifyGetData index is contacts if (isChangeContact)...");
            } else {
                //there is data exits and only change tab menu ui about contact.
                Message msg = new Message();
                msg.what = MSG_REFRESH_TAB_MENU_UI;
                msg.arg1 = BtConstant.TYPE_CONTACTS;
                mRefreshViewHandler.sendMessage(msg);
                LogUtils.log(TAG, "notifyGetData index is contacts else");
            }
        } else {
            LogUtils.log(TAG, "notifyGetData mUpdateRecords :" + mUpdateRecords);
            boolean isChangeRecord = mKeyboardFragment != null && mKeyboardFragment.isListEmpty();
            if (mUpdateRecords || isChangeRecord) {
                //only no data app will still get data.
                msgUse.arg1 = BtConstant.TYPE_KEYBORD;
                BtCallMainView.this.notify(msgUse, FLAG_RUN_SYNC);
                mUpdateRecords = false;
                LogUtils.log(TAG, "notifyGetData mUpdateRecords || isChangeRecord");
            } else {
                //there is data exits and only change tab menu ui about keyboard.
                Message msg = new Message();
                msg.what = MSG_REFRESH_TAB_MENU_UI;
                msg.arg1 = BtConstant.TYPE_KEYBORD;
                mRefreshViewHandler.sendMessage(msg);
                LogUtils.log(TAG, "notifyGetData else...");
            }
        }
    }
    /**
     * set menu select background.
     *
     * @param index button id
     */
    private void setSelectBackground(int index) {
        LogUtils.log(TAG, "setSelectBackground index" + index);
        if (mLayouBtnSearch == null || mLayouBtnContacts == null || mLayoutBtnKeyboard == null) {
            return;
        }
        switch (index) {
            case R.id.ll_keyboard:
                mLayouBtnContacts.setBackground(null);
                mLayouBtnSearch.setBackground(null);
                setLetterLayoutShow(false);
                break;
            case R.id.ll_contacts:
                mLayoutBtnKeyboard.setBackground(null);
                mLayouBtnSearch.setBackground(null);
                setLetterLayoutShow(true);
                break;
            case R.id.ll_search:
                break;
            default:
                break;
        }
    }
    /**
     * set letter layout show.
     *
     * @param flag show flag
     */
    public void setLetterLayoutShow(boolean flag) {
        if (mLayoutBelow == null) {
            LogUtils.log(TAG, "LetterLayout is null :");
            return;
        }
        LogUtils.log(TAG, "setLetterLayoutShow :" + flag);
        if (flag) {
            mLayoutBelow.setVisibility(View.VISIBLE);
            LogUtils.log(TAG, "setLetterLayoutShow View.VISIBLE");
        } else {
            mLayoutBelow.setVisibility(View.GONE);
            LogUtils.log(TAG, "setLetterLayoutShow View.GONE");
        }
    }
    @Override
    public void releaseView() {
        // TODO Auto-generated method stub
        mContext = null;
        if (mKeyboardFragment != null) {
            mKeyboardFragment.clearWheelAnimator();
        }
        if (mPhonebookAllFragment != null) {
            mPhonebookAllFragment.clearWheelAnimator();
        }
        removePop();
        mRefreshViewHandler.removeCallbacksAndMessages(null);
        mActivity.finish();//fix bug #12488
    }
    @Override
    public void setAdapterAllData(List<PhonebookItem> list, int index, int dataType) {
        LogUtils.log(TAG, "setAdapterAllData(), index :" + index + " , dataType :" + dataType);
        LogUtils.log(TAG, "setAdapterAllData" + list.size());
        if (dataType == BtConstant.TYPE_CONTACTS) {
            mPhoneBookData.clear();
            mPhoneBookData.addAll(list);
        } else {
            mRecordData.clear();
            mRecordData.addAll(list);
            LogUtils.log(TAG, "setAdapterAllData else_addAll(list)");
        }
        Message msg = new Message();
        msg.what = MSG_REFRESH_ALL_DATA;
        msg.arg2 = dataType;
        msg.obj = index;
        mRefreshViewHandler.sendMessage(msg);
    }
    @Override
    public void setAdapterSearchData(List<PhonebookItem> list) {
    }
    @Override
    public void setAdapterDataByDial(List<PhonebookItem> list) {
        if (mContext == null) {
            LogUtils.log(TAG, "setAdapterDataByDial mContext null");
            return;
        }
        if (TextUtils.isEmpty(getCurrentEditText())) {
            LogUtils.log(TAG, "setAdapterDataByDial return");
            mFlagReq = false;
            return;
        }
        mRecordData.clear();
        mRecordData.addAll(list);
        Message msg = new Message();
        msg.what = MSG_REFRESH_ALL_DATA_BY_DIAL;
        msg.arg1 = BtConstant.TYPE_CONTACTS;
        msg.arg2 = BtConstant.TYPE_SEARCH;
        mRefreshViewHandler.sendMessage(msg);
        if (!TextUtils.isEmpty(mReqKey)) {
            reqSearchContacts(mReqKey);
            LogUtils.log(TAG, "setAdapterDataByDial reqSearchContacts_ReqKey:" + mReqKey);
            mReqKey = "";
        } else {
            mFlagReq = false;
            LogUtils.log(TAG, "setAdapterDataByDial else");
        }
    }
    @Override
    public void setPhoneBookListPosition(int index, boolean isUseData) {
        LogUtils.log(TAG, "setPhoneBookListPosition index:" + index);
        if (isUseData) {
            mBtnUse.setSelected(true);
            mSidebarLetter.setSelectedIndex(-1);
        } else {
            mBtnUse.setSelected(false);
        }
        mBtnUse.setAlpha(BtConstant.NORMAL_ALPHA);
        Message msg = new Message();
        msg.what = MSG_REFRESH_LETTER_CHANGE;
        msg.arg1 = index;
        mRefreshViewHandler.sendMessage(msg);
    }
    @Override
    public void setRecycleViewScrollMode(boolean isScroll) {
        if (mPhonebookAllFragment != null) {
            mPhonebookAllFragment.setRecycleViewScrollMode(isScroll);
        }
    }
    @Override
    public void initData(boolean isSync) {
        LogUtils.log(TAG, "initData isSync:" + isSync);
        if (mContext == null) {
            LogUtils.log(TAG, "initData mContext:null");
            return;
        }
        Message msg = new Message();
        msg.what = MSG_REFRESH_INIT;
        if (isSync) {
            msg.arg1 = STATUS_SYNC;
        } else {
            msg.arg1 = STATUS_SYNC_NOT;
        }
        mRefreshViewHandler.sendMessage(msg);
    }
    @Override
    public void showNoData(List<PhonebookItem> list, int type) {
        LogUtils.log(TAG, "showNoData type :" + type);
        mPhoneBookData.clear();
        Message msg = new Message();
        msg.what = MSG_REFRESH_ALL_DATA;
        msg.arg1 = BtConstant.TYPE_CONTACTS;
        msg.arg2 = type;
        mRefreshViewHandler.sendMessage(msg);
    }
    @Override
    public void setLetterData(HashMap<String, Integer> map, boolean isUseData, int index) {
        if (mLetterMap != null) {
            mLetterMap.clear();
            mLetterMap.putAll(map);
        }
        Message msg = new Message();
        msg.what = MSG_REFRESH_LETTER_DATA;
        msg.obj = isUseData;
        msg.arg2 = index;
        mRefreshViewHandler.sendMessage(msg);
    }
    @Override
    public void displayDownloadContactProgress(int currentSize, int totalSize) {
        if (mContext == null) {
            LogUtils.log(TAG, "displayDownloadContactProgress mContext null");
            return;
        }
        Message message = new Message();
        message.what = MSG_REFRESH_SYNC_CONTACTS;
        message.arg1 = currentSize;
        message.arg2 = totalSize;
        mRefreshViewHandler.sendMessage(message);
    }
    @Override
    public void displayDownloadRecordProgress(int currentSize, int totalSize) {
        if (mContext == null) {
            LogUtils.log(TAG, "displayDownloadRecordProgress mContext null");
            return;
        }
        Message message = new Message();
        message.what = MSG_REFRESH_SYNC_RECORDS;
        message.arg1 = currentSize;
        message.arg2 = totalSize;
        mRefreshViewHandler.sendMessage(message);
    }
    @Override
    public void clearEditInput() {
        LogUtils.log(TAG, "clearEditInput().");
        if (mKeyboardFragment != null) {
            mKeyboardFragment.clearEditInput();
        }
    }
    @Override
    public void toastDownloadWaring(int type) {
        LogUtils.log(TAG, "toastDownloadWaring type:" + type);
        if (mContext == null) {
            LogUtils.log(TAG, "toastDownloadWaring mContext:null");
            return;
        }
        Message msg = new Message();
        msg.what = MSG_REFRESH_DOWNLOAD_WARNING;
        msg.arg1 = type;
        mRefreshViewHandler.sendMessage(msg);
    }
    @Override
    public void showLoadingAnimator(int isLoading) {
        if (isLoading == BtConstant.CONTACT_LOADED) {
            mRefreshViewHandler.sendEmptyMessage(MSG_REFRESH_LOAD_DATA_END);
        } else {
            if (isLoading == BtConstant.CONTACT_LOADING) {
                mRefreshViewHandler.sendEmptyMessage(MSG_REFRESH_LOAD_DATA_START);
            }
        }
    }
    @Override
    public void notifySearchData(List<PhonebookItem> list) {
    }
    @Override
    public void updateRecordsData(List<PhonebookItem> list) {
        if (mContext == null) {
            LogUtils.log(TAG, "updateRecordsData mContext null");
            return;
        }
        mRecordData.clear();
        mRecordData.addAll(list);
        Message msg = new Message();
        msg.what = MSG_REFRESH_RECORD_DATA;
        msg.arg1 = STATUS_SYNC;
        msg.arg2 = BtConstant.TYPE_KEYBORD;
        mRefreshViewHandler.sendMessage(msg);
    }
    @Override
    public void switchTabMenu(int type) {
        Message msg = new Message();
        msg.what = MSG_REFRESH_TAB_MENU;
        msg.arg1 = type;
        mRefreshViewHandler.sendMessage(msg);
    }
    @Override
    public void changeFragment(int type) {
        Message msg = new Message();
        msg.what = MSG_REFRESH_FRAGMENT;
        msg.arg1 = type;
        mRefreshViewHandler.sendMessage(msg);
    }
    @Override
    public void changeTabMenuUi(int type) {
        Message msg = new Message();
        msg.what = MSG_REFRESH_TAB_MENU_UI;
        msg.arg1 = type;
        mRefreshViewHandler.sendMessage(msg);
    }
    private void switchTabList(int type) {
        LogUtils.log(TAG, "switchTabList type: " + type);
        if (type == BtConstant.TYPE_CONTACTS) {
            //switch contacts UI.
            LogUtils.log(TAG, "switchTabList mIsKeyboardShow: " + mIsKeyboardShow);
            if (!mIsKeyboardShow) {
                onContactsViewShow();
                LogUtils.log(TAG, "switchTabList onContactsViewShow()");
            }
            FragmentTransaction transaction1 = mFragmentManager.beginTransaction();
            LogUtils.log(TAG, "beginTransaction()");
            transaction1.show(mPhonebookAllFragment).hide(mKeyboardFragment);
            transaction1.commitAllowingStateLoss();
            setNoContactsTipVisible(View.GONE);
            notifyGetData(R.id.ll_contacts);
            LogUtils.log(TAG, "switchTabList notifyGetData(R.id.ll_contacts);");
            setLetterLayoutShow(true);
        } else {
            //switch search UI.
            if (type == BtConstant.TYPE_SEARCH) {
                Intent intent = new Intent(mActivity, SearchActivity.class);
                mActivity.startActivityForResult(intent, BtConstant.CALL_CODE_REQUEST);
            } else {
                //switch records UI.
                onContactsViewHide();
                FragmentTransaction transaction2 = mFragmentManager.beginTransaction();
                transaction2.show(mKeyboardFragment).hide(mPhonebookAllFragment);
                transaction2.commitAllowingStateLoss();
                notifyGetData(R.id.ll_keyboard);
                LogUtils.log(TAG, "switchTabList notifyGetData(R.id.ll_keyboard);");
                setLetterLayoutShow(false);
            }
        }
    }
    @Override
    public void setSelectedUse(boolean isSelected) {
        mBtnUse.setSelected(isSelected);
    }
    @Override
    public void refreshLetterPosition(int index) {
        LogUtils.log(TAG, "refreshLetterPosition index:" + index);
        boolean selectLetter = index == BtConstant.VALUE_UNKNOWN;
        if (selectLetter) {
            mBtnUse.setSelected(true);
            LogUtils.log(TAG, "refreshLetterPosition if (selectLetter)");
        } else {
            mBtnUse.setSelected(false);
            LogUtils.log(TAG, "refreshLetterPosition else");
        }
        mBtnUse.setAlpha(BtConstant.NORMAL_ALPHA);
        if (!isShownTabKeyboard()) {
            mSidebarLetter.setSelectedIndex(index);
        }
    }
    @Override
    public void cancelSyncAnimator() {
        mRefreshViewHandler.sendEmptyMessage(MSG_REFRESH_SYNC_END);
    }
    /**
     * refresh view handler.
     */
    private Handler mRefreshViewHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mContext == null) {
                LogUtils.log(TAG, "handlerRefreshView mContext null");
                return;
            }
            int what = msg.what;
            LogUtils.log(TAG, "handlerRefreshView msg :" + what);
            switch (what) {
                case MSG_REFRESH_ALL_DATA:
                    if (mKeyboardFragment != null) {
                        updateListData(msg);
                        setDownloadProgressBarShow();
                    }
                    checkBtnEnableState();
                    break;
                case MSG_REFRESH_ALL_DATA_BY_DIAL:
                    if (mKeyboardFragment != null) {
                        mKeyboardFragment.updateDialData(mRecordData, msg.arg2);
                        showNotice(mRecordData, BtConstant.TYPE_KEYBORD);
                        showNotice(mRecordData, msg.arg2);
                        setDownloadProgressBarShow();
                    }
                    checkBtnEnableState();
                    break;
                case MSG_REFRESH_INIT:
                    initDownloadView(msg);
                    break;
                case MSG_REFRESH_LETTER_CHANGE:
                    if (mPhonebookAllFragment != null) {
                        mPhonebookAllFragment.gotoIndexLetter(msg.arg1);
                    }
                    break;
                case MSG_REFRESH_SYNC_CONTACTS:
                    mLoadState = true;
                    showSyncDownloadProgress(msg);
                    break;
                case MSG_REFRESH_SYNC_RECORDS:
                    mLoadState = true;
                    showSyncDownloadProgress(msg);
                    break;
                case MSG_REFRESH_DOWNLOAD_WARNING:
                    toastDownloadWarning(msg.arg1);
                    break;
                case MSG_REFRESH_LOAD_DATA_START:
                    showLoadDataMsg(BtConstant.CONTACT_LOADING);
                    mLoadState = true;
                    break;
                case MSG_REFRESH_LOAD_DATA_END:
                    showLoadDataMsg(BtConstant.CONTACT_LOADED);
                    mLoadState = false;
                    break;
                case MSG_REFRESH_SYNC_END:
                    stopSyncAnimator();
                    break;
                case MSG_REFRESH_SHOW_DATA:
                    //hide no contacts.
                    mLoadState = true;
                    setNoContactsTipVisible(View.GONE);
                    notifyGetData(R.id.ll_keyboard);
                    break;
                case MSG_REFRESH_TAB_MENU:
                    switchTabMenuUi(msg.arg1, MSG_REFRESH_TAB_MENU);
                    switchTabList(msg.arg1);
                    break;
                case MSG_REFRESH_RECORD_DATA:
                    if (mKeyboardFragment != null) {
                        initDownloadView(msg);
                        setDownloadProgressBarShow();
                        mKeyboardFragment.updateDialData(mRecordData, msg.arg2);
                        showNotice(mRecordData, BtConstant.TYPE_KEYBORD);
                    }
                    checkBtnEnableState();
                    break;
                case MSG_REFRESH_TAB_MENU_UI:
                    switchTabMenuUi(msg.arg1, MSG_REFRESH_TAB_MENU_UI);
                    break;
                case MSG_REFRESH_CALL_END:
                    setPhoneBookLocation(msg.arg1);
                    break;
                case MSG_REFRESH_CALL_END_LOCATION:
                    refrshPhonebookLocation(msg);
                    break;
                case MSG_REFRESH_FRAGMENT:
                    switchFragment(msg.arg1);
                    break;
                case MSG_REFRESH_LETTER_DATA:
                    setPinyinLetterData(msg);
                    break;
                case MSG_REFRESH_CLEAN_EDIT_NUMBER:
                    if (mKeyboardFragment != null) {
                        LogUtils.log(TAG, "clearEditNumber call fragment");
                        mKeyboardFragment.clearEditNumber();
                    }
                    break;
                default:
                    break;
            }
        }
    };
    /**
     * update pinyin letter data.
     *
     * @param msg msg
     */
    private void setPinyinLetterData(Message msg) {
        if (mSidebarLetter != null) {
            boolean isUseData = false;
            if (msg.obj != null) {
                isUseData = (boolean) msg.obj;
            }
            if (isUseData) {
                mBtnUse.setSelected(true);
            } else {
                mBtnUse.setSelected(false);
            }
            mBtnUse.setAlpha(BtConstant.NORMAL_ALPHA);
            mSidebarLetter.setLetterData(mLetterMap, msg.arg2);
        }
    }
    /**
     * During bluetooth is disconnected,ui displays only.
     *
     * @param type type
     */
    private void switchFragment(int type) {
        if (type == BtConstant.TYPE_CONTACTS) {
            LogUtils.log(TAG, "switchFragment type is TYPE_CONTACTS");
            FragmentTransaction transaction1 = mFragmentManager.beginTransaction();
            transaction1.show(mPhonebookAllFragment).hide(mKeyboardFragment);
            transaction1.commitAllowingStateLoss();
            mSwitch.setChecked(true);
            setSelectBackground(R.id.ll_contacts);
            boolean shownBtnDownload = mBtnDownload.isShown();
            LogUtils.log(TAG, "setLetterLayoutShow　shown：" + shownBtnDownload);
            if (isShownPbDownload() || shownBtnDownload) {
                setLetterLayoutShow(false);
            } else {
                setLetterLayoutShow(true);
            }
        } else {
            if (type != BtConstant.TYPE_SEARCH) {
                LogUtils.log(TAG, "switchFragment type is TYPE_SEARCH");
                FragmentTransaction transaction2 = mFragmentManager.beginTransaction();
                transaction2.show(mKeyboardFragment).hide(mPhonebookAllFragment);
                transaction2.commitAllowingStateLoss();
                setSelectBackground(R.id.ll_keyboard);
            }
        }
        setNoContactsTipShow(View.GONE);
    }
    @Override
    public boolean isShownPbDownload() {
        return mPbDownload.isShown();
    }
    @Override
    public void clearEditNumber() {
        if (mKeyboardFragment != null) {
            LogUtils.log(TAG, "clearEditNumber message send remove");
            Message msg = new Message();
            msg.what = MSG_REFRESH_CLEAN_EDIT_NUMBER;
            mRefreshViewHandler.removeMessages(MSG_REFRESH_CLEAN_EDIT_NUMBER);
            mRefreshViewHandler.sendMessageDelayed(msg, HANDLER_CLEAN_NUMBER_DELAY);
        }
    }
    /**
     * set phoneBook location.
     *
     * @param postion postion
     */
    private void setPhoneBookLocation(int postion) {
        if (mPhonebookAllFragment != null) {
            mPhonebookAllFragment.setListPosition(postion);
        }
    }
    /**
     * refresh phoneBook location.
     *
     * @param msg msg
     */
    private void refrshPhonebookLocation(Message msg) {
        if (mKeyboardFragment != null && mKeyboardFragment.isKeyboardShown()) {
            FragmentTransaction transaction1 = mFragmentManager.beginTransaction();
            transaction1.show(mPhonebookAllFragment).hide(mKeyboardFragment);
            transaction1.commitAllowingStateLoss();
        }
        if (mPhonebookAllFragment != null) {
            if (msg.obj != null) {
                LogUtils.log(TAG, "refrshPhonebookLocation setDataUpdate ");
                mPhonebookAllFragment.setDataUpdate(mPhoneBookData, BtConstant.TYPE_CONTACTS, (int) msg.obj);
                showNotice(mPhoneBookData,BtConstant.TYPE_CONTACTS);
                int obj = (int) msg.obj;
                LogUtils.log(TAG, "refrshPhonebookLocation setDataUpdate" + BtConstant.TYPE_CONTACTS + "obj: " + obj);
            } else {
                mPhonebookAllFragment.setDataUpdate(mPhoneBookData, BtConstant.TYPE_CONTACTS,
                        BtConstant.RECORD_FLAG_VALUE);
                showNotice(mPhoneBookData,BtConstant.TYPE_CONTACTS);
            }
            setSelectBackground(R.id.ll_contacts);
        }
        setLetterLayoutShow(true);
        setNoContactsTipVisible(View.GONE);
    }
    /**
     * change tab menu selected status.
     *
     * @param type type
     */
    private void switchTabMenuUi(int type, int flag) {
        LogUtils.log(TAG, "switchTabMenuUi type :" + type);
        if (type == BtConstant.TYPE_CONTACTS) {
            setSelectBackground(R.id.ll_contacts);
            showNotice(mPhoneBookData, BtConstant.TYPE_CONTACTS);
            LogUtils.log(TAG, "switchTabMenuUi " + BtConstant.TYPE_CONTACTS);
        } else {
            if (type != BtConstant.TYPE_SEARCH) {
                setSelectBackground(R.id.ll_keyboard);
                showNotice(mRecordData, BtConstant.TYPE_KEYBORD);
                LogUtils.log(TAG, "switchTabMenuUi showNotice keyboard");
            } else {
                LogUtils.log(TAG, "switchTabMenuUi showNotice search flag: " + flag);
                if (flag == MSG_REFRESH_TAB_MENU_UI) {
                    showNotice(mRecordData, BtConstant.TYPE_SEARCH);
                }
            }
        }
        setDownloadProgressBarShow();
        LogUtils.log(TAG, "setDownloadProgressBarShow()");
        checkBtnEnableState();
    }
    /**
     * init download view display.
     *
     * @param msg msg
     */
    private void initDownloadView(Message msg) {
        if (msg.arg1 == STATUS_SYNC_NOT) {
            setDownloadBtnShow(false);
            setDownloadProgressBarShow();
            setNoContactsTipShow(View.GONE);
            setWheelViewVisibility(View.GONE);
            enableDownloadBtn();
            setMainBackground(R.drawable.btcall_bg_download_res);
        } else {
            setDownloadBtnShow(true);
            setDownloadProgressBarShow();
            setNoContactsTipShow(View.GONE);
            setWheelViewVisibility(View.VISIBLE);
            enableDownloadBtn();
            checkBtnEnableState();
            setMainBackground(R.drawable.btcall_bg);
        }
    }
    /**
     * set fragment wheel view visibility.
     *
     * @param visibility visibility
     */
    private void setWheelViewVisibility(int visibility) {
        if (mPhonebookAllFragment != null) {
            mPhonebookAllFragment.setWheelViewVisibility(visibility);
        }
        if (mKeyboardFragment != null) {
            mKeyboardFragment.setWheelViewVisibility(visibility);
        }
    }
    /**
     * enable download button.
     */
    public void enableDownloadBtn() {
        if (mBtnDownload != null) {
            mBtnDownload.setEnabled(true);
        }
    }
    /**
     * set download button visibility.
     */
    public void setDownloadBtnShow(boolean visible) {
        LogUtils.log(TAG, "setDownloadBtnShow()" + visible);
        if (visible) {
            mBtnDownload.setVisibility(View.GONE);
            mSyncWheelView.setVisibility(View.GONE);
            mTextMsg.setVisibility(View.GONE);
        } else {
            mBtnDownload.setVisibility(View.VISIBLE);
            mSyncWheelView.setVisibility(View.VISIBLE);
            mTextMsg.setVisibility(View.VISIBLE);
            mTextMsg.setText(mContext.getResources().getString(R.string.download_msg));
            mSyncWheelView.setBackgroundResource(R.drawable.sync_roller_000);
        }
    }
    /**
     * display contacts download process.
     *
     * @param currentSize currentSize
     * @param totalSize   totalSize
     */
    public void showContactDownloadProgress(int currentSize, int totalSize) {
        mTextMsg.setText(mContext.getString(R.string.tip_download_contact));
        mTvDownloadCount.setVisibility(View.VISIBLE);
        mBtnDownload.setVisibility(View.GONE);
        mPbDownload.setVisibility(View.VISIBLE);
        String text = currentSize + "/" + totalSize;
        mTvDownloadCount.setText(text);
        mPbDownload.setMax(totalSize);
        LogUtils.log(TAG, "showContactDownloadProgress_setMax(totalSize): " + totalSize);
        mPbDownload.setProgress(currentSize);
        LogUtils.log(TAG, "showContactDownloadProgress_setProgress(currentSize): " + currentSize);
        LogUtils.log(TAG, "showContactDownloadProgress_setText_tip_download_record ");
        startSyncAnimator();
    }
    /**
     * display records download process.
     *
     * @param currentSize currentSize
     * @param totalSize   totalSize
     */
    public void showRecordDownloadProgress(int currentSize, int totalSize) {
        mTextMsg.setText(mContext.getString(R.string.tip_download_record));
        mTvDownloadCount.setVisibility(View.INVISIBLE);
        mBtnDownload.setVisibility(View.GONE);
        mPbDownload.setVisibility(View.VISIBLE);
        mPbDownload.setMax(totalSize);
        LogUtils.log(TAG, "showRecordDownloadProgress_setMax(totalSize): " + totalSize);
        mPbDownload.setProgress(currentSize);
        LogUtils.log(TAG, "showRecordDownloadProgress_setProgress(currentSize): " + currentSize);
        LogUtils.log(TAG, "showRecordDownloadProgress_setText_tip_download_record ");
        startSyncAnimator();
    }
    /**
     * display loading data message.
     *
     * @param isLoading isLoading
     */
    public void showLoadingDataMsg(int isLoading) {
        if (isLoading == BtConstant.CONTACT_LOADING) {
            mLoadingLayout.setVisibility(View.VISIBLE);
            LogUtils.log(TAG, "showLoadingDataMsg isLoading == BtConstant.CONTACT_LOADING");
            mBtcallLoadingIm.startAnimation(mRotateAnimation);
        } else {
            mLoadingLayout.setVisibility(View.GONE);
            LogUtils.log(TAG, "showLoadingDataMsg ELSE");
            mBtcallLoadingIm.clearAnimation();
        }
        mTextMsg.setVisibility(View.GONE);
        mBtnDownload.setVisibility(View.GONE);
        mPbDownload.setVisibility(View.GONE);
        mTvDownloadCount.setVisibility(View.GONE);
    }
    /**
     * remove pop.
     */
    public void removePop() {
        if (mPopupManager != null) {
            mPopupManager.hideProPopup();
            mPopupManager = null;
            LogUtils.log(TAG, "removePop data.");
        }
    }
    /**
     * start animation about sync contacts and records.
     */
    public void startSyncAnimator() {
        if (mRotateAnimator == null) {
            mSyncWheelView.setVisibility(View.VISIBLE);
            mSyncWheelView.setBackgroundResource(R.anim.sync_wheel_animator);
            mRotateAnimator = (AnimationDrawable) mSyncWheelView.getBackground();
        }
        if (!mRotateAnimator.isRunning()) {
            mRotateAnimator.start();
        }
    }
    /**
     * cancel animation about sync contacts and records.
     */
    public void stopSyncAnimator() {
        if (mRotateAnimator != null) {
            mRotateAnimator.stop();
            mRotateAnimator = null;
        }
        mSyncWheelView.setVisibility(View.GONE);
    }
    /**
     * set download view visibility.
     */
    public void setDownloadProgressBarShow() {
        mPbDownload.setVisibility(View.GONE);
        mTvDownloadCount.setVisibility(View.GONE);
    }
    /**
     * set download progress view visibility.
     *
     * @param isDownload isDownload
     */
    public void setDownload(boolean isDownload) {
        if (isDownload) {
            mBtnDownload.setVisibility(View.GONE);
            mTvDownloadCount.setVisibility(View.VISIBLE);
            mPbDownload.setVisibility(View.VISIBLE);
        } else {
            mTextMsg.setVisibility(View.VISIBLE);
            mTvDownloadCount.setVisibility(View.GONE);
            mPbDownload.setVisibility(View.GONE);
        }
    }
    /**
     * notice user about no data.
     *
     * @param text text
     */
    public void showNoDataTip(String text) {
        mTextTip.setText(text);
    }
    /**
     * set no contacts tip visibility.
     *
     * @param visible visible
     */
    public void setNoContactsTipShow(int visible) {
        mTextTip.setVisibility(visible);
    }
    /**
     * tip about no records and no result.
     *
     * @param list  list
     * @param index index
     */
    private void showNotice(List<PhonebookItem> list, int index) {
        if (mBtnDownload.getVisibility() == View.VISIBLE) {
            return;
        }
        if (list != null && mContext != null && !list.isEmpty()) {
            if (list.get(0).getFirstType().equals((mContext.getResources().getString(R.string.use)
            ))) {
                LogUtils.log(TAG, "has use contacts");
                mBtnUse.setSelected(true);
                mBtnUse.setEnabled(true);
            } else {
                mBtnUse.setEnabled(false);
                LogUtils.log(TAG, "no use contacts");
            }
        }
        switch (index) {
            case BtConstant.TYPE_CONTACTS:
                LogUtils.log(TAG, "showNotice caseTYPE_CONTACTS:");
                boolean isContactEmpty = list == null || list.size() == BtConstant.LIST_SIZE_ZERO;
                if (isContactEmpty && mLoadState == false) {
                    mTextTipContacts.setVisibility(View.VISIBLE);
                    mTextTipContacts.setText(mContext.getString(R.string.tip_no_contacts));
                    LogUtils.log(TAG, "showNotice caseTYPE_CONTACTS: isContactEmpty");
                } else {
                    mTextTipContacts.setVisibility(View.GONE);
                }
                setNoContactsTipShow(View.GONE);
                break;
            case BtConstant.TYPE_SEARCH:
                boolean isRecordEmpty = list == null || list.size() == BtConstant.LIST_SIZE_ONE;
                if (isRecordEmpty) {
                    setNoContactsTipShow(View.VISIBLE);
                    showNoDataTip(mContext.getResources().getString(R.string.tip_no_result));
                    LogUtils.log(TAG, "showNotice caseTYPE_SEARCH: if");
                } else {
                    setNoContactsTipShow(View.GONE);
                    LogUtils.log(TAG, "showNotice caseTYPE_SEARCH: else");
                }
                break;
            default:
                LogUtils.log(TAG, "showNotice default: ");
                boolean isEmptyList = list == null || list.size() == BtConstant.LIST_SIZE_ONE;
                //boolean shownPbDownload = mPbDownload.isShown();
                //boolean shownBtnDownload = mBtnDownload.isShown();
                if ((mLoadState == false) && isEmptyList) {
                    setNoContactsTipShow(View.VISIBLE);
                    showNoDataTip(mContext.getResources().getString(R.string.tip_no_records));
                    LogUtils.log(TAG, "tip_no_records: ");
                } else {
                    LogUtils.log(TAG, "ELSE View.GONE");
                    setNoContactsTipShow(View.GONE);
                }
                mTextTipContacts.setVisibility(View.GONE);
                break;
        }
    }
    /**
     * set main layout background resource.
     *
     * @param id id
     */
    private void setMainBackground(int id) {
        if (mLayoutMain != null) {
            mLayoutMain.setBackgroundResource(id);
        }
    }
    /**
     * update fragment list data.
     *
     * @param msg msg
     */
    private void updateListData(Message msg) {
        if (msg.arg2 != BtConstant.TYPE_CONTACTS) {
            mKeyboardFragment.updateDialData(mRecordData, msg.arg2);
            showNotice(mRecordData, BtConstant.TYPE_KEYBORD);
            setSelectBackground(R.id.ll_keyboard);
        } else {
            if (mPhonebookAllFragment != null) {
                if (msg.obj != null) {
                    LogUtils.log(TAG, "updateListData setDataUpdate " + msg.arg2);
                    mPhonebookAllFragment.setDataUpdate(mPhoneBookData, msg.arg2, (int) msg.obj);
                    showNotice(mPhoneBookData,BtConstant.TYPE_CONTACTS);
                } else {
                    LogUtils.log(TAG, "updateListData setDataUpdate else" + msg.arg2);
                    mPhonebookAllFragment.setDataUpdate(mPhoneBookData, msg.arg2, BtConstant.RECORD_FLAG_VALUE);
                    showNotice(mPhoneBookData,BtConstant.TYPE_CONTACTS);
                }
                showNotice(mPhoneBookData, BtConstant.TYPE_CONTACTS);
                setSelectBackground(R.id.ll_contacts);
            }
        }
    }
    /**
     * enable or disable Use button based on data.
     */
    private void checkBtnEnableState() {
        boolean isEnable = mPhoneBookData == null || mPhoneBookData.isEmpty();
        if (isEnable) {
            mBtnUse.setEnabled(false);
            mBtnUse.setAlpha(BtConstant.CONTACTS_PHOTO_SHADER_ALPHA);
        } else {
            if (mContext != null) {
                if (mPhoneBookData.get(0).getFirstType().equals((mContext.getResources().getString(R.string.use)))) {
                    LogUtils.log(TAG, "has use contacts");
                    mBtnUse.setEnabled(true);
                    mBtnUse.setSelected(true);
                } else {
                    LogUtils.log(TAG, "no use contacts");
                    mBtnUse.setEnabled(false);
                }
                mBtnUse.setAlpha(BtConstant.NORMAL_ALPHA);
            }
        }
    }
    /**
     * display sync data current progress.
     *
     * @param message message
     */
    private void showSyncDownloadProgress(Message message) {
        if (mKeyboardFragment == null) {
            return;
        }
        if (message.what == MSG_REFRESH_SYNC_RECORDS) {
            showRecordDownloadProgress(message.arg1, message.arg2);
        } else if (message.what == MSG_REFRESH_SYNC_CONTACTS) {
            showContactDownloadProgress(message.arg1, message.arg2);
        }
    }
    /**
     * display load data message.
     *
     * @param isLoading isLoading
     */
    private void showLoadDataMsg(int isLoading) {
        showLoadingDataMsg(isLoading);
    }
    /**
     * download Phone book.
     */
    public void downloadPb() {
        Message msg = Message.obtain();
        msg.what = BtActionDefine.ACTION_VIEW_NOTIFY_DOWNLOAD;
        BtCallMainView.this.notify(msg, FLAG_RUN_SYNC);
    }
    /**
     * request dial call.
     *
     * @param number number
     */
    private void reqDialCall(String number) {
        Message msg = Message.obtain();
        msg.what = BtActionDefine.ACTION_VIEW_NOTIFY_DIAL_CALL;
        Bundle bundle = new Bundle();
        bundle.putString(BtConstant.NUMBER, number);
        msg.setData(bundle);
        BtCallMainView.this.notify(msg, FLAG_RUN_SYNC);
    }
    /**
     * request contacts by number.
     *
     * @param number number.
     */
    private void reqSearchContactsByNumber(String number) {
        LogUtils.log(TAG, "reqSearchContactsByNumber number" + number);
        if (!TextUtils.isEmpty(number)) {
            if (!mFlagReq) {
                reqSearchContacts(number);
                LogUtils.log(TAG, "reqSearchContactsByNumber true" + number);
            } else {
                mReqKey = number;
                LogUtils.log(TAG, "reqSearchContactsByNumber false" + mReqKey);
            }
        } else {
            mUpdateRecords = true;
            mRefreshViewHandler.sendEmptyMessage(MSG_REFRESH_SHOW_DATA);
            mReqKey = "";
            LogUtils.log(TAG, "reqSearchContactsByNumber else");
        }
    }
    /**
     * request contacts.
     *
     * @param number number
     */
    private void reqSearchContacts(String number) {
        Message msg = Message.obtain();
        msg.what = BtActionDefine.ACTION_VIEW_NOTIFY_NUMBER_SEARCH;
        Bundle data = new Bundle();
        data.putString(BtConstant.NUMBER_KEY, number);
        msg.setData(data);
        BtCallMainView.this.notify(msg, FLAG_RUN_SYNC);
        mFlagReq = true;
    }
    /**
     * get current edittext.
     */
    private String getCurrentEditText() {
        if (mKeyboardFragment != null) {
            return mKeyboardFragment.getEditInput();
        } else {
            return null;
        }
    }
    /**
     * show tip about no contact.
     *
     * @param visible visible
     */
    private void setNoContactsTipVisible(int visible) {
        setNoContactsTipShow(visible);
    }
    /**
     * toast notify user download limit.
     */
    public void toastDownloadWarning(int type) {
        Toast toast = new Toast(mContext);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        RelativeLayout toastLayout = (RelativeLayout) inflater.inflate(R.layout.toast_view, null);
        TextView txtToast = (TextView) toastLayout.findViewById(R.id.text_toast);
        if (type == BtActionDefine.ACTION_DOWNLOAD_FAIL_WARNING) {
            txtToast.setText(mContext.getResources().getString(R.string.tip_download_fail));
            initData(false);
        } else {
            txtToast.setText(mContext.getResources().getString(R.string.tip_download_waring));
        }
        toast.setView(toastLayout);
        toast.show();
        LogUtils.log(TAG, "toastDownloadWarning end");
    }
    @Override
    public void onClickDialCall(String number, String name) {
        LogUtils.log(TAG, "onClickDialCall number:" + number);
        reqDialCall(number);
    }
    @Override
    public void onLocatePosition(String number, int position) {
        Message msg = Message.obtain();
        msg.what = BtActionDefine.ACTION_LIST_LEFT_VISIBLE_GET;
        Bundle data = new Bundle();
        data.putString(BtConstant.NUMBER, number);
        data.putInt(BtConstant.POSITION, position);
        msg.setData(data);
        BtCallMainView.this.notify(msg, FLAG_RUN_SYNC);
    }
    @Override
    public void onClickDownload() {
        LogUtils.log(TAG, "onClickDownload downloadPb");
        downloadPb();
    }
    @Override
    public void onSearchContactsByNumber(String key) {
        if (mContext == null) {
            LogUtils.log(TAG, "onSearchContactsByNumber mContext null");
            return;
        }
        if (mLoadState == true) {
            LogUtils.log(TAG, "onSearchContactsByNumber return");
            return;
        }
        reqSearchContactsByNumber(key);
        LogUtils.log(TAG, "onSearchContactsByNumber reqSearchContactsByNumber(key)");
    }
    @Override
    public void onSearchByNumber(String key) {
    }
    @Override
    public void onClickDeleteBtn() {
        notifyDataMiningClickEvent(BtActionDefine.DATAMINING_ACTION_KEYBOARD_DELETE_CLICK);
    }
    @Override
    public void onContactsViewShow() {
        notifyDataMiningClickEvent(BtActionDefine.DATAMINING_ACTION_PHONEBOOK_BEGIN);
    }
    @Override
    public void onContactsViewHide() {
        notifyDataMiningClickEvent(BtActionDefine.DATAMINING_ACTION_PHONEBOOK_END);
    }
    @Override
    public void onLetterPositionChange(int index) {
        Message msg = Message.obtain();
        msg.what = BtActionDefine.ACTION_NOTIFY_LETTER_CHANGE;
        msg.arg1 = index;
        LogUtils.log(TAG, "onLetterPositionChange index: " + index);
        if (isShownTabKeyboard()) {
            msg.arg2 = BtConstant.TYPE_KEYBORD;
        } else {
            msg.arg2 = BtConstant.TYPE_CONTACTS;
        }
        BtCallMainView.this.notify(msg, FLAG_RUN_SYNC);
    }
    @Override
    public void onTouchingLetterChanged(String letter) {
        Message msgLetter = Message.obtain();
        msgLetter.what = BtActionDefine.ACTION_VIEW_NOTIFY_LETTER;
        Bundle data = new Bundle();
        data.putString(BtConstant.LETTER, letter);
        msgLetter.setData(data);
        BtCallMainView.this.notify(msgLetter, FLAG_RUN_SYNC);
    }
    @Override
    public void onTouchLetterStop(String str) {
        if (mPhonebookAllFragment != null) {
            mPhonebookAllFragment.setRecycleViewScrollMode(false);
            mPhonebookAllFragment.stopWheelAnimator();
        }
    }
}
