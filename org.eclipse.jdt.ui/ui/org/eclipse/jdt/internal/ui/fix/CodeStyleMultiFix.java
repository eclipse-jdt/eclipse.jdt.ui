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

	private static final String CHANGE_NON_STATIC_ACCESS_TO_STATIC_SETTINGS_ID= "ChangeNonStaticAccessToStatic"; //$NON-NLS-1$
	private static final String ADD_THIS_QUALIFIER_SETTINGS_ID= "AddThisQualifier"; //$NON-NLS-1$
	
	private boolean fAddThisQualifier;
	private boolean fChangeNonStaticAccessToStatic;
	
	public CodeStyleMultiFix(boolean addThisQualifier, boolean changeNonStaticAccessToStatic) {
		fAddThisQualifier= addThisQualifier;
		fChangeNonStaticAccessToStatic= changeNonStaticAccessToStatic;
	}

	public CodeStyleMultiFix(IDialogSettings settings) {
		this(settings.getBoolean(ADD_THIS_QUALIFIER_SETTINGS_ID), settings.getBoolean(CHANGE_NON_STATIC_ACCESS_TO_STATIC_SETTINGS_ID));
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return CodeStyleFix.createCleanUp(compilationUnit, fAddThisQualifier, fChangeNonStaticAccessToStatic);
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (fChangeNonStaticAccessToStatic)
			options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.WARNING);
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
		
		Button removeNonStaticAccess= new Button(composite, SWT.CHECK);
		removeNonStaticAccess.setText(MultiFixMessages.CodeStyleMultiFix_ChangeNonStaticAccess_description);
		removeNonStaticAccess.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removeNonStaticAccess.setSelection(fChangeNonStaticAccessToStatic);
		removeNonStaticAccess.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fChangeNonStaticAccessToStatic= ((Button)e.getSource()).getSelection();
			}
		});
		
		return composite;
	}

	public void saveSettings(IDialogSettings settings) {
		settings.put(ADD_THIS_QUALIFIER_SETTINGS_ID, fAddThisQualifier);
		settings.put(CHANGE_NON_STATIC_ACCESS_TO_STATIC_SETTINGS_ID, fChangeNonStaticAccessToStatic);
	}

}
