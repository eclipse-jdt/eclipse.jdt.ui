/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class ASTResolving {
	
	public static ITypeBinding guessBindingForReference(ASTNode node) {
		return Bindings.normalizeTypeBinding(getPossibleReferenceBinding(node));
	}
		
	private static ITypeBinding getPossibleReferenceBinding(ASTNode node) {	
		ASTNode parent= node.getParent();
		switch (parent.getNodeType()) {
		case ASTNode.ASSIGNMENT:
			Assignment assignment= (Assignment) parent;
			if (node.equals(assignment.getLeftHandSide())) {
				// field write access: xx= expression
				return assignment.getRightHandSide().resolveTypeBinding();
			}
			// read access
			return assignment.getLeftHandSide().resolveTypeBinding();
		case ASTNode.INFIX_EXPRESSION:
			InfixExpression infix= (InfixExpression) parent;
			InfixExpression.Operator op= infix.getOperator();
			if (op == InfixExpression.Operator.CONDITIONAL_AND || op == InfixExpression.Operator.CONDITIONAL_OR) {
				// boolean operation
				return infix.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			} else if (op == InfixExpression.Operator.LEFT_SHIFT || op == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED || op == InfixExpression.Operator.RIGHT_SHIFT_SIGNED) {
				// assymetric operation
				return infix.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
			if (node.equals(infix.getLeftOperand())) {
				//	xx op expression
				ITypeBinding rigthHandBinding= infix.getRightOperand().resolveTypeBinding();
				if (rigthHandBinding != null) {
					return rigthHandBinding;
				}
			} else {
				// expression op xx
				ITypeBinding leftHandBinding= infix.getLeftOperand().resolveTypeBinding();
				if (leftHandBinding != null) {
					return leftHandBinding;
				}
			}
			if (op != InfixExpression.Operator.EQUALS && op != InfixExpression.Operator.NOT_EQUALS) {
				return infix.getAST().resolveWellKnownType("int"); //$NON-NLS-1$ 
			}
			break;
		case ASTNode.INSTANCEOF_EXPRESSION:
			InstanceofExpression instanceofExpression= (InstanceofExpression) parent;
			return instanceofExpression.getRightOperand().resolveBinding();
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
			VariableDeclarationFragment frag= (VariableDeclarationFragment) parent;
			if (frag.getInitializer().equals(node)) {
				return frag.getName().resolveTypeBinding();
			}
			break;
		case ASTNode.SUPER_METHOD_INVOCATION:
			SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) parent;
			IMethodBinding superMethodBinding= ASTNodes.getMethodBinding(superMethodInvocation.getName());
			if (superMethodBinding != null) {
				return getParameterTypeBinding(node, superMethodInvocation.arguments(), superMethodBinding);
			}
			break;			
		case ASTNode.METHOD_INVOCATION:
			MethodInvocation methodInvocation= (MethodInvocation) parent;
			IMethodBinding methodBinding= ASTNodes.getMethodBinding(methodInvocation.getName());
			if (methodBinding != null) {
				return getParameterTypeBinding(node, methodInvocation.arguments(), methodBinding);
			}
			break;			
		case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
			SuperConstructorInvocation superInvocation= (SuperConstructorInvocation) parent;
			IMethodBinding superBinding= superInvocation.resolveConstructorBinding();
			if (superBinding != null) {
				return getParameterTypeBinding(node, superInvocation.arguments(), superBinding);
			}
			break;
		case ASTNode.CONSTRUCTOR_INVOCATION:
			ConstructorInvocation constrInvocation= (ConstructorInvocation) parent;
			IMethodBinding constrBinding= constrInvocation.resolveConstructorBinding();
			if (constrBinding != null) {
				return getParameterTypeBinding(node, constrInvocation.arguments(), constrBinding);
			}
			break;
		case ASTNode.CLASS_INSTANCE_CREATION:
			ClassInstanceCreation creation= (ClassInstanceCreation) parent;
			IMethodBinding creationBinding= creation.resolveConstructorBinding();
			if (creationBinding != null) {
				return getParameterTypeBinding(node, creation.arguments(), creationBinding);
			}
			break;
		case ASTNode.PARENTHESIZED_EXPRESSION:
			return guessBindingForReference(parent);
		case ASTNode.ARRAY_ACCESS:
			if (((ArrayAccess) parent).getIndex().equals(node)) {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			} else {
				return getPossibleReferenceBinding(parent);
			}
		case ASTNode.ARRAY_CREATION:
			if (((ArrayCreation) parent).dimensions().contains(node)) {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
			break;		
		case ASTNode.ARRAY_INITIALIZER:
			ASTNode initializerParent= parent.getParent();
			if (initializerParent instanceof ArrayCreation) {
				return ((ArrayCreation) initializerParent).getType().getElementType().resolveBinding();
			}
			break;
		case ASTNode.CONDITIONAL_EXPRESSION:
			ConditionalExpression expression= (ConditionalExpression) parent;
			if (node.equals(expression.getExpression())) {
				return parent.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			}
			if (node.equals(expression.getElseExpression())) {
				return expression.getThenExpression().resolveTypeBinding();
			}
			return expression.getElseExpression().resolveTypeBinding();
		case ASTNode.POSTFIX_EXPRESSION:
			return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
		case ASTNode.PREFIX_EXPRESSION:
			if (((PrefixExpression) parent).getOperator() == PrefixExpression.Operator.NOT) {
				return parent.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			}
			return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
		case ASTNode.IF_STATEMENT:
		case ASTNode.WHILE_STATEMENT:
		case ASTNode.DO_STATEMENT:
			if (node instanceof Expression) {
				return parent.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			}
			break;
		case ASTNode.SWITCH_STATEMENT:
			if (((SwitchStatement) parent).getExpression().equals(node)) {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
			break;
		case ASTNode.RETURN_STATEMENT:
			MethodDeclaration decl= findParentMethodDeclaration(parent);
			if (decl != null) {
				return decl.getReturnType().resolveBinding();
			}
			break;
		case ASTNode.CAST_EXPRESSION:
			return ((CastExpression) parent).getType().resolveBinding();
		case ASTNode.THROW_STATEMENT:
		case ASTNode.CATCH_CLAUSE:
            return parent.getAST().resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
		case ASTNode.FIELD_ACCESS:
			if (node.equals(((FieldAccess) parent).getName())) {
				return getPossibleReferenceBinding(parent);
			}
			break;
			case ASTNode.SUPER_FIELD_ACCESS:
				return getPossibleReferenceBinding(parent);
		case ASTNode.QUALIFIED_NAME:
			if (node.equals(((QualifiedName) parent).getName())) {
				return getPossibleReferenceBinding(parent);
			}
			break;
		default:
			// do nothing
		}
			
		return null;
	}
	
	public static Type guessTypeForReference(AST ast, ASTNode node) {
		ASTNode parent= node.getParent();
		while (parent != null) {
			switch (parent.getNodeType()) {
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
					if (((VariableDeclarationFragment) parent).getInitializer() == node) {
						return ASTNodes.getType(ast, (VariableDeclaration) parent);
					}
					return null;
				case ASTNode.SINGLE_VARIABLE_DECLARATION:
					if (((VariableDeclarationFragment) parent).getInitializer() == node) {
						return ASTNodes.getType(ast, (VariableDeclaration) parent);
					}
					return null;
				case ASTNode.ARRAY_ACCESS:
					if (!((ArrayAccess) parent).getIndex().equals(node)) {
						Type type= guessTypeForReference(ast, parent);
						if (type != null) {
							return ast.newArrayType(type);
						}
					}
					return null;
				case ASTNode.FIELD_ACCESS:
					if (node.equals(((FieldAccess) parent).getName())) {
						node= parent;
						parent= parent.getParent();
					} else {
						return null;
					}
					break;
				case ASTNode.SUPER_FIELD_ACCESS:
				case ASTNode.PARENTHESIZED_EXPRESSION:
					node= parent;
					parent= parent.getParent();
					break;
				case ASTNode.QUALIFIED_NAME:
					if (node.equals(((QualifiedName) parent).getName())) {
						node= parent;
						parent= parent.getParent();
					} else {
						return null;
					}
					break;
				default:
					return null;
			}
		}
		return null;
	}
	

	private static ITypeBinding getParameterTypeBinding(ASTNode node, List args, IMethodBinding binding) {
		ITypeBinding[] paramTypes= binding.getParameterTypes();
		int index= args.indexOf(node);
		if (index >= 0 && index < paramTypes.length) {
			return paramTypes[index];
		}
		return null;
	}
	
    public static ITypeBinding guessBindingForTypeReference(ASTNode node) {
    	return Bindings.normalizeTypeBinding(getPossibleTypeBinding(node));
    }
    	
    private static ITypeBinding getPossibleTypeBinding(ASTNode node) {
    	ASTNode parent= node.getParent();
    	while (parent instanceof Type) {
    		parent= parent.getParent();
    	}
    	switch (parent.getNodeType()) {
    	case ASTNode.VARIABLE_DECLARATION_STATEMENT:
    		return guessVariableType(((VariableDeclarationStatement) parent).fragments());
		case ASTNode.FIELD_DECLARATION:
			return guessVariableType(((FieldDeclaration) parent).fragments());
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION:	
			return guessVariableType(((VariableDeclarationExpression) parent).fragments());
		case ASTNode.SINGLE_VARIABLE_DECLARATION:
			SingleVariableDeclaration varDecl= (SingleVariableDeclaration) parent;
			if (varDecl.getInitializer() != null) {
				return varDecl.getInitializer().resolveTypeBinding();
			}
			break;
		case ASTNode.ARRAY_CREATION:
			ArrayCreation creation= (ArrayCreation) parent;
			if (creation.getInitializer() != null) {
				return creation.getInitializer().resolveTypeBinding();
			}
			return getPossibleReferenceBinding(parent);
        case ASTNode.TYPE_LITERAL:
        case ASTNode.CLASS_INSTANCE_CREATION:
        	return getPossibleReferenceBinding(parent);
            					
     	}   	
    	return null;
    }
    
   	private static ITypeBinding guessVariableType(List fragments) {
		for (Iterator iter= fragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment frag= (VariableDeclarationFragment) iter.next();
			if (frag.getInitializer() != null) {
				return frag.getInitializer().resolveTypeBinding();
			}
		}
		return null;
	}
	 
	public static MethodDeclaration findParentMethodDeclaration(ASTNode node) {
		while ((node != null) && (node.getNodeType() != ASTNode.METHOD_DECLARATION)) {
			node= node.getParent();
		}
		return (MethodDeclaration) node;
	}
	
	public static BodyDeclaration findParentBodyDeclaration(ASTNode node) {
		while ((node != null) && (!(node instanceof BodyDeclaration))) {
			node= node.getParent();
		}
		return (BodyDeclaration) node;
	}
	
	public static CompilationUnit findParentCompilationUnit(ASTNode node) {
		return (CompilationUnit) findAncestor(node, ASTNode.COMPILATION_UNIT);
	}
	
	/**
	 * Returns either a TypeDeclaration or an AnonymousTypeDeclaration
	 * @param node
	 * @return CompilationUnit
	 */
	public static ASTNode findParentType(ASTNode node) {
		while ((node != null) && (node.getNodeType() != ASTNode.TYPE_DECLARATION) && (node.getNodeType() != ASTNode.ANONYMOUS_CLASS_DECLARATION)) {
			node= node.getParent();
		}
		return node;
	}
	
	public static ASTNode findAncestor(ASTNode node, int nodeType) {
		while ((node != null) && (node.getNodeType() != nodeType)) {
			node= node.getParent();
		}
		return node;
	}	
	
	public static Statement findParentStatement(ASTNode node) {
		while ((node != null) && (!(node instanceof Statement))) {
			node= node.getParent();
			if (node instanceof BodyDeclaration) {
				return null;
			}
		}
		return (Statement) node;
	}
	
	public static TryStatement findParentTryStatement(ASTNode node) {
		while ((node != null) && (!(node instanceof TryStatement))) {
			node= node.getParent();
			if (node instanceof BodyDeclaration) {
				return null;
			}
		}
		return (TryStatement) node;
	}	
	
	public static boolean isInStaticContext(ASTNode selectedNode) {
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration) {
			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			if (methodDecl.isConstructor()) {
				Statement statement= findParentStatement(selectedNode);
				if (statement instanceof ConstructorInvocation || statement instanceof SuperConstructorInvocation) {
					return true; // argument in a this or super call 
				}
			}
			return Modifier.isStatic(methodDecl.getModifiers());
		} else if (decl instanceof Initializer) {
			return Modifier.isStatic(((Initializer)decl).getModifiers());
		} else if (decl instanceof FieldDeclaration) {
			return Modifier.isStatic(((FieldDeclaration)decl).getModifiers());
		}
		return false;
	}	
	
	public static boolean isWriteAccess(Name selectedNode) {
		ASTNode curr= selectedNode;
		ASTNode parent= curr.getParent();
		while (parent != null) {
			switch (parent.getNodeType()) {
				case ASTNode.QUALIFIED_NAME:
					if (((QualifiedName) parent).getQualifier() == curr) {
						return false;
					}
					break;
				case ASTNode.FIELD_ACCESS:
					if (((FieldAccess) parent).getExpression() == curr) {
						return false;
					}
					break;					
				case ASTNode.SUPER_FIELD_ACCESS:
					break;
				case ASTNode.ASSIGNMENT:
					return ((Assignment) parent).getLeftHandSide() == curr;
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				case ASTNode.SINGLE_VARIABLE_DECLARATION:
					return ((VariableDeclaration) parent).getName() == curr;
				case ASTNode.POSTFIX_EXPRESSION:
				case ASTNode.PREFIX_EXPRESSION:
					return true;
				default:
					return false;
			}
					
			curr= parent;
			parent= curr.getParent();
		}
		return false;
	}
	
				
	public static Type getTypeFromTypeBinding(AST ast, ITypeBinding binding) {
		if (binding.isArray()) {
			int dim= binding.getDimensions();
			return ast.newArrayType(getTypeFromTypeBinding(ast, binding.getElementType()), dim);
		} else if (binding.isPrimitive()) {
			String name= binding.getName();
			return ast.newPrimitiveType(PrimitiveType.toCode(name));
		} else if (!binding.isNullType() && !binding.isAnonymous()) {
			return ast.newSimpleType(ast.newSimpleName(binding.getName()));
		}
		return null;
	}
	
	private static TypeDeclaration findTypeDeclaration(List decls, String name) {
		for (Iterator iter= decls.iterator(); iter.hasNext();) {
			ASTNode elem= (ASTNode) iter.next();
			if (elem instanceof TypeDeclaration) {
				TypeDeclaration decl= (TypeDeclaration) elem;
				if (name.equals(decl.getName().getIdentifier())) {
					return decl;
				}
			}
		}
		return null;
	}

	public static TypeDeclaration findTypeDeclaration(CompilationUnit root, ITypeBinding binding) {
		ArrayList names= new ArrayList(5);
		while (binding != null) {
			names.add(binding.getName());
			binding= binding.getDeclaringClass();
		}
		List types= root.types();
		for (int i= names.size() - 1; i >= 0; i--) {
			String name= (String) names.get(i);
			TypeDeclaration decl= findTypeDeclaration(types, name);
			if (decl == null || i == 0) {
				return decl;
			}
			types= decl.bodyDeclarations();
		}
		return null;
	}
	
	public static String getFullName(Name name) {
		return ASTNodes.asString(name);
	}
	
	public static String getQualifier(Name name) {
		if (name.isQualifiedName()) {
			return getFullName(((QualifiedName) name).getQualifier());
		}
		return ""; //$NON-NLS-1$
	}
	
	public static String getSimpleName(Name name) {
		if (name.isQualifiedName()) {
			return ((QualifiedName) name).getName().getIdentifier();
		} else {
			return ((SimpleName) name).getIdentifier();
		}
	}
	
	public static ICompilationUnit findCompilationUnitForBinding(ICompilationUnit cu, CompilationUnit astRoot, ITypeBinding binding) throws JavaModelException {
		if (binding != null && binding.isFromSource() && astRoot.findDeclaringNode(binding) == null) {
			ICompilationUnit targetCU= Bindings.findCompilationUnit(binding, cu.getJavaProject());
			if (targetCU != null) {
				return JavaModelUtil.toWorkingCopy(targetCU);
			}
			return null;
		}
		return cu;
	}
	
	
	private static final Code[] CODE_ORDER= { PrimitiveType.CHAR, PrimitiveType.SHORT, PrimitiveType.INT, PrimitiveType.LONG, PrimitiveType.FLOAT, PrimitiveType.DOUBLE };
	
	public static ITypeBinding[] getRelaxingTypes(AST ast, ITypeBinding type) {
		HashSet res= new HashSet();
		res.add(type);
		if (type.isArray()) {
			res.add(ast.resolveWellKnownType("java.lang.Object")); //$NON-NLS-1$
			res.add(ast.resolveWellKnownType("java.io.Serializable")); //$NON-NLS-1$
			res.add(ast.resolveWellKnownType("java.lang.Cloneable")); //$NON-NLS-1$
		} else if (type.isPrimitive()) {
			Code code= PrimitiveType.toCode(type.getName());
			boolean found= false;
			for (int i= 0; i < CODE_ORDER.length; i++) {
				if (found) {
					String typeName= CODE_ORDER[i].toString();
					res.add(ast.resolveWellKnownType(typeName));
				}
				if (code == CODE_ORDER[i]) {
					found= true;
				}
			}
		} else {
			collectRelaxingTypes(res, type);
		}
		return (ITypeBinding[]) res.toArray(new ITypeBinding[res.size()]);
	}
		
	private static void collectRelaxingTypes(Set res, ITypeBinding type) {
		ITypeBinding[] interfaces= type.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			ITypeBinding curr= interfaces[i];
			res.add(curr);
			collectRelaxingTypes(res, curr);
		}
		ITypeBinding binding= type.getSuperclass();
		if (binding != null) {
			res.add(binding);
			collectRelaxingTypes(res, binding);			
		}
	}	

}
