/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ParameterObjectFactory {

	private String fClassName;
	private ICompilationUnit fCompilationUnit;
	private boolean fCreateComments;
	private boolean fCreateGetter;
	private boolean fCreateSetter;
	private String fEnclosingType;
	private String fPackage;
	private List fVariables;

	public ParameterObjectFactory(ICompilationUnit cu) {
		super();
		this.fCompilationUnit= cu;
	}

	public RefactoringStatus checkConditions() {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkTypeName(fClassName));
		// TODO: Check for availability
		return result;
	}

	public TypeDeclaration createClassDeclaration(ASTRewrite rewriter, ICompilationUnit unit, ImportRewrite imports, String fqn) {
		AST ast= rewriter.getAST();
		TypeDeclaration typeDeclaration= ast.newTypeDeclaration();
		typeDeclaration.setName(ast.newSimpleName(fClassName));
		List body= typeDeclaration.bodyDeclarations();
		for (Iterator iter= fVariables.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (isValidField(pi)) {
				FieldDeclaration declaration= createField(rewriter, pi, imports);
				body.add(declaration);
			}
		}
		MethodDeclaration constructor= createConstructor(rewriter, imports);
		body.add(constructor);
		for (Iterator iter= fVariables.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (fCreateGetter && isValidField(pi)) {
				ASTNode getter= createGetter(rewriter, pi, imports, fqn);
				body.add(getter);
			}
			if (fCreateSetter && isValidField(pi)) {
				if (!Modifier.isFinal(pi.getOldBinding().getModifiers())) {
					ASTNode setter= createSetter(rewriter, pi, imports, fqn);
					body.add(setter);
				}
			}
		}

		return typeDeclaration;
	}

	private MethodDeclaration createConstructor(ASTRewrite rewriter, ImportRewrite imports) {
		AST ast= rewriter.getAST();
		MethodDeclaration methodDeclaration= ast.newMethodDeclaration();
		methodDeclaration.setName(ast.newSimpleName(fClassName));
		methodDeclaration.setConstructor(true);
		methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		List parameters= methodDeclaration.parameters();
		Block block= ast.newBlock();
		methodDeclaration.setBody(block);
		List statements= block.statements();
		List validParameter=new ArrayList();
		for (Iterator iter= fVariables.iterator(); iter.hasNext();) {
			ParameterInfo pi=(ParameterInfo) iter.next();
			if (isValidField(pi)) {
				validParameter.add(pi);
			}
		}
		for (Iterator iter= validParameter.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			SingleVariableDeclaration svd= ast.newSingleVariableDeclaration();
			ITypeBinding typeBinding= pi.getNewTypeBinding();
			if (!iter.hasNext() && typeBinding.isArray()){
				int dimensions= typeBinding.getDimensions();
				if (dimensions==1){
					typeBinding=typeBinding.getComponentType();
				} else {
					typeBinding=typeBinding.createArrayType(dimensions-1);
				}
				svd.setVarargs(true);
			}
			Type fieldType= imports.addImport(typeBinding, ast);
			svd.setType(fieldType);
			svd.setName(getFieldName(ast, pi));
			parameters.add(svd);
			FieldAccess fieldAccess= ast.newFieldAccess();
			fieldAccess.setName(getFieldName(ast, pi));
			fieldAccess.setExpression(ast.newThisExpression());
			Assignment assignment= ast.newAssignment();
			assignment.setLeftHandSide(fieldAccess);
			assignment.setRightHandSide(getFieldName(ast, pi));
			statements.add(ast.newExpressionStatement(assignment));
		}
		return methodDeclaration;
	}

	private FieldDeclaration createField(ASTRewrite rewriter, ParameterInfo pi, ImportRewrite imports) {
		AST ast= rewriter.getAST();
		IVariableBinding variable= pi.getOldBinding();
		VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
		fragment.setName(getFieldName(ast, pi));
		FieldDeclaration declaration= ast.newFieldDeclaration(fragment);
		List modifiers= ast.newModifiers((variable.getModifiers() & ~Modifier.FINAL) | Modifier.PUBLIC);
		declaration.modifiers().addAll(modifiers);
		declaration.setType(imports.addImport(pi.getNewTypeBinding(), ast));
		return declaration;
	}

	public Expression createFieldReadAccess(ParameterInfo pi, String paramName, AST ast) {
		if (!fCreateGetter) {
			return ast.newName(new String[] { paramName, pi.getNewName() });
		} else {
			MethodInvocation method= ast.newMethodInvocation();
			method.setName(ast.newSimpleName(getGetterName(pi, ast)));
			method.setExpression(ast.newSimpleName(paramName));
			return method;
		}
	}
	//XXX Change to complete implementation in later revision
	private ASTNode createGetter(ASTRewrite rewriter, ParameterInfo pi, ImportRewrite imports, String fullyQualifiedName) {
		AST ast= rewriter.getAST();
		String getterName= getGetterName(pi, ast);
		MethodDeclaration methodDeclaration= ast.newMethodDeclaration();
		methodDeclaration.setName(ast.newSimpleName(getterName));
		methodDeclaration.setReturnType2(imports.addImport(pi.getNewTypeBinding(), ast));
		methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		Block block= ast.newBlock();
		methodDeclaration.setBody(block);
		try {
			String bodyContent= CodeGeneration.getGetterMethodBodyContent(fCompilationUnit, fullyQualifiedName, getterName, pi.getNewName(), StubUtility.getLineDelimiterUsed(fCompilationUnit));
			ASTNode getterBody= rewriter.createStringPlaceholder(bodyContent, ASTNode.EXPRESSION_STATEMENT);
			block.statements().add(getterBody);
			return methodDeclaration;
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		FieldAccess fieldAccess= ast.newFieldAccess();
		fieldAccess.setName(getFieldName(ast, pi));
		ReturnStatement returnStatement= ast.newReturnStatement();
		returnStatement.setExpression(fieldAccess);
		block.statements().add(returnStatement);
		return methodDeclaration;
	}

	public ExpressionStatement createInitializer(ParameterInfo pi, ASTRewrite rewrite, String paramName, ImportRewrite imports) {
		AST ast= rewrite.getAST();
		VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(pi.getOldName()));
		fragment.setInitializer(createFieldReadAccess(pi, paramName, ast));
		VariableDeclarationExpression declaration= ast.newVariableDeclarationExpression(fragment);
		IVariableBinding variable= pi.getOldBinding();
		declaration.setType(imports.addImport(pi.getNewTypeBinding(), ast));
		int modifiers= variable.getModifiers();
		List newModifiers= ast.newModifiers(modifiers);
		declaration.modifiers().addAll(newModifiers);
		return ast.newExpressionStatement(declaration);
	}

	//XXX Change to complete implementation in later revision
	private ASTNode createSetter(ASTRewrite rewriter, ParameterInfo pi, ImportRewrite imports, String fullyQualifiedName) {
		AST ast= rewriter.getAST();
		MethodDeclaration methodDeclaration= ast.newMethodDeclaration();
		String setterName= getSetterName(pi, ast);
		methodDeclaration.setName(ast.newSimpleName(setterName));
		methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		SingleVariableDeclaration variable= ast.newSingleVariableDeclaration();
		variable.setType(imports.addImport(pi.getNewTypeBinding(), ast));
		String paramName= "new" + Character.toUpperCase(pi.getNewName().charAt(0)) + pi.getNewName().substring(1); //$NON-NLS-1$ //XXX bad idea!
		variable.setName(ast.newSimpleName(paramName));
		methodDeclaration.parameters().add(variable);
		Block block= ast.newBlock();
		methodDeclaration.setBody(block);
		String fieldName= pi.getNewName();
		try {
			String bodyContent= CodeGeneration.getSetterMethodBodyContent(fCompilationUnit, fullyQualifiedName, setterName, fieldName, paramName, StubUtility.getLineDelimiterUsed(fCompilationUnit));
			ASTNode setterBody= rewriter.createStringPlaceholder(bodyContent, ASTNode.EXPRESSION_STATEMENT);
			block.statements().add(setterBody);
			return methodDeclaration;
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	public Type createType(ImportRewrite imports, AST ast, boolean asTopLevelClass) {
		String concatenateName= null;
		if (asTopLevelClass)
			concatenateName=JavaModelUtil.concatenateName(fPackage, fClassName);
		else
			concatenateName=fEnclosingType;
		String addImport= imports.addImport(concatenateName);
		if (asTopLevelClass)
			return ast.newSimpleType(ast.newName(addImport));
		return ast.newQualifiedType(ast.newSimpleType(ast.newName(addImport)), ast.newSimpleName(fClassName)); 
	}

	public String getClassName() {
		return fClassName;
	}

	public String getEnclosingType() {
		return fEnclosingType;
	}

	private SimpleName getFieldName(AST ast, ParameterInfo pi) {
		return ast.newSimpleName(pi.getNewName());
	}

	private String getGetterName(ParameterInfo pi, AST ast) {
		return suggestGetterName(pi, ast);
	}

	public String getPackage() {
		return fPackage;
	}

	public ParameterInfo getParameterInfo(String identifier) {
		for (Iterator iter= fVariables.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (pi.getOldName().equals(identifier))
				return pi;
		}
		return null;
	}

	private String getSetterName(ParameterInfo pi, AST ast) {
		return suggestSetterName(pi, ast);
	}

	public boolean isCreateComments() {
		return fCreateComments;
	}

	public boolean isCreateGetter() {
		return fCreateGetter;
	}

	public boolean isCreateSetter() {
		return fCreateSetter;
	}

	private boolean isValidField(ParameterInfo pi) {
		return pi.isCreateField() && !pi.isAdded();
	}

	public void moveDown(ParameterInfo selected) {
		int idx= fVariables.indexOf(selected);
		Assert.isTrue(idx >= 0 && idx < fVariables.size() - 1);
		int nextIdx= idx + 1;
		ParameterInfo next= (ParameterInfo) fVariables.get(nextIdx);
		if (next.isAdded()){
			nextIdx++;
			Assert.isTrue(nextIdx<=fVariables.size()-1);
			next=(ParameterInfo) fVariables.get(nextIdx);
		}
		fVariables.set(idx, next);
		fVariables.set(nextIdx, selected);
	}

	public void moveUp(ParameterInfo selected) {
		int idx= fVariables.indexOf(selected);
		Assert.isTrue(idx > 0);
		int prevIdx= idx - 1;
		ParameterInfo prev= (ParameterInfo) fVariables.get(prevIdx);
		if (prev.isAdded()){
			prevIdx--;
			Assert.isTrue(prevIdx>=0);
			prev=(ParameterInfo) fVariables.get(prevIdx);
		}
		fVariables.set(idx, prev);
		fVariables.set(prevIdx, selected);
	}

	public void setClassName(String className) {
		this.fClassName= className;
	}

	public void setCreateComments(boolean selection) {
		fCreateComments=selection;
	}

	public void setCreateGetter(boolean createGetter) {
		this.fCreateGetter= createGetter;
	}

	public void setCreateSetter(boolean createSetter) {
		this.fCreateSetter= createSetter;
	}

	public void setEnclosingType(String enclosingType) {
		fEnclosingType= enclosingType;
	}

	public void setPackage(String typeQualifier) {
		this.fPackage= typeQualifier;
	}

	public void setVariables(List parameters) {
		fVariables= parameters;
	}

	private String suggestGetterName(ParameterInfo pi, AST ast) {
		ITypeBinding type= pi.getNewTypeBinding();
		boolean isBoolean= ast.resolveWellKnownType("boolean").isEqualTo(type) || ast.resolveWellKnownType("java.lang.Boolean").isEqualTo(type); //$NON-NLS-1$//$NON-NLS-2$
		return NamingConventions.suggestGetterName(fCompilationUnit.getJavaProject(), pi.getNewName(), Flags.AccPublic, isBoolean, null);
	}

	private String suggestSetterName(ParameterInfo pi, AST ast) {
		ITypeBinding type= pi.getNewTypeBinding();
		boolean isBoolean= ast.resolveWellKnownType("boolean").isEqualTo(type) || ast.resolveWellKnownType("java.lang.Boolean").isEqualTo(type); //$NON-NLS-1$//$NON-NLS-2$
		return NamingConventions.suggestSetterName(fCompilationUnit.getJavaProject(), pi.getNewName(), Flags.AccPublic, isBoolean, null);
	}

	/**
	 * Updates the position of the newly inserted parameterObject so that it is directly after the first checked parameter
	 * @param parameterObjectReference
	 */
	public void updateParameterPosition(ParameterInfo parameterObjectReference) {
		fVariables.remove(parameterObjectReference);
		for (ListIterator iterator= fVariables.listIterator(); iterator.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iterator.next();
			if (isValidField(pi)){
				iterator.add(parameterObjectReference);
				return;
			}
		}
	}

}
