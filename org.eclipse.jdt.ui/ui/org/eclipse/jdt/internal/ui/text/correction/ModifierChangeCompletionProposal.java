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

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class ModifierChangeCompletionProposal extends LinkedCorrectionProposal {

	private IBinding fBinding;
	private ASTNode fNode;
	private int fIncludedModifiers;
	private int fExcludedModifiers;
	
	public ModifierChangeCompletionProposal(String label, ICompilationUnit targetCU, IBinding binding, ASTNode node, int includedModifiers, int excludedModifiers, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		fBinding= binding;
		fNode= node;
		fIncludedModifiers= includedModifiers;
		fExcludedModifiers= excludedModifiers;
	}
	
	protected ASTRewrite getRewrite() {
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode boundNode= astRoot.findDeclaringNode(fBinding);
		ASTNode declNode= null;
		
		TextEditGroup selectionDescription= null;
		
		if (boundNode != null) {
			declNode= boundNode; // is same CU
		} else {
			selectionDescription= new TextEditGroup("selection"); // in different CU, needs selection //$NON-NLS-1$
			//setSelectionDescription(selectionDescription);
			ASTParser astParser= ASTParser.newParser(AST.JLS2);
			astParser.setSource(getCompilationUnit());
			astParser.setResolveBindings(true);
			CompilationUnit newRoot= (CompilationUnit) astParser.createAST(null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			ASTRewrite rewrite= ASTRewrite.create(declNode.getAST());
			if (declNode instanceof MethodDeclaration) {
				MethodDeclaration methodDecl= (MethodDeclaration) declNode;
				int newModifiers= (methodDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
				
				rewrite.set(methodDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), selectionDescription);
			} else if (declNode instanceof VariableDeclarationFragment) {
				ASTNode parent= declNode.getParent();
				if (parent instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) parent;
					int newModifiers= (fieldDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
				
					rewrite.set(fieldDecl, FieldDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), selectionDescription);	
				} else if (parent instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement varDecl= (VariableDeclarationStatement) parent;
					int newModifiers= (varDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
					
					rewrite.set(varDecl, VariableDeclarationStatement.MODIFIERS_PROPERTY, new Integer(newModifiers), selectionDescription);	
				} else if (parent instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression varDecl= (VariableDeclarationExpression) parent;
					int newModifiers= (varDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
					
					rewrite.set(varDecl, VariableDeclarationExpression.MODIFIERS_PROPERTY, new Integer(newModifiers), selectionDescription);
				}
			} else if (declNode instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration variableDeclaration= (SingleVariableDeclaration) declNode;
				int newModifiers= (variableDeclaration.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
				
				rewrite.set(variableDeclaration, SingleVariableDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), selectionDescription);
				
			} else if (declNode instanceof TypeDeclaration) {
				TypeDeclaration typeDecl= (TypeDeclaration) declNode;
				int newModifiers= (typeDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
				
				rewrite.set(typeDecl, TypeDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), selectionDescription);
			}
			return rewrite;
		}
		return null;
	}
	
	
}
