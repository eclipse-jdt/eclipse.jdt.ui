/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Add imports to a compilation unit.
 * The input is an array of full qualified type names. No elimination of unnecessary
 * imports is done (use ImportStructure for this). Duplicates are not added.
 * If the compilation unit is open in an editor, be sure to pass over its working copy.
 */
public class AddImportsOperation implements IWorkspaceRunnable {
	
	private ICompilationUnit fCompilationUnit;
	private IJavaElement[] fImports;
	private boolean fDoSave;
	
	private CodeGenerationSettings fSettings;
	
	/**
	 * Generate import statements for the passed java elements
	 * Elements must be of type IType (-> single import) or IPackageFragment
	 * (on-demand-import). Other JavaElements are ignored
	 */
	public AddImportsOperation(ICompilationUnit cu, IJavaElement[] imports, CodeGenerationSettings settings, boolean save) {
		super();
		fImports= imports;
		fCompilationUnit= cu;
		fDoSave= save;
		fSettings= settings;
	}

	/**
	 * Runs the operation.
	 * @throws OperationCanceledException Runtime error thrown when operation is cancelled.
	 */
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}			
			
			int nImports= fImports.length;
			monitor.beginTask(CodeGenerationMessages.getString("AddImportsOperation.description"), 2); //$NON-NLS-1$
		
			ImportsStructure impStructure= new ImportsStructure(fCompilationUnit, fSettings.importOrder, fSettings.importThreshold, true);
			impStructure.setFindAmbiguousImports(true);
			for (int i= 0; i < nImports; i++) {
				IJavaElement imp= fImports[i];
				if (imp instanceof IType) {
					IType type= (IType)imp;
					impStructure.addImport(JavaModelUtil.getTypeContainerName(type), type.getElementName());
				} else if (imp instanceof IPackageFragment) {
					String packageName= ((IPackageFragment)imp).getElementName();
					impStructure.addImport(packageName, "*"); //$NON-NLS-1$
				}
			}
			monitor.worked(1);
			impStructure.create(fDoSave, null);
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
		
}
