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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Workspace runnable to add accessor methods to fields.
 * 
 * @since 3.1
 */
public final class AddGetterSetterOperation implements IWorkspaceRunnable {

	private final String[] EMPTY= new String[0];

	private boolean fApply= true;

	private boolean fCreateComments;

	private TextEdit fEdit= null;

	private int fFlags;

	private IField[] fGetterFields;

	private IField[] fGetterSetterFields;

	private IJavaElement fInsertPosition;

	private boolean fSave;

	private IField[] fSetterFields;

	private boolean fSkipAllExisting;

	private boolean fSkipAllFinalSetters;

	private IRequestQuery fSkipExistingQuery;

	private IRequestQuery fSkipFinalSettersQuery;

	private boolean fSort;

	private IType fType;

	public AddGetterSetterOperation(IType type, IField[] getterFields, IField[] setterFields, IField[] getterSetterFields, IRequestQuery skipFinalSettersQuery, IRequestQuery skipExistingQuery, IJavaElement elementPosition, boolean apply, boolean save) {
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
		fApply= apply;
	}

	private void addNewAccessor(IField field, ListRewrite rewrite, IType type, String contents, ASTNode insertion) throws JavaModelException {
		String delimiter= StubUtility.getLineDelimiterUsed(type);
		MethodDeclaration declaration= (MethodDeclaration) rewrite.getASTRewrite().createStringPlaceholder(CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, contents, 0, null, delimiter, field.getJavaProject()) + delimiter, ASTNode.METHOD_DECLARATION);
		if (insertion != null)
			rewrite.insertBefore(declaration, insertion, null);
		else
			rewrite.insertLast(declaration, null);
	}

	private void generateGetter(IField field, ListRewrite rewrite) throws CoreException, OperationCanceledException {
		IType type= field.getDeclaringType();
		String name= GetterSetterUtil.getGetterName(field, null);
		IMethod existing= JavaModelUtil.findMethod(name, EMPTY, false, type);
		if (existing == null || !querySkipExistingMethods(existing)) {
			String stub= GetterSetterUtil.getGetterStub(field, name, fCreateComments, fFlags | (field.getFlags() & Flags.AccStatic));
			IJavaElement sibling= null;
			if (existing != null) {
				sibling= StubUtility.findNextSibling(existing);
				removeExistingAccessor(rewrite, existing);
			} else
				sibling= fInsertPosition;
			ASTNode insertion= null;
			if (sibling instanceof IMethod)
				insertion= (MethodDeclaration) ASTNodes.getParent(NodeFinder.perform(rewrite.getParent().getRoot(), ((IMethod) fInsertPosition).getNameRange()), MethodDeclaration.class);
			addNewAccessor(field, rewrite, type, stub, insertion);
		}
	}

	private void generateSetter(IField field, ListRewrite rewrite) throws CoreException, OperationCanceledException {
		IType type= field.getDeclaringType();
		String name= GetterSetterUtil.getSetterName(field, null);
		IMethod existing= JavaModelUtil.findMethod(name, new String[] { field.getTypeSignature()}, false, type);
		if ((!Flags.isFinal(field.getFlags()) || !querySkipFinalSetters(field)) && (existing == null || querySkipExistingMethods(existing))) {
			String stub= GetterSetterUtil.getSetterStub(field, name, fCreateComments, fFlags | (field.getFlags() & Flags.AccStatic));
			IJavaElement sibling= null;
			if (existing != null) {
				sibling= StubUtility.findNextSibling(existing);
				removeExistingAccessor(rewrite, existing);
			} else
				sibling= fInsertPosition;
			ASTNode insertion= null;
			if (sibling instanceof IMethod)
				insertion= (MethodDeclaration) ASTNodes.getParent(NodeFinder.perform(rewrite.getParent().getRoot(), ((IMethod) fInsertPosition).getNameRange()), MethodDeclaration.class);
			addNewAccessor(field, rewrite, type, stub, insertion);
		}
	}

	public final TextEdit getResultingEdit() {
		return fEdit;
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
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

	private void removeExistingAccessor(ListRewrite rewrite, IMethod accessor) throws JavaModelException {
		MethodDeclaration declaration= (MethodDeclaration) ASTNodes.getParent(NodeFinder.perform(rewrite.getParent().getRoot(), accessor.getNameRange()), MethodDeclaration.class);
		if (declaration != null)
			rewrite.remove(declaration, null);
	}

	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.setTaskName(CodeGenerationMessages.getString("AddGetterSetterOperation.description")); //$NON-NLS-1$
			monitor.beginTask("", fGetterFields.length + fSetterFields.length); //$NON-NLS-1$
			final ICompilationUnit unit= fType.getCompilationUnit();
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(unit);
			ListRewrite rewriter= null;
			if (fType.isAnonymous()) {
				final ClassInstanceCreation creation= (ClassInstanceCreation) ASTNodes.getParent(NodeFinder.perform(rewrite.getRoot(), fType.getNameRange()), ClassInstanceCreation.class);
				if (creation != null) {
					final AnonymousClassDeclaration declaration= creation.getAnonymousClassDeclaration();
					if (declaration != null)
						rewriter= rewrite.getASTRewrite().getListRewrite(declaration, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
				}
			} else {
				final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(NodeFinder.perform(rewrite.getRoot(), fType.getNameRange()), AbstractTypeDeclaration.class);
				if (declaration != null)
					rewriter= rewrite.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
			}
			if (rewriter != null) {
				ITextFileBuffer buffer= null;
				IDocument document= null;
				try {
					if (!JavaModelUtil.isPrimary(unit))
						document= new Document(unit.getBuffer().getContents());
					else {
						buffer= RefactoringFileBuffers.acquire(unit);
						document= buffer.getDocument();
					}
					fSkipAllFinalSetters= (fSkipFinalSettersQuery == null);
					fSkipAllExisting= (fSkipExistingQuery == null);
					if (!fSort) {
						for (int index= 0; index < fGetterSetterFields.length; index++) {
							generateGetter(fGetterSetterFields[index], rewriter);
							generateSetter(fGetterSetterFields[index], rewriter);
							monitor.worked(1);
							if (monitor.isCanceled()) {
								throw new OperationCanceledException();
							}
						}
					}
					for (int index= 0; index < fGetterFields.length; index++) {
						generateGetter(fGetterFields[index], rewriter);
						monitor.worked(1);
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
					}
					for (int index= 0; index < fSetterFields.length; index++) {
						generateSetter(fSetterFields[index], rewriter);
						monitor.worked(1);
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
					}
					final Change result= rewrite.createChange();
					if (result instanceof CompilationUnitChange) {
						final CompilationUnitChange change= (CompilationUnitChange) result;
						final TextEdit edit= change.getEdit();
						if (edit != null) {
							try {
								fEdit= edit;
								if (fApply)
									edit.apply(document, TextEdit.UPDATE_REGIONS);
								if (fSave) {
									if (buffer != null)
										buffer.commit(new SubProgressMonitor(monitor, 1), true);
									else
										unit.getBuffer().setContents(document.get());
								}
							} catch (Exception exception) {
								throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, exception.getLocalizedMessage(), exception));
							}
						}
					}
				} finally {
					if (buffer != null)
						RefactoringFileBuffers.release(unit);
				}
			}
		} finally {
			monitor.done();
		}
	}

	public void setCreateComments(boolean createComments) {
		fCreateComments= createComments;
	}

	public void setFlags(int flags) {
		fFlags= flags;
	}

	public void setSort(boolean sort) {
		fSort= sort;
	}
}