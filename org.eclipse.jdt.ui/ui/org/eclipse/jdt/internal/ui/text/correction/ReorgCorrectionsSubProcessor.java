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

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreatePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class ReorgCorrectionsSubProcessor {
	
	public static void getWrongTypeNameProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length == 2) {
			ICompilationUnit cu= problemPos.getCompilationUnit();
			
			// rename type
			Path path= new Path(args[0]);
			String newName= path.removeFileExtension().lastSegment();
			String label= CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.renametype.description", newName); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(label, problemPos, newName, 1));
			
			String newCUName= args[1] + ".java"; //$NON-NLS-1$
			ICompilationUnit newCU= ((IPackageFragment) (cu.getParent())).getCompilationUnit(newCUName);
			if (!newCU.exists()) {
				RenameCompilationUnitChange change= new RenameCompilationUnitChange(cu, newCUName);
	
				label= CorrectionMessages.getFormattedString("ReorgCorrectionsSubProcessor.renamecu.description", newCUName); //$NON-NLS-1$
				// rename cu
				proposals.add(new ChangeCorrectionProposal(label, change, 2, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME)));
			}
		}
	}
	
	public static void getWrongPackageDeclNameProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length == 1) {
			ICompilationUnit cu= problemPos.getCompilationUnit();
			
			// correct pack decl
			proposals.add(new CorrectPackageDeclarationProposal(problemPos, 1));

			// move to pack
			IPackageFragment currPack= (IPackageFragment) cu.getParent();
			
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
	
	public static void removeImportStatementProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		
		TextBuffer buffer= null;
		try {
			buffer= aquireTextBuffer(problemPos.getCompilationUnit());
			int line= buffer.getLineOfOffset(problemPos.getOffset());
			if (line != -1) {
				TextRegion region= buffer.getLineInformation(line);
				int start= region.getOffset();
				int end= start + region.getLength();
				if (line > 0) {
					region= buffer.getLineInformation(line - 1);
					start= region.getOffset() + region.getLength();
				} 
				String label= CorrectionMessages.getString("ReorgCorrectionsSubProcessor.unusedimport.description"); //$NON-NLS-1$
				ReplaceCorrectionProposal proposal= new ReplaceCorrectionProposal(label, problemPos.getCompilationUnit(), start, end - start, "", 0); //$NON-NLS-1$
				proposal.setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_DELETE_IMPORT));
				proposals.add(proposal); 
			}
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
		 }
	}
		
	private static TextBuffer aquireTextBuffer(ICompilationUnit cu) throws CoreException {
		cu= JavaModelUtil.toOriginal(cu);
		IFile file= (IFile) cu.getUnderlyingResource();
		return TextBuffer.acquire(file);
	}		

}
