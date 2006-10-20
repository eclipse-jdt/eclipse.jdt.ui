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

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.ImportsFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ImportsCleanUp extends AbstractCleanUp {
	
	private static int ORGANIZE_IMPORTS= 1;
	private CodeGenerationSettings fCodeGeneratorSettings;

	public ImportsCleanUp(Map options) {
		super(options);
    }
	
	public ImportsCleanUp() {
		super();
    }

	/**
     * {@inheritDoc}
     */
    protected int createFlag(Map options) {
		int result= 0;
    	
		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.ORGANIZE_IMPORTS))) {
			result |= ORGANIZE_IMPORTS;	
		}
    	
	    return result;
    }

	/**
     * {@inheritDoc}
     */
    public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
	    return false;
    }

	/**
     * {@inheritDoc}
     */
    public IFix createFix(final CompilationUnit compilationUnit) throws CoreException {
		return ImportsFix.createCleanUp(compilationUnit, fCodeGeneratorSettings,
				isFlag(ORGANIZE_IMPORTS));
	}

	/**
     * {@inheritDoc}
     */
    public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
	    return null;
    }

    /**
     * {@inheritDoc}
     */
    public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		RefactoringStatus result= super.checkPreConditions(project, compilationUnits, monitor);
    	
		if (isFlag(ORGANIZE_IMPORTS))
    		fCodeGeneratorSettings= JavaPreferencesSettings.getCodeGenerationSettings(project);
    	
    	return result;
    }
    
    /**
     * {@inheritDoc}
     */
    public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
    	fCodeGeneratorSettings= null;
    	
        return super.checkPostConditions(monitor);
    }
	/**
     * {@inheritDoc}
     */
    public int getDefaultFlag() {
	    return 0;
    }

	/**
     * {@inheritDoc}
     */
    public String[] getDescriptions() {
    	if (isFlag(ORGANIZE_IMPORTS))
    		return new String[] {MultiFixMessages.ImportsCleanUp_OrganizeImports_Description};
    		
	    return null;
    }

	/**
     * {@inheritDoc}
     */
    public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		if (isFlag(ORGANIZE_IMPORTS)) {
			buf.append("import org.model.Engine;\n"); //$NON-NLS-1$
		} else {
			buf.append("import org.model.*;\n"); //$NON-NLS-1$
		}
		
		return buf.toString();
    }

	/**
     * {@inheritDoc}
     */
    public Map getRequiredOptions() {
	    return null;
    }

	/**
     * {@inheritDoc}
     */
    public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
	    return -1;
    }

	/**
     * {@inheritDoc}
     */
    public boolean needsFreshAST(CompilationUnit compilationUnit) {
    	if (isFlag(ORGANIZE_IMPORTS))
	    	return true;
    	
    	return super.needsFreshAST(compilationUnit);
    }

}
