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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

public class ModifierChangeCompletionProposal extends ASTRewriteCorrectionProposal {

	private IBinding fBinding;
	private ASTNode fNode;
	private boolean fIsInDifferentCU;
	private int fIncludedModifiers;
	private int fExcludedModifiers;
	
	public ModifierChangeCompletionProposal(String label, ICompilationUnit targetCU, IBinding binding, ASTNode node, int includedModifiers, int excludedModifiers, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		fBinding= binding;
		fNode= node;
		fIncludedModifiers= includedModifiers;
		fExcludedModifiers= excludedModifiers;
	}
	
	protected ASTRewrite getRewrite() throws CoreException {
		ASTRewrite rewrite;
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode boundNode= astRoot.findDeclaringNode(fBinding);
		ASTNode declNode= null;
		if (boundNode != null) {
			fIsInDifferentCU= false;
			rewrite= new ASTRewrite(astRoot);
			declNode= boundNode;
		} else {
			fIsInDifferentCU= true;
			CompilationUnit newRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			rewrite= new ASTRewrite(newRoot);
			
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			AST ast= declNode.getAST();
			if (declNode instanceof MethodDeclaration) {
				MethodDeclaration methodDecl= (MethodDeclaration) declNode;
				int newModifiers= (methodDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
				
				MethodDeclaration modifiedNode= ast.newMethodDeclaration();
				modifiedNode.setConstructor(methodDecl.isConstructor()); // no change
				modifiedNode.setExtraDimensions(methodDecl.getExtraDimensions()); // no change
				modifiedNode.setModifiers(newModifiers);
				
				rewrite.markAsModified(methodDecl, modifiedNode);
			} else if (declNode instanceof VariableDeclarationFragment) {
				if (declNode.getParent() instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) declNode.getParent();
					int newModifiers= (fieldDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
					
					FieldDeclaration modifiedNode= ast.newFieldDeclaration(ast.newVariableDeclarationFragment());
					modifiedNode.setModifiers(newModifiers);
					
					rewrite.markAsModified(fieldDecl, modifiedNode);					
				}
			} else if (declNode instanceof TypeDeclaration) {
				TypeDeclaration typeDecl= (TypeDeclaration) declNode;
				int newModifiers= (typeDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
				
				TypeDeclaration modifiedNode= ast.newTypeDeclaration();
				modifiedNode.setInterface(typeDecl.isInterface()); // no change
				modifiedNode.setModifiers(newModifiers);
				
				rewrite.markAsModified(typeDecl, modifiedNode);				
			}
		}
		
		return rewrite;
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
