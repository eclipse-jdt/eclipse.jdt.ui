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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;

public class ASTResolving {
	
	private static class SelectionFinder extends GenericVisitor {
		
		private int fStart;
		private int fEnd;
		
		private ASTNode fCoveringNode;
		private ASTNode fCoveredNode;
		
		public SelectionFinder(int offset, int length) {
			fStart= offset;
			fEnd= offset + length;
		}

		protected boolean visitNode(ASTNode node) {
			int nodeStart= node.getStartPosition();
			int nodeEnd= nodeStart + node.getLength();
			if (nodeStart <= fStart && fEnd <= nodeEnd) {
				fCoveringNode= node;
			} else {
				return false;
			}
			if (fStart <= nodeStart && nodeEnd <= fEnd) {
				fCoveredNode= node;
				return false;
			}
			return true;
		}

		/**
		 * Returns the coveredNode.
		 * @return ASTNode
		 */
		public ASTNode getCoveredNode() {
			return fCoveredNode;
		}

		/**
		 * Returns the coveringNode.
		 * @return ASTNode
		 */
		public ASTNode getCoveringNode() {
			return fCoveringNode;
		}

	}
	

	public static ASTNode findCoveringNode(CompilationUnit cuNode, int offset, int length) {
		SelectionFinder selectionFinder= new SelectionFinder(offset, length);
		cuNode.accept(selectionFinder);
		return selectionFinder.getCoveringNode();
	}

