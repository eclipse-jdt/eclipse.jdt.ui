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
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class AssignToVariableAssistProposal extends ASTRewriteCorrectionProposal {

	public static final int LOCAL= 1;
	public static final int FIELD= 2;

	private int  fVariableKind;
	private ExpressionStatement fExpressionStatement;
	private ITypeBinding fTypeBinding;

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

		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(ast.newSimpleName(varName));
		newDeclFrag.setInitializer((Expression) rewrite.createCopy(expression));
		
		VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(newDeclFrag);
		String typeName= addImport(fTypeBinding);
		newDecl.setType(ASTNodeFactory.newType(ast, typeName));
		
		rewrite.markAsReplaced(fExpressionStatement, newDecl, "ID"); //$NON-NLS-1$
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

		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(ast.newSimpleName(varName));
		
		FieldDeclaration newDecl= ast.newFieldDeclaration(newDeclFrag);
		String typeName= addImport(fTypeBinding);
		newDecl.setType(ASTNodeFactory.newType(ast, typeName));
		newDecl.setModifiers(Modifier.PRIVATE);
		
		Assignment assignment= ast.newAssignment();
		assignment.setLeftHandSide(ast.newSimpleName(varName));
		assignment.setRightHandSide((Expression) rewrite.createCopy(expression));

		rewrite.markAsReplaced(expression, assignment, "ID"); //$NON-NLS-1$
		
		decls.add(findInsertIndex(decls, fExpressionStatement.getStartPosition()), newDecl);
		
		rewrite.markAsInserted(newDecl);
		return rewrite;		
	}
	
	private String suggestLocalVariableNames(ITypeBinding binding) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		ITypeBinding base= binding.isArray() ? binding.getElementType() : binding;
		IPackageBinding packBinding= base.getPackage();
		String packName= packBinding != null ? packBinding.getName() : ""; //$NON-NLS-1$
		
		String[] excludedNames= new String[0];
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
		
		String[] excludedNames= new String[0];
		String typeName= base.getName();
		String[] names= NamingConventions.suggestFieldNames(project, packName, typeName, binding.getDimensions(), binding.getModifiers(), excludedNames);
		if (names.length == 0) {
			return "class1"; // fix for pr, remoev after 20030127 //$NON-NLS-1$
		}
		return names[0];		
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
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
	 */
	public Point getSelection(IDocument document) {
		try {
			CompilationUnitChange change= getCompilationUnitChange();
			GroupDescription[] desc= change.getGroupDescriptions();
			TextRange range= change.getNewTextRange(desc[0].getTextEdits());
			
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(document.get(range.getOffset(), range.getLength()).toCharArray());
			Point res= new Point(0, 0);
			int tok= scanner.getNextToken();
			while (tok != ITerminalSymbols.TokenNameEOF) {
				res.x= scanner.getCurrentTokenStartPosition() + range.getOffset();
				res.y= scanner.getCurrentTokenEndPosition() - scanner.getCurrentTokenStartPosition() + 1;
				tok= scanner.getNextToken();
				if (tok == ITerminalSymbols.TokenNameEQUAL) {
					return res;
				}
			}
		} catch (CoreException e) {
			// can't happen (change already exists at this point)
		} catch (InvalidInputException e) {
			// ignore
		} catch (BadLocationException e) {
			// ignore
		}
		return null;
	}



}
