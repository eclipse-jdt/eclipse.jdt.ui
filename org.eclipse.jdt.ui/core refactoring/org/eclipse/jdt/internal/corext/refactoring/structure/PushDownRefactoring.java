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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class PushDownRefactoring extends HierarchyRefactoring {
	
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
				return new int[] { PUSH_DOWN_ACTION, NO_ACTION};

			return new int[] { PUSH_DOWN_ACTION, PUSH_ABSTRACT_ACTION, NO_ACTION};
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
	
	// caches
	private ITypeHierarchy fCachedClassHierarchy;

	private PushDownRefactoring(IMember[] members, CodeGenerationSettings settings){
		super(members, settings);
	}

	public static PushDownRefactoring create(IMember[] members, CodeGenerationSettings preferenceSettings) throws JavaModelException{
		if (! isAvailable(members))
			return null;
		if (isOneTypeWithPushableMembers(members)) {
			PushDownRefactoring result= new PushDownRefactoring(new IMember[0], preferenceSettings);
			result.fCachedDeclaringType= getSingleTopLevelType(members);
			return result;
		}
		return new PushDownRefactoring(members, preferenceSettings);
	}

	public static boolean isAvailable(IMember[] members) throws JavaModelException {
		if (isOneTypeWithPushableMembers(members))
			return true;
		final IType type= getSingleTopLevelType(members);
		if (type != null && JdtFlags.isEnum(type))
			return false;
		return members != null && members.length != 0 && areAllPushable(members) && haveCommonDeclaringType(members);
	}

	private static boolean isOneTypeWithPushableMembers(IMember[] members) throws JavaModelException {
		IType singleTopLevelType= getSingleTopLevelType(members);
		return (singleTopLevelType != null && getPushableMembers(singleTopLevelType).length != 0);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("PushDownRefactoring.name"); //$NON-NLS-1$
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
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			fMembersToMove= WorkingCopyUtil.getOriginals(fMembersToMove);
			
			result.merge(checkPossibleSubclasses(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;						
			result.merge(checkDeclaringType(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;			
			result.merge(checkIfMembersExist());
			if (result.hasFatalError())
				return result;			
			
			fMemberInfos= createInfosForAllPushableFieldsAndMethods(getDeclaringType());
			List list= Arrays.asList(fMembersToMove);
			for (int i= 0; i < fMemberInfos.length; i++) {
				MemberActionInfo info= fMemberInfos[i];
				if (list.contains(info.getMember()))
					info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
			}
			return result;	
		} finally {
			pm.done();
		}
	}
	
	private RefactoringStatus checkPossibleSubclasses(IProgressMonitor pm) throws JavaModelException {
		IType[] modifiableSubclasses= getDestinationClassesForNonAbstractMembers(pm);
		if (modifiableSubclasses.length == 0){
			String msg= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.no_subclasses", new String[]{createTypeLabel(getDeclaringType())});//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
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
		if (member.getElementType() != IJavaElement.METHOD && member.getElementType() != IJavaElement.FIELD)
			return false;
		if (JdtFlags.isEnum(member))
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (JdtFlags.isStatic(member))
			return false;
		if (member.getElementType() == IJavaElement.METHOD) {
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
		List list= Arrays.asList(getAdditionalRequiredMembers(pm));
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (list.contains(info.getMember()))
				info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
		}
	}
	
	public IMember[] getAdditionalRequiredMembers(IProgressMonitor pm) throws JavaModelException {
		IMember[] members= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
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
		IProgressMonitor pm1= new SubProgressMonitor(pm, 1);
		pm1.beginTask("", 2); //$NON-NLS-1$
		IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[]{member}, new SubProgressMonitor(pm1, 1));
		SubProgressMonitor pm2= new SubProgressMonitor(pm1, 1);
		pm2.beginTask("", requiredMethods.length); //$NON-NLS-1$
		for (int i= 0; i < requiredMethods.length; i++) {
			IMethod method= requiredMethods[i];
			if (! MethodChecks.isVirtual(method) && (method.getDeclaringType().equals(getDeclaringType()) && ! queue.contains(method) && isPushable(method)))
				queue.add(method);
		}
		pm2.done();
		IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[]{member}, new SubProgressMonitor(pm, 1));
		for (int i= 0; i < requiredFields.length; i++) {
			IField field= requiredFields[i];
			if (field.getDeclaringType().equals(getDeclaringType()) && ! queue.contains(field) && isPushable(field))
				queue.add(field);
		}
		pm.done();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.creating_preview"), 5); //$NON-NLS-1$
			clearCaches();
						
			RefactoringStatus result= new RefactoringStatus();
			
			result.merge(checkMembersInDestinationClasses(new SubProgressMonitor(pm, 1)));
			result.merge(checkElementsAccessedByModifiedMembers(new SubProgressMonitor(pm, 1)));
			result.merge(checkReferencesToPushedDownMembers(new SubProgressMonitor(pm, 1)));
			if (! JdtFlags.isAbstract(getDeclaringType()) && 
			getInfosForNewMethodsToBeDeclaredAbstract().length != 0)
				result.merge(checkCallsToClassConstructors(getDeclaringType(), new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);	

			if (result.hasFatalError())
				return result;

			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} finally {
			pm.done();
		}	
	}

	private RefactoringStatus checkReferencesToPushedDownMembers(IProgressMonitor pm) throws JavaModelException {
		List fields= new ArrayList(fMemberInfos.length);
		for (int i1= 0; i1 < fMemberInfos.length; i1++) {
			MemberActionInfo info= fMemberInfos[i1];
			if (info.isToBePushedDown())
				fields.add(info.getMember());
		}
		IMember[] membersToPush= (IMember[]) fields.toArray(new IMember[fields.size()]);
		RefactoringStatus result= new RefactoringStatus();
		List movedMembers= Arrays.asList(MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass()));
		pm.beginTask("", membersToPush.length); //$NON-NLS-1$
		for (int i= 0; i < membersToPush.length; i++) {
			IMember member= membersToPush[i];
			String label= createLabel(member);
			IJavaElement[] referencing= getReferencingElementsFromSameClass(member, new SubProgressMonitor(pm, 1), result);
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

	private static IJavaElement[] getReferencingElementsFromSameClass(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		SearchPattern pattern= RefactoringSearchEngine.createOrPattern(new IJavaElement[]{member}, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[]{member.getDeclaringType()});
		SearchResultGroup[] groups= RefactoringSearchEngine.search(pattern, scope, pm, status);
		Set result= new HashSet(3);
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			SearchMatch[] results= group.getSearchResults();
			for (int j= 0; j < results.length; j++) {
				SearchMatch searchResult= results[i];
				result.add(SearchUtils.getEnclosingJavaElement(searchResult));
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
		IType[] accessedTypes= getTypesReferencedInMovedMembers(pm);
		for (int i= 0; i < subclasses.length; i++) {
			IType targetClass= subclasses[i];
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (int j= 0; j < accessedTypes.length; j++) {
				IType type= accessedTypes[j];
				if (! canBeAccessedFrom(type, targetClass, targetSupertypes)){
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
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		List pushedDownList= Arrays.asList(membersToPushDown);
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(membersToPushDown, pm);
		for (int i= 0; i < subclasses.length; i++) {	
			IType targetClass= subclasses[i];
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (int j= 0; j < accessedFields.length; j++) {
				IField field= accessedFields[j];
				boolean isAccessible= 	pushedDownList.contains(field) || 
										canBeAccessedFrom(field, targetClass, targetSupertypes);
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
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		List pushedDownList= Arrays.asList(membersToPushDown);
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(membersToPushDown, pm);
		for (int i= 0; i < subclasses.length; i++) {
			IType targetClass= subclasses[i];
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (int j= 0; j < accessedMethods.length; j++) {
				IMethod method= accessedMethods[j];
				boolean isAccessible= pushedDownList.contains(method) ||
									   canBeAccessedFrom(method, targetClass, targetSupertypes);
				if (! isAccessible){
					String message= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.method_not_accessible", new String[]{createMethodLabel(method), createTypeLabel(targetClass)}); //$NON-NLS-1$
					result.addError(message, JavaStatusContext.create(method));
				}
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkMembersInDestinationClasses(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());

		IType[] destinationClassesForNonAbstract= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
		result.merge(checkNonAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForNonAbstract));
		List list= Arrays.asList(getAbstractMembers(getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1))));
		
		IType[] destinationClassesForAbstract= (IType[]) list.toArray(new IType[list.size()]);
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
		List list= new ArrayList(); //Arrays.asList does not support removing
		list.addAll(Arrays.asList(membersToPushDown));
		list.removeAll(Arrays.asList(getAbstractMembers(membersToPushDown)));
		IMember[] nonAbstractMembersToPushDown= (IMember[]) list.toArray(new IMember[list.size()]);
		for (int i= 0; i < destinationClassesForNonAbstract.length; i++) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(nonAbstractMembersToPushDown, destinationClassesForNonAbstract[i]));
		}
		return result;
	}
	
	//--  change creation -------
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("PushDownRefactoring.change_name"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally {
			pm.done();
			clearCaches();
		}
	}

	private TextChangeManager createChangeManager(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.preview"), 8); //$NON-NLS-1$
			final CompilationUnitRewrite sourceRewriter= new CompilationUnitRewrite(getDeclaringType().getCompilationUnit());
			final Map importMap= new HashMap();
			addImportsToSubclasses(importMap, new SubProgressMonitor(monitor, 1));
			final TextChangeManager manager= new TextChangeManager();
			final MemberActionInfo[] copyAbstract= getInfosForAbstractMembersToBeCreatedInSubclassesOfDeclaringClass();
			final MemberActionInfo[] copyEffected= getInfosForNonAbstractMembersToBeCreatedInSubclassesOfDeclaringClass();
			final IType[] effectedDestinations= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(monitor, 1));
			List list= Arrays.asList(getAbstractMembers(getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(monitor, 1))));
			final IType[] abstractDestinations= (IType[]) list.toArray(new IType[list.size()]);
			IType[] subTypes= getHierarchyOfDeclaringClass(new SubProgressMonitor(monitor, 1)).getSubclasses(getDeclaringType());
			Set result= new HashSet(subTypes.length + 1);
			for (int i1= 0; i1 < subTypes.length; i1++)
				result.add(subTypes[i1].getCompilationUnit());
			result.add(getDeclaringType().getCompilationUnit());
			final ICompilationUnit[] involvedUnits= (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
			final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 4);
			subMonitor.beginTask("", involvedUnits.length); //$NON-NLS-1$
			ICompilationUnit currentUnit= null;
			CompilationUnitRewrite unitRewriter= null;
			for (int index= 0; index < involvedUnits.length; index++) {
				currentUnit= involvedUnits[index];
				if (currentUnit.equals(sourceRewriter.getCu()))
					unitRewriter= sourceRewriter;
				else
					unitRewriter= new CompilationUnitRewrite(currentUnit);
				if (currentUnit.equals(sourceRewriter.getCu())) {
					final TypeDeclaration declaration= ASTNodeSearchUtil.getTypeDeclarationNode(getDeclaringType(), unitRewriter.getRoot());
					if (! JdtFlags.isAbstract(getDeclaringType()) && 
					getInfosForNewMethodsToBeDeclaredAbstract().length != 0)
						ModifierRewrite.create(unitRewriter.getASTRewrite(), declaration).setModifiers((Modifier.ABSTRACT | declaration.getModifiers()), null);
					deleteDeclarationNodes(sourceRewriter, false, unitRewriter, Arrays.asList(getMembersToDeleteFromDeclaringClass()));
					MemberActionInfo[] methods= getInfosForNewMethodsToBeDeclaredAbstract();
					for (int i= 0; i < methods.length; i++)
						declareMethodAbstract(methods[i], sourceRewriter, unitRewriter);
				}
				final Set imports= (Set) importMap.get(currentUnit);
				if (imports != null) {
					IType type= null;
					for (final Iterator iterator= imports.iterator(); iterator.hasNext();) {
						type= (IType) iterator.next();
						final String fullyQualifiedName= type.getFullyQualifiedName('.');
						unitRewriter.getImportRewrite().addImport(fullyQualifiedName);
						unitRewriter.getImportRemover().registerAddedImport(fullyQualifiedName);
					}
				}
				copyMembers(copyAbstract, abstractDestinations, sourceRewriter, unitRewriter);
				copyMembers(copyEffected, effectedDestinations, sourceRewriter, unitRewriter);
				manager.manage(currentUnit, unitRewriter.createChange());
				subMonitor.worked(1);
				if (monitor.isCanceled())
					throw new OperationCanceledException();
			}
			return manager;
		} finally {
			monitor.done();
		}
	}

	private void copyMembers(MemberActionInfo[] infos, IType[] destinations, CompilationUnitRewrite sourceRewriter, CompilationUnitRewrite unitRewriter) throws JavaModelException {
		IType type= null;
		TypeVariableMaplet[] mapping= null;
		for (int index= 0; index < destinations.length; index++) {
			type= destinations[index];
			mapping= TypeVariableUtil.superTypeToInheritedType(getDeclaringType(), type);
			if (unitRewriter.getCu().equals(type.getCompilationUnit()))
				createAll(infos, ASTNodeSearchUtil.getTypeDeclarationNode(type, unitRewriter.getRoot()), sourceRewriter.getRoot(), mapping, unitRewriter);
		}
	}

	private void addImportsToSubclasses(Map imports, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 4); //$NON-NLS-1$
		IMember[] allMembers= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		List list1= new ArrayList(); //Arrays.asList does not support removing
		list1.addAll(Arrays.asList(allMembers));
		list1.removeAll(Arrays.asList(getAbstractMembers(allMembers)));
		IMember[] nonAbstractMembers= (IMember[]) list1.toArray(new IMember[list1.size()]);
		IType[] destForNonAbstract= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
		addImportsToTypesReferencedInMembers(imports, nonAbstractMembers, destForNonAbstract, new SubProgressMonitor(pm, 1));
		IMember[] abstractMembers= getAbstractMembers(allMembers);
		List list= Arrays.asList(getAbstractMembers(getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1))));
		IType[] destForAbstract= (IType[]) list.toArray(new IType[list.size()]);
		addImportsToTypesReferencedInMembers(imports, abstractMembers, destForAbstract, new SubProgressMonitor(pm, 1));
		pm.done();
	}

	private void addImportsToTypesReferencedInMembers(Map importMap, IMember[] members, IType[] destinations, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 1); //$NON-NLS-1$
		final IType[] types= ReferenceFinderUtil.getTypesReferencedIn(members, monitor);
		for (int index= 0; index < destinations.length; index++) {
			final ICompilationUnit unit= destinations[index].getCompilationUnit();
			for (int j= 0; j < types.length; j++) {
				Set imports= null;
				if (importMap.containsKey(unit))
					imports= (Set) importMap.get(unit);
				else {
					imports= new HashSet();
					importMap.put(unit, imports);
				}
				imports.add(types[j]);
			}
		}
		monitor.done();
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

	private void createAll(MemberActionInfo[] members, TypeDeclaration declaration, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, CompilationUnitRewrite rewriter) throws JavaModelException {
		for (int i= 0; i < members.length; i++) {
			MemberActionInfo info= members[i];
			if (info.isFieldInfo()) {
				FieldDeclaration newField= createNewFieldDeclarationNode(info, declaringCuNode, mapping, rewriter.getASTRewrite());
				rewriter.getASTRewrite().getListRewrite(declaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newField, ASTNodes.getInsertionIndex(newField, declaration.bodyDeclarations()), rewriter.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
			} else {
				MethodDeclaration newMethod= createNewMethodDeclarationNode(info, declaringCuNode, mapping, rewriter.getASTRewrite());
				rewriter.getASTRewrite().getListRewrite(declaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, declaration.bodyDeclarations()), rewriter.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
			}
		}
	}
	
	private MethodDeclaration createNewMethodDeclarationNode(MemberActionInfo info, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite) throws JavaModelException {
		Assert.isTrue(! info.isFieldInfo());
		IMethod method= (IMethod)info.getMember();
		MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode(method, declaringCuNode);
		AST ast= rewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		copyBodyOfPushedDownMethod(rewrite, method, oldMethod, newMethod, mapping);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		if (info.copyJavadocToCopiesInSubclasses())
			copyJavadocNode(rewrite, method, oldMethod, newMethod);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, info.getNewModifiersForCopyInSubclass(oldMethod.getModifiers())));
		newMethod.setName(ast.newSimpleName(oldMethod.getName().getIdentifier()));
		copyReturnType(rewrite, method.getCompilationUnit(), oldMethod, newMethod, mapping);
		copyParameters(rewrite, method.getCompilationUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		return newMethod;
	}

	private void copyBodyOfPushedDownMethod(ASTRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod, TypeVariableMaplet[] mapping) throws JavaModelException {
		Block body= oldMethod.getBody();
		if (body == null) {
			newMethod.setBody(null);
			return;
		}
		try {
			final IDocument document= new Document(method.getCompilationUnit().getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(body.getAST());
			final ITrackedNodePosition position= rewriter.track(body);
			body.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, getDeclaringType().getCompilationUnit().getJavaProject().getOptions(true)).apply(document, TextEdit.NONE);
			String content= document.get(position.getStartPosition(), position.getLength());
			String[] lines= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
	}
	
	private FieldDeclaration createNewFieldDeclarationNode(MemberActionInfo info, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite) throws JavaModelException {
		Assert.isTrue(info.isFieldInfo());
		IField field= (IField)info.getMember();
		AST ast= rewrite.getAST();
		VariableDeclarationFragment oldFieldFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, declaringCuNode);
		VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
		newFragment.setExtraDimensions(oldFieldFragment.getExtraDimensions());
		Expression initializer= oldFieldFragment.getInitializer();
		if (initializer != null) {
			Expression newInitializer= null;
			if (mapping.length > 0)
				newInitializer= createPlaceholderForExpression(initializer, field.getCompilationUnit(), mapping, rewrite);
			else
				newInitializer= createPlaceholderForExpression(initializer, field.getCompilationUnit(), rewrite);
			newFragment.setInitializer(newInitializer);
		}	
		newFragment.setName(ast.newSimpleName(oldFieldFragment.getName().getIdentifier()));
		FieldDeclaration newField= ast.newFieldDeclaration(newFragment);
		FieldDeclaration oldField= ASTNodeSearchUtil.getFieldDeclarationNode(field, declaringCuNode);
		if (info.copyJavadocToCopiesInSubclasses())
			copyJavadocNode(rewrite, field, oldField, newField);
		newField.modifiers().addAll(ASTNodeFactory.newModifiers(ast, info.getNewModifiersForCopyInSubclass(oldField.getModifiers())));
		
		Type oldType= oldField.getType();
		ICompilationUnit cu= field.getCompilationUnit();
		Type newType= null;
		if (mapping.length > 0) {
			newType= createPlaceholderForType(oldType, cu, mapping, rewrite);
		} else
			newType= createPlaceholderForType(oldType, cu, rewrite);
		newField.setType(newType);
		return newField;
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

	private IType[] getDestinationClassesForNonAbstractMembers(IProgressMonitor pm) throws JavaModelException {
		IType[] allDirectSubclasses= getHierarchyOfDeclaringClass(pm).getSubclasses(getDeclaringType());
		List result= new ArrayList(allDirectSubclasses.length);
		for (int i= 0; i < allDirectSubclasses.length; i++) {
			IType subclass= allDirectSubclasses[i];
			if (subclass.exists() && !subclass.isBinary() && !subclass.isReadOnly() && subclass.getCompilationUnit() != null && subclass.isStructureKnown())
				result.add(subclass);
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	private ITypeHierarchy getHierarchyOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		try{
			if (fCachedClassHierarchy != null)
				return fCachedClassHierarchy;
			fCachedClassHierarchy= getDeclaringType().newTypeHierarchy(pm);
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

	private MemberActionInfo[] getInfosForNewMethodsToBeDeclaredAbstract() throws JavaModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isNewMethodToBeDeclaredAbstract())
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private void declareMethodAbstract(MemberActionInfo info, CompilationUnitRewrite sourceRewrite, CompilationUnitRewrite unitRewrite) throws JavaModelException {
		Assert.isTrue(! info.isFieldInfo());
		IMethod method= (IMethod)info.getMember();
		if (JdtFlags.isAbstract(method))
			return;
		final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, sourceRewrite.getRoot());
		unitRewrite.getASTRewrite().remove(declaration.getBody(), null);
		sourceRewrite.getImportRemover().registerRemovedNode(declaration.getBody());
		ModifierRewrite.create(unitRewrite.getASTRewrite(), declaration).setModifiers(info.getNewModifiersForOriginal(declaration.getModifiers()), null);
	}
}