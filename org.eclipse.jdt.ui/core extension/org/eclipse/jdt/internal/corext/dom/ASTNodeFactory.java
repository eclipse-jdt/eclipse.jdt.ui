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
package org.eclipse.jdt.internal.corext.dom;

import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class ASTNodeFactory {

	private static final String STATEMENT_HEADER= "class __X__ { void __x__() { "; //$NON-NLS-1$
	private static final String STATEMENT_FOOTER= "}}"; //$NON-NLS-1$
	
	private static final String TYPE_HEADER= "class __X__ { abstract "; //$NON-NLS-1$
	private static final String TYPE_FOOTER= " __f__(); }}"; //$NON-NLS-1$
	
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
		ASTParser p= ASTParser.newParser(AST.JLS2);
		p.setSource(buffer.toString().toCharArray());
		CompilationUnit root= (CompilationUnit) p.createAST(null);
		ASTNode result= ASTNode.copySubtree(ast, NodeFinder.perform(root, STATEMENT_HEADER.length(), content.length()));
		result.accept(new PositionClearer());
		return result;
	}
	
	public static Name newName(AST ast, String name) {
		StringTokenizer tok= new StringTokenizer(name, "."); //$NON-NLS-1$
		Name res= null;
		while (tok.hasMoreTokens()) {
			SimpleName curr= ast.newSimpleName(tok.nextToken());
			if (res == null) {
				res= curr;
			} else {
				res= ast.newQualifiedName(res, curr);
			}
		}
		return res;
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
		ASTNode type= typeDecl.getMethods()[0].getReturnType();
		ASTNode result= ASTNode.copySubtree(ast, type);
		result.accept(new PositionClearer());
		return (Type)result;
	}
	
	public static Type newType(AST ast, ITypeBinding binding, boolean fullyQualify) {
		if (binding.isPrimitive()) {
			String name= binding.getName();
			return ast.newPrimitiveType(PrimitiveType.toCode(name));
		} else if (binding.isArray()) {
			Type elementType= newType(ast, binding.getElementType(), fullyQualify);
			return ast.newArrayType(elementType, binding.getDimensions());
		} else {
			if (fullyQualify)
				return ast.newSimpleType(ast.newName(Bindings.getAllNameComponents(binding)));
			else
				return ast.newSimpleType(ast.newName(Bindings.getNameComponents(binding)));	
		}
	}
	
	/**
	 * Returns the new type node corresponding to the type of the given declaration
	 * including the extra dimensions.
	 * @param ast The AST to create the resulting type with.
	 * @param declaration The variable declaration to get the type from
	 * @return A new type node created with the given AST.
	 */
	public static Type newType(AST ast, VariableDeclaration declaration) {
		Type type= ASTNodes.getType(declaration);
		int extraDim= ASTNodes.getExtraDimensions(declaration);
	
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
	 * @return Returns the Null-literal for reference types, a boolen-literal for a boolean type, a number
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
	 * Returns an expression that is assignable to the given typebinding. <code>null</code> is
	 * returned if the type is the 'void' type.
	 * 
	 * @param ast The AST to create the expression for
	 * @param type The type binding to which the returned expression is compatible to
	 * @return Returns the Null-literal for reference types, a boolen-literal for a boolean type, a number
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


}
