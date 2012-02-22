/*******************************************************************************mFolder
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;

import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogUtils;

public class AsyncRefreshTask extends AsyncTask<Void, Void, Void> {
    private static final String LOG_TAG = new LogUtils().getLogTag();
    private Context mContext;
    private Folder mFolder;
    private Cursor mFolderCursor;
    private ContentObserver mFolderObserver;

    public AsyncRefreshTask(Context context, Folder folder) {
        mContext = context;
        mFolder = folder;
        mFolderObserver = new FolderObserver();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        String refreshUri = mFolder.refreshUri;
        if (!TextUtils.isEmpty(refreshUri)) {
            // Watch for changes on the folder.
            mFolderCursor = mContext.getContentResolver().query(Uri.parse(mFolder.uri),
                    UIProvider.FOLDERS_PROJECTION, null, null, null);
            mFolderCursor.registerContentObserver(mFolderObserver);
            // TODO: (mindyp) Start the spinner here.
            mContext.getContentResolver().query(Uri.parse(refreshUri), null, null, null, null);
        }
        return null;
    }

    private class FolderObserver extends ContentObserver {
        public FolderObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            // TODO: (mindyp) Check the new folder status. If syncing is
            // complete "SUCCESS", stop the spinner here.
            // If error, stop the spinner and show the error icon.
            // Remove the listener.
            if (mFolderObserver != null) {
                mFolderCursor.unregisterContentObserver(mFolderObserver);
                mFolderObserver = null;
            }
            // TODO: make this async.
            mFolderCursor.close();
            mFolderCursor = mContext.getContentResolver().query(Uri.parse(mFolder.uri),
                    UIProvider.FOLDERS_PROJECTION, null, null, null);
            mFolderCursor.moveToFirst();
            Folder folder = new Folder(mFolderCursor);
            switch (folder.syncStatus) {
                case UIProvider.LastSyncResult.SUCCESS:
                    // Stop the spinner here.
                    // Don't add a new listener; the sync is done.
                    break;
                default:
                    // re-add the listener
                    mFolderCursor.registerContentObserver(mFolderObserver);
                    break;
            }
            LogUtils.v(LOG_TAG, "FOLDER STATUS = " + folder.syncStatus);
        }
    }
}
