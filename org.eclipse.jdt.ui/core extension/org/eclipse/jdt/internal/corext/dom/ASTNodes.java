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
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

public class ASTNodes {

	public static final int NODE_ONLY=					0;
	public static final int INCLUDE_FIRST_PARENT= 	1;
	public static final int INCLUDE_ALL_PARENTS= 	2;
	
	public static final int WARINING=						1 << 0;
	public static final int ERROR=							1 << 1;
	public static final int PROBLEMS=						WARINING | ERROR;

	private static final Message[] EMPTY_MESSAGES= new Message[0];
	private static final IProblem[] EMPTY_PROBLEMS= new IProblem[0];
	
	/**
	 * @deprecated Use list finder
	 */
	private static class NextSiblingVisitor extends GenericVisitor {
		private ASTNode fNode;
		private ASTNode fParent;
		private ASTNode fResult;
		private boolean fNodeFound;
		public NextSiblingVisitor(ASTNode node) {
			fNode= node;
			fParent= node.getParent();
		}
		protected boolean visitNode(ASTNode node) {
			if (node == fParent)
				return true;
			if (node == fNode) {
				fNodeFound= true;
			}
			if (fNodeFound && node.getParent() == fParent)
				fResult= node;
			return false;
		}
		public static ASTNode perform(ASTNode node) {
			ASTNode parent= node.getParent();
			if (parent == null)
				return null;
			NextSiblingVisitor visitor= new NextSiblingVisitor(node);
			parent.accept(visitor);
			return visitor.fResult;
		}
	}
	
