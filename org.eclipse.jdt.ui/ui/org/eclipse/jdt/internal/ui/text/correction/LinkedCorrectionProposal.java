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
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 *
 */
public class LinkedCorrectionProposal extends ASTRewriteCorrectionProposal {
	
	private TextEditGroup fSelectionDescription;
	private List fLinkedPositions;
	private Map fLinkProposals;

	public LinkedCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, rewrite, relevance, image);
		fSelectionDescription= null;
		fLinkedPositions= null;
		fLinkProposals= null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getSelectionDescription()
	 */
	protected TextEditGroup getSelectionDescription() {
		return fSelectionDescription;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getLinkedRanges()
	 */
	protected TextEditGroup[] getLinkedRanges() {
		if (fLinkedPositions != null && !fLinkedPositions.isEmpty()) {
			return (TextEditGroup[]) fLinkedPositions.toArray(new TextEditGroup[fLinkedPositions.size()]);
		}
		return null;
	}
	
	public TextEditGroup markAsSelection(ASTRewrite rewrite, ASTNode node) {
		fSelectionDescription= new TextEditGroup("selection"); //$NON-NLS-1$
		rewrite.markAsTracked(node, fSelectionDescription);
		return fSelectionDescription;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getLinkedModeProposals(java.lang.String)
	 */
	protected ICompletionProposal[] getLinkedModeProposals(String name) {
		if (fLinkProposals == null) {
			return null;
		}
		List proposals= (List) fLinkProposals.get(name);
		if (proposals != null) {
			ICompletionProposal[] res= (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
			if (res.length > 1) {
				// keep first entry at first position
				Arrays.sort(res, 1, res.length - 1, new JavaCompletionProposalComparator());
			}
			return res;
		}
		return null;
	}
	
	public void addLinkedModeProposal(String name, String proposal) {
		addLinkedModeProposal(name, new LinkedModeProposal(proposal));
	}
	
	public void addLinkedModeProposal(String name, ITypeBinding proposal) {
		addLinkedModeProposal(name, new LinkedModeProposal(getCompilationUnit(), proposal));
	}	
	
	public void addLinkedModeProposal(String name, IJavaCompletionProposal proposal) {
		if (fLinkProposals == null) {
			fLinkProposals= new HashMap();
		}
		List proposals= (List) fLinkProposals.get(name);
		if (proposals == null) {
			proposals= new ArrayList(10);
			fLinkProposals.put(name, proposals);			
		}
		proposals.add(proposal);
	}
	
	public TextEditGroup markAsLinked(ASTRewrite rewrite, ASTNode node, boolean isFirst, String kind) {
		TextEditGroup description= new TextEditGroup(kind);
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
	
	public void setSelectionDescription(TextEditGroup desc) {
		fSelectionDescription= desc;
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
				ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(binding.isInterface(), binding.isMember(), binding.getModifiers());
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
		public boolean validate(IDocument document, int offset, DocumentEvent event) { return false;}
	}
	
}
