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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsSolver;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Partial implementation of a hierarchy refactoring processor used in pull up,
 * push down and extract supertype refactorings.
 * <p>
 * This processor provides common functionality to move members in a type
 * hierarchy, and to perform a "Use Supertype" refactoring afterwards.
 * </p>
 *
 * @since 3.2
 */
public abstract class HierarchyProcessor extends SuperTypeRefactoringProcessor {

	/**
	 * AST node visitor which performs the actual mapping.
	 */
	public static class TypeVariableMapper extends ASTVisitor {

		/** The type variable mapping to use */
		protected final TypeVariableMaplet[] fMapping;

		/** The AST rewrite to use */
		protected final ASTRewrite fRewrite;

		/**
		 * Creates a new type variable mapper.
		 *
		 * @param rewrite
		 *            The AST rewrite to use
		 * @param mapping
		 *            The type variable mapping to use
		 */
		public TypeVariableMapper(final ASTRewrite rewrite, final TypeVariableMaplet[] mapping) {
			Assert.isNotNull(rewrite);
			Assert.isNotNull(mapping);
			fRewrite= rewrite;
			fMapping= mapping;
		}

		@Override
		public final boolean visit(final SimpleName node) {
			final ITypeBinding binding= node.resolveTypeBinding();
			if (binding != null && binding.isTypeVariable()) {
				String name= null;
				for (TypeVariableMaplet mapping : fMapping) {
					name= binding.getName();
					if (mapping.getSourceName().equals(name) && node.getIdentifier().equals(name)) {
						final MethodDeclaration declaration= ASTNodes.getParent(node, MethodDeclaration.class);
						if (declaration != null) {
							final IMethodBinding method= declaration.resolveBinding();
							if (method != null) {
								for (ITypeBinding b : method.getTypeParameters()) {
									if (b.isEqualTo(binding)) {
										return true;
									}
								}
							}
						}
						fRewrite.set(node, SimpleName.IDENTIFIER_PROPERTY, mapping.getTargetName(), null);
					}
				}
			}
			return true;
		}
	}

	protected static boolean areAllFragmentsDeleted(final FieldDeclaration declaration, final List<ASTNode> declarationNodes) {
		return declarationNodes.containsAll(declaration.fragments());
	}

