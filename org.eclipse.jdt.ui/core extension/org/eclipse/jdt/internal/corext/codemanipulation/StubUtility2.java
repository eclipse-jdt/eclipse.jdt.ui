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
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Utilities for code generation based on ast rewrite.
 * 
 * @since 3.1
 */
public final class StubUtility2 {

	public static MethodDeclaration createConstructorStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, AST ast, IMethodBinding binding, String type, int modifiers, boolean omitSuper, CodeGenerationSettings settings) throws CoreException {

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers & ~Modifier.ABSTRACT & ~Modifier.NATIVE));
		decl.setName(ast.newSimpleName(type));
		decl.setConstructor(true);

		ITypeBinding[] typeParams= binding.getTypeParameters();
		List typeParameters= decl.typeParameters();
		for (int i= 0; i < typeParams.length; i++) {
			ITypeBinding curr= typeParams[i];
			TypeParameter newTypeParam= ast.newTypeParameter();
			newTypeParam.setName(ast.newSimpleName(curr.getName()));
			ITypeBinding[] typeBounds= curr.getTypeBounds();
			if (typeBounds.length != 1 || !"java.lang.Object".equals(typeBounds[0].getQualifiedName())) {//$NON-NLS-1$
				List newTypeBounds= newTypeParam.typeBounds();
				for (int k= 0; k < typeBounds.length; k++) {
					newTypeBounds.add(imports.addImport(typeBounds[k], ast));
				}
			}
			typeParameters.add(newTypeParam);
		}

		List parameters= createParameters(unit, imports, ast, binding, decl);

		List thrownExceptions= decl.thrownExceptions();
		ITypeBinding[] excTypes= binding.getExceptionTypes();
		for (int i= 0; i < excTypes.length; i++) {
			String excTypeName= imports.addImport(excTypes[i]);
			thrownExceptions.add(ASTNodeFactory.newName(ast, excTypeName));
		}

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);
		String bodyStatement= ""; //$NON-NLS-1$
		if (!omitSuper) {
			SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
			SingleVariableDeclaration varDecl= null;
			for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= (SingleVariableDeclaration) iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			bodyStatement= ASTNodes.asFormattedString(invocation, 0, delimiter);
		}

		String placeHolder= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), true, bodyStatement, delimiter);
		if (placeHolder != null) {
			ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
			body.statements().add(todoNode);
		}

		if (settings != null && settings.createComments) {
			String string= getMethodComment(unit, type, decl, binding, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	public static MethodDeclaration createConstructorStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ITypeBinding typeBinding, AST ast, IMethodBinding superConstructor, IVariableBinding[] variableBindings, int modifiers, CodeGenerationSettings settings) throws CoreException {

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers & ~Modifier.ABSTRACT & ~Modifier.NATIVE));
		decl.setName(ast.newSimpleName(typeBinding.getName()));
		decl.setConstructor(true);

		List parameters= decl.parameters();
		if (superConstructor != null) {
			ITypeBinding[] typeParams= superConstructor.getTypeParameters();
			List typeParameters= decl.typeParameters();
			for (int i= 0; i < typeParams.length; i++) {
				ITypeBinding curr= typeParams[i];
				TypeParameter newTypeParam= ast.newTypeParameter();
				newTypeParam.setName(ast.newSimpleName(curr.getName()));
				ITypeBinding[] typeBounds= curr.getTypeBounds();
				if (typeBounds.length != 1 || !"java.lang.Object".equals(typeBounds[0].getQualifiedName())) {//$NON-NLS-1$
					List newTypeBounds= newTypeParam.typeBounds();
					for (int k= 0; k < typeBounds.length; k++) {
						newTypeBounds.add(imports.addImport(typeBounds[k], ast));
					}
				}
				typeParameters.add(newTypeParam);
			}

			createParameters(unit, imports, ast, superConstructor, decl);

			ITypeBinding[] params= superConstructor.getParameterTypes();
			String[] paramNames= suggestArgumentNames(unit.getJavaProject(), superConstructor);
			for (int i= 0; i < params.length; i++) {
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setType(imports.addImport(params[i], ast));
				var.setName(ast.newSimpleName(paramNames[i]));
				parameters.add(var);
			}

			List thrownExceptions= decl.thrownExceptions();
			ITypeBinding[] excTypes= superConstructor.getExceptionTypes();
			for (int i= 0; i < excTypes.length; i++) {
				String excTypeName= imports.addImport(excTypes[i]);
				thrownExceptions.add(ASTNodeFactory.newName(ast, excTypeName));
			}
		}

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);

		String bodyStatement= ""; //$NON-NLS-1$
		if (superConstructor != null) {
			SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
			SingleVariableDeclaration varDecl= null;
			for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= (SingleVariableDeclaration) iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			bodyStatement= ASTNodes.asFormattedString(invocation, 0, delimiter);
		}

		for (int i= 0; i < variableBindings.length; i++) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			var.setType(imports.addImport(variableBindings[i].getType(), ast));
			var.setName(ast.newSimpleName(variableBindings[i].getName()));
			parameters.add(var);
		}

		String placeHolder= CodeGeneration.getMethodBodyContent(unit, typeBinding.getName(), typeBinding.getName(), true, bodyStatement, delimiter);
		if (placeHolder != null) {
			ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
			body.statements().add(todoNode);
		}

		for (int i= 0; i < variableBindings.length; i++) {
			FieldAccess access= ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName(variableBindings[i].getName()));
			Assignment assignment= ast.newAssignment();
			assignment.setLeftHandSide(access);
			assignment.setRightHandSide(ast.newSimpleName(variableBindings[i].getName()));
			assignment.setOperator(Assignment.Operator.ASSIGN);
			body.statements().add(ast.newExpressionStatement(assignment));
		}

		if (settings != null && settings.createComments) {
			String string= getMethodComment(unit, typeBinding.getName(), decl, superConstructor, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	public static MethodDeclaration createDelegationStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, AST ast, IBinding[] bindings, CodeGenerationSettings settings) throws CoreException {
		Assert.isNotNull(bindings);
		Assert.isTrue(bindings.length == 2);
		Assert.isTrue(bindings[0] instanceof IVariableBinding);
		Assert.isTrue(bindings[1] instanceof IMethodBinding);

		IVariableBinding variableBinding= (IVariableBinding) bindings[0];
		IMethodBinding methodBinding= (IMethodBinding) bindings[1];

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, methodBinding.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE));

		decl.setName(ast.newSimpleName(methodBinding.getName()));
		decl.setConstructor(false);

		ITypeBinding[] typeParams= methodBinding.getTypeParameters();
		List typeParameters= decl.typeParameters();
		for (int i= 0; i < typeParams.length; i++) {
			ITypeBinding curr= typeParams[i];
			TypeParameter newTypeParam= ast.newTypeParameter();
			newTypeParam.setName(ast.newSimpleName(curr.getName()));
			ITypeBinding[] typeBounds= curr.getTypeBounds();
			if (typeBounds.length != 1 || !"java.lang.Object".equals(typeBounds[0].getQualifiedName())) {//$NON-NLS-1$
				List newTypeBounds= newTypeParam.typeBounds();
				for (int k= 0; k < typeBounds.length; k++) {
					newTypeBounds.add(imports.addImport(typeBounds[k], ast));
				}
			}
			typeParameters.add(newTypeParam);
		}

		decl.setReturnType2(imports.addImport(methodBinding.getReturnType(), ast));

		List parameters= decl.parameters();
		ITypeBinding[] params= methodBinding.getParameterTypes();
		String[] paramNames= suggestArgumentNames(unit.getJavaProject(), methodBinding);
		for (int i= 0; i < params.length; i++) {
			SingleVariableDeclaration varDecl= ast.newSingleVariableDeclaration();
			if (params[i].isWildcardType() && !params[i].isUpperbound())
				varDecl.setType(imports.addImport(params[i].getBound(), ast));
			else {
				if (methodBinding.isVarargs() && params[i].isArray() && i == params.length - 1) {
					StringBuffer buffer= new StringBuffer(imports.addImport(params[i].getElementType()));
					for (int dim= 1; dim < params[i].getDimensions(); dim++)
						buffer.append("[]"); //$NON-NLS-1$
					varDecl.setType(ASTNodeFactory.newType(ast, buffer.toString()));
					varDecl.setVarargs(true);
				} else
					varDecl.setType(imports.addImport(params[i], ast));
			}
			varDecl.setName(ast.newSimpleName(paramNames[i]));
			parameters.add(varDecl);
		}

		List thrownExceptions= decl.thrownExceptions();
		ITypeBinding[] excTypes= methodBinding.getExceptionTypes();
		for (int i= 0; i < excTypes.length; i++) {
			String excTypeName= imports.addImport(excTypes[i]);
			thrownExceptions.add(ASTNodeFactory.newName(ast, excTypeName));
		}

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);

		String bodyStatement= ""; //$NON-NLS-1$
		Statement statement= null;
		MethodInvocation invocation= ast.newMethodInvocation();
		invocation.setName(ast.newSimpleName(methodBinding.getName()));
		List arguments= invocation.arguments();
		for (int i= 0; i < params.length; i++)
			arguments.add(ast.newSimpleName(paramNames[i]));
		if (settings.useKeywordThis) {
			FieldAccess access= ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName(variableBinding.getName()));
			invocation.setExpression(access);
		} else
			invocation.setExpression(ast.newSimpleName(variableBinding.getName()));
		if (methodBinding.getReturnType().isPrimitive() && methodBinding.getReturnType().getName().equals("void")) {//$NON-NLS-1$
			statement= ast.newExpressionStatement(invocation);
		} else {
			ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(invocation);
			statement= returnStatement;
		}
		bodyStatement= ASTNodes.asFormattedString(statement, 0, delimiter);

		ITypeBinding declaringType= variableBinding.getDeclaringClass();
		String qualifiedName= declaringType.getQualifiedName();
		IPackageBinding packageBinding= declaringType.getPackage();
		if (packageBinding != null) {
			if (packageBinding.getName().length() > 0 && qualifiedName.startsWith(packageBinding.getName()))
				qualifiedName= qualifiedName.substring(packageBinding.getName().length());
		}
		String placeHolder= CodeGeneration.getMethodBodyContent(unit, qualifiedName, bindings[1].getName(), false, bodyStatement, delimiter);
		if (placeHolder != null) {
			ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
			body.statements().add(todoNode);
		}

		if (settings != null && settings.createComments) {
			String string= getMethodComment(unit, qualifiedName, decl, methodBinding, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	public static MethodDeclaration createImplementationStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, AST ast, IMethodBinding binding, String type, CodeGenerationSettings settings, boolean annotations) throws CoreException {

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, binding.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE));

		decl.setName(ast.newSimpleName(binding.getName()));
		decl.setConstructor(false);

		ITypeBinding[] typeParams= binding.getTypeParameters();
		List typeParameters= decl.typeParameters();
		for (int i= 0; i < typeParams.length; i++) {
			ITypeBinding curr= typeParams[i];
			TypeParameter newTypeParam= ast.newTypeParameter();
			newTypeParam.setName(ast.newSimpleName(curr.getName()));
			ITypeBinding[] typeBounds= curr.getTypeBounds();
			if (typeBounds.length != 1 || !"java.lang.Object".equals(typeBounds[0].getQualifiedName())) {//$NON-NLS-1$
				List newTypeBounds= newTypeParam.typeBounds();
				for (int k= 0; k < typeBounds.length; k++) {
					newTypeBounds.add(imports.addImport(typeBounds[k], ast));
				}
			}
			typeParameters.add(newTypeParam);
		}

		decl.setReturnType2(imports.addImport(binding.getReturnType(), ast));

		List parameters= createParameters(unit, imports, ast, binding, decl);

		List thrownExceptions= decl.thrownExceptions();
		ITypeBinding[] excTypes= binding.getExceptionTypes();
		for (int i= 0; i < excTypes.length; i++) {
			String excTypeName= imports.addImport(excTypes[i]);
			thrownExceptions.add(ASTNodeFactory.newName(ast, excTypeName));
		}

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);

		String bodyStatement= ""; //$NON-NLS-1$
		ITypeBinding declaringType= binding.getDeclaringClass();
		if (Modifier.isAbstract(binding.getModifiers()) || declaringType.isAnnotation() || declaringType.isInterface()) {
			Expression expression= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType2(), decl.getExtraDimensions());
			if (expression != null) {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(expression);
				bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter);
			}
		} else {
			SuperMethodInvocation invocation= ast.newSuperMethodInvocation();
			invocation.setName(ast.newSimpleName(binding.getName()));
			SingleVariableDeclaration varDecl= null;
			for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= (SingleVariableDeclaration) iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			Expression expression= invocation;
			Type returnType= decl.getReturnType2();
			if (returnType != null && (returnType instanceof PrimitiveType) && ((PrimitiveType) returnType).getPrimitiveTypeCode().equals(PrimitiveType.VOID)) {
				bodyStatement= ASTNodes.asFormattedString(ast.newExpressionStatement(expression), 0, delimiter);
			} else {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(expression);
				bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter);
			}
		}

		String placeHolder= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), false, bodyStatement, delimiter);
		if (placeHolder != null) {
			ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
			body.statements().add(todoNode);
		}

		if (settings != null && settings.createComments) {
			String string= getMethodComment(unit, type, decl, binding, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		if (annotations) {
			final Annotation marker= rewrite.getAST().newMarkerAnnotation();
			marker.setTypeName(rewrite.getAST().newSimpleName("Override")); //$NON-NLS-1$
			rewrite.getListRewrite(decl, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(marker, null);
		}
		return decl;
	}

	public static MethodDeclaration createImplementationStub(ICompilationUnit unit, ASTRewrite rewrite, ImportsStructure structure, AST ast, IMethodBinding binding, String type, CodeGenerationSettings settings, boolean annotations) throws CoreException {

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, binding.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE));

		decl.setName(ast.newSimpleName(binding.getName()));
		decl.setConstructor(false);

		ITypeBinding[] typeParams= binding.getTypeParameters();
		List typeParameters= decl.typeParameters();
		for (int i= 0; i < typeParams.length; i++) {
			ITypeBinding curr= typeParams[i];
			TypeParameter newTypeParam= ast.newTypeParameter();
			newTypeParam.setName(ast.newSimpleName(curr.getName()));
			ITypeBinding[] typeBounds= curr.getTypeBounds();
			if (typeBounds.length != 1 || !"java.lang.Object".equals(typeBounds[0].getQualifiedName())) {//$NON-NLS-1$
				List newTypeBounds= newTypeParam.typeBounds();
				for (int k= 0; k < typeBounds.length; k++) {
					newTypeBounds.add(structure.addImport(typeBounds[k], ast));
				}
			}
			typeParameters.add(newTypeParam);
		}

		decl.setReturnType2(structure.addImport(binding.getReturnType(), ast));

		List parameters= createParameters(unit, structure, ast, binding, decl);

		List thrownExceptions= decl.thrownExceptions();
		ITypeBinding[] excTypes= binding.getExceptionTypes();
		for (int i= 0; i < excTypes.length; i++) {
			String excTypeName= structure.addImport(excTypes[i]);
			thrownExceptions.add(ASTNodeFactory.newName(ast, excTypeName));
		}

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);

		String bodyStatement= ""; //$NON-NLS-1$
		ITypeBinding declaringType= binding.getDeclaringClass();
		if (Modifier.isAbstract(binding.getModifiers()) || declaringType.isAnnotation() || declaringType.isInterface()) {
			Expression expression= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType2(), decl.getExtraDimensions());
			if (expression != null) {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(expression);
				bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter);
			}
		} else {
			SuperMethodInvocation invocation= ast.newSuperMethodInvocation();
			invocation.setName(ast.newSimpleName(binding.getName()));
			SingleVariableDeclaration varDecl= null;
			for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= (SingleVariableDeclaration) iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			Expression expression= invocation;
			Type returnType= decl.getReturnType2();
			if (returnType != null && (returnType instanceof PrimitiveType) && ((PrimitiveType) returnType).getPrimitiveTypeCode().equals(PrimitiveType.VOID)) {
				bodyStatement= ASTNodes.asFormattedString(ast.newExpressionStatement(expression), 0, delimiter);
			} else {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(expression);
				bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter);
			}
		}

		String placeHolder= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), false, bodyStatement, delimiter);
		if (placeHolder != null) {
			ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
			body.statements().add(todoNode);
		}

		if (settings != null && settings.createComments) {
			String string= getMethodComment(unit, type, decl, binding, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		if (annotations) {
			final Annotation marker= rewrite.getAST().newMarkerAnnotation();
			marker.setTypeName(rewrite.getAST().newSimpleName("Override")); //$NON-NLS-1$
			rewrite.getListRewrite(decl, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(marker, null);
		}
		return decl;
	}

	private static List createParameters(ICompilationUnit unit, ImportRewrite imports, AST ast, IMethodBinding binding, MethodDeclaration decl) {
		List parameters= decl.parameters();
		ITypeBinding[] params= binding.getParameterTypes();
		String[] paramNames= suggestArgumentNames(unit.getJavaProject(), binding);
		for (int i= 0; i < params.length; i++) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			if (binding.isVarargs() && params[i].isArray() && i == params.length - 1) {
				StringBuffer buffer= new StringBuffer(imports.addImport(params[i].getElementType()));
				for (int dim= 1; dim < params[i].getDimensions(); dim++)
					buffer.append("[]"); //$NON-NLS-1$
				var.setType(ASTNodeFactory.newType(ast, buffer.toString()));
				var.setVarargs(true);
			} else
				var.setType(imports.addImport(params[i], ast));
			var.setName(ast.newSimpleName(paramNames[i]));
			parameters.add(var);
		}
		return parameters;
	}

	private static List createParameters(ICompilationUnit unit, ImportsStructure imports, AST ast, IMethodBinding binding, MethodDeclaration decl) {
		List parameters= decl.parameters();
		ITypeBinding[] params= binding.getParameterTypes();
		String[] paramNames= suggestArgumentNames(unit.getJavaProject(), binding);
		for (int i= 0; i < params.length; i++) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			if (binding.isVarargs() && params[i].isArray() && i == params.length - 1) {
				StringBuffer buffer= new StringBuffer(imports.addImport(params[i].getElementType()));
				for (int dim= 1; dim < params[i].getDimensions(); dim++)
					buffer.append("[]"); //$NON-NLS-1$
				var.setType(ASTNodeFactory.newType(ast, buffer.toString()));
				var.setVarargs(true);
			} else
				var.setType(imports.addImport(params[i], ast));
			var.setName(ast.newSimpleName(paramNames[i]));
			parameters.add(var);
		}
		return parameters;
	}

	private static IMethodBinding findMethodBinding(IMethodBinding method, List allMethods) {
		for (int i= 0; i < allMethods.size(); i++) {
			IMethodBinding curr= (IMethodBinding) allMethods.get(i);
			if (Bindings.isEqualMethod(method, curr.getName(), curr.getParameterTypes())) {
				return curr;
			}
		}
		return null;
	}

	private static IMethodBinding findOverridingMethod(IMethodBinding method, List allMethods) {
		for (int i= 0; i < allMethods.size(); i++) {
			IMethodBinding curr= (IMethodBinding) allMethods.get(i);
			if (Bindings.areOverriddenMethods(curr, method) || Bindings.isEqualMethod(curr, method.getName(), method.getParameterTypes()))
				return curr;
		}
		return null;
	}

	private static void findUnimplementedInterfaceMethods(ITypeBinding typeBinding, HashSet visited, ArrayList allMethods, IPackageBinding currPack, ArrayList toImplement) {
		if (visited.add(typeBinding)) {
			IMethodBinding[] typeMethods= typeBinding.getDeclaredMethods();
			for (int i= 0; i < typeMethods.length; i++) {
				IMethodBinding curr= typeMethods[i];
				IMethodBinding impl= findMethodBinding(curr, allMethods);
				if (impl == null || !Bindings.isVisibleInHierarchy(impl, currPack) || ((curr.getExceptionTypes().length < impl.getExceptionTypes().length) && !Modifier.isFinal(impl.getModifiers()))) {
					if (impl != null)
						allMethods.remove(impl);
					toImplement.add(curr);
					allMethods.add(curr);
				}
			}
			ITypeBinding[] superInterfaces= typeBinding.getInterfaces();
			for (int i= 0; i < superInterfaces.length; i++)
				findUnimplementedInterfaceMethods(superInterfaces[i], visited, allMethods, currPack, toImplement);
		}
	}

	public static IBinding[][] getDelegatableMethods(ITypeBinding binding) {
		List allTuples= new ArrayList();
		IVariableBinding[] typeFields= binding.getDeclaredFields();
		for (int index= 0; index < typeFields.length; index++) {
			IVariableBinding fieldBinding= typeFields[index];
			if (fieldBinding.isField() && !fieldBinding.isEnumConstant() && !fieldBinding.isSynthetic()) {
				ITypeBinding typeBinding= fieldBinding.getType();
				if (typeBinding.isTypeVariable()) {
					ITypeBinding[] typeBounds= typeBinding.getTypeBounds();
					for (int offset= 0; offset < typeBounds.length; offset++) {
						IMethodBinding[] candidates= getDelegateCandidates(typeBounds[offset], binding);
						for (int candidate= 0; candidate < candidates.length; candidate++)
							allTuples.add(new IBinding[] { fieldBinding, candidates[candidate]});
					}
				} else {
					IMethodBinding[] candidates= getDelegateCandidates(typeBinding, binding);
					for (int candidate= 0; candidate < candidates.length; candidate++)
						allTuples.add(new IBinding[] { fieldBinding, candidates[candidate]});
				}
			}
		}
		// list of tuple<IVariableBinding, IMethodBinding>
		return (IBinding[][]) allTuples.toArray(new IBinding[allTuples.size()][2]);
	}

	private static IMethodBinding[] getDelegateCandidates(ITypeBinding binding, ITypeBinding hierarchy) {
		List allMethods= new ArrayList();
		boolean isInterface= binding.isInterface();
		IMethodBinding[] typeMethods= binding.getDeclaredMethods();
		for (int index= 0; index < typeMethods.length; index++) {
			final int modifiers= typeMethods[index].getModifiers();
			if (!typeMethods[index].isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isFinal((modifiers)) && (isInterface || Modifier.isPublic(modifiers))) {
				ITypeBinding[] parameterBindings= typeMethods[index].getParameterTypes();
				boolean upper= false;
				for (int offset= 0; offset < parameterBindings.length; offset++) {
					if (parameterBindings[offset].isWildcardType() && parameterBindings[offset].isUpperbound())
						upper= true;
				}
				if (!upper)
					allMethods.add(typeMethods[index]);
			}
		}
		return (IMethodBinding[]) allMethods.toArray(new IMethodBinding[allMethods.size()]);
	}

	private static String getMethodComment(ICompilationUnit cu, String typeName, MethodDeclaration decl, IMethodBinding overridden, String lineDelimiter) throws CoreException {
		if (overridden != null) {
			overridden= overridden.getMethodDeclaration();
			String declaringClassQualifiedName= overridden.getDeclaringClass().getQualifiedName();
			String[] parameterTypesQualifiedNames= getParameterTypesQualifiedNames(overridden);
			return StubUtility.getMethodComment(cu, typeName, decl, true, overridden.isDeprecated(), declaringClassQualifiedName, parameterTypesQualifiedNames, lineDelimiter);
		} else {
			return StubUtility.getMethodComment(cu, typeName, decl, false, false, null, null, lineDelimiter);
		}
	}

	public static IMethodBinding[] getOverridableMethods(ITypeBinding typeBinding, boolean isSubType) {
		List allMethods= new ArrayList();
		IMethodBinding[] typeMethods= typeBinding.getDeclaredMethods();
		for (int index= 0; index < typeMethods.length; index++) {
			final int modifiers= typeMethods[index].getModifiers();
			if (!typeMethods[index].isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers))
				allMethods.add(typeMethods[index]);
		}
		ITypeBinding clazz= typeBinding.getSuperclass();
		while (clazz != null) {
			IMethodBinding[] methods= clazz.getDeclaredMethods();
			for (int offset= 0; offset < methods.length; offset++) {
				final int modifiers= methods[offset].getModifiers();
				if (!methods[offset].isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
					if (findOverridingMethod(methods[offset], allMethods) == null)
						allMethods.add(methods[offset]);
				}
			}
			clazz= clazz.getSuperclass();
		}
		clazz= typeBinding;
		while (clazz != null) {
			ITypeBinding[] superInterfaces= clazz.getInterfaces();
			for (int index= 0; index < superInterfaces.length; index++) {
				IMethodBinding[] methods= superInterfaces[index].getDeclaredMethods();
				for (int offset= 0; offset < methods.length; offset++) {
					final int modifiers= methods[offset].getModifiers();
					if (!methods[offset].isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
						if (findOverridingMethod(methods[offset], allMethods) == null && !Modifier.isStatic(modifiers))
							allMethods.add(methods[offset]);
					}
				}
			}
			clazz= clazz.getSuperclass();
		}
		if (!isSubType)
			allMethods.removeAll(Arrays.asList(typeMethods));
		int modifiers= 0;
		for (int index= allMethods.size() - 1; index >= 0; index--) {
			IMethodBinding method= (IMethodBinding) allMethods.get(index);
			modifiers= method.getModifiers();
			if (Modifier.isFinal(modifiers))
				allMethods.remove(index);
		}
		return (IMethodBinding[]) allMethods.toArray(new IMethodBinding[allMethods.size()]);
	}

	private static String[] getParameterTypesQualifiedNames(IMethodBinding binding) {
		ITypeBinding[] typeBindings= binding.getParameterTypes();
		String[] result= new String[typeBindings.length];
		for (int i= 0; i < result.length; i++) {
			if (typeBindings[i].isTypeVariable())
				result[i]= typeBindings[i].getName();
			else {
				if (binding.isVarargs() && typeBindings[i].isArray() && i == typeBindings.length - 1) {
					StringBuffer buffer= new StringBuffer(typeBindings[i].getElementType().getQualifiedName());
					for (int dim= 1; dim < typeBindings[i].getDimensions(); dim++)
						buffer.append("[]"); //$NON-NLS-1$
					buffer.append("..."); //$NON-NLS-1$
					result[i]= buffer.toString();
				} else
					result[i]= typeBindings[i].getTypeDeclaration().getQualifiedName();
			}
		}
		return result;
	}

	public static IMethodBinding[] getUnimplementedMethods(ITypeBinding typeBinding) {
		ArrayList allMethods= new ArrayList();
		ArrayList toImplement= new ArrayList();

		IMethodBinding[] typeMethods= typeBinding.getDeclaredMethods();
		for (int i= 0; i < typeMethods.length; i++) {
			IMethodBinding curr= typeMethods[i];
			int modifiers= curr.getModifiers();
			if (!curr.isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
				allMethods.add(curr);
			}
		}

		ITypeBinding superClass= typeBinding.getSuperclass();
		while (superClass != null) {
			typeMethods= superClass.getDeclaredMethods();
			for (int i= 0; i < typeMethods.length; i++) {
				IMethodBinding curr= typeMethods[i];
				int modifiers= curr.getModifiers();
				if (!curr.isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
					if (findMethodBinding(curr, allMethods) == null) {
						allMethods.add(curr);
					}
				}
			}
			superClass= superClass.getSuperclass();
		}

		for (int i= 0; i < allMethods.size(); i++) {
			IMethodBinding curr= (IMethodBinding) allMethods.get(i);
			int modifiers= curr.getModifiers();
			if ((Modifier.isAbstract(modifiers) || curr.getDeclaringClass().isInterface()) && (typeBinding != curr.getDeclaringClass())) {
				// implement all abstract methods
				toImplement.add(curr);
			}
		}

		HashSet visited= new HashSet();
		ITypeBinding curr= typeBinding;
		while (curr != null) {
			ITypeBinding[] superInterfaces= curr.getInterfaces();
			for (int i= 0; i < superInterfaces.length; i++) {
				findUnimplementedInterfaceMethods(superInterfaces[i], visited, allMethods, typeBinding.getPackage(), toImplement);
			}
			curr= curr.getSuperclass();
		}

		return (IMethodBinding[]) toImplement.toArray(new IMethodBinding[toImplement.size()]);
	}

	public static IMethodBinding[] getVisibleConstructors(ITypeBinding binding) {
		List constructorMethods= new ArrayList();
		ITypeBinding superType= binding.getSuperclass();
		if (superType == null)
			return new IMethodBinding[0];
		IMethodBinding[] superMethods= superType.getDeclaredMethods();
		boolean constuctorFound= false;
		IMethodBinding[] methods= binding.getDeclaredMethods();
		for (int index= 0; index < superMethods.length; index++) {
			IMethodBinding method= superMethods[index];
			if (method.isConstructor()) {
				constuctorFound= true;
				if (Bindings.isVisibleInHierarchy(method, binding.getPackage()) && !Bindings.containsSignatureEquivalentConstructor(methods, method))
					constructorMethods.add(method);
			}
		}
		if (!constuctorFound) {
			superType= binding;
			while (superType.getSuperclass() != null)
				superType= superType.getSuperclass();
			IMethodBinding method= Bindings.findMethodInType(superType, "Object", new ITypeBinding[0]); //$NON-NLS-1$
			if (!Bindings.containsOverridingMethod(methods, method))
				constructorMethods.add(method);
		}
		return (IMethodBinding[]) constructorMethods.toArray(new IMethodBinding[constructorMethods.size()]);
	}

	private static String[] suggestArgumentNames(IJavaProject project, IMethodBinding binding) {
		int nParams= binding.getParameterTypes().length;
		if (nParams > 0) {
			try {
				IMethod method= Bindings.findMethod(binding, project);
				if (method != null) {
					return StubUtility.suggestArgumentNames(project, method.getParameterNames());
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		String[] names= new String[nParams];
		for (int i= 0; i < names.length; i++) {
			names[i]= "arg" + i; //$NON-NLS-1$
		}
		return names;
	}

	/**
	 * Creates a new stub utility.
	 */
	private StubUtility2() {
		// Not for instantiation
	}
}