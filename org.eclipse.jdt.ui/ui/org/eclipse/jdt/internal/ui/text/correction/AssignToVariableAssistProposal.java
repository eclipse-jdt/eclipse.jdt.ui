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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
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
		
		rewrite.markAsReplaced(fExpressionStatement, newDecl); 
		
		markAsLinked(rewrite, newDeclFrag.getName(), true, "name"); //$NON-NLS-1$
		markAsLinked(rewrite, newDecl.getType(), false, "type"); //$NON-NLS-1$
		markAsSelection(rewrite, newDecl);

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
		assignment.setRightHandSide((Expression) rewrite.createCopy(expression));

		SimpleName accessName= ast.newSimpleName(varName);
		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEGEN_KEYWORD_THIS)) {
			FieldAccess fieldAccess= ast.newFieldAccess();
			fieldAccess.setName(accessName);
			fieldAccess.setExpression(ast.newThisExpression());
			assignment.setLeftHandSide(fieldAccess);
		} else {
			assignment.setLeftHandSide(accessName);
		}
		rewrite.markAsReplaced(expression, assignment);
		
		markAsLinked(rewrite, newDeclFrag.getName(), false, "name"); //$NON-NLS-1$
		markAsLinked(rewrite, newDecl.getType(), false, "type"); //$NON-NLS-1$
		markAsLinked(rewrite, accessName, true, "name");  //$NON-NLS-1$
		markAsSelection(rewrite, fExpressionStatement);
		
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
	

}
