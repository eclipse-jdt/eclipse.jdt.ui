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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.BadLocationException;

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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.ui.refactoring.IRefactoringProcessorIds;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;

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

public class MoveStaticMembersProcessor extends MoveProcessor {
	
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
		if (! isAvailable(elements))
			return null;
		return new MoveStaticMembersProcessor(elements, preferenceSettings);
	}
	
	public static boolean isAvailable(IMember[] elements) throws JavaModelException{
		if (elements == null)
			return false;

		if (elements.length == 0)
			return false;
		
		if (! areAllMoveable(elements))
			return false;		

		if (! haveCommonDeclaringType(elements))
			return false;
		
		return true;
	}
	
	private static boolean areAllMoveable(IMember[] elements) throws JavaModelException{
		for (int i = 0; i < elements.length; i++) {
			if (! isMoveable(elements[i]))
				return false;
		}
		return true;
	}
	
	private static boolean isMoveable(IMember member) throws JavaModelException{
		// Initializers have no bindings -> would need special handling in MoveStaticMemberAnalyzer, etc.
		if (member.getElementType() != IJavaElement.METHOD && 
			member.getElementType() != IJavaElement.FIELD &&
			member.getElementType() != IJavaElement.TYPE)
				return false;
		if (JdtFlags.isEnum(member))
			return false;
		
		if (member.getDeclaringType() == null)
			return false;
		
		if (! Checks.isAvailable(member))
			return false;
			
		if (member.getElementType() == IJavaElement.METHOD && member.getDeclaringType().isInterface())
			return false;
				
		if (member.getElementType() == IJavaElement.METHOD && ! JdtFlags.isStatic(member))
			return false;

		if (member.getElementType() == IJavaElement.METHOD && ((IMethod)member).isConstructor())
			return false;
			
		if (member.getElementType() == IJavaElement.TYPE && ! JdtFlags.isStatic(member))
			return false;
			
		if (! member.getDeclaringType().isInterface() && ! JdtFlags.isStatic(member))
			return false;
			
		return true;
	}
	
	private static boolean haveCommonDeclaringType(IMember[] members){
		IType declaringType= members[0].getDeclaringType(); //index safe - checked in areAllMoveable()
		for (int i= 0; i < members.length; i++) {
			if (! declaringType.equals(members[i].getDeclaringType()))
				return false;			
		}	
		return true;
	}
	
	//---- Move Processor -------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isApplicable() throws CoreException {
		return isAvailable(fMembersToMove);
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
		return RefactoringCoreMessages.getString("MoveMembersRefactoring.Move_Members"); //$NON-NLS-1$
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
	
	private IType resolveType(String fullyQualifiedTypeName) throws JavaModelException{
		return getDeclaringType().getJavaProject().findType(fullyQualifiedTypeName);
	}
	
	//---- Activation checking ------------------------------------
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkDeclaringType());
			pm.worked(1);
			if (result.hasFatalError())
				return result;			
			
			fSource= new CompilationUnitRewrite(fMembersToMove[0].getCompilationUnit());
			fSourceBinding= getSourceBinding();
			fMemberBindings= getMemberBindings();
			if (fSourceBinding == null || hasUnresolvedMemberBinding()) {
				result.addFatalError(RefactoringCoreMessages.getFormattedString(
					"MoveMembersRefactoring.compile_errors", //$NON-NLS-1$
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
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.Object"));	 //$NON-NLS-1$

		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.binary"));	 //$NON-NLS-1$

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.read_only"));	 //$NON-NLS-1$
		
		return null;
	}
	
	//---- Input checking ------------------------------------

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		initializeForFinalConditionChecking();
		try {
			pm.beginTask(RefactoringCoreMessages.getString("MoveMembersRefactoring.Checking_preconditions"), 10); //$NON-NLS-1$
			
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
	
	private void initializeForFinalConditionChecking() {
		// clears some internal state since final condition checking
		// can be executed more than once.
		fTarget= null;
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
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.not_found", fDestinationTypeName);//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		
		if (fDestinationType.equals(getDeclaringType())){
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.same", //$NON-NLS-1$
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);	
		}	
		
		if (! fDestinationType.exists()){
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.not_exist", //$NON-NLS-1$
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
			
		if (fDestinationType.isBinary()){
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.dest_binary", //$NON-NLS-1$
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);
		}	

		RefactoringStatus result= new RefactoringStatus();				

		if (fDestinationType.isInterface() && ! getDeclaringType().isInterface())
			result.merge(checkFieldsForInterface());
		if (result.hasFatalError())
			return result;

		// no checking required for moving interface fields to classes
				
		if (! canDeclareStaticMembers(fDestinationType)){
			String message= RefactoringCoreMessages.getString("MoveMembersRefactoring.static_declaration"); //$NON-NLS-1$
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
				String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.inside", //$NON-NLS-1$
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
				String message= RefactoringCoreMessages.getString("MoveMembersRefactoring.only_public_static"); //$NON-NLS-1$
				result.addError(message, JavaStatusContext.create(fMembersToMove[i]));
			}
		}
		return result;
	}

	private boolean canMoveToInterface(IMember member) throws JavaModelException {
		int flags= member.getFlags();
		switch (member.getElementType()) {
			case IJavaElement.FIELD :
				if (! (Flags.isPublic(flags) && Flags.isStatic(flags) && Flags.isFinal(flags)))
					return false;
				VariableDeclarationFragment declaration= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, fSource.getRoot());
				return declaration.getInitializer() != null;

			case IJavaElement.TYPE :
				return (Flags.isPublic(flags) && Flags.isStatic(flags));
				
			default :
				return false;
		}
	}

	private static boolean canDeclareStaticMembers(IType type) throws JavaModelException {
		return (JdtFlags.isStatic(type)) || (type.getDeclaringType() == null);
	}
	
	private RefactoringStatus checkAccessedMembersAvailability(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 3); //$NON-NLS-1$
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
		pm.setTaskName(RefactoringCoreMessages.getString("MoveMembersRefactoring.check_availability")); //$NON-NLS-1$
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
			pm.beginTask("", typeMembers.length + 1); //$NON-NLS-1$
			for (int i= 0; i < typeMembers.length; i++) {
				if (typeMembers[i] instanceof IInitializer)
					pm.worked(1);
				else
					result.merge(checkMovedMemberAvailability((IMember) typeMembers[i], new SubProgressMonitor(pm, 1)));
			}
		} else {
			pm.beginTask("", 1); //$NON-NLS-1$
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
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.moved_field", //$NON-NLS-1$ 
								new String[]{JavaElementUtil.createFieldSignature((IField)member), 
									JavaModelUtil.getFullyQualifiedName(accessingType),
									JavaModelUtil.getFullyQualifiedName(declaringType)});
				else
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.accessed_field", //$NON-NLS-1$
								new String[]{JavaElementUtil.createFieldSignature((IField)member), 
									JavaModelUtil.getFullyQualifiedName(accessingType)});
				return message;
			}			
			case IJavaElement.METHOD: {
				if (moved)
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.moved_method", //$NON-NLS-1$
								new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
									JavaModelUtil.getFullyQualifiedName(accessingType),
									JavaModelUtil.getFullyQualifiedName(declaringType)});
				else				 
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.accessed_method", //$NON-NLS-1$
								new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
									JavaModelUtil.getFullyQualifiedName(accessingType)});
								 
				return message;		
			}			
			case IJavaElement.TYPE:{
				if (moved)
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.moved_type", //$NON-NLS-1$
								new String[]{JavaModelUtil.getFullyQualifiedName(((IType)member)), 
									JavaModelUtil.getFullyQualifiedName(accessingType),
									JavaModelUtil.getFullyQualifiedName(declaringType)});
				else
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.accessed_type", //$NON-NLS-1$
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
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES));
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
			if (liesWithin(fMembersToMove[i].getSourceRange(), referenceStart))
				return true;
		}
		return false;
	}

	private static boolean liesWithin(ISourceRange range, int offset) {
		return range.getOffset() <= offset && range.getOffset() + range.getLength() >= offset;
	}

	private RefactoringStatus checkNativeMovedMethods(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", fMembersToMove.length); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (fMembersToMove[i].getElementType() != IJavaElement.METHOD)
				continue;
			if (! JdtFlags.isNative(fMembersToMove[i]))
				continue;
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.native", //$NON-NLS-1$
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
	
	private void createChange(List modifiedCus, RefactoringStatus status, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 4); //$NON-NLS-1$
		fChange= new DynamicValidationStateChange(RefactoringCoreMessages.getString("MoveMembersRefactoring.move_members")); //$NON-NLS-1$
		fTarget= getCuRewrite(fDestinationType.getCompilationUnit());
		ITypeBinding targetBinding= getDestinationBinding();
		if (targetBinding == null) {
			status.addFatalError(RefactoringCoreMessages.getFormattedString(
				"MoveMembersRefactoring.compile_errors", //$NON-NLS-1$
				fTarget.getCu().getElementName()));
			pm.done();
			return;
		}
		
		try {
			// First update references in moved members, in order to extract the source.
			String[] memberSources= getUpdatedMemberSource(status, fMemberDeclarations, targetBinding);
			pm.worked(1);
			if (status.hasFatalError())
				return;
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2();
			engine.setPattern(fMembersToMove, IJavaSearchConstants.REFERENCES);
			engine.setFiltering(true, true);
			engine.setScope(RefactoringScopeFactory.create(fMembersToMove));
			engine.setStatus(status);
			engine.searchPattern(new SubProgressMonitor(pm, 1));
			ICompilationUnit[] units= engine.getCompilationUnits();
			modifiedCus.addAll(Arrays.asList(units));
			SubProgressMonitor sub= new SubProgressMonitor(pm, 1);
			sub.beginTask("", units.length); //$NON-NLS-1$
			for (int i= 0; i < units.length; i++) {
				ICompilationUnit unit= units[i];
				CompilationUnitRewrite ast= getCuRewrite(unit);
				ReferenceAnalyzer analyzer= new ReferenceAnalyzer(
					ast, fMemberBindings, targetBinding, fSourceBinding);
				ast.getRoot().accept(analyzer);
				status.merge(analyzer.getStatus());
				if (status.hasFatalError()) {
					fChange= null;
					return;
				}
				if (analyzer.needsTargetImport())
					ast.getImportRewrite().addImport(targetBinding);
				if (!isSourceOrTarget(unit))
					fChange.add(ast.createChange());
				sub.worked(1);
			}
			status.merge(moveMembers(fMemberDeclarations, memberSources));
			fChange.add(fSource.createChange());
			modifiedCus.add(fSource.getCu());
			if (! fSource.getCu().equals(fTarget.getCu())) {
				fChange.add(fTarget.createChange());
				modifiedCus.add(fTarget.getCu());
			}
			pm.worked(1);
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
	
	private boolean isSourceOrTarget(ICompilationUnit unit) {
		return fSource.getCu().equals(unit) || fTarget.getCu().equals(unit);
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
	
	private ITypeBinding getSourceBinding() throws JavaModelException {
		ASTNode node= NodeFinder.perform(fSource.getRoot(), fMembersToMove[0].getDeclaringType().getNameRange());
		return (ITypeBinding)((SimpleName)node).resolveBinding();
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
		// update references in moved members
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			if (isSourceNotTarget)
				typeRefs.addAll(TypeReferenceFinder.perform(declaration));
			MovedMemberAnalyzer analyzer= new MovedMemberAnalyzer(fSource, fMemberBindings, fSourceBinding, target);
			declaration.accept(analyzer);
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
		try {
			ITextFileBuffer buffer= RefactoringFileBuffers.connect(fSource.getCu());
			TextEdit edit= fSource.getASTRewrite().rewriteAST(buffer.getDocument(), fSource.getCu().getJavaProject().getOptions(true));
			edit.apply(buffer.getDocument(), TextEdit.UPDATE_REGIONS);
			for (int i= 0; i < members.length; i++) {
				updatedMemberSources[i]= getUpdatedMember(buffer, members[i]);
			}
			fSource.clearASTRewrite();
			return updatedMemberSources;
		} finally {
			RefactoringFileBuffers.disconnect(fSource.getCu());
		}
	}

	private String getUpdatedMember(ITextFileBuffer buffer, BodyDeclaration declaration) throws BadLocationException {
		ITrackedNodePosition trackedPosition= (ITrackedNodePosition) declaration.getProperty(TRACKED_POSITION_PROPERTY);
		return Strings.trimIndentation(buffer.getDocument().get(trackedPosition.getStartPosition(), trackedPosition.getLength()), fPreferences.tabWidth, false);
	}

	private RefactoringStatus moveMembers(BodyDeclaration[] members, String[] sources) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		AbstractTypeDeclaration destination= getDestinationDeclaration();
		ListRewrite containerRewrite= fTarget.getASTRewrite().getListRewrite(destination, destination.getBodyDeclarationsProperty());
		
		TextEditGroup delete= fSource.createGroupDescription(RefactoringCoreMessages.getString("MoveMembersRefactoring.deleteMembers")); //$NON-NLS-1$
		TextEditGroup add= fTarget.createGroupDescription(RefactoringCoreMessages.getString("MoveMembersRefactoring.addMembers")); //$NON-NLS-1$
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
				status.addFatalError(RefactoringCoreMessages.getString("MoveMembersRefactoring.multi_var_fields")); //$NON-NLS-1$
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
	
	private AbstractTypeDeclaration getDestinationDeclaration() throws JavaModelException {
		return (AbstractTypeDeclaration)
			ASTNodes.getParent(
				NodeFinder.perform(fTarget.getRoot(), fDestinationType.getNameRange()),
				AbstractTypeDeclaration.class);
	}
}
