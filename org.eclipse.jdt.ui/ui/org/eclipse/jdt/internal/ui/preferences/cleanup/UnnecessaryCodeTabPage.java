/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.TypeParametersCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantModifiersCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantSemicolonsCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;

public final class UnnecessaryCodeTabPage extends AbstractCleanUpTabPage {

	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.unnecessary_code"; //$NON-NLS-1$

    public UnnecessaryCodeTabPage() {
    	super();
    }

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
		return new AbstractCleanUp[] {
				new UnusedCodeCleanUp(values),
				new UnnecessaryCodeCleanUp(values),
				new StringCleanUp(values),
				new TypeParametersCleanUp(values),
				new RedundantModifiersCleanUp(values),
				new RedundantSemicolonsCleanUp(values)
		};
	}

    @Override
	protected void doCreatePreferences(Composite composite, int numColumns) {

    	Group unusedCodeGroup= createGroup(5, composite, CleanUpMessages.UnnecessaryCodeTabPage_GroupName_UnusedCode);

    	CheckboxPreference removeImports= createCheckboxPref(unusedCodeGroup, 5, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedImports, CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(removeImports);

    	final CheckboxPreference unusedMembersPref= createCheckboxPref(unusedCodeGroup, 5, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedMembers, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpModifyDialog.FALSE_TRUE);
		intent(unusedCodeGroup);
		final CheckboxPreference typesPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedTypes, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference constructorPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedConstructors, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference fieldsPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedFields, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference methodsPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedMethods, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(unusedMembersPref, new CheckboxPreference[] {typesPref, constructorPref, fieldsPref, methodsPref});

    	CheckboxPreference removeLocals= createCheckboxPref(unusedCodeGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedLocalVariables, CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(removeLocals);

    	Group unnecessaryGroup= createGroup(numColumns, composite, CleanUpMessages.UnnecessaryCodeTabPage_GroupName_UnnecessaryCode);

    	CheckboxPreference casts= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnnecessaryCasts, CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(casts);

    	CheckboxPreference nls= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnnecessaryNLSTags, CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(nls);

		CheckboxPreference typeArgs= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_RedundantTypeArguments, CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(typeArgs);

		CheckboxPreference modifiers= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_RedundantModifiers, CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(modifiers);

		CheckboxPreference semicolons= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_RedundantSemicolons, CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(semicolons);
    }

}
