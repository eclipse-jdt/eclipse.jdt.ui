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
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsSolver;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ISourceConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Refactoring processor to replace type occurrences by a super type.
 */
public final class UseSuperTypeProcessor extends SuperTypeRefactoringProcessor {

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.useSuperTypeProcessor"; //$NON-NLS-1$

	/** The text change manager */
	private TextChangeManager fChangeManager= null;

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
			monitor.setTaskName(RefactoringCoreMessages.getString("UseSuperTypeProcessor.checking")); //$NON-NLS-1$
			fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 1), status);
			if (!status.hasFatalError())
				status.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getRefactoring().getValidationContext()));
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
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("UseSuperTypeProcessor.checking")); //$NON-NLS-1$
			status.merge(Checks.checkIfCuBroken(fSubType));
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
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.creating")); //$NON-NLS-1$
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("UseSupertypeWherePossibleRefactoring.name"), fChangeManager.getAllChanges());//$NON-NLS-1$
		} finally {
			monitor.done();
		}
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
	protected TextChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException, CoreException {
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 3); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("UseSuperTypeProcessor.creating")); //$NON-NLS-1$
			final TextChangeManager manager= new TextChangeManager();
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(fOwner, fSubType.getCompilationUnit());
			final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(fSubType, rewrite.getRoot());
			if (subDeclaration != null) {
				final ITypeBinding subBinding= subDeclaration.resolveBinding();
				if (subBinding != null) {
					final ITypeBinding superBinding= Bindings.findTypeInHierarchy(subBinding, fSuperType.getFullyQualifiedName('.'));
					if (superBinding != null) {
						solveSuperTypeConstraints(rewrite.getCu(), rewrite.getRoot(), fSubType, subBinding, superBinding, new SubProgressMonitor(monitor, 1), status);
						if (!status.hasFatalError())
							rewriteTypeOccurrences(manager, rewrite, rewrite.getCu(), rewrite.getRoot(), new HashSet(), status, new SubProgressMonitor(monitor, 1));
					}
				}
			}
			return manager;
		} finally {
			monitor.done();
		}
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
		return RefactoringCoreMessages.getFormattedString("UseSuperTypeProcessor.name", new String[] { fSubType.getElementName(), fSuperType.getElementName()}); //$NON-NLS-1$
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
	 * @see org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor#rewriteTypeOccurrences(org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager, org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.core.dom.CompilationUnit, java.util.Set)
	 */
	protected final void rewriteTypeOccurrences(final TextChangeManager manager, final CompilationUnitRewrite subRewrite, final ICompilationUnit unit, final CompilationUnit node, final Set replacements) throws CoreException {
		final Collection collection= (Collection) fTypeOccurrences.get(unit);
		if (collection != null && !collection.isEmpty()) {
			TType type= null;
			ISourceConstraintVariable variable= null;
			CompilationUnitRewrite rewrite= null;
			final ICompilationUnit subUnit= subRewrite.getCu();
			if (subUnit.equals(unit))
				rewrite= subRewrite;
			else
				rewrite= new CompilationUnitRewrite(unit, node);
			for (final Iterator iterator= collection.iterator(); iterator.hasNext();) {
				variable= (ISourceConstraintVariable) iterator.next();
				type= (TType) variable.getData(SuperTypeConstraintsSolver.DATA_TYPE_ESTIMATE);
				if (type != null && variable instanceof ITypeConstraintVariable) {
					final CompilationUnitRange range= ((ITypeConstraintVariable) variable).getRange();
					rewriteTypeOccurrence(range, rewrite, node, rewrite.createGroupDescription(RefactoringCoreMessages.getString("SuperTypeRefactoringProcessor.update_type_occurrence"))); //$NON-NLS-1$
					manager.manage(unit.getPrimary(), rewrite.createChange());
				}
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