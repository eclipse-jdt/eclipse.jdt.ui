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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.Flags;
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
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public final class PushDownRefactoring extends HierarchyRefactoring {

	public static class MemberActionInfo implements IMemberActionInfo {

		public static final int NO_ACTION= 2;

		public static final int PUSH_ABSTRACT_ACTION= 1;

		public static final int PUSH_DOWN_ACTION= 0;

		private static void assertValidAction(IMember member, int action) {
			if (member instanceof IMethod)
				Assert.isTrue(action == PUSH_ABSTRACT_ACTION || action == NO_ACTION || action == PUSH_DOWN_ACTION);
			else if (member instanceof IField)
				Assert.isTrue(action == NO_ACTION || action == PUSH_DOWN_ACTION);
		}

		public static MemberActionInfo create(IMember member) {
			return new MemberActionInfo(member, NO_ACTION);
		}

		static IMember[] getMembers(MemberActionInfo[] infos) {
			IMember[] result= new IMember[infos.length];
			for (int i= 0; i < result.length; i++) {
				result[i]= infos[i].getMember();
			}
			return result;
		}

		private int fAction;

		private final IMember fMember;

		private MemberActionInfo(IMember member, int action) {
			assertValidAction(member, action);
			Assert.isTrue(member instanceof IField || member instanceof IMethod);
			fMember= member;
			fAction= action;
		}

		boolean copyJavadocToCopiesInSubclasses() {
			return isToBeDeletedFromDeclaringClass();
		}

		public int getAction() {
			return fAction;
		}

		public int[] getAvailableActions() {
			if (isFieldInfo())
				return new int[] { PUSH_DOWN_ACTION, NO_ACTION};

			return new int[] { PUSH_DOWN_ACTION, PUSH_ABSTRACT_ACTION, NO_ACTION};
		}

		public IMember getMember() {
			return fMember;
		}

		int getNewModifiersForCopyInSubclass(int oldModifiers) throws JavaModelException {
			if (isFieldInfo())
				return oldModifiers;
			if (isToBeDeletedFromDeclaringClass())
				return oldModifiers;
			int modifiers= oldModifiers;
			if (isNewMethodToBeDeclaredAbstract()) {
				if (!JdtFlags.isPublic(fMember))
					modifiers= Modifier.PROTECTED | JdtFlags.clearAccessModifiers(modifiers);
			}
			return modifiers;
		}

		int getNewModifiersForOriginal(int oldModifiers) throws JavaModelException {
			if (isFieldInfo())
				return oldModifiers;
			if (isToBeDeletedFromDeclaringClass())
				return oldModifiers;
			int modifiers= oldModifiers;
			if (isNewMethodToBeDeclaredAbstract()) {
				modifiers= JdtFlags.clearFlag(Modifier.FINAL | Modifier.NATIVE, oldModifiers);
				modifiers|= Modifier.ABSTRACT;

				if (!JdtFlags.isPublic(fMember))
					modifiers= Modifier.PROTECTED | JdtFlags.clearAccessModifiers(modifiers);
			}
			return modifiers;
		}

		public boolean isActive() {
			return getAction() != NO_ACTION;
		}

		// XXX incorrect to have a method like this in the model
		public boolean isEditable() {
			if (isFieldInfo())
				return false;
			if (getAction() == MemberActionInfo.NO_ACTION)
				return false;
			return true;
		}

		boolean isFieldInfo() {
			return fMember instanceof IField;
		}

		boolean isNewMethodToBeDeclaredAbstract() throws JavaModelException {
			return !isFieldInfo() && !JdtFlags.isAbstract(fMember) && fAction == PUSH_ABSTRACT_ACTION;
		}

		boolean isToBeCreatedInSubclassesOfDeclaringClass() {
			return fAction != NO_ACTION;
		}

		boolean isToBeDeletedFromDeclaringClass() {
			return isToBePushedDown();
		}

		public boolean isToBePushedDown() {
			return fAction == PUSH_DOWN_ACTION;
		}

		public void setAction(int action) {
			assertValidAction(fMember, action);
			if (isFieldInfo())
				Assert.isTrue(action != PUSH_ABSTRACT_ACTION);
			fAction= action;
		}

	}

	private static void addAllPushable(IMember[] members, List list) throws JavaModelException {
		for (int i= 0; i < members.length; i++) {
			if (isPushable(members[i]))
				list.add(members[i]);
		}
	}

	private static boolean areAllPushable(IMember[] members) throws JavaModelException {
		for (int i= 0; i < members.length; i++) {
			if (!isPushable(members[i]))
				return false;
		}
		return true;
	}

	public static PushDownRefactoring create(IMember[] members) throws JavaModelException {
		if (!isAvailable(members))
			return null;
		if (isOneTypeWithPushableMembers(members)) {
			PushDownRefactoring result= new PushDownRefactoring(new IMember[0]);
			result.fDeclaringType= getSingleTopLevelType(members);
			return result;
		}
		return new PushDownRefactoring(members);
	}

	private static MemberActionInfo[] createInfosForAllPushableFieldsAndMethods(IType type) throws JavaModelException {
		List result= new ArrayList();
		IMember[] pushableMembers= getPushableMembers(type);
		for (int i= 0; i < pushableMembers.length; i++) {
			result.add(MemberActionInfo.create(pushableMembers[i]));
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private static IMember[] getAbstractMembers(IMember[] members) throws JavaModelException {
		List result= new ArrayList(members.length);
		for (int i= 0; i < members.length; i++) {
			IMember member= members[i];
			if (JdtFlags.isAbstract(member))
				result.add(member);
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private static CompilationUnitRewrite getCompilationUnitRewrite(final Map rewrites, final ICompilationUnit unit) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) rewrites.get(unit);
		if (rewrite == null) {
			rewrite= new CompilationUnitRewrite(unit);
			rewrites.put(unit, rewrite);
		}
		return rewrite;
	}

	private static IMember[] getPushableMembers(IType type) throws JavaModelException {
		List list= new ArrayList(3);
		addAllPushable(type.getFields(), list);
		addAllPushable(type.getMethods(), list);
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	private static IJavaElement[] getReferencingElementsFromSameClass(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(member);
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
		engine.setFiltering(true, true);
		engine.setScope(SearchEngine.createJavaSearchScope(new IJavaElement[] { member.getDeclaringType()}));
		engine.setStatus(status);
		engine.searchPattern(new SubProgressMonitor(pm, 1));
		SearchResultGroup[] groups= (SearchResultGroup[]) engine.getResults();
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

	private ITypeHierarchy fCachedClassHierarchy;

	private MemberActionInfo[] fMemberInfos;

	private PushDownRefactoring(IMember[] members) {
		super(members);
	}

	private void addAllRequiredPushableMembers(List queue, IMember member, IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.calculating_required"), 2); //$NON-NLS-1$
		IProgressMonitor sub= new SubProgressMonitor(monitor, 1);
		sub.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.calculating_required"), 2); //$NON-NLS-1$
		IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[] { member}, new SubProgressMonitor(sub, 1));
		sub= new SubProgressMonitor(sub, 1);
		sub.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.calculating_required"), requiredMethods.length); //$NON-NLS-1$
		for (int index= 0; index < requiredMethods.length; index++) {
			IMethod method= requiredMethods[index];
			if (!MethodChecks.isVirtual(method) && (method.getDeclaringType().equals(getDeclaringType()) && !queue.contains(method) && isPushable(method)))
				queue.add(method);
		}
		sub.done();
		IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[] { member}, new SubProgressMonitor(monitor, 1));
		for (int index= 0; index < requiredFields.length; index++) {
			IField field= requiredFields[index];
			if (field.getDeclaringType().equals(getDeclaringType()) && !queue.contains(field) && isPushable(field))
				queue.add(field);
		}
		monitor.done();
	}

	private RefactoringStatus checkAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForAbstract) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] abstractMembersToPushDown= getAbstractMembers(membersToPushDown);
		for (int index= 0; index < destinationClassesForAbstract.length; index++) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(abstractMembersToPushDown, destinationClassesForAbstract[index]));
		}
		return result;
	}

	private RefactoringStatus checkAccessedFields(IType[] subclasses, IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		List pushedDownList= Arrays.asList(membersToPushDown);
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(membersToPushDown, pm);
		for (int i= 0; i < subclasses.length; i++) {
			IType targetClass= subclasses[i];
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (int j= 0; j < accessedFields.length; j++) {
				IField field= accessedFields[j];
				boolean isAccessible= pushedDownList.contains(field) || canBeAccessedFrom(field, targetClass, targetSupertypes) || Flags.isEnum(field.getFlags());
				if (!isAccessible) {
					String message= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.field_not_accessible", new String[] { createFieldLabel(field), createTypeLabel(targetClass)}); //$NON-NLS-1$
					result.addError(message, JavaStatusContext.create(field));
				}
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IType[] subclasses, IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		List pushedDownList= Arrays.asList(membersToPushDown);
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(membersToPushDown, pm);
		for (int index= 0; index < subclasses.length; index++) {
			IType targetClass= subclasses[index];
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (int offset= 0; offset < accessedMethods.length; offset++) {
				IMethod method= accessedMethods[offset];
				boolean isAccessible= pushedDownList.contains(method) || canBeAccessedFrom(method, targetClass, targetSupertypes);
				if (!isAccessible) {
					String message= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.method_not_accessible", new String[] { createMethodLabel(method), createTypeLabel(targetClass)}); //$NON-NLS-1$
					result.addError(message, JavaStatusContext.create(method));
				}
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedTypes(IType[] subclasses, IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypesReferencedInMovedMembers(pm);
		for (int index= 0; index < subclasses.length; index++) {
			IType targetClass= subclasses[index];
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (int offset= 0; offset < accessedTypes.length; offset++) {
				IType type= accessedTypes[offset];
				if (!canBeAccessedFrom(type, targetClass, targetSupertypes)) {
					String message= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.type_not_accessible", new String[] { createTypeLabel(type), createTypeLabel(targetClass)}); //$NON-NLS-1$
					result.addError(message, JavaStatusContext.create(type));
				}
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkElementsAccessedByModifiedMembers(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.check_references"), 3); //$NON-NLS-1$
		IType[] subclasses= getAbstractDestinations(new SubProgressMonitor(pm, 1));
		result.merge(checkAccessedTypes(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(subclasses, new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.checking"), 5); //$NON-NLS-1$
			clearCaches();
			final RefactoringStatus result= new RefactoringStatus();
			result.merge(checkMembersInDestinationClasses(new SubProgressMonitor(monitor, 1)));
			result.merge(checkElementsAccessedByModifiedMembers(new SubProgressMonitor(monitor, 1)));
			result.merge(checkReferencesToPushedDownMembers(new SubProgressMonitor(monitor, 1)));
			if (!JdtFlags.isAbstract(getDeclaringType()) && getAbstractDeclarationInfos().length != 0)
				result.merge(checkCallsToClassConstructors(getDeclaringType(), new SubProgressMonitor(monitor, 1)));
			else
				monitor.worked(1);

			if (result.hasFatalError())
				return result;

			fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 1), result);
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getValidationContext()));
			return result;
		} finally {
			monitor.done();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.checking"), 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			fMembersToMove= WorkingCopyUtil.getOriginals(fMembersToMove);

			result.merge(checkPossibleSubclasses(new SubProgressMonitor(monitor, 1)));
			if (result.hasFatalError())
				return result;
			result.merge(checkDeclaringType(new SubProgressMonitor(monitor, 1)));
			if (result.hasFatalError())
				return result;
			result.merge(checkIfMembersExist());
			if (result.hasFatalError())
				return result;

			fMemberInfos= createInfosForAllPushableFieldsAndMethods(getDeclaringType());
			List list= Arrays.asList(fMembersToMove);
			for (int offset= 0; offset < fMemberInfos.length; offset++) {
				MemberActionInfo info= fMemberInfos[offset];
				if (list.contains(info.getMember()))
					info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
			}
			return result;
		} finally {
			monitor.done();
		}
	}

	private RefactoringStatus checkMembersInDestinationClasses(IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.checking"), 2); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());

		IType[] destinationClassesForNonAbstract= getAbstractDestinations(new SubProgressMonitor(monitor, 1));
		result.merge(checkNonAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForNonAbstract));
		List list= Arrays.asList(getAbstractMembers(getAbstractDestinations(new SubProgressMonitor(monitor, 1))));

		IType[] destinationClassesForAbstract= (IType[]) list.toArray(new IType[list.size()]);
		result.merge(checkAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForAbstract));
		monitor.done();
		return result;
	}

	private RefactoringStatus checkNonAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForNonAbstract) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		List list= new ArrayList(); // Arrays.asList does not support removing
		list.addAll(Arrays.asList(membersToPushDown));
		list.removeAll(Arrays.asList(getAbstractMembers(membersToPushDown)));
		IMember[] nonAbstractMembersToPushDown= (IMember[]) list.toArray(new IMember[list.size()]);
		for (int i= 0; i < destinationClassesForNonAbstract.length; i++) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(nonAbstractMembersToPushDown, destinationClassesForNonAbstract[i]));
		}
		return result;
	}

	private RefactoringStatus checkPossibleSubclasses(IProgressMonitor pm) throws JavaModelException {
		IType[] modifiableSubclasses= getAbstractDestinations(pm);
		if (modifiableSubclasses.length == 0) {
			String msg= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.no_subclasses", new String[] { createTypeLabel(getDeclaringType())});//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		return new RefactoringStatus();
	}

	private RefactoringStatus checkReferencesToPushedDownMembers(IProgressMonitor monitor) throws JavaModelException {
		List fields= new ArrayList(fMemberInfos.length);
		for (int index= 0; index < fMemberInfos.length; index++) {
			MemberActionInfo info= fMemberInfos[index];
			if (info.isToBePushedDown())
				fields.add(info.getMember());
		}
		IMember[] membersToPush= (IMember[]) fields.toArray(new IMember[fields.size()]);
		RefactoringStatus result= new RefactoringStatus();
		List movedMembers= Arrays.asList(MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass()));
		monitor.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.check_references"), membersToPush.length); //$NON-NLS-1$
		for (int index= 0; index < membersToPush.length; index++) {
			IMember member= membersToPush[index];
			String label= createLabel(member);
			IJavaElement[] referencing= getReferencingElementsFromSameClass(member, new SubProgressMonitor(monitor, 1), result);
			for (int offset= 0; offset < referencing.length; offset++) {
				IJavaElement element= referencing[offset];
				if (movedMembers.contains(element))
					continue;
				if (!(element instanceof IMember))
					continue;
				IMember referencingMember= (IMember) element;
				Object[] keys= { label, createLabel(referencingMember)};
				String msg= RefactoringCoreMessages.getFormattedString("PushDownRefactoring.referenced", keys); //$NON-NLS-1$
				result.addError(msg, JavaStatusContext.create(referencingMember));
			}
		}
		monitor.done();
		return result;
	}

	public void computeAdditionalRequiredMembersToPushDown(IProgressMonitor monitor) throws JavaModelException {
		List list= Arrays.asList(getAdditionalRequiredMembers(monitor));
		for (int index= 0; index < fMemberInfos.length; index++) {
			MemberActionInfo info= fMemberInfos[index];
			if (list.contains(info.getMember()))
				info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
		}
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
			Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(method.getJavaProject()), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
	}

	private void copyMembers(Collection adjustors, Map adjustments, Map rewrites, RefactoringStatus status, MemberActionInfo[] infos, IType[] destinations, CompilationUnitRewrite sourceRewriter, CompilationUnitRewrite unitRewriter, IProgressMonitor monitor) throws JavaModelException {
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.checking"), 1); //$NON-NLS-1$
			IType type= null;
			TypeVariableMaplet[] mapping= null;
			for (int index= 0; index < destinations.length; index++) {
				type= destinations[index];
				mapping= TypeVariableUtil.superTypeToInheritedType(getDeclaringType(), type);
				if (unitRewriter.getCu().equals(type.getCompilationUnit())) {
					IMember member= null;
					MemberVisibilityAdjustor adjustor= null;
					AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unitRewriter.getRoot());
					for (int offset= infos.length - 1; offset >= 0; offset--) {
						member= infos[offset].getMember();
						adjustor= new MemberVisibilityAdjustor(type, member);
						if (infos[offset].isNewMethodToBeDeclaredAbstract())
							adjustor.setIncoming(false);
						adjustor.setRewrite(sourceRewriter.getASTRewrite(), sourceRewriter.getRoot());
						adjustor.setRewrites(rewrites);

						// TW: set to error if bug 78387 is fixed
						adjustor.setFailureSeverity(RefactoringStatus.WARNING);

						adjustor.setStatus(status);
						adjustor.setAdjustments(adjustments);
						adjustor.adjustVisibility(new SubProgressMonitor(monitor, 1));
						adjustors.add(adjustor);
						if (infos[offset].isFieldInfo()) {
							final VariableDeclarationFragment oldField= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) infos[offset].getMember(), sourceRewriter.getRoot());
							FieldDeclaration newField= createNewFieldDeclarationNode(infos[offset], sourceRewriter.getRoot(), mapping, unitRewriter.getASTRewrite(), oldField);
							unitRewriter.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newField, ASTNodes.getInsertionIndex(newField, declaration.bodyDeclarations()), unitRewriter.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
							ImportUpdateUtil.addImports(unitRewriter, oldField.getParent(), new HashMap(), new HashMap(), false);
						} else {
							final MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) infos[offset].getMember(), sourceRewriter.getRoot());
							MethodDeclaration newMethod= createNewMethodDeclarationNode(infos[offset], sourceRewriter.getRoot(), mapping, unitRewriter.getASTRewrite(), oldMethod);
							unitRewriter.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, declaration.bodyDeclarations()), unitRewriter.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
							ImportUpdateUtil.addImports(unitRewriter, oldMethod, new HashMap(), new HashMap(), false);
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("PushDownRefactoring.change_name"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally {
			pm.done();
			clearCaches();
		}
	}

	private TextChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.checking"), 7); //$NON-NLS-1$
			final ICompilationUnit source= getDeclaringType().getCompilationUnit();
			final CompilationUnitRewrite sourceRewriter= new CompilationUnitRewrite(source);
			final Map rewrites= new HashMap(2);
			rewrites.put(source, sourceRewriter);
			IType[] types= getHierarchyOfDeclaringClass(new SubProgressMonitor(monitor, 1)).getSubclasses(getDeclaringType());
			final Set result= new HashSet(types.length + 1);
			for (int index= 0; index < types.length; index++)
				result.add(types[index].getCompilationUnit());
			result.add(source);
			final Map adjustments= new HashMap();
			final List adjustors= new ArrayList();
			final ICompilationUnit[] units= (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
			ICompilationUnit unit= null;
			CompilationUnitRewrite rewrite= null;
			final IProgressMonitor sub= new SubProgressMonitor(monitor, 4);
			try {
				sub.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.checking"), units.length * 4); //$NON-NLS-1$
				for (int index= 0; index < units.length; index++) {
					unit= units[index];
					rewrite= getCompilationUnitRewrite(rewrites, unit);
					if (unit.equals(sourceRewriter.getCu())) {
						final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(getDeclaringType(), rewrite.getRoot());
						if (!JdtFlags.isAbstract(getDeclaringType()) && getAbstractDeclarationInfos().length != 0)
							ModifierRewrite.create(rewrite.getASTRewrite(), declaration).setModifiers((Modifier.ABSTRACT | declaration.getModifiers()), null);
						deleteDeclarationNodes(sourceRewriter, false, rewrite, Arrays.asList(getDeletableMembers()));
						MemberActionInfo[] methods= getAbstractDeclarationInfos();
						for (int offset= 0; offset < methods.length; offset++)
							declareMethodAbstract(methods[offset], sourceRewriter, rewrite);
					}
					final IMember[] members= getAbstractMembers(getAbstractDestinations(new SubProgressMonitor(monitor, 1)));
					final IType[] classes= new IType[members.length];
					for (int offset= 0; offset < members.length; offset++)
						classes[offset]= (IType) members[offset];
					copyMembers(adjustors, adjustments, rewrites, status, getAbstractMemberInfos(), classes, sourceRewriter, rewrite, sub);
					copyMembers(adjustors, adjustments, rewrites, status, getEffectedMemberInfos(), getAbstractDestinations(new SubProgressMonitor(monitor, 1)), sourceRewriter, rewrite, sub);
					if (monitor.isCanceled())
						throw new OperationCanceledException();
				}
			} finally {
				sub.done();
			}
			if (!adjustors.isEmpty() && !adjustments.isEmpty()) {
				final MemberVisibilityAdjustor adjustor= (MemberVisibilityAdjustor) adjustors.get(0);
				adjustor.rewriteVisibility(new SubProgressMonitor(monitor, 1));
			}
			final TextChangeManager manager= new TextChangeManager();
			for (final Iterator iterator= rewrites.keySet().iterator(); iterator.hasNext();) {
				unit= (ICompilationUnit) iterator.next();
				rewrite= (CompilationUnitRewrite) rewrites.get(unit);
				if (rewrite != null)
					manager.manage(unit, rewrite.createChange());
			}
			return manager;
		} finally {
			monitor.done();
		}
	}

	private FieldDeclaration createNewFieldDeclarationNode(MemberActionInfo info, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite, VariableDeclarationFragment oldFieldFragment) throws JavaModelException {
		Assert.isTrue(info.isFieldInfo());
		IField field= (IField) info.getMember();
		AST ast= rewrite.getAST();
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

	private MethodDeclaration createNewMethodDeclarationNode(MemberActionInfo info, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite, MethodDeclaration oldMethod) throws JavaModelException {
		Assert.isTrue(!info.isFieldInfo());
		IMethod method= (IMethod) info.getMember();
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
		copyTypeParameters(oldMethod, newMethod);
		return newMethod;
	}

	private void declareMethodAbstract(MemberActionInfo info, CompilationUnitRewrite sourceRewrite, CompilationUnitRewrite unitRewrite) throws JavaModelException {
		Assert.isTrue(!info.isFieldInfo());
		IMethod method= (IMethod) info.getMember();
		if (JdtFlags.isAbstract(method))
			return;
		final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, sourceRewrite.getRoot());
		unitRewrite.getASTRewrite().remove(declaration.getBody(), null);
		sourceRewrite.getImportRemover().registerRemovedNode(declaration.getBody());
		ModifierRewrite.create(unitRewrite.getASTRewrite(), declaration).setModifiers(info.getNewModifiersForOriginal(declaration.getModifiers()), null);
	}

	private MemberActionInfo[] getAbstractDeclarationInfos() throws JavaModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int index= 0; index < fMemberInfos.length; index++) {
			MemberActionInfo info= fMemberInfos[index];
			if (info.isNewMethodToBeDeclaredAbstract())
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private IType[] getAbstractDestinations(IProgressMonitor monitor) throws JavaModelException {
		IType[] allDirectSubclasses= getHierarchyOfDeclaringClass(monitor).getSubclasses(getDeclaringType());
		List result= new ArrayList(allDirectSubclasses.length);
		for (int index= 0; index < allDirectSubclasses.length; index++) {
			IType subclass= allDirectSubclasses[index];
			if (subclass.exists() && !subclass.isBinary() && !subclass.isReadOnly() && subclass.getCompilationUnit() != null && subclass.isStructureKnown())
				result.add(subclass);
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	private MemberActionInfo[] getAbstractMemberInfos() throws JavaModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int index= 0; index < fMemberInfos.length; index++) {
			MemberActionInfo info= fMemberInfos[index];
			if (info.isToBeCreatedInSubclassesOfDeclaringClass() && JdtFlags.isAbstract(info.getMember()))
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	public IMember[] getAdditionalRequiredMembers(IProgressMonitor monitor) throws JavaModelException {
		IMember[] members= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		monitor.beginTask(RefactoringCoreMessages.getString("PushDownRefactoring.calculating_required"), members.length);// not true, but not easy to give anything better //$NON-NLS-1$
		List queue= new ArrayList(members.length);
		queue.addAll(Arrays.asList(members));
		if (queue.isEmpty())
			return new IMember[0];
		int i= 0;
		IMember current;
		do {
			current= (IMember) queue.get(i);
			addAllRequiredPushableMembers(queue, current, new SubProgressMonitor(monitor, 1));
			i++;
			if (queue.size() == i)
				current= null;
		} while (current != null);
		queue.removeAll(Arrays.asList(members));// report only additional
		return (IMember[]) queue.toArray(new IMember[queue.size()]);
	}

	private IMember[] getDeletableMembers() {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBeDeletedFromDeclaringClass())
				result.add(info.getMember());
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private MemberActionInfo[] getEffectedMemberInfos() throws JavaModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBeCreatedInSubclassesOfDeclaringClass() && !JdtFlags.isAbstract(info.getMember()))
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private ITypeHierarchy getHierarchyOfDeclaringClass(IProgressMonitor monitor) throws JavaModelException {
		try {
			if (fCachedClassHierarchy != null)
				return fCachedClassHierarchy;
			fCachedClassHierarchy= getDeclaringType().newTypeHierarchy(monitor);
			return fCachedClassHierarchy;
		} finally {
			monitor.done();
		}
	}

	private MemberActionInfo[] getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass() throws JavaModelException {
		MemberActionInfo[] abs= getAbstractMemberInfos();
		MemberActionInfo[] nonabs= getEffectedMemberInfos();
		List result= new ArrayList(abs.length + nonabs.length);
		result.addAll(Arrays.asList(abs));
		result.addAll(Arrays.asList(nonabs));
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	public MemberActionInfo[] getMemberActionInfos() {
		return fMemberInfos;
	}

	public String getName() {
		return RefactoringCoreMessages.getString("PushDownRefactoring.name"); //$NON-NLS-1$
	}
}