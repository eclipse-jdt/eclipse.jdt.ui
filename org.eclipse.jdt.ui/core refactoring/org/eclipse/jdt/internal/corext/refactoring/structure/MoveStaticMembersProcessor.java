/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.ui.refactoring.IRefactoringProcessorIds;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

public class MoveStaticMembersProcessor extends MoveProcessor {
	
	private IMember[] fMembersToMove;
	private IType fDestinationType;
	private String fDestinationTypeName;
	
	private CodeGenerationSettings fPreferences;
	private CompositeChange fChange;
	private ASTData fSource;
	private ITypeBinding fSourceBinding; 
	private ASTData fTarget;
	private IBinding[] fMemberBindings;
	private BodyDeclaration[] fMemberDeclarations;

	public static class ASTData {
		public ASTData(ICompilationUnit u, boolean resolveBindings) {
			unit= u;
			root= new RefactoringASTParser(AST.JLS2).parse(unit, resolveBindings);
			rewriter= new OldASTRewrite(root);
		}
		
		public ASTData(ICompilationUnit u, boolean resolveBindings, CodeGenerationSettings settings) throws CoreException {
			this(u, resolveBindings);
			groups= new ArrayList();
			imports= new ImportRewrite(unit);
		}
		public ICompilationUnit unit;
		public CompilationUnit root;
		public OldASTRewrite rewriter;
		public List groups;
		public ImportRewrite imports;
		
		public TextEditGroup createGroupDescription(String name) {
			TextEditGroup result= new TextEditGroup(name);
			groups.add(result);
			return result;
		}
		public void reset(CodeGenerationSettings settings) throws CoreException {
			clearRewrite();
			imports= new ImportRewrite(unit);
		}
		public void clearRewrite() {
			rewriter.removeModifications();
			groups= new ArrayList();
		}
		public TextChange createChange() throws CoreException {
			CompilationUnitChange result= new CompilationUnitChange(unit.getElementName(), unit);
			TextBuffer buffer= TextBuffer.acquire(getFile(unit));
			try {
				MultiTextEdit edit= new MultiTextEdit();
				rewriter.rewriteNode(buffer, edit);
				if (!imports.isEmpty())
					edit.addChild(imports.createEdit(buffer.getDocument()));
				result.setEdit(edit);
				for (Iterator iter= groups.iterator(); iter.hasNext();) {
					result.addTextEditGroup((TextEditGroup)iter.next());
				}
			} finally {
				TextBuffer.release(buffer);
			}
			return result;
		}
		private static IFile getFile(ICompilationUnit cu) {
			return (IFile)WorkingCopyUtil.getOriginal(cu).getResource();
		}
	}
	
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
		//Initializers have no bindings -> would need special handling in MoveStaticMemberAnalyzer, etc.
		if (member.getElementType() != IJavaElement.METHOD && 
			member.getElementType() != IJavaElement.FIELD &&
			member.getElementType() != IJavaElement.TYPE)
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
			
			fSource= new ASTData(fMembersToMove[0].getCompilationUnit(), true);
			fSourceBinding= getSourceBinding();
			fMemberBindings= getMemberBindings();
			if (fSourceBinding == null || hasUnresolvedMemberBinding()) {
				result.addFatalError(RefactoringCoreMessages.getFormattedString(
					"MoveMembersRefactoring.compile_errors", //$NON-NLS-1$
					fSource.unit.getElementName()));
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
		try {
			pm.beginTask(RefactoringCoreMessages.getString("MoveMembersRefactoring.Checking_preconditions"), 10); //$NON-NLS-1$
			
			RefactoringStatus result= new RefactoringStatus();	
			
			fSource.reset(fPreferences);
			
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
			if (checker != null) {
				checker.addFiles(getAllFilesToModify(modifiedCus));
			}
			return result;
		} finally {
			pm.done();
		}	
	}
	
	private IFile[] getAllFilesToModify(List modifiedCus) {
		Set result= new HashSet();
		IResource resource= fDestinationType.getCompilationUnit().getResource();
		if (result != null)
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
				VariableDeclarationFragment declaration= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, fSource.root);
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

	//XXX - refactor the 3 checkAccessed*Availability() into 1
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

