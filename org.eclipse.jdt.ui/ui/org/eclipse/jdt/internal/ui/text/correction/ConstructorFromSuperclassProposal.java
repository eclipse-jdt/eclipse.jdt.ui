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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NewASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class ConstructorFromSuperclassProposal extends LinkedCorrectionProposal {

	private TypeDeclaration fTypeNode;
	private IMethodBinding fSuperConstructor;

	public ConstructorFromSuperclassProposal(ICompilationUnit cu, TypeDeclaration typeNode, IMethodBinding superConstructor, int relevance) {
		super(null, cu, null, relevance, null);
		fTypeNode= typeNode;
		fSuperConstructor= superConstructor;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPlugin.getImageDescriptorRegistry().get(
			new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE)
		);
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		StringBuffer buf= new StringBuffer();
		buf.append(fTypeNode.getName().getIdentifier());
		buf.append('(');
		if (fSuperConstructor != null) {
			ITypeBinding[] paramTypes= fSuperConstructor.getParameterTypes();
			for (int i= 0; i < paramTypes.length; i++) {
				if (i > 0) {
					buf.append(',');
				}
				buf.append(paramTypes[i].getName());
			}			
		}
		buf.append(')');	
		return CorrectionMessages.getFormattedString("ConstructorFromSuperclassProposal.description", buf.toString()); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		ASTRewrite rewrite= new ASTRewrite(fTypeNode);
		AST ast= fTypeNode.getAST();
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		if (!settings.createComments) {
			settings= null;
		}
		
		MethodDeclaration newMethodDecl= createNewMethodDeclaration(ast, fSuperConstructor, rewrite, settings);
		rewrite.getListRewrite(fTypeNode, ASTNodeConstants.BODY_DECLARATIONS).insertFirst(newMethodDecl, null);
		
		addLinkedRanges(rewrite, newMethodDecl);
		
		return rewrite;
	}
	
	private void addLinkedRanges(ASTRewrite rewrite, MethodDeclaration newStub) {
		List parameters= newStub.parameters();
		for (int i= 0; i < parameters.size(); i++) {
			SingleVariableDeclaration curr= (SingleVariableDeclaration) parameters.get(i);
			String name= curr.getName().getIdentifier();
			markAsLinked(rewrite, curr.getType(), false, "arg_type_" + name); //$NON-NLS-1$
			markAsLinked(rewrite, curr.getName(), false, "arg_name_" + name); //$NON-NLS-1$
		}
	}	
	
	private MethodDeclaration createNewMethodDeclaration(AST ast, IMethodBinding binding, ASTRewrite rewrite, CodeGenerationSettings commentSettings) throws CoreException {
		String name= fTypeNode.getName().getIdentifier();
		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.setConstructor(true);
		decl.setName(ast.newSimpleName(name));
		Block body= ast.newBlock();
		decl.setBody(body);
		
		SuperConstructorInvocation invocation= null;
		
		List parameters= decl.parameters();
		String[] paramNames= getArgumentNames(binding);
		
		ITypeBinding enclosingInstance= getEnclosingInstance();
		if (enclosingInstance != null) {
			invocation= addEnclosingInstanceAccess(rewrite, parameters, paramNames, enclosingInstance);
		}

		if (binding == null) {
			decl.setModifiers(Modifier.PUBLIC);
		} else {
			decl.setModifiers(binding.getModifiers());
		
			ITypeBinding[] params= binding.getParameterTypes();
			for (int i= 0; i < params.length; i++) {
				String paramTypeName= getImportRewrite().addImport(params[i]);
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setType(ASTNodeFactory.newType(ast, paramTypeName));				
				var.setName(ast.newSimpleName(paramNames[i]));
				parameters.add(var);
			}		
			
			List thrownExceptions= decl.thrownExceptions();
			ITypeBinding[] excTypes= binding.getExceptionTypes();
			for (int i= 0; i < excTypes.length; i++) {
				String excTypeName= getImportRewrite().addImport(excTypes[i]);
				thrownExceptions.add(ASTNodeFactory.newName(ast, excTypeName));
			}
		
			if (invocation == null) {
				invocation= ast.newSuperConstructorInvocation();
			}
			
			List arguments= invocation.arguments();
			for (int i= 0; i < paramNames.length; i++) {
				Name argument= ast.newSimpleName(paramNames[i]);
				arguments.add(argument);
				markAsLinked(rewrite, argument, false, "arg_name_" + paramNames[i]); //$NON-NLS-1$
			}
		}
		
		String bodyStatement= (invocation == null) ? "" : ASTNodes.asFormattedString(invocation, 0, String.valueOf('\n')); //$NON-NLS-1$
		String placeHolder= CodeGeneration.getMethodBodyContent(getCompilationUnit(), name, name, true, bodyStatement, String.valueOf('\n')); 	
		if (placeHolder != null) {
			ASTNode todoNode= rewrite.createPlaceholder(placeHolder, NewASTRewrite.STATEMENT);
			body.statements().add(todoNode);
		}
		if (commentSettings != null) {
			String string= CodeGeneration.getMethodComment(getCompilationUnit(), name, decl, null, String.valueOf('\n'));
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createPlaceholder(string, NewASTRewrite.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}
	
	private SuperConstructorInvocation addEnclosingInstanceAccess(ASTRewrite rewrite, List parameters, String[] paramNames, ITypeBinding enclosingInstance) throws CoreException {
		AST ast= rewrite.getAST();
		SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
		
		String paramTypeName= getImportRewrite().addImport(enclosingInstance);
		
		SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
		var.setType(ASTNodeFactory.newType(ast, paramTypeName));
		String[] enclosingArgNames= StubUtility.getArgumentNameSuggestions(getCompilationUnit().getJavaProject(), enclosingInstance.getName(), paramNames);
		String firstName= enclosingArgNames[0];
		var.setName(ast.newSimpleName(firstName));
		parameters.add(var);
		
		Name enclosing= ast.newSimpleName(firstName);
		invocation.setExpression(enclosing);
		
		String key= "arg_name_" + firstName; //$NON-NLS-1$
		markAsLinked(rewrite, enclosing, false, key);
		for (int i= 0; i < enclosingArgNames.length; i++) {
			addLinkedModeProposal(key, enclosingArgNames[i]); // alternative names
		}
		return invocation;
	}

	private ITypeBinding getEnclosingInstance() {
		ITypeBinding currBinding= fTypeNode.resolveBinding();
		if (currBinding == null || Modifier.isStatic(currBinding.getModifiers())) {
			return null;
		}
		ITypeBinding superBinding= currBinding.getSuperclass();
		if (superBinding == null || superBinding.getDeclaringClass() == null || Modifier.isStatic(superBinding.getModifiers())) {
			return null;
		}
		ITypeBinding enclosing= superBinding.getDeclaringClass();
		
		while (currBinding != null) {
			if (Bindings.isSuperType(enclosing, currBinding)) {
				return null; // enclosing in scope
			}
			if (Modifier.isStatic(currBinding.getModifiers())) {
				return null; // no more enclosing instances
			}
			currBinding= currBinding.getDeclaringClass();
		}
		return enclosing;
	}


	private String[] getArgumentNames(IMethodBinding binding) {
		if (binding == null) {
			return new String[0];
		}
		
		int nParams= binding.getParameterTypes().length;
		if (nParams > 0) {
			try {
				IJavaProject project= getCompilationUnit().getJavaProject();
				IMethod method= Bindings.findMethod(binding, project);
				if (method != null) {
					return StubUtility.suggestArgumentNames(project, method.getParameterNames());
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}			
		String[] names= new String[nParams];
		for (int i= 0; i < names.length; i++) {
			names[i]= "arg" + i; //$NON-NLS-1$
		}
		return names;
	}
}
