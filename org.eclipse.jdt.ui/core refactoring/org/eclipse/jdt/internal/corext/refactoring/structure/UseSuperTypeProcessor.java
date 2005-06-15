/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsSolver;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ISourceConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Refactoring processor to replace type occurrences by a super type.
 */
public final class UseSuperTypeProcessor extends SuperTypeRefactoringProcessor {

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.useSuperTypeProcessor"; //$NON-NLS-1$

	/**
	 * Finds the type with the given fully qualified name (generic type parameters included) in the hierarchy.
	 * 
	 * @param type The hierarchy type to find the super type in
	 * @param name The fully qualified name of the super type
	 * @return The found super type, or <code>null</code>
	 */
	protected static ITypeBinding findTypeInHierarchy(final ITypeBinding type, final String name) {
		if (type.isArray() || type.isPrimitive())
			return null;
		if (name.equals(type.getTypeDeclaration().getQualifiedName()))
			return type;
		final ITypeBinding binding= type.getSuperclass();
		if (binding != null) {
			final ITypeBinding result= findTypeInHierarchy(binding, name);
			if (result != null)
				return result;
		}
		final ITypeBinding[] bindings= type.getInterfaces();
		for (int index= 0; index < bindings.length; index++) {
			final ITypeBinding result= findTypeInHierarchy(bindings[index], name);
			if (result != null)
				return result;
		}
		return null;
	}

	/** The text change manager */
	private TextChangeManager fChangeManager= null;

	/** The number of files affected by the last change generation */
	private int fChanges= 0;

	/** The subtype to replace */
	private final IType fSubType;

	/** The supertype as replacement */
	private IType fSuperType= null;

	/** The supertypes of the subtype */
	private IType[] fSuperTypes= null;

	/**
	 * Creates a new super type processor.
	 * 
	 * @param subType the subtype to replace its occurrences
	 */
	public UseSuperTypeProcessor(final IType subType) {
		Assert.isNotNull(subType);
		fSubType= subType;
	}

