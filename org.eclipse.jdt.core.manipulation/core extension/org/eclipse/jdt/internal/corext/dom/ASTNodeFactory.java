/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Red Hat Inc. - refactored to jdt.core.manipulation
 *     Fabrice TIERCELIN - Methods to identify a signature
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;

import org.eclipse.jdt.internal.ui.util.ASTHelper;

/**
 * JDT-UI-internal helper methods to create new {@link ASTNode}s.
 * Complements <code>AST#new*(..)</code> and <code>ImportRewrite#add*(..)</code>.
 *
 * see JDTUIHelperClasses
 */
public class ASTNodeFactory {

	private static final String STATEMENT_HEADER= "class __X__ { void __x__() { "; //$NON-NLS-1$
	private static final String STATEMENT_FOOTER= "}}"; //$NON-NLS-1$

	private static final String TYPE_HEADER= "class __X__ { abstract "; //$NON-NLS-1$
	private static final String TYPE_FOOTER= " __f__(); }}"; //$NON-NLS-1$

	private static final String TYPEPARAM_HEADER= "class __X__ { abstract <"; //$NON-NLS-1$
	private static final String TYPEPARAM_FOOTER= "> void __f__(); }}"; //$NON-NLS-1$

	private static class PositionClearer extends GenericVisitor {

		public PositionClearer() {
			super(true);
		}

		@Override
		protected boolean visitNode(ASTNode node) {
			node.setSourceRange(-1, 0);
			return true;
		}
	}

	private ASTNodeFactory() {
		// no instance;
	}

	/**
	 * Parenthesizes the provided expression if its type requires it.
	 *
	 * @param ast The AST to create the resulting node with.
	 * @param expression the expression to conditionally return parenthesized
	 * @return the parenthesized expression of the provided expression to return or this expression
	 *         itself
	 */
	public static Expression parenthesizeIfNeeded(AST ast, Expression expression) {
		switch (expression.getNodeType()) {
		case ASTNode.ANNOTATION_TYPE_DECLARATION:
		case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
		case ASTNode.ANONYMOUS_CLASS_DECLARATION:
		case ASTNode.ARRAY_ACCESS:
		case ASTNode.ARRAY_CREATION:
		case ASTNode.ARRAY_INITIALIZER:
		case ASTNode.BOOLEAN_LITERAL:
		case ASTNode.CHARACTER_LITERAL:
		case ASTNode.CLASS_INSTANCE_CREATION:
		case ASTNode.CREATION_REFERENCE:
		case ASTNode.EXPRESSION_METHOD_REFERENCE:
		case ASTNode.FIELD_ACCESS:
		case ASTNode.MEMBER_REF:
		case ASTNode.METHOD_INVOCATION:
		case ASTNode.METHOD_REF:
		case ASTNode.NULL_LITERAL:
		case ASTNode.NUMBER_LITERAL:
		case ASTNode.PARENTHESIZED_EXPRESSION:
		case ASTNode.POSTFIX_EXPRESSION:
		case ASTNode.PREFIX_EXPRESSION:
		case ASTNode.QUALIFIED_NAME:
		case ASTNode.SIMPLE_NAME:
		case ASTNode.STRING_LITERAL:
		case ASTNode.SUPER_FIELD_ACCESS:
		case ASTNode.SUPER_METHOD_INVOCATION:
		case ASTNode.SUPER_METHOD_REFERENCE:
		case ASTNode.THIS_EXPRESSION:
		case ASTNode.TYPE_LITERAL:
		case ASTNode.TYPE_METHOD_REFERENCE:
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
			return expression;

		default:
			return parenthesize(ast, expression);
		}
	}

	/**
	 * Builds a new {@link ParenthesizedExpression} instance.
	 *
	 * @param ast The AST to create the resulting node with.
	 * @param expression the expression to wrap with parentheses
	 * @return a new parenthesized expression
	 */
	public static ParenthesizedExpression parenthesize(AST ast, Expression expression) {
		final ParenthesizedExpression pe= ast.newParenthesizedExpression();
		pe.setExpression(expression);
		return pe;
	}

