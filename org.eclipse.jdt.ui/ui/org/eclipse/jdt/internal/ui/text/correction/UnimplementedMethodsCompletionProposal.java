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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class UnimplementedMethodsCompletionProposal extends ASTRewriteCorrectionProposal {

	private ASTNode fTypeNode;

	public UnimplementedMethodsCompletionProposal(ICompilationUnit cu, ASTNode typeNode, int relevance) {
		super(null, cu, null, relevance, null);
		setDisplayName(CorrectionMessages.getString("UnimplementedMethodsCompletionProposal.description"));//$NON-NLS-1$
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		
		fTypeNode= typeNode;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		ITypeBinding binding;
		List bodyDecls;
		if (fTypeNode instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration decl= (AnonymousClassDeclaration) fTypeNode;
			binding= decl.resolveBinding();
			bodyDecls= decl.bodyDeclarations();
		} else {
			TypeDeclaration decl= (TypeDeclaration) fTypeNode;
			binding= decl.resolveBinding();
			bodyDecls= decl.bodyDeclarations();
		}
		IMethodBinding[] methods= evalUnimplementedMethods(binding);
		
		ASTRewrite rewrite= new ASTRewrite(fTypeNode);
		AST ast= fTypeNode.getAST();
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		if (!settings.createComments || binding.isAnonymous()) {
			settings= null;
		}
		
		for (int i= 0; i < methods.length; i++) {
			MethodDeclaration newMethodDecl= createNewMethodDeclaration(ast, methods[i], rewrite, settings);
			rewrite.markAsInserted(newMethodDecl);
			bodyDecls.add(newMethodDecl);
		}
		return rewrite;
	}
	
	private MethodDeclaration createNewMethodDeclaration(AST ast, IMethodBinding binding, ASTRewrite rewrite, CodeGenerationSettings commentSettings) throws CoreException {
		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.setModifiers(binding.getModifiers() & ~Modifier.ABSTRACT);
		decl.setName(ast.newSimpleName(binding.getName()));
		
		ITypeBinding returnTypeBinding= binding.getReturnType();
		addImport(returnTypeBinding);
		
		decl.setReturnType(ASTResolving.getTypeFromTypeBinding(ast, returnTypeBinding));
		
		List parameters= decl.parameters();
		ITypeBinding[] params= binding.getParameterTypes();
		for (int i= 0; i < params.length; i++) {
			ITypeBinding curr= params[i];
			addImport(curr);		
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			var.setType(ASTResolving.getTypeFromTypeBinding(ast, curr));
			var.setName(ast.newSimpleName("arg" + i));
			parameters.add(var);
		}		
		
		List thrownExceptions= decl.thrownExceptions();
		ITypeBinding[] excTypes= binding.getExceptionTypes();
		for (int i= 0; i < excTypes.length; i++) {
			ITypeBinding curr= excTypes[i];
			addImport(curr);
			thrownExceptions.add(ast.newSimpleName(curr.getName())); // can only be singe type, no array
		}
		
		Block body= ast.newBlock();
		decl.setBody(body);
		
		Expression expression= ASTResolving.getInitExpression(decl.getReturnType());
		if (expression != null) {
			ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(expression);
			body.statements().add(returnStatement);
		}
		
		if (commentSettings != null) {
			StringBuffer buf= new StringBuffer();
			String fullTypeName= Bindings.getFullyQualifiedName(binding.getDeclaringClass());
			String[] fullParamNames= new String[params.length];
			for (int i= 0; i < fullParamNames.length; i++) {
				fullParamNames[i]= Bindings.getFullyQualifiedName(params[i]);
			}
			StubUtility.genJavaDocSeeTag(fullTypeName, binding.getName(), fullParamNames, commentSettings.createNonJavadocComments, binding.isDeprecated(), buf);
			Javadoc javadoc;
			if (commentSettings.createNonJavadocComments) {
				String str= buf.substring(0, buf.length() -  1); // cut away last line delim
				javadoc= (Javadoc) rewrite.createPlaceholder(str, ASTRewrite.JAVADOC);
			} else {
				javadoc= ast.newJavadoc();
				javadoc.setComment(buf.toString());
			}
			decl.setJavadoc(javadoc);
		}
		
		return decl;
	}
	
		
	private void findUnimplementedInterfaceMethods(ITypeBinding typeBinding, ArrayList visited, ArrayList allMethods, ArrayList toImplement) {
		visited.add(typeBinding);
		IMethodBinding[] typeMethods= typeBinding.getDeclaredMethods();
		for (int i= 0; i < typeMethods.length; i++) {
			IMethodBinding curr= typeMethods[i];
			int modifiers= curr.getModifiers();
			if (!Modifier.isStatic(modifiers)) {
				IMethodBinding impl= findMethod(curr, allMethods);
				if (impl == null || ((curr.getExceptionTypes().length < impl.getExceptionTypes().length) && !Modifier.isFinal(impl.getModifiers()))) {
					if (impl != null) {
						allMethods.remove(impl);
					}
					// implement an interface method when it does not exist in the hierarchy
					// or when it throws less exceptions that the implemented
					toImplement.add(curr);
					allMethods.add(curr);
				}
			}
		}
		ITypeBinding[] superInterfaces= typeBinding.getInterfaces();
		for (int i= 0; i < superInterfaces.length; i++) {
			if (!visited.contains(superInterfaces[i])) {
				findUnimplementedInterfaceMethods(superInterfaces[i], visited, allMethods, toImplement);
			}
		}
	}

	private IMethodBinding[] evalUnimplementedMethods(ITypeBinding typeBinding) {
		ArrayList allMethods= new ArrayList();
		ArrayList toImplement= new ArrayList();
		
		IMethodBinding[] typeMethods= typeBinding.getDeclaredMethods();
		for (int i= 0; i < typeMethods.length; i++) {
			IMethodBinding curr= typeMethods[i];
			int modifiers= curr.getModifiers();
			if (!curr.isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
				allMethods.add(curr);
			}
		}

		ITypeBinding superClass= typeBinding.getSuperclass();
		while (superClass != null) {
			typeMethods= superClass.getDeclaredMethods();
			for (int i= 0; i < typeMethods.length; i++) {
				IMethodBinding curr= typeMethods[i];
				int modifiers= curr.getModifiers();
				if (!curr.isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
					if (findMethod(curr, allMethods) == null) {
						allMethods.add(curr);
					}
				}
			}
			superClass= superClass.getSuperclass();
		}	

		for (int i= 0; i < allMethods.size(); i++) {
			IMethodBinding curr= (IMethodBinding) allMethods.get(i);
			int modifiers= curr.getModifiers();
			if ((Modifier.isAbstract(modifiers) || curr.getDeclaringClass().isInterface()) && (typeBinding != curr.getDeclaringClass())) {
				// implement all abstract methods
				toImplement.add(curr);
			}
		}

		ArrayList visited= new ArrayList();
		ITypeBinding[] superInterfaces= typeBinding.getInterfaces();
		for (int i= 0; i < superInterfaces.length; i++) {
			findUnimplementedInterfaceMethods(superInterfaces[i], visited, allMethods, toImplement);
		}
		
		return (IMethodBinding[]) toImplement.toArray(new IMethodBinding[toImplement.size()]);
	}
		
	private IMethodBinding findMethod(IMethodBinding method, ArrayList allMethods) {
		for (int i= 0; i < allMethods.size(); i++) {
			IMethodBinding curr= (IMethodBinding) allMethods.get(i);
			if (Bindings.isEqualMethod(method, curr.getName(), curr.getParameterTypes())) {
				return curr;
			}
		}
		return null;
	}
	

}
