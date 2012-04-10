/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

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
		List<AbstractTypeDeclaration> list= root.types();
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
		List<AbstractTypeDeclaration> list= root.types();
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
		List<Modifier> res= new ArrayList<Modifier>(modifierNodes.size());
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

	public static Annotation newAnnotation(AST ast, IAnnotationBinding annotation, ImportRewrite importRewrite, ImportRewriteContext context) {
		Type type= importRewrite.addImport(annotation.getAnnotationType(), ast, context);
		Name name;
		if (type instanceof SimpleType) {
			SimpleType simpleType= (SimpleType) type;
			name= simpleType.getName();
			// pay ransom to allow reuse of 'name':
			simpleType.setName(ast.newSimpleName("a")); //$NON-NLS-1$
		} else {
			name= ast.newName(ASTNodes.asString(type));
		}
		
		IMemberValuePairBinding[] mvps= annotation.getDeclaredMemberValuePairs();
		if (mvps.length == 0) {
			MarkerAnnotation result= ast.newMarkerAnnotation();
			result.setTypeName(name);
			return result;
		} else if (mvps.length == 1 && "value".equals(mvps[0].getName())) { //$NON-NLS-1$
			SingleMemberAnnotation result= ast.newSingleMemberAnnotation();
			result.setTypeName(name);
			Object value= mvps[0].getValue();
			if (value != null)
				result.setValue(newAnnotationValue(ast, value, importRewrite, context));
			return result;
		} else {
			NormalAnnotation result= ast.newNormalAnnotation();
			result.setTypeName(name);
			List<MemberValuePair> values= result.values();
			for (int i= 0; i < mvps.length; i++) {
				IMemberValuePairBinding mvp= mvps[i];
				MemberValuePair mvpNode= ast.newMemberValuePair();
				mvpNode.setName(ast.newSimpleName(mvp.getName()));
				Object value= mvp.getValue();
				if (value != null)
					mvpNode.setValue(newAnnotationValue(ast, value, importRewrite, context));
				values.add(mvpNode);
			}
			return result;
		}
	}

	public static Expression newAnnotationValue(AST ast, Object value, ImportRewrite importRewrite, ImportRewriteContext context) {
		if (value instanceof Boolean) {
			return ast.newBooleanLiteral(((Boolean) value).booleanValue());
			
		} else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
				|| value instanceof Float || value instanceof Double) {
			return ast.newNumberLiteral(value.toString());
			
		} else if (value instanceof Character) {
			CharacterLiteral result= ast.newCharacterLiteral();
			result.setCharValue(((Character) value).charValue());
			return result;
			
		} else if (value instanceof ITypeBinding) {
			TypeLiteral result= ast.newTypeLiteral();
			result.setType(importRewrite.addImport((ITypeBinding) value, ast, context));
			return result;
			
		} else if (value instanceof String) {
			StringLiteral result= ast.newStringLiteral();
			result.setLiteralValue((String) value);
			return result;
			
		} else if (value instanceof IVariableBinding) {
			IVariableBinding variable= (IVariableBinding) value;
			
			FieldAccess result= ast.newFieldAccess();
			result.setName(ast.newSimpleName(variable.getName()));
			Type type= importRewrite.addImport(variable.getType(), ast, context);
			Name name;
			if (type instanceof SimpleType) {
				SimpleType simpleType= (SimpleType) type;
				name= simpleType.getName();
				// pay ransom to allow reuse of 'name':
				simpleType.setName(ast.newSimpleName("a")); //$NON-NLS-1$
			} else {
				name= ast.newName(ASTNodes.asString(type));
			}
			result.setExpression(name);
			return result;
			
		} else if (value instanceof IAnnotationBinding) {
			return newAnnotation(ast, (IAnnotationBinding) value, importRewrite, context);
			
		} else if (value instanceof Object[]) {
			Object[] values= (Object[]) value;
			if (values.length == 1)
				return newAnnotationValue(ast, values[0], importRewrite, context);
			
			ArrayInitializer initializer= ast.newArrayInitializer();
			List<Expression> expressions= initializer.expressions();
			for (Object val : values)
				expressions.add(newAnnotationValue(ast, val, importRewrite, context));
			return initializer;
			
		} else {
			return null;
		}
	}

}
