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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

/**
 * Rename processor to rename type parameters.
 */
public final class RenameTypeParameterProcessor extends JavaRenameProcessor implements INameUpdating, IReferenceUpdating {

	/**
	 * AST visitor which searches for occurrences of the type parameter.
	 */
	public final class OccurrenceFinder extends ASTVisitor {

		/** The binding of the type parameter */
		private final IBinding fBinding;

		/** The node of the type parameter name */
		private final SimpleName fName;

		/** The resulting occurrences */
		private final List fResult= new ArrayList();

		/** The status of the visiting process */
		private final RefactoringStatus fStatus;

		/**
		 * Creates a new occurrence finder.
		 * 
		 * @param name
		 *        the name of the type parameter
		 * @param status
		 *        the status to update
		 */
		public OccurrenceFinder(final SimpleName name, final RefactoringStatus status) {
			Assert.isNotNull(name);
			Assert.isNotNull(status);
			fName= name;
			fStatus= status;
			fBinding= name.resolveBinding();
		}

		/**
		 * Returns the resulting occurrences.
		 * 
		 * @return the resulting occurrences
		 */
		public final ITypedRegion[] getResult() {
			final ITypedRegion[] regions= new ITypedRegion[fResult.size()];
			fResult.toArray(regions);
			return regions;
		}

		public final boolean visit(final SimpleName node) {
			final ITypeBinding binding= node.resolveTypeBinding();
			if (binding != null && binding.isTypeVariable() && Bindings.equals(binding, fBinding) && node.getIdentifier().equals(fName.getIdentifier()))
				fResult.add(new TypedRegion(node.getStartPosition(), node.getLength(), ((node == fName) ? TYPE_DECLARATION : TYPE_REFERENCE)));

			return true;
		}

		public final boolean visit(final TypeDeclaration node) {
			final String name= node.getName().getIdentifier();
			if (name.equals(getNewElementName()))
				fStatus.addError(RefactoringCoreMessages.getFormattedString("RenameTypeParameterRefactoring.type_parameter_inner_class_clash", new String[] { name}), JavaStatusContext.create(fTypeParameter.getDeclaringMember().getCompilationUnit(), new SourceRange(node))); //$NON-NLS-1$

			return super.visit(node);
		}
	}

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameTypeParameterProcessor"; //$NON-NLS-1$

	/** The declaration type */
	private static final String TYPE_DECLARATION= "declaration"; //$NON-NLS-1$

	/** The reference type */
	private static final String TYPE_REFERENCE= "reference"; //$NON-NLS-1$

	/** The change manager */
	private TextChangeManager fChangeManager= null;

	/** The occurrences as regions in the compilation unit buffer */
	private ITypedRegion[] fOccurrences= new ITypedRegion[0];

	/** The type parameter to rename */
	private final ITypeParameter fTypeParameter;

	/** Should references to the type parameter be updated? */
	private boolean fUpdateReferences= true;

