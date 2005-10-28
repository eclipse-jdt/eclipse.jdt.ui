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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Create fixes which can remove unused code
 * @see org.eclipse.jdt.internal.corext.fix.UnusedCodeFix
 *
 */
public class UnusedCodeMultiFix extends AbstractMultiFix {

	private static final String REMOVE_UNUSED_LOCAL_VARIABLE_MULTI_FIX_DESCRIPTION= MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedVariable_description;
	private static final String REMOVE_UNUSED_FIELD_MULTI_FIX_DESCRIPTION= MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedField_description;
	private static final String REMOVE_UNUSED_PRIVATE_TYPES_MULTI_FIX_DESCRIPTION= MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedType_description;
	private static final String REMOVE_UNUSED_PRIVATE_CONSTRUCTORS_MULTI_FIX_DESCRIPTION= MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedConstructor_description;
	private static final String REMOVE_UNUSED_PRIVATE_METHODS_MULTI_FIX_DESCRIPTION= MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedMethod_description;
	private static final String REMOVE_UNUSED_IMPORT_MULTI_FIX_DESCRIPTION= MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedImport_description;
	
	private static final String REMOVE_UNUSED_PRIVATE_CONSTRUCTORS_SETTINGS_ID= "RemoveUnusedPrivateConstructors"; //$NON-NLS-1$
	private static final String REMOVE_UNUSED_IMPORTS_SETTINGS_ID= "RemoveUnusedImports"; //$NON-NLS-1$
	private static final String REMOVE_UNUSED_PRIVATE_METHODES_SETTINGS_ID= "RemoveUnusedPrivateMethods"; //$NON-NLS-1$
	private static final String REMOVE_UNUSED_PRIVATE_FIELDS_SETTINGS_ID= "RemoveUnusedPrivateFields"; //$NON-NLS-1$
	private static final String REMOVE_UNUSED_PRIVATE_TYPE_SETTINGS_ID= "RemoveUnusedPrivateType"; //$NON-NLS-1$
	private static final String REMOVE_UNUSED_LOCAL_VARIABLES_SETTINGS_ID= "RemoveUnusedLocals"; //$NON-NLS-1$
	
	private boolean fRemoveUnusedImports;
	private boolean fRemoveUnusedPrivateMethods;
	private boolean fRemoveUnusedPrivateConstructors;
	private boolean fRemoveUnusedPrivateFields;
	private boolean fRemoveUnusedPrivateTypes;
	private boolean fRemoveUnusedLocalVariables;

	public UnusedCodeMultiFix(boolean removeUnusedImports, boolean removeUnusedPrivateMethods, 
			boolean removeUnusedPrivateConstructors, boolean removeUnusedPrivateFields,
			boolean removeUnusedPrivateTypes, boolean removeUnusedLocalVariables) {
		
		fRemoveUnusedImports= removeUnusedImports;
		fRemoveUnusedPrivateMethods= removeUnusedPrivateMethods;
		fRemoveUnusedPrivateConstructors= removeUnusedPrivateConstructors;
		fRemoveUnusedPrivateFields= removeUnusedPrivateFields;
		fRemoveUnusedPrivateTypes= removeUnusedPrivateTypes;
		fRemoveUnusedLocalVariables= removeUnusedLocalVariables;
	}

	public UnusedCodeMultiFix(IDialogSettings settings) {
		this(	settings.getBoolean(REMOVE_UNUSED_IMPORTS_SETTINGS_ID), 
				settings.getBoolean(REMOVE_UNUSED_PRIVATE_METHODES_SETTINGS_ID),
				settings.getBoolean(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS_SETTINGS_ID),
				settings.getBoolean(REMOVE_UNUSED_PRIVATE_FIELDS_SETTINGS_ID),
				settings.getBoolean(REMOVE_UNUSED_PRIVATE_TYPE_SETTINGS_ID),
				settings.getBoolean(REMOVE_UNUSED_LOCAL_VARIABLES_SETTINGS_ID)
				);
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		
		List/*<ImportDeclaration>*/ removeImports= new ArrayList();
		List/*<SimpleName>*/ removeNames= new ArrayList();

		IProblem[] problems= compilationUnit.getProblems();

		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= getProblemLocation(problems[i]);
			if (fRemoveUnusedImports && problem.getProblemId() == IProblem.UnusedImport) {
				
				ImportDeclaration node= UnusedCodeFix.getImportDeclaration(problem, compilationUnit);
				
				if (node != null) {
					removeImports.add(node);
				}
			}
			if (
					(fRemoveUnusedPrivateMethods && problem.getProblemId() == IProblem.UnusedPrivateMethod) ||
					(fRemoveUnusedPrivateConstructors && problem.getProblemId() == IProblem.UnusedPrivateConstructor) ||
					(fRemoveUnusedPrivateFields && problem.getProblemId() == IProblem.UnusedPrivateField) ||
					(fRemoveUnusedPrivateTypes && problem.getProblemId() == IProblem.UnusedPrivateType) ||
					(fRemoveUnusedLocalVariables && problem.getProblemId() == IProblem.LocalVariableIsNeverUsed)
					) {
				SimpleName name= UnusedCodeFix.getUnusedName(compilationUnit, problem);
				if (name != null) {
					IBinding binding= name.resolveBinding();
					if (binding != null) {
						removeNames.add(name);
					}
				}
			}
		}
		
