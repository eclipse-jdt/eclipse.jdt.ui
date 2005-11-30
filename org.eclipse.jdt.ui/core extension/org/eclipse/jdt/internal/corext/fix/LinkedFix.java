/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

import org.eclipse.jdt.internal.corext.codemanipulation.NewImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.text.correction.LinkedCorrectionProposal.LinkedModeGroup;

public class LinkedFix extends AbstractFix {
	
	public interface ILinkedFixRewriteOperation extends IFixRewriteOperation {
		public void rewriteAST(
				ASTRewrite rewrite, 
				NewImportRewrite importRewrite, 
				CompilationUnit compilationUnit,
				List/*<TextEditGroup>*/ textEditGroups,
				IPositionLinkable callback) throws CoreException;
	}

	public interface IPositionLinkable {

		/**
		 * Adds a linked position to be shown when the fix is applied. All position with the
		 * same group id are linked.
		 * @param position The position to add.
		 * @param isFirst If set, the cursor is jumped to first.
		 * @param groupID The id of the group the position belongs to. All positions in the same group
		 * are linked.
		 */
		public void addLinkedPosition(ITrackedNodePosition position, boolean isFirst, String groupID);

		/**
		 * Sets the end position of the linked mode to the end of the passed range.
		 * @param position The position that describes the end position of the linked mode.
		 */
		public void setEndPosition(ITrackedNodePosition position);

		/**
		 * Adds a linked position proposal to the group with the given id.
		 * @param groupID The id of the group that should present the proposal
		 * @param proposal The string to propose.
		 * @param image The image to show for the position proposal or <code>null</code> if
		 * no image is desired.
		 */
		public void addLinkedPositionProposal(String groupID, String proposal, Image image);

		/**
		 * Adds a linked position proposal to the group with the given id.
		 * @param groupID The id of the group that should present the proposal
		 * 	@param displayString The name of the proposal
		 * @param proposal The string to insert.
		 * @param image The image to show for the position proposal or <code>null</code> if
		 * no image is desired.
		 */
		public void addLinkedPositionProposal(String groupID, String displayString, String proposal, Image image);

		/**
		 * Adds a linked position proposal to the group with the given id.
		 * @param groupID The id of the group that should present the proposal
		 * @param proposal The binding to use as type name proposal.
		 */
		public void addLinkedPositionProposal(String groupID, ITypeBinding proposal);

		/**
		 * Adds a linked position proposal to the group with the given id.
		 * @param groupID The id of the group that should present the proposal
		 * @param proposal The proposal to present.
		 */
		public void addLinkedPositionProposal(String groupID, IJavaCompletionProposal proposal);

		/**
		 * Returns all collected linked mode groups.
		 *
		 * @return all collected linked mode groups
		 */
		public LinkedModeGroup[] getLinkedModeGroups();

	}
	
	public static final IPositionLinkable NULL_LINKABLE= new IPositionLinkable() {
		public void addLinkedPosition(ITrackedNodePosition position, boolean isFirst, String groupID) {}
		public void setEndPosition(ITrackedNodePosition position) {}
		public void addLinkedPositionProposal(String groupID, String proposal, Image image) {}
		public void addLinkedPositionProposal(String groupID, String displayString, String proposal, Image image) {}
		public void addLinkedPositionProposal(String groupID, ITypeBinding proposal) {}
		public void addLinkedPositionProposal(String groupID, IJavaCompletionProposal proposal) {}
		public LinkedModeGroup[] getLinkedModeGroups() {return new LinkedModeGroup[0];}
	};
	
	private final IFixRewriteOperation[] fFixRewrites;
	private final CompilationUnit fCompilationUnit;

	protected LinkedFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewrites) {
		super(name, (ICompilationUnit)compilationUnit.getJavaElement());
		fCompilationUnit= compilationUnit;
		fFixRewrites= fixRewrites;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		return createChange(NULL_LINKABLE);
	}
	
	public TextChange createChange(IPositionLinkable callback) throws CoreException {
		if (fFixRewrites == null || fFixRewrites.length == 0)
			return null;

		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite((ICompilationUnit)fCompilationUnit.getJavaElement(), fCompilationUnit);
	
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		NewImportRewrite importRewrite= cuRewrite.getImportRewrite().getNewImportRewrite();
		
		List/*<TextEditGroup>*/ groups= new ArrayList();
		
		for (int i= 0; i < fFixRewrites.length; i++) {
			IFixRewriteOperation adapter= fFixRewrites[i];
			if (adapter instanceof ILinkedFixRewriteOperation) {
				ILinkedFixRewriteOperation linkedAdapter= (ILinkedFixRewriteOperation)adapter;
				linkedAdapter.rewriteAST(rewrite, importRewrite, fCompilationUnit, groups, callback);
			} else {
				adapter.rewriteAST(rewrite, importRewrite, fCompilationUnit, groups);
			}
		}
		
		CompilationUnitChange result= cuRewrite.createChange();
		
		for (Iterator iter= groups.iterator(); iter.hasNext();) {
			TextEditGroup group= (TextEditGroup)iter.next();
			result.addTextEditGroup(group);
		}
		return result;
	}
}
