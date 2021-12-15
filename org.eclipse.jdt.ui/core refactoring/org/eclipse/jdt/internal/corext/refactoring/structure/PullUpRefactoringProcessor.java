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
 *     Benjamin Muskalla - 228950: [pull up] exception if target calls super with multiple parameters
 *     Jerome Cambon <jerome.cambon@oracle.com> - [code style] don't generate redundant modifiers "public static final abstract" for interface members - https://bugs.eclipse.org/71627
 *     Microsoft Corporation - read formatting options from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
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
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.ASTVisitor;
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
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CodeGeneration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.PullUpDescriptor;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.MemberVisibilityAdjustor.IncomingMemberVisibilityAdjustment;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsSolver;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;

/**
 * Refactoring processor for the pull up refactoring.
 *
 * @since 3.2
 */
public class PullUpRefactoringProcessor extends HierarchyProcessor {

	/**
	 * AST node visitor which performs the actual mapping.
	 */
	private static class PullUpAstNodeMapper extends TypeVariableMapper {

		/** Are we in an anonymous class declaration? */
		private boolean fAnonymousClassDeclaration= false;

		/** The source compilation unit rewrite to use */
		private final CompilationUnitRewrite fSourceRewriter;

		/** The super reference type */
		private final IType fSuperReferenceType;

		/** The target compilation unit rewrite to use */
		private final CompilationUnitRewrite fTargetRewriter;

		/** Are we in a type declaration statement? */
		private boolean fTypeDeclarationStatement= false;

		/** The binding of the enclosing method */
		private final IMethodBinding fEnclosingMethod;

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
		 * @param enclosing the binding of the enclosing method
		 */
		public PullUpAstNodeMapper(final CompilationUnitRewrite sourceRewriter, final CompilationUnitRewrite targetRewriter, final ASTRewrite rewrite, final IType type, final TypeVariableMaplet[] mapping, final IMethodBinding enclosing) {
			super(rewrite, mapping);
			Assert.isNotNull(rewrite);
			Assert.isNotNull(type);
			fSourceRewriter= sourceRewriter;
			fTargetRewriter= targetRewriter;
			fSuperReferenceType= type;
			fEnclosingMethod= enclosing;
		}

		@Override
		public final void endVisit(final AnonymousClassDeclaration node) {
			fAnonymousClassDeclaration= false;
			super.endVisit(node);
		}

		@Override
		public final void endVisit(final TypeDeclarationStatement node) {
			fTypeDeclarationStatement= false;
			super.endVisit(node);
		}

		@Override
		public final boolean visit(final AnonymousClassDeclaration node) {
			fAnonymousClassDeclaration= true;
			return super.visit(node);
		}

		@Override
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

		@Override
		public final boolean visit(final SimpleType node) {
			if (node.getName().isSimpleName()) {
				ITypeBinding binding= node.resolveBinding();
				if (binding == null) {
					return false;
				}
				CheckTypeNameInTarget check= new CheckTypeNameInTarget(binding);
				try {
					fTargetRewriter.getRoot().accept(check);
				} catch (AbortSearchException e) {
					// do nothing
				}
				if (check.isQualificationRequired() && binding.getPackage() != null && !binding.getPackage().getName().isEmpty()) {
					String newName= binding.getPackage().getName() + "."  + node.getName().getFullyQualifiedName(); //$NON-NLS-1$
					QualifiedType newQualifiedType= (QualifiedType)fRewrite.createStringPlaceholder(newName, ASTNode.QUALIFIED_TYPE);
					fRewrite.replace(node, newQualifiedType, null);
					return false;
				}
			}
			return false;
		}

		@Override
		public final boolean visit(final SuperMethodInvocation node) {
			if (!fAnonymousClassDeclaration && !fTypeDeclarationStatement) {
				final IBinding superBinding= node.getName().resolveBinding();
				if (superBinding instanceof IMethodBinding) {
					final IMethodBinding extended= (IMethodBinding) superBinding;
					if (fEnclosingMethod != null && fEnclosingMethod.overrides(extended))
						return true;
					final ITypeBinding declaringBinding= extended.getDeclaringClass();
					if (declaringBinding != null) {
						final IType type= (IType) declaringBinding.getJavaElement();
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
				final List<Expression> arguments= node.arguments();
				if (arguments != null && arguments.size() > 0) {
					final ListRewrite rewriter= fRewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
					ListRewrite superRewriter= fRewrite.getListRewrite(node, SuperMethodInvocation.ARGUMENTS_PROPERTY);
					ASTNode copyTarget= superRewriter.createCopyTarget(arguments.get(0), arguments.get(arguments.size() - 1));
					rewriter.insertLast(copyTarget, null);
				}
				fRewrite.replace(node, invocation, null);
				if (!fSourceRewriter.getCu().equals(fTargetRewriter.getCu()))
					fSourceRewriter.getImportRemover().registerRemovedNode(node);
				return true;
			}
			return false;
		}

		@Override
		public final boolean visit(final TypeDeclarationStatement node) {
			fTypeDeclarationStatement= true;
			return super.visit(node);
		}

		private class CheckTypeNameInTarget extends ASTVisitor {

			private final ITypeBinding fBinding;
			private boolean fQualificationRequired;

			public CheckTypeNameInTarget(ITypeBinding binding) {
				this.fBinding= binding;
			}

			@Override
			public boolean visit(SimpleType node) {
				if (!node.getName().isSimpleName() || !node.getName().getFullyQualifiedName().equals(fBinding.getName())) {
					return false;
				}
				ITypeBinding binding= node.resolveBinding();
				if (binding != null) {
					if (!binding.isEqualTo(fBinding)) {
						fQualificationRequired= true;
						throw new AbortSearchException();
					}
				}
				return false;
			}

			public boolean isQualificationRequired() {
				return fQualificationRequired;
			}
		}
	}

	protected static final String ATTRIBUTE_ABSTRACT= "abstract"; //$NON-NLS-1$

	protected static final String ATTRIBUTE_DELETE= "delete"; //$NON-NLS-1$

	protected static final String ATTRIBUTE_PULL= "pull"; //$NON-NLS-1$

	protected static final String ATTRIBUTE_STUBS= "stubs"; //$NON-NLS-1$

	private static final String IDENTIFIER= "org.eclipse.jdt.ui.pullUpProcessor"; //$NON-NLS-1$

	/** The pull up group category set */
	private static final GroupCategorySet SET_PULL_UP= new GroupCategorySet(new GroupCategory("org.eclipse.jdt.internal.corext.pullUp", //$NON-NLS-1$
			RefactoringCoreMessages.PullUpRefactoring_category_name, RefactoringCoreMessages.PullUpRefactoring_category_description));

	private static void addMatchingMember(final Map<IMember, Set<IMember>> mapping, final IMember key, final IMember matchingMember) {
		Set<IMember> matchingSet;
		if (mapping.containsKey(key)) {
			matchingSet= mapping.get(key);
		} else {
			matchingSet= new HashSet<>();
			mapping.put(key, matchingSet);
		}
		Assert.isTrue(!matchingSet.contains(matchingMember));
		matchingSet.add(matchingMember);
	}

	private static Block createMethodStub(final MethodDeclaration method, final AST ast, ICompilationUnit cu, ASTRewrite rewrite, String targetTypeName) {
		final Block body= ast.newBlock();
		final Expression expression= ASTNodeFactory.newDefaultExpression(ast, method.getReturnType2(), method.getExtraDimensions());
		try {
			String delimiter= cu.findRecommendedLineSeparator();
			String bodyStatement= ""; //$NON-NLS-1$
			if (expression != null) {
				final ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(expression);
				bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, FormatterProfileManager.getProjectSettings(cu.getJavaProject()));
			}
			String placeHolder= CodeGeneration.getMethodBodyContent(cu, targetTypeName, method.getName().getIdentifier(), false, bodyStatement, delimiter);
			if (placeHolder != null) {
				ReturnStatement todoNode= (ReturnStatement) rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				body.statements().add(todoNode);
			}
		} catch (CoreException e) {
			// return empty body
		}
		return body;
	}

	private static Set<IType> getAffectedSubTypes(final ITypeHierarchy hierarchy, final IType type) throws JavaModelException {
		 IType[] types= null;
		 final boolean isInterface= type.isInterface();
		if (isInterface) {
			 final Collection<IType> remove= new ArrayList<>();
			 final List<IType> list= new ArrayList<>(Arrays.asList(hierarchy.getSubtypes(type)));
			 for (IType element : list) {
				if (element.isInterface())
					remove.add(element);
			}
			 list.removeAll(remove);
			 types= list.toArray(new IType[list.size()]);
		 } else
			 types= hierarchy.getSubclasses(type);
		final Set<IType> result= new HashSet<>();
		for (IType type2 : types) {
			if (!isInterface && JdtFlags.isAbstract(type2))
				result.addAll(getAffectedSubTypes(hierarchy, type2));
			else
				result.add(type2);
		}
		return result;
	}

	private static IMember[] getMembers(final IMember[] members, final int type) {
		final List<IJavaElement> list= Arrays.asList(JavaElementUtil.getElementsOfType(members, type));
		return list.toArray(new IMember[list.size()]);
	}

	private static void mergeMaps(final Map<IMember, Set<IMember>> result, final Map<IMember, Set<IMember>> map) {
		for (final Entry<IMember, Set<IMember>> entry : result.entrySet()) {
			final IMember key= entry.getKey();
			if (map.containsKey(key)) {
				final Set<IMember> resultSet= entry.getValue();
				final Set<IMember> mapSet= map.get(key);
				resultSet.addAll(mapSet);
			}
		}
	}

	private static void upgradeMap(final Map<IMember, Set<IMember>> result, final Map<IMember, Set<IMember>> map) {
		for (final Entry<IMember, Set<IMember>> entry : map.entrySet()) {
			final IMember key= entry.getKey();
			if (!result.containsKey(key)) {
				final Set<IMember> mapSet= entry.getValue();
				final Set<IMember> resultSet= new HashSet<>(mapSet);
				result.put(key, resultSet);
			}
		}
	}

	/** The methods to be declared abstract */
	protected IMethod[] fAbstractMethods= new IMethod[0];

	/** The cached supertype hierarchy of the declaring type */
	private ITypeHierarchy fCachedDeclaringSuperTypeHierarchy;

