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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class MoveStaticMembersRefactoring extends Refactoring {
	
	private IMember[] fMembers;
	private IType fDestinationType;
	private String fDestinationTypeName;
	private TextChangeManager fChangeManager;
	private final ImportEditManager fImportManager;

	private MoveStaticMembersRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings){
		Assert.isNotNull(elements);
		Assert.isNotNull(preferenceSettings);
		fMembers= (IMember[])SourceReferenceUtil.sortByOffset(elements);
		fImportManager= new ImportEditManager(preferenceSettings);
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
	
	public IMember[] getMovedMembers(){
		return fMembers;
	}
	
	private IType resolveType(String fullyQualifiedTypeName) throws JavaModelException{
		return getDeclaringType().getJavaProject().findType(fullyQualifiedTypeName);
	}

	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkDeclaringType());
			pm.worked(1);
			if (result.hasFatalError())
				return result;			
			
			fMembers= getOriginals(fMembers);
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("MoveMembersRefactoring.Checking_preconditions"), 5); //$NON-NLS-1$
			
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
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
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
	
	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
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
	
	private static boolean canDeclareStaticMembers(IType type) throws JavaModelException {
		return (JdtFlags.isStatic(type)) || (type.getDeclaringType() == null);
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
	
	public IType getDeclaringType(){
		//all methods declared in same type - checked in precondition
		return  fMembers[0].getDeclaringType(); //index safe - checked in constructor
	}
	
	private static boolean haveCommonDeclaringType(IMember[] members){
		IType declaringType= members[0].getDeclaringType(); //index safe - checked in constructor
		for (int i= 0; i < members.length; i++) {
			if (! declaringType.equals(members[i].getDeclaringType()))
				return false;			
		}	
		return true;
	}
	
	private static IMember[] getOriginals(IMember[] members){
		IMember[] result= new IMember[members.length];
		for (int i= 0; i < members.length; i++) {
			result[i]= (IMember)WorkingCopyUtil.getOriginal(members[i]);
		}
		return result;
	}
	
	//------------------------
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			return new CompositeChange(RefactoringCoreMessages.getString("MoveMembersRefactoring.move_members"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}	
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("MoveMembersRefactoring.analyzing"), 6); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			addCopyMembersChange(new SubProgressMonitor(pm, 1), manager);
			
			if (destinationCUNeedsAddedImports())
				addImportsToDestinationCu(new SubProgressMonitor(pm, 1));
				
			if (sourceCUNeedsAddedImports())
				addImportsToSourceCu();
			pm.worked(1);	
			
			addDeleteMembersChange(new SubProgressMonitor(pm, 1), manager);

			addModifyReferencesToMovedMembers(new SubProgressMonitor(pm, 1), manager);

			fImportManager.fill(manager);
			
			return manager;
		} finally{
			pm.done();
		}	
	}
	
	private boolean destinationCUNeedsAddedImports() {
		return ! getDeclaringType().getCompilationUnit().equals(fDestinationType.getCompilationUnit());
	}
	
	private boolean sourceCUNeedsAddedImports() {
		return ! getDeclaringType().getCompilationUnit().equals(fDestinationType.getCompilationUnit());
	}

	private void addCopyMembersChange(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", fMembers.length);	 //$NON-NLS-1$
		for (int i = fMembers.length - 1; i >= 0; i--) { //backwards - to preserve order
			addCopyMemberChange(manager, fMembers[i], new SubProgressMonitor(pm, 1));
		}
		pm.done();
	}
	
	private void addCopyMemberChange(TextChangeManager manager, IMember member, IProgressMonitor pm) throws CoreException {
		String source= MemberMoveUtil.computeNewSource(member, fDestinationType, pm, fImportManager, fMembers);
		String changeName= RefactoringCoreMessages.getString("MoveMembersRefactoring.Copy") + member.getElementName();								 //$NON-NLS-1$
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fDestinationType.getCompilationUnit());
		manager.get(cu).addTextEdit(changeName, createAddMemberEdit(source, member.getElementType()));
	}
	
	
	private TextEdit createAddMemberEdit(String source, int memberType) throws JavaModelException {
		IMember sibling= getLastMember(fDestinationType, memberType);
		String[] sourceLines= Strings.removeTrailingEmptyLines(Strings.convertIntoLines(source));
		if (sibling != null)
			return new MemberEdit(sibling, MemberEdit.INSERT_AFTER, sourceLines, CodeFormatterUtil.getTabWidth());
		return new MemberEdit(fDestinationType, MemberEdit.ADD_AT_END, sourceLines, CodeFormatterUtil.getTabWidth());
	}
	
	private void addDeleteMembersChange(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", fMembers.length); //$NON-NLS-1$
		for (int i = 0; i < fMembers.length; i++) {
			String changeName= RefactoringCoreMessages.getString("MoveMembersRefactoring.delete") + fMembers[i].getElementName(); //$NON-NLS-1$
			DeleteSourceReferenceEdit edit= new DeleteSourceReferenceEdit(fMembers[i], fMembers[i].getCompilationUnit());
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fMembers[i].getCompilationUnit());
			manager.get(cu).addTextEdit(changeName, edit);
			pm.worked(1);
		}
		pm.done();
	}
		
	private void addImportsToDestinationCu(IProgressMonitor pm) throws CoreException {
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fDestinationType.getCompilationUnit());		
		IType[] referencedTypes= ReferenceFinderUtil.getTypesReferencedIn(fMembers, pm);
		for (int i= 0; i < referencedTypes.length; i++) {
			fImportManager.addImportTo(referencedTypes[i], cu);
		}
	}
	
	private void addImportsToSourceCu() throws CoreException {
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getDeclaringType().getCompilationUnit());		
		fImportManager.addImportTo(fDestinationType, cu);
	}
	
	private void addModifyReferencesToMovedMembers(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", fMembers.length); //$NON-NLS-1$
		for (int i= 0; i < fMembers.length; i++) {
			addModifyReferencesToMovedMember(fMembers[i], new SubProgressMonitor(pm, 1), manager);	
		}
		pm.done();
	}
	
	private void addModifyReferencesToMovedMember(IMember member, IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		
		SearchResultGroup[] results= findReferencesToMember(member, new SubProgressMonitor(pm, 1));
		
		for (int i= 0; i < results.length; i++) {
			SearchResultGroup searchResultGroup= results[i];
			ICompilationUnit cu= searchResultGroup.getCompilationUnit();
			if (cu == null)
				continue;
			ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
			SearchResultGroup modifiedGroup= searchResultGroup;
	
			if (ResourceUtil.getResource(getDeclaringType()).equals(ResourceUtil.getResource(wc)))
				modifiedGroup= removeReferencesEnclosedIn(fMembers, searchResultGroup);
	
			modifyReferencesToMovedMember(member, manager, modifiedGroup, wc);
			fImportManager.addImportTo(fDestinationType, wc);
		}
		pm.done();
	}
	
	private static SearchResultGroup removeReferencesEnclosedIn(IJavaElement[] elements, SearchResultGroup group){
		List elementList= Arrays.asList(elements);
		
		List searchResultList= new ArrayList(group.getSearchResults().length);
		SearchResult[] searchResults= group.getSearchResults();
		for (int i= 0; i < searchResults.length; i++) {
			if (! elementList.contains(searchResults[i].getEnclosingElement()))
				searchResultList.add(searchResults[i]);
		}
		SearchResult[] searchResultArray= (SearchResult[]) searchResultList.toArray(new SearchResult[searchResultList.size()]);
		return new SearchResultGroup(group.getResource(), searchResultArray);
	}

	private void modifyReferencesToMovedMember(IMember member, TextChangeManager manager, SearchResultGroup searchResultGroup, ICompilationUnit cu) throws JavaModelException, CoreException {
		ISourceRange[] ranges= findMemberReferences(member.getElementType(), searchResultGroup);
		String text= fDestinationType.getElementName() + "." + member.getElementName(); //$NON-NLS-1$
		for (int i= 0; i < ranges.length; i++) {
			ISourceRange iSourceRange= ranges[i];
			TextEdit edit= SimpleTextEdit.createReplace(iSourceRange.getOffset(), iSourceRange.getLength(), text);
			manager.get(cu).addTextEdit(RefactoringCoreMessages.getString("MoveMembersRefactoring.convert"), edit); //$NON-NLS-1$
		}	
	}

	private static ISourceRange[] findMemberReferences(int memberType, SearchResultGroup searchResultGroup) throws JavaModelException {
		Assert.isTrue(memberType == IJavaElement.METHOD || memberType == IJavaElement.FIELD);
		if (memberType == IJavaElement.METHOD)
			return MethodInvocationFinder.findMessageSendRanges(searchResultGroup);
		else
			return FieldReferenceFinder.findFieldReferenceRanges(searchResultGroup);
	}

	private static SearchResultGroup[] findReferencesToMember(IMember member, IProgressMonitor pm) throws JavaModelException {
		ISearchPattern pattern= SearchEngine.createSearchPattern(member, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= RefactoringScopeFactory.create(member);
		return RefactoringSearchEngine.search(pm, scope, pattern);
	}
			
	//--- helpers

	private static IMember getLastMember(IType type, int elementType) throws JavaModelException {
		if (elementType == IJavaElement.METHOD)
			return getLastMethod(type);
		if (elementType == IJavaElement.FIELD)
			return getLastField(type);
		Assert.isTrue(false);
		return null;	
	}
	
	private static IMethod getLastMethod(IType type) throws JavaModelException {
		if (type == null)
			return null;
		IMethod[] methods= type.getMethods();
		if (methods.length == 0)
			return null;
		return methods[methods.length - 1];	
	}
	
	private static IField getLastField(IType type) throws JavaModelException {
		if (type == null)
			return null;
		IField[] fields= type.getFields();
		if (fields.length == 0)
			return null;
		return fields[fields.length - 1];	
	}
	
	private static class MemberMoveUtil {

		private final ImportEditManager fImportManager;
		private final Set fMovedMembers;
		private final ICompilationUnit fDestinationCu;
	
		private MemberMoveUtil(ImportEditManager importManager, IMember[] movedMembers, IType destinationType) {
			Assert.isNotNull(importManager);
			fImportManager= importManager;
			fDestinationCu= WorkingCopyUtil.getWorkingCopyIfExists(destinationType.getCompilationUnit());	
			fMovedMembers= new HashSet(Arrays.asList(movedMembers));
		}
	
		public static String computeNewSource(IMember member, IType destinationType, IProgressMonitor pm, ImportEditManager manager, IMember[] allMovedMembers) throws JavaModelException {
			return new MemberMoveUtil(manager, allMovedMembers, destinationType).computeNewSource(member, pm);
		}
	
		private String computeNewSource(IMember member, IProgressMonitor pm) throws JavaModelException {
			String originalSource= SourceRangeComputer.computeSource(member);
			StringBuffer modifiedSource= new StringBuffer(originalSource);
		
			//ISourceRange -> String (new source)
			Map accessModifications= getStaticMemberAccessesInMovedMember(member, pm);
			ISourceRange[] ranges= (ISourceRange[]) accessModifications.keySet().toArray(new ISourceRange[accessModifications.keySet().size()]);
			ISourceRange[] sortedRanges= SourceRange.reverseSortByOffset(ranges);
		
			ISourceRange originalRange= SourceRangeComputer.computeSourceRange(member, member.getCompilationUnit().getSource());
		
			for (int i= 0; i < sortedRanges.length; i++) {
				int start= sortedRanges[i].getOffset() - originalRange.getOffset();
				int end= start + sortedRanges[i].getLength();
				modifiedSource.replace(start, end, (String)accessModifications.get(sortedRanges[i]));
			}
			return modifiedSource.toString();
		}
	
		//ISourceRange -> String (new source)
		private Map getStaticMemberAccessesInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException{
			pm.beginTask("", 3); //$NON-NLS-1$
			Map resultMap= new HashMap();
			resultMap.putAll(getFieldAccessModificationsInMovedMember(member, new SubProgressMonitor(pm, 1)));
			resultMap.putAll(getMethodSendsInMovedMember(member, new SubProgressMonitor(pm, 1)));
			resultMap.putAll(getTypeReferencesInMovedMember(member, new SubProgressMonitor(pm, 1)));
			pm.done();
			return resultMap;
		}

		//ISourceRange -> String (new source)
		private Map getFieldAccessModificationsInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 2); //$NON-NLS-1$
			Map result= new HashMap();
			IField[] fields= ReferenceFinderUtil.getFieldsReferencedIn(new IMember[]{member}, new SubProgressMonitor(pm, 1));
			ICompilationUnit cu= getWorkingCopyForDeclaringTypeCu(member);
			IMember[] interestingFields= getMembersThatNeedReferenceConversion(fields);
			for (int i= 0; i < interestingFields.length; i++) {
				IField field= (IField)interestingFields[i];
				//XX side effect
				fImportManager.addImportTo(field.getDeclaringType(), fDestinationCu);
				String newSource= field.getDeclaringType().getElementName() + "." + field.getElementName(); //$NON-NLS-1$
				SearchResult[] searchResults= findReferencesInMember(member, field, new SubProgressMonitor(pm, 1));
				ISourceRange[] ranges= FieldReferenceFinder.findFieldReferenceRanges(searchResults, cu);
				putAllToMap(result, newSource, ranges);		
			}
			return result;
		}

		//ISourceRange -> String (new source)
		private Map getMethodSendsInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 2); //$NON-NLS-1$
			Map result= new HashMap();
			IMethod[] methods= ReferenceFinderUtil.getMethodsReferencedIn(new IMember[]{member}, new SubProgressMonitor(pm, 1));
			ICompilationUnit cu= getWorkingCopyForDeclaringTypeCu(member);
			IMember[] interestingMethods= getMembersThatNeedReferenceConversion(methods);
			for (int i= 0; i < interestingMethods.length; i++) {
				IMethod method= (IMethod)interestingMethods[i];
				//XX side effect
				fImportManager.addImportTo(method.getDeclaringType(), fDestinationCu);
				String newSource= method.getDeclaringType().getElementName() + "." + method.getElementName(); //$NON-NLS-1$
				SearchResult[] searchResults= findReferencesInMember(member, method, new SubProgressMonitor(pm, 1));
				ISourceRange[] ranges= MethodInvocationFinder.findMessageSendRanges(searchResults, cu);
				putAllToMap(result, newSource, ranges);
			}
			return result;
		}
	
		//ISourceRange -> String (new source)
		private Map getTypeReferencesInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 2); //$NON-NLS-1$
			Map result= new HashMap();
			IType[] types= ReferenceFinderUtil.getTypesReferencedIn(new IMember[]{member}, new SubProgressMonitor(pm, 1));
			ICompilationUnit cu= getWorkingCopyForDeclaringTypeCu(member);
			IMember[] interestingTypes= getMembersThatNeedReferenceConversion(types);
			for (int i= 0; i < interestingTypes.length; i++) {
				IType type= (IType)interestingTypes[i];
				//XX side effect
				fImportManager.addImportTo(type.getDeclaringType(), cu);
				String newSource= type.getDeclaringType().getElementName() + "." + type.getElementName(); //$NON-NLS-1$
				SearchResult[] searchResults= findReferencesInMember(member, type, new SubProgressMonitor(pm, 1));
				ISourceRange[] ranges= TypeReferenceFinder.findTypeReferenceRanges(searchResults, cu);
				putAllToMap(result, newSource, ranges);
			}
			return result;
		}

		private static ICompilationUnit getWorkingCopyForDeclaringTypeCu(IMember member){
			return WorkingCopyUtil.getWorkingCopyIfExists(member.getDeclaringType().getCompilationUnit());		
		}
	
		private IMember[] getMembersThatNeedReferenceConversion(IMember[] members) throws JavaModelException{
			Set memberSet= new HashSet(); //using set to remove dups
			for (int i= 0; i < members.length; i++) {
				if (willNeedToConvertReferenceTo(members[i]))
					memberSet.add(members[i]);
			}
			return (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
		}
	
		private boolean willNeedToConvertReferenceTo(IMember member) throws JavaModelException{
			if (! member.exists())
				return false;
			if (fMovedMembers.contains(member))
				return false;
			if (! JdtFlags.isStatic(member)) //convert all static references
				return false;
			return true;		
		}

		private static SearchResult[] findReferencesInMember(IMember scopeMember, IMember referenceMember, IProgressMonitor pm) throws JavaModelException {
			IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[]{scopeMember});
			SearchResultCollector collector= new SearchResultCollector(pm);
			new SearchEngine().search(ResourcesPlugin.getWorkspace(), referenceMember, IJavaSearchConstants.REFERENCES, scope, collector);
			List results= collector.getResults();
			SearchResult[] searchResults= (SearchResult[]) results.toArray(new SearchResult[results.size()]);
			return searchResults;
		}
			
		private static void putAllToMap(Map result, String newSource, ISourceRange[] ranges) {
			for (int i= 0; i < ranges.length; i++) {
				result.put(ranges[i], newSource);
			}
		}
	}
	private static class TypeReferenceFinder {

		public static ISourceRange[] findTypeReferenceRanges(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException {
			Assert.isNotNull(searchResults);
			if (searchResults.length == 0)
				return new ISourceRange[0];
			
			TypeReferenceFinderVisitor visitor= new TypeReferenceFinderVisitor(searchResults);
			AST.parseCompilationUnit(cu, false).accept(visitor);
			return visitor.getFoundRanges();
		}
		
		private static class TypeReferenceFinderVisitor extends ASTVisitor{
			
			private Collection fFoundRanges;
			private SearchResult[] fSearchResults;
			
			TypeReferenceFinderVisitor(SearchResult[] searchResults){
				fSearchResults= searchResults;
				fFoundRanges= new ArrayList(0);
			}
			
			ISourceRange[] getFoundRanges(){
				return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
			}
			
			private static boolean areReportedForSameNode(ASTNode node, SearchResult searchResult){
				if (node.getStartPosition() != searchResult.getStart())
					return false;
				if (ASTNodes.getExclusiveEnd(node) < searchResult.getEnd())	
					return false;
			
				return true;			
			}
			
			private boolean isReported(ASTNode node){
				for (int i= 0; i < fSearchResults.length; i++) {
					if (areReportedForSameNode(node, fSearchResults[i]))
						return true;
				}
				return false;
			}
			
			//--- visit methods ---
	
			public boolean visit(SimpleName node) {
				if (! isReported(node))
					return true;
					
				fFoundRanges.add(new SourceRange(node));				
				return false;	
			}
	
		}
	}
	private static class FieldReferenceFinder {

		public static ISourceRange[] findFieldReferenceRanges(SearchResultGroup searchResultGroup) throws JavaModelException {
			ICompilationUnit cu= searchResultGroup.getCompilationUnit();
			if (cu == null)
				return new ISourceRange[0];
			return findFieldReferenceRanges(searchResultGroup.getSearchResults(), cu);
		}
	
		public static ISourceRange[] findFieldReferenceRanges(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException {
			Assert.isNotNull(searchResults);
			if (searchResults.length == 0)
				return new ISourceRange[0];
			FieldReferenceFinderVisitor visitor= new FieldReferenceFinderVisitor(searchResults);
			AST.parseCompilationUnit(cu, false).accept(visitor);
			return visitor.getFoundRanges();
		}
	
		///---- ast visitor ----------
	
		private static class FieldReferenceFinderVisitor extends ASTVisitor{
			private Collection fFoundRanges;
			private SearchResult[] fSearchResults;
		
			FieldReferenceFinderVisitor(SearchResult[] searchResults){
				fFoundRanges= new ArrayList();
				fSearchResults= searchResults;
			}
		
			ISourceRange[] getFoundRanges(){
				return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
			}
		
			private static boolean areReportedForSameNode(SimpleName node, SearchResult searchResult){
				if (node.getStartPosition() != searchResult.getStart())
					return false;
				if (ASTNodes.getExclusiveEnd(node) != searchResult.getEnd())	
					return false;
				
				return true;	
			}
	
			private static boolean areReportedForSameNode(FieldAccess node, SearchResult searchResult){
				if (node.getStartPosition() > searchResult.getStart())
					return false;
				if (ASTNodes.getExclusiveEnd(node) != searchResult.getEnd())	
					return false;
				if (node.getName().getStartPosition() != searchResult.getStart())
					return false;
				
				return true;	
			}	
		
			private static boolean areReportedForSameNode(QualifiedName node, SearchResult searchResult){
				if (node.getStartPosition() > searchResult.getStart())
					return false;
				if (ASTNodes.getExclusiveEnd(node) != searchResult.getEnd())	
					return false;
				
				return true;	
			}
		
			private boolean isReported(FieldAccess node){
				for (int i= 0; i < fSearchResults.length; i++) {
					if (areReportedForSameNode(node, fSearchResults[i]))
						return true;
				}
				return false;
			}
		
			private boolean isReported(QualifiedName node){
				for (int i= 0; i < fSearchResults.length; i++) {
					if (areReportedForSameNode(node, fSearchResults[i]))
						return true;
				}
				return false;
			}
		
			private boolean isReported(SimpleName node){
				for (int i= 0; i < fSearchResults.length; i++) {
					if (areReportedForSameNode(node, fSearchResults[i]))
						return true;
				}
				return false;
			}
		
			//-- visit methods ---
		
			public boolean visit(FieldAccess node) {
				if (! isReported(node))
					return true;
			
				fFoundRanges.add(new SourceRange(node));
				return false;
			}
	
			public boolean visit(QualifiedName node) {
				if (! isReported(node))
					return true;
			
				fFoundRanges.add(new SourceRange(node));
				return false;
			}
	
			public boolean visit(SimpleName node) {
				if (! isReported(node))
					return true;
			
				fFoundRanges.add(new SourceRange(node));
				return false;
			}
		
		}
	}
}