	/**
	 * Builds a new {@link PrefixExpression} instance using the not operator ('!').
	 *
	 * @param ast The AST to create the resulting node with.
	 * @param expression the expression to negate
	 * @return a new prefix expression
	 */
	public static Expression not(final AST ast, final Expression expression) {
		final PrefixExpression prefixExpression= ast.newPrefixExpression();
		prefixExpression.setOperator(PrefixExpression.Operator.NOT);
		prefixExpression.setOperand(parenthesizeIfNeeded(ast, expression));
		return prefixExpression;
	}

	/**
	 * Negates the provided expression and applies the provided copy operation on
	 * the returned expression:
	 *
	 * isValid  =>  !isValid
	 * !isValid =>  isValid
	 * true                           =>  false
	 * false                          =>  true
	 * i > 0                          =>  i <= 0
	 * isValid || isEnabled           =>  !isValid && !isEnabled
	 * !isValid || !isEnabled         =>  isValid && isEnabled
	 * isValid ? (i > 0) : !isEnabled =>  isValid ? (i <= 0) : isEnabled
	 *
	 * @param ast The AST to create the resulting node with.
	 * @param rewrite the rewrite
	 * @param booleanExpression the expression to negate
	 * @param isMove False if the returned nodes need to be new nodes
	 * @return the negated expression, as move or copy
	 */
	public static Expression negate(final AST ast, final ASTRewrite rewrite, final Expression booleanExpression, final boolean isMove) {
		Expression unparenthesedExpression= ASTNodes.getUnparenthesedExpression(booleanExpression);

		if (unparenthesedExpression instanceof PrefixExpression) {
			PrefixExpression prefixExpression= (PrefixExpression) unparenthesedExpression;

			if (ASTNodes.hasOperator(prefixExpression, PrefixExpression.Operator.NOT)) {
				Expression otherExpression= prefixExpression.getOperand();
				PrefixExpression otherPrefixExpression= ASTNodes.as(otherExpression, PrefixExpression.class);

				if (otherPrefixExpression != null && ASTNodes.hasOperator(otherPrefixExpression, PrefixExpression.Operator.NOT)) {
					return negate(ast, rewrite, otherPrefixExpression.getOperand(), isMove);
				}

				return isMove ? ASTNodes.createMoveTarget(rewrite, otherExpression) : ((Expression) rewrite.createCopyTarget(otherExpression));
			}
		} else if (unparenthesedExpression instanceof InfixExpression) {
			InfixExpression booleanOperation= (InfixExpression) unparenthesedExpression;
			InfixExpression.Operator negatedOperator= ASTNodes.negatedInfixOperator(booleanOperation.getOperator());

			if (negatedOperator != null) {
				return getNegatedOperation(ast, rewrite, booleanOperation, negatedOperator, isMove);
			}
		} else if (unparenthesedExpression instanceof ConditionalExpression) {
			ConditionalExpression aConditionalExpression= (ConditionalExpression) unparenthesedExpression;

			ConditionalExpression newConditionalExpression= ast.newConditionalExpression();
			newConditionalExpression.setExpression(isMove ? ASTNodes.createMoveTarget(rewrite, aConditionalExpression.getExpression()) : ((Expression) rewrite.createCopyTarget(aConditionalExpression.getExpression())));
			newConditionalExpression.setThenExpression(negate(ast, rewrite, aConditionalExpression.getThenExpression(), isMove));
			newConditionalExpression.setElseExpression(negate(ast, rewrite, aConditionalExpression.getElseExpression(), isMove));
			return newConditionalExpression;
		} else {
			Boolean constant= ASTNodes.getBooleanLiteral(unparenthesedExpression);

			if (constant != null) {
				return ast.newBooleanLiteral(!constant.booleanValue());
			}
		}

		if (isMove) {
			return not(ast, ASTNodes.createMoveTarget(rewrite, unparenthesedExpression));
		}

		return not(ast, (Expression) rewrite.createCopyTarget(unparenthesedExpression));
	}

