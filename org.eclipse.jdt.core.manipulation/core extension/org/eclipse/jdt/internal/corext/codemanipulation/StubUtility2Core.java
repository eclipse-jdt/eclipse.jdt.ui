/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - copied and pared down to methods needed by jdt.core.manipulation
 *     Microsoft Corporation - copied methods needed by jdt.core.manipulation
 *     Microsoft Corporation - read formatting options from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CodeGeneration;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation.DelegateEntry;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManagerCore;
import org.eclipse.jdt.internal.ui.util.ASTHelper;

/**
 * Utilities for code generation based on AST rewrite.
 *
 * @since 1.10
 */
public final class StubUtility2Core {

	/* This method should work with all AST levels. */
	public static MethodDeclaration createImplementationStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context,
			IMethodBinding binding, ITypeBinding targetType, CodeGenerationSettings settings, boolean inInterface, ASTNode astNode) throws CoreException {
		return createImplementationStub(unit, rewrite, imports, context, binding, null, targetType, settings, inInterface, astNode);
	}

	public static MethodDeclaration createImplementationStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context,
			IMethodBinding binding, String[] parameterNames, ITypeBinding targetType, CodeGenerationSettings settings, boolean inInterface, ASTNode astNode) throws CoreException {
		Assert.isNotNull(imports);
		Assert.isNotNull(rewrite);

		AST ast= rewrite.getAST();
		String type= Bindings.getTypeQualifiedName(targetType);

		IJavaProject javaProject= unit.getJavaProject();
		EnumSet<TypeLocation> nullnessDefault= null;
		if (astNode != null && JavaCore.ENABLED.equals(javaProject.getOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, true)))
			nullnessDefault= RedundantNullnessTypeAnnotationsFilter.determineNonNullByDefaultLocations(astNode, RedundantNullnessTypeAnnotationsFilter.determineNonNullByDefaultNames(javaProject));

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(StubUtility2Core.getImplementationModifiers(ast, binding, inInterface, imports, context, nullnessDefault));

		decl.setName(ast.newSimpleName(binding.getName()));
		decl.setConstructor(false);

		ITypeBinding bindingReturnType= binding.getReturnType();
		bindingReturnType = StubUtility2Core.replaceWildcardsAndCaptures(bindingReturnType);

		if (JavaModelUtil.is50OrHigher(javaProject)) {
			StubUtility2Core.createTypeParameters(imports, context, ast, binding, decl);

		} else {
			bindingReturnType= bindingReturnType.getErasure();
		}

		decl.setReturnType2(imports.addImport(bindingReturnType, ast, context, TypeLocation.RETURN_TYPE));

		List<SingleVariableDeclaration> parameters= StubUtility2Core.createParameters(javaProject, imports, context, ast, binding, parameterNames, decl, nullnessDefault);

		StubUtility2Core.createThrownExceptions(decl, binding, imports, context, ast);

		String delimiter= unit.findRecommendedLineSeparator();
		int modifiers= binding.getModifiers();
		ITypeBinding declaringType= binding.getDeclaringClass();
		ITypeBinding typeObject= ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
		if (!inInterface || (declaringType != typeObject && JavaModelUtil.is1d8OrHigher(javaProject))) {
			// generate a method body

			Map<String, String> options= FormatterProfileManagerCore.getProjectSettings(javaProject);

			Block body= ast.newBlock();
			decl.setBody(body);

			String bodyStatement= ""; //$NON-NLS-1$
			if (Modifier.isAbstract(modifiers)) {
				Expression expression= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType2(), bindingReturnType, decl.getExtraDimensions());
				if (expression != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);
				}
			} else {
				SuperMethodInvocation invocation= ast.newSuperMethodInvocation();
				if (declaringType.isInterface()) {
					ITypeBinding supertype= Bindings.findImmediateSuperTypeInHierarchy(targetType, declaringType.getTypeDeclaration().getQualifiedName());
					if (supertype == null) { // should not happen, but better use the type we have rather than failing
						supertype= declaringType;
					}
					if (supertype.isInterface()) {
						String qualifier= imports.addImport(supertype.getTypeDeclaration(), context);
						Name name= ASTNodeFactory.newName(ast, qualifier);
						invocation.setQualifier(name);
					}
				}
				invocation.setName(ast.newSimpleName(binding.getName()));
				SingleVariableDeclaration varDecl= null;
				for (Iterator<SingleVariableDeclaration> iterator= parameters.iterator(); iterator.hasNext();) {
					varDecl= iterator.next();
					invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
				}
				Expression expression= invocation;
				Type returnType= decl.getReturnType2();
				if (returnType instanceof PrimitiveType && ((PrimitiveType) returnType).getPrimitiveTypeCode().equals(PrimitiveType.VOID)) {
					bodyStatement= ASTNodes.asFormattedString(ast.newExpressionStatement(expression), 0, delimiter, options);
				} else {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);
				}
			}

			String placeHolder= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), false, bodyStatement, delimiter);
			if (placeHolder != null) {
				ReturnStatement todoNode= (ReturnStatement) rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				body.statements().add(todoNode);
			}
		}

		if (settings != null && settings.createComments) {
			String string= CodeGeneration.getMethodComment(unit, type, decl, binding, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}

		// According to JLS8 9.2, an interface doesn't implicitly declare non-public members of Object,
		// and JLS8 9.6.4.4 doesn't allow @Override for these methods (clone and finalize).
		boolean skipOverride= inInterface && declaringType == typeObject && !Modifier.isPublic(modifiers);

		if (!skipOverride) {
			StubUtility2Core.addOverrideAnnotation(settings, javaProject, rewrite, imports, decl, binding.getDeclaringClass().isInterface(), null);
		}

		return decl;
	}


	public static MethodDeclaration createConstructorStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context, IMethodBinding binding, String type, int modifiers, boolean omitSuperForDefConst, boolean todo, CodeGenerationSettings settings) throws CoreException {
		return StubUtility2Core.createConstructorStub(unit, rewrite, imports, context, binding, type, modifiers, omitSuperForDefConst, todo, settings, FormatterProfileManagerCore.getProjectSettings(unit.getJavaProject()));
	}


	/* This method should work with all AST levels. */
	public static MethodDeclaration createConstructorStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context, IMethodBinding binding, String type,
			int modifiers, boolean omitSuperForDefConst, boolean todo, CodeGenerationSettings settings, Map<String, String> formatSettings) throws CoreException {
		AST ast= rewrite.getAST();
		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers & ~Modifier.ABSTRACT & ~Modifier.NATIVE));
		decl.setName(ast.newSimpleName(type));
		decl.setConstructor(true);

		StubUtility2Core.createTypeParameters(imports, context, ast, binding, decl);

		List<SingleVariableDeclaration> parameters= StubUtility2Core.createParameters(unit.getJavaProject(), imports, context, ast, binding, null, decl);

		StubUtility2Core.createThrownExceptions(decl, binding, imports, context, ast);

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);
		String bodyStatement= ""; //$NON-NLS-1$
		if (!omitSuperForDefConst || !parameters.isEmpty()) {
			SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
			SingleVariableDeclaration varDecl= null;
			for (Iterator<SingleVariableDeclaration> iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			bodyStatement= ASTNodes.asFormattedString(invocation, 0, delimiter, formatSettings == null ? unit.getOptions(true) : formatSettings);
		}

		if (todo) {
			String placeHolder= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), true, bodyStatement, delimiter);
			if (placeHolder != null) {
				ReturnStatement todoNode= (ReturnStatement) rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				body.statements().add(todoNode);
			}
		} else {
			ReturnStatement statementNode= (ReturnStatement) rewrite.createStringPlaceholder(bodyStatement, ASTNode.RETURN_STATEMENT);
			body.statements().add(statementNode);
		}

		if (settings != null && settings.createComments) {
			String string= CodeGeneration.getMethodComment(unit, type, decl, binding, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	public static MethodDeclaration createConstructorStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context, ITypeBinding typeBinding, IMethodBinding superConstructor, IVariableBinding[] variableBindings, int modifiers, CodeGenerationSettings settings) throws CoreException {
		AST ast= rewrite.getAST();

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers & ~Modifier.ABSTRACT & ~Modifier.NATIVE));
		decl.setName(ast.newSimpleName(typeBinding.getName()));
		decl.setConstructor(true);

		List<SingleVariableDeclaration> parameters= decl.parameters();
		if (superConstructor != null) {
			createTypeParameters(imports, context, ast, superConstructor, decl);

			createParameters(unit.getJavaProject(), imports, context, ast, superConstructor, null, decl);

			createThrownExceptions(decl, superConstructor, imports, context, ast);
		}

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);

		if (superConstructor != null) {
			SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
			SingleVariableDeclaration varDecl= null;
			for (Iterator<SingleVariableDeclaration> iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			body.statements().add(invocation);
		}

		List<String> prohibited= new ArrayList<>();
		for (SingleVariableDeclaration singleVariableDeclaration : parameters)
			prohibited.add(singleVariableDeclaration.getName().getIdentifier());
		String param= null;
		List<String> list= new ArrayList<>(prohibited);
		String[] excluded= null;
		for (IVariableBinding variableBinding : variableBindings) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			var.setType(imports.addImport(variableBinding.getType(), ast, context, TypeLocation.PARAMETER));
			excluded= new String[list.size()];
			list.toArray(excluded);
			param= suggestParameterName(unit, variableBinding, excluded);
			list.add(param);
			var.setName(ast.newSimpleName(param));
			parameters.add(var);
		}

		list= new ArrayList<>(prohibited);
		for (IVariableBinding variableBinding : variableBindings) {
			excluded= new String[list.size()];
			list.toArray(excluded);
			final String paramName= suggestParameterName(unit, variableBinding, excluded);
			list.add(paramName);
			final String fieldName= variableBinding.getName();
			Expression expression= null;
			if (paramName.equals(fieldName) || settings.useKeywordThis) {
				FieldAccess access= ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName(fieldName));
				expression= access;
			} else
				expression= ast.newSimpleName(fieldName);
			Assignment assignment= ast.newAssignment();
			assignment.setLeftHandSide(expression);
			assignment.setRightHandSide(ast.newSimpleName(paramName));
			assignment.setOperator(Assignment.Operator.ASSIGN);
			body.statements().add(ast.newExpressionStatement(assignment));
		}

		if (settings != null && settings.createComments) {
			String string= CodeGeneration.getMethodComment(unit, typeBinding.getName(), decl, superConstructor, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	public static MethodDeclaration createDelegationStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context, IMethodBinding delegate, IVariableBinding delegatingField, CodeGenerationSettings settings) throws CoreException {
		Assert.isNotNull(delegate);
		Assert.isNotNull(delegatingField);
		Assert.isNotNull(settings);

		AST ast= rewrite.getAST();

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, delegate.getModifiers() & ~Modifier.DEFAULT & ~Modifier.SYNCHRONIZED & ~Modifier.ABSTRACT & ~Modifier.NATIVE));

		decl.setName(ast.newSimpleName(delegate.getName()));
		decl.setConstructor(false);

		createTypeParameters(imports, context, ast, delegate, decl);

		decl.setReturnType2(imports.addImport(delegate.getReturnType(), ast, context, TypeLocation.RETURN_TYPE));

		List<SingleVariableDeclaration> params= createParameters(unit.getJavaProject(), imports, context, ast, delegate, null, decl);

		createThrownExceptions(decl, delegate, imports, context, ast);

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);

		Statement statement= null;
		MethodInvocation invocation= ast.newMethodInvocation();
		invocation.setName(ast.newSimpleName(delegate.getName()));
		List<Expression> arguments= invocation.arguments();
		for (SingleVariableDeclaration param : params)
			arguments.add(ast.newSimpleName(param.getName().getIdentifier()));
		if (settings.useKeywordThis) {
			FieldAccess access= ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName(delegatingField.getName()));
			invocation.setExpression(access);
		} else
			invocation.setExpression(ast.newSimpleName(delegatingField.getName()));
		if (delegate.getReturnType().isPrimitive() && "void".equals(delegate.getReturnType().getName())) {//$NON-NLS-1$
			statement= ast.newExpressionStatement(invocation);
		} else {
			ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(invocation);
			statement= returnStatement;
		}
		body.statements().add(statement);

		ITypeBinding declaringType= delegatingField.getDeclaringClass();
		if (declaringType == null) { // can be null for
			return decl;
		}

		String qualifiedName= declaringType.getQualifiedName();
		IPackageBinding packageBinding= declaringType.getPackage();
		if (packageBinding != null) {
			if (packageBinding.getName().length() > 0 && qualifiedName.startsWith(packageBinding.getName()))
				qualifiedName= qualifiedName.substring(packageBinding.getName().length());
		}

		if (settings.createComments) {
			/*
			 * TODO: have API for delegate method comments This is an inlined
			 * version of
			 * {@link CodeGeneration#getMethodComment(ICompilationUnit, String, MethodDeclaration, IMethodBinding, String)}
			 */
			delegate= delegate.getMethodDeclaration();
			String declaringClassQualifiedName= delegate.getDeclaringClass().getQualifiedName();
			String linkToMethodName= delegate.getName();
			String[] parameterTypesQualifiedNames= StubUtility.getParameterTypeNamesForSeeTag(delegate);
			String string= StubUtility.getMethodComment(unit, qualifiedName, decl, delegate.isDeprecated(), linkToMethodName, declaringClassQualifiedName, parameterTypesQualifiedNames, true, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	public static MethodDeclaration createImplementationStubCore(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context,
			IMethodBinding binding, ITypeBinding targetType, CodeGenerationSettings settings, boolean inInterface,
			ASTNode astNode, boolean snippetStringSupport) throws CoreException {
		return createImplementationStubCore(unit, rewrite, imports, context, binding, null, targetType, settings,
				inInterface, astNode, snippetStringSupport);
	}

	public static MethodDeclaration createImplementationStubCore(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context,
			IMethodBinding binding, String[] parameterNames, ITypeBinding targetType, CodeGenerationSettings settings, boolean inInterface, ASTNode astNode, boolean snippetStringSupport) throws CoreException {
		return createImplementationStubCore(unit, rewrite, imports, context, binding, parameterNames, targetType, settings,
				inInterface, false, astNode, snippetStringSupport);
	}

	public static MethodDeclaration createImplementationStubCore(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context,
			IMethodBinding binding, String[] parameterNames, ITypeBinding targetType, CodeGenerationSettings settings, boolean inInterface, boolean useAlternativeMethodBody, ASTNode astNode, boolean snippetStringSupport) throws CoreException {
		Assert.isNotNull(imports);
		Assert.isNotNull(rewrite);

		AST ast= rewrite.getAST();
		String type= Bindings.getTypeQualifiedName(targetType);

		IJavaProject javaProject= unit.getJavaProject();
		EnumSet<TypeLocation> nullnessDefault= null;
		if (astNode != null && JavaCore.ENABLED.equals(javaProject.getOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, true))) {
			nullnessDefault= RedundantNullnessTypeAnnotationsFilter.determineNonNullByDefaultLocations(astNode, RedundantNullnessTypeAnnotationsFilter.determineNonNullByDefaultNames(javaProject));
		}

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(StubUtility2Core.getImplementationModifiers(ast, binding, inInterface, imports, context, nullnessDefault));

		decl.setName(ast.newSimpleName(binding.getName()));
		decl.setConstructor(false);

		ITypeBinding bindingReturnType= binding.getReturnType();
		bindingReturnType= StubUtility2Core.replaceWildcardsAndCaptures(bindingReturnType);

		if (JavaModelUtil.is50OrHigher(javaProject)) {
			StubUtility2Core.createTypeParameters(imports, context, ast, binding, decl);

		} else {
			bindingReturnType= bindingReturnType.getErasure();
		}

		decl.setReturnType2(imports.addImport(bindingReturnType, ast, context, TypeLocation.RETURN_TYPE));

		List<SingleVariableDeclaration> parameters= StubUtility2Core.createParameters(javaProject, imports, context, ast, binding, parameterNames, decl, nullnessDefault);

		StubUtility2Core.createThrownExceptions(decl, binding, imports, context, ast);

		String delimiter= unit.findRecommendedLineSeparator();
		int modifiers= binding.getModifiers();
		ITypeBinding declaringType= binding.getDeclaringClass();
		ITypeBinding typeObject= ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
		if (!inInterface || (declaringType != typeObject && JavaModelUtil.is1d8OrHigher(javaProject))) {
			// generate a method body

			Map<String, String> options= unit.getOptions(true);

			Block body= ast.newBlock();
			decl.setBody(body);

			String bodyStatement= ""; //$NON-NLS-1$
			if (Modifier.isAbstract(modifiers)) {
				Expression expression= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType2(), bindingReturnType, decl.getExtraDimensions());
				if (expression != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);
				}
			} else {
				SuperMethodInvocation invocation= ast.newSuperMethodInvocation();
				if (declaringType.isInterface()) {
					ITypeBinding supertype= Bindings.findImmediateSuperTypeInHierarchy(targetType, declaringType.getTypeDeclaration().getQualifiedName());
					if (supertype == null) { // should not happen, but better use the type we have rather than failing
						supertype= declaringType;
					}
					if (supertype.isInterface()) {
						String qualifier= imports.addImport(supertype.getTypeDeclaration(), context);
						Name name= ASTNodeFactory.newName(ast, qualifier);
						invocation.setQualifier(name);
					}
				}
				invocation.setName(ast.newSimpleName(binding.getName()));

				for (SingleVariableDeclaration varDecl : parameters) {
					invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
				}
				Expression expression= invocation;
				Type returnType= decl.getReturnType2();
				if (returnType instanceof PrimitiveType && ((PrimitiveType) returnType).getPrimitiveTypeCode().equals(PrimitiveType.VOID)) {
					bodyStatement= ASTNodes.asFormattedString(ast.newExpressionStatement(expression), 0, delimiter, options);
				} else {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);
				}
			}

			if (bodyStatement != null) {
				StringBuilder placeHolder= new StringBuilder();
				if (snippetStringSupport) {
					final String ESCAPE_DOLLAR= "\\\\\\$"; //$NON-NLS-1$
					final String DOLLAR= "\\$"; //$NON-NLS-1$
					bodyStatement= bodyStatement.replaceAll(DOLLAR, ESCAPE_DOLLAR);
				}
				String bodyContent= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), false, useAlternativeMethodBody, bodyStatement, delimiter);

				if (snippetStringSupport) {
					placeHolder.append("${0"); //$NON-NLS-1$
					if (bodyContent != null) {
						placeHolder.append(":"); //$NON-NLS-1$
						placeHolder.append(bodyContent);
					}
					placeHolder.append("}"); //$NON-NLS-1$
				} else {
					if (bodyContent != null) {
						placeHolder.append(bodyContent);
					}
				}

				if (bodyContent != null || snippetStringSupport) {
					ReturnStatement todoNode= (ReturnStatement) rewrite.createStringPlaceholder(placeHolder.toString(), ASTNode.RETURN_STATEMENT);
					body.statements().add(todoNode);
				}
			}
		}

		if (settings != null && settings.createComments) {
			String string= CodeGeneration.getMethodComment(unit, type, decl, binding, delimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createStringPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}

		// According to JLS8 9.2, an interface doesn't implicitly declare non-public members of Object,
		// and JLS8 9.6.4.4 doesn't allow @Override for these methods (clone and finalize).
		boolean skipOverride= inInterface && declaringType == typeObject && !Modifier.isPublic(modifiers);

		if (!skipOverride) {
			StubUtility2Core.addOverrideAnnotation(settings, javaProject, rewrite, imports, decl, binding.getDeclaringClass().isInterface(), null);
		}
		return decl;
	}

	public static void createTypeParameters(ImportRewrite imports, ImportRewriteContext context, AST ast, IMethodBinding binding, MethodDeclaration decl) {
		List<TypeParameter> typeParameters= decl.typeParameters();
		for (ITypeBinding curr : binding.getTypeParameters()) {
			TypeParameter newTypeParam= ast.newTypeParameter();
			newTypeParam.setName(ast.newSimpleName(curr.getName()));
			ITypeBinding[] typeBounds= curr.getTypeBounds();
			if (typeBounds.length != 1 || !"java.lang.Object".equals(typeBounds[0].getQualifiedName())) { //$NON-NLS-1$
				List<Type> newTypeBounds= newTypeParam.typeBounds();
				for (ITypeBinding typeBound : typeBounds) {
					newTypeBounds.add(imports.addImport(typeBound, ast, context, TypeLocation.TYPE_BOUND));
				}
			}
			typeParameters.add(newTypeParam);
		}
	}

	public static List<SingleVariableDeclaration> createParameters(IJavaProject project, ImportRewrite imports, ImportRewriteContext context, AST ast, IMethodBinding binding, String[] paramNames, MethodDeclaration decl) {
		return createParameters(project, imports, context, ast, binding, paramNames, decl, null);
	}
	public static List<SingleVariableDeclaration> createParameters(IJavaProject project, ImportRewrite imports, ImportRewriteContext context, AST ast,
			IMethodBinding binding, String[] paramNames, MethodDeclaration decl, EnumSet<TypeLocation> nullnessDefault) {
		boolean is50OrHigher= JavaModelUtil.is50OrHigher(project);
		List<SingleVariableDeclaration> parameters= decl.parameters();
		ITypeBinding[] params= binding.getParameterTypes();
		if (paramNames == null || paramNames.length < params.length) {
			paramNames= StubUtility.suggestArgumentNames(project, binding);
		}
		for (int i= 0; i < params.length; i++) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			ITypeBinding type= params[i];
			type=replaceWildcardsAndCaptures(type);
			if (!is50OrHigher) {
				type= type.getErasure();
				var.setType(imports.addImport(type, ast, context, TypeLocation.PARAMETER));
			} else if (binding.isVarargs() && type.isArray() && i == params.length - 1) {
				var.setVarargs(true);
				/*
				 * Varargs annotations are special.
				 * Example:
				 *     foo(@O Object @A [] @B ... arg)
				 * => @B is not an annotation on the array dimension that constitutes the vararg.
				 * It's the type annotation of the *innermost* array dimension.
				 */
				int dimensions= type.getDimensions();
				@SuppressWarnings("unchecked")
				List<Annotation>[] dimensionAnnotations= (List<Annotation>[]) new List<?>[dimensions];
				for (int dim= 0; dim < dimensions; dim++) {
					dimensionAnnotations[dim]= new ArrayList<>();
					for (IAnnotationBinding annotation : type.getTypeAnnotations()) {
						dimensionAnnotations[dim].add(imports.addAnnotation(annotation, ast, context));
					}
					type= type.getComponentType();
				}

				Type elementType= imports.addImport(type, ast, context);
				if (dimensions == 1) {
					var.setType(elementType);
				} else {
					ArrayType arrayType= ast.newArrayType(elementType, dimensions - 1);
					List<Dimension> dimensionNodes= arrayType.dimensions();
					for (int dim= 0; dim < dimensions - 1; dim++) { // all except the innermost dimension
						Dimension dimension= dimensionNodes.get(dim);
						dimension.annotations().addAll(dimensionAnnotations[dim]);
					}
					var.setType(arrayType);
				}
				List<Annotation> varargTypeAnnotations= dimensionAnnotations[dimensions - 1];
				var.varargsAnnotations().addAll(varargTypeAnnotations);
			} else {
				var.setType(imports.addImport(type, ast, context, TypeLocation.PARAMETER));
			}
			var.setName(ast.newSimpleName(paramNames[i]));
			IAnnotationBinding[] annotations= binding.getParameterAnnotations(i);
			for (IAnnotationBinding annotation : annotations) {
				if (StubUtility2Core.isCopyOnInheritAnnotation(annotation.getAnnotationType(), project, nullnessDefault, TypeLocation.PARAMETER))
					var.modifiers().add(imports.addAnnotation(annotation, ast, context));
			}
			parameters.add(var);
		}
		return parameters;
	}

	public static void createThrownExceptions(MethodDeclaration decl, IMethodBinding method, ImportRewrite imports, ImportRewriteContext context, AST ast) {
		ITypeBinding[] excTypes= method.getExceptionTypes();
		if (ast.apiLevel() >= ASTHelper.JLS8) {
			List<Type> thrownExceptions= decl.thrownExceptionTypes();
			for (ITypeBinding t : excTypes) {
				Type excType= imports.addImport(t, ast, context, TypeLocation.EXCEPTION);
				thrownExceptions.add(excType);
			}
		} else {
			List<Name> thrownExceptions= getThrownExceptions(decl);
			for (ITypeBinding excType : excTypes) {
				String excTypeName= imports.addImport(excType, context);
				thrownExceptions.add(ASTNodeFactory.newName(ast, excTypeName));
			}
		}
	}

	/**
	 * @param decl method declaration
	 * @return thrown exception names
	 * @deprecated to avoid deprecation warnings
	 */
	@Deprecated
	private static List<Name> getThrownExceptions(MethodDeclaration decl) {
		return decl.thrownExceptions();
	}


	private static IMethodBinding findOverridingMethod(IMethodBinding method, List<IMethodBinding> allMethods) {
		for (IMethodBinding curr : allMethods) {
			if (Bindings.areOverriddenMethods(curr, method) || Bindings.isSubsignature(curr, method))
				return curr;
		}
		return null;
	}

	public static List<IExtendedModifier> getImplementationModifiers(AST ast, IMethodBinding method, boolean inInterface, ImportRewrite importRewrite, ImportRewriteContext context, EnumSet<TypeLocation> nullnessDefault) throws JavaModelException {
		IJavaProject javaProject= importRewrite.getCompilationUnit().getJavaProject();
		int modifiers= method.getModifiers();
		if (inInterface) {
			modifiers= modifiers & ~Modifier.PROTECTED & ~Modifier.PUBLIC;
			if (Modifier.isAbstract(modifiers) && JavaModelUtil.is1d8OrHigher(javaProject)) {
				modifiers= modifiers | Modifier.DEFAULT;
			}
		} else {
			modifiers= modifiers & ~Modifier.DEFAULT;
		}
		modifiers= modifiers & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.PRIVATE;
		IAnnotationBinding[] annotations= method.getAnnotations();

		if (modifiers != Modifier.NONE && annotations.length > 0) {
			// need an AST of the source method to preserve order of modifiers
			IMethod iMethod= (IMethod) method.getJavaElement();
			if (iMethod != null && JavaElementUtil.isSourceAvailable(iMethod)) {
				ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
				parser.setSource(iMethod.getTypeRoot());
				parser.setIgnoreMethodBodies(true);
				CompilationUnit otherCU= (CompilationUnit) parser.createAST(null);
				ASTNode otherMethod= NodeFinder.perform(otherCU, iMethod.getSourceRange());
				if (otherMethod instanceof MethodDeclaration) {
					MethodDeclaration otherMD= (MethodDeclaration) otherMethod;
					ArrayList<IExtendedModifier> result= new ArrayList<>();
					List<IExtendedModifier> otherModifiers= otherMD.modifiers();
					for (IExtendedModifier otherModifier : otherModifiers) {
						if (otherModifier instanceof Modifier) {
							int otherFlag= ((Modifier) otherModifier).getKeyword().toFlagValue();
							if ((otherFlag & modifiers) != 0) {
								modifiers= ~otherFlag & modifiers;
								result.addAll(ast.newModifiers(otherFlag));
							}
						} else {
							Annotation otherAnnotation= (Annotation) otherModifier;
							String n= otherAnnotation.getTypeName().getFullyQualifiedName();
							for (IAnnotationBinding annotation : annotations) {
								ITypeBinding otherAnnotationType= annotation.getAnnotationType();
								String qn= otherAnnotationType.getQualifiedName();
								if (qn.endsWith(n) && (qn.length() == n.length() || qn.charAt(qn.length() - n.length() - 1) == '.')) {
									if (StubUtility2Core.isCopyOnInheritAnnotation(otherAnnotationType, javaProject, nullnessDefault, TypeLocation.RETURN_TYPE))
										result.add(importRewrite.addAnnotation(annotation, ast, context));
									break;
								}
							}
						}
					}
					result.addAll(ASTNodeFactory.newModifiers(ast, modifiers));
					return result;
				}
			}
		}

		ArrayList<IExtendedModifier> result= new ArrayList<>();

		for (IAnnotationBinding annotation : annotations) {
			if (StubUtility2Core.isCopyOnInheritAnnotation(annotation.getAnnotationType(), javaProject, nullnessDefault, TypeLocation.RETURN_TYPE))
				result.add(importRewrite.addAnnotation(annotation, ast, context));
		}

		result.addAll(ASTNodeFactory.newModifiers(ast, modifiers));

		return result;
	}

	public static DelegateEntry[] getDelegatableMethods(ITypeBinding binding) {
		final List<DelegateEntry> tuples= new ArrayList<>();
		final List<IMethodBinding> declared= new ArrayList<>();
		IMethodBinding[] typeMethods= binding.getDeclaredMethods();
		for (IMethodBinding typeMethod : typeMethods) {
			if (!typeMethod.isSyntheticRecordMethod()) {
				declared.add(typeMethod);
			}
		}
		for (IVariableBinding fieldBinding : binding.getDeclaredFields()) {
			if (fieldBinding.isField() && !fieldBinding.isEnumConstant() && !fieldBinding.isSynthetic())
				getDelegatableMethods(new ArrayList<>(declared), fieldBinding, fieldBinding.getType(), binding, tuples);
		}
		// list of tuple<IVariableBinding, IMethodBinding>
		return tuples.toArray(new DelegateEntry[tuples.size()]);
	}

	private static void getDelegatableMethods(List<IMethodBinding> methods, IVariableBinding fieldBinding, ITypeBinding typeBinding, ITypeBinding binding, List<DelegateEntry> result) {
		boolean match= false;
		if (typeBinding.isTypeVariable()) {
			ITypeBinding[] typeBounds= typeBinding.getTypeBounds();
			if (typeBounds.length > 0) {
				for (ITypeBinding typeBound : typeBounds) {
					getDelegatableMethods(methods, fieldBinding, typeBound, binding, result);
				}
			} else {
				ITypeBinding objectBinding= Bindings.findTypeInHierarchy(binding, "java.lang.Object"); //$NON-NLS-1$
				if (objectBinding != null) {
					getDelegatableMethods(methods, fieldBinding, objectBinding, binding, result);
				}
			}
		} else {
			for (IMethodBinding candidate : getDelegateCandidates(typeBinding, binding)) {
				match= false;
				final IMethodBinding methodBinding= candidate;
				for (int offset= 0; offset < methods.size() && !match; offset++) {
					if (Bindings.areOverriddenMethods(methods.get(offset), methodBinding))
						match= true;
				}
				if (!match) {
					result.add(new DelegateEntry(methodBinding, fieldBinding));
					methods.add(methodBinding);
				}
			}
			final ITypeBinding superclass= typeBinding.getSuperclass();
			if (superclass != null)
				getDelegatableMethods(methods, fieldBinding, superclass, binding, result);
			for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
				getDelegatableMethods(methods, fieldBinding, superInterface, binding, result);
			}
		}
	}

	private static IMethodBinding[] getDelegateCandidates(ITypeBinding binding, ITypeBinding hierarchy) {
		List<IMethodBinding> allMethods= new ArrayList<>();
		boolean isInterface= binding.isInterface();
		for (IMethodBinding typeMethod : binding.getDeclaredMethods()) {
			final int modifiers= typeMethod.getModifiers();
			if (!typeMethod.isConstructor() && !Modifier.isStatic(modifiers) && (isInterface || Modifier.isPublic(modifiers))) {
				IMethodBinding result= Bindings.findOverriddenMethodInHierarchy(hierarchy, typeMethod);
				if (result != null
						&& Flags.isFinal(result.getModifiers())
						&& !result.isSyntheticRecordMethod())
					continue;
				ITypeBinding[] parameterBindings= typeMethod.getParameterTypes();
				boolean upper= false;
				for (ITypeBinding parameterBinding : parameterBindings) {
					if (parameterBinding.isWildcardType() && parameterBinding.isUpperbound()) {
						upper= true;
					}
				}
				if (!upper) {
					allMethods.add(typeMethod);
				}
			}
		}
		return allMethods.toArray(new IMethodBinding[allMethods.size()]);
	}

	public static IMethodBinding[] getOverridableMethods(AST ast, ITypeBinding typeBinding, boolean isSubType) {
		List<IMethodBinding> allMethods= new ArrayList<>();
		IMethodBinding[] typeMethods= typeBinding.getDeclaredMethods();
		for (IMethodBinding typeMethod : typeMethods) {
			final int modifiers= typeMethod.getModifiers();
			if (!typeMethod.isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && !typeMethod.isSyntheticRecordMethod()) {
				allMethods.add(typeMethod);
			}
		}
		ITypeBinding clazz= typeBinding.getSuperclass();
		while (clazz != null) {
			for (IMethodBinding method : clazz.getDeclaredMethods()) {
				final int modifiers= method.getModifiers();
				if (!method.isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
					if (findOverridingMethod(method, allMethods) == null) {
						allMethods.add(method);
					}
				}
			}
			clazz= clazz.getSuperclass();
		}
		clazz= typeBinding;
		while (clazz != null) {
			for (ITypeBinding superInterface : clazz.getInterfaces()) {
				getOverridableMethods(ast, superInterface, allMethods);
			}
			clazz= clazz.getSuperclass();
		}
		if (typeBinding.isInterface())
			getOverridableMethods(ast, ast.resolveWellKnownType("java.lang.Object"), allMethods); //$NON-NLS-1$
		if (!isSubType)
			allMethods.removeAll(Arrays.asList(typeMethods));
		for (int index= allMethods.size() - 1; index >= 0; index--) {
			IMethodBinding method= allMethods.get(index);
			if (Modifier.isFinal(method.getModifiers()))
				allMethods.remove(index);
		}
		return allMethods.toArray(new IMethodBinding[allMethods.size()]);
	}

	private static void getOverridableMethods(AST ast, ITypeBinding superBinding, List<IMethodBinding> allMethods) {
		for (IMethodBinding method : superBinding.getDeclaredMethods()) {
			final int modifiers= method.getModifiers();
			if (!method.isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
				if (findOverridingMethod(method, allMethods) == null) {
					allMethods.add(method);
				}
			}
		}
		for (ITypeBinding superInterface : superBinding.getInterfaces()) {
			getOverridableMethods(ast, superInterface, allMethods);
		}
	}

	private static String suggestParameterName(ICompilationUnit unit, IVariableBinding binding, String[] excluded) {
		String name= StubUtility.getBaseName(binding, unit.getJavaProject());
		return StubUtility.suggestArgumentName(unit.getJavaProject(), name, excluded);
	}

	@SuppressWarnings("unchecked")
	public static Collection<IMethodBinding> getMethodsIn(ITypeBinding type, Predicate<IMethodBinding> ignoreTheseMethods) {
		if (type == null) {
			return Collections.EMPTY_LIST;
		}

		if (ignoreTheseMethods == null) {
			ignoreTheseMethods= m->false;
		}
		List<IMethodBinding> allMethods= new ArrayList<>();
		IMethodBinding[] declaredMethods= type.getDeclaredMethods();
		for (IMethodBinding method : declaredMethods) {
			if (!isStatic(method) && !method.isConstructor() && !isPrivate(method) && !ignoreTheseMethods.test(method)) {
				allMethods.add(method);
			}
		}

		ITypeBinding[] interfaces= type.getInterfaces();
		Collection<IMethodBinding>[] interfaceMethods;

		if (!type.isInterface()) {
			Collection<IMethodBinding> superMethods= getMethodsIn(type.getSuperclass(), ignoreTheseMethods);
			interfaceMethods= new Collection[interfaces.length + 1];
			interfaceMethods[interfaceMethods.length - 1]= superMethods;
			for (IMethodBinding method : superMethods) {
				if (isConcrete(method) && Bindings.isVisibleInHierarchy(method, type.getPackage()) && findSubSignatureMethod(method, allMethods) == null) {
					allMethods.add(method);
				}
			}
		} else {
			interfaceMethods= new Collection[interfaces.length];
		}

		for (int i= 0; i < interfaces.length; i++) {
			interfaceMethods[i]= getMethodsIn(interfaces[i], ignoreTheseMethods);
		}

		List<IMethodBinding> allInterfaceMethods= new ArrayList<>();

		for (int i= 0; i < interfaceMethods.length; i++) {
			for (IMethodBinding method : interfaceMethods[i]) {
				if (isDefault(method) || isAbstract(method)) {
					IMethodBinding previouslyFound= findSubSignatureMethod(method, allMethods);
					if (previouslyFound == null) {
						IMethodBinding subSigMethod= findSubSignatureMethod(method, allInterfaceMethods);
						IMethodBinding superSigMethod= findSuperSignatureMethod(method, allInterfaceMethods);

						if (superSigMethod != null && method.overrides(superSigMethod)) {
							allInterfaceMethods.remove(subSigMethod);
							allInterfaceMethods.add(method);
						} else {
							if (subSigMethod != null && superSigMethod != null) {
								if (!subSigMethod.getReturnType().getErasure().isSubTypeCompatible(method.getReturnType().getErasure())) {
									// keep the most specific one
									allInterfaceMethods.remove(subSigMethod);
									allInterfaceMethods.add(method);
								}
							} else if (superSigMethod != null) {
								allInterfaceMethods.remove(superSigMethod);
								allInterfaceMethods.add(method);
							} else if (subSigMethod == null) {
								allInterfaceMethods.add(method);
							}
						}
					}
				}
			}
		}

		allMethods.addAll(allInterfaceMethods);

		return allMethods;
	}

	private static IMethodBinding findSubSignatureMethod(IMethodBinding method, Collection<IMethodBinding> methods) {
		for (IMethodBinding candidate : methods) {
			if (candidate.getName().equals(method.getName()) && candidate.isSubsignature(method)) {
				return candidate;
			}
		}
		return null;
	}

	private static IMethodBinding findSuperSignatureMethod(IMethodBinding method, Collection<IMethodBinding> methods) {
		for (IMethodBinding candidate : methods) {
			if (candidate.getName().equals(method.getName()) && method.isSubsignature(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean isStatic(IMethodBinding method) {
		return Modifier.isStatic(method.getModifiers());
	}

	private static boolean isPrivate(IMethodBinding method) {
		return Modifier.isPrivate(method.getModifiers());
	}

	private static boolean isAbstract(IMethodBinding method) {
		return Modifier.isAbstract(method.getModifiers());
	}

	private static boolean isDefault(IMethodBinding method) {
		return Modifier.isDefault(method.getModifiers());
	}

	private static boolean isConcrete(IMethodBinding method) {
		return !isAbstract(method) && !isDefault(method);
	}


	public static Predicate<IMethodBinding> ignoreAbstractsOfInput(final ITypeBinding type) {
		return m->isAbstract(m) && type == m.getDeclaringClass();
	}

	public static Predicate<IMethodBinding> IMPLEMENT_RECORD_SYNTHETICS= (IMethodBinding m) -> m.isSyntheticRecordMethod();

	public static IMethodBinding[] getUnimplementedMethods(ITypeBinding typeBinding, Predicate<IMethodBinding> ignoreTheseMethods) {
		List<IMethodBinding> abstractMethods= new ArrayList<>();
		for (IMethodBinding method : getMethodsIn(typeBinding, ignoreTheseMethods)) {
			if (isAbstract(method)) {
				abstractMethods.add(method);
			}
		}
		return abstractMethods.toArray(new IMethodBinding[abstractMethods.size()]);
	}

	public static IMethodBinding[] getVisibleConstructors(ITypeBinding binding, boolean accountExisting, boolean proposeDefault) {
		List<IMethodBinding> constructorMethods= new ArrayList<>();
		List<IMethodBinding> existingConstructors= null;
		ITypeBinding superType= binding.getSuperclass();
		if (superType == null)
			return new IMethodBinding[0];
		if (accountExisting) {
			IMethodBinding[] methods= binding.getDeclaredMethods();
			existingConstructors= new ArrayList<>(methods.length);
			for (IMethodBinding method : methods) {
				if (method.isConstructor() && !method.isDefaultConstructor())
					existingConstructors.add(method);
			}
		}
		if (existingConstructors != null)
			constructorMethods.addAll(existingConstructors);
		IMethodBinding[] methods= binding.getDeclaredMethods();
		for (IMethodBinding method : superType.getDeclaredMethods()) {
			if (method.isConstructor()) {
				if (Bindings.isVisibleInHierarchy(method, binding.getPackage()) && (!accountExisting || !Bindings.containsSignatureEquivalentConstructor(methods, method)))
					constructorMethods.add(method);
			}
		}
		if (existingConstructors != null)
			constructorMethods.removeAll(existingConstructors);
		if (constructorMethods.isEmpty()) {
			superType= binding;
			while (superType.getSuperclass() != null)
				superType= superType.getSuperclass();
			IMethodBinding method= Bindings.findMethodInType(superType, "Object", new ITypeBinding[0]); //$NON-NLS-1$
			if (method != null) {
				if ((proposeDefault || !accountExisting || existingConstructors == null || existingConstructors.isEmpty()) && (!accountExisting || !Bindings.containsSignatureEquivalentConstructor(methods, method)))
					constructorMethods.add(method);
			}
		}
		return constructorMethods.toArray(new IMethodBinding[constructorMethods.size()]);
	}


	/**
	 * Evaluates the insertion position of a new node.
	 *
	 * @param listRewrite The list rewriter to which the new node will be added
	 * @param sibling The Java element before which the new element should be added.
	 * @return the AST node of the list to insert before or null to insert as last.
	 * @throws JavaModelException thrown if accessing the Java element failed
	 */

	public static ASTNode getNodeToInsertBefore(ListRewrite listRewrite, IJavaElement sibling) throws JavaModelException {
		if (sibling instanceof IMember) {
			ISourceRange sourceRange= ((IMember) sibling).getSourceRange();
			if (sourceRange == null) {
				return null;
			}
			int insertPos= sourceRange.getOffset();

			for (ASTNode curr : (List<? extends ASTNode>)listRewrite.getOriginalList()) {
				if (curr.getStartPosition() >= insertPos) {
					return curr;
				}
			}
		}
		return null;
	}

	/**
	 * Adds <code>@Override</code> annotation to <code>methodDecl</code> if not already present and
	 * if requested by code style settings or compiler errors/warnings settings.
	 *
	 * @param settings the code generation style settings, may be <code>null</code>
	 * @param project the Java project used to access the compiler settings
	 * @param rewrite the ASTRewrite
	 * @param imports the ImportRewrite
	 * @param methodDecl the method declaration to add the annotation to
	 * @param isDeclaringTypeInterface <code>true</code> if the type declaring the method overridden
	 *            by <code>methodDecl</code> is an interface
	 * @param group the text edit group, may be <code>null</code>
	 */
	public static void addOverrideAnnotation(CodeGenerationSettings settings, IJavaProject project, ASTRewrite rewrite, ImportRewrite imports, MethodDeclaration methodDecl,
			boolean isDeclaringTypeInterface, TextEditGroup group) {
		if (!JavaModelUtil.is50OrHigher(project)) {
			return;
		}
		if (isDeclaringTypeInterface) {
			String version= project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
			if (JavaModelUtil.isVersionLessThan(version, JavaCore.VERSION_1_6))
				return; // not allowed in 1.5
			if (JavaCore.DISABLED.equals(project.getOption(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION, true)))
				return; // user doesn't want to use 1.6 style
		}
		if ((settings != null && settings.overrideAnnotation) || !JavaCore.IGNORE.equals(project.getOption(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, true))) {
			createOverrideAnnotation(rewrite, imports, methodDecl, group);
		}
	}

	public static void createOverrideAnnotation(ASTRewrite rewrite, ImportRewrite imports, MethodDeclaration decl, TextEditGroup group) {
		if (findAnnotation("java.lang.Override", decl.modifiers()) != null) { //$NON-NLS-1$
			return; // No need to add duplicate annotation
		}
		AST ast= rewrite.getAST();
		ASTNode root= decl.getRoot();
		ImportRewriteContext context= null;
		if (root instanceof CompilationUnit) {
			context= new ContextSensitiveImportRewriteContext((CompilationUnit) root, decl.getStartPosition(), imports);
		}
		Annotation marker= ast.newMarkerAnnotation();
		marker.setTypeName(ast.newName(imports.addImport("java.lang.Override", context))); //$NON-NLS-1$
		rewrite.getListRewrite(decl, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(marker, group);
	}

	public static boolean isCopyOnInheritAnnotation(ITypeBinding annotationType, IJavaProject project, EnumSet<TypeLocation> nullnessDefault, TypeLocation typeLocation) {
		if (JavaCore.ENABLED.equals(project.getOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, true)))
			return false;
		if (nullnessDefault != null && Bindings.isNonNullAnnotation(annotationType, project)) {
			if (!nullnessDefault.contains(typeLocation)) {
				return true;
			}
			return false; // nonnull within the scope of @NonNullByDefault: don't copy
		}
		return Bindings.isAnyNullAnnotation(annotationType, project);
	}

	public static Annotation findAnnotation(String qualifiedTypeName, List<IExtendedModifier> modifiers) {
		for (IExtendedModifier curr : modifiers) {
			if (curr instanceof Annotation) {
				Annotation annot= (Annotation) curr;
				ITypeBinding binding= annot.getTypeName().resolveTypeBinding();
				if (binding != null && qualifiedTypeName.equals(binding.getQualifiedName())) {
					return annot;
				}
			}
		}
		return null;
	}

	public static ITypeBinding replaceWildcardsAndCaptures(ITypeBinding type) {
		while (type.isWildcardType() || type.isCapture() || (type.isArray() && type.getElementType().isCapture())) {
			ITypeBinding bound = type.getBound();
			type = (bound != null) ? bound : type.getErasure();
		}
		return type;
	}

	/**
	 * Creates a new stub utility.
	 */
	private StubUtility2Core() {
		// Not for instantiation
	}

}
