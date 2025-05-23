/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jerome Cambon <jerome.cambon@oracle.com> - [code style] don't generate redundant modifiers "public static final abstract" for interface members - https://bugs.eclipse.org/71627
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *     Microsoft Corporation - read formatting options from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveStaticMembersDescriptor;
import org.eclipse.jdt.core.refactoring.participants.IRefactoringProcessorIds;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTesterCore;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.delegates.DelegateFieldCreator;
import org.eclipse.jdt.internal.corext.refactoring.delegates.DelegateMethodCreator;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.structure.MemberVisibilityAdjustor.IncomingMemberVisibilityAdjustment;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.Progress;


public final class MoveStaticMembersProcessor extends MoveProcessor implements IDelegateUpdating {

	private static final String ATTRIBUTE_DELEGATE="delegate"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DEPRECATE="deprecate"; //$NON-NLS-1$
	private static final String TRACKED_POSITION_PROPERTY= "MoveStaticMembersProcessor.trackedPosition"; //$NON-NLS-1$

	private IMember[] fMembersToMove;
	private IType fDestinationType;
	private String fDestinationTypeName;

	private CodeGenerationSettings fPreferences;
	private CompositeChange fChange;
	private CompilationUnitRewrite fSource;
	private ITypeBinding fSourceBinding;
	private CompilationUnitRewrite fTarget;
	private IBinding[] fMemberBindings;
	private BodyDeclaration[] fMemberDeclarations;
	private boolean fDelegateUpdating;
	private boolean fDelegateDeprecation;