	private static Expression getNegatedOperation(final AST ast, final ASTRewrite rewrite, final InfixExpression booleanOperation, final InfixExpression.Operator negatedOperator, final boolean isMove) {
		List<Expression> allOperands= ASTNodes.allOperands(booleanOperation);
		List<Expression> allTargetOperands;

		if (ASTNodes.hasOperator(booleanOperation, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.AND,
				InfixExpression.Operator.OR)) {
			allTargetOperands= new ArrayList<>(allOperands.size());

			for (Expression booleanOperand : allOperands) {
				Expression negatedOperand= negate(ast, rewrite, booleanOperand, isMove);

				if (negatedOperand != null) {
					allTargetOperands.add(negatedOperand);
				} else {
					PrefixExpression prefixExpression= ast.newPrefixExpression();
					prefixExpression.setOperator(PrefixExpression.Operator.NOT);
					Expression targetOperand= isMove ? ASTNodes.createMoveTarget(rewrite, booleanOperand) : ((Expression) rewrite.createCopyTarget(booleanOperand));
					prefixExpression.setOperand(parenthesizeIfNeeded(ast, targetOperand));

					allTargetOperands.add(prefixExpression);
				}
			}
		} else if (isMove) {
			allTargetOperands= ASTNodes.createMoveTarget(rewrite, allOperands);
		} else {
			allTargetOperands= new ArrayList<>(allOperands.size());

			for (Expression anOperand : allOperands) {
				allTargetOperands.add((Expression) rewrite.createCopyTarget(anOperand));
			}
		}

		InfixExpression newInfixExpression= ast.newInfixExpression();
		newInfixExpression.setOperator(negatedOperator);
		newInfixExpression.setLeftOperand(allTargetOperands.remove(0));
		newInfixExpression.setRightOperand(allTargetOperands.remove(0));
		newInfixExpression.extendedOperands().addAll(allTargetOperands);

		ParenthesizedExpression parenthesizedExpression= ast.newParenthesizedExpression();
		parenthesizedExpression.setExpression(newInfixExpression);
		return parenthesizedExpression;
	}

	public static ASTNode newStatement(AST ast, String content) {
		StringBuilder buffer= new StringBuilder(STATEMENT_HEADER);
		buffer.append(content);
		buffer.append(STATEMENT_FOOTER);
		ASTParser p= ASTParser.newParser(ast.apiLevel());
		p.setSource(buffer.toString().toCharArray());
		CompilationUnit root= (CompilationUnit) p.createAST(null);
		ASTNode result= ASTNode.copySubtree(ast, NodeFinder.perform(root, STATEMENT_HEADER.length(), content.length()));
		result.accept(new PositionClearer());
		return result;
	}

	public static Name newName(AST ast, String qualifiedName) {
		return ast.newName(qualifiedName);
	}

	public static TypeParameter newTypeParameter(AST ast, String content) {
		StringBuilder buffer= new StringBuilder(TYPEPARAM_HEADER);
		buffer.append(content);
		buffer.append(TYPEPARAM_FOOTER);
		ASTParser p= ASTParser.newParser(ast.apiLevel());
		p.setSource(buffer.toString().toCharArray());
		CompilationUnit root= (CompilationUnit) p.createAST(null);
		List<AbstractTypeDeclaration> list= root.types();
		TypeDeclaration typeDecl= (TypeDeclaration) list.get(0);
		MethodDeclaration methodDecl= typeDecl.getMethods()[0];
		TypeParameter tp= (TypeParameter) methodDecl.typeParameters().get(0);
		ASTNode result= ASTNode.copySubtree(ast, tp);
		result.accept(new PositionClearer());
		return (TypeParameter) result;
	}


	public static Type newType(AST ast, String content) {
		StringBuilder buffer= new StringBuilder(TYPE_HEADER);
		buffer.append(content);
		buffer.append(TYPE_FOOTER);
		ASTParser p= ASTParser.newParser(ast.apiLevel());
		p.setSource(buffer.toString().toCharArray());
		CompilationUnit root= (CompilationUnit) p.createAST(null);
		List<AbstractTypeDeclaration> list= root.types();
		TypeDeclaration typeDecl= (TypeDeclaration) list.get(0);
		MethodDeclaration methodDecl= typeDecl.getMethods()[0];
		ASTNode type= methodDecl.getReturnType2();
		ASTNode result= ASTNode.copySubtree(ast, type);
		result.accept(new PositionClearer());
		return (Type)result;
	}

