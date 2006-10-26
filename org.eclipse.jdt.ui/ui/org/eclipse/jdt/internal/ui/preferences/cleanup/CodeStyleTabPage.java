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

import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;
import org.eclipse.jdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialog;

public final class CodeStyleTabPage extends ModifyDialogTabPage {
	
    private final Map fValues;

    private CleanUpPreview fCleanUpPreview;

    public CodeStyleTabPage(ModifyDialog dialog, Map values) {
	    super(dialog, values);
	    fValues= values;
    }

    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fCleanUpPreview= new CleanUpPreview(parent, new ICleanUp[] {
        		new ControlStatementsCleanUp(fValues),
        		new ExpressionsCleanUp(fValues),
        		new VariableDeclarationCleanUp(fValues)
        }, false);
    	return fCleanUpPreview;
    }

    protected void doCreatePreferences(Composite composite, int numColumns) {
    	
    	Group controlGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_ControlStatments);
    	final CheckboxPreference useBlockPref= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseBlocks, CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS, CleanUpModifyDialog.FALSE_TRUE);

    	intent(controlGroup);
		final RadioPreference useBlockAlwaysPref= createRadioPref(controlGroup, numColumns - 1, CleanUpMessages.CodeStyleTabPage_RadioName_AlwaysUseBlocks, CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		
		intent(controlGroup);
		final RadioPreference useBlockJDTStylePref= createRadioPref(controlGroup, numColumns - 1, CleanUpMessages.CodeStyleTabPage_RadioName_UseBlocksSpecial, CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW, CleanUpModifyDialog.FALSE_TRUE);
		
		intent(controlGroup);
		final RadioPreference useBlockNeverPref= createRadioPref(controlGroup, numColumns - 1, CleanUpMessages.CodeStyleTabPage_RadioName_NeverUseBlocks, CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER, CleanUpModifyDialog.FALSE_TRUE);
		
    	useBlockPref.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			useBlockAlwaysPref.setEnabled(useBlockPref.getChecked());
    			useBlockJDTStylePref.setEnabled(useBlockPref.getChecked());
    			useBlockNeverPref.setEnabled(useBlockPref.getChecked());
    		}
    		
    	});
    	
		useBlockAlwaysPref.setEnabled(useBlockPref.getChecked());
		useBlockJDTStylePref.setEnabled(useBlockPref.getChecked());
		useBlockNeverPref.setEnabled(useBlockPref.getChecked());
    	
    	createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_ConvertForLoopToEnhanced, CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpModifyDialog.FALSE_TRUE);
    	
    	Group expressionsGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_Expressions);
    	final CheckboxPreference useParenthesesPref= createCheckboxPref(expressionsGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseParentheses, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, CleanUpModifyDialog.FALSE_TRUE);
    	
		intent(expressionsGroup);
		final RadioPreference useParenthesesAlwaysPref= createRadioPref(expressionsGroup, 1, CleanUpMessages.CodeStyleTabPage_RadioName_AlwaysUseParantheses, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		final RadioPreference useParenthesesNeverPref= createRadioPref(expressionsGroup, 1, CleanUpMessages.CodeStyleTabPage_RadioName_NeverUseParantheses, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER, CleanUpModifyDialog.FALSE_TRUE);
    	
    	useParenthesesPref.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			useParenthesesAlwaysPref.setEnabled(useParenthesesPref.getChecked());
    			useParenthesesNeverPref.setEnabled(useParenthesesPref.getChecked());
    		}
    		
    	});

		useParenthesesAlwaysPref.setEnabled(useParenthesesPref.getChecked());
		useParenthesesNeverPref.setEnabled(useParenthesesPref.getChecked());

		Group variableGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_VariableDeclarations);
    	final CheckboxPreference useFinalPref= createCheckboxPref(variableGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinal, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, CleanUpModifyDialog.FALSE_TRUE);
    	
		intent(variableGroup);
		final CheckboxPreference useFinalFieldsPref= createCheckboxPref(variableGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinalForFields, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference useFinalParametersPref= createCheckboxPref(variableGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinalForParameters, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference useFinalVariablesPref= createCheckboxPref(variableGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinalForLocals, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpModifyDialog.FALSE_TRUE);
    	
    	useFinalPref.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			useFinalFieldsPref.setEnabled(useFinalPref.getChecked());
    			useFinalParametersPref.setEnabled(useFinalPref.getChecked());
    			useFinalVariablesPref.setEnabled(useFinalPref.getChecked());
    		}
    		
    	});

		useFinalFieldsPref.setEnabled(useFinalPref.getChecked());
		useFinalParametersPref.setEnabled(useFinalPref.getChecked());
		useFinalVariablesPref.setEnabled(useFinalPref.getChecked());
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