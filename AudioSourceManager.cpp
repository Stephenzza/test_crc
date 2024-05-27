/**
 * Copyright (c) 2019
 * All Rights Reserved by Thunder Software Technology Co., Ltd and its affiliates.
 * You may not use, copy, distribute, modify, transmit in any form this file
 * except in compliance with THUNDERSOFT in writing by applicable law.
 */
#define LOG_TAG "AudioSourceManager"
/**#define LOG_NDEBUG 0*/
#include <binder/IPCThreadState.h>
#include <cutils/properties.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sqlite3.h>
#include <tinyxml2.h>
#include <vector>
#include <string>
#include "AudioSourceManager.h"
namespace android {
using tinyxml2::XMLDocument;
using tinyxml2::XML_SUCCESS;
using tinyxml2::XMLElement;
#ifdef LOG_NDEBUG
#define AswAssert(expr) do { if (!(expr)) { \
             ALOGE("assert fail: %s at %s:%d\n", \
             #expr, __FUNCTION__, __LINE__); }} while (0)
#else
#define AswAssert(expr)
#endif
#define ASM_TABLENAME_AUDIOSOURCE "audiosource"
#define ASM_TABLENAME_AUDIOSDATA "audiodata"
#define ASM_AUDIOSOURCE_COL_ID "id"
#define ASM_AUDIOSOURCE_COL_ATTR_USAGE "attr_usage"
#define ASM_AUDIOSOURCE_COL_ATTR_AUDIO_USAGE "attr_audio_usage"
#define ASM_AUDIOSOURCE_COL_ATTR_CONTEXT_TYPE "attr_context_type"
#define ASM_AUDIOSOURCE_COL_ATTR_SOURCE "attr_source"
#define ASM_AUDIOSOURCE_COL_ATTR_FLAGS "attr_flags"
#define ASM_AUDIOSOURCE_COL_DISPLAY_ID "display_id"
#define ASM_AUDIOSOURCE_COL_CLIENT_ID "client_id"
#define ASM_AUDIOSOURCE_COL_CLIENT_UID "client_uid"
#define ASM_AUDIOSOURCE_COL_PACKAGE_NAME "package_name"
#define ASM_AUDIOSOURCE_COL_GAIN_REQUEST "gain_request"
#define ASM_AUDIOSOURCE_COL_LOSS_RECEIVED "loss_received"
#define ASM_AUDIOSOURCE_COL_SOURCE_FLAGS "flags"
#define ASM_AUDIOSOURCE_COL_SDK_TARGET "sdk_target"
#define ASM_AUDIOSOURCE_COL_GEN_COUNT "gen_count"
#define ASM_AUDIOSOURCE_COL_CREATE_TIME "create_time"
#define ASM_AUDIOSOURCE_COL_UPDATE_TIME "update_time"
#define ASM_AUDIODATA_COL_ID "id"
#define ASM_AUDIODATA_COL_DATA_ID "audiodata_id"
#define ASM_AUDIODATA_COL_DATA_VALUE "audiodata_value"
/**
 * ---------------------------------------------------------
 * audiosource sql
 * ---------------------------------------------------------
*/
#define ASM_SQL_CREATE_AUDIOSOURCE_TABLE "CREATE TABLE " \
                ASM_TABLENAME_AUDIOSOURCE "(" \
                ASM_AUDIOSOURCE_COL_ID " INTEGER PRIMARY KEY AUTOINCREMENT, " \
                ASM_AUDIOSOURCE_COL_ATTR_USAGE " INTEGER, " \
                ASM_AUDIOSOURCE_COL_ATTR_AUDIO_USAGE " INTEGER, " \
                ASM_AUDIOSOURCE_COL_ATTR_CONTEXT_TYPE " INTEGER, " \
                ASM_AUDIOSOURCE_COL_ATTR_SOURCE " INTEGER, " \
                ASM_AUDIOSOURCE_COL_ATTR_FLAGS " INTEGER, " \
                ASM_AUDIOSOURCE_COL_DISPLAY_ID " INTEGER," \
                ASM_AUDIOSOURCE_COL_CLIENT_ID " TEXT," \
                ASM_AUDIOSOURCE_COL_CLIENT_UID " INTEGER," \
                ASM_AUDIOSOURCE_COL_PACKAGE_NAME " TEXT," \
                ASM_AUDIOSOURCE_COL_GAIN_REQUEST " INTEGER," \
                ASM_AUDIOSOURCE_COL_LOSS_RECEIVED " INTEGER," \
                ASM_AUDIOSOURCE_COL_SOURCE_FLAGS " INTEGER, " \
                ASM_AUDIOSOURCE_COL_SDK_TARGET " INTEGER, " \
                ASM_AUDIOSOURCE_COL_GEN_COUNT " INTEGER, " \
                ASM_AUDIOSOURCE_COL_CREATE_TIME " TEXT, " \
                ASM_AUDIOSOURCE_COL_UPDATE_TIME " TEXT " \
                ");"
#define ASM_SQL_CHECK_AUDIO_SOURCE_TABLE "SELECT COUNT(*) " \
                " FROM " ASM_TABLENAME_AUDIOSOURCE ";"
#define ASM_SQL_SELECT_AUDIO_SOURCE "SELECT * FROM " \
                ASM_TABLENAME_AUDIOSOURCE \
                " WHERE " ASM_AUDIOSOURCE_COL_ID " = ?;"
#define ASM_SQL_AUDIO_SOURCE_CNT "SELECT COUNT(*) FROM " \
               ASM_TABLENAME_AUDIOSOURCE \
                " WHERE " ASM_AUDIOSOURCE_COL_ID " = ?;"
#define ASM_SQL_INSERT_AUDIO_SOURCE_TABLE "INSERT INTO " \
                ASM_TABLENAME_AUDIOSOURCE "( " \
                ASM_AUDIOSOURCE_COL_ID "," \
                ASM_AUDIOSOURCE_COL_ATTR_USAGE "," \
                ASM_AUDIOSOURCE_COL_ATTR_AUDIO_USAGE "," \
                ASM_AUDIOSOURCE_COL_ATTR_CONTEXT_TYPE "," \
                ASM_AUDIOSOURCE_COL_ATTR_SOURCE "," \
                ASM_AUDIOSOURCE_COL_ATTR_FLAGS "," \
                ASM_AUDIOSOURCE_COL_DISPLAY_ID "," \
                ASM_AUDIOSOURCE_COL_CLIENT_ID "," \
                ASM_AUDIOSOURCE_COL_CLIENT_UID "," \
                ASM_AUDIOSOURCE_COL_PACKAGE_NAME "," \
                ASM_AUDIOSOURCE_COL_GAIN_REQUEST "," \
                ASM_AUDIOSOURCE_COL_LOSS_RECEIVED "," \
                ASM_AUDIOSOURCE_COL_SOURCE_FLAGS "," \
                ASM_AUDIOSOURCE_COL_SDK_TARGET "," \
                ASM_AUDIOSOURCE_COL_GEN_COUNT "," \
                ASM_AUDIOSOURCE_COL_CREATE_TIME "," \
                ASM_AUDIOSOURCE_COL_UPDATE_TIME ") VALUES ( " \
                " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," \
                " ?, ?, ?, ?, ? );"
#define ASM_SQL_UPDATE_AUDIO_SOURCE_TABLE "UPDATE " \
                ASM_TABLENAME_AUDIOSOURCE " SET " \
                ASM_AUDIOSOURCE_COL_ID " = ?," \
                ASM_AUDIOSOURCE_COL_ATTR_USAGE " = ?," \
                ASM_AUDIOSOURCE_COL_ATTR_AUDIO_USAGE " = ?," \
                ASM_AUDIOSOURCE_COL_ATTR_CONTEXT_TYPE " = ?," \
                ASM_AUDIOSOURCE_COL_ATTR_SOURCE " = ?," \
                ASM_AUDIOSOURCE_COL_ATTR_FLAGS " = ?," \
                ASM_AUDIOSOURCE_COL_DISPLAY_ID " = ?," \
                ASM_AUDIOSOURCE_COL_CLIENT_ID " = ?," \
                ASM_AUDIOSOURCE_COL_CLIENT_UID " = ?," \
                ASM_AUDIOSOURCE_COL_PACKAGE_NAME " = ?," \
                ASM_AUDIOSOURCE_COL_GAIN_REQUEST " = ?," \
                ASM_AUDIOSOURCE_COL_LOSS_RECEIVED " = ?," \
                ASM_AUDIOSOURCE_COL_SOURCE_FLAGS " = ?," \
                ASM_AUDIOSOURCE_COL_SDK_TARGET " = ?," \
                ASM_AUDIOSOURCE_COL_GEN_COUNT " = ?," \
                ASM_AUDIOSOURCE_COL_CREATE_TIME " = ?," \
                ASM_AUDIOSOURCE_COL_UPDATE_TIME " = ?" \
                " WHERE " ASM_AUDIOSOURCE_COL_ID " = ?;"
#define ASM_SQL_DROP_AUDIO_SOURCE_TABLE "DROP TABLE " \
                ASM_TABLENAME_AUDIOSOURCE \
                ";"
#define ASM_SQL_DELETE_AUDIO_SOURCE "DELETE FROM " \
                ASM_TABLENAME_AUDIOSOURCE ";"
/**
 * ---------------------------------------------------------
 * audiodata  sql
 * ---------------------------------------------------------
*/
#define ASM_SQL_CREATE_AUDIODATA_TABLE "CREATE TABLE " \
                ASM_TABLENAME_AUDIOSDATA " (" \
                ASM_AUDIODATA_COL_ID " INTEGER PRIMARY KEY AUTOINCREMENT, " \
                ASM_AUDIODATA_COL_DATA_ID " INTEGER, " \
                ASM_AUDIODATA_COL_DATA_VALUE " NONE " \
                ");"
#define ASM_SQL_CHECK_AUDIO_DATA_TABLE "SELECT COUNT(*) " \
                " FROM " ASM_TABLENAME_AUDIOSDATA ";"
#define  ASM_SQL_SELECT_AUDIO_DATA "SELECT * FROM " \
                ASM_TABLENAME_AUDIOSDATA \
                " WHERE " ASM_AUDIODATA_COL_DATA_ID " = ?;"
#define ASM_SQL_AUDIO_DATA_CNT "SELECT COUNT(*) FROM " \
                ASM_TABLENAME_AUDIOSDATA \
                " WHERE " ASM_AUDIODATA_COL_DATA_ID " = ?;"
#define ASM_SQL_INSERT_AUDIO_DATA_TABLE "INSERT INTO " \
                ASM_TABLENAME_AUDIOSDATA "( " \
                ASM_AUDIODATA_COL_DATA_ID "," \
                ASM_AUDIODATA_COL_DATA_VALUE " ) VALUES ( " \
                " ?, ?" ");"
#define ASM_SQL_UPDATE_AUDIO_DATA_TABLE "UPDATE " \
                ASM_TABLENAME_AUDIOSDATA " SET " \
                ASM_AUDIODATA_COL_DATA_ID " = ?," \
                ASM_AUDIODATA_COL_DATA_VALUE " = ?" \
                " WHERE " ASM_AUDIODATA_COL_DATA_ID " = ?;"
#define ASM_SQL_DROP_AUDIO_DATA_TABLE "DROP TABLE " \
                ASM_TABLENAME_AUDIOSDATA \
                ";"
#define ASM_SQL_DELETE_AUDIO_DATA "DELETE FROM " \
                ASM_TABLENAME_AUDIOSDATA ";"
/**
 * ---------------------------------------------------------
 * sql  end
 * ---------------------------------------------------------
*/
const char* kDefaultAudioSourceDb = "/data/asm/audiosource.db";
static const int AUDIO_SOURCE_NONE = -1;
static const int AUDIO_SOURCE_ALL = -1;
static const int AUDIO_SOURCE_FLAG_NONE = -1;
static const int DISPLAY_MAIN = 0;
static const int DISPLAY_ASSIST = 1;
static const int DSP_MODE_MEDIA = 0;
static const int DSP_MODE_BTPHONE = 1;
String8 needRestorePkg = String8("");
Mutex mAudioSourcesLock;
Mutex mAudioDataDbLock;
static bool getProp(const char* prop) {
    char value[PROPERTY_VALUE_MAX] = { '\0' };
    return (property_get(prop, value, "0") > 0) && (atoi(value) == 1);
}
static sqlite3* mSqlite = NULL;
static bool mDbOpen = false;
static const int kStackIndexStart = 1;
static int mStackIndex = kStackIndexStart;
static bool isAsmBoot = getProp("sys.asm.boot");
AudioSourceManager::AudioSourceManager() {
    mAudioSourceDb = new KeyAudioSourceDb();
    mFocusInfoDb = new KeyFocusInfoDb();
    mAudioDataDb = new KeyAudioDataDb();
    ALOGD("Create, isAsmBoot: %d", isAsmBoot ? 1 : 0);
    char value[PROPERTY_VALUE_MAX] = { '\0' };
    property_get("sys.asm.boot", value, "-1");
    bool isBootStart = (atoi(value) == -1);
    if (isBootStart) {
        ALOGD("Restore audio source state.");
        property_set("sys.asm.boot", "0");
    } else {
        char value[PROPERTY_VALUE_MAX] = { '\0' };
        property_get("sys.asm.stack_index", value, "1");
        ALOGD("Service died or start, get stack index from property: %s",
                value);
        mStackIndex = atoi(value);
    }
}
void AudioSourceManager::onFirstRef() {
}
AudioSourceManager::~AudioSourceManager() {
    if (mDbOpen) {
        closeAudioSourceDb();
        ALOGD("~ %s mDbOpen = false", __FUNCTION__);
        mDbOpen = false;
    }
    if (mAudioSourceDb != NULL) {
        mAudioSourceDb->clear();
        delete mAudioSourceDb;
        mAudioSourceDb = NULL;
    }
    if (mAudioDataDb != NULL) {
        mAudioDataDb->clear();
        delete mAudioDataDb;
        mAudioDataDb = NULL;
    }
    if (mFocusInfoDb != NULL) {
        mFocusInfoDb->clear();
        delete mFocusInfoDb;
        mFocusInfoDb = NULL;
    }
}
int AudioSourceManager::closeAudioSourceDb() {
    int sqlResult = SQLITE_OK;
    int ret = NO_ERROR;
    if (mSqlite != NULL) {
        sqlResult = sqlite3_close(mSqlite);
        if (sqlResult != SQLITE_OK) {
            ret = ASM_SQLITE_ERROR;
            ALOGW("Cant close sqlite err:%d", sqlResult);
        }
        mSqlite = NULL;
    }
    mDbOpen = false;
    return ret;
}
void AudioSourceManager::removeAudioSourceDb() {
    int ret = 0;
    ret = remove(kDefaultAudioSourceDb);
    if (ret != 0) {
        ALOGE("audiosource: remove audio source db failed");
        return;
    }
    mSqlite = NULL;
    mDbOpen = false;
    ALOGD("~ %s mDbOpen = false", __FUNCTION__);
}
/**
 * ---------------------------------------------------------
 * audiosource  function
 * ---------------------------------------------------------
*/
int AudioSourceManager::getAudioSourceTableCnt(int dataid) {
    if (!mSqlite)
        return UNKNOWN_ERROR;
    int sqlResult = SQLITE_OK;
    sqlite3_stmt *stmt = NULL;
    int updateCnt = 0;
    String8 sql(ASM_SQL_AUDIO_SOURCE_CNT);
    sqlResult = sqlite3_prepare_v2(mSqlite, sql.string(), strlen(sql.string()),
            &stmt, NULL);
    if (sqlResult == SQLITE_OK) {
        int bindIdx = 1;
        sqlite3_reset(stmt);
        sqlite3_bind_int(stmt, bindIdx++, dataid);
        sqlResult = sqlite3_step(stmt);
        if (sqlResult == SQLITE_ROW) {
            updateCnt = (unsigned int) sqlite3_column_int(stmt, 0);
        } else {
            updateCnt = 0;
        }
        sqlite3_finalize(stmt);
        stmt = NULL;
    }
    return updateCnt;
}
int AudioSourceManager::updateAudioSourceTable(sp<AudioSourceDb>
        mAudioSourceDb) {
    if (!mSqlite)
        return UNKNOWN_ERROR;
    AswAssert(mAudioSourceDb.get());
    int ret = NO_ERROR;
    int sqlResult = SQLITE_OK;
    sqlite3_stmt *stmt = NULL;
    int updateCnt = getAudioSourceTableCnt(mAudioSourceDb->dataid);
    if (updateCnt > 0) {
        String8 sql(ASM_SQL_UPDATE_AUDIO_SOURCE_TABLE);
        sqlResult = sqlite3_prepare_v2(mSqlite,
                sql.string(), strlen(ASM_SQL_UPDATE_AUDIO_SOURCE_TABLE),
                &stmt, NULL);
    } else {
        String8 sql(ASM_SQL_INSERT_AUDIO_SOURCE_TABLE);
        sqlResult = sqlite3_prepare_v2(mSqlite,
                sql.string(), strlen(ASM_SQL_INSERT_AUDIO_SOURCE_TABLE),
                &stmt, NULL);
    }
    if (sqlResult == SQLITE_OK) {
        sqlite3_reset(stmt);
        int bindIdx = 1;
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->dataid);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->attr_usage);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->attr_audio_usage);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->attr_context_type);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->attr_source);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->attr_flags);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->display_id);
        sqlite3_bind_text(stmt, bindIdx++, mAudioSourceDb->client_id,
                (mAudioSourceDb->client_id).length(), SQLITE_TRANSIENT);
        sqlite3_bind_int(stmt, bindIdx++ , mAudioSourceDb->client_uid);
        sqlite3_bind_text(stmt, bindIdx++, mAudioSourceDb->package_name,
                (mAudioSourceDb->package_name).length(), SQLITE_TRANSIENT);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->gain_request);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->loss_received);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->flags);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->sdk_target);
        sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->gen_count);
        sqlite3_bind_text(stmt, bindIdx++, mAudioSourceDb->create_time,
                (mAudioSourceDb->create_time).length(), SQLITE_TRANSIENT);
        sqlite3_bind_text(stmt, bindIdx++, mAudioSourceDb->update_time,
                (mAudioSourceDb->update_time).length(), SQLITE_TRANSIENT);
        if (updateCnt > 0) {
            sqlite3_bind_int(stmt, bindIdx++, mAudioSourceDb->dataid);
        }
        sqlResult = sqlite3_step(stmt);
        if (sqlResult != SQLITE_DONE) {
            ALOGE("Sql step error:%d", sqlResult);
        }
        sqlite3_finalize(stmt);
        stmt = NULL;
    } else {
        ALOGE("Sql prepare error:%d", sqlResult);
        ret = ASM_SQLITE_ERROR;
    }
    return ret;
}
int AudioSourceManager::getAudioSourceTable(int audioDataId) {
    int ret = NO_ERROR;
    int sqlResult = SQLITE_OK;
    int dataId = -1;
    sqlite3_stmt *stmt = NULL;
    String8 sql(ASM_SQL_SELECT_AUDIO_SOURCE);
    sqlResult = sqlite3_prepare_v2(mSqlite, sql.string(),
            sql.length(), &stmt, NULL);
    if (sqlResult == SQLITE_OK) {
        sqlite3_reset(stmt);
        int bindIdx = 1;
        sqlite3_bind_int(stmt, bindIdx++, audioDataId);
        while (sqlite3_step(stmt) == SQLITE_ROW) {
            int rowIdx = 0;
            dataId = sqlite3_column_int(stmt, rowIdx);
            if (audioDataId == dataId) {
                sp<AudioSourceDb> mmAudioSourceDb = new AudioSourceDb();
                mmAudioSourceDb->dataid = sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->attr_usage =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->attr_audio_usage =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->attr_context_type =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->attr_source =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->attr_flags =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->display_id =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->client_id = convertString((const char*)
                        sqlite3_column_text(stmt, rowIdx++));
                mmAudioSourceDb->client_uid =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->package_name =
                        convertString((const char*)
                        sqlite3_column_text(stmt, rowIdx++));
                mmAudioSourceDb->gain_request =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->loss_received =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->flags = sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->sdk_target =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->gen_count =
                        sqlite3_column_int(stmt, rowIdx++);
                mmAudioSourceDb->create_time =
                        convertString((const char*)
                        sqlite3_column_text(stmt, rowIdx++));
                mmAudioSourceDb->update_time = convertString((const char*)
                        sqlite3_column_text(stmt, rowIdx++));
                mAudioSourceDb->add(mmAudioSourceDb);
            }
        }
        sqlite3_finalize(stmt);
        stmt = NULL;
    } else {
        ALOGE("Sql prepare error:%d", sqlResult);
        ret = ASM_SQLITE_ERROR;
    }
    return ret;
}
int AudioSourceManager::checkAudioSourceTable() {
    if (!mSqlite)
        return UNKNOWN_ERROR;
    int ret = 0;
    int sqlResult = NO_ERROR;
    sqlite3_stmt *stmt = NULL;
    unsigned int tableCnt = 0;
    String8 sql(ASM_SQL_CHECK_AUDIO_SOURCE_TABLE);
    sqlResult = sqlite3_prepare_v2(mSqlite, sql.string(), strlen(sql.string()),
            &stmt, NULL);
    if (sqlResult == SQLITE_OK) {
        sqlite3_reset(stmt);
        sqlResult = sqlite3_step(stmt);
        if (sqlResult == SQLITE_ROW) {
            tableCnt = (unsigned int) sqlite3_column_int(stmt, 0);
        } else {
            ALOGE("Sql step error:%d", sqlResult);
        }
        sqlite3_finalize(stmt);
        stmt = NULL;
    } else {
        ALOGE("Sql prepare error:%d", sqlResult);
        ret = ASM_SQLITE_ERROR;
    }
    if (tableCnt > 0) {
        ret = tableCnt;
    }
    return ret;
}
int AudioSourceManager::createAudioSourceTable() {
    if (!mSqlite)
        return UNKNOWN_ERROR;
    int ret = ASM_SQLITE_ERROR;
    int sqlResult = SQLITE_OK;
    char *strErr = NULL;
    String8 sqlDrop(ASM_SQL_DROP_AUDIO_SOURCE_TABLE);
    if ((sqlResult = sqlite3_exec(mSqlite, sqlDrop.string(),
            0, 0, &strErr)) != SQLITE_OK) {
        ALOGW("Cant drop sqlite db table  err:%d", sqlResult);
    }
    String8 sql(ASM_SQL_CREATE_AUDIOSOURCE_TABLE);
    if ((sqlResult = sqlite3_exec(mSqlite, sql.string(),
            0, 0, &strErr)) != SQLITE_OK) {
        ret = ASM_SQLITE_ERROR;
        ALOGE("Cant create sqlite db table err:%d", sqlResult);
    } else {
        ret = NO_ERROR;
    }
    if (strErr != NULL) {
        sqlite3_free(strErr);
        strErr = NULL;
    }
    return ret;
}
int AudioSourceManager::openOrCreateAudioSourceDb() {
    int ret = NO_ERROR;
    int sqlResult = SQLITE_OK;
    ALOGV("openOrCreateAudioSourceDb mDbOpen%d", mDbOpen);
    sqlResult = sqlite3_open_v2(kDefaultAudioSourceDb, &mSqlite,
            (SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE), NULL);
    if (sqlResult != SQLITE_OK) {
        ALOGE("Cant open sqlite db:%s, err:%d", kDefaultAudioSourceDb,
                sqlResult);
        ret = ASM_SQLITE_ERROR;
        mSqlite = NULL;
        return ret;
    } else {
        ALOGV("suceess to open audio source db, mDbOpen:%d", mDbOpen);
        if (checkAudioSourceTable() <= 0) {
            createAudioSourceTable();
        }
        mDbOpen = true;
    }
    return ret;
}
int AudioSourceManager::openAudioSourceDb() {
    if (mDbOpen) {
        return NO_ERROR;
    }
    int ret = NO_ERROR;
    struct stat fileStat;
    if (stat(kDefaultAudioSourceDb, &fileStat) != 0) {
        ALOGW("source db is not exist, db:%s", kDefaultAudioSourceDb);
    }
    ret = openOrCreateAudioSourceDb();
    return ret;
}
int AudioSourceManager::saveAudioSource(int dataid,
         struct FocusInfo& savedata) {
    Mutex::Autolock _l(mLock);
    int ret = openAudioSourceDb();
    if (ret != NO_ERROR) {
        return ret;
    }
    focus_info_t mAudioSource;
    memset(&mAudioSource, 0, sizeof(focus_info_t));
    mAudioSource = savedata;
    sp<AudioSourceDb> mmAudioSourceDb = new AudioSourceDb();
    mmAudioSourceDb->dataid = dataid;
    mmAudioSourceDb->attr_usage = mAudioSource.attribute.usage;
    mmAudioSourceDb->attr_audio_usage =
            mAudioSource.attribute.audio_usage;
    mmAudioSourceDb->attr_context_type =
            mAudioSource.attribute.context_type;
    mmAudioSourceDb->attr_source = mAudioSource.attribute.source;
    mmAudioSourceDb->attr_flags = mAudioSource.attribute.flags;
    mmAudioSourceDb->display_id = 0;
    mmAudioSourceDb->client_id = String8(mAudioSource.client_id);
    mmAudioSourceDb->client_uid = mAudioSource.client_uid;
    mmAudioSourceDb->package_name = String8(mAudioSource.package_name);
    mmAudioSourceDb->gain_request = mAudioSource.gain_request;
    mmAudioSourceDb->loss_received = mAudioSource.loss_received;
    mmAudioSourceDb->flags = mAudioSource.flags;
    mmAudioSourceDb->sdk_target = mAudioSource.sdk_target;
    mmAudioSourceDb->gen_count = mAudioSource.gen_count;
    mmAudioSourceDb->create_time = String8("");
    mmAudioSourceDb->update_time = String8("");
    ret = updateAudioSourceTable(mmAudioSourceDb);
    closeAudioSourceDb();
    return ret;
}
int AudioSourceManager::readAudioSource(int dataid,
        struct FocusInfo* readdata) {
    Mutex::Autolock _l(mLock);
    int ret = openAudioSourceDb();
    if (ret != NO_ERROR) {
        return AUDIO_SOURCE_NONE;
    }
    focus_info_t mAudioSource;
    memset(&mAudioSource, 0, sizeof(focus_info_t));
    int audioDataId = AUDIO_SOURCE_NONE;
    ret = getAudioSourceTable(dataid);
    if (mAudioSourceDb != NULL && mAudioSourceDb->size() > 0) {
        int size = mAudioSourceDb->size();
        for (int i = 0; i < size; i++) {
            audioDataId = mAudioSourceDb->itemAt(i)->dataid;
            if (audioDataId == dataid) {
                mAudioSource.attribute.usage =
                        mAudioSourceDb->itemAt(i)->attr_usage;
                mAudioSource.attribute.audio_usage =
                        mAudioSourceDb->itemAt(i)->attr_audio_usage;
                mAudioSource.attribute.context_type =
                        mAudioSourceDb->itemAt(i)->attr_context_type;
                mAudioSource.attribute.source =
                        mAudioSourceDb->itemAt(i)->attr_source;
                mAudioSource.attribute.flags =
                        mAudioSourceDb->itemAt(i)->attr_flags;
                memcpy(mAudioSource.client_id,
                        (mAudioSourceDb->itemAt(i)->client_id).string(),
                        (mAudioSourceDb->itemAt(i)->client_id).size());
                mAudioSource.client_uid = mAudioSourceDb->itemAt(i)->client_uid;
                memcpy(mAudioSource.package_name,
                        (mAudioSourceDb->itemAt(i)->package_name).string(),
                        (mAudioSourceDb->itemAt(i)->package_name).size());
                mAudioSource.gain_request =
                        mAudioSourceDb->itemAt(i)->gain_request;
                mAudioSource.loss_received =
                        mAudioSourceDb->itemAt(i)->loss_received;
                mAudioSource.flags = mAudioSourceDb->itemAt(i)->flags;
                mAudioSource.sdk_target = mAudioSourceDb->itemAt(i)->sdk_target;
                mAudioSource.gen_count = mAudioSourceDb->itemAt(i)->gen_count;
                break;
            }
        }
    }
    memcpy(readdata, &mAudioSource, sizeof(focus_info_t));
    closeAudioSourceDb();
    return ret;
}
int AudioSourceManager::getAudioSourceCount() {
    Mutex::Autolock _l(mLock);
    int ret = 0;
    ret = openAudioSourceDb();
    if (ret != NO_ERROR) {
        ret = 0;
        return ret;
    }
    ret = checkAudioSourceTable();
    closeAudioSourceDb();
    return ret;
}
/**
 * ---------------------------------------------------------
 * audiodata  function
 * ---------------------------------------------------------
*/
int AudioSourceManager::getAudioDateTableCnt(int audioDataId) {
    if (!mSqlite)
        return UNKNOWN_ERROR;
    int sqlResult = SQLITE_OK;
    sqlite3_stmt *stmt = NULL;
    int updateCnt = 0;
    String8 sql(ASM_SQL_AUDIO_DATA_CNT);
    sqlResult = sqlite3_prepare_v2(mSqlite, sql.string(), strlen(sql.string()),
            &stmt, NULL);
    if (sqlResult == SQLITE_OK) {
        int bindIdx = 1;
        sqlite3_reset(stmt);
        sqlite3_bind_int(stmt, bindIdx++, audioDataId);
        sqlResult = sqlite3_step(stmt);
        if (sqlResult == SQLITE_ROW) {
            updateCnt = (unsigned int) sqlite3_column_int(stmt, 0);
        } else {
            updateCnt = 0;
        }
        sqlite3_finalize(stmt);
        stmt = NULL;
    }
    return updateCnt;
}
int AudioSourceManager::updateAudioDataTable(sp<AudioDataDb> audioDataDb,
        int buffer_size) {
    if (!mSqlite)
        return UNKNOWN_ERROR;
    AswAssert(audioDataDb.get());
    int ret = NO_ERROR;
    int sqlResult = SQLITE_OK;
    sqlite3_stmt *stmt = NULL;
    int updateCnt = getAudioDateTableCnt(audioDataDb->audioDataId);
    if (updateCnt > 0) {
        String8 sql(ASM_SQL_UPDATE_AUDIO_DATA_TABLE);
        sqlResult = sqlite3_prepare_v2(mSqlite,
                sql.string(), strlen(ASM_SQL_UPDATE_AUDIO_DATA_TABLE),
                &stmt, NULL);
    } else {
        String8 sql(ASM_SQL_INSERT_AUDIO_DATA_TABLE);
        sqlResult = sqlite3_prepare_v2(mSqlite,
                sql.string(), strlen(ASM_SQL_INSERT_AUDIO_DATA_TABLE),
                &stmt, NULL);
    }
    if (sqlResult == SQLITE_OK) {
        sqlite3_reset(stmt);
        int bindIdx = 1;
        sqlite3_bind_int(stmt, bindIdx++, audioDataDb->audioDataId);
        sqlite3_bind_blob(stmt, bindIdx++, audioDataDb->audioDataValue,
                buffer_size, SQLITE_STATIC);
        if (updateCnt > 0) {
            sqlite3_bind_int(stmt, bindIdx++, audioDataDb->audioDataId);
        }
        sqlResult = sqlite3_step(stmt);
        if (sqlResult != SQLITE_DONE) {
            ALOGE("Sql step error:%d", sqlResult);
        }
        sqlite3_finalize(stmt);
        stmt = NULL;
    } else {
        ALOGE("Sql prepare error:%d", sqlResult);
        ret = ASM_SQLITE_ERROR;
    }
    return ret;
}
int AudioSourceManager::getAudioDataTable(int audioDataId, int buffer_size) {
    int ret = NO_ERROR;
    int sqlResult = SQLITE_OK;
    int dataId = -1;
    sqlite3_stmt *stmt = NULL;
    if (mAudioDataDb != NULL) {
        mAudioDataDbLock.lock();
        mAudioDataDb->clear();
        mAudioDataDbLock.unlock();
    }
    String8 sql(ASM_SQL_SELECT_AUDIO_DATA);
    sqlResult = sqlite3_prepare_v2(mSqlite, sql.string(), sql.length(),
            &stmt, NULL);
    if (sqlResult == SQLITE_OK) {
        sqlite3_reset(stmt);
        int bindIdx = 1;
        sqlite3_bind_int(stmt, bindIdx++, audioDataId);
        while (sqlite3_step(stmt) == SQLITE_ROW) {
            int rowIdx = 1;
            dataId = sqlite3_column_int(stmt, rowIdx);
            if (audioDataId == dataId) {
                sp<AudioDataDb> audioDataDb = new AudioDataDb();
                audioDataDb->audioDataId = sqlite3_column_int(stmt, rowIdx++);
                unsigned char * p = (unsigned char *) sqlite3_column_blob(stmt,
                        rowIdx++);
                audioDataDb->audioDataValue = (unsigned char*)
                        malloc(buffer_size);
                memset(audioDataDb->audioDataValue, 0, buffer_size);
                memcpy(audioDataDb->audioDataValue, p, buffer_size);
                mAudioDataDb->add(audioDataDb);
            }
        }
        sqlite3_finalize(stmt);
        stmt = NULL;
    } else {
        ALOGE("Sql prepare error:%d", sqlResult);
        ret = ASM_SQLITE_ERROR;
    }
    return ret;
}
int AudioSourceManager::checkAudioDataTable() {
    if (!mSqlite)
        return UNKNOWN_ERROR;
    int ret = ASM_SQLITE_ERROR;
    int sqlResult = NO_ERROR;
    sqlite3_stmt *stmt = NULL;
    unsigned int tableCnt = 0;
    String8 sql(ASM_SQL_CHECK_AUDIO_DATA_TABLE);
    sqlResult = sqlite3_prepare_v2(mSqlite, sql.string(), strlen(sql.string()),
            &stmt, NULL);
    if (sqlResult == SQLITE_OK) {
        sqlite3_reset(stmt);
        sqlResult = sqlite3_step(stmt);
        if (sqlResult == SQLITE_ROW) {
            tableCnt = (unsigned int) sqlite3_column_int(stmt, 0);
        } else {
            ALOGE("Sql step error:%d", sqlResult);
        }
        sqlite3_finalize(stmt);
        stmt = NULL;
    } else {
        ALOGE("Sql prepare error:%d", sqlResult);
        ret = ASM_SQLITE_ERROR;
    }
    if (tableCnt > 0) {
        ret = NO_ERROR;
    }
    return ret;
}
int AudioSourceManager::createAudioDataTable() {
    if (!mSqlite)
        return UNKNOWN_ERROR;
    int ret = ASM_SQLITE_ERROR;
    int sqlResult = SQLITE_OK;
    char *strErr = NULL;
    String8 sqlDrop(ASM_SQL_DROP_AUDIO_DATA_TABLE);
    if ((sqlResult = sqlite3_exec(mSqlite, sqlDrop.string(),
            0, 0, &strErr)) != SQLITE_OK) {
        ALOGW("Cant drop sqlite db table err:%d", sqlResult);
    }
    String8 sql(ASM_SQL_CREATE_AUDIODATA_TABLE);
    if ((sqlResult = sqlite3_exec(mSqlite, sql.string(),
            0, 0, &strErr)) != SQLITE_OK) {
        ret = ASM_SQLITE_ERROR;
        ALOGE("Cant create sqlite db table err:%d", sqlResult);
    } else {
        ret = NO_ERROR;
    }
    if (strErr != NULL) {
        sqlite3_free(strErr);
        strErr = NULL;
    }
    return ret;
}
int AudioSourceManager::openOrCreateAudioDataDb() {
    int ret = NO_ERROR;
    int sqlResult = SQLITE_OK;
    ALOGV("openOrCreateAudioSourceDb mDbOpen%d", mDbOpen);
    sqlResult = sqlite3_open_v2(kDefaultAudioSourceDb, &mSqlite,
            (SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE), NULL);
    if (sqlResult != SQLITE_OK) {
        ALOGE("Cant open sqlite db:%s, err:%d", kDefaultAudioSourceDb,
                sqlResult);
        ret = ASM_SQLITE_ERROR;
        mSqlite = NULL;
        return ret;
    } else {
        ALOGV("suceess to open audio source db, mDbOpen:%d", mDbOpen);
        if (checkAudioDataTable() != NO_ERROR) {
            createAudioDataTable();
        }
        mDbOpen = true;
    }
    return ret;
}
int AudioSourceManager::openAudioDataDb() {
    if (mDbOpen) {
        return NO_ERROR;
    }
    int ret = NO_ERROR;
    struct stat fileStat;
    if (stat(kDefaultAudioSourceDb, &fileStat) != 0) {
        ALOGW("source db is not exist, db:%s", kDefaultAudioSourceDb);
    }
    ret = openOrCreateAudioDataDb();
    return ret;
}
int AudioSourceManager::saveAudioData(int dataid, unsigned char* buffer,
         int buffer_size) {
    Mutex::Autolock _l(mLock);
    int ret = NO_ERROR;
    ret = openAudioDataDb();
    if (ret != NO_ERROR) {
        return ret;
    }
    sp<AudioDataDb> audioDataDb = new AudioDataDb();
    audioDataDb->audioDataId = dataid;
    audioDataDb->audioDataValue = (unsigned char*)malloc(buffer_size);
    memset(audioDataDb->audioDataValue, 0, buffer_size);
    memcpy(audioDataDb->audioDataValue,  buffer, buffer_size);
    ret = updateAudioDataTable(audioDataDb, buffer_size);
    if (audioDataDb->audioDataValue != NULL) {
        free(audioDataDb->audioDataValue);
        audioDataDb->audioDataValue = NULL;
    }
    closeAudioSourceDb();
    return ret;
}
int AudioSourceManager::readAudioData(int dataid, unsigned char* buffer,
        int buffer_size) {
    Mutex::Autolock _l(mLock);
    int ret = openAudioDataDb();
    if (ret != NO_ERROR) {
        return AUDIO_SOURCE_NONE;
    }
    int audioDataId = AUDIO_SOURCE_NONE;
    ret = getAudioDataTable(dataid, buffer_size);
    if (mAudioDataDb != NULL && mAudioDataDb->size() > 0) {
        int size = mAudioDataDb->size();
        for (int i = 0; i < size; i++) {
            audioDataId = mAudioDataDb->itemAt(i)->audioDataId;
            if (audioDataId == dataid) {
                if (mAudioDataDb->itemAt(i)->audioDataValue != NULL) {
                    memcpy(buffer, mAudioDataDb->itemAt(i)->audioDataValue,
                            buffer_size);
                    free(mAudioDataDb->itemAt(i)->audioDataValue);
                    mAudioDataDb->itemAt(i)->audioDataValue = NULL;
                }
                break;
            }
        }
    }
    closeAudioSourceDb();
    return ret;
}
};  // namespace android
