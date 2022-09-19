/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
 *     Microsoft Corporation - read formatting options from the compilation unit
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.PushDownDescriptor;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.structure.MemberVisibilityAdjustor.IncomingMemberVisibilityAdjustment;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;


/**
 * Refactoring processor for the push down refactoring.
 *
 * @since 3.2
 */
public final class PushDownRefactoringProcessor extends HierarchyProcessor {

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

		public static MemberActionInfo create(IMember member, int action) {
			return new MemberActionInfo(member, action);
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
				return new int[] { PUSH_DOWN_ACTION, NO_ACTION };

			return new int[] { PUSH_DOWN_ACTION, PUSH_ABSTRACT_ACTION, NO_ACTION };
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

		@Override
		public boolean isActive() {
			return getAction() != NO_ACTION;
		}

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

	private static final String ATTRIBUTE_ABSTRACT= "abstract"; //$NON-NLS-1$

	private static final String ATTRIBUTE_PUSH= "push"; //$NON-NLS-1$

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.pushDownProcessor"; //$NON-NLS-1$

	/** The push down group category set */
	private static final GroupCategorySet SET_PUSH_DOWN= new GroupCategorySet(new GroupCategory("org.eclipse.jdt.internal.corext.pushDown", //$NON-NLS-1$
			RefactoringCoreMessages.PushDownRefactoring_category_name, RefactoringCoreMessages.PushDownRefactoring_category_description));

	private static MemberActionInfo[] createInfosForAllPushableFieldsAndMethods(IType type) throws JavaModelException {
		List<MemberActionInfo> result= new ArrayList<>();
		for (IMember pushableMember : RefactoringAvailabilityTester.getPushDownMembers(type)) {
			result.add(MemberActionInfo.create(pushableMember, MemberActionInfo.NO_ACTION));
		}
		return result.toArray(new MemberActionInfo[result.size()]);
	}

	private static IMember[] getAbstractMembers(IMember[] members) throws JavaModelException {
		List<IMember> result= new ArrayList<>(members.length);
		for (IMember member : members) {
			if (JdtFlags.isAbstract(member))
				result.add(member);
		}
		return result.toArray(new IMember[result.size()]);
	}

	private static CompilationUnitRewrite getCompilationUnitRewrite(final Map<ICompilationUnit, CompilationUnitRewrite> rewrites, final ICompilationUnit unit) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite= rewrites.get(unit);
		if (rewrite == null) {
			rewrite= new CompilationUnitRewrite(unit);
			rewrites.put(unit, rewrite);
		}
		return rewrite;
	}

