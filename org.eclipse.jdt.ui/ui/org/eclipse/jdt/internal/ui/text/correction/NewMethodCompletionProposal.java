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
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewMethodCompletionProposal extends ASTRewriteCorrectionProposal {

	private ASTNode fNode; // MethodInvocation, ConstructorInvocation, SuperConstructorInvocation, ClassInstanceCreation, SuperMethodInvocation
	private List fArguments;
	private ITypeBinding fSenderBinding;
	private boolean fIsInDifferentCU;
	
	public NewMethodCompletionProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode,  List arguments, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		
		fNode= invocationNode;
		fArguments= arguments;
		fSenderBinding= binding;
	}
		
	protected ASTRewrite getRewrite() throws CoreException {
		ASTRewrite rewrite;
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode typeDecl= astRoot.findDeclaringNode(fSenderBinding);
		ASTNode newTypeDecl= null;
		if (typeDecl != null) {
			fIsInDifferentCU= false;
			rewrite= new ASTRewrite(astRoot);
			newTypeDecl= typeDecl;
		} else {
			fIsInDifferentCU= true;
			CompilationUnit newRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			rewrite= new ASTRewrite(newRoot);
			
			newTypeDecl= newRoot.findDeclaringNode(fSenderBinding.getKey());
		}
		if (newTypeDecl != null) {
			List methods;
			if (fSenderBinding.isAnonymous()) {
				methods= ((AnonymousClassDeclaration) newTypeDecl).bodyDeclarations();
			} else {
				methods= ((TypeDeclaration) newTypeDecl).bodyDeclarations();
			}
			MethodDeclaration newStub= getStub(newTypeDecl.getAST());
			
			if (!fIsInDifferentCU) {
				methods.add(findInsertIndex(methods, fNode.getStartPosition()), newStub);
			} else if (isConstructor()) {
				methods.add(0, newStub);
			} else {
				methods.add(newStub);
			}
			rewrite.markAsInserted(newStub);
		}
		
		return rewrite;
	}
	
	private boolean isConstructor() {
		return fNode.getNodeType() != ASTNode.METHOD_INVOCATION && fNode.getNodeType() != ASTNode.SUPER_METHOD_INVOCATION;
	}
	
	private MethodDeclaration getStub(AST ast) throws CoreException {
		MethodDeclaration decl= ast.newMethodDeclaration();
		
		decl.setConstructor(isConstructor());
		decl.setModifiers(evaluateModifiers());
		decl.setName(ast.newSimpleName(getMethodName()));
		
		NameProposer nameProposer= new NameProposer();
		List arguments= fArguments;
		List params= decl.parameters();
		
		int nArguments= arguments.size();
		ArrayList names= new ArrayList(nArguments);
		for (int i= 0; i < arguments.size(); i++) {
			Expression elem= (Expression) arguments.get(i);
			SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
			Type type= evaluateParameterType(ast, elem);
			param.setType(type);
			param.setName(ast.newSimpleName(getParameterName(nameProposer, names, elem, type)));
			params.add(param);
		}
		
		Block body= null;
		if (!isConstructor()) {
			Type returnType= evaluateMethodType(ast);
			if (returnType == null) {
				decl.setReturnType(ast.newPrimitiveType(PrimitiveType.VOID));
				body= ast.newBlock();
			} else {
				decl.setReturnType(returnType);
				if (!fSenderBinding.isInterface()) {
					body= ast.newBlock();
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(ASTResolving.getInitExpression(returnType));
					body.statements().add(returnStatement);
				}
			}
		}
		decl.setBody(body);

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		if (settings.createComments && !fSenderBinding.isAnonymous()) {
			StringBuffer buf= new StringBuffer();
			String[] namesArray= (String[]) names.toArray(new String[names.size()]);
			String name= decl.getName().getIdentifier();
			if (isConstructor()) {
				StubUtility.genJavaDocStub("Constructor " + name, namesArray, null, null, buf);
			} else {
				String returnTypeSig= Signature.createTypeSignature(ASTNodes.asString(decl.getReturnType()), true);
				StubUtility.genJavaDocStub("Method " + name, namesArray, returnTypeSig, null, buf);
			}	
			Javadoc javadoc= ast.newJavadoc();
			javadoc.setComment(buf.toString());
			decl.setJavadoc(javadoc);
		}
		return decl;
	}
			
	private String getParameterName(NameProposer nameProposer, ArrayList takenNames, Expression argNode, Type type) {
		String name;
		if (argNode instanceof SimpleName) {
			name= ((SimpleName) argNode).getIdentifier();
		} else {
			name= nameProposer.proposeParameterName(ASTNodes.asString(type));
		}
		String base= name;
		int i= 1;
		while (takenNames.contains(name)) {
			name= base + i++;
		}
		takenNames.add(name);
		return name;
	}
		
	private int findInsertIndex(List decls, int currPos) {
		int nDecls= decls.size();
		for (int i= 0; i < nDecls; i++) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof MethodDeclaration && currPos < curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return nDecls;
	}
	
	private String getMethodName() {
		if (fNode instanceof MethodInvocation) {
			return ((MethodInvocation)fNode).getName().getIdentifier();
		} else if (fNode instanceof SuperMethodInvocation) {
			return ((SuperMethodInvocation)fNode).getName().getIdentifier();
		} else {
			return fSenderBinding.getName(); // name of the class
		}
	}
	
	private int evaluateModifiers() {
		if (fSenderBinding.isInterface()) {
			// copy the modifiers for interface members
			IMethodBinding[] methods= fSenderBinding.getDeclaredMethods();
			if (methods.length > 0) {
				return methods[0].getModifiers();
			}
			return 0;
		}
		if (fNode instanceof MethodInvocation) {
			int modifiers= 0;
			Expression expression= ((MethodInvocation)fNode).getExpression();
			if (expression != null) {
				if (expression instanceof Name && ((Name) expression).resolveBinding().getKind() == IBinding.TYPE) {
					modifiers |= Modifier.STATIC;
				}
			} else if (ASTResolving.isInStaticContext(fNode)) {
				modifiers |= Modifier.STATIC;
			}

			if (fIsInDifferentCU) {
				modifiers |= Modifier.PUBLIC;
			} else {
				modifiers |= Modifier.PRIVATE;
			}
			return modifiers;
		}
		return Modifier.PRIVATE;
		
	}
	
	private Type evaluateMethodType(AST ast) throws CoreException {
		ITypeBinding binding= ASTResolving.guessBindingForReference(fNode);
		if (binding != null) {
			addImport(binding);
			return ASTResolving.getTypeFromTypeBinding(ast, binding);
		}
		return null;
	}
	
	private Type evaluateParameterType(AST ast, Expression expr) throws CoreException {
		ITypeBinding binding= ASTResolving.getTypeBinding(expr.resolveTypeBinding());
		if (binding != null) {
			addImport(binding);
			return ASTResolving.getTypeFromTypeBinding(ast, binding);
		}
		return ast.newSimpleType(ast.newSimpleName("Object"));
	}
	
	public void apply(IDocument document) {
		try {
			CompilationUnitChange change= getCompilationUnitChange();
			
			IEditorPart part= null;
			if (fIsInDifferentCU) {
				change.setKeepExecutedTextEdits(true);
				part= EditorUtility.openInEditor(getCompilationUnit(), true);
			}
			super.apply(document);
		
			if (part instanceof ITextEditor) {
				TextRange range= change.getExecutedTextEdit(change.getEdit()).getTextRange();		
				((ITextEditor) part).selectAndReveal(range.getOffset(), range.getLength());
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}		
	}	
}
