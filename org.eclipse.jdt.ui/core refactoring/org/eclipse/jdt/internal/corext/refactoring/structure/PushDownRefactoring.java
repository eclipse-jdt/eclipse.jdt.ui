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

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class PushDownRefactoring extends Refactoring {
	
	public static class MemberActionInfo implements IMemberActionInfo{

		private final IMember fMember;
		private int fAction;
		public static final int PUSH_DOWN_ACTION= 		0;
		public static final int PUSH_ABSTRACT_ACTION= 	1;
		public static final int NO_ACTION= 				2;
		private MemberActionInfo(IMember member, int action){
			assertValidAction(member, action);
			Assert.isTrue(member instanceof IField || member instanceof IMethod);
			fMember= member;
			fAction= action;
		}
		
		public void setAction(int action){
			assertValidAction(fMember, action);
			if (isFieldInfo())
				Assert.isTrue(action != PUSH_ABSTRACT_ACTION);
			fAction= action;	
		}
	
		public int[] getAvailableActions(){
			if (isFieldInfo())
				return new int[]{PUSH_DOWN_ACTION, NO_ACTION};
			else
				return new int[]{PUSH_DOWN_ACTION, PUSH_ABSTRACT_ACTION, NO_ACTION};
		}
		
		private static void assertValidAction(IMember member, int action) {
			if (member instanceof IMethod)
				Assert.isTrue(	action == PUSH_ABSTRACT_ACTION || 
								action == NO_ACTION ||
								action == PUSH_DOWN_ACTION);
			else if (member instanceof IField)
				Assert.isTrue(	action == NO_ACTION ||
								action == PUSH_DOWN_ACTION);
		}
		
		public static MemberActionInfo create(IMember member) {
			return new MemberActionInfo(member, NO_ACTION);
		}

		public boolean isToBePushedDown() {
			return fAction == PUSH_DOWN_ACTION;
		}

		boolean isToBeDeletedFromDeclaringClass() {
			return isToBePushedDown();
		}

		boolean isNewMethodToBeDeclaredAbstract() throws JavaModelException {
			return 
				! isFieldInfo() &&
				! JdtFlags.isAbstract(fMember) &&
				fAction == PUSH_ABSTRACT_ACTION;
		}		

		boolean isToBeCreatedInSubclassesOfDeclaringClass() {
			return fAction != NO_ACTION;
		}

		public IMember getMember() {
			return fMember;
		}
		
		public int getAction(){
			return fAction;
		}

		boolean isFieldInfo() {
			return fMember instanceof IField;
		}

		int getNewModifiersForCopyInSubclass(int oldModifiers) throws JavaModelException {
			if (isFieldInfo())
				return oldModifiers;
			if (isToBeDeletedFromDeclaringClass())
				return oldModifiers;
			int modifiers= oldModifiers;	
			if (isNewMethodToBeDeclaredAbstract()){
				if (! JdtFlags.isPublic(fMember))
					modifiers= Modifier.PROTECTED | JdtFlags.clearAccessModifiers(modifiers);
			}
			return modifiers;
		}
		
		int getNewModifiersForOriginal(int oldModifiers) throws JavaModelException{
			if (isFieldInfo())
				return oldModifiers;
			if (isToBeDeletedFromDeclaringClass())
				return oldModifiers;
			int modifiers= oldModifiers;	
			if (isNewMethodToBeDeclaredAbstract()){
				modifiers= JdtFlags.clearFlag(Modifier.FINAL | Modifier.NATIVE, oldModifiers);
				modifiers |= Modifier.ABSTRACT;

				if (! JdtFlags.isPublic(fMember))
					modifiers= Modifier.PROTECTED | JdtFlags.clearAccessModifiers(modifiers);
			}
			return modifiers;
		}

		//XXX incorrect to have a method like this in the model
		public boolean isEditable(){
			if (isFieldInfo())
				return false;
			if (getAction() == MemberActionInfo.NO_ACTION)
				return false;
			return true;
		}

		boolean copyJavadocToCopiesInSubclasses() {
			return isToBeDeletedFromDeclaringClass();
		}
		
		static IMember[] getMembers(MemberActionInfo[] infos){
			IMember[] result= new IMember[infos.length];
			for (int i= 0; i < result.length; i++) {
				result[i]= infos[i].getMember();
			}
			return result;
		}

		public boolean isActive() {
			return getAction() != NO_ACTION;
		}

	}
	
	private MemberActionInfo[] fMemberInfos;
	private IMember[] fSelectedMembers;
	private TextChangeManager fChangeManager;
	
	private final ImportRewriteManager fImportManager;

	//caches
	private IType fCachedDeclaringClass;
	private ITypeHierarchy fCachedClassHierarchy;
	private IType[] fTypesReferencedInPushedDownMembers;
	
	private PushDownRefactoring(IMember[] members, CodeGenerationSettings preferenceSettings){
		Assert.isNotNull(members);
		Assert.isNotNull(preferenceSettings);
		fSelectedMembers= (IMember[])SourceReferenceUtil.sortByOffset(members);
		fImportManager= new ImportRewriteManager(preferenceSettings);
	}

	public static PushDownRefactoring create(IMember[] members, CodeGenerationSettings preferenceSettings) throws JavaModelException{
		if (! isAvailable(members))
			return null;
		if (isOneTypeWithPushableMembers(members)) {
			PushDownRefactoring result= new PushDownRefactoring(new IMember[0], preferenceSettings);
			result.fCachedDeclaringClass= getSingleTopLevelType(members);
			return result;
		}
		return new PushDownRefactoring(members, preferenceSettings);
	}
	
	public static boolean isAvailable(IMember[] members) throws JavaModelException{
		if (isOneTypeWithPushableMembers(members))
			return true;

		return 	members != null &&
				members.length != 0 &&
				areAllPushable(members) &&
				haveCommonDeclaringType(members);
	}
	
	private static boolean isOneTypeWithPushableMembers(IMember[] members) throws JavaModelException {
		IType singleTopLevelType= getSingleTopLevelType(members);
		return (singleTopLevelType != null && getPushableMembers(singleTopLevelType).length != 0);
	}
	
	private static IType getSingleTopLevelType(IMember[] members) {
		if (members != null && members.length == 1 && Checks.isTopLevelType(members[0]))
			return (IType)members[0];
		return null;
	}
		
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("PushDownRefactoring.name"); //$NON-NLS-1$
	}

	public IType getDeclaringClass(){
		if (fCachedDeclaringClass != null)
			return fCachedDeclaringClass;
		//all members declared in same type - checked in precondition
		fCachedDeclaringClass= (IType)WorkingCopyUtil.getOriginal(fSelectedMembers[0].getDeclaringType()); //index safe - checked in constructor
		return fCachedDeclaringClass;
	}
	
	private ICompilationUnit getDeclaringWorkingCopy(){
		return WorkingCopyUtil.getWorkingCopyIfExists(getDeclaringClass().getCompilationUnit());
	}

	private static boolean haveCommonDeclaringType(IMember[] members) {
		if (members.length == 0)
			return false;
		IType declaringType= members[0].getDeclaringType();
		if (declaringType == null)
			return false;
		for (int i= 0; i < members.length; i++) {
			if (! declaringType.equals(members[i].getDeclaringType()))
				return false;
		}
		return true;
	}

	private static boolean areAllPushable(IMember[] members) throws JavaModelException {
		for (int i = 0; i < members.length; i++) {
			if (! isPushable(members[i]))
				return false;
		}
		return true;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			fSelectedMembers= WorkingCopyUtil.getOriginals(fSelectedMembers);
			
			result.merge(checkPossibleSubclasses(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;						
			result.merge(checkDeclaringType());
			if (result.hasFatalError())
				return result;			
			result.merge(checkIfMembersExist());
			if (result.hasFatalError())
				return result;			
			
			fMemberInfos= createInfosForAllPushableFieldsAndMethods(getDeclaringClass());
			setInfoAction(MemberActionInfo.PUSH_DOWN_ACTION, fSelectedMembers);
			return result;	
		} finally {
			pm.done();
		}
	}
	
	private void setInfoAction(int action, IMember[] members) {
		List list= Arrays.asList(members);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (list.contains(info.getMember()))
				info.setAction(action);
		}
	}

	private RefactoringStatus checkPossibleSubclasses(IProgressMonitor pm) throws JavaModelException {
		IType[] modifiableSubclasses= getDestinationClassesForNonAbstractMembers(pm);
		if (modifiableSubclasses.length == 0){
			String msg= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.no_subclasses", new String[]{createTypeLabel(getDeclaringClass())});//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		return new RefactoringStatus();
	}

	private RefactoringStatus checkIfMembersExist() {
		for (int i= 0; i < fSelectedMembers.length; i++) {
			IMember orig= fSelectedMembers[i];
			if (orig == null || ! orig.exists()){
				String message= RefactoringCoreMessages.getString("PushDownRefactoring.not_in_saved"); //$NON-NLS-1$
				return RefactoringStatus.createFatalErrorStatus(message);
			}	
		}
		return new RefactoringStatus();
	}

	private RefactoringStatus checkDeclaringType() throws JavaModelException {
		IType declaringType= getDeclaringClass();
				
		if (declaringType.isInterface()) //for now
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PushDownRefactoring.interface_members")); //$NON-NLS-1$
		
		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PushDownRefactoring.members_of_binary")); //$NON-NLS-1$

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PushDownRefactoring.members_of_read-only")); //$NON-NLS-1$
		
		return new RefactoringStatus();
	}

	private static MemberActionInfo[] createInfosForAllPushableFieldsAndMethods(IType type) throws JavaModelException {
		List result= new ArrayList();
		IMember[] pushableMembers= getPushableMembers(type);
		for (int i= 0; i < pushableMembers.length; i++) {
			result.add(MemberActionInfo.create(pushableMembers[i]));
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private static IMember[] getPushableMembers(IType type) throws JavaModelException {
		List list= new ArrayList(3);
		addAllPushable(type.getFields(), list);
		addAllPushable(type.getMethods(), list);
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	private static void addAllPushable(IMember[] members, List list) throws JavaModelException{
		for (int i= 0; i < members.length; i++) {
			if (isPushable(members[i]))
				list.add(members[i]);
		}	
	}

	private static boolean isPushable(IMember member) throws JavaModelException {
		if (member.getElementType() != IJavaElement.METHOD && 
			member.getElementType() != IJavaElement.FIELD)
			return false;

		if (! Checks.isAvailable(member))
			return false;
		
		if (JdtFlags.isStatic(member))
			return false;
			
		if (member.getElementType() == IJavaElement.METHOD){
			IMethod method= (IMethod) member;
			if (method.isConstructor())
				return false;
			
			if (JdtFlags.isNative(method))
				return false;
		}
		return true;
	}

	public MemberActionInfo[] getMemberActionInfos(){
		return fMemberInfos;
	}
	
	public void computeAdditionalRequiredMembersToPushDown(IProgressMonitor pm) throws JavaModelException {
		IMember[] additional= getAdditionalRequiredMembers(pm);
		setInfoAction(MemberActionInfo.PUSH_DOWN_ACTION, additional);
	}
	
	public IMember[] getAdditionalRequiredMembers(IProgressMonitor pm) throws JavaModelException {
		IMember[] members= getMembersToBeCreatedInSubclasses();
		pm.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.calculating"), members.length);//not true, but not easy to give anything better //$NON-NLS-1$
		List queue= new ArrayList(members.length);
		queue.addAll(Arrays.asList(members));
		if (queue.isEmpty())
			return new IMember[0];
		int i= 0;
		IMember current;
		do{
			current= (IMember)queue.get(i);
			addAllRequiredPushableMembers(queue, current, new SubProgressMonitor(pm, 1));
			i++;
			if (queue.size() == i)
				current= null;
		} while(current != null);
		queue.removeAll(Arrays.asList(members));//report only additional
		return (IMember[]) queue.toArray(new IMember[queue.size()]);
	}
	
	private void addAllRequiredPushableMembers(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		addAllRequiredPushableMethods(queue, member, new SubProgressMonitor(pm, 1));
		addAllRequiredPushableFields(queue, member, new SubProgressMonitor(pm, 1));
		pm.done();
	}
	
	private void addAllRequiredPushableFields(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[]{member}, pm);
		for (int i= 0; i < requiredFields.length; i++) {
			IField field= requiredFields[i];
			if (isRequiredPushableMember(queue, field))
				queue.add(field);
		}
	}
	
	private void addAllRequiredPushableMethods(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[]{member}, new SubProgressMonitor(pm, 1));
		SubProgressMonitor sPm= new SubProgressMonitor(pm, 1);
		sPm.beginTask("", requiredMethods.length); //$NON-NLS-1$
		for (int i= 0; i < requiredMethods.length; i++) {
			IMethod method= requiredMethods[i];
			if (! MethodChecks.isVirtual(method) && isRequiredPushableMember(queue, method))
				queue.add(method);
		}
		sPm.done();
	}

	private boolean isRequiredPushableMember(List queue, IMember member) throws JavaModelException {
		return member.getDeclaringType().equals(getDeclaringClass()) && ! queue.contains(member) && isPushable(member);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.creating_preview"), 5); //$NON-NLS-1$
			clearCaches();
						
			RefactoringStatus result= new RefactoringStatus();
			
			result.merge(checkMembersInDestinationClasses(new SubProgressMonitor(pm, 1)));
			result.merge(checkElementsAccessedByModifiedMembers(new SubProgressMonitor(pm, 1)));
			result.merge(checkReferencesToPushedDownMembers(new SubProgressMonitor(pm, 1)));
			if (shouldMakeDeclaringClassAbstract())
				result.merge(checkCallsToDeclaringClassConstructors(new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);	

			if (result.hasFatalError())
				return result;

			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}	
	}

	private void clearCaches() {
		fTypesReferencedInPushedDownMembers= null;
		fImportManager.clear();
	}

	private RefactoringStatus checkReferencesToPushedDownMembers(IProgressMonitor pm) throws JavaModelException {
		IMember[] membersToPush= getMembersToPushDown();
		RefactoringStatus result= new RefactoringStatus();
		List movedMembers= Arrays.asList(getMembersToBeCreatedInSubclasses());
		pm.beginTask("", membersToPush.length); //$NON-NLS-1$
		for (int i= 0; i < membersToPush.length; i++) {
			IMember member= membersToPush[i];
			String label= createLabel(member);
			IJavaElement[] referencing= getReferencingElementsFromSameClass(member, new SubProgressMonitor(pm, 1));
			for (int j= 0; j < referencing.length; j++) {
				IJavaElement element= referencing[j];
				if (movedMembers.contains(element))
					continue;
				if (! (element instanceof IMember))
					continue;
				IMember referencingMember= (IMember)element;	
				Object[] keys= {label, createLabel(referencingMember)};
				String msg= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.referenced", keys); //$NON-NLS-1$
				result.addError(msg, JavaStatusContext.create(referencingMember));
			}
		}
		pm.done();
		return result;	
	}

	private static IJavaElement[] getReferencingElementsFromSameClass(IMember member, IProgressMonitor pm) throws JavaModelException {
		ISearchPattern pattern= RefactoringSearchEngine.createSearchPattern(new IJavaElement[]{member}, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[]{member.getDeclaringType()});
		SearchResultGroup[] groups= RefactoringSearchEngine.search(pm, scope, pattern);
		Set result= new HashSet(3);
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			SearchResult[] results= group.getSearchResults();
			for (int j= 0; j < results.length; j++) {
				SearchResult searchResult= results[i];
				result.add(searchResult.getEnclosingElement());
			}
		}
		return (IJavaElement[]) result.toArray(new IJavaElement[result.size()]);
	}

	private RefactoringStatus checkElementsAccessedByModifiedMembers(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.checking"), 3); //$NON-NLS-1$
		IType[] subclasses= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
		result.merge(checkAccessedTypes(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(subclasses, new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAccessedTypes(IType[] subclasses, IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypeReferencedInPushedDownMembers(pm);
		for (int i= 0; i < subclasses.length; i++) {
			IType targetClass= subclasses[i];
			for (int j= 0; j < accessedTypes.length; j++) {
				IType type= accessedTypes[j];
				if (! canBeAccessedFrom(type, targetClass)){
					String message= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.type_not_accessible", new String[]{createTypeLabel(type), createTypeLabel(targetClass)}); //$NON-NLS-1$
					result.addError(message, JavaStatusContext.create(type));
				}	
			}
		}
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAccessedFields(IType[] subclasses, IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= getMembersToBeCreatedInSubclasses();
		List pushedDownList= Arrays.asList(membersToPushDown);
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(membersToPushDown, pm);
		for (int i= 0; i < subclasses.length; i++) {	
			IType targetClass= subclasses[i];
			for (int j= 0; j < accessedFields.length; j++) {
				IField field= accessedFields[j];
				boolean isAccessible= 	pushedDownList.contains(field) || 
										canBeAccessedFrom(field, targetClass);
				if (! isAccessible){
					String message= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.field_not_accessible", new String[]{createFieldLabel(field), createTypeLabel(targetClass)}); //$NON-NLS-1$
					result.addError(message, JavaStatusContext.create(field));
				} 
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IType[] subclasses, IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= getMembersToBeCreatedInSubclasses();
		List pushedDownList= Arrays.asList(membersToPushDown);
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(membersToPushDown, pm);
		for (int i= 0; i < subclasses.length; i++) {
			IType targetClass= subclasses[i];
			for (int j= 0; j < accessedMethods.length; j++) {
				IMethod method= accessedMethods[j];
				boolean isAccessible= canBeAccessedFrom(method, targetClass) ||
									   pushedDownList.contains(method);
				if (! isAccessible){
					String message= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.method_not_accessible", new String[]{createMethodLabel(method), createTypeLabel(targetClass)}); //$NON-NLS-1$
					result.addError(message, JavaStatusContext.create(method));
				}
			}
		}
		pm.done();
		return result;
	}
	
	private boolean canBeAccessedFrom(IMember member, IType newHome) throws JavaModelException{
		Assert.isTrue(!(member instanceof IInitializer));
		if (! member.exists())
			return false;
			
		if (newHome.equals(member.getDeclaringType()))
			return true;
			
		if (newHome.equals(member))
			return true;	
		
		if (JdtFlags.isPrivate(member))
			return false;
			
		if (member.getDeclaringType() == null){ //top level
			if (! (member instanceof IType))
				return false;

			if (JdtFlags.isPublic(member))
				return true;
			
			if (! JdtFlags.isPackageVisible(member))
				return false;
			
			return JavaModelUtil.isSamePackage(((IType)member).getPackageFragment(), newHome.getPackageFragment());		
		} else {
			IType enclosingType= member.getDeclaringType();
			
			if (! canBeAccessedFrom(enclosingType, newHome))
				return false;

			boolean samePackage= JavaModelUtil.isSamePackage(enclosingType.getPackageFragment(), newHome.getPackageFragment());
			if (samePackage)
				return true; //private checked before

			if (enclosingType.equals(getDeclaringClass()))
				return JdtFlags.isPublic(member) || JdtFlags.isProtected(member);
			else
				return JdtFlags.isPublic(member);
		}
	}

	private IType[] getTypeReferencedInPushedDownMembers(IProgressMonitor pm) throws JavaModelException {
		if (fTypesReferencedInPushedDownMembers == null)
			fTypesReferencedInPushedDownMembers= ReferenceFinderUtil.getTypesReferencedIn(getMembersToBeCreatedInSubclasses(), pm);
		return fTypesReferencedInPushedDownMembers;
	}

	private IMember[] getMembersToBeCreatedInSubclasses() throws JavaModelException {
		return MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
	}

	private IMember[] getMembersToPushDown() {
		List fields= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBePushedDown())
				fields.add(info.getMember());
		}
		return (IMember[]) fields.toArray(new IMember[fields.size()]);
	}
	
	private RefactoringStatus checkCallsToDeclaringClassConstructors(IProgressMonitor pm) throws JavaModelException {
		SearchResultGroup[] groups= ConstructorReferenceFinder.getConstructorReferences(getDeclaringClass(), pm);

		String msg= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.gets_instantiated", new Object[]{createTypeLabel(getDeclaringClass())}); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < groups.length; i++) {
			ICompilationUnit cu= groups[i].getCompilationUnit();
			if (cu == null)
				continue;
			CompilationUnit cuNode= AST.parseCompilationUnit(cu, false);
			ASTNode[] refNodes= ASTNodeSearchUtil.getAstNodes(groups[i].getSearchResults(), cuNode);
			for (int j= 0; j < refNodes.length; j++) {
				ASTNode node= refNodes[j];
				if ((node instanceof ClassInstanceCreation) || ConstructorReferenceFinder.isImplicitConstructorReferenceNodeInClassCreations(node)){
					Context context= JavaStatusContext.create(cu, node);
					result.addError(msg, context);
				}
			}
		}
		return result;
	}
	
	private RefactoringStatus checkMembersInDestinationClasses(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= getMembersToBeCreatedInSubclasses();

		IType[] destinationClassesForNonAbstract= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
		result.merge(checkNonAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForNonAbstract));
		
		IType[] destinationClassesForAbstract= getDestinationClassesForAbstractMembers(new SubProgressMonitor(pm, 1));
		result.merge(checkAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForAbstract));
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForAbstract) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] abstractMembersToPushDown= getAbstractMembers(membersToPushDown);
		for (int i= 0; i < destinationClassesForAbstract.length; i++) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(abstractMembersToPushDown, destinationClassesForAbstract[i]));
		}
		return result;
	}
	
	private RefactoringStatus checkNonAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForNonAbstract) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] nonAbstractMembersToPushDown= getNonAbstractMembers(membersToPushDown);
		for (int i= 0; i < destinationClassesForNonAbstract.length; i++) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(nonAbstractMembersToPushDown, destinationClassesForNonAbstract[i]));
		}
		return result;
	}
	
	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	//--  change creation -------
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			return new CompositeChange("Push down", fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
			clearCaches();
		}
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.preview"), 4); //$NON-NLS-1$

			addImportsToSubclasses(new SubProgressMonitor(pm, 1));

			TextChangeManager manager= new TextChangeManager();

			MemberActionInfo[] abstractMembersToCopyToSubclasses= getInfosForAbstractMembersToBeCreatedInSubclassesOfDeclaringClass();
			MemberActionInfo[] nonAbstractMembersToCopyToSubclasses= getInfosForNonAbstractMembersToBeCreatedInSubclassesOfDeclaringClass();
			IType[] destinationsForNonAbstract= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
			IType[] destinationsForAbstract= getDestinationClassesForAbstractMembers(new SubProgressMonitor(pm, 1));
			
			ICompilationUnit declaringCu= getDeclaringWorkingCopy();
			CompilationUnit declaringCuNode= AST.parseCompilationUnit(declaringCu, true);
			ICompilationUnit[] cus= getCusToProcess(new SubProgressMonitor(pm, 1));
			for (int i= 0; i < cus.length; i++) {
				ICompilationUnit cu= cus[i];
				CompilationUnit cuNode= cu.equals(declaringCu) ? declaringCuNode: AST.parseCompilationUnit(cu, true);
				ASTRewrite rewrite= new ASTRewrite(cuNode);

				if (cu.equals(declaringCu)){
					TypeDeclaration declaringClass= ASTNodeSearchUtil.getTypeDeclarationNode(getDeclaringClass(), cuNode);
					if (shouldMakeDeclaringClassAbstract())
						makeDeclaringClassAbstract(declaringClass, rewrite);
					deleteDeclarationNodes(cuNode, rewrite, Arrays.asList(getMembersToDeleteFromDeclaringClass()));
					makeMethodsAbstractInDeclaringClass(declaringCuNode, rewrite);
				}
				copyMembers(abstractMembersToCopyToSubclasses, destinationsForAbstract, declaringCuNode, cu, cuNode, rewrite);
				copyMembers(nonAbstractMembersToCopyToSubclasses, destinationsForNonAbstract, declaringCuNode, cu, cuNode, rewrite);
				addTextEditFromRewrite(manager, cu, rewrite);
			}
			return manager;
		} finally{
			pm.done();
		}
	}

	private void copyMembers(MemberActionInfo[] membersToCopyToSubclasses, IType[] destinationTypes, CompilationUnit declaringCuNode, ICompilationUnit cu, CompilationUnit cuNode, ASTRewrite rewrite) throws JavaModelException {
		for (int i= 0; i < destinationTypes.length; i++) {
			IType dest= destinationTypes[i];
			if (cu.equals(WorkingCopyUtil.getWorkingCopyIfExists(dest.getCompilationUnit()))) {
				List bodyDeclarations= ASTNodeSearchUtil.getBodyDeclarationList(dest, cuNode);
				createAll(membersToCopyToSubclasses, bodyDeclarations, declaringCuNode, rewrite);
			}
		}
	}

	private void addTextEditFromRewrite(TextChangeManager manager, ICompilationUnit cu, ASTRewrite rewrite) throws CoreException {
		TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
		TextEdit resultingEdits= new MultiTextEdit();
		rewrite.rewriteNode(textBuffer, resultingEdits);

		TextChange textChange= manager.get(cu);
		if (fImportManager.hasImportEditFor(cu))
			resultingEdits.addChild(fImportManager.getImportRewrite(cu).createEdit(textBuffer));
		textChange.addTextEdit(RefactoringCoreMessages.getString("PushDownRefactoring.25"), resultingEdits); //$NON-NLS-1$
		rewrite.removeModifications();
	}

	private ICompilationUnit[] getCusToProcess(IProgressMonitor pm) throws JavaModelException {
		IType[] subTypes= getAllDirectSubclassesOfDeclaringClass(pm);
		Set result= new HashSet(subTypes.length + 1);
		for (int i= 0; i < subTypes.length; i++) {
			result.add(WorkingCopyUtil.getWorkingCopyIfExists(subTypes[i].getCompilationUnit()));
		}
		result.add(getDeclaringWorkingCopy());
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	private void addImportsToSubclasses(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 4); //$NON-NLS-1$
		IMember[] allMembers= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		IMember[] nonAbstractMembers= getNonAbstractMembers(allMembers);
		IType[] destForNonAbstract= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
		addImportsToTypesReferencedInMembers(nonAbstractMembers, destForNonAbstract, new SubProgressMonitor(pm, 1));

		IMember[] abstractMembers= getAbstractMembers(allMembers);
		IType[] destForAbstract= getDestinationClassesForAbstractMembers(new SubProgressMonitor(pm, 1));
		addImportsToTypesReferencedInMembers(abstractMembers, destForAbstract, new SubProgressMonitor(pm, 1));
		pm.done();
	}
	
	private void addImportsToTypesReferencedInMembers(IMember[] members, IType[] destinationClasses, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", 1); //$NON-NLS-1$
		IType[] typesReferenced= getTypesReferencedIn(members, pm);
		for (int i= 0; i < destinationClasses.length; i++) {
			ICompilationUnit cu= getWorkingCopyOfCu(destinationClasses[i]);
			for (int j= 0; j < typesReferenced.length; j++) {
				fImportManager.addImportTo(typesReferenced[j], cu);
			}
		}
		pm.done();
	}
	
	private IType[] getTypesReferencedIn(IMember[] members, IProgressMonitor pm) throws JavaModelException {
		return ReferenceFinderUtil.getTypesReferencedIn(members, pm);
	}

	/*
	 * members: List<IMember>
	 */
	//XXX copied from pull up
	private static void deleteDeclarationNodes(CompilationUnit cuNode, ASTRewrite rewrite, List members) throws JavaModelException {
		List declarationNodes= getDeclarationNodes(cuNode, members);
		for (Iterator iter= declarationNodes.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			if (node instanceof VariableDeclarationFragment){
				if (node.getParent() instanceof FieldDeclaration){
					FieldDeclaration fd= (FieldDeclaration)node.getParent();
					if (areAllFragmentsDeleted(fd, declarationNodes))
						rewrite.markAsRemoved(fd);
					else
						rewrite.markAsRemoved(node);
				}
			} else {
				rewrite.markAsRemoved(node);
			}
		}
	}
	
	//XXX copied from pull up
	private static boolean areAllFragmentsDeleted(FieldDeclaration fd, List declarationNodes) {
		for (Iterator iter= fd.fragments().iterator(); iter.hasNext();) {
			if (! declarationNodes.contains(iter.next()))
				return false;
		}
		return true;
	}

	/*
	 * return List<ASTNode>
	 * members List<IMember>
	 */
	//XXX copied from pull up
	private static List getDeclarationNodes(CompilationUnit cuNode, List members) throws JavaModelException {
		List result= new ArrayList(members.size());
		for (Iterator iter= members.iterator(); iter.hasNext(); ) {
			IMember member= (IMember)iter.next();
			ASTNode declarationNode= null;
			if (member instanceof IField)
				declarationNode= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField)member, cuNode);
			else if (member instanceof IType)
				declarationNode= ASTNodeSearchUtil.getTypeDeclarationNode((IType)member, cuNode);
			else if (member instanceof IMethod)
				declarationNode= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod)member, cuNode);
			if (declarationNode != null)
				result.add(declarationNode);
		}
		return result;
	}
	
	private IMember[] getMembersToDeleteFromDeclaringClass() {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBeDeletedFromDeclaringClass())
				result.add(info.getMember());
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private void createAll(MemberActionInfo[] members, List bodyDeclarations, CompilationUnit declaringCuNode, ASTRewrite rewrite) throws JavaModelException {
		for (int i= 0; i < members.length; i++) {
			create(members[i], bodyDeclarations, declaringCuNode, rewrite);
		}
	}
	
	private void create(MemberActionInfo info, List bodyDeclarations, CompilationUnit declaringCuNode, ASTRewrite rewrite) throws JavaModelException {
		if (info.isFieldInfo())
			createField(info, bodyDeclarations, declaringCuNode, rewrite);
		else
			createMethod(info, bodyDeclarations, declaringCuNode, rewrite);
	}

	private void createMethod(MemberActionInfo info, List bodyDeclarations, CompilationUnit declaringCuNode, ASTRewrite rewrite) throws JavaModelException {
		MethodDeclaration newMethod= createNewMethodDeclarationNode(info, declaringCuNode, rewrite);
		rewrite.markAsInserted(newMethod);
		bodyDeclarations.add(ASTNodes.getInsertionIndex(newMethod, bodyDeclarations), newMethod);
	}

	private MethodDeclaration createNewMethodDeclarationNode(MemberActionInfo info, CompilationUnit declaringCuNode, ASTRewrite rewrite) throws JavaModelException {
		Assert.isTrue(! info.isFieldInfo());
		IMethod method= (IMethod)info.getMember();
		MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode(method, declaringCuNode);
		AST ast= getAST(rewrite);
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		copyBodyOfPushedDownMethod(rewrite, method, oldMethod, newMethod);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		if (info.copyJavadocToCopiesInSubclasses())
			copyJavadocNode(rewrite, method, oldMethod, newMethod);
		newMethod.setModifiers(getNewModifiersForCopiedMethod(info, oldMethod));
		newMethod.setName(ast.newSimpleName(oldMethod.getName().getIdentifier()));
		copyReturnType(rewrite, oldMethod, newMethod);
		copyParameters(rewrite, oldMethod, newMethod);
		copyThrownExceptions(oldMethod, newMethod);
		return newMethod;
	}

	private static void copyBodyOfPushedDownMethod(ASTRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		if (oldMethod.getBody() == null){
			newMethod.setBody(null);
			return;
		}
		Block oldBody= oldMethod.getBody();
		String oldBodySource= getBufferText(oldBody, method.getCompilationUnit());
		String[] lines= Strings.convertIntoLines(oldBodySource);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		oldBodySource= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
		Block newBody= (Block)targetRewrite.createPlaceholder(oldBodySource, ASTRewrite.BLOCK);
		newMethod.setBody(newBody);
	}
	
	private int getNewModifiersForCopiedMethod(MemberActionInfo info, MethodDeclaration oldMethod) throws JavaModelException {
		return info.getNewModifiersForCopyInSubclass(oldMethod.getModifiers());
	}

	private void copyThrownExceptions(MethodDeclaration oldMethod, MethodDeclaration newMethod) {
		AST ast= newMethod.getAST();
		for (int i= 0, n= oldMethod.thrownExceptions().size(); i < n; i++) {
			Name oldException= (Name)oldMethod.thrownExceptions().get(i);
			if (oldException.isSimpleName()){
				Name newException= ast.newSimpleName(((SimpleName)oldException).getIdentifier());
				newMethod.thrownExceptions().add(i, newException);
			}	else {
				Name newException= (Name)ASTNode.copySubtree(ast, oldException);
				newMethod.thrownExceptions().add(i, newException);
			}
		}
	}
	
	private void copyParameters(ASTRewrite targetRewrite, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		for (int i= 0, n= oldMethod.parameters().size(); i < n; i++) {
			SingleVariableDeclaration oldParam= (SingleVariableDeclaration)oldMethod.parameters().get(i);
			SingleVariableDeclaration newParam= createPlaceholderForSingleVariableDeclaration(oldParam, getDeclaringWorkingCopy(), targetRewrite);
			newMethod.parameters().add(i, newParam);
		}
	}
	
	private void copyReturnType(ASTRewrite targetRewrite, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		Type newReturnType= createPlaceholderForType(oldMethod.getReturnType(), getDeclaringWorkingCopy(), targetRewrite);
		newMethod.setReturnType(newReturnType);
	}

	private void createField(MemberActionInfo info, List bodyDeclarations, CompilationUnit declaringCuNode, ASTRewrite rewrite) throws JavaModelException {
		FieldDeclaration newField= createNewFieldDeclarationNode(info, declaringCuNode, rewrite);
		rewrite.markAsInserted(newField);
		bodyDeclarations.add(ASTNodes.getInsertionIndex(newField, bodyDeclarations), newField);
	}

	private FieldDeclaration createNewFieldDeclarationNode(MemberActionInfo info, CompilationUnit declaringCuNode, ASTRewrite rewrite) throws JavaModelException {
		Assert.isTrue(info.isFieldInfo());
		IField field= (IField)info.getMember();
		AST ast= getAST(rewrite);
		VariableDeclarationFragment oldFieldFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, declaringCuNode);
		VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
		newFragment.setExtraDimensions(oldFieldFragment.getExtraDimensions());
		if (oldFieldFragment.getInitializer() != null){
			Expression newInitializer= createPlaceholderForExpression(oldFieldFragment.getInitializer(), getDeclaringWorkingCopy(), rewrite);
			newFragment.setInitializer(newInitializer);
		}	
		newFragment.setName(ast.newSimpleName(oldFieldFragment.getName().getIdentifier()));
		FieldDeclaration newField= ast.newFieldDeclaration(newFragment);
		FieldDeclaration oldField= ASTNodeSearchUtil.getFieldDeclarationNode(field, declaringCuNode);
		if (info.copyJavadocToCopiesInSubclasses())
			copyJavadocNode(rewrite, field, oldField, newField);
		newField.setModifiers(getNewModifiersForCopiedField(info, oldField));
		
		Type newType= createPlaceholderForType(oldField.getType(), getDeclaringWorkingCopy(), rewrite);
		newField.setType(newType);
		return newField;
	}

	private int getNewModifiersForCopiedField(MemberActionInfo info, FieldDeclaration oldField) throws JavaModelException {
		return info.getNewModifiersForCopyInSubclass(oldField.getModifiers());
	}

	private static void copyJavadocNode(ASTRewrite rewrite, IMember member, BodyDeclaration oldDeclaration, BodyDeclaration newDeclaration) throws JavaModelException {
		Javadoc oldJavaDoc= oldDeclaration.getJavadoc();
		if (oldJavaDoc == null)
			return;
		String source= oldJavaDoc.getComment();
		String[] lines= Strings.convertIntoLines(source);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		source= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(member));
		Javadoc newJavaDoc= (Javadoc)rewrite.createPlaceholder(source, ASTRewrite.JAVADOC);
		newDeclaration.setJavadoc(newJavaDoc);
	}
	
	private static IType[] toTypeArray(IMember[] member){
		List list= Arrays.asList(member);
		return (IType[]) list.toArray(new IType[list.size()]);
	}
	
	private static IMember[] getNonAbstractMembers(IMember[] members) throws JavaModelException{
		List list= new ArrayList(); //Arrays.asList does not support removing
		list.addAll(Arrays.asList(members));
		list.removeAll(Arrays.asList(getAbstractMembers(members)));
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}
	
	private static IMember[] getAbstractMembers(IMember[] members) throws JavaModelException{
		List result= new ArrayList(members.length);
		for (int i= 0; i < members.length; i++) {
			IMember member= members[i];
			if (JdtFlags.isAbstract(member))
				result.add(member);
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private IType[] getDestinationClassesForAbstractMembers(IProgressMonitor pm) throws JavaModelException {
		return toTypeArray(getAbstractMembers(getDestinationClassesForNonAbstractMembers(pm)));
	}
	
	private IType[] getDestinationClassesForNonAbstractMembers(IProgressMonitor pm) throws JavaModelException {
		IType[] allDirectSubclasses= getAllDirectSubclassesOfDeclaringClass(pm);
		List result= new ArrayList(allDirectSubclasses.length);
		for (int i= 0; i < allDirectSubclasses.length; i++) {
			IType subclass= allDirectSubclasses[i];
			if (isModifiable(subclass))
				result.add(subclass);
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	private static boolean isModifiable(IType clazz) throws JavaModelException {
		return clazz.exists() &&
			   ! clazz.isBinary() &&
			   ! clazz.isReadOnly() &&
				clazz.getCompilationUnit() != null &&
				clazz.isStructureKnown();
	}

	private IType[] getAllDirectSubclassesOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		return getHierarchyOfDeclaringClass(pm).getSubclasses(getDeclaringClass());
	}

	public ITypeHierarchy getHierarchyOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		try{
			if (fCachedClassHierarchy != null)
				return fCachedClassHierarchy;
			fCachedClassHierarchy= getDeclaringClass().newTypeHierarchy(pm);
			return fCachedClassHierarchy;
		} finally{
			pm.done();
		}	
	}

	private MemberActionInfo[] getInfosForAbstractMembersToBeCreatedInSubclassesOfDeclaringClass() throws JavaModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBeCreatedInSubclassesOfDeclaringClass() && JdtFlags.isAbstract(info.getMember()))
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}
	
	private MemberActionInfo[] getInfosForNonAbstractMembersToBeCreatedInSubclassesOfDeclaringClass() throws JavaModelException{
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBeCreatedInSubclassesOfDeclaringClass() && ! JdtFlags.isAbstract(info.getMember()))
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}
	
	private MemberActionInfo[] getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass() throws JavaModelException {
		MemberActionInfo[] abs= getInfosForAbstractMembersToBeCreatedInSubclassesOfDeclaringClass();
		MemberActionInfo[] nonabs= getInfosForNonAbstractMembersToBeCreatedInSubclassesOfDeclaringClass();
		List result= new ArrayList(abs.length + nonabs.length);
		result.addAll(Arrays.asList(abs));
		result.addAll(Arrays.asList(nonabs));
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private void makeDeclaringClassAbstract(TypeDeclaration declaration, ASTRewrite rewrite) {
		int newModifiers= createNewModifiersForMakingDeclaringClassAbstract(declaration);
		rewrite.markAsReplaced(declaration, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
	}

	private int createNewModifiersForMakingDeclaringClassAbstract(TypeDeclaration declaration) {
		return Modifier.ABSTRACT | declaration.getModifiers();
	}

	private boolean shouldMakeDeclaringClassAbstract() throws JavaModelException {
		return ! JdtFlags.isAbstract(getDeclaringClass()) && 
				getInfosForNewMethodsToBeDeclaredAbstract().length != 0;
	}

	private void makeMethodsAbstractInDeclaringClass(CompilationUnit cuNode, ASTRewrite rewrite) throws JavaModelException {
		MemberActionInfo[] methods= getInfosForNewMethodsToBeDeclaredAbstract();
		for (int i= 0; i < methods.length; i++) {
			declareMethodAbstract(methods[i], cuNode, rewrite);
		}
	}

	private MemberActionInfo[] getInfosForNewMethodsToBeDeclaredAbstract() throws JavaModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isNewMethodToBeDeclaredAbstract())
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private void declareMethodAbstract(MemberActionInfo info, CompilationUnit cuNode, ASTRewrite rewrite) throws JavaModelException {
		Assert.isTrue(! info.isFieldInfo());
		IMethod method= (IMethod)info.getMember();
		if (JdtFlags.isAbstract(method))
			return;
		MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, cuNode);
		rewrite.markAsRemoved(declaration.getBody());
		
		int newModifiers= createModifiersForMethodMadeAbstract(info, declaration.getModifiers());
		rewrite.markAsReplaced(declaration, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
	}

	private int createModifiersForMethodMadeAbstract(MemberActionInfo info, int oldModifiers) throws JavaModelException {
		return info.getNewModifiersForOriginal(oldModifiers);
	}

	private ICompilationUnit getWorkingCopyOfCu(IMember member){
		return WorkingCopyUtil.getWorkingCopyIfExists(member.getCompilationUnit());
	}
	
	private static AST getAST(ASTRewrite rewrite){
		return rewrite.getAST();
	}

	//--
	private static String createLabel(IMember member){
		if (member instanceof IType)
			return createTypeLabel((IType)member);
		else if (member instanceof IMethod)
			return createMethodLabel((IMethod)member);
		else if (member instanceof IField)
			return createFieldLabel((IField)member);
		else if (member instanceof IInitializer)
			return RefactoringCoreMessages.getString("PushDownRefactoring.initializer"); //$NON-NLS-1$
		Assert.isTrue(false);
		return null;	
	}

	private static String createTypeLabel(IType type){
		return JavaModelUtil.getFullyQualifiedName(type);
	}

	private static String createFieldLabel(IField field){
		return field.getElementName();
	}

	private static String createMethodLabel(IMethod method){
		return JavaElementUtil.createMethodSignature(method);
	}
	
	private static String getBufferText(ASTNode node, ICompilationUnit cu) throws JavaModelException{
		return cu.getBuffer().getText(node.getStartPosition(), node.getLength());
	}
	
	//---- placeholder creators ----
	
	private static Expression createPlaceholderForExpression(Expression expression, ICompilationUnit cu, ASTRewrite rewrite) throws JavaModelException{
		return (Expression)rewrite.createPlaceholder(getBufferText(expression, cu), ASTRewrite.EXPRESSION);
	}
			
	private static SingleVariableDeclaration createPlaceholderForSingleVariableDeclaration(SingleVariableDeclaration declaration, ICompilationUnit cu, ASTRewrite rewrite) throws JavaModelException{
		return (SingleVariableDeclaration)rewrite.createPlaceholder(getBufferText(declaration, cu), ASTRewrite.SINGLEVAR_DECLARATION);
	}
	
	private static Type createPlaceholderForType(Type type, ICompilationUnit cu, ASTRewrite rewrite) throws JavaModelException{
		return (Type)rewrite.createPlaceholder(getBufferText(type, cu), ASTRewrite.TYPE);
	}

}