	/**
	 * Returns an {@link ArrayType} that adds one dimension to the given type node.
	 * If the given node is already an ArrayType, then a new {@link Dimension}
	 * without annotations is inserted at the first position.
	 *
	 * @param type the type to be wrapped
	 * @return the array type
	 * @since 3.10
	 */
	public static ArrayType newArrayType(Type type) {
		if (type instanceof ArrayType) {
			Dimension dimension= type.getAST().newDimension();
			ArrayType arrayType= (ArrayType) type;
			arrayType.dimensions().add(0, dimension); // first dimension is outermost
			return arrayType;
		} else {
			return type.getAST().newArrayType(type);
		}
	}

	/**
	 * Returns the new type node corresponding to the type of the given declaration
	 * including the extra dimensions.
	 * @param ast The AST to create the resulting type with.
	 * @param declaration The variable declaration to get the type from
	 * @return a new type node created with the given AST.
	 */
	public static Type newType(AST ast, VariableDeclaration declaration) {
		return newType(ast, declaration, null, null);
	}

	/**
	 * Returns the new type node corresponding to the type of the given declaration
	 * including the extra dimensions. If the type is a {@link UnionType}, use the LUB type.
	 * If the <code>importRewrite</code> is <code>null</code>, the type may be fully-qualified.
	 *
	 * @param ast The AST to create the resulting type with.
	 * @param declaration The variable declaration to get the type from
	 * @param importRewrite the import rewrite to use, or <code>null</code>
	 * @param context the import rewrite context, or <code>null</code>
	 * @return a new type node created with the given AST.
	 *
	 * @since 3.7.1
	 */
	public static Type newType(AST ast, VariableDeclaration declaration, ImportRewrite importRewrite, ImportRewriteContext context) {
		if (declaration instanceof VariableDeclarationFragment && declaration.getParent() instanceof LambdaExpression) {
			return newType((LambdaExpression) declaration.getParent(), (VariableDeclarationFragment) declaration, ast, importRewrite, context);
		}

		Type type= ASTNodes.getType(declaration);
		if (declaration instanceof SingleVariableDeclaration) {
			Type type2= ((SingleVariableDeclaration) declaration).getType();
			if (type2 instanceof UnionType) {
				ITypeBinding typeBinding= type2.resolveBinding();
				if (typeBinding != null) {
					if (importRewrite != null) {
						type= importRewrite.addImport(typeBinding, ast, context);
						return type;
					} else {
						String qualifiedName= typeBinding.getQualifiedName();
						if (qualifiedName.length() > 0) {
							type= ast.newSimpleType(ast.newName(qualifiedName));
							return type;
						}
					}
				}
				// XXX: fallback for intersection types or unresolved types: take first type of union
				type= (Type) ((UnionType) type2).types().get(0);
				return type;
			}
		}

		type= (Type) ASTNode.copySubtree(ast, type);

		List<Dimension> extraDimensions= declaration.extraDimensions();
		if (!extraDimensions.isEmpty()) {
			ArrayType arrayType;
			if (type instanceof ArrayType) {
				arrayType= (ArrayType) type;
			} else {
				arrayType= ast.newArrayType(type, 0);
				type= arrayType;
			}
			arrayType.dimensions().addAll(ASTNode.copySubtrees(ast, extraDimensions));
		}
		return type;
	}

	private static Type newType(LambdaExpression lambdaExpression, VariableDeclarationFragment declaration, AST ast, ImportRewrite importRewrite, ImportRewriteContext context) {
		IMethodBinding method= lambdaExpression.resolveMethodBinding();
		if (method != null) {
			ITypeBinding[] parameterTypes= method.getParameterTypes();
			int index= lambdaExpression.parameters().indexOf(declaration);
			ITypeBinding typeBinding= parameterTypes[index];
			if (importRewrite != null) {
				return importRewrite.addImport(typeBinding, ast, context);
			} else {
				String qualifiedName= typeBinding.getQualifiedName();
				if (qualifiedName.length() > 0) {
					return newType(ast, qualifiedName);
				}
			}
		}
		// fall-back
		return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
	}