		IType[] blindAccessorTypes= getTypesNotSeeingMovedMember(memberToMove, new SubProgressMonitor(pm, 1));
		for (int k= 0; k < blindAccessorTypes.length; k++) {
			String message= createNonAccessibleMemberMessage(memberToMove, blindAccessorTypes[k],/*moved*/true);
			result.addError(message, JavaStatusContext.create(memberToMove));
		}
		pm.done();
		return result;
	}
	
	private IType[] getTypesNotSeeingMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
		if (JdtFlags.isPublic(member) && JdtFlags.isPublic(fDestinationType))
			return new IType[0];

		HashSet blindAccessorTypes= new HashSet(); // referencing, but access to destination type illegal
		SearchResultGroup[] references= getReferences(member, new SubProgressMonitor(pm, 1));
		for (int i = 0; i < references.length; i++) {
			SearchResult[] searchResults= references[i].getSearchResults();
			for (int k= 0; k < searchResults.length; k++) {
				SearchResult searchResult= searchResults[k];
				IJavaElement element= searchResult.getEnclosingElement();
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
	
	private static SearchResultGroup[] getReferences(IMember member, IProgressMonitor pm) throws JavaModelException {
		IJavaSearchScope scope= RefactoringScopeFactory.create(member);
		ISearchPattern pattern= SearchEngine.createSearchPattern(member, IJavaSearchConstants.REFERENCES);
		SearchResultGroup[] references= RefactoringSearchEngine.search(pm, scope, pattern);
		return references;
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

	private boolean isWithinMemberToMove(SearchResult result) throws JavaModelException {
		ICompilationUnit referenceCU= result.getCompilationUnit();
		if (! referenceCU.equals(fSource.unit))
			return false;
		int referenceStart= result.getStart();
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
		fTarget= getASTData(fDestinationType.getCompilationUnit());
		ITypeBinding targetBinding= getDestinationBinding();
		if (targetBinding == null) {
			status.addFatalError(RefactoringCoreMessages.getFormattedString(
				"MoveMembersRefactoring.compile_errors", //$NON-NLS-1$
				fTarget.unit.getElementName()));
			pm.done();
			return;
		}
		
		// First update references in moved members can extract the source.
		String[] memberSources= getUpdatedMemberSource(status, fMemberDeclarations, targetBinding);
		pm.worked(1);
		if (status.hasFatalError())
			return;
		ICompilationUnit[] affectedCUs= RefactoringSearchEngine.findAffectedCompilationUnits(
			new SubProgressMonitor(pm, 1), RefactoringScopeFactory.create(fMembersToMove),
			RefactoringSearchEngine.createSearchPattern(fMembersToMove, IJavaSearchConstants.REFERENCES));
		modifiedCus.addAll(Arrays.asList(affectedCUs));
		SubProgressMonitor sub= new SubProgressMonitor(pm, 1);
		sub.beginTask("", affectedCUs.length); //$NON-NLS-1$
		for (int i= 0; i < affectedCUs.length; i++) {
			ICompilationUnit unit= affectedCUs[i];
			ASTData ast= getASTData(unit);
			ReferenceAnalyzer analyzer= new ReferenceAnalyzer(
				ast, fMemberBindings, targetBinding, fSourceBinding);
			ast.root.accept(analyzer);
			status.merge(analyzer.getStatus());
			status.merge(Checks.validateEdit(unit));
			if (status.hasFatalError()) {
				fChange= null;
				return;
			}
			if (analyzer.needsTargetImport())
				ast.imports.addImport(targetBinding);
			if (!isSourceOrTarget(unit))
				fChange.add(ast.createChange());
			sub.worked(1);
		}
		status.merge(moveMembers(fMemberDeclarations, memberSources));
		fChange.add(fSource.createChange());
		status.merge(Checks.validateEdit(fSource.unit));
		if (! fSource.unit.equals(fTarget.unit)) {
			fChange.add(fTarget.createChange());
			status.merge(Checks.validateEdit(fTarget.unit));
		}
		pm.worked(1);
	}
	
	private ASTData getASTData(ICompilationUnit unit) throws CoreException {
		if (fSource.unit.equals(unit))
			return fSource;
		if (fTarget != null && fTarget.unit.equals(unit))
			return fTarget;
		return new ASTData(unit, true, fPreferences);
	}
	
	private boolean isSourceOrTarget(ICompilationUnit unit) {
		return fSource.unit.equals(unit) || fTarget.unit.equals(unit);
	}
	
	private ITypeBinding getDestinationBinding() throws JavaModelException {
		ASTNode node= NodeFinder.perform(fTarget.root, fDestinationType.getNameRange());
		return (ITypeBinding)((SimpleName)node).resolveBinding();
	}
	
	private ITypeBinding getSourceBinding() throws JavaModelException {
		ASTNode node= NodeFinder.perform(fSource.root, fMembersToMove[0].getDeclaringType().getNameRange());
		return (ITypeBinding)((SimpleName)node).resolveBinding();
	}
	
	private IBinding[] getMemberBindings() throws JavaModelException {
		IBinding[] result= new IBinding[fMembersToMove.length];
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			SimpleName name= (SimpleName)NodeFinder.perform(fSource.root, member.getNameRange());
			result[i]= name.resolveBinding();
		}
		return result;
	}
	
	private String[] getUpdatedMemberSource(RefactoringStatus status, BodyDeclaration[] members, ITypeBinding target) throws CoreException {
		List typeRefs= new ArrayList();
		boolean targetNeedsSourceImport= false;
		// update references in moved members
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			typeRefs.addAll(TypeReferenceFinder.perform(declaration));
			MovedMemberAnalyzer analyzer= new MovedMemberAnalyzer(fSource, fMemberBindings, fSourceBinding, target);
			declaration.accept(analyzer);
			if (getDeclaringType().isInterface() && ! fDestinationType.isInterface()) {
				if (declaration instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) declaration;
					int psfModifiers= Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
					if ((fieldDecl.getModifiers() & psfModifiers) != psfModifiers)
						fSource.rewriter.set(fieldDecl, FieldDeclaration.MODIFIERS_PROPERTY, new Integer(psfModifiers), null);
				} else if (declaration instanceof TypeDeclaration) {
					TypeDeclaration typeDecl= (TypeDeclaration) declaration;
					int psModifiers= Modifier.PUBLIC | Modifier.STATIC;
					if ((typeDecl.getModifiers() & psModifiers) != psModifiers) {
						Integer newModifiers= new Integer((typeDecl.getModifiers() | psModifiers));
						fSource.rewriter.set(typeDecl, FieldDeclaration.MODIFIERS_PROPERTY, newModifiers, null);
					}
				}
			}
			TextEditGroup group= new TextEditGroup(RefactoringCoreMessages.getString("MoveMembersRefactoring.move_declaration")); //$NON-NLS-1$
			fSource.rewriter.markAsTracked(declaration, group);
			declaration.setProperty("group", group); //$NON-NLS-1$
			targetNeedsSourceImport |= analyzer.targetNeedsSourceImport();
			status.merge(analyzer.getStatus()); 
		}
		// Adjust imports
		if (targetNeedsSourceImport && (fTarget != fSource))
			fTarget.imports.addImport(fSourceBinding);
		for (Iterator iter= typeRefs.iterator(); iter.hasNext();) {
			ITypeBinding binding= (ITypeBinding)iter.next();
			fTarget.imports.addImport(binding);
		}
		// extract updated members
		String[] updatedMemberSources= new String[members.length];
		TextBuffer buffer= TextBuffer.create(fSource.unit.getSource());
		TextBufferEditor editor= new TextBufferEditor(buffer);
		MultiTextEdit edit= new MultiTextEdit();
		fSource.rewriter.rewriteNode(buffer, edit);
		editor.add(edit);
		editor.performEdits(new NullProgressMonitor());
		for (int i= 0; i < members.length; i++) {
			updatedMemberSources[i]= getUpdatedMember(buffer, members[i]);
		}
		fSource.clearRewrite();
		return updatedMemberSources;		
	}
	
	private String getUpdatedMember(TextBuffer buffer, BodyDeclaration declaration) {
		TextEditGroup groupDescription= (TextEditGroup) declaration.getProperty("group"); //$NON-NLS-1$
		IRegion textRange= groupDescription.getRegion();
		String newSource= buffer.getContent(textRange.getOffset(), textRange.getLength());
		return Strings.trimIndentation(newSource, fPreferences.tabWidth, false);
	}

	private RefactoringStatus moveMembers(BodyDeclaration[] members, String[] sources) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		TypeDeclaration destination= getDestinationDeclaration();
		List container= destination.bodyDeclarations();
			
		TextEditGroup delete= fSource.createGroupDescription(RefactoringCoreMessages.getString("MoveMembersRefactoring.deleteMembers")); //$NON-NLS-1$
		TextEditGroup add= fTarget.createGroupDescription(RefactoringCoreMessages.getString("MoveMembersRefactoring.addMembers")); //$NON-NLS-1$
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			fSource.rewriter.remove(declaration, delete);
			ASTNode node= fTarget.rewriter.createStringPlaceholder(sources[i], declaration.getNodeType());
			fTarget.rewriter.markAsInserted(node, add);
			container.add(ASTNodes.getInsertionIndex((BodyDeclaration)node, container), node);
		}
		return result;
	}
	
	private BodyDeclaration[] getASTMembers(RefactoringStatus status) throws JavaModelException {
		BodyDeclaration[] result= new BodyDeclaration[fMembersToMove.length];
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			ASTNode node= NodeFinder.perform(fSource.root, member.getNameRange());
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
	
	private TypeDeclaration getDestinationDeclaration() throws JavaModelException {
		return (TypeDeclaration)
			ASTNodes.getParent(
				NodeFinder.perform(fTarget.root, fDestinationType.getNameRange()),
				TypeDeclaration.class);
	}
}
