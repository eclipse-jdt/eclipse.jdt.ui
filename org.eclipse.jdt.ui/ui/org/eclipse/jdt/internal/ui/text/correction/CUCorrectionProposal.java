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

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.compare.JavaTokenComparator;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.link.LinkedEnvironment;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionGroup;
import org.eclipse.jdt.internal.ui.text.link.LinkedUIControl;


public class CUCorrectionProposal extends ChangeCorrectionProposal  {

	private ICompilationUnit fCompilationUnit;
	private TextEdit fRootEdit;
	private ImportRewrite fImportRewrite;
	
	public CUCorrectionProposal(String name, ICompilationUnit cu, int relevance) {
		this(name, cu, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}
	
	public CUCorrectionProposal(String name, ICompilationUnit cu, int relevance, Image image) {
		super(name, null, relevance, image);
		fRootEdit= new MultiTextEdit();
		fCompilationUnit= cu;
		fImportRewrite= null;
	}
	
	public CUCorrectionProposal(String name, CompilationUnitChange change, int relevance, Image image) {
		super(name, change, relevance, image);
		fCompilationUnit= change.getCompilationUnit();
	}	
	
	/**
	 * @deprecated Override addEdits instead.
	 */
	protected final CompilationUnitChange createCompilationUnitChange(String name, ICompilationUnit cu, TextEdit rootEdit) {
		return null;
	}
	
	private CompilationUnitChange createCompilationUnitChange(String name) throws CoreException {
		CompilationUnitChange change= new CompilationUnitChange(name, getCompilationUnit());
		change.setEdit(getRootTextEdit());
		change.setSave(false);
		setChange(change);
		
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(change.getFile());
			addEdits(buffer);
			if (fImportRewrite != null && !fImportRewrite.isEmpty()) {
				getRootTextEdit().addChild(fImportRewrite.createEdit(buffer));
			}
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
		}
		return change;
	}
	
	/**
	 * Called when the <code>CompilationUnitChange</code> is created. Subclasses can override to
	 * add text edits to the change.
	 * @param buffer Buffer of the underlying compilation unit. To be accessed read only.
	 * @throws CoreException
	 */
	protected void addEdits(TextBuffer buffer) throws CoreException {
	}

	/*
	 * @see ChangeCorrectionProposal#getChange()
	 */
	protected Change getChange() throws CoreException {
		Change change= super.getChange();
		if (change == null) {
			return createCompilationUnitChange(getDisplayString());
		}
		return change;
	}
	
	// import management
	
	public ImportRewrite getImportRewrite() throws CoreException {
		if (fImportRewrite == null) {
			fImportRewrite= new ImportRewrite(getCompilationUnit(), JavaPreferencesSettings.getCodeGenerationSettings());
		}
		return fImportRewrite;
	}
	
	public void setImportRewrite(ImportRewrite rewrite) {
		fImportRewrite= rewrite;
	}
		
	protected TextEditGroup[] getLinkedRanges() {
		return null;
	}
	
	protected ICompletionProposal[] getLinkedModeProposals(String name) {
		return null;
	}
	
	
	protected TextEditGroup getSelectionDescription() {
		return null;
	}
		
	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		
		try {
			CompilationUnitChange change= getCompilationUnitChange();

			TextBuffer previewConent= new TextBuffer(change.getPreviewDocument());
			String currentConentString= change.getCurrentContent();
			
			ITokenComparator leftSide= new JavaTokenComparator(previewConent.getContent(), true); 
			ITokenComparator rightSide= new JavaTokenComparator(currentConentString, true);
			
			RangeDifference[] differences= RangeDifferencer.findRanges(leftSide, rightSide);
			for (int i= 0; i < differences.length; i++) {
				RangeDifference curr= differences[i];
				int start= leftSide.getTokenStart(curr.leftStart());
				int end= leftSide.getTokenStart(curr.leftEnd());
				if (curr.kind() == RangeDifference.CHANGE && curr.leftLength() > 0) {
					buf.append("<b>"); //$NON-NLS-1$
					appendContent(previewConent, start, end, buf, false);
					buf.append("</b>"); //$NON-NLS-1$
				} else if (curr.kind() == RangeDifference.NOCHANGE) {
					appendContent(previewConent, start, end, buf, true);
				}
			}			
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return buf.toString();
	}
	
	private final int surroundLines= 1;

