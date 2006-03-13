/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
import java.util.Collection;
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
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.osgi.util.NLS;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
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
import org.eclipse.jdt.core.dom.MarkerAnnotation;
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
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsSolver;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ISourceConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Refactoring processor for the pull up refactoring.
 * 
 * @since 3.2
 */
public class PullUpRefactoringProcessor extends HierarchyProcessor {

	/** The pull up group category set */
	private static final GroupCategorySet SET_PULL_UP= new GroupCategorySet(new GroupCategory("org.eclipse.jdt.internal.corext.pullUp", //$NON-NLS-1$
			RefactoringCoreMessages.PullUpRefactoring_category_name, RefactoringCoreMessages.PullUpRefactoring_category_description));

	/**
	 * AST node visitor which performs the actual mapping.
	 */
	private static class PullUpAstNodeMapper extends TypeVariableMapper {

		/** Are we in an anonymous class declaration? */
		protected boolean fAnonymousClassDeclaration= false;

		/** The source compilation unit rewrite to use */
		protected final CompilationUnitRewrite fSourceRewriter;

		/** The super reference type */
		protected final IType fSuperReferenceType;

		/** The target compilation unit rewrite to use */
		protected final CompilationUnitRewrite fTargetRewriter;

		/** Are we in a type declaration statement? */
		protected boolean fTypeDeclarationStatement= false;

