/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class CorrectPackageDeclarationFixCore implements IProposableFix {

	private final ICompilationUnit fCompilationUnit;
	private final IProblemLocation fLocation;

	private CorrectPackageDeclarationFixCore(CompilationUnit compilationUnit, IProblemLocation location) {
		this.fCompilationUnit= (ICompilationUnit)compilationUnit.getJavaElement();
		this.fLocation= location;
	}

	public static CorrectPackageDeclarationFixCore create(CompilationUnit cu, IProblemLocation location) {
		if (!isValidProposal((ICompilationUnit) cu.getJavaElement())) {
			return null;
		}
		return new CorrectPackageDeclarationFixCore(cu, location);
	}

	private static boolean isValidProposal(ICompilationUnit cu) {
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

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {

		IPackageFragment parentPack= (IPackageFragment) fCompilationUnit.getParent();
		IPackageDeclaration[] decls= fCompilationUnit.getPackageDeclarations();

		CompilationUnitChange change= new CompilationUnitChange(getDisplayString(), fCompilationUnit);
		MultiTextEdit root= new MultiTextEdit();

		if (parentPack.isDefaultPackage() && decls.length > 0) {
			for (IPackageDeclaration decl : decls) {
				ISourceRange range= decl.getSourceRange();
				root.addChild(new DeleteEdit(range.getOffset(), range.getLength()));
			}
		} else if (!parentPack.isDefaultPackage() && decls.length == 0) {
			String lineDelim= StubUtility.getLineDelimiterUsed(fCompilationUnit);
			String str= "package " + parentPack.getElementName() + ';' + lineDelim + lineDelim; //$NON-NLS-1$
			root.addChild(new InsertEdit(0, str));
		} else {
			root.addChild(new ReplaceEdit(fLocation.getOffset(), fLocation.getLength(), parentPack.getElementName()));
		}
		change.setEdit(root);
		return change;
	}

	@Override
	public String getDisplayString() {
		return CorrectionMessages.CorrectPackageDeclarationProposal_name;
	}

	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	@Override
	public IStatus getStatus() {
		return null;
	}

}
