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
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Resources;
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
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode boundNode= astRoot.findDeclaringNode(fBinding);
		ASTNode declNode= null;
		if (boundNode != null) {
			fIsInDifferentCU= false;
			declNode= boundNode;
		} else {
			fIsInDifferentCU= true;
			CompilationUnit newRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			ASTRewrite rewrite= new ASTRewrite(declNode.getParent());
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
				ASTNode parent= declNode.getParent();
				if (parent instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) parent;
					int newModifiers= (fieldDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
					
					FieldDeclaration modifiedNode= ast.newFieldDeclaration(ast.newVariableDeclarationFragment());
					modifiedNode.setModifiers(newModifiers);
					
					rewrite.markAsModified(fieldDecl, modifiedNode);					
				} else if (parent instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement varDecl= (VariableDeclarationStatement) parent;
					int newModifiers= (varDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
					
					VariableDeclarationStatement modifiedNode= ast.newVariableDeclarationStatement(ast.newVariableDeclarationFragment());
					modifiedNode.setModifiers(newModifiers);
					
					rewrite.markAsModified(varDecl, modifiedNode);
				} else if (parent instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression varDecl= (VariableDeclarationExpression) parent;
					int newModifiers= (varDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
					
					VariableDeclarationExpression modifiedNode= ast.newVariableDeclarationExpression(ast.newVariableDeclarationFragment());
					modifiedNode.setModifiers(newModifiers);
					
					rewrite.markAsModified(varDecl, modifiedNode);					
				}
			} else if (declNode instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration variableDeclaration= (SingleVariableDeclaration) declNode;
				int newModifiers= (variableDeclaration.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
				
				SingleVariableDeclaration modifiedNode= ast.newSingleVariableDeclaration();
				modifiedNode.setExtraDimensions(variableDeclaration.getExtraDimensions()); // no change
				modifiedNode.setModifiers(newModifiers);
				
				rewrite.markAsModified(variableDeclaration, modifiedNode);				
			} else if (declNode instanceof TypeDeclaration) {
				TypeDeclaration typeDecl= (TypeDeclaration) declNode;
				int newModifiers= (typeDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers;
				
				TypeDeclaration modifiedNode= ast.newTypeDeclaration();
				modifiedNode.setInterface(typeDecl.isInterface()); // no change
				modifiedNode.setModifiers(newModifiers);
				
				rewrite.markAsModified(typeDecl, modifiedNode);				
			}
			return rewrite;
		}
		return null;
	}
		
	public void apply(IDocument document) {
		try {
			CompilationUnitChange change= getCompilationUnitChange();
			
			IEditorPart part= null;
			if (fIsInDifferentCU) {
				ICompilationUnit unit= getCompilationUnit();
				IStatus status= Resources.makeCommittable(JavaModelUtil.toOriginal(unit).getResource(), null);
				if (!status.isOK()) {
					String label= CorrectionMessages.getString("ModifierChangeCompletionProposal.error.title"); //$NON-NLS-1$
					String message= CorrectionMessages.getString("ModifierChangeCompletionProposal.error.message"); //$NON-NLS-1$
					ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), label, message, status);
					return;
				}
				change.setKeepExecutedTextEdits(true);
				part= EditorUtility.openInEditor(unit, true);
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
