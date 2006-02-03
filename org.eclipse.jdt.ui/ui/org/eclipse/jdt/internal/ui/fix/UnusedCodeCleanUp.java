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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

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
	
	/**
	 * Removes unused casts.
	 */
	public static final int REMOVE_UNUSED_CAST= 64;
	
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
				isFlag(REMOVE_UNUSED_IMPORTS),
				isFlag(REMOVE_UNUSED_CAST));
	}
	

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return UnusedCodeFix.createCleanUp(compilationUnit, problems,
				isFlag(REMOVE_UNUSED_PRIVATE_METHODS), 
				isFlag(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS), 
				isFlag(REMOVE_UNUSED_PRIVATE_FIELDS), 
				isFlag(REMOVE_UNUSED_PRIVATE_TYPES), 
				isFlag(REMOVE_UNUSED_LOCAL_VARIABLES), 
				isFlag(REMOVE_UNUSED_IMPORTS),
				isFlag(REMOVE_UNUSED_CAST));
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		
		if (isFlag(REMOVE_UNUSED_IMPORTS))
			options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.WARNING);

		if (isFlag(REMOVE_UNUSED_PRIVATE_METHODS | REMOVE_UNUSED_PRIVATE_CONSTRUCTORS | REMOVE_UNUSED_PRIVATE_FIELDS | REMOVE_UNUSED_PRIVATE_TYPES))
			options.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.WARNING);
		
		if (isFlag(REMOVE_UNUSED_LOCAL_VARIABLES))
			options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.WARNING);
		
		if (isFlag(REMOVE_UNUSED_CAST))
			options.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.WARNING);

		return options;
	}

	public Control createConfigurationControl(Composite composite, IJavaProject project) {

		indent(addCheckBox(composite, REMOVE_UNUSED_IMPORTS, MultiFixMessages.UnusedCodeCleanUp_unusedImports_checkBoxLabel));
		
			Button button= new Button(composite, SWT.CHECK);
			button.setText(MultiFixMessages.UnusedCodeCleanUp_unusedPrivateMembers_checkBoxLabel);
			button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		indent(button);
			
		Composite sub= new Composite(composite, SWT.NONE);
		sub.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout layout= new GridLayout(1, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		sub.setLayout(layout);
		indent(sub);
		
			final int[] flags= new int[] {REMOVE_UNUSED_PRIVATE_TYPES, REMOVE_UNUSED_PRIVATE_CONSTRUCTORS, REMOVE_UNUSED_PRIVATE_METHODS, REMOVE_UNUSED_PRIVATE_FIELDS};
			final int[] uiFlags= new int[] {1073741824, 536870912, 268435456, 134217728};
			final String[] labels= new String[] {MultiFixMessages.UnusedCodeCleanUp_unusedTypes_checkBoxLabel, MultiFixMessages.UnusedCodeCleanUp_unusedConstructors_checkBoxLabel, MultiFixMessages.UnusedCodeCleanUp_unusedMethods_checkBoxLabel, MultiFixMessages.UnusedCodeCleanUp_unusedFields_checkBoxLabel};
		
			createSubGroup(sub, button, SWT.CHECK, flags, labels, uiFlags, false);
		
		indent(addCheckBox(composite, REMOVE_UNUSED_LOCAL_VARIABLES, MultiFixMessages.UnusedCodeCleanUp_unusedLocalVariables_checkBoxLabel));	
		indent(addCheckBox(composite, REMOVE_UNUSED_CAST, MultiFixMessages.UnusedCodeCleanUp_unnecessaryCasts_checkBoxLabel));
		
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
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedImport_description);
		if (isFlag(REMOVE_UNUSED_PRIVATE_METHODS))
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedMethod_description);
		if (isFlag(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS))
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedConstructor_description);
		if (isFlag(REMOVE_UNUSED_PRIVATE_TYPES))
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedType_description);
		if (isFlag(REMOVE_UNUSED_PRIVATE_FIELDS))
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedField_description);
		if (isFlag(REMOVE_UNUSED_LOCAL_VARIABLES))
			result.add(MultiFixMessages.UnusedCodeMultiFix_RemoveUnusedVariable_description);
		if (isFlag(REMOVE_UNUSED_CAST))
			result.add(MultiFixMessages.UnusedCodeCleanUp_RemoveUnusedCasts_description);
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isFlag(REMOVE_UNUSED_IMPORTS)) {
			UnusedCodeFix fix= UnusedCodeFix.createRemoveUnusedImportFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		if (isFlag(REMOVE_UNUSED_PRIVATE_METHODS) ||
				isFlag(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS) ||
				isFlag(REMOVE_UNUSED_PRIVATE_TYPES) ||
				isFlag(REMOVE_UNUSED_PRIVATE_FIELDS) ||
				isFlag(REMOVE_UNUSED_LOCAL_VARIABLES)) 
		{
			UnusedCodeFix fix= UnusedCodeFix.createUnusedMemberFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		if (isFlag(REMOVE_UNUSED_CAST)) {
			IFix fix= UnusedCodeFix.createRemoveUnusedCastFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isFlag(REMOVE_UNUSED_IMPORTS)) {
			for (int i=0;i<problems.length;i++) {
				int id= problems[i].getID();
				if (id == IProblem.UnusedImport || id == IProblem.DuplicateImport || id == IProblem.ConflictingImport ||
					    id == IProblem.CannotImportPackage || id == IProblem.ImportNotFound)
					result++;
			}
		}
		if (isFlag(REMOVE_UNUSED_PRIVATE_METHODS))
			result+= getNumberOfProblems(problems, IProblem.UnusedPrivateMethod);
		if (isFlag(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS))
			result+= getNumberOfProblems(problems, IProblem.UnusedPrivateConstructor);
		if (isFlag(REMOVE_UNUSED_PRIVATE_TYPES))
			result+= getNumberOfProblems(problems, IProblem.UnusedPrivateType);
		if (isFlag(REMOVE_UNUSED_PRIVATE_FIELDS))
			result+= getNumberOfProblems(problems, IProblem.UnusedPrivateField);
		if (isFlag(REMOVE_UNUSED_LOCAL_VARIABLES))
			result+= getNumberOfProblems(problems, IProblem.LocalVariableIsNeverUsed);
		if (isFlag(REMOVE_UNUSED_CAST))
			result+= getNumberOfProblems(problems, IProblem.UnnecessaryCast);
		return result;
	}

}
