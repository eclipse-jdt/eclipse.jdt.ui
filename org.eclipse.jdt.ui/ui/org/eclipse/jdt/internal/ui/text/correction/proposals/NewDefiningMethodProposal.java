/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.manipulation.CodeStyleConfiguration;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class NewDefiningMethodProposal extends AbstractMethodCorrectionProposal {

	private final IMethodBinding fMethod;

	private final String[] fParamNames;

	private boolean fAddOverrideAnnotation;

	public NewDefiningMethodProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, ITypeBinding binding, IMethodBinding method, String[] paramNames, boolean addOverride, int relevance) {
		super(label, targetCU, invocationNode, binding, relevance, null);
		fMethod= method;
		fParamNames= paramNames;
		fAddOverrideAnnotation= addOverride;

		ImageDescriptor desc= JavaElementImageProvider.getMethodImageDescriptor(binding.isInterface() || binding.isAnnotation(), method.getModifiers());
		setImage(JavaPlugin.getImageDescriptorRegistry().get(desc));
	}

	@Override
	protected boolean isConstructor() {
		return fMethod.isConstructor();
	}

	@Override
	protected void performChange(IEditorPart part, IDocument document) throws CoreException {
		if (fAddOverrideAnnotation) {
			addOverrideAnnotation(document);
		}
		super.performChange(part, document);
	}

	private void addOverrideAnnotation(IDocument document) throws CoreException {
		MethodDeclaration oldMethodDeclaration= (MethodDeclaration) ASTNodes.findDeclaration(fMethod, getInvocationNode());
		CompilationUnit findParentCompilationUnit= ASTResolving.findParentCompilationUnit(oldMethodDeclaration);
		IJavaProject javaProject= findParentCompilationUnit.getJavaElement().getJavaProject();
		String version= javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		if (JavaModelUtil.isVersionLessThan(version, JavaCore.VERSION_1_5)) {
			return;
		}
		IType type= javaProject.findType(fMethod.getDeclaringClass().getQualifiedName());
		ICompilationUnit compilationUnit= type.getCompilationUnit();
		ImportRewrite importRewrite= CodeStyleConfiguration.createImportRewrite(compilationUnit, true);

		AST ast= oldMethodDeclaration.getAST();
		ASTRewrite astRewrite= ASTRewrite.create(ast);
		Annotation marker= ast.newMarkerAnnotation();
		marker.setTypeName(ast.newName(importRewrite.addImport("java.lang.Override", null))); //$NON-NLS-1$
		astRewrite.getListRewrite(oldMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(marker, null);

		try {
			TextEdit importEdits= importRewrite.rewriteImports(new NullProgressMonitor());
			TextEdit edits= astRewrite.rewriteAST();
			importEdits.addChild(edits);

			importEdits.apply(document);
			compilationUnit.getBuffer().setContents(document.get());
			compilationUnit.save(new NullProgressMonitor(), true);
		} catch (MalformedTreeException | BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	@Override
	protected void addNewParameters(ASTRewrite rewrite, List<String> takenNames, List<SingleVariableDeclaration> params, ImportRewriteContext context) throws CoreException {
		AST ast= rewrite.getAST();
		ImportRewrite importRewrite= getImportRewrite();
		ITypeBinding[] bindings= fMethod.getParameterTypes();

		IJavaProject project= getCompilationUnit().getJavaProject();
		String[][] paramNames= StubUtility.suggestArgumentNamesWithProposals(project, fParamNames);

		for (int i= 0; i < bindings.length; i++) {
			ITypeBinding curr= bindings[i];

			String[] proposedNames= paramNames[i];

			SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();

			newParam.setType(importRewrite.addImport(curr, ast, context, TypeLocation.PARAMETER));
			newParam.setName(ast.newSimpleName(proposedNames[0]));

			params.add(newParam);

			String groupId= "arg_name_" + i; //$NON-NLS-1$
			addLinkedPosition(rewrite.track(newParam.getName()), false, groupId);

			for (String proposedName : proposedNames) {
				addLinkedPositionProposal(groupId, proposedName, null);
			}
		}
		if (params.isEmpty() || bindings.length == 0) {
			return;
		}
		if (fMethod.isVarargs()) {
			// only last parameter can be vararg
			SingleVariableDeclaration singleVariableDeclaration= params.get(bindings.length - 1);
			singleVariableDeclaration.setVarargs(true);
			Type type= singleVariableDeclaration.getType();
			if (type != null && type.isArrayType()) {
				List<Dimension> dimensions= ((ArrayType) type).dimensions();
				if (dimensions.isEmpty()) {
					return;
				}
				// remove last dimension added by vararg conversion
				dimensions.remove(dimensions.size() - 1);
			}
		}
	}

	@Override
	protected void addNewJavaDoc(ASTRewrite rewrite, MethodDeclaration decl) throws CoreException {
		final Javadoc oldJavadoc= ((MethodDeclaration) ASTNodes.findDeclaration(fMethod, getInvocationNode())).getJavadoc();
		if (oldJavadoc != null) {
			String newJavadocString= ASTNodes.getNodeSource(oldJavadoc, false, true);
			if (newJavadocString != null) {
				decl.setJavadoc((Javadoc) rewrite.createStringPlaceholder(newJavadocString, ASTNode.JAVADOC));
			}
		}
	}

	@Override
	protected SimpleName getNewName(ASTRewrite rewrite) {
		AST ast= rewrite.getAST();
		SimpleName nameNode= ast.newSimpleName(fMethod.getName());
		return nameNode;
	}

	private int evaluateModifiers() {
		if (getSenderBinding().isInterface()) {
			return 0;
		} else {
			int modifiers= fMethod.getModifiers();
			if (Modifier.isPrivate(modifiers)) {
				modifiers|= Modifier.PROTECTED;
			}
			return modifiers & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.ABSTRACT | Modifier.STRICTFP);
		}
	}

	@Override
	protected void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List<IExtendedModifier> modifiers) {
		modifiers.addAll(rewrite.getAST().newModifiers(evaluateModifiers()));
	}

	@Override
	protected Type getNewMethodType(ASTRewrite rewrite, ImportRewriteContext context) throws CoreException {
		return getImportRewrite().addImport(fMethod.getReturnType(), rewrite.getAST(), context, TypeLocation.RETURN_TYPE);
	}

	@Override
	protected void addNewExceptions(ASTRewrite rewrite, List<Type> exceptions, ImportRewriteContext context) throws CoreException {
		AST ast= rewrite.getAST();
		ImportRewrite importRewrite= getImportRewrite();
		ITypeBinding[] bindings= fMethod.getExceptionTypes();
		for (int i= 0; i < bindings.length; i++) {
			Type newType= importRewrite.addImport(bindings[i], ast, context, TypeLocation.EXCEPTION);
			exceptions.add(newType);

			addLinkedPosition(rewrite.track(newType), false, "exc_type_" + i); //$NON-NLS-1$
		}
	}

	@Override
	protected void addNewTypeParameters(ASTRewrite rewrite, List<String> takenNames, List<TypeParameter> typeParameters, ImportRewriteContext context) throws CoreException {
		AST ast= rewrite.getAST();
		for (ITypeBinding current : fMethod.getTypeParameters()) {
			TypeParameter newTypeParameter= ast.newTypeParameter();
			newTypeParameter.setName(ast.newSimpleName(current.getName()));
			ITypeBinding[] typeBounds= current.getTypeBounds();
			if (typeBounds.length != 1 || !"java.lang.Object".equals(typeBounds[0].getQualifiedName())) {//$NON-NLS-1$
				List<Type> newTypeBounds= newTypeParameter.typeBounds();
				for (ITypeBinding typeBound : typeBounds) {
					newTypeBounds.add(getImportRewrite().addImport(typeBound, ast, context, TypeLocation.TYPE_BOUND));
				}
			}
			typeParameters.add(newTypeParameter);
		}
	}
}