	/** The cached type hierarchy of the destination type */
	private ITypeHierarchy fCachedDestinationTypeHierarchy;

	/** The cached set of skipped supertypes */
	private Set<IType> fCachedSkippedSuperTypes;

	/** The map of compilation units to compilation unit rewrites */
	protected Map<ICompilationUnit, CompilationUnitRewrite> fCompilationUnitRewrites;

	/** Should method stubs be generated in subtypes? */
	protected boolean fCreateMethodStubs= true;

	/** The methods to be deleted in subtypes */
	protected IMethod[] fDeletedMethods= new IMethod[0];

	/** The destination type */
	protected IType fDestinationType;

	/**
	 * Creates a new pull up refactoring processor.
	 *
	 * @param members
	 *            the members to pull up
	 * @param settings
	 *            the code generation settings
	 */
	public PullUpRefactoringProcessor(final IMember[] members, final CodeGenerationSettings settings) {
		this(members, settings, false);
	}

	/**
	 * Creates a new pull up processor from refactoring arguments.
	 *
	 * @param arguments
	 *            the refactoring arguments
	 * @param status
	 *            the resulting status
	 */
	public PullUpRefactoringProcessor(JavaRefactoringArguments arguments, RefactoringStatus status) {
		this(null, null, false);
		RefactoringStatus initializeStatus= initialize(arguments);
		status.merge(initializeStatus);
	}

	/**
	 * Creates a new pull up refactoring processor.
	 *
	 * @param members
	 *            the members to pull up, or <code>null</code> if invoked by
	 *            scripting
	 * @param settings
	 *            the code generation settings, or <code>null</code> if
	 *            invoked by scripting
	 * @param layer
	 *            <code>true</code> to create a working copy layer,
	 *            <code>false</code> otherwise
	 */
	protected PullUpRefactoringProcessor(final IMember[] members, final CodeGenerationSettings settings, final boolean layer) {
		super(members, settings, layer);
		if (members != null) {
			final IType type= RefactoringAvailabilityTester.getTopLevelType(fMembersToMove);
			try {
				if (type != null && RefactoringAvailabilityTester.getPullUpMembers(type).length != 0) {
					fCachedDeclaringType= RefactoringAvailabilityTester.getTopLevelType(fMembersToMove);
					fMembersToMove= new IMember[0];
				}
			} catch (JavaModelException exception) {
				JavaPlugin.log(exception);
			}
		}
	}

	private void addAllRequiredPullableMembers(final List<IMember> queue, final IMember member, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(queue);
		Assert.isNotNull(member);
		Assert.isNotNull(monitor);
		SubProgressMonitor sub= null;
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, 6);