	private static class TypeReferenceFinder extends ASTVisitor {
		List<IBinding> fResult= new ArrayList<>();
		Set<ITypeBinding> fDefined= new HashSet<>();
		public static List<IBinding> perform(ASTNode root) {
			TypeReferenceFinder visitor= new TypeReferenceFinder();
			root.accept(visitor);
			return visitor.fResult;
		}
		@Override
		public boolean visit(TypeDeclaration node) {
			fDefined.add(node.resolveBinding());
			return true;
		}
		@Override
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (!(binding instanceof ITypeBinding))
				return true;
			if (!fDefined.contains(binding))
				fResult.add(binding);
			return true;
		}

		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			fDefined.add(node.resolveBinding());
			return true;
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			fDefined.add(node.resolveBinding());
			return true;
		}
	}

	/**
	 * Creates a new move static members processor.
	 * @param members the members to move, or <code>null</code> if invoked by scripting
	 * @param settings the code generation settings, or <code>null</code> if invoked by scripting
	 */
	public MoveStaticMembersProcessor(IMember[] members, CodeGenerationSettings settings) {
		fMembersToMove= members;
		fPreferences= settings;
		fDelegateUpdating= false;
		fDelegateDeprecation= true;
	}

	public MoveStaticMembersProcessor(JavaRefactoringArguments arguments, RefactoringStatus status) {
		fDelegateUpdating= false;
		fDelegateDeprecation= true;
		RefactoringStatus initializeStatus= initialize(arguments);
		status.merge(initializeStatus);
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTesterCore.isMoveStaticMembersAvailable(fMembersToMove);
	}

	@Override
	public Object[] getElements() {
		Object[] result= new Object[fMembersToMove.length];
		System.arraycopy(fMembersToMove, 0, result, 0, fMembersToMove.length);
		return result;
	}

	@Override
	public String getIdentifier() {
		return IRefactoringProcessorIds.MOVE_STATIC_MEMBERS_PROCESSOR;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		List<MoveParticipant> result= new ArrayList<>();
		MoveArguments args= new MoveArguments(fDestinationType, true);
		String[] natures= JavaProcessors.computeAffectedNaturs(fMembersToMove);
		for (IMember memberToMove : fMembersToMove) {
			result.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(
				status, this, memberToMove, args, natures, sharedParticipants)));
		}
		return result.toArray(new RefactoringParticipant[result.size()]);
	}

	//------------------- IDelegateUpdating ----------------------

	@Override
	public boolean canEnableDelegateUpdating() {
		try {
			for (IMember memberToMove : fMembersToMove) {
				if (isDelegateCreationAvailable(memberToMove))
					return true;
			}
		} catch (JavaModelException e) {
			return false;
		}
		return false;
	}

	private boolean isDelegateCreationAvailable(IMember member) throws JavaModelException {
		if (member instanceof IMethod)
			return true;
		if (member instanceof IField && RefactoringAvailabilityTesterCore.isDelegateCreationAvailable(((IField)member)))
			return true;
		return false;
	}

	@Override
	public boolean getDelegateUpdating() {
		return fDelegateUpdating;
	}

	@Override
	public void setDelegateUpdating(boolean updating) {
		fDelegateUpdating= updating;
	}

	@Override
	public boolean getDeprecateDelegates() {
		return fDelegateDeprecation;
	}

	@Override
	public void setDeprecateDelegates(boolean deprecate) {
		fDelegateDeprecation= deprecate;
	}

	@Override
	public String getProcessorName() {
		return RefactoringCoreMessages.MoveMembersRefactoring_Move_Members;
	}

	public IType getDestinationType() {
		return fDestinationType;
	}

	public void setDestinationTypeFullyQualifiedName(String fullyQualifiedTypeName) throws JavaModelException {
		Assert.isNotNull(fullyQualifiedTypeName);
		fDestinationType= resolveType(fullyQualifiedTypeName);
		//workaround for bug 36032: IJavaProject#findType(..) doesn't find secondary type
		fDestinationTypeName= fullyQualifiedTypeName;
	}

	public IMember[] getMembersToMove() {
		return fMembersToMove;
	}

	public IType getDeclaringType() {
		//all methods declared in same type - checked in precondition
		return  fMembersToMove[0].getDeclaringType(); //index safe - checked in areAllMoveable()
	}

	private IType resolveType(String qualifiedTypeName) throws JavaModelException{
		IType type= getDeclaringType().getJavaProject().findType(qualifiedTypeName);
		if (type == null)
			type= getDeclaringType().getJavaProject().findType(getDeclaringType().getPackageFragment().getElementName(), qualifiedTypeName);
		return type;
	}

	//---- Activation checking ------------------------------------

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, 1);
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkDeclaringType());
			pm.worked(1);
			if (result.hasFatalError())
				return result;

			fSource= new CompilationUnitRewrite(fMembersToMove[0].getCompilationUnit());
			fSourceBinding= (ITypeBinding)((SimpleName)NodeFinder.perform(fSource.getRoot(), fMembersToMove[0].getDeclaringType().getNameRange())).resolveBinding();
			fMemberBindings= getMemberBindings();
			if (fSourceBinding == null || hasUnresolvedMemberBinding()) {
				result.addFatalError(Messages.format(
					RefactoringCoreMessages.MoveMembersRefactoring_compile_errors,
					BasicElementLabels.getFileName(fSource.getCu())));
			}
			fMemberDeclarations= getASTMembers(result);
			return result;
		} finally {
			pm.done();
		}
	}

	private boolean hasUnresolvedMemberBinding() {
		for (IBinding memberBinding : fMemberBindings) {
			if (memberBinding == null)
				return true;
		}
		return false;
	}

	private RefactoringStatus checkDeclaringType(){
		IType declaringType= getDeclaringType();

		if ("java.lang.Object".equals(declaringType.getFullyQualifiedName('.'))) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MoveMembersRefactoring_Object);

		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MoveMembersRefactoring_binary);

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MoveMembersRefactoring_read_only);

		return null;
	}

	//---- Input checking ------------------------------------

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		fTarget= null;
		try {
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, 10);

			RefactoringStatus result= new RefactoringStatus();

			fSource.clearASTAndImportRewrites();

			result.merge(checkDestinationType());
			if (result.hasFatalError())
				return result;

			result.merge(checkDestinationInsideTypeToMove());
			if (result.hasFatalError())
				return result;

			result.merge(MemberCheckUtil.checkMembersInDestinationType(fMembersToMove, fDestinationType));
			if (result.hasFatalError())
				return result;

			result.merge(checkNativeMovedMethods(Progress.subMonitor(pm, 1)));

			if (result.hasFatalError())
				return result;

			result.merge(checkMemberOverride());

			List<ICompilationUnit> modifiedCus= new ArrayList<>();
			createChange(modifiedCus, result, Progress.subMonitor(pm, 7));
			ResourceChangeChecker checker= context.getChecker(ResourceChangeChecker.class);
			for (IFile changedFile : getAllFilesToModify(modifiedCus)) {
				checker.getDeltaFactory().change(changedFile);
			}

			return result;
		} finally {
			pm.done();
		}
	}

	private class OverrideFinder extends ASTVisitor {
		private IBinding fOverrideBinding;

		public IBinding getOverideBinding() {
			return fOverrideBinding;
		}

		@Override
		public boolean visit(SimpleName node) {
			if (node.getLocationInParent() != QualifiedName.NAME_PROPERTY &&
					node.getLocationInParent() != FieldAccess.NAME_PROPERTY &&
					node.getLocationInParent() != SuperFieldAccess.NAME_PROPERTY ||
					(node.getParent() instanceof MethodInvocation methodInvocation &&
							methodInvocation.getExpression() == null)) {
				IBinding binding= node.resolveBinding();
				if (binding instanceof IMethodBinding methodBinding) {
					// we only look for method calls for methods that are declared in a class that is neither
					// the target or the source (other conflicts will be caught elsewhere)
					String methodBindingClassName= methodBinding.getDeclaringClass().getQualifiedName();
					if (!methodBindingClassName.equals(getDeclaringType().getFullyQualifiedName('.')) &&
							!methodBindingClassName.equals(getDestinationType().getFullyQualifiedName('.'))) {
						for (IMember member : fMembersToMove) {
							if (member.getElementType() == IJavaElement.METHOD) {
								IMethod memberMethod= (IMethod)member;
								try {
									if (memberMethod.getElementName().equals(methodBinding.getName()) &&
											Bindings.sameParameters(methodBinding, memberMethod)) {
										fOverrideBinding= methodBinding;
										throw new AbortSearchException();
									}
								} catch (JavaModelException e) {
									// do nothing
								}
							}
						}
					}
				} else if (binding instanceof IVariableBinding varBinding && varBinding.isField()) {
					// we only look for method calls for methods that are declared in a class that is neither
					// the target or the source (other conflicts will be caught elsewhere)
					String varBindingClassName= varBinding.getDeclaringClass().getQualifiedName();
					if (!varBindingClassName.equals(getDeclaringType().getFullyQualifiedName('.')) &&
							!varBindingClassName.equals(getDestinationType().getFullyQualifiedName('.'))) {
						for (IMember member : fMembersToMove) {
							if (member.getElementType() == IJavaElement.FIELD &&
									member.getElementName().equals(varBinding.getName())) {
								fOverrideBinding= varBinding;
								throw new AbortSearchException();
							}
						}
					}
				}
			}
			return true;
		}
	}

	private RefactoringStatus checkMemberOverride() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		fTarget= getCuRewrite(fDestinationType.getCompilationUnit());
		AbstractTypeDeclaration decl= getDestinationNode();
		OverrideFinder overrideFinder= new OverrideFinder();
		try {
			decl.accept(overrideFinder);
		} catch (AbortSearchException e) {
			IBinding overrideRef= overrideFinder.getOverideBinding();
			String originalClassName= overrideRef instanceof IVariableBinding varBinding ? varBinding.getDeclaringClass().getName() : ((IMethodBinding)overrideRef).getDeclaringClass().getName();
			IJavaElement element= overrideRef.getJavaElement();
			result.addError(Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_override_ref,
					new Object[] {decl.getName(), element.getElementName(), originalClassName }),
					JavaStatusContext.create((IMember) element));
		}
		return result;
	}

	private IFile[] getAllFilesToModify(List<ICompilationUnit> modifiedCus) {
		Set<IResource> result= new HashSet<>();
		IResource resource= fDestinationType.getCompilationUnit().getResource();
		result.add(resource);
		for (IMember memberToMove : fMembersToMove) {
			resource= memberToMove.getCompilationUnit().getResource();
			if (resource != null)
				result.add(resource);
		}
		for (ICompilationUnit unit : modifiedCus) {
			if (unit.getResource() != null)
				result.add(unit.getResource());
		}
		return result.toArray(new IFile[result.size()]);
	}

	private RefactoringStatus checkDestinationType() throws JavaModelException {
		if (fDestinationType == null){
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_not_found, BasicElementLabels.getJavaElementName(fDestinationTypeName));
			return RefactoringStatus.createFatalErrorStatus(message);
		}

		if (fDestinationType.equals(getDeclaringType())){
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_same,
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);
		}

		if (! fDestinationType.exists()){
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_not_exist,
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);
		}

		if (fDestinationType.isBinary()){
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_dest_binary,
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);
		}

		RefactoringStatus result= new RefactoringStatus();

		if (fDestinationType.isInterface())
			result.merge(checkMoveToInterface());
		if (result.hasFatalError())
			return result;

		// no checking required for moving interface fields to classes

		if (!JdtFlags.isStatic(fDestinationType) && fDestinationType.getDeclaringType() != null) {
			String message= RefactoringCoreMessages.MoveMembersRefactoring_static_declaration;
			result.addError(message);
		}

		return result;
	}

	private RefactoringStatus checkDestinationInsideTypeToMove() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (IMember memberToMove : fMembersToMove) {
			if (! (memberToMove instanceof IType))
				continue;
			IType type= (IType) memberToMove;
			if (fDestinationType.equals(type) || JavaElementUtil.isAncestorOf(type, fDestinationType)) {
				String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_inside,
						new String[] { getQualifiedTypeLabel(type), getQualifiedTypeLabel(fDestinationType)});
				RefactoringStatusContext context= JavaStatusContext.create(fDestinationType.getCompilationUnit(), fDestinationType.getNameRange());
				result.addFatalError(message, context);
				return result;
			}
		}
		return result;
	}

	private RefactoringStatus checkMoveToInterface() throws JavaModelException {
		//could be more clever and make field final if it is only written once...
		RefactoringStatus result= new RefactoringStatus();
		boolean declaringIsInterface= getDeclaringType().isInterface();
		if (declaringIsInterface)
			return result;
		String moveMembersMsg= RefactoringCoreMessages.MoveMembersRefactoring_only_public_static_18;
		for (IMember memberToMove : fMembersToMove) {
			if (!canMoveToInterface(memberToMove)) {
				result.addError(moveMembersMsg, JavaStatusContext.create(memberToMove));
			} else if (!Flags.isPublic(memberToMove.getFlags()) && !declaringIsInterface) {
				result.addWarning(RefactoringCoreMessages.MoveMembersRefactoring_member_will_be_public, JavaStatusContext.create(memberToMove));
			}
		}
		return result;
	}

	private boolean canMoveToInterface(IMember member) throws JavaModelException {
		int flags= member.getFlags();
		switch (member.getElementType()) {
			case IJavaElement.FIELD:
				if (!Flags.isStatic(flags) || !Flags.isFinal(flags))
					return false;
				if (Flags.isEnum(flags))
					return false;
				VariableDeclarationFragment declaration= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, fSource.getRoot());
				if (declaration != null)
					return declaration.getInitializer() != null;
				return false;
			case IJavaElement.TYPE: {
				IType type= (IType) member;
				if (type.isInterface() && !Checks.isTopLevel(type))
					return true;
				return Flags.isStatic(flags);
			}
			case IJavaElement.METHOD: {
				return Flags.isStatic(flags);
			}
			default:
				return false;
		}
	}

	private RefactoringStatus checkMovedMemberAvailability(IMember memberToMove, IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (memberToMove instanceof IType) { // recursively check accessibility of member type's members
			IJavaElement[] typeMembers= memberToMove.getChildren();
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, typeMembers.length + 1);
			for (IJavaElement typeMember : typeMembers) {
				if (typeMember instanceof IInitializer)
					pm.worked(1);
				else
					result.merge(checkMovedMemberAvailability((IMember) typeMember, Progress.subMonitor(pm, 1)));
			}
		} else {
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, 1);
		}

		for (IType blindAccessorType : getTypesNotSeeingMovedMember(memberToMove, Progress.subMonitor(pm, 1), result)) {
			String message= createNonAccessibleMemberMessage(memberToMove, blindAccessorType,/*moved*/true);
			result.addError(message, JavaStatusContext.create(memberToMove));
		}
		pm.done();
		return result;
	}

	private IType[] getTypesNotSeeingMovedMember(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		if (JdtFlags.isPublic(member) && JdtFlags.isPublic(fDestinationType))
			return new IType[0];

		HashSet<IType> blindAccessorTypes= new HashSet<>(); // referencing, but access to destination type illegal
		for (SearchResultGroup reference : getReferences(member, Progress.subMonitor(pm, 1), status)) {
			for (SearchMatch searchResult : reference.getSearchResults()) {
				IJavaElement element= SearchUtils.getEnclosingJavaElement(searchResult);
				IType type= (IType) element.getAncestor(IJavaElement.TYPE);
				if (type != null //reference can e.g. be an import declaration
						&& ! blindAccessorTypes.contains(type)
						&& ! isWithinMemberToMove(searchResult)
						&& !isVisibleFrom(getDestinationType(), type)) {
					blindAccessorTypes.add(type);
				}
			}
		}

		if (fDelegateUpdating && isDelegateCreationAvailable(member)) {
			// ensure moved member is visible from the delegate
			IType type= member.getDeclaringType();
			if (!blindAccessorTypes.contains(type) && !isVisibleFrom(getDestinationType(), type))
				blindAccessorTypes.add(type);
		}

		return blindAccessorTypes.toArray(new IType[blindAccessorTypes.size()]);
	}

	private String createNonAccessibleMemberMessage(IMember member, IType accessingType, boolean moved){
		//Non-visibility can have various reasons and always displaying all visibility
		//flags for all enclosing elements would be too heavy. Context reveals exact cause.
		IType declaringType= moved ? getDestinationType() : getDeclaringType();
		String message;
		switch (member.getElementType()){
			case IJavaElement.FIELD: {
				if (moved)
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_moved_field,
								new String[]{JavaElementUtil.createFieldSignature((IField)member),
							getQualifiedTypeLabel(accessingType),
							getQualifiedTypeLabel(declaringType)});
				else
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_accessed_field,
								new String[]{JavaElementUtil.createFieldSignature((IField)member),
							getQualifiedTypeLabel(accessingType)});
				return message;
			}
			case IJavaElement.METHOD: {
				if (moved)
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_moved_method,
								new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
							getQualifiedTypeLabel(accessingType),
							getQualifiedTypeLabel(declaringType)});
				else
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_accessed_method,
								new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
							getQualifiedTypeLabel(accessingType)});

				return message;
			}
			case IJavaElement.TYPE:{
				if (moved)
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_moved_type,
								new String[]{getQualifiedTypeLabel((IType)member),
							getQualifiedTypeLabel(accessingType),
							getQualifiedTypeLabel(declaringType)});
				else
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_accessed_type,
								new String[]{ getQualifiedTypeLabel((IType)member), getQualifiedTypeLabel(accessingType)});
				return message;
			}
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	private String getQualifiedTypeLabel(IType accessingType) {
		return BasicElementLabels.getJavaCodeString(accessingType.getFullyQualifiedName('.'));
	}

	private static SearchResultGroup[] getReferences(IMember member, IProgressMonitor monitor, RefactoringStatus status) throws JavaModelException {
		SearchPattern pattern= SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		if (pattern == null) {
			return new SearchResultGroup[0];
		}
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(pattern);
		engine.setFiltering(true, true);
		engine.setScope(RefactoringScopeFactory.create(member));
		engine.setStatus(status);
		engine.searchPattern(Progress.subMonitor(monitor, 1));
		return (SearchResultGroup[]) engine.getResults();
	}

	private static boolean isVisibleFrom(IType newMemberDeclaringType, IType accessingType) throws JavaModelException {
		int memberVisibility= JdtFlags.getVisibilityCode(newMemberDeclaringType);

		IType declaringType= newMemberDeclaringType.getDeclaringType();
		while (declaringType != null) { //get lowest visibility in all parent types of newMemberDeclaringType
			memberVisibility= JdtFlags.getLowerVisibility(
					memberVisibility, JdtFlags.getVisibilityCode(declaringType));
			declaringType= declaringType.getDeclaringType();
		}

		switch (memberVisibility) {
			case Modifier.PRIVATE :
				return isEqualOrEnclosedType(accessingType, newMemberDeclaringType);

			case Modifier.NONE :
				return JavaModelUtil.isSamePackage(accessingType.getPackageFragment(), newMemberDeclaringType.getPackageFragment());

			case Modifier.PROTECTED :
				return JavaModelUtil.isSamePackage(accessingType.getPackageFragment(), newMemberDeclaringType.getPackageFragment())
						|| accessingType.newSupertypeHierarchy(null).contains(newMemberDeclaringType);

			case Modifier.PUBLIC :
				return true;

			default:
				Assert.isTrue(false);
				return false;
		}
	}

	private static boolean isEqualOrEnclosedType(IType inner, IType outer) {
		while (inner != null) {
			if (inner.equals(outer))
				return true;
			else
				inner= inner.getDeclaringType();
		}
		return false;
	}

	private boolean isWithinMemberToMove(SearchMatch result) throws JavaModelException {
		ICompilationUnit referenceCU= SearchUtils.getCompilationUnit(result);
		if (! referenceCU.equals(fSource.getCu()))
			return false;
		int referenceStart= result.getOffset();
		for (IMember memberToMove : fMembersToMove) {
			ISourceRange range= memberToMove.getSourceRange();
			if (range.getOffset() <= referenceStart && range.getOffset() + range.getLength() >= referenceStart)
				return true;
		}
		return false;
	}

	private RefactoringStatus checkNativeMovedMethods(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, fMembersToMove.length);
		RefactoringStatus result= new RefactoringStatus();
		for (IMember memberToMove : fMembersToMove) {
			if (memberToMove.getElementType() != IJavaElement.METHOD)
				continue;
			if (! JdtFlags.isNative(memberToMove))
				continue;
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_native,
				JavaElementUtil.createMethodSignature((IMethod) memberToMove));
			result.addWarning(message, JavaStatusContext.create(memberToMove));
			pm.worked(1);
		}
		pm.done();
		return result;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.done();
		return fChange;
	}

	private void createChange(List<ICompilationUnit> modifiedCus, RefactoringStatus status, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_creating, 5);
		fChange= new DynamicValidationRefactoringChange(createDescriptor(), RefactoringCoreMessages.MoveMembersRefactoring_move_members);
		fTarget= getCuRewrite(fDestinationType.getCompilationUnit());
		ITypeBinding targetBinding= getDestinationBinding();
		if (targetBinding == null) {
			status.addFatalError(Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_compile_errors, BasicElementLabels.getFileName(fTarget.getCu())));
			monitor.done();
			return;
		}

		try {
			Map<IMember, IncomingMemberVisibilityAdjustment> adjustments= new HashMap<>();
			IProgressMonitor sub= Progress.subMonitorSupressed(monitor, 1);
			sub.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_creating, fMembersToMove.length);
			Set<IMember> rewritten= new HashSet<>();
			for (IMember member : fMembersToMove) {
				final MemberVisibilityAdjustor adjustor= new MemberVisibilityAdjustor(fDestinationType, member);
				adjustor.setAdjustments(adjustments);
				adjustor.setStatus(status);
				adjustor.setVisibilitySeverity(RefactoringStatus.WARNING);
				adjustor.setFailureSeverity(RefactoringStatus.WARNING);
				adjustor.setRewrite(fSource.getASTRewrite(), fSource.getRoot());
				adjustor.adjustVisibility(new NullProgressMonitor());

				if (fDelegateUpdating && isDelegateCreationAvailable(member)) {
					// Add a visibility adjustment so the moved member
					// will be visible from within the delegate
					ModifierKeyword threshold= adjustor.getVisibilityThreshold(member, fDestinationType, new NullProgressMonitor());
					IncomingMemberVisibilityAdjustment adjustment= adjustments.get(member);
					ModifierKeyword kw= adjustment != null ? adjustment.getKeyword() : ModifierKeyword.fromFlagValue(JdtFlags.getVisibilityCode(member));
					if (MemberVisibilityAdjustor.hasLowerVisibility(kw, threshold)) {
						adjustments.put(member, new MemberVisibilityAdjustor.IncomingMemberVisibilityAdjustment(member, threshold, RefactoringStatus.createWarningStatus(Messages.format(MemberVisibilityAdjustor.getMessage(member), new String[] { MemberVisibilityAdjustor.getLabel(member), MemberVisibilityAdjustor.getLabel(threshold)}), JavaStatusContext.create(member))));
					}
				}

				// Check if destination type is visible from references ->
				// error message if not (for example, when moving into a private type)
				status.merge(checkMovedMemberAvailability(member, Progress.subMonitor(sub, 1)));
				// Put rewrite info into code and into status
				for (IMember iMember : rewritten) {
					adjustments.remove(iMember);
				}
				rewritten.addAll(adjustments.keySet());
				adjustor.rewriteVisibility(new NullProgressMonitor());
			}

			// First update references in moved members, in order to extract the
			// source.
			String[] memberSources= getUpdatedMemberSource(status, fMemberDeclarations, targetBinding);
			monitor.worked(1);
			if (status.hasFatalError())
				return;

			ReferencesInBinaryContext binaryRefs= new ReferencesInBinaryContext(RefactoringCoreMessages.ReferencesInBinaryContext_ref_in_binaries_description_plural);
			IJavaSearchScope scope= RefactoringScopeFactory.create(fMembersToMove, false);
			SearchPattern pattern= RefactoringSearchEngine.createOrPattern(fMembersToMove, IJavaSearchConstants.ALL_OCCURRENCES);
			final HashSet<ICompilationUnit> affectedCompilationUnits= new HashSet<>();

			CollectingSearchRequestor requestor= new CollectingSearchRequestor(binaryRefs) {
				private ICompilationUnit fLastCU;
				@Override
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					if (filterMatch(match))
						return;
					if (match.getAccuracy() == SearchMatch.A_INACCURATE)
						return;
					ICompilationUnit unit= SearchUtils.getCompilationUnit(match);
					if (unit != null && ! unit.equals(fLastCU)) {
						fLastCU= unit;
						affectedCompilationUnits.add(unit);
					}
				}
			};
			RefactoringSearchEngine.search(pattern, scope, requestor, new NullProgressMonitor(), status);
			binaryRefs.addErrorIfNecessary(status);
			ICompilationUnit[] units= affectedCompilationUnits.toArray(new ICompilationUnit[affectedCompilationUnits.size()]);

			modifiedCus.addAll(Arrays.asList(units));
			final MemberVisibilityAdjustor adjustor= new MemberVisibilityAdjustor(fDestinationType, fDestinationType);
			sub= Progress.subMonitor(monitor, 1);
			sub.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_creating, units.length);
			for (ICompilationUnit unit : units) {
				CompilationUnitRewrite rewrite= getCuRewrite(unit);
				adjustor.setRewrites(Collections.singletonMap(unit, rewrite));
				adjustor.setAdjustments(adjustments);
				adjustor.rewriteVisibility(unit, Progress.subMonitor(sub, 1));
				ReferenceAnalyzer analyzer= new ReferenceAnalyzer(rewrite, fMemberBindings, targetBinding, fSourceBinding);
				rewrite.getRoot().accept(analyzer);
				status.merge(analyzer.getStatus());
				if (status.hasFatalError()) {
					fChange= null;
					return;
				}
				if (!fSource.getCu().equals(unit) && !fTarget.getCu().equals(unit))
					fChange.add(rewrite.createChange(true));
			}
			status.merge(moveMembers(fMemberDeclarations, memberSources));
			fChange.add(fSource.createChange(true));
			modifiedCus.add(fSource.getCu());
			if (!fSource.getCu().equals(fTarget.getCu())) {
				fChange.add(fTarget.createChange(true));
				modifiedCus.add(fTarget.getCu());
			}
			monitor.worked(1);
		} catch (BadLocationException exception) {
			JavaManipulationPlugin.log(exception);
		}
	}

	private MoveStaticMembersDescriptor createDescriptor() {
		final IMember[] members= getMembersToMove();
		String project= null;
		final IJavaProject javaProject= getDeclaringType().getJavaProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		String header= null;
		if (members.length == 1)
			header= Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_descriptor_description_single, new String[] { JavaElementLabelsCore.getElementLabel(members[0], JavaElementLabelsCore.ALL_FULLY_QUALIFIED), getQualifiedTypeLabel(fDestinationType) });
		else
			header= Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_descriptor_description_multi, new String[] { String.valueOf(members.length), getQualifiedTypeLabel(fDestinationType) });
		int flags= JavaRefactoringDescriptor.JAR_MIGRATION | JavaRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
		final IType declaring= members[0].getDeclaringType();
		try {
			if (declaring.isLocal() || declaring.isAnonymous())
				flags|= JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		} catch (JavaModelException exception) {
			JavaManipulationPlugin.log(exception);
		}
		final String description= members.length == 1 ? Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_description_descriptor_short_multi, BasicElementLabels.getJavaElementName(members[0].getElementName())) : RefactoringCoreMessages.MoveMembersRefactoring_move_members;
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_target_element_pattern, getQualifiedTypeLabel(fDestinationType)));
		final MoveStaticMembersDescriptor descriptor= RefactoringSignatureDescriptorFactory.createMoveStaticMembersDescriptor();
		descriptor.setProject(project);
		descriptor.setDescription(description);
		descriptor.setComment(comment.asString());
		descriptor.setFlags(flags);
		descriptor.setDestinationType(fDestinationType);
		descriptor.setKeepOriginal(fDelegateUpdating);
		descriptor.setDeprecateDelegate(fDelegateDeprecation);
		descriptor.setMembers(members);
		return descriptor;
	}

	private CompilationUnitRewrite getCuRewrite(ICompilationUnit unit) {
		if (fSource.getCu().equals(unit))
			return fSource;
		if (fTarget != null && fTarget.getCu().equals(unit))
			return fTarget;
		return new CompilationUnitRewrite(unit);
	}

	private AbstractTypeDeclaration getDestinationNode() throws JavaModelException {
		AbstractTypeDeclaration destination= ASTNodes.getParent(
				NodeFinder.perform(fTarget.getRoot(), fDestinationType.getNameRange()),
				AbstractTypeDeclaration.class);
		return destination;
	}

	private ITypeBinding getDestinationBinding() throws JavaModelException {
		ASTNode node= NodeFinder.perform(fTarget.getRoot(), fDestinationType.getNameRange());
		if (!(node instanceof SimpleName))
			return null;
		IBinding binding= ((SimpleName)node).resolveBinding();
		if (!(binding instanceof ITypeBinding))
			return null;
		return (ITypeBinding)binding;
	}

	private IBinding[] getMemberBindings() throws JavaModelException {
		IBinding[] result= new IBinding[fMembersToMove.length];
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			SimpleName name= (SimpleName)NodeFinder.perform(fSource.getRoot(), member.getNameRange());
			result[i]= name.resolveBinding();
		}
		return result;
	}

	private class MethodTypeVisitor extends ASTVisitor {

		final ASTRewrite fASTRewrite;
		final Set<String> fImportedTypeNames;
		public MethodTypeVisitor(final ASTRewrite astRewrite, final Set<String> importedTypeNames) {
			this.fASTRewrite= astRewrite;
			this.fImportedTypeNames= importedTypeNames;
		}

		@Override
		public boolean visit(SimpleName node) {
			if (fImportedTypeNames.contains(node.getFullyQualifiedName())) {
				if (node.getLocationInParent() == SimpleType.NAME_PROPERTY) {
					ITypeBinding typeBinding= node.resolveTypeBinding();
					if (typeBinding != null && !typeBinding.isLocal()) {
						AST ast= fASTRewrite.getAST();
						Type newType= ast.newSimpleType(ast.newName(typeBinding.getQualifiedName()));
						fASTRewrite.set(node.getParent(), node.getLocationInParent(), newType, null);
					}
				}
			}
			return super.visit(node);
		}
	}
	private String[] getUpdatedMemberSource(RefactoringStatus status, BodyDeclaration[] members, ITypeBinding target) throws CoreException, BadLocationException {
		List<IBinding> typeRefs= new ArrayList<>();
		boolean targetNeedsSourceImport= false;
		boolean isSourceNotTarget= fSource != fTarget;
		Set<IBinding> exclude= new HashSet<>();
		for (BodyDeclaration declaration : members) {
			if (declaration instanceof AbstractTypeDeclaration) {
				AbstractTypeDeclaration type= (AbstractTypeDeclaration) declaration;
				ITypeBinding binding= type.resolveBinding();
				if (binding != null)
					exclude.add(binding);
			} else if (declaration instanceof MethodDeclaration) {
				MethodDeclaration method= (MethodDeclaration) declaration;
				IMethodBinding binding= method.resolveBinding();
				if (binding != null)
					exclude.add(binding);
			} else if (declaration instanceof FieldDeclaration) {
				FieldDeclaration field= (FieldDeclaration) declaration;
				for (final Iterator<VariableDeclarationFragment> iterator= field.fragments().iterator(); iterator.hasNext();) {
					VariableDeclarationFragment fragment= iterator.next();
					IVariableBinding binding= fragment.resolveBinding();
					if (binding != null)
						exclude.add(binding);
				}
			}
		}
		ImportRewriteContext context= new ContextSensitiveImportRewriteContext(getDestinationNode(), fTarget.getImportRewrite());
		for (BodyDeclaration declaration : members) {
			if (isSourceNotTarget)
				typeRefs.addAll(TypeReferenceFinder.perform(declaration));
			MovedMemberAnalyzer analyzer= new MovedMemberAnalyzer(fSource, fMemberBindings, fSourceBinding, target);
			declaration.accept(analyzer);
			ImportRewriteUtil.addImports(fTarget, context, declaration, new HashMap<>(), new HashMap<>(), exclude, false);
			if (getDeclaringType().isInterface() && !fDestinationType.isInterface()) {
				if (declaration instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) declaration;
					int psfModifiers= Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
					if ((fieldDecl.getModifiers() & psfModifiers) != psfModifiers) {
						ModifierRewrite.create(fSource.getASTRewrite(), fieldDecl).setModifiers(psfModifiers, null);
					}
				} else if (declaration instanceof AbstractTypeDeclaration) {
					AbstractTypeDeclaration typeDecl= (AbstractTypeDeclaration) declaration;
					int psModifiers= Modifier.PUBLIC | Modifier.STATIC;
					if ((typeDecl.getModifiers() & psModifiers) != psModifiers) {
						ModifierRewrite.create(fSource.getASTRewrite(), typeDecl).setModifiers(typeDecl.getModifiers() | psModifiers, null);
					}
				} else if (declaration instanceof MethodDeclaration) {
					MethodDeclaration methodDecl= (MethodDeclaration) declaration;
					int psModifiers= Modifier.PUBLIC | Modifier.STATIC;
					if ((methodDecl.getModifiers() & psModifiers) != psModifiers) {
						ModifierRewrite.create(fSource.getASTRewrite(), methodDecl).setModifiers(methodDecl.getModifiers() | psModifiers, null);
					}
				}
			}

			if (declaration instanceof MethodDeclaration methodDecl) {
				Set<String> importedTypeNames= getImportedTypeNames(fSource.getCu(), fTarget.getCu());
				MethodTypeVisitor visitor= new MethodTypeVisitor(fSource.getASTRewrite(), importedTypeNames);
				methodDecl.accept(visitor);
			}

			if (fDestinationType.isInterface()) {
				int modifiers= declaration.getModifiers();
				modifiers= JdtFlags.clearAccessModifiers(modifiers);
				modifiers= JdtFlags.clearFlag(Modifier.ABSTRACT | Modifier.FINAL, modifiers);
				if (!(declaration instanceof MethodDeclaration)) {
					modifiers= JdtFlags.clearFlag(Modifier.STATIC, modifiers);
				}
				ModifierRewrite.create(fSource.getASTRewrite(), declaration).setModifiers(modifiers, null);
			}
			ITrackedNodePosition trackedPosition= fSource.getASTRewrite().track(declaration);
			declaration.setProperty(TRACKED_POSITION_PROPERTY, trackedPosition);
			targetNeedsSourceImport|= analyzer.targetNeedsSourceImport();
			status.merge(analyzer.getStatus());
		}
		// Adjust imports
		if (targetNeedsSourceImport && isSourceNotTarget) {
			fTarget.getImportRewrite().addImport(fSourceBinding, context);
		}
		if (isSourceNotTarget) {
			for (IBinding iBinding : typeRefs) {
				ITypeBinding binding= (ITypeBinding) iBinding;
				fTarget.getImportRewrite().addImport(binding, context);
			}
		}
		// extract updated members
		String[] updatedMemberSources= new String[members.length];
		IDocument document= new Document(fSource.getCu().getBuffer().getContents());
		TextEdit edit= fSource.getASTRewrite().rewriteAST(document, fSource.getCu().getOptions(true));
		edit.apply(document, TextEdit.UPDATE_REGIONS);
		for (int i= 0; i < members.length; i++) {
			updatedMemberSources[i]= getUpdatedMember(document, members[i]);
		}
		fSource.clearASTRewrite();
		return updatedMemberSources;
	}

	public static Set<String> getImportedTypeNames(ICompilationUnit sourceCu, ICompilationUnit targetCu) throws JavaModelException {
		IImportDeclaration[] targetImports= targetCu.getImports();
		Set<String> targetImportNames= new HashSet<>();
		for (IImportDeclaration importDecl : targetImports) {
			targetImportNames.add(importDecl.getElementName());
		}
		IImportDeclaration[] sourceImports= sourceCu.getImports();
		for (IImportDeclaration importDecl : sourceImports) {
			String name= importDecl.getElementName();
			if (targetImportNames.contains(name)) {
				targetImportNames.remove(name);
			}
		}
		Set<String> importedTypeNames= new HashSet<>();
		for (String targetImportName : targetImportNames) {
			int index= targetImportName.lastIndexOf("."); //$NON-NLS-1$
			if (index > 0) {
				importedTypeNames.add(targetImportName.substring(index + 1));
			} else {
				importedTypeNames.add(targetImportName);
			}
		}
		return importedTypeNames;
	}

	private String getUpdatedMember(IDocument document, BodyDeclaration declaration) throws BadLocationException {
		ITrackedNodePosition trackedPosition= (ITrackedNodePosition) declaration.getProperty(TRACKED_POSITION_PROPERTY);
		return Strings.trimIndentation(document.get(trackedPosition.getStartPosition(), trackedPosition.getLength()), fPreferences.tabWidth, fPreferences.indentWidth, false);
	}

	private RefactoringStatus moveMembers(BodyDeclaration[] members, String[] sources) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		AbstractTypeDeclaration destination= getDestinationNode();
		ListRewrite containerRewrite= fTarget.getASTRewrite().getListRewrite(destination, destination.getBodyDeclarationsProperty());

		TextEditGroup delete= fSource.createGroupDescription(RefactoringCoreMessages.MoveMembersRefactoring_deleteMembers);
		TextEditGroup add= fTarget.createGroupDescription(RefactoringCoreMessages.MoveMembersRefactoring_addMembers);
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			ASTNode removeImportsOf= null;
			boolean addedDelegate= false;

			if (fDelegateUpdating) {
				if (declaration instanceof MethodDeclaration) {

					DelegateMethodCreator creator= new DelegateMethodCreator();
					creator.setDeclaration(declaration);
					creator.setDeclareDeprecated(fDelegateDeprecation);
					creator.setSourceRewrite(fSource);
					creator.setCopy(false);
					creator.setNewLocation(getDestinationBinding());
					creator.prepareDelegate();
					creator.createEdit();

					removeImportsOf= ((MethodDeclaration) declaration).getBody();
					addedDelegate= true;
				}
				if (declaration instanceof FieldDeclaration) {

					// Note: this FieldDeclaration only has one fragment (@see #getASTMembers(RefactoringStatus))
					final VariableDeclarationFragment frag= (VariableDeclarationFragment) ((FieldDeclaration) declaration).fragments().get(0);

					if (!Modifier.isFinal(declaration.getModifiers())) {
						// Don't create a delegate for non-final fields
						result.addInfo(Messages.format(RefactoringCoreMessages.DelegateCreator_cannot_create_field_delegate_not_final, BasicElementLabels.getJavaElementName(frag.getName().getIdentifier())), null);
					} else if (frag.getInitializer() == null) {
						// Don't create a delegate without an initializer.
						result.addInfo(Messages.format(RefactoringCoreMessages.DelegateCreator_cannot_create_field_delegate_no_initializer, BasicElementLabels.getJavaElementName(frag.getName().getIdentifier())), null);
					} else {
						DelegateFieldCreator creator= new DelegateFieldCreator();
						creator.setDeclaration(declaration);
						creator.setDeclareDeprecated(fDelegateDeprecation);
						creator.setSourceRewrite(fSource);
						creator.setCopy(false);
						creator.setNewLocation(getDestinationBinding());
						creator.prepareDelegate();
						creator.createEdit();

						removeImportsOf= frag.getInitializer();
						addedDelegate= true;
					}
				}
				if (declaration instanceof AbstractTypeDeclaration) {
					result.addInfo(Messages.format(RefactoringCoreMessages.DelegateCreator_cannot_create_delegate_for_type, BasicElementLabels.getJavaElementName(((AbstractTypeDeclaration) declaration).getName().getIdentifier())),
							null);
				}
			}

			if (!addedDelegate) {
				fSource.getASTRewrite().remove(declaration, delete);
				removeImportsOf= declaration;
			}

			if (removeImportsOf != null && fSource != fTarget)
				fSource.getImportRemover().registerRemovedNode(removeImportsOf);

			ASTNode node= fTarget.getASTRewrite().createStringPlaceholder(sources[i], declaration.getNodeType());
			List<BodyDeclaration> container= containerRewrite.getRewrittenList();
			int insertionIndex= BodyDeclarationRewrite.getInsertionIndex((BodyDeclaration) node, container);
			containerRewrite.insertAt(node, insertionIndex, add);
		}
		return result;
	}

	private BodyDeclaration[] getASTMembers(RefactoringStatus status) throws JavaModelException {
		BodyDeclaration[] result= new BodyDeclaration[fMembersToMove.length];
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			ASTNode node= NodeFinder.perform(fSource.getRoot(), member.getNameRange());
			result[i]= ASTNodes.getParent(node, BodyDeclaration.class);

			//Fix for bug 42383: exclude multiple VariableDeclarationFragments ("int a=1, b=2")
			//ReferenceAnalyzer#visit(FieldDeclaration node) depends on fragments().size() != 1 !
			if (result[i] instanceof FieldDeclaration
					&& ((FieldDeclaration) result[i]).fragments().size() != 1) {
				status.addFatalError(RefactoringCoreMessages.MoveMembersRefactoring_multi_var_fields);
				return result;
			}

		}

		//Sorting members is important for field declarations referring to previous fields.
		Arrays.sort(result, (o1, o2) -> o1.getStartPosition() - o2.getStartPosition());
		return result;
	}

	private RefactoringStatus initialize(JavaRefactoringArguments extended) {
		String handle= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
		if (handle != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
			if (element == null || !element.exists() || element.getElementType() != IJavaElement.TYPE)
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getProcessorName(), IJavaRefactorings.MOVE_STATIC_MEMBERS);
			else {
				fDestinationType= (IType) element;
				fDestinationTypeName= fDestinationType.getFullyQualifiedName();
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
		final String delegate= extended.getAttribute(ATTRIBUTE_DELEGATE);
		if (delegate != null) {
			fDelegateUpdating= Boolean.parseBoolean(delegate);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELEGATE));
		final String deprecate= extended.getAttribute(ATTRIBUTE_DEPRECATE);
		if (deprecate != null) {
			fDelegateDeprecation= Boolean.parseBoolean(deprecate);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DEPRECATE));
		int count= 1;
		final List<IJavaElement> elements= new ArrayList<>();
		String attribute= JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + count;
		final RefactoringStatus status= new RefactoringStatus();
		while ((handle= extended.getAttribute(attribute)) != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
			if (element == null || !element.exists())
				status.merge(JavaRefactoringDescriptorUtil.createInputWarningStatus(element, getProcessorName(), IJavaRefactorings.MOVE_STATIC_MEMBERS));
			else
				elements.add(element);
			count++;
			attribute= JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + count;
		}
		fMembersToMove= elements.toArray(new IMember[elements.size()]);
		if (elements.isEmpty())
			return JavaRefactoringDescriptorUtil.createInputFatalStatus(null, getProcessorName(), IJavaRefactorings.MOVE_STATIC_MEMBERS);
		IJavaProject project= null;
		if (fMembersToMove.length > 0)
			project= fMembersToMove[0].getJavaProject();
		fPreferences= JavaPreferencesSettings.getCodeGenerationSettings(project);
		if (!status.isOK())
			return status;
		return new RefactoringStatus();
	}

	@Override
	public String getDelegateUpdatingTitle(boolean plural) {
		if (plural)
			return RefactoringCoreMessages.DelegateMethodCreator_keep_original_moved_plural_member;
		else
			return RefactoringCoreMessages.DelegateMethodCreator_keep_original_moved_singular_member;
	}
}
