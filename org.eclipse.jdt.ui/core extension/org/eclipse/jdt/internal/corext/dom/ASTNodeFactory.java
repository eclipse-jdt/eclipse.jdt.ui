/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

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

		protected boolean visitNode(ASTNode node) {
			node.setSourceRange(-1, 0);
			return true;
		}
	}

	private ASTNodeFactory() {
		// no instance;
	}

	public static ASTNode newStatement(AST ast, String content) {
		StringBuffer buffer= new StringBuffer(STATEMENT_HEADER);
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
		StringBuffer buffer= new StringBuffer(TYPEPARAM_HEADER);
		buffer.append(content);
		buffer.append(TYPEPARAM_FOOTER);
		ASTParser p= ASTParser.newParser(ast.apiLevel());
		p.setSource(buffer.toString().toCharArray());
		CompilationUnit root= (CompilationUnit) p.createAST(null);
		List list= root.types();
		TypeDeclaration typeDecl= (TypeDeclaration) list.get(0);
		MethodDeclaration methodDecl= typeDecl.getMethods()[0];
		TypeParameter tp= (TypeParameter) methodDecl.typeParameters().get(0);
		ASTNode result= ASTNode.copySubtree(ast, tp);
		result.accept(new PositionClearer());
		return (TypeParameter) result;
	}


	public static Type newType(AST ast, String content) {
		StringBuffer buffer= new StringBuffer(TYPE_HEADER);
		buffer.append(content);
		buffer.append(TYPE_FOOTER);
		ASTParser p= ASTParser.newParser(ast.apiLevel());
		p.setSource(buffer.toString().toCharArray());
		CompilationUnit root= (CompilationUnit) p.createAST(null);
		List list= root.types();
		TypeDeclaration typeDecl= (TypeDeclaration) list.get(0);
		MethodDeclaration methodDecl= typeDecl.getMethods()[0];
		ASTNode type= methodDecl.getReturnType2();
		ASTNode result= ASTNode.copySubtree(ast, type);
		result.accept(new PositionClearer());
		return (Type)result;
	}

	/**
	 * Returns the new type node corresponding to the type of the given declaration
	 * including the extra dimensions.
	 * @param ast The AST to create the resulting type with.
	 * @param declaration The variable declaration to get the type from
	 * @return a new type node created with the given AST.
	 */
	public static Type newType(AST ast, VariableDeclaration declaration) {
		Type type= ASTNodes.getType(declaration);
		int extraDim= declaration.getExtraDimensions();

		type= (Type) ASTNode.copySubtree(ast, type);
		for (int i= 0; i < extraDim; i++) {
			type= ast.newArrayType(type);
		}
		return type;
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
			if ("boolean".equals(name)) { //$NON-NLS-1$
				return ast.newBooleanLiteral(false);
			} else if ("void".equals(name)) { //$NON-NLS-1$
				return null;
			} else {
				return ast.newNumberLiteral("0"); //$NON-NLS-1$
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
	public static List newModifiers(AST ast, int modifiers) {
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
	public static List newModifiers(AST ast, List modifierNodes) {
		List res= new ArrayList(modifierNodes.size());
		for (int i= 0; i < modifierNodes.size(); i++) {
			Object curr= modifierNodes.get(i);
			if (curr instanceof Modifier) {
				res.add(ast.newModifier(((Modifier) curr).getKeyword()));
			}
		}
		return res;
	}

	public static Expression newInfixExpression(AST ast, Operator operator, ArrayList/*<Expression>*/ operands) {
		if (operands.size() == 1)
			return (Expression) operands.get(0);

		InfixExpression result= ast.newInfixExpression();
		result.setOperator(operator);
		result.setLeftOperand((Expression) operands.get(0));
		result.setRightOperand((Expression) operands.get(1));
		result.extendedOperands().addAll(operands.subList(2, operands.size()));
		return result;
	}

}
