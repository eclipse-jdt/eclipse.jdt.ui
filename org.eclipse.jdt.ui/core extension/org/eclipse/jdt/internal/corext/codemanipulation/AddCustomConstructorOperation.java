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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility.GenStubSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Creates a custom constructor with fields initialized.
 * If the type is open in an editor, be sure to pass over the types working working copy.
 */
public class AddCustomConstructorOperation implements IWorkspaceRunnable {
	private IJavaElement fInsertPosition;
	private IField[] fSelected;
	private IType fType;
	private IMethod fConstructorCreated;
	private boolean fDoSave;
	private CodeGenerationSettings fSettings;
	
	public AddCustomConstructorOperation(IType type, CodeGenerationSettings settings, IField[] selected, boolean save, IJavaElement insertPosition) {
		super();
		fType= type;
		fDoSave= save;
		fConstructorCreated= null;
		fSettings= settings;
		fSelected= selected;
		fInsertPosition= insertPosition;
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
			monitor.setTaskName(CodeGenerationMessages.getString("AddCustomConstructorOperation.description")); //$NON-NLS-1$
			monitor.beginTask("", 3); //$NON-NLS-1$
			
			ITypeHierarchy hierarchy= fType.newSupertypeHierarchy(new SubProgressMonitor(monitor, 1));
			monitor.worked(1);
			
			ImportsStructure imports= new ImportsStructure(fType.getCompilationUnit(), fSettings.importOrder, fSettings.importThreshold, true);
			String defaultConstructor= getDefaultConstructor(fType, hierarchy, imports);
			int firstParenIndex= defaultConstructor.indexOf("(") + 1; //$NON-NLS-1$
			int closingBraceIndex= defaultConstructor.indexOf("}"); //$NON-NLS-1$
			StringBuffer buf= new StringBuffer(defaultConstructor.substring(0, firstParenIndex));
			String[] params= new String[fSelected.length];
			for (int i= 0; i < fSelected.length; i++) {
				buf.append(Signature.toString(fSelected[i].getTypeSignature()));				
				buf.append(" "); //$NON-NLS-1$
				IJavaProject project= fSelected[i].getJavaProject();
				String accessName= NamingConventions.removePrefixAndSuffixForFieldName(project, fSelected[i].getElementName(), fSelected[i].getFlags());
				if (accessName.length() > 0) {
					char first= accessName.charAt(0);
					if (Character.isLowerCase(first)) {
						accessName= Character.toUpperCase(first) + accessName.substring(1);
					}
				}

				String paramName= StubUtility.guessArgumentName(project, accessName, new String[0]);
				
				buf.append(params[i]= paramName);
				if (i != fSelected.length - 1)
					buf.append(","); //$NON-NLS-1$					
			}
			buf.append(defaultConstructor.substring(firstParenIndex, closingBraceIndex));
			for (int i= 0; i < fSelected.length; i++) {
				String fieldName= fSelected[i].getElementName();
				boolean isStatic= Flags.isStatic(fSelected[i].getFlags());
				if (isStatic) {
					buf.append(fSelected[i].getParent().getElementName());
					buf.append("."); //$NON-NLS-1$
				}	
				else if ((fSettings.useKeywordThis) || params[i].equals(fieldName))
					buf.append("this."); //$NON-NLS-1$				
				buf.append(fieldName);
				buf.append(" = "); //$NON-NLS-1$
				buf.append(params[i]);
				buf.append(";\n"); //$NON-NLS-1$
			}
			buf.append("}"); //$NON-NLS-1$
			
			String lineDelim= StubUtility.getLineDelimiterUsed(fType);
			int indent= StubUtility.getIndentUsed(fType) + 1;
				
			String formattedContent= StubUtility.codeFormat(buf.toString(), indent, lineDelim) + lineDelim;
			fConstructorCreated= fType.createMethod(formattedContent, fInsertPosition, true, null);

			monitor.worked(1);		
			imports.create(fDoSave, null);
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}
		
	private String getDefaultConstructor(IType type, ITypeHierarchy hierarchy, ImportsStructure imports)  {
		try {
			ICompilationUnit cu= type.getCompilationUnit();
			GenStubSettings genStubSettings= new GenStubSettings(fSettings);	
				
			IType objectType= type.getJavaProject().findType("java.lang.Object"); //$NON-NLS-1$
			IMethod curr= objectType.getMethod("Object", new String[0]);  //$NON-NLS-1$
			IMethod defaultConstructor= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			IMethod desc= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			String str= StubUtility.genStub(cu, type.getElementName(), defaultConstructor, desc.getDeclaringType(), genStubSettings, imports);				
			return str;	

		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}
		return null;
	}	
	
	/**
	 * Returns the created constructor. To be called after a sucessful run.
	 */	
	public IMethod getCreatedConstructor() {
		return fConstructorCreated;
	}	
}
