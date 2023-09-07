package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;

import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;

public class AddSafeVarargsProposalCore extends LinkedCorrectionProposalCore {
	private IMethodBinding fMethodBinding;
	private MethodDeclaration fMethodDeclaration;

	public AddSafeVarargsProposalCore(String label, ICompilationUnit cu, MethodDeclaration methodDeclaration, IMethodBinding methodBinding, int relevance) {
		super(label, cu, null, relevance);
		fMethodDeclaration= methodDeclaration;
		fMethodBinding= methodBinding;
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		if (fMethodDeclaration == null) {
			CompilationUnit astRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			fMethodDeclaration= (MethodDeclaration) astRoot.findDeclaringNode(fMethodBinding.getKey());
		}
		AST ast= fMethodDeclaration.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		ListRewrite listRewrite= rewrite.getListRewrite(fMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);

		MarkerAnnotation annotation= ast.newMarkerAnnotation();
		String importString= createImportRewrite((CompilationUnit) fMethodDeclaration.getRoot()).addImport("java.lang.SafeVarargs"); //$NON-NLS-1$
		annotation.setTypeName(ast.newName(importString));
		listRewrite.insertFirst(annotation, null);

		// set up linked mode
		addLinkedPosition(rewrite.track(annotation), true, "annotation"); //$NON-NLS-1$

		return rewrite;
	}

}