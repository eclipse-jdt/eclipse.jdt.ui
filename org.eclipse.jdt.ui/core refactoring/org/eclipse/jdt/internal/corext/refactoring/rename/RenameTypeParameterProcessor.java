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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

/**
 * Rename processor to rename type parameters.
 */
public final class RenameTypeParameterProcessor extends JavaRenameProcessor implements INameUpdating, IReferenceUpdating {

	/**
	 * AST visitor which searches for occurrences of the type parameter.
	 */
	public final class RenameTypeParameterVisitor extends ASTVisitor {

		/** The binding of the type parameter */
		private final IBinding fBinding;

		/** The node of the type parameter name */
		private final SimpleName fName;

		/** The compilation unit rewrite to use */
		private final CompilationUnitRewrite fRewrite;

		/** The status of the visiting process */
		private final RefactoringStatus fStatus;

		/**
		 * Creates a new rename type parameter visitor.
		 * 
		 * @param rewrite the compilation unit rewrite to use
		 * @param range the source range of the type parameter
		 * @param status the status to update
		 */
		public RenameTypeParameterVisitor(final CompilationUnitRewrite rewrite, final ISourceRange range, final RefactoringStatus status) {
			Assert.isNotNull(rewrite);
			Assert.isNotNull(range);
			Assert.isNotNull(status);
			fRewrite= rewrite;
			fName= (SimpleName) NodeFinder.perform(rewrite.getRoot(), range);
			fBinding= fName.resolveBinding();
			fStatus= status;
		}

		/**
		 * Returns the resulting change.
		 * 
		 * @return the resulting change
		 * @throws CoreException if the change could not be created
		 */
		public final Change getResult() throws CoreException {
			return fRewrite.createChange();
		}

		public final boolean visit(final AnnotationTypeDeclaration node) {
			final String name= node.getName().getIdentifier();
			if (name.equals(getNewElementName())) {
				fStatus.addError(RefactoringCoreMessages.getFormattedString("RenameTypeParameterRefactoring.type_parameter_inner_class_clash", new String[] { name}), JavaStatusContext.create(fTypeParameter.getDeclaringMember().getCompilationUnit(), new SourceRange(node))); //$NON-NLS-1$
				return false;
			}
			return true;
		}

		public final boolean visit(final EnumDeclaration node) {
			final String name= node.getName().getIdentifier();
			if (name.equals(getNewElementName())) {
				fStatus.addError(RefactoringCoreMessages.getFormattedString("RenameTypeParameterRefactoring.type_parameter_inner_class_clash", new String[] { name}), JavaStatusContext.create(fTypeParameter.getDeclaringMember().getCompilationUnit(), new SourceRange(node))); //$NON-NLS-1$
				return false;
			}
			return true;
		}

