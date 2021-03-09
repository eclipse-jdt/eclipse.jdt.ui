/*******************************************************************************
 * Copyright (c) 2018, 2019 Red Hat and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ASTHelper;

public class NewProviderMethodDeclaration extends AbstractMethodCorrectionProposal {

	private IType fReturnType;

	public NewProviderMethodDeclaration(String label, ICompilationUnit targetCU, ASTNode invocationNode, ITypeBinding binding, int relevance, Image image, IType returnType) {
		super(label, targetCU, invocationNode, binding, relevance, image);
		this.fReturnType= returnType;
	}

	@Override
	protected void performChange(IEditorPart part, IDocument document) throws CoreException {
		ICompilationUnit compilationUnit= getCompilationUnit();
		ITypeHierarchy th= fReturnType.newTypeHierarchy(compilationUnit.getJavaProject(), null);
		IType[] subTypes= th.getAllSubtypes(fReturnType);

		List<IType> typeProposals= new ArrayList<> (Arrays.asList(fReturnType));
		typeProposals.addAll(Arrays.asList(subTypes));
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);

		ASTParser parser= ASTParser.newParser(ASTHelper.JLS9);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(compilationUnit.getJavaProject());
		parser.setSource(compilationUnit);
		parser.setUnitName(compilationUnit.getPath().toOSString());
		parser.setResolveBindings(true);
		IBinding [] typeBindings= parser.createBindings(typeProposals.toArray(new IJavaElement[0]), null);

		IPackageFragment pack= (IPackageFragment) compilationUnit.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		for (int i=0; i < typeProposals.size(); i++) {
			IType t= typeProposals.get(i);
			if (compilationUnit.equals(t.getCompilationUnit()) || JavaModelUtil.isVisible(t, pack)) {
				if (typeBindings[i] instanceof ITypeBinding) {
					addLinkedPositionProposal("return_type", (ITypeBinding) typeBindings[i]); //$NON-NLS-1$
				} else {
					addLinkedPositionProposal("return_type", t.getElementName(), image); //$NON-NLS-1$
				}
			}
		}
		super.performChange(part, document);
	}

	@Override
	protected boolean isConstructor() {
		return false;
	}

	@Override
	protected void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List<IExtendedModifier> modifiers) {
		modifiers.addAll(rewrite.getAST().newModifiers(Modifier.PUBLIC | Modifier.STATIC));
	}

	@Override
	protected void addNewTypeParameters(ASTRewrite rewrite, List<String> takenNames, List<TypeParameter> params, ImportRewriteContext context) throws CoreException {
		// No type parameters needed, void type
	}

	@Override
	protected void addNewParameters(ASTRewrite rewrite, List<String> takenNames, List<SingleVariableDeclaration> params, ImportRewriteContext context) throws CoreException {
		// no parameters needed, void type
	}

	@Override
	protected void addNewExceptions(ASTRewrite rewrite, List<Type> exceptions, ImportRewriteContext context) throws CoreException {
		// no exceptions thrown
	}

	@Override
	protected SimpleName getNewName(ASTRewrite rewrite) {
		AST ast= rewrite.getAST();
		SimpleName nameNode= ast.newSimpleName("provider"); //$NON-NLS-1$ // TypeConstants.PROVIDER
		return nameNode;
	}

	@Override
	protected Type getNewMethodType(ASTRewrite rewrite, ImportRewriteContext context) throws CoreException {
		getImportRewrite().addImport(fReturnType.getFullyQualifiedName());
		AST ast= rewrite.getAST();
		Type type= ast.newSimpleType(ast.newSimpleName(fReturnType.getElementName()));
		addLinkedPosition(rewrite.track(type), true, "return_type"); //$NON-NLS-1$
		return type;
	}

}
