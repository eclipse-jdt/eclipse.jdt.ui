/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug "inline method - doesn't handle implicit cast" (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class ASTNodes {

	public static final int NODE_ONLY=					0;
	public static final int INCLUDE_FIRST_PARENT= 	1;
	public static final int INCLUDE_ALL_PARENTS= 	2;
	
	public static final int WARINING=						1 << 0;
	public static final int ERROR=							1 << 1;
	public static final int PROBLEMS=						WARINING | ERROR;

	private static final Message[] EMPTY_MESSAGES= new Message[0];
	private static final IProblem[] EMPTY_PROBLEMS= new IProblem[0];
	
	private static class ChildrenCollector extends GenericVisitor {
		public List result;

		public ChildrenCollector() {
			result= null;
		}
		protected boolean visitNode(ASTNode node) {
			if (result == null) { // first visitNode: on the node's parent: do nothing, return true
				result= new ArrayList();
				return true;
			}
			result.add(node);
			return false;
		}
	}
	
	private static class ListFinder extends ASTVisitor {
		public List result;
		
		private ASTNode fNode;
		public ListFinder(ASTNode node) {
			fNode= node;
		}
		public boolean visit(AnonymousClassDeclaration node) {
			test(node.bodyDeclarations());
			return false;
		}
		public boolean visit(ArrayCreation node) {
			test(node.dimensions());
			return false;
		}
		public boolean visit(ArrayInitializer node) {
			test(node.expressions());
			return false;
		}		
		public boolean visit(Block node) {
			test(node.statements());
			return false;
		}
		public boolean visit(ClassInstanceCreation node) {
			test(node.arguments());
			return false;
		}
		public boolean visit(CompilationUnit node) {
			test(node.imports());
			test(node.types());
			return false;
		}		
		public boolean visit(ConstructorInvocation node) {
			test(node.arguments());
			return false;
		}
		public boolean visit(FieldDeclaration node) {
			test(node.fragments());
			return false;
		}
		public boolean visit(ForStatement node) {
			test(node.initializers());
			test(node.updaters());
			return false;
		}
		public boolean visit(MethodDeclaration node) {
			test(node.parameters());
			test(node.thrownExceptions());
			return false;
		}
		public boolean visit(MethodInvocation node) {
			test(node.arguments());
			return false;
		}
		public boolean visit(SuperConstructorInvocation node) {
			test(node.arguments());
			return false;
		}
		public boolean visit(SuperMethodInvocation node) {
			test(node.arguments());
			return false;
		}
		public boolean visit(SwitchStatement node) {
			test(node.statements());
			return false;
		}
		public boolean visit(TryStatement node) {
			test(node.catchClauses());
			return false;
		}		
		public boolean visit(TypeDeclaration node) {
			test(node.bodyDeclarations());
			test(node.superInterfaces());
			return false;
		}		
		public boolean visit(VariableDeclarationExpression node) {
			test(node.fragments());
			return false;
		}
		public boolean visit(VariableDeclarationStatement node) {
			test(node.fragments());
			return false;
		}
		private void test(List nodes) {
			if (nodes.contains(fNode)) {
				result= nodes;
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
	
	public static String asFormattedString(ASTNode node, int indent, String lineDelim) {
		String unformatted= asString(node);
		TextEdit edit= CodeFormatterUtil.format2(node, unformatted, indent, lineDelim, null);
		if (edit != null) {
			return CodeFormatterUtil.evaluateFormatterEdit(unformatted, edit, null);
		}
		return unformatted; // unknown node
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
    
	/**
	 * Returns a list of the direct chidrens of a node. The siblings are ordered by start offset.
	 */    
	public static List getChildren(ASTNode node) {
		ChildrenCollector visitor= new ChildrenCollector();
		node.accept(visitor);
		return visitor.result;		
	}
	
	/**
	 * Returns the element type. This is a convenience method that returns its 
	 * argument if it is a simple type and the element type if the parameter is an array type.
	 */
	public static Type getElementType(Type type){
		if (! type.isArrayType()) 
			return type;
		return ((ArrayType)type).getElementType();
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
			TypeDeclaration type= (TypeDeclaration)getParent(invocation, ASTNode.TYPE_DECLARATION);
			if (type != null)
				return type.resolveBinding();
		}
		return result;
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
	
	public static ISourceRange getNodeRangeWithComments(ASTNode node, TokenScanner scanner) {
		int tokenStart= node.getStartPosition();
		int lastPos= 0;
		int nextPos= -1;
		ASTNode parent= node.getParent();
		if (parent != null && parent.getStartPosition() != -1) {
			lastPos= parent.getStartPosition();
		}
		List siblings= ASTNodes.getChildren(parent);
		for (int i= 0; i < siblings.size(); i++) {
			ASTNode curr= (ASTNode) siblings.get(i);
			int offset= curr.getStartPosition();
			if (offset != -1) {
				if (offset < tokenStart) {
					lastPos= getExclusiveEnd(curr);
				}
				if (offset > tokenStart) {
					nextPos= offset;
					break;
				}
			}
		}
		return getNodeRangeWithComments(node, lastPos, nextPos, scanner);
	}
	
	public static ISourceRange getNodeRangeWithComments(ASTNode node, int prevEnd, int nextStart, TokenScanner scanner) {
		try {
			int tokenStart= node.getStartPosition();
			int start= scanner.getTokenCommentStart(prevEnd, tokenStart);
			int end= scanner.getTokenCommentEnd(tokenStart + node.getLength(), nextStart);
			return new SourceRange(start, end - start);
		} catch (CoreException e) {
			return new SourceRange(node.getStartPosition(), node.getLength());
		}
	}		
	
	/*
	 * @param List<BodyDeclaration> container
	 */
	public static int getInsertionIndex(BodyDeclaration member, List container) {
		int memberType= member.getNodeType();
		if (memberType == ASTNode.TYPE_DECLARATION || memberType == ASTNode.INITIALIZER)
			return 0;
		int defaultIndex= container.size();
		if (memberType == ASTNode.FIELD_DECLARATION) {
			int first= -1;
			int last= -1;
			int i= 0;
			for (Iterator iter= container.iterator(); iter.hasNext(); i++) {
				int nodeType= ((BodyDeclaration)iter.next()).getNodeType();
				switch (nodeType) {
					case ASTNode.FIELD_DECLARATION:
						last= i;
						break;
					case ASTNode.METHOD_DECLARATION:
						if (first == -1)
							first= i;
						break;
				}
			}
			if (last != -1)
				return ++last;
			if (first != -1)
				return first;
			return defaultIndex;
		}
		if (memberType == ASTNode.METHOD_DECLARATION) {
			int last= -1;
			int i= 0;
			for (Iterator iter= container.iterator(); iter.hasNext(); i++) {
				int nodeType= ((BodyDeclaration)iter.next()).getNodeType();
				switch (nodeType) {
					case ASTNode.METHOD_DECLARATION:
						last= i;
				}
			}
			if (last != -1)
				return ++last;
			return defaultIndex;
		}
		return defaultIndex;
	}
	
	public static SimpleName getLeftMostSimpleName(QualifiedName name) {
		final SimpleName[] result= new SimpleName[1];
		ASTVisitor visitor= new ASTVisitor() {
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