	protected static RefactoringStatus checkProjectCompliance(CompilationUnitRewrite sourceRewriter, IType destination, IMember[] members) {
		RefactoringStatus status= new RefactoringStatus();
		if (!JavaModelUtil.is50OrHigher(destination.getJavaProject())) {
			for (IMember member : members) {
				try {
					BodyDeclaration decl= ASTNodeSearchUtil.getBodyDeclarationNode(member, sourceRewriter.getRoot());
					if (decl != null) {
						for (final Iterator<IExtendedModifier> iterator= decl.modifiers().iterator(); iterator.hasNext();) {
							boolean reported= false;
							final IExtendedModifier modifier= iterator.next();
							if (!reported && modifier.isAnnotation()) {
								status.merge(RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.PullUpRefactoring_incompatible_langauge_constructs, new String[]{JavaElementLabels.getTextLabel(member, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_DEFAULT)}), JavaStatusContext.create(member)));
								reported= true;
							}
						}
					}
				} catch (JavaModelException exception) {
					JavaPlugin.log(exception);
				}
				if (member instanceof IMethod) {
					final IMethod method= (IMethod) member;
					try {
						if (Flags.isVarargs(method.getFlags())) {
							status.merge(RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.PullUpRefactoring_incompatible_language_constructs1, new String[]{JavaElementLabels.getTextLabel(member, JavaElementLabels.ALL_FULLY_QUALIFIED), JavaElementLabels.getTextLabel(destination, JavaElementLabels.ALL_DEFAULT)}), JavaStatusContext.create(member)));
						}
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
				}
			}
		}
		return status;
	}

	protected static void copyExtraDimensions(final VariableDeclaration oldVarDeclaration, final VariableDeclaration newVarDeclaration) {
		final AST ast= newVarDeclaration.getAST();
		for (Object extraDimension : oldVarDeclaration.extraDimensions()) {
			newVarDeclaration.extraDimensions().add(ASTNode.copySubtree(ast, (Dimension) extraDimension));
		}
	}

	protected static void copyExtraDimensions(final MethodDeclaration oldMethod, final MethodDeclaration newMethod) {
		final AST ast= newMethod.getAST();
		for (Object extraDimension : oldMethod.extraDimensions()) {
			newMethod.extraDimensions().add(ASTNode.copySubtree(ast, (Dimension) extraDimension));
		}
	}

	protected static void copyAnnotations(final FieldDeclaration oldField, final FieldDeclaration newField) {
		final AST ast= newField.getAST();
		for (Object element : oldField.modifiers()) {
			final IExtendedModifier modifier= (IExtendedModifier) element;
			final List<IExtendedModifier> modifiers= newField.modifiers();
			if (modifier.isAnnotation() && !modifiers.contains(modifier))
				modifiers.add((IExtendedModifier) ASTNode.copySubtree(ast, (Annotation) modifier));
		}
	}

	protected static void copyAnnotations(final MethodDeclaration oldMethod, final MethodDeclaration newMethod) {
		final AST ast= newMethod.getAST();
		for (Object element : oldMethod.modifiers()) {
			final IExtendedModifier modifier= (IExtendedModifier) element;
			final List<IExtendedModifier> modifiers= newMethod.modifiers();
			if (modifier.isAnnotation() && !modifiers.contains(modifier))
				modifiers.add((IExtendedModifier) ASTNode.copySubtree(ast, (Annotation) modifier));
		}
	}

	protected static void copyJavadocNode(final ASTRewrite rewrite, final BodyDeclaration oldDeclaration, final BodyDeclaration newDeclaration) {
		final Javadoc predecessor= oldDeclaration.getJavadoc();
		if (predecessor != null) {
			String newString= ASTNodes.getNodeSource(predecessor, false, true);
			if (newString != null) {
				newDeclaration.setJavadoc((Javadoc) rewrite.createStringPlaceholder(newString, ASTNode.JAVADOC));
			}
		}
	}

	protected static void copyThrownExceptions(final MethodDeclaration oldMethod, final MethodDeclaration newMethod) {
		final AST ast= newMethod.getAST();
		for (Object thrownExceptionType : oldMethod.thrownExceptionTypes()) {
			newMethod.thrownExceptionTypes().add(ASTNode.copySubtree(ast, (Type) thrownExceptionType));
		}
	}

	protected static void copyTypeParameters(final MethodDeclaration oldMethod, final MethodDeclaration newMethod) {
		final AST ast= newMethod.getAST();
		for (Object typeParameter : oldMethod.typeParameters()) {
			newMethod.typeParameters().add(ASTNode.copySubtree(ast, (TypeParameter) typeParameter));
		}
	}

	protected static String createLabel(final IMember member) {
		if (member instanceof IType
				|| member instanceof IMethod
				|| member instanceof IField) {
			return JavaElementLabels.getTextLabel(member, JavaElementLabels.ALL_FULLY_QUALIFIED);
		} else if (member instanceof IInitializer)
			return RefactoringCoreMessages.HierarchyRefactoring_initializer;
		Assert.isTrue(false);
		return null;
	}

	protected static FieldDeclaration createNewFieldDeclarationNode(final ASTRewrite rewrite, final CompilationUnit unit, final IField field, final VariableDeclarationFragment oldFieldFragment, final TypeVariableMaplet[] mapping, final IProgressMonitor monitor, final RefactoringStatus status, final int modifiers) throws JavaModelException {
		final VariableDeclarationFragment newFragment= rewrite.getAST().newVariableDeclarationFragment();
		copyExtraDimensions(oldFieldFragment, newFragment);
		if (oldFieldFragment.getInitializer() != null) {
			Expression newInitializer= null;
			if (mapping.length > 0)
				newInitializer= createPlaceholderForExpression(oldFieldFragment.getInitializer(), field.getCompilationUnit(), mapping, rewrite);
			else
				newInitializer= createPlaceholderForExpression(oldFieldFragment.getInitializer(), field.getCompilationUnit(), rewrite);
			newFragment.setInitializer(newInitializer);
		}
		newFragment.setName(((SimpleName) ASTNode.copySubtree(rewrite.getAST(), oldFieldFragment.getName())));
		final FieldDeclaration newField= rewrite.getAST().newFieldDeclaration(newFragment);
		final FieldDeclaration oldField= ASTNodeSearchUtil.getFieldDeclarationNode(field, unit);
		copyJavadocNode(rewrite, oldField, newField);
		copyAnnotations(oldField, newField);
		newField.modifiers().addAll(ASTNodeFactory.newModifiers(rewrite.getAST(), modifiers));
		final Type oldType= oldField.getType();
		Type newType= null;
		if (mapping.length > 0) {
			newType= createPlaceholderForType(oldType, field.getCompilationUnit(), mapping, rewrite);
		} else
			newType= createPlaceholderForType(oldType, field.getCompilationUnit(), rewrite);
		newField.setType(newType);
		return newField;
	}

	protected static Expression createPlaceholderForExpression(final Expression expression, final ICompilationUnit declaringCu, final ASTRewrite rewrite) throws JavaModelException {
		return (Expression) rewrite.createStringPlaceholder(declaringCu.getBuffer().getText(expression.getStartPosition(), expression.getLength()), ASTNode.METHOD_INVOCATION);
	}

	protected static Expression createPlaceholderForExpression(final Expression expression, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaModelException {
		Expression result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(expression.getAST());
			final ITrackedNodePosition position= rewriter.track(expression);
			expression.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, declaringCu.getOptions(true)).apply(document, TextEdit.NONE);
			result= (Expression) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.METHOD_INVOCATION);
		} catch (MalformedTreeException | BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static BodyDeclaration createPlaceholderForProtectedTypeDeclaration(final BodyDeclaration bodyDeclaration, final CompilationUnit declaringCuNode, final ICompilationUnit declaringCu, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaModelException {
		String text= null;
		try {
			final ASTRewrite rewriter= ASTRewrite.create(bodyDeclaration.getAST());
			ModifierRewrite.create(rewriter, bodyDeclaration).setVisibility(Modifier.PROTECTED, null);
			final ITrackedNodePosition position= rewriter.track(bodyDeclaration);
			final IDocument document= new Document(declaringCu.getBuffer().getText(declaringCuNode.getStartPosition(), declaringCuNode.getLength()));
			rewriter.rewriteAST(document, declaringCu.getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
			text= document.get(position.getStartPosition(), position.getLength());
		} catch (BadLocationException exception) {
			text= getNewText(bodyDeclaration, declaringCu, removeIndentation);
		}
		return (BodyDeclaration) rewrite.createStringPlaceholder(text, ASTNode.TYPE_DECLARATION);
	}

	protected static BodyDeclaration createPlaceholderForProtectedTypeDeclaration(final BodyDeclaration bodyDeclaration, final CompilationUnit declaringCuNode, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaModelException {
		BodyDeclaration result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(bodyDeclaration.getAST());
			final ITrackedNodePosition position= rewriter.track(bodyDeclaration);
			bodyDeclaration.accept(new TypeVariableMapper(rewriter, mapping) {

				@Override
				public final boolean visit(final AnnotationTypeDeclaration node) {
					ModifierRewrite.create(fRewrite, bodyDeclaration).setVisibility(Modifier.PROTECTED, null);
					return true;
				}

				@Override
				public final boolean visit(final EnumDeclaration node) {
					ModifierRewrite.create(fRewrite, bodyDeclaration).setVisibility(Modifier.PROTECTED, null);
					return true;
				}

				@Override
				public final boolean visit(final TypeDeclaration node) {
					ModifierRewrite.create(fRewrite, bodyDeclaration).setVisibility(Modifier.PROTECTED, null);
					return true;
				}
			});
			rewriter.rewriteAST(document, declaringCu.getOptions(true)).apply(document, TextEdit.NONE);
			result= (BodyDeclaration) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.TYPE_DECLARATION);
		} catch (MalformedTreeException | BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static SingleVariableDeclaration createPlaceholderForSingleVariableDeclaration(final SingleVariableDeclaration declaration, final ICompilationUnit declaringCu, final ASTRewrite rewrite) throws JavaModelException {
		return (SingleVariableDeclaration) rewrite.createStringPlaceholder(declaringCu.getBuffer().getText(declaration.getStartPosition(), declaration.getLength()), ASTNode.SINGLE_VARIABLE_DECLARATION);
	}

	protected static SingleVariableDeclaration createPlaceholderForSingleVariableDeclaration(final SingleVariableDeclaration declaration, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaModelException {
		SingleVariableDeclaration result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(declaration.getAST());
			final ITrackedNodePosition position= rewriter.track(declaration);
			declaration.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, declaringCu.getOptions(true)).apply(document, TextEdit.NONE);
			result= (SingleVariableDeclaration) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.SINGLE_VARIABLE_DECLARATION);
		} catch (MalformedTreeException | BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static Type createPlaceholderForType(final Type type, final ICompilationUnit declaringCu, final ASTRewrite rewrite) throws JavaModelException {
		return (Type) rewrite.createStringPlaceholder(declaringCu.getBuffer().getText(type.getStartPosition(), type.getLength()), ASTNode.SIMPLE_TYPE);
	}

	protected static Type createPlaceholderForType(final Type type, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaModelException {
		Type result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(type.getAST());
			final ITrackedNodePosition position= rewriter.track(type);
			type.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, declaringCu.getOptions(true)).apply(document, TextEdit.NONE);
			result= (Type) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.SIMPLE_TYPE);
		} catch (MalformedTreeException | BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static BodyDeclaration createPlaceholderForTypeDeclaration(final BodyDeclaration bodyDeclaration, final ICompilationUnit declaringCu, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaModelException {
		return (BodyDeclaration) rewrite.createStringPlaceholder(getNewText(bodyDeclaration, declaringCu, removeIndentation), ASTNode.TYPE_DECLARATION);
	}

	protected static BodyDeclaration createPlaceholderForTypeDeclaration(final BodyDeclaration bodyDeclaration, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaModelException {
		BodyDeclaration result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(bodyDeclaration.getAST());
			final ITrackedNodePosition position= rewriter.track(bodyDeclaration);
			bodyDeclaration.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, declaringCu.getOptions(true)).apply(document, TextEdit.NONE);
			result= (BodyDeclaration) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.TYPE_DECLARATION);
		} catch (MalformedTreeException | BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static void deleteDeclarationNodes(final CompilationUnitRewrite sourceRewriter, final boolean sameCu, final CompilationUnitRewrite unitRewriter, final List<IMember> members, final GroupCategorySet set) throws JavaModelException {
		final List<ASTNode> declarationNodes= getDeclarationNodes(unitRewriter.getRoot(), members);
		for (ASTNode node : declarationNodes) {
			final ASTRewrite rewriter= unitRewriter.getASTRewrite();
			final ImportRemover remover= unitRewriter.getImportRemover();
			if (node instanceof VariableDeclarationFragment) {
				if (node.getParent() instanceof FieldDeclaration) {
					final FieldDeclaration declaration= (FieldDeclaration) node.getParent();
					if (areAllFragmentsDeleted(declaration, declarationNodes)) {
						rewriter.remove(declaration, unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_remove_member, set));
						if (!sameCu)
							remover.registerRemovedNode(declaration);
					} else {
						rewriter.remove(node, unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_remove_member, set));
						if (!sameCu)
							remover.registerRemovedNode(node);
					}
				}
			} else {
				rewriter.remove(node, unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_remove_member, set));
				if (!sameCu)
					remover.registerRemovedNode(node);
			}
		}
	}

	protected static List<ASTNode> getDeclarationNodes(final CompilationUnit cuNode, final List<IMember> members) throws JavaModelException {
		final List<ASTNode> result= new ArrayList<>(members.size());
		for (IMember member : members) {
			ASTNode node= null;
			if (member instanceof IField) {
				if (Flags.isEnum(member.getFlags()))
					node= ASTNodeSearchUtil.getEnumConstantDeclaration((IField) member, cuNode);
				else
					node= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, cuNode);
			} else if (member instanceof IType)
				node= ASTNodeSearchUtil.getAbstractTypeDeclarationNode((IType) member, cuNode);
			else if (member instanceof IMethod)
				node= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, cuNode);
			if (node != null)
				result.add(node);
		}
		return result;
	}

	protected static String getNewText(final ASTNode node, final ICompilationUnit declaringCu, final boolean removeIndentation) throws JavaModelException {
		final String result= declaringCu.getBuffer().getText(node.getStartPosition(), node.getLength());
		if (removeIndentation)
			return getUnindentedText(result, declaringCu);

		return result;
	}

	protected static String getUnindentedText(final String text, final ICompilationUnit declaringCu) throws JavaModelException {
		final String[] lines= Strings.convertIntoLines(text);
		Strings.trimIndentation(lines, declaringCu, false);
		return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(declaringCu));
	}

	/** The cached declaring type */
	protected IType fCachedDeclaringType;

	/** The cached member references */
	protected final Map<IMember, Object[]> fCachedMembersReferences= new HashMap<>(2);

	/** The cached type references */
	protected IType[] fCachedReferencedTypes;

	/** The text edit based change manager */
	protected TextEditBasedChangeManager fChangeManager;

	/** Does the refactoring use a working copy layer? */
	protected final boolean fLayer;

	/** The members to move (may be in working copies) */
	protected IMember[] fMembersToMove;

	/**
	 * Creates a new hierarchy processor.
	 *
	 * @param members
	 *            the members, or <code>null</code> if invoked by scripting
	 * @param settings
	 *            the code generation settings to use
	 * @param layer
	 *            <code>true</code> to create a working copy layer,
	 *            <code>false</code> otherwise
	 */
	protected HierarchyProcessor(final IMember[] members, final CodeGenerationSettings settings, boolean layer) {
		super(settings);
		fLayer= layer;
		if (members != null) {
			fMembersToMove= (IMember[]) SourceReferenceUtil.sortByOffset(members);
			if (layer && fMembersToMove.length > 0) {
				final ICompilationUnit original= fMembersToMove[0].getCompilationUnit();
				if (original != null) {
					try {
						final ICompilationUnit copy= getSharedWorkingCopy(original.getPrimary(), new NullProgressMonitor());
						if (copy != null) {
							for (int index= 0; index < fMembersToMove.length; index++) {
								final IJavaElement[] elements= copy.findElements(fMembersToMove[index]);
								if (elements != null && elements.length > 0 && elements[0] instanceof IMember) {
									fMembersToMove[index]= (IMember) elements[0];
								}
							}
						}
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
				}
			}
		}
	}

	protected boolean canBeAccessedFrom(final IMember member, final IType target, final ITypeHierarchy hierarchy) throws JavaModelException {
		Assert.isTrue(!(member instanceof IInitializer));
		return member.exists();
	}

	protected RefactoringStatus checkConstructorCalls(final IType type, final IProgressMonitor monitor) throws JavaModelException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 2);
			final RefactoringStatus result= new RefactoringStatus();
			final String message= Messages.format(RefactoringCoreMessages.HierarchyRefactoring_gets_instantiated, new Object[] { JavaElementLabels.getTextLabel(type, JavaElementLabels.ALL_FULLY_QUALIFIED)});

			for (SearchResultGroup group : ConstructorReferenceFinder.getConstructorReferences(type, fOwner, new SubProgressMonitor(monitor, 1), result)) {
				ICompilationUnit unit= group.getCompilationUnit();
				if (unit != null) {
					final CompilationUnit cuNode= RefactoringASTParser.parseWithASTProvider(unit, false, new SubProgressMonitor(monitor, 1));
					for (ASTNode node : ASTNodeSearchUtil.getAstNodes(group.getSearchResults(), cuNode)) {
						if ((node instanceof ClassInstanceCreation) || ConstructorReferenceFinder.isImplicitConstructorReferenceNodeInClassCreations(node)) {
							final RefactoringStatusContext context= JavaStatusContext.create(unit, node);
							result.addError(message, context);
						}
					}
				}
			}
			return result;
		} finally {
			monitor.done();
		}
	}

	protected RefactoringStatus checkDeclaringType(final IProgressMonitor monitor) throws JavaModelException {
		try {
			final IType type= getDeclaringType();
			if (type.isEnum())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.HierarchyRefactoring_enum_members);
			if (type.isAnnotation())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.HierarchyRefactoring_annotation_members);
			if (type.isInterface())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.HierarchyRefactoring_interface_members);
			if (type.isBinary())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.HierarchyRefactoring_members_of_binary);
			if (type.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.HierarchyRefactoring_members_of_read_only);
			return new RefactoringStatus();
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	protected RefactoringStatus checkIfMembersExist() {
		final RefactoringStatus result= new RefactoringStatus();
		for (IMember member : fMembersToMove) {
			if (member == null || !member.exists())
				result.addFatalError(RefactoringCoreMessages.HierarchyRefactoring_does_not_exist);
		}
		return result;
	}

	protected void clearCaches() {
		fCachedReferencedTypes= null;
	}

	protected void copyParameters(final ASTRewrite rewrite, final ICompilationUnit unit, final MethodDeclaration oldMethod, final MethodDeclaration newMethod, final TypeVariableMaplet[] mapping) throws JavaModelException {
		SingleVariableDeclaration newDeclaration= null;
		for (Object parameter : oldMethod.parameters()) {
			final SingleVariableDeclaration oldDeclaration= (SingleVariableDeclaration) parameter;
			if (mapping.length > 0)
				newDeclaration= createPlaceholderForSingleVariableDeclaration(oldDeclaration, unit, mapping, rewrite);
			else
				newDeclaration= createPlaceholderForSingleVariableDeclaration(oldDeclaration, unit, rewrite);
			newMethod.parameters().add(newDeclaration);
		}
	}

	protected void copyReturnType(final ASTRewrite rewrite, final ICompilationUnit unit, final MethodDeclaration oldMethod, final MethodDeclaration newMethod, final TypeVariableMaplet[] mapping) throws JavaModelException {
		Type newReturnType= null;
		if (mapping.length > 0)
			newReturnType= createPlaceholderForType(oldMethod.getReturnType2(), unit, mapping, rewrite);
		else
			newReturnType= createPlaceholderForType(oldMethod.getReturnType2(), unit, rewrite);
		newMethod.setReturnType2(newReturnType);
	}

	@Override
	protected SuperTypeConstraintsSolver createContraintSolver(final SuperTypeConstraintsModel model) {
		return new SuperTypeConstraintsSolver(model);
	}

	public IType getDeclaringType() {
		if (fCachedDeclaringType != null)
			return fCachedDeclaringType;
		fCachedDeclaringType= RefactoringAvailabilityTester.getTopLevelType(fMembersToMove);
		if (fCachedDeclaringType == null)
			fCachedDeclaringType= fMembersToMove[0].getDeclaringType();
		return fCachedDeclaringType;
	}

	public IMember[] getMembersToMove() {
		return fMembersToMove;
	}

	protected IType[] getTypesReferencedInMovedMembers(final IProgressMonitor monitor) throws JavaModelException {
		if (fCachedReferencedTypes == null) {
			final IType[] types= ReferenceFinderUtil.getTypesReferencedIn(fMembersToMove, fOwner, monitor);
			final List<IType> result= new ArrayList<>(types.length);
			final List<IMember> members= Arrays.asList(fMembersToMove);
			for (IType type : types) {
				if (!members.contains(type) && !type.equals(getDeclaringType())) {
					result.add(type);
				}
			}
			fCachedReferencedTypes= new IType[result.size()];
			result.toArray(fCachedReferencedTypes);
		}
		return fCachedReferencedTypes;
	}

	protected boolean hasNonMovedReferences(final IMember member, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		if (!fCachedMembersReferences.containsKey(member)) {
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
			engine.setFiltering(true, true);
			engine.setStatus(status);
			engine.setOwner(fOwner);
			engine.setScope(RefactoringScopeFactory.create(member));
			engine.searchPattern(new SubProgressMonitor(monitor, 1));
			fCachedMembersReferences.put(member, engine.getResults());
		}
		final SearchResultGroup[] groups= (SearchResultGroup[]) fCachedMembersReferences.get(member);
		if (groups.length == 0)
			return false;
		else if (groups.length > 1)
			return true;
		final ICompilationUnit unit= groups[0].getCompilationUnit();
		if (!getDeclaringType().getCompilationUnit().equals(unit))
			return true;
		for (SearchMatch match : groups[0].getSearchResults()) {
			if (!isMovedReference(match)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isMovedReference(final SearchMatch match) throws JavaModelException {
		for (IMember m : fMembersToMove) {
			ISourceRange range= m.getSourceRange();
			if (range.getOffset() <= match.getOffset() && range.getOffset() + range.getLength() >= match.getOffset())
				return true;
		}
		return false;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(final RefactoringStatus status, final SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	protected boolean needsVisibilityAdjustment(final IMember member, final boolean references, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		if (JdtFlags.isPublic(member) || JdtFlags.isProtected(member))
			return false;
		if (!references)
			return true;
		return hasNonMovedReferences(member, monitor, status);
	}
}
