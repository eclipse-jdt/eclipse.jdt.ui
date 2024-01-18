/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.util.QualifiedTypeNameHistory;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

public class AddImportCorrectionProposal extends ASTRewriteCorrectionProposal {

	static String JAVA_BASE= AddImportCorrectionProposalCore.JAVA_BASE;
	private AddModuleRequiresCorrectionProposal additional = null;
	public AddImportCorrectionProposal(AddImportCorrectionProposalCore core, Image image) {
		super(core.getName(), core.getCompilationUnit(), null, core.getRelevance(), image, core);
	}
	public AddImportCorrectionProposal(String name, ICompilationUnit cu, int relevance, Image image, String qualifierName, String typeName, SimpleName node) {
		super(name, cu, ASTRewrite.create(node.getAST()), relevance, image, new AddImportCorrectionProposalCore(name, cu, relevance, qualifierName, typeName, node));
	}

	public String getQualifiedTypeName() {
		return ((AddImportCorrectionProposalCore) getDelegate()).getQualifiedTypeName();
	}

	public AddModuleRequiresCorrectionProposal getAdditionalProposal() {
		if( additional == null ) {
			AddImportCorrectionProposalCore del = (AddImportCorrectionProposalCore) getDelegate();
			AddModuleRequiresCorrectionProposalCore core= del.getAdditionalChangeCorrectionProposal();
			additional = new AddModuleRequiresCorrectionProposal(core);
		}
		return additional;
	}

	@Override
	protected void performChange(IEditorPart activeEditor, IDocument document) throws CoreException {
		super.performChange(activeEditor, document);
		rememberSelection();
	}

	private void rememberSelection() {
		QualifiedTypeNameHistory.remember(getQualifiedTypeName());
	}
}
