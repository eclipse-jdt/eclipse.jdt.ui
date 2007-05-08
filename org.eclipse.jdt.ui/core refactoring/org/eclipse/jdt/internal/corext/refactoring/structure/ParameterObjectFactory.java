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
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.CodeGeneration;

public class ParameterObjectFactory {

	private String fClassName;
	private ICompilationUnit fCompilationUnit;
	private boolean fCreateComments;// initialized with setting from StubUtility
	private boolean fCreateGetter;
	private boolean fCreateSetter;
	private String fEnclosingType;
	private String fPackage;
	private List fVariables;

	public ParameterObjectFactory(ICompilationUnit cu) {
		super();
		this.fCompilationUnit= cu;
		this.fCreateComments= StubUtility.doAddComments(cu.getJavaProject());
	}

	public RefactoringStatus checkConditions() {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkTypeName(fClassName));
		// TODO: Check for availability
		return result;
	}

	public TypeDeclaration createClassDeclaration(ICompilationUnit unit, String declaringType, CompilationUnitRewrite cuRewrite) throws CoreException {
		AST ast= cuRewrite.getAST();
		TypeDeclaration typeDeclaration= ast.newTypeDeclaration();
		typeDeclaration.setName(ast.newSimpleName(fClassName));
		List body= typeDeclaration.bodyDeclarations();
		for (Iterator iter= fVariables.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (isValidField(pi)) {
				FieldDeclaration declaration= createField(pi, unit, cuRewrite);
				body.add(declaration);
			}
		}
		MethodDeclaration constructor= createConstructor(unit, declaringType, cuRewrite);
		body.add(constructor);
		for (Iterator iter= fVariables.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (fCreateGetter && isValidField(pi)) {
				ASTNode getter= createGetter(pi, declaringType, unit, cuRewrite);
				body.add(getter);
			}
			if (fCreateSetter && isValidField(pi)) {
				if (!Modifier.isFinal(pi.getOldBinding().getModifiers())) {
					ASTNode setter= createSetter(pi, declaringType, unit, cuRewrite);
					body.add(setter);
				}
			}
		}

		return typeDeclaration;
	}

	private MethodDeclaration createConstructor(ICompilationUnit unit, String declaringTypeName, CompilationUnitRewrite cuRewrite) throws CoreException {
		AST ast= cuRewrite.getAST();
		MethodDeclaration methodDeclaration= ast.newMethodDeclaration();
		methodDeclaration.setName(ast.newSimpleName(fClassName));
		methodDeclaration.setConstructor(true);
		methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		String lineDelimiter= StubUtility.getLineDelimiterUsed(unit);
		if (fCreateComments) {
			String comment= CodeGeneration.getMethodComment(unit, declaringTypeName, methodDeclaration, null, lineDelimiter);
			if (comment != null) {
				Javadoc doc= (Javadoc) cuRewrite.getASTRewrite().createStringPlaceholder(comment, ASTNode.JAVADOC);
				methodDeclaration.setJavadoc(doc);
			}
		}
		List parameters= methodDeclaration.parameters();
		Block block= ast.newBlock();
		methodDeclaration.setBody(block);
		List statements= block.statements();
		List validParameter= new ArrayList();
		for (Iterator iter= fVariables.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			if (isValidField(pi)) {
				validParameter.add(pi);
			}
		}
		for (Iterator iter= validParameter.iterator(); iter.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iter.next();
			SingleVariableDeclaration svd= ast.newSingleVariableDeclaration();
			ITypeBinding typeBinding= pi.getNewTypeBinding();
			if (!iter.hasNext() && typeBinding.isArray() && JavaModelUtil.is50OrHigher(fCompilationUnit.getJavaProject())) {
				int dimensions= typeBinding.getDimensions();
				if (dimensions == 1) {
					typeBinding= typeBinding.getComponentType();
				} else {
					typeBinding= typeBinding.createArrayType(dimensions - 1);
				}
				svd.setVarargs(true);
			}
			Type fieldType= importBinding(typeBinding,cuRewrite);
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

	private Type importBinding(ITypeBinding typeBinding, CompilationUnitRewrite cuRewrite) {
		Type type= cuRewrite.getImportRewrite().addImport(typeBinding, cuRewrite.getAST());
		cuRewrite.getImportRemover().registerAddedImports(type);
		return type;
	}

	private FieldDeclaration createField(ParameterInfo pi, ICompilationUnit unit, CompilationUnitRewrite cuRewrite) throws CoreException {
		AST ast= cuRewrite.getAST();
		VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
		String lineDelim= StubUtility.getLineDelimiterUsed(unit);
		SimpleName fieldName= getFieldName(ast, pi);
		fragment.setName(fieldName);
		FieldDeclaration declaration= ast.newFieldDeclaration(fragment);
		if (fCreateComments) {
			String comment= StubUtility.getFieldComment(unit, pi.getNewTypeName(), pi.getNewName(), lineDelim);
			if (comment != null) {
				Javadoc doc= (Javadoc) cuRewrite.getASTRewrite().createStringPlaceholder(comment, ASTNode.JAVADOC);
				declaration.setJavadoc(doc);
			}
		}
		int visibility= Modifier.PUBLIC;
		if (fCreateGetter) {
			visibility= Modifier.PRIVATE;
		}
		List modifiers= ast.newModifiers(visibility);
		declaration.modifiers().addAll(modifiers);
		declaration.setType(importBinding(pi.getNewTypeBinding(), cuRewrite));
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

	private ASTNode createGetter(ParameterInfo pi, String declaringType, ICompilationUnit cu, CompilationUnitRewrite cuRewrite) throws CoreException {
		AST ast= cuRewrite.getAST();
		MethodDeclaration methodDeclaration= ast.newMethodDeclaration();
		String fieldName= pi.getNewName();
		String getterName= getGetterName(pi, ast);
		String lineDelim= StubUtility.getLineDelimiterUsed(cu);
		String bareFieldname= NamingConventions.removePrefixAndSuffixForFieldName(cu.getJavaProject(), fieldName, Flags.AccPrivate);
		if (fCreateComments) {
			String comment= CodeGeneration.getGetterComment(cu, declaringType, getterName, fieldName, pi.getNewTypeName(), bareFieldname, lineDelim);
			if (comment != null)
				methodDeclaration.setJavadoc((Javadoc) cuRewrite.getASTRewrite().createStringPlaceholder(comment, ASTNode.JAVADOC));
		}
		methodDeclaration.setName(ast.newSimpleName(getterName));
		methodDeclaration.setReturnType2(importBinding(pi.getNewTypeBinding(), cuRewrite));
		methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		Block block= ast.newBlock();
		methodDeclaration.setBody(block);
		boolean useThis= StubUtility.useThisForFieldAccess(cu.getJavaProject());
		if (useThis) {
			fieldName= "this." + fieldName; //$NON-NLS-1$
		}
		String bodyContent= CodeGeneration.getGetterMethodBodyContent(cu, declaringType, getterName, fieldName, lineDelim);
		ASTNode getterBody= cuRewrite.getASTRewrite().createStringPlaceholder(bodyContent, ASTNode.EXPRESSION_STATEMENT);
		block.statements().add(getterBody);
		return methodDeclaration;
	}

	public ExpressionStatement createInitializer(ParameterInfo pi, String paramName, CompilationUnitRewrite cuRewrite) {
		AST ast= cuRewrite.getAST();
		VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(pi.getOldName()));
		fragment.setInitializer(createFieldReadAccess(pi, paramName, ast));
		VariableDeclarationExpression declaration= ast.newVariableDeclarationExpression(fragment);
		IVariableBinding variable= pi.getOldBinding();
		declaration.setType(importBinding(pi.getNewTypeBinding(), cuRewrite));
		int modifiers= variable.getModifiers();
		List newModifiers= ast.newModifiers(modifiers);
		declaration.modifiers().addAll(newModifiers);
		return ast.newExpressionStatement(declaration);
	}

	private ASTNode createSetter(ParameterInfo pi, String declaringType, ICompilationUnit cu, CompilationUnitRewrite cuRewrite) throws CoreException {
		AST ast= cuRewrite.getAST();
		MethodDeclaration methodDeclaration= ast.newMethodDeclaration();
		String fieldName= pi.getNewName();
		String setterName= getSetterName(pi, ast);
		String lineDelim= StubUtility.getLineDelimiterUsed(cu);
		String bareFieldname= NamingConventions.removePrefixAndSuffixForFieldName(cu.getJavaProject(), fieldName, Flags.AccPrivate);
		String paramName= StubUtility.suggestArgumentName(cu.getJavaProject(), bareFieldname, null);
		if (fCreateComments) {
			String comment= CodeGeneration.getSetterComment(cu, declaringType, setterName, fieldName, pi.getNewTypeName(), paramName, bareFieldname, lineDelim);
			if (comment != null)
				methodDeclaration.setJavadoc((Javadoc) cuRewrite.getASTRewrite().createStringPlaceholder(comment, ASTNode.JAVADOC));
		}
		methodDeclaration.setName(ast.newSimpleName(setterName));
		methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		SingleVariableDeclaration variable= ast.newSingleVariableDeclaration();
		variable.setType(importBinding(pi.getNewTypeBinding(), cuRewrite));
		variable.setName(ast.newSimpleName(paramName));
		methodDeclaration.parameters().add(variable);
		Block block= ast.newBlock();
		methodDeclaration.setBody(block);
		boolean useThis= StubUtility.useThisForFieldAccess(cu.getJavaProject());
		if (useThis || fieldName.equals(paramName)) {
			fieldName= "this." + fieldName; //$NON-NLS-1$
		}
		String bodyContent= CodeGeneration.getSetterMethodBodyContent(fCompilationUnit, declaringType, setterName, fieldName, paramName, lineDelim);
		ASTNode setterBody= cuRewrite.getASTRewrite().createStringPlaceholder(bodyContent, ASTNode.EXPRESSION_STATEMENT);
		block.statements().add(setterBody);
		return methodDeclaration;
	}

	public Type createType(boolean asTopLevelClass, CompilationUnitRewrite cuRewrite) {
		String concatenateName= null;
		if (asTopLevelClass) {
			concatenateName= JavaModelUtil.concatenateName(fPackage, fClassName);
		} else {
			concatenateName= JavaModelUtil.concatenateName(fEnclosingType, fClassName);
		}
		String addedImport= cuRewrite.getImportRewrite().addImport(concatenateName);
		cuRewrite.getImportRemover().registerAddedImport(addedImport);
		AST ast=cuRewrite.getAST();
		return ast.newSimpleType(ast.newName(addedImport));
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
		if (next.isAdded()) {
			nextIdx++;
			Assert.isTrue(nextIdx <= fVariables.size() - 1);
			next= (ParameterInfo) fVariables.get(nextIdx);
		}
		fVariables.set(idx, next);
		fVariables.set(nextIdx, selected);
	}

	public void moveUp(ParameterInfo selected) {
		int idx= fVariables.indexOf(selected);
		Assert.isTrue(idx > 0);
		int prevIdx= idx - 1;
		ParameterInfo prev= (ParameterInfo) fVariables.get(prevIdx);
		if (prev.isAdded()) {
			prevIdx--;
			Assert.isTrue(prevIdx >= 0);
			prev= (ParameterInfo) fVariables.get(prevIdx);
		}
		fVariables.set(idx, prev);
		fVariables.set(prevIdx, selected);
	}

	public void setClassName(String className) {
		this.fClassName= className;
	}

	public void setCreateComments(boolean selection) {
		fCreateComments= selection;
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
	 * Updates the position of the newly inserted parameterObject so that it is
	 * directly after the first checked parameter
	 * 
	 * @param parameterObjectReference
	 */
	public void updateParameterPosition(ParameterInfo parameterObjectReference) {
		fVariables.remove(parameterObjectReference);
		for (ListIterator iterator= fVariables.listIterator(); iterator.hasNext();) {
			ParameterInfo pi= (ParameterInfo) iterator.next();
			if (isValidField(pi)) {
				iterator.add(parameterObjectReference);
				return;
			}
		}
	}

}
