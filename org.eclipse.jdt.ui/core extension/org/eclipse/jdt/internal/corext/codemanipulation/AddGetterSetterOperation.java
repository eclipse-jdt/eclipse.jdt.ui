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

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * For given fields, method stubs for getters and setters are created.
 */
public class AddGetterSetterOperation implements IWorkspaceRunnable {
	
	private IJavaElement fInsertPosition;
	private int fFlags;
	private boolean fSort;

	private final String[] EMPTY= new String[0];
	
	private IType fType;
	private IField[] fGetterFields;
	private IField[] fSetterFields;
	private IField[] fGetterSetterFields;

	private IRequestQuery fSkipExistingQuery;
	private IRequestQuery fSkipFinalSettersQuery;
	
	private boolean fSkipAllFinalSetters;
	private boolean fSkipAllExisting;

	private boolean fCreateComments;
	private boolean fSave;
		
	public AddGetterSetterOperation(IType type, IField[] getterFields, IField[] setterFields, IField[] getterSetterFields, IRequestQuery skipFinalSettersQuery, IRequestQuery skipExistingQuery, IJavaElement elementPosition, boolean save) {
		fType= type;
		fGetterFields= getterFields;
		fSetterFields= setterFields;
		fGetterSetterFields= getterSetterFields;
		fSkipExistingQuery= skipExistingQuery;
		fSkipFinalSettersQuery= skipFinalSettersQuery;
		fInsertPosition= elementPosition;
		fCreateComments= true;
		fFlags= Flags.AccPublic;
		fSort= false;
	}

	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.setTaskName(CodeGenerationMessages.getString("AddGetterSetterOperation.description")); //$NON-NLS-1$
			monitor.beginTask("", fGetterFields.length + fSetterFields.length); //$NON-NLS-1$
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(fType.getCompilationUnit());
			ListRewrite rewriter= null;
			if (fType.isAnonymous()) {
				final ClassInstanceCreation creation= ASTNodeSearchUtil.getClassInstanceCreationNode(fType, rewrite.getRoot());
				if (creation != null) {
					final AnonymousClassDeclaration declaration= creation.getAnonymousClassDeclaration();
					if (declaration != null)
						rewriter= rewrite.getASTRewrite().getListRewrite(declaration, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
				}
			} else {
				final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(fType, rewrite.getRoot());
				if (declaration != null)
					rewriter= rewrite.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
			}
			if (rewriter != null) {
				try {
					final ITextFileBuffer buffer= RefactoringFileBuffers.acquire(fType.getCompilationUnit());
					fSkipAllFinalSetters= (fSkipFinalSettersQuery == null);
					fSkipAllExisting= (fSkipExistingQuery == null);
					if (!fSort) {
						for (int i= 0; i < fGetterSetterFields.length; i++) {
							generateGetter(fGetterSetterFields[i], rewriter);
							generateSetter(fGetterSetterFields[i], rewriter);
							monitor.worked(1);
							if (monitor.isCanceled()){
								throw new OperationCanceledException();
							}	
						}	
					}
					for (int i= 0; i < fGetterFields.length; i++) {
						generateGetter(fGetterFields[i], rewriter);
						monitor.worked(1);
						if (monitor.isCanceled()){
							throw new OperationCanceledException();
						}	
					}
					for (int i= 0; i < fSetterFields.length; i++) {
						generateSetter(fSetterFields[i], rewriter);
						monitor.worked(1);
						if (monitor.isCanceled()){
							throw new OperationCanceledException();
						}	
					}		
					final Change result= rewrite.createChange();
					if (result instanceof CompilationUnitChange) {
						final CompilationUnitChange change= (CompilationUnitChange) result;
						final TextEdit edit= change.getEdit();
						if (edit != null) {
							try {
								edit.apply(buffer.getDocument(), TextEdit.UPDATE_REGIONS);
								if (fSave)
									buffer.commit(new SubProgressMonitor(monitor, 1), true);
							} catch (Exception exception) {
								throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, exception.getLocalizedMessage(), exception));
							}
						}
					}
				} finally {
					RefactoringFileBuffers.release(fType.getCompilationUnit());
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
	
	private void generateGetter(IField field, ListRewrite rewrite) throws CoreException, OperationCanceledException {
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
				removeExistingAccessor(rewrite, existingGetter);
			}
			else
				sibling= fInsertPosition;
			ASTNode insertion= null;
			if (sibling instanceof IMethod)
				insertion= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) fInsertPosition, (CompilationUnit) rewrite.getParent().getRoot());
			
			addNewAccessor(field, rewrite, parentType, stub, insertion);
		}
	}

	private void removeExistingAccessor(ListRewrite rewrite, IMethod accessor) throws JavaModelException {
		MethodDeclaration decl= ASTNodeSearchUtil.getMethodDeclarationNode(accessor, (CompilationUnit) rewrite.getParent().getRoot());
		if (decl != null)
			rewrite.remove(decl, null);
	}

	private void generateSetter(IField field, ListRewrite rewrite) throws CoreException, OperationCanceledException {
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
				removeExistingAccessor(rewrite, existingSetter);
			}
			else
				sibling= fInsertPosition;			
			ASTNode insertion= null;
			if (sibling instanceof IMethod)
				insertion= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) fInsertPosition, (CompilationUnit) rewrite.getParent().getRoot());
		
			addNewAccessor(field, rewrite, parentType, stub, insertion);
		}
	}

	private void addNewAccessor(IField field, ListRewrite rewrite, IType type, String contents, ASTNode insertion) throws JavaModelException {
		String lineDelim= StubUtility.getLineDelimiterUsed(type);
		MethodDeclaration decl= (MethodDeclaration) rewrite.getASTRewrite().createStringPlaceholder(CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, contents, 0, null, lineDelim, field.getJavaProject()) + lineDelim, ASTNode.METHOD_DECLARATION);
		if (insertion != null)
			rewrite.insertBefore(decl, insertion, null);
		else
			rewrite.insertLast(decl, null);
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

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
}