	private static IJavaElement[] getReferencingElementsFromSameClass(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(member);
		SearchPattern pattern= SearchPattern.createPattern(member,
				IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		if (pattern == null) {
			return new IJavaElement[0];
		}
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(pattern);
		engine.setFiltering(true, true);
		IType declaringType= member.getDeclaringType();
		engine.setScope(SearchEngine.createJavaSearchScope(new IJavaElement[] { declaringType }));
		engine.setStatus(status);
		engine.searchPattern(new SubProgressMonitor(pm, 1));
		Set<IJavaElement> result= new HashSet<>(3);
		ICompilationUnit cu= member.getCompilationUnit();
		String source= cu.getSource();
		CompilationUnit cuRoot= null;
		for (SearchResultGroup group : (SearchResultGroup[]) engine.getResults()) {
			outer:
			for (SearchMatch searchResult : group.getSearchResults()) {
				if (source.charAt(searchResult.getOffset() - 1) == '.') {
					if (cuRoot == null) {
						cuRoot= getCompilationUnitRoot(cu, source);
					}
					if (cuRoot != null) {
						ASTNode node= NodeFinder.perform(cuRoot, searchResult.getOffset(), searchResult.getLength());
						if (node != null && node instanceof MethodInvocation) {
							MethodInvocation methodInvocation= (MethodInvocation)node;
							Expression expression= methodInvocation.getExpression();
							if (expression != null && !(expression instanceof ThisExpression)) {
								if (expression instanceof ClassInstanceCreation) {
									ClassInstanceCreation cic= (ClassInstanceCreation)expression;
									Type t= cic.getType();
									if (t.isSimpleType() && !((SimpleType)t).getName().getFullyQualifiedName().equals(declaringType.getElementName())) {
										continue;
									}
								} else if (expression instanceof SimpleName) {
									String referenceName= ((SimpleName)expression).getFullyQualifiedName();
									final TypeDeclaration thisType= ASTNodes.getFirstAncestorOrNull(methodInvocation, TypeDeclaration.class);
									if (thisType != null) {
										final String thisTypeName= thisType.getName().getFullyQualifiedName();
										for (FieldDeclaration fieldDeclaration : thisType.getFields()) {
											List<VariableDeclarationFragment> fragments= fieldDeclaration.fragments();
											for (VariableDeclarationFragment fragment : fragments) {
												SimpleName name= fragment.getName();
												if (name.getFullyQualifiedName().equals(referenceName)) {
													Type fieldType= fieldDeclaration.getType();
													if (!fieldType.isSimpleType()) {
														continue outer;
													}
													SimpleType simpleType= (SimpleType)fieldType;
													if (!simpleType.getName().getFullyQualifiedName().equals(thisTypeName)) {
														continue outer;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				result.add(SearchUtils.getEnclosingJavaElement(searchResult));
			}
		}
		return result.toArray(new IJavaElement[result.size()]);
	}

	private static CompilationUnit getCompilationUnitRoot(ICompilationUnit cu, String source) {
		Map<String, String> options = cu.getJavaProject().getOptions(true);
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(false);
		parser.setSource(source.toCharArray());
		parser.setCompilerOptions(options);
		parser.setProject(cu.getJavaProject());
		CompilationUnit selectionCURoot= (CompilationUnit) parser.createAST(null);
		return selectionCURoot;
	}

	private ITypeHierarchy fCachedClassHierarchy;

	private MemberActionInfo[] fMemberInfos;

	/**
	 * Creates a new push down refactoring processor.
	 *
	 * @param members
	 *            the members to pull up
	 */
	public PushDownRefactoringProcessor(IMember[] members) {
		super(members, null, false);
		if (members != null) {
			final IType type= RefactoringAvailabilityTester.getTopLevelType(members);
			try {
				if (type != null && RefactoringAvailabilityTester.getPushDownMembers(type).length != 0) {
					fMembersToMove= new IMember[0];
					fCachedDeclaringType= type;
				}
			} catch (JavaModelException exception) {
				JavaPlugin.log(exception);
			}
		}
	}

	/**
	 * Creates a new push down refactoring processor from refactoring arguments.
	 *
	 * @param arguments
	 *            the refactoring arguments
	 * @param status
	 *            the resulting status
	 */
	public PushDownRefactoringProcessor(JavaRefactoringArguments arguments, RefactoringStatus status) {
		super(null, null, false);
		RefactoringStatus initializeStatus= initialize(arguments);
		status.merge(initializeStatus);
	}

	private void addAllRequiredPushableMembers(List<IMember> queue, IMember member, IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_calculating_required, 2);
		IProgressMonitor sub= new SubProgressMonitor(monitor, 1);
		sub.beginTask(RefactoringCoreMessages.PushDownRefactoring_calculating_required, 2);
		IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[] { member }, new SubProgressMonitor(sub, 1));
		sub= new SubProgressMonitor(sub, 1);
		sub.beginTask(RefactoringCoreMessages.PushDownRefactoring_calculating_required, requiredMethods.length);
		for (IMethod method : requiredMethods) {
			if (!MethodChecks.isVirtual(method) && (method.getDeclaringType().equals(getDeclaringType()) && !queue.contains(method) && RefactoringAvailabilityTester.isPushDownAvailable(method)))
				queue.add(method);
		}
		sub.done();
		for (IField field : ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[] { member }, new SubProgressMonitor(monitor, 1))) {
			if (field.getDeclaringType().equals(getDeclaringType()) && !queue.contains(field) && RefactoringAvailabilityTester.isPushDownAvailable(field))
				queue.add(field);
		}
		monitor.done();
	}

	private RefactoringStatus checkAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForAbstract) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] abstractMembersToPushDown= getAbstractMembers(membersToPushDown);
		for (IType type : destinationClassesForAbstract) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(abstractMembersToPushDown, type));
		}
		return result;
	}

	private RefactoringStatus checkAccessedFields(IType[] subclasses, IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		List<IMember> pushedDownList= Arrays.asList(membersToPushDown);
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(membersToPushDown, pm);
		for (IType targetClass : subclasses) {
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (IField field : accessedFields) {
				boolean isAccessible= pushedDownList.contains(field) || canBeAccessedFrom(field, targetClass, targetSupertypes) || Flags.isEnum(field.getFlags());
				if (!isAccessible) {
					String message= Messages.format(RefactoringCoreMessages.PushDownRefactoring_field_not_accessible, new String[] { JavaElementLabels.getTextLabel(field, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(targetClass, JavaElementLabels.ALL_FULLY_QUALIFIED) });
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
		List<IMember> pushedDownList= Arrays.asList(membersToPushDown);
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(membersToPushDown, pm);
		for (IType targetClass : subclasses) {
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (IMethod method : accessedMethods) {
				boolean isAccessible= pushedDownList.contains(method) || canBeAccessedFrom(method, targetClass, targetSupertypes);
				if (!isAccessible) {
					String message= Messages.format(RefactoringCoreMessages.PushDownRefactoring_method_not_accessible, new String[] { JavaElementLabels.getTextLabel(method, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(targetClass, JavaElementLabels.ALL_FULLY_QUALIFIED) });
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
		for (IType targetClass : subclasses) {
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (IType type : accessedTypes) {
				if (!canBeAccessedFrom(type, targetClass, targetSupertypes)) {
					String message= Messages.format(RefactoringCoreMessages.PushDownRefactoring_type_not_accessible, new String[] { JavaElementLabels.getTextLabel(type, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(targetClass, JavaElementLabels.ALL_FULLY_QUALIFIED) });
					result.addError(message, JavaStatusContext.create(type));
				}
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkElementsAccessedByModifiedMembers(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask(RefactoringCoreMessages.PushDownRefactoring_check_references, 3);
		IType[] subclasses= getAbstractDestinations(new SubProgressMonitor(pm, 1));
		result.merge(checkAccessedTypes(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(subclasses, new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 5);
			clearCaches();
			ICompilationUnit unit= getDeclaringType().getCompilationUnit();
			if (fLayer)
				unit= unit.findWorkingCopy(fOwner);
			resetWorkingCopies(unit);
			final RefactoringStatus result= new RefactoringStatus();
			result.merge(checkMembersInDestinationClasses(new SubProgressMonitor(monitor, 1)));
			result.merge(checkElementsAccessedByModifiedMembers(new SubProgressMonitor(monitor, 1)));
			result.merge(checkReferencesToPushedDownMembers(new SubProgressMonitor(monitor, 1)));
			if (!JdtFlags.isAbstract(getDeclaringType()) && getAbstractDeclarationInfos().length != 0)
				result.merge(checkConstructorCalls(getDeclaringType(), new SubProgressMonitor(monitor, 1)));
			else
				monitor.worked(1);
			if (result.hasFatalError())
				return result;
			List<IMember> members= new ArrayList<>(fMemberInfos.length);
			for (MemberActionInfo memberInfo : fMemberInfos) {
				if (memberInfo.getAction() != MemberActionInfo.NO_ACTION) {
					members.add(memberInfo.getMember());
				}
			}
			fMembersToMove= members.toArray(new IMember[members.size()]);
			fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 1), result);
			if (result.hasFatalError())
				return result;

			Checks.addModifiedFilesToChecker(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), context);

			return result;
		} finally {
			monitor.done();
		}
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 1);
			RefactoringStatus status= new RefactoringStatus();
			status.merge(checkPossibleSubclasses(new SubProgressMonitor(monitor, 1)));
			if (status.hasFatalError())
				return status;
			status.merge(checkDeclaringType(new SubProgressMonitor(monitor, 1)));
			if (status.hasFatalError())
				return status;
			status.merge(checkIfMembersExist());
			if (status.hasFatalError())
				return status;
			fMemberInfos= createInfosForAllPushableFieldsAndMethods(getDeclaringType());
			List<IMember> list= Arrays.asList(fMembersToMove);
			for (MemberActionInfo info : fMemberInfos) {
				if (list.contains(info.getMember()))
					info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	private RefactoringStatus checkMembersInDestinationClasses(IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 2);
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());

		IType[] destinationClassesForNonAbstract= getAbstractDestinations(new SubProgressMonitor(monitor, 1));
		result.merge(checkNonAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForNonAbstract));
		List<IMember> list= Arrays.asList(getAbstractMembers(getAbstractDestinations(new SubProgressMonitor(monitor, 1))));

		IType[] destinationClassesForAbstract= list.toArray(new IType[list.size()]);
		result.merge(checkAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForAbstract));
		monitor.done();
		return result;
	}

	private RefactoringStatus checkNonAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForNonAbstract) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		List<IMember> list= new ArrayList<>(Arrays.asList(membersToPushDown)); // Arrays.asList does not support removing
		list.removeAll(Arrays.asList(getAbstractMembers(membersToPushDown)));
		IMember[] nonAbstractMembersToPushDown= list.toArray(new IMember[list.size()]);
		for (IType type : destinationClassesForNonAbstract) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(nonAbstractMembersToPushDown, type));
		}
		return result;
	}

	private RefactoringStatus checkPossibleSubclasses(IProgressMonitor pm) throws JavaModelException {
		IType[] modifiableSubclasses= getAbstractDestinations(pm);
		if (modifiableSubclasses.length == 0) {
			String msg= Messages.format(RefactoringCoreMessages.PushDownRefactoring_no_subclasses, new String[] { JavaElementLabels.getTextLabel(getDeclaringType(), JavaElementLabels.ALL_FULLY_QUALIFIED) });
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		return new RefactoringStatus();
	}

	private RefactoringStatus checkReferencesToPushedDownMembers(IProgressMonitor monitor) throws JavaModelException {
		List<IMember> fields= new ArrayList<>(fMemberInfos.length);
		for (MemberActionInfo info : fMemberInfos) {
			if (info.isToBePushedDown())
				fields.add(info.getMember());
		}
		IMember[] membersToPush= fields.toArray(new IMember[fields.size()]);
		RefactoringStatus result= new RefactoringStatus();
		List<IMember> movedMembers= Arrays.asList(MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass()));
		monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_check_references, membersToPush.length);
		for (IMember member : membersToPush) {
			String label= createLabel(member);
			for (IJavaElement element : getReferencingElementsFromSameClass(member, new SubProgressMonitor(monitor, 1), result)) {
				if (movedMembers.contains(element))
					continue;
				if (!(element instanceof IMember))
					continue;
				IMember referencingMember= (IMember) element;
				Object[] keys= { label, createLabel(referencingMember) };
				String msg= Messages.format(RefactoringCoreMessages.PushDownRefactoring_referenced, keys);
				result.addError(msg, JavaStatusContext.create(referencingMember));
			}
		}
		monitor.done();
		return result;
	}

	public void computeAdditionalRequiredMembersToPushDown(IProgressMonitor monitor) throws JavaModelException {
		List<IMember> list= Arrays.asList(getAdditionalRequiredMembers(monitor));
		for (MemberActionInfo info : fMemberInfos) {
			if (list.contains(info.getMember()))
				info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
		}
	}

	/**
	 * AST node visitor which performs This check and removes qualifier if needed
	 */
	public static class ThisVisitor extends ASTVisitor {

		private final IType fDeclaringType;
		private final ASTRewrite fRewrite;

		public ThisVisitor(final ASTRewrite rewrite, final IType declaringType) {
			Assert.isNotNull(rewrite);
			Assert.isNotNull(declaringType);
			fRewrite= rewrite;
			fDeclaringType= declaringType;
		}

		@Override
		public final boolean visit(ThisExpression node) {
			Name q= node.getQualifier();
			if (q == null) {
				return false;
			}
			String qName= q.getFullyQualifiedName();
			if (qName.equals(fDeclaringType.getElementName()) || qName.equals(fDeclaringType.getFullyQualifiedName())) {
				fRewrite.set(node, ThisExpression.QUALIFIER_PROPERTY, null, null);
			}
			return false;
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
			body.accept(new ThisVisitor(rewriter, fCachedDeclaringType));
			rewriter.rewriteAST(document, getDeclaringType().getCompilationUnit().getOptions(true)).apply(document, TextEdit.NONE);
			String content= document.get(position.getStartPosition(), position.getLength());
			String[] lines= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, method.getCompilationUnit(), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException | BadLocationException exception) {
			JavaPlugin.log(exception);
		}
	}

	private void copyMembers(Collection<MemberVisibilityAdjustor> adjustors, Map<IMember, IncomingMemberVisibilityAdjustment> adjustments, Map<ICompilationUnit, CompilationUnitRewrite> rewrites, RefactoringStatus status, MemberActionInfo[] infos, IType[] destinations, CompilationUnitRewrite sourceRewriter, CompilationUnitRewrite unitRewriter, IProgressMonitor monitor) throws JavaModelException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 1);
			IType type= null;
			TypeVariableMaplet[] mapping= null;
			for (IType destination : destinations) {
				type= destination;
				mapping= TypeVariableUtil.superTypeToInheritedType(getDeclaringType(), type);
				if (unitRewriter.getCu().equals(type.getCompilationUnit())) {
					IMember member= null;
					MemberVisibilityAdjustor adjustor= null;
					AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unitRewriter.getRoot());
					ImportRewriteContext context= new ContextSensitiveImportRewriteContext(declaration, unitRewriter.getImportRewrite());
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
						adjustments.remove(member);
						adjustors.add(adjustor);
						status.merge(checkProjectCompliance(getCompilationUnitRewrite(rewrites, getDeclaringType().getCompilationUnit()), type, new IMember[] {infos[offset].getMember()}));
						if (infos[offset].isFieldInfo()) {
							final VariableDeclarationFragment oldField= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) infos[offset].getMember(), sourceRewriter.getRoot());
							if (oldField != null) {
								FieldDeclaration newField= createNewFieldDeclarationNode(infos[offset], sourceRewriter.getRoot(), mapping, unitRewriter.getASTRewrite(), oldField);
								unitRewriter.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newField, BodyDeclarationRewrite.getInsertionIndex(newField, declaration.bodyDeclarations()), unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PUSH_DOWN));
								ImportRewriteUtil.addImports(unitRewriter, context, oldField.getParent(), new HashMap<Name, String>(), new HashMap<Name, String>(), false);
							}
						} else {
							final MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) infos[offset].getMember(), sourceRewriter.getRoot());
							if (oldMethod != null) {
								MethodDeclaration newMethod= createNewMethodDeclarationNode(infos[offset], mapping, unitRewriter, oldMethod);
								unitRewriter.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newMethod, BodyDeclarationRewrite.getInsertionIndex(newMethod, declaration.bodyDeclarations()), unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PUSH_DOWN));
								ImportRewriteUtil.addImports(unitRewriter, context, oldMethod, new HashMap<Name, String>(), new HashMap<Name, String>(), false);
							}
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			final Map<String, String> arguments= new HashMap<>();
			String project= null;
			final IType declaring= getDeclaringType();
			final IJavaProject javaProject= declaring.getJavaProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			int flags= JavaRefactoringDescriptor.JAR_MIGRATION | JavaRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			try {
				if (declaring.isLocal() || declaring.isAnonymous())
					flags|= JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
			} catch (JavaModelException exception) {
				JavaPlugin.log(exception);
			}
			final String description= fMembersToMove.length == 1 ? Messages.format(RefactoringCoreMessages.PushDownRefactoring_descriptor_description_short_multi, BasicElementLabels.getJavaElementName(fMembersToMove[0].getElementName())) : RefactoringCoreMessages.PushDownRefactoring_descriptor_description_short;
			final String header= fMembersToMove.length == 1 ? Messages.format(RefactoringCoreMessages.PushDownRefactoring_descriptor_description_full, new String[] { JavaElementLabels.getElementLabel(fMembersToMove[0], JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getElementLabel(declaring, JavaElementLabels.ALL_FULLY_QUALIFIED) }) : Messages.format(RefactoringCoreMessages.PushDownRefactoring_descriptor_description, new String[] { JavaElementLabels.getElementLabel(declaring, JavaElementLabels.ALL_FULLY_QUALIFIED) });
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			final String[] settings= new String[fMembersToMove.length];
			for (int index= 0; index < settings.length; index++)
				settings[index]= JavaElementLabels.getElementLabel(fMembersToMove[index], JavaElementLabels.ALL_FULLY_QUALIFIED);
			comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.PushDownRefactoring_pushed_members_pattern, settings));
			addSuperTypeSettings(comment, true);
			final PushDownDescriptor descriptor= RefactoringSignatureDescriptorFactory.createPushDownDescriptor(project, description, comment.asString(), arguments, flags);
			if (fCachedDeclaringType != null)
				arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT, JavaRefactoringDescriptorUtil.elementToHandle(project, fCachedDeclaringType));
			for (int index= 0; index < fMembersToMove.length; index++) {
				arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (index + 1), JavaRefactoringDescriptorUtil.elementToHandle(project, fMembersToMove[index]));
				for (MemberActionInfo memberInfo : fMemberInfos) {
					if (memberInfo.getMember().equals(fMembersToMove[index])) {
						switch (memberInfo.getAction()) {
							case MemberActionInfo.PUSH_ABSTRACT_ACTION:
								arguments.put(ATTRIBUTE_ABSTRACT + (index + 1), Boolean.TRUE.toString());
								break;
							case MemberActionInfo.PUSH_DOWN_ACTION:
								arguments.put(ATTRIBUTE_PUSH + (index + 1), Boolean.TRUE.toString());
								break;
						}
					}
				}
			}
			return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.PushDownRefactoring_change_name, fChangeManager.getAllChanges());
		} finally {
			pm.done();
			clearCaches();
		}
	}

	private TextEditBasedChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 7);
			final ICompilationUnit source= getDeclaringType().getCompilationUnit();
			final CompilationUnitRewrite sourceRewriter= new CompilationUnitRewrite(source);
			final Map<ICompilationUnit, CompilationUnitRewrite> rewrites= new HashMap<>(2);
			rewrites.put(source, sourceRewriter);
			IType[] types= getHierarchyOfDeclaringClass(new SubProgressMonitor(monitor, 1)).getSubclasses(getDeclaringType());
			final Set<ICompilationUnit> result= new HashSet<>(types.length + 1);
			for (IType type : types) {
				ICompilationUnit cu= type.getCompilationUnit();
				if (cu != null) { // subclasses can be in binaries
					result.add(cu);
				}
			}
			result.add(source);
			final Map<IMember, IncomingMemberVisibilityAdjustment> adjustments= new HashMap<>();
			final List<MemberVisibilityAdjustor> adjustors= new ArrayList<>();
			CompilationUnitRewrite rewrite= null;
			final IProgressMonitor sub= new SubProgressMonitor(monitor, 4);
			try {
				sub.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, result.size() * 4);
				for (ICompilationUnit unit : result) {
					rewrite= getCompilationUnitRewrite(rewrites, unit);
					if (unit.equals(sourceRewriter.getCu())) {
						final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(getDeclaringType(), rewrite.getRoot());
						if (!JdtFlags.isAbstract(getDeclaringType()) && getAbstractDeclarationInfos().length != 0)
							ModifierRewrite.create(rewrite.getASTRewrite(), declaration).setModifiers((Modifier.ABSTRACT | declaration.getModifiers()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.PushDownRefactoring_make_abstract, SET_PUSH_DOWN));
						deleteDeclarationNodes(sourceRewriter, false, rewrite, Arrays.asList(getDeletableMembers()), SET_PUSH_DOWN);
						for (MemberActionInfo method : getAbstractDeclarationInfos()) {
							declareMethodAbstract(method, sourceRewriter, rewrite);
						}
					}
					final IMember[] members= getAbstractMembers(getAbstractDestinations(new SubProgressMonitor(monitor, 1)));
					final IType[] classes= new IType[members.length];
					for (int offset= 0; offset < members.length; offset++) {
						classes[offset]= (IType) members[offset];
					}
					copyMembers(adjustors, adjustments, rewrites, status, getAbstractMemberInfos(), classes, sourceRewriter, rewrite, sub);
					copyMembers(adjustors, adjustments, rewrites, status, getAffectedMemberInfos(), getAbstractDestinations(new SubProgressMonitor(monitor, 1)), sourceRewriter, rewrite, sub);
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
				}
				removeOverrideAnnotation(rewrites);

			} finally {
				sub.done();
			}
			if (!adjustors.isEmpty() && !adjustments.isEmpty()) {
				final MemberVisibilityAdjustor adjustor= adjustors.get(0);
				adjustor.rewriteVisibility(new SubProgressMonitor(monitor, 1));
			}
			final TextEditBasedChangeManager manager= new TextEditBasedChangeManager();
			for (Map.Entry<ICompilationUnit, CompilationUnitRewrite> entry : rewrites.entrySet()) {
				ICompilationUnit unit= entry.getKey();
				rewrite= entry.getValue();
				if (rewrite != null)
					manager.manage(unit, rewrite.createChange(true));
			}
			return manager;
		} finally {
			monitor.done();
		}
	}

	/*
	 * Check if target method in subclass has '@Override' annotation, and if so removes it.
	 */
	private void removeOverrideAnnotation(Map<ICompilationUnit, CompilationUnitRewrite> rewrites) throws JavaModelException {
		IType[] subclasses= fCachedClassHierarchy.getSubclasses(fCachedDeclaringType);

		for (IMember memberActionInfo : fMembersToMove) {
			if (memberActionInfo.getElementType() != IJavaElement.METHOD) {
				continue;
			}
			IMethod method= (IMethod) memberActionInfo;
			nextClass: for (IType iType : subclasses) {
				for (IMethod iMethod : iType.getMethods()) {
					if (iMethod.isSimilar(method)) {
						CompilationUnitRewrite rewrite= getCompilationUnitRewrite(rewrites, iType.getCompilationUnit());
						MethodDeclaration methodDeclarationNode= ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, rewrite.getRoot());
						if (methodDeclarationNode == null) {
							continue;
						}
						ListRewrite listRewrite= rewrite.getASTRewrite().getListRewrite(methodDeclarationNode, MethodDeclaration.MODIFIERS2_PROPERTY);
						for (IExtendedModifier iExtendedModifier : (List<IExtendedModifier>) methodDeclarationNode.modifiers()) {
							if (iExtendedModifier.isAnnotation() &&
									"Override".equals(((Annotation) iExtendedModifier).getTypeName().getFullyQualifiedName())) { //$NON-NLS-1$
								listRewrite.remove((Annotation) iExtendedModifier, null);
								continue nextClass; // resume checking with next target class
							}
						}
					}
				}
			}
		}
	}

	private FieldDeclaration createNewFieldDeclarationNode(MemberActionInfo info, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite, VariableDeclarationFragment oldFieldFragment) throws JavaModelException {
		Assert.isTrue(info.isFieldInfo());
		IField field= (IField) info.getMember();
		AST ast= rewrite.getAST();
		VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
		copyExtraDimensions(oldFieldFragment, newFragment);
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
			copyJavadocNode(rewrite, oldField, newField);
		copyAnnotations(oldField, newField);
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

	private MethodDeclaration createNewMethodDeclarationNode(MemberActionInfo info, TypeVariableMaplet[] mapping, CompilationUnitRewrite rewriter, MethodDeclaration oldMethod) throws JavaModelException {
		Assert.isTrue(!info.isFieldInfo());
		IMethod method= (IMethod) info.getMember();
		ASTRewrite rewrite= rewriter.getASTRewrite();
		AST ast= rewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		copyBodyOfPushedDownMethod(rewrite, method, oldMethod, newMethod, mapping);
		newMethod.setConstructor(oldMethod.isConstructor());
		copyExtraDimensions(oldMethod, newMethod);
		if (info.copyJavadocToCopiesInSubclasses())
			copyJavadocNode(rewrite, oldMethod, newMethod);
		final IJavaProject project= rewriter.getCu().getJavaProject();
		if (info.isNewMethodToBeDeclaredAbstract() && JavaModelUtil.is50OrHigher(project) && JavaPreferencesSettings.getCodeGenerationSettings(rewriter.getCu()).overrideAnnotation) {
			final MarkerAnnotation annotation= ast.newMarkerAnnotation();
			annotation.setTypeName(ast.newSimpleName("Override")); //$NON-NLS-1$
			newMethod.modifiers().add(annotation);
		}
		copyAnnotations(oldMethod, newMethod);
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
		List<MemberActionInfo> result= new ArrayList<>(fMemberInfos.length);
		for (MemberActionInfo info : fMemberInfos) {
			if (info.isNewMethodToBeDeclaredAbstract())
				result.add(info);
		}
		return result.toArray(new MemberActionInfo[result.size()]);
	}

	private IType[] getAbstractDestinations(IProgressMonitor monitor) throws JavaModelException {
		IType[] allDirectSubclasses= getHierarchyOfDeclaringClass(monitor).getSubclasses(getDeclaringType());
		List<IType> result= new ArrayList<>(allDirectSubclasses.length);
		for (IType subclass : allDirectSubclasses) {
			if (subclass.exists() && !subclass.isBinary() && !subclass.isReadOnly() && subclass.getCompilationUnit() != null && subclass.isStructureKnown())
				result.add(subclass);
		}
		return result.toArray(new IType[result.size()]);
	}

	private MemberActionInfo[] getAbstractMemberInfos() throws JavaModelException {
		List<MemberActionInfo> result= new ArrayList<>(fMemberInfos.length);
		for (MemberActionInfo info : fMemberInfos) {
			if (info.isToBeCreatedInSubclassesOfDeclaringClass() && JdtFlags.isAbstract(info.getMember()))
				result.add(info);
		}
		return result.toArray(new MemberActionInfo[result.size()]);
	}

	public IMember[] getAdditionalRequiredMembers(IProgressMonitor monitor) throws JavaModelException {
		IMember[] members= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_calculating_required, members.length);// not
		// true,
		// but
		// not
		// easy
		// to
		// give
		// anything
		// better
		List<IMember> queue= new ArrayList<>(members.length);
		queue.addAll(Arrays.asList(members));
		if (queue.isEmpty())
			return new IMember[0];
		int i= 0;
		IMember current;
		do {
			current= queue.get(i);
			addAllRequiredPushableMembers(queue, current, new SubProgressMonitor(monitor, 1));
			i++;
			if (queue.size() == i)
				current= null;
		} while (current != null);
		queue.removeAll(Arrays.asList(members));// report only additional
		return queue.toArray(new IMember[queue.size()]);
	}

	private IMember[] getDeletableMembers() {
		List<IMember> result= new ArrayList<>(fMemberInfos.length);
		for (MemberActionInfo info : fMemberInfos) {
			if (info.isToBeDeletedFromDeclaringClass())
				result.add(info.getMember());
		}
		return result.toArray(new IMember[result.size()]);
	}

	private MemberActionInfo[] getAffectedMemberInfos() throws JavaModelException {
		List<MemberActionInfo> result= new ArrayList<>(fMemberInfos.length);
		for (MemberActionInfo info : fMemberInfos) {
			if (info.isToBeCreatedInSubclassesOfDeclaringClass() && !JdtFlags.isAbstract(info.getMember()))
				result.add(info);
		}
		return result.toArray(new MemberActionInfo[result.size()]);
	}

	@Override
	public Object[] getElements() {
		return fMembersToMove;
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

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	private MemberActionInfo[] getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass() throws JavaModelException {
		MemberActionInfo[] abs= getAbstractMemberInfos();
		MemberActionInfo[] nonabs= getAffectedMemberInfos();
		List<MemberActionInfo> result= new ArrayList<>(abs.length + nonabs.length);
		result.addAll(Arrays.asList(abs));
		result.addAll(Arrays.asList(nonabs));
		return result.toArray(new MemberActionInfo[result.size()]);
	}

	public MemberActionInfo[] getMemberActionInfos() {
		return fMemberInfos;
	}

	@Override
	public String getProcessorName() {
		return RefactoringCoreMessages.PushDownRefactoring_name;
	}

	private RefactoringStatus initialize(JavaRefactoringArguments extended) {
		String handle= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
		if (handle != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
			if (element == null || !element.exists() || element.getElementType() != IJavaElement.TYPE)
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getProcessorName(), IJavaRefactorings.PUSH_DOWN);
			else
				fCachedDeclaringType= (IType) element;
		}
		int count= 1;
		final List<IJavaElement> elements= new ArrayList<>();
		final List<MemberActionInfo> infos= new ArrayList<>();
		String attribute= JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + count;
		final RefactoringStatus status= new RefactoringStatus();
		while ((handle= extended.getAttribute(attribute)) != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
			if (element == null || !element.exists())
				status.merge(JavaRefactoringDescriptorUtil.createInputWarningStatus(element, getProcessorName(), IJavaRefactorings.PUSH_DOWN));
			else
				elements.add(element);
			if (extended.getAttribute(ATTRIBUTE_ABSTRACT + count) != null)
				infos.add(MemberActionInfo.create((IMember) element, MemberActionInfo.PUSH_ABSTRACT_ACTION));
			else if (extended.getAttribute(ATTRIBUTE_PUSH + count) != null)
				infos.add(MemberActionInfo.create((IMember) element, MemberActionInfo.PUSH_DOWN_ACTION));
			else
				infos.add(MemberActionInfo.create((IMember) element, MemberActionInfo.NO_ACTION));
			count++;
			attribute= JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + count;
		}
		fMembersToMove= elements.toArray(new IMember[elements.size()]);
		fMemberInfos= infos.toArray(new MemberActionInfo[infos.size()]);
		if (!status.isOK())
			return status;
		return new RefactoringStatus();
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isPushDownAvailable(fMembersToMove);
	}

	@Override
	protected void rewriteTypeOccurrences(final TextEditBasedChangeManager manager, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final ICompilationUnit unit, final CompilationUnit node, final Set<String> replacements, final IProgressMonitor monitor) throws CoreException {
		// Not needed
	}
}