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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * A proposal for quick fixes and quick assists that works on a AST rewriter and enters the
 * linked mode when the proposal is set up.
 * Either a rewriter is directly passed in the constructor or method {@link #getRewrite()} is overridden
 * to provide the AST rewriter that is evaluated to the document when the proposal is
 * applied.
 * @since 3.0
 */
public class LinkedCorrectionProposal extends ASTRewriteCorrectionProposal {

	public static class LinkedModeGroup {
		final List fPositions= new ArrayList(); // list of ITrackedNodePosition
		final List fProposals= new ArrayList(); // list of IJavaCompletionProposal
		
		public ITrackedNodePosition[] getPositions() {
			return (ITrackedNodePosition[])fPositions.toArray(new ITrackedNodePosition[fPositions.size()]);
		}
		public IJavaCompletionProposal[] getProposals() {
			return (IJavaCompletionProposal[])fProposals.toArray(new IJavaCompletionProposal[fProposals.size()]);
		}
	}
	
	private static class LinkedModeExitPolicy implements LinkedModeUI.IExitPolicy {
	
		public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
			if (event.character  == '=') {
				return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
			}
			return null;
		}
		
	}
	
	private ITrackedNodePosition fSelectionDescription;
	private Map/*<String, LinkModeGroup>*/ fLinkGroups;
	private List fPositionOrder;

	/**
	 * Constructs a linked correction proposal.
	 * @param name The display name of the proposal.
	 * @param cu The compilation unit that is modified.
	 * @param rewrite The AST rewrite that is invoked when the proposal is applied
	 *  <code>null</code> can be passed if {@link #getRewrite()} is overridden.
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	public LinkedCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, rewrite, relevance, image);
		fSelectionDescription= null;
		fLinkGroups= null;
	}
	
	/**
	 * Adds a linked position to be shown when the proposal is applied. All position with the
	 * same group id are linked.
	 * @param position The position to add.
	 * @param isFirst If set, the proposal is jumped to first.
	 * @param groupID The id of the group the proposal belongs to. All proposals in the same group
	 * are linked.
	 */
	public void addLinkedPosition(ITrackedNodePosition position, boolean isFirst, String groupID) {
		getLinkedModeGroup(groupID).fPositions.add(position);
		if (fPositionOrder == null) {
			fPositionOrder= new ArrayList();
		}
		if (isFirst) {
			fPositionOrder.add(0, position);
		} else {
			fPositionOrder.add(position);
		}
	}
	
	/**
	 * Sets the end position of the linked mode to the end of the passed range.
	 * @param position The position that describes the end position of the linked mode.
	 */
	public void setEndPosition(ITrackedNodePosition position) {
		fSelectionDescription= position;
	}
	
	/**
	 * Adds a linked position proposal to the group with the given id.
	 * @param groupID The id of the group that should present the proposal
	 * @param proposal The string to propose.
	 * @param image The image to show for the position proposal or <code>null</code> if
	 * no image is desired.
	 */
	public void addLinkedPositionProposal(String groupID, String proposal, Image image) {
		addLinkedPositionProposal(groupID, new LinkedModeProposal(proposal));
	}
	
	/**
	 * Adds a linked position proposal to the group with the given id.
	 * @param groupID The id of the group that should present the proposal
	 * @param proposal The binding to use as type name proposal.
	 */
	public void addLinkedPositionProposal(String groupID, ITypeBinding proposal) {
		addLinkedPositionProposal(groupID, new LinkedModeProposal(getCompilationUnit(), proposal));
	}	
	
	/**
	 * Adds a linked position proposal to the group with the given id.
	 * @param groupID The id of the group that should present the proposal
	 * @param proposal The proposal to present.
	 */
	public void addLinkedPositionProposal(String groupID, IJavaCompletionProposal proposal) {
		getLinkedModeGroup(groupID).fProposals.add(proposal);
	}
	
	/**
	 * Returns all collected linked mode groups.
	 * 
	 * @return all collected linked mode groups
	 */
	public LinkedModeGroup[] getLinkedModeGroups() {
		if (fLinkGroups == null)
			return new LinkedModeGroup[0];
		Collection values= fLinkGroups.values();
		return (LinkedModeGroup[])values.toArray(new LinkedModeGroup[values.size()]);
	}

	
	private LinkedModeGroup getLinkedModeGroup(String name) {
		if (fLinkGroups == null) {
			fLinkGroups= new HashMap();
		}
		LinkedModeGroup linkedGroup= (LinkedModeGroup) fLinkGroups.get(name);
		if (linkedGroup == null) {
			linkedGroup= new LinkedModeGroup();
			fLinkGroups.put(name, linkedGroup);			
		}
		return linkedGroup;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal#performChange(org.eclipse.jface.text.IDocument, org.eclipse.ui.IEditorPart)
	 */
	protected void performChange(IEditorPart part, IDocument document) throws CoreException {
		try {
			super.performChange(part, document);
			if (part == null) {
				return;
			}
			
			if (fLinkGroups != null && !fLinkGroups.isEmpty() && part instanceof JavaEditor) {
				// enter linked mode
				ITextViewer viewer= ((JavaEditor) part).getViewer();
				enterLinkedMode(viewer);
			} else if (fSelectionDescription != null && part instanceof ITextEditor) {
				// select a result
				int pos= fSelectionDescription.getStartPosition() + fSelectionDescription.getLength();
				((ITextEditor) part).selectAndReveal(pos, 0);
			}
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		}

	}
	
	private void enterLinkedMode(ITextViewer viewer) throws BadLocationException {
		IDocument document= viewer.getDocument();
		
		LinkedModeModel model= new LinkedModeModel();
		boolean added= false;
		
		Iterator iterator= fLinkGroups.values().iterator();
		while (iterator.hasNext()) {
			LinkedModeGroup curr= (LinkedModeGroup) iterator.next();
			List positions= curr.fPositions;
			
			if (!positions.isEmpty()) {
				LinkedPositionGroup group= new LinkedPositionGroup();

				IJavaCompletionProposal[] linkedModeProposals= curr.getProposals();
				if (linkedModeProposals.length <= 1) {
					for (int i= 0; i < positions.size(); i++) {
						ITrackedNodePosition pos= (ITrackedNodePosition) positions.get(i);
						if (pos.getStartPosition() != -1) {
							group.addPosition(new LinkedPosition(document, pos.getStartPosition(), pos.getLength(), fPositionOrder.indexOf(pos)));
						}
					}
				} else {
					for (int i= 0; i < positions.size(); i++) {
						ITrackedNodePosition pos= (ITrackedNodePosition) positions.get(i);
						if (pos.getStartPosition() != -1) {
							ProposalPosition proposalPosition= new ProposalPosition(document, pos.getStartPosition(), pos.getLength(), fPositionOrder.indexOf(pos), linkedModeProposals);
							for (int j= 0; j < linkedModeProposals.length; j++) {
								if (linkedModeProposals[j] instanceof LinkedModeProposal)
									((LinkedModeProposal)linkedModeProposals[j]).addPosition(proposalPosition);
							}
							group.addPosition(proposalPosition);
						}
					}
				}
				model.addGroup(group);
				added= true;
			}
		}

		model.forceInstall();
		JavaEditor editor= getJavaEditor();
		if (editor != null) {
			model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
		}
		
		if (added) { // only set up UI if there are any positions set
			LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
			if (fSelectionDescription != null && fSelectionDescription.getStartPosition() != -1) {
				ui.setExitPosition(viewer, fSelectionDescription.getStartPosition() + fSelectionDescription.getLength(), 0, Integer.MAX_VALUE);				
			} else {
				int cursorPosition= viewer.getSelectedRange().x;
				if (cursorPosition != 0) {
					ui.setExitPosition(viewer, cursorPosition, 0, Integer.MAX_VALUE);
				}
			}	
			ui.setExitPolicy(new LinkedModeExitPolicy());
			ui.enter();
			
			IRegion region= ui.getSelectedRegion();
			viewer.setSelectedRange(region.getOffset(), region.getLength());	
			viewer.revealRange(region.getOffset(), region.getLength());
		}
	}
	
	/**
	 * Returns the currently active java editor, or <code>null</code> if it 
	 * cannot be determined.
	 * 
	 * @return  the currently active java editor, or <code>null</code>
	 */
	private JavaEditor getJavaEditor() {
		IEditorPart part= JavaPlugin.getActivePage().getActiveEditor();
		if (part instanceof JavaEditor)
			return (JavaEditor) part;
		else
			return null;
	}

	private static class LinkedModeProposal implements IJavaCompletionProposal, ICompletionProposalExtension2 {

		private String fProposal;
		private ITypeBinding fTypeProposal;
		private ICompilationUnit fCompilationUnit;
		/** The set of positions that share this proposal */
		private Set fPositions;

		public LinkedModeProposal(String proposal) {
			fProposal= proposal;
		}
		
		public LinkedModeProposal(ICompilationUnit unit, ITypeBinding typeProposal) {
			this(typeProposal.getName());
			fTypeProposal= typeProposal;
			fCompilationUnit= unit;
		}
	
		public void addPosition(Position position) {
			if (fPositions == null)
				fPositions= new HashSet();
			fPositions.add(position);
		}
	
		private ImportsStructure getImportStructure() throws CoreException {
			IJavaProject project= fCompilationUnit.getJavaProject();
			String[] prefOrder= JavaPreferencesSettings.getImportOrderPreference(project);
			int threshold= JavaPreferencesSettings.getImportNumberThreshold(project);					
			ImportsStructure impStructure= new ImportsStructure(fCompilationUnit, prefOrder, threshold, true);
			return impStructure;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
		 */
		public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
			IDocument document= viewer.getDocument();
			try {
				String replaceString= fProposal;
				ImportsStructure impStructure= null;
				if (fTypeProposal != null) {
					impStructure= getImportStructure();
					replaceString= impStructure.addImport(fTypeProposal);
				}
				IRegion region= getReplaceRegion(viewer, offset);
				document.replace(region.getOffset(), region.getLength(), replaceString);
			
				if (impStructure != null) {
					impStructure.create(false, null);
				}
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}	
		
		/*
		 * Returns the registered position for a given offset. 
		 */
		private Position getCurrentPosition(int offset) {
			if (fPositions != null) {
				for (Iterator it= fPositions.iterator(); it.hasNext();) {
					Position position= (Position) it.next();
					if (position.overlapsWith(offset, 0)) {
						return position;
					}
				}
			}
			return null;
		}
		
		/*
		 * Returns the region to be replaced by this proposal.
		 */
		private IRegion getReplaceRegion(ITextViewer viewer, int offset) {
			Position pos= getCurrentPosition(offset);
			if (pos != null)
				return new Region(pos.getOffset(), pos.getLength());
			
			Point point= viewer.getSelectedRange();
			return new Region(point.x, point.y);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
		 */
		public String getDisplayString() {
			if (fTypeProposal == null || fTypeProposal.getPackage() == null) {
				return fProposal;
			}
			StringBuffer buf= new StringBuffer();
			buf.append(fProposal);
			buf.append(JavaElementLabels.CONCAT_STRING);
			if (fTypeProposal.getPackage().isUnnamed()) {
				buf.append(JavaElementLabels.DEFAULT_PACKAGE);
			} else {
				buf.append(fTypeProposal.getPackage().getName());
			}
			return buf.toString();
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
		 */
		public Image getImage() {
			if (fTypeProposal != null) {
				ITypeBinding binding= fTypeProposal;
				if (binding.isArray()) {
					binding= fTypeProposal.getElementType();
				}
				if (binding.isPrimitive()) {
					return null;
				}
				boolean isInInterface= binding.isMember() && binding.getDeclaringClass().isInterface();
				ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(binding.isMember(), isInInterface, binding.getModifiers() | Flags.AccInterface, false);
				return JavaPlugin.getImageDescriptorRegistry().get(descriptor);
			}
			return null;
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposal#getRelevance()
		 */
		public int getRelevance() {
			return 0;
		}		
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
		 */
		public void apply(IDocument document) {
			// not called
		}

		public Point getSelection(IDocument document) { return null; }
		public String getAdditionalProposalInfo() { return null; }
		public IContextInformation getContextInformation() { return null; }
		public void selected(ITextViewer viewer, boolean smartToggle) {}
		public void unselected(ITextViewer viewer) {}
		
		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
		 */
		public boolean validate(IDocument document, int offset, DocumentEvent event) {
			// ignore event
			String insert= getDisplayString();
			if (insert == null)
				return false;
			
			int off;
			Position pos= getCurrentPosition(offset);
			if (pos != null) {
				off= pos.getOffset();
			} else {
				off= Math.max(0, offset - insert.length());
			}
			int length= offset - off;
			
			if (offset <= document.getLength()) {
				try {
					String content= document.get(off, length);
					if (insert.startsWith(content))
						return true;
				} catch (BadLocationException e) {
					JavaPlugin.log(e);
					// and ignore and return false
				}
			}
			return false;
		}
	}
	
}
