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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;

public class ChangeMethodSignatureProposal extends ASTRewriteCorrectionProposal {
	
	public static interface ChangeDescription {
	}
	
	public static class SwapDescription implements ChangeDescription {
		int index;
		public SwapDescription(int index) {
			this.index= index;
		}
	}
	
	public static class RemoveDescription implements ChangeDescription {
	}
	
	public static class EditDescription implements ChangeDescription {
		String name;
		ITypeBinding type;
		public EditDescription(ITypeBinding type, String name) {
			this.type= type;
			this.name= name;
		}
	}
	
	public static class InsertDescription implements ChangeDescription {
		String name;
		ITypeBinding type;
		public InsertDescription(ITypeBinding type, String name) {
			this.type= type;
			this.name= name;
		}
	}	
		
	private CompilationUnit fRoot;
	private IMethodBinding fSenderBinding;
	private ChangeDescription[] fParameterChanges;
		
	public ChangeMethodSignatureProposal(String label, ICompilationUnit targetCU, CompilationUnit root, IMethodBinding binding, ChangeDescription[] changes, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		
		fRoot= root;
		fSenderBinding= binding;
		fParameterChanges= changes;
	}
	
	protected ASTRewrite getRewrite() throws CoreException {
		ASTNode methodDecl= fRoot.findDeclaringNode(fSenderBinding);
		ASTNode newMethodDecl= null;
		CompilationUnit astRoot= fRoot;
		boolean isInDifferentCU;
		if (methodDecl != null) {
			isInDifferentCU= false;
			newMethodDecl= methodDecl;
		} else {
			isInDifferentCU= true;
			astRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			newMethodDecl= astRoot.findDeclaringNode(fSenderBinding.getKey());
		}
		if (newMethodDecl instanceof MethodDeclaration) {
			ASTRewrite rewrite= new ASTRewrite(newMethodDecl);
			modifySignature(rewrite, (MethodDeclaration) newMethodDecl, isInDifferentCU);
			return rewrite;
		}
		return null;
	}
	
	private final String NAME_SUGGESTION= "name_suggestion"; //$NON-NLS-1$
	
	protected void modifySignature(ASTRewrite rewrite, MethodDeclaration methodDecl, boolean isInDifferentCU) throws CoreException {
		String rewriteDesc= isInDifferentCU ? SELECTION_GROUP_DESC : null;
		List parameters= methodDecl.parameters();
		// create a copy to not loose the indexes
		SingleVariableDeclaration[] oldParameters= (SingleVariableDeclaration[]) parameters.toArray(new SingleVariableDeclaration[parameters.size()]);
		AST ast= methodDecl.getAST();
		int k= 0; // index over the oldParameters
		ArrayList createdVariables= new ArrayList();
		ArrayList usedNames= new ArrayList();
		
		for (int i= 0; i < fParameterChanges.length; i++) {
			ChangeDescription curr= fParameterChanges[i];
			if (curr == null) {
				usedNames.add(oldParameters[k].getName().getIdentifier());
				k++;
			} else if (curr instanceof InsertDescription) {
				InsertDescription desc= (InsertDescription) curr;
				SingleVariableDeclaration newNode= ast.newSingleVariableDeclaration();
				String type= addImport(desc.type);
				newNode.setType(ASTNodeFactory.newType(ast, type));
				
				// set name later
				newNode.setProperty(NAME_SUGGESTION, desc.name);

				createdVariables.add(newNode);
				
				rewrite.markAsInserted(newNode, rewriteDesc);
				parameters.add(i, newNode);
			} else if (curr instanceof RemoveDescription) {
				rewrite.markAsRemoved(oldParameters[k], rewriteDesc);
				k++;
			} else if (curr instanceof EditDescription) {
				EditDescription desc= (EditDescription) curr;

				SingleVariableDeclaration newNode= ast.newSingleVariableDeclaration();
				String type= addImport(desc.type);
				newNode.setType(ASTNodeFactory.newType(ast, type));
				//	set name later
				newNode.setProperty(NAME_SUGGESTION, desc.name);
				
				rewrite.markAsReplaced(oldParameters[k], newNode);
				createdVariables.add(newNode);
				k++;
			} else if (curr instanceof SwapDescription) {
				SingleVariableDeclaration decl1= oldParameters[k];
				SingleVariableDeclaration decl2= oldParameters[((SwapDescription) curr).index];
				
				rewrite.markAsReplaced(decl1, rewrite.createCopy(decl2), rewriteDesc);
				rewrite.markAsReplaced(decl2, rewrite.createCopy(decl1), rewriteDesc);
				
				usedNames.add(decl1.getName().getIdentifier());
				k++;	
			}
		}
		
		if (!createdVariables.isEmpty()) {
			// avoid take a name of a local variable inside
			CompilationUnit root= (CompilationUnit) methodDecl.getRoot();
			IBinding[] bindings= (new ScopeAnalyzer(root)).getDeclarationsAfter(methodDecl.getBody().getStartPosition(), ScopeAnalyzer.VARIABLES);
			for (int i= 0; i < bindings.length; i++) {
				usedNames.add(bindings[i].getName());
			}
		}
		
		// set names for new parameters
		for (int i= 0; i < createdVariables.size(); i++) {
			SingleVariableDeclaration var= (SingleVariableDeclaration) createdVariables.get(i);
			String suggestedName= (String) var.getProperty(NAME_SUGGESTION);
			int dim= 0;
			if (suggestedName == null) {
				Type type= var.getType();
				if (type.isArrayType()) {
					dim= ((ArrayType) type).getDimensions();
					type= ((ArrayType) type).getElementType();
				}
				suggestedName= ASTNodes.asString(type);
			}
			String[] excludedNames= (String[]) usedNames.toArray(new String[usedNames.size()]);
			
			String name= NamingConventions.suggestArgumentNames(getCompilationUnit().getJavaProject(), "", suggestedName, dim, excludedNames)[0]; //$NON-NLS-1$
			var.setName(ast.newSimpleName(name));
			usedNames.add(name);
		}
	
		
	}
}
