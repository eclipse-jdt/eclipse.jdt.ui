/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.Assert;

public class ASTNodes {

	public static final int NODE_ONLY=					0;
	public static final int INCLUDE_FIRST_PARENT= 	1;
	public static final int INCLUDE_ALL_PARENTS= 	2;
	
	public static final int WARINING=						1 << 0;
	public static final int ERROR=							1 << 1;
	public static final int PROBLEMS=						WARINING | ERROR;

	private static final Message[] EMPTY_MESSAGES= new Message[0];
	private static final IProblem[] EMPTY_PROBLEMS= new IProblem[0];
	
	private static final int[] MODIFIERS= {Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.ABSTRACT, Modifier.STATIC,
		Modifier.FINAL, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.NATIVE, Modifier.SYNCHRONIZED, Modifier.STRICTFP };
	private static final String[] MODIFIER_STRINGS= { "public", "protected", "private", "abstract", "static",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		"final", "transient", "volatile", "native", "synchronized", "strictfp" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

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

	private ASTNodes() {
		// no instance;
	}

	public static String asString(ASTNode node) {
		ASTFlattener flattener= new ASTFlattener();
		node.accept(flattener);
		return flattener.getResult();
	}

    public static String modifierString(int mod) {
		StringBuffer result = new StringBuffer();
		int counter= 0;
		for (int i= 0; i < MODIFIERS.length; i++) {
			if ((mod & MODIFIERS[i]) != 0) {
				if (counter++ > 0)
					result.append(" "); //$NON-NLS-1$
				result.append(MODIFIER_STRINGS[i]);
			}
		}
		return result.toString();
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
		if (expression == null)
			return false;
		int type= expression.getNodeType();
		return type == ASTNode.CHARACTER_LITERAL || type == ASTNode.NULL_LITERAL || type == ASTNode.NUMBER_LITERAL ||
					type == ASTNode.STRING_LITERAL || type == ASTNode.TYPE_LITERAL;
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

	public static int getDelimiterToken(ASTNode node) {
		if (node instanceof VariableDeclarationFragment)
			return ITerminalSymbols.TokenNameCOMMA;
		if (node instanceof SingleVariableDeclaration)
			return ITerminalSymbols.TokenNameCOMMA;
		ASTNode parent= node.getParent();
		if (node instanceof Expression && parent instanceof ForStatement) {
			List updaters= ((ForStatement)parent).updaters();
			if (updaters.contains(node))
				return ITerminalSymbols.TokenNameCOMMA;
		}
		return -1;
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
	
	//recursive
	public static String getNameIdentifier(Name name) {
		if (name.isSimpleName())
			return ((SimpleName) name).getIdentifier();
		if (name.isQualifiedName()) {
			QualifiedName qn= (QualifiedName) name;
			return getNameIdentifier(qn.getQualifier()) + "." + qn.getName().getIdentifier(); //$NON-NLS-1$
		}
		Assert.isTrue(false);
		return ""; //$NON-NLS-1$
	}
	
	public static int clearAccessModifiers(int flags) {
		return clearFlag(Modifier.PROTECTED, clearFlag(Modifier.PUBLIC, clearFlag(Modifier.PRIVATE, flags)));
	}
	
	public static int clearFlag(int flag, int flags){
		return flags & ~ flag;
	}
	
	public static String[] getIdentifiers(QualifiedName qualifiedName) {
		List result= getIdentifierList(qualifiedName);
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	private static List getIdentifierList(Name name) {
		if (name.isSimpleName()){
			List l= new ArrayList(1);
			l.add(((SimpleName)name).getIdentifier());
			return l;
		}	else {
			List l= getIdentifierList(((QualifiedName)name).getQualifier());
			l.add(((QualifiedName)name).getName().getIdentifier());
			return l;
		}
	}
}
