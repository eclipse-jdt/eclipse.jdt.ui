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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedEnvironment;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedUIControl;
import org.eclipse.jface.text.link.ProposalPosition;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.link.EditorHistoryUpdater;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ITrackedNodePosition;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 *
 */
public class LinkedCorrectionProposal extends ASTRewriteCorrectionProposal {

	private static class LinkedModeGroup {
		public List positions= new ArrayList(); // list of ITrackedNodePosition
		public List proposals= new ArrayList(); // list of ICompletionProposal
	}
	
	private ITrackedNodePosition fSelectionDescription;
	private Map fLinkGroups;
	private List fPositionOrder;

	public LinkedCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, rewrite, relevance, image);
		fSelectionDescription= null;
		fLinkGroups= null;
	}
		
	
	public void markAsSelection(ASTRewrite rewrite, ASTNode node) {
		fSelectionDescription= rewrite.markAsTracked(node);
	}
	
	
	public void addLinkedModeProposal(String name, String proposal) {
		addLinkedModeProposal(name, new LinkedModeProposal(proposal));
	}
	
	public void addLinkedModeProposal(String name, ITypeBinding proposal) {
		addLinkedModeProposal(name, new LinkedModeProposal(getCompilationUnit(), proposal));
	}	
	
	public void addLinkedModeProposal(String name, IJavaCompletionProposal proposal) {
		getLinkedModeGroup(name).proposals.add(proposal);
	}
	
	public void markAsLinked(ASTRewrite rewrite, ASTNode node, boolean isFirst, String name) {
		ITrackedNodePosition position= rewrite.markAsTracked(node);
		getLinkedModeGroup(name).positions.add(position);
		if (fPositionOrder == null) {
			fPositionOrder= new ArrayList();
		}
		if (isFirst) {
			fPositionOrder.add(0, position);
		} else {
			fPositionOrder.add(position);
		}
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
	
	/*(non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal#performChange(org.eclipse.jface.text.IDocument, org.eclipse.ui.IEditorPart)
	 */
	protected void performChange(IDocument document, IEditorPart part) throws CoreException {
		try {
			super.performChange(document, part);

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
		
		LinkedEnvironment environment= new LinkedEnvironment();
		boolean added= false;
		
		Iterator iterator= fLinkGroups.values().iterator();
		while (iterator.hasNext()) {
			LinkedModeGroup curr= (LinkedModeGroup) iterator.next();
			List positions= curr.positions;
			
			if (!positions.isEmpty()) {
				LinkedPositionGroup group= new LinkedPositionGroup();

				List proposals= curr.proposals;
				if (proposals.size() <= 1) {
					for (int i= 0; i < positions.size(); i++) {
						ITrackedNodePosition pos= (ITrackedNodePosition) positions.get(i);
						if (pos.getStartPosition() != -1) {
							group.createPosition(document, pos.getStartPosition(), pos.getLength(), fPositionOrder.indexOf(pos));
						}
					}
				} else {
					ICompletionProposal[] linkedModeProposals= (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
					Arrays.sort(linkedModeProposals, JavaCompletionProposalComparator.getInstance());
					for (int i= 0; i < positions.size(); i++) {
						ITrackedNodePosition pos= (ITrackedNodePosition) positions.get(i);
						if (pos.getStartPosition() != -1) {
							group.addPosition(new ProposalPosition(document, pos.getStartPosition(), pos.getLength(), fPositionOrder.indexOf(pos), linkedModeProposals));
						}
					}
				}
				environment.addGroup(group);
				added= true;
			}
		}

		environment.forceInstall();
		
		if (added) { // only set up UI if there are any positions set
			LinkedUIControl ui= new LinkedUIControl(environment, viewer);
			ui.setPositionListener(new EditorHistoryUpdater());
			if (fSelectionDescription != null && fSelectionDescription.getStartPosition() != -1) {
				ui.setExitPosition(viewer, fSelectionDescription.getStartPosition() + fSelectionDescription.getLength(), 0, Integer.MAX_VALUE);				
			} else {
				int cursorPosition= viewer.getSelectedRange().x;
				if (cursorPosition != 0) {
					ui.setExitPosition(viewer, cursorPosition, 0, Integer.MAX_VALUE);
				}
			}	
			ui.enter();
			
			IRegion region= ui.getSelectedRegion();
			viewer.setSelectedRange(region.getOffset(), region.getLength());	
			viewer.revealRange(region.getOffset(), region.getLength());
		}
	}
	
	
	public static class LinkedModeProposal implements IJavaCompletionProposal, ICompletionProposalExtension2 {

		private String fProposal;
		private ITypeBinding fTypeProposal;
		private ICompilationUnit fCompilationUnit;

		public LinkedModeProposal(String proposal) {
			fProposal= proposal;
		}
	
		public LinkedModeProposal(ICompilationUnit unit, ITypeBinding typeProposal) {
			this(typeProposal.getName());
			fTypeProposal= typeProposal;
			fCompilationUnit= unit;
		}
	
		private ImportsStructure getImportStructure() throws CoreException {
			IPreferenceStore store= PreferenceConstants.getPreferenceStore();
			String[] prefOrder= JavaPreferencesSettings.getImportOrderPreference(store);
			int threshold= JavaPreferencesSettings.getImportNumberThreshold(store);					
			ImportsStructure impStructure= new ImportsStructure(fCompilationUnit, prefOrder, threshold, true);
			return impStructure;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
		 */
		public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
			IDocument document= viewer.getDocument();
			Point point= viewer.getSelectedRange();
			try {
				String replaceString= fProposal;
				ImportsStructure impStructure= null;
				if (fTypeProposal != null) {
					impStructure= getImportStructure();
					replaceString= impStructure.addImport(fTypeProposal);
				}
				document.replace(point.x, point.y, replaceString);
			
				if (impStructure != null) {
					impStructure.create(false, null);
				}
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
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
				ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(binding.isInterface(), binding.isMember(), isInInterface, binding.getModifiers());
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
			
			int start= offset - insert.length();
			if (start >= 0 && offset <= document.getLength()) {
				try {
					String content= document.get(start, offset - start);
//					while (content.length() > 0) {
						if (insert.startsWith(content))
							return true;
//						else
//							content= content.substring(1);
//					}
				} catch (BadLocationException e) {
					JavaPlugin.log(e);
					// and ignore and return false
				}
			}
			return false;
		}
	}
	
}
