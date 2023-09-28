/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * <pre>
 * - propose local instance
 * - propose field instance
 * </pre>
 */
public class CreateVariableReferenceProposal extends LinkedCorrectionProposal {

	private ITypeBinding fTypeNode;

	private VariableDeclarationFragment fSelectedNode;

	private String varName= null;
	private String varClass= null;

	public CreateVariableReferenceProposal(ICompilationUnit cu, VariableDeclarationFragment selectedNode, ITypeBinding typeNode, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		fSelectedNode= selectedNode;
		fTypeNode= typeNode;
	}

	public boolean hasProposal() {
		if(fSelectedNode.getInitializer().getNodeType() != ASTNode.QUALIFIED_NAME) {
			return false;
		}
		IVariableBinding variabelReference= findReference();
		if (variabelReference != null) {
			varName= variabelReference.getName();
			return true;
		}
		return false;
	}

	@Override
	public Image getImage() {
		return JavaPlugin.getImageDescriptorRegistry().get(
				new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE));
	}

	@Override
	public String getName() {
		return Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createnew_reference_to_variable, new Object[] { varName, varClass });
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		IVariableBinding findInstance= findReference();
		if (findInstance == null) {
			return null;
		}
		AST ast= fSelectedNode.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		CompilationUnit cu= (CompilationUnit) fSelectedNode.getRoot();
		createImportRewrite(cu);

		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(fSelectedNode.getName().getIdentifier()));
		vdf.setInitializer(ast.newSimpleName(varName));

		rewrite.replace(fSelectedNode, vdf, null);
		return rewrite;
	}

	/*
	 * search local/class/superclass
	 */
	private IVariableBinding findReference() {
		BodyDeclaration parent= ASTNodes.getParent(fSelectedNode, BodyDeclaration.class);
		String qualifiedTypeName= Bindings.getFullyQualifiedName(fTypeNode);
		// search local block
		Set<SimpleName> localVariableIdentifiers= ASTNodes.getLocalVariableIdentifiers(parent, true);
		for (SimpleName name : localVariableIdentifiers) {
			if(name.getStartPosition() > fSelectedNode.getStartPosition()) {
				continue;
			}
			VariableDeclaration vd= (VariableDeclaration) name.getParent();
			if (fSelectedNode.getName().getIdentifier().equals(vd.getName().getIdentifier())) {
				continue;
			}
			IVariableBinding variableBinding= vd.resolveBinding();
			if(variableBinding == null) {
				continue;
			}
			ITypeBinding variableBindingType= variableBinding.getType();
			if(variableBindingType == null) {
				continue;
			}
			String qualifiedTypeName2= Bindings.getFullyQualifiedName(variableBindingType);
			if (qualifiedTypeName.equals(qualifiedTypeName2)) {
				varClass= ((TypeDeclaration)ASTResolving.findAncestor(name, ASTNode.TYPE_DECLARATION)).getName().getIdentifier();
				return ((VariableDeclaration) name.getParent()).resolveBinding();
			}
		}
		// search class hierarchy
		List<IVariableBinding> visibleLocalVariablesInScope= getVisibleLocalVariablesInScope(fSelectedNode);
		for (IVariableBinding binding : visibleLocalVariablesInScope) {
			// if we live in an static body, we must access a static reference
			if (Modifier.isStatic(parent.getModifiers()) && !Modifier.isStatic(binding.getModifiers())) {
				continue;
			}
			if (Bindings.equals(binding.getType(), fTypeNode)) {
				if(binding.isField()) {
					varClass= binding.getDeclaringClass().getName();
				} else {
					varClass= "local block"; //$NON-NLS-1$
				}
				return binding;
			}
		}
		return null;
	}

	private static List<IVariableBinding> getVisibleLocalVariablesInScope(ASTNode node) {
		List<IVariableBinding> variableNames= new ArrayList<>();
		CompilationUnit root= (CompilationUnit) node.getRoot();
		IBinding[] bindings= new ScopeAnalyzer(root).getDeclarationsInScope(node.getStartPosition(), ScopeAnalyzer.VARIABLES | ScopeAnalyzer.CHECK_VISIBILITY);
		for (IBinding binding : bindings) {
			variableNames.add((IVariableBinding) binding);
		}
		return variableNames;
	}
}
