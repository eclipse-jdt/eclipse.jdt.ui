/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.ExpressionsFix;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class ExpressionsCleanUp extends AbstractCleanUp {
	
	/**
	 * Add paranoic parenthesis around conditional expressions.<p>
	 * i.e.:<pre><code>
	 * boolean b= i > 10 && i < 100 || i > 20;
	 * ->
	 * boolean b= ((i > 10) && (i < 100)) || (i > 20);</pre></code>
	 */
	public static final int ADD_PARANOIC_PARENTHESIS= 1;
	
	/**
	 * Remove unnecessary parenthesis around conditional expressions.<p>
	 * i.e.:<pre><code>
	 * boolean b= ((i > 10) && (i < 100)) || (i > 20);
	 * ->
	 * boolean b= i > 10 && i < 100 || i > 20;</pre></code>
	 */
	public static final int REMOVE_UNNECESSARY_PARENTHESIS= 2;

	private static final int DEFAULT_FLAG= 0;
	private static final String SECTION_NAME= "CleanUp_Expressions"; //$NON-NLS-1$

	private Button[] fButtons;

	public ExpressionsCleanUp(int flag) {
		super(flag);
	}

	public ExpressionsCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return ExpressionsFix.createCleanUp(compilationUnit, 
				isFlag(ADD_PARANOIC_PARENTHESIS),
				isFlag(REMOVE_UNNECESSARY_PARENTHESIS));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		return createFix(compilationUnit);
	}

	public Map getRequiredOptions() {
		return new Hashtable();
	}

	public Control createConfigurationControl(Composite parent, IJavaProject project) {
		fButtons= new Button[3];
		
		Button button= new Button(parent, SWT.CHECK);
		button.setText(MultiFixMessages.ExpressionsCleanUp_parenthesisAroundConditions_checkBoxLabel);
		button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		fButtons[0]= button;

		final int[] flags= new int[] {ADD_PARANOIC_PARENTHESIS, REMOVE_UNNECESSARY_PARENTHESIS};
		final int[] uiFlags= new int[] {1073741824, 536870912};
		final String[] labels= new String[] {MultiFixMessages.ExpressionsCleanUp_addParanoiac_checkBoxLabel, MultiFixMessages.ExpressionsCleanUp_removeUnnecessary_checkBoxLabel};
	
		Button[] buttons= createSubGroup(parent, button, SWT.RADIO, flags, labels, uiFlags, false);
		for (int i= 0; i < buttons.length; i++) {
			fButtons[i+1]= buttons[i];
		}
		
		return parent;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void select(int flags) {
		if (fButtons == null)
			return;
		
		if (((flags & ADD_PARANOIC_PARENTHESIS) != 0) && ((flags & REMOVE_UNNECESSARY_PARENTHESIS) != 0)) {
			setFlag(ADD_PARANOIC_PARENTHESIS, fButtons[1].getSelection());
			setFlag(REMOVE_UNNECESSARY_PARENTHESIS, fButtons[2].getSelection());
			fButtons[0].setSelection(true);
			fButtons[1].setEnabled(true);
			fButtons[2].setEnabled(true);
		} else if ((flags & ADD_PARANOIC_PARENTHESIS) != 0) {
			setFlag(ADD_PARANOIC_PARENTHESIS, true);
			setFlag(REMOVE_UNNECESSARY_PARENTHESIS, false);
			fButtons[0].setSelection(true);
			fButtons[1].setSelection(true);
			fButtons[2].setSelection(false);
			fButtons[1].setEnabled(true);
			fButtons[2].setEnabled(true);
		} else if ((flags & REMOVE_UNNECESSARY_PARENTHESIS) != 0) {
			setFlag(ADD_PARANOIC_PARENTHESIS, false);
			setFlag(REMOVE_UNNECESSARY_PARENTHESIS, true);
			fButtons[0].setSelection(true);
			fButtons[1].setSelection(false);
			fButtons[2].setSelection(true);
			fButtons[1].setEnabled(true);
			fButtons[2].setEnabled(true);
		} else {
			setFlag(ADD_PARANOIC_PARENTHESIS, false);
			setFlag(REMOVE_UNNECESSARY_PARENTHESIS, false);
			fButtons[0].setSelection(false);
			fButtons[1].setEnabled(false);
			fButtons[2].setEnabled(false);
		}
	}

	public void saveSettings(IDialogSettings settings) {
		super.saveSettings(getSection(settings, SECTION_NAME));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isFlag(ADD_PARANOIC_PARENTHESIS)) 
			result.add(MultiFixMessages.ExpressionsCleanUp_addParanoiac_description);
		
		if (isFlag(REMOVE_UNNECESSARY_PARENTHESIS)) 
			result.add(MultiFixMessages.ExpressionsCleanUp_removeUnnecessary_description);
		
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isFlag(ADD_PARANOIC_PARENTHESIS)) {
			IFix fix= ExpressionsFix.createAddParanoidalParenthesisFix(compilationUnit, new ASTNode[] {problem.getCoveredNode(compilationUnit)});
			if (fix != null)
				return true;
		}
		if (isFlag(REMOVE_UNNECESSARY_PARENTHESIS)) {
			IFix fix= ExpressionsFix.createRemoveUnnecessaryParenthesisFix(compilationUnit, new ASTNode[] {problem.getCoveredNode(compilationUnit)});
			if (fix != null)
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
		return -1;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDefaultFlag() {
		return DEFAULT_FLAG;
	}
}