		/**
		 * Creates a new pull up ast node mapper.
		 * 
		 * @param sourceRewriter
		 *            the source compilation unit rewrite to use
		 * @param targetRewriter
		 *            the target compilation unit rewrite to use
		 * @param rewrite
		 *            the AST rewrite to use
		 * @param type
		 *            the super reference type
		 * @param mapping
		 *            the type variable mapping
		 */
		public PullUpAstNodeMapper(final CompilationUnitRewrite sourceRewriter, final CompilationUnitRewrite targetRewriter, final ASTRewrite rewrite, final IType type, final TypeVariableMaplet[] mapping) {
			super(rewrite, mapping);
			Assert.isNotNull(rewrite);
			Assert.isNotNull(type);
			fSourceRewriter= sourceRewriter;
			fTargetRewriter= targetRewriter;
			fSuperReferenceType= type;
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
					final ITypeBinding binding= ((IMethodBinding) name).getDeclaringClass();
					if (binding != null) {
						final IType type= (IType) binding.getJavaElement();
						if (!fSuperReferenceType.equals(type))
							return true;
					}
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

	private static final String ATTRIBUTE_ABSTRACT= "abstract"; //$NON-NLS-1$

	private static final String ATTRIBUTE_DELETE= "delete"; //$NON-NLS-1$

	private static final String ATTRIBUTE_PULL= "pull"; //$NON-NLS-1$

	private static final String ATTRIBUTE_STUBS= "stubs"; //$NON-NLS-1$

	private static final String ID_PULL_UP= "org.eclipse.jdt.ui.pull.up"; //$NON-NLS-1$

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.pullUpProcessor"; //$NON-NLS-1$

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

	private CodeGenerationSettings fCodeGenerationSettings;

	private boolean fCreateMethodStubs;

	private IMethod[] fMethodsToDeclareAbstract;

	private IMethod[] fMethodsToDelete;

	/** Should occurrences of the type be replaced by the supertype? */
	private boolean fReplace= false;

	private IType fTargetType;

	/**
	 * Creates a new pull up refactoring processor.
	 * 
	 * @param members
	 *            the members to pull up, or <code>null</code> if invoked by
	 *            scripting
	 * @param settings
	 *            the code generation settings, or <code>null</code> if
	 *            invoked by scripting
	 */
	public PullUpRefactoringProcessor(final IMember[] members, final CodeGenerationSettings settings) {
		super(members);
		if (members != null) {
			final IType type= RefactoringAvailabilityTester.getTopLevelType(members);
			try {
				if (type != null && RefactoringAvailabilityTester.getPullUpMembers(type).length != 0) {
					fMembersToMove= new IMember[0];
					fDeclaringType= RefactoringAvailabilityTester.getTopLevelType(members);
				}
			} catch (JavaModelException exception) {
				JavaPlugin.log(exception);
			}
		}
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
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, 3);
			IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[] { member}, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			boolean isStatic= false;
			try {
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, requiredMethods.length);
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
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, requiredFields.length);
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
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, requiredMethods.length);
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
		TypeVariableMaplet[] mapping= TypeVariableUtil.composeMappings(TypeVariableUtil.subTypeToSuperType(getDeclaringType(), getTargetClass()), TypeVariableUtil.superTypeToInheritedType(getTargetClass(), ((IType) typeToCreateStubIn.resolveBinding().getJavaElement())));
		copyReturnType(rewriter.getASTRewrite(), getDeclaringType().getCompilationUnit(), methodToCreateStubFor, newMethod, mapping);
		copyParameters(rewriter.getASTRewrite(), getDeclaringType().getCompilationUnit(), methodToCreateStubFor, newMethod, mapping);
		copyThrownExceptions(methodToCreateStubFor, newMethod);
		newMethod.setJavadoc(createJavadocForStub(typeToCreateStubIn.getName().getIdentifier(), methodToCreateStubFor, newMethod, newCu, rewriter.getASTRewrite()));
		ImportRewriteUtil.addImports(rewriter, newMethod, new HashMap(), new HashMap(), false);
		rewriter.getASTRewrite().getListRewrite(typeToCreateStubIn, typeToCreateStubIn.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, typeToCreateStubIn.bodyDeclarations()), rewriter.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_method_stub, SET_PULL_UP));
	}

	private void addMethodStubsToNonAbstractSubclassesOfTargetClass(List concreteSubclasses, CompilationUnit declaringCuNode, CompilationUnitRewrite unitRewriter, Map adjustments, IProgressMonitor monitor, RefactoringStatus status) throws CoreException {
		IType declaringType= getDeclaringType();
		IMethod[] methods= getAbstractMethods();
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, concreteSubclasses.size());
		for (Iterator iter= concreteSubclasses.iterator(); iter.hasNext();) {
			IType clazz= (IType) iter.next();
			if (clazz.equals(declaringType))
				continue;
			AbstractTypeDeclaration classToCreateStubIn= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(clazz, unitRewriter.getRoot());
			ICompilationUnit cuToCreateStubIn= clazz.getCompilationUnit();
			IProgressMonitor sub= new SubProgressMonitor(monitor, 1);
			sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, methods.length);
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
				IType type= member.getDeclaringType();
				if (type != null)
					return hierarchy.contains(type);
				return false;
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

	private RefactoringStatus checkAccessedFields(IProgressMonitor pm, ITypeHierarchy hierarchy) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 2);
		RefactoringStatus result= new RefactoringStatus();

		List pulledUpList= Arrays.asList(fMembersToMove);
		List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1)));
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fMembersToMove, new SubProgressMonitor(pm, 1));

		IType targetClass= getTargetClass();
		for (int i= 0; i < accessedFields.length; i++) {
			IField field= accessedFields[i];
			if (!field.exists())
				continue;

			boolean isAccessible= pulledUpList.contains(field) || deletedList.contains(field) || canBeAccessedFrom(field, targetClass, hierarchy) || Flags.isEnum(field.getFlags());
			if (!isAccessible) {
				String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_field_not_accessible, new String[] { createFieldLabel(field), createTypeLabel(targetClass)});
				result.addError(message, JavaStatusContext.create(field));
			} else if (getSkippedSuperclasses(new SubProgressMonitor(pm, 1)).contains(field.getDeclaringType())) {
				String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_field_cannot_be_accessed, new String[] { createFieldLabel(field), createTypeLabel(targetClass)});
				result.addError(message, JavaStatusContext.create(field));
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IProgressMonitor pm, ITypeHierarchy hierarchy) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 2);
		RefactoringStatus result= new RefactoringStatus();

		List pulledUpList= Arrays.asList(fMembersToMove);
		List declaredAbstractList= Arrays.asList(fMethodsToDeclareAbstract);
		List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1)));
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fMembersToMove, new SubProgressMonitor(pm, 1));

		IType targetClass= getTargetClass();
		for (int i= 0; i < accessedMethods.length; i++) {
			IMethod method= accessedMethods[i];
			if (!method.exists())
				continue;
			boolean isAccessible= pulledUpList.contains(method) || deletedList.contains(method) || declaredAbstractList.contains(method) || canBeAccessedFrom(method, targetClass, hierarchy);
			if (!isAccessible) {
				String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_method_not_accessible, new String[] { createMethodLabel(method), createTypeLabel(targetClass)});
				result.addError(message, JavaStatusContext.create(method));
			} else if (getSkippedSuperclasses(new SubProgressMonitor(pm, 1)).contains(method.getDeclaringType())) {
				String[] keys= { createMethodLabel(method), createTypeLabel(targetClass)};
				String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_method_cannot_be_accessed, keys);
				result.addError(message, JavaStatusContext.create(method));
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedTypes(IProgressMonitor pm, ITypeHierarchy hierarchy) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypesReferencedInMovedMembers(pm);
		IType targetClass= getTargetClass();
		List pulledUpList= Arrays.asList(fMembersToMove);
		for (int i= 0; i < accessedTypes.length; i++) {
			IType iType= accessedTypes[i];
			if (!iType.exists())
				continue;

			if (!canBeAccessedFrom(iType, targetClass, hierarchy) && !pulledUpList.contains(iType)) {
				String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_type_not_accessible, new String[] { createTypeLabel(iType), createTypeLabel(targetClass)});
				result.addError(message, JavaStatusContext.create(iType));
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccesses(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 3);
		final ITypeHierarchy hierarchy= getTargetClass().newSupertypeHierarchy(null);
		result.merge(checkAccessedTypes(new SubProgressMonitor(pm, 1), hierarchy));
		result.merge(checkAccessedFields(new SubProgressMonitor(pm, 1), hierarchy));
		result.merge(checkAccessedMethods(new SubProgressMonitor(pm, 1), hierarchy));
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
						result.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_lower_default_visibility, new String[] { createMethodLabel(method), createTypeLabel(method.getDeclaringType())}), JavaStatusContext.create(method));
				} else if (!JdtFlags.isPublic(method) && !JdtFlags.isProtected(method))
					result.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_lower_protected_visibility, new String[] { createMethodLabel(method), createTypeLabel(method.getDeclaringType())}), JavaStatusContext.create(method));
			}
		}
	}

	protected RefactoringStatus checkDeclaringType(IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus status= super.checkDeclaringType(monitor);
		if (JavaModelUtil.getFullyQualifiedName(getDeclaringType()).equals("java.lang.Object")) //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PullUpRefactoring_no_java_lang_Object));
		status.merge(checkSuperclassesOfDeclaringClass(monitor));
		return status;
	}

	private void checkFieldTypes(IProgressMonitor pm, RefactoringStatus result) throws JavaModelException {
		Map mapping= getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass(), true);
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (fMembersToMove[i].getElementType() != IJavaElement.FIELD)
				continue;
			IField field= (IField) fMembersToMove[i];
			String type= Signature.toString(field.getTypeSignature());
			Assert.isTrue(mapping.containsKey(field));
			for (Iterator iter= ((Set) mapping.get(field)).iterator(); iter.hasNext();) {
				IField matchingField= (IField) iter.next();
				if (field.equals(matchingField))
					continue;
				if (type.equals(Signature.toString(matchingField.getTypeSignature())))
					continue;
				String[] keys= { createFieldLabel(matchingField), createTypeLabel(matchingField.getDeclaringType())};
				String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_different_field_type, keys);
				RefactoringStatusContext context= JavaStatusContext.create(matchingField.getCompilationUnit(), matchingField.getSourceRange());
				result.addError(message, context);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		try {
			pm.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 7);
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
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getRefactoring().getValidationContext()));
			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkFinalFields(IProgressMonitor monitor) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, fMembersToMove.length);
		for (int index= 0; index < fMembersToMove.length; index++) {
			IMember member= fMembersToMove[index];
			if (member.getElementType() == IJavaElement.FIELD) {
				if (!JdtFlags.isStatic(member)) {
					if (JdtFlags.isFinal(member)) {
						RefactoringStatusContext context= JavaStatusContext.create(member);
						result.addWarning(RefactoringCoreMessages.PullUpRefactoring_final_fields, context);
					} else if (getTargetClass().isInterface()) {
						RefactoringStatusContext context= JavaStatusContext.create(member);
						result.addWarning(RefactoringCoreMessages.PullUpRefactoring_non_final_pull_up_to_interface, context);
					}
				}
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
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, pullables.length);

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
							status.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_variable_not_available, new String[] { unmapped[0], declaring.getSuperclassName()}), JavaStatusContext.create(member));
							break;
						case 2:
							status.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_variable2_not_available, new String[] { unmapped[0], unmapped[1], declaring.getSuperclassName()}), JavaStatusContext.create(member));
							break;
						case 3:
							status.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_variable3_not_available, new String[] { unmapped[0], unmapped[1], unmapped[2], declaring.getSuperclassName()}), JavaStatusContext.create(member));
							break;
						default:
							status.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_variables_not_available, new String[] { declaring.getSuperclassName()}), JavaStatusContext.create(member));
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
		String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_Field_declared_in_class, keys);
		RefactoringStatusContext context= JavaStatusContext.create(fieldInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfMethodDeclaredIn(IMethod iMethod, IType type) throws JavaModelException {
		IMethod methodInType= JavaModelUtil.findMethod(iMethod.getElementName(), iMethod.getParameterTypes(), iMethod.isConstructor(), type);
		if (methodInType == null || !methodInType.exists())
			return null;
		String[] keys= { createMethodLabel(methodInType), createTypeLabel(type)};
		String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_Method_declared_in_class, keys);
		RefactoringStatusContext context= JavaStatusContext.create(methodInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfSkippingOverElements(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
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
		String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_declared_in_class, keys);
		RefactoringStatusContext context= JavaStatusContext.create(typeInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
			RefactoringStatus status= new RefactoringStatus();
			status.merge(checkDeclaringType(new SubProgressMonitor(monitor, 1)));
			if (status.hasFatalError())
				return status;
			status.merge(checkIfMembersExist());
			if (status.hasFatalError())
				return status;
			return status;
		} finally {
			monitor.done();
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
		pm.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 3);
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
			String returnType= Signature.toString(Signature.getReturnType(method.getSignature()).toString());
			Assert.isTrue(mapping.containsKey(method));
			for (Iterator iter= ((Set) mapping.get(method)).iterator(); iter.hasNext();) {
				IMethod matchingMethod= (IMethod) iter.next();
				if (method.equals(matchingMethod))
					continue;
				if (!notDeletedMembersInSubtypes.contains(matchingMethod))
					continue;
				if (returnType.equals(Signature.toString(Signature.getReturnType(matchingMethod.getSignature()).toString())))
					continue;
				String[] keys= { createMethodLabel(matchingMethod), createTypeLabel(matchingMethod.getDeclaringType())};
				String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_different_method_return_type, keys);
				RefactoringStatusContext context= JavaStatusContext.create(matchingMethod.getCompilationUnit(), matchingMethod.getNameRange());
				result.addError(message, context);
			}
		}

	}

	private RefactoringStatus checkSuperclassesOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (getPossibleTargetClasses(result, pm).length == 0 && !result.hasFatalError()) {
			String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_not_this_type, new String[] { createTypeLabel(getDeclaringType())});
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
			rewrite.rewriteAST(document, method.getJavaProject().getOptions(true)).apply(document, TextEdit.NONE);
			String content= document.get(position.getStartPosition(), position.getLength());
			final String[] lines= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, method.getJavaProject(), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.getASTRewrite().createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
	}

	private void createAbstractMethod(IMethod sourceMethod, CompilationUnitRewrite sourceRewriter, CompilationUnit declaringCuNode, AbstractTypeDeclaration targetClass, TypeVariableMaplet[] mapping, CompilationUnitRewrite targetRewrite, Map adjustments, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		if (JavaModelUtil.is50OrHigher(sourceMethod.getJavaProject()) && (fCodeGenerationSettings.overrideAnnotation || JavaCore.ERROR.equals(sourceMethod.getJavaProject().getOption(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, true)))) {
			final MarkerAnnotation annotation= sourceRewriter.getAST().newMarkerAnnotation();
			annotation.setTypeName(sourceRewriter.getAST().newSimpleName("Override")); //$NON-NLS-1$
			sourceRewriter.getASTRewrite().getListRewrite(oldMethod, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(annotation, sourceRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_override_annotation, SET_PULL_UP));
		}
		MethodDeclaration newMethod= targetRewrite.getAST().newMethodDeclaration();
		newMethod.setBody(null);
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		newMethod.setJavadoc(null);
		int modifiers= getModifiersWithUpdatedVisibility(sourceMethod, Modifier.ABSTRACT | JdtFlags.clearFlag(Modifier.NATIVE | Modifier.FINAL, sourceMethod.getFlags()), adjustments, pm, false, status);
		if (oldMethod.isVarargs())
			modifiers&= ~Flags.AccVarargs;
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(targetRewrite.getAST(), modifiers));
		newMethod.setName(((SimpleName) ASTNode.copySubtree(targetRewrite.getAST(), oldMethod.getName())));
		copyReturnType(targetRewrite.getASTRewrite(), getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyParameters(targetRewrite.getASTRewrite(), getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		ImportRewriteUtil.addImports(targetRewrite, newMethod, new HashMap(), new HashMap(), false);
		targetRewrite.getASTRewrite().getListRewrite(targetClass, targetClass.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, targetClass.bodyDeclarations()), targetRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_abstract_method, SET_PULL_UP));
	}

	/**
	 * {@inheritDoc}
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			return new DynamicValidationStateChange(RefactoringCoreMessages.PullUpRefactoring_Pull_Up, fChangeManager.getAllChanges()) {

				public final ChangeDescriptor getDescriptor() {
					final Map arguments= new HashMap();
					String project= null;
					final IType declaring= getDeclaringType();
					final IJavaProject javaProject= declaring.getJavaProject();
					if (javaProject != null)
						project= javaProject.getElementName();
					int flags= JavaRefactoringDescriptor.JAR_IMPORTABLE | JavaRefactoringDescriptor.JAR_REFACTORABLE | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
					try {
						if (declaring.isLocal() || declaring.isAnonymous())
							flags|= JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
					final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(ID_PULL_UP, project, fMembersToMove.length == 1 ? NLS.bind(RefactoringCoreMessages.PullUpRefactoring_descriptor_description_full, new String[] { JavaElementLabels.getElementLabel(fMembersToMove[0], JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getElementLabel(declaring, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getElementLabel(fTargetType, JavaElementLabels.ALL_FULLY_QUALIFIED)}) : NLS.bind(RefactoringCoreMessages.PullUpRefactoring_descriptor_description, new String[] { JavaElementLabels.getElementLabel(declaring, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getElementLabel(fTargetType, JavaElementLabels.ALL_FULLY_QUALIFIED)}), getComment(), arguments, flags);
					arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fTargetType));
					if (fDeclaringType != null)
						arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1, descriptor.elementToHandle(fDeclaringType));
					arguments.put(ATTRIBUTE_PULL, new Integer(fMembersToMove.length).toString());
					for (int offset= 0; offset < fMembersToMove.length; offset++)
						arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 2), descriptor.elementToHandle(fMembersToMove[offset]));
					arguments.put(ATTRIBUTE_DELETE, new Integer(fMethodsToDelete.length).toString());
					for (int offset= 0; offset < fMethodsToDelete.length; offset++)
						arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + 2), descriptor.elementToHandle(fMethodsToDelete[offset]));
					arguments.put(ATTRIBUTE_ABSTRACT, new Integer(fMethodsToDeclareAbstract.length).toString());
					for (int offset= 0; offset < fMethodsToDeclareAbstract.length; offset++)
						arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + fMethodsToDelete.length + 2), descriptor.elementToHandle(fMethodsToDeclareAbstract[offset]));
					arguments.put(ATTRIBUTE_STUBS, Boolean.valueOf(fCreateMethodStubs).toString());
					return new RefactoringChangeDescriptor(descriptor);
				}
			};
		} finally {
			pm.done();
			clearCaches();
		}
	}

	private TextChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 8);
			final ICompilationUnit source= getDeclaringType().getCompilationUnit();
			final IType targetClass= getTargetClass();
			final ICompilationUnit target= targetClass.getCompilationUnit();
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
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, units.length * 11);
				for (int index= 0; index < units.length; index++) {
					unit= units[index];
					if (!(source.equals(unit) || target.equals(unit) || deleteMap.containsKey(unit) || effectedMap.containsKey(unit))) {
						sub.worked(10);
						continue;
					}
					rewrite= getCompilationUnitRewrite(rewrites, unit);
					if (deleteMap.containsKey(unit) && !targetClass.isInterface())
						deleteDeclarationNodes(sourceRewriter, sourceRewriter.getCu().equals(targetRewriter.getCu()), rewrite, (List) deleteMap.get(unit), SET_PULL_UP);
					final CompilationUnit root= sourceRewriter.getRoot();
					if (unit.equals(target)) {
						final ASTRewrite rewriter= rewrite.getASTRewrite();
						if (!JdtFlags.isAbstract(targetClass) && !targetClass.isInterface() && getAbstractMethods().length > 0) {
							final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(targetClass, rewrite.getRoot());
							ModifierRewrite.create(rewriter, declaration).setModifiers(declaration.getModifiers() | Modifier.ABSTRACT, rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_make_target_abstract, SET_PULL_UP));
						}
						final TypeVariableMaplet[] mapping= TypeVariableUtil.subTypeToSuperType(getDeclaringType(), targetClass);
						final IProgressMonitor subsub= new SubProgressMonitor(sub, 1);
						final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(targetClass, rewrite.getRoot());
						fMembersToMove= JavaElementUtil.sortByOffset(fMembersToMove);
						subsub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, fMembersToMove.length);
						IMember member= null;
						for (int offset= fMembersToMove.length - 1; offset >= 0; offset--) {
							member= fMembersToMove[offset];
							adjustor= new MemberVisibilityAdjustor(targetClass, member);
							adjustor.setRewrite(sourceRewriter.getASTRewrite(), root);

							// TW: set to error if bug 78387 is fixed
							adjustor.setFailureSeverity(RefactoringStatus.WARNING);

							adjustor.setRewrites(rewrites);
							adjustor.setStatus(status);
							adjustor.setAdjustments(adjustments);
							adjustor.adjustVisibility(new SubProgressMonitor(subsub, 1));
							adjustments.remove(member);
							if (member instanceof IField) {
								final VariableDeclarationFragment oldField= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, root);
								if (oldField != null) {
									int flags= getModifiersWithUpdatedVisibility(member, member.getFlags(), adjustments, new SubProgressMonitor(subsub, 1), true, status);
									if (targetClass.isInterface())
										flags|= Flags.AccFinal;
									FieldDeclaration newField= createNewFieldDeclarationNode(rewriter, root, (IField) member, oldField, mapping, new SubProgressMonitor(subsub, 1), status, flags);
									rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newField, ASTNodes.getInsertionIndex(newField, declaration.bodyDeclarations()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PULL_UP));
									ImportRewriteUtil.addImports(rewrite, oldField.getParent(), new HashMap(), new HashMap(), false);
								}
							} else if (member instanceof IMethod) {
								final MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, root);
								if (oldMethod != null) {
									MethodDeclaration newMethod= createNewMethodDeclarationNode(sourceRewriter, rewrite, ((IMethod) member), oldMethod, root, mapping, adjustments, new SubProgressMonitor(subsub, 1), status);
									rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, declaration.bodyDeclarations()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PULL_UP));
									ImportRewriteUtil.addImports(rewrite, oldMethod, new HashMap(), new HashMap(), false);
								}
							} else if (member instanceof IType) {
								final AbstractTypeDeclaration oldType= ASTNodeSearchUtil.getAbstractTypeDeclarationNode((IType) member, root);
								if (oldType != null) {
									BodyDeclaration newType= createNewTypeDeclarationNode(((IType) member), oldType, root, mapping, rewriter);
									rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newType, ASTNodes.getInsertionIndex(newType, declaration.bodyDeclarations()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PULL_UP));
									ImportRewriteUtil.addImports(rewrite, oldType, new HashMap(), new HashMap(), false);
								}
							} else
								Assert.isTrue(false);
							subsub.worked(1);
						}
						subsub.done();
						for (int offset= 0; offset < fMethodsToDeclareAbstract.length; offset++)
							createAbstractMethod(fMethodsToDeclareAbstract[offset], sourceRewriter, root, declaration, mapping, rewrite, adjustments, new SubProgressMonitor(sub, 1), status);
					} else
						sub.worked(2);
					if (unit.equals(sourceRewriter.getCu())) {
						final IProgressMonitor subsub= new SubProgressMonitor(sub, 1);
						subsub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, fMethodsToDeclareAbstract.length * 2);
						IMethod method= null;
						for (int offset= 0; offset < fMethodsToDeclareAbstract.length; offset++) {
							method= fMethodsToDeclareAbstract[offset];
							adjustor= new MemberVisibilityAdjustor(targetClass, method);
							adjustor.setRewrite(sourceRewriter.getASTRewrite(), root);
							adjustor.setRewrites(rewrites);

							// TW: set to error if bug 78387 is fixed
							adjustor.setFailureSeverity(RefactoringStatus.WARNING);

							adjustor.setStatus(status);
							adjustor.setAdjustments(adjustments);
							if (needsVisibilityAdjustment(method, false, new SubProgressMonitor(subsub, 1), status))
								adjustments.put(method, new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(method, Modifier.ModifierKeyword.PROTECTED_KEYWORD, RefactoringStatus.createWarningStatus(Messages.format(RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_method_warning, new String[] { MemberVisibilityAdjustor.getLabel(method), RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_protected}), JavaStatusContext.create(method))));
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
			if (fReplace) {
				for (final Iterator iterator= rewrites.keySet().iterator(); iterator.hasNext();) {
					unit= (ICompilationUnit) iterator.next();
					rewrite= (CompilationUnitRewrite) rewrites.get(unit);
					if (rewrite != null)
						manager.manage(unit, rewrite.createChange(false, null));
				}
				TextEdit edit= null;
				TextChange change= null;
				Map workingcopies= new HashMap();
				IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
				try {
					subMonitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, rewrites.keySet().size());
					for (final Iterator iterator= rewrites.keySet().iterator(); iterator.hasNext();) {
						unit= (ICompilationUnit) iterator.next();
						change= manager.get(unit);
						if (change != null) {
							edit= change.getEdit();
							if (edit != null) {
								final ICompilationUnit copy= createWorkingCopy(unit, edit, status, new SubProgressMonitor(monitor, 1));
								if (copy != null)
									workingcopies.put(unit, copy);
							}
						}
					}
					final ICompilationUnit current= (ICompilationUnit) workingcopies.get(sourceRewriter.getCu());
					if (current != null)
						rewriteTypeOccurrences(manager, sourceRewriter, current, new HashSet(), status, new SubProgressMonitor(monitor, 1));
				} finally {
					subMonitor.done();
					for (final Iterator iterator= workingcopies.values().iterator(); iterator.hasNext();) {
						((ICompilationUnit) iterator.next()).discardWorkingCopy();
					}
				}
			}
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
				String comment= CodeGeneration.getMethodComment(cu, enclosingTypeName, newMethodNode, false, binding.getName(), fullTypeName, fullParamNames, StubUtility.getLineDelimiterUsed(cu));
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
		if (!getTargetClass().isInterface())
			copyBodyOfPulledUpMethod(sourceRewrite, targetRewrite, sourceMethod, oldMethod, newMethod, mapping, pm);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		copyJavadocNode(rewrite, sourceMethod, oldMethod, newMethod);
		int modifiers= getModifiersWithUpdatedVisibility(sourceMethod, sourceMethod.getFlags(), adjustments, pm, true, status);
		if (oldMethod.isVarargs())
			modifiers&= ~Flags.AccVarargs;
		copyAnnotations(oldMethod, newMethod);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers));
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

	private ICompilationUnit createWorkingCopy(ICompilationUnit unit, TextEdit edit, RefactoringStatus status, IProgressMonitor monitor) {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
			ICompilationUnit copy= ((IPackageFragment) unit.getParent()).getCompilationUnit(unit.getElementName()).getWorkingCopy(fOwner, null, new SubProgressMonitor(monitor, 1));
			IDocument document= new Document(unit.getBuffer().getContents());
			edit.apply(document, TextEdit.UPDATE_REGIONS);
			copy.getBuffer().setContents(document.get());
			JavaModelUtil.reconcile(copy);
			return copy;
		} catch (JavaModelException exception) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
		} catch (MalformedTreeException exception) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
		} catch (BadLocationException exception) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
		} finally {
			monitor.done();
		}
		return null;
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
		pm.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, members.length);// not
		// true,
		// but
		// not
		// easy
		// to
		// give
		// anything
		// better
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

	/**
	 * {@inheritDoc}
	 */
	public Object[] getElements() {
		return fMembersToMove;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIdentifier() {
		return IDENTIFIER;
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
		Map result= new HashMap();// IMember -> Set of IMembers (of the same
		// type as key)
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
		Map result= new HashMap(); // IMember -> Set of IMembers (of the same
		// type as key)
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
			IMember[] typesToDelete= getMembersOfType(fMembersToMove, IJavaElement.TYPE);
			IMember[] matchingElements= getMatchingElements(pm, false);
			IMember[] matchingFields= getMembersOfType(matchingElements, IJavaElement.FIELD);
			return JavaElementUtil.merge(JavaElementUtil.merge(matchingFields, typesToDelete), fMethodsToDelete);
		} finally {
			pm.done();
		}
	}

	private int getModifiersWithUpdatedVisibility(IMember member, int modifiers, Map adjustments, IProgressMonitor monitor, boolean considerReferences, RefactoringStatus status) throws JavaModelException {
		if (needsVisibilityAdjustment(member, considerReferences, monitor, status)) {
			final MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment adjustment= new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(member, Modifier.ModifierKeyword.PROTECTED_KEYWORD, RefactoringStatus.createWarningStatus(Messages.format(MemberVisibilityAdjustor.getMessage(member), new String[] { MemberVisibilityAdjustor.getLabel(member), MemberVisibilityAdjustor.getLabel(Modifier.ModifierKeyword.PROTECTED_KEYWORD)})));
			adjustment.setNeedsRewriting(false);
			adjustments.put(member, adjustment);
			return JdtFlags.clearAccessModifiers(modifiers) | Modifier.PROTECTED;
		}
		return modifiers;
	}

	private Set getNotDeletedMembers(IProgressMonitor pm) throws JavaModelException {
		Set matchingSet= new HashSet();
		pm.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 2);
		matchingSet.addAll(Arrays.asList(getMatchingElements(new SubProgressMonitor(pm, 1), true)));
		matchingSet.removeAll(Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1))));
		pm.done();
		return matchingSet;
	}

	public IType[] getPossibleTargetClasses(RefactoringStatus status, IProgressMonitor pm) throws JavaModelException {
		IType[] superClasses= getDeclaringType().newSupertypeHierarchy(pm).getAllSupertypes(getDeclaringType());
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
			status.addFatalError(RefactoringCoreMessages.PullUPRefactoring_not_java_lang_object);
		else if (superClasses.length == binary)
			status.addFatalError(RefactoringCoreMessages.PullUPRefactoring_no_all_binary);

		Collections.reverse(superClassList);
		return (IType[]) superClassList.toArray(new IType[superClassList.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getProcessorName() {
		return RefactoringCoreMessages.PullUpRefactoring_Pull_Up;
	}

	public IMember[] getPullableMembersOfDeclaringType() {
		try {
			return RefactoringAvailabilityTester.getPullUpMembers(getDeclaringType());
		} catch (JavaModelException e) {
			return new IMember[0];
		}
	}

	// skipped super classes are those declared in the hierarchy between the
	// declaring type of the selected members
	// and the target type
	private Set getSkippedSuperclasses(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
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

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			String handle= extended.getAttribute(JavaRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaElement element= JavaRefactoringDescriptor.handleToElement(extended.getProject(), handle);
				if (element == null || element.getElementType() != IJavaElement.TYPE)
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_input_not_exists, ID_PULL_UP));
				else
					fTargetType= (IType) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptor.ATTRIBUTE_INPUT));
			handle= extended.getAttribute(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1);
			if (handle != null) {
				final IJavaElement element= JavaRefactoringDescriptor.handleToElement(extended.getProject(), handle);
				if (element == null || element.getElementType() != IJavaElement.TYPE)
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_input_not_exists, ID_PULL_UP));
				else
					fDeclaringType= (IType) element;
			}
			final String instance= extended.getAttribute(ATTRIBUTE_STUBS);
			if (instance != null) {
				fCreateMethodStubs= Boolean.valueOf(instance).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_STUBS));
			int pullCount= 0;
			int abstractCount= 0;
			int deleteCount= 0;
			String value= extended.getAttribute(ATTRIBUTE_ABSTRACT);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					abstractCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ABSTRACT));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ABSTRACT));
			value= extended.getAttribute(ATTRIBUTE_DELETE);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					deleteCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE));
			value= extended.getAttribute(ATTRIBUTE_PULL);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					pullCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PULL));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PULL));
			final RefactoringStatus status= new RefactoringStatus();
			List elements= new ArrayList();
			for (int index= 0; index < pullCount; index++) {
				final String attribute= JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 2);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaElement element= JavaRefactoringDescriptor.handleToElement(extended.getProject(), handle);
					if (element == null)
						status.merge(RefactoringStatus.createWarningStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_input_not_exists, ID_PULL_UP)));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fMembersToMove= (IMember[]) elements.toArray(new IMember[elements.size()]);
			elements= new ArrayList();
			for (int index= 0; index < deleteCount; index++) {
				final String attribute= JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (pullCount + index + 2);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaElement element= JavaRefactoringDescriptor.handleToElement(extended.getProject(), handle);
					if (element == null)
						status.merge(RefactoringStatus.createWarningStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_input_not_exists, ID_PULL_UP)));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fMethodsToDelete= (IMethod[]) elements.toArray(new IMethod[elements.size()]);
			elements= new ArrayList();
			for (int index= 0; index < abstractCount; index++) {
				final String attribute= JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (pullCount + abstractCount + index + 2);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaElement element= JavaRefactoringDescriptor.handleToElement(extended.getProject(), handle);
					if (element == null)
						status.merge(RefactoringStatus.createWarningStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_input_not_exists, ID_PULL_UP)));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fMethodsToDeclareAbstract= (IMethod[]) elements.toArray(new IMethod[elements.size()]);
			IJavaProject project= null;
			if (fMembersToMove.length > 0)
				project= fMembersToMove[0].getJavaProject();
			fCodeGenerationSettings= JavaPreferencesSettings.getCodeGenerationSettings(project);
			if (!status.isOK())
				return status;
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isPullUpAvailable(fMembersToMove);
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
			IMethod found= JavaModelUtil.findMethodInHierarchy(hierarchy, targetClass, name, paramTypes, isConstructor);
			return found != null && MethodChecks.isVirtual(found);
		} finally {
			pm.done();
		}
	}

	/**
	 * Should occurrences of the type be replaced by the interface?
	 * 
	 * @return <code>true</code> if the should be replaced, <code>false</code>
	 *         otherwise
	 */
	public boolean isReplace() {
		return fReplace;
	}

	private boolean isRequiredPullableMember(List queue, IMember member) throws JavaModelException {
		if (member.getDeclaringType() == null) // not a member
			return false;
		return member.getDeclaringType().equals(getDeclaringType()) && !queue.contains(member) && RefactoringAvailabilityTester.isPullUpAvailable(member);
	}

	private boolean isVirtualAccessibleFromTargetClass(IMethod method, IProgressMonitor pm) throws JavaModelException {
		try {
			return MethodChecks.isVirtual(method) && isDeclaredInTargetClassOrItsSuperclass(method, pm);
		} finally {
			pm.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void rewriteTypeOccurrences(final TextChangeManager manager, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final ICompilationUnit unit, final CompilationUnit node, Set replacements) throws CoreException {
		CompilationUnitRewrite currentRewrite= null;
		final boolean isSubUnit= rewrite.getCu().equals(unit.getPrimary());
		if (isSubUnit)
			currentRewrite= rewrite;
		else
			currentRewrite= new CompilationUnitRewrite(unit, node);
		final Collection collection= (Collection) fTypeOccurrences.get(unit);
		if (collection != null && !collection.isEmpty()) {
			TType estimate= null;
			ISourceConstraintVariable variable= null;
			ITypeConstraintVariable constraint= null;
			for (final Iterator iterator= collection.iterator(); iterator.hasNext();) {
				variable= (ISourceConstraintVariable) iterator.next();
				if (variable instanceof ITypeConstraintVariable) {
					constraint= (ITypeConstraintVariable) variable;
					estimate= (TType) constraint.getData(SuperTypeConstraintsSolver.DATA_TYPE_ESTIMATE);
					if (estimate != null) {
						final CompilationUnitRange range= constraint.getRange();
						if (isSubUnit)
							rewriteTypeOccurrence(range, estimate, requestor, currentRewrite, node, replacements, currentRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence, SET_SUPER_TYPE));
						else {
							final ASTNode result= NodeFinder.perform(node, range.getSourceRange());
							if (result != null)
								rewriteTypeOccurrence(estimate, currentRewrite, result, currentRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence, SET_SUPER_TYPE));
						}
					}
				}
			}
		}
		if (!isSubUnit) {
			final TextChange change= currentRewrite.createChange();
			if (change != null)
				manager.manage(unit, change);
		}
	}

	private void rewriteTypeOccurrences(final TextChangeManager manager, final CompilationUnitRewrite sourceRewrite, final ICompilationUnit copy, final Set replacements, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 20); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.PullUpRefactoring_checking);
			try {
				final IType declaring= getDeclaringType();
				final IJavaProject project= declaring.getJavaProject();
				final ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setWorkingCopyOwner(fOwner);
				parser.setResolveBindings(true);
				parser.setProject(project);
				parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
				parser.createASTs(new ICompilationUnit[] { copy}, new String[0], new ASTRequestor() {

					public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
						try {
							final IType subType= (IType) JavaModelUtil.findInCompilationUnit(unit, declaring);
							final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(subType, node);
							if (subDeclaration != null) {
								final ITypeBinding subBinding= subDeclaration.resolveBinding();
								if (subBinding != null) {
									String name= null;
									ITypeBinding superBinding= null;
									final ITypeBinding[] superBindings= Bindings.getAllSuperTypes(subBinding);
									for (int index= 0; index < superBindings.length; index++) {
										name= superBindings[index].getName();
										if (name.startsWith(fTargetType.getElementName()))
											superBinding= superBindings[index];
									}
									if (superBinding != null) {
										solveSuperTypeConstraints(unit, node, subType, subBinding, superBinding, new SubProgressMonitor(monitor, 14), status);
										if (!status.hasFatalError())
											rewriteTypeOccurrences(manager, this, sourceRewrite, unit, node, replacements, status, new SubProgressMonitor(monitor, 3));
									}
								}
							}
						} catch (JavaModelException exception) {
							JavaPlugin.log(exception);
							status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
						}
					}

					public final void acceptBinding(final String key, final IBinding binding) {
						// Do nothing
					}
				}, new SubProgressMonitor(monitor, 1));
			} finally {

			}
		} finally {
			monitor.done();
		}
	}

	public void setCreateMethodStubs(boolean create) {
		fCreateMethodStubs= create;
	}

	public void setMembersToMove(IMember[] elements) {
		Assert.isNotNull(elements);
		fMembersToMove= (IMember[]) SourceReferenceUtil.sortByOffset(elements);
	}

	public void setMethodsToDeclareAbstract(IMethod[] methods) {
		Assert.isNotNull(methods);
		fMethodsToDeclareAbstract= methods;
	}

	public void setMethodsToDelete(IMethod[] methodsToDelete) {
		Assert.isNotNull(methodsToDelete);
		fMethodsToDelete= methodsToDelete;
	}

	/**
	 * Determines whether occurrences of the type should be replaced by the
	 * interface.
	 * 
	 * @param replace
	 *            <code>true</code> to replace occurrences where possible,
	 *            <code>false</code> otherwise
	 */
	public void setReplace(final boolean replace) {
		fReplace= replace;
	}

	public void setTargetClass(IType targetType) {
		Assert.isNotNull(targetType);
		if (!targetType.equals(fTargetType))
			fCachedTargetClassHierarchy= null;
		fTargetType= targetType;
	}
}