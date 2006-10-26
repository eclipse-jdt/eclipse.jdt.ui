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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialog;

public final class MemberAccessesTabPage extends ModifyDialogTabPage {
	
    private final Map fValues;

    private CleanUpPreview fCleanUpPreview;

    public MemberAccessesTabPage(ModifyDialog dialog, Map values) {
	    super(dialog, values);
	    fValues= values;
    }

    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fCleanUpPreview= new CleanUpPreview(parent, new ICleanUp[] {
        	new CodeStyleCleanUp(fValues)		
        }, false);
    	return fCleanUpPreview;
    }

    protected void doCreatePreferences(Composite composite, int numColumns) {
    	
    	Group instanceGroup= createGroup(numColumns, composite, CleanUpMessages.MemberAccessesTabPage_GroupName_NonStaticAccesses);
    	final CheckboxPreference thisFieldPref= createCheckboxPref(instanceGroup, numColumns, CleanUpMessages.MemberAccessesTabPage_CheckboxName_FieldQualifier, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpModifyDialog.FALSE_TRUE);
    	
    	Composite sub= new Composite(instanceGroup, SWT.NONE);
		sub.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout layout= new GridLayout(3, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		sub.setLayout(layout);

		intent(sub);
		final RadioPreference thisFieldAlwaysPref= createRadioPref(sub, 1, CleanUpMessages.MemberAccessesTabPage_RadioName_AlwaysThisForFields, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		final RadioPreference thisFieldNecessaryPref= createRadioPref(sub, 1, CleanUpMessages.MemberAccessesTabPage_RadioName_NeverThisForFields, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY, CleanUpModifyDialog.FALSE_TRUE);
    	
    	thisFieldPref.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			thisFieldAlwaysPref.setEnabled(thisFieldPref.getChecked());
    			thisFieldNecessaryPref.setEnabled(thisFieldPref.getChecked());
    		}
    		
    	});
    	
    	thisFieldAlwaysPref.setEnabled(thisFieldPref.getChecked());
    	thisFieldNecessaryPref.setEnabled(thisFieldPref.getChecked());

    	final CheckboxPreference thisMethodPref= createCheckboxPref(instanceGroup, numColumns, CleanUpMessages.MemberAccessesTabPage_CheckboxName_MethodQualifier, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, CleanUpModifyDialog.FALSE_TRUE);
    	
    	sub= new Composite(instanceGroup, SWT.NONE);
		sub.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		layout= new GridLayout(3, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		sub.setLayout(layout);

		intent(sub);
		final RadioPreference thisMethodAlwaysPref= createRadioPref(sub, 1, CleanUpMessages.MemberAccessesTabPage_RadioName_AlwaysThisForMethods, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		final RadioPreference thisMethodNecessaryPref= createRadioPref(sub, 1, CleanUpMessages.MemberAccessesTabPage_RadioName_NeverThisForMethods, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY, CleanUpModifyDialog.FALSE_TRUE);
    	
    	thisMethodPref.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			thisMethodAlwaysPref.setEnabled(thisMethodPref.getChecked());
    			thisMethodNecessaryPref.setEnabled(thisMethodPref.getChecked());
    		}
    		
    	});
    	
    	thisMethodAlwaysPref.setEnabled(thisMethodPref.getChecked());
    	thisMethodNecessaryPref.setEnabled(thisMethodPref.getChecked());

    	Group staticGroup= createGroup(numColumns, composite, CleanUpMessages.MemberAccessesTabPage_GroupName_StaticAccesses);
    	final CheckboxPreference staticMemberPref= createCheckboxPref(staticGroup, numColumns, CleanUpMessages.MemberAccessesTabPage_CheckboxName_QualifyWithDeclaringClass, CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpModifyDialog.FALSE_TRUE);
    	
		intent(staticGroup);
		final CheckboxPreference staticFieldPref= createCheckboxPref(staticGroup, numColumns - 1, CleanUpMessages.MemberAccessesTabPage_CheckboxName_QualifyFieldWithDeclaringClass, CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD, CleanUpModifyDialog.FALSE_TRUE);
		
		intent(staticGroup);
		final CheckboxPreference staticMethodPref= createCheckboxPref(staticGroup, numColumns - 1, CleanUpMessages.MemberAccessesTabPage_CheckboxName_QualifyMethodWithDeclaringClass, CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD, CleanUpModifyDialog.FALSE_TRUE);

		intent(staticGroup);
		final CheckboxPreference accessesThroughSubtypesPref= createCheckboxPref(staticGroup, numColumns - 1, CleanUpMessages.MemberAccessesTabPage_CheckboxName_ChangeAccessesThroughSubtypes, CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpModifyDialog.FALSE_TRUE);

		intent(staticGroup);
		final CheckboxPreference accessesThroughInstancesPref= createCheckboxPref(staticGroup, numColumns - 1, CleanUpMessages.MemberAccessesTabPage_CheckboxName_ChangeAccessesThroughInstances, CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpModifyDialog.FALSE_TRUE);

    	staticMemberPref.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			staticFieldPref.setEnabled(staticMemberPref.getChecked());
    			staticMethodPref.setEnabled(staticMemberPref.getChecked());
    			accessesThroughSubtypesPref.setEnabled(staticMemberPref.getChecked());
    			accessesThroughInstancesPref.setEnabled(staticMemberPref.getChecked());
    		}
    		
    	});

		staticFieldPref.setEnabled(staticMemberPref.getChecked());
		staticMethodPref.setEnabled(staticMemberPref.getChecked());
		accessesThroughSubtypesPref.setEnabled(staticMemberPref.getChecked());
		accessesThroughInstancesPref.setEnabled(staticMemberPref.getChecked());
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