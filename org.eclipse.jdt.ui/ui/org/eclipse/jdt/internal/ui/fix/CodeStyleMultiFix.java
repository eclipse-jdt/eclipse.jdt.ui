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

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CodeStyleFix;
import org.eclipse.jdt.internal.corext.fix.IFix;

/**
 * Creates fixes which can resolve code style issues 
 * @see org.eclipse.jdt.internal.corext.fix.CodeStyleFix
 */
public class CodeStyleMultiFix extends AbstractMultiFix {

	private static final String ADD_BLOCK_TO_CONTROL_STATEMENTS_SETTINGS_ID= "AddBlockToControlStatements"; //$NON-NLS-1$
	private static final String CHANGE_INDIRECT_STATIC_ACCESS_TO_STATIC_SETTINGS_ID= "ChangeIndirectStaticAccessToStatic"; //$NON-NLS-1$
	private static final String QUALIFY_STATIC_FIELD_ACCESS_SETTINGS_ID= "QualifyStaticFieldAccessWithDeclaringClass"; //$NON-NLS-1$
	private static final String CHANGE_NON_STATIC_ACCESS_TO_STATIC_SETTINGS_ID= "ChangeNonStaticAccessToStatic"; //$NON-NLS-1$
	private static final String ADD_THIS_QUALIFIER_SETTINGS_ID= "AddThisQualifier"; //$NON-NLS-1$
	
	private boolean fAddThisQualifier;
	private boolean fChangeNonStaticAccessToStatic;
	private boolean fQualifyStaticFieldAccessWithDeclaringClass;
	private boolean fChangeIndirectStaticAccessToDirect;
	private boolean fAddBlockToControlStatements;
	
	public CodeStyleMultiFix(boolean qualifyFieldAccess, 
			boolean changeNonStaticAccessToStatic, 
			boolean qualifyStaticFieldAccess, boolean changeIndirectStaticAccessToDirect, 
			boolean addBlockToControlStatements) {
		
		fAddThisQualifier= qualifyFieldAccess;
		fChangeNonStaticAccessToStatic= changeNonStaticAccessToStatic;
		fQualifyStaticFieldAccessWithDeclaringClass= qualifyStaticFieldAccess;
		fChangeIndirectStaticAccessToDirect= changeIndirectStaticAccessToDirect;
		fAddBlockToControlStatements= addBlockToControlStatements;
	}

	public CodeStyleMultiFix(IDialogSettings settings) {
		this(settings.getBoolean(ADD_THIS_QUALIFIER_SETTINGS_ID), 
				settings.getBoolean(CHANGE_NON_STATIC_ACCESS_TO_STATIC_SETTINGS_ID),
				settings.getBoolean(QUALIFY_STATIC_FIELD_ACCESS_SETTINGS_ID),
				settings.getBoolean(CHANGE_INDIRECT_STATIC_ACCESS_TO_STATIC_SETTINGS_ID),
				settings.getBoolean(ADD_BLOCK_TO_CONTROL_STATEMENTS_SETTINGS_ID));
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return CodeStyleFix.createCleanUp(compilationUnit, 
				fAddThisQualifier, 
				fChangeNonStaticAccessToStatic, 
				fQualifyStaticFieldAccessWithDeclaringClass, 
				fChangeIndirectStaticAccessToDirect,
				fAddBlockToControlStatements);
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (fChangeNonStaticAccessToStatic)
			options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.WARNING);
		if (fChangeIndirectStaticAccessToDirect)
			options.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.WARNING);
		return options;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		Button addThisQualifier= new Button(composite, SWT.CHECK);
		addThisQualifier.setText(MultiFixMessages.CodeStyleMultiFix_AddThisQualifier_description);
		addThisQualifier.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		addThisQualifier.setSelection(fAddThisQualifier);
		addThisQualifier.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAddThisQualifier= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button qualifyStaticAccess= new Button(composite, SWT.CHECK);
		qualifyStaticAccess.setText(MultiFixMessages.CodeStyleMultiFix_QualifyAccessToStaticField);
		qualifyStaticAccess.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		qualifyStaticAccess.setSelection(fQualifyStaticFieldAccessWithDeclaringClass);
		qualifyStaticAccess.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fQualifyStaticFieldAccessWithDeclaringClass= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button removeNonStaticAccess= new Button(composite, SWT.CHECK);
		removeNonStaticAccess.setText(MultiFixMessages.CodeStyleMultiFix_ChangeNonStaticAccess_description);
		removeNonStaticAccess.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removeNonStaticAccess.setSelection(fChangeNonStaticAccessToStatic);
		removeNonStaticAccess.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fChangeNonStaticAccessToStatic= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button indirectStaticAccess= new Button(composite, SWT.CHECK);
		indirectStaticAccess.setText(MultiFixMessages.CodeStyleMultiFix_ChangeIndirectAccessToStaticToDirect);
		indirectStaticAccess.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		indirectStaticAccess.setSelection(fChangeIndirectStaticAccessToDirect);
		indirectStaticAccess.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fChangeIndirectStaticAccessToDirect= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button addBlock= new Button(composite, SWT.CHECK);
		addBlock.setText(MultiFixMessages.CodeStyleMultiFix_ConvertSingleStatementInControlBodeyToBlock_description);
		addBlock.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		addBlock.setSelection(fAddBlockToControlStatements);
		addBlock.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAddBlockToControlStatements= ((Button)e.getSource()).getSelection();
			}
		});
		
		return composite;
	}

	public void saveSettings(IDialogSettings settings) {
		settings.put(ADD_THIS_QUALIFIER_SETTINGS_ID, fAddThisQualifier);
		settings.put(CHANGE_NON_STATIC_ACCESS_TO_STATIC_SETTINGS_ID, fChangeNonStaticAccessToStatic);
		settings.put(QUALIFY_STATIC_FIELD_ACCESS_SETTINGS_ID, fQualifyStaticFieldAccessWithDeclaringClass);
		settings.put(CHANGE_INDIRECT_STATIC_ACCESS_TO_STATIC_SETTINGS_ID, fChangeIndirectStaticAccessToDirect);
		settings.put(ADD_BLOCK_TO_CONTROL_STATEMENTS_SETTINGS_ID, fAddBlockToControlStatements);
	}

}
