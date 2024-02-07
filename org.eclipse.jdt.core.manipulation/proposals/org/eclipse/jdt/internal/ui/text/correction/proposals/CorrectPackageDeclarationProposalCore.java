/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectPackageDeclarationProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class CorrectPackageDeclarationProposalCore extends CUCorrectionProposalCore {
	private IProblemLocation fLocation;

	public CorrectPackageDeclarationProposalCore(ICompilationUnit cu, IProblemLocation location, int relevance) {
		super(CorrectionMessages.CorrectPackageDeclarationProposal_name, cu, relevance);
		fLocation= location;
	}

	@Override
	public void addEdits(IDocument doc, TextEdit root) throws CoreException {
		super.addEdits(doc, root);

		ICompilationUnit cu= getCompilationUnit();

		IPackageFragment parentPack= (IPackageFragment) cu.getParent();
		IPackageDeclaration[] decls= cu.getPackageDeclarations();

		if (parentPack.isDefaultPackage() && decls.length > 0) {
			for (IPackageDeclaration decl : decls) {
				ISourceRange range= decl.getSourceRange();
				root.addChild(new DeleteEdit(range.getOffset(), range.getLength()));
			}
			return;
		}
		if (!parentPack.isDefaultPackage() && decls.length == 0) {
			String lineDelim= StubUtility.getLineDelimiterUsed(cu);
			String str= "package " + parentPack.getElementName() + ';' + lineDelim + lineDelim; //$NON-NLS-1$
			root.addChild(new InsertEdit(0, str));
			return;
		}

		root.addChild(new ReplaceEdit(fLocation.getOffset(), fLocation.getLength(), parentPack.getElementName()));
	}

	@Override
	public String getName() {
		ICompilationUnit cu= getCompilationUnit();
		IPackageFragment parentPack= (IPackageFragment) cu.getParent();
		try {
			IPackageDeclaration[] decls= cu.getPackageDeclarations();
			if (parentPack.isDefaultPackage() && decls.length > 0) {
				return Messages.format(CorrectionMessages.CorrectPackageDeclarationProposal_remove_description, BasicElementLabels.getJavaElementName(decls[0].getElementName()));
			}
			if (!parentPack.isDefaultPackage() && decls.length == 0) {
				return (Messages.format(CorrectionMessages.CorrectPackageDeclarationProposal_add_description,  JavaElementLabelsCore.getElementLabel(parentPack, JavaElementLabelsCore.ALL_DEFAULT)));
			}
		} catch(JavaModelException e) {
			JavaManipulationPlugin.log(e);
		}
		return (Messages.format(CorrectionMessages.CorrectPackageDeclarationProposal_change_description, JavaElementLabelsCore.getElementLabel(parentPack, JavaElementLabelsCore.ALL_DEFAULT)));
	}

	public static boolean isValidProposal(ICompilationUnit cu) {
		boolean isValid= true;
		IPackageFragment parentPack= (IPackageFragment) cu.getParent();
		try {
			IPackageDeclaration[] decls= cu.getPackageDeclarations();
			if (parentPack.isDefaultPackage() && decls.length > 0) {
				IJavaProject jProject = parentPack.getJavaProject();
				if (jProject != null && JavaModelUtil.is9OrHigher(jProject)) {
					try {
						IModuleDescription desc= jProject.getModuleDescription();
						if (desc!= null && desc.exists()) {
							isValid= false;
						}
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} catch(JavaModelException e) {
			JavaManipulationPlugin.log(e);
		}
		return isValid;
	}
}