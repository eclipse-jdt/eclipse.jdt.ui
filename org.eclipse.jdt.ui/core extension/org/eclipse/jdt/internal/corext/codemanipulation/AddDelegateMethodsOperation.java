/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

public class AddDelegateMethodsOperation implements IWorkspaceRunnable {	
	private List fList;	
	private IType fType; 		// Type to add methods to
	private IJavaElement fInsertPosition;
	private List fCreatedMethods;
	private CodeGenerationSettings fCodeSettings;

	public AddDelegateMethodsOperation(List resultList, CodeGenerationSettings settings, IType type, IJavaElement elementPosition) {
		fList = resultList;
		fType = type;
		fCreatedMethods = new ArrayList();
		fInsertPosition= elementPosition;
		fCodeSettings= settings;
	}

	public IMethod[] getCreatedMethods() {
		return (IMethod[]) fCreatedMethods.toArray(new IMethod[fCreatedMethods.size()]);
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}		
		try {
			int listSize= fList.size();
			String message = ActionMessages.getFormattedString("AddDelegateMethodsOperation.monitor.message", String.valueOf(listSize)); //$NON-NLS-1$
			monitor.setTaskName(message);
			monitor.beginTask("", listSize); //$NON-NLS-1$
			monitor.worked(1);
			
			boolean addComments = fCodeSettings.createComments;
	
			// already existing methods
			IMethod[] existingMethods = fType.getMethods();
			//the delimiter used
			String lineDelim = StubUtility.getLineDelimiterUsed(fType);
			// the indent used + 1
			int indent = StubUtility.getIndentUsed(fType) + 1;
	
			// perhaps we have to add import statements
			final ImportsStructure imports =
				new ImportsStructure(fType.getCompilationUnit(), fCodeSettings.importOrder, fCodeSettings.importThreshold, true);
	
			ITypeHierarchy typeHierarchy = fType.newSupertypeHierarchy(null);
	
			for (int i = 0; i < listSize; i++) {
				//check for cancel each iteration
				if (monitor.isCanceled()) {
					if (i > 0) {
						imports.create(false, null);
					}
					return;
				}
	
				String content = null;
				Methods2Field wrapper = (Methods2Field) fList.get(i);
				IMethod curr = wrapper.method;
				IField field = wrapper.field;
				
				monitor.subTask(JavaElementLabels.getElementLabel(curr, JavaElementLabels.M_PARAMETER_TYPES));
					
				IMethod overwrittenMethod =
					JavaModelUtil.findMethodImplementationInHierarchy(
						typeHierarchy,
						fType,
						curr.getElementName(),
						curr.getParameterTypes(),
						curr.isConstructor());
				if (overwrittenMethod == null) {
					content = createStub(field, curr, addComments, overwrittenMethod, imports);
				} else {
					// we could ask before overwriting final methods
	
					IMethod declaration =
						JavaModelUtil.findMethodDeclarationInHierarchy(
							typeHierarchy,
							fType,
							curr.getElementName(),
							curr.getParameterTypes(),
							curr.isConstructor());
					content = createStub(field, declaration, addComments, overwrittenMethod, imports);
				}
				IJavaElement sibling= fInsertPosition;
				IMethod existing =
					JavaModelUtil.findMethod(
						curr.getElementName(),
						curr.getParameterTypes(),
						curr.isConstructor(),
						existingMethods);
				if (existing != null) {
					// we could ask before replacing a method
					continue;
				} else if (curr.isConstructor() && existingMethods.length > 0) {
					// add constructors at the beginning
					sibling = existingMethods[0];
				}
	
				String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, content, indent, null, lineDelim, fType.getJavaProject()) + lineDelim;
				IMethod created= fType.createMethod(formattedContent, sibling, true, null);
				fCreatedMethods.add(created);
					
				monitor.worked(1);			
			}
			imports.create(false, null);
		} finally {
			monitor.done();
		}
	}

	private String createStub(IField field, IMethod curr, boolean addComment, IMethod overridden, IImportsStructure imports) throws CoreException {
		String methodName= curr.getElementName();
		String[] paramNames= StubUtility.suggestArgumentNames(curr.getJavaProject(), curr.getParameterNames());
		String returnTypSig= curr.getReturnType();

		StringBuffer buf= new StringBuffer();
		if (addComment) {
			String[] typeParamNames= StubUtility.getTypeParameterNames(curr.getTypeParameters());
			String comment= CodeGeneration.getMethodComment(fType.getCompilationUnit(), fType.getElementName(), methodName, paramNames, curr.getExceptionTypes(), returnTypSig, typeParamNames, overridden, String.valueOf('\n'));
			if (comment != null) {
				buf.append(comment);
				buf.append('\n');
			}
		}

		String methodDeclaration= null;
		if (fType.isClass()) {
			StringBuffer body= new StringBuffer();
			if (!Signature.SIG_VOID.equals(returnTypSig)) {
				body.append("return "); //$NON-NLS-1$
			}
			if (JdtFlags.isStatic(curr)) {
				body.append(resolveTypeOfField(field).getElementName());
			} else {
				if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEGEN_KEYWORD_THIS)) {
					body.append("this."); //$NON-NLS-1$
				}
				body.append(field.getElementName());
			}
			body.append('.').append(methodName).append('(');
			for (int i= 0; i < paramNames.length; i++) {
				body.append(paramNames[i]);
				if (i < paramNames.length - 1)
					body.append(',');
			}
			body.append(");"); //$NON-NLS-1$
			methodDeclaration= body.toString();
		}
		int flags= curr.getFlags() & ~Flags.AccSynchronized;
		
		StubUtility.genMethodDeclaration(fType.getElementName(), curr, flags, methodDeclaration, imports, buf);

		return buf.toString();
	}
	
	/** 
	 * returns Type of field.
	 * 
	 * if field is primitive null is returned.
	 * if field is array java.lang.Object is returned.
	 **/
	private static IType resolveTypeOfField(IField field) throws JavaModelException {
		boolean isPrimitive = hasPrimitiveType(field);
		boolean isArray = isArray(field);
		if (!isPrimitive && !isArray) {
			String typeName = JavaModelUtil.getResolvedTypeName(field.getTypeSignature(), field.getDeclaringType());
			//if the CU has errors its possible no type name is resolved
			return typeName != null ? field.getJavaProject().findType(typeName) : null;
		} else if (isArray) {
			return getJavaLangObject(field.getJavaProject()); 
		}
		return null;

	}
	
	private static boolean hasPrimitiveType(IField field) throws JavaModelException {
		String signature = field.getTypeSignature();
		char first = Signature.getElementType(signature).charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED);
	}
	
	private static IType getJavaLangObject(IJavaProject project) throws JavaModelException {
		return JavaModelUtil.findType(project, "java.lang.Object");//$NON-NLS-1$
	}

	private static boolean isArray(IField field) throws JavaModelException {
		return Signature.getArrayCount(field.getTypeSignature()) > 0;
	}
	
	/**
	 * to map from dialog results to corresponding fields
	 */
	public static class Methods2Field {
		public IMethod method = null; 		// method to wrap
		public IField field = null;			// field where method is declared
		
		public Methods2Field(IMethod method, IField field) {
			this.method = method;
			this.field = field;
		}

	}

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	
}
