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

import org.eclipse.text.edits.TextEdit;

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

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.StringFix;
import org.eclipse.jdt.internal.corext.fix.TextChangeFix;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;

/**
 * Create fixes which can solve problems in connection with Strings
 * @see org.eclipse.jdt.internal.corext.fix.StringFix
 *
 */
public class StringMultiFix extends AbstractMultiFix {

	private static final String REMOVE_NLS_TAG_SETTINGS_ID= "RemoveNlsTag"; //$NON-NLS-1$
	private static final String ADD_NLS_TAG_SETTINGS_ID= "AddNlsTag"; //$NON-NLS-1$
	
	private boolean fAddNlsTag;
	private boolean fRemoveNlsTag;

	public StringMultiFix(boolean addNLSTag, boolean removeNLSTag) {
		init(addNLSTag, removeNLSTag);
	}

	public StringMultiFix(IDialogSettings settings) {
		if (settings.get(ADD_NLS_TAG_SETTINGS_ID) == null) {
			settings.put(ADD_NLS_TAG_SETTINGS_ID, false);
		}
		if (settings.get(REMOVE_NLS_TAG_SETTINGS_ID) == null) {
			settings.put(REMOVE_NLS_TAG_SETTINGS_ID, true);
		}
		init(	settings.getBoolean(ADD_NLS_TAG_SETTINGS_ID), 
				settings.getBoolean(REMOVE_NLS_TAG_SETTINGS_ID));
	}

	private void init(boolean addNLSTag, boolean removeNLSTag) {
		fAddNlsTag= addNLSTag;
		fRemoveNlsTag= removeNLSTag;
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		if (!fAddNlsTag && !fRemoveNlsTag)
			return null;
		
		IProblem[] problems= compilationUnit.getProblems();
		
		if (problems.length == 0)
			return null;
		
		CompilationUnitChange result= null;
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			if (fAddNlsTag && problem.getID() == IProblem.NonExternalizedStringLiteral) {
				TextEdit edit= NLSUtil.createNLSEdit(cu, problem.getSourceStart());
				if (edit != null) {
					if (result == null) 
						result= new CompilationUnitChange("", cu); //$NON-NLS-1$
					TextChangeCompatibility.addTextEdit(result, MultiFixMessages.StringMultiFix_AddMissingNonNls_description, edit);
				}
			}
			if (fRemoveNlsTag && problem.getID() == IProblem.UnnecessaryNLSTag) {
				IBuffer buffer= cu.getBuffer();
				if (buffer != null) {
					TextEdit edit= StringFix.getReplace(problem.getSourceStart(), problem.getSourceEnd() - problem.getSourceStart() + 1, buffer, false);
					if (edit != null) {
						if (result == null)
							result= new CompilationUnitChange("", cu); //$NON-NLS-1$
						TextChangeCompatibility.addTextEdit(result, MultiFixMessages.StringMultiFix_RemoveUnnecessaryNonNls_description, edit);
					}
				}
			}
		}
		if (result == null)
			return null;
		
		return new TextChangeFix("", cu, result); //$NON-NLS-1$
	}

	public Map getRequiredOptions() {
		Map result= new Hashtable();
		if (fAddNlsTag || fRemoveNlsTag)
			result.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		return result;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		Button addNLSTag= new Button(composite, SWT.CHECK);
		addNLSTag.setText(MultiFixMessages.StringMultiFix_AddMissingNonNls_description);
		addNLSTag.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		addNLSTag.setSelection(fAddNlsTag);
		addNLSTag.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAddNlsTag= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button removeNLSTag= new Button(composite, SWT.CHECK);
		removeNLSTag.setText(MultiFixMessages.StringMultiFix_RemoveUnnecessaryNonNls_description);
		removeNLSTag.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removeNLSTag.setSelection(fRemoveNlsTag);
		removeNLSTag.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRemoveNlsTag= ((Button)e.getSource()).getSelection();
			}
		});
		
		return composite;
	}

	public void saveSettings(IDialogSettings settings) {
		settings.put(ADD_NLS_TAG_SETTINGS_ID, fAddNlsTag);
		settings.put(REMOVE_NLS_TAG_SETTINGS_ID, fRemoveNlsTag);
	}

}