	private void appendContent(TextBuffer text, int startOffset, int endOffset, StringBuffer buf, boolean surroundLinesOnly) {
		int startLine= text.getLineOfOffset(startOffset);
		int endLine= text.getLineOfOffset(endOffset);
		
		boolean dotsAdded= false;
		if (surroundLinesOnly && startOffset == 0) { // no surround lines for the top no-change range
			startLine= Math.max(endLine - surroundLines, 0);
			buf.append("...<br>"); //$NON-NLS-1$
			dotsAdded= true;
		}
		
		for (int i= startLine; i <= endLine; i++) {
			if (surroundLinesOnly) {
				if ((i - startLine > surroundLines) && (endLine - i > surroundLines)) {
					if (!dotsAdded) {
						buf.append("...<br>"); //$NON-NLS-1$
						dotsAdded= true;
					} else if (endOffset == text.getLength()) {
						return; // no surround lines for the bottom no-change range
					}
					continue;
				}
			}
			
			TextRegion lineInfo= text.getLineInformation(i);
			int start= lineInfo.getOffset();
			int end= start + lineInfo.getLength();

			int from= Math.max(start, startOffset);
			int to= Math.min(end, endOffset);
			String content= text.getContent(from, to - from);
			if (surroundLinesOnly && (from == start) && Strings.containsOnlyWhitespaces(content)) {
				continue; // ignore empty lines exept when range started in the middle of a line
			}
			buf.append(content);
			if (to == end && to != endOffset) { // new line when at the end of the line, and not end of range
				buf.append("<br>"); //$NON-NLS-1$
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(IDocument document) {
		try {
			ICompilationUnit unit= JavaModelUtil.toOriginal(getCompilationUnit());
			IStatus status= Resources.makeCommittable(unit.getResource(), null);
			if (!status.isOK()) {
				String label= CorrectionMessages.getString("CUCorrectionProposal.error.title"); //$NON-NLS-1$
				String message= CorrectionMessages.getString("CUCorrectionProposal.error.message"); //$NON-NLS-1$
				ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), label, message, status);
				return;
			}
			
			CompilationUnitChange change= getCompilationUnitChange();
			
			TextEditGroup selection= getSelectionDescription();
			TextEditGroup[] linked= getLinkedRanges();
			
			IEditorPart part= null;
			if (selection != null || linked != null) {
				change.setKeepPreviewEdits(true);
				
				part= EditorUtility.isOpenInEditor(unit);
				if (part == null) {
					part= EditorUtility.openInEditor(unit, true);
				}
				IWorkbenchPage page= JavaPlugin.getActivePage();
				if (page != null && part != null) {
					page.bringToTop(part);
				}
				if (part != null) {
					part.setFocus();
				}
			}
			
			super.apply(document);

			if (part == null) {
				return;
			}
			
			if (linked != null && part instanceof JavaEditor) {
				// enter linked mode
				ITextViewer viewer= ((JavaEditor) part).getViewer();
				enterLinkedMode(change, viewer, linked, selection);
			} else if (selection != null && part instanceof ITextEditor) {
				// select a result
				IRegion range= TextEdit.getCoverage(change.getPreviewEdits(selection.getTextEdits()));
				int pos= range.getOffset() + range.getLength();
				((ITextEditor) part).selectAndReveal(pos, 0);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}		
	}
					
	private void enterLinkedMode(CompilationUnitChange change, ITextViewer viewer, TextEditGroup[] linked, TextEditGroup selection) throws BadLocationException {
		IDocument document= viewer.getDocument();
		
		HashMap map= new HashMap();
		
		for (int i= 0; i < linked.length; i++) {
			TextEditGroup curr= linked[i];
			String name= curr.getName(); // name used as key for link mode proposals & as kind for linked mode
			LinkedPositionGroup group= (LinkedPositionGroup) map.get(name);
			if (group == null) {
				group= new LinkedPositionGroup();
				map.put(name, group);
			}
				
			TextEdit[] textEdits= curr.getTextEdits();
			if (name != null && textEdits.length > 0) {
				IRegion range= TextEdit.getCoverage(change.getPreviewEdits(textEdits));
				if (range != null) {	// all edits could be deleted
					ICompletionProposal[] linkedModeProposals= getLinkedModeProposals(name);
					if (linkedModeProposals != null && linkedModeProposals.length > 1) {
						group.createPosition(document, range.getOffset(), range.getLength(), i, linkedModeProposals);
					} else {
						group.createPosition(document, range.getOffset(), range.getLength(), i);
					}
				}
			}
		}
		
		LinkedEnvironment environment= new LinkedEnvironment();
		boolean added= false;
		for (Iterator it= map.values().iterator(); it.hasNext(); ) {
			LinkedPositionGroup group= (LinkedPositionGroup) it.next();
			if (!group.isEmtpy()) {
				environment.addGroup(group);
				added= true;
			}
		}
		
		environment.forceInstall();
		
		if (added) { // only set up UI if there are any positions set
			LinkedUIControl ui= new LinkedUIControl(environment, viewer);
			if (selection != null) {
				TextEdit[] textEdits= selection.getTextEdits();
				if (textEdits.length > 0) {
					IRegion range= TextEdit.getCoverage(change.getPreviewEdits(textEdits));
					if (range != null)
						ui.setExitPosition(viewer, range.getOffset() + range.getLength(), 0, Integer.MAX_VALUE);
				}					
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

	/**
	 * Gets the compilationUnitChange.
	 * @return Returns a CompilationUnitChange
	 */
	public CompilationUnitChange getCompilationUnitChange() throws CoreException {
		return (CompilationUnitChange) getChange();
	}

	/**
	 * @return Returns the root text edit
	 */
	public TextEdit getRootTextEdit() {
		return fRootEdit;
	}
	
	/**
	 * Returns the compilationUnit.
	 * @return ICompilationUnit
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		try {
			CompilationUnitChange change= getCompilationUnitChange();
			return change.getPreviewContent();
		} catch (CoreException e) {
		}
		return super.toString();
	}



}
