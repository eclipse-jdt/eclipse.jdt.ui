/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.actions.OrganizeImportsAction;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreatePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ReorgCorrectionsSubProcessor {
	
	public static void getWrongTypeNameProposals(ICorrectionContext context, List proposals) throws CoreException {
		String[] args= context.getProblemArguments();
		if (args.length == 2) {
			ICompilationUnit cu= context.getCompilationUnit();
			
			// rename type
			proposals.add(new CorrectMainTypeNameProposal(cu, args[1], 1));
			
			String newCUName= args[1] + ".java"; //$NON-NLS-1$
			ICompilationUnit newCU= ((IPackageFragment) (cu.getParent())).getCompilationUnit(newCUName);
			if (!newCU.exists()) {
				RenameCompilationUnitChange change= new RenameCompilationUnitChange(cu, newCUName);
	
				// rename cu
				String label= CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.renamecu.description", newCUName); //$NON-NLS-1$
				proposals.add(new ChangeCorrectionProposal(label, change, 2, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME)));
			}
		}
	}
	
	public static void getWrongPackageDeclNameProposals(ICorrectionContext context, List proposals) throws CoreException {
		String[] args= context.getProblemArguments();
		if (args.length == 1) {
			ICompilationUnit cu= context.getCompilationUnit();
			
			// correct pack decl
			proposals.add(new CorrectPackageDeclarationProposal(context, 1));

			// move to pack
			IPackageDeclaration[] packDecls= cu.getPackageDeclarations();
			String newPackName= packDecls.length > 0 ? packDecls[0].getElementName() : ""; //$NON-NLS-1$
			
			IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(cu);
			IPackageFragment newPack= root.getPackageFragment(newPackName);
			
			ICompilationUnit newCU= newPack.getCompilationUnit(cu.getElementName());
			if (!newCU.exists()) {
				String label;
				if (newPack.isDefaultPackage()) {
					label= CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.movecu.default.description", cu.getElementName()); //$NON-NLS-1$
				} else {
					label= CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.movecu.description", new Object[] { cu.getElementName(), newPack.getElementName() }); //$NON-NLS-1$
				}
				CompositeChange composite= new CompositeChange(label);
				composite.add(new CreatePackageChange(newPack));
				composite.add(new MoveCompilationUnitChange(cu, newPack));
	
				proposals.add(new ChangeCorrectionProposal(label, composite, 2, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_MOVE)));
			}
		}
	}
	
	public static void removeImportStatementProposals(ICorrectionContext context, List proposals) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();
		IJavaElement elem= cu.getElementAt(context.getOffset());
		if (elem instanceof IImportDeclaration) {
			String label= CorrectionMessages.getString("ReorgCorrectionsSubProcessor.unusedimport.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_DELETE_IMPORT);
			CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, 1, image);
			ImportEdit importEdit= new ImportEdit(cu, JavaPreferencesSettings.getCodeGenerationSettings());
			importEdit.removeImport(elem.getElementName());
			proposal.getRootTextEdit().add(importEdit);
			proposals.add(proposal);
		}
		
		String name= CorrectionMessages.getString("ReorgCorrectionsSubProcessor.organizeimports.description"); //$NON-NLS-1$
		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, 0) {
			public void apply(IDocument document) {
				IEditorInput input= new FileEditorInput((IFile) JavaModelUtil.toOriginal(cu).getResource());
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
