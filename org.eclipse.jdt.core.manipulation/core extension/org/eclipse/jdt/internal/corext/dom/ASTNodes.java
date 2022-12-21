/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug "inline method - doesn't handle implicit cast" (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug Encapsulate field can fail when two variables in one variable declaration (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=51540).
 *     Stephan Herrmann - Configuration for
 *		 Bug 463360 - [override method][null] generating method override should not create redundant null annotations
 *     Fabrice TIERCELIN - Methods to identify a signature
 *     Pierre-Yves B. (pyvesdev@gmail.com) - contributed fix for
 *       Bug 434747 - [inline] Inlining a local variable leads to ambiguity with overloaded methods
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.IndentManipulation;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TypeEnvironment;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * JDT-UI-internal helper methods that deal with {@link ASTNode}s:
 * <ul>
 * <li>additional operations on {@link ASTNode}s and subtypes</li>
 * <li>finding related nodes in an AST</li>
 * <li>some methods that deal with bindings (new such methods should go into {@link Bindings})</li>
 * </ul>
 */
// @see JDTUIHelperClasses
public class ASTNodes {
	/**
	 * This constant should be merged with org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.UNTOUCH_COMMENT
	 */
	public static final String UNTOUCH_COMMENT= "untouchComment"; //$NON-NLS-1$

	public static final int NODE_ONLY=				0;
	public static final int INCLUDE_FIRST_PARENT= 	1;
	public static final int INCLUDE_ALL_PARENTS= 	2;

	public static final int WARNING=				1 << 0;
	public static final int ERROR=					1 << 1;
	public static final int INFO=					1 << 2;
	public static final int PROBLEMS=				WARNING | ERROR | INFO;
	public static final int EXCESSIVE_OPERAND_NUMBER= 5;

	private static final Message[] EMPTY_MESSAGES= new Message[0];
	private static final IProblem[] EMPTY_PROBLEMS= new IProblem[0];

	private static final int CLEAR_VISIBILITY= ~(Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);

	/** Enum representing the possible side effect of an expression. */
	public enum ExprActivity {
		/** Does nothing. */
		PASSIVE_WITHOUT_FALLING_THROUGH(0),

		/** Does nothing but may fall through. */
		PASSIVE(1),

		/** May modify something. */
		CAN_BE_ACTIVE(2),

		/** Modify something. */
		ACTIVE(3);

		private final int asInteger;

		ExprActivity(int asInteger) {
			this.asInteger= asInteger;
		}
	}

	private static final class ExprActivityVisitor extends InterruptibleVisitor {
		private ExprActivity activityLevel= ExprActivity.PASSIVE_WITHOUT_FALLING_THROUGH;

		public ExprActivity getActivityLevel() {
			return activityLevel;
		}

		@Override
		public boolean visit(CastExpression node) {
			setActivityLevel(ExprActivity.PASSIVE);
			return true;
		}

		@Override
		public boolean visit(ArrayAccess node) {
			setActivityLevel(ExprActivity.PASSIVE);
			return true;
		}

		@Override
		public boolean visit(FieldAccess node) {
			setActivityLevel(ExprActivity.PASSIVE);
			return true;
		}

		@Override
		public boolean visit(QualifiedName node) {
			if (node.getQualifier() == null
					|| node.getQualifier().resolveBinding() == null
					|| node.getQualifier().resolveBinding().getKind() != IBinding.PACKAGE
							&& node.getQualifier().resolveBinding().getKind() != IBinding.TYPE) {
				setActivityLevel(ExprActivity.PASSIVE);
			}

			return true;
		}

		@Override
		public boolean visit(Assignment node) {
			setActivityLevel(ExprActivity.ACTIVE);
			return interruptVisit();
		}

		@Override
		public boolean visit(PrefixExpression node) {
			if (hasOperator(node, PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT)) {
				setActivityLevel(ExprActivity.ACTIVE);
				return interruptVisit();
			} else if (hasType(node.getOperand(), Object.class.getCanonicalName())) {
				setActivityLevel(ExprActivity.PASSIVE);
			}

			return true;
		}

		@Override
		public boolean visit(PostfixExpression node) {
			setActivityLevel(ExprActivity.ACTIVE);
			return interruptVisit();
		}

		@Override
		public boolean visit(InfixExpression node) {
			if (hasOperator(node, InfixExpression.Operator.DIVIDE)) {
				setActivityLevel(ExprActivity.PASSIVE);
			} else {
				for (Expression operand : allOperands(node)) {
					if (hasType(operand, Object.class.getCanonicalName())) {
						setActivityLevel(ExprActivity.PASSIVE);
						break;
					}
				}
			}

			if (hasOperator(node, InfixExpression.Operator.PLUS) && hasType(node, String.class.getCanonicalName())
					&& (mayCallActiveToString(node.getLeftOperand())
							|| mayCallActiveToString(node.getRightOperand())
							|| mayCallActiveToString(node.extendedOperands()))) {
				setActivityLevel(ExprActivity.CAN_BE_ACTIVE);
			}

			return true;
		}

		private boolean mayCallActiveToString(List<Expression> extendedOperands) {
			if (extendedOperands != null) {
				for (Expression expression : extendedOperands) {
					if (mayCallActiveToString(expression)) {
						return true;
					}
				}
			}

			return false;
		}

		private boolean mayCallActiveToString(Expression expression) {
			return !hasType(expression,
					String.class.getCanonicalName(),
					boolean.class.getSimpleName(),
					int.class.getSimpleName(),
					long.class.getSimpleName(),
					double.class.getSimpleName(),
					float.class.getSimpleName(),
					short.class.getSimpleName(),
					char.class.getSimpleName(),
					Boolean.class.getCanonicalName(),
					Integer.class.getCanonicalName(),
					Long.class.getCanonicalName(),
					Double.class.getCanonicalName(),
					Float.class.getCanonicalName(),
					Short.class.getCanonicalName(),
					Character.class.getCanonicalName())
					&& !(expression instanceof PrefixExpression)
					&& !(expression instanceof InfixExpression)
					&& !(expression instanceof PostfixExpression);
		}

		@Override
		public boolean visit(SuperMethodInvocation node) {
			setActivityLevel(ExprActivity.CAN_BE_ACTIVE);
			return true;
		}

		@Override
		public boolean visit(MethodInvocation node) {
			setActivityLevel(ExprActivity.CAN_BE_ACTIVE);
			return true;
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			setActivityLevel(ExprActivity.CAN_BE_ACTIVE);
			return true;
		}

		@Override
		public boolean visit(ThrowStatement node) {
			setActivityLevel(ExprActivity.CAN_BE_ACTIVE);
			return true;
		}

		private void setActivityLevel(final ExprActivity newActivityLevel) {
			if (activityLevel.asInteger < newActivityLevel.asInteger) {
				activityLevel= newActivityLevel;
			}
		}
	}

	private ASTNodes() {
		// no instance;
	}

	public static String asString(ASTNode node) {
		ASTFlattener flattener= new ASTFlattener();
		node.accept(flattener);
		return flattener.getResult();
	}

	public static String asFormattedString(ASTNode node, int indent, String lineDelim, Map<String, String> options) {
		String unformatted= asString(node);
		TextEdit edit= CodeFormatterUtil.format2(node, unformatted, indent, lineDelim, options);
		if (edit != null) {
			Document document= new Document(unformatted);
			try {
				edit.apply(document, TextEdit.NONE);
			} catch (BadLocationException e) {
				// bug in the formatter
				JavaManipulationPlugin.log(e);
			}
			return document.get();
		}
		return unformatted; // unknown node
	}


	/**
	 * Returns the source of the given node from the location where it was parsed.
	 * @param node the node to get the source from
	 * @param extendedRange if set, the extended ranges of the nodes should ne used
	 * @param removeIndent if set, the indentation is removed.
	 * @return return the source for the given node or null if accessing the source failed.
	 */
	public static String getNodeSource(ASTNode node, boolean extendedRange, boolean removeIndent) {
		ASTNode root= node.getRoot();
		if (root instanceof CompilationUnit) {
			CompilationUnit astRoot= (CompilationUnit) root;
			ITypeRoot typeRoot= astRoot.getTypeRoot();
			try {
				if (typeRoot != null && typeRoot.getBuffer() != null) {
					IBuffer buffer= typeRoot.getBuffer();
					int offset= extendedRange ? astRoot.getExtendedStartPosition(node) : node.getStartPosition();
					int length= extendedRange ? astRoot.getExtendedLength(node) : node.getLength();
					String str= buffer.getText(offset, length);
					if (removeIndent) {
						IJavaProject project= typeRoot.getJavaProject();
						int indent= getIndentUsed(buffer, node.getStartPosition(), project);
						str= Strings.changeIndent(str, indent, project, "", typeRoot.findRecommendedLineSeparator()); //$NON-NLS-1$
					}
					return str;
				}
			} catch (JavaModelException e) {
				// ignore
			}
		}
		return null;
	}

	private static int getIndentUsed(IBuffer buffer, int offset, IJavaProject project) {
		int i= offset;
		// find beginning of line
		while (i > 0 && !IndentManipulation.isLineDelimiterChar(buffer.getChar(i - 1))) {
			i--;
		}
		return Strings.computeIndentUnits(buffer.getText(i, offset - i), project);
	}

    /**
     * Returns the list that contains the given ASTNode. If the node
     * isn't part of any list, <code>null</code> is returned.
     *
     * @param node the node in question
     * @return the list that contains the node or <code>null</code>
     */
    public static List<? extends ASTNode> getContainingList(ASTNode node) {
    	StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
    	if (locationInParent != null && locationInParent.isChildListProperty()) {
    		return getChildListProperty(node.getParent(), (ChildListPropertyDescriptor) locationInParent);
    	}
    	return null;
    }

	/**
	 * Variant of {@link ASTNode#getStructuralProperty(StructuralPropertyDescriptor)} that avoids
	 * unchecked casts in the caller.
	 * <p>
	 * To improve type-safety, callers can add the expected element type as explicit type argument, e.g.:
	 * <p>
	 * {@code ASTNodes.<BodyDeclaration>getChildListProperty(typeDecl, bodyDeclarationsProperty)}
	 *
	 * @param node the node
	 * @param propertyDescriptor the child list property to get
	 * @return the child list
	 * @exception RuntimeException if this node does not have the given property
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ASTNode> List<T> getChildListProperty(ASTNode node, ChildListPropertyDescriptor propertyDescriptor) {
		return (List<T>) node.getStructuralProperty(propertyDescriptor);
	}

	/**
	 * Returns a list of the direct children of a node. The siblings are ordered by start offset.
	 * @param node the node to get the children for
	 * @return the children
	 */
	public static List<ASTNode> getChildren(ASTNode node) {
		ChildrenCollector visitor= new ChildrenCollector();
		node.accept(visitor);
		return visitor.result;
	}

	private static class ChildrenCollector extends GenericVisitor {
		public List<ASTNode> result;

		public ChildrenCollector() {
			super(true);
			result= null;
		}
		@Override
		protected boolean visitNode(ASTNode node) {
			if (result == null) { // first visitNode: on the node's parent: do nothing, return true
				result= new ArrayList<>();
				return true;
			}
			result.add(node);
			return false;
		}
	}

	/**
	 * Returns true if this is an existing node, i.e. it was created as part of
	 * a parsing process of a source code file. Returns false if this is a newly
	 * created node which has not yet been given a source position.
	 *
	 * @param node the node to be tested.
	 * @return true if this is an existing node, false if not.
	 */
	public static boolean isExistingNode(ASTNode node) {
		return node.getStartPosition() != -1;
	}

	/**
	 * Returns the element type. This is a convenience method that returns its
	 * argument if it is a simple type and the element type if the parameter is an array type.
	 * @param type The type to get the element type from.
	 * @return The element type of the type or the type itself.
	 */
	public static Type getElementType(Type type) {
		if (! type.isArrayType())
			return type;
		return ((ArrayType)type).getElementType();
	}

	/**
	 * Filter a list of type bindings to remove any bindings that are sub-classes of others already in the list
	 *
	 * @param typeBindings - list of ITypeBinding to filter
	 * @return updated list of ITypeBinding
	 */
	public static List<ITypeBinding> filterSubtypes(List<ITypeBinding> typeBindings) {
		List<ITypeBinding> filteredBindings= new ArrayList<>(typeBindings);
		for (Iterator<ITypeBinding> subtypeIterator= filteredBindings.iterator(); subtypeIterator.hasNext();) {
			ITypeBinding iTypeBinding= subtypeIterator.next();
			for (ITypeBinding superTypeBinding : filteredBindings) {
				if (!iTypeBinding.equals(superTypeBinding) && iTypeBinding.isSubTypeCompatible(superTypeBinding)) {
					subtypeIterator.remove();
					break;
				}
			}
		}
		return filteredBindings;
	}

	public static ASTNode findDeclaration(IBinding binding, ASTNode root) {
		root= root.getRoot();
		if (root instanceof CompilationUnit) {
			return ((CompilationUnit)root).findDeclaringNode(binding);
		}
		return null;
	}

	public static VariableDeclaration findVariableDeclaration(IVariableBinding binding, ASTNode root) {
		if (binding.isField())
			return null;
		ASTNode result= findDeclaration(binding, root);
		if (result instanceof VariableDeclaration)
				return (VariableDeclaration)result;

		return null;
	}

	/**
	 * Returns the type node for the given declaration.
	 *
	 * @param declaration the declaration
	 * @return the type node or <code>null</code> if the given declaration represents a type
	 *         inferred parameter in lambda expression
	 */
	public static Type getType(VariableDeclaration declaration) {
		if (declaration instanceof SingleVariableDeclaration) {
			return ((SingleVariableDeclaration)declaration).getType();
		} else if (declaration instanceof VariableDeclarationFragment) {
			ASTNode parent= ((VariableDeclarationFragment)declaration).getParent();
			if (parent instanceof VariableDeclarationExpression)
				return ((VariableDeclarationExpression)parent).getType();
			else if (parent instanceof VariableDeclarationStatement)
				return ((VariableDeclarationStatement)parent).getType();
			else if (parent instanceof FieldDeclaration)
				return ((FieldDeclaration)parent).getType();
			else if (parent instanceof LambdaExpression)
				return null;
		}
		Assert.isTrue(false, "Unknown VariableDeclaration"); //$NON-NLS-1$
		return null;
	}

	public static int getDimensions(VariableDeclaration declaration) {
		int dim= declaration.getExtraDimensions();
		if (declaration instanceof VariableDeclarationFragment && declaration.getParent() instanceof LambdaExpression) {
			LambdaExpression lambda= (LambdaExpression) declaration.getParent();
			IMethodBinding methodBinding= lambda.resolveMethodBinding();
			if (methodBinding != null) {
				ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
				int index= lambda.parameters().indexOf(declaration);
				ITypeBinding typeBinding= parameterTypes[index];
				return typeBinding.getDimensions();
			}
		} else {
			Type type= getType(declaration);
			if (type instanceof ArrayType) {
				dim+= ((ArrayType) type).getDimensions();
			}
		}
		return dim;
	}

	public static List<IExtendedModifier> getModifiers(VariableDeclaration declaration) {
		Assert.isNotNull(declaration);
		if (declaration instanceof SingleVariableDeclaration) {
			return ((SingleVariableDeclaration)declaration).modifiers();
		} else if (declaration instanceof VariableDeclarationFragment) {
			ASTNode parent= declaration.getParent();
			if (parent instanceof VariableDeclarationExpression)
				return ((VariableDeclarationExpression)parent).modifiers();
			else if (parent instanceof VariableDeclarationStatement)
				return ((VariableDeclarationStatement)parent).modifiers();
		}
		return new ArrayList<>(0);
	}

	public static boolean isSingleDeclaration(VariableDeclaration declaration) {
		Assert.isNotNull(declaration);
		if (declaration instanceof SingleVariableDeclaration) {
			return true;
		} else if (declaration instanceof VariableDeclarationFragment) {
			ASTNode parent= declaration.getParent();
			if (parent instanceof VariableDeclarationExpression)
				return ((VariableDeclarationExpression)parent).fragments().size() == 1;
			else if (parent instanceof VariableDeclarationStatement)
				return ((VariableDeclarationStatement)parent).fragments().size() == 1;
		}
		return false;
	}

	/**
	 * Returns whether the provided expression is an instance of the qualified type
	 * name.
	 *
	 * @param expression        the expression to analyze
	 * @param qualifiedTypeName the qualified type name
	 * @return {@code true} if the provided expression is an instance of the
	 *         qualified type name, {@code false} otherwise
	 */
	public static boolean instanceOf(final Expression expression, final String qualifiedTypeName) {
		return expression != null && instanceOf(expression.resolveTypeBinding(), qualifiedTypeName);
	}

	/**
	 * Returns whether the provided type binding is an instance of the qualified
	 * type name.
	 *
	 * @param typeBinding       the type binding to analyze
	 * @param qualifiedTypeName the qualified type name
	 * @return true if the provided type binding is an instance of the qualified
	 *         type name, false otherwise
	 */
	public static boolean instanceOf(final ITypeBinding typeBinding, final String qualifiedTypeName) {
		return findImplementedType(typeBinding, qualifiedTypeName) != null;
	}

	/**
	 * Returns whether the provided expression represents an array.
	 *
	 * @param expression the expression to analyze
	 * @return true the provided expression represents an array, false otherwise
	 */
	public static boolean isArray(final Expression expression) {
		if (expression != null) {
			ITypeBinding typeBinding= expression.resolveTypeBinding();
			return typeBinding != null && typeBinding.isArray();
		}

		return false;
	}

	private static boolean isEnumConstant(final Expression expression) {
		if (expression instanceof Name) {
			IBinding binding= ((Name) expression).resolveBinding();
			if (binding instanceof IVariableBinding) {
				return ((IVariableBinding) binding).isEnumConstant();
			}
		}

		return false;
	}

	/**
	 * Returns whether the provided expression is hard-coded as a literal in the
	 * byte code ignoring parentheses.
	 *
	 * @param expression the expression to check
	 * @return true if the provided expression is hard-coded as a literal in the
	 *         byte code ignoring parentheses, false otherwise
	 */
	public static boolean isHardCoded(final Expression expression) {
		if (expression == null) {
			return false;
		}

		switch (expression.getNodeType()) {
		case ASTNode.BOOLEAN_LITERAL:
		case ASTNode.CHARACTER_LITERAL:
		case ASTNode.NUMBER_LITERAL:
		case ASTNode.STRING_LITERAL:
		case ASTNode.NULL_LITERAL:
			return true;

		case ASTNode.INFIX_EXPRESSION:
			for (Expression operand : allOperands((InfixExpression) expression)) {
				if (!isHardCoded(operand)) {
					return false;
				}
			}

			return true;

		case ASTNode.PREFIX_EXPRESSION:
			PrefixExpression prefixExpression= (PrefixExpression) expression;
			return isHardCoded(prefixExpression.getOperand());

		case ASTNode.POSTFIX_EXPRESSION:
			PostfixExpression postfixExpression= (PostfixExpression) expression;
			return isHardCoded(postfixExpression.getOperand());

		case ASTNode.CAST_EXPRESSION:
			return isHardCoded(((CastExpression) expression).getExpression());

		case ASTNode.PARENTHESIZED_EXPRESSION:
			return isHardCoded(((ParenthesizedExpression) expression).getExpression());

		default:
			return expression.resolveConstantExpressionValue() != null || isEnumConstant(expression);
		}
	}

	/**
	 * Integer literal.
	 *
	 * @param input The input
	 * @return Integer literal.
	 */
	public static Long getIntegerLiteral(final Expression input) {
		if (input == null) {
			return null;
		}

		Object number= input.resolveConstantExpressionValue();

		if (number instanceof Short) {
			return Long.valueOf(((Short) number).intValue());
		}

		if (number instanceof Integer) {
			return Long.valueOf(((Integer) number).intValue());
		}

		if (number instanceof Long) {
			return (Long) number;
		}

		InfixExpression operation= as(input, InfixExpression.class);

		if (operation != null
				&& hasOperator(operation,
						// All numerical operators
						InfixExpression.Operator.AND,
						InfixExpression.Operator.DIVIDE,
						InfixExpression.Operator.LEFT_SHIFT,
						InfixExpression.Operator.MINUS,
						InfixExpression.Operator.OR,
						InfixExpression.Operator.PLUS,
						InfixExpression.Operator.REMAINDER,
						InfixExpression.Operator.RIGHT_SHIFT_SIGNED,
						InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED,
						InfixExpression.Operator.TIMES,
						InfixExpression.Operator.XOR)) {
			List<Expression> operands= allOperands(operation);
			Long leftValue= getIntegerLiteral(operands.remove(0));

			if (leftValue == null) {
				return null;
			}

			long result= leftValue;

			for (Expression operand : operands) {
				Long newObject= getIntegerLiteral(operand);

				if (newObject == null) {
					return null;
				}

				long newValue= newObject;

				if (hasOperator(operation, InfixExpression.Operator.PLUS)) {
					result= result + newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.MINUS)) {
					result= result - newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.TIMES)) {
					result= result * newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.AND)) {
					result= result & newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.OR)) {
					result= result | newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.XOR)) {
					result= result ^ newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.LEFT_SHIFT)) {
					result= result << newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.REMAINDER)) {
					result= result % newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.RIGHT_SHIFT_SIGNED)) {
					result= result >> newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED)) {
					result= result >>> newValue;
				} else if (hasOperator(operation, InfixExpression.Operator.DIVIDE) && result % newValue == 0) {
					result= result / newValue;
				} else {
					return null;
				}
			}

			return result;
		}

		PrefixExpression negativeContant= as(input, PrefixExpression.class);

		if (negativeContant != null && hasOperator(negativeContant, PrefixExpression.Operator.MINUS)) {
			Long value= getIntegerLiteral(negativeContant.getOperand());

			if (value != null) {
				return -value.longValue();
			}
		}

		return null;
	}

	/**
	 * Returns a peremptory value, if any.
	 *
	 * @param peremptoryExpression A possible peremptory expression
	 * @return A peremptory value, if any
	 */
	public static Object peremptoryValue(final Expression peremptoryExpression) {
		Object constantExpression= peremptoryExpression.resolveConstantExpressionValue();

		if (constantExpression != null) {
			return constantExpression;
		}

		InfixExpression infixExpression= as(peremptoryExpression, InfixExpression.class);

		if (infixExpression != null
				&& !infixExpression.hasExtendedOperands()
				&& hasOperator(infixExpression, InfixExpression.Operator.EQUALS, InfixExpression.Operator.NOT_EQUALS)) {
			if (match(infixExpression.getLeftOperand(), infixExpression.getRightOperand())) {
				return hasOperator(infixExpression, InfixExpression.Operator.EQUALS);
			}

			if (ASTSemanticMatcher.INSTANCE.matchNegative(infixExpression.getLeftOperand(), infixExpression.getRightOperand())) {
				return hasOperator(infixExpression, InfixExpression.Operator.NOT_EQUALS);
			}
		}

		return null;
	}

	/**
	 * Get the field simple name.
	 *
	 * @param expression The expression
	 *
	 * @return the field simple name
	 */
	public static SimpleName getField(final Expression expression) {
		SimpleName simpleName= as(expression, SimpleName.class);

		if (simpleName != null) {
			return simpleName;
		}

		FieldAccess fieldName= as(expression, FieldAccess.class);

		if (fieldName != null) {
			ThisExpression thisExpression= as(fieldName.getExpression(), ThisExpression.class);

			if (thisExpression != null) {
				if (thisExpression.getQualifier() == null) {
					return fieldName.getName();
				}

				if (thisExpression.getQualifier().isSimpleName()) {
					SimpleName qualifier= (SimpleName) thisExpression.getQualifier();
					TypeDeclaration visitedClass= getTypedAncestor(expression, TypeDeclaration.class);

					if (visitedClass != null
							&& isSameVariable(visitedClass.getName(), qualifier)) {
						return fieldName.getName();
					}
				}
			}
		}

		SuperFieldAccess superFieldAccess= as(expression, SuperFieldAccess.class);

		if (superFieldAccess != null) {
			if (superFieldAccess.getQualifier() == null) {
				return superFieldAccess.getName();
			}

			if (superFieldAccess.getQualifier().isSimpleName()) {
				SimpleName qualifier= (SimpleName) superFieldAccess.getQualifier();
				TypeDeclaration visitedClass= getTypedAncestor(expression, TypeDeclaration.class);

				if (visitedClass != null
						&& isSameVariable(visitedClass.getName(), qualifier)) {
					return superFieldAccess.getName();
				}
			}
		}

		return null;
	}

	public static boolean isLiteral(Expression expression) {
		int type= expression.getNodeType();
		return type == ASTNode.BOOLEAN_LITERAL || type == ASTNode.CHARACTER_LITERAL || type == ASTNode.NULL_LITERAL ||
			type == ASTNode.NUMBER_LITERAL || type == ASTNode.STRING_LITERAL || type == ASTNode.TYPE_LITERAL || type == ASTNode.TEXT_BLOCK;
	}

	public static boolean isLabel(SimpleName name) {
		int parentType= name.getParent().getNodeType();
		return parentType == ASTNode.LABELED_STATEMENT ||
				(parentType == ASTNode.BREAK_STATEMENT && name.getLocationInParent() == BreakStatement.LABEL_PROPERTY) ||
				parentType != ASTNode.CONTINUE_STATEMENT;
	}

	/**
	 * Return the identifiers of variables declared inside the given statement.
	 *
	 * @param node               The node to visit
	 * @param includeInnerScopes True if blocks are visited too.
	 *
	 * @return The ids of the declared variables.
	 */
	public static Set<SimpleName> getLocalVariableIdentifiers(final ASTNode node, final boolean includeInnerScopes) {
		if (node == null) {
			return Collections.emptySet();
		}

		VarDeclarationIdentifierVisitor visitor= new VarDeclarationIdentifierVisitor(node, includeInnerScopes);
		node.accept(visitor);
		return visitor.getVariableNames();
	}

	/**
	 * Return true if the node changes nothing and throws no exceptions.
	 *
	 * @param node The node to visit.
	 *
	 * @return True if the node changes nothing and throws no exceptions.
	 */
	public static boolean isPassiveWithoutFallingThrough(final ASTNode node) {
		final ExprActivityVisitor visitor= new ExprActivityVisitor();
		visitor.traverseNodeInterruptibly(node);
		return ExprActivity.PASSIVE_WITHOUT_FALLING_THROUGH.equals(visitor.getActivityLevel());
	}

	public static boolean isStatic(BodyDeclaration declaration) {
		return Modifier.isStatic(declaration.getModifiers());
	}

	/**
	 * Return true if the node changes nothing.
	 *
	 * @param node The node to visit.
	 *
	 * @return True if the node changes nothing.
	 */
	public static boolean isPassive(final ASTNode node) {
		ExprActivityVisitor visitor= new ExprActivityVisitor();
		visitor.traverseNodeInterruptibly(node);
		return ExprActivity.PASSIVE_WITHOUT_FALLING_THROUGH.equals(visitor.getActivityLevel())
				|| ExprActivity.PASSIVE.equals(visitor.getActivityLevel());
	}

	/**
	 * True if the method is static, false if it is not or null if it is unknown.
	 *
	 * @param method The method
	 * @return True if the method is static, false if it is not or null if it is unknown.
	 */
	public static Boolean isStatic(final MethodInvocation method) {
		Expression calledType= method.getExpression();

		if (method.resolveMethodBinding() != null) {
			return Modifier.isStatic(method.resolveMethodBinding().getModifiers());
		}

		if ((calledType instanceof Name)
				&& ((Name) calledType).resolveBinding() != null
				&& ((Name) calledType).resolveBinding().getKind() == IBinding.TYPE) {
			return Boolean.TRUE;
		}

		return null;
	}

	/**
	 * Return true if it always ends with a jump statement.
	 *
	 * @param statement the statement
	 * @return true if the statement falls through.
	 */
	public static boolean fallsThrough(final Statement statement) {
		List<Statement> statements= asList(statement);

		if (statements.isEmpty()) {
			return false;
		}

		Statement lastStatement= statements.get(statements.size() - 1);
		switch (lastStatement.getNodeType()) {
		case ASTNode.RETURN_STATEMENT:
		case ASTNode.THROW_STATEMENT:
		case ASTNode.BREAK_STATEMENT:
		case ASTNode.CONTINUE_STATEMENT:
			return true;

		case ASTNode.BLOCK:
			Block block= (Block) lastStatement;
			return fallsThrough(block);

		case ASTNode.IF_STATEMENT:
			IfStatement ifStatement= (IfStatement) lastStatement;
			Statement thenStatement= ifStatement.getThenStatement();
			Statement elseStatement= ifStatement.getElseStatement();
			return fallsThrough(thenStatement) && fallsThrough(elseStatement);

		case ASTNode.TRY_STATEMENT:
			TryStatement tryStatement= (TryStatement) lastStatement;

			if (!fallsThrough(tryStatement.getBody())
					|| (tryStatement.getFinally() != null && fallsThrough(tryStatement.getFinally()))) {
				return false;
			}

			if (tryStatement.catchClauses() != null) {
				for (Object catchClause : tryStatement.catchClauses()) {
					if (!fallsThrough(((CatchClause) catchClause).getBody())) {
						return false;
					}
				}
			}

			return true;

		default:
			return false;
		}
	}

	public static List<BodyDeclaration> getBodyDeclarations(ASTNode node) {
		if (node instanceof AbstractTypeDeclaration) {
			return ((AbstractTypeDeclaration)node).bodyDeclarations();
		} else if (node instanceof AnonymousClassDeclaration) {
			return ((AnonymousClassDeclaration)node).bodyDeclarations();
		}
		// should not happen.
		Assert.isTrue(false);
		return null;
	}

	/**
	 * Returns the structural property descriptor for the "bodyDeclarations" property
	 * of this node (element type: {@link BodyDeclaration}).
	 *
	 * @param node the node, either an {@link AbstractTypeDeclaration} or an {@link AnonymousClassDeclaration}
	 * @return the property descriptor
	 */
	public static ChildListPropertyDescriptor getBodyDeclarationsProperty(ASTNode node) {
		if (node instanceof AbstractTypeDeclaration) {
			return ((AbstractTypeDeclaration)node).getBodyDeclarationsProperty();
		} else if (node instanceof AnonymousClassDeclaration) {
			return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
		}
		// should not happen.
		Assert.isTrue(false);
		return null;
	}

	/**
	 * Returns the simple name of the type, followed by array dimensions.
	 * Skips qualifiers, type arguments, and type annotations.
	 * <p>
	 * Does <b>not</b> work for WildcardTypes, etc.!
	 *
	 * @param type a type that has a simple name
	 * @return the simple name, followed by array dimensions
	 * @see #getSimpleNameIdentifier(Name)
	 * @since 3.10
	 */
	public static String getTypeName(Type type) {
		final StringBuilder buffer= new StringBuilder();
		ASTVisitor visitor= new ASTVisitor() {
			@Override
			public boolean visit(PrimitiveType node) {
				buffer.append(node.getPrimitiveTypeCode().toString());
				return false;
			}
			@Override
			public boolean visit(SimpleType node) {
				buffer.append(getSimpleNameIdentifier(node.getName()));
				return false;
			}
			@Override
			public boolean visit(QualifiedType node) {
				buffer.append(node.getName().getIdentifier());
				return false;
			}
			@Override
			public boolean visit(NameQualifiedType node) {
				buffer.append(node.getName().getIdentifier());
				return false;
			}
			@Override
			public boolean visit(ParameterizedType node) {
				node.getType().accept(this);
				return false;
			}
			@Override
			public void endVisit(ArrayType node) {
				for (int i= 0; i < node.dimensions().size(); i++) {
					buffer.append("[]"); //$NON-NLS-1$
				}
			}
		};
		type.accept(visitor);
		return buffer.toString();
	}

	/**
	 * Returns the (potentially qualified) name of a type, followed by array dimensions.
	 * Skips type arguments and type annotations.
	 *
	 * @param type a type that has a name
	 * @return the name, followed by array dimensions
	 * @since 3.10
	 */
	public static String getQualifiedTypeName(Type type) {
		final StringBuilder buffer= new StringBuilder();
		ASTVisitor visitor= new ASTVisitor() {
			@Override
			public boolean visit(SimpleType node) {
				buffer.append(node.getName().getFullyQualifiedName());
				return false;
			}
			@Override
			public boolean visit(QualifiedType node) {
				node.getQualifier().accept(this);
				buffer.append('.');
				buffer.append(node.getName().getIdentifier());
				return false;
			}
			@Override
			public boolean visit(NameQualifiedType node) {
				buffer.append(node.getQualifier().getFullyQualifiedName());
				buffer.append('.');
				buffer.append(node.getName().getIdentifier());
				return false;
			}
			@Override
			public boolean visit(ParameterizedType node) {
				node.getType().accept(this);
				return false;
			}
			@Override
			public void endVisit(ArrayType node) {
				for (int i= 0; i < node.dimensions().size(); i++) {
					buffer.append("[]"); //$NON-NLS-1$
				}
			}
		};
		type.accept(visitor);
		return buffer.toString();
	}

	/**
	 * Returns the {@link Boolean} object value represented by the provided
	 * expression.
	 *
	 * @param node the expression to analyze
	 * @return the {@link Boolean} object value if the provided expression
	 *         represents one, null otherwise
	 */
	public static Boolean getBooleanLiteral(final ASTNode node) {
		if (!(node instanceof Expression)) {
			return null;
		}

		Expression expression= (Expression) node;
		final BooleanLiteral bl= as(expression, BooleanLiteral.class);

		if (bl != null) {
			return bl.booleanValue();
		}

		final QualifiedName qn= as(expression, QualifiedName.class);

		if (hasType(qn, Boolean.class.getCanonicalName())) {
			return getBooleanObject(qn);
		}

		return null;
	}

	/**
	 * Returns whether the provided statement has the provided type.
	 *
	 * @param statement the statement to test
	 * @param stmtClass the type to test the statement against
	 * @return {@code true} if the provided statement has the provided type,
	 *         {@code false} otherwise
	 */
	public static boolean is(final Statement statement, final Class<? extends Statement> stmtClass) {
		return as(statement, stmtClass) != null;
	}

	/**
	 * Returns whether the provided expression has the provided type.
	 *
	 * @param expression the expression to test
	 * @param exprClass  the type to test the expression against
	 * @return {@code true} if the provided expression has the provided type,
	 *         {@code false} otherwise
	 */
	public static boolean is(final Expression expression, final Class<? extends Expression> exprClass) {
		return as(expression, exprClass) != null;
	}

	/**
	 * Casts the provided statement to an object of the provided type if type
	 * matches.
	 *
	 * @param <T>       the required statement type
	 * @param statement the statement to cast
	 * @param stmtClass the class representing the required statement type
	 * @return the provided statement as an object of the provided type if type matches, null otherwise
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Statement> T as(final Statement statement, final Class<T> stmtClass) {
		if (statement == null) {
			return null;
		}

		List<Statement> statements= asList(statement);
		if (statements.size() == 1) {
			Statement oneStatement= statements.get(0);

			if (stmtClass.isAssignableFrom(oneStatement.getClass())) {
				return (T) oneStatement;
			}
		}

		return null;
	}

	/**
	 * Casts the provided expression to an object of the provided type if type matches.
	 *
	 * @param <T> the required expression type
	 * @param expression the expression to cast
	 * @param exprClass the class representing the required expression type
	 * @return the provided expression as an object of the provided type if type matches, null
	 *         otherwise
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Expression> T as(Expression expression, Class<T> exprClass) {
		if (expression != null) {
			if (exprClass.isAssignableFrom(expression.getClass())) {
				return (T) expression;
			} else if (expression instanceof ParenthesizedExpression) {
				expression= ASTNodes.getUnparenthesedExpression(expression);

				return as(expression, exprClass);
			}
		}
		return null;
	}

	/**
	 * Returns the {@link Expression} of a specified type out of the provided
	 * {@link Statement}. Note the provided statement is first converted to an
	 * {@link ExpressionStatement} if possible.
	 *
	 * @param <T>       the required expression type
	 * @param statement the statement
	 * @param exprClass the class representing the required expression type
	 * @return the {@link Expression} of a specified type out of an
	 *         {@link ExpressionStatement}
	 */
	public static <T extends Expression> T asExpression(final Statement statement, final Class<T> exprClass) {
		ExpressionStatement es= as(statement, ExpressionStatement.class);

		if (es != null) {
			return as(es.getExpression(), exprClass);
		}

		return null;
	}

	/**
	 * Returns the provided statement as a non null list of statements:
	 * <ul>
	 * <li>if the statement is null, then an empty list is returned</li>
	 * <li>if the statement is a {@link Block}, then its children are returned</li>
	 * <li>otherwise, the current node is returned wrapped in a list</li>
	 * </ul>
	 *
	 * @param statement the statement to analyze
	 * @return the provided statement as a non null list of statements
	 */
	public static List<Statement> asList(Statement statement) {
		if (statement == null) {
			return Collections.emptyList();
		}

		if (statement instanceof Block) {
			return ((Block) statement).statements();
		}

		return Arrays.asList(statement);
	}

	/**
	 * Return the items of an infix expression in the order it is specified. It reverses the operator if needed.
	 *
	 * @param <F>          the required expression type
	 * @param <S>          the required expression type
	 * @param node         the supposed infix expression
	 * @param firstClass   the class representing the required expression type
	 * @param secondClass  the class representing the required expression type
	 * @return the items of an infix expression in the order it is specified. It reverses the operator if needed.
	 */
	public static <F extends Expression, S extends Expression> OrderedInfixExpression<F, S> orderedInfix(final Expression node, final Class<F> firstClass, final Class<S> secondClass) {
		InfixExpression expression= as(node, InfixExpression.class);

		if (expression == null || expression.hasExtendedOperands()) {
			return null;
		}

		if (firstClass != null && firstClass.equals(secondClass)) {
			F first= as(expression.getLeftOperand(), firstClass);
			S second= as(expression.getRightOperand(), secondClass);

			if (first != null && second != null) {
				return new OrderedInfixExpression<>(first, expression.getOperator(), second);
			}
		} else {
			F leftFirst= as(expression.getLeftOperand(), firstClass);
			S rightSecond= as(expression.getRightOperand(), secondClass);

			if (leftFirst != null && rightSecond != null) {
				return new OrderedInfixExpression<>(leftFirst, expression.getOperator(), rightSecond);
			}

			InfixExpression.Operator mirroredOperator= mirrorOperator(expression);

			if (mirroredOperator != null) {
				F rightFirst= as(expression.getRightOperand(), firstClass);
				S leftSecond= as(expression.getLeftOperand(), secondClass);

				if (rightFirst != null && leftSecond != null) {
					return new OrderedInfixExpression<>(rightFirst, mirroredOperator, leftSecond);
				}
			}
		}

		return null;
	}

	private static InfixExpression.Operator mirrorOperator(final InfixExpression expression) {
		if (Arrays.asList(
				InfixExpression.Operator.AND,
				InfixExpression.Operator.CONDITIONAL_AND,
				InfixExpression.Operator.CONDITIONAL_OR,
				InfixExpression.Operator.EQUALS,
				InfixExpression.Operator.NOT_EQUALS,
				InfixExpression.Operator.OR,
				InfixExpression.Operator.PLUS,
				InfixExpression.Operator.TIMES,
				InfixExpression.Operator.XOR).contains(expression.getOperator())) {
			return expression.getOperator();
		} else if (InfixExpression.Operator.GREATER.equals(expression.getOperator())) {
			return InfixExpression.Operator.LESS;
		} else if (InfixExpression.Operator.GREATER_EQUALS.equals(expression.getOperator())) {
			return InfixExpression.Operator.LESS_EQUALS;
		} else if (InfixExpression.Operator.LESS.equals(expression.getOperator())) {
			return InfixExpression.Operator.GREATER;
		} else if (InfixExpression.Operator.LESS_EQUALS.equals(expression.getOperator())) {
			return InfixExpression.Operator.GREATER_EQUALS;
		}

		return null;
	}

	/**
	 * Returns whether the two provided expressions are cast compatible.
	 *
	 * @param expr1 the first expression
	 * @param expr2 the second expression
	 * @return {@code true} if the two provided expressions are cast compatible,
	 *         {@code false} otherwise
	 * @see ITypeBinding#isCastCompatible(ITypeBinding)
	 */
	public static boolean isCastCompatible(final Expression expr1, final Expression expr2) {
		ITypeBinding tb1= expr1.resolveTypeBinding();
		ITypeBinding tb2= expr2.resolveTypeBinding();
		return tb1 != null && tb2 != null && tb1.isCastCompatible(tb2);
	}

	/**
	 * Returns true if the if statement is in a else statement, that is to say is an else-if statement.
	 *
	 * @param node The if statement
	 * @return true if the if statement is in a else statement
	 */
	public static boolean isInElse(final IfStatement node) {
		if (node == null) {
			return false;
		}

		ASTNode parent= node.getParent();

		if (parent instanceof IfStatement) {
			IfStatement is= (IfStatement) parent;
			return node.equals(is.getElseStatement());
		}

		return false;
	}

	/**
	 * Returns all the operands from the provided infix expressions.
	 *
	 * @param node the infix expression
	 * @return a List of expressions
	 */
	public static List<Expression> allOperands(InfixExpression node) {
		List<Expression> extOps= node.extendedOperands();
		List<Expression> operands= new ArrayList<>(2 + extOps.size());
		operands.add(node.getLeftOperand());
		operands.add(node.getRightOperand());
		operands.addAll(extOps);

		List<Expression> optimizedOperands= new ArrayList<>();

		for (Expression expression : operands) {
			if (expression instanceof InfixExpression && hasOperator((InfixExpression) expression, node.getOperator())) {
				optimizedOperands.addAll(allOperands((InfixExpression) expression));
			} else {
				optimizedOperands.add(expression);
			}
		}

		return optimizedOperands;
	}

	/**
	 * Returns true if variables are declared with the same identifier after the given statement.
	 *
	 * @param node The start
	 * @param statementInBlock The statement with variables
	 * @return true if variables are declared with the same identifier after the given statement.
	 */
	public static boolean hasVariableConflict(final Statement node, final Statement statementInBlock) {
		Set<SimpleName> existingVariableNames= getLocalVariableIdentifiers(statementInBlock, false);

		for (Statement statement : getNextSiblings(node)) {
			VarConflictVisitor varOccurrenceVisitor= new VarConflictVisitor(existingVariableNames, true);
			varOccurrenceVisitor.traverseNodeInterruptibly(statement);

			if (varOccurrenceVisitor.isVarConflicting()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns the number of logical operands in the expression.
	 *
	 * @param node The expression
	 * @return the number of logical operands in the expression
	 */
	public static int getNbOperands(final Expression node) {
		InfixExpression infixExpression= as(node, InfixExpression.class);

		if (infixExpression == null
				|| !hasOperator(infixExpression, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.CONDITIONAL_OR)
				&& (!hasOperator(infixExpression, InfixExpression.Operator.AND, InfixExpression.Operator.OR, InfixExpression.Operator.XOR)
						|| !hasType(infixExpression.getLeftOperand(), boolean.class.getCanonicalName(), Boolean.class.getCanonicalName()))) {
			return 1;
		}

		int nbOperands= 0;

		for (Expression operand : allOperands(infixExpression)) {
			nbOperands+= getNbOperands(operand);
		}

		return nbOperands;
	}

	/**
	 * Returns the {@link Boolean} object constant value represented by the provided qualified name.
	 *
	 * @param qualifiedName the qualified name that must represent a Boolean object constant
	 * @return the {@link Boolean} object constant value represented by the provided qualified name,
	 *         or null if the qualified name does not represent a {@link Boolean} object constant
	 *         value.
	 */
	public static Boolean getBooleanObject(final QualifiedName qualifiedName) {
		final String fqn= qualifiedName.getFullyQualifiedName();
		if ("Boolean.TRUE".equals(fqn)) { //$NON-NLS-1$
			return Boolean.TRUE;
		} else if ("Boolean.FALSE".equals(fqn)) { //$NON-NLS-1$
			return Boolean.FALSE;
		}
		return null;
	}

	/**
	 * Returns the {@link ITypeBinding} of the {@link VariableDeclaration}.
	 *
	 * @param varDecl the variable declaration
	 * @return the fragment's type binding, or null if none can be found
	 */
	public static ITypeBinding resolveTypeBinding(final VariableDeclaration varDecl) {
		if (varDecl != null) {
			IVariableBinding varBinding= varDecl.resolveBinding();

			if (varBinding != null) {
				return varBinding.getType();
			}
		}

		return null;
	}

	/**
	 * Returns whether the provided operator is the same as the one of provided node.
	 *
	 * @param node the node for which to test the operator
	 * @param expectedOperator the first operator to test
	 * @param additionalExpectedOperators the other operators to test too
	 * @return true if the provided node has the provided operator, false otherwise.
	 */
	public static boolean hasOperator(Assignment node, Assignment.Operator expectedOperator, Assignment.Operator... additionalExpectedOperators) {
		return node != null && isOperatorInList(node.getOperator(), expectedOperator, additionalExpectedOperators);
	}

	/**
	 * Returns whether the provided operator is the same as the one of provided node.
	 *
	 * @param node the node for which to test the operator
	 * @param expectedOperator the first operator to test
	 * @param additionalExpectedOperators the other operators to test too
	 * @return true if the provided node has the provided operator, false otherwise.
	 */
	public static boolean hasOperator(InfixExpression node, InfixExpression.Operator expectedOperator, InfixExpression.Operator... additionalExpectedOperators) {
		return node != null && isOperatorInList(node.getOperator(), expectedOperator, additionalExpectedOperators);
	}

	/**
	 * Returns whether the provided operator is the same as the one of provided node.
	 *
	 * @param node the node for which to test the operator
	 * @param expectedOperator the first operator to test
	 * @param additionalExpectedOperators the other operators to test too
	 * @return true if the provided node has the provided operator, false otherwise.
	 */
	public static boolean hasOperator(PrefixExpression node, PrefixExpression.Operator expectedOperator, PrefixExpression.Operator... additionalExpectedOperators) {
		return node != null && isOperatorInList(node.getOperator(), expectedOperator, additionalExpectedOperators);
	}

	/**
	 * Returns whether the provided operator is the same as the one of provided node.
	 *
	 * @param node the node for which to test the operator
	 * @param expectedOperator the first operator to test
	 * @param additionalExpectedOperators the other operators to test too
	 * @return true if the provided node has the provided operator, false otherwise.
	 */
	public static boolean hasOperator(PostfixExpression node, PostfixExpression.Operator expectedOperator, PostfixExpression.Operator... additionalExpectedOperators) {
		return node != null && isOperatorInList(node.getOperator(), expectedOperator, additionalExpectedOperators);
	}

	private static <O> boolean isOperatorInList(O actualOperator, O expectedOperator, O[] additionalExpectedOperators) {
		return actualOperator != null && (actualOperator.equals(expectedOperator) || Arrays.asList(additionalExpectedOperators).contains(actualOperator));
	}

	/**
	 * Returns whether the provided expression evaluates to exactly one of the provided type.
	 *
	 * @param expression the expression to analyze
	 * @param oneOfQualifiedTypeNames the type binding qualified name must be equal to one of these
	 *            qualified type names
	 * @return true if the provided expression evaluates to exactly one of the provided type, false
	 *         otherwise
	 */
	public static boolean hasType(Expression expression, String... oneOfQualifiedTypeNames) {
		return expression != null && hasType(expression.resolveTypeBinding(), oneOfQualifiedTypeNames);
	}

	/**
	 * Returns whether the provided type binding is exactly one of the provided type.
	 *
	 * @param typeBinding the type binding to analyze
	 * @param oneOfQualifiedTypeNames the type binding qualified name must be equal to one of these
	 *            qualified type names
	 * @return {@code true} if the provided type binding is exactly one of the provided type,
	 *         {@code false} otherwise
	 */
	public static boolean hasType(final ITypeBinding typeBinding, String... oneOfQualifiedTypeNames) {
		if (typeBinding != null) {
			final String qualifiedName= typeBinding.getErasure().getQualifiedName();
			for (String qualifiedTypeName : oneOfQualifiedTypeNames) {
				if (qualifiedTypeName.equals(qualifiedName)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the opposite infix operator. For boolean operators, the operands should be negated
	 * too.
	 *
	 * @param operator the infix operator
	 * @return the opposite infix operator
	 */
	public static InfixExpression.Operator negatedInfixOperator(InfixExpression.Operator operator) {
		if (InfixExpression.Operator.LESS.equals(operator))
			return InfixExpression.Operator.GREATER_EQUALS;

		if (InfixExpression.Operator.LESS_EQUALS.equals(operator))
			return InfixExpression.Operator.GREATER;

		if (InfixExpression.Operator.GREATER.equals(operator))
			return InfixExpression.Operator.LESS_EQUALS;

		if (InfixExpression.Operator.GREATER_EQUALS.equals(operator))
			return InfixExpression.Operator.LESS;

		if (InfixExpression.Operator.EQUALS.equals(operator))
			return InfixExpression.Operator.NOT_EQUALS;

		if (InfixExpression.Operator.NOT_EQUALS.equals(operator))
			return InfixExpression.Operator.EQUALS;

		if (InfixExpression.Operator.CONDITIONAL_AND.equals(operator))
			return InfixExpression.Operator.CONDITIONAL_OR;

		if (InfixExpression.Operator.CONDITIONAL_OR.equals(operator))
			return InfixExpression.Operator.CONDITIONAL_AND;

		return null;
	}

	public static InfixExpression.Operator convertToInfixOperator(Assignment.Operator operator) {
		if (operator.equals(Assignment.Operator.PLUS_ASSIGN))
			return InfixExpression.Operator.PLUS;

		if (operator.equals(Assignment.Operator.MINUS_ASSIGN))
			return InfixExpression.Operator.MINUS;

		if (operator.equals(Assignment.Operator.TIMES_ASSIGN))
			return InfixExpression.Operator.TIMES;

		if (operator.equals(Assignment.Operator.DIVIDE_ASSIGN))
			return InfixExpression.Operator.DIVIDE;

		if (operator.equals(Assignment.Operator.BIT_AND_ASSIGN))
			return InfixExpression.Operator.AND;

		if (operator.equals(Assignment.Operator.BIT_OR_ASSIGN))
			return InfixExpression.Operator.OR;

		if (operator.equals(Assignment.Operator.BIT_XOR_ASSIGN))
			return InfixExpression.Operator.XOR;

		if (operator.equals(Assignment.Operator.REMAINDER_ASSIGN))
			return InfixExpression.Operator.REMAINDER;

		if (operator.equals(Assignment.Operator.LEFT_SHIFT_ASSIGN))
			return InfixExpression.Operator.LEFT_SHIFT;

		if (operator.equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN))
			return InfixExpression.Operator.RIGHT_SHIFT_SIGNED;

		if (operator.equals(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN))
			return InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;

		Assert.isTrue(false, "Cannot convert assignment operator"); //$NON-NLS-1$
		return null;
	}

	/**
	 * Returns true if a node at a given location is a body of a control statement. Such body nodes are
	 * interesting as when replacing them, it has to be evaluates if a Block is needed instead.
	 * E.g. <code> if (x) do(); -> if (x) { do1(); do2() } </code>
	 *
	 * @param locationInParent Location of the body node
	 * @return Returns true if the location is a body node location of a control statement.
	 */
	public static boolean isControlStatementBody(StructuralPropertyDescriptor locationInParent) {
		return locationInParent == IfStatement.THEN_STATEMENT_PROPERTY
			|| locationInParent == IfStatement.ELSE_STATEMENT_PROPERTY
			|| locationInParent == ForStatement.BODY_PROPERTY
			|| locationInParent == EnhancedForStatement.BODY_PROPERTY
			|| locationInParent == WhileStatement.BODY_PROPERTY
			|| locationInParent == DoStatement.BODY_PROPERTY;
	}

	/**
	 * Returns the type to which an inlined variable initializer should be cast, or
	 * <code>null</code> if no cast is necessary.
	 *
	 * @param initializer the initializer expression of the variable to inline
	 * @param reference the reference to the variable (which is to be inlined)
	 * @return a type binding to which the initializer should be cast, or <code>null</code> iff no cast is necessary
	 * @since 3.6
	 */
	public static ITypeBinding getExplicitCast(Expression initializer, Expression reference) {
		ITypeBinding initializerType= initializer.resolveTypeBinding();
		ITypeBinding referenceType= reference.resolveTypeBinding();
		if (initializerType == null || referenceType == null)
			return null;

		if (initializerType.isPrimitive() && referenceType.isPrimitive() && ! referenceType.isEqualTo(initializerType)) {
			return referenceType;

		} else if (initializerType.isPrimitive() && ! referenceType.isPrimitive()) { // initializer is autoboxed
			ITypeBinding unboxedReferenceType= Bindings.getUnboxedTypeBinding(referenceType, reference.getAST());
			if (!unboxedReferenceType.isEqualTo(initializerType))
				return unboxedReferenceType;
			else if (needsExplicitBoxing(reference))
				return referenceType;

		} else if (! initializerType.isPrimitive() && referenceType.isPrimitive()) { // initializer is autounboxed
			ITypeBinding unboxedInitializerType= Bindings.getUnboxedTypeBinding(initializerType, reference.getAST());
			if (!unboxedInitializerType.isEqualTo(referenceType))
				return referenceType;

		} else if (initializerType.isRawType() && referenceType.isParameterizedType()) {
			return referenceType; // don't lose the unchecked conversion

		} else if (initializer instanceof LambdaExpression || initializer instanceof MethodReference) {
			if (isTargetAmbiguous(reference, isExplicitlyTypedLambda(initializer))) {
				return referenceType;
			} else {
				ITypeBinding targetType= getTargetType(reference);
				if (targetType == null || targetType != referenceType) {
					return referenceType;
				}
			}

		} else if (! TypeRules.canAssign(initializerType, referenceType)) {
			if (!Bindings.containsTypeVariables(referenceType))
				return referenceType;

		} else if (!initializerType.isEqualTo(referenceType)) {
			if (isTargetAmbiguous(reference, initializerType)) {
				return referenceType;
			}
		}

		return null;
	}

	/**
	 * Checks whether overloaded methods can result in an ambiguous method call or a semantic change when the
	 * <code>expression</code> argument is replaced with a poly expression form of the functional
	 * interface instance.
	 *
	 * @param expression the method argument, which is a functional interface instance
	 * @param expressionIsExplicitlyTyped <code>true</code> iff the intended replacement for <code>expression</code>
	 *         is an explicitly typed lambda expression (JLS8 15.27.1)
	 * @return <code>true</code> if overloaded methods can result in an ambiguous method call or a semantic change,
	 *         <code>false</code> otherwise
	 *
	 * @since 3.10
	 */
	public static boolean isTargetAmbiguous(Expression expression, boolean expressionIsExplicitlyTyped) {
		ParentSummary targetSummary= getParentSummary(expression);
		if (targetSummary == null) {
			return false;
		}

		if (targetSummary.methodBinding != null) {
			ITypeBinding invocationTargetType= getInvocationType(expression.getParent(), targetSummary.methodBinding, targetSummary.invocationQualifier);
			if (invocationTargetType != null) {
				TypeBindingVisitor visitor= new FunctionalInterfaceAmbiguousMethodAnalyzer(invocationTargetType, targetSummary.methodBinding, targetSummary.argumentIndex,
						targetSummary.argumentCount, expressionIsExplicitlyTyped);
				return !visitor.visit(invocationTargetType)
						|| !Bindings.visitHierarchy(invocationTargetType, visitor);
			}
		}

		return true;
	}

	/**
	 * Checks whether overloaded methods can result in an ambiguous method call or a semantic change
	 * when the <code>expression</code> argument is inlined.
	 *
	 * @param expression the method argument, which is a functional interface instance
	 * @param initializerType the initializer type of the variable to inline
	 * @return <code>true</code> if overloaded methods can result in an ambiguous method call or a
	 *         semantic change, <code>false</code> otherwise
	 *
	 * @since 3.19
	 */
	public static boolean isTargetAmbiguous(Expression expression, ITypeBinding initializerType) {
		ParentSummary parentSummary= getParentSummary(expression);
		if (parentSummary == null) {
			return false;
		}

		IMethodBinding methodBinding= parentSummary.methodBinding;
		if (methodBinding != null) {
			ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
			int argumentIndex= parentSummary.argumentIndex;
			if (methodBinding.isVarargs() && argumentIndex >= parameterTypes.length - 1) {
				argumentIndex= parameterTypes.length - 1;
				initializerType= initializerType.createArrayType(1);
			}
			parameterTypes[argumentIndex]= initializerType;

			ITypeBinding invocationType= getInvocationType(expression.getParent(), methodBinding, parentSummary.invocationQualifier);
			if (invocationType != null) {
				TypeEnvironment typeEnvironment= new TypeEnvironment();
				TypeBindingVisitor visitor= new AmbiguousMethodAnalyzer(typeEnvironment, methodBinding, typeEnvironment.create(parameterTypes));
				if (!visitor.visit(invocationType)) {
					return true;
				} else if (invocationType.isInterface()) {
					return !Bindings.visitInterfaces(invocationType, visitor);
				} else if (Modifier.isAbstract(invocationType.getModifiers())) {
					return !Bindings.visitHierarchy(invocationType, visitor);
				} else {
					// it is not needed to visit interfaces if receiver is a concrete class
					return !Bindings.visitSuperclasses(invocationType, visitor);
				}
			}
		}

		return true;
	}

	private static ParentSummary getParentSummary(Expression expression) {
		StructuralPropertyDescriptor locationInParent= expression.getLocationInParent();

		while (locationInParent == ParenthesizedExpression.EXPRESSION_PROPERTY
				|| locationInParent == ConditionalExpression.THEN_EXPRESSION_PROPERTY
				|| locationInParent == ConditionalExpression.ELSE_EXPRESSION_PROPERTY) {
			expression= (Expression) expression.getParent();
			locationInParent= expression.getLocationInParent();
		}

		ASTNode parent= expression.getParent();
		IMethodBinding methodBinding;
		int argumentIndex;
		int argumentCount;
		Expression invocationQualifier= null;
		if (locationInParent == MethodInvocation.ARGUMENTS_PROPERTY) {
			MethodInvocation methodInvocation= (MethodInvocation) parent;
			methodBinding= methodInvocation.resolveMethodBinding();
			argumentIndex= methodInvocation.arguments().indexOf(expression);
			argumentCount= methodInvocation.arguments().size();
			invocationQualifier= methodInvocation.getExpression();
		} else if (locationInParent == SuperMethodInvocation.ARGUMENTS_PROPERTY) {
			SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) parent;
			methodBinding= superMethodInvocation.resolveMethodBinding();
			argumentIndex= superMethodInvocation.arguments().indexOf(expression);
			argumentCount= superMethodInvocation.arguments().size();
			invocationQualifier= superMethodInvocation.getQualifier();
		} else if (locationInParent == ConstructorInvocation.ARGUMENTS_PROPERTY) {
			ConstructorInvocation constructorInvocation= (ConstructorInvocation) parent;
			methodBinding= constructorInvocation.resolveConstructorBinding();
			argumentIndex= constructorInvocation.arguments().indexOf(expression);
			argumentCount= constructorInvocation.arguments().size();
		} else if (locationInParent == SuperConstructorInvocation.ARGUMENTS_PROPERTY) {
			SuperConstructorInvocation superConstructorInvocation= (SuperConstructorInvocation) parent;
			methodBinding= superConstructorInvocation.resolveConstructorBinding();
			argumentIndex= superConstructorInvocation.arguments().indexOf(expression);
			argumentCount= superConstructorInvocation.arguments().size();
		} else if (locationInParent == ClassInstanceCreation.ARGUMENTS_PROPERTY) {
			ClassInstanceCreation creation= (ClassInstanceCreation) parent;
			methodBinding= creation.resolveConstructorBinding();
			argumentIndex= creation.arguments().indexOf(expression);
			argumentCount= creation.arguments().size();
		} else if (locationInParent == EnumConstantDeclaration.ARGUMENTS_PROPERTY) {
			EnumConstantDeclaration enumConstantDecl= (EnumConstantDeclaration) parent;
			methodBinding= enumConstantDecl.resolveConstructorBinding();
			argumentIndex= enumConstantDecl.arguments().indexOf(expression);
			argumentCount= enumConstantDecl.arguments().size();
		} else {
			return null;
		}

		return new ParentSummary(methodBinding, argumentIndex, argumentCount, invocationQualifier);
	}

	private static class ParentSummary {

		private final IMethodBinding methodBinding;

		private final int argumentIndex;

		private final int argumentCount;

		private final Expression invocationQualifier;

		ParentSummary(IMethodBinding methodBinding, int argumentIndex, int argumentCount, Expression invocationQualifier) {
			this.methodBinding= methodBinding;
			this.argumentIndex= argumentIndex;
			this.argumentCount= argumentCount;
			this.invocationQualifier= invocationQualifier;
		}
	}

	/**
	 * Returns the binding of the type which declares the method being invoked.
	 *
	 * @param invocationNode the method invocation node
	 * @param methodBinding binding of the method being invoked
	 * @param invocationQualifier the qualifier used for method invocation, or <code>null</code> if
	 *            none
	 * @return the binding of the type which declares the method being invoked, or <code>null</code>
	 *         if the type cannot be resolved
	 */
	public static ITypeBinding getInvocationType(ASTNode invocationNode, IMethodBinding methodBinding, Expression invocationQualifier) {
		ITypeBinding invocationType;
		if (invocationNode instanceof MethodInvocation || invocationNode instanceof SuperMethodInvocation) {
			if (invocationQualifier != null) {
				invocationType= invocationQualifier.resolveTypeBinding();
				if (invocationType != null && invocationNode instanceof SuperMethodInvocation) {
					invocationType= invocationType.getSuperclass();
				}
			} else {
				ITypeBinding enclosingType= getEnclosingType(invocationNode);
				if (enclosingType != null && invocationNode instanceof SuperMethodInvocation) {
					enclosingType= enclosingType.getSuperclass();
				}
				if (enclosingType != null) {
					IMethodBinding methodInHierarchy= Bindings.findMethodInHierarchy(enclosingType, methodBinding.getName(), methodBinding.getParameterTypes());
					if (methodInHierarchy != null) {
						invocationType= enclosingType;
					} else {
						invocationType= methodBinding.getDeclaringClass();
					}
				} else {
					// not expected
					invocationType= methodBinding.getDeclaringClass();
				}
			}
		} else {
			invocationType= methodBinding.getDeclaringClass();
		}
		return invocationType;
	}

	private static class AmbiguousMethodAnalyzer implements TypeBindingVisitor {
		private TypeEnvironment fTypeEnvironment;
		private TType[] fTypes;
		private IMethodBinding fOriginal;

		public AmbiguousMethodAnalyzer(TypeEnvironment typeEnvironment, IMethodBinding original, TType[] types) {
			fTypeEnvironment= typeEnvironment;
			fOriginal= original;
			fTypes= types;
		}

		@Override
		public boolean visit(ITypeBinding node) {
			IMethodBinding[] methods= node.getDeclaredMethods();
			for (IMethodBinding candidate : methods) {
				if (candidate == fOriginal) {
					continue;
				}
				if (fOriginal.getName().equals(candidate.getName())) {
					if (canImplicitlyCall(candidate)) {
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * Returns <code>true</code> if the method can be called without explicit casts; otherwise
		 * <code>false</code>.
		 *
		 * @param candidate the method to test
		 * @return <code>true</code> if the method can be called without explicit casts
		 */
		private boolean canImplicitlyCall(IMethodBinding candidate) {
			ITypeBinding[] parameters= candidate.getParameterTypes();
			if (parameters.length != fTypes.length) {
				return false;
			}
			for (int i= 0; i < parameters.length; i++) {
				if (!fTypes[i].canAssignTo(fTypeEnvironment.create(parameters[i]))) {
					return false;
				}
			}
			return true;
		}
	}

	private static class FunctionalInterfaceAmbiguousMethodAnalyzer implements TypeBindingVisitor {
		private ITypeBinding fDeclaringType;
		private IMethodBinding fOriginalMethod;
		private int fArgIndex;
		private int fArgumentCount;
		private boolean fExpressionIsExplicitlyTyped;

		/**
		 * @param declaringType the type binding declaring the <code>originalMethod</code>
		 * @param originalMethod the method declaration binding corresponding to the method call
		 * @param argumentIndex the index of the functional interface instance argument in the
		 *            method call
		 * @param argumentCount the number of arguments in the method call
		 * @param expressionIsExplicitlyTyped <code>true</code> iff the intended replacement for <code>expression</code>
		 *         is an explicitly typed lambda expression (JLS8 15.27.1)
		 */
		public FunctionalInterfaceAmbiguousMethodAnalyzer(ITypeBinding declaringType, IMethodBinding originalMethod, int argumentIndex, int argumentCount, boolean expressionIsExplicitlyTyped) {
			fDeclaringType= declaringType;
			fOriginalMethod= originalMethod;
			fArgIndex= argumentIndex;
			fArgumentCount= argumentCount;
			fExpressionIsExplicitlyTyped= expressionIsExplicitlyTyped;
		}

		@Override
		public boolean visit(ITypeBinding type) {
			for (IMethodBinding candidate : type.getDeclaredMethods()) {
				if (candidate.getMethodDeclaration() == fOriginalMethod.getMethodDeclaration()) {
					continue;
				}
				ITypeBinding candidateDeclaringType= candidate.getDeclaringClass();
				if (fDeclaringType != candidateDeclaringType) {
					int modifiers= candidate.getModifiers();
					if (candidateDeclaringType.isInterface() && Modifier.isStatic(modifiers)) {
						continue;
					}
					if (Modifier.isPrivate(modifiers)) {
						continue;
					}
				}
				if (fOriginalMethod.getName().equals(candidate.getName()) && !fOriginalMethod.overrides(candidate)) {
					ITypeBinding[] originalParameterTypes= fOriginalMethod.getParameterTypes();
					ITypeBinding[] candidateParameterTypes= candidate.getParameterTypes();

					boolean couldBeAmbiguous;
					if (originalParameterTypes.length == candidateParameterTypes.length) {
						couldBeAmbiguous= true;
					} else if (fOriginalMethod.isVarargs() || candidate.isVarargs() ) {
						int candidateMinArgumentCount= candidateParameterTypes.length;
						if (candidate.isVarargs())
							candidateMinArgumentCount--;
						couldBeAmbiguous= fArgumentCount >= candidateMinArgumentCount;
					} else {
						couldBeAmbiguous= false;
					}
					if (couldBeAmbiguous) {
						ITypeBinding parameterType= ASTResolving.getParameterTypeBinding(candidate, fArgIndex);
						if (parameterType != null && parameterType.getFunctionalInterfaceMethod() != null) {
							if (!fExpressionIsExplicitlyTyped) {
								/* According to JLS8 15.12.2.2, implicitly typed lambda expressions are not "pertinent to applicability"
								 * and hence potentially applicable methods are always "applicable by strict invocation",
								 * regardless of whether argument expressions are compatible with the method's parameter types or not.
								 * If there are multiple such methods, 15.12.2.5 results in an ambiguous method invocation.
								 */
								return false;
							}
							/* Explicitly typed lambda expressions are pertinent to applicability, and hence
							 * compatibility with the corresponding method parameter type is checked. And since this check
							 * separates functional interface methods by their void-compatibility state, functional interfaces
							 * with a different void compatibility are not applicable any more and hence can't cause
							 * an ambiguous method invocation.
							 */
							ITypeBinding origParamType= ASTResolving.getParameterTypeBinding(fOriginalMethod, fArgIndex);
							boolean originalIsVoidCompatible=  Bindings.isVoidType(origParamType.getFunctionalInterfaceMethod().getReturnType());
							boolean candidateIsVoidCompatible= Bindings.isVoidType(parameterType.getFunctionalInterfaceMethod().getReturnType());
							if (originalIsVoidCompatible == candidateIsVoidCompatible) {
								return false;
							}
						}
					}
				}
			}
			return true;
		}
	}

	/**
	 * Derives the target type defined at the location of the given expression if the target context
	 * supports poly expressions.
	 *
	 * @param expression the expression at whose location the target type is required
	 * @return the type binding of the target type defined at the location of the given expression
	 *         if the target context supports poly expressions, or <code>null</code> if the target
	 *         type could not be derived
	 *
	 * @since 3.10
	 */
	public static ITypeBinding getTargetType(Expression expression) {
		ASTNode parent= expression.getParent();
		StructuralPropertyDescriptor locationInParent= expression.getLocationInParent();

		if (locationInParent == VariableDeclarationFragment.INITIALIZER_PROPERTY || locationInParent == SingleVariableDeclaration.INITIALIZER_PROPERTY) {
			return ((VariableDeclaration) parent).getName().resolveTypeBinding();

		} else if (locationInParent == Assignment.RIGHT_HAND_SIDE_PROPERTY) {
			return ((Assignment) parent).getLeftHandSide().resolveTypeBinding();

		} else if (locationInParent == ReturnStatement.EXPRESSION_PROPERTY) {
			return getTargetTypeForReturnStmt((ReturnStatement) parent);

		} else if (locationInParent == ArrayInitializer.EXPRESSIONS_PROPERTY) {
			return getTargetTypeForArrayInitializer((ArrayInitializer) parent);

		} else if (locationInParent == ArrayAccess.INDEX_PROPERTY) {
			return parent.getAST().resolveWellKnownType(int.class.getSimpleName());

		} else if (locationInParent == ConditionalExpression.EXPRESSION_PROPERTY
				|| locationInParent == IfStatement.EXPRESSION_PROPERTY
				|| locationInParent == WhileStatement.EXPRESSION_PROPERTY
				|| locationInParent == DoStatement.EXPRESSION_PROPERTY) {
			return parent.getAST().resolveWellKnownType(boolean.class.getSimpleName());

		} else if (locationInParent == SwitchStatement.EXPRESSION_PROPERTY) {
			final ITypeBinding discriminentType= expression.resolveTypeBinding();
			if (discriminentType == null) {
				return null;
			}
			if (discriminentType.isPrimitive() || discriminentType.isEnum()
					|| discriminentType.getQualifiedName().equals(String.class.getCanonicalName())) {
				return discriminentType;
			} else {
				return Bindings.getUnboxedTypeBinding(discriminentType, parent.getAST());
			}

		} else if (locationInParent == MethodInvocation.ARGUMENTS_PROPERTY) {
			MethodInvocation methodInvocation= (MethodInvocation) parent;
			IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
			if (methodBinding != null) {
				return getParameterTypeBinding(expression, methodInvocation.arguments(), methodBinding);
			}

		} else if (locationInParent == SuperMethodInvocation.ARGUMENTS_PROPERTY) {
			SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) parent;
			IMethodBinding superMethodBinding= superMethodInvocation.resolveMethodBinding();
			if (superMethodBinding != null) {
				return getParameterTypeBinding(expression, superMethodInvocation.arguments(), superMethodBinding);
			}

		} else if (locationInParent == ConstructorInvocation.ARGUMENTS_PROPERTY) {
			ConstructorInvocation constructorInvocation= (ConstructorInvocation) parent;
			IMethodBinding constructorBinding= constructorInvocation.resolveConstructorBinding();
			if (constructorBinding != null) {
				return getParameterTypeBinding(expression, constructorInvocation.arguments(), constructorBinding);
			}

		} else if (locationInParent == SuperConstructorInvocation.ARGUMENTS_PROPERTY) {
			SuperConstructorInvocation superConstructorInvocation= (SuperConstructorInvocation) parent;
			IMethodBinding superConstructorBinding= superConstructorInvocation.resolveConstructorBinding();
			if (superConstructorBinding != null) {
				return getParameterTypeBinding(expression, superConstructorInvocation.arguments(), superConstructorBinding);
			}

		} else if (locationInParent == ClassInstanceCreation.ARGUMENTS_PROPERTY) {
			ClassInstanceCreation creation= (ClassInstanceCreation) parent;
			IMethodBinding creationBinding= creation.resolveConstructorBinding();
			if (creationBinding != null) {
				return getParameterTypeBinding(expression, creation.arguments(), creationBinding);
			}

		} else if (locationInParent == EnumConstantDeclaration.ARGUMENTS_PROPERTY) {
			EnumConstantDeclaration enumConstantDecl= (EnumConstantDeclaration) parent;
			IMethodBinding enumConstructorBinding= enumConstantDecl.resolveConstructorBinding();
			if (enumConstructorBinding != null) {
				return getParameterTypeBinding(expression, enumConstantDecl.arguments(), enumConstructorBinding);
			}

		} else if (locationInParent == LambdaExpression.BODY_PROPERTY) {
			IMethodBinding methodBinding= ((LambdaExpression) parent).resolveMethodBinding();
			if (methodBinding != null) {
				return methodBinding.getReturnType();
			}

		} else if (locationInParent == ConditionalExpression.THEN_EXPRESSION_PROPERTY || locationInParent == ConditionalExpression.ELSE_EXPRESSION_PROPERTY) {
			return getTargetType((ConditionalExpression) parent);

		} else if (locationInParent == CastExpression.EXPRESSION_PROPERTY) {
			return ((CastExpression) parent).getType().resolveBinding();

		} else if (locationInParent == ParenthesizedExpression.EXPRESSION_PROPERTY) {
			return getTargetType((ParenthesizedExpression) parent);

		}
		return null;
	}

	private static ITypeBinding getParameterTypeBinding(Expression expression, List<Expression> arguments, IMethodBinding methodBinding) {
		int index= arguments.indexOf(expression);
		return ASTResolving.getParameterTypeBinding(methodBinding, index);
	}

	private static ITypeBinding getTargetTypeForArrayInitializer(ArrayInitializer arrayInitializer) {
		ASTNode initializerParent= arrayInitializer.getParent();
		while (initializerParent instanceof ArrayInitializer) {
			initializerParent= initializerParent.getParent();
		}
		if (initializerParent instanceof ArrayCreation) {
			return ((ArrayCreation) initializerParent).getType().getElementType().resolveBinding();
		} else if (initializerParent instanceof VariableDeclaration) {
			ITypeBinding typeBinding= ((VariableDeclaration) initializerParent).getName().resolveTypeBinding();
			if (typeBinding != null) {
				return typeBinding.getElementType();
			}
		}
		return null;
	}

	private static ITypeBinding getTargetTypeForReturnStmt(ReturnStatement returnStmt) {
		LambdaExpression enclosingLambdaExpr= ASTResolving.findEnclosingLambdaExpression(returnStmt);
		if (enclosingLambdaExpr != null) {
			IMethodBinding methodBinding= enclosingLambdaExpr.resolveMethodBinding();
			return methodBinding == null ? null : methodBinding.getReturnType();
		}
		MethodDeclaration enclosingMethodDecl= ASTResolving.findParentMethodDeclaration(returnStmt);
		if (enclosingMethodDecl != null) {
			IMethodBinding methodBinding= enclosingMethodDecl.resolveBinding();
			return methodBinding == null ? null : methodBinding.getReturnType();
		}
		return null;
	}

	/**
	 * Returns the statement at the same level as its siblings.
	 *
	 * @param node the start node
	 * @return the statement at the same level
	 */
	private static Statement statementAtLevel(final Statement node) {
		ASTNode parent= node.getParent();

		if (parent instanceof LabeledStatement) {
			return statementAtLevel((LabeledStatement) parent);
		}

		return node;
	}

	/**
	 * Returns true if a sibling may exist.
	 *
	 * @param node the start node
	 * @return true if a sibling may exist
	 */
	public static boolean canHaveSiblings(final Statement node) {
		ASTNode statementAtLevel= statementAtLevel(node);
		ASTNode parent= statementAtLevel.getParent();

		return parent instanceof Block
				|| parent instanceof SwitchStatement && statementAtLevel.getLocationInParent() == SwitchStatement.STATEMENTS_PROPERTY;
	}

	/**
	 * Returns the previous statement in the same block if it exists.
	 *
	 * @param startNode the start node
	 * @return the previous statement in the same block if it exists, null otherwise
	 */
	public static Statement getPreviousSibling(final Statement startNode) {
		List<Statement> siblings= getSiblings(startNode, false);

		if (siblings.isEmpty()) {
			return null;
		}

		return siblings.get(siblings.size() - 1);
	}

	/**
	 * Returns the previous statement in the source file if it exists.
	 *
	 * @param startNode the start node
	 * @return the previous statement in the source file if it exists, null
	 *         otherwise
	 */
	public static Statement getPreviousStatement(final Statement startNode) {
		Statement previousSibling= getPreviousSibling(startNode);
		if (previousSibling != null) {
			return previousSibling;
		}
		ASTNode parent= startNode.getParent();
		if (parent instanceof Statement) {
			return getPreviousStatement((Statement) parent);
		}

		return null;
	}

	/**
	 * Returns the previous statements in the same block if it exists.
	 *
	 * @param startNode the start node
	 * @return the previous statements in the same block if it exists, empty list
	 *         otherwise
	 */
	public static List<Statement> getPreviousSiblings(final Statement startNode) {
		return getSiblings(startNode, false);
	}

	private static List<Statement> getSiblings(final Statement startNode, final boolean isForward) {
		Statement statementAtLevel= statementAtLevel(startNode);

		if (canHaveSiblings(statementAtLevel)) {
			List<Statement> statements;
			if (statementAtLevel.getParent() instanceof SwitchStatement) {
				statements= ((SwitchStatement) statementAtLevel.getParent()).statements();
			} else {
				statements= asList((Statement) statementAtLevel.getParent());
			}

			int indexOfNode= statements.indexOf(statementAtLevel);
			int siblingIndex= indexOfNode + (isForward ? 1 : -1);

			if (0 <= siblingIndex && siblingIndex < statements.size()) {
				if (isForward) {
					return statements.subList(siblingIndex, statements.size());
				}

				return statements.subList(0, siblingIndex + 1);
			}
		}

		return Collections.emptyList();
	}

	/**
	 * Returns the next statement in the source file if it exists.
	 *
	 * @param startNode the start node
	 * @return the next statement in the source file if it exists, null otherwise
	 */
	public static Statement getNextStatement(final Statement startNode) {
		Statement nextSibling= getNextSibling(startNode);

		if (nextSibling != null) {
			return nextSibling;
		}

		ASTNode parent= startNode.getParent();
		if (parent instanceof Statement) {
			return getNextStatement((Statement) parent);
		}

		return null;
	}

	/**
	 * Returns the next statement in the same block if it exists.
	 *
	 * @param startNode the start node
	 * @return the next statement in the same block if it exists, null otherwise
	 */
	public static Statement getNextSibling(final Statement startNode) {
		List<Statement> siblings= getSiblings(startNode, true);

		if (siblings.isEmpty()) {
			return null;
		}

		return siblings.get(0);
	}

	/**
	 * Returns the next statements in the same block if it exists.
	 *
	 * @param startNode the start node
	 * @return the next statements in the same block if it exists, empty list
	 *         otherwise
	 */
	public static List<Statement> getNextSiblings(final Statement startNode) {
		return getSiblings(startNode, true);
	}

	/**
	 * Returns whether an expression at the given location needs explicit boxing.
	 *
	 * @param expression the expression
	 * @return <code>true</code> iff an expression at the given location needs explicit boxing
	 * @since 3.6
	 */
	private static boolean needsExplicitBoxing(Expression expression) {
		StructuralPropertyDescriptor locationInParent= expression.getLocationInParent();
		if (locationInParent == ParenthesizedExpression.EXPRESSION_PROPERTY)
			return needsExplicitBoxing((ParenthesizedExpression) expression.getParent());

		if (locationInParent == ClassInstanceCreation.EXPRESSION_PROPERTY
				|| locationInParent == FieldAccess.EXPRESSION_PROPERTY
				|| locationInParent == MethodInvocation.EXPRESSION_PROPERTY)
			return true;

		return false;
	}

	/**
	 * Checks whether the given expression is a lambda expression with explicitly typed parameters.
	 *
	 * @param expression the expression to check
	 * @return <code>true</code> if the expression is a lambda expression with explicitly typed
	 *         parameters or no parameters, <code>false</code> otherwise
	 */
	public static boolean isExplicitlyTypedLambda(Expression expression) {
		if (!(expression instanceof LambdaExpression))
			return false;
		LambdaExpression lambda= (LambdaExpression) expression;
		List<VariableDeclaration> parameters= lambda.parameters();
		if (parameters.isEmpty())
			return true;
		return parameters.get(0) instanceof SingleVariableDeclaration;
	}

	/**
	 * Returns the first ancestor of the provided node which has the required type.
	 *
	 * @param <T>           the required ancestor's type
	 * @param node          the start node
	 * @param ancestorClass the required ancestor's type
	 * @return the first ancestor of the provided node which has the required type,
	 *         {@code null} if no suitable ancestor can be found
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ASTNode> T getTypedAncestor(final ASTNode node, final Class<T> ancestorClass) {
		if (node == null || node.getParent() == null) {
			return null;
		}

		ASTNode parent= node.getParent();

		if (ancestorClass.isAssignableFrom(parent.getClass())) {
			return (T) parent;
		}

		return getTypedAncestor(parent, ancestorClass);
	}

	/**
	 * Returns the first ancestor of the provided node which has any of the required types.
	 *
	 * @param node the start node
	 * @param ancestorClass the required ancestor's type
	 * @param ancestorClasses the required ancestor's types
	 * @return the first ancestor of the provided node which has any of the required type, or
	 *         {@code null}
	 */
	@SafeVarargs
	public static ASTNode getFirstAncestorOrNull(final ASTNode node, final Class<? extends ASTNode> ancestorClass, final Class<? extends ASTNode>... ancestorClasses) {
		if (node == null || node.getParent() == null) {
			return null;
		}

		ASTNode parent= node.getParent();

		if (ancestorClass.isAssignableFrom(parent.getClass())
				|| instanceOf(parent, ancestorClasses)) {
			return parent;
		}

		return getFirstAncestorOrNull(parent, ancestorClass, ancestorClasses);
	}

	/**
	 * Returns the first ancestor of the provided node which has any of the required types.
	 *
	 * @param node the start node
	 * @param ancestorClass the required ancestor's type
	 * @return the first ancestor of the provided node which has any of the required type, or
	 *         {@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ASTNode> T getFirstAncestorOrNull(final ASTNode node, final Class<T> ancestorClass) {
		if (node == null || node.getParent() == null) {
			return null;
		}

		ASTNode parent= node.getParent();

		if (ancestorClass.isAssignableFrom(parent.getClass())
				) {
			return (T) parent;
		}

		return getFirstAncestorOrNull(parent, ancestorClass);
	}

	/**
	 * Returns the closest ancestor of <code>node</code> that is an instance of <code>parentClass</code>, or <code>null</code> if none.
	 * <p>
	 * <b>Warning:</b> This method does not stop at any boundaries like parentheses, statements, body declarations, etc.
	 * The resulting node may be in a totally different scope than the given node.
	 * Consider using one of the {@link ASTResolving}<code>.find(..)</code> methods instead.
	 * </p>
	 * @param node the node
	 * @param parentClass the class of the sought ancestor node
	 * @return the closest ancestor of <code>node</code> that is an instance of <code>parentClass</code>, or <code>null</code> if none
	 */
	public static <T extends ASTNode> T getParent(ASTNode node, Class<T> parentClass) {
		do {
			node= node.getParent();
		} while (node != null && !parentClass.isInstance(node));
		return parentClass.cast(node);
	}

	/**
	 * Returns the closest ancestor of <code>node</code> whose type is <code>nodeType</code>, or <code>null</code> if none.
	 * <p>
	 * <b>Warning:</b> This method does not stop at any boundaries like parentheses, statements, body declarations, etc.
	 * The resulting node may be in a totally different scope than the given node.
	 * Consider using one of the {@link ASTResolving}<code>.find(..)</code> methods instead.
	 * </p>
	 * @param node the node
	 * @param nodeType the node type constant from {@link ASTNode}
	 * @return the closest ancestor of <code>node</code> whose type is <code>nodeType</code>, or <code>null</code> if none
	 */
	public static ASTNode getParent(ASTNode node, int nodeType) {
		do {
			node= node.getParent();
		} while (node != null && node.getNodeType() != nodeType);
		return node;
	}

	public static ASTNode findParent(ASTNode node, StructuralPropertyDescriptor[][] pathes) {
		for (StructuralPropertyDescriptor[] path : pathes) {
			ASTNode current= node;
			int d= path.length - 1;
			for (; d >= 0 && current != null; d--) {
				StructuralPropertyDescriptor descriptor= path[d];
				if (!descriptor.equals(current.getLocationInParent()))
					break;
				current= current.getParent();
			}
			if (d < 0)
				return current;
		}
		return null;
	}

	/**
	 * Returns the highest compatible parent node only linked by a chain of accepted classes.
	 *
	 * @param node            the node
	 * @param compatibleClasses the classes to include when looking for the parent
	 *                        node
	 * @return the last parent node of the provided classes, or the current node
	 *         otherwise
	 */
	@SafeVarargs
	public static ASTNode getHighestCompatibleNode(final ASTNode node, final Class<? extends ASTNode>... compatibleClasses) {
		ASTNode parent= node.getParent();

		if (instanceOf(parent, compatibleClasses)) {
			return getHighestCompatibleNode(parent, compatibleClasses);
		}

		return node;
	}

	@SafeVarargs
	private static boolean instanceOf(final ASTNode node, final Class<? extends ASTNode>... classes) {
		if (node == null) {
			return false;
		}

		for (Class<? extends ASTNode> clazz : classes) {
			if (clazz.isAssignableFrom(node.getClass())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * For {@link Name} or {@link Type} nodes, returns the topmost {@link Type} node
	 * that shares the same type binding as the given node.
	 *
	 * @param node an ASTNode
	 * @return the normalized {@link Type} node or the original node
	 */
	public static ASTNode getNormalizedNode(ASTNode node) {
		ASTNode current= node;
		// normalize name
		if (QualifiedName.NAME_PROPERTY.equals(current.getLocationInParent())) {
			current= current.getParent();
		}
		// normalize type
		if (QualifiedType.NAME_PROPERTY.equals(current.getLocationInParent())
				|| SimpleType.NAME_PROPERTY.equals(current.getLocationInParent())
				|| NameQualifiedType.NAME_PROPERTY.equals(current.getLocationInParent())) {
			current= current.getParent();
		}
		// normalize parameterized types
		if (ParameterizedType.TYPE_PROPERTY.equals(current.getLocationInParent())) {
			current= current.getParent();
		}
		return current;
	}

	/**
	 * Returns a set made of all the method bindings which are overridden by the
	 * provided method binding.
	 *
	 * @param overridingMethod the overriding method binding
	 * @return a set made of all the method bindings which are overridden by the
	 *         provided method binding
	 */
	public static Set<IMethodBinding> getOverridenMethods(final IMethodBinding overridingMethod) {
		Set<IMethodBinding> results= new HashSet<>();
		findOverridenMethods(overridingMethod, results, overridingMethod.getDeclaringClass());
		return results;
	}

	private static void findOverridenMethods(final IMethodBinding overridingMethod, final Set<IMethodBinding> results,
			final ITypeBinding declaringClass) {
		ITypeBinding superclass= declaringClass.getSuperclass();
		if (superclass != null && !addOverridenMethods(overridingMethod, superclass, results)) {
			findOverridenMethods(overridingMethod, results, superclass);
		}

		for (ITypeBinding itf : declaringClass.getInterfaces()) {
			if (!addOverridenMethods(overridingMethod, itf, results)) {
				findOverridenMethods(overridingMethod, results, itf);
			}
		}
	}

	private static boolean addOverridenMethods(final IMethodBinding overridingMethod, final ITypeBinding superType,
			final Set<IMethodBinding> results) {
		for (IMethodBinding methodFromType : superType.getDeclaredMethods()) {
			if (overridingMethod.overrides(methodFromType) && !results.add(methodFromType)) {
				// Type has already been visited
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns the null-checked expression if the provided node is a null check.
	 *
	 * @param expression the suspected null-checked expression
	 * @return the null-checked expression if the provided node is a null-check, or
	 *         {@code null} otherwise.
	 */
	public static Expression getNullCheckedExpression(final Expression expression) {
		InfixExpression infixExpression= as(expression, InfixExpression.class);

		if (infixExpression != null
				&& !infixExpression.hasExtendedOperands()
						&& hasOperator(infixExpression, InfixExpression.Operator.NOT_EQUALS)) {
			if (is(infixExpression.getRightOperand(), NullLiteral.class)) {
				return infixExpression.getLeftOperand();
			}

			if (is(infixExpression.getLeftOperand(), NullLiteral.class)) {
				return infixExpression.getRightOperand();
			}
		}

		return null;
	}

	/**
	 * Returns the unique {@link VariableDeclarationFragment} declared in the
	 * provided {@link VariableDeclarationStatement}.
	 *
	 * @param node the statement from which to extract the unique fragment
	 * @return the unique fragment declared in the provided variable declaration
	 *         statement, or {@code null} if more than one exist.
	 */
	public static VariableDeclarationFragment getUniqueFragment(final Statement node) {
		VariableDeclarationStatement statement= as(node, VariableDeclarationStatement.class);

		if (statement == null) {
			return null;
		}

		List<VariableDeclarationFragment> fragments= statement.fragments();
		return fragments.size() == 1 ? fragments.get(0) : null;
	}

	/**
     * Returns the same node after removing any parentheses around it.
     *
     * @param node the node around which parentheses must be removed
     * @return the same node after removing any parentheses around it. If there are
     *         no parentheses around it then the exact same node is returned
     */
    public static ASTNode getUnparenthesedExpression(ASTNode node) {
        if (node instanceof Expression) {
            return getUnparenthesedExpression((Expression) node);
        }
        return node;
    }

    /**
     * Returns the same expression after removing any parentheses around it.
     *
     * @param expression the expression around which parentheses must be removed
     * @return the same expression after removing any parentheses around it If there
     *         are no parentheses around it then the exact same expression is
     *         returned
     */
    public static Expression getUnparenthesedExpression(Expression expression) {
		while (expression != null && expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			expression= ((ParenthesizedExpression) expression).getExpression();
        }
        return expression;
    }

	/**
	 * Returns <code>true</code> iff <code>parent</code> is a true ancestor of <code>node</code>
	 * (i.e. returns <code>false</code> if <code>parent == node</code>).
	 *
	 * @param node node to test
	 * @param parent assumed parent
	 * @return <code>true</code> iff <code>parent</code> is a true ancestor of <code>node</code>
	 */
	public static boolean isParent(ASTNode node, ASTNode parent) {
		Assert.isNotNull(parent);
		do {
			node= node.getParent();
			if (node == parent)
				return true;
		} while (node != null);
		return false;
	}

	/**
	 * Returns whether the provided binding represents a local variable.
	 *
	 * @param binding the binding to analyze
	 * @return {@code true} if the provided binding represents a local variable,
	 *         {@code false} otherwise
	 */
	public static boolean isLocalVariable(final IBinding binding) {
		if (binding != null && binding.getKind() == IBinding.VARIABLE) {
			IVariableBinding bnd= (IVariableBinding) binding;
			return !bnd.isField() && !bnd.isEnumConstant();
		}

		return false;
	}

	/**
	 * Returns whether the provided binding and expression represent the same local
	 * variable.
	 *
	 * @param binding    the binding to analyze
	 * @param expression the expression to analyze
	 * @return {@code true} if the provided binding and expression represent the
	 *         same local variable, {@code false} otherwise
	 */
	public static boolean isSameLocalVariable(final IBinding binding, final Expression expression) {
		return isLocalVariable(binding) && expression != null && expression.getNodeType() == ASTNode.SIMPLE_NAME
				// No need to use IVariableBinding.isEqualTo(IBinding) since we are looking for
				// a *local* variable
				&& binding.equals(((SimpleName) expression).resolveBinding());
	}

	/**
	 * Returns whether the provided expressions represent the same local variable.
	 *
	 * @param expr1 the first expression to analyze
	 * @param expr2 the second expression to analyze
	 * @return {@code true} if the provided expressions represent the same local
	 *         variable, {@code false} otherwise
	 */
	public static boolean isSameLocalVariable(final Expression expr1, final Expression expr2) {
		return expr1 != null && expr1.getNodeType() == ASTNode.SIMPLE_NAME
				&& isSameLocalVariable(((SimpleName) expr1).resolveBinding(), expr2);
	}

	/**
	 * Returns whether the provided variable declaration and expression represent
	 * the same local variable.
	 *
	 * @param varDecl    the variable declaration to analyze
	 * @param expression the expression to analyze
	 * @return {@code true} if the provided nodes represent the same local variable,
	 *         {@code false} otherwise
	 */
	public static boolean isSameLocalVariable(final VariableDeclaration varDecl, final Expression expression) {
		return varDecl != null && isSameLocalVariable(varDecl.resolveBinding(), expression);
	}

	private static boolean areVariableBindingsEqual(final ASTNode node1, final ASTNode node2) {
		return areBindingsEqual(varBinding(node1), varBinding(node2));
	}

	/**
	 * Returns whether to bindings are equal.
	 *
	 * @param ast1 the first binding
	 * @param ast2 the second binding
	 * @return {@code true} when bindings are equal, {@code false} otherwise
	 */
	public static boolean areBindingsEqual(final IBinding ast1, final IBinding ast2) {
		return ast1 != null && ast2 != null && ast1.isEqualTo(ast2);
	}

	private static IBinding varBinding(final ASTNode node) {
		switch (node.getNodeType()) {
		case ASTNode.THIS_EXPRESSION:
			return ((ThisExpression) node).resolveTypeBinding();

		case ASTNode.FIELD_ACCESS:
			return ((FieldAccess) node).resolveFieldBinding();

		case ASTNode.QUALIFIED_NAME:
		case ASTNode.SIMPLE_NAME:
			return ((Name) node).resolveBinding();

		case ASTNode.SINGLE_VARIABLE_DECLARATION:
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
			return ((VariableDeclaration) node).resolveBinding();

		default:
			return null;
		}
	}

	/**
	 * Returns whether the two provided names represent the same variable.
	 *
	 * @param name1 the first name to compare
	 * @param name2 the second name to compare
	 * @return true if the two provided names represent the same variable, false
	 *         otherwise
	 */
	public static boolean isSameVariable(final SimpleName name1, final QualifiedName name2) {
		return false;
	}

	/**
	 * Returns whether the two provided expressions represent the same variable.
	 *
	 * @param name1  the first expression to compare
	 * @param field2 the second expression to compare
	 * @return true if the two provided expressions represent the same variable,
	 *         false otherwise
	 */
	public static boolean isSameVariable(final SimpleName name1, final FieldAccess field2) {
		return as(field2.getExpression(), ThisExpression.class) != null && areVariableBindingsEqual(field2, name1);
	}

	/**
	 * Returns whether the two provided qualified names represent the same variable.
	 *
	 * @param name1 the first qualified name to compare
	 * @param name2 the second qualified name to compare
	 * @return true if the two provided qualified names represent the same variable,
	 *         false otherwise
	 */
	public static boolean isSameVariable(final QualifiedName name1, final QualifiedName name2) {
		return areVariableBindingsEqual(name1, name2) && isSameVariable(name1.getQualifier(), name2.getQualifier());
	}

	/**
	 * Returns whether the two provided expressions represent the same variable.
	 *
	 * @param name1  the first expression to compare
	 * @param field2 the second expression to compare
	 * @return true if the two provided expressions represent the same variable,
	 *         false otherwise
	 */
	public static boolean isSameVariable(final QualifiedName name1, final FieldAccess field2) {
		return areVariableBindingsEqual(name1, field2) && isSameVariable(field2.getExpression(), name1.getQualifier());
	}

	/**
	 * Returns whether the two provided field accesses represent the same variable.
	 *
	 * @param field1 the first field access to compare
	 * @param field2 the second field access to compare
	 * @return true if the two provided field accesses represent the same variable,
	 *         false otherwise
	 */
	public static boolean isSameVariable(final FieldAccess field1, final FieldAccess field2) {
		return areVariableBindingsEqual(field1, field2)
				&& isSameVariable(field1.getExpression(), field2.getExpression());
	}

	/**
	 * Returns whether the provided nodes all represent the same variable.
	 *
	 * @param node0      the first node to compare
	 * @param otherNodes the other nodes to compare
	 * @return true if all the provided nodes represent the same variable, false
	 *         otherwise
	 */
	public static boolean areSameVariables(final ASTNode node0, final ASTNode... otherNodes) {
		for (ASTNode nodeN : otherNodes) {
			if (!isSameVariable(node0, nodeN)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns whether the two provided nodes represent the same variable.
	 *
	 * @param node1 the first node to compare
	 * @param node2 the second node to compare
	 * @return true if the two provided nodes represent the same variable, false
	 *         otherwise
	 */
	public static boolean isSameVariable(ASTNode node1, ASTNode node2) {
		node1= getUnparenthesedExpression(node1);
		node2= getUnparenthesedExpression(node2);

		if (node1 == null || node2 == null) {
			return false;
		}

		switch (node1.getNodeType()) {
		case ASTNode.THIS_EXPRESSION:
			return node2.getNodeType() == ASTNode.THIS_EXPRESSION;

		case ASTNode.SIMPLE_NAME:
			SimpleName sn= (SimpleName) node1;
			switch (node2.getNodeType()) {
			case ASTNode.QUALIFIED_NAME:
				return isSameVariable(sn, (QualifiedName) node2);

			case ASTNode.FIELD_ACCESS:
				return isSameVariable(sn, (FieldAccess) node2);
			}
			break;

		case ASTNode.QUALIFIED_NAME:
			QualifiedName qn= (QualifiedName) node1;
			switch (node2.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				return isSameVariable((SimpleName) node2, qn);

			case ASTNode.QUALIFIED_NAME:
				return isSameVariable(qn, (QualifiedName) node2);

			case ASTNode.FIELD_ACCESS:
				return isSameVariable(qn, (FieldAccess) node2);
			}
			break;

		case ASTNode.FIELD_ACCESS:
			FieldAccess fa= (FieldAccess) node1;
			switch (node2.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				return isSameVariable((SimpleName) node2, fa);

			case ASTNode.QUALIFIED_NAME:
				return isSameVariable((QualifiedName) node2, fa);

			case ASTNode.FIELD_ACCESS:
				return isSameVariable(fa, (FieldAccess) node2);
			}
		}

		return areVariableBindingsEqual(node1, node2);
	}

	public static int getExclusiveEnd(ASTNode node){
		return node.getStartPosition() + node.getLength();
	}

	public static int getInclusiveEnd(ASTNode node){
		return node.getStartPosition() + node.getLength() - 1;
	}

	public static IMethodBinding getMethodBinding(Name node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IMethodBinding)
			return (IMethodBinding)binding;
		return null;
	}

	public static IVariableBinding getVariableBinding(Name node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IVariableBinding)
			return (IVariableBinding)binding;
		return null;
	}

	public static IVariableBinding getLocalVariableBinding(Name node) {
		IVariableBinding result= getVariableBinding(node);
		if (result == null || result.isField())
			return null;

		return result;
	}

	public static IVariableBinding getFieldBinding(Name node) {
		IVariableBinding result= getVariableBinding(node);
		if (result == null || !result.isField())
			return null;

		return result;
	}

	public static ITypeBinding getTypeBinding(Name node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof ITypeBinding)
			return (ITypeBinding)binding;
		return null;
	}

	/**
	 * Returns whether the provided expression evaluates to a primitive type.
	 *
	 * @param expression        the expression to analyze
	 * @param primitiveTypeName the primitive type name
	 * @return true if the provided expression evaluates to a primitive type, false
	 *         otherwise
	 */
	public static boolean isPrimitive(final Expression expression, final String primitiveTypeName) {
		return expression != null && isPrimitive(expression.resolveTypeBinding(), primitiveTypeName);
	}

	/**
	 * Returns whether the provided type binding represents the provided primitive
	 * type.
	 *
	 * @param typeBinding       the type binding to analyze
	 * @param primitiveTypeName the primitive type name
	 * @return true if the provided type binding represents the provided primitive
	 *         type, false otherwise
	 */
	private static boolean isPrimitive(final ITypeBinding typeBinding, final String primitiveTypeName) {
		return typeBinding != null && typeBinding.isPrimitive()
				&& typeBinding.getQualifiedName().equals(primitiveTypeName);
	}

	/**
	 * Returns the receiver's type binding of the given method invocation.
	 *
	 * @param invocation method invocation to resolve type of
	 * @return the type binding of the receiver
	 */
	public static ITypeBinding getReceiverTypeBinding(MethodInvocation invocation) {
		ITypeBinding result= null;
		Expression exp= invocation.getExpression();
		if(exp != null) {
			return exp.resolveTypeBinding();
		}
		else {
			AbstractTypeDeclaration type= getParent(invocation, AbstractTypeDeclaration.class);
			if (type != null)
				return type.resolveBinding();
		}
		return result;
	}

	public static ITypeBinding getEnclosingType(ASTNode node) {
		while(node != null) {
			if (node instanceof AbstractTypeDeclaration) {
				return ((AbstractTypeDeclaration)node).resolveBinding();
			} else if (node instanceof AnonymousClassDeclaration) {
				return ((AnonymousClassDeclaration)node).resolveBinding();
			}
			node= node.getParent();
		}
		return null;
	}

	public static IProblem[] getProblems(ASTNode node, int scope, int severity) {
		ASTNode root= node.getRoot();
		if (!(root instanceof CompilationUnit))
			return EMPTY_PROBLEMS;
		IProblem[] problems= ((CompilationUnit)root).getProblems();
		if (root == node)
			return problems;
		final int iterations= computeIterations(scope);
		List<IProblem> result= new ArrayList<>(5);
		for (IProblem problem : problems) {
			boolean consider= false;
			if ((severity & PROBLEMS) == PROBLEMS)
				consider= true;
			else if ((severity & WARNING) != 0)
				consider= problem.isWarning();
			else if ((severity & ERROR) != 0)
				consider= problem.isError();
			else if ((severity & INFO) != 0)
				consider= problem.isInfo();
			if (consider) {
				ASTNode temp= node;
				int count= iterations;
				do {
					int nodeOffset= temp.getStartPosition();
					int problemOffset= problem.getSourceStart();
					if (nodeOffset <= problemOffset && problemOffset < nodeOffset + temp.getLength()) {
						result.add(problem);
						count= 0;
					} else {
						count--;
					}
				} while ((temp= temp.getParent()) != null && count > 0);
			}
		}
		return result.toArray(new IProblem[result.size()]);
	}

	public static Message[] getMessages(ASTNode node, int flags) {
		ASTNode root= node.getRoot();
		if (!(root instanceof CompilationUnit))
			return EMPTY_MESSAGES;
		Message[] messages= ((CompilationUnit)root).getMessages();
		if (root == node)
			return messages;
		final int iterations= computeIterations(flags);
		List<Message> result= new ArrayList<>(5);
		for (Message message : messages) {
			ASTNode temp= node;
			int count= iterations;
			do {
				int nodeOffset= temp.getStartPosition();
				int messageOffset= message.getStartPosition();
				if (nodeOffset <= messageOffset && messageOffset < nodeOffset + temp.getLength()) {
					result.add(message);
					count= 0;
				} else {
					count--;
				}
			} while ((temp= temp.getParent()) != null && count > 0);
		}
		return result.toArray(new Message[result.size()]);
	}

	private static int computeIterations(int flags) {
		switch (flags) {
			case NODE_ONLY:
				return 1;
			case INCLUDE_ALL_PARENTS:
				return Integer.MAX_VALUE;
			case INCLUDE_FIRST_PARENT:
				return 2;
			default:
				return 1;
		}
	}

	public static SimpleName getLeftMostSimpleName(Name name) {
		if (name instanceof SimpleName) {
			return (SimpleName)name;
		} else {
			final SimpleName[] result= new SimpleName[1];
			ASTVisitor visitor= new ASTVisitor() {
				@Override
				public boolean visit(QualifiedName qualifiedName) {
					Name left= qualifiedName.getQualifier();
					if (left instanceof SimpleName)
						result[0]= (SimpleName)left;
					else
						left.accept(this);
					return false;
				}
			};
			name.accept(visitor);
			return result[0];
		}
	}

	/**
	 * Returns the topmost ancestor of <code>name</code> that is still a {@link Name}.
	 * <p>
	 * <b>Note:</b> The returned node may resolve to a different binding than the given <code>name</code>!
	 *
	 * @param name a name node
	 * @return the topmost name
	 * @see #getNormalizedNode(ASTNode)
	 */
	public static Name getTopMostName(Name name) {
		Name result= name;
		while(result.getParent() instanceof Name) {
			result= (Name)result.getParent();
		}
		return result;
	}

	/**
	 * Returns the topmost ancestor of <code>node</code> that is a {@link Type} (but not a {@link UnionType}).
	 * <p>
	 * <b>Note:</b> The returned node often resolves to a different binding than the given <code>node</code>!
	 *
	 * @param node the starting node, can be <code>null</code>
	 * @return the topmost type or <code>null</code> if the node is not a descendant of a type node
	 * @see #getNormalizedNode(ASTNode)
	 */
	public static Type getTopMostType(ASTNode node) {
		ASTNode result= null;
		while (node instanceof Type && !(node instanceof UnionType)
				|| node instanceof Name
				|| node instanceof Annotation || node instanceof MemberValuePair
				|| node instanceof Expression) { // Expression could maybe be reduced to expression node types that can appear in an annotation
			result= node;
			node= node.getParent();
		}

		if (result instanceof Type)
			return (Type) result;

		return null;
	}

	public static int changeVisibility(int modifiers, int visibility) {
		return (modifiers & CLEAR_VISIBILITY) | visibility;
	}

	/**
	 * Adds flags to the given node and all its descendants.
	 * @param root The root node
	 * @param flags The flags to set
	 */
	public static void setFlagsToAST(ASTNode root, final int flags) {
		root.accept(new GenericVisitor(true) {
			@Override
			protected boolean visitNode(ASTNode node) {
				node.setFlags(node.getFlags() | flags);
				return true;
			}
		});
	}

	public static String getQualifier(Name name) {
		if (name.isQualifiedName()) {
			return ((QualifiedName) name).getQualifier().getFullyQualifiedName();
		}
		return ""; //$NON-NLS-1$
	}

	public static String getSimpleNameIdentifier(Name name) {
		if (name.isQualifiedName()) {
			return ((QualifiedName) name).getName().getIdentifier();
		} else {
			return ((SimpleName) name).getIdentifier();
		}
	}

	public static boolean isDeclaration(Name name) {
		if (name.isQualifiedName()) {
			return ((QualifiedName) name).getName().isDeclaration();
		} else {
			return ((SimpleName) name).isDeclaration();
		}
	}

	/**
	 * Returns whether the provided qualified name accesses a field with the
	 * provided signature.
	 *
	 * @param node              the qualified name to compare
	 * @param qualifiedTypeName the qualified name of the type declaring the field
	 * @param fieldNames        the field names
	 * @return true if the provided qualified name matches the provided field
	 *         signature, false otherwise
	 */
	public static boolean isField(final QualifiedName node, final String qualifiedTypeName, final String... fieldNames) {
		return instanceOf(node, qualifiedTypeName)
				&& Arrays.asList(fieldNames).contains(node.getName().getIdentifier());
	}

	/**
	 * Returns whether a checked exception is supposed to be caught.
	 *
	 * @param node the node
	 * @return true if a checked exception is supposed to be caught.
	 */
	public static boolean isExceptionExpected(final ASTNode node) {
		ASTNode parentNode= getFirstAncestorOrNull(node, TryStatement.class, BodyDeclaration.class);

		while (parentNode instanceof TryStatement) {
			TryStatement tryStatement= (TryStatement) parentNode;
			List<CatchClause> catchClauses= tryStatement.catchClauses();

			for (CatchClause catchClause : catchClauses) {
				if (catchClause.getException().getType() != null
						&& !instanceOf(catchClause.getException().getType().resolveBinding(),
								RuntimeException.class.getCanonicalName())) {
					return true;
				}
			}

			parentNode= getFirstAncestorOrNull(parentNode, TryStatement.class, BodyDeclaration.class);
		}

		return false;
	}

	/**
	 * Returns if another method or constructor has the given signature.
	 * It is useful to know if a refactoring of the given method or constructor is possible without conflict.
	 * The method or constructor in parameter does not need to match this signature (usually not).
	 *
	 * @param methodOrConstructor A method, super method, constructor or super constructor
	 * @param methodOrConstructorBinding The associated binding
	 * @param expectedArgumentTypes The argument types that should match
	 * @return True if another method or constructor has the given signature
	 */
	public static boolean hasConflictingMethodOrConstructor(
			final ASTNode methodOrConstructor,
			final IMethodBinding methodOrConstructorBinding,
			final ITypeBinding[] expectedArgumentTypes) {
		TypeDeclaration typeDeclaration= getTypedAncestor(methodOrConstructor, TypeDeclaration.class);

		if (typeDeclaration == null) {
			return true;
		}

		ITypeBinding type= typeDeclaration.resolveBinding();

		if (type == null) {
			return true;
		}

		boolean inSameClass= true;
		// Figure out the type where we need to start looking at methods in the hierarchy.
		// If we have a new class instance or super method call or this expression or
		// we have a static call that is qualified, we use the referenced class as the starting point.
		// If we have a non-qualified method call, we use the class containing the call.
		// Otherwise, we bail on the clean-up.
		if (methodOrConstructor instanceof ClassInstanceCreation) {
			type= ((ClassInstanceCreation) methodOrConstructor).resolveTypeBinding();
			inSameClass= type.isNested();
		} else if (methodOrConstructor instanceof MethodInvocation) {
			MethodInvocation methodInvocation= (MethodInvocation) methodOrConstructor;
			Expression expression= methodInvocation.getExpression();

			if (expression == null) {
				ASTNode root= methodOrConstructor.getRoot();

				if (root instanceof CompilationUnit) {
					CompilationUnit compilationUnit= (CompilationUnit) root;
					List<ImportDeclaration> imports= compilationUnit.imports();
					String localPackage= null;

					if (compilationUnit.getPackage() != null && compilationUnit.getPackage().getName() != null) {
						localPackage= compilationUnit.getPackage().getName().getFullyQualifiedName();
					}

					for (ImportDeclaration oneImport : imports) {
						if (oneImport.isStatic()
								&& !oneImport.isOnDemand()
								&& oneImport.getName() instanceof QualifiedName) {
							QualifiedName methodName= (QualifiedName) oneImport.getName();
							String methodIdentifier= methodName.getName().getIdentifier();
							ITypeBinding conflictingType= methodName.getQualifier().resolveTypeBinding();

							if (conflictingType == null) {
								return true; // Error on side of caution
							}

							String importPackage= null;

							if (conflictingType.getPackage() != null) {
								importPackage= conflictingType.getPackage().getName();
							}

							boolean inSamePackage= Objects.equals(localPackage, importPackage);

							for (IMethodBinding declaredMethod : conflictingType.getDeclaredMethods()) {
								if (methodIdentifier.equals(declaredMethod.getName())
										&& isMethodMatching(expectedArgumentTypes, methodOrConstructorBinding, false, inSamePackage, declaredMethod)) {
									return true;
								}
							}
						}
					}
				}

				if (Modifier.isStatic(methodOrConstructorBinding.getModifiers())) {
					inSameClass= methodOrConstructorBinding.getDeclaringClass().isEqualTo(type);
					type= methodOrConstructorBinding.getDeclaringClass();
				}
			} else if (!(expression instanceof ThisExpression)) {
				inSameClass= methodOrConstructorBinding.getDeclaringClass().isEqualTo(type);
				type= expression.resolveTypeBinding();
			}
		} else if (methodOrConstructor instanceof SuperMethodInvocation) {
			inSameClass= type.isNested();
			type= type.getSuperclass();
		} else {
			return true; // Error on side of caution
		}

		if (type == null) {
			return true;
		}

		return hasEquivalentMethodForInheritedTypes(expectedArgumentTypes, methodOrConstructorBinding, type, type, inSameClass);
	}

	private static boolean hasEquivalentMethodForInheritedTypes(
			final ITypeBinding[] parameterTypesForConflictingMethod,
			final IMethodBinding binding,
			final ITypeBinding type,
			final ITypeBinding origType,
			final boolean wasInSameClass) {
		ITypeBinding superType= type;
		boolean inSameClass= wasInSameClass;

		while (superType != null) {
			IPackageBinding packageBinding= superType.getPackage();
			boolean inSamePackage= packageBinding.isEqualTo(origType.getPackage());

			if (hasEquivalentMethodForOneType(parameterTypesForConflictingMethod, binding, superType, inSameClass, inSamePackage)) {
				return true;
			}

			if (superType.isNested()) {
				if (hasEquivalentMethodForInheritedTypes(parameterTypesForConflictingMethod, binding, superType.getDeclaringClass(), origType, inSameClass)) {
					return true;
				}

				superType= superType.getSuperclass();
				inSameClass&= superType.isNested();
			} else {
				superType= superType.getSuperclass();
				inSameClass= false;
			}
		}

		return false;
	}

	private static boolean hasEquivalentMethodForOneType(
			final ITypeBinding[] parameterTypesForConflictingMethod,
			final IMethodBinding binding,
			final ITypeBinding type,
			final boolean inSameClass,
			final boolean inSamePackage) {
		for (IMethodBinding method : type.getDeclaredMethods()) {
			if (isMethodMatching(parameterTypesForConflictingMethod, binding, inSameClass, inSamePackage, method)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isMethodMatching(
			final ITypeBinding[] parameterTypesForConflictingMethod,
			final IMethodBinding binding,
			final boolean inSameClass,
			final boolean inSamePackage,
			final IMethodBinding testedMethod) {
		int methodModifiers= testedMethod.getModifiers();
		ITypeBinding[] parameterTypes= testedMethod.getParameterTypes();

		if (!binding.isEqualTo(testedMethod)
				&& parameterTypesForConflictingMethod.length == parameterTypes.length
				&& binding.getName().equals(testedMethod.getName())
				&& (inSameClass || Modifier.isPublic(methodModifiers) || Modifier.isProtected(methodModifiers)
						|| (inSamePackage && !Modifier.isPrivate(methodModifiers)))) {
			for (int i= 0; i < parameterTypesForConflictingMethod.length; i++) {
				if (parameterTypesForConflictingMethod[i] == null || parameterTypes[i] == null) {
					return true;
				}

				if (!parameterTypesForConflictingMethod[i].isAssignmentCompatible(parameterTypes[i])) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * Returns whether the provided method invocation invokes a method with the
	 * provided method signature. The method signature is compared against the
	 * erasure of the invoked method.
	 *
	 * @param node                         the method invocation to compare
	 * @param typeQualifiedName            the qualified name of the type declaring
	 *                                     the method
	 * @param methodName                   the method name
	 * @param parameterTypesQualifiedNames the qualified names of the parameter
	 *                                     types
	 * @return true if the provided method invocation matches the provided method
	 *         signature, false otherwise
	 */
	public static boolean usesGivenSignature(final MethodInvocation node, final String typeQualifiedName, final String methodName,
			final String... parameterTypesQualifiedNames) {
		return node != null
				&& usesGivenSignature(node.resolveMethodBinding(), typeQualifiedName, methodName, parameterTypesQualifiedNames);
	}

	/**
	 * Returns whether the provided method declaration declares a method with the
	 * provided method signature. The method signature is compared against the
	 * erasure of the declared method.
	 *
	 * @param actualMethod                 the actual method declaration
	 * @param typeQualifiedName            the expected qualified name of the type declaring
	 *                                     the expected method
	 * @param methodName                   the expected method name
	 * @param parameterTypesQualifiedNames the expected qualified names of the parameter
	 *                                     types
	 * @return true if the provided method declaration matches the provided method
	 *         signature, false otherwise
	 */
	public static boolean usesGivenSignature(final MethodDeclaration actualMethod, final String typeQualifiedName, final String methodName,
			final String... parameterTypesQualifiedNames) {
		return actualMethod != null
				&& usesGivenSignature(actualMethod.resolveBinding(), typeQualifiedName, methodName, parameterTypesQualifiedNames);
	}

	/**
	 * Returns whether the provided method binding has the provided method signature. The method
	 * signature is compared against the erasure of the invoked method.
	 *
	 * @param methodBinding the method binding to compare
	 * @param typeQualifiedName the qualified name of the type declaring the method
	 * @param methodName the method name
	 * @param parameterTypesQualifiedNames the qualified names of the parameter types
	 * @return true if the provided method invocation matches the provided method signature, false
	 *         otherwise
	 */
	public static boolean usesGivenSignature(final IMethodBinding methodBinding, final String typeQualifiedName, final String methodName,
			final String... parameterTypesQualifiedNames) {
		// Let's do the fast checks first
		if (methodBinding == null || !methodName.equals(methodBinding.getName())
				|| methodBinding.getParameterTypes().length != parameterTypesQualifiedNames.length) {
			return false;
		}

		// OK more heavy checks now
		ITypeBinding declaringClass= methodBinding.getDeclaringClass();
		ITypeBinding implementedType= findImplementedType(declaringClass, typeQualifiedName);

		if (parameterTypesMatch(implementedType, methodBinding, parameterTypesQualifiedNames)) {
			return true;
		}

		// A lot more heavy checks
		IMethodBinding overriddenMethod= findOverridenMethod(declaringClass, typeQualifiedName, methodName,
				parameterTypesQualifiedNames);

		if (overriddenMethod != null && methodBinding.overrides(overriddenMethod)) {
			return true;
		}

		IMethodBinding methodDeclaration= methodBinding.getMethodDeclaration();
		return methodDeclaration != null && methodDeclaration != methodBinding
				&& usesGivenSignature(methodDeclaration, typeQualifiedName, methodName, parameterTypesQualifiedNames);
	}

	private static boolean parameterTypesMatch(final ITypeBinding implementedType, final IMethodBinding methodBinding,
			final String[] parameterTypesQualifiedNames) {
		if (implementedType != null && !implementedType.isRawType()) {
			ITypeBinding erasure= implementedType.getErasure();

			if (erasure.isGenericType() || erasure.isParameterizedType()) {
				return parameterizedTypesMatch(implementedType, erasure, methodBinding);
			}
		}

		return implementedType != null && concreteTypesMatch(methodBinding.getParameterTypes(), parameterTypesQualifiedNames);
	}

	private static IMethodBinding findOverridenMethod(final ITypeBinding typeBinding, final String typeQualifiedName,
			final String methodName, final String[] parameterTypesQualifiedNames) {
		// Superclass
		ITypeBinding superclassBinding= typeBinding.getSuperclass();

		if (superclassBinding != null) {
			superclassBinding= superclassBinding.getErasure();

			if (typeQualifiedName.equals(superclassBinding.getErasure().getQualifiedName())) {
				// Found the type
				return findOverridenMethod(methodName, parameterTypesQualifiedNames,
						superclassBinding.getDeclaredMethods());
			}

			IMethodBinding overridenMethod= findOverridenMethod(superclassBinding, typeQualifiedName, methodName,
					parameterTypesQualifiedNames);

			if (overridenMethod != null) {
				return overridenMethod;
			}
		}

		// Interfaces
		for (ITypeBinding itfBinding : typeBinding.getInterfaces()) {
			itfBinding= itfBinding.getErasure();

			if (typeQualifiedName.equals(itfBinding.getQualifiedName())) {
				// Found the type
				return findOverridenMethod(methodName, parameterTypesQualifiedNames, itfBinding.getDeclaredMethods());
			}

			IMethodBinding overridenMethod= findOverridenMethod(itfBinding, typeQualifiedName, methodName,
					parameterTypesQualifiedNames);

			if (overridenMethod != null) {
				return overridenMethod;
			}
		}

		return null;
	}

	private static IMethodBinding findOverridenMethod(final String methodName, final String[] parameterTypesQualifiedNames,
			final IMethodBinding[] declaredMethods) {
		for (IMethodBinding methodBinding : declaredMethods) {
			IMethodBinding methodDecl= methodBinding.getMethodDeclaration();

			if (methodBinding.getName().equals(methodName) && methodDecl != null
					&& concreteTypesMatch(methodDecl.getParameterTypes(), parameterTypesQualifiedNames)) {
				return methodBinding;
			}
		}

		return null;
	}

	private static boolean concreteTypesMatch(final ITypeBinding[] typeBindings, final String... typesQualifiedNames) {
		if (typeBindings.length != typesQualifiedNames.length) {
			return false;
		}

		for (int i= 0; i < typesQualifiedNames.length; i++) {
			if (!typesQualifiedNames[i].equals(typeBindings[i].getQualifiedName())
					&& !typesQualifiedNames[i].equals(Bindings.getBoxedTypeName(typeBindings[i].getQualifiedName()))
					&& !typesQualifiedNames[i].equals(Bindings.getUnboxedTypeName(typeBindings[i].getQualifiedName()))) {
				return false;
			}
		}

		return true;
	}

	private static boolean parameterizedTypesMatch(final ITypeBinding clazz, final ITypeBinding clazzErasure,
			IMethodBinding methodBinding) {
		if (clazz.isParameterizedType() && !clazz.equals(clazzErasure)) {
			Map<ITypeBinding, ITypeBinding> genericToConcreteTypeParamsFromClass= getGenericToConcreteTypeParamsMap(
					clazz, clazzErasure);

			for (IMethodBinding declaredMethod : clazzErasure.getDeclaredMethods()) {
				if (declaredMethod.getName().equals(methodBinding.getName())) {
					Map<ITypeBinding, ITypeBinding> genericToConcreteTypeParams= getGenericToConcreteTypeParamsMap(
							methodBinding, declaredMethod);
					genericToConcreteTypeParams.putAll(genericToConcreteTypeParamsFromClass);

					if (parameterizedTypesMatch(genericToConcreteTypeParams, methodBinding, declaredMethod)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private static Map<ITypeBinding, ITypeBinding> getGenericToConcreteTypeParamsMap(final IMethodBinding method,
			final IMethodBinding methodErasure) {
		return getGenericToConcreteTypeParamsMap(method.getTypeArguments(), methodErasure.getTypeParameters());
	}

	private static Map<ITypeBinding, ITypeBinding> getGenericToConcreteTypeParamsMap(final ITypeBinding clazz,
			final ITypeBinding clazzErasure) {
		return getGenericToConcreteTypeParamsMap(clazz.getTypeArguments(), clazzErasure.getTypeParameters());
	}

	private static Map<ITypeBinding, ITypeBinding> getGenericToConcreteTypeParamsMap(final ITypeBinding[] typeParams,
			final ITypeBinding[] genericTypeParams) {
		final Map<ITypeBinding, ITypeBinding> results= new HashMap<>();
		for (int i= 0; i < genericTypeParams.length && i < typeParams.length; i++) {
			results.put(genericTypeParams[i], typeParams[i]);
		}
		return results;
	}

	private static boolean parameterizedTypesMatch(final Map<ITypeBinding, ITypeBinding> genericToConcreteTypeParams,
			final IMethodBinding parameterizedMethod, final IMethodBinding genericMethod) {
		ITypeBinding[] paramTypes= parameterizedMethod.getParameterTypes();
		ITypeBinding[] genericParamTypes= genericMethod.getParameterTypes();

		if (paramTypes.length != genericParamTypes.length) {
			return false;
		}

		for (int i= 0; i < genericParamTypes.length; i++) {
			ITypeBinding genericParamType= genericParamTypes[i];
			ITypeBinding concreteParamType= null;

			if (genericParamType.isArray()) {
				ITypeBinding concreteElementType= genericToConcreteTypeParams.get(genericParamType.getElementType());

				if (concreteElementType != null) {
					concreteParamType= concreteElementType.createArrayType(genericParamType.getDimensions());
				}
			} else {
				concreteParamType= genericToConcreteTypeParams.get(genericParamType);
			}

			if (concreteParamType == null) {
				concreteParamType= genericParamType;
			}

			final ITypeBinding erasure1= paramTypes[i].getErasure();
			final String erasureName1;
			if (erasure1.isPrimitive()) {
				erasureName1= Bindings.getBoxedTypeName(erasure1.getQualifiedName());
			} else {
				erasureName1= erasure1.getQualifiedName();
			}

			final ITypeBinding erasure2= concreteParamType.getErasure();
			final String erasureName2;
			if (erasure2.isPrimitive()) {
				erasureName2= Bindings.getBoxedTypeName(erasure2.getQualifiedName());
			} else {
				erasureName2= erasure2.getQualifiedName();
			}

			if (!erasureName1.equals(erasureName2)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the type binding for the provided qualified type name if it can be found in the type
	 * hierarchy of the provided type binding.
	 *
	 * @param typeBinding the type binding to analyze
	 * @param qualifiedTypeName the qualified type name to find
	 * @return the type binding for the provided qualified type name if it can be found in the type
	 *         hierarchy of the provided type binding, or {@code null} otherwise
	 */
	public static ITypeBinding findImplementedType(final ITypeBinding typeBinding, final String qualifiedTypeName) {
		if (typeBinding == null) {
			return null;
		}

		ITypeBinding typeErasure= typeBinding.getErasure();

		if (qualifiedTypeName.equals(typeBinding.getQualifiedName())
				|| qualifiedTypeName.equals(typeErasure.getQualifiedName())) {
			return typeBinding;
		}

		return findImplementedType2(typeBinding, qualifiedTypeName);
	}

	private static ITypeBinding findImplementedType2(final ITypeBinding typeBinding, final String qualifiedTypeName) {
		final ITypeBinding superclass= typeBinding.getSuperclass();

		if (superclass != null) {
			String superClassQualifiedName= superclass.getErasure().getQualifiedName();

			if (qualifiedTypeName.equals(superClassQualifiedName)) {
				return superclass;
			}

			ITypeBinding implementedType= findImplementedType2(superclass, qualifiedTypeName);

			if (implementedType != null) {
				return implementedType;
			}
		}

		for (ITypeBinding itfBinding : typeBinding.getInterfaces()) {
			String itfQualifiedName= itfBinding.getErasure().getQualifiedName();

			if (qualifiedTypeName.equals(itfQualifiedName)) {
				return itfBinding;
			}

			ITypeBinding implementedType= findImplementedType2(itfBinding, qualifiedTypeName);

			if (implementedType != null) {
				return implementedType;
			}
		}

		return null;
	}

	public static Modifier findModifierNode(int flag, List<IExtendedModifier> modifiers) {
		for (IExtendedModifier curr : modifiers) {
			if (curr instanceof Modifier && ((Modifier) curr).getKeyword().toFlagValue() == flag) {
				return (Modifier) curr;
			}
		}
		return null;
	}

	/**
	 * Returns whether the two provided codes structurally match.
	 *
	 * @param referenceStatements the first code to compare
	 * @param comparedStatements  the second code to compare
	 * @return true if the two provided codes structurally match, false otherwise
	 */
	public static boolean match(final List<Statement> referenceStatements, final List<Statement> comparedStatements) {
		return match(ASTSemanticMatcher.INSTANCE, referenceStatements, comparedStatements);
	}

	/**
	 * Returns whether the two provided codes structurally match.
	 *
	 * @param matcher the AST matcher
	 * @param referenceStatements the first code to compare
	 * @param comparedStatements  the second code to compare
	 * @return true if the two provided codes structurally match, false otherwise
	 */
	public static boolean match(final ASTSemanticMatcher matcher, final List<Statement> referenceStatements, final List<Statement> comparedStatements) {
		if (referenceStatements.size() != comparedStatements.size()) {
			return false;
		}

		for (int codeLine= 0; codeLine < referenceStatements.size(); codeLine++) {
			if (!match(matcher, referenceStatements.get(codeLine), comparedStatements.get(codeLine))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns whether the two provided nodes structurally match.
	 *
	 * @param node1 the first node to compare
	 * @param node2 the second node to compare
	 * @return true if the two provided nodes structurally match, false otherwise
	 */
	public static boolean match(final ASTNode node1, final ASTNode node2) {
		return match(ASTSemanticMatcher.INSTANCE, node1, node2);
	}

	/**
	 * Returns whether the two provided nodes structurally match.
	 *
	 * @param matcher the AST matcher
	 * @param node1   the first node to compare
	 * @param node2   the second node to compare
	 * @return true if the two provided nodes structurally match, false otherwise
	 */
	public static boolean match(final ASTSemanticMatcher matcher, final ASTNode node1, final ASTNode node2) {
		return matcher.safeSubtreeMatch(node1, node2);
	}

	public static ITypeBinding getTypeBinding(CompilationUnit root, IType type) throws JavaModelException {
		if (type.isAnonymous()) {
			final IJavaElement parent= type.getParent();
			if (parent instanceof IField && Flags.isEnum(((IMember) parent).getFlags())) {
				final EnumConstantDeclaration constant= (EnumConstantDeclaration) NodeFinder.perform(root, ((ISourceReference) parent).getSourceRange());
				if (constant != null) {
					final AnonymousClassDeclaration declaration= constant.getAnonymousClassDeclaration();
					if (declaration != null)
						return declaration.resolveBinding();
				}
			} else {
				final ClassInstanceCreation creation= getParent(NodeFinder.perform(root, type.getNameRange()), ClassInstanceCreation.class);
				if (creation != null)
					return creation.resolveTypeBinding();
			}
		} else {
			final AbstractTypeDeclaration declaration= getParent(NodeFinder.perform(root, type.getNameRange()), AbstractTypeDeclaration.class);
			if (declaration != null)
				return declaration.resolveBinding();
		}
		return null;
	}

	/**
	 * Escapes a string value to a literal that can be used in Java source.
	 *
	 * @param stringValue the string value
	 * @return the escaped string
	 * @see StringLiteral#getEscapedValue()
	 */
	public static String getEscapedStringLiteral(String stringValue) {
		StringLiteral stringLiteral= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false).newStringLiteral();
		stringLiteral.setLiteralValue(stringValue);
		return stringLiteral.getEscapedValue();
	}

	/**
	 * Escapes a character value to a literal that can be used in Java source.
	 *
	 * @param ch the character value
	 * @return the escaped string
	 * @see CharacterLiteral#getEscapedValue()
	 */
	public static String getEscapedCharacterLiteral(char ch) {
		CharacterLiteral characterLiteral= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false).newCharacterLiteral();
		characterLiteral.setCharValue(ch);
		return characterLiteral.getEscapedValue();
	}

	/**
	 * If the given <code>node</code> has already been rewritten, undo that rewrite and return the
	 * replacement version of the node. Otherwise, return the result of
	 * {@link ASTRewrite#createCopyTarget(ASTNode)}.
	 *
	 * @param rewrite ASTRewrite for the given node
	 * @param node the node to get the replacement or to create a copy placeholder for
	 * @param group the edit group which collects the corresponding text edits, or <code>null</code>
	 *            if ungrouped
	 * @return the replacement node if the given <code>node</code> has already been rewritten or the
	 *         new copy placeholder node
	 */
	public static ASTNode getCopyOrReplacement(ASTRewrite rewrite, ASTNode node, TextEditGroup group) {
		ASTNode rewrittenNode= (ASTNode) rewrite.get(node.getParent(), node.getLocationInParent());
		if (rewrittenNode != node) {
			// Undo previous rewrite to avoid the problem that the same node would be inserted in two places:
			rewrite.replace(rewrittenNode, node, group);
			return rewrittenNode;
		}
		return rewrite.createCopyTarget(node);
	}

	/**
	 * Type-safe variant of {@link ASTRewrite#createMoveTarget(ASTNode)}.
	 *
	 * @param rewrite ASTRewrite for the given node
	 * @param nodes the nodes to create a move placeholder for
	 * @return the new placeholder nodes
	 * @throws IllegalArgumentException if the node is null, or if the node
	 * is not part of the rewrite's AST
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ASTNode> List<T> createMoveTarget(final ASTRewrite rewrite, final Collection<T> nodes) {
		if (nodes != null) {
			List<T> newNodes= new ArrayList<>(nodes.size());

			for (T node : nodes) {
				newNodes.add((T) rewrite.createMoveTarget(node));
			}

			return newNodes;
		}

		return null;
	}

	/**
	 * Type-safe variant of {@link ASTRewrite#createMoveTarget(ASTNode)}.
	 *
	 * @param rewrite ASTRewrite for the given node
	 * @param node the node to create a move placeholder for
	 * @return the new placeholder node
	 * @throws IllegalArgumentException if the node is null, or if the node
	 * is not part of the rewrite's AST
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ASTNode> T createMoveTarget(ASTRewrite rewrite, T node) {
		return (T) rewrite.createMoveTarget(node);
	}

	/**
	 * Type-safe variant of {@link ASTNode#copySubtree(AST, ASTNode)}.
	 *
	 * @param target the AST that is to own the nodes in the result
	 * @param node the node to copy, or <code>null</code> if none
	 * @return the copied node, or <code>null</code> if <code>node</code>
	 *    is <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ASTNode> T copySubtree(AST target, T node) {
		return (T) ASTNode.copySubtree(target, node);
	}

	/**
	 * Removes the provided node from the AST leaving the leading comment.
	 *
	 * @param rewrite     The AST Rewriter
	 * @param node        The node to remove
	 * @param editGroup   The edit group
	 * @see ASTRewrite#remove(ASTNode, org.eclipse.text.edits.TextEditGroup)
	 */
	public static void removeButKeepComment(final ASTRewrite rewrite, final ASTNode node, final TextEditGroup editGroup) {
		node.setProperty(UNTOUCH_COMMENT, Boolean.TRUE);
		rewrite.remove(node, editGroup);
	}

	/**
	 * Replaces the provided node from the AST with the provided replacement node.
	 *
	 * @param rewrite     The AST Rewriter
	 * @param node        The node to remove
	 * @param replacement The replacement node
	 * @param editGroup   The edit group
	 * @see ASTRewrite#replace(ASTNode, ASTNode,
	 *      org.eclipse.text.edits.TextEditGroup)
	 */
	public static void replaceButKeepComment(final ASTRewrite rewrite, final ASTNode node, final ASTNode replacement, final TextEditGroup editGroup) {
		node.setProperty(UNTOUCH_COMMENT, Boolean.TRUE);
		rewrite.replace(node, replacement, editGroup);
	}

	/**
	 * Should match the last NLS comment before end of the line
	 */
	static final Pattern comment= Pattern.compile("([ ]*\\/\\/\\$NON-NLS-[0-9]\\$) *$"); //$NON-NLS-1$
	/**
	 * Should match leading whitespaces - not sure why eclipse does not allow to use \h instead of [ \t]
	 */
	static final Pattern leadingspaces_start= Pattern.compile("^[ \t]*"); //$NON-NLS-1$
	/**
	 * Should match all leading whitespaces at each start of a line
	 * We have to eat them otherwise formatting is broken
	 */
	static final Pattern leadingspaces= Pattern.compile("\n[ \t]*"); //$NON-NLS-1$

	/**
	 * Replaces the provided node from the AST with the provided replacement node.
	 * Remove one NLS comment at the same time.
	 *
	 * @param rewrite	The AST Rewriter
	 * @param visited	The node to remove
	 * @param replace_with_Call	The replacement node
	 * @param editGroup	The edit group
	 * @param cuRewrite	The cu rewrite
	 * @throws CoreException Exception to be thrown to allow error handling in case of problem to compute the replacement
	 */
	public static void replaceAndRemoveNLS(final ASTRewrite rewrite, final ASTNode visited, final ASTNode replace_with_Call, final TextEditGroup editGroup, final CompilationUnitRewrite cuRewrite) throws CoreException {
		String original= null;
		ASTNode replacement= null;
		try {
			ASTNode st=getFirstAncestorOrNull(visited, Statement.class, FieldDeclaration.class);
			CompilationUnit cu= (CompilationUnit)st.getRoot();
			String buffer= cuRewrite.getCu().getBuffer().getContents();
			int origStart= cu.getExtendedStartPosition(st);
			int origLength= cu.getExtendedLength(st);
			original= buffer.substring(origStart, origStart + origLength);
			original= comment.matcher(original).replaceFirst(""); //$NON-NLS-1$
			original= leadingspaces_start.matcher(original).replaceAll(""); //$NON-NLS-1$
			original= leadingspaces.matcher(original).replaceAll("\n"); //$NON-NLS-1$
			String visitedString= buffer.substring(visited.getStartPosition(), visited.getStartPosition() + visited.getLength());
			// we are using the toString() method to get string representation of replace_with_Call so tweak string to
			// add spaces between parameters
			String replacementCallString= replace_with_Call.toString().replaceAll(",", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			String originalmodified= original.replace(visitedString, replacementCallString);
			replacement= rewrite.createStringPlaceholder(originalmodified, st.getNodeType());
			rewrite.replace(st, replacement, editGroup);
		} catch (JavaModelException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
	}
	/**
	 * Returns a list of local variable names which are visible at the given node.
	 *
	 * @param node the AST node
	 * @return a list of local variable names visible at the given node
	 * @see ScopeAnalyzer#getDeclarationsInScope(int, int)
	 * @since 3.10
	 */
	public static List<String> getVisibleLocalVariablesInScope(ASTNode node) {
		List<String> variableNames= new ArrayList<>();
		CompilationUnit root= (CompilationUnit) node.getRoot();
		IBinding[] bindings= new ScopeAnalyzer(root).
				getDeclarationsInScope(node.getStartPosition(), ScopeAnalyzer.VARIABLES | ScopeAnalyzer.NO_FIELDS | ScopeAnalyzer.CHECK_VISIBILITY);
		for (IBinding binding : bindings) {
			variableNames.add(binding.getName());
		}
		return variableNames;
	}

	/**
	 * Checks whether the given <code>exprStatement</code> has a semicolon at the end.
	 *
	 * @param exprStatement the {@link ExpressionStatement} to check the semicolon
	 * @param cu the compilation unit
	 * @return <code>true</code> if the given <code>exprStatement</code> has a semicolon at the end,
	 *         <code>false</code> otherwise
	 */
	public static boolean hasSemicolon(ExpressionStatement exprStatement, ICompilationUnit cu) {
		boolean hasSemicolon= true;
		if ((exprStatement.getFlags() & ASTNode.RECOVERED) != 0) {
			try {
				Expression expression= exprStatement.getExpression();
				TokenScanner scanner= new TokenScanner(cu);
				hasSemicolon= scanner.readNext(expression.getStartPosition() + expression.getLength(), true) == ITerminalSymbols.TokenNameSEMICOLON;
			} catch (CoreException e) {
				hasSemicolon= false;
			}
		}
		return hasSemicolon;
	}

	/**
	 * Checks if the given <code>node</code> is a {@link VariableDeclarationStatement}
	 * or a {@link SimpleName} whose type is 'var'.
	 *
	 * @param node the AST node
	 * @param astRoot the AST node of the compilation unit
	 * @return <code>true</code> if the given {@link ASTNode} represents a
	 * {@link SimpleName} or {@link VariableDeclarationStatement} that has a 'var' type
	 * and <code>false</code> otherwise.
	 */
	public static boolean isVarType(ASTNode node, CompilationUnit astRoot) {
		IJavaElement root= astRoot.getJavaElement();
		if (root == null) {
			return false;
		}
		IJavaProject javaProject= root.getJavaProject();
		if (javaProject == null) {
			return false;
		}
		if (!JavaModelUtil.is10OrHigher(javaProject)) {
			return false;
		}

		Type type= null;
		if (node instanceof SimpleName) {
			SimpleName name= (SimpleName) node;
			IBinding binding= name.resolveBinding();
			if (!(binding instanceof IVariableBinding)) {
				return false;
			}

			IVariableBinding varBinding= (IVariableBinding) binding;
			if (varBinding.isField() || varBinding.isParameter()) {
				return false;
			}

			ASTNode varDeclaration= astRoot.findDeclaringNode(varBinding);
			if (varDeclaration == null) {
				return false;
			}

			ITypeBinding typeBinding= varBinding.getType();
			if (typeBinding == null || typeBinding.isAnonymous() || typeBinding.isIntersectionType() || typeBinding.isWildcardType()) {
				return false;
			}

			if (varDeclaration instanceof SingleVariableDeclaration) {
				type= ((SingleVariableDeclaration) varDeclaration).getType();
			} else if (varDeclaration instanceof VariableDeclarationFragment) {
				ASTNode parent= varDeclaration.getParent();
				if (parent instanceof VariableDeclarationStatement) {
					type= ((VariableDeclarationStatement) parent).getType();
				} else if (parent instanceof VariableDeclarationExpression) {
					type= ((VariableDeclarationExpression) parent).getType();
				}
			}
		} else if (node instanceof VariableDeclarationStatement) {
			type= ((VariableDeclarationStatement)node).getType();
		} else {
			return false;
		}

		return type == null ? false : type.isVar();
	}

	/**
	 * Return a list of leading comments for a specified node
	 *
	 * @param node - ASTNode in a CompilationUnit
	 * @return list of Comment nodes
	 */
	public static List<Comment> getLeadingComments(ASTNode node) {
		List<Comment> comments= new ArrayList<>();
		CompilationUnit cu= (CompilationUnit)node.getRoot();
		List<Comment> commentList= cu.getCommentList();
		for (Comment commentFromList : commentList) {
			if (commentFromList.getStartPosition() >= cu.getExtendedStartPosition(node)
					&& commentFromList.getStartPosition() + commentFromList.getLength() < node.getStartPosition()) {
				comments.add(commentFromList);
			}
		}
		return comments;
	}

	/**
	 * Return a list of trailing comments for a specified node
	 *
	 * @param node - ASTNode in a CompilationUnit
	 * @return list of Comment nodes
	 */
	public static List<Comment> getTrailingComments(ASTNode node) {
		List<Comment> comments= new ArrayList<>();
		CompilationUnit cu= (CompilationUnit)node.getRoot();
		List<Comment> commentList= cu.getCommentList();
		int extendedStart= cu.getExtendedStartPosition(node);
		int extendedLength= cu.getExtendedLength(node);
		for (Comment commentFromList : commentList) {
			if (commentFromList.getStartPosition() > node.getStartPosition()
					&& commentFromList.getStartPosition() < extendedStart + extendedLength) {
				comments.add(commentFromList);
			}
		}
		return comments;
	}

	/**
	 * Get the number of Type references in a Compilation Unit - used for determining
	 * if an import can be removed.
	 *
	 * @param typeBinding - binding of the type in question
	 * @param cu - compilation unit
	 * @return integer count of times type is referenced (may be 0 if bindings cannot be resolved)
	 */
	public static int getNumberOfTypeReferences(ITypeBinding typeBinding, CompilationUnit cu) {
		class CounterVisitor extends ASTVisitor {
			private int counter= 0;
			private void checkType(Type type) {
				if (type != null && !type.isParameterizedType()) {
					ITypeBinding binding= type.resolveBinding();
					if (binding != null) {
						if (binding.isArray()) {
							binding= binding.getElementType();
						}
						if (binding.isEqualTo(typeBinding)) {
							++counter;
						}
					}
				}
			}
			public int getCounter() {
				return counter;
			}
			@Override
			public boolean visit(ArrayCreation node) {
				Type type= node.getType();
				checkType(type);
				return true;
			}
			@Override
			public boolean visit(MethodDeclaration node) {
				Type type= node.getReturnType2();
				checkType(type);
				List<Type> exceptions= node.thrownExceptionTypes();
				for (Type t : exceptions) {
					checkType(t);
				}
				return true;
			}
			@Override
			public boolean visit(ClassInstanceCreation node) {
				Type type= node.getType();
				checkType(type);
				return true;
			}
			@Override
			public boolean visit(SingleVariableDeclaration node) {
				Type type= node.getType();
				checkType(type);
				return true;
			}
			@Override
			public boolean visit(CastExpression node) {
				Type type= node.getType();
				checkType(type);
				return true;
			}
			@Override
			public boolean visit(VariableDeclarationExpression node) {
				Type type= node.getType();
				checkType(type);
				return true;
			}
			@Override
			public boolean visit(InstanceofExpression node) {
				Type type= node.getRightOperand();
				checkType(type);
				return true;
			}
			@Override
			public boolean visit(FieldDeclaration node) {
				Type type= node.getType();
				checkType(type);
				return true;
			}
			@Override
			public boolean visit(ParameterizedType node) {
				Type type= node.getType();
				checkType(type);
				List<Type> types= node.typeArguments();
				for (Type t : types) {
					checkType(t);
				}
				return true;
			}
			@Override
			public boolean visit(TypeDeclaration node) {
				List<Type> types= node.typeParameters();
				for (Type t : types) {
					checkType(t);
				}
				return true;
			}
			@Override
			public boolean visit(RecordDeclaration node) {
				List<Type> types= node.typeParameters();
				for (Type t : types) {
					checkType(t);
				}
				return true;
			}
		}
		CounterVisitor visitor= new CounterVisitor();
		cu.accept(visitor);
		return visitor.getCounter();
	}
}
