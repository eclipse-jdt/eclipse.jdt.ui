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

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public abstract class AbstractMethodCompletionProposal extends LinkedCorrectionProposal {

	private ASTNode fNode;
	private ITypeBinding fSenderBinding;
		
	public AbstractMethodCompletionProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		
		fNode= invocationNode;
		fSenderBinding= binding;
	}
	
	protected ASTNode getInvocationNode() {
		return fNode;
	}
	
	protected ITypeBinding getSenderBinding() {
		return fSenderBinding;
	}
			
	protected ASTRewrite getRewrite() throws CoreException {
		ITypeBinding sender= fSenderBinding;
		if (sender.isParameterizedType() || sender.isRawType()) {
			sender= sender.getGenericType();
		}
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode typeDecl= astRoot.findDeclaringNode(sender);
		ASTNode newTypeDecl= null;
		boolean isInDifferentCU;
		if (typeDecl != null) {
			isInDifferentCU= false;
			newTypeDecl= typeDecl;
		} else {
			isInDifferentCU= true;
			ASTParser astParser= ASTParser.newParser(ASTProvider.AST_LEVEL);
			astParser.setSource(getCompilationUnit());
			astParser.setResolveBindings(true);
			astRoot= (CompilationUnit) astParser.createAST(null);
			newTypeDecl= astRoot.findDeclaringNode(sender.getKey());
		}
		if (newTypeDecl != null) {
			ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
			
			MethodDeclaration newStub= getStub(rewrite, newTypeDecl);
			
			ChildListPropertyDescriptor property= ASTNodes.getBodyDeclarationsProperty(newTypeDecl);
			List members= (List) newTypeDecl.getStructuralProperty(property);
			
			int insertIndex;
			if (isConstructor()) {
				insertIndex= findConstructorInsertIndex(members);
			} else if (!isInDifferentCU) {
				insertIndex= findMethodInsertIndex(members, fNode.getStartPosition());
			} else {
				insertIndex= members.size();
			}
			ListRewrite listRewriter= rewrite.getListRewrite(newTypeDecl, property);
			listRewriter.insertAt(newStub, insertIndex, null);	
			
			return rewrite;
		}
		return null;
	}
		
	private MethodDeclaration getStub(ASTRewrite rewrite, ASTNode targetTypeDecl) throws CoreException {
		AST ast= targetTypeDecl.getAST();
		MethodDeclaration decl= ast.newMethodDeclaration();
		
		SimpleName newNameNode= getNewName(rewrite);
		
		decl.setConstructor(isConstructor());
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, evaluateModifiers(targetTypeDecl)));
		
		ArrayList takenNames= new ArrayList();
		addNewTypeParameters(rewrite, takenNames, decl.typeParameters());
		
		decl.setName(newNameNode);
		
		IVariableBinding[] declaredFields= fSenderBinding.getDeclaredFields();
		for (int i= 0; i < declaredFields.length; i++) { // avoid to take parameter names that are equal to field names
			takenNames.add(declaredFields[i].getName());
		}
		
		addNewParameters(rewrite, takenNames, decl.parameters());
		addNewExceptions(rewrite, decl.thrownExceptions());
	
		Block body= null;

		String bodyStatement= ""; //$NON-NLS-1$
		if (!isConstructor()) {
			Type returnType= getNewMethodType(rewrite);
			if (returnType == null) {
				decl.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
			} else {
				decl.setReturnType2(returnType);
			}
			if (!fSenderBinding.isInterface() && returnType != null) {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(ASTNodeFactory.newDefaultExpression(ast, returnType, 0));
				bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, String.valueOf('\n'));
			}
		}
		if (!fSenderBinding.isInterface()) {
			body= ast.newBlock();
			String placeHolder= CodeGeneration.getMethodBodyContent(getCompilationUnit(), fSenderBinding.getName(), newNameNode.getIdentifier(), isConstructor(), bodyStatement, String.valueOf('\n')); 	
			if (placeHolder != null) {
				ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				body.statements().add(todoNode);
			}
		}
		decl.setBody(body);

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(getCompilationUnit().getJavaProject());
		if (settings.createComments && !fSenderBinding.isAnonymous()) {
			String string= CodeGeneration.getMethodComment(getCompilationUnit(), fSenderBinding.getName(), decl, null, String.valueOf('\n'));
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}
	
	
			
	private int findMethodInsertIndex(List decls, int currPos) {
		int nDecls= decls.size();
		for (int i= 0; i < nDecls; i++) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof MethodDeclaration && currPos < curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return nDecls;
	}
	
	private int findConstructorInsertIndex(List decls) {
		int nDecls= decls.size();
		int lastMethod= 0;
		for (int i= nDecls - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof MethodDeclaration) {
				if (((MethodDeclaration) curr).isConstructor()) {
					return i + 1;
				}
				lastMethod= i;
			}
		}
		return lastMethod;
	}
	
	protected abstract boolean isConstructor();

	protected abstract void addNewTypeParameters(ASTRewrite rewrite, List takenNames, List params) throws CoreException;
	protected abstract void addNewParameters(ASTRewrite rewrite, List takenNames, List params) throws CoreException;
	protected abstract void addNewExceptions(ASTRewrite rewrite, List exceptions) throws CoreException;

	protected abstract SimpleName getNewName(ASTRewrite rewrite);
	protected abstract int evaluateModifiers(ASTNode targetTypeDecl);
	protected abstract Type getNewMethodType(ASTRewrite rewrite) throws CoreException;


}