		if (removeImports.size() == 0 && removeNames.size() == 0)
			return null;
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		ImportDeclaration[] imports= (ImportDeclaration[])removeImports.toArray(new ImportDeclaration[removeImports.size()]);
		SimpleName[] names= (SimpleName[])removeNames.toArray(new SimpleName[removeNames.size()]);
		return new UnusedCodeFix("", cu, imports, names, (names.length == 0)?null:compilationUnit); //$NON-NLS-1$
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (fRemoveUnusedImports) {
			options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.WARNING);
		}
		if (fRemoveUnusedPrivateMethods || fRemoveUnusedPrivateConstructors || fRemoveUnusedPrivateFields || fRemoveUnusedPrivateTypes) {
			options.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.WARNING);
		}
		if (fRemoveUnusedLocalVariables) {
			options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.WARNING);
		}
		return options;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		Button removeUnusedImport= new Button(composite, SWT.CHECK);
		removeUnusedImport.setText(REMOVE_UNUSED_IMPORT_MULTI_FIX_DESCRIPTION);
		removeUnusedImport.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removeUnusedImport.setSelection(fRemoveUnusedImports);
		removeUnusedImport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRemoveUnusedImports= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button removePrivateMethods= new Button(composite, SWT.CHECK);
		removePrivateMethods.setText(REMOVE_UNUSED_PRIVATE_METHODS_MULTI_FIX_DESCRIPTION);
		removePrivateMethods.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removePrivateMethods.setSelection(fRemoveUnusedPrivateMethods);
		removePrivateMethods.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRemoveUnusedPrivateMethods= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button removePrivateConstructors= new Button(composite, SWT.CHECK);
		removePrivateConstructors.setText(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS_MULTI_FIX_DESCRIPTION);
		removePrivateConstructors.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removePrivateConstructors.setSelection(fRemoveUnusedPrivateConstructors);
		removePrivateConstructors.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRemoveUnusedPrivateConstructors= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button removePrivateType= new Button(composite, SWT.CHECK);
		removePrivateType.setText(REMOVE_UNUSED_PRIVATE_TYPES_MULTI_FIX_DESCRIPTION);
		removePrivateType.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removePrivateType.setSelection(fRemoveUnusedPrivateTypes);
		removePrivateType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRemoveUnusedPrivateTypes= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button removePrivateFields= new Button(composite, SWT.CHECK);
		removePrivateFields.setText(REMOVE_UNUSED_FIELD_MULTI_FIX_DESCRIPTION);
		removePrivateFields.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removePrivateFields.setSelection(fRemoveUnusedPrivateFields);
		removePrivateFields.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRemoveUnusedPrivateFields= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button removeLocals= new Button(composite, SWT.CHECK);
		removeLocals.setText(REMOVE_UNUSED_LOCAL_VARIABLE_MULTI_FIX_DESCRIPTION);
		removeLocals.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removeLocals.setSelection(fRemoveUnusedLocalVariables);
		removeLocals.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRemoveUnusedLocalVariables= ((Button)e.getSource()).getSelection();
			}
		});
		
		return composite;
	}

	public void saveSettings(IDialogSettings settings) {
		settings.put(REMOVE_UNUSED_IMPORTS_SETTINGS_ID, fRemoveUnusedImports);
		settings.put(REMOVE_UNUSED_PRIVATE_METHODES_SETTINGS_ID, fRemoveUnusedPrivateMethods);
		settings.put(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS_SETTINGS_ID, fRemoveUnusedPrivateConstructors);
		settings.put(REMOVE_UNUSED_PRIVATE_FIELDS_SETTINGS_ID, fRemoveUnusedPrivateFields);
		settings.put(REMOVE_UNUSED_PRIVATE_TYPE_SETTINGS_ID, fRemoveUnusedPrivateTypes);
		settings.put(REMOVE_UNUSED_LOCAL_VARIABLES_SETTINGS_ID, fRemoveUnusedLocalVariables);
	}

}
