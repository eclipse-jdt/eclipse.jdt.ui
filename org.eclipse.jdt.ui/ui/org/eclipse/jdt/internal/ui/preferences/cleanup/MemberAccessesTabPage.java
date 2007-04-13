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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialog;

public final class MemberAccessesTabPage extends CleanUpTabPage {

    public MemberAccessesTabPage(ModifyDialog dialog, Map values) {
	    this(dialog, values, false);
    }

    public MemberAccessesTabPage(IModificationListener listener, Map values, boolean isSaveParticipantConfiguration) {
    	super(listener, values, isSaveParticipantConfiguration);
    }
    
    protected ICleanUp[] createPreviewCleanUps(Map values) {
    	return new ICleanUp[] {
        	new CodeStyleCleanUp(values)		
        };
    }

    protected void doCreatePreferences(Composite composite, int numColumns) {
    	
    	Group instanceGroup= createGroup(numColumns, composite, CleanUpMessages.MemberAccessesTabPage_GroupName_NonStaticAccesses);
    	
    	final CheckboxPreference thisFieldPref= createCheckboxPref(instanceGroup, numColumns, CleanUpMessages.MemberAccessesTabPage_CheckboxName_FieldQualifier, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpModifyDialog.FALSE_TRUE);
    	intent(instanceGroup);
		final RadioPreference thisFieldAlwaysPref= createRadioPref(instanceGroup, 1, CleanUpMessages.MemberAccessesTabPage_RadioName_AlwaysThisForFields, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		final RadioPreference thisFieldNecessaryPref= createRadioPref(instanceGroup, 1, CleanUpMessages.MemberAccessesTabPage_RadioName_NeverThisForFields, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY, CleanUpModifyDialog.FALSE_TRUE);    	
		registerSlavePreference(thisFieldPref, new ButtonPreference[] {thisFieldAlwaysPref, thisFieldNecessaryPref});

    	final CheckboxPreference thisMethodPref= createCheckboxPref(instanceGroup, numColumns, CleanUpMessages.MemberAccessesTabPage_CheckboxName_MethodQualifier, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, CleanUpModifyDialog.FALSE_TRUE);    	
    	intent(instanceGroup);
		final RadioPreference thisMethodAlwaysPref= createRadioPref(instanceGroup, 1, CleanUpMessages.MemberAccessesTabPage_RadioName_AlwaysThisForMethods, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		final RadioPreference thisMethodNecessaryPref= createRadioPref(instanceGroup, 1, CleanUpMessages.MemberAccessesTabPage_RadioName_NeverThisForMethods, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(thisMethodPref, new ButtonPreference[] {thisMethodAlwaysPref, thisMethodNecessaryPref});
    	
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
		registerSlavePreference(staticMemberPref, new ButtonPreference[] {staticFieldPref, staticMethodPref, accessesThroughSubtypesPref, accessesThroughInstancesPref});
    }
}