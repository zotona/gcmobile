/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.radicaldynamic.groupinform.activities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.adapters.AccountFolderListAdapter;
import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.logic.AccountFolder;
import com.radicaldynamic.groupinform.logic.InformOnlineState;
import com.radicaldynamic.groupinform.utilities.FileUtils;
import com.radicaldynamic.groupinform.utilities.HttpUtils;

/*
 * 
 */
public class AccountFolderList extends ListActivity
{
    private static final String t = "AccountFolderList: ";

    private static final int MENU_ADD = Menu.FIRST;
    private static final int MENU_SYNC_LIST = Menu.FIRST + 1;
    
    private static final int CONTEXT_MENU_EDIT = Menu.FIRST;
    
    public static final int DIALOG_DENIED_NOT_OWNER = 0;
    public static final int DIALOG_OPENING = 1;
    
    private RefreshViewTask mRefreshViewTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);        
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.tf_form_folders));
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        loadScreen();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        boolean enabled = false;
        
        if (Collect.getInstance().getIoService().isSignedIn())
            enabled = true;
        
        menu.add(0, CONTEXT_MENU_EDIT, 0, getString(R.string.tf_edit_folder))
            .setEnabled(enabled);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = null;
        
        switch (id) {
        case DIALOG_DENIED_NOT_OWNER:            
            builder
                .setIcon(R.drawable.ic_dialog_info)
                .setTitle(R.string.tf_unable_to_edit_folder_not_owner_title)
                .setMessage(R.string.tf_unable_to_edit_folder_not_owner_msg)
                .setPositiveButton(R.string.tf_remove, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

            dialog = builder.create();
            break;
            
        case DIALOG_OPENING:
            dialog = ProgressDialog.show(this, "", getText(R.string.tf_opening_please_wait));
        }

        return dialog;    
    }    

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        boolean enabled = false;
        
        if (Collect.getInstance().getIoService().isSignedIn())
            enabled = true;
        
        menu.add(0, MENU_ADD, 0, getString(R.string.tf_create_folder))
            .setIcon(R.drawable.ic_menu_add)
            .setEnabled(enabled);
        
        menu.add(0, MENU_SYNC_LIST, 0, getString(R.string.tf_replication_list))
            .setIcon(R.drawable.ic_menu_sync_list)
            .setEnabled(enabled);
        
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        
        switch (item.getItemId()) {
        case CONTEXT_MENU_EDIT:
            AccountFolder folder = (AccountFolder) getListView().getItemAtPosition(info.position);
            
            if (Collect.getInstance().getInformOnlineState().getDeviceId().equals(folder.getOwnerId())) {
                Intent i = new Intent(this, AccountFolderActivity.class);
                i.putExtra(AccountFolderActivity.KEY_FOLDER_ID, folder.getId());
                i.putExtra(AccountFolderActivity.KEY_FOLDER_REV, folder.getRev());            
                i.putExtra(AccountFolderActivity.KEY_FOLDER_OWNER, folder.getOwnerId());
                i.putExtra(AccountFolderActivity.KEY_FOLDER_NAME, folder.getName());
                i.putExtra(AccountFolderActivity.KEY_FOLDER_DESC, folder.getDescription());
                i.putExtra(AccountFolderActivity.KEY_FOLDER_VISIBILITY, folder.getVisibility());
                startActivity(i);
            } else {
                showDialog(DIALOG_DENIED_NOT_OWNER);
            }           
            
            break;
            
        default:
            return super.onContextItemSelected(item);
        }
        
        return true;
    }

    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        AccountFolder folder = (AccountFolder) getListAdapter().getItem(position);
        new SelectFolderTask().execute(folder);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_ADD:
            startActivity(new Intent(this, AccountFolderActivity.class).putExtra(AccountFolderActivity.KEY_NEW_FOLDER, true));
            break;
            
        case MENU_SYNC_LIST:
            startActivity(new Intent(this, AccountFolderReplicationList.class));
            break;
        }
        
        return super.onOptionsItemSelected(item);
    }

    /*
     * Refresh the main form browser view as requested by the user
     */
    private class RefreshViewTask extends AsyncTask<Void, Void, Void>
    {
        private ArrayList<AccountFolder> folders = new ArrayList<AccountFolder>();

        @Override
        protected Void doInBackground(Void... nothing)
        {
            if (FileUtils.isFileOlderThan(getCacheDir() + File.separator + FileUtils.FOLDER_CACHE_FILE, FileUtils.TIME_TWO_MINUTES))                    
                fetchFolderList();

            folders = loadFolderList();
            
            return null;
        }

        @Override
        protected void onPreExecute()
        {
            //setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            RelativeLayout onscreenProgress = (RelativeLayout) findViewById(R.id.progress);
            onscreenProgress.setVisibility(View.GONE);
            
            if (folders.isEmpty()) {
                TextView nothingToDisplay = (TextView) findViewById(R.id.nothingToDisplay);
                nothingToDisplay.setVisibility(View.VISIBLE);
            } else {
                AccountFolderListAdapter adapter;
                
                adapter = new AccountFolderListAdapter(
                        getApplicationContext(),
                        R.layout.folder_list_item,
                        folders);

                setListAdapter(adapter);
            }            
        }
    }
    
    private class SelectFolderTask extends AsyncTask<AccountFolder, Void, Void>
    {
        AccountFolder folder = null;
        boolean folderReady = true;
        
        @Override
        protected Void doInBackground(AccountFolder... selection)
        {
            folder = selection[0];
            
            // Initialize any databases that should be synchronized
            if (folder.isReplicated())
                if (!Collect.getInstance().getDbService().isDbLocal(folder.getId()))                  
                    folderReady = Collect.getInstance().getDbService().initLocalDb(folder.getId());
            
            return null;
        }

        @Override
        protected void onPreExecute()
        {
            showDialog(DIALOG_OPENING);
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            dismissDialog(DIALOG_OPENING);
            
            if (folderReady) {            
                Collect.getInstance().getInformOnlineState().setSelectedDatabase(folder.getId());                  
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Unable to open " + folder.getName() + ". Please try again later.", Toast.LENGTH_LONG).show();
            }
        }
    }    
    
    /*
     * Fetch a new folder list from Inform Online and store it on disk
     */
    public static void fetchFolderList()
    {
        Log.d(Collect.LOGTAG, t + "fetching new list of folders");
        
        // Try to ping the service to see if it is "up"
        String folderListUrl = Collect.getInstance().getInformOnlineState().getServerUrl() + "/folder/list";
        String getResult = HttpUtils.getUrlData(folderListUrl);
        JSONObject jsonFolderList;
        
        try {
            Log.d(Collect.LOGTAG, t + "parsing getResult " + getResult);                
            jsonFolderList = (JSONObject) new JSONTokener(getResult).nextValue();
            
            String result = jsonFolderList.optString(InformOnlineState.RESULT, InformOnlineState.ERROR);
            
            if (result.equals(InformOnlineState.OK)) {
                // Write out list of jsonFolders for later retrieval by loadFoldersList()
                JSONArray jsonFolders = jsonFolderList.getJSONArray("folders");

                try {
                    // Write out a folder list cache file
                    FileOutputStream fos = new FileOutputStream(new File(Collect.getInstance().getCacheDir(), FileUtils.FOLDER_CACHE_FILE));
                    fos.write(jsonFolders.toString().getBytes());
                    fos.close();
                } catch (Exception e) {
                    Log.e(Collect.LOGTAG, t + "unable to write folder cache: " + e.toString());
                    e.printStackTrace();
                }
            } else {
                // There was a problem.. handle it!
            }
        } catch (NullPointerException e) {
            // Communication error
            Log.e(Collect.LOGTAG, t + "no getResult to parse.  Communication error with node.js server?");
            e.printStackTrace();
        } catch (JSONException e) {
            // Parse error (malformed result)
            Log.e(Collect.LOGTAG, t + "failed to parse getResult " + getResult);
            e.printStackTrace();
        }
    }

    public static ArrayList<AccountFolder> loadFolderList()
    {
        Log.d(Collect.LOGTAG , t + "loading folder cache");
        
        ArrayList<AccountFolder> folders = new ArrayList<AccountFolder>();
        
        if (!new File(Collect.getInstance().getCacheDir(), FileUtils.FOLDER_CACHE_FILE).exists()) {
            Log.d(Collect.LOGTAG, t + "folder cache file cannot be read: aborting loadFolderList()");
            return folders;
        }
        
        try {
            FileInputStream fis = new FileInputStream(new File(Collect.getInstance().getCacheDir(), FileUtils.FOLDER_CACHE_FILE));
            InputStreamReader reader = new InputStreamReader(fis);
            BufferedReader buffer = new BufferedReader(reader, 8192);
            StringBuilder sb = new StringBuilder();
            
            String cur;

            while ((cur = buffer.readLine()) != null) {
                sb.append(cur + "\n");
            }
            
            buffer.close();
            reader.close();
            fis.close();
            
            try {
                JSONArray jsonFolders = (JSONArray) new JSONTokener(sb.toString()).nextValue();
                
                for (int i = 0; i < jsonFolders.length(); i++) {
                    JSONObject jsonFolder = jsonFolders.getJSONObject(i);
                    
                    AccountFolder folder = new AccountFolder(
                            jsonFolder.getString("id"),
                            jsonFolder.getString("rev"),
                            jsonFolder.getString("owner"),
                            jsonFolder.getString("name"),
                            jsonFolder.getString("description"),
                            jsonFolder.getString("visibility"),
                            jsonFolder.getBoolean("replication"));
                    
                    folders.add(folder);
                    
                    // Also update the account folder hash since this will be needed by BrowserActivity, among other things
                    Collect.getInstance().getInformOnlineState().getAccountFolders().put(folder.getId(), folder);
                }
            } catch (JSONException e) {
                // Parse error (malformed result)
                Log.e(Collect.LOGTAG, t + "failed to parse JSON " + sb.toString());
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(Collect.LOGTAG, t + "unable to read folder cache: " + e.toString());
            e.printStackTrace();
        }
     
        return folders;
    }

    /**
     * Load the various elements of the screen that must wait for other tasks to
     * complete
     */
    private void loadScreen()
    {
        mRefreshViewTask = new RefreshViewTask();
        mRefreshViewTask.execute();

        registerForContextMenu(getListView());
    }
}