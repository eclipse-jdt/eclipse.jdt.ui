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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * For given fields, method stubs for getters and setters are created.
 */
public class AddGetterSetterOperation implements IWorkspaceRunnable {
	
	private IJavaElement fInsertPosition;
	private int fFlags;
	private boolean fSort;

	private final String[] EMPTY= new String[0];
	
	private IField[] fGetterFields;
	private IField[] fSetterFields;
	private IField[] fGetterSetterFields;
	private List fCreatedAccessors;
	
	private IRequestQuery fSkipExistingQuery;
	private IRequestQuery fSkipFinalSettersQuery;
	
	private boolean fSkipAllFinalSetters;
	private boolean fSkipAllExisting;

	private boolean fCreateComments;
		
	public AddGetterSetterOperation(IField[] getterFields, IField[] setterFields, IField[] getterSetterFields, IRequestQuery skipFinalSettersQuery, IRequestQuery skipExistingQuery, IJavaElement elementPosition) {
		super();
		fGetterFields= getterFields;
		fSetterFields= setterFields;
		fGetterSetterFields= getterSetterFields;
		fSkipExistingQuery= skipExistingQuery;
		fSkipFinalSettersQuery= skipFinalSettersQuery;
		fCreatedAccessors= new ArrayList();
		fInsertPosition= elementPosition;
		fCreateComments= true;
		fFlags= Flags.AccPublic;
		fSort= false;
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
		IType parentType= field.getDeclaringType();

		String getterName= GetterSetterUtil.getGetterName(field, null);
		IMethod existingGetter= JavaModelUtil.findMethod(getterName, EMPTY, false, parentType);
		boolean doCreateGetter= ((existingGetter == null) || !querySkipExistingMethods(existingGetter));

		if (doCreateGetter) {		
			int flags= fFlags | (field.getFlags() & Flags.AccStatic);
			String stub= GetterSetterUtil.getGetterStub(field, getterName, fCreateComments, flags);
			
			IJavaElement sibling= null;
			if (existingGetter != null) {
				sibling= StubUtility.findNextSibling(existingGetter);
				existingGetter.delete(false, null);
			}
			else
				sibling= getInsertPosition();
			
			String lineDelim= StubUtility.getLineDelimiterUsed(parentType);
			int indent= StubUtility.getIndentUsed(field);
			
			String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, stub, indent, null, lineDelim, field.getJavaProject()) + lineDelim;
			fCreatedAccessors.add(parentType.createMethod(formattedContent, sibling, true, null));
		}
	}

	private void generateSetter(IField field) throws CoreException, OperationCanceledException {
		boolean isFinal= Flags.isFinal(field.getFlags());
		
		IType parentType= field.getDeclaringType();
		String setterName= GetterSetterUtil.getSetterName(field, null);

		String returnSig= field.getTypeSignature();
		String[] args= new String[] { returnSig };		
		IMethod existingSetter= JavaModelUtil.findMethod(setterName, args, false, parentType);			
		boolean doCreateSetter= ((!isFinal || !querySkipFinalSetters(field)) && (existingSetter == null || querySkipExistingMethods(existingSetter)));

		if (doCreateSetter) {
			int flags= fFlags | (field.getFlags() & Flags.AccStatic);
			String stub= GetterSetterUtil.getSetterStub(field, setterName, fCreateComments, flags);
			
			IJavaElement sibling= null;
			if (existingSetter != null) {
				sibling= StubUtility.findNextSibling(existingSetter);
				existingSetter.delete(false, null);
			}
			else
				sibling= getInsertPosition();			
			
			String lineDelim= StubUtility.getLineDelimiterUsed(parentType);
			int indent= StubUtility.getIndentUsed(field);
			
			String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, stub, indent, null, lineDelim, field.getJavaProject()) + lineDelim;
			fCreatedAccessors.add(parentType.createMethod(formattedContent, sibling, true, null));
		}
	}
	
	/**
	 * Returns the created accessors. To be called after a successful run.
	 */
	public IMethod[] getCreatedAccessors() {
		return (IMethod[]) fCreatedAccessors.toArray(new IMethod[fCreatedAccessors.size()]);
	}

	public void setSort(boolean sort) {
		fSort = sort;
	}

	public void setFlags(int flags) {
		fFlags = flags;
	}
	
	public void setCreateComments(boolean createComments) {
		fCreateComments= createComments;
	}

	public IJavaElement getInsertPosition() {
		return fInsertPosition;
	}

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

}
	
