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

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewMethodCompletionProposal extends ASTRewriteCorrectionProposal {

	private MethodInvocation fNode;
	private ITypeBinding fSenderBinding;
	private boolean fIsLocalChange;
	
	public NewMethodCompletionProposal(String label, ICompilationUnit targetCU, MethodInvocation node, ITypeBinding binding, int relevance) throws CoreException {
		super(label, targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC));
		
		fNode= node;
		fSenderBinding= binding;
	}
		
	protected ASTRewrite getRewrite() {
		ASTRewrite rewrite;
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode typeDecl= astRoot.findDeclaringNode(fSenderBinding);
		ASTNode newTypeDecl= null;
		if (typeDecl != null) {
			fIsLocalChange= true;
			ASTCloner cloner= new ASTCloner(new AST(), astRoot);
			CompilationUnit newRoot= (CompilationUnit) cloner.getClonedRoot();
			rewrite= new ASTRewrite(newRoot);
			
			newTypeDecl= cloner.getCloned(typeDecl);
		} else {
			fIsLocalChange= false;
			CompilationUnit newRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			rewrite= new ASTRewrite(newRoot);
			
			newTypeDecl= ASTResolving.findTypeDeclaration(newRoot, fSenderBinding);
		}
		if (newTypeDecl != null) {
			List methods;
			if (fSenderBinding.isAnonymous()) {
				methods= ((AnonymousClassDeclaration) newTypeDecl).bodyDeclarations();
			} else {
				methods= ((TypeDeclaration) newTypeDecl).bodyDeclarations();
			}
			MethodDeclaration newStub= getStub(newTypeDecl.getAST());
			if (fIsLocalChange) {
				methods.add(findInsertIndex(methods, fNode.getStartPosition()), newStub);
			} else {
				methods.add(newStub);
			}
			rewrite.markAsInserted(newStub);
		}
		
		return rewrite;
	}


	private MethodDeclaration getStub(AST ast) {
		MethodDeclaration decl= ast.newMethodDeclaration();
		Type returnType= evaluateMethodType(ast);
		
		decl.setConstructor(false);
		decl.setModifiers(evaluateModifiers());
		decl.setReturnType(returnType);
		decl.setName(ast.newSimpleName(fNode.getName().getIdentifier()));
		
		NameProposer nameProposer= new NameProposer();
		List arguments= fNode.arguments();
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
		
		ReturnStatement returnStatement= ast.newReturnStatement();
		returnStatement.setExpression(ASTResolving.getInitExpression(returnType));
		
		Block body= ast.newBlock();
		body.statements().add(returnStatement);
		
		decl.setBody(body);

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		if (settings.createComments && !fSenderBinding.isAnonymous()) {
			StringBuffer buf= new StringBuffer();
			String[] namesArray= (String[]) names.toArray(new String[names.size()]);
			StubUtility.genJavaDocStub("Method " + fNode.getName().getIdentifier(), namesArray, Signature.createTypeSignature(ASTNodes.asString(returnType), true), null, buf);
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
		for (int i= decls.size() - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof MethodDeclaration && currPos > curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return 0;
	}
	
	private int evaluateModifiers() {
		int modifiers= 0;
		Expression expression= fNode.getExpression();
		if (expression != null) {
			if (expression instanceof Name && ((Name) expression).resolveBinding().getKind() == IBinding.TYPE) {
				modifiers |= Modifier.STATIC;
			}
		} else if (ASTResolving.isInStaticContext(fNode)) {
			modifiers |= Modifier.STATIC;
		}
		
		if (fIsLocalChange) {
			modifiers |= Modifier.PRIVATE;
		} else if (!fSenderBinding.isInterface()) {
			modifiers |= Modifier.PUBLIC;
		}
		return modifiers;
	}
	
	
	private Type evaluateMethodType(AST ast) {
		ITypeBinding binding= ASTResolving.getTypeBinding(fNode);
		if (binding != null) {
			ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
			if (!baseType.isPrimitive()) {
				addImport(Bindings.getFullyQualifiedName(baseType));
			}
			return ASTResolving.getTypeFromTypeBinding(ast, baseType);
		}
		return ast.newPrimitiveType(PrimitiveType.VOID);
	}
	
	private Type evaluateParameterType(AST ast, Expression expr) {
		ITypeBinding binding= expr.resolveTypeBinding();
		if (binding != null && !binding.isNullType()) {
			ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
			if (!baseType.isPrimitive()) {
				addImport(Bindings.getFullyQualifiedName(baseType));
			}
			return ASTResolving.getTypeFromTypeBinding(ast, baseType);
		}
		return ast.newSimpleType(ast.newSimpleName("Object"));
	}
	
	public void apply(IDocument document) {
		try {
			CompilationUnitChange change= getCompilationUnitChange();
			
			IEditorPart part= null;
			if (!fIsLocalChange) {
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