	/**
	 * Creates a new rename type parameter processor.
	 * 
	 * @param parameter
	 *        the type parameter to rename
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
				if (fUpdateReferences) {
					monitor.setTaskName(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.searching")); //$NON-NLS-1$
					fOccurrences= computeReferences(new SubProgressMonitor(monitor, 1), status);
					monitor.setTaskName(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.checking")); //$NON-NLS-1$
				} else {
					fOccurrences= new ITypedRegion[0];
					monitor.worked(1);
				}
				status.merge(createChanges(new SubProgressMonitor(monitor, 1)));
				if (status.hasFatalError())
					return status;

				final ValidateEditChecker checker= (ValidateEditChecker) context.getChecker(ValidateEditChecker.class);
				checker.addFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()));
				monitor.worked(1);
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

	/**
	 * Computes the references of the type parameter within the compilation unit.
	 * 
	 * @param monitor
	 *        the progress monitor to display progress
	 * @param status
	 *        the status to update
	 * @return the regions of the found references
	 * @throws JavaModelException
	 *         if the AST of the compilation unit could not be created
	 */
	private ITypedRegion[] computeReferences(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.searching"), 2); //$NON-NLS-1$
			final IMember member= fTypeParameter.getDeclaringMember();
			final CompilationUnit root= new RefactoringASTParser(AST.JLS3).parse(member.getCompilationUnit(), true, monitor);
			ASTNode declaration= null;
			if (member instanceof IMethod) {
				declaration= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, root);
			} else if (member instanceof IType) {
				declaration= ASTNodeSearchUtil.getTypeDeclarationNode((IType) member, root);
			} else {
				JavaPlugin.logErrorMessage("Unexpected sub-type of IMember: " + member.getClass().getName()); //$NON-NLS-1$
				Assert.isTrue(false);
			}
			monitor.worked(1);
			final OccurrenceFinder visitor= new OccurrenceFinder(((SimpleName) NodeFinder.perform(root, fTypeParameter.getNameRange())), status);
			declaration.accept(visitor);
			return visitor.getResult();
		} finally {
			monitor.done();
		}
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		try {
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("Change.javaChanges"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the necessary changes for the renaming of the type parameter.
	 * 
	 * @param monitor
	 *        the progress monitor to display progress
	 * @return the status of the operation
	 * @throws JavaModelException
	 *         if one of the changes could not be created
	 */
	private RefactoringStatus createChanges(final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.checking"), 1); //$NON-NLS-1$
			fChangeManager= new TextChangeManager(true);
			if (fUpdateReferences)
				status.merge(createReferenceChanges(new SubProgressMonitor(monitor, 1)));
			else {
				createDeclarationChange();
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Creates the change to update the type parameter declaration.
	 * 
	 * @throws JavaModelException
	 *         if the source range of the type parameter could not be determined
	 */
	private void createDeclarationChange() throws JavaModelException {
		TextChangeCompatibility.addTextEdit(fChangeManager.get(fTypeParameter.getDeclaringMember().getCompilationUnit()), RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.update_type_parameter_declaration"), new ReplaceEdit(fTypeParameter.getNameRange().getOffset(), fTypeParameter.getElementName().length(), getNewElementName())); //$NON-NLS-1$
	}

	/**
	 * Creates the changes to update references to the type parameter.
	 * 
	 * @param monitor
	 *        the progress monitor to display progress
	 * @return the status of the operation
	 */
	private RefactoringStatus createReferenceChanges(final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", fOccurrences.length); //$NON-NLS-1$
			final String reference= RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.update_type_parameter_reference"); //$NON-NLS-1$
			final String declaration= RefactoringCoreMessages.getString("RenameTypeParameterRefactoring.update_type_parameter_declaration"); //$NON-NLS-1$
			final ICompilationUnit unit= fTypeParameter.getDeclaringMember().getCompilationUnit();
			ITypedRegion occurrence= null;
			for (int index= 0; index < fOccurrences.length; index++) {
				occurrence= fOccurrences[index];
				if (occurrence.getType().equals(TYPE_REFERENCE))
					TextChangeCompatibility.addTextEdit(fChangeManager.get(unit), reference, createTextChange(occurrence));
				else
					TextChangeCompatibility.addTextEdit(fChangeManager.get(unit), declaration, createTextChange(occurrence));
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Creates the text change for the specified region.
	 * 
	 * @param region
	 *        the region to create a text change for
	 * @return the corresponding text edit
	 */
	private TextEdit createTextChange(final IRegion region) {
		final String name= fTypeParameter.getElementName();
		final int offset= region.getOffset() + region.getLength() - name.length();
		return new ReplaceEdit(offset, name.length(), getNewElementName());
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
		result.addAll(Arrays.asList(ParticipantManager.loadRenameParticipants(status, this, fTypeParameter, new RenameArguments(getNewElementName(), getUpdateReferences()), natures, shared)));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating#setUpdateReferences(boolean)
	 */
	public final void setUpdateReferences(final boolean update) {
		fUpdateReferences= update;
	}
}