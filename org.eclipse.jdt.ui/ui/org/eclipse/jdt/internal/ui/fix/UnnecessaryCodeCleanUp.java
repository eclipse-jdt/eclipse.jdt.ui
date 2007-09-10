/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class UnnecessaryCodeCleanUp extends AbstractMultiFix {
		
	public UnnecessaryCodeCleanUp(Map options) {
		super(options);
	}
	
	public UnnecessaryCodeCleanUp() {
		super();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS), false, getRequiredOptions());
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		return UnusedCodeFix.createCleanUp(compilationUnit, 
				false, 
				false, 
				false, 
				false, 
				false, 
				false,
				isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS));
	}

	/**
	 * {@inheritDoc}
	 */
	protected IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		return UnusedCodeFix.createCleanUp(compilationUnit, problems,
				false, 
				false, 
				false, 
				false, 
				false, 
				false,
				isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS));
	}

	private Map getRequiredOptions() {
		Map result= new Hashtable();

		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS))
			result.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.WARNING);

		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS))
			result.add(MultiFixMessages.UnusedCodeCleanUp_RemoveUnusedCasts_description);
		return (String[])result.toArray(new String[result.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS)) {
			buf.append("Boolean b= Boolean.TRUE;\n"); //$NON-NLS-1$
		} else {
			buf.append("Boolean b= (Boolean) Boolean.TRUE;\n"); //$NON-NLS-1$
		}
		
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() == IProblem.UnnecessaryCast)
			return isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS))
			result+= getNumberOfProblems(problems, IProblem.UnnecessaryCast);
		return result;
	}
}
