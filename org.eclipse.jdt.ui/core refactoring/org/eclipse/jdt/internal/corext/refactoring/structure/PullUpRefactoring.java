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
import java.util.Collections;
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
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public final class PullUpRefactoring extends HierarchyRefactoring {

	/**
	 * AST node visitor which performs the actual mapping.
	 */
	public static class PullUpAstNodeMapper extends TypeVariableMapper {

		/** Are we in an anonymous class declaration? */
		protected boolean fAnonymousClassDeclaration= false;

		/** The qualified type name where the super references are referring to */
		protected final String fQualifiedName;

		/** The source compilation unit rewrite to use */
		protected final CompilationUnitRewrite fSourceRewriter;

		/** The target compilation unit rewrite to use */
		protected final CompilationUnitRewrite fTargetRewriter;

		/** Are we in a type declaration statement? */
		protected boolean fTypeDeclarationStatement= false;

		/**
		 * Creates a new pull up ast node mapper.
		 * 
		 * @param sourceRewriter the source compilation unit rewrite to use
		 * @param targetRewriter the target compilation unit rewrite to use
		 * @param rewrite the AST rewrite to use
		 * @param type the super reference type
		 * @param mapping the type variable mapping
		 */
		public PullUpAstNodeMapper(final CompilationUnitRewrite sourceRewriter, final CompilationUnitRewrite targetRewriter, final ASTRewrite rewrite, final IType type, final TypeVariableMaplet[] mapping) {
			super(rewrite, mapping);
			Assert.isNotNull(rewrite);
			Assert.isNotNull(type);
			fSourceRewriter= sourceRewriter;
			fTargetRewriter= targetRewriter;
			fQualifiedName= JavaModelUtil.getFullyQualifiedName(type);
		}

		public final void endVisit(final AnonymousClassDeclaration node) {
			fAnonymousClassDeclaration= false;
			super.endVisit(node);
		}

		public final void endVisit(final TypeDeclarationStatement node) {
			fTypeDeclarationStatement= false;
			super.endVisit(node);
		}

		public final boolean visit(final AnonymousClassDeclaration node) {
			fAnonymousClassDeclaration= true;
			return super.visit(node);
		}

		public final boolean visit(final SuperFieldAccess node) {
			if (!fAnonymousClassDeclaration && !fTypeDeclarationStatement) {
				final AST ast= node.getAST();
				final FieldAccess access= ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName(node.getName().getIdentifier()));
				fRewrite.replace(node, access, null);
				if (!fSourceRewriter.getCu().equals(fTargetRewriter.getCu()))
					fSourceRewriter.getImportRemover().registerRemovedNode(node);
				return true;
			}
			return false;
		}

		public final boolean visit(final SuperMethodInvocation node) {
			if (!fAnonymousClassDeclaration && !fTypeDeclarationStatement) {
				final IBinding name= node.getName().resolveBinding();
				if (name != null && name.getKind() == IBinding.METHOD) {
					final ITypeBinding type= ((IMethodBinding) name).getDeclaringClass();

					// TW replace by comparison type.getJavaElement().equals(super_reference_type) (see 78087)

					if (type != null && !fQualifiedName.equals(Bindings.getFullyQualifiedName(type)))
						return true;
				}
				final AST ast= node.getAST();
				final ThisExpression expression= ast.newThisExpression();
				final MethodInvocation invocation= ast.newMethodInvocation();
				final SimpleName simple= ast.newSimpleName(node.getName().getIdentifier());
				invocation.setName(simple);
				invocation.setExpression(expression);
				final List arguments= (List) node.getStructuralProperty(SuperMethodInvocation.ARGUMENTS_PROPERTY);
				if (arguments != null && arguments.size() > 0) {
					final ListRewrite rewriter= fRewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
					rewriter.insertLast(rewriter.createCopyTarget((ASTNode) arguments.get(0), (ASTNode) arguments.get(arguments.size() - 1)), null);
				}
				fRewrite.replace(node, invocation, null);
				if (!fSourceRewriter.getCu().equals(fTargetRewriter.getCu()))
					fSourceRewriter.getImportRemover().registerRemovedNode(node);
				return true;
			}
			return false;
		}

		public final boolean visit(final TypeDeclarationStatement node) {
			fTypeDeclarationStatement= true;
			return super.visit(node);
		}
	}

	private static void addToMapping(Map mapping, IMember key, IMember matchingMember) {
		Set matchingSet;
		if (mapping.containsKey(key)) {
			matchingSet= (Set) mapping.get(key);
		} else {
			matchingSet= new HashSet();
			mapping.put(key, matchingSet);
		}
		Assert.isTrue(!matchingSet.contains(matchingMember));
		matchingSet.add(matchingMember);
	}

	private static boolean areAllPullable(IMember[] members) throws JavaModelException {
		for (int i= 0; i < members.length; i++) {
			if (!isPullable(members[i]))
				return false;
		}
		return true;
	}

	public static PullUpRefactoring create(IMember[] members, CodeGenerationSettings preferenceSettings) throws JavaModelException {
		if (!isAvailable(members))
			return null;
		if (isOneTypeWithPullableMembers(members)) {
			PullUpRefactoring result= new PullUpRefactoring(new IMember[0], preferenceSettings);
			result.fDeclaringType= getSingleTopLevelType(members);
			return result;
		}
		return new PullUpRefactoring(members, preferenceSettings);
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

	private static IMember[] getMembersOfType(IMember[] members, int type) {
		List list= Arrays.asList(JavaElementUtil.getElementsOfType(members, type));
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	private static Block getMethodStubBody(MethodDeclaration method, AST ast) {
		Block body= ast.newBlock();
		Expression expression= ASTNodeFactory.newDefaultExpression(ast, method.getReturnType2(), method.getExtraDimensions());
		if (expression != null) {
			ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(expression);
			body.statements().add(returnStatement);
		}
		return body;
	}

	private static Set getNonAbstractSubclasses(ITypeHierarchy hierarchy, IType type) throws JavaModelException {
		IType[] subclasses= hierarchy.getSubclasses(type);
		Set result= new HashSet();
		for (int i= 0; i < subclasses.length; i++) {
			IType subclass= subclasses[i];
			if (JdtFlags.isAbstract(subclass))
				result.addAll(getNonAbstractSubclasses(hierarchy, subclass));
			else
				result.add(subclass);
		}
		return result;
	}

	private static IMethod[] getOriginals(IMethod[] methods) {
		IMethod[] result= new IMethod[methods.length];
		for (int i= 0; i < methods.length; i++) {
			result[i]= (IMethod) WorkingCopyUtil.getOriginal(methods[i]);
		}
		return result;
	}

	private static IMember[] getPullableMembers(IType type) throws JavaModelException {
		List list= new ArrayList(3);
		IMember[] members= type.getFields();
		for (int i= 0; i < members.length; i++) {
			if (isPullable(members[i]))
				list.add(members[i]);
		}
		IMember[] members1= type.getMethods();
		for (int i= 0; i < members1.length; i++) {
			if (isPullable(members1[i]))
				list.add(members1[i]);
		}
		IMember[] members2= type.getTypes();
		for (int i= 0; i < members2.length; i++) {
			if (isPullable(members2[i]))
				list.add(members2[i]);
		}
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	public static boolean isAvailable(IMember[] members) throws JavaModelException {
		if (isOneTypeWithPullableMembers(members))
			return true;
		return members != null && members.length != 0 && areAllPullable(members) && haveCommonDeclaringType(members);
	}

	private static boolean isOneTypeWithPullableMembers(IMember[] members) throws JavaModelException {
		IType singleTopLevelType= getSingleTopLevelType(members);
		return (singleTopLevelType != null && getPullableMembers(singleTopLevelType).length != 0);
	}

	private static boolean isPullable(IMember member) throws JavaModelException {
		if (member.getElementType() != IJavaElement.METHOD && member.getElementType() != IJavaElement.FIELD && member.getElementType() != IJavaElement.TYPE)
			return false;
		if (JdtFlags.isEnum(member) && member.getElementType() != IJavaElement.TYPE)
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (member instanceof IType) {
			if (!JdtFlags.isStatic(member) && !JdtFlags.isEnum(member) && !JdtFlags.isAnnotation(member))
				return false;
		}
		if (member instanceof IMethod) {
			IMethod method= (IMethod) member;
			if (method.isConstructor())
				return false;

			if (JdtFlags.isNative(method)) // for now - move to input preconditions
				return false;
		}
		return true;
	}

	private static void mergeSetsForCommonKeys(Map result, Map map) {
		for (Iterator iter= result.keySet().iterator(); iter.hasNext();) {
			IMember key= (IMember) iter.next();
			if (map.containsKey(key)) {
				Set resultSet= (Set) result.get(key);
				Set mapSet= (Set) map.get(key);
				resultSet.addAll(mapSet);
			}
		}
	}

	private static void putAllThatDoNotExistInResultYet(Map result, Map map) {
		for (Iterator iter= map.keySet().iterator(); iter.hasNext();) {
			IMember key= (IMember) iter.next();
			if (!result.containsKey(key)) {
				Set mapSet= (Set) map.get(key);
				Set resultSet= new HashSet(mapSet);
				result.put(key, resultSet);
			}
		}
	}

	private Set fCachedSkippedSuperclasses; // Set<IType>

	private ITypeHierarchy fCachedTargetClassHierarchy;

	private final CodeGenerationSettings fCodeGenerationSettings;

	private boolean fCreateMethodStubs;

	private IMethod[] fMethodsToDeclareAbstract;

	private IMethod[] fMethodsToDelete;

	private IType fTargetType;

	private PullUpRefactoring(IMember[] elements, CodeGenerationSettings settings) {
		super(elements);
		fCodeGenerationSettings= settings;
		fMethodsToDelete= new IMethod[0];
		fMethodsToDeclareAbstract= new IMethod[0];
		fCreateMethodStubs= true;
	}

	private void addAllRequiredPullableMembers(List queue, IMember member, IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(queue);
		Assert.isNotNull(member);
		Assert.isNotNull(monitor);
		SubProgressMonitor sub= null;
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.calculating_required"), 3); //$NON-NLS-1$
			IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[] { member}, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			boolean isStatic= false;
			try {
				sub.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.calculating_required"), requiredMethods.length); //$NON-NLS-1$
				isStatic= JdtFlags.isStatic(member);
				for (int index= 0; index < requiredMethods.length; index++) {
					IMethod requiredMethod= requiredMethods[index];
					if (isStatic && !JdtFlags.isStatic(requiredMethod))
						continue;
					if (isRequiredPullableMember(queue, requiredMethod) && !isVirtualAccessibleFromTargetClass(requiredMethod, new SubProgressMonitor(sub, 1)))
						queue.add(requiredMethod);
				}
			} finally {
				sub.done();
			}
			IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[] { member}, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			try {
				sub.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.calculating_required"), requiredFields.length); //$NON-NLS-1$
				isStatic= JdtFlags.isStatic(member);
				for (int index= 0; index < requiredFields.length; index++) {
					IField requiredField= requiredFields[index];
					if (isStatic && !JdtFlags.isStatic(requiredField))
						continue;
					if (isRequiredPullableMember(queue, requiredField))
						queue.add(requiredField);
				}
			} finally {
				sub.done();
			}
			IType[] requiredTypes= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[] { member}, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			try {
				sub.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.calculating_required"), requiredMethods.length); //$NON-NLS-1$
				isStatic= JdtFlags.isStatic(member);
				for (int index= 0; index < requiredTypes.length; index++) {
					IType requiredType= requiredTypes[index];
					if (isStatic && !JdtFlags.isStatic(requiredType))
						continue;
					if (isRequiredPullableMember(queue, requiredType))
						queue.add(requiredType);
				}
			} finally {
				sub.done();
			}
		} finally {
			monitor.done();
		}
	}

	private void addMethodStubForAbstractMethod(IMethod sourceMethod, CompilationUnit declaringCuNode, AbstractTypeDeclaration typeToCreateStubIn, ICompilationUnit newCu, CompilationUnitRewrite rewriter, Map adjustments, IProgressMonitor monitor, RefactoringStatus status) throws CoreException {
		MethodDeclaration methodToCreateStubFor= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		final AST ast= rewriter.getRoot().getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(getMethodStubBody(methodToCreateStubFor, ast));
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(methodToCreateStubFor.getExtraDimensions());
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getModifiersWithUpdatedVisibility(sourceMethod, JdtFlags.clearFlag(Modifier.NATIVE | Modifier.ABSTRACT, methodToCreateStubFor.getModifiers()), adjustments, new SubProgressMonitor(monitor, 1), false, status)));
		newMethod.setName(((SimpleName) ASTNode.copySubtree(ast, methodToCreateStubFor.getName())));
		TypeVariableMaplet[] mapping= TypeVariableUtil.composeMappings(TypeVariableUtil.subTypeToSuperType(getDeclaringType(), getTargetClass()), TypeVariableUtil.superTypeToInheritedType(getTargetClass(), Bindings.findType(typeToCreateStubIn.resolveBinding(), newCu.getJavaProject())));
		copyReturnType(rewriter.getASTRewrite(), getDeclaringType().getCompilationUnit(), methodToCreateStubFor, newMethod, mapping);
		copyParameters(rewriter.getASTRewrite(), getDeclaringType().getCompilationUnit(), methodToCreateStubFor, newMethod, mapping);
		copyThrownExceptions(methodToCreateStubFor, newMethod);
		newMethod.setJavadoc(createJavadocForStub(typeToCreateStubIn.getName().getIdentifier(), methodToCreateStubFor, newMethod, newCu, rewriter.getASTRewrite()));
		ImportUpdateUtil.addImports(rewriter, newMethod, new HashMap(), new HashMap(), false);
		rewriter.getASTRewrite().getListRewrite(typeToCreateStubIn, typeToCreateStubIn.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, typeToCreateStubIn.bodyDeclarations()), rewriter.createGroupDescription(RefactoringCoreMessages.getString("PullUpRefactoring.add_method_stub"))); //$NON-NLS-1$
	}

	private void addMethodStubsToNonAbstractSubclassesOfTargetClass(List concreteSubclasses, CompilationUnit declaringCuNode, CompilationUnitRewrite unitRewriter, Map adjustments, IProgressMonitor monitor, RefactoringStatus status) throws CoreException {
		IType declaringType= getDeclaringType();
		IMethod[] methods= getAbstractMethods();
		monitor.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), concreteSubclasses.size()); //$NON-NLS-1$
		for (Iterator iter= concreteSubclasses.iterator(); iter.hasNext();) {
			IType clazz= (IType) iter.next();
			if (clazz.equals(declaringType))
				continue;
			AbstractTypeDeclaration classToCreateStubIn= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(clazz, unitRewriter.getRoot());
			ICompilationUnit cuToCreateStubIn= clazz.getCompilationUnit();
			IProgressMonitor sub= new SubProgressMonitor(monitor, 1);
			sub.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), methods.length); //$NON-NLS-1$
			for (int j= 0; j < methods.length; j++) {
				IMethod method= methods[j];
				if (null == JavaModelUtil.findMethod(method.getElementName(), method.getParameterTypes(), method.isConstructor(), clazz)) {
					addMethodStubForAbstractMethod(method, declaringCuNode, classToCreateStubIn, cuToCreateStubIn, unitRewriter, adjustments, new SubProgressMonitor(sub, 1), status);
				}
			}
			sub.done();
		}
		monitor.done();
	}

	protected boolean canBeAccessedFrom(IMember member, IType target, ITypeHierarchy hierarchy) throws JavaModelException {
		if (super.canBeAccessedFrom(member, target, hierarchy)) {
			if (target.equals(member.getDeclaringType()))
				return true;
			if (target.equals(member))
				return true;
			if (member instanceof IMethod) {
				final IMethod method= (IMethod) member;
				final IMethod stub= target.getMethod(method.getElementName(), method.getParameterTypes());
				if (stub.exists())
					return true;
			}
			if (member.getDeclaringType() == null) {
				if (!(member instanceof IType))
					return false;
				if (JdtFlags.isPublic(member))
					return true;
				if (!JdtFlags.isPackageVisible(member))
					return false;
				if (JavaModelUtil.isSamePackage(((IType) member).getPackageFragment(), target.getPackageFragment()))
					return true;
				return hierarchy.contains(member.getDeclaringType());
			}
			final IType declaringType= member.getDeclaringType();
			if (!canBeAccessedFrom(declaringType, target, hierarchy))
				return false;
			if (declaringType.equals(getDeclaringType()))
				return false;
			return true;
		}
		return false;
	}

	private RefactoringStatus checkAccessedFields(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking_referenced_elements"), 2); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();

		List pulledUpList= Arrays.asList(fMembersToMove);
		List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1)));
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fMembersToMove, new SubProgressMonitor(pm, 1));

		IType targetClass= getTargetClass();
		ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
		for (int i= 0; i < accessedFields.length; i++) {
			IField field= accessedFields[i];
			if (!field.exists())
				continue;

			boolean isAccessible= pulledUpList.contains(field) || deletedList.contains(field) || canBeAccessedFrom(field, targetClass, targetSupertypes) || Flags.isEnum(field.getFlags());
			if (!isAccessible) {
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.field_not_accessible", new String[] { createFieldLabel(field), createTypeLabel(targetClass)}); //$NON-NLS-1$
				result.addError(message, JavaStatusContext.create(field));
			} else if (getSkippedSuperclasses(new SubProgressMonitor(pm, 1)).contains(field.getDeclaringType())) {
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.field_cannot_be_accessed", new String[] { createFieldLabel(field), createTypeLabel(targetClass)}); //$NON-NLS-1$
				result.addError(message, JavaStatusContext.create(field));
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking_referenced_elements"), 2); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();

		List pulledUpList= Arrays.asList(fMembersToMove);
		List declaredAbstractList= Arrays.asList(fMethodsToDeclareAbstract);
		List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1)));
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fMembersToMove, new SubProgressMonitor(pm, 1));

		IType targetClass= getTargetClass();
		ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
		for (int i= 0; i < accessedMethods.length; i++) {
			IMethod method= accessedMethods[i];
			if (!method.exists())
				continue;
			boolean isAccessible= pulledUpList.contains(method) || deletedList.contains(method) || declaredAbstractList.contains(method) || canBeAccessedFrom(method, targetClass, targetSupertypes);
			if (!isAccessible) {
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.method_not_accessible", //$NON-NLS-1$
						new String[] { createMethodLabel(method), createTypeLabel(targetClass)});
				result.addError(message, JavaStatusContext.create(method));
			} else if (getSkippedSuperclasses(new SubProgressMonitor(pm, 1)).contains(method.getDeclaringType())) {
				String[] keys= { createMethodLabel(method), createTypeLabel(targetClass)};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.method_cannot_be_accessed", keys); //$NON-NLS-1$
				result.addError(message, JavaStatusContext.create(method));
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedTypes(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypesReferencedInMovedMembers(pm);
		IType targetClass= getTargetClass();
		ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
		List pulledUpList= Arrays.asList(fMembersToMove);
		for (int i= 0; i < accessedTypes.length; i++) {
			IType iType= accessedTypes[i];
			if (!iType.exists())
				continue;

			if (!canBeAccessedFrom(iType, targetClass, targetSupertypes) && !pulledUpList.contains(iType)) {
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.type_not_accessible", //$NON-NLS-1$
						new String[] { createTypeLabel(iType), createTypeLabel(targetClass)});
				result.addError(message, JavaStatusContext.create(iType));
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccesses(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking_referenced_elements"), 3); //$NON-NLS-1$
		result.merge(checkAccessedTypes(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}

	private void checkAccessModifiers(RefactoringStatus result, Set notDeletedMembersInSubtypes) throws JavaModelException {
		List toDeclareAbstract= Arrays.asList(fMethodsToDeclareAbstract);
		for (Iterator iter= notDeletedMembersInSubtypes.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			if (member.getElementType() == IJavaElement.METHOD && !toDeclareAbstract.contains(member)) {
				IMethod method= ((IMethod) member);
				if (method.getDeclaringType().getPackageFragment().equals(fTargetType.getPackageFragment())) {
					if (JdtFlags.isPrivate(method))
						result.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.lower_default_visibility", new String[] { createMethodLabel(method), createTypeLabel(method.getDeclaringType())}), JavaStatusContext.create(method)); //$NON-NLS-1$
				} else if (!JdtFlags.isPublic(method) && !JdtFlags.isProtected(method))
					result.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.lower_protected_visibility", new String[] { createMethodLabel(method), createTypeLabel(method.getDeclaringType())}), JavaStatusContext.create(method)); //$NON-NLS-1$
			}
		}
	}

	protected RefactoringStatus checkDeclaringType(IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus status= super.checkDeclaringType(monitor);
		if (JavaModelUtil.getFullyQualifiedName(getDeclaringType()).equals("java.lang.Object")) //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_java.lang.Object"))); //$NON-NLS-1$
		status.merge(checkSuperclassesOfDeclaringClass(monitor));
		return status;
	}

	private void checkFieldTypes(IProgressMonitor pm, RefactoringStatus result) throws JavaModelException {
		Map mapping= getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass(), true);
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (fMembersToMove[i].getElementType() != IJavaElement.FIELD)
				continue;
			IField field= (IField) fMembersToMove[i];
			String type= getTypeName(field);
			Assert.isTrue(mapping.containsKey(field));
			for (Iterator iter= ((Set) mapping.get(field)).iterator(); iter.hasNext();) {
				IField matchingField= (IField) iter.next();
				if (field.equals(matchingField))
					continue;
				if (type.equals(getTypeName(matchingField)))
					continue;
				String[] keys= { createFieldLabel(matchingField), createTypeLabel(matchingField.getDeclaringType())};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.different_field_type", //$NON-NLS-1$
						keys);
				RefactoringStatusContext context= JavaStatusContext.create(matchingField.getCompilationUnit(), matchingField.getSourceRange());
				result.addError(message, context);
			}
		}
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), 7); //$NON-NLS-1$
			clearCaches();

			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkGenericDeclaringType(new SubProgressMonitor(pm, 1)));
			result.merge(checkFinalFields(new SubProgressMonitor(pm, 1)));
			result.merge(checkAccesses(new SubProgressMonitor(pm, 1)));
			result.merge(checkMembersInTypeAndAllSubtypes(new SubProgressMonitor(pm, 2)));
			result.merge(checkIfSkippingOverElements(new SubProgressMonitor(pm, 1)));
			if (pm.isCanceled())
				throw new OperationCanceledException();
			if (!JdtFlags.isAbstract(getTargetClass()) && getAbstractMethods().length > 0)
				result.merge(checkCallsToClassConstructors(getTargetClass(), new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			if (result.hasFatalError())
				return result;

			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1), result);
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getValidationContext()));
			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkFinalFields(IProgressMonitor monitor) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		monitor.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), fMembersToMove.length); //$NON-NLS-1$
		for (int index= 0; index < fMembersToMove.length; index++) {
			IMember member= fMembersToMove[index];
			if (member.getElementType() == IJavaElement.FIELD && !JdtFlags.isStatic(member) && JdtFlags.isFinal(member)) {
				RefactoringStatusContext context= JavaStatusContext.create(member);
				result.addWarning(RefactoringCoreMessages.getString("PullUpRefactoring.final_fields"), context); //$NON-NLS-1$
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
		}
		monitor.done();
		return result;
	}

	private RefactoringStatus checkGenericDeclaringType(final SubProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(monitor);

		RefactoringStatus status= new RefactoringStatus();
		try {
			final IMember[] pullables= getMembersToMove();
			monitor.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), pullables.length); //$NON-NLS-1$

			final IType declaring= getDeclaringType();
			final ITypeParameter[] parameters= declaring.getTypeParameters();
			if (parameters.length > 0) {
				final TypeVariableMaplet[] mapping= TypeVariableUtil.subTypeToInheritedType(declaring);
				IMember member= null;
				int length= 0;
				for (int index= 0; index < pullables.length; index++) {
					member= pullables[index];
					final String[] unmapped= TypeVariableUtil.getUnmappedVariables(mapping, declaring, member);
					length= unmapped.length;
					switch (length) {
						case 0:
							break;
						case 1:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variable_not_available", new String[] { unmapped[0], declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
							break;
						case 2:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variable2_not_available", new String[] { unmapped[0], unmapped[1], declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
							break;
						case 3:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variable3_not_available", new String[] { unmapped[0], unmapped[1], unmapped[2], declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
							break;
						default:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variables_not_available", new String[] { declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
					}
					monitor.worked(1);
					if (monitor.isCanceled())
						throw new OperationCanceledException();
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	private RefactoringStatus checkIfDeclaredIn(IMember element, IType type) throws JavaModelException {
		if (element instanceof IMethod)
			return checkIfMethodDeclaredIn((IMethod) element, type);
		else if (element instanceof IField)
			return checkIfFieldDeclaredIn((IField) element, type);
		else if (element instanceof IType)
			return checkIfTypeDeclaredIn((IType) element, type);
		Assert.isTrue(false);
		return null;
	}

	private RefactoringStatus checkIfFieldDeclaredIn(IField iField, IType type) {
		IField fieldInType= type.getField(iField.getElementName());
		if (!fieldInType.exists())
			return null;
		String[] keys= { createFieldLabel(fieldInType), createTypeLabel(type)};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Field_declared_in_class", keys); //$NON-NLS-1$
		RefactoringStatusContext context= JavaStatusContext.create(fieldInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfMethodDeclaredIn(IMethod iMethod, IType type) throws JavaModelException {
		IMethod methodInType= JavaModelUtil.findMethod(iMethod.getElementName(), iMethod.getParameterTypes(), iMethod.isConstructor(), type);
		if (methodInType == null || !methodInType.exists())
			return null;
		String[] keys= { createMethodLabel(methodInType), createTypeLabel(type)};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Method_declared_in_class", keys); //$NON-NLS-1$
		RefactoringStatusContext context= JavaStatusContext.create(methodInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfSkippingOverElements(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), 1); //$NON-NLS-1$
		try {
			Set skippedSuperclassSet= getSkippedSuperclasses(new SubProgressMonitor(pm, 1));
			IType[] skippedSuperclasses= (IType[]) skippedSuperclassSet.toArray(new IType[skippedSuperclassSet.size()]);
			RefactoringStatus result= new RefactoringStatus();
			for (int i= 0; i < fMembersToMove.length; i++) {
				IMember element= fMembersToMove[i];
				for (int j= 0; j < skippedSuperclasses.length; j++) {
					result.merge(checkIfDeclaredIn(element, skippedSuperclasses[j]));
				}
			}
			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkIfTypeDeclaredIn(IType iType, IType type) {
		IType typeInType= type.getType(iType.getElementName());
		if (!typeInType.exists())
			return null;
		String[] keys= { createTypeLabel(typeInType), createTypeLabel(type)};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_declared_in_class", keys); //$NON-NLS-1$
		RefactoringStatusContext context= JavaStatusContext.create(typeInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();

			fMembersToMove= WorkingCopyUtil.getOriginals(fMembersToMove);

			result.merge(checkDeclaringType(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;
			result.merge(checkIfMembersExist());
			if (result.hasFatalError())
				return result;
			return result;
		} finally {
			pm.done();
		}
	}

	private void checkMembersInDestinationType(RefactoringStatus result, Set notDeletedMembersInTargetType) throws JavaModelException {
		IMember[] membersToBeCreatedInTargetClass= getMembersToBeCreatedInTargetClass();
		List newMembersInDestinationType= new ArrayList(membersToBeCreatedInTargetClass.length);
		newMembersInDestinationType.addAll(Arrays.asList(membersToBeCreatedInTargetClass));
		newMembersInDestinationType.addAll(notDeletedMembersInTargetType);
		newMembersInDestinationType.removeAll(Arrays.asList(fMethodsToDelete));
		IMember[] members= (IMember[]) newMembersInDestinationType.toArray(new IMember[newMembersInDestinationType.size()]);
		result.merge(MemberCheckUtil.checkMembersInDestinationType(members, getTargetClass()));
	}

	private RefactoringStatus checkMembersInTypeAndAllSubtypes(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), 3); //$NON-NLS-1$
		Set notDeletedMembers= getNotDeletedMembers(new SubProgressMonitor(pm, 1));
		Set notDeletedMembersInTargetType= new HashSet();
		Set notDeletedMembersInSubtypes= new HashSet();
		for (Iterator iter= notDeletedMembers.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			if (getTargetClass().equals(member.getDeclaringType()))
				notDeletedMembersInTargetType.add(member);
			else
				notDeletedMembersInSubtypes.add(member);
		}
		checkMembersInDestinationType(result, notDeletedMembersInTargetType);
		checkAccessModifiers(result, notDeletedMembersInSubtypes);
		checkMethodReturnTypes(new SubProgressMonitor(pm, 1), result, notDeletedMembersInSubtypes);
		checkFieldTypes(new SubProgressMonitor(pm, 1), result);
		pm.done();
		return result;
	}

	private void checkMethodReturnTypes(IProgressMonitor pm, RefactoringStatus result, Set notDeletedMembersInSubtypes) throws JavaModelException {
		Map mapping= getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass(), true);
		IMember[] members= getMembersToBeCreatedInTargetClass();
		for (int i= 0; i < members.length; i++) {
			if (members[i].getElementType() != IJavaElement.METHOD)
				continue;
			IMethod method= (IMethod) members[i];
			String returnType= getReturnTypeName(method);
			Assert.isTrue(mapping.containsKey(method));
			for (Iterator iter= ((Set) mapping.get(method)).iterator(); iter.hasNext();) {
				IMethod matchingMethod= (IMethod) iter.next();
				if (method.equals(matchingMethod))
					continue;
				if (!notDeletedMembersInSubtypes.contains(matchingMethod))
					continue;
				if (returnType.equals(getReturnTypeName(matchingMethod)))
					continue;
				String[] keys= { createMethodLabel(matchingMethod), createTypeLabel(matchingMethod.getDeclaringType())};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.different_method_return_type", //$NON-NLS-1$
						keys);
				RefactoringStatusContext context= JavaStatusContext.create(matchingMethod.getCompilationUnit(), matchingMethod.getNameRange());
				result.addError(message, context);
			}
		}

	}

	private RefactoringStatus checkSuperclassesOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (getPossibleTargetClasses(result, pm).length == 0 && !result.hasFatalError()) {
			String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.not_this_type", new String[] { createTypeLabel(getDeclaringType())});//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		return result;
	}

	protected void clearCaches() {
		super.clearCaches();
		fCachedMembersReferences.clear();
		fCachedTargetClassHierarchy= null;
	}

	private void copyBodyOfPulledUpMethod(CompilationUnitRewrite sourceRewrite, CompilationUnitRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod, TypeVariableMaplet[] mapping, IProgressMonitor pm) throws JavaModelException {
		Block body= oldMethod.getBody();
		if (body == null) {
			newMethod.setBody(null);
			return;
		}
		try {
			final IDocument document= new Document(method.getCompilationUnit().getBuffer().getContents());
			final ASTRewrite rewrite= ASTRewrite.create(body.getAST());
			final ITrackedNodePosition position= rewrite.track(body);
			body.accept(new PullUpAstNodeMapper(sourceRewrite, targetRewrite, rewrite, getSuperclassOfDeclaringClass(pm), mapping));
			rewrite.rewriteAST(document, null).apply(document, TextEdit.NONE);
			String content= document.get(position.getStartPosition(), position.getLength());
			final String[] lines= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(method.getJavaProject()), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.getASTRewrite().createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
	}

	private void createAbstractMethods(IMethod sourceMethod, CompilationUnit declaringCuNode, AbstractTypeDeclaration targetClass, TypeVariableMaplet[] mapping, CompilationUnitRewrite targetRewrite, Map adjustments, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		AST ast= targetRewrite.getASTRewrite().getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(null);
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		newMethod.setJavadoc(null);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getModifiersWithUpdatedVisibility(sourceMethod, Modifier.ABSTRACT | JdtFlags.clearFlag(Modifier.NATIVE | Modifier.FINAL, sourceMethod.getFlags()), adjustments, pm, false, status)));
		newMethod.setName(((SimpleName) ASTNode.copySubtree(ast, oldMethod.getName())));
		copyReturnType(targetRewrite.getASTRewrite(), getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyParameters(targetRewrite.getASTRewrite(), getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		ImportUpdateUtil.addImports(targetRewrite, newMethod, new HashMap(), new HashMap(), false);
		targetRewrite.getASTRewrite().getListRewrite(targetClass, targetClass.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, targetClass.bodyDeclarations()), targetRewrite.createGroupDescription(RefactoringCoreMessages.getString("PullUpRefactoring.add_abstract_method"))); //$NON-NLS-1$
	}

	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("PullUpRefactoring.Pull_Up"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally {
			pm.done();
			clearCaches();
		}
	}

	private TextChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), 6); //$NON-NLS-1$
			final ICompilationUnit source= getDeclaringType().getCompilationUnit();
			final ICompilationUnit target= getTargetClass().getCompilationUnit();
			final CompilationUnitRewrite sourceRewriter= new CompilationUnitRewrite(source);
			final CompilationUnitRewrite targetRewriter= new CompilationUnitRewrite(target);
			final Map rewrites= new HashMap(2);
			rewrites.put(source, sourceRewriter);
			rewrites.put(target, targetRewriter);
			final Map deleteMap= createMembersToDeleteMap(new SubProgressMonitor(monitor, 1));
			final Map effectedMap= createNonAbstractSubclassesMapping(new SubProgressMonitor(monitor, 1));
			final ICompilationUnit[] units= getInvolvedCompilationUnits(new SubProgressMonitor(monitor, 1));
			ICompilationUnit unit= null;
			CompilationUnitRewrite rewrite= null;
			final Map adjustments= new HashMap();
			MemberVisibilityAdjustor adjustor= null;
			final IProgressMonitor sub= new SubProgressMonitor(monitor, 1);
			try {
				sub.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), units.length * 11); //$NON-NLS-1$
				for (int index= 0; index < units.length; index++) {
					unit= units[index];
					if (!(source.equals(unit) || target.equals(unit) || deleteMap.containsKey(unit) || effectedMap.containsKey(unit))) {
						sub.worked(10);
						continue;
					}
					rewrite= getCompilationUnitRewrite(rewrites, unit);
					if (deleteMap.containsKey(unit))
						deleteDeclarationNodes(sourceRewriter, sourceRewriter.getCu().equals(targetRewriter.getCu()), rewrite, (List) deleteMap.get(unit));
					final CompilationUnit root= sourceRewriter.getRoot();
					if (unit.equals(target)) {
						final ASTRewrite rewriter= rewrite.getASTRewrite();
						if (!JdtFlags.isAbstract(getTargetClass()) && getAbstractMethods().length > 0) {
							final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(getTargetClass(), rewrite.getRoot());
							ModifierRewrite.create(rewriter, declaration).setModifiers(declaration.getModifiers() | Modifier.ABSTRACT, rewrite.createGroupDescription(RefactoringCoreMessages.getString("PullUpRefactoring.make_target_abstract"))); //$NON-NLS-1$
						}
						final TypeVariableMaplet[] mapping= TypeVariableUtil.subTypeToSuperType(getDeclaringType(), getTargetClass());
						final IProgressMonitor subsub= new SubProgressMonitor(sub, 1);
						final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(getTargetClass(), rewrite.getRoot());
						fMembersToMove= JavaElementUtil.sortByOffset(fMembersToMove);
						subsub.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), fMembersToMove.length); //$NON-NLS-1$
						IMember member= null;
						for (int offset= fMembersToMove.length - 1; offset >= 0; offset--) {
							member= fMembersToMove[offset];
							adjustor= new MemberVisibilityAdjustor(getTargetClass(), member);
							adjustor.setRewrite(sourceRewriter.getASTRewrite(), root);

							// TW: set to error if bug 78387 is fixed
							adjustor.setFailureSeverity(RefactoringStatus.WARNING);

							adjustor.setRewrites(rewrites);
							adjustor.setStatus(status);
							adjustor.setAdjustments(adjustments);
							adjustor.adjustVisibility(new SubProgressMonitor(subsub, 1));
							if (member instanceof IField) {
								final VariableDeclarationFragment oldField= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, root);
								FieldDeclaration newField= createNewFieldDeclarationNode(rewriter, root, (IField) member, oldField, mapping, new SubProgressMonitor(subsub, 1), status, getModifiersWithUpdatedVisibility(member, member.getFlags(), adjustments, new SubProgressMonitor(subsub, 1), true, status));
								rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newField, ASTNodes.getInsertionIndex(newField, declaration.bodyDeclarations()), rewrite.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
								ImportUpdateUtil.addImports(rewrite, oldField.getParent(), new HashMap(), new HashMap(), false);
							} else if (member instanceof IMethod) {
								final MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, root);
								MethodDeclaration newMethod= createNewMethodDeclarationNode(sourceRewriter, rewrite, ((IMethod) member), oldMethod, root, mapping, adjustments, new SubProgressMonitor(subsub, 1), status);
								rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, declaration.bodyDeclarations()), rewrite.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
								ImportUpdateUtil.addImports(rewrite, oldMethod, new HashMap(), new HashMap(), false);
							} else if (member instanceof IType) {
								final AbstractTypeDeclaration oldType= ASTNodeSearchUtil.getAbstractTypeDeclarationNode((IType) member, root);
								BodyDeclaration newType= createNewTypeDeclarationNode(((IType) member), oldType, root, mapping, rewriter);
								rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newType, ASTNodes.getInsertionIndex(newType, declaration.bodyDeclarations()), rewrite.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
								ImportUpdateUtil.addImports(rewrite, oldType, new HashMap(), new HashMap(), false);
							} else
								Assert.isTrue(false);
							subsub.worked(1);
						}
						subsub.done();
						for (int offset= 0; offset < fMethodsToDeclareAbstract.length; offset++)
							createAbstractMethods(fMethodsToDeclareAbstract[offset], root, declaration, mapping, rewrite, adjustments, new SubProgressMonitor(sub, 1), status);
					} else
						sub.worked(2);
					if (unit.equals(sourceRewriter.getCu())) {
						final IProgressMonitor subsub= new SubProgressMonitor(sub, 1);
						subsub.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), fMethodsToDeclareAbstract.length * 2); //$NON-NLS-1$
						IMethod method= null;
						for (int offset= 0; offset < fMethodsToDeclareAbstract.length; offset++) {
							method= fMethodsToDeclareAbstract[offset];
							adjustor= new MemberVisibilityAdjustor(getTargetClass(), method);
							adjustor.setRewrite(sourceRewriter.getASTRewrite(), root);
							adjustor.setRewrites(rewrites);

							// TW: set to error if bug 78387 is fixed
							adjustor.setFailureSeverity(RefactoringStatus.WARNING);

							adjustor.setStatus(status);
							adjustor.setAdjustments(adjustments);
							if (needsVisibilityAdjustment(method, false, new SubProgressMonitor(subsub, 1), status))
								adjustments.put(method, new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(method, Modifier.ModifierKeyword.PROTECTED_KEYWORD, RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MemberVisibilityAdjustor.change_visibility_method_warning", new String[] { MemberVisibilityAdjustor.getLabel(method), RefactoringCoreMessages.getString("MemberVisibilityAdjustor.change_visibility_protected")}), JavaStatusContext.create(method)))); //$NON-NLS-1$ //$NON-NLS-2$
						}
					} else
						sub.worked(2);
					if (effectedMap.containsKey(unit))
						addMethodStubsToNonAbstractSubclassesOfTargetClass((List) effectedMap.get(unit), root, rewrite, adjustments, new SubProgressMonitor(sub, 2), status);
					if (sub.isCanceled())
						throw new OperationCanceledException();
				}
			} finally {
				sub.done();
			}
			if (adjustor != null && !adjustments.isEmpty())
				adjustor.rewriteVisibility(new SubProgressMonitor(monitor, 1));
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

	private Javadoc createJavadocForStub(String enclosingTypeName, MethodDeclaration oldMethod, MethodDeclaration newMethodNode, ICompilationUnit cu, ASTRewrite rewrite) throws CoreException {
		if (fCodeGenerationSettings.createComments) {
			IMethodBinding binding= oldMethod.resolveBinding();
			if (binding != null) {
				ITypeBinding[] params= binding.getParameterTypes();
				String fullTypeName= JavaModelUtil.getFullyQualifiedName(getTargetClass());
				String[] fullParamNames= new String[params.length];
				for (int i= 0; i < fullParamNames.length; i++) {
					fullParamNames[i]= Bindings.getFullyQualifiedName(params[i]);
				}
				String comment= StubUtility.getMethodComment(cu, enclosingTypeName, newMethodNode, true, false, fullTypeName, fullParamNames, String.valueOf('\n'));
				return (Javadoc) rewrite.createStringPlaceholder(comment, ASTNode.JAVADOC);
			}
		}
		return null;
	}

	private Map createMembersToDeleteMap(IProgressMonitor pm) throws JavaModelException {
		IMember[] membersToDelete= getMembersToDelete(pm);
		Map result= new HashMap();
		for (int i= 0; i < membersToDelete.length; i++) {
			IMember member= membersToDelete[i];
			ICompilationUnit cu= member.getCompilationUnit();
			if (!result.containsKey(cu))
				result.put(cu, new ArrayList(1));
			((List) result.get(cu)).add(member);
		}
		return result;
	}

	private MethodDeclaration createNewMethodDeclarationNode(CompilationUnitRewrite sourceRewrite, CompilationUnitRewrite targetRewrite, IMethod sourceMethod, MethodDeclaration oldMethod, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, Map adjustments, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		ASTRewrite rewrite= targetRewrite.getASTRewrite();
		AST ast= rewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		copyBodyOfPulledUpMethod(sourceRewrite, targetRewrite, sourceMethod, oldMethod, newMethod, mapping, pm);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		copyJavadocNode(rewrite, sourceMethod, oldMethod, newMethod);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getModifiersWithUpdatedVisibility(sourceMethod, sourceMethod.getFlags(), adjustments, pm, true, status)));
		newMethod.setName(((SimpleName) ASTNode.copySubtree(ast, oldMethod.getName())));
		copyReturnType(rewrite, getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyParameters(rewrite, getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		copyTypeParameters(oldMethod, newMethod);
		return newMethod;
	}

	private BodyDeclaration createNewTypeDeclarationNode(IType type, AbstractTypeDeclaration oldType, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite) throws JavaModelException {
		ICompilationUnit declaringCu= getDeclaringType().getCompilationUnit();
		if (!JdtFlags.isPublic(type) && !JdtFlags.isProtected(type)) {
			if (mapping.length > 0)
				return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

			return createPlaceholderForProtectedTypeDeclaration(oldType, declaringCuNode, declaringCu, rewrite, true);
		}
		if (mapping.length > 0)
			return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

		return createPlaceholderForTypeDeclaration(oldType, declaringCu, rewrite, true);
	}

	private Map createNonAbstractSubclassesMapping(IProgressMonitor pm) throws JavaModelException {
		if (!(fCreateMethodStubs && getAbstractMethods().length > 0))
			return new HashMap(0);
		Set nonAbstractSubclasses= getNonAbstractSubclasses(getTypeHierarchyOfTargetClass(pm), getTargetClass());
		Map result= new HashMap();
		for (Iterator iter= nonAbstractSubclasses.iterator(); iter.hasNext();) {
			IType type= (IType) iter.next();
			ICompilationUnit cu= type.getCompilationUnit();
			if (!result.containsKey(cu))
				result.put(cu, new ArrayList(1));
			((List) result.get(cu)).add(type);
		}
		return result;
	}

	private IMethod[] getAbstractMethods() throws JavaModelException {
		IMethod[] toDeclareAbstract= fMethodsToDeclareAbstract;
		IMethod[] abstractPulledUp= getAbstractMethodsToPullUp();
		List result= new ArrayList(toDeclareAbstract.length + abstractPulledUp.length);
		result.addAll(Arrays.asList(toDeclareAbstract));
		result.addAll(Arrays.asList(abstractPulledUp));
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	private IMethod[] getAbstractMethodsToPullUp() throws JavaModelException {
		List result= new ArrayList(fMembersToMove.length);
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			if (member instanceof IMethod && JdtFlags.isAbstract(member))
				result.add(member);
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	public IMember[] getAdditionalRequiredMembersToPullUp(IProgressMonitor pm) throws JavaModelException {
		IMember[] members= getMembersToBeCreatedInTargetClass();
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.calculating_required"), members.length);// not true, but not easy to give anything better //$NON-NLS-1$
		List queue= new ArrayList(members.length);
		queue.addAll(Arrays.asList(members));
		if (queue.isEmpty())
			return new IMember[0];
		int i= 0;
		IMember current;
		do {
			current= (IMember) queue.get(i);
			addAllRequiredPullableMembers(queue, current, new SubProgressMonitor(pm, 1));
			i++;
			if (queue.size() == i)
				current= null;
		} while (current != null);
		queue.removeAll(Arrays.asList(members));// report only additional
		return (IMember[]) queue.toArray(new IMember[queue.size()]);
	}

	public boolean getCreateMethodStubs() {
		return fCreateMethodStubs;
	}

	private ICompilationUnit[] getInvolvedCompilationUnits(IProgressMonitor pm) throws JavaModelException {
		IType[] allSubtypes= getTypeHierarchyOfTargetClass(pm).getAllSubtypes(getTargetClass());
		Set result= new HashSet(allSubtypes.length);
		for (int i= 0; i < allSubtypes.length; i++) {
			result.add(allSubtypes[i].getCompilationUnit());
		}
		result.add(getTargetClass().getCompilationUnit());
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	public IMember[] getMatchingElements(IProgressMonitor pm, boolean includeMethodsToDeclareAbstract) throws JavaModelException {
		try {
			Set result= new HashSet();
			IType targetClass= getTargetClass();
			Map matching= getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass(), includeMethodsToDeclareAbstract);
			for (Iterator iter= matching.keySet().iterator(); iter.hasNext();) {
				IMember key= (IMember) iter.next();
				Assert.isTrue(!key.getDeclaringType().equals(targetClass));
				result.addAll((Set) matching.get(key));
			}
			return (IMember[]) result.toArray(new IMember[result.size()]);
		} finally {
			pm.done();
		}
	}

	private Map getMatchingMembersMapping(IType analyzedType) throws JavaModelException {
		Map result= new HashMap();// IMember -> Set of IMembers (of the same type as key)
		IMember[] members= getMembersToBeCreatedInTargetClass();
		for (int i= 0; i < members.length; i++) {
			IMember member= members[i];
			if (member instanceof IMethod) {
				IMethod method= (IMethod) member;
				IMethod found= MemberCheckUtil.findMethod(method, analyzedType.getMethods());
				if (found != null)
					addToMapping(result, method, found);
			} else if (member instanceof IField) {
				IField field= (IField) member;
				IField found= analyzedType.getField(field.getElementName());
				if (found.exists())
					addToMapping(result, field, found);
			} else if (member instanceof IType) {
				IType type= (IType) member;
				IType found= analyzedType.getType(type.getElementName());
				if (found.exists())
					addToMapping(result, type, found);
			} else
				Assert.isTrue(false);
		}

		return result;
	}

	private Map getMatchingMembersMappingFromTypeAndAllSubtypes(ITypeHierarchy hierarchy, IType type, boolean includeMethodsToDeclareAbstract) throws JavaModelException {
		Map result= new HashMap(); // IMember -> Set of IMembers (of the same type as key)
		result.putAll(getMatchingMembersMapping(type));
		IType[] subTypes= hierarchy.getAllSubtypes(type);
		for (int i= 0; i < subTypes.length; i++) {
			Map map= getMatchingMembersMapping(subTypes[i]);
			mergeSetsForCommonKeys(result, map);
			putAllThatDoNotExistInResultYet(result, map);
		}
		if (includeMethodsToDeclareAbstract)
			return result;

		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			if (result.containsKey(fMethodsToDeclareAbstract[i]))
				result.remove(fMethodsToDeclareAbstract[i]);
		}
		return result;
	}

	private IMember[] getMembersToBeCreatedInTargetClass() {
		List result= new ArrayList(fMembersToMove.length + fMethodsToDeclareAbstract.length);
		result.addAll(Arrays.asList(fMembersToMove));
		result.addAll(Arrays.asList(fMethodsToDeclareAbstract));
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private IMember[] getMembersToDelete(IProgressMonitor pm) throws JavaModelException {
		try {
			IMember[] typesToDelete= WorkingCopyUtil.getOriginals(getMembersOfType(fMembersToMove, IJavaElement.TYPE));
			IMember[] matchingElements= getMatchingElements(pm, false);
			IMember[] matchingFields= WorkingCopyUtil.getOriginals(getMembersOfType(matchingElements, IJavaElement.FIELD));
			return JavaElementUtil.merge(JavaElementUtil.merge(matchingFields, typesToDelete), fMethodsToDelete);
		} finally {
			pm.done();
		}
	}

	private int getModifiersWithUpdatedVisibility(IMember member, int modifiers, Map adjustments, IProgressMonitor monitor, boolean considerReferences, RefactoringStatus status) throws JavaModelException {
		if (needsVisibilityAdjustment(member, considerReferences, monitor, status)) {
			final MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment adjustment= new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(member, Modifier.ModifierKeyword.PROTECTED_KEYWORD, RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString(MemberVisibilityAdjustor.getMessage(member), new String[] { MemberVisibilityAdjustor.getLabel(member), MemberVisibilityAdjustor.getLabel(Modifier.ModifierKeyword.PROTECTED_KEYWORD)})));
			adjustment.setNeedsRewriting(false);
			adjustments.put(member, adjustment);
			return JdtFlags.clearAccessModifiers(modifiers) | Modifier.PROTECTED;
		}
		return modifiers;
	}

	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("PullUpRefactoring.Pull_Up"); //$NON-NLS-1$
	}

	/* @return not deleted matching members of membersToBeCreatedInTargetClass */
	private Set getNotDeletedMembers(IProgressMonitor pm) throws JavaModelException {
		Set matchingSet= new HashSet();
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), 2); //$NON-NLS-1$
		matchingSet.addAll(Arrays.asList(getMatchingElements(new SubProgressMonitor(pm, 1), true)));
		matchingSet.removeAll(Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1))));
		pm.done();
		return matchingSet;
	}

	public IType[] getPossibleTargetClasses(RefactoringStatus status, IProgressMonitor pm) throws JavaModelException {
		IType[] superClasses= getDeclaringType().newSupertypeHierarchy(pm).getAllSuperclasses(getDeclaringType());
		List superClassList= new ArrayList(superClasses.length);
		int binary= 0;
		for (int i= 0; i < superClasses.length; i++) {
			IType superclass= superClasses[i];
			if (superclass != null && superclass.exists() && !superclass.isReadOnly() && !superclass.isBinary() && !"java.lang.Object".equals(superclass.getFullyQualifiedName())) { //$NON-NLS-1$
				superClassList.add(superclass);
			} else {
				if (superclass != null && superclass.isBinary()) {
					binary++;
				}
			}
		}
		if (superClasses.length == 1 && superClasses[0].getFullyQualifiedName().equals("java.lang.Object")) //$NON-NLS-1$
			status.addFatalError(RefactoringCoreMessages.getString("PullUPRefactoring.not_java_lang_object")); //$NON-NLS-1$
		else if (superClasses.length == binary)
			status.addFatalError(RefactoringCoreMessages.getString("PullUPRefactoring.no_all_binary")); //$NON-NLS-1$

		Collections.reverse(superClassList);
		return (IType[]) superClassList.toArray(new IType[superClassList.size()]);
	}

	public IMember[] getPullableMembersOfDeclaringType() {
		try {
			return getPullableMembers(getDeclaringType());
		} catch (JavaModelException e) {
			return new IMember[0];
		}
	}

	// skipped super classes are those declared in the hierarchy between the declaring type of the selected members
	// and the target type
	private Set getSkippedSuperclasses(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking"), 1); //$NON-NLS-1$
		try {
			if (fCachedSkippedSuperclasses != null && getTypeHierarchyOfTargetClass(new SubProgressMonitor(pm, 1)).getType().equals(getTargetClass()))
				return fCachedSkippedSuperclasses;
			ITypeHierarchy hierarchy= getTypeHierarchyOfTargetClass(new SubProgressMonitor(pm, 1));
			fCachedSkippedSuperclasses= new HashSet(2);
			IType current= hierarchy.getSuperclass(getDeclaringType());
			while (current != null && !current.equals(getTargetClass())) {
				fCachedSkippedSuperclasses.add(current);
				current= hierarchy.getSuperclass(current);
			}
			return fCachedSkippedSuperclasses;
		} finally {
			pm.done();
		}
	}

	private IType getSuperclassOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		IType declaringType= getDeclaringType();
		return declaringType.newSupertypeHierarchy(pm).getSuperclass(declaringType);
	}

	public IType getTargetClass() {
		return fTargetType;
	}

	public ITypeHierarchy getTypeHierarchyOfTargetClass(IProgressMonitor pm) throws JavaModelException {
		try {
			if (fCachedTargetClassHierarchy != null && fCachedTargetClassHierarchy.getType().equals(getTargetClass()))
				return fCachedTargetClassHierarchy;
			fCachedTargetClassHierarchy= getTargetClass().newTypeHierarchy(pm);
			return fCachedTargetClassHierarchy;
		} finally {
			pm.done();
		}
	}

	private boolean isDeclaredInTargetClassOrItsSuperclass(IMethod method, IProgressMonitor pm) throws JavaModelException {
		try {
			boolean isConstructor= false;
			String[] paramTypes= method.getParameterTypes();
			String name= method.getElementName();
			IType targetClass= getTargetClass();
			ITypeHierarchy hierarchy= getTypeHierarchyOfTargetClass(pm);
			IMethod first= JavaModelUtil.findMethod(name, paramTypes, isConstructor, targetClass);
			if (first != null && MethodChecks.isVirtual(first))
				return true;
			IMethod found= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, targetClass, name, paramTypes, isConstructor);
			return found != null && MethodChecks.isVirtual(found);
		} finally {
			pm.done();
		}
	}

	private boolean isRequiredPullableMember(List queue, IMember member) throws JavaModelException {
		if (member.getDeclaringType() == null) // not a member
			return false;
		return member.getDeclaringType().equals(getDeclaringType()) && !queue.contains(member) && isPullable(member);
	}

	private boolean isVirtualAccessibleFromTargetClass(IMethod method, IProgressMonitor pm) throws JavaModelException {
		try {
			return MethodChecks.isVirtual(method) && isDeclaredInTargetClassOrItsSuperclass(method, pm);
		} finally {
			pm.done();
		}
	}

	public void setCreateMethodStubs(boolean create) {
		fCreateMethodStubs= create;
	}

	/*
	 * no validation is done here - the members must be a subset of those returned by the call to getPullableMembersOfDeclaringType
	 */
	public void setMembersToMove(IMember[] elements) {
		Assert.isNotNull(elements);
		fMembersToMove= (IMember[]) SourceReferenceUtil.sortByOffset(elements);
		fMembersToMove= WorkingCopyUtil.getOriginals(fMembersToMove);
	}

	/*
	 * no validation is done here - the members must be a subset of those returned by the call to getPullableMembersOfDeclaringType
	 */
	public void setMethodsToDeclareAbstract(IMethod[] methods) {
		Assert.isNotNull(methods);
		fMethodsToDeclareAbstract= getOriginals(methods);
	}

	/**
	 * Sets the methodsToDelete.
	 * 
	 * @param methodsToDelete The methodsToDelete to set no validation is done on these methods - they will simply be removed. it is the caller's responsibility to ensure that the selection makes sense from the behavior-preservation point of view.
	 */
	public void setMethodsToDelete(IMethod[] methodsToDelete) {
		Assert.isNotNull(methodsToDelete);
		fMethodsToDelete= getOriginals(methodsToDelete);
	}

	public void setTargetClass(IType targetType) {
		Assert.isNotNull(targetType);
		if (!targetType.equals(fTargetType))
			fCachedTargetClassHierarchy= null;
		fTargetType= targetType;
	}
}