	public static Type newNonVarType(AST ast, VariableDeclaration declaration, ImportRewrite importRewrite, ImportRewriteContext context) {
		if (declaration.getAST().apiLevel() < ASTHelper.JLS10) {
			return newType(ast, declaration, importRewrite, context);
		}

		if (declaration instanceof VariableDeclarationFragment && declaration.getParent() instanceof LambdaExpression) {
			return newType((LambdaExpression) declaration.getParent(), (VariableDeclarationFragment) declaration, ast, importRewrite, context);
		}

		Type type= ASTNodes.getType(declaration);
		Type finalType= null;
		if (declaration instanceof SingleVariableDeclaration) {
			finalType= ((SingleVariableDeclaration) declaration).getType();
		} else if (type != null) {
			finalType= type;
		}
		if (finalType != null && finalType.isVar()) {
			ITypeBinding typeBinding= finalType.resolveBinding();
			if (typeBinding != null) {
				if (importRewrite != null) {
					finalType= importRewrite.addImport(typeBinding, ast, context);
					return finalType;
				} else {
					String qualifiedName= typeBinding.getQualifiedName();
					if (qualifiedName.length() > 0) {
						finalType= ast.newSimpleType(ast.newName(qualifiedName));
						return finalType;
					}
				}
			}
			return finalType;
		} else {
			return newType(ast, declaration, importRewrite, context);
		}
	}

	/**
	 * Returns the new type node representing the return type of <code>lambdaExpression</code>
	 * including the extra dimensions.
	 *
	 * @param lambdaExpression the lambda expression
	 * @param ast the AST to create the return type with
	 * @param importRewrite the import rewrite to use, or <code>null</code>
	 * @param context the import rewrite context, or <code>null</code>
	 * @return a new type node created with the given AST representing the return type of
	 *         <code>lambdaExpression</code>
	 *
	 * @since 3.10
	 */
	public static Type newReturnType(LambdaExpression lambdaExpression, AST ast, ImportRewrite importRewrite, ImportRewriteContext context) {
		IMethodBinding method= lambdaExpression.resolveMethodBinding();
		if (method != null) {
			ITypeBinding returnTypeBinding= method.getReturnType();
			if (importRewrite != null) {
				return importRewrite.addImport(returnTypeBinding, ast);
			} else {
				String qualifiedName= returnTypeBinding.getQualifiedName();
				if (qualifiedName.length() > 0) {
					return newType(ast, qualifiedName);
				}
			}
		}
		// fall-back
		return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
	}

	/**
	 * Returns an expression that is assignable to the given type. <code>null</code> is
	 * returned if the type is the 'void' type.
	 *
	 * @param ast The AST to create the expression for
	 * @param type The type of the returned expression
	 * @param extraDimensions Extra dimensions to the type
	 * @return the Null-literal for reference types, a boolean-literal for a boolean type, a number
	 * literal for primitive types or <code>null</code> if the type is void.
	 */
	public static Expression newDefaultExpression(AST ast, Type type, int extraDimensions) {
		if (extraDimensions == 0 && type.isPrimitiveType()) {
			PrimitiveType primitiveType= (PrimitiveType) type;
			if (primitiveType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
				return ast.newBooleanLiteral(false);
			} else if (primitiveType.getPrimitiveTypeCode() == PrimitiveType.VOID) {
				return null;
			} else {
				return ast.newNumberLiteral("0"); //$NON-NLS-1$
			}
		}
		return ast.newNullLiteral();
	}

