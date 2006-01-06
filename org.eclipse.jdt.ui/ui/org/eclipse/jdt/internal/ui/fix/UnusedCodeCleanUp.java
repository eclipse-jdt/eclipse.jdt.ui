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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;

/**
 * Create fixes which can remove unused code
 * @see org.eclipse.jdt.internal.corext.fix.UnusedCodeFix
 *
 */
public class UnusedCodeCleanUp extends AbstractCleanUp {
	
	/**
	 * Removes unused imports.
	 */
	public static final int REMOVE_UNUSED_IMPORTS= 1;
	
	/**
	 * Removes unused private constructors.
	 */
	public static final int REMOVE_UNUSED_PRIVATE_CONSTRUCTORS= 2;
	
	/**
	 * Removes unused private methods.
	 */
	public static final int REMOVE_UNUSED_PRIVATE_METHODS= 4;
	
	/**
	 * Removes unused private fields.
	 */
	public static final int REMOVE_UNUSED_PRIVATE_FIELDS= 8;
	
	/**
	 * Removes unused private types.
	 */
	public static final int REMOVE_UNUSED_PRIVATE_TYPES= 16;
	
	/**
	 * Removes unused local variables.
	 */
	public static final int REMOVE_UNUSED_LOCAL_VARIABLES= 32;
	
	private static final int DEFAULT_FLAG= REMOVE_UNUSED_IMPORTS;
	private static final String SECTION_NAME= "CleanUp_UnusedCode"; //$NON-NLS-1$
	
	public UnusedCodeCleanUp(int flag) {
		super(flag);
	}

	public UnusedCodeCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return UnusedCodeFix.createCleanUp(compilationUnit, 
				isFlag(REMOVE_UNUSED_PRIVATE_METHODS), 
				isFlag(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS), 
				isFlag(REMOVE_UNUSED_PRIVATE_FIELDS), 
				isFlag(REMOVE_UNUSED_PRIVATE_TYPES), 
				isFlag(REMOVE_UNUSED_LOCAL_VARIABLES), 
				isFlag(REMOVE_UNUSED_IMPORTS));
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		
		if (isFlag(REMOVE_UNUSED_IMPORTS))
			options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.WARNING);

		if (isFlag(REMOVE_UNUSED_PRIVATE_METHODS | REMOVE_UNUSED_PRIVATE_CONSTRUCTORS | REMOVE_UNUSED_PRIVATE_FIELDS | REMOVE_UNUSED_PRIVATE_TYPES))
			options.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.WARNING);
		
		if (isFlag(REMOVE_UNUSED_LOCAL_VARIABLES))
			options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.WARNING);

		return options;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		addCheckBox(composite, REMOVE_UNUSED_IMPORTS, MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedImport_description);
		addCheckBox(composite, REMOVE_UNUSED_PRIVATE_METHODS, MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedMethod_description);
		addCheckBox(composite, REMOVE_UNUSED_PRIVATE_CONSTRUCTORS, MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedConstructor_description);
		addCheckBox(composite, REMOVE_UNUSED_PRIVATE_TYPES, MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedType_description);
		addCheckBox(composite, REMOVE_UNUSED_PRIVATE_FIELDS, MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedField_description);
		addCheckBox(composite, REMOVE_UNUSED_LOCAL_VARIABLES, MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedVariable_description);
		
		return composite;
	}

	public void saveSettings(IDialogSettings settings) {
		super.saveSettings(getSection(settings, SECTION_NAME));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isFlag(REMOVE_UNUSED_IMPORTS))
			result.add(removeMemonic(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedImport_description));
		if (isFlag(REMOVE_UNUSED_PRIVATE_METHODS))
			result.add(removeMemonic(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedMethod_description));
		if (isFlag(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS))
			result.add(removeMemonic(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedConstructor_description));
		if (isFlag(REMOVE_UNUSED_PRIVATE_TYPES))
			result.add(removeMemonic(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedType_description));
		if (isFlag(REMOVE_UNUSED_PRIVATE_FIELDS))
			result.add(removeMemonic(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedField_description));
		if (isFlag(REMOVE_UNUSED_LOCAL_VARIABLES))
			result.add(removeMemonic(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedVariable_description));
		return (String[])result.toArray(new String[result.size()]);
	}

}
