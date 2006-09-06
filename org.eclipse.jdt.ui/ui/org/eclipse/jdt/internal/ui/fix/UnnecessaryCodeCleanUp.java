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

public class UnnecessaryCodeCleanUp extends AbstractCleanUp {
	
	/**
	 * Removes unused casts.
	 */
	public static final int REMOVE_UNUSED_CAST= 1;
	
	private static final int DEFAULT_FLAG= REMOVE_UNUSED_CAST;
	private static final String SECTION_NAME= "CleanUp_UnnecessaryCode"; //$NON-NLS-1$

	public UnnecessaryCodeCleanUp(int flag) {
		super(flag);
	}

	public UnnecessaryCodeCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}
	
	public UnnecessaryCodeCleanUp(Map options) {
		super(options);
	}
	
	public UnnecessaryCodeCleanUp() {
		super();
	}
	
	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return UnusedCodeFix.createCleanUp(compilationUnit, 
				false, 
				false, 
				false, 
				false, 
				false, 
				false,
				isFlag(REMOVE_UNUSED_CAST));
	}
	

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return UnusedCodeFix.createCleanUp(compilationUnit, problems,
				false, 
				false, 
				false, 
				false, 
				false, 
				false,
				isFlag(REMOVE_UNUSED_CAST));
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();

		if (isFlag(REMOVE_UNUSED_CAST))
			options.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.WARNING);

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
		if (isFlag(REMOVE_UNUSED_CAST))
			result.add(MultiFixMessages.UnusedCodeCleanUp_RemoveUnusedCasts_description);
		return (String[])result.toArray(new String[result.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		if (isFlag(REMOVE_UNUSED_CAST)) {
			buf.append("Boolean b= Boolean.TRUE;\n"); //$NON-NLS-1$
		} else {
			buf.append("Boolean b= (Boolean) Boolean.TRUE;\n"); //$NON-NLS-1$
		}
		
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
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
		if (isFlag(REMOVE_UNUSED_CAST))
			result+= getNumberOfProblems(problems, IProblem.UnnecessaryCast);
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
		
		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.REMOVE_UNNECESSARY_CASTS))) {
			result|= REMOVE_UNUSED_CAST;
		}
		
	    return result;
    }

}
