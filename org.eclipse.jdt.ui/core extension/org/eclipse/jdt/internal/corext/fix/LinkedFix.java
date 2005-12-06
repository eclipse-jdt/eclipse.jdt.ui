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

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

import org.eclipse.jdt.internal.corext.codemanipulation.NewImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class LinkedFix extends AbstractFix {//implements IPositionLinkable {
	
	public interface ILinkedFixRewriteOperation extends IFixRewriteOperation {
		public ITrackedNodePosition rewriteAST(
				ASTRewrite rewrite, 
				NewImportRewrite importRewrite, 
				CompilationUnit compilationUnit,
				List/*<TextEditGroup>*/ textEditGroups,
				List/*<PositionGroup>*/ positionGroups) throws CoreException;
	}
	
	public static class PositionGroup {

		private final String fGroupId;
		private final List/*<ITrackedNodePosition>*/ fPositions;
		private final List/*<String>*/ fProposals;
		private final List/*<String>*/ fDisplayStrings;
		private ITrackedNodePosition fFirstPosition;
		
		public ITrackedNodePosition getFirstPosition() {
			return fFirstPosition;
		}

		public PositionGroup(String groupID) {
			fGroupId= groupID;
			fPositions= new ArrayList();
			fProposals= new ArrayList();
			fDisplayStrings= new ArrayList();
		}

		public void addPosition(ITrackedNodePosition position) {
			fPositions.add(position);
		}
		
		public void addFirstPosition(ITrackedNodePosition position) {
			addPosition(position);
			fFirstPosition= position;
		}

		public void addProposal(String displayString, String proposal) {
			fProposals.add(proposal);
			fDisplayStrings.add(displayString);
		}

		public String getGroupId() {
			return fGroupId;
		}

		public ITrackedNodePosition[] getPositions() {
			return (ITrackedNodePosition[])fPositions.toArray(new ITrackedNodePosition[fPositions.size()]);
		}

		public String[] getDisplayStrings() {
			return (String[])fDisplayStrings.toArray(new String[fDisplayStrings.size()]);
		}

		public String[] getProposals() {
			return (String[])fProposals.toArray(new String[fProposals.size()]);
		}
	}
	
	private final IFixRewriteOperation[] fFixRewrites;
	private final CompilationUnit fCompilationUnit;
	private final List fPositionGroups;
	private ITrackedNodePosition fEndPosition;

	protected LinkedFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewrites) {
		super(name, compilationUnit, null);
		fCompilationUnit= compilationUnit;
		fFixRewrites= fixRewrites;
		fPositionGroups= new ArrayList();
	}
	
	public ITrackedNodePosition getEndPosition() {
		return fEndPosition;
	}
	
	public PositionGroup[] getPositionGroups() {
		return (PositionGroup[])fPositionGroups.toArray(new PositionGroup[fPositionGroups.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		if (fFixRewrites == null || fFixRewrites.length == 0)
			return null;

		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite((ICompilationUnit)fCompilationUnit.getJavaElement(), fCompilationUnit);
	
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		NewImportRewrite importRewrite= cuRewrite.getImportRewrite().getNewImportRewrite();
		
		List/*<TextEditGroup>*/ groups= new ArrayList();

		fEndPosition= null;
		fPositionGroups.clear();
		
		for (int i= 0; i < fFixRewrites.length; i++) {
			IFixRewriteOperation adapter= fFixRewrites[i];
			if (adapter instanceof ILinkedFixRewriteOperation) {
				ILinkedFixRewriteOperation linkedAdapter= (ILinkedFixRewriteOperation)adapter;
				fEndPosition= linkedAdapter.rewriteAST(rewrite, importRewrite, fCompilationUnit, groups, fPositionGroups);
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
