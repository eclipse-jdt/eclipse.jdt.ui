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

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 *
 */
public class NewDefiningMethodProposal extends AbstractMethodCompletionProposal {

	private static final String KEY_NAME= "name"; //$NON-NLS-1$
	private static final String KEY_TYPE= "type"; //$NON-NLS-1$
	
	private final IMethodBinding fMethod;
	private final String[] fParamNames;

	public NewDefiningMethodProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, ITypeBinding binding, IMethodBinding method, String[] paramNames, int relevance) {
		super(label,targetCU,invocationNode,binding,relevance,null);
		fMethod= method;
		fParamNames= paramNames;
		
		ImageDescriptor desc= JavaElementImageProvider.getMethodImageDescriptor(binding.isInterface(), method.getModifiers());
		setImage(JavaPlugin.getImageDescriptorRegistry().get(desc));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#isConstructor()
	 */
	protected boolean isConstructor() {
		return fMethod.isConstructor();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#addNewParameters(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, java.util.List, java.util.List)
	 */
	protected void addNewParameters(ASTRewrite rewrite, List takenNames, List params) throws CoreException {
		AST ast= rewrite.getAST();
		ImportRewrite importRewrite= getImportRewrite();
		ITypeBinding[] bindings= fMethod.getParameterTypes();
		
		IJavaProject project= getCompilationUnit().getJavaProject();
		String[] paramNames= StubUtility.suggestArgumentNames(project, fParamNames);
		
		for (int i= 0; i < bindings.length; i++) {
			ITypeBinding curr= bindings[i];
			
			SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();
			
			String typeName= importRewrite.addImport(curr);
			newParam.setType(ASTNodeFactory.newType(ast, typeName));
			newParam.setName(ast.newSimpleName(paramNames[i]));
			
			params.add(newParam);
			
			addLinkedPosition(rewrite.track(newParam.getType()), false, "arg_type_" + i); //$NON-NLS-1$
			addLinkedPosition(rewrite.track(newParam.getName()), false, "arg_name_" + i); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#getNewName()
	 */
	protected SimpleName getNewName(ASTRewrite rewrite) {
		AST ast= rewrite.getAST();
		SimpleName nameNode= ast.newSimpleName(fMethod.getName());
		addLinkedPosition(rewrite.track(nameNode), false, KEY_NAME);
		return nameNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#evaluateModifiers(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected int evaluateModifiers(ASTNode targetTypeDecl) {
		if (getSenderBinding().isInterface()) {
			return 0;
		} else {
			int modifiers= fMethod.getModifiers();
			if (Modifier.isPrivate(modifiers)) {
				modifiers |= Modifier.PROTECTED;
			}
			return modifiers & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.ABSTRACT | Modifier.STRICTFP);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#getNewMethodType(org.eclipse.jdt.core.dom.rewrite.ASTRewrite)
	 */
	protected Type getNewMethodType(ASTRewrite rewrite) throws CoreException {
		String typeName= getImportRewrite().addImport(fMethod.getReturnType());
		Type newTypeNode= ASTNodeFactory.newType(rewrite.getAST(), typeName);
		
		addLinkedPosition(rewrite.track(newTypeNode), false, KEY_TYPE);
		return newTypeNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#addNewExceptions(org.eclipse.jdt.core.dom.AST, java.util.List)
	 */
	protected void addNewExceptions(ASTRewrite rewrite, List exceptions) throws CoreException {
		AST ast= rewrite.getAST();
		ImportRewrite importRewrite= getImportRewrite();
		ITypeBinding[] bindings= fMethod.getExceptionTypes();
		for (int i= 0; i < bindings.length; i++) {
			String typeName= importRewrite.addImport(bindings[i]);
			Name newNode= ASTNodeFactory.newName(ast, typeName);
			exceptions.add(newNode);
			
			addLinkedPosition(rewrite.track(newNode), false, "exc_type_" + i); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractMethodCompletionProposal#addNewTypeParameters(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, java.util.List, java.util.List)
	 */
	protected void addNewTypeParameters(ASTRewrite rewrite, List takenNames, List params) throws CoreException {

	}

}
