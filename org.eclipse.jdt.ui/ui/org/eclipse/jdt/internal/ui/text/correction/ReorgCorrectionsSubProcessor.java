/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import org.eclipse.jdt.ui.actions.OrganizeImportsAction;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreatePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class ReorgCorrectionsSubProcessor {
	
	public static void getWrongTypeNameProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		String[] args= problem.getProblemArguments();
		if (args.length == 2) {
			ICompilationUnit cu= context.getCompilationUnit();
			boolean isLinked= cu.getResource().isLinked();
			
			// rename type
			proposals.add(new CorrectMainTypeNameProposal(cu, args[1], 5));
			
			String newCUName= args[1] + ".java"; //$NON-NLS-1$
			ICompilationUnit newCU= ((IPackageFragment) (cu.getParent())).getCompilationUnit(newCUName);
			if (!newCU.exists() && !isLinked && !JavaConventions.validateCompilationUnitName(newCUName).matches(IStatus.ERROR)) {
				RenameCompilationUnitChange change= new RenameCompilationUnitChange(cu, newCUName);
	
				// rename cu
				String label= CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.renamecu.description", newCUName); //$NON-NLS-1$
				proposals.add(new ChangeCorrectionProposal(label, change, 6, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME)));
			}
		}
	}
	
	public static void getWrongPackageDeclNameProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		String[] args= problem.getProblemArguments();
		if (args.length == 1) {
			ICompilationUnit cu= context.getCompilationUnit();
			boolean isLinked= cu.getResource().isLinked();
			
			// correct pack decl
			int relevance= cu.getPackageDeclarations().length == 0 ? 7 : 5; // bug 38357
			proposals.add(new CorrectPackageDeclarationProposal(cu, problem, relevance));

			// move to pack
			IPackageDeclaration[] packDecls= cu.getPackageDeclarations();
			String newPackName= packDecls.length > 0 ? packDecls[0].getElementName() : ""; //$NON-NLS-1$
			
			IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(cu);
			IPackageFragment newPack= root.getPackageFragment(newPackName);
			
			ICompilationUnit newCU= newPack.getCompilationUnit(cu.getElementName());
			if (!newCU.exists() && !isLinked) {
				String label;
				if (newPack.isDefaultPackage()) {
					label= CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.movecu.default.description", cu.getElementName()); //$NON-NLS-1$
				} else {
					label= CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.movecu.description", new Object[] { cu.getElementName(), newPack.getElementName() }); //$NON-NLS-1$
				}
				CompositeChange composite= new CompositeChange(label);
				composite.add(new CreatePackageChange(newPack));
				composite.add(new MoveCompilationUnitChange(cu, newPack));
	
				proposals.add(new ChangeCorrectionProposal(label, composite, 6, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_MOVE)));
			}
		}
	}
	
	public static void removeImportStatementProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode != null) {
			ASTNode node= ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
			if (node instanceof ImportDeclaration) {
				ASTRewrite rewrite= new ASTRewrite(node.getParent());

				rewrite.markAsRemoved(node);
			
				String label= CorrectionMessages.getString("ReorgCorrectionsSubProcessor.unusedimport.description"); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_DELETE_IMPORT);

				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
				proposal.ensureNoModifications();
				proposals.add(proposal);
			}
		}
		
		String name= CorrectionMessages.getString("ReorgCorrectionsSubProcessor.organizeimports.description"); //$NON-NLS-1$
		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, 5) {
			public void apply(IDocument document) {
				IEditorInput input= new FileEditorInput((IFile) cu.getResource());
				IWorkbenchPage p= JavaPlugin.getActivePage();
				if (p == null) {
					return;
				}
				IEditorPart part= p.findEditor(input);
				if (part instanceof JavaEditor) {
					OrganizeImportsAction action= new OrganizeImportsAction((JavaEditor) part);
					action.run(cu);						
				}					
			}
		};
		proposals.add(proposal);
	}
}
