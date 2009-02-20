/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
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
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation.DelegateEntry;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.CodeGeneration;

/**
 * Utilities for code generation based on ast rewrite.
 *
 * @since 3.1
 */
public final class StubUtility2 {

	public static void addOverrideAnnotation(IJavaProject project, ASTRewrite rewrite, MethodDeclaration decl, IMethodBinding binding) {
		String version= project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		if (!binding.getDeclaringClass().isInterface() || !JavaModelUtil.isVersionLessThan(version, JavaCore.VERSION_1_6)) {
			final Annotation marker= rewrite.getAST().newMarkerAnnotation();
			marker.setTypeName(rewrite.getAST().newSimpleName("Override")); //$NON-NLS-1$
			rewrite.getListRewrite(decl, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(marker, null);
		}
	}

	public static MethodDeclaration createConstructorStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, IMethodBinding binding, String type, int modifiers, boolean omitSuperForDefConst, boolean todo, CodeGenerationSettings settings) throws CoreException {
		AST ast= rewrite.getAST();
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

		List parameters= createParameters(unit.getJavaProject(), imports, null, ast, binding, decl);

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
		if (!omitSuperForDefConst || !parameters.isEmpty()) {
			SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
			SingleVariableDeclaration varDecl= null;
			for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= (SingleVariableDeclaration) iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			bodyStatement= ASTNodes.asFormattedString(invocation, 0, delimiter, unit.getJavaProject().getOptions(true));
		}

		if (todo) {
			String placeHolder= CodeGeneration.getMethodBodyContent(unit, type, binding.getName(), true, bodyStatement, delimiter);
			if (placeHolder != null) {
				ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				body.statements().add(todoNode);
			}
		} else {
			ASTNode statementNode= rewrite.createStringPlaceholder(bodyStatement, ASTNode.RETURN_STATEMENT);
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

	public static MethodDeclaration createConstructorStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ITypeBinding typeBinding, IMethodBinding superConstructor, IVariableBinding[] variableBindings, int modifiers, CodeGenerationSettings settings) throws CoreException {
		AST ast= rewrite.getAST();

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

			createParameters(unit.getJavaProject(), imports, null, ast, superConstructor, decl);

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

		if (superConstructor != null) {
			SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();
			SingleVariableDeclaration varDecl= null;
			for (Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				varDecl= (SingleVariableDeclaration) iterator.next();
				invocation.arguments().add(ast.newSimpleName(varDecl.getName().getIdentifier()));
			}
			body.statements().add(invocation);
		}

		List prohibited= new ArrayList();
		for (final Iterator iterator= parameters.iterator(); iterator.hasNext();)
			prohibited.add(((SingleVariableDeclaration) iterator.next()).getName().getIdentifier());
		String param= null;
		List list= new ArrayList(prohibited);
		String[] excluded= null;
		for (int i= 0; i < variableBindings.length; i++) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			var.setType(imports.addImport(variableBindings[i].getType(), ast));
			excluded= new String[list.size()];
			list.toArray(excluded);
			param= suggestParameterName(unit, variableBindings[i], excluded);
			list.add(param);
			var.setName(ast.newSimpleName(param));
			parameters.add(var);
		}

		list= new ArrayList(prohibited);
		for (int i= 0; i < variableBindings.length; i++) {
			excluded= new String[list.size()];
			list.toArray(excluded);
			final String paramName= suggestParameterName(unit, variableBindings[i], excluded);
			list.add(paramName);
			final String fieldName= variableBindings[i].getName();
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
		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, delegate.getModifiers() & ~Modifier.SYNCHRONIZED & ~Modifier.ABSTRACT & ~Modifier.NATIVE));

		decl.setName(ast.newSimpleName(delegate.getName()));
		decl.setConstructor(false);

		ITypeBinding[] typeParams= delegate.getTypeParameters();
		List typeParameters= decl.typeParameters();
		for (int i= 0; i < typeParams.length; i++) {
			ITypeBinding curr= typeParams[i];
			TypeParameter newTypeParam= ast.newTypeParameter();
			newTypeParam.setName(ast.newSimpleName(curr.getName()));
			ITypeBinding[] typeBounds= curr.getTypeBounds();
			if (typeBounds.length != 1 || !"java.lang.Object".equals(typeBounds[0].getQualifiedName())) {//$NON-NLS-1$
				List newTypeBounds= newTypeParam.typeBounds();
				for (int k= 0; k < typeBounds.length; k++) {
					newTypeBounds.add(imports.addImport(typeBounds[k], ast, context));
				}
			}
			typeParameters.add(newTypeParam);
		}

