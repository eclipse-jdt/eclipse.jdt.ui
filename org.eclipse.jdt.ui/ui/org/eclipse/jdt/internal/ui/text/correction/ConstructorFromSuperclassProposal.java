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

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class ConstructorFromSuperclassProposal extends ASTRewriteCorrectionProposal {

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
		return CorrectionMessages.getFormattedString("ConstructorFromSuperclassProposal.description", buf.toString());
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
		rewrite.markAsInserted(newMethodDecl);
		fTypeNode.bodyDeclarations().add(0, newMethodDecl);

		return rewrite;
	}
	
	private MethodDeclaration createNewMethodDeclaration(AST ast, IMethodBinding binding, ASTRewrite rewrite, CodeGenerationSettings commentSettings) throws CoreException {
		String name= fTypeNode.getName().getIdentifier();
		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.setConstructor(true);
		decl.setName(ast.newSimpleName(name));
		Block body= ast.newBlock();
		decl.setBody(body);		
	
		String bodyStatement= "";
		if (binding == null) {
			decl.setModifiers(Modifier.PUBLIC);
			if (commentSettings != null) {
				StringBuffer buf= new StringBuffer();
				StubUtility.genJavaDocStub("Constructor", new String[0], null, new String[0], buf);
				Javadoc javadoc= ast.newJavadoc();
				javadoc.setComment(buf.toString());
				decl.setJavadoc(javadoc);
			}
		} else {
			decl.setModifiers(binding.getModifiers());
		
			List parameters= decl.parameters();
			ITypeBinding[] params= binding.getParameterTypes();
			String[] paramNames= getArgumentNames(binding);
			for (int i= 0; i < params.length; i++) {
				ITypeBinding curr= params[i];
				addImport(curr);		
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setType(ASTResolving.getTypeFromTypeBinding(ast, curr));
				var.setName(ast.newSimpleName(paramNames[i]));
				parameters.add(var);
			}		
			
			List thrownExceptions= decl.thrownExceptions();
			ITypeBinding[] excTypes= binding.getExceptionTypes();
			for (int i= 0; i < excTypes.length; i++) {
				ITypeBinding curr= excTypes[i];
				addImport(curr);
				thrownExceptions.add(ast.newSimpleName(curr.getName())); // can only be single type, no array
			}
		
			SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
			List arguments= invocation.arguments();
			for (int i= 0; i < paramNames.length; i++) {
				arguments.add(ast.newSimpleName(paramNames[i]));
			}
			bodyStatement= ASTNodes.asFormattedString(invocation, 0, String.valueOf('\n'));
		
			if (commentSettings != null) {
				StringBuffer buf= new StringBuffer();
				String[] excTypeRefs= new String[excTypes.length];
				for (int i= 0; i < excTypeRefs.length; i++) {
					excTypeRefs[i]= Signature.createTypeSignature(excTypes[i].getName(), false);
				}
				StubUtility.genJavaDocStub("Constructor", paramNames, null, excTypeRefs, buf);
				String comment= buf.toString();
				Javadoc javadoc= ast.newJavadoc();
				javadoc.setComment(comment);
				decl.setJavadoc(javadoc);
			}
		}
		String placeHolder= StubUtility.getBodyStubTemplate(false, getCompilationUnit().getJavaProject(), name, name, bodyStatement); 	
		if (placeHolder != null) {
			ASTNode todoNode= rewrite.createPlaceholder(placeHolder, ASTRewrite.STATEMENT);
			body.statements().add(todoNode);
		}			
		
		
		return decl;
	}


	private String[] getArgumentNames(IMethodBinding binding) {
		int nParams= binding.getParameterTypes().length;
		if (nParams > 0) {
			try {
				IMethod method= Binding2JavaModel.find(binding, getCompilationUnit().getJavaProject());
				if (method != null) {
					return method.getParameterNames();
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}			
		String[] names= new String[nParams];
		for (int i= 0; i < names.length; i++) {
			names[i]= "arg" + i;
		}
		return names;
	}
}
