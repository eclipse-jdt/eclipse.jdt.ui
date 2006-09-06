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
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialog;

final class UnnecessaryCodeTabPage extends ModifyDialogTabPage {
	
    private final Map fValues;

    private CleanUpPreview fCleanUpPreview;

    public UnnecessaryCodeTabPage(ModifyDialog dialog, Map values) {
	    super(dialog, values);
	    fValues= values;
    }

    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fCleanUpPreview= new CleanUpPreview(parent, new ICleanUp[] {
        		new UnusedCodeCleanUp(fValues),
        		new UnnecessaryCodeCleanUp(fValues),
        		new StringCleanUp(fValues)
        }, false);
    	return fCleanUpPreview;
    }

    protected void doCreatePreferences(Composite composite, int numColumns) {
    	
    	Group unusedCodeGroup= createGroup(5, composite, CleanUpMessages.UnnecessaryCodeTabPage_GroupName_UnusedCode);
    	
    	createCheckboxPref(unusedCodeGroup, 5, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedImports, CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpModifyDialog.FALSE_TRUE);
    	
    	final CheckboxPreference unusedMembersPref= createCheckboxPref(unusedCodeGroup, 5, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedMembers, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpModifyDialog.FALSE_TRUE);
    	
		intent(unusedCodeGroup);
		
		final CheckboxPreference typesPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedTypes, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference constructorPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedConstructors, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference fieldsPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedFields, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference methodsPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedMethods, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpModifyDialog.FALSE_TRUE);

    	unusedMembersPref.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			typesPref.setEnabled(unusedMembersPref.getChecked());
    			constructorPref.setEnabled(unusedMembersPref.getChecked());
    			fieldsPref.setEnabled(unusedMembersPref.getChecked());
    			methodsPref.setEnabled(unusedMembersPref.getChecked());
    		}
    	});

		typesPref.setEnabled(unusedMembersPref.getChecked());
		constructorPref.setEnabled(unusedMembersPref.getChecked());
		fieldsPref.setEnabled(unusedMembersPref.getChecked());
		methodsPref.setEnabled(unusedMembersPref.getChecked());
		
    	createCheckboxPref(unusedCodeGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedLocalVariables, CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpModifyDialog.FALSE_TRUE);
    	
    	Group unnecessaryGroup= createGroup(numColumns, composite, CleanUpMessages.UnnecessaryCodeTabPage_GroupName_UnnecessaryCode);
    	
    	createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnnecessaryCasts, CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpModifyDialog.FALSE_TRUE);
    	createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnnecessaryNLSTags, CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS, CleanUpModifyDialog.FALSE_TRUE);
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