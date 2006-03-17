/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Refactoring processor for the extract supertype refactoring.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeProcessor extends PullUpRefactoringProcessor {

	/** The extract supertype group category set */
	private static final GroupCategorySet SET_EXTRACT_SUPERTYPE= new GroupCategorySet(new GroupCategory("org.eclipse.jdt.internal.corext.extractSupertype", //$NON-NLS-1$
			RefactoringCoreMessages.ExtractSupertypeProcessor_category_name, RefactoringCoreMessages.ExtractSupertypeProcessor_category_description));

	/** The possible extract supertype candidates, or the empty array */
	private IType[] fPossibleCandidates= {};

	/** The name of the extracted type */
	private String fTypeName= null;

	/** The types where to extract the supertype */
	private IType[] fTypesToExtract= {};

	/** The working copies (working copy owner is <code>fOwner</code>) */
	private Set fWorkingCopies= new HashSet(8);

	/**
	 * Creates a new extract supertype refactoring processor.
	 * 
	 * @param members
	 *            the members to extract, or <code>null</code> if invoked by
	 *            scripting
	 * @param settings
	 *            the code generation settings, or <code>null</code> if
	 *            invoked by scripting
	 */
	public ExtractSupertypeProcessor(final IMember[] members, final CodeGenerationSettings settings) {
		super(members, settings);
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus checkDeclaringSuperTypes(final IProgressMonitor monitor) throws JavaModelException {
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void clearCaches() {
		super.clearCaches();
		try {
			for (final Iterator iterator= fWorkingCopies.iterator(); iterator.hasNext();) {
				final ICompilationUnit unit= (ICompilationUnit) iterator.next();
				try {
					unit.discardWorkingCopy();
				} catch (JavaModelException exception) {
					JavaPlugin.log(exception);
				}
			}
		} finally {
			fWorkingCopies.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		// TODO: implement
		return super.createChange(monitor);
	}

	/**
	 * Creates the extracted type.
	 * 
	 * @param type
	 *            the supertype, or <code>null</code> if no supertype is
	 *            available
	 * @param monitor
	 *            the progress monitor
	 * @return a status describing the outcome of the operation
	 */
	private RefactoringStatus createExtractedType(final IType type, final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_checking, 1);

		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Creates a new signature of a subtype.
	 * 
	 * @param rewrite
	 *            the source compilation unit rewrite
	 * @param declaration
	 *            the type declaration
	 * @param superName
	 *            the supertype name
	 * @param status
	 *            the refactoring status
	 * @param monitor
	 *            the progress monitor to use
	 * @throws JavaModelException
	 *             if the type parameters cannot be retrieved
	 */
	protected final void createTypeSignature(final CompilationUnitRewrite rewrite, final AbstractTypeDeclaration declaration, final String superName, final RefactoringStatus status, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(superName);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		final AST ast= declaration.getAST();
		final IType declaring= getDeclaringType();
		final ITypeParameter[] parameters= declaring.getTypeParameters();
		Type type= ast.newSimpleType(ast.newSimpleName(superName));
		if (parameters.length > 0) {
			final ParameterizedType parameterized= ast.newParameterizedType(type);
			for (int index= 0; index < parameters.length; index++)
				parameterized.typeArguments().add(ast.newSimpleType(ast.newSimpleName(parameters[index].getElementName())));
			type= parameterized;
		}
		final ASTRewrite rewriter= rewrite.getASTRewrite();
		if (declaration instanceof TypeDeclaration) {
			final TypeDeclaration extended= (TypeDeclaration) declaration;
			final Type superClass= extended.getSuperclassType();
			if (superClass != null)
				rewriter.replace(superClass, type, rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.ExtractSupertypeProcessor_add_supertype, SET_EXTRACT_SUPERTYPE));
			else
				rewriter.set(extended, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, type, rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.ExtractSupertypeProcessor_add_supertype, SET_EXTRACT_SUPERTYPE));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus createWorkingCopyLayer(final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_checking, 100);
			final IType declaring= getDeclaringType();
			final ITypeHierarchy hierarchy= getDeclaringSuperTypeHierarchy(new SubProgressMonitor(monitor, 1));
			status.merge(createExtractedType(hierarchy.getSuperclass(declaring), new SubProgressMonitor(monitor, 10)));
			if (status.hasFatalError())
				return status;

		} catch (JavaModelException exception) {
			JavaPlugin.log(exception);
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Returns the possible candidates where a supertype can be extracted.
	 * <p>
	 * This includes the declaring type.
	 * </p>
	 * 
	 * @return the array of candidates, or the empty array
	 */
	public IType[] getPossibleCandidates(final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		if (fPossibleCandidates == null || fPossibleCandidates.length == 0) {
			final IType declaring= getDeclaringType();
			if (declaring != null) {
				try {
					monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_computing_possible_types, 10);
					final IType superType= getDeclaringSuperTypeHierarchy(new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).getSuperclass(declaring);
					if (superType != null) {
						fPossibleCandidates= superType.newTypeHierarchy(fOwner, new SubProgressMonitor(monitor, 9, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).getSubtypes(superType);
					}
				} catch (JavaModelException exception) {
					JavaPlugin.log(exception);
				} finally {
					monitor.done();
				}
			}
		}
		return fPossibleCandidates;
	}

	/**
	 * Returns the type name.
	 * 
	 * @return the type name
	 */
	public String getTypeName() {
		return fTypeName;
	}

	/**
	 * Returns the types to extract.
	 * 
	 * @return the types to extract
	 */
	public IType[] getTypesToExtract() {
		return fTypesToExtract;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		// TODO: implement
		return super.initialize(arguments);
	}

	/**
	 * Sets the type name.
	 * 
	 * @param name
	 *            the type name
	 */
	public void setTypeName(final String name) {
		Assert.isNotNull(name);
		fTypeName= name;
	}

	/**
	 * Sets the types to extract. Must be a subset of
	 * <code>getPossibleCandidates()</code>. If the declaring type is not
	 * contained, it will automatically be added.
	 * 
	 * @param types
	 *            the types to extract
	 */
	public void setTypesToExtract(final IType[] types) {
		Assert.isNotNull(types);
		fTypesToExtract= types;
	}
}