/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;

public class ASTResolving {

	public static ASTNode findSelectedNode(CompilationUnit cuNode, int offset, int length) {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(offset, length), true);
		cuNode.accept(analyzer);
		
		return analyzer.getFirstSelectedNode();
	}
	
	public static ITypeBinding getTypeBinding(ASTNode node) {
		ITypeBinding binding= getPossibleTypeBinding(node);
		if (binding != null) {
			if (binding.isAnonymous()) {
				return binding.getSuperclass();
			} else if (binding.isPrimitive()) {
				String name= binding.getName();
				if ("void".equals(name) || "null".equals(name)) {
					return node.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
			}
		}
		return binding; 
	}
			
			
		
		
	private static ITypeBinding getPossibleTypeBinding(ASTNode node) {	
		ASTNode parent= node.getParent();
		if (parent instanceof Assignment) {
			Assignment assignment= (Assignment) parent;
			if (node.equals(assignment.getLeftHandSide())) {
				// field write access: xx= expression
				return assignment.getRightHandSide().resolveTypeBinding();
			}
			// read access
			return assignment.getLeftHandSide().resolveTypeBinding();
		} else if (parent instanceof InfixExpression) {
			InfixExpression infix= (InfixExpression) parent;
			InfixExpression.Operator op= infix.getOperator();
			if (node.equals(infix.getLeftOperand())) {
				// xx == expression
				if (op == InfixExpression.Operator.INSTANCEOF) {
					ASTNode left= infix.getRightOperand();
					if (left instanceof SimpleName) {
						return ASTNodes.getTypeBinding(((SimpleName) left));
					}
				}
				return infix.getRightOperand().resolveTypeBinding();
			}
			// expression == xx
			if (op == InfixExpression.Operator.LEFT_SHIFT || op == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED		
					|| op == InfixExpression.Operator.RIGHT_SHIFT_SIGNED) {
				return infix.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
			return infix.getLeftOperand().resolveTypeBinding();
		} else if (parent instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment frag= (VariableDeclarationFragment) parent;
			if (frag.getInitializer().equals(node)) {
				// int val= xx;
				VariableDeclarationStatement stmt= (VariableDeclarationStatement) frag.getParent();
				return stmt.getType().resolveBinding();
			}
		} else if (parent instanceof MethodInvocation) {
			MethodInvocation invocation= (MethodInvocation) parent;
			SimpleName name= invocation.getName();
			IMethodBinding binding= ASTNodes.getMethodBinding(name);
			if (binding != null) {
				return getParameterTypeBinding(node, invocation.arguments(), binding);
			}				
		} else if (parent instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation invocation= (SuperConstructorInvocation) parent;
			IMethodBinding binding= invocation.resolveConstructorBinding();
			if (binding != null) {
				return getParameterTypeBinding(node, invocation.arguments(), binding);
			}
		} else if (parent instanceof ConstructorInvocation) {
			ConstructorInvocation invocation= (ConstructorInvocation) parent;
			IMethodBinding binding= invocation.resolveConstructorBinding();
			if (binding != null) {
				return getParameterTypeBinding(node, invocation.arguments(), binding);
			}			
		} else if (parent instanceof ClassInstanceCreation) {
			ClassInstanceCreation creation= (ClassInstanceCreation) parent;
			IMethodBinding binding= creation.resolveConstructorBinding();
			if (binding != null) {
				return getParameterTypeBinding(node, creation.arguments(), binding);
			}
		} else if (parent instanceof ParenthesizedExpression) {
			return getTypeBinding(parent);
		} else if (parent instanceof ArrayAccess) {
			if (((ArrayAccess) parent).getIndex().equals(node)) {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
		} else if (parent instanceof ArrayCreation) {
			if (((ArrayCreation) parent).dimensions().contains(node)) {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}			
		} else if (parent instanceof ArrayInitializer) {
			ASTNode creation= parent.getParent();
			if (creation instanceof ArrayCreation) {
				return ((ArrayCreation) creation).getType().getElementType().resolveBinding();
			}
		} else if (parent instanceof ConditionalExpression) {
			ConditionalExpression expression= (ConditionalExpression) parent;
			if (node.equals(expression.getExpression())) {
				return parent.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			} else {
				if (node.equals(expression.getElseExpression())) {
					return expression.getThenExpression().resolveTypeBinding();
				}
				return expression.getElseExpression().resolveTypeBinding();
			}
		} else if (parent instanceof PostfixExpression) {
			return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
		} else if (parent instanceof PrefixExpression) {
			if (((PrefixExpression) parent).getOperator() == PrefixExpression.Operator.NOT) {
				return parent.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			} else {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
		} else if (parent instanceof IfStatement || parent instanceof WhileStatement || parent instanceof DoStatement) {
			if (node instanceof Expression) {
				return parent.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
			}
		} else if (parent instanceof SwitchStatement) {
			if (((SwitchStatement) parent).getExpression().equals(node)) {
				return parent.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
		} else if (parent instanceof ReturnStatement) {
			MethodDeclaration decl= findParentMethodDeclaration(parent);
			if (decl != null) {
				return decl.getReturnType().resolveBinding();
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
	
	private static MethodDeclaration findParentMethodDeclaration(ASTNode node) {
		while ((node != null) && !(node instanceof MethodDeclaration)) {
			node= node.getParent();
		}
		return (MethodDeclaration) node;
	}
	
	public static IScanner createScanner(ICompilationUnit cu, int pos) throws InvalidInputException, JavaModelException {
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		IBuffer buf= cu.getBuffer();
		scanner.setSource(buf.getCharacters());
		scanner.resetTo(pos, buf.getLength());
		return scanner;
	}
	
	

}
