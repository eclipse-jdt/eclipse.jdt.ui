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

import java.util.ArrayList;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility.GenStubSettings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

/**
 * Evaluates all unimplemented methods and creates them.
 * If the type is open in an editor, be sure to pass over the types working working copy.
 */
public class AddUnimplementedConstructorsOperation implements IWorkspaceRunnable {

	private IJavaElement fInsertPosition;
	private IMethod[] fSelected;
	private IType fType;
	private IMethod[] fCreatedMethods;
	private boolean fDoSave;
	private CodeGenerationSettings fSettings;
	private int fVisibility;
	private boolean fOmitSuper;
	
	public AddUnimplementedConstructorsOperation(IType type, CodeGenerationSettings settings, IMethod[] selected, boolean save, IJavaElement insertPosition) {
		super();
		fType= type;
		fDoSave= save;
		fCreatedMethods= null;
		fSettings= settings;
		fSelected= selected;
		fInsertPosition= insertPosition;
		fVisibility= 0;
		fOmitSuper= false;
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
			monitor.setTaskName(CodeGenerationMessages.getString("AddUnimplementedMethodsOperation.description")); //$NON-NLS-1$
			monitor.beginTask("", 3); //$NON-NLS-1$
			
			ITypeHierarchy hierarchy= fType.newSupertypeHierarchy(new SubProgressMonitor(monitor, 1));
			monitor.worked(1);
			
			ImportsStructure imports= new ImportsStructure(fType.getCompilationUnit(), fSettings.importOrder, fSettings.importThreshold, true);
			String[] toImplement= genOverrideStubs(fSelected, fType, hierarchy, fSettings, imports);
			
			int nToImplement= toImplement.length;
			ArrayList createdMethods= new ArrayList(nToImplement);
			
			if (nToImplement > 0) {
				String lineDelim= StubUtility.getLineDelimiterUsed(fType);
				int indent= StubUtility.getIndentUsed(fType) + 1;
				
				for (int i= 0; i < nToImplement; i++) {
					String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, toImplement[i], indent, null, lineDelim, fType.getJavaProject()) + lineDelim;
					IMethod curr= fType.createMethod(formattedContent, fInsertPosition, true, null);
					createdMethods.add(curr);
				}
				monitor.worked(1);	
	
				imports.create(fDoSave, null);
				monitor.worked(1);
			} else {
				monitor.worked(2);
			}

			fCreatedMethods= new IMethod[createdMethods.size()];
			createdMethods.toArray(fCreatedMethods);
		} finally {
			monitor.done();
		}
	}
	
	private String[] genOverrideStubs(IMethod[] methodsToImplement, IType type, ITypeHierarchy hierarchy, CodeGenerationSettings settings, IImportsStructure imports) throws CoreException {
		GenStubSettings genStubSettings= new GenStubSettings(settings);
		genStubSettings.methodOverwrites= true;
		ICompilationUnit cu= type.getCompilationUnit();
		String[] result= new String[methodsToImplement.length];
		for (int i= 0; i < methodsToImplement.length; i++) {
			IMethod curr= methodsToImplement[i];
			IMethod overrides= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			if (overrides != null) {				
				curr= overrides;
				// Ignore the omit super() checkbox setting unless the default constructor
				if ((curr.getNumberOfParameters() == 0) && (isOmitSuper()))
					genStubSettings.callSuper= false;
				else
					genStubSettings.callSuper= true;				
			}
			genStubSettings.methodModifiers= fVisibility | JdtFlags.clearAccessModifiers(curr.getFlags());

			IMethod desc= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			if (desc == null) {
				desc= curr;
			}
			result[i]= StubUtility.genStub(cu, type.getElementName(), curr, desc.getDeclaringType(), genStubSettings, imports);
		}
		return result;
	}
		
	/**
	 * Returns the created accessors. To be called after a sucessful run.
	 */	
	public IMethod[] getCreatedMethods() {
		return fCreatedMethods;
	}
	
	public int getVisbility() {
		return fVisibility;
	}
		
	public void setVisbility(int visbility) {
		fVisibility= visbility;
	}
	
	/**
	 * Determines whether super() is called when the default constructor is created 
	 */
	public void setOmitSuper(boolean omitSuper) {
		fOmitSuper= omitSuper;
	}
	
	public boolean isOmitSuper() {
		return fOmitSuper;
	}
	
	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	

}
