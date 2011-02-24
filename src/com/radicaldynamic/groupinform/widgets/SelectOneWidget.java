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

package com.radicaldynamic.groupinform.widgets;

import java.util.Vector;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import com.radicaldynamic.groupinform.views.AbstractFolioView;
import com.radicaldynamic.groupinform.views.IAVTLayout;
import com.radicaldynamic.groupinform.widgets.AbstractQuestionWidget.OnDescendantRequestFocusChangeListener.FocusChangeState;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * SelectOneWidgets handles select-one fields using radio buttons.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SelectOneWidget extends AbstractQuestionWidget implements OnCheckedChangeListener, IMultipartSelectWidget {
    
    private boolean insideUpdate = false;

    /**
     * The buttons ordering is the same as the prompt's Vector<SelectChoice>
     */
    final RadioButton[] buttons;


    public SelectOneWidget(Handler handler, Context context, FormEntryPrompt prompt) {
        super(handler, context, prompt);
        int dim = 0;
        if ( prompt.getSelectChoices() != null ) {
        	dim = prompt.getSelectChoices().size();
        }
        if ( dim == 0 ) {
        	buttons = null; 
        } else {
        	buttons = new RadioButton[dim];
        }
    }
    
    @Override
	public IAnswerData getAnswer() {

    	if ( buttons != null ) {
    		for ( int i = 0 ; i < buttons.length ; ++i ) {
	    		RadioButton b = buttons[i];
	    		if ( b.isChecked() ) {
	    			SelectChoice sc = prompt.getSelectChoices().elementAt(i);
	                return new SelectOneData(new Selection(sc));
	    		}
	    	}
    	}
    	return null;
    }

    @Override
    protected void buildViewBodyImpl() {
		Vector<SelectChoice> items = prompt.getSelectChoices();
		if ( items != null ) {
			boolean first = true;
	    	for ( SelectChoice c : items ) {
	    		if ( !first ) {
    		        // Add a dividing line before all but the first element
    		        View divider = new View(getContext());
    		        
    		        divider.setId(AbstractQuestionWidget.newUniqueId());
    		        divider.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
    	            addView(divider);
	    		}
	    		first = false;
	    		buildSelectElement(c);
	    	}
		}
    }

	@Override
	public ViewGroup buildSelectElement(SelectChoice sc) {
		Vector<SelectChoice> items = prompt.getSelectChoices();
		if ( items == null ) {
			// should never get here...
			throw new IllegalStateException("no selection choices!");
		}

		int i;
		for ( i = 0 ; i < items.size() ; ++i ) {
			if ( items.get(i).equals(sc) ) break;
		}
		
		if ( i == items.size() ) {
			throw new IllegalArgumentException("selection choice not found!");
		}

        RadioButton r = new RadioButton(getContext());
        buttons[i] = r;

        r.setId(AbstractQuestionWidget.newUniqueId());
        r.setText(prompt.getSelectChoiceText(sc));
        r.setTextSize(TypedValue.COMPLEX_UNIT_DIP, AbstractFolioView.APPLICATION_FONTSIZE);
        r.setEnabled(!prompt.isReadOnly());

        r.setOnCheckedChangeListener(this);

        String audioURI =
                prompt.getSpecialFormSelectChoiceText(sc, FormEntryCaption.TEXT_FORM_AUDIO);


        String imageURI =
                prompt.getSpecialFormSelectChoiceText(sc, FormEntryCaption.TEXT_FORM_IMAGE);
        

        String videoURI =
        		prompt.getSpecialFormSelectChoiceText(sc, "video");
        
        String bigImageURI = null;
        bigImageURI = prompt.getSpecialFormSelectChoiceText(sc, "big-image");
         

        IAVTLayout mediaLayout = new IAVTLayout(getContext());
        mediaLayout.setId(AbstractQuestionWidget.newUniqueId());
        mediaLayout.setAVT(r, audioURI, imageURI, videoURI, bigImageURI);
        
        addView(mediaLayout);
        return this;
	}

    protected void updateViewAfterAnswer() {
    	try {
    		insideUpdate = true;
	        String s = null;
	        if (prompt.getAnswerValue() != null) {
	            s = ((Selection) prompt.getAnswerValue().getValue()).getValue();
	        }
	        
	        Vector<SelectChoice> items = prompt.getSelectChoices();
	        if ( items != null ) {
	        	for ( int i = 0 ; i < items.size(); ++i ) {
	        		String sMatch = items.get(i).getValue();
	                RadioButton r = buttons[i];
	        		r.setChecked(sMatch.equals(s));
	        	}
	        }
    	} finally {
    		insideUpdate = false;
    	}
    }

    @Override
    public void setEnabled(boolean isEnabled) {
    	if ( buttons != null ) {
	    	for ( View v : buttons) {
	    		v.setEnabled(isEnabled && !prompt.isReadOnly());
	    	}
    	}
    }

    @Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    	Log.i(SelectOneWidget.class.getName(), 
    			"onCheckedChanged isChecked: " + Boolean.toString(isChecked));

    	// no-op if read-only
    	// no-op if insideUpdate
    	// no-op if not checked (i.e., we are unchecking a button)
    	if (!prompt.isReadOnly() && !insideUpdate && isChecked) {
		    
    		// make sure all others are unchecked...
	        if ( buttons != null ) {
		        for (RadioButton button : buttons) {
		            if (button.isChecked() && (buttonView != button)) {
		                button.setChecked(false);
		            }
		        }
	        }

        	// hide the soft keyboard if it is displayed for some other control...
        	setFocus(getContext());
	        signalDescendant(FocusChangeState.FLUSH_CHANGE_TO_MODEL);
    	}
    }
}