	public static ASTNode findSelectedNode(CompilationUnit cuNode, int offset, int length) {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(offset, length), true);
		cuNode.accept(analyzer);
		if (analyzer.hasSelectedNodes()) {
			return analyzer.getFirstSelectedNode();
		}
		return analyzer.getLastCoveringNode();
	}
	
	public static ITypeBinding getTypeBinding(ITypeBinding binding) {
		if (binding != null && !binding.isNullType() && !"void".equals(binding.getName())) {
			if (binding.isAnonymous()) {
				ITypeBinding[] baseBindings= binding.getInterfaces();
				if (baseBindings.length > 0) {
					return baseBindings[0];
				}
				return binding.getSuperclass();
			}
			return binding;
		}
		return null;
	}
	
	
	public static ITypeBinding guessBindingForReference(ASTNode node) {
		return getTypeBinding(getPossibleReferenceBinding(node));
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
			if (node.equals(infix.getLeftOperand())) {
				// xx == expression
				return infix.getRightOperand().resolveTypeBinding();
			}
			// expression == xx
			InfixExpression.Operator op= infix.getOperator();
			if (op == InfixExpression.Operator.LEFT_SHIFT || op == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED		
					|| op == InfixExpression.Operator.RIGHT_SHIFT_SIGNED) {
				return infix.getAST().resolveWellKnownType("int"); //$NON-NLS-1$
			}
			return infix.getLeftOperand().resolveTypeBinding();
		case ASTNode.INSTANCEOF_EXPRESSION:
			InstanceofExpression instanceofExpression= (InstanceofExpression) parent;
			return instanceofExpression.getRightOperand().resolveBinding();
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
			VariableDeclarationFragment frag= (VariableDeclarationFragment) parent;
			if (frag.getInitializer().equals(node)) {
				ASTNode declaration= frag.getParent();
				if (declaration instanceof VariableDeclarationStatement) {
					return ((VariableDeclarationStatement)declaration).getType().resolveBinding();
				} else if (declaration instanceof FieldDeclaration) {
					return ((FieldDeclaration)declaration).getType().resolveBinding();
				}
			}
			break;
		case ASTNode.SUPER_METHOD_INVOCATION:
			MethodInvocation superMethodInvocation= (MethodInvocation) parent;
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
			}
			break;
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
		case ASTNode.QUALIFIED_NAME:
			if (node.equals(((QualifiedName) parent).getName())) {
				return getPossibleReferenceBinding(parent);
			}
			break;
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
	
    public static ITypeBinding guessBindingForTypeReference(ASTNode node, boolean ignoreDeclarations) {
    	return getTypeBinding(getPossibleTypeBinding(node, ignoreDeclarations));
    }
    	
    private static ITypeBinding getPossibleTypeBinding(ASTNode node, boolean ignoreDeclarations) {
    	AST ast= node.getAST();
    	ASTNode parent= node.getParent();
    	while (parent instanceof Type) {
    		parent= parent.getParent();
    	}
    	switch (parent.getNodeType()) {
    	case ASTNode.METHOD_DECLARATION:
			MethodDeclaration decl= (MethodDeclaration) parent;
			if (decl.thrownExceptions().contains(node)) {
				return ast.resolveWellKnownType("java.lang.Exception");
			}
			break;
		case ASTNode.INSTANCEOF_EXPRESSION:
			InstanceofExpression instanceofExpression= (InstanceofExpression) parent;
			return instanceofExpression.getLeftOperand().resolveTypeBinding();
    	case ASTNode.VARIABLE_DECLARATION_STATEMENT:
    		if (ignoreDeclarations) {
    			return null;
    		}
    		return guessVariableType(((VariableDeclarationStatement) parent).fragments());
		case ASTNode.FIELD_DECLARATION:
    		if (ignoreDeclarations) {
    			return null;
    		}		
			return guessVariableType(((FieldDeclaration) parent).fragments());
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
    		if (ignoreDeclarations) {
    			return null;
    		}		
			return guessVariableType(((VariableDeclarationExpression) parent).fragments());
		case ASTNode.SINGLE_VARIABLE_DECLARATION:
    		if (ignoreDeclarations) {
    			return null;
    		}		
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
        case ASTNode.CATCH_CLAUSE:
        case ASTNode.THROW_STATEMENT:
            return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
        case ASTNode.TYPE_LITERAL:
        case ASTNode.CLASS_INSTANCE_CREATION:
        case ASTNode.CAST_EXPRESSION:
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
		while ((node != null) && (node.getNodeType() != ASTNode.COMPILATION_UNIT)) {
			node= node.getParent();
		}
		return (CompilationUnit) node;
	}
	
	/**
	 * Returns either a TypeDeclaration or an AnonymousTypeDeclaration	 * @param node	 * @return CompilationUnit	 */
	public static ASTNode findParentType(ASTNode node) {
		while ((node != null) && (node.getNodeType() != ASTNode.TYPE_DECLARATION) && (node.getNodeType() != ASTNode.ANONYMOUS_CLASS_DECLARATION)) {
			node= node.getParent();
		}
		return node;
	}
	
	/**
	 * Returns the type binding of the node's parent type declararation
	 * @param node
	 * @return CompilationUnit
	 */
	public static ITypeBinding getBindingOfParentType(ASTNode node) {
		while (node != null) {
			if (node instanceof TypeDeclaration) {
				return ((TypeDeclaration) node).resolveBinding();
			} else if (node instanceof AnonymousClassDeclaration) {
				return ((AnonymousClassDeclaration) node).resolveBinding();
			}
			node= node.getParent();
		}
		return null;
	}		
	
	
	public static Statement findParentStatement(ASTNode node) {
		while ((node != null) && (!(node instanceof Statement))) {
			node= node.getParent();
		}
		return (Statement) node;
	}
	
	public static boolean isInStaticContext(ASTNode selectedNode) {
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration) {
			return Modifier.isStatic(((MethodDeclaration)decl).getModifiers());
		} else if (decl instanceof Initializer) {
			return Modifier.isStatic(((Initializer)decl).getModifiers());
		} else if (decl instanceof FieldDeclaration) {
			return Modifier.isStatic(((FieldDeclaration)decl).getModifiers());
		}
		return false;
	}	
	
	
	public static IScanner createScanner(ICompilationUnit cu, int pos) throws InvalidInputException, JavaModelException {
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		IBuffer buf= cu.getBuffer();
		scanner.setSource(buf.getCharacters());
		scanner.resetTo(pos, buf.getLength());
		return scanner;
	}
	
	public static void overreadToken(IScanner scanner, int[] prevTokens) throws InvalidInputException {
		boolean found;
		do {
			found= false;
			int curr= scanner.getNextToken();
			if (curr == ITerminalSymbols.TokenNameEOF) {
				throw new InvalidInputException("End of File");
			}
			for (int i= 0; i < prevTokens.length; i++) {
				if (prevTokens[i] == curr) {
					found= true;
					break;
				}
			}
		} while (found);
	}
		
	public static void readToToken(IScanner scanner, int tok) throws InvalidInputException {
		int curr= 0;
		do {
			curr= scanner.getNextToken();
			if (curr == ITerminalSymbols.TokenNameEOF) {
				throw new InvalidInputException("End of File");
			}
		} while (curr != tok); 
	}	
	
	public static int getPositionAfter(IScanner scanner, int token) throws InvalidInputException {
		readToToken(scanner, token);
		return scanner.getCurrentTokenEndPosition() + 1;
	}
	
	public static int getPositionBefore(IScanner scanner, int token) throws InvalidInputException {
		readToToken(scanner, token);
		return scanner.getCurrentTokenStartPosition();
	}
	
	public static int getPositionAfter(IScanner scanner, int defaultPos, int[] tokens) throws InvalidInputException {
		int pos= defaultPos;
		loop: while(true) {
			int curr= scanner.getNextToken();
			if (curr == ITerminalSymbols.TokenNameEOF) {
				return pos;
			}
			for (int i= 0; i < tokens.length; i++) {
				if (tokens[i] == curr) {
					pos= scanner.getCurrentTokenEndPosition() + 1;
					continue loop;
				}
			}
			return pos;
		}
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
	
	public static Expression getInitExpression(Type type) {
		if (type.isPrimitiveType()) {
			PrimitiveType primitiveType= (PrimitiveType) type;
			if (primitiveType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
				return type.getAST().newBooleanLiteral(false);
			} else if (primitiveType.getPrimitiveTypeCode() == PrimitiveType.VOID) {
				return null;				
			} else {
				return type.getAST().newNumberLiteral("0");
			}
		}
		return type.getAST().newNullLiteral();
	}
	
	private static class NodeFoundException extends RuntimeException {
		public ASTNode node;
	}
	
	
	public static ASTNode findClonedNode(ASTNode cloneRoot, final ASTNode node) {
		try {
			cloneRoot.accept(new ASTVisitor() {
				public void preVisit(ASTNode curr) {
					if (curr.getNodeType() == node.getNodeType() && curr.getStartPosition() == node.getStartPosition() && curr.getLength() == node.getLength()) {
						NodeFoundException exc= new NodeFoundException();
						exc.node= curr;
						throw exc;
					}
				}
			});
		} catch (NodeFoundException e) {
			return e.node;
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
	
	public static String getQualifier(Name name) {
		if (name.isQualifiedName()) {
			return ASTNodes.asString(((QualifiedName) name).getQualifier());
		}
		return "";
	}
	
	public static String getSimpleName(Name name) {
		if (name.isQualifiedName()) {
			return ((QualifiedName) name).getName().getIdentifier();
		} else {
			return ((SimpleName) name).getIdentifier();
		}
	}	
	
	
}
