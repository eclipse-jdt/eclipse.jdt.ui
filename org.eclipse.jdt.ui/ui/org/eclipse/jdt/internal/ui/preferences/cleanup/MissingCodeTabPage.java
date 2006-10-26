/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.PotentialProgrammingProblemsCleanUp;
import org.eclipse.jdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialog;

public final class MissingCodeTabPage extends ModifyDialogTabPage {
	
    private final Map fValues;

    private CleanUpPreview fCleanUpPreview;

    public MissingCodeTabPage(ModifyDialog dialog, Map values) {
	    super(dialog, values);
	    fValues= values;
    }

    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fCleanUpPreview= new CleanUpPreview(parent, new ICleanUp[] {
        		new Java50CleanUp(fValues),
        		new PotentialProgrammingProblemsCleanUp(fValues)
        }, false);
    	return fCleanUpPreview;
    }

    protected void doCreatePreferences(Composite composite, int numColumns) {
    	
    	Group annotationsGroup= createGroup(numColumns, composite, CleanUpMessages.MissingCodeTabPage_GroupName_Annotations);
    	
    	final CheckboxPreference annotationsPref= createCheckboxPref(annotationsGroup, numColumns, CleanUpMessages.MissingCodeTabPage_CheckboxName_AddMissingAnnotations, CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpModifyDialog.FALSE_TRUE);
    	
    	intent(annotationsGroup);		
		final CheckboxPreference overridePref= createCheckboxPref(annotationsGroup, numColumns - 1, CleanUpMessages.MissingCodeTabPage_CheckboxName_AddMissingOverrideAnnotations, CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpModifyDialog.FALSE_TRUE);
		
		intent(annotationsGroup);
		final CheckboxPreference deprecatedPref= createCheckboxPref(annotationsGroup, numColumns - 1, CleanUpMessages.MissingCodeTabPage_CheckboxName_AddMissingDeprecatedAnnotations, CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpModifyDialog.FALSE_TRUE);
		
    	annotationsPref.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			overridePref.setEnabled(annotationsPref.getChecked());
    			deprecatedPref.setEnabled(annotationsPref.getChecked());
    		}
    	});

		overridePref.setEnabled(annotationsPref.getChecked());
		deprecatedPref.setEnabled(annotationsPref.getChecked());
		
		Group pppGroup= createGroup(numColumns, composite, CleanUpMessages.MissingCodeTabPage_GroupName_PotentialProgrammingProblems);
    	final CheckboxPreference addSUIDPref= createCheckboxPref(pppGroup, numColumns, CleanUpMessages.MissingCodeTabPage_CheckboxName_AddSUID, CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID, CleanUpModifyDialog.FALSE_TRUE);
    	
		intent(pppGroup);
		final RadioPreference generatedPref= createRadioPref(pppGroup, 1, CleanUpMessages.MissingCodeTabPage_RadioName_AddGeneratedSUID, CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED, CleanUpModifyDialog.FALSE_TRUE);
		final RadioPreference defaultPref= createRadioPref(pppGroup, 1, CleanUpMessages.MissingCodeTabPage_RadioName_AddDefaultSUID, CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, CleanUpModifyDialog.FALSE_TRUE);
    	
    	addSUIDPref.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			generatedPref.setEnabled(addSUIDPref.getChecked());
    			defaultPref.setEnabled(addSUIDPref.getChecked());
    		}
    		
    	});

		generatedPref.setEnabled(addSUIDPref.getChecked());
		defaultPref.setEnabled(addSUIDPref.getChecked());
    }

    private void intent(Composite group) {
        Label l= new Label(group, SWT.NONE);
    	GridData gd= new GridData();
    	gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(4);
    	l.setLayoutData(gd);
    }

    protected void doUpdatePreview() {
    	fCleanUpPreview.setWorkingValues(fValues);
    	fCleanUpPreview.update();
    }

    protected void initializePage() {
    	fCleanUpPreview.update();
    }
}