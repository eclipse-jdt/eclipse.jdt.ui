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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class MoveStaticMembersRefactoring extends Refactoring {
	
	private IMember[] fMembers;
	private IType fDestinationType;
	private String fDestinationTypeName;
	
	private CodeGenerationSettings fPreferences;
	private CompositeChange fChange;
	private ASTData fSource;
	private ITypeBinding fSourceBinding; 
	private ASTData fTarget;
	private IBinding[] fMemberBindings;

	public static class ASTData {
		public ASTData(ICompilationUnit u, boolean resolveBindings, CodeGenerationSettings settings) throws JavaModelException {
			unit= u;
			root= AST.parseCompilationUnit(unit, resolveBindings);
			rewriter= new ASTRewrite(root);
			groups= new ArrayList();
			imports= new ImportEdit(unit, settings);
		}
		public ICompilationUnit unit;
		public CompilationUnit root;
		public ASTRewrite rewriter;
		public List groups;
		public ImportEdit imports;
		
		public GroupDescription createGroupDescription(String name) {
			GroupDescription result= new GroupDescription(name);
			groups.add(result);
			return result;
		}
		public void removeModifications() {
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
					edit.add(imports);
				result.setEdit(edit);
				result.addGroupDescriptions((GroupDescription[])groups.toArray(new GroupDescription[groups.size()]));
			} finally {
				TextBuffer.release(buffer);
			}
			return result;
		}
		private static IFile getFile(ICompilationUnit cu) throws CoreException {
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

	private MoveStaticMembersRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings) {
		Assert.isNotNull(elements);
		Assert.isNotNull(preferenceSettings);
		fMembers= elements;
		fPreferences= preferenceSettings;
	}
	
	public static MoveStaticMembersRefactoring create(IMember[] elements, CodeGenerationSettings preferenceSettings) throws JavaModelException{
		if (! isAvailable(elements))
			return null;
		return new MoveStaticMembersRefactoring(elements, preferenceSettings);
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
		if (member.getElementType() != IJavaElement.METHOD && 
			member.getElementType() != IJavaElement.FIELD)
				return false;

		if (! Checks.isAvailable(member))
			return false;
			
		if (member.getElementType() == IJavaElement.METHOD && member.getDeclaringType().isInterface())
			return false;
				
		if (member.getElementType() == IJavaElement.METHOD && ! JdtFlags.isStatic(member))
			return false;

		if (! member.getDeclaringType().isInterface() && ! JdtFlags.isStatic(member))
			return false;
			
		if (member.getElementType() == IJavaElement.METHOD && ((IMethod)member).isConstructor())
			return false;
			
		return true;
	}
	
	private static boolean haveCommonDeclaringType(IMember[] members){
		IType declaringType= members[0].getDeclaringType(); //index safe - checked in constructor
		for (int i= 0; i < members.length; i++) {
			if (! declaringType.equals(members[i].getDeclaringType()))
				return false;			
		}	
		return true;
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("MoveMembersRefactoring.Move_Members"); //$NON-NLS-1$
	}

	public IType getDestinationType() {
		return fDestinationType;
	}

	public void setDestinationTypeFullyQualifiedName(String fullyQualifiedTypeName) throws JavaModelException {
		Assert.isNotNull(fullyQualifiedTypeName);
		fDestinationType= resolveType(fullyQualifiedTypeName);
		fDestinationTypeName= fullyQualifiedTypeName;
	}
	
	public IMember[] getMovedMembers() {
		return fMembers;
	}
	
	public IType getDeclaringType() {
		//all methods declared in same type - checked in precondition
		return  fMembers[0].getDeclaringType(); //index safe - checked in constructor
	}
	
	private IType resolveType(String fullyQualifiedTypeName) throws JavaModelException{
		return getDeclaringType().getJavaProject().findType(fullyQualifiedTypeName);
	}
	
	//---- Activation checking ------------------------------------

	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkDeclaringType());
			pm.worked(1);
			if (result.hasFatalError())
				return result;			
			
			fSource= new ASTData(fMembers[0].getCompilationUnit(), true, fPreferences);
			fSourceBinding= getSourceBinding();
			fMemberBindings= getMemberBindings();
			if (fSourceBinding == null || hasUnresolvedMemberBinding()) {
				result.addFatalError(RefactoringCoreMessages.getFormattedString(
					"MoveMembersRefactoring.compile_errors", //$NON-NLS-1$
					fSource.unit.getElementName()));
			}
			return result;
		} finally{
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
	
	private RefactoringStatus checkDeclaringType() throws JavaModelException{
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

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("MoveMembersRefactoring.Checking_preconditions"), 10); //$NON-NLS-1$
			
			RefactoringStatus result= new RefactoringStatus();	
			
			result.merge(checkDestinationType());			
			if (result.hasFatalError())
				return result;
						
			result.merge(MemberCheckUtil.checkMembersInDestinationType(fMembers, fDestinationType));	
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
			
			createChange(result, new SubProgressMonitor(pm, 7));
			return result;
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} finally{
			pm.done();
		}	
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

		if (fDestinationType.isInterface() && ! getDeclaringType().isInterface()){
			String message= RefactoringCoreMessages.getString("MoveMembersRefactoring.interface_fields"); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	

		if (! fDestinationType.isInterface() && getDeclaringType().isInterface()){
			String message= RefactoringCoreMessages.getString("MoveMembersRefactoring.interface_members"); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	

		RefactoringStatus result= new RefactoringStatus();				
		
		if (! canDeclareStaticMembers(fDestinationType)){
			String message= RefactoringCoreMessages.getString("MoveMembersRefactoring.static_declaration"); //$NON-NLS-1$
			result.addError(message);
		}	
				
		return result;	
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
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fMembers, pm);
		List movedElementList= Arrays.asList(fMembers);
		for (int i= 0; i < accessedMethods.length; i++) {
			if (movedElementList.contains(accessedMethods[i]))
				continue;
			if (! JdtFlags.isStatic(accessedMethods[i])) //safely ignore non-static 
				continue;
			if (! isVisibleFrom(accessedMethods[i], fDestinationType, accessedMethods[i].getDeclaringType())){
				String msg= createNonAccessibleMemberMessage(accessedMethods[i], fDestinationType, false);
				result.addWarning(msg, JavaStatusContext.create(accessedMethods[i]));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedTypesAvailability(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= ReferenceFinderUtil.getTypesReferencedIn(fMembers, pm);
		List movedElementList= Arrays.asList(fMembers);
		for (int i= 0; i < accessedTypes.length; i++) {
			if (movedElementList.contains(accessedTypes[i]))
				continue;
			if (! JdtFlags.isStatic(accessedTypes[i])) //safely ignore non-static 
				continue;
			if (! isVisibleFrom(accessedTypes[i], fDestinationType, accessedTypes[i].getDeclaringType())){
				String msg= createNonAccessibleMemberMessage(accessedTypes[i], fDestinationType, false);
				result.addWarning(msg, JavaStatusContext.create(accessedTypes[i]));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedFieldsAvailability(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fMembers, pm);
		List movedElementList= Arrays.asList(fMembers);
		for (int i= 0; i < accessedFields.length; i++) {
			if (movedElementList.contains(accessedFields[i]))
				continue;
			if (! JdtFlags.isStatic(accessedFields[i])) //safely ignore non-static 
				continue;
			if (! isVisibleFrom(accessedFields[i], fDestinationType, accessedFields[i].getDeclaringType())){
				String msg= createNonAccessibleMemberMessage(accessedFields[i], fDestinationType, false);
				result.addWarning(msg, JavaStatusContext.create(accessedFields[i]));
			}	
		}
		return result;
	}
	
	private static boolean isVisibleFrom(IMember member, IType accessingType, IType newMemberDeclaringType) throws JavaModelException{
		if (JdtFlags.isPrivate(member))
			return newMemberDeclaringType.equals(accessingType); //roughly
		if (JdtFlags.isPublic(member)){
			if (JdtFlags.isPublic(newMemberDeclaringType)) //roughly
				return true;
			return accessingType.getPackageFragment().equals(newMemberDeclaringType.getPackageFragment());  //roughly
		}	
		if (JdtFlags.isProtected(member)){ //FIX ME
			if (JdtFlags.isPublic(newMemberDeclaringType))
				return true;
			return accessingType.getPackageFragment().equals(newMemberDeclaringType.getPackageFragment());
		}	
		else	
			//default visibility
			return accessingType.getPackageFragment().equals(newMemberDeclaringType.getPackageFragment());  //roughly
	}
	
	private static String createNonAccessibleMemberMessage(IMember member, IType type, boolean moved) throws JavaModelException{
		switch (member.getElementType()){
			case IJavaElement.FIELD: {
				String message;
				if (moved)
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.moved_field", //$NON-NLS-1$ 
								new String[]{JavaElementUtil.createFieldSignature((IField)member), 
									 createAccessModifierString(member),
									JavaModelUtil.getFullyQualifiedName(type)});
				else
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.accessed_field", //$NON-NLS-1$
								new String[]{JavaElementUtil.createFieldSignature((IField)member), 
									 createAccessModifierString(member),
									JavaModelUtil.getFullyQualifiedName(type)});
				return message;
			}			
			case IJavaElement.METHOD: {
				String message;
				if (moved)
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.moved_method", //$NON-NLS-1$
									new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
									createAccessModifierString(member),
									JavaModelUtil.getFullyQualifiedName(type)});
				else				 
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.accessed_method", //$NON-NLS-1$
									new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
									createAccessModifierString(member),
									JavaModelUtil.getFullyQualifiedName(type)});
								 
				return message;		
			}			
			case IJavaElement.TYPE:{
				String message;
				if (moved)
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.moved_type", //$NON-NLS-1$
						new String[]{JavaModelUtil.getFullyQualifiedName(((IType)member)), 
									createAccessModifierString(member),
									JavaModelUtil.getFullyQualifiedName(type)});
				else
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.accessed_type", //$NON-NLS-1$
						new String[]{JavaModelUtil.getFullyQualifiedName(((IType)member)), 
									createAccessModifierString(member),
									JavaModelUtil.getFullyQualifiedName(type)});
				return message;
			}			
			default:
				Assert.isTrue(false);
				return null;
		}
	}
	
	private static String createAccessModifierString(IMember member) throws JavaModelException{
		if (JdtFlags.isPublic(member))
			return RefactoringCoreMessages.getString("MoveMembersRefactoring.public"); //$NON-NLS-1$
		else if (JdtFlags.isProtected(member))
			return RefactoringCoreMessages.getString("MoveMembersRefactoring.protected"); //$NON-NLS-1$
		else if (JdtFlags.isPrivate(member))
			return RefactoringCoreMessages.getString("MoveMembersRefactoring.private"); //$NON-NLS-1$
		else	
			return RefactoringCoreMessages.getString("MoveMembersRefactoring.package-visible"); //$NON-NLS-1$
	}
	
	private RefactoringStatus checkMovedMembersAvailability(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", fMembers.length); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembers.length; i++) {
			//XXX issues too many warnings - should check references to moved members
			if (! isVisibleFrom(fMembers[i], fMembers[i].getDeclaringType(), fDestinationType)){
				String message= createNonAccessibleMemberMessage(fMembers[i], fMembers[i].getDeclaringType(), true);
				result.addWarning(message, JavaStatusContext.create(fMembers[i]));
			}	
			pm.worked(1);
		}
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkNativeMovedMethods(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", fMembers.length); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembers.length; i++) {
			if (fMembers[i].getElementType() != IJavaElement.METHOD)
				continue;
			if (! JdtFlags.isNative(fMembers[i]))
				continue;
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.native", //$NON-NLS-1$
				JavaElementUtil.createMethodSignature((IMethod)fMembers[i]));
			result.addWarning(message, JavaStatusContext.create(fMembers[i]));
			pm.worked(1);
		}
		pm.done();
		return result;		
	}
	
	//---- change creation ---------------------------------------
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.done();
		return fChange;
	}
	
	private void createChange(RefactoringStatus status, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 4); //$NON-NLS-1$
		fChange= new CompositeChange(RefactoringCoreMessages.getString("MoveMembersRefactoring.move_members")); //$NON-NLS-1$
		fTarget= new ASTData(fDestinationType.getCompilationUnit(), true, fPreferences);
		ITypeBinding targetBinding= getDestinationBinding();
		if (targetBinding == null) {
			status.addFatalError(RefactoringCoreMessages.getFormattedString(
				"MoveMembersRefactoring.compile_errors", //$NON-NLS-1$
				fTarget.unit.getElementName()));
			pm.done();
			return;
		}
		BodyDeclaration[] members= getASTMembers();
		// First update references in moved members can extract the source.
		String[] memberSources= getUpdatedMemberSource(status, members, targetBinding);
		pm.worked(1);
		if (status.hasFatalError())
			return;
		ICompilationUnit[] affectedCUs= RefactoringSearchEngine.findAffectedCompilationUnits(
			new SubProgressMonitor(pm, 1), RefactoringScopeFactory.create(fMembers),
			RefactoringSearchEngine.createSearchPattern(fMembers, IJavaSearchConstants.REFERENCES));
		SubProgressMonitor sub= new SubProgressMonitor(pm, 1);
		sub.beginTask("", affectedCUs.length); //$NON-NLS-1$
		for (int i= 0; i < affectedCUs.length; i++) {
			ICompilationUnit unit= affectedCUs[i];
			ASTData ast= getASTData(unit);
			ReferenceAnalyzer analyzer= new ReferenceAnalyzer(
				fSourceBinding, targetBinding, fMemberBindings, ast);
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
		status.merge(moveMembers(members, memberSources));
		fChange.add(fSource.createChange());
		status.merge(Checks.validateEdit(fSource.unit));
		fChange.add(fTarget.createChange());
		status.merge(Checks.validateEdit(fTarget.unit));
		pm.worked(1);
	}
	
	private ASTData getASTData(ICompilationUnit unit) throws JavaModelException {
		if (fSource.unit.equals(unit))
			return fSource;
		if (fTarget.unit.equals(unit))
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
		ASTNode node= NodeFinder.perform(fSource.root, fMembers[0].getDeclaringType().getNameRange());
		return (ITypeBinding)((SimpleName)node).resolveBinding();
	}
	
	private IBinding[] getMemberBindings() throws JavaModelException {
		IBinding[] result= new IBinding[fMembers.length];
		for (int i= 0; i < fMembers.length; i++) {
			IMember member= fMembers[i];
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
			MovedMemberAnalyzer analyzer= new MovedMemberAnalyzer(fSource, fSourceBinding, fMemberBindings, target);
			declaration.accept(analyzer);
			targetNeedsSourceImport &= analyzer.targetNeedsSourceImport();
			status.merge(analyzer.getStatus()); 
		}
		// Adjust imports
		if (targetNeedsSourceImport)
			fTarget.imports.addImport(fSourceBinding);
		for (Iterator iter= typeRefs.iterator(); iter.hasNext();) {
			ITypeBinding binding= (ITypeBinding)iter.next();
			fTarget.imports.addImport(binding);
		}
		
		String[] result= new String[fMembers.length];
		TextBuffer buffer= TextBuffer.create(fSource.unit.getSource());
		TextBufferEditor editor= new TextBufferEditor(buffer);
		MultiTextEdit edit= new MultiTextEdit();
		fSource.rewriter.rewriteNode(buffer, edit);
		editor.add(edit);
		editor.performEdits(new NullProgressMonitor());
		ICompilationUnit wc= (ICompilationUnit)JavaModelUtil.toOriginal(fSource.unit).getWorkingCopy();
		try {
			wc.getBuffer().setContents(buffer.getContent());
			wc.reconcile();
			for (int i= 0; i < fMembers.length; i++) {
				IMember member= JavaModelUtil.findMemberInCompilationUnit(wc, fMembers[i]);
				result[i]= Strings.trimIndentation(member.getSource(), fPreferences.tabWidth, false);
			}
			
		} finally {
			wc.destroy();
		}
		fSource.removeModifications();
		return result;		
	}
	
	private RefactoringStatus moveMembers(BodyDeclaration[] members, String[] sources) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		TypeDeclaration destination= getDestinationDeclaration();
		List container= destination.bodyDeclarations();
			
		GroupDescription delete= fSource.createGroupDescription(RefactoringCoreMessages.getString("MoveMembersRefactoring.deleteMembers")); //$NON-NLS-1$
		GroupDescription add= fTarget.createGroupDescription(RefactoringCoreMessages.getString("MoveMembersRefactoring.addMembers")); //$NON-NLS-1$
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			fSource.rewriter.markAsRemoved(declaration, delete);
			ASTNode node= fTarget.rewriter.createPlaceholder(
				sources[i],
				ASTRewrite.getPlaceholderType(declaration));
			fTarget.rewriter.markAsInserted(node, add);
			container.add(ASTNodes.getInsertionIndex((BodyDeclaration)node, container), node);
		}
		return result;
	}
	
	private BodyDeclaration[] getASTMembers() throws JavaModelException {
		BodyDeclaration[] result= new BodyDeclaration[fMembers.length];
		for (int i= 0; i < fMembers.length; i++) {
			IMember member= fMembers[i];
			ASTNode node= NodeFinder.perform(fSource.root, member.getNameRange());
			result[i]= (BodyDeclaration)ASTNodes.getParent(node, BodyDeclaration.class);
		}
		return result;
	}
	
	private TypeDeclaration getDestinationDeclaration() throws JavaModelException {
		return (TypeDeclaration)NodeFinder.perform(fTarget.root, fDestinationType.getSourceRange());
	}	
}