		decl.setReturnType2(imports.addImport(delegate.getReturnType(), ast, context));

		List parameters= decl.parameters();
		ITypeBinding[] params= delegate.getParameterTypes();
		String[] paramNames= StubUtility.suggestArgumentNames(unit.getJavaProject(), delegate);
		for (int i= 0; i < params.length; i++) {
			SingleVariableDeclaration varDecl= ast.newSingleVariableDeclaration();
			if (params[i].isWildcardType() && !params[i].isUpperbound())
				varDecl.setType(imports.addImport(params[i].getBound(), ast, context));
			else {
				if (delegate.isVarargs() && params[i].isArray() && i == params.length - 1) {
					StringBuffer buffer= new StringBuffer(imports.addImport(params[i].getElementType(), context));
					for (int dim= 1; dim < params[i].getDimensions(); dim++)
						buffer.append("[]"); //$NON-NLS-1$
					varDecl.setType(ASTNodeFactory.newType(ast, buffer.toString()));
					varDecl.setVarargs(true);
				} else
					varDecl.setType(imports.addImport(params[i], ast, context));
			}
			varDecl.setName(ast.newSimpleName(paramNames[i]));
			parameters.add(varDecl);
		}

		List thrownExceptions= decl.thrownExceptions();
		ITypeBinding[] excTypes= delegate.getExceptionTypes();
		for (int i= 0; i < excTypes.length; i++) {
			String excTypeName= imports.addImport(excTypes[i], context);
			thrownExceptions.add(ASTNodeFactory.newName(ast, excTypeName));
		}

		Block body= ast.newBlock();
		decl.setBody(body);

		String delimiter= StubUtility.getLineDelimiterUsed(unit);

