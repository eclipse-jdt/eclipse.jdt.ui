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
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * For given fields, method stubs for getters and setters are created.
 */
public class AddGetterSetterOperation implements IWorkspaceRunnable {
	
	private IJavaElement fInsertPosition;
	private int fVisibility;
	private boolean fSort;
	private boolean fSynchronized;
	private boolean fFinal;

	private final String[] EMPTY= new String[0];
	
	private IField[] fGetterFields;
	private IField[] fSetterFields;
	private IField[] fGetterSetterFields;
	private List fCreatedAccessors;
	
	private IRequestQuery fSkipExistingQuery;
	private IRequestQuery fSkipFinalSettersQuery;
	
	private boolean fSkipAllFinalSetters;
	private boolean fSkipAllExisting;

	private CodeGenerationSettings fSettings;
	
	/**
	 * Creates the operation.
	 * @param fields The fields to create setter/getters for.
	 * @param skipFinalSettersQuery Callback to ask if the setter can be skipped for a final field.
	 *        Argument of the query is the final field. <code>null</code> is a valid input and stands for skip all.
	 * @param skipExistingQuery Callback to ask if setter / getters that already exist can be skipped.
	 *        Argument of the query is the existing method. <code>null</code> is a valid input and stands for skip all.
	 */
	public AddGetterSetterOperation(IField[] fields, CodeGenerationSettings settings, IRequestQuery skipFinalSettersQuery, IRequestQuery skipExistingQuery, IJavaElement elementPosition) {
		this(fields, fields, fields, settings, skipFinalSettersQuery, skipExistingQuery, elementPosition);
	}
	
	public AddGetterSetterOperation(IField[] getterFields, IField[] setterFields, IField[] getterSetterFields, CodeGenerationSettings settings, IRequestQuery skipFinalSettersQuery, IRequestQuery skipExistingQuery, IJavaElement elementPosition) {
		super();
		fGetterFields= getterFields;
		fSetterFields= setterFields;
		fGetterSetterFields= getterSetterFields;
		fSkipExistingQuery= skipExistingQuery;
		fSkipFinalSettersQuery= skipFinalSettersQuery;
		fSettings= settings;
		fCreatedAccessors= new ArrayList();
		fInsertPosition= elementPosition;
	}
	
	/**
	 * Runs the operation.
	 * @throws OperationCanceledException Runtime error thrown when operation is cancelled.
	 */
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.setTaskName(CodeGenerationMessages.getString("AddGetterSetterOperation.description")); //$NON-NLS-1$
			monitor.beginTask("", fGetterFields.length + fSetterFields.length); //$NON-NLS-1$

			fSkipAllFinalSetters= (fSkipFinalSettersQuery == null);
			fSkipAllExisting= (fSkipExistingQuery == null);
			