	/**
	 * Creates a new super type processor.
	 * 
	 * @param subType the subtype to replace its occurrences
	 * @param superType the supertype as replacement
	 */
	public UseSuperTypeProcessor(final IType subType, final IType superType) {
		Assert.isNotNull(subType);
		Assert.isNotNull(superType);
		fSubType= subType;
		fSuperType= superType;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	public final RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(context);
		final RefactoringStatus status= new RefactoringStatus();
		fChangeManager= new TextChangeManager();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.UseSuperTypeProcessor_checking);
			fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 1), status);
			if (!status.hasFatalError()) {
				final RefactoringStatus validation= Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getRefactoring().getValidationContext());
				if (!validation.isOK())
					return validation;
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final RefactoringStatus checkInitialConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.UseSuperTypeProcessor_checking);
			fSuperTypes= JavaModelUtil.getAllSuperTypes(fSubType, new SubProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
		return status;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		try {
			fChanges= 0;
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			final TextChange[] changes= fChangeManager.getAllChanges();
			if (changes != null && changes.length != 0) {
				fChanges= changes.length;
				return new DynamicValidationStateChange(RefactoringCoreMessages.UseSupertypeWherePossibleRefactoring_name, fChangeManager.getAllChanges());
			}
		} finally {
			monitor.done();
		}
		return null;
	}

	/**
	 * Creates the text change manager for this processor.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param status the refactoring status
	 * @return the created text change manager
	 * @throws JavaModelException if the method declaration could not be found
	 * @throws CoreException if the changes could not be generated
	 */
	protected final TextChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException, CoreException {
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 3); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.UseSuperTypeProcessor_creating);
			final TextChangeManager manager= new TextChangeManager();
			final IJavaProject project= fSubType.getJavaProject();
			final ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setWorkingCopyOwner(fOwner);
			parser.setResolveBindings(true);
			parser.setProject(project);
			parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
			if (fSubType.isBinary() || fSubType.isReadOnly()) {
				final IBinding[] bindings= parser.createBindings(new IJavaElement[] { fSubType, fSuperType}, new SubProgressMonitor(monitor, 1));
				if (bindings != null && bindings.length == 2 && bindings[0] instanceof ITypeBinding && bindings[1] instanceof ITypeBinding) {
					solveSuperTypeConstraints(null, null, fSubType, (ITypeBinding) bindings[0], (ITypeBinding) bindings[1], new SubProgressMonitor(monitor, 1), status);
					if (!status.hasFatalError())
						rewriteTypeOccurrences(manager, null, null, null, null, new HashSet(), status, new SubProgressMonitor(monitor, 1));
				}
			} else {
				parser.createASTs(new ICompilationUnit[] { fSubType.getCompilationUnit()}, new String[0], new ASTRequestor() {

					public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
						try {
							final CompilationUnitRewrite subRewrite= new CompilationUnitRewrite(fOwner, unit, node);
							final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(fSubType, subRewrite.getRoot());
							if (subDeclaration != null) {
								final ITypeBinding subBinding= subDeclaration.resolveBinding();
								if (subBinding != null) {
									final ITypeBinding superBinding= findTypeInHierarchy(subBinding, fSuperType.getFullyQualifiedName('.'));
									if (superBinding != null) {
										solveSuperTypeConstraints(subRewrite.getCu(), subRewrite.getRoot(), fSubType, subBinding, superBinding, new SubProgressMonitor(monitor, 1), status);
										if (!status.hasFatalError()) {
											rewriteTypeOccurrences(manager, this, subRewrite, subRewrite.getCu(), subRewrite.getRoot(), new HashSet(), status, new SubProgressMonitor(monitor, 1));
											final TextChange change= subRewrite.createChange();
											if (change != null)
												manager.manage(subRewrite.getCu(), change);
										}
									}
								}
							}
						} catch (CoreException exception) {
							JavaPlugin.log(exception);
							status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.UseSuperTypeProcessor_internal_error));
						}
					}

					public final void acceptBinding(final String key, final IBinding binding) {
						// Do nothing
					}
				}, new SubProgressMonitor(monitor, 1));
			}
			return manager;
		} finally {
			monitor.done();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor#createContraintSolver(org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsModel)
	 */
	protected final SuperTypeConstraintsSolver createContraintSolver(final SuperTypeConstraintsModel model) {
		return new SuperTypeConstraintsSolver(model);
	}

	/**
	 * Returns the number of files that are affected from the last change generation.
	 * 
	 * @return The number of files which are affected
	 */
	public final int getChanges() {
		return fChanges;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	public final Object[] getElements() {
		return new Object[] { fSubType};
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public final String getIdentifier() {
		return IDENTIFIER;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public final String getProcessorName() {
		return Messages.format(RefactoringCoreMessages.UseSuperTypeProcessor_name, new String[] { fSubType.getElementName(), fSuperType.getElementName()});
	}

	/**
	 * Returns the subtype to be replaced.
	 * 
	 * @return The subtype to be replaced
	 */
	public final IType getSubType() {
		return fSubType;
	}

	/**
	 * Returns the supertype as replacement.
	 * 
	 * @return The supertype as replacement
	 */
	public final IType getSuperType() {
		return fSuperType;
	}

	/**
	 * Returns the supertypes as possible replacements.
	 * 
	 * @return the supertypes as replacements
	 */
	public final IType[] getSuperTypes() {
		return fSuperTypes;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public final boolean isApplicable() throws CoreException {
		return Checks.isAvailable(fSubType) && Checks.isAvailable(fSuperType) && !fSubType.isAnonymous() && !fSubType.isAnnotation() && !fSuperType.isAnonymous() && !fSuperType.isAnnotation() && !fSuperType.isEnum();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#loadParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus,org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
	 */
	public final RefactoringParticipant[] loadParticipants(final RefactoringStatus status, final SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor#rewriteTypeOccurrences(org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager, org.eclipse.jdt.core.dom.ASTRequestor, org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.core.dom.CompilationUnit, java.util.Set)
	 */
	protected final void rewriteTypeOccurrences(final TextChangeManager manager, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final ICompilationUnit unit, final CompilationUnit node, final Set replacements) throws CoreException {
		final Collection collection= (Collection) fTypeOccurrences.get(unit);
		if (collection != null && !collection.isEmpty()) {
			TType estimate= null;
			ISourceConstraintVariable variable= null;
			CompilationUnitRewrite currentRewrite= null;
			final ICompilationUnit sourceUnit= rewrite.getCu();
			if (sourceUnit.equals(unit))
				currentRewrite= rewrite;
			else
				currentRewrite= new CompilationUnitRewrite(unit, node);
			for (final Iterator iterator= collection.iterator(); iterator.hasNext();) {
				variable= (ISourceConstraintVariable) iterator.next();
				estimate= (TType) variable.getData(SuperTypeConstraintsSolver.DATA_TYPE_ESTIMATE);
				if (estimate != null && variable instanceof ITypeConstraintVariable) {
					final ASTNode result= NodeFinder.perform(node, ((ITypeConstraintVariable) variable).getRange().getSourceRange());
					if (result != null)
						rewriteTypeOccurrence(estimate, currentRewrite, result, currentRewrite.createGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence));
				}
			}
			if (!sourceUnit.equals(unit)) {
				final TextChange change= currentRewrite.createChange();
				if (change != null)
					manager.manage(unit, change);
			}
		}
	}

	/**
	 * Sets the supertype as replacement..
	 * 
	 * @param type The supertype to set
	 */
	public final void setSuperType(final IType type) {
		Assert.isNotNull(type);

		fSuperType= type;
	}
}