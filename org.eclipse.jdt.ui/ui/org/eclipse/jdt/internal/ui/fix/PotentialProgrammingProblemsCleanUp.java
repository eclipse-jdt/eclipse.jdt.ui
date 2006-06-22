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

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.PotentialProgrammingProblemsFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class PotentialProgrammingProblemsCleanUp extends AbstractCleanUp {
	
	/**
	 * Adds a generated serial version id to subtypes of
	 * java.io.Serializable and java.io.Externalizable
	 * 
	 * public class E implements Serializable {}
	 * ->
	 * public class E implements Serializable {
	 * 		private static final long serialVersionUID = 4381024239L;
	 * }
	 */
	public static final int ADD_CALCULATED_SERIAL_VERSION_ID= 1;
	
	/**
	 * Adds a default serial version it to subtypes of
	 * java.io.Serializable and java.io.Externalizable
	 * 
	 * public class E implements Serializable {}
	 * ->
	 * public class E implements Serializable {
	 * 		private static final long serialVersionUID = 1L;
	 * }
	 */
	public static final int ADD_DEFAULT_SERIAL_VERSION_ID= 2;
	
	/**
	 * Adds a default serial version it to subtypes of
	 * java.io.Serializable and java.io.Externalizable
	 * 
	 * public class E implements Serializable {}
	 * ->
	 * public class E implements Serializable {
	 * 		private static final long serialVersionUID = 84504L;
	 * }
	 */
	public static final int ADD_RANDOM_SERIAL_VERSION_ID= 4;

	private static final int DEFAULT_FLAG= 0;
	private static final String SECTION_NAME= "CleanUp_PotentialProgrammingProblems0"; //$NON-NLS-1$

	public PotentialProgrammingProblemsCleanUp(int flag) {
		super(flag);
	}

	public PotentialProgrammingProblemsCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return PotentialProgrammingProblemsFix.createCleanUp(compilationUnit,
				isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || 
				isFlag(ADD_DEFAULT_SERIAL_VERSION_ID) ||
				isFlag(ADD_RANDOM_SERIAL_VERSION_ID));
	}

	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return PotentialProgrammingProblemsFix.createCleanUp(compilationUnit, problems, 
				isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || 
				isFlag(ADD_DEFAULT_SERIAL_VERSION_ID) ||
				isFlag(ADD_RANDOM_SERIAL_VERSION_ID));
	}

	/**
	 * {@inheritDoc}
	 */
	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || 
				isFlag(ADD_DEFAULT_SERIAL_VERSION_ID)  ||
				isFlag(ADD_RANDOM_SERIAL_VERSION_ID))
			options.put(JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION, JavaCore.WARNING);
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
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID))
			result.add(MultiFixMessages.SerialVersionCleanUp_Generated_description);
		if (isFlag(ADD_DEFAULT_SERIAL_VERSION_ID))
			result.add(MultiFixMessages.CodeStyleCleanUp_addDefaultSerialVersionId_description);
		if (isFlag(ADD_RANDOM_SERIAL_VERSION_ID))
			result.add(MultiFixMessages.PotentialProgrammingProblemsCleanUp_RandomSerialId_description);
		return (String[])result.toArray(new String[result.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		buf.append("class E implements java.io.Serializable{\n"); //$NON-NLS-1$
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID)) {
			buf.append("    private static final long serialVersionUID = -391484377137870342L;\n"); //$NON-NLS-1$
		} else if (isFlag(ADD_DEFAULT_SERIAL_VERSION_ID)) {
			buf.append("    private static final long serialVersionUID = 1L;\n"); //$NON-NLS-1$
		}
		buf.append("}\n"); //$NON-NLS-1$
		
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || isFlag(ADD_DEFAULT_SERIAL_VERSION_ID)) {
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
		return PotentialProgrammingProblemsFix.checkPreConditions(project, compilationUnits, monitor,
				isFlag(ADD_CALCULATED_SERIAL_VERSION_ID),
				isFlag(ADD_DEFAULT_SERIAL_VERSION_ID),
				isFlag(ADD_RANDOM_SERIAL_VERSION_ID));
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
		if (isFlag(ADD_CALCULATED_SERIAL_VERSION_ID) || isFlag(ADD_DEFAULT_SERIAL_VERSION_ID) || isFlag(ADD_RANDOM_SERIAL_VERSION_ID))
			return getNumberOfProblems(compilationUnit.getProblems(), IProblem.MissingSerialVersion);
		
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDefaultFlag() {
		return DEFAULT_FLAG;
	}

}
