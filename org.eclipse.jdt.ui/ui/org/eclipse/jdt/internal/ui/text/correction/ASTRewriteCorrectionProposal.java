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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
  */
public class ASTRewriteCorrectionProposal extends CUCorrectionProposal {

	private ASTRewrite fRewrite;
	private ImportEdit fImportEdit;
	
	private GroupDescription fSelectionDescription;
	private List fLinkedPositions;

	public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, relevance, image);
		fRewrite= rewrite;
		fImportEdit= null;
		fSelectionDescription= null;
		fLinkedPositions= null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getSelectionDescription()
	 */
	protected GroupDescription getSelectionDescription() {
		return fSelectionDescription;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getLinkedRanges()
	 */
	protected GroupDescription[] getLinkedRanges() {
		if (fLinkedPositions != null && !fLinkedPositions.isEmpty()) {
			return (GroupDescription[]) fLinkedPositions.toArray(new GroupDescription[fLinkedPositions.size()]);
		}
		return null;
	}
	
	public GroupDescription markAsSelection(ASTRewrite rewrite, ASTNode node) {
		fSelectionDescription= new GroupDescription();
		rewrite.markAsTracked(node, fSelectionDescription);
		return fSelectionDescription;
	}
	
	
	public GroupDescription markAsLinked(ASTRewrite rewrite, ASTNode node, boolean isFirst, String kind) {
		GroupDescription description= new GroupDescription(kind);
		rewrite.markAsTracked(node, description);
		if (fLinkedPositions == null) {
			fLinkedPositions= new ArrayList();
		}
		if (isFirst) {
			fLinkedPositions.add(0, description);
		} else {
			fLinkedPositions.add(description);
		}
		return description;
	}
	
	public void setSelectionDescription(GroupDescription desc) {
		fSelectionDescription= desc;
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#createCompilationUnitChange(String, ICompilationUnit, TextEdit)
	 */
	protected CompilationUnitChange createCompilationUnitChange(String name, ICompilationUnit cu, TextEdit rootEdit) throws CoreException {
		CompilationUnitChange change= super.createCompilationUnitChange(name, cu, rootEdit);
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(change.getFile());
			ASTRewrite rewrite= getRewrite();
			if (rewrite != null) {
				rewrite.rewriteNode(buffer, rootEdit);
				rewrite.removeModifications();
			}
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
		}
		if (fImportEdit != null && !fImportEdit.isEmpty()) {
			rootEdit.add(fImportEdit);
		}
		return change;
	}
	
	private ImportEdit getImportEdit() throws CoreException {
		if (fImportEdit == null) {
			fImportEdit= new ImportEdit(getCompilationUnit(), JavaPreferencesSettings.getCodeGenerationSettings());
		}
		return fImportEdit;
	}
	
	
	public String addImport(String qualifiedTypeName) throws CoreException {
		return getImportEdit().addImport(qualifiedTypeName);
	}
	
	public String addImport(ITypeBinding binding) throws CoreException {
		return getImportEdit().addImport(binding);
	}	
	
	protected ASTRewrite getRewrite() throws CoreException {
		return fRewrite;
	}
	
	public void ensureNoModifications() throws CoreException {
		if (fRewrite != null && fRewrite.hasASTModifications()) {
			getChange(); // force the rewriting
		}
	}


}
