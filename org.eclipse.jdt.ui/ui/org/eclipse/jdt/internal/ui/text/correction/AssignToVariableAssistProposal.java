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

import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.NamingConventions;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Proposals for 'Assign to variable' quick assist
 * - Assign an expression from an ExpressionStatement to a local or field
 * - Assign a parameter to a field
 * */
public class AssignToVariableAssistProposal extends LinkedCorrectionProposal {

	public static final int LOCAL= 1;
	public static final int FIELD= 2;
	
	private final String KEY_NAME= "name";  //$NON-NLS-1$
	private final String KEY_TYPE= "type";  //$NON-NLS-1$

	private final int  fVariableKind;
	private final ASTNode fNodeToAssign; // ExpressionStatement or SingleVariableDeclaration
	private final ITypeBinding fTypeBinding;
		
	public AssignToVariableAssistProposal(ICompilationUnit cu, int variableKind, ExpressionStatement node, ITypeBinding typeBinding, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
	
		fVariableKind= variableKind;
		fNodeToAssign= node;
		fTypeBinding= typeBinding;
		if (variableKind == LOCAL) {
			setDisplayName(CorrectionMessages.getString("AssignToVariableAssistProposal.assigntolocal.description")); //$NON-NLS-1$
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL));
		} else {
			setDisplayName(CorrectionMessages.getString("AssignToVariableAssistProposal.assigntofield.description")); //$NON-NLS-1$
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
		}
	}
	
	public AssignToVariableAssistProposal(ICompilationUnit cu, SingleVariableDeclaration parameter, ITypeBinding typeBinding, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
	
		fVariableKind= FIELD;
		fNodeToAssign= parameter;
		fTypeBinding= typeBinding;
		setDisplayName(CorrectionMessages.getString("AssignToVariableAssistProposal.assignparamtofield.description")); //$NON-NLS-1$
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
	}	
				
	protected ASTRewrite getRewrite() throws CoreException {
		if (fVariableKind == FIELD) {
			return doAddField();
		} else { // LOCAL
			return doAddLocal();
		}
	}

	private ASTRewrite doAddLocal() throws CoreException {
		Expression expression= ((ExpressionStatement) fNodeToAssign).getExpression();
		AST ast= fNodeToAssign.getAST();
		
		ASTRewrite rewrite= ASTRewrite.create(ast);

		String varName= suggestLocalVariableNames(fTypeBinding);
				
		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(ast.newSimpleName(varName));
		newDeclFrag.setInitializer((Expression) rewrite.createCopyTarget(expression));
		
		// trick for bug 43248: use an VariableDeclarationExpression and keep the ExpressionStatement
		VariableDeclarationExpression newDecl= ast.newVariableDeclarationExpression(newDeclFrag);
		
		Type type= evaluateType(ast);
		newDecl.setType(type);
		
		rewrite.replace(expression, newDecl, null); 
		
		addLinkedPosition(rewrite.track(newDeclFrag.getName()), true, KEY_NAME);
		addLinkedPosition(rewrite.track(newDecl.getType()), false, KEY_TYPE);
		setEndPosition(rewrite.track(newDecl));

		return rewrite;
	}

	private ASTRewrite doAddField() throws CoreException {
		boolean isParamToField= fNodeToAssign.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION;
			
		ASTNode newTypeDecl= ASTResolving.findParentType(fNodeToAssign);
		if (newTypeDecl == null) {
			return null;
		}
		
		Expression expression= isParamToField ? ((SingleVariableDeclaration) fNodeToAssign).getName() : ((ExpressionStatement) fNodeToAssign).getExpression();
		
		boolean isAnonymous= newTypeDecl.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION;
		ChildListPropertyDescriptor property=  isAnonymous ? AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY : TypeDeclaration.BODY_DECLARATIONS_PROPERTY;
		List decls= (List) newTypeDecl.getStructuralProperty(property);
		
		AST ast= newTypeDecl.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		
		BodyDeclaration bodyDecl= ASTResolving.findParentBodyDeclaration(fNodeToAssign);
		Block body;
		if (bodyDecl instanceof MethodDeclaration) {
			body= ((MethodDeclaration) bodyDecl).getBody();
		} else if (bodyDecl instanceof Initializer) {
			body= ((Initializer) bodyDecl).getBody();
		} else {
			return null;
		}
		
		boolean isStatic= Modifier.isStatic(bodyDecl.getModifiers()) && !isAnonymous;
		boolean isConstructorParam= isParamToField && fNodeToAssign.getParent() instanceof MethodDeclaration && ((MethodDeclaration) fNodeToAssign.getParent()).isConstructor();
		int modifiers= Modifier.PRIVATE;
		if (isStatic) {
			modifiers |= Modifier.STATIC;
		} else if (isConstructorParam) {
			modifiers |= Modifier.FINAL;
		}
		
		String varName= suggestFieldNames(fTypeBinding, expression, modifiers);
		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(ast.newSimpleName(varName));
				
		FieldDeclaration newDecl= ast.newFieldDeclaration(newDeclFrag);
		
		Type type= evaluateType(ast);
		newDecl.setType(type);
		newDecl.setModifiers(modifiers);
		
		Assignment assignment= ast.newAssignment();
		assignment.setRightHandSide((Expression) rewrite.createCopyTarget(expression));

		boolean needsThis= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEGEN_KEYWORD_THIS);
		if (isParamToField) {
			needsThis |= varName.equals(((SimpleName) expression).getIdentifier());
		}

		SimpleName accessName= ast.newSimpleName(varName);
		if (needsThis) {
			FieldAccess fieldAccess= ast.newFieldAccess();
			fieldAccess.setName(accessName);
			if (isStatic) {
				String typeName= ((TypeDeclaration) newTypeDecl).getName().getIdentifier();
				fieldAccess.setExpression(ast.newSimpleName(typeName));
			} else {
				fieldAccess.setExpression(ast.newThisExpression());
			}
			assignment.setLeftHandSide(fieldAccess);
		} else {
			assignment.setLeftHandSide(accessName);
		}
		
		int insertIndex= findFieldInsertIndex(decls, fNodeToAssign.getStartPosition());
		rewrite.getListRewrite(newTypeDecl, property).insertAt(newDecl, insertIndex, null);

		ASTNode selectionNode;
		if (isParamToField) {
			// assign parameter to field
			ExpressionStatement statement= ast.newExpressionStatement(assignment);
			int insertIdx=  findAssignmentInsertIndex(body.statements());
			rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY).insertAt(statement, insertIdx, null);
			selectionNode= statement;
			
		} else {			
			rewrite.replace(expression, assignment, null);
			selectionNode= fNodeToAssign;
		} 
		
		addLinkedPosition(rewrite.track(newDeclFrag.getName()), false, KEY_NAME);
		addLinkedPosition(rewrite.track(newDecl.getType()), false, KEY_TYPE);
		addLinkedPosition(rewrite.track(accessName), true, KEY_NAME);
		setEndPosition(rewrite.track(selectionNode));
		
		return rewrite;		
	}

	private Type evaluateType(AST ast) throws CoreException {
		ITypeBinding[] proposals= ASTResolving.getRelaxingTypes(ast, fTypeBinding);
		for (int i= 0; i < proposals.length; i++) {
			addLinkedPositionProposal(KEY_TYPE, proposals[i]);
		}
		String typeName= getImportRewrite().addImport(fTypeBinding);
		return ASTNodeFactory.newType(ast, typeName);
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
		for (int i= 0; i < names.length; i++) {
			addLinkedPositionProposal(KEY_NAME, names[i], null);
		}
		return names[0]; 
	}
	
	private String suggestFieldNames(ITypeBinding binding, Expression expression, int modifiers) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		ITypeBinding base= binding.isArray() ? binding.getElementType() : binding;
		IPackageBinding packBinding= base.getPackage();
		String packName= packBinding != null ? packBinding.getName() : ""; //$NON-NLS-1$
		
		String[] excludedNames= getUsedVariableNames();
		String result= null;
		HashSet taken= new HashSet();
		
		if (expression instanceof SimpleName) {
			String name= ((SimpleName) expression).getIdentifier();
			// bug 38111
			String[] argname= StubUtility.getFieldNameSuggestions(project, name, modifiers, excludedNames);
			for (int i= 0; i < argname.length; i++) {
				String curr= argname[i];
				if (result == null || curr.length() > result.length()) {
					result= curr;
				}
				if (taken.add(curr)) {
					addLinkedPositionProposal(KEY_NAME, curr, null);
				}
			}			
		}

		String typeName= base.getName();
		String[] names= NamingConventions.suggestFieldNames(project, packName, typeName, binding.getDimensions(), modifiers, excludedNames);
		if (names.length == 0) {
			return "class1"; // fix for pr, remoev after 20030127 //$NON-NLS-1$
		}
		for (int i= 0; i < names.length; i++) {
			String curr= names[i];
			if (taken.add(curr)) {
				addLinkedPositionProposal(KEY_NAME, curr, null);
			}
		}
		if (result == null) {
			result= names[0];
		}
		return result;		
	}
	
	private String[] getUsedVariableNames() {
		CompilationUnit root= (CompilationUnit) fNodeToAssign.getRoot();
		IBinding[] varsBefore= (new ScopeAnalyzer(root)).getDeclarationsInScope(fNodeToAssign.getStartPosition(), ScopeAnalyzer.VARIABLES);
		IBinding[] varsAfter= (new ScopeAnalyzer(root)).getDeclarationsAfter(fNodeToAssign.getStartPosition() + fNodeToAssign.getLength(), ScopeAnalyzer.VARIABLES);
		
		String[] names= new String[varsBefore.length + varsAfter.length];
		for (int i= 0; i < varsBefore.length; i++) {
			names[i]= varsBefore[i].getName();
		}
		for (int i= varsBefore.length; i < names.length; i++) {
			names[i]= varsAfter[i].getName();
		}
		return names;
	}

	private int findAssignmentInsertIndex(List statements) {

		HashSet paramsBefore= new HashSet();
		List params = ((MethodDeclaration) fNodeToAssign.getParent()).parameters();
		for (int i = 0; i < params.size() && (params.get(i) != fNodeToAssign); i++) {
			SingleVariableDeclaration decl= (SingleVariableDeclaration) params.get(i);
			paramsBefore.add(decl.getName().getIdentifier());
		}
		
		int i= 0;
		for (i = 0; i < statements.size(); i++) {
			Statement curr= (Statement) statements.get(i);
			switch (curr.getNodeType()) {
				case ASTNode.CONSTRUCTOR_INVOCATION:
				case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
					break;
				case ASTNode.EXPRESSION_STATEMENT:
					Expression expr= ((ExpressionStatement) curr).getExpression();
					if (expr instanceof Assignment) {
						Assignment assignment= (Assignment) expr;
						Expression rightHand = assignment.getRightHandSide();
						if (rightHand instanceof SimpleName && paramsBefore.contains(((SimpleName) rightHand).getIdentifier())) {
							IVariableBinding binding = Bindings.getAssignedVariable(assignment);
							if (binding == null || binding.isField()) {
								break;
							}
						}
					}
					return i;
				default:
					return i;
			
			}
		}
		return i;
		
	}
	
	private int findFieldInsertIndex(List decls, int currPos) {
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