		Statement statement= null;
		MethodInvocation invocation= ast.newMethodInvocation();
		invocation.setName(ast.newSimpleName(delegate.getName()));
		List arguments= invocation.arguments();
		for (int i= 0; i < params.length; i++)
			arguments.add(ast.newSimpleName(paramNames[i]));
		if (settings.useKeywordThis) {
			FieldAccess access= ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName(delegatingField.getName()));
			invocation.setExpression(access);
		} else
			invocation.setExpression(ast.newSimpleName(delegatingField.getName()));
		if (delegate.getReturnType().isPrimitive() && delegate.getReturnType().getName().equals("void")) {//$NON-NLS-1$
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

	public static MethodDeclaration createImplementationStub(ICompilationUnit unit, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context, IMethodBinding binding, String type, CodeGenerationSettings settings, boolean deferred) throws CoreException {
		Assert.isNotNull(imports);
		Assert.isNotNull(rewrite);

		AST ast= rewrite.getAST();

		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.modifiers().addAll(getImplementationModifiers(ast, binding, deferred));

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
					newTypeBounds.add(imports.addImport(typeBounds[k], ast, context));
				}
			}
			typeParameters.add(newTypeParam);
		}

		decl.setReturnType2(imports.addImport(binding.getReturnType(), ast, context));

		List parameters= createParameters(unit.getJavaProject(), imports, context, ast, binding, decl);

		List thrownExceptions= decl.thrownExceptions();
		ITypeBinding[] excTypes= binding.getExceptionTypes();
		for (int i= 0; i < excTypes.length; i++) {
			String excTypeName= imports.addImport(excTypes[i], context);
			thrownExceptions.add(ASTNodeFactory.newName(ast, excTypeName));
		}

		String delimiter= unit.findRecommendedLineSeparator();
		if (!deferred) {
			Map options= unit.getJavaProject().getOptions(true);

			Block body= ast.newBlock();
			decl.setBody(body);

			String bodyStatement= ""; //$NON-NLS-1$
			ITypeBinding declaringType= binding.getDeclaringClass();
			if (Modifier.isAbstract(binding.getModifiers()) || declaringType.isAnnotation() || declaringType.isInterface()) {
				Expression expression= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType2(), decl.getExtraDimensions());
				if (expression != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);
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
				ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
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
		if (settings != null && settings.overrideAnnotation && JavaModelUtil.is50OrHigher(unit.getJavaProject())) {
			addOverrideAnnotation(unit.getJavaProject(), rewrite, decl, binding);
		}

		return decl;
	}

	private static List createParameters(IJavaProject project, ImportRewrite imports, ImportRewriteContext context, AST ast, IMethodBinding binding, MethodDeclaration decl) {
		List parameters= decl.parameters();
		ITypeBinding[] params= binding.getParameterTypes();
		String[] paramNames= StubUtility.suggestArgumentNames(project, binding);
		for (int i= 0; i < params.length; i++) {
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			if (binding.isVarargs() && params[i].isArray() && i == params.length - 1) {
				StringBuffer buffer= new StringBuffer(imports.addImport(params[i].getElementType(), context));
				for (int dim= 1; dim < params[i].getDimensions(); dim++)
					buffer.append("[]"); //$NON-NLS-1$
				var.setType(ASTNodeFactory.newType(ast, buffer.toString()));
				var.setVarargs(true);
			} else
				var.setType(imports.addImport(params[i], ast, context));
			var.setName(ast.newSimpleName(paramNames[i]));
			parameters.add(var);
		}
		return parameters;
	}

	private static IMethodBinding findMethodBinding(IMethodBinding method, List allMethods) {
		for (int i= 0; i < allMethods.size(); i++) {
			IMethodBinding curr= (IMethodBinding) allMethods.get(i);
			if (Bindings.isSubsignature(method, curr)) {
				return curr;
			}
		}
		return null;
	}

	private static IMethodBinding findOverridingMethod(IMethodBinding method, List allMethods) {
		for (int i= 0; i < allMethods.size(); i++) {
			IMethodBinding curr= (IMethodBinding) allMethods.get(i);
			if (Bindings.areOverriddenMethods(curr, method) || Bindings.isSubsignature(curr, method))
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
				if (impl == null || !Bindings.isVisibleInHierarchy(impl, currPack)) {
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

	public static DelegateEntry[] getDelegatableMethods(ITypeBinding binding) {
		final List tuples= new ArrayList();
		final List declared= new ArrayList();
		IMethodBinding[] typeMethods= binding.getDeclaredMethods();
		for (int index= 0; index < typeMethods.length; index++)
			declared.add(typeMethods[index]);
		IVariableBinding[] typeFields= binding.getDeclaredFields();
		for (int index= 0; index < typeFields.length; index++) {
			IVariableBinding fieldBinding= typeFields[index];
			if (fieldBinding.isField() && !fieldBinding.isEnumConstant() && !fieldBinding.isSynthetic())
				getDelegatableMethods(new ArrayList(declared), fieldBinding, fieldBinding.getType(), binding, tuples);
		}
		// list of tuple<IVariableBinding, IMethodBinding>
		return (DelegateEntry[]) tuples.toArray(new DelegateEntry[tuples.size()]);
	}

	private static void getDelegatableMethods(List methods, IVariableBinding fieldBinding, ITypeBinding typeBinding, ITypeBinding binding, List/*DelegateEntry*/ result) {
		boolean match= false;
		if (typeBinding.isTypeVariable()) {
			ITypeBinding[] typeBounds= typeBinding.getTypeBounds();
			if (typeBounds.length > 0) {
				for (int i= 0; i < typeBounds.length; i++) {
					getDelegatableMethods(methods, fieldBinding, typeBounds[i], binding, result);
				}
			} else {
				ITypeBinding objectBinding= Bindings.findTypeInHierarchy(binding, "java.lang.Object"); //$NON-NLS-1$
				if (objectBinding != null) {
					getDelegatableMethods(methods, fieldBinding, objectBinding, binding, result);
				}
			}
		} else {
			IMethodBinding[] candidates= getDelegateCandidates(typeBinding, binding);
			for (int index= 0; index < candidates.length; index++) {
				match= false;
				final IMethodBinding methodBinding= candidates[index];
				for (int offset= 0; offset < methods.size() && !match; offset++) {
					if (Bindings.areOverriddenMethods((IMethodBinding) methods.get(offset), methodBinding))
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
			ITypeBinding[] superInterfaces= typeBinding.getInterfaces();
			for (int offset= 0; offset < superInterfaces.length; offset++)
				getDelegatableMethods(methods, fieldBinding, superInterfaces[offset], binding, result);
		}
	}

	private static IMethodBinding[] getDelegateCandidates(ITypeBinding binding, ITypeBinding hierarchy) {
		List allMethods= new ArrayList();
		boolean isInterface= binding.isInterface();
		IMethodBinding[] typeMethods= binding.getDeclaredMethods();
		for (int index= 0; index < typeMethods.length; index++) {
			final int modifiers= typeMethods[index].getModifiers();
			if (!typeMethods[index].isConstructor() && !Modifier.isStatic(modifiers) && (isInterface || Modifier.isPublic(modifiers))) {
				IMethodBinding result= Bindings.findOverriddenMethodInHierarchy(hierarchy, typeMethods[index]);
				if (result != null && Flags.isFinal(result.getModifiers()))
					continue;
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

	private static List getImplementationModifiers(AST ast, IMethodBinding method, boolean deferred) {
		int modifiers= method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.PRIVATE;
		if (deferred) {
			modifiers= modifiers & ~Modifier.PROTECTED;
			modifiers= modifiers | Modifier.PUBLIC;
		}
		return ASTNodeFactory.newModifiers(ast, modifiers);
	}

	public static IMethodBinding[] getOverridableMethods(AST ast, ITypeBinding typeBinding, boolean isSubType) {
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
				getOverridableMethods(ast, superInterfaces[index], allMethods);
			}
			clazz= clazz.getSuperclass();
		}
		if (typeBinding.isInterface())
			getOverridableMethods(ast, ast.resolveWellKnownType("java.lang.Object"), allMethods); //$NON-NLS-1$
		if (!isSubType)
			allMethods.removeAll(Arrays.asList(typeMethods));
		int modifiers= 0;
		if (!typeBinding.isInterface()) {
			for (int index= allMethods.size() - 1; index >= 0; index--) {
				IMethodBinding method= (IMethodBinding) allMethods.get(index);
				modifiers= method.getModifiers();
				if (Modifier.isFinal(modifiers))
					allMethods.remove(index);
			}
		}
		return (IMethodBinding[]) allMethods.toArray(new IMethodBinding[allMethods.size()]);
	}

	private static void getOverridableMethods(AST ast, ITypeBinding superBinding, List allMethods) {
		IMethodBinding[] methods= superBinding.getDeclaredMethods();
		for (int offset= 0; offset < methods.length; offset++) {
			final int modifiers= methods[offset].getModifiers();
			if (!methods[offset].isConstructor() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
				if (findOverridingMethod(methods[offset], allMethods) == null && !Modifier.isStatic(modifiers))
					allMethods.add(methods[offset]);
			}
		}
		ITypeBinding[] superInterfaces= superBinding.getInterfaces();
		for (int index= 0; index < superInterfaces.length; index++) {
			getOverridableMethods(ast, superInterfaces[index], allMethods);
		}
	}

	private static String suggestParameterName(ICompilationUnit unit, IVariableBinding binding, String[] excluded) {
		String name= StubUtility.getBaseName(binding, unit.getJavaProject());
		return StubUtility.suggestArgumentName(unit.getJavaProject(), name, excluded);
	}

	public static IMethodBinding[] getUnimplementedMethods(ITypeBinding typeBinding) {
		return getUnimplementedMethods(typeBinding, false);
	}

	public static IMethodBinding[] getUnimplementedMethods(ITypeBinding typeBinding, boolean implementAbstractsOfInput) {
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
			if ((Modifier.isAbstract(modifiers) || curr.getDeclaringClass().isInterface()) && (implementAbstractsOfInput || typeBinding != curr.getDeclaringClass())) {
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

	public static IMethodBinding[] getVisibleConstructors(ITypeBinding binding, boolean accountExisting, boolean proposeDefault) {
		List constructorMethods= new ArrayList();
		List existingConstructors= null;
		ITypeBinding superType= binding.getSuperclass();
		if (superType == null)
			return new IMethodBinding[0];
		if (accountExisting) {
			IMethodBinding[] methods= binding.getDeclaredMethods();
			existingConstructors= new ArrayList(methods.length);
			for (int index= 0; index < methods.length; index++) {
				IMethodBinding method= methods[index];
				if (method.isConstructor() && !method.isDefaultConstructor())
					existingConstructors.add(method);
			}
		}
		if (existingConstructors != null)
			constructorMethods.addAll(existingConstructors);
		IMethodBinding[] methods= binding.getDeclaredMethods();
		IMethodBinding[] superMethods= superType.getDeclaredMethods();
		for (int index= 0; index < superMethods.length; index++) {
			IMethodBinding method= superMethods[index];
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
		return (IMethodBinding[]) constructorMethods.toArray(new IMethodBinding[constructorMethods.size()]);
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

			List members= listRewrite.getOriginalList();
			for (int i= 0; i < members.size(); i++) {
				ASTNode curr= (ASTNode) members.get(i);
				if (curr.getStartPosition() >= insertPos) {
					return curr;
				}
			}
		}
		return null;
	}

	/**
	 * Creates a new stub utility.
	 */
	private StubUtility2() {
		// Not for instantiation
	}
}
