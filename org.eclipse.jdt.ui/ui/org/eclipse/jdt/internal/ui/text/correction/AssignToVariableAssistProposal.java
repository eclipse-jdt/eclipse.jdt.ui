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

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;

public class AssignToVariableAssistProposal extends ASTRewriteCorrectionProposal implements ICompletionProposalExtension2 {

	public static final int LOCAL= 1;
	public static final int FIELD= 2;

	private int  fVariableKind;
	private ExpressionStatement fExpressionStatement;
	private ITypeBinding fTypeBinding;
	private IRegion fSelectedRegion;

	public AssignToVariableAssistProposal(ICompilationUnit cu, int variableKind, ExpressionStatement node, ITypeBinding typeBinding, int relevance) {
		super(null, cu, null, relevance, null);
	
		fVariableKind= variableKind;
		fExpressionStatement= node;
		fTypeBinding= typeBinding;
		if (variableKind == LOCAL) {
			setDisplayName(CorrectionMessages.getString("AssignToVariableAssistProposal.assigntolocal.description")); //$NON-NLS-1$
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL));
		} else {
			setDisplayName(CorrectionMessages.getString("AssignToVariableAssistProposal.assigntofield.description")); //$NON-NLS-1$
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#createCompilationUnitChange(java.lang.String, org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.internal.corext.textmanipulation.TextEdit)
	 */
	protected CompilationUnitChange createCompilationUnitChange(String name, ICompilationUnit cu, TextEdit rootEdit) throws CoreException {
		CompilationUnitChange change= super.createCompilationUnitChange(name, cu, rootEdit);
		change.setKeepExecutedTextEdits(true);
		return change;
	}
		
	protected ASTRewrite getRewrite() throws CoreException {
		if (fVariableKind == FIELD) {
			return doAddField();
		} else { // LOCAL
			return doAddLocal();
		}
	}

	private ASTRewrite doAddLocal() throws CoreException {
		Expression expression= fExpressionStatement.getExpression();
		ASTRewrite rewrite= new ASTRewrite(fExpressionStatement.getParent());
		AST ast= fExpressionStatement.getAST();

		String varName= suggestLocalVariableNames(fTypeBinding);

		SimpleName name= ast.newSimpleName(varName);
		rewrite.markAsTracked(name, "LINKED"); //$NON-NLS-1$
		
		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(name);
		newDeclFrag.setInitializer((Expression) rewrite.createCopy(expression));
		
		VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(newDeclFrag);
		String typeName= addImport(fTypeBinding);
		newDecl.setType(ASTNodeFactory.newType(ast, typeName));
		
		rewrite.markAsReplaced(fExpressionStatement, newDecl); //$NON-NLS-1$
		rewrite.markAsTracked(newDecl, "ENDPOS"); //$NON-NLS-1$
		return rewrite;
	}

	private ASTRewrite doAddField() throws CoreException {
		ASTNode newTypeDecl= ASTResolving.findParentType(fExpressionStatement);
		Expression expression= fExpressionStatement.getExpression();
		
		boolean isAnonymous= newTypeDecl.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION;
		List decls= isAnonymous ?  ((AnonymousClassDeclaration) newTypeDecl).bodyDeclarations() :  ((TypeDeclaration) newTypeDecl).bodyDeclarations();
		
		ASTRewrite rewrite= new ASTRewrite(newTypeDecl);
		AST ast= fExpressionStatement.getAST();
		
		String varName= suggestFieldNames(fTypeBinding);

		SimpleName name= ast.newSimpleName(varName);
		rewrite.markAsTracked(name, "LINKED-1"); //$NON-NLS-1$
		
		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(name);
				
		FieldDeclaration newDecl= ast.newFieldDeclaration(newDeclFrag);
		String typeName= addImport(fTypeBinding);
		newDecl.setType(ASTNodeFactory.newType(ast, typeName));
		newDecl.setModifiers(Modifier.PRIVATE);
		
		Assignment assignment= ast.newAssignment();
		assignment.setRightHandSide((Expression) rewrite.createCopy(expression));

		SimpleName accessName= ast.newSimpleName(varName);
		rewrite.markAsTracked(accessName, "LINKED"); //$NON-NLS-1$
		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEGEN_KEYWORD_THIS)) {
			FieldAccess fieldAccess= ast.newFieldAccess();
			fieldAccess.setName(accessName);
			fieldAccess.setExpression(ast.newThisExpression());
			assignment.setLeftHandSide(fieldAccess);
		} else {
			assignment.setLeftHandSide(accessName);
		}
		rewrite.markAsReplaced(expression, assignment); //$NON-NLS-1$
		rewrite.markAsTracked(fExpressionStatement, "ENDPOS"); //$NON-NLS-1$
		
		decls.add(findInsertIndex(decls, fExpressionStatement.getStartPosition()), newDecl);
		
		rewrite.markAsInserted(newDecl);
		return rewrite;		
	}
	
	private String suggestLocalVariableNames(ITypeBinding binding) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		ITypeBinding base= binding.isArray() ? binding.getElementType() : binding;
		IPackageBinding packBinding= base.getPackage();
		String packName= packBinding != null ? packBinding.getName() : ""; //$NON-NLS-1$
		
		String[] excludedNames= getUsedVariableNames();
		String typeName= base.getName();
		String[] names= NamingConventions.suggestLocalVariableNames(project, packName, typeName, binding.getDimensions(), excludedNames);
		if (names.length == 0) {
			return "class1"; // fix for pr, remoev after 20030127 //$NON-NLS-1$
		}
		return names[0]; 
	}
	
	private String suggestFieldNames(ITypeBinding binding) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		ITypeBinding base= binding.isArray() ? binding.getElementType() : binding;
		IPackageBinding packBinding= base.getPackage();
		String packName= packBinding != null ? packBinding.getName() : ""; //$NON-NLS-1$
		
		String[] excludedNames= getUsedVariableNames();
		String typeName= base.getName();
		String[] names= NamingConventions.suggestFieldNames(project, packName, typeName, binding.getDimensions(), binding.getModifiers(), excludedNames);
		if (names.length == 0) {
			return "class1"; // fix for pr, remoev after 20030127 //$NON-NLS-1$
		}
		return names[0];		
	}
	
	private String[] getUsedVariableNames() {
		CompilationUnit root= (CompilationUnit) fExpressionStatement.getRoot();
		IBinding[] bindings= (new ScopeAnalyzer(root)).getDeclarationsInScope(fExpressionStatement.getStartPosition(), ScopeAnalyzer.VARIABLES);
		String[] names= new String[bindings.length];
		for (int i= 0; i < names.length; i++) {
			names[i]= bindings[i].getName();
		}
		return names;
	}
		
	
	private int findInsertIndex(List decls, int currPos) {
		for (int i= decls.size() - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof FieldDeclaration && currPos > curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return 0;
	}
		
	/**
	 * Returns the variable kind.
	 * @return int
	 */
	public int getVariableKind() {
		return fVariableKind;
	}
	

	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		try {
			IDocument document= viewer.getDocument();

			super.apply(document);
			
			LinkedPositionManager manager= new LinkedPositionManager(document);
			LinkedPositionUI editor= new LinkedPositionUI(viewer, manager);
			
			CompilationUnitChange change= getCompilationUnitChange();
			GroupDescription[] descriptions= change.getGroupDescriptions();
			for (int i= 0; i < descriptions.length; i++) {
				GroupDescription curr= descriptions[i];
				String name= curr.getName();
				if (name.startsWith("LINKED")) { //$NON-NLS-1$
					TextRange range= change.getNewTextRange(curr.getTextEdits());
					manager.addPosition(range.getOffset(), range.getLength());
					if (name.equals("LINKED")) { //$NON-NLS-1$
						editor.setInitialOffset(range.getOffset());
					}
				} else if (name.equals("ENDPOS")) { //$NON-NLS-1$
					TextRange range= change.getNewTextRange(curr.getTextEdits());
					editor.setFinalCaretOffset(range.getExclusiveEnd());
				}
			}
			editor.enter();
			
			fSelectedRegion= editor.getSelectedRegion();
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
	}
		
	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer, boolean)
	 */
	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(org.eclipse.jface.text.ITextViewer)
	 */
	public void unselected(ITextViewer viewer) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 */
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return false;
	}	
	

}
