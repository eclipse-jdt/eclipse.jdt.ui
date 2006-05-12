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
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
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

	/** The empty strings constant */
	private static final String[] EMPTY_STRINGS= new String[0];

	/** The accessor fields */
	private final IField[] fAccessorFields;

	/** Should the resulting edit be applied? */
	private boolean fApply= true;

	/** The resulting text edit */
	private TextEdit fEdit= null;

	/** The getter fields */
	private final IField[] fGetterFields;

	/** The insertion point, or <code>null</code> */
	private final IJavaElement fInsert;

	/** Should the compilation unit content be saved? */
	private final boolean fSave;

	/** The setter fields */
	private final IField[] fSetterFields;

	/** The code generation settings to use */
	private final CodeGenerationSettings fSettings;

	/** Should all existing members be skipped? */
	private boolean fSkipAllExisting= false;

	/** Should all final setters be skipped? */
	private boolean fSkipAllFinalSetters= false;

	/** The skip existing request query */
	private final IRequestQuery fSkipExistingQuery;

	/** The skip final setters query */
	private final IRequestQuery fSkipFinalSettersQuery;

	/** Should the accessors be sorted? */
	private boolean fSort= false;

	/** The type declaration to add the constructors to */
	private final IType fType;

	/** The compilation unit ast node */
	private final CompilationUnit fUnit;

	/** The visibility flags of the new accessors */
	private int fVisibility= Modifier.PUBLIC;

	/**
	 * Creates a new add getter setter operation.
	 * 
	 * @param type the type to add the accessors to
	 * @param getters the fields to create getters for
	 * @param setters the fields to create setters for
	 * @param accessors the fields to create both
	 * @param unit the compilation unit ast node
	 * @param skipFinalSettersQuery the request query
	 * @param skipExistingQuery the request query
	 * @param insert the insertion point, or <code>null</code>
	 * @param settings the code generation settings to use
	 * @param apply <code>true</code> if the resulting edit should be applied, <code>false</code> otherwise
	 * @param save <code>true</code> if the changed compilation unit should be saved, <code>false</code> otherwise
	 */
	public AddGetterSetterOperation(final IType type, final IField[] getters, final IField[] setters, final IField[] accessors, final CompilationUnit unit, final IRequestQuery skipFinalSettersQuery, final IRequestQuery skipExistingQuery, final IJavaElement insert, final CodeGenerationSettings settings, final boolean apply, final boolean save) {
		Assert.isNotNull(type);
		Assert.isNotNull(unit);
		Assert.isNotNull(settings);
		fType= type;
		fGetterFields= getters;
		fSetterFields= setters;
		fAccessorFields= accessors;
		fUnit= unit;
		fSkipExistingQuery= skipExistingQuery;
		fSkipFinalSettersQuery= skipFinalSettersQuery;
		fInsert= insert;
		fSettings= settings;
		fSave= save;
		fApply= apply;
	}

	/**
	 * Adds a new accessor for the specified field.
	 * 
	 * @param type the type
	 * @param field the field
	 * @param contents the contents of the accessor method
	 * @param rewrite the list rewrite to use
	 * @param insertion the insertion point
	 * @throws JavaModelException if an error occurs
	 */
	private void addNewAccessor(final IType type, final IField field, final String contents, final ListRewrite rewrite, final ASTNode insertion) throws JavaModelException {
		final String delimiter= StubUtility.getLineDelimiterUsed(type);
		final MethodDeclaration declaration= (MethodDeclaration) rewrite.getASTRewrite().createStringPlaceholder(CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, contents, 0, null, delimiter, field.getJavaProject()), ASTNode.METHOD_DECLARATION);
		if (insertion != null)
			rewrite.insertBefore(declaration, insertion, null);
		else
			rewrite.insertLast(declaration, null);
	}

	/**
	 * Generates a new getter method for the specified field
	 * 
	 * @param field the field
	 * @param rewrite the list rewrite to use
	 * @throws CoreException if an error occurs
	 * @throws OperationCanceledException if the operation has been cancelled
	 */
	private void generateGetterMethod(final IField field, final ListRewrite rewrite) throws CoreException, OperationCanceledException {
		final IType type= field.getDeclaringType();
		final String name= GetterSetterUtil.getGetterName(field, null);
		final IMethod existing= JavaModelUtil.findMethod(name, EMPTY_STRINGS, false, type);
		if (existing == null || !querySkipExistingMethods(existing)) {
			IJavaElement sibling= null;
			if (existing != null) {
				sibling= StubUtility.findNextSibling(existing);
				removeExistingAccessor(existing, rewrite);
			} else
				sibling= fInsert;
			ASTNode insertion= null;
			if (sibling instanceof IMethod)
				insertion= ASTNodes.getParent(NodeFinder.perform(rewrite.getParent().getRoot(), ((IMethod) fInsert).getNameRange()), MethodDeclaration.class);
			addNewAccessor(type, field, GetterSetterUtil.getGetterStub(field, name, fSettings.createComments, fVisibility | (field.getFlags() & Flags.AccStatic)), rewrite, insertion);
		}
	}

	/**
	 * Generates a new setter method for the specified field
	 * 
	 * @param field the field
	 * @param rewrite the list rewrite to use
	 * @throws CoreException if an error occurs
	 * @throws OperationCanceledException if the operation has been cancelled
	 */
	private void generateSetterMethod(final IField field, final ListRewrite rewrite) throws CoreException, OperationCanceledException {
		final IType type= field.getDeclaringType();
		final String name= GetterSetterUtil.getSetterName(field, null);
		final IMethod existing= JavaModelUtil.findMethod(name, new String[] { field.getTypeSignature()}, false, type);
		if ((!Flags.isFinal(field.getFlags()) || !querySkipFinalSetters(field)) && (existing == null || querySkipExistingMethods(existing))) {
			IJavaElement sibling= null;
			if (existing != null) {
				sibling= StubUtility.findNextSibling(existing);
				removeExistingAccessor(existing, rewrite);
			} else
				sibling= fInsert;
			ASTNode insertion= null;
			if (sibling instanceof IMethod)
				insertion= ASTNodes.getParent(NodeFinder.perform(rewrite.getParent().getRoot(), ((IMethod) fInsert).getNameRange()), MethodDeclaration.class);
			addNewAccessor(type, field, GetterSetterUtil.getSetterStub(field, name, fSettings.createComments, fVisibility | (field.getFlags() & Flags.AccStatic)), rewrite, insertion);
		}
	}

	/**
	 * Returns the resulting text edit.
	 * 
	 * @return the resulting text edit
	 */
	public final TextEdit getResultingEdit() {
		return fEdit;
	}

	/**
	 * Returns the scheduling rule for this operation.
	 * 
	 * @return the scheduling rule
	 */
	public final ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * Returns the visibility modifier of the generated constructors.
	 * 
	 * @return the visibility modifier
	 */
	public final int getVisibility() {
		return fVisibility;
	}

	/**
	 * Should all existing members be skipped?
	 * 
	 * @return <code>true</code> if they should be skipped, <code>false</code> otherwise
	 */
	public final boolean isSkipAllExisting() {
		return fSkipAllExisting;
	}

	/**
	 * Should all final setters be skipped?
	 * 
	 * @return <code>true</code> if final setters should be skipped, <code>false</code> otherwise
	 */
	public final boolean isSkipAllFinalSetters() {
		return fSkipAllFinalSetters;
	}

	/**
	 * Queries the user whether to skip existing methods.
	 * 
	 * @param method the method in question
	 * @return <code>true</code> to skip existing methods, <code>false</code> otherwise
	 * @throws OperationCanceledException if the operation has been cancelled
	 */
	private boolean querySkipExistingMethods(final IMethod method) throws OperationCanceledException {
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

	/**
	 * Queries the user whether to skip final setters of existing fields.
	 * 
	 * @param field the field in question
	 * @return <code>true</code> to skip final setters, <code>false</code> otherwise
	 * @throws OperationCanceledException if the operation has been cancelled
	 */
	private boolean querySkipFinalSetters(final IField field) throws OperationCanceledException {
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

	/**
	 * Removes an existing accessor method.
	 * 
	 * @param accessor the accessor method to remove
	 * @param rewrite the list rewrite to use
	 * @throws JavaModelException if an error occurs
	 */
	private void removeExistingAccessor(final IMethod accessor, final ListRewrite rewrite) throws JavaModelException {
		final MethodDeclaration declaration= (MethodDeclaration) ASTNodes.getParent(NodeFinder.perform(rewrite.getParent().getRoot(), accessor.getNameRange()), MethodDeclaration.class);
		if (declaration != null)
			rewrite.remove(declaration, null);
	}

	/*
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.setTaskName(CodeGenerationMessages.AddGetterSetterOperation_description); 
			monitor.beginTask("", fGetterFields.length + fSetterFields.length); //$NON-NLS-1$
			final ICompilationUnit unit= fType.getCompilationUnit();
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(unit, fUnit);
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
						for (int index= 0; index < fAccessorFields.length; index++) {
							generateGetterMethod(fAccessorFields[index], rewriter);
							generateSetterMethod(fAccessorFields[index], rewriter);
							monitor.worked(1);
							if (monitor.isCanceled()) {
								throw new OperationCanceledException();
							}
						}
					}
					for (int index= 0; index < fGetterFields.length; index++) {
						generateGetterMethod(fGetterFields[index], rewriter);
						monitor.worked(1);
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
					}
					for (int index= 0; index < fSetterFields.length; index++) {
						generateSetterMethod(fSetterFields[index], rewriter);
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

	/**
	 * Determines whether existing members should be skipped.
	 * 
	 * @param skip <code>true</code> to skip existing members, <code>false</code> otherwise
	 */
	public final void setSkipAllExisting(final boolean skip) {
		fSkipAllExisting= skip;
	}

	/**
	 * Determines whether final setters should be skipped.
	 * 
	 * @param skip <code>true</code> to skip final setters, <code>false</code> otherwise
	 */
	public final void setSkipAllFinalSetters(final boolean skip) {
		fSkipAllFinalSetters= skip;
	}

	public void setSort(boolean sort) {
		fSort= sort;
	}

	/**
	 * Sets the visibility modifier of the generated constructors.
	 * 
	 * @param visibility the visibility modifier
	 */
	public final void setVisibility(final int visibility) {
		fVisibility= visibility;
	}
}
