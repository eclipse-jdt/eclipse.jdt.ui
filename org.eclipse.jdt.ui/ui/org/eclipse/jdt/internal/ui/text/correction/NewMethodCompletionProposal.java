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

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;

public class NewMethodCompletionProposal extends AbstractMethodCompletionProposal {

	private List fArguments;
		
	public NewMethodCompletionProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode,  List arguments, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, invocationNode, binding, relevance, image);
		fArguments= arguments;
	}
				
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#updateLinkedNodes(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, org.eclipse.jdt.core.dom.MethodDeclaration, boolean)
	 */
	protected void updateLinkedNodes(ASTRewrite rewrite, MethodDeclaration newStub, boolean isInDifferentCU) {
		super.updateLinkedNodes(rewrite, newStub, isInDifferentCU);
		if (!isInDifferentCU) {
			Name invocationName= getInvocationNameNode();
			if (invocationName != null) {
				addLinkedPosition(rewrite.track(invocationName), true, KEY_NAME);
			}				
		}
	}
	
	private Name getInvocationNameNode() {
		ASTNode node= getInvocationNode();
		if (node instanceof MethodInvocation) {
			return ((MethodInvocation)node).getName();
		} else if (node instanceof SuperMethodInvocation) {
			return ((SuperMethodInvocation)node).getName();
		} else if (node instanceof ClassInstanceCreation) {
			return ((ClassInstanceCreation)node).getName();
		}		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#evaluateModifiers(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected int evaluateModifiers(ASTNode targetTypeDecl) {
		if (getSenderBinding().isInterface()) {
			// for interface members copy the modifiers from an existing field
			MethodDeclaration[] methodDecls= ((TypeDeclaration) targetTypeDecl).getMethods();
			if (methodDecls.length > 0) {
				return methodDecls[0].getModifiers();
			}
			return 0;
		}
		ASTNode invocationNode= getInvocationNode();
		if (invocationNode instanceof MethodInvocation) {
			int modifiers= 0;
			Expression expression= ((MethodInvocation)invocationNode).getExpression();
			if (expression != null) {
				if (expression instanceof Name && ((Name) expression).resolveBinding().getKind() == IBinding.TYPE) {
					modifiers |= Modifier.STATIC;
				}
			} else if (ASTResolving.isInStaticContext(invocationNode)) {
				modifiers |= Modifier.STATIC;
			}
			ASTNode node= ASTResolving.findParentType(invocationNode);
			if (targetTypeDecl.equals(node)) {
				modifiers |= Modifier.PRIVATE;
			} else if (node instanceof AnonymousClassDeclaration && ASTNodes.isParent(node, targetTypeDecl)) {
				modifiers |= Modifier.PROTECTED;
			} else {
				modifiers |= Modifier.PUBLIC;
			}
			return modifiers;
		}
		return Modifier.PUBLIC;
	}
	
	/*(non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#isConstructor()
	 */
	protected boolean isConstructor() {
		ASTNode node= getInvocationNode();
		
		return node.getNodeType() != ASTNode.METHOD_INVOCATION && node.getNodeType() != ASTNode.SUPER_METHOD_INVOCATION;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#getMethodName()
	 */
	protected String getMethodName() {
		ASTNode invocationNode= getInvocationNode();
		
		if (invocationNode instanceof MethodInvocation) {
			return ((MethodInvocation)invocationNode).getName().getIdentifier();
		} else if (invocationNode instanceof SuperMethodInvocation) {
			return ((SuperMethodInvocation)invocationNode).getName().getIdentifier();
		} else {
			return getSenderBinding().getName(); // name of the class
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#evaluateMethodType(org.eclipse.jdt.core.dom.AST)
	 */
	protected Type evaluateMethodType(AST ast) throws CoreException {
		ASTNode node= getInvocationNode();
		
		ITypeBinding binding= ASTResolving.guessBindingForReference(node);
		if (binding != null) {
			String typeName= getImportRewrite().addImport(binding);
			return ASTNodeFactory.newType(ast, typeName);			
		} else {
			ASTNode parent= node.getParent();
			if (parent instanceof ExpressionStatement) {
				return null;
			}
			Type type= ASTResolving.guessTypeForReference(ast, node);
			if (type != null) {
				return type;
			}
			return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
		}
	}
	
	/*(non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#addNewParameters(org.eclipse.jdt.core.dom.AST, java.util.List, java.util.List)
	 */
	protected void addNewParameters(AST ast, ASTRewrite rewrite, List takenNames, List params) throws CoreException {
		List arguments= fArguments;

		for (int i= 0; i < arguments.size(); i++) {
			Expression elem= (Expression) arguments.get(i);
			SingleVariableDeclaration param= ast.newSingleVariableDeclaration();

			// argument type
			String argTypeKey= "arg_type_" + i; //$NON-NLS-1$
			Type type= evaluateParameterTypes(ast, elem, argTypeKey);
			param.setType(type);

			// argument name
			String argNameKey= "arg_name_" + i; //$NON-NLS-1$
			String name= evaluateParameterNames(takenNames, elem, type, argNameKey);
			param.setName(ast.newSimpleName(name));

			params.add(param);

			addLinkedPosition(rewrite.track(param.getType()), false, argTypeKey);
			addLinkedPosition(rewrite.track(param.getName()), false, argNameKey);
		}
	}
	
	private Type evaluateParameterTypes(AST ast, Expression elem, String key) throws CoreException {
		ITypeBinding binding= Bindings.normalizeTypeBinding(elem.resolveTypeBinding());
		if (binding != null) {
			ITypeBinding[] typeProposals= ASTResolving.getRelaxingTypes(ast, binding);
			for (int i= 0; i < typeProposals.length; i++) {
				addLinkedPositionProposal(key, typeProposals[i]);
			}		
			String typeName= getImportRewrite().addImport(binding);
			return ASTNodeFactory.newType(ast, typeName);
		}
		return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
	}
	
	private String evaluateParameterNames(List takenNames, Expression argNode, Type type, String key) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		String[] excludedNames= (String[]) takenNames.toArray(new String[takenNames.size()]);
		String favourite= null;
		if (argNode instanceof SimpleName) {
			SimpleName name= (SimpleName) argNode;
			favourite= StubUtility.suggestArgumentName(project, name.getIdentifier(), excludedNames);
		}
		
		int dim= 0;
		if (type.isArrayType()) {
			ArrayType arrayType= (ArrayType) type;
			dim= arrayType.getDimensions();
			type= arrayType.getElementType();
		}
		String typeName= ASTNodes.asString(type);
		String packName= Signature.getQualifier(typeName);
		
		String[] names= NamingConventions.suggestArgumentNames(project, packName, typeName, dim, excludedNames);
		if (favourite == null) {
			favourite= names[0];
		}
		for (int i= 0; i < names.length; i++) {
			addLinkedPositionProposal(key, names[i], null);
		}
		
		takenNames.add(favourite);
		return favourite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#addNewExceptions(org.eclipse.jdt.core.dom.AST, java.util.List)
	 */
	protected void addNewExceptions(AST ast, ASTRewrite rewrite,List params) throws CoreException {
	}	

}
