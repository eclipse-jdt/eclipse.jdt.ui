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
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.PotentialProgrammingProblemsFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class PotentialProgrammingProblemsCleanUp extends AbstractCleanUp {
	
	public PotentialProgrammingProblemsCleanUp(Map options) {
		super(options);
	}
	
	public PotentialProgrammingProblemsCleanUp() {
		super();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return PotentialProgrammingProblemsFix.createCleanUp(compilationUnit,
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED)) || 
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT)));
	}

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return PotentialProgrammingProblemsFix.createCleanUp(compilationUnit, problems, 
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED)) || 
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT)));
	}

	/**
	 * {@inheritDoc}
	 */
	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED)) || 
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT)))
			options.put(JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION, JavaCore.WARNING);
		return options;
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED)))
			result.add(MultiFixMessages.SerialVersionCleanUp_Generated_description);
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT)))
			result.add(MultiFixMessages.CodeStyleCleanUp_addDefaultSerialVersionId_description);
		
		return (String[])result.toArray(new String[result.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		buf.append("class E implements java.io.Serializable{\n"); //$NON-NLS-1$
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED))) {
			buf.append("    private static final long serialVersionUID = -391484377137870342L;\n"); //$NON-NLS-1$
		} else if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT))) {
			buf.append("    private static final long serialVersionUID = 1L;\n"); //$NON-NLS-1$
		}
		buf.append("}\n"); //$NON-NLS-1$
		
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED))
				|| (isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT))) {
			IFix[] fix= PotentialProgrammingProblemsFix.createMissingSerialVersionFixes(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		super.checkPreConditions(project, compilationUnits, null);
		return PotentialProgrammingProblemsFix.checkPreConditions(project, compilationUnits, monitor,
				isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED),
				isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT),
				false);
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		return PotentialProgrammingProblemsFix.checkPostConditions(monitor);
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED)) || 
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT)))
			return getNumberOfProblems(compilationUnit.getProblems(), IProblem.MissingSerialVersion);
		
		return 0;
	}
}
