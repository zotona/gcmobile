package com.radicaldynamic.turboform.application;

import java.util.ArrayList;

import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.form.api.FormEntryController;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.documents.FormDocument;
import com.radicaldynamic.turboform.logic.FileReferenceFactory;
import com.radicaldynamic.turboform.services.CouchDbService;
import com.radicaldynamic.turboform.xform.Control;

public class Collect extends Application {
    public final static String LOGTAG = "TurboForm";
    public static CouchDbService mDb = null;    
	private static Collect singleton = null;
	
	private FormEntryController formEntryController = null;
    private FileReferenceFactory factory = null;
    private IBinder viewToken = null;
    
    private boolean firstReferenceInitialization = true;
    
    private ArrayList<String> instanceBrowseList = new ArrayList<String>();
    private ArrayList<Control> formBuilderControlState = null;
    private FormDocument formBuilderForm = null;    

	/* (non-Javadoc)
	 * @see android.app.Application#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/* (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		singleton = this;
	}
	
    public static Collect getInstance()
    {
        return singleton;
    }

	public void setFormEntryController(FormEntryController formEntryController) 
	{
		this.formEntryController = formEntryController;
	}
	
	public FormEntryController getFormEntryController() 
	{
		return formEntryController;
	}	

    public void setInstanceBrowseList(ArrayList<String> instanceBrowseList)
    {
        this.instanceBrowseList = instanceBrowseList;
    }

    public ArrayList<String> getInstanceBrowseList()
    {
        return instanceBrowseList;
    }

	public void registerMediaPath(String mediaPath)
	{
	    Log.d(LOGTAG, "Registering media path " + mediaPath);
	    
	    if (factory != null) {
    		ReferenceManager._().removeReferenceFactory(factory);
        }               
        
    	factory = new FileReferenceFactory(mediaPath);
        ReferenceManager._().addReferenceFactory(factory);
        
    	if (firstReferenceInitialization) {
    		firstReferenceInitialization = false;
            ReferenceManager._().addRootTranslator(new RootTranslator("jr://images/", "jr://file/"));
            ReferenceManager._().addRootTranslator(new RootTranslator("jr://audio/", "jr://file/"));
            ReferenceManager._().addRootTranslator(new RootTranslator("jr://video/", "jr://file/"));
        }
	}	
	
    /**
     * Creates and displays a dialog displaying the violated constraint.
     */
    public void showSoftKeyboard(View v)
    {
        InputMethodManager inputManager = (InputMethodManager) getBaseContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        IBinder b = v.getWindowToken();
        if (viewToken != null && !viewToken.equals(b)) {
            inputManager.hideSoftInputFromInputMethod(viewToken, 0);
        }

        if (inputManager.isActive(v))
            return;
        inputManager.showSoftInput(v, 0);
        viewToken = b;
    }

    public void hideSoftKeyboard(View c)
    {
        InputMethodManager inputManager = (InputMethodManager) getBaseContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (viewToken != null) {
            inputManager.hideSoftInputFromWindow(viewToken, 0);
        }
        viewToken = null;

        if (c != null) {
            if (inputManager.isActive()) {
                inputManager.hideSoftInputFromWindow(c
                        .getApplicationWindowToken(), 0);
            }
        }
    }

	public void createConstraintToast(String constraintText, int saveStatus) 
	{
		switch (saveStatus) {
		case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
			if (constraintText == null) {
				constraintText = getString(R.string.invalid_answer_error);
			}
			break;
		case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
			constraintText = getString(R.string.required_answer_error);
			break;
		}

		showCustomToast(constraintText);
	}

	private void showCustomToast(String message) 
	{
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.toast_view, null);

		// Set the text in the view
		TextView tv = (TextView) view.findViewById(R.id.message);
		tv.setText(message);

		Toast t = new Toast(this);
		t.setView(view);
		t.setDuration(Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

    public void setFormBuilderControlState(ArrayList<Control> formBuilderControlState)
    {
        this.formBuilderControlState = formBuilderControlState;
    }

    public ArrayList<Control> getFormBuilderControlState()
    {
        return formBuilderControlState;
    }

    public void setFormBuilderForm(FormDocument formBuilderForm)
    {
        this.formBuilderForm = formBuilderForm;
    }

    public FormDocument getFormBuilderForm()
    {
        return formBuilderForm;
    }
}