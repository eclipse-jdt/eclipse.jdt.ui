/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Red Hat Inc - separate core logic from UI images
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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.manipulation.CodeStyleConfiguration;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class NewDefiningMethodProposal extends AbstractMethodCorrectionProposal {

	private boolean fAddOverrideAnnotation;

	private final IMethodBinding fMethod;

	public NewDefiningMethodProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, ITypeBinding binding, IMethodBinding method, String[] paramNames, boolean addOverride,
			int relevance) {
		super(label, targetCU, relevance, null, new NewDefiningMethodProposalCore(label, targetCU, invocationNode, binding, method, paramNames, relevance));
		ImageDescriptor desc= JavaElementImageProvider.getMethodImageDescriptor(binding.isInterface() || binding.isAnnotation(), method.getModifiers());
		setImage(JavaPlugin.getImageDescriptorRegistry().get(desc));
		fAddOverrideAnnotation= addOverride;
		fMethod= method;
	}


	@Override
	protected void performChange(IEditorPart part, IDocument document) throws CoreException {
		if (fAddOverrideAnnotation) {
			// Should this really be done in the UI api call and not in the core API call?
			addOverrideAnnotation(document);
		}
		super.performChange(part, document);
	}

	private void addOverrideAnnotation(IDocument document) throws CoreException {
		MethodDeclaration oldMethodDeclaration= (MethodDeclaration) ASTNodes.findDeclaration(fMethod, getInvocationNode());
		CompilationUnit findParentCompilationUnit= ASTResolving.findParentCompilationUnit(oldMethodDeclaration);
		IJavaProject javaProject= findParentCompilationUnit.getJavaElement().getJavaProject();
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

	public boolean canAddOverridenAnnotation () {
		return fAddOverrideAnnotation;
	}

	@Override
	protected void addNewExceptions(ASTRewrite rewrite, List<Type> exceptions, ImportRewriteContext context) throws CoreException {
		((NewDefiningMethodProposalCore) getDelegate()).addNewExceptions(rewrite, exceptions, context);
	}

	@Override
	protected void addNewParameters(ASTRewrite rewrite, List<String> takenNames, List<SingleVariableDeclaration> params, ImportRewriteContext context) throws CoreException {
		((NewDefiningMethodProposalCore) getDelegate()).addNewParameters(rewrite, takenNames, params, context);
	}

	@Override
	protected void addNewTypeParameters(ASTRewrite rewrite, List<String> takenNames, List<TypeParameter> params, ImportRewriteContext context) throws CoreException {
		((NewDefiningMethodProposalCore) getDelegate()).addNewTypeParameters(rewrite, takenNames, params, context);
	}

	@Override
	protected void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List<IExtendedModifier> modifiers) {
		((NewDefiningMethodProposalCore) getDelegate()).addNewModifiers(rewrite, targetTypeDecl, modifiers);
	}

	@Override
	protected void addNewJavaDoc(ASTRewrite rewrite, MethodDeclaration decl) throws CoreException {
		((NewDefiningMethodProposalCore) getDelegate()).addNewJavaDoc(rewrite, decl);
	}

	@Override
	protected SimpleName getNewName(ASTRewrite rewrite) {
		return ((NewDefiningMethodProposalCore) getDelegate()).getNewName(rewrite);
	}

	@Override
	protected Type getNewMethodType(ASTRewrite rewrite, ImportRewriteContext context) throws CoreException {
		return ((NewDefiningMethodProposalCore) getDelegate()).getNewMethodType(rewrite, context);
	}

	@Override
	protected boolean isConstructor() {
		return ((NewDefiningMethodProposalCore) getDelegate()).isConstructor();
	}
}
