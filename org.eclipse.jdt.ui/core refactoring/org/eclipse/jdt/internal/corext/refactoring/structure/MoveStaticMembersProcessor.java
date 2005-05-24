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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.refactoring.IRefactoringProcessorIds;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public final class MoveStaticMembersProcessor extends MoveProcessor {
	
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

	private static class TypeReferenceFinder extends ASTVisitor {
		List fResult= new ArrayList();
		Set fDefined= new HashSet();
		public static List perform(ASTNode root) {
			TypeReferenceFinder visitor= new TypeReferenceFinder();
			root.accept(visitor);
			return visitor.fResult;
		}
		public boolean visit(TypeDeclaration node) {
			fDefined.add(node.resolveBinding());
			return true;
		}
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (!(binding instanceof ITypeBinding))
				return true;
			if (!fDefined.contains(binding))
				fResult.add(binding);
			return true;
		}

		public boolean visit(AnnotationTypeDeclaration node) {
			fDefined.add(node.resolveBinding());
			return true;
		}

		public boolean visit(EnumDeclaration node) {
			fDefined.add(node.resolveBinding());
			return true;
		}
	}

	private MoveStaticMembersProcessor(IMember[] elements, CodeGenerationSettings preferenceSettings) {
		Assert.isNotNull(elements);
		Assert.isNotNull(preferenceSettings);
		fMembersToMove= elements;
		fPreferences= preferenceSettings;
	}
	
	public static MoveStaticMembersProcessor create(IMember[] elements, CodeGenerationSettings preferenceSettings) throws JavaModelException{
		if (! RefactoringAvailabilityTester.isMoveStaticMembersAvailable(elements))
			return null;
		return new MoveStaticMembersProcessor(elements, preferenceSettings);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isMoveStaticMembersAvailable(fMembersToMove);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object[] getElements() {
		Object[] result= new Object[fMembersToMove.length];
		System.arraycopy(fMembersToMove, 0, result, 0, fMembersToMove.length);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIdentifier() {
		return IRefactoringProcessorIds.MOVE_STATIC_MEMBERS_PROCESSOR;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		List result= new ArrayList();
		MoveArguments args= new MoveArguments(fDestinationType, true);
		String[] natures= JavaProcessors.computeAffectedNaturs(fMembersToMove);
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			result.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(
				status, this, member, args, natures, sharedParticipants)));
		}
		return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
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
					RefactoringCoreMessages.MoveMembersRefactoring_compile_errors, //$NON-NLS-1$
					fSource.getCu().getElementName()));
			}
			fMemberDeclarations= getASTMembers(result);
			return result;
		} finally {
			pm.done();
		}	
	}
	
	private boolean hasUnresolvedMemberBinding() {
		for (int i= 0; i < fMemberBindings.length; i++) {
			if (fMemberBindings[i] == null)
				return true;
		}
		return false;
	}
	
	private RefactoringStatus checkDeclaringType(){
		IType declaringType= getDeclaringType();
				
		if (JavaModelUtil.getFullyQualifiedName(declaringType).equals("java.lang.Object")) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MoveMembersRefactoring_Object);	 

		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MoveMembersRefactoring_binary);	 

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MoveMembersRefactoring_read_only);	 
		
		return null;
	}
	
	//---- Input checking ------------------------------------

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
			
			result.merge(checkAccessedMembersAvailability(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;

			result.merge(checkMovedMembersAvailability(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;
			
			result.merge(checkNativeMovedMethods(new SubProgressMonitor(pm, 1)));
			
			if (result.hasFatalError())
				return result;
			
			List modifiedCus= new ArrayList();
			createChange(modifiedCus, result, new SubProgressMonitor(pm, 7));
			ValidateEditChecker checker= (ValidateEditChecker)context.getChecker(ValidateEditChecker.class);
			checker.addFiles(getAllFilesToModify(modifiedCus));
			
			return result;
		} finally {
			pm.done();
		}	
	}
	
	private IFile[] getAllFilesToModify(List modifiedCus) {
		Set result= new HashSet();
		IResource resource= fDestinationType.getCompilationUnit().getResource();
		result.add(resource);
		for (int i= 0; i < fMembersToMove.length; i++) {
			resource= fMembersToMove[i].getCompilationUnit().getResource();
			if (resource != null)
				result.add(resource);
		}
		for (Iterator iter= modifiedCus.iterator(); iter.hasNext();) {
			ICompilationUnit unit= (ICompilationUnit)iter.next();
			if (unit.getResource() != null)
				result.add(unit.getResource());
		}
		return (IFile[]) result.toArray(new IFile[result.size()]);
	}

	private RefactoringStatus checkDestinationType() throws JavaModelException {			
		if (fDestinationType == null){
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_not_found, fDestinationTypeName);
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

		if (fDestinationType.isInterface() && ! getDeclaringType().isInterface())
			result.merge(checkFieldsForInterface());
		if (result.hasFatalError())
			return result;

		// no checking required for moving interface fields to classes
				
		if (! ((JdtFlags.isStatic(fDestinationType)) || (fDestinationType.getDeclaringType() == null))){
			String message= RefactoringCoreMessages.MoveMembersRefactoring_static_declaration; 
			result.addError(message);
		}	
				
		return result;	
	}
	
	private RefactoringStatus checkDestinationInsideTypeToMove() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (! (fMembersToMove[i] instanceof IType))
				continue;
			IType type= (IType) fMembersToMove[i];
			if (fDestinationType.equals(type) || JavaElementUtil.isAncestorOf(type, fDestinationType)) {
				String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_inside, 
						new String[] {JavaModelUtil.getFullyQualifiedName(type),
								JavaModelUtil.getFullyQualifiedName(fDestinationType)});
				RefactoringStatusContext context= JavaStatusContext.create(fDestinationType.getCompilationUnit(), fDestinationType.getNameRange());
				result.addFatalError(message, context);
				return result;
			}
		}
		return result;
	}

	private RefactoringStatus checkFieldsForInterface() throws JavaModelException {
		//could be more clever and make field final if it is only written once...
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (! canMoveToInterface(fMembersToMove[i])) {
				String message= RefactoringCoreMessages.MoveMembersRefactoring_only_public_static; 
				result.addError(message, JavaStatusContext.create(fMembersToMove[i]));
			}
		}
		return result;
	}

	private boolean canMoveToInterface(IMember member) throws JavaModelException {
		int flags= member.getFlags();
		switch (member.getElementType()) {
			case IJavaElement.FIELD:
				if (!(Flags.isPublic(flags) && Flags.isStatic(flags) && Flags.isFinal(flags)))
					return false;
				if (Flags.isEnum(flags))
					return false;
				VariableDeclarationFragment declaration= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, fSource.getRoot());
				if (declaration != null)
					return declaration.getInitializer() != null;

			case IJavaElement.TYPE:
				return (Flags.isPublic(flags) && Flags.isStatic(flags));

			default:
				return false;
		}
	}

	private RefactoringStatus checkAccessedMembersAvailability(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, 3); 
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAccessedMethodsAvailability(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFieldsAvailability(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedTypesAvailability(new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethodsAvailability(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fMembersToMove, pm);
		List movedElementList= Arrays.asList(fMembersToMove);
		for (int i= 0; i < accessedMethods.length; i++) {
			if (containsAncestorOf(movedElementList, accessedMethods[i]))
				continue;
			if (! JdtFlags.isStatic(accessedMethods[i])) //safely ignore non-static 
				continue;
			if (! isVisibleFrom(accessedMethods[i], accessedMethods[i].getDeclaringType(), fDestinationType)){
				String msg= createNonAccessibleMemberMessage(accessedMethods[i], fDestinationType, false);
				result.addError(msg, JavaStatusContext.create(accessedMethods[i]));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedTypesAvailability(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= ReferenceFinderUtil.getTypesReferencedIn(fMembersToMove, pm);
		List movedElementList= Arrays.asList(fMembersToMove);
		for (int i= 0; i < accessedTypes.length; i++) {
			if (containsAncestorOf(movedElementList, accessedTypes[i]))
				continue;
			if (! JdtFlags.isStatic(accessedTypes[i])) //safely ignore non-static 
				continue;
			if (! isVisibleFrom(accessedTypes[i], accessedTypes[i].getDeclaringType(), fDestinationType)){
				String msg= createNonAccessibleMemberMessage(accessedTypes[i], fDestinationType, false);
				result.addError(msg, JavaStatusContext.create(accessedTypes[i]));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedFieldsAvailability(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fMembersToMove, pm);
		List movedElementList= Arrays.asList(fMembersToMove);
		for (int i= 0; i < accessedFields.length; i++) {
			if (containsAncestorOf(movedElementList, accessedFields[i]))
				continue;
			if (! JdtFlags.isStatic(accessedFields[i])) //safely ignore non-static 
				continue;
			if (! isVisibleFrom(accessedFields[i], accessedFields[i].getDeclaringType(), fDestinationType)){
				String msg= createNonAccessibleMemberMessage(accessedFields[i], fDestinationType, false);
				result.addError(msg, JavaStatusContext.create(accessedFields[i]));
			}	
		}
		return result;
	}
	
	private boolean containsAncestorOf(List movedElementList, IMember accessedMember) {
		IJavaElement element= accessedMember;
		do {
			if (movedElementList.contains(element))
				return true;
			element= element.getParent();
		} while (element instanceof IMember);
		return false;
	}

	private RefactoringStatus checkMovedMembersAvailability(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", fMembersToMove.length); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.MoveMembersRefactoring_check_availability); 
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembersToMove.length; i++) {
			result.merge(checkMovedMemberAvailability(fMembersToMove[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkMovedMemberAvailability(IMember memberToMove, IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (memberToMove instanceof IType) { // recursively check accessibility of member type's members
			IJavaElement[] typeMembers= ((IType) memberToMove).getChildren();
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, typeMembers.length + 1); 
			for (int i= 0; i < typeMembers.length; i++) {
				if (typeMembers[i] instanceof IInitializer)
					pm.worked(1);
				else
					result.merge(checkMovedMemberAvailability((IMember) typeMembers[i], new SubProgressMonitor(pm, 1)));
			}
		} else {
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, 1); 
		}

		IType[] blindAccessorTypes= getTypesNotSeeingMovedMember(memberToMove, new SubProgressMonitor(pm, 1), result);
		for (int k= 0; k < blindAccessorTypes.length; k++) {
			String message= createNonAccessibleMemberMessage(memberToMove, blindAccessorTypes[k],/*moved*/true);
			result.addError(message, JavaStatusContext.create(memberToMove));
		}
		pm.done();
		return result;
	}
	
	private IType[] getTypesNotSeeingMovedMember(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		if (JdtFlags.isPublic(member) && JdtFlags.isPublic(fDestinationType))
			return new IType[0];

		HashSet blindAccessorTypes= new HashSet(); // referencing, but access to destination type illegal
		SearchResultGroup[] references= getReferences(member, new SubProgressMonitor(pm, 1), status);
		for (int i = 0; i < references.length; i++) {
			SearchMatch[] searchResults= references[i].getSearchResults();
			for (int k= 0; k < searchResults.length; k++) {
				SearchMatch searchResult= searchResults[k];
				IJavaElement element= SearchUtils.getEnclosingJavaElement(searchResult);
				IType type= (IType) element.getAncestor(IJavaElement.TYPE);
				if (type != null //reference can e.g. be an import declaration
						&& ! blindAccessorTypes.contains(type)
						&& ! isWithinMemberToMove(searchResult)
						&& ! isVisibleFrom(member, getDestinationType(), type)) {
					blindAccessorTypes.add(type);
				}
			}
		}
		
		return (IType[]) blindAccessorTypes.toArray(new IType[blindAccessorTypes.size()]);
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
									JavaModelUtil.getFullyQualifiedName(accessingType),
									JavaModelUtil.getFullyQualifiedName(declaringType)});
				else
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_accessed_field, 
								new String[]{JavaElementUtil.createFieldSignature((IField)member), 
									JavaModelUtil.getFullyQualifiedName(accessingType)});
				return message;
			}			
			case IJavaElement.METHOD: {
				if (moved)
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_moved_method, 
								new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
									JavaModelUtil.getFullyQualifiedName(accessingType),
									JavaModelUtil.getFullyQualifiedName(declaringType)});
				else				 
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_accessed_method, 
								new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
									JavaModelUtil.getFullyQualifiedName(accessingType)});
								 
				return message;		
			}			
			case IJavaElement.TYPE:{
				if (moved)
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_moved_type, 
								new String[]{JavaModelUtil.getFullyQualifiedName(((IType)member)), 
									JavaModelUtil.getFullyQualifiedName(accessingType),
									JavaModelUtil.getFullyQualifiedName(declaringType)});
				else
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_accessed_type, 
								new String[]{JavaModelUtil.getFullyQualifiedName(((IType)member)), 
									JavaModelUtil.getFullyQualifiedName(accessingType)});
				return message;
			}			
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	private static SearchResultGroup[] getReferences(IMember member, IProgressMonitor monitor, RefactoringStatus status) throws JavaModelException {
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
		engine.setFiltering(true, true);
		engine.setScope(RefactoringScopeFactory.create(member));
		engine.setStatus(status);
		engine.searchPattern(new SubProgressMonitor(monitor, 1));
		return (SearchResultGroup[]) engine.getResults();
	}

	private static boolean isVisibleFrom(IMember member, IType newMemberDeclaringType, IType accessingType) throws JavaModelException{
		int memberVisibility= JdtFlags.getLowerVisibility(
			JdtFlags.getVisibilityCode(member),
			JdtFlags.getVisibilityCode(newMemberDeclaringType));
			
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
		for (int i= 0; i < fMembersToMove.length; i++) {
			ISourceRange range= fMembersToMove[i].getSourceRange();
			if (range.getOffset() <= referenceStart && range.getOffset() + range.getLength() >= referenceStart)
				return true;
		}
		return false;
	}

	private RefactoringStatus checkNativeMovedMethods(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, fMembersToMove.length); 
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (fMembersToMove[i].getElementType() != IJavaElement.METHOD)
				continue;
			if (! JdtFlags.isNative(fMembersToMove[i]))
				continue;
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_native, 
				JavaElementUtil.createMethodSignature((IMethod)fMembersToMove[i]));
			result.addWarning(message, JavaStatusContext.create(fMembersToMove[i]));
			pm.worked(1);
		}
		pm.done();
		return result;		
	}
	
	//---- change creation ---------------------------------------
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.done();
		return fChange;
	}
	
	private void createChange(List modifiedCus, RefactoringStatus status, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_creating, 5); 
		fChange= new DynamicValidationStateChange(RefactoringCoreMessages.MoveMembersRefactoring_move_members); 
		fTarget= getCuRewrite(fDestinationType.getCompilationUnit());
		ITypeBinding targetBinding= getDestinationBinding();
		if (targetBinding == null) {
			status.addFatalError(Messages.format(
				RefactoringCoreMessages.MoveMembersRefactoring_compile_errors, //$NON-NLS-1$
				fTarget.getCu().getElementName()));
			monitor.done();
			return;
		}
		
		try {
			// First update references in moved members, in order to extract the source.
			String[] memberSources= getUpdatedMemberSource(status, fMemberDeclarations, targetBinding);
			monitor.worked(1);
			if (status.hasFatalError())
				return;
			Map adjustments= new HashMap();
			IMember member= null;
			SubProgressMonitor sub= new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
			sub.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_creating, fMembersToMove.length); 
			for (int index= 0; index < fMembersToMove.length; index++) {
				member= fMembersToMove[index];
				final MemberVisibilityAdjustor adjustor= new MemberVisibilityAdjustor(fDestinationType, member);
				adjustor.setAdjustments(adjustments);
				adjustor.setStatus(status);
				adjustor.setGetters(true);
				adjustor.setSetters(true);
				adjustor.setVisibilitySeverity(RefactoringStatus.WARNING);
				adjustor.setFailureSeverity(RefactoringStatus.WARNING);
				adjustor.adjustVisibility(new NullProgressMonitor());
			}
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2();
			engine.setPattern(fMembersToMove, IJavaSearchConstants.REFERENCES);
			engine.setGranularity(RefactoringSearchEngine2.GRANULARITY_COMPILATION_UNIT);
			engine.setFiltering(true, true);
			engine.setScope(RefactoringScopeFactory.create(fMembersToMove));
			engine.setStatus(status);
			engine.searchPattern(new NullProgressMonitor());
			ICompilationUnit[] units= engine.getAffectedCompilationUnits();
			modifiedCus.addAll(Arrays.asList(units));
			final MemberVisibilityAdjustor adjustor= new MemberVisibilityAdjustor(fDestinationType, fDestinationType);
			sub= new SubProgressMonitor(monitor, 1);
			sub.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_creating, units.length); 
			for (int index= 0; index < units.length; index++) {
				ICompilationUnit unit= units[index];
				CompilationUnitRewrite rewrite= getCuRewrite(unit);
				adjustor.setRewrites(Collections.singletonMap(unit, rewrite));
				adjustor.setAdjustments(adjustments);
				adjustor.rewriteVisibility(unit, new SubProgressMonitor(sub, 1));
				ReferenceAnalyzer analyzer= new ReferenceAnalyzer(rewrite, fMemberBindings, targetBinding, fSourceBinding);
				rewrite.getRoot().accept(analyzer);
				status.merge(analyzer.getStatus());
				if (status.hasFatalError()) {
					fChange= null;
					return;
				}
				if (analyzer.needsTargetImport())
					rewrite.getImportRewrite().addImport(targetBinding);
				if (!(fSource.getCu().equals(unit) || fTarget.getCu().equals(unit)))
					fChange.add(rewrite.createChange());
			}
			status.merge(moveMembers(fMemberDeclarations, memberSources));
			fChange.add(fSource.createChange());
			modifiedCus.add(fSource.getCu());
			if (! fSource.getCu().equals(fTarget.getCu())) {
				fChange.add(fTarget.createChange());
				modifiedCus.add(fTarget.getCu());
			}
			monitor.worked(1);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
	}
	
	private CompilationUnitRewrite getCuRewrite(ICompilationUnit unit) {
		if (fSource.getCu().equals(unit))
			return fSource;
		if (fTarget != null && fTarget.getCu().equals(unit))
			return fTarget;
		return new CompilationUnitRewrite(unit);
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
	
	private String[] getUpdatedMemberSource(RefactoringStatus status, BodyDeclaration[] members, ITypeBinding target) throws CoreException, BadLocationException {
		List typeRefs= new ArrayList();
		boolean targetNeedsSourceImport= false;
		boolean isSourceNotTarget= fSource != fTarget;
		Set exclude= new HashSet();
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
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
				for (final Iterator iterator= field.fragments().iterator(); iterator.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment) iterator.next();
					IVariableBinding binding= fragment.resolveBinding();
					if (binding != null)
						exclude.add(binding);
				}
			}
		}
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			if (isSourceNotTarget)
				typeRefs.addAll(TypeReferenceFinder.perform(declaration));
			MovedMemberAnalyzer analyzer= new MovedMemberAnalyzer(fSource, fMemberBindings, fSourceBinding, target);
			declaration.accept(analyzer);
			ImportRewriteUtil.addImports(fTarget, declaration, new HashMap(), new HashMap(), exclude, false);
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
				}
			}
			ITrackedNodePosition trackedPosition= fSource.getASTRewrite().track(declaration);
			declaration.setProperty(TRACKED_POSITION_PROPERTY, trackedPosition);
			targetNeedsSourceImport|= analyzer.targetNeedsSourceImport();
			status.merge(analyzer.getStatus());
		}
		// Adjust imports
		if (targetNeedsSourceImport && isSourceNotTarget) {
			fTarget.getImportRewrite().addImport(fSourceBinding);
		}
		if (isSourceNotTarget) {
			for (Iterator iter= typeRefs.iterator(); iter.hasNext();) {
				ITypeBinding binding= (ITypeBinding) iter.next();
				fTarget.getImportRewrite().addImport(binding);
			}
		}
		// extract updated members
		String[] updatedMemberSources= new String[members.length];
		IDocument document= new Document(fSource.getCu().getBuffer().getContents());
		TextEdit edit= fSource.getASTRewrite().rewriteAST(document, fSource.getCu().getJavaProject().getOptions(true));
		edit.apply(document, TextEdit.UPDATE_REGIONS);
		for (int i= 0; i < members.length; i++) {
			updatedMemberSources[i]= getUpdatedMember(document, members[i]);
		}
		fSource.clearASTRewrite();
		return updatedMemberSources;
	}

	private String getUpdatedMember(IDocument document, BodyDeclaration declaration) throws BadLocationException {
		ITrackedNodePosition trackedPosition= (ITrackedNodePosition) declaration.getProperty(TRACKED_POSITION_PROPERTY);
		return Strings.trimIndentation(document.get(trackedPosition.getStartPosition(), trackedPosition.getLength()), fPreferences.tabWidth, fPreferences.indentWidth, false);
	}

	private RefactoringStatus moveMembers(BodyDeclaration[] members, String[] sources) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		AbstractTypeDeclaration destination= (AbstractTypeDeclaration)
		ASTNodes.getParent(
			NodeFinder.perform(fTarget.getRoot(), fDestinationType.getNameRange()),
			AbstractTypeDeclaration.class);
		ListRewrite containerRewrite= fTarget.getASTRewrite().getListRewrite(destination, destination.getBodyDeclarationsProperty());
		
		TextEditGroup delete= fSource.createGroupDescription(RefactoringCoreMessages.MoveMembersRefactoring_deleteMembers); 
		TextEditGroup add= fTarget.createGroupDescription(RefactoringCoreMessages.MoveMembersRefactoring_addMembers); 
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			fSource.getASTRewrite().remove(declaration, delete);
			if (fSource != fTarget)
				fSource.getImportRemover().registerRemovedNode(declaration);
			ASTNode node= fTarget.getASTRewrite().createStringPlaceholder(sources[i], declaration.getNodeType());
			List container= containerRewrite.getRewrittenList();
			int insertionIndex= ASTNodes.getInsertionIndex((BodyDeclaration) node, container);
			containerRewrite.insertAt(node, insertionIndex, add);
		}
		return result;
	}
	
	private BodyDeclaration[] getASTMembers(RefactoringStatus status) throws JavaModelException {
		BodyDeclaration[] result= new BodyDeclaration[fMembersToMove.length];
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			ASTNode node= NodeFinder.perform(fSource.getRoot(), member.getNameRange());
			result[i]= (BodyDeclaration)ASTNodes.getParent(node, BodyDeclaration.class);

			//Fix for bug 42383: exclude multiple VariableDeclarationFragments ("int a=1, b=2")
			//ReferenceAnalyzer#visit(FieldDeclaration node) depends on fragments().size() != 1 !
			if (result[i] instanceof FieldDeclaration 
					&& ((FieldDeclaration) result[i]).fragments().size() != 1) {
				status.addFatalError(RefactoringCoreMessages.MoveMembersRefactoring_multi_var_fields); 
				return result;
			}
			
		}
	
		//Sorting members is important for field declarations referring to previous fields.
		Arrays.sort(result, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((BodyDeclaration) o1).getStartPosition()
						- ((BodyDeclaration) o2).getStartPosition();
			}
		});
		return result;
	}
}