	/**
	 * Returns an expression that is assignable to the given type binding. <code>null</code> is
	 * returned if the type is the 'void' type.
	 *
	 * @param ast The AST to create the expression for
	 * @param type The type binding to which the returned expression is compatible to
	 * @return the Null-literal for reference types, a boolean-literal for a boolean type, a number
	 * literal for primitive types or <code>null</code> if the type is void.
	 */
	public static Expression newDefaultExpression(AST ast, ITypeBinding type) {
		if (type.isPrimitive()) {
			String name= type.getName();
			boolean nomatch= false;
			if (name != null) switch (name) {
			case "boolean": //$NON-NLS-1$
				return ast.newBooleanLiteral(false);
			case "void": //$NON-NLS-1$
				return null;
			default:
				nomatch= true;
				break;
			}
			if (nomatch) {
				return ast.newNumberLiteral("0");//$NON-NLS-1$
			}
		}
		return ast.newNullLiteral();
	}

	/**
	 * Returns a list of newly created Modifier nodes corresponding to the given modifier flags.
	 * @param ast The AST to create the nodes for.
	 * @param modifiers The modifier flags describing the modifier nodes to create.
	 * @return Returns a list of nodes of type {@link Modifier}.
	 */
	public static List<Modifier> newModifiers(AST ast, int modifiers) {
		return ast.newModifiers(modifiers);
	}

	/**
	 * Returns a list of newly created Modifier nodes corresponding to a given list of existing modifiers.
	 * @param ast The AST to create the nodes for.
	 * @param modifierNodes The modifier nodes describing the modifier nodes to create. Only
	 * nodes of type {@link Modifier} are looked at and cloned. To create a full copy of the list consider
	 * to use {@link ASTNode#copySubtrees(AST, List)}.
	 * @return Returns a list of nodes of type {@link Modifier}.
	 */
	public static List<Modifier> newModifiers(AST ast, List<? extends IExtendedModifier> modifierNodes) {
		List<Modifier> res= new ArrayList<>(modifierNodes.size());
		for (int i= 0; i < modifierNodes.size(); i++) {
			Object curr= modifierNodes.get(i);
			if (curr instanceof Modifier) {
				res.add(ast.newModifier(((Modifier) curr).getKeyword()));
			}
		}
		return res;
	}

	public static Expression newInfixExpression(AST ast, Operator operator, ArrayList<Expression> operands) {
		if (operands.size() == 1)
			return operands.get(0);

		InfixExpression result= ast.newInfixExpression();
		result.setOperator(operator);
		result.setLeftOperand(operands.get(0));
		result.setRightOperand(operands.get(1));
		result.extendedOperands().addAll(operands.subList(2, operands.size()));
		return result;
	}

	/**
	 * Create a Type suitable as the creationType in a ClassInstanceCreation expression.
	 * @param ast The AST to create the nodes for.
	 * @param typeBinding binding representing the given class type
	 * @param importRewrite the import rewrite to use
	 * @param importContext the import context used to determine which (null) annotations to consider
	 * @return a Type suitable as the creationType in a ClassInstanceCreation expression.
	 */
	public static Type newCreationType(AST ast, ITypeBinding typeBinding, ImportRewrite importRewrite, ImportRewriteContext importContext) {
		if (typeBinding.isParameterizedType()) {
			Type baseType= newCreationType(ast, typeBinding.getTypeDeclaration(), importRewrite, importContext);
			IAnnotationBinding[] typeAnnotations= importContext.removeRedundantTypeAnnotations(typeBinding.getTypeAnnotations(), TypeLocation.NEW, typeBinding);
			for (IAnnotationBinding typeAnnotation : typeAnnotations) {
				((AnnotatableType)baseType).annotations().add(importRewrite.addAnnotation(typeAnnotation, ast, importContext));
			}
			ParameterizedType parameterizedType= ast.newParameterizedType(baseType);
			for (ITypeBinding typeArgument : typeBinding.getTypeArguments()) {
				typeArgument= StubUtility2Core.replaceWildcardsAndCaptures(typeArgument);
				parameterizedType.typeArguments().add(importRewrite.addImport(typeArgument, ast, importContext, TypeLocation.TYPE_ARGUMENT));
			}
			return parameterizedType;
		} else {
			return importRewrite.addImport(typeBinding, ast, importContext, TypeLocation.NEW);
		}
	}
}