	private static class ListFinder extends ASTVisitor {
		private List result;
		private ASTNode fNode;
		public ListFinder(ASTNode node) {
			fNode= node;
		}
		public boolean visit(AnonymousClassDeclaration node) {
			test(node.bodyDeclarations());
			return false;
		}
		public boolean visit(Block node) {
			test(node.statements());
			return false;
		}
		public boolean visit(SwitchStatement node) {
			test(node.statements());
			return false;
		}
		public boolean visit(TypeDeclaration node) {
			test(node.bodyDeclarations());
			return false;
		}
		private boolean test(List nodes) {
			if (nodes.indexOf(fNode) != -1) {
				result= nodes;
				return true;
			}
			return false;
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
	
	public static String asFormattedString(ASTNode node, int indent, String lineDelim) {
		ASTFlattener flattener= new ASTFlattener();
		node.accept(flattener);
		return flattener.getFormattedResult(indent, lineDelim);
	}	

    /**
     * Returns the list that contains the given ASTNode. If the node
     * isn't part of any list, <code>null</code> is returned.
     * 
     * @param node the node in question 
     * @return the list that contains the node or <code>null</code>
     */
    public static List getContainingList(ASTNode node) {
    	if (node.getParent() == null)
    		return null;
    	ListFinder finder= new ListFinder(node);
    	node.getParent().accept(finder);
    	return finder.result;
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
	 * Returns the type node for the given declaration. The returned node
	 * is a copy and is owned by a different AST. The returned node contains
	 * any extra dimensions.
	 */
	public static Type getType(VariableDeclaration declaration) {
		AST ast= new AST();
		Type type= null;
		if (declaration instanceof SingleVariableDeclaration) {
			type= ((SingleVariableDeclaration)declaration).getType();
		} else if (declaration instanceof VariableDeclarationFragment) {
			ASTNode parent= ((VariableDeclarationFragment)declaration).getParent();
			if (parent instanceof VariableDeclarationExpression)
				type= ((VariableDeclarationExpression)parent).getType();
			else if (parent instanceof VariableDeclarationStatement)
				type= ((VariableDeclarationStatement)parent).getType();
		}
		if (type == null)
			return null;
		type= (Type)ASTNode.copySubtree(ast, type);
		int extraDim= 0;
		if (declaration.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			extraDim= ((VariableDeclarationFragment)declaration).getExtraDimensions();
		} else if (declaration.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION) {
			extraDim= ((SingleVariableDeclaration)declaration).getExtraDimensions();
		}
		for (int i= 0; i < extraDim; i++) {
			type= ast.newArrayType(type);
		}
		return type;		
	}
	
	public static int getModifiers(VariableDeclaration declaration) {
		Assert.isNotNull(declaration);
		if (declaration instanceof SingleVariableDeclaration) {
			return ((SingleVariableDeclaration)declaration).getModifiers();
		} else if (declaration instanceof VariableDeclarationFragment) {
			ASTNode parent= declaration.getParent();
			if (parent instanceof VariableDeclarationExpression)
				return ((VariableDeclarationExpression)parent).getModifiers();
			else if (parent instanceof VariableDeclarationStatement)
				return ((VariableDeclarationStatement)parent).getModifiers();
		}
		return 0;		
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
	
	public static boolean isLiteral(Expression expression) {
		int type= expression.getNodeType();
		return type == ASTNode.BOOLEAN_LITERAL || type == ASTNode.CHARACTER_LITERAL || type == ASTNode.NULL_LITERAL || 
			type == ASTNode.NUMBER_LITERAL || type == ASTNode.STRING_LITERAL || type == ASTNode.TYPE_LITERAL;
	}
	
	public static boolean isInvocation(Expression expression) {
		int type= expression.getNodeType();
		return type == ASTNode.METHOD_INVOCATION || type == ASTNode.SUPER_METHOD_INVOCATION;
	}
	
	public static String getTypeName(Type type) {
		final StringBuffer buffer= new StringBuffer();
		ASTVisitor visitor= new ASTVisitor() {
			public boolean visit(PrimitiveType node) {
				buffer.append(node.getPrimitiveTypeCode().toString());
				return false;
			}
			public boolean visit(SimpleName node) {
				buffer.append(node.getIdentifier());
				return false;
			}
			public boolean visit(QualifiedName node) {
				buffer.append(node.getName().getIdentifier());
				return false;
			}
			public void endVisit(ArrayType node) {
				buffer.append("[]"); //$NON-NLS-1$
			}
		};
		type.accept(visitor);
		return buffer.toString();
	}
	
	public static boolean needsParentheses(Expression expression) {
		int type= expression.getNodeType();
		return type == ASTNode.INFIX_EXPRESSION || type == ASTNode.CONDITIONAL_EXPRESSION ||
			type == ASTNode.PREFIX_EXPRESSION || type == ASTNode.POSTFIX_EXPRESSION ||
			type == ASTNode.CAST_EXPRESSION;
	}
	
	public static boolean substituteMustBeParenthesized(Expression substitute, Expression location) {
    	if (!needsParentheses(substitute))
    		return false;
    		
    	ASTNode parent= location.getParent();
    	if (parent instanceof VariableDeclarationFragment){
    		VariableDeclarationFragment vdf= (VariableDeclarationFragment)parent;
    		if (vdf.getInitializer().equals(location))
    			return false;
    	} else if (parent instanceof MethodInvocation){
    		MethodInvocation mi= (MethodInvocation)parent;
    		if (mi.arguments().contains(location))
    			return false;
    	} else if (parent instanceof ReturnStatement)
    		return false;
    		
        return true;		
	}
	
	public static ASTNode getParent(ASTNode node, Class parentClass) {
		do {
			node= node.getParent();
		} while (node != null && !parentClass.isInstance(node));
		return node;
	}
	
	public static ASTNode getParent(ASTNode node, int nodeType) {
		do {
			node= node.getParent();
		} while (node != null && node.getNodeType() != nodeType);
		return node;
	}	
	
	public static boolean isParent(ASTNode node, ASTNode parent) {
		Assert.isNotNull(parent);
		do {
			node= node.getParent();
			if (node == parent)
				return true;
		} while (node != null);
		return false;
	}
	
	public static ASTNode getNextSibling(ASTNode node) {
		return NextSiblingVisitor.perform(node);
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
	 * Expands the range of the node passed in <code>nodes</code> to cover all comments
	 * determined by <code>start</code> and <code>length</code> in the given text buffer. 
	 */
	public static void expandRange(ASTNode[] nodes, TextBuffer buffer, int start, int length) throws CoreException {
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(buffer.getContent(start, length).toCharArray());
		TokenScanner tokenizer= new TokenScanner(scanner);
		ASTNode node= nodes[0]; 
		int pos= tokenizer.getNextStartOffset(0, false);
		int newStart= start + pos;
		if (newStart < node.getStartPosition())
			node.setSourceRange(newStart, node.getLength() + node.getStartPosition() - newStart);
		
		node= nodes[nodes.length - 1];
		int scannerStart= node.getStartPosition() + node.getLength() - start;
		pos= scannerStart;
		try {
			while (true) {
				pos= tokenizer.getNextEndOffset(pos, false); 
			}
		} catch (CoreException e) {
		}
		node.setSourceRange(node.getStartPosition(), node.getLength() + pos - scannerStart);
	}
	
	public static IProblem[] getProblems(ASTNode node, int scope, int severity) {
		ASTNode root= node.getRoot();
		if (!(root instanceof CompilationUnit))
			return EMPTY_PROBLEMS;
		IProblem[] problems= ((CompilationUnit)root).getProblems();
		if (root == node)
			return problems;
		final int iterations= computeIterations(scope);
		List result= new ArrayList(5);
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			boolean consider= false;
			if ((severity & PROBLEMS) == PROBLEMS)
				consider= true;
			else if ((severity & WARINING) != 0)
				consider= problem.isWarning();
			else if ((severity & ERROR) != 0)
				consider= problem.isError();
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
		return (IProblem[]) result.toArray(new IProblem[result.size()]);
	}
	
	public static Message[] getMessages(ASTNode node, int flags) {
		ASTNode root= node.getRoot();
		if (!(root instanceof CompilationUnit))
			return EMPTY_MESSAGES;
		Message[] messages= ((CompilationUnit)root).getMessages();
		if (root == node)
			return messages;
		final int iterations= computeIterations(flags);
		List result= new ArrayList(5);
		for (int i= 0; i < messages.length; i++) {
			Message message= messages[i];
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
		return (Message[]) result.toArray(new Message[result.size()]);
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
}
