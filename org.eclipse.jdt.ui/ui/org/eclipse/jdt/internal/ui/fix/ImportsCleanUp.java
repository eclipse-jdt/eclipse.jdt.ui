/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ImportsFix;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ImportsCleanUp extends AbstractCleanUp {

	private CodeGenerationSettings fCodeGeneratorSettings;
	private RefactoringStatus fStatus;

	public ImportsCleanUp(Map<String, String> options) {
		super(options);
    }

	public ImportsCleanUp() {
		super();
    }

	@Override
	public CleanUpRequirements getRequirements() {
		boolean isOrganizeImports= isEnabled(CleanUpConstants.ORGANIZE_IMPORTS);
		return new CleanUpRequirements(isOrganizeImports, isOrganizeImports, false, null);
	}

    @Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
    	CompilationUnit compilationUnit= context.getAST();
    	if (compilationUnit == null)
    		return null;

		return ImportsFix.createCleanUp(compilationUnit, fCodeGeneratorSettings,
				isEnabled(CleanUpConstants.ORGANIZE_IMPORTS), fStatus);
	}

    @Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {

		if (isEnabled(CleanUpConstants.ORGANIZE_IMPORTS)) {
    		fCodeGeneratorSettings= JavaPreferencesSettings.getCodeGenerationSettings(project);
    		fStatus= new RefactoringStatus();
		}

		return super.checkPreConditions(project, compilationUnits, monitor);
    }

    @Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
    	fCodeGeneratorSettings= null;
    	try {
	    	if (fStatus == null || fStatus.isOK()) {
	    		return super.checkPostConditions(monitor);
	    	} else {
	    		return fStatus;
	    	}
    	} finally {
    		fStatus= null;
    	}
    }

    @Override
	public String[] getStepDescriptions() {
    	if (isEnabled(CleanUpConstants.ORGANIZE_IMPORTS))
    		return new String[] {MultiFixMessages.ImportsCleanUp_OrganizeImports_Description};

	    return null;
    }

    @Override
	public String getPreview() {
    	StringBuilder buf= new StringBuilder();

		if (isEnabled(CleanUpConstants.ORGANIZE_IMPORTS)) {
			buf.append("import org.model.Engine;\n"); //$NON-NLS-1$
		} else {
			buf.append("import org.model.*;\n"); //$NON-NLS-1$
		}

		return buf.toString();
    }

}
