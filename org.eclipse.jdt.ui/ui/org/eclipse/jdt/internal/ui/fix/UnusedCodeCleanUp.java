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
package org.eclipse.jdt.internal.ui.fix;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
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
	
	private static final int DEFAULT_FLAG= REMOVE_UNUSED_IMPORTS;
	private static final String SECTION_NAME= "CleanUp_UnusedCode"; //$NON-NLS-1$
	
	public UnusedCodeCleanUp(int flag) {
		super(flag);
	}

	public UnusedCodeCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}
	
	public UnusedCodeCleanUp(Map options) {
		super(options);
	}
	
	public UnusedCodeCleanUp() {
		super();
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
				false);
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
				false);
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
		return (String[])result.toArray(new String[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		if (isFlag(REMOVE_UNUSED_IMPORTS)) {
		} else {
			buf.append("import pack.Bar;\n"); //$NON-NLS-1$
		}
		buf.append("class Example {\n"); //$NON-NLS-1$
		if (isFlag(REMOVE_UNUSED_PRIVATE_TYPES)) {
		} else {
			buf.append("    private class Sub {}\n"); //$NON-NLS-1$
		}
		if (isFlag(REMOVE_UNUSED_PRIVATE_CONSTRUCTORS)) {
		} else {
			buf.append("    private Example() {}\n"); //$NON-NLS-1$
		}
		if (isFlag(REMOVE_UNUSED_PRIVATE_FIELDS)) {
		} else {
			buf.append("    private int fField;\n"); //$NON-NLS-1$
		}
		if (isFlag(REMOVE_UNUSED_PRIVATE_METHODS)) {
		} else {
			buf.append("    private void foo() {}\n"); //$NON-NLS-1$
		}
		buf.append("    public void bar() {\n"); //$NON-NLS-1$
		if (isFlag(REMOVE_UNUSED_LOCAL_VARIABLES)) {
		} else {
			buf.append("        int i= 10;\n"); //$NON-NLS-1$
		}
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		
		return buf.toString();
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
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDefaultFlag() {
		return DEFAULT_FLAG;
	}
	
    protected int createFlag(Map options) {
    	int result= 0;
    	
    	if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS))) {
    		result|= REMOVE_UNUSED_IMPORTS;
    	}
    	if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS))) {
    		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS))) {
    			result|= REMOVE_UNUSED_PRIVATE_CONSTRUCTORS;
    		}
    		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS))) {
    			result|= REMOVE_UNUSED_PRIVATE_FIELDS;
    		}
    		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS))) {
    			result|= REMOVE_UNUSED_PRIVATE_METHODS;
    		}
    		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES))) {
    			result|= REMOVE_UNUSED_PRIVATE_TYPES;
    		}
    	}
    	if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES))) {
    		result|= REMOVE_UNUSED_LOCAL_VARIABLES;
    	}
    	
	    return result;
    }


}
