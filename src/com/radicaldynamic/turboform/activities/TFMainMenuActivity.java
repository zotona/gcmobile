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

package com.radicaldynamic.turboform.activities;

import java.util.ArrayList;

import com.radicaldynamic.turboform.R;

import com.radicaldynamic.turboform.database.FileDbAdapter;
import com.radicaldynamic.turboform.preferences.ServerPreferences;
import com.radicaldynamic.turboform.utilities.FileUtils;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Responsible for displaying buttons to launch the major activities. Launches some activities based
 * on returns of others.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class TFMainMenuActivity extends ListActivity {

	private AlertDialog mAlertDialog;	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tf_main_menu);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.main_menu));
        
        // Quit early if there is something wrong with the SD card
        if (!FileUtils.storageReady()) {
        	createErrorDialog(getString(R.string.no_sd_error),true);
        }
        
        refreshView(FileDbAdapter.TYPE_FORM, null);
        registerForContextMenu(getListView());
        
        Spinner s1 = (Spinner) findViewById(R.id.filter);        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.tf_main_menu_list_filters, android.R.layout.simple_spinner_item);        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);        
        s1.setAdapter(adapter);        
        s1.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                        case 0:
                            refreshView(FileDbAdapter.TYPE_FORM, null);
                            break;
                        case 1:
                            refreshView(FileDbAdapter.TYPE_INSTANCE, FileDbAdapter.STATUS_INCOMPLETE);
                            break;
                        case 2:
                            refreshView(FileDbAdapter.TYPE_INSTANCE, FileDbAdapter.STATUS_COMPLETE);
                        }                        
                    }

                    public void onNothingSelected(AdapterView<?> parent) {        
                    }
                });        
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
    	dismissDialogs();
        super.onPause();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        /* Spinner must reflect results of refreshView() below */
        Spinner s1 = (Spinner) findViewById(R.id.filter); 
        s1.setSelection(0);
        
        refreshView(FileDbAdapter.TYPE_FORM, null);
    }
    
    /**
     * Provide some helpful hints when the application is started post onCreate().
     * 
     * Called when the main window associated with the activity has been attached to the window manager.
     * This is used because we cannot open the options menu during onCreate().
     */
    @Override
    public void onAttachedToWindow() {      
        ArrayList<String> forms = FileUtils.getValidFormsAsArrayList(FileUtils.FORMS_PATH);
    
        if (forms != null) {
            if (forms.size() > 0) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.tf_begin_record_hint),
                        Toast.LENGTH_SHORT).show();            
            } else { 
                openOptionsMenu();
            
                Toast.makeText(getApplicationContext(),
                        getString(R.string.tf_add_form_hint),
                        Toast.LENGTH_LONG).show();
            }
        }       
    }
    
    /**
     * Get form list from database and insert into view.
     */
    private void refreshView(String type, String status) {
        // Get all forms that match the status.
        FileDbAdapter fda = new FileDbAdapter();
        
        fda.open();
        fda.addOrphanForms();
        
        Cursor c = fda.fetchFilesByType(type, status);
        startManagingCursor(c);

        // Create data and views for cursor adapter
        String[] data = new String[] {
                FileDbAdapter.KEY_DISPLAY, FileDbAdapter.KEY_META
        };
        
        int[] view = new int[] {
                android.R.id.text1, android.R.id.text2
        };

        // Render total instance view
        SimpleCursorAdapter instances =
            new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, c, data, view);
        
        setListAdapter(instances);        
       
        fda.close();       
    }


    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        // Get full path to the form
        Cursor c = (Cursor) getListAdapter().getItem(position);
        String formPath = c.getString(c.getColumnIndex(FileDbAdapter.KEY_FILEPATH));

        ComponentName callingActivity = getCallingActivity();

        if (callingActivity == null) {
            // Not called as an Intent, handle regularly
            Intent i = new Intent("com.radicaldynamic.turboform.action.FormEntry");
            i.putExtra(FormEntryActivity.KEY_FORMPATH, formPath);
            startActivity(i);
        } else {
            // Called as an intent, return path of form to calling activity
            Intent i = new Intent();
            i.putExtra(FormEntryActivity.KEY_FORMPATH, formPath);
            setResult(RESULT_OK, i);

            finish();
        }        
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tf_main_menu_context, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
      // AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      switch (item.getItemId()) {
      default:
        return super.onContextItemSelected(item);
      }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tf_main_menu_options, menu);
        return true;
    } 

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i = null;
        
        switch (item.getItemId()) {
        case R.id.tf_sync:
            return true;
        case R.id.tf_manage:
        //case R.id.tf_manage_forms:
            i = new Intent(this, TFManageForms.class);
            startActivity(i);
            return true;
        //case R.id.tf_manage_form_records:
        //    return true;
        case R.id.tf_personal_preferences:
            i = new Intent(this, ServerPreferences.class);
            startActivity(i);
            return true;
        case R.id.tf_simple_sharing:
            return true;
        case R.id.tf_web_publishing:
            return true;
        case R.id.tf_web_services:
            return true;        
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setMessage(errorMsg);
		
		DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON1:
					if (shouldExit) {
						finish();
					}
					break;
				}
			}
		};
		
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.ok), errorListener);
		mAlertDialog.show();
	}
    
    
    /**
	 * Dismiss any showing dialogs that we manage.
	 */
	private void dismissDialogs() {
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}
	}
}