			final IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[] { member}, fOwner, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			boolean isStatic= false;
			try {
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, requiredMethods.length);
				isStatic= JdtFlags.isStatic(member);
				for (IMethod requiredMethod : requiredMethods) {
					if (isStatic && !JdtFlags.isStatic(requiredMethod))
						continue;
					if (isRequiredPullableMember(queue, requiredMethod)
							&& (!MethodChecks.isVirtual(requiredMethod) || !isAvailableInDestination(requiredMethod, new SubProgressMonitor(sub, 1))))
						queue.add(requiredMethod);
				}
			} finally {
				sub.done();
			}
			final IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[] { member}, fOwner, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			try {
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, requiredFields.length);
				isStatic= JdtFlags.isStatic(member);
				for (final IField requiredField : requiredFields) {
					if (isStatic && !JdtFlags.isStatic(requiredField))
						continue;
					if (isRequiredPullableMember(queue, requiredField))
						queue.add(requiredField);
				}
			} finally {
				sub.done();
			}
			final IType[] requiredTypes= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[] { member}, fOwner, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			try {
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, requiredMethods.length);
				isStatic= JdtFlags.isStatic(member);
				for (final IType requiredType : requiredTypes) {
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

	private void addMethodStubForAbstractMethod(final IMethod sourceMethod, final CompilationUnit declaringCuNode, final AbstractTypeDeclaration typeToCreateStubIn, final ICompilationUnit newCu, final CompilationUnitRewrite rewriter, final Map<IMember, IncomingMemberVisibilityAdjustment> adjustments, final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		final MethodDeclaration methodToCreateStubFor= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		final AST ast= rewriter.getRoot().getAST();
		final MethodDeclaration newMethod= ast.newMethodDeclaration();
		ASTRewrite astRewrite= rewriter.getASTRewrite();
		newMethod.setBody(createMethodStub(methodToCreateStubFor, ast, newCu, astRewrite, Bindings.getFullyQualifiedName(typeToCreateStubIn.resolveBinding())));
		newMethod.setConstructor(false);
		copyExtraDimensions(methodToCreateStubFor, newMethod);
		int modifiers= getModifiersWithUpdatedVisibility(sourceMethod, JdtFlags.clearFlag(Modifier.NATIVE | Modifier.ABSTRACT, methodToCreateStubFor.getModifiers()), adjustments,
				new SubProgressMonitor(monitor, 1), false, status);
		if (getDestinationType().isInterface()) {
			modifiers= modifiers | Modifier.PUBLIC;
		}
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers));
		newMethod.setName(((SimpleName) ASTNode.copySubtree(ast, methodToCreateStubFor.getName())));
		final TypeVariableMaplet[] mapping= TypeVariableUtil.composeMappings(TypeVariableUtil.subTypeToSuperType(getDeclaringType(), getDestinationType()), TypeVariableUtil.superTypeToInheritedType(getDestinationType(), ((IType) typeToCreateStubIn.resolveBinding().getJavaElement())));
		copyReturnType(astRewrite, getDeclaringType().getCompilationUnit(), methodToCreateStubFor, newMethod, mapping);
		copyParameters(astRewrite, getDeclaringType().getCompilationUnit(), methodToCreateStubFor, newMethod, mapping);
		copyThrownExceptions(methodToCreateStubFor, newMethod);
		copyTypeParameters(methodToCreateStubFor, newMethod);
		newMethod.setJavadoc(createJavadocForStub(typeToCreateStubIn.getName().getIdentifier(), methodToCreateStubFor, newMethod, newCu, astRewrite));
		ImportRewrite importRewrite= rewriter.getImportRewrite();
		ImportRewriteContext context= new ContextSensitiveImportRewriteContext(typeToCreateStubIn, importRewrite);
		ImportRewriteUtil.addImports(rewriter, context, methodToCreateStubFor, new HashMap<Name, String>(), new HashMap<Name, String>(), true);
		IJavaProject javaProject= newCu.getJavaProject();
		CodeGenerationSettings codeGenerationSettings= JavaPreferencesSettings.getCodeGenerationSettings(javaProject);
		StubUtility2Core.addOverrideAnnotation(codeGenerationSettings, javaProject, astRewrite, importRewrite, newMethod, getDeclaringType().isInterface(),
				rewriter.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_override_annotation, SET_PULL_UP));
		astRewrite.getListRewrite(typeToCreateStubIn, typeToCreateStubIn.getBodyDeclarationsProperty()).insertAt(newMethod, BodyDeclarationRewrite.getInsertionIndex(newMethod, typeToCreateStubIn.bodyDeclarations()), rewriter.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_method_stub, SET_PULL_UP));
	}

	private void addNecessaryMethodStubs(final List<IType> affected, final CompilationUnit root, final CompilationUnitRewrite unitRewriter, final Map<IMember, IncomingMemberVisibilityAdjustment> adjustments, final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		final IType declaringType= getDeclaringType();
		final IMethod[] methods= getAbstractMethods();
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, affected.size());
			for (IType type : affected) {
				if (type.equals(declaringType))
					continue;
				final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unitRewriter.getRoot());
				final ICompilationUnit unit= type.getCompilationUnit();
				final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
				try {
					subMonitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, methods.length);
					for (final IMethod method : methods) {
						if (null == JavaModelUtil.findMethod(method.getElementName(), method.getParameterTypes(), method.isConstructor(), type)) {
							addMethodStubForAbstractMethod(method, root, declaration, unit, unitRewriter, adjustments, new SubProgressMonitor(subMonitor, 1), status);
						}
					}
					subMonitor.done();
				} finally {
					subMonitor.done();
				}
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	protected boolean canBeAccessedFrom(final IMember member, final IType target, final ITypeHierarchy hierarchy) throws JavaModelException {
		if (super.canBeAccessedFrom(member, target, hierarchy)) {
			if (target.isInterface())
				return true;
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
				final IType type= member.getDeclaringType();
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

	private RefactoringStatus checkAccessedFields(final IProgressMonitor monitor, final ITypeHierarchy hierarchy) throws JavaModelException {
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 2);
		final RefactoringStatus result= new RefactoringStatus();

		final List<IMember> pulledUpList= Arrays.asList(fMembersToMove);
		final List<IMember> deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(monitor, 1)));
		final IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fMembersToMove, fOwner, new SubProgressMonitor(monitor, 1));

		final IType destination= getDestinationType();
		for (final IField field : accessedFields) {
			if (!field.exists())
				continue;

			boolean isAccessible= pulledUpList.contains(field) || deletedList.contains(field) || canBeAccessedFrom(field, destination, hierarchy) || Flags.isEnum(field.getFlags());
			if (!isAccessible) {
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_field_not_accessible, new String[] { JavaElementLabels.getTextLabel(field, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED)});
				result.addError(message, JavaStatusContext.create(field));
			} else if (getSkippedSuperTypes(new SubProgressMonitor(monitor, 1)).contains(field.getDeclaringType())) {
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_field_cannot_be_accessed, new String[] { JavaElementLabels.getTextLabel(field, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED)});
				result.addError(message, JavaStatusContext.create(field));
			}
		}
		monitor.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethods(final IProgressMonitor monitor, final ITypeHierarchy hierarchy) throws JavaModelException {
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 2);
		final RefactoringStatus result= new RefactoringStatus();

		final List<IMember> pulledUpList= Arrays.asList(fMembersToMove);
		final List<IMethod> declaredAbstractList= Arrays.asList(fAbstractMethods);
		final List<IMember> deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(monitor, 1)));
		final IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fMembersToMove, fOwner, new SubProgressMonitor(monitor, 1));

		final IType destination= getDestinationType();
		for (final IMethod method : accessedMethods) {
			if (!method.exists())
				continue;
			boolean isAccessible= pulledUpList.contains(method) || deletedList.contains(method) || declaredAbstractList.contains(method) || canBeAccessedFrom(method, destination, hierarchy);
			if (!isAccessible) {
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_method_not_accessible, new String[] { JavaElementLabels.getTextLabel(method, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED)});
				result.addError(message, JavaStatusContext.create(method));
			} else if (getSkippedSuperTypes(new SubProgressMonitor(monitor, 1)).contains(method.getDeclaringType())) {
				final String[] keys= { JavaElementLabels.getTextLabel(method, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED)};
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_method_cannot_be_accessed, keys);
				result.addError(message, JavaStatusContext.create(method));
			}
		}
		monitor.done();
		return result;
	}

	private RefactoringStatus checkAccessedTypes(final IProgressMonitor monitor, final ITypeHierarchy hierarchy) throws JavaModelException {
		final RefactoringStatus result= new RefactoringStatus();
		final IType destination= getDestinationType();
		final List<IMember> pulledUpList= Arrays.asList(fMembersToMove);
		for (final IType type : getTypesReferencedInMovedMembers(monitor)) {
			if (!type.exists())
				continue;

			if (!canBeAccessedFrom(type, destination, hierarchy) && !pulledUpList.contains(type)) {
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_type_not_accessible, new String[] { JavaElementLabels.getTextLabel(type, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_FULLY_QUALIFIED)});
				result.addError(message, JavaStatusContext.create(type));
			}
		}
		monitor.done();
		return result;
	}

	private RefactoringStatus checkAccesses(final IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus result= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 4);
			final ITypeHierarchy hierarchy= getDestinationType().newSupertypeHierarchy(fOwner, new SubProgressMonitor(monitor, 1));
			result.merge(checkAccessedTypes(new SubProgressMonitor(monitor, 1), hierarchy));
			result.merge(checkAccessedFields(new SubProgressMonitor(monitor, 1), hierarchy));
			result.merge(checkAccessedMethods(new SubProgressMonitor(monitor, 1), hierarchy));
		} finally {
			monitor.done();
		}
		return result;
	}

	private void checkAccessModifiers(final RefactoringStatus result, final Set<IMember> notDeletedMembersInSubtypes) throws JavaModelException {
		if (fDestinationType.isInterface())
			return;
		final List<IMethod> toDeclareAbstract= Arrays.asList(fAbstractMethods);
		for (IMember member : notDeletedMembersInSubtypes) {
			if (member.getElementType() == IJavaElement.METHOD && !toDeclareAbstract.contains(member)) {
				final IMethod method= ((IMethod) member);
				if (method.getDeclaringType().getPackageFragment().equals(fDestinationType.getPackageFragment())) {
					if (JdtFlags.isPrivate(method))
						result.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_lower_default_visibility, new String[] { JavaElementLabels.getTextLabel(method, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(method.getDeclaringType(), JavaElementLabels.ALL_FULLY_QUALIFIED)}), JavaStatusContext.create(method));
				} else if (!JdtFlags.isPublic(method) && !JdtFlags.isProtected(method))
					result.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_lower_protected_visibility, new String[] { JavaElementLabels.getTextLabel(method, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(method.getDeclaringType(), JavaElementLabels.ALL_FULLY_QUALIFIED)}), JavaStatusContext.create(method));
			}
		}
	}

	protected RefactoringStatus checkDeclaringSuperTypes(final IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus result= new RefactoringStatus();
		if (getCandidateTypes(result, monitor).length == 0 && !result.hasFatalError()) {
			final String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_not_this_type, new String[] { JavaElementLabels.getTextLabel(getDeclaringType(), JavaElementLabels.ALL_FULLY_QUALIFIED)});
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		return result;
	}

	@Override
	protected RefactoringStatus checkDeclaringType(final IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus status= super.checkDeclaringType(monitor);
		if ("java.lang.Object".equals(getDeclaringType().getFullyQualifiedName('.'))) //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PullUpRefactoring_no_java_lang_Object));
		status.merge(checkDeclaringSuperTypes(monitor));
		return status;
	}

	private void checkFieldTypes(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		final Map<IMember, Set<IMember>> mapping= getMatchingMembers(getDestinationTypeHierarchy(monitor), getDestinationType(), true);
		for (IMember element : fMembersToMove) {
			if (element.getElementType() != IJavaElement.FIELD)
				continue;
			final IField field= (IField) element;
			final String type= Signature.toString(field.getTypeSignature());
			Assert.isTrue(mapping.containsKey(field));
			for (IMember member : mapping.get(field)) {
				final IField matchingField= (IField) member;
				if (field.equals(matchingField))
					continue;
				if (type.equals(Signature.toString(matchingField.getTypeSignature())))
					continue;
				final String[] keys= { JavaElementLabels.getTextLabel(matchingField, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(matchingField.getDeclaringType(), JavaElementLabels.ALL_FULLY_QUALIFIED)};
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_different_field_type, keys);
				final RefactoringStatusContext context= JavaStatusContext.create(matchingField.getCompilationUnit(), matchingField.getSourceRange());
				status.addError(message, context);
			}
		}
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 12);
			clearCaches();

			final RefactoringStatus result= new RefactoringStatus();
			result.merge(createWorkingCopyLayer(new SubProgressMonitor(monitor, 4)));
			if (result.hasFatalError())
				return result;
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			result.merge(checkGenericDeclaringType(new SubProgressMonitor(monitor, 1)));
			result.merge(checkFinalFields(new SubProgressMonitor(monitor, 1)));
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			result.merge(checkAccesses(new SubProgressMonitor(monitor, 1)));
			result.merge(checkMembersInTypeAndAllSubtypes(new SubProgressMonitor(monitor, 2)));
			result.merge(checkIfSkippingOverElements(new SubProgressMonitor(monitor, 1)));
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			if (!JdtFlags.isAbstract(getDestinationType()) && getAbstractMethods().length > 0)
				result.merge(checkConstructorCalls(getDestinationType(), new SubProgressMonitor(monitor, 1)));
			else
				monitor.worked(1);
			if (result.hasFatalError())
				return result;
			fCompilationUnitRewrites= new HashMap<>(3);
			result.merge(checkProjectCompliance(getCompilationUnitRewrite(fCompilationUnitRewrites, getDeclaringType().getCompilationUnit()), getDestinationType(), fMembersToMove));
			fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 1), result);

			Checks.addModifiedFilesToChecker(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), context);

			return result;
		} finally {
			monitor.done();
		}
	}

	protected RefactoringStatus checkFinalFields(final IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus result= new RefactoringStatus();
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, fMembersToMove.length);
		for (final IMember member : fMembersToMove) {
			if (member.getElementType() == IJavaElement.FIELD) {
				if (!JdtFlags.isStatic(member)) {
					if (JdtFlags.isFinal(member)) {
						final RefactoringStatusContext context= JavaStatusContext.create(member);
						result.addWarning(RefactoringCoreMessages.PullUpRefactoring_final_fields, context);
					} else if (getDestinationType().isInterface()) {
						final RefactoringStatusContext context= JavaStatusContext.create(member);
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

		final RefactoringStatus status= new RefactoringStatus();
		try {
			final IMember[] pullables= getMembersToMove();
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, pullables.length);

			final IType declaring= getDeclaringType();
			final ITypeParameter[] parameters= declaring.getTypeParameters();
			if (parameters.length > 0) {
				final TypeVariableMaplet[] mapping= TypeVariableUtil.subTypeToInheritedType(declaring);
				IMember member= null;
				int length= 0;
				for (IMember pullable : pullables) {
					member= pullable;
					final String[] unmapped= TypeVariableUtil.getUnmappedVariables(mapping, declaring, member);
					length= unmapped.length;

					String superClassLabel= BasicElementLabels.getJavaElementName(declaring.getSuperclassName());
					switch (length) {
						case 0:
							break;
						case 1:
							status.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_variable_not_available, new String[] { unmapped[0], superClassLabel}), JavaStatusContext.create(member));
							break;
						case 2:
							status.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_variable2_not_available, new String[] { unmapped[0], unmapped[1], superClassLabel}), JavaStatusContext.create(member));
							break;
						case 3:
							status.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_variable3_not_available, new String[] { unmapped[0], unmapped[1], unmapped[2], superClassLabel}), JavaStatusContext.create(member));
							break;
						default:
							status.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_variables_not_available, new String[] { superClassLabel}), JavaStatusContext.create(member));
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

	private RefactoringStatus checkIfDeclaredIn(final IMember element, final IType type) throws JavaModelException {
		if (element instanceof IMethod)
			return checkIfMethodDeclaredIn((IMethod) element, type);
		else if (element instanceof IField)
			return checkIfFieldDeclaredIn((IField) element, type);
		else if (element instanceof IType)
			return checkIfTypeDeclaredIn((IType) element, type);
		Assert.isTrue(false);
		return null;
	}

	private RefactoringStatus checkIfFieldDeclaredIn(final IField iField, final IType type) throws JavaModelException {
		final IField fieldInType= type.getField(iField.getElementName());
		if (!fieldInType.exists() || !iField.getTypeSignature().equals(fieldInType.getTypeSignature())) {
			return null;
		}
		final String[] keys= { JavaElementLabels.getTextLabel(fieldInType, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(type, JavaElementLabels.ALL_FULLY_QUALIFIED)};
		final String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_Field_declared_in_class, keys);
		final RefactoringStatusContext context= JavaStatusContext.create(fieldInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfMethodDeclaredIn(final IMethod iMethod, final IType type) throws JavaModelException {
		final IMethod methodInType= JavaModelUtil.findMethod(iMethod.getElementName(), iMethod.getParameterTypes(), iMethod.isConstructor(), type);
		if (methodInType == null || !methodInType.exists())
			return null;
		final String[] keys= { JavaElementLabels.getTextLabel(methodInType, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(type, JavaElementLabels.ALL_FULLY_QUALIFIED)};
		final String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_Method_declared_in_class, keys);
		final RefactoringStatusContext context= JavaStatusContext.create(methodInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfSkippingOverElements(final IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
		try {
			final Set<IType> skippedTypes= getSkippedSuperTypes(new SubProgressMonitor(monitor, 1));
			final IType[] skipped= skippedTypes.toArray(new IType[skippedTypes.size()]);
			final RefactoringStatus result= new RefactoringStatus();
			for (final IMember element : fMembersToMove) {
				for (IType element2 : skipped) {
					result.merge(checkIfDeclaredIn(element, element2));
				}
			}
			return result;
		} finally {
			monitor.done();
		}
	}

	private RefactoringStatus checkIfTypeDeclaredIn(final IType iType, final IType type) {
		final IType typeInType= type.getType(iType.getElementName());
		if (!typeInType.exists())
			return null;
		final String[] keys= { JavaElementLabels.getTextLabel(typeInType, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(type, JavaElementLabels.ALL_FULLY_QUALIFIED)};
		final String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_declared_in_class, keys);
		final RefactoringStatusContext context= JavaStatusContext.create(typeInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	@Override
	public RefactoringStatus checkInitialConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
			final RefactoringStatus status= new RefactoringStatus();
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

	private void checkMembersInDestinationType(final RefactoringStatus status, final Set<IMember> set) throws JavaModelException {
		final IMember[] destinationMembers= getCreatedDestinationMembers();
		final List<IMember> list= new ArrayList<>(destinationMembers.length);
		list.addAll(Arrays.asList(destinationMembers));
		list.addAll(set);
		list.removeAll(Arrays.asList(fDeletedMethods));
		final IMember[] members= list.toArray(new IMember[list.size()]);
		status.merge(MemberCheckUtil.checkMembersInDestinationType(members, getDestinationType()));
	}

	private RefactoringStatus checkMembersInTypeAndAllSubtypes(final IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus result= new RefactoringStatus();
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 3);
		final Set<IMember> notDeletedMembers= getNotDeletedMembers(new SubProgressMonitor(monitor, 1));
		final Set<IMember> notDeletedMembersInTargetType= new HashSet<>();
		final Set<IMember> notDeletedMembersInSubtypes= new HashSet<>();
		for (IMember member : notDeletedMembers) {
			if (getDestinationType().equals(member.getDeclaringType()))
				notDeletedMembersInTargetType.add(member);
			else
				notDeletedMembersInSubtypes.add(member);
		}
		checkMembersInDestinationType(result, notDeletedMembersInTargetType);
		checkAccessModifiers(result, notDeletedMembersInSubtypes);
		checkMethodReturnTypes(new SubProgressMonitor(monitor, 1), result, notDeletedMembersInSubtypes);
		checkFieldTypes(new SubProgressMonitor(monitor, 1), result);
		monitor.done();
		return result;
	}

	private void checkMethodReturnTypes(final IProgressMonitor monitor, final RefactoringStatus status, final Set<IMember> notDeletedMembersInSubtypes) throws JavaModelException {
		final Map<IMember, Set<IMember>> mapping= getMatchingMembers(getDestinationTypeHierarchy(monitor), getDestinationType(), true);
		for (IMember member : getCreatedDestinationMembers()) {
			if (member.getElementType() != IJavaElement.METHOD)
				continue;
			final IMethod method= (IMethod) member;
			if (mapping.containsKey(method)) {
				final Set<IMember> set= mapping.get(method);
				if (set != null) {
					final String returnType= Signature.toString(Signature.getReturnType(method.getSignature()).toString());
					for (IMember iMember : set) {
						final IMethod matchingMethod= (IMethod) iMember;
						if (method.equals(matchingMethod))
							continue;
						if (!notDeletedMembersInSubtypes.contains(matchingMethod))
							continue;
						if (returnType.equals(Signature.toString(Signature.getReturnType(matchingMethod.getSignature()).toString())))
							continue;
						final String[] keys= { JavaElementLabels.getTextLabel(matchingMethod, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(matchingMethod.getDeclaringType(), JavaElementLabels.ALL_FULLY_QUALIFIED)};
						final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_different_method_return_type, keys);
						final RefactoringStatusContext context= JavaStatusContext.create(matchingMethod.getCompilationUnit(), matchingMethod.getNameRange());
						status.addError(message, context);
					}
				}
			}
		}
	}

	@Override
	protected void clearCaches() {
		super.clearCaches();
		fCachedMembersReferences.clear();
		fCachedDestinationTypeHierarchy= null;
		fCachedDeclaringSuperTypeHierarchy= null;
	}

	private void copyBodyOfPulledUpMethod(final CompilationUnitRewrite sourceRewrite, final CompilationUnitRewrite targetRewrite, final IMethod method, final MethodDeclaration oldMethod, final MethodDeclaration newMethod, final TypeVariableMaplet[] mapping, final IProgressMonitor monitor) throws JavaModelException {
		final Block body= oldMethod.getBody();
		if (body == null) {
			newMethod.setBody(null);
			return;
		}
		try {
			final IDocument document= new Document(method.getCompilationUnit().getBuffer().getContents());
			final ASTRewrite rewrite= ASTRewrite.create(body.getAST());
			final ITrackedNodePosition position= rewrite.track(body);
			body.accept(new PullUpAstNodeMapper(sourceRewrite, targetRewrite, rewrite, getDeclaringSuperTypeHierarchy(monitor).getSuperclass(getDeclaringType()), mapping, oldMethod.resolveBinding()));
			rewrite.rewriteAST(document, method.getCompilationUnit().getOptions(true)).apply(document, TextEdit.NONE);
			String content= document.get(position.getStartPosition(), position.getLength());
			final String[] lines= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, method.getCompilationUnit(), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.getASTRewrite().createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException | BadLocationException exception) {
			JavaPlugin.log(exception);
		}
	}

	private void createAbstractMethod(final IMethod sourceMethod, final CompilationUnitRewrite sourceRewriter, final CompilationUnit declaringCuNode, final AbstractTypeDeclaration destination, final TypeVariableMaplet[] mapping, final CompilationUnitRewrite targetRewrite, final Map<IMember, IncomingMemberVisibilityAdjustment> adjustments, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		final MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		ITypeBinding destinationBinding= destination.resolveBinding();
		StubUtility2Core.addOverrideAnnotation(fSettings, sourceMethod.getJavaProject(), sourceRewriter.getASTRewrite(), sourceRewriter.getImportRewrite(), oldMethod,
				destinationBinding == null ? false : destinationBinding.isInterface(),
				sourceRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_override_annotation, SET_PULL_UP));
		final MethodDeclaration newMethod= targetRewrite.getAST().newMethodDeclaration();
		newMethod.setBody(null);
		newMethod.setConstructor(false);
		copyExtraDimensions(oldMethod, newMethod);

		final Javadoc oldJavadoc= oldMethod.getJavadoc();
		if (oldJavadoc != null) {
			String newJavadocString= ASTNodes.getNodeSource(oldJavadoc, false, true);
			if (newJavadocString != null) {
				newMethod.setJavadoc((Javadoc) targetRewrite.getASTRewrite().createStringPlaceholder(newJavadocString, ASTNode.JAVADOC));
			}
		}

		int modifiers= getModifiersWithUpdatedVisibility(sourceMethod, Modifier.ABSTRACT | JdtFlags.clearFlag(Modifier.NATIVE | Modifier.FINAL, sourceMethod.getFlags()), adjustments, monitor, false, status);
		if (oldMethod.isVarargs())
			modifiers&= ~Flags.AccVarargs;
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(targetRewrite.getAST(), modifiers));
		newMethod.setName(((SimpleName) ASTNode.copySubtree(targetRewrite.getAST(), oldMethod.getName())));
		copyReturnType(targetRewrite.getASTRewrite(), getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyParameters(targetRewrite.getASTRewrite(), getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		copyTypeParameters(oldMethod, newMethod);
		ImportRewriteContext context= new ContextSensitiveImportRewriteContext(destination, targetRewrite.getImportRewrite());
		ImportRewriteUtil.addImports(targetRewrite, context, oldMethod, new HashMap<Name, String>(), new HashMap<Name, String>(), true);
		targetRewrite.getASTRewrite().getListRewrite(destination, destination.getBodyDeclarationsProperty()).insertAt(newMethod, org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite.getInsertionIndex(newMethod, destination.bodyDeclarations()), targetRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_abstract_method, SET_PULL_UP));
	}

	@Override
	public Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
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
			final String description= fMembersToMove.length == 1 ? Messages.format(RefactoringCoreMessages.PullUpRefactoring_descriptor_description_short, new String[] { JavaElementLabels.getElementLabel(fMembersToMove[0], JavaElementLabels.ALL_DEFAULT), JavaElementLabels.getElementLabel(fDestinationType, JavaElementLabels.ALL_DEFAULT)}) : Messages.format(RefactoringCoreMessages.PullUpRefactoring_descriptor_description_short_multiple, BasicElementLabels.getJavaElementName(fDestinationType.getElementName()));
			final String header= fMembersToMove.length == 1 ? Messages.format(RefactoringCoreMessages.PullUpRefactoring_descriptor_description_full, new String[] { JavaElementLabels.getElementLabel(fMembersToMove[0], JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getElementLabel(declaring, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getElementLabel(fDestinationType, JavaElementLabels.ALL_FULLY_QUALIFIED)}) : Messages.format(RefactoringCoreMessages.PullUpRefactoring_descriptor_description, new String[] { JavaElementLabels.getElementLabel(declaring, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getElementLabel(fDestinationType, JavaElementLabels.ALL_FULLY_QUALIFIED)});
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			comment.addSetting(Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_target_element_pattern, JavaElementLabels.getElementLabel(fDestinationType, JavaElementLabels.ALL_FULLY_QUALIFIED)));
			addSuperTypeSettings(comment, true);
			final PullUpDescriptor descriptor= RefactoringSignatureDescriptorFactory.createPullUpDescriptor(project, description, comment.asString(), arguments, flags);
			arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT, JavaRefactoringDescriptorUtil.elementToHandle(project, fDestinationType));
			arguments.put(ATTRIBUTE_REPLACE, Boolean.toString(fReplace));
			arguments.put(ATTRIBUTE_INSTANCEOF, Boolean.toString(fInstanceOf));
			arguments.put(ATTRIBUTE_STUBS, Boolean.toString(fCreateMethodStubs));
			arguments.put(ATTRIBUTE_PULL, Integer.toString(fMembersToMove.length));
			for (int offset= 0; offset < fMembersToMove.length; offset++)
				arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (offset + 1), JavaRefactoringDescriptorUtil.elementToHandle(project, fMembersToMove[offset]));
			arguments.put(ATTRIBUTE_DELETE, Integer.toString(fDeletedMethods.length));
			for (int offset= 0; offset < fDeletedMethods.length; offset++)
				arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + 1), JavaRefactoringDescriptorUtil.elementToHandle(project, fDeletedMethods[offset]));
			arguments.put(ATTRIBUTE_ABSTRACT, Integer.toString(fAbstractMethods.length));
			for (int offset= 0; offset < fAbstractMethods.length; offset++)
				arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + fDeletedMethods.length + 1), JavaRefactoringDescriptorUtil.elementToHandle(project, fAbstractMethods[offset]));
			return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.PullUpRefactoring_Pull_Up, fChangeManager.getAllChanges());
		} finally {
			monitor.done();
			clearCaches();
		}
	}

	private TextEditBasedChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 24);
			final ICompilationUnit source= getDeclaringType().getCompilationUnit();
			final IType destination= getDestinationType();
			final ICompilationUnit target= destination.getCompilationUnit();
			final CompilationUnitRewrite sourceRewriter= getCompilationUnitRewrite(fCompilationUnitRewrites, source);
			final CompilationUnitRewrite targetRewriter= getCompilationUnitRewrite(fCompilationUnitRewrites, target);
			final Map<ICompilationUnit, ArrayList<IMember>> deleteMap= createMembersToDeleteMap(new SubProgressMonitor(monitor, 1));
			final Map<ICompilationUnit, ArrayList<IType>> affectedMap= createAffectedTypesMap(new SubProgressMonitor(monitor, 1));
			final ICompilationUnit[] units= getAffectedCompilationUnits(new SubProgressMonitor(monitor, 1));

			final Map<IMember, IncomingMemberVisibilityAdjustment> adjustments= new HashMap<>();
			MemberVisibilityAdjustor adjustor= null;
			final IProgressMonitor sub= new SubProgressMonitor(monitor, 1);
			try {
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, units.length * 11);
				for (ICompilationUnit unit : units) {
					if (!source.equals(unit)
							&& !target.equals(unit)
							&& !deleteMap.containsKey(unit)
							&& !affectedMap.containsKey(unit)) {
						sub.worked(10);
						continue;
					}
					CompilationUnitRewrite rewrite= getCompilationUnitRewrite(fCompilationUnitRewrites, unit);
					if (deleteMap.containsKey(unit)) {
						LinkedList<IMember> list= new LinkedList<>(deleteMap.get(unit));
						if (destination.isInterface()) {
							for (final Iterator<IMember> iterator= list.iterator(); iterator.hasNext();) {
								final IMember member= iterator.next();
								if (member instanceof IMethod)
									iterator.remove();
							}
						}
						deleteDeclarationNodes(sourceRewriter, sourceRewriter.getCu().equals(targetRewriter.getCu()), rewrite, list, SET_PULL_UP);
					}
					final CompilationUnit root= sourceRewriter.getRoot();
					if (unit.equals(target)) {
						final ASTRewrite rewriter= rewrite.getASTRewrite();
						if (!JdtFlags.isAbstract(destination) && !destination.isInterface() && getAbstractMethods().length > 0) {
							final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(destination, rewrite.getRoot());
							ModifierRewrite.create(rewriter, declaration).setModifiers(declaration.getModifiers() | Modifier.ABSTRACT, rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_make_target_abstract, SET_PULL_UP));
						}
						final TypeVariableMaplet[] mapping= TypeVariableUtil.subTypeToSuperType(getDeclaringType(), destination);
						final IProgressMonitor subsub= new SubProgressMonitor(sub, 1);
						final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(destination, rewrite.getRoot());
						ImportRewriteContext context= new ContextSensitiveImportRewriteContext(declaration, rewrite.getImportRewrite());
						fMembersToMove= JavaElementUtil.sortByOffset(fMembersToMove);
						subsub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, fMembersToMove.length);
						IMember member= null;
						for (int offset= fMembersToMove.length - 1; offset >= 0; offset--) {
							member= fMembersToMove[offset];
							adjustor= new MemberVisibilityAdjustor(destination, member);
							adjustor.setRewrite(sourceRewriter.getASTRewrite(), root);

							// TW: set to error if bug 78387 is fixed
							adjustor.setFailureSeverity(RefactoringStatus.WARNING);

							adjustor.setOwner(fOwner);
							adjustor.setRewrites(fCompilationUnitRewrites);
							adjustor.setStatus(status);
							adjustor.setAdjustments(adjustments);
							adjustor.adjustVisibility(new SubProgressMonitor(subsub, 1));
							adjustments.remove(member);
							if (member instanceof IField) {
								final VariableDeclarationFragment oldField= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, root);
								if (oldField != null) {
									int flags= getModifiersWithUpdatedVisibility(member, member.getFlags(), adjustments, new SubProgressMonitor(subsub, 1), true, status);
									final FieldDeclaration newField= createNewFieldDeclarationNode(rewriter, root, (IField) member, oldField, mapping, new SubProgressMonitor(subsub, 1), status, flags);
									rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newField, org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite.getInsertionIndex(newField, declaration.bodyDeclarations()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PULL_UP));
									ImportRewriteUtil.addImports(rewrite, context, oldField.getParent(), new HashMap<Name, String>(), new HashMap<Name, String>(), false);

									if (oldField.getParent() instanceof FieldDeclaration) {
										// set fully qualified type name for pulled up types
										FieldDeclaration vds= (FieldDeclaration) oldField.getParent();
										ITypeBinding oldTypeBinding= vds.getType().resolveBinding();
										if (oldTypeBinding == null || oldTypeBinding.isPrimitive()) {
											continue; // no qualified types for primitives
										}
										String sourcePackage= oldTypeBinding.getPackage() == null ? "" : oldTypeBinding.getPackage().getName(); //$NON-NLS-1$
										String targetPackage= targetRewriter.getRoot().getPackage() == null ? "" : targetRewriter.getRoot().getPackage().getName().toString(); //$NON-NLS-1$
										String targetTypeBinding= targetPackage + "." + oldTypeBinding.getName(); //$NON-NLS-1$

										// Find the same type-name field but fully qualified.
										// In that case it won't shadow the pulled up field
										boolean qualifiedTypeNameInTarget= true;
										String sourceSignature= ((IField) member).getTypeSignature();
										for (IField targetField : fDestinationType.getFields()) {
											if (sourceSignature.equals(targetField.getTypeSignature())) {
												qualifiedTypeNameInTarget= false;
												break;
											}
										}
										//check if same type name is accessible in new package (targetPackage + <type>)
										IType findTargetType= target.getJavaProject().findType(targetTypeBinding);
										if (!qualifiedTypeNameInTarget && findTargetType != null) {
											if (sourcePackage.isEmpty() ^ targetPackage.isEmpty()) {
												// not one package can be default package, either none or both
												status.merge(RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.PullUpRefactoring_moving_fromto_default_package,
														new String[] { JavaElementLabels.getTextLabel(member, JavaElementLabels.ALL_FULLY_QUALIFIED) }), JavaStatusContext.create(member)));
											}

											if (!sourcePackage.isEmpty()) {
												Name newName= rewrite.getAST().newName(sourcePackage);
												SimpleName newSimpleName= rewrite.getAST().newSimpleName(oldTypeBinding.getName());
												SimpleType newSimpleType= null;
												if (targetPackage.equals(sourcePackage)) {
													// if source type is in same package as target then we don't need the fully qualified name
													newSimpleType= rewrite.getAST().newSimpleType(newSimpleName);
												} else {
													QualifiedName newQualifiedTypeName= rewrite.getAST().newQualifiedName(newName, newSimpleName);
													newSimpleType= rewrite.getAST().newSimpleType(newQualifiedTypeName);
												}
												newField.setType(newSimpleType);
											}
										}
									}
								}
							} else if (member instanceof IMethod) {
								final MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, root);
								if (oldMethod != null) {
									if (JdtFlags.isStatic(member) && fDestinationType.isInterface())
										status.merge(RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.PullUpRefactoring_moving_static_method_to_interface, new String[] { JavaElementLabels.getTextLabel(member, JavaElementLabels.ALL_FULLY_QUALIFIED)}), JavaStatusContext.create(member)));
									final MethodDeclaration newMethod= createNewMethodDeclarationNode(sourceRewriter, rewrite, ((IMethod) member), oldMethod, mapping, adjustments, new SubProgressMonitor(subsub, 1), status);
									rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newMethod, org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite.getInsertionIndex(newMethod, declaration.bodyDeclarations()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PULL_UP));
									ImportRewriteUtil.addImports(rewrite, context, oldMethod, new HashMap<Name, String>(), new HashMap<Name, String>(), newMethod.getBody() == null);
								}
							} else if (member instanceof IType) {
								final AbstractTypeDeclaration oldType= ASTNodeSearchUtil.getAbstractTypeDeclarationNode((IType) member, root);
								if (oldType != null) {
									final BodyDeclaration newType= createNewTypeDeclarationNode(((IType) member), oldType, root, mapping, rewriter);
									rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newType, org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite.getInsertionIndex(newType, declaration.bodyDeclarations()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PULL_UP));
									ImportRewriteUtil.addImports(rewrite, context, oldType, new HashMap<Name, String>(), new HashMap<Name, String>(), false);
								}
							} else
								Assert.isTrue(false);
							subsub.worked(1);
						}
						subsub.done();
						for (IMethod abstractMethod : fAbstractMethods)
							createAbstractMethod(abstractMethod, sourceRewriter, root, declaration, mapping, rewrite, adjustments, new SubProgressMonitor(sub, 1), status);
					} else
						sub.worked(2);
					if (unit.equals(sourceRewriter.getCu())) {
						final IProgressMonitor subsub= new SubProgressMonitor(sub, 1);
						subsub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, fAbstractMethods.length * 2);
						for (IMethod method : fAbstractMethods) {
							adjustor= new MemberVisibilityAdjustor(destination, method);
							adjustor.setRewrite(sourceRewriter.getASTRewrite(), root);
							adjustor.setRewrites(fCompilationUnitRewrites);

							// TW: set to error if bug 78387 is fixed
							adjustor.setFailureSeverity(RefactoringStatus.WARNING);

							adjustor.setOwner(fOwner);
							adjustor.setStatus(status);
							adjustor.setAdjustments(adjustments);
							if (destination.isInterface() && !JdtFlags.isPublic(method)) {
								adjustments.put(method, new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(method, Modifier.ModifierKeyword.PUBLIC_KEYWORD, RefactoringStatus.createWarningStatus(Messages.format(RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_method_warning, new String[] { MemberVisibilityAdjustor.getLabel(method), RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_public}), JavaStatusContext.create(method))));
							} else if (needsVisibilityAdjustment(method, false, new SubProgressMonitor(subsub, 1), status)) {
								adjustments.put(method, new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(method, Modifier.ModifierKeyword.PROTECTED_KEYWORD, RefactoringStatus.createWarningStatus(Messages.format(RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_method_warning, new String[] { MemberVisibilityAdjustor.getLabel(method), RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_protected}), JavaStatusContext.create(method))));
							}
						}
					} else
						sub.worked(2);
					if (affectedMap.containsKey(unit))
						addNecessaryMethodStubs(affectedMap.get(unit), root, rewrite, adjustments, new SubProgressMonitor(sub, 2), status);
					if (sub.isCanceled())
						throw new OperationCanceledException();
				}
			} finally {
				sub.done();
			}
			if (adjustor != null && !adjustments.isEmpty())
				adjustor.rewriteVisibility(new SubProgressMonitor(monitor, 1));
			final TextEditBasedChangeManager manager= new TextEditBasedChangeManager();
			if (fReplace) {
				final Set<Entry<ICompilationUnit, CompilationUnitRewrite>> entrySet= fCompilationUnitRewrites.entrySet();
				for (final Entry<ICompilationUnit, CompilationUnitRewrite> entry : entrySet) {
					ICompilationUnit unit= entry.getKey();
					CompilationUnitRewrite rewrite= entry.getValue();
					if (rewrite != null) {
						final CompilationUnitChange change= rewrite.createChange(false);
						if (change != null)
							manager.manage(unit, change);
					}
				}
				TextEdit edit= null;
				TextEditBasedChange change= null;
				final Map<ICompilationUnit, ICompilationUnit> workingcopies= new HashMap<>();
				final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
				try {
					subMonitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, entrySet.size());
					for (final Entry<ICompilationUnit, CompilationUnitRewrite> entry : entrySet) {
						ICompilationUnit unit= entry.getKey();
						change= manager.get(unit);
						if (change instanceof TextChange) {
							edit= ((TextChange) change).getEdit();
							if (edit != null) {
								final ICompilationUnit copy= createWorkingCopy(unit, edit, status, new SubProgressMonitor(monitor, 1));
								if (copy != null)
									workingcopies.put(unit, copy);
							}
						}
					}
					final ICompilationUnit current= workingcopies.get(sourceRewriter.getCu());
					if (current != null)
						rewriteTypeOccurrences(manager, sourceRewriter, current, new HashSet<String>(), status, new SubProgressMonitor(monitor, 16));
				} finally {
					subMonitor.done();
					for (ICompilationUnit unit : manager.getAllCompilationUnits()) {
						CompilationUnitChange current= (CompilationUnitChange) manager.get(unit);
						if (change != null && current.getEdit() == null)
							manager.remove(unit);
					}
				}
			}
			registerChanges(manager);
			return manager;
		} finally {
			fCompilationUnitRewrites.clear();
			monitor.done();
		}
	}

	private Map<ICompilationUnit, ArrayList<IType>> createAffectedTypesMap(final IProgressMonitor monitor) throws JavaModelException {
		if (!fCreateMethodStubs || (getAbstractMethods().length <= 0))
			return new HashMap<>(0);
		final Set<IType> affected= getAffectedSubTypes(getDestinationTypeHierarchy(monitor), getDestinationType());
		final Map<ICompilationUnit, ArrayList<IType>> result= new HashMap<>();
		for (IType type : affected) {
			final ICompilationUnit unit= type.getCompilationUnit();
			if (!result.containsKey(unit))
				result.put(unit, new ArrayList<IType>(1));
			result.get(unit).add(type);
		}
		return result;
	}

	private Javadoc createJavadocForStub(final String enclosingTypeName, final MethodDeclaration oldMethod, final MethodDeclaration newMethodNode, final ICompilationUnit cu, final ASTRewrite rewrite) throws CoreException {
		if (fSettings.createComments) {
			final IMethodBinding binding= oldMethod.resolveBinding();
			if (binding != null) {
				final ITypeBinding[] params= binding.getParameterTypes();
				final String fullTypeName= getDestinationType().getFullyQualifiedName('.');
				final String[] fullParamNames= new String[params.length];
				for (int i= 0; i < fullParamNames.length; i++) {
					fullParamNames[i]= Bindings.getFullyQualifiedName(params[i]);
				}
				final String comment= CodeGeneration.getMethodComment(cu, enclosingTypeName, newMethodNode, false, binding.getName(), fullTypeName, fullParamNames, StubUtility.getLineDelimiterUsed(cu));
				if (comment != null)
					return (Javadoc) rewrite.createStringPlaceholder(comment, ASTNode.JAVADOC);
			}
		}
		return null;
	}

	private Map<ICompilationUnit, ArrayList<IMember>> createMembersToDeleteMap(final IProgressMonitor monitor) throws JavaModelException {
		final Map<ICompilationUnit, ArrayList<IMember>> result= new HashMap<>();
		for (final IMember member : getMembersToDelete(monitor)) {
			final ICompilationUnit cu= member.getCompilationUnit();
			if (!result.containsKey(cu))
				result.put(cu, new ArrayList<IMember>(1));
			result.get(cu).add(member);
		}
		return result;
	}

	private MethodDeclaration createNewMethodDeclarationNode(final CompilationUnitRewrite sourceRewrite, final CompilationUnitRewrite targetRewrite, final IMethod sourceMethod, final MethodDeclaration oldMethod, final TypeVariableMaplet[] mapping, final Map<IMember, IncomingMemberVisibilityAdjustment> adjustments, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		final ASTRewrite rewrite= targetRewrite.getASTRewrite();
		final AST ast= rewrite.getAST();
		ITypeBinding destinationBinding= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(getDestinationType(), targetRewrite.getRoot()).resolveBinding();
		StubUtility2Core.addOverrideAnnotation(fSettings, sourceMethod.getJavaProject(), sourceRewrite.getASTRewrite(), sourceRewrite.getImportRewrite(), oldMethod,
				destinationBinding == null ? false : destinationBinding.isInterface(),
				sourceRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_override_annotation, SET_PULL_UP));
		final MethodDeclaration newMethod= ast.newMethodDeclaration();
		if (!getDestinationType().isInterface())
			copyBodyOfPulledUpMethod(sourceRewrite, targetRewrite, sourceMethod, oldMethod, newMethod, mapping, monitor);
		newMethod.setConstructor(oldMethod.isConstructor());
		copyExtraDimensions(oldMethod, newMethod);
		copyJavadocNode(rewrite, oldMethod, newMethod);
		int modifiers= getModifiersWithUpdatedVisibility(sourceMethod, sourceMethod.getFlags(), adjustments, monitor, true, status);
		if (fDeletedMethods.length == 0 || getDestinationType().isInterface()) {
			modifiers&= ~Flags.AccFinal;
		}

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

	private BodyDeclaration createNewTypeDeclarationNode(final IType type, final AbstractTypeDeclaration oldType, final CompilationUnit declaringCuNode, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaModelException {
		final ICompilationUnit declaringCu= getDeclaringType().getCompilationUnit();
		if (!JdtFlags.isPublic(type) && !JdtFlags.isProtected(type)) {
			if (mapping.length > 0)
				return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

			return createPlaceholderForProtectedTypeDeclaration(oldType, declaringCuNode, declaringCu, rewrite, true);
		}
		if (mapping.length > 0)
			return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

		return createPlaceholderForTypeDeclaration(oldType, declaringCu, rewrite, true);
	}

	private ICompilationUnit createWorkingCopy(final ICompilationUnit unit, final TextEdit edit, final RefactoringStatus status, final IProgressMonitor monitor) {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
			final ICompilationUnit copy= getSharedWorkingCopy(unit.getPrimary(), new SubProgressMonitor(monitor, 1));
			final IDocument document= new Document(unit.getBuffer().getContents());
			edit.apply(document, TextEdit.UPDATE_REGIONS);
			copy.getBuffer().setContents(document.get());
			JavaModelUtil.reconcile(copy);
			return copy;
		} catch (JavaModelException | MalformedTreeException | BadLocationException exception) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
		} finally {
			monitor.done();
		}
		return null;
	}

	/**
	 * Creates a working copy layer if necessary.
	 *
	 * @param monitor
	 *            the progress monitor to use
	 * @return a status describing the outcome of the operation
	 */
	protected RefactoringStatus createWorkingCopyLayer(IProgressMonitor monitor) {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
			ICompilationUnit unit= getDeclaringType().getCompilationUnit();
			if (fLayer)
				unit= unit.findWorkingCopy(fOwner);
			resetWorkingCopies(unit);
			return new RefactoringStatus();
		} finally {
			monitor.done();
		}
	}

	private IMethod[] getAbstractMethods() throws JavaModelException {
		final IMethod[] toDeclareAbstract= fAbstractMethods;
		final IMethod[] abstractPulledUp= getAbstractMethodsToPullUp();
		final Set<IMember> result= new LinkedHashSet<>(toDeclareAbstract.length + abstractPulledUp.length + fMembersToMove.length);
		if (fDestinationType.isInterface()) {
			for (IMember element : fMembersToMove) {
				if (element.getElementType() == IJavaElement.METHOD) {
					result.add(element);
				}
			}
		}
		result.addAll(Arrays.asList(toDeclareAbstract));
		result.addAll(Arrays.asList(abstractPulledUp));
		return result.toArray(new IMethod[result.size()]);
	}

	private IMethod[] getAbstractMethodsToPullUp() throws JavaModelException {
		final List<IMember> result= new ArrayList<>(fMembersToMove.length);
		for (final IMember member : fMembersToMove) {
			if (member instanceof IMethod && JdtFlags.isAbstract(member))
				result.add(member);
		}
		return result.toArray(new IMethod[result.size()]);
	}

	public IMember[] getAdditionalRequiredMembersToPullUp(final IProgressMonitor monitor) throws JavaModelException {
		final IMember[] members= getCreatedDestinationMembers();
		List<IMember> queue;
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, getDeclaringType().getChildren().length);// maximum
			queue= new ArrayList<>(members.length);
			queue.addAll(Arrays.asList(members));
			if (queue.isEmpty())
				return new IMember[0];
			int i= 0;
			IMember current;
			do {
				current= queue.get(i);
				addAllRequiredPullableMembers(queue, current, new SubProgressMonitor(monitor, 1));
				i++;
				if (queue.size() == i)
					current= null;
			} while (current != null);
			queue.removeAll(Arrays.asList(members));// report only additional
		} finally {
			monitor.done();
		}
		return queue.toArray(new IMember[queue.size()]);
	}

	private ICompilationUnit[] getAffectedCompilationUnits(final IProgressMonitor monitor) throws JavaModelException {
		final IType[] allSubtypes= getDestinationTypeHierarchy(monitor).getAllSubtypes(getDestinationType());
		final Set<ICompilationUnit> result= new HashSet<>(allSubtypes.length);
		for (IType subtype : allSubtypes) {
			ICompilationUnit cu= subtype.getCompilationUnit();
			if (cu != null)
				result.add(cu);
		}
		result.add(getDestinationType().getCompilationUnit());
		return result.toArray(new ICompilationUnit[result.size()]);
	}

	public IType[] getCandidateTypes(final RefactoringStatus status, final IProgressMonitor monitor) throws JavaModelException {
		final IType declaring= getDeclaringType();
		final IType[] superTypes= declaring.newSupertypeHierarchy(fOwner, monitor).getAllSupertypes(declaring);
		final List<IType> list= new ArrayList<>(superTypes.length);
		int binary= 0;
		for (final IType type : superTypes) {
			if (type != null && type.exists() && !type.isReadOnly() && !type.isBinary() && !"java.lang.Object".equals(type.getFullyQualifiedName())) { //$NON-NLS-1$
				list.add(type);
			} else {
				if (type != null && type.isBinary()) {
					binary++;
				}
			}
		}
		if (superTypes.length == 1 && "java.lang.Object".equals(superTypes[0].getFullyQualifiedName())) //$NON-NLS-1$
			status.addFatalError(RefactoringCoreMessages.PullUPRefactoring_not_java_lang_object);
		else if (superTypes.length == binary)
			status.addFatalError(RefactoringCoreMessages.PullUPRefactoring_no_all_binary);

		Collections.reverse(list);
		return list.toArray(new IType[list.size()]);
	}

	protected CompilationUnitRewrite getCompilationUnitRewrite(final Map<ICompilationUnit, CompilationUnitRewrite> rewrites, final ICompilationUnit unit) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite= rewrites.get(unit);
		if (rewrite == null) {
			rewrite= new CompilationUnitRewrite(fOwner, unit);
			rewrites.put(unit, rewrite);
		}
		return rewrite;
	}

	private IMember[] getCreatedDestinationMembers() {
		final List<IMember> result= new ArrayList<>(fMembersToMove.length + fAbstractMethods.length);
		result.addAll(Arrays.asList(fMembersToMove));
		result.addAll(Arrays.asList(fAbstractMethods));
		return result.toArray(new IMember[result.size()]);
	}

	public boolean getCreateMethodStubs() {
		return fCreateMethodStubs;
	}

	public ITypeHierarchy getDeclaringSuperTypeHierarchy(final IProgressMonitor monitor) throws JavaModelException {
		try {
			if (fCachedDeclaringSuperTypeHierarchy != null)
				return fCachedDeclaringSuperTypeHierarchy;
			fCachedDeclaringSuperTypeHierarchy= getDeclaringType().newSupertypeHierarchy(fOwner, monitor);
			return fCachedDeclaringSuperTypeHierarchy;
		} finally {
			monitor.done();
		}
	}

	public IType getDestinationType() {
		return fDestinationType;
	}

	public ITypeHierarchy getDestinationTypeHierarchy(final IProgressMonitor monitor) throws JavaModelException {
		try {
			if (fCachedDestinationTypeHierarchy != null && fCachedDestinationTypeHierarchy.getType().equals(getDestinationType()))
				return fCachedDestinationTypeHierarchy;
			fCachedDestinationTypeHierarchy= getDestinationType().newTypeHierarchy(fOwner, monitor);
			return fCachedDestinationTypeHierarchy;
		} finally {
			monitor.done();
		}
	}

	@Override
	public Object[] getElements() {
		return fMembersToMove;
	}

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	public IMember[] getMatchingElements(final IProgressMonitor monitor, final boolean includeAbstract) throws JavaModelException {
		try {
			final Set<IMember> result= new HashSet<>();
			final IType destination= getDestinationType();
			final Map<IMember, Set<IMember>> matching= getMatchingMembers(getDestinationTypeHierarchy(monitor), getDestinationType(), includeAbstract);
			for (Entry<IMember, Set<IMember>> entry : matching.entrySet()) {
				IMember key= entry.getKey();
				Assert.isTrue(!key.getDeclaringType().equals(destination));
				result.addAll(entry.getValue());
			}
			return result.toArray(new IMember[result.size()]);
		} finally {
			monitor.done();
		}
	}

	private Map<IMember, Set<IMember>> getMatchingMembers(final ITypeHierarchy hierarchy, final IType type, final boolean includeAbstract) throws JavaModelException {
		final Map<IMember, Set<IMember>> result= new HashMap<>(getMatchingMembersMapping(type));
		for (IType subType : hierarchy.getAllSubtypes(type)) {
			final Map<IMember, Set<IMember>> map= getMatchingMembersMapping(subType);
			mergeMaps(result, map);
			upgradeMap(result, map);
		}
		if (includeAbstract)
			return result;

		for (IMethod abstractMethod : fAbstractMethods) {
			if (result.containsKey(abstractMethod))
				result.remove(abstractMethod);
		}
		return result;
	}

	private Map<IMember, Set<IMember>> getMatchingMembersMapping(final IType initial) throws JavaModelException {
		final Map<IMember, Set<IMember>> result= new HashMap<>();
		for (final IMember member : getCreatedDestinationMembers()) {
			if (member instanceof IMethod) {
				final IMethod method= (IMethod) member;
				final IMethod found= MemberCheckUtil.findMethod(method, initial.getMethods());
				if (found != null)
					addMatchingMember(result, method, found);
			} else if (member instanceof IField) {
				final IField field= (IField) member;
				final IField found= initial.getField(field.getElementName());
				if (found.exists() && field.getTypeSignature().equals(found.getTypeSignature())) {
					addMatchingMember(result, field, found);
				}
			} else if (member instanceof IType) {
				final IType type= (IType) member;
				final IType found= initial.getType(type.getElementName());
				if (found.exists())
					addMatchingMember(result, type, found);
			} else
				Assert.isTrue(false);
		}

		return result;
	}

	private IMember[] getMembersToDelete(final IProgressMonitor monitor) throws JavaModelException {
		try {
			final IMember[] typesToDelete= getMembers(fMembersToMove, IJavaElement.TYPE);
			final IMember[] matchingElements= getMatchingElements(monitor, false);
			final IMember[] matchingFields= getMembers(matchingElements, IJavaElement.FIELD);

			List<IMember> toDelete= new ArrayList<>();
			IMember[] all= JavaElementUtil.merge(matchingFields, typesToDelete);

			toDelete.addAll(Arrays.asList(fMembersToMove));
			toDelete.retainAll(Arrays.asList(all));

			return JavaElementUtil.merge(toDelete.toArray(new IMember[0]), fDeletedMethods);
		} finally {
			monitor.done();
		}
	}

	private int getModifiersWithUpdatedVisibility(final IMember member, final int modifiers, final Map<IMember, IncomingMemberVisibilityAdjustment> adjustments, final IProgressMonitor monitor, final boolean considerReferences, final RefactoringStatus status) throws JavaModelException {
		if (getDestinationType().isInterface()) {
			int flags= JdtFlags.clearAccessModifiers(modifiers);
			flags= JdtFlags.clearFlag(Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL, flags);
			return flags;
		}
		if (needsVisibilityAdjustment(member, considerReferences, monitor, status)) {
			final MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment adjustment= new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(member, Modifier.ModifierKeyword.PROTECTED_KEYWORD, RefactoringStatus.createWarningStatus(Messages.format(MemberVisibilityAdjustor.getMessage(member), new String[] { MemberVisibilityAdjustor.getLabel(member), MemberVisibilityAdjustor.getLabel(Modifier.ModifierKeyword.PROTECTED_KEYWORD)})));
			adjustment.setNeedsRewriting(false);
			adjustments.put(member, adjustment);
			return JdtFlags.clearAccessModifiers(modifiers) | Modifier.PROTECTED;
		}
		return modifiers;
	}

	private Set<IMember> getNotDeletedMembers(final IProgressMonitor monitor) throws JavaModelException {
		final Set<IMember> matchingSet= new HashSet<>();
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 2);
		matchingSet.addAll(Arrays.asList(getMatchingElements(new SubProgressMonitor(monitor, 1), true)));
		matchingSet.removeAll(Arrays.asList(getMembersToDelete(new SubProgressMonitor(monitor, 1))));
		monitor.done();
		return matchingSet;
	}

	@Override
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
	private Set<IType> getSkippedSuperTypes(final IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
		try {
			if (fCachedSkippedSuperTypes != null && getDestinationTypeHierarchy(new SubProgressMonitor(monitor, 1)).getType().equals(getDestinationType()))
				return fCachedSkippedSuperTypes;
			final ITypeHierarchy hierarchy= getDestinationTypeHierarchy(new SubProgressMonitor(monitor, 1));
			fCachedSkippedSuperTypes= new HashSet<>(2);
			IType current= hierarchy.getSuperclass(getDeclaringType());
			while (current != null && !current.equals(getDestinationType())) {
				fCachedSkippedSuperTypes.add(current);
				current= hierarchy.getSuperclass(current);
			}
			return fCachedSkippedSuperTypes;
		} finally {
			monitor.done();
		}
	}

	private RefactoringStatus initialize(final JavaRefactoringArguments extended) {
		String handle= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
		if (handle != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
			if (element == null || !element.exists() || element.getElementType() != IJavaElement.TYPE)
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getProcessorName(), IJavaRefactorings.PULL_UP);
			else
				fDestinationType= (IType) element;
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
		final String stubs= extended.getAttribute(ATTRIBUTE_STUBS);
		if (stubs != null) {
			fCreateMethodStubs= Boolean.parseBoolean(stubs);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_STUBS));
		final String instance= extended.getAttribute(ATTRIBUTE_INSTANCEOF);
		if (instance != null) {
			fInstanceOf= Boolean.parseBoolean(instance);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_INSTANCEOF));
		final String replace= extended.getAttribute(ATTRIBUTE_REPLACE);
		if (replace != null) {
			fReplace= Boolean.parseBoolean(replace);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REPLACE));
		int pullCount= 0;
		int abstractCount= 0;
		int deleteCount= 0;
		String value= extended.getAttribute(ATTRIBUTE_ABSTRACT);
		if (value != null && !"".equals(value)) {//$NON-NLS-1$
			try {
				abstractCount= Integer.parseInt(value);
			} catch (NumberFormatException exception) {
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ABSTRACT));
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ABSTRACT));
		value= extended.getAttribute(ATTRIBUTE_DELETE);
		if (value != null && !"".equals(value)) {//$NON-NLS-1$
			try {
				deleteCount= Integer.parseInt(value);
			} catch (NumberFormatException exception) {
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE));
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE));
		value= extended.getAttribute(ATTRIBUTE_PULL);
		if (value != null && !"".equals(value)) {//$NON-NLS-1$
			try {
				pullCount= Integer.parseInt(value);
			} catch (NumberFormatException exception) {
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PULL));
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PULL));
		final RefactoringStatus status= new RefactoringStatus();
		List<IJavaElement> elements= new ArrayList<>();
		for (int index= 0; index < pullCount; index++) {
			final String attribute= JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (index + 1);
			handle= extended.getAttribute(attribute);
			if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
				final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists())
					status.merge(JavaRefactoringDescriptorUtil.createInputWarningStatus(element, getProcessorName(), IJavaRefactorings.PULL_UP));
				else
					elements.add(element);
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
		}
		fMembersToMove= elements.toArray(new IMember[elements.size()]);
		elements= new ArrayList<>();
		for (int index= 0; index < deleteCount; index++) {
			final String attribute= JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (pullCount + index + 1);
			handle= extended.getAttribute(attribute);
			if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
				final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists())
					status.merge(JavaRefactoringDescriptorUtil.createInputWarningStatus(element, getProcessorName(), IJavaRefactorings.PULL_UP));
				else
					elements.add(element);
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
		}
		fDeletedMethods= elements.toArray(new IMethod[elements.size()]);
		elements= new ArrayList<>();
		for (int index= 0; index < abstractCount; index++) {
			final String attribute= JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (pullCount + abstractCount + index + 1);
			handle= extended.getAttribute(attribute);
			if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
				final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists())
					status.merge(JavaRefactoringDescriptorUtil.createInputWarningStatus(element, getProcessorName(), IJavaRefactorings.PULL_UP));
				else
					elements.add(element);
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
		}
		fAbstractMethods= elements.toArray(new IMethod[elements.size()]);
		IJavaProject project= null;
		if (fMembersToMove.length > 0)
			project= fMembersToMove[0].getJavaProject();
		fSettings= JavaPreferencesSettings.getCodeGenerationSettings(project);
		if (!status.isOK())
			return status;
		return new RefactoringStatus();
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isPullUpAvailable(fMembersToMove);
	}

	private boolean isAvailableInDestination(final IMethod method, final IProgressMonitor monitor) throws JavaModelException {
		try {
			final IType destination= getDestinationType();
			final IMethod first= JavaModelUtil.findMethod(method.getElementName(), method.getParameterTypes(), false, destination);
			if (first != null && MethodChecks.isVirtual(first))
				return true;
			final ITypeHierarchy hierarchy= getDestinationTypeHierarchy(monitor);
			final IMethod found= JavaModelUtil.findMethodInHierarchy(hierarchy, destination, method.getElementName(), method.getParameterTypes(), false);
			return found != null && MethodChecks.isVirtual(found);
		} finally {
			monitor.done();
		}
	}

	private boolean isRequiredPullableMember(final List<IMember> queue, final IMember member) throws JavaModelException {
		final IType declaring= member.getDeclaringType();
		if (declaring == null) // not a member
			return false;
		return declaring.equals(getDeclaringType()) && !queue.contains(member) && RefactoringAvailabilityTester.isPullUpAvailable(member);
	}

	protected void registerChanges(final TextEditBasedChangeManager manager) throws CoreException {
		for (final Entry<ICompilationUnit, CompilationUnitRewrite> entry : fCompilationUnitRewrites.entrySet()) {
			CompilationUnitRewrite rewrite= entry.getValue();
			if (rewrite != null) {
				final CompilationUnitChange change= rewrite.createChange(true);
				if (change != null)
					manager.manage(entry.getKey(), change);
			}
		}
	}

	/**
	 * Resets the environment before the first wizard page becomes visible.
	 */
	public void resetEnvironment() {
		ICompilationUnit unit= getDeclaringType().getCompilationUnit();
		if (fLayer)
			unit= unit.findWorkingCopy(fOwner);
		resetWorkingCopies(unit);
	}

	@Override
	protected void rewriteTypeOccurrences(final TextEditBasedChangeManager manager, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final ICompilationUnit unit, final CompilationUnit node, final Set<String> replacements, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			CompilationUnitRewrite currentRewrite= null;
			final CompilationUnitRewrite existingRewrite= fCompilationUnitRewrites.get(unit.getPrimary());
			final boolean isTouched= existingRewrite != null;
			if (isTouched)
				currentRewrite= existingRewrite;
			else
				currentRewrite= new CompilationUnitRewrite(unit, node);
			final Collection<ITypeConstraintVariable> collection= fTypeOccurrences.get(unit);
			if (collection != null && !collection.isEmpty()) {
				final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 100);
				try {
					subMonitor.beginTask("", collection.size() * 10); //$NON-NLS-1$
					subMonitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
					for (ITypeConstraintVariable iTypeConstraintVariable : collection) {
						if (iTypeConstraintVariable != null) {
							TType estimate= (TType) iTypeConstraintVariable.getData(SuperTypeConstraintsSolver.DATA_TYPE_ESTIMATE);
							if (estimate != null) {
								final CompilationUnitRange range= iTypeConstraintVariable.getRange();
								if (isTouched)
									rewriteTypeOccurrence(range, estimate, requestor, currentRewrite, node, replacements, currentRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence, SET_SUPER_TYPE));
								else {
									final ASTNode result= NodeFinder.perform(node, range.getSourceRange());
									if (result != null)
										rewriteTypeOccurrence(estimate, currentRewrite, result, currentRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence, SET_SUPER_TYPE));
								}
								subMonitor.worked(10);
							}
						}
					}
				} finally {
					subMonitor.done();
				}
			}
			if (!isTouched) {
				final TextChange change= currentRewrite.createChange(true);
				if (change != null)
					manager.manage(unit, change);
			}
		} finally {
			monitor.done();
		}
	}

	protected void rewriteTypeOccurrences(final TextEditBasedChangeManager manager, final CompilationUnitRewrite sourceRewrite, final ICompilationUnit copy, final Set<String> replacements, final RefactoringStatus status, final IProgressMonitor monitor) {
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.PullUpRefactoring_checking);
			final IType declaring= getDeclaringType();
			final IJavaProject project= declaring.getJavaProject();
			final ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			parser.setWorkingCopyOwner(fOwner);
			parser.setResolveBindings(true);
			parser.setProject(project);
			parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
			parser.createASTs(new ICompilationUnit[] { copy}, new String[0], new ASTRequestor() {

				@Override
				public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
					try {
						final IType subType= (IType) JavaModelUtil.findInCompilationUnit(unit, declaring);
						final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(subType, node);
						if (subDeclaration != null) {
							final ITypeBinding subBinding= subDeclaration.resolveBinding();
							if (subBinding != null) {
								ITypeBinding superBinding= null;
								for (ITypeBinding superBinding2 : Bindings.getAllSuperTypes(subBinding)) {
									String name= superBinding2.getName();
									if (name.startsWith(fDestinationType.getElementName()))
										superBinding= superBinding2;
								}
								if (superBinding != null) {
									solveSuperTypeConstraints(unit, node, subType, subBinding, superBinding, new SubProgressMonitor(monitor, 80), status);
									if (!status.hasFatalError())
										rewriteTypeOccurrences(manager, this, sourceRewrite, unit, node, replacements, status, new SubProgressMonitor(monitor, 120));
								}
							}
						}
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
						status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
					}
				}

				@Override
				public final void acceptBinding(final String key, final IBinding binding) {
					// Do nothing
				}
			}, new NullProgressMonitor());
		} finally {
			monitor.done();
		}
	}

	/**
	 * Sets the methods to declare abstract.
	 *
	 * @param methods
	 *            the methods to declare abstract
	 */
	public void setAbstractMethods(final IMethod[] methods) {
		Assert.isNotNull(methods);
		fAbstractMethods= methods;
	}

	/**
	 * Determines whether to create method stubs for non-implemented abstract
	 * methods.
	 *
	 * @param create
	 *            <code>true</code> to create stubs, <code>false</code>
	 *            otherwise
	 */
	public void setCreateMethodStubs(final boolean create) {
		fCreateMethodStubs= create;
	}

	/**
	 * Sets the methods to delete
	 *
	 * @param methods
	 *            the methods to delete
	 */
	public void setDeletedMethods(final IMethod[] methods) {
		Assert.isNotNull(methods);
		fDeletedMethods= methods;
	}

	/**
	 * Sets the destination type.
	 *
	 * @param type
	 *            the destination type
	 */
	public void setDestinationType(final IType type) {
		Assert.isNotNull(type);
		if (!type.equals(fDestinationType))
			fCachedDestinationTypeHierarchy= null;
		fDestinationType= type;
	}

	/**
	 * Sets the members to move.
	 *
	 * @param members
	 *            the members to move
	 */
	public void setMembersToMove(final IMember[] members) {
		Assert.isNotNull(members);
		fMembersToMove= (IMember[]) SourceReferenceUtil.sortByOffset(members);
	}
}