		public final boolean visit(final SimpleName node) {
			final ITypeBinding binding= node.resolveTypeBinding();
			if (binding != null && binding.isTypeVariable() && Bindings.equals(binding, fBinding) && node.getIdentifier().equals(fName.getIdentifier())) {
				if (node != fName) {
					if (fUpdateReferences)
						fRewrite.getASTRewrite().set(node, SimpleName.IDENTIFIER_PROPERTY, getNewElementName(), fRewrite.createGroupDescription(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.update_type_parameter_reference"))); //$NON-NLS-1$
				} else
					fRewrite.getASTRewrite().set(node, SimpleName.IDENTIFIER_PROPERTY, getNewElementName(), fRewrite.createGroupDescription(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.update_type_parameter_declaration"))); //$NON-NLS-1$
			}
			return true;
		}

		public final boolean visit(final TypeDeclaration node) {
			final String name= node.getName().getIdentifier();
			if (name.equals(getNewElementName())) {
				fStatus.addError(RefactoringCoreMessages.getFormattedString("RenameTypeParameterRefactoring.type_parameter_inner_class_clash", new String[] { name}), JavaStatusContext.create(fTypeParameter.getDeclaringMember().getCompilationUnit(), new SourceRange(node))); //$NON-NLS-1$
				return false;
			}
			return true;
		}
	}

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameTypeParameterProcessor"; //$NON-NLS-1$

	/** The change object */
	private Change fChange= null;

	/** The type parameter to rename */
	private final ITypeParameter fTypeParameter;

	/** Should references to the type parameter be updated? */
	private boolean fUpdateReferences= true;

	/**
	 * Creates a new rename type parameter processor.
	 * 
	 * @param parameter the type parameter to rename
	 */
	public RenameTypeParameterProcessor(final ITypeParameter parameter) {
		Assert.isNotNull(parameter);
		fTypeParameter= parameter;
		setNewElementName(fTypeParameter.getElementName());
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating#canEnableUpdateReferences()
	 */
	public final boolean canEnableUpdateReferences() {
		return true;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	public final RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(context);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 5); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.checking")); //$NON-NLS-1$
			status.merge(Checks.checkIfCuBroken(fTypeParameter.getDeclaringMember()));
			monitor.worked(1);
			if (!status.hasFatalError()) {
				status.merge(checkNewElementName(getNewElementName()));
				monitor.worked(1);
				monitor.setTaskName(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.searching")); //$NON-NLS-1$
				status.merge(createRenameChanges(new SubProgressMonitor(monitor, 2)));
				monitor.setTaskName(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.checking")); //$NON-NLS-1$
				if (status.hasFatalError())
					return status;
				final ValidateEditChecker checker= (ValidateEditChecker) context.getChecker(ValidateEditChecker.class);
				monitor.worked(1);
				checker.addFile(ResourceUtil.getFile(fTypeParameter.getDeclaringMember().getCompilationUnit()));
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
		if (!fTypeParameter.exists())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getFormattedString("RenameTypeParameterRefactoring.deleted", fTypeParameter.getDeclaringMember().getCompilationUnit().getElementName())); //$NON-NLS-1$
		return Checks.checkIfCuBroken(fTypeParameter.getDeclaringMember());
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#checkNewElementName(java.lang.String)
	 */
	public final RefactoringStatus checkNewElementName(final String name) throws CoreException {
		Assert.isNotNull(name);
		final RefactoringStatus result= Checks.checkTypeParameterName(name);
		if (Checks.startsWithLowerCase(name))
			result.addWarning(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.should_start_lowercase")); //$NON-NLS-1$
		if (Checks.isAlreadyNamed(fTypeParameter, name))
			result.addFatalError(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.another_name")); //$NON-NLS-1$

		final IMember member= fTypeParameter.getDeclaringMember();
		if (member instanceof IType) {
			final IType type= (IType) member;
			if (type.getTypeParameter(name).exists())
				result.addFatalError(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.class_type_parameter_already_defined")); //$NON-NLS-1$
		} else if (member instanceof IMethod) {
			final IMethod method= (IMethod) member;
			if (method.getTypeParameter(name).exists())
				result.addFatalError(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.method_type_parameter_already_defined")); //$NON-NLS-1$
		} else {
			JavaPlugin.logErrorMessage("Unexpected sub-type of IMember: " + member.getClass().getName()); //$NON-NLS-1$
			Assert.isTrue(false);
		}
		return result;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		try {
			return fChange;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the necessary changes for the renaming of the type parameter.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @return the status of the operation
	 * @throws CoreException if the change could not be generated
	 */
	private RefactoringStatus createRenameChanges(final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.searching"), 2); //$NON-NLS-1$
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(fTypeParameter.getDeclaringMember().getCompilationUnit());
			final IMember member= fTypeParameter.getDeclaringMember();
			final CompilationUnit root= rewrite.getRoot();
			ASTNode declaration= null;
			if (member instanceof IMethod) {
				declaration= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, root);
			} else if (member instanceof IType) {
				declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode((IType) member, root);
			} else {
				JavaPlugin.logErrorMessage("Unexpected sub-type of IMember: " + member.getClass().getName()); //$NON-NLS-1$
				Assert.isTrue(false);
			}
			monitor.worked(1);
			final RenameTypeParameterVisitor visitor= new RenameTypeParameterVisitor(rewrite, fTypeParameter.getNameRange(), status);
			declaration.accept(visitor);
			fChange= visitor.getResult();
		} finally {
			monitor.done();
		}
		return status;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#getAffectedProjectNatures()
	 */
	protected final String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fTypeParameter);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#getCurrentElementName()
	 */
	public final String getCurrentElementName() {
		return fTypeParameter.getElementName();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	public final Object[] getElements() {
		return new Object[] { fTypeParameter};
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public final String getIdentifier() {
		return IDENTIFIER;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#getNewElement()
	 */
	public final Object getNewElement() throws CoreException {
		final IMember member= fTypeParameter.getDeclaringMember();
		if (member instanceof IType) {
			final IType type= (IType) member;
			return type.getTypeParameter(getNewElementName());
		} else if (member instanceof IMethod) {
			final IMethod method= (IMethod) member;
			return method.getTypeParameter(getNewElementName());
		} else {
			JavaPlugin.logErrorMessage("Unexpected sub-type of IMember: " + member.getClass().getName()); //$NON-NLS-1$
			Assert.isTrue(false);
		}
		return null;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public final String getProcessorName() {
		return RefactoringCoreMessages.getFormattedString("RenameTypeParameterProcessor.name", new String[] { fTypeParameter.getElementName(), getNewElementName()}); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#getUpdateReferences()
	 */
	public final boolean getUpdateReferences() {
		return fUpdateReferences;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public final boolean isApplicable() throws CoreException {
		return Checks.isAvailable(fTypeParameter);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#loadDerivedParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus, java.util.List, java.lang.String[], org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
	 */
	protected final void loadDerivedParticipants(final RefactoringStatus status, final List result, final String[] natures, final SharableParticipants shared) throws CoreException {
		// Do nothing
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#needsSavedEditors()
	 */
	public boolean needsSavedEditors() {
		return false;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating#setUpdateReferences(boolean)
	 */
	public final void setUpdateReferences(final boolean update) {
		fUpdateReferences= update;
	}
}