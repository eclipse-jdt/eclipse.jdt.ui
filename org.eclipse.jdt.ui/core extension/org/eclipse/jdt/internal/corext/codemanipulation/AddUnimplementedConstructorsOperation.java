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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility.GenStubSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

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
					String formattedContent= StubUtility.codeFormat(toImplement[i], indent, lineDelim) + lineDelim;
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
	
	public String[] genOverrideStubs(IMethod[] methodsToImplement, IType type, ITypeHierarchy hierarchy, CodeGenerationSettings settings, IImportsStructure imports) throws CoreException {
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

			IMethod desc= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			if (desc == null) {
				desc= curr;
			}
			result[i]= genStub(cu, type.getElementName(), curr, desc.getDeclaringType(), genStubSettings, imports);
		}
		return result;
	}
	
	public String genStub(ICompilationUnit cu, String destTypeName, IMethod method, IType definingType, GenStubSettings settings, IImportsStructure imports) throws CoreException {
		String methName= method.getElementName();
		String[] paramNames= method.getParameterNames();
		String returnType= method.isConstructor() ? null : method.getReturnType();
		String lineDelimiter= String.valueOf('\n'); // reformatting required
			
		StringBuffer buf= new StringBuffer();
		// add method comment
		if (settings.createComments && cu != null) {
			IMethod overridden= null;
			if (settings.methodOverwrites && returnType != null) {
				overridden= JavaModelUtil.findMethod(methName, method.getParameterTypes(), false, definingType.getMethods());
			}
			String comment= StubUtility.getMethodComment(cu, destTypeName, methName, paramNames, method.getExceptionTypes(), returnType, overridden, lineDelimiter);
			if (comment != null) {
				buf.append(comment);
			} else {
				buf.append("/**").append(lineDelimiter); //$NON-NLS-1$
				buf.append(" *").append(lineDelimiter); //$NON-NLS-1$
				buf.append(" */").append(lineDelimiter); //$NON-NLS-1$							
			}
			buf.append(lineDelimiter);
		}
		// add method declaration
		String bodyContent= null;
		if (!settings.noBody) {
			String bodyStatement= StubUtility.getDefaultMethodBodyStatement(methName, paramNames, returnType, settings.callSuper);
			bodyContent= StubUtility.getMethodBodyContent(returnType == null, method.getJavaProject(), destTypeName, methName, bodyStatement, lineDelimiter);
			if (bodyContent == null) {
				bodyContent= ""; //$NON-NLS-1$
			}
		}
		StubUtility.genMethodDeclaration(destTypeName, method, fVisibility, bodyContent, imports, buf);
		return buf.toString();
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

}