			// create pairs first: http://bugs.eclipse.org/bugs/show_bug.cgi?id=35870
			if (!fSort) {
				for (int i= 0; i < fGetterSetterFields.length; i++) {
					generateGetter(fGetterSetterFields[i]);
					generateSetter(fGetterSetterFields[i]);
					monitor.worked(1);
					if (monitor.isCanceled()){
						throw new OperationCanceledException();
					}	
				}	
			}
			for (int i= 0; i < fGetterFields.length; i++) {
				generateGetter(fGetterFields[i]);
				monitor.worked(1);
				if (monitor.isCanceled()){
					throw new OperationCanceledException();
				}	
			}
			for (int i= 0; i < fSetterFields.length; i++) {
				generateSetter(fSetterFields[i]);
				monitor.worked(1);
				if (monitor.isCanceled()){
					throw new OperationCanceledException();
				}	
			}			
		} finally {
			monitor.done();
		}
	}	

	private boolean querySkipFinalSetters(IField field) throws OperationCanceledException {
		if (!fSkipAllFinalSetters) {
			switch (fSkipFinalSettersQuery.doQuery(field)) {
				case IRequestQuery.CANCEL:
					throw new OperationCanceledException();
				case IRequestQuery.NO:
					return false;
				case IRequestQuery.YES_ALL:
					fSkipAllFinalSetters= true;
			}
		}
		return true;
	}
	
	private boolean querySkipExistingMethods(IMethod method) throws OperationCanceledException {
		if (!fSkipAllExisting) {
			switch (fSkipExistingQuery.doQuery(method)) {
				case IRequestQuery.CANCEL:
					throw new OperationCanceledException();
				case IRequestQuery.NO:
					return false;
				case IRequestQuery.YES_ALL:
					fSkipAllExisting= true;
			}
		}
		return true;
	}	
	
	private void generateGetter(IField field) throws CoreException, OperationCanceledException {
		String fieldName= field.getElementName();
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=38879
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();

		IJavaProject project= field.getJavaProject();
		
		boolean isStatic= Flags.isStatic(field.getFlags());

		String typeName= Signature.toString(field.getTypeSignature());
		String accessorName = NamingConventions.removePrefixAndSuffixForFieldName(project, fieldName, field.getFlags());
		
		IType parentType= field.getDeclaringType();
		
		boolean addComments= fSettings.createComments;
		String lineDelim= StubUtility.getLineDelimiterUsed(parentType);
		int indent= StubUtility.getIndentUsed(field);

		String getterName= GetterSetterUtil.getGetterName(field, null);
		IMethod existingGetter= JavaModelUtil.findMethod(getterName, EMPTY, false, parentType);
		boolean doCreateGetter= ((existingGetter == null) || !querySkipExistingMethods(existingGetter));

		if (doCreateGetter) {			
			// create the getter stub
			StringBuffer buf= new StringBuffer();
			if (addComments) {
				String comment= CodeGeneration.getGetterComment(field.getCompilationUnit(), parentType.getTypeQualifiedName('.'), getterName, field.getElementName(), typeName, accessorName, String.valueOf('\n'));
				if (comment != null) {
					buf.append(comment);
					buf.append('\n');
				}					
			}
			
			buf.append(JdtFlags.getVisibilityString(fVisibility));
			buf.append(' ');			
			if (isStatic)
				buf.append("static "); //$NON-NLS-1$
			if (fSynchronized)
				buf.append("synchronized "); //$NON-NLS-1$
			if (fFinal)
				buf.append("final "); //$NON-NLS-1$
				
			buf.append(typeName);
			buf.append(' ');
			buf.append(getterName);
			buf.append("() {\n"); //$NON-NLS-1$
			
			if (settings.useKeywordThis && !isStatic) {
				fieldName= "this." + fieldName; //$NON-NLS-1$
			}
			
			String body= CodeGeneration.getGetterMethodBodyContent(field.getCompilationUnit(), parentType.getTypeQualifiedName('.'), getterName, fieldName, String.valueOf('\n'));
			if (body != null) {
				buf.append(body);
			}
			buf.append("}\n"); //$NON-NLS-1$
			
			IJavaElement sibling= null;
			if (existingGetter != null) {
				sibling= StubUtility.findNextSibling(existingGetter);
				existingGetter.delete(false, null);
			}
			else
				sibling= getInsertPosition();
			
			String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, buf.toString(), indent, null, lineDelim, null) + lineDelim;
			fCreatedAccessors.add(parentType.createMethod(formattedContent, sibling, true, null));
		}
	}

	private void generateSetter(IField field) throws CoreException, OperationCanceledException {
		
		String fieldName= field.getElementName();
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();

		boolean isStatic= Flags.isStatic(field.getFlags());
			
		IJavaProject project= field.getJavaProject();
		
		String returnSig= field.getTypeSignature();
		
		String accessorName = NamingConventions.removePrefixAndSuffixForFieldName(project, fieldName, field.getFlags());
		String argname= StubUtility.suggestArgumentName(project, accessorName, EMPTY);
	
		boolean isFinal= Flags.isFinal(field.getFlags());

		String typeName= Signature.toString(returnSig);
		
		IType parentType= field.getDeclaringType();
		
		boolean addComments= fSettings.createComments;
		String lineDelim= StubUtility.getLineDelimiterUsed(parentType);
		int indent= StubUtility.getIndentUsed(field);

		String setterName= GetterSetterUtil.getSetterName(field, null);

		String[] args= new String[] { returnSig };		
		IMethod existingSetter= JavaModelUtil.findMethod(setterName, args, false, parentType);			
		boolean doCreateSetter= ((!isFinal || !querySkipFinalSetters(field)) && (existingSetter == null || querySkipExistingMethods(existingSetter)));

		if (doCreateSetter) {
			// create the setter stub
			StringBuffer buf= new StringBuffer();
			if (addComments) {
				String comment= CodeGeneration.getSetterComment(field.getCompilationUnit(), parentType.getTypeQualifiedName('.'), setterName, field.getElementName(), typeName, argname, accessorName, String.valueOf('\n'));
				if (comment != null) {
					buf.append(comment);
					buf.append('\n');
				}
			}
			buf.append(JdtFlags.getVisibilityString(fVisibility));
			buf.append(' ');	
			if (isStatic)
				buf.append("static "); //$NON-NLS-1$
			if (fSynchronized)
				buf.append("synchronized "); //$NON-NLS-1$
			if (fFinal)
				buf.append("final "); //$NON-NLS-1$				
				
			buf.append("void "); //$NON-NLS-1$
			buf.append(setterName);
			buf.append('('); 
			buf.append(typeName); 
			buf.append(' '); 
			buf.append(argname); 
			buf.append(") {\n"); //$NON-NLS-1$
			if (argname.equals(fieldName) || (settings.useKeywordThis && !isStatic)) {
				if (isStatic)
					fieldName= parentType.getElementName() + '.' + fieldName;
				else
					fieldName= "this." + fieldName; //$NON-NLS-1$
			}
			String body= CodeGeneration.getSetterMethodBodyContent(field.getCompilationUnit(), parentType.getTypeQualifiedName('.'), setterName, fieldName, argname, String.valueOf('\n'));
			if (body != null) {
				buf.append(body);
			}
			buf.append("}\n"); //$NON-NLS-1$			
			
			IJavaElement sibling= null;
			if (existingSetter != null) {
				sibling= StubUtility.findNextSibling(existingSetter);
				existingSetter.delete(false, null);
			}
			else
				sibling= getInsertPosition();			
			
   String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, buf.toString(), indent, null, lineDelim, null) + lineDelim;
			fCreatedAccessors.add(parentType.createMethod(formattedContent, sibling, true, null));
		}
	}			
	
	/**
	 * Returns the created accessors. To be called after a sucessful run.
	 */
	public IMethod[] getCreatedAccessors() {
		return (IMethod[]) fCreatedAccessors.toArray(new IMethod[fCreatedAccessors.size()]);
	}

	/**
	 * @param fSort
	 */
	public void setSort(boolean sort) {
		fSort = sort;
	}

	/**
	 * @param fVisibility
	 */
	public void setVisibility(int visibility) {
		fVisibility = visibility;
	}
	
	/**
	 * @param syncSet
	 */
	public void setSynchronized(boolean syncSet) {
		fSynchronized = syncSet;
	}
	
	/**
	 * @param finalSet
	 */
	public void setFinal(boolean finalSet) {
		fFinal = finalSet;
	}			
	/**
	 * @return
	 */
	public IJavaElement getInsertPosition() {
		return fInsertPosition;
	}
}
