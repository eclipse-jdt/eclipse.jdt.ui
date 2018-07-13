/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - copied and pared down to methods needed by jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.EnumSet;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Utilities for code generation based on AST rewrite.
 *
 * @since 1.10
 */
public final class StubUtility2Core {

	public static ITypeBinding replaceWildcardsAndCaptures(ITypeBinding type) {
		while (type.isWildcardType() || type.isCapture() || (type.isArray() && type.getElementType().isCapture())) {
			ITypeBinding bound= type.getBound();
			type= (bound != null) ? bound : type.getErasure();
		}
		return type;
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
		for (int i= 0; i < modifiers.size(); i++) {
			IExtendedModifier curr= modifiers.get(i);
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


}
