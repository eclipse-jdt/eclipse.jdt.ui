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

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

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
			ASTParser astParser= ASTParser.newParser(ASTProvider.AST_LEVEL);
			astParser.setSource(getCompilationUnit());
			astParser.setResolveBindings(true);
			CompilationUnit newRoot= (CompilationUnit) astParser.createAST(null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			ASTRewrite rewrite= ASTRewrite.create(declNode.getAST());
			ListRewrite modifierRewrite= null;
			if (declNode instanceof MethodDeclaration) {
				modifierRewrite= rewrite.getListRewrite(declNode, MethodDeclaration.MODIFIERS2_PROPERTY);
			} else if (declNode instanceof VariableDeclarationFragment) {
				ASTNode parent= declNode.getParent();
				if (parent instanceof FieldDeclaration) {
					modifierRewrite= rewrite.getListRewrite(parent, FieldDeclaration.MODIFIERS2_PROPERTY);
				} else if (parent instanceof VariableDeclarationStatement) {
					modifierRewrite= rewrite.getListRewrite(parent, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
				} else if (parent instanceof VariableDeclarationExpression) {
					modifierRewrite= rewrite.getListRewrite(parent, VariableDeclarationExpression.MODIFIERS2_PROPERTY);
				}
			} else if (declNode instanceof SingleVariableDeclaration) {
				modifierRewrite= rewrite.getListRewrite(declNode, SingleVariableDeclaration.MODIFIERS2_PROPERTY);	
			} else if (declNode instanceof TypeDeclaration) {
				modifierRewrite= rewrite.getListRewrite(declNode, TypeDeclaration.MODIFIERS2_PROPERTY);	
			}
			if (modifierRewrite != null) {
				// remove modifiers
				List originalList= modifierRewrite.getOriginalList();
				for (int i= 0; i < originalList.size(); i++) {
					ASTNode curr= (ASTNode) originalList.get(i);
					if (curr instanceof Modifier && ((fExcludedModifiers & ((Modifier)curr).getKeyword().toFlagValue()) != 0)) {
						modifierRewrite.remove(curr, selectionDescription);
					}
				}
				// add modifiers
				List includedNodes= new ArrayList();
				int visibilityFlags= Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;	
				ASTNodeFactory.addModifiers(rewrite.getAST(), fIncludedModifiers, includedNodes);
				int firstPos= 0;
				for (int i= 0; i < includedNodes.size(); i++) {
					Modifier curr= (Modifier) includedNodes.get(i);
					if ((curr.getKeyword().toFlagValue() & visibilityFlags) != 0) {
						modifierRewrite.insertAt(curr, firstPos++, selectionDescription);
					} else {
						modifierRewrite.insertLast(curr, selectionDescription);
					}
				}
			}
			return rewrite;
		}
		return null;
	}
	
	
}
