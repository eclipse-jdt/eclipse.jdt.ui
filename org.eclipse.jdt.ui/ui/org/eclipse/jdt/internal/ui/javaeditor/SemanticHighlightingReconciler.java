/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;

import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager.HighlightedPosition;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager.Highlighting;
import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;


/**
 * Semantic highlighting reconciler - Background thread implementation.
 * 
 * @since 3.0
 */
public class SemanticHighlightingReconciler implements IJavaReconcilingListener {

	/**
	 * Collects positions from the AST.
	 */
	private class PositionCollector extends GenericVisitor {
		
		/** The semantic token */
		private SemanticToken fToken= new SemanticToken();
		
		/*
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
		 */
		protected boolean visitNode(ASTNode node) {
			if ((node.getFlags() & ASTNode.MALFORMED) == ASTNode.MALFORMED) {
				retainPositions(node.getStartPosition(), node.getLength());
				return false; 
			}
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SimpleName)
		 */
		public boolean visit(SimpleName node) {
			fToken.update(node);
			for (int i= 0, n= fSemanticHighlightings.length; i < n; i++) {
				SemanticHighlighting semanticHighlighting= fSemanticHighlightings[i];
				if (semanticHighlighting.consumes(fToken)) {
					addPosition(node.getStartPosition(), node.getLength(), fHighlightings[i]);
					break;
				}
			}
			fToken.clear();
			return false;
		}
		
		/**
		 * Add a position with the given range and highlighting iff it does not exist already.
		 * @param offset The range offset
		 * @param length The range length
		 * @param highlighting The highlighting
		 */
		private void addPosition(int offset, int length, Highlighting highlighting) {
			boolean isExisting= false;
			// TODO: use binary search
			for (int i= 0, n= fRemovedPositions.size(); i < n; i++) {
				HighlightedPosition position= (HighlightedPosition) fRemovedPositions.get(i);
				if (position == null)
					continue;
				if (position.isEqual(offset, length, highlighting)) {
					isExisting= true;
					fRemovedPositions.set(i, null);
					fNOfRemovedPositions--;
					break;
				}
			}

			if (!isExisting) {
				Position position= fPresenter.createHighlightedPosition(offset, length, highlighting);
				fAddedPositions.add(position);
			}
		}

		/**
		 * Retain the positions completely contained in the given range.
		 * @param offset The range offset
		 * @param length The range length
		 */
		private void retainPositions(int offset, int length) {
			// TODO: use binary search
			for (int i= 0, n= fRemovedPositions.size(); i < n; i++) {
				HighlightedPosition position= (HighlightedPosition) fRemovedPositions.get(i);
				if (position != null && position.isContained(offset, length)) {
					fRemovedPositions.set(i, null);
					fNOfRemovedPositions--;
				}
			}
		}
	}
	
	/** Position collector */
	private PositionCollector fCollector= new PositionCollector();
	
	/** The compilation unit editor this semantic highlighting reconciler is installed on */
	private CompilationUnitEditor fEditor;
	/** The semantic highlighting presenter */
	private SemanticHighlightingPresenter fPresenter;
	/** Semantic highlightings */
	private SemanticHighlighting[] fSemanticHighlightings;
	/** Highlightings */
	private Highlighting[] fHighlightings;
	
	/** Background job's added highlighted positions */
	private List fAddedPositions= new ArrayList();
	/** Background job's removed highlighted positions */
	private List fRemovedPositions= new ArrayList();
	/** Number of removed positions */
	private int fNOfRemovedPositions;
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#aboutToBeReconciled()
	 */
	public void aboutToBeReconciled() {
		// Do nothing
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#reconciled(CompilationUnit, boolean, IProgressMonitor)
	 */
	public void reconciled(CompilationUnit ast, boolean forced, IProgressMonitor progressMonitor) {
		fPresenter.setCanceled(progressMonitor.isCanceled());
		
		if (ast == null || fPresenter.isCanceled())
			return;
		
		ASTNode[] subtrees= getAffectedSubtrees(ast);
		if (subtrees.length == 0)
			return;
		
		startReconcilingPositions();
		
		reconcilePositions(subtrees);
		
		TextPresentation textPresentation= fPresenter.createPresentation(fAddedPositions, fRemovedPositions);
		
		fPresenter.updatePresentation(textPresentation, fAddedPositions, fRemovedPositions);
		
		stopReconcilingPositions();
	}
	
	/**
	 * @param node Root node
	 * @return Array of subtrees that may be affected by past document changes
	 */
	private ASTNode[] getAffectedSubtrees(ASTNode node) {
		// TODO: only return nodes which are affected by document changes - would require an 'anchor' concept for taking distant effects into account
		return new ASTNode[] { node };
	}

	/**
	 * Start reconciling positions.
	 */
	private void startReconcilingPositions() {
		fPresenter.addAllPositions(fRemovedPositions);
		fNOfRemovedPositions= fRemovedPositions.size();
	}
	
	/**
	 * Reconcile positions based on the AST subtrees
	 * 
	 * @param subtrees the AST subtrees
	 */
	private void reconcilePositions(ASTNode[] subtrees) {
		// FIXME: remove positions not covered by subtrees
		for (int i= 0, n= subtrees.length; i < n; i++)
			subtrees[i].accept(fCollector);
		List oldPositions= fRemovedPositions;
		List newPositions= new ArrayList(fNOfRemovedPositions);
		for (int i= 0, n= oldPositions.size(); i < n; i ++) {
			Object current= oldPositions.get(i);
			if (current != null)
				newPositions.add(current);
		}
		fRemovedPositions= newPositions;
	}

	/**
	 * Stop reconciling positions.
	 */
	private void stopReconcilingPositions() {
		fRemovedPositions.clear();
		fNOfRemovedPositions= 0;
		fAddedPositions.clear();
	}

	/**
	 * Install this reconciler on the given editor, presenter and highlightings.
	 * @param editor The editor
	 * @param presenter The semantic highlighting presenter
	 * @param semanticHighlightings The semantic highlightings
	 * @param highlightings The highlightings
	 */
	public void install(CompilationUnitEditor editor, SemanticHighlightingPresenter presenter, SemanticHighlighting[] semanticHighlightings, Highlighting[] highlightings) {
		fPresenter= presenter;
		fSemanticHighlightings= semanticHighlightings;
		fHighlightings= highlightings;
		
		fEditor= editor;
		fEditor.addReconcileListener(this);
	}
	
	/**
	 * Uninstall this reconciler from the editor
	 */
	public void uninstall() {
		if (fEditor != null) {
			fEditor.removeReconcileListener(this);
			fEditor= null;
		}
		
		fSemanticHighlightings= null;
		fHighlightings= null;
		fPresenter= null;
	}
}
