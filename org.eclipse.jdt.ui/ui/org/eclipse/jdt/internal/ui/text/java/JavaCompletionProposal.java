/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.text.java;

 
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitFlags;


public class JavaCompletionProposal implements IJavaCompletionProposal, ICompletionProposalExtension, ICompletionProposalExtension2 {

	private String fDisplayString;
	private String fReplacementString;
	private int fReplacementOffset;
	private int fReplacementLength;
	private int fCursorPosition;
	private Image fImage;
	private IContextInformation fContextInformation;
	private int fContextInformationPosition;
	private ProposalInfo fProposalInfo;
	private char[] fTriggerCharacters;
	protected boolean fToggleEating;
	protected ITextViewer fTextViewer;	
	
	private int fRelevance;
	private StyleRange fRememberedStyleRange;

	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal
	 * If set to <code>null</code>, the replacement string will be taken as display string.
	 */
	public JavaCompletionProposal(String replacementString, int replacementOffset, int replacementLength, Image image, String displayString, int relevance) {
		this(replacementString, replacementOffset, replacementLength, image, displayString, relevance, null);
	}

	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal
	 * @param viewer the text viewer for which this proposal is computed, may be <code>null</code>
	 * If set to <code>null</code>, the replacement string will be taken as display string.
	 */
	public JavaCompletionProposal(String replacementString, int replacementOffset, int replacementLength, Image image, String displayString, int relevance, ITextViewer viewer) {
		Assert.isNotNull(replacementString);
		Assert.isTrue(replacementOffset >= 0);
		Assert.isTrue(replacementLength >= 0);
				
		fReplacementString= replacementString;
		fReplacementOffset= replacementOffset;
		fReplacementLength= replacementLength;
		fImage= image;
		fDisplayString= displayString != null ? displayString : replacementString;
		fRelevance= relevance;
		fTextViewer= viewer;

		fCursorPosition= replacementString.length();
	
		fContextInformation= null;
		fContextInformationPosition= -1;
		fTriggerCharacters= null;
		fProposalInfo= null;
	}
	
	/**
	 * Sets the context information.
	 * @param contentInformation The context information associated with this proposal
	 */
	public void setContextInformation(IContextInformation contextInformation) {
		fContextInformation= contextInformation;
		fContextInformationPosition= (fContextInformation != null ? fCursorPosition : -1);
	}
	
	/**
	 * Sets the trigger characters.
	 * @param triggerCharacters The set of characters which can trigger the application of this completion proposal
	 */
	public void setTriggerCharacters(char[] triggerCharacters) {
		fTriggerCharacters= triggerCharacters;
	}
	
	/**
	 * Sets the proposal info.
	 * @param additionalProposalInfo The additional information associated with this proposal or <code>null</code>
	 */
	public void setProposalInfo(ProposalInfo proposalInfo) {
		fProposalInfo= proposalInfo;
	}
	
	/**
	 * Sets the cursor position relative to the insertion offset. By default this is the length of the completion string
	 * (Cursor positioned after the completion)
	 * @param cursorPosition The cursorPosition to set
	 */
	public void setCursorPosition(int cursorPosition) {
		Assert.isTrue(cursorPosition >= 0);
		fCursorPosition= cursorPosition;
		fContextInformationPosition= (fContextInformation != null ? fCursorPosition : -1);
	}	
	
	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char, int)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		try {
			// patch replacement length
			int delta= offset - (fReplacementOffset + fReplacementLength);
			if (delta > 0)
				fReplacementLength += delta;
			
			String string;
			if (trigger == (char) 0) {
				string= fReplacementString;
			} else {
				StringBuffer buffer= new StringBuffer(fReplacementString);

				// fix for PR #5533. Assumes that no eating takes place.
				if ((fCursorPosition > 0 && fCursorPosition <= buffer.length() && buffer.charAt(fCursorPosition - 1) != trigger)) {
					buffer.insert(fCursorPosition, trigger);
					++fCursorPosition;
				}
				
				string= buffer.toString();
			}

			replace(document, fReplacementOffset, fReplacementLength, string);

			if (fTextViewer != null && string != null) {
				int index= string.indexOf("()"); //$NON-NLS-1$
				if (index != -1) {
					
					IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
					if (preferenceStore.getBoolean(CompilationUnitEditor.CLOSE_BRACKETS)) {
	
						int newOffset= fReplacementOffset + index;
		
						LinkedPositionManager manager= new LinkedPositionManager(document);
						manager.addPosition(newOffset + 1, 0);
		
						LinkedPositionUI editor= new LinkedPositionUI(fTextViewer, manager);
						editor.setExitPolicy(new ExitPolicy(')'));
						editor.setFinalCaretOffset(newOffset + 2);
						editor.enter();							
					}
				}
			}

		} catch (BadLocationException x) {
			// ignore
		}		
	}
	
	private static class ExitPolicy implements LinkedPositionUI.ExitPolicy {
		
		final char fExitCharacter;
		
		public ExitPolicy(char exitCharacter) {
			fExitCharacter= exitCharacter;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitPolicy#doExit(org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager, org.eclipse.swt.events.VerifyEvent, int, int)
		 */
		public ExitFlags doExit(LinkedPositionManager manager, VerifyEvent event, int offset, int length) {
			
			if (event.character == fExitCharacter) {
				if (manager.anyPositionIncludes(offset, length))
					return new ExitFlags(LinkedPositionUI.COMMIT| LinkedPositionUI.UPDATE_CARET, false);
				else
					return new ExitFlags(LinkedPositionUI.COMMIT, true);
			}	
			
			switch (event.character) {			
			case '\b':
				if (manager.getFirstPosition().length == 0)
					return new ExitFlags(0, true);
				else
					return null;
				
			case '\n':
			case '\r':
				return new ExitFlags(LinkedPositionUI.COMMIT, true);
				
			default:
				return null;
			}						
		}

	}	
	
	// #6410 - File unchanged but dirtied by code assist
	private void replace(IDocument document, int offset, int length, String string) throws BadLocationException {
		if (!document.get(offset, length).equals(string))
			document.replace(offset, length, string);
	}

	/*
	 * @see ICompletionProposal#apply
	 */
	public void apply(IDocument document) {
		apply(document, (char) 0, fReplacementOffset + fReplacementLength);
	}
	
	/*
	 * @see ICompletionProposal#getSelection
	 */
	public Point getSelection(IDocument document) {
		return new Point(fReplacementOffset + fCursorPosition, 0);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return fContextInformation;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fDisplayString;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		if (fProposalInfo != null) {
			return fProposalInfo.getInfo();
		}
		return null;
	}
	
	/*
	 * @see ICompletionProposalExtension#getTriggerCharacters()
	 */
	public char[] getTriggerCharacters() {
		return fTriggerCharacters;
	}

	/*
	 * @see ICompletionProposalExtension#getContextInformationPosition()
	 */
	public int getContextInformationPosition() {
		return fReplacementOffset + fContextInformationPosition;
	}
	
	/**
	 * Gets the replacement offset.
	 * @return Returns a int
	 */
	public int getReplacementOffset() {
		return fReplacementOffset;
	}

	/**
	 * Sets the replacement offset.
	 * @param replacementOffset The replacement offset to set
	 */
	public void setReplacementOffset(int replacementOffset) {
		Assert.isTrue(replacementOffset >= 0);
		fReplacementOffset= replacementOffset;
	}	

	/**
	 * Gets the replacement length.
	 * @return Returns a int
	 */
	public int getReplacementLength() {
		return fReplacementLength;
	}

	/**
	 * Sets the replacement length.
	 * @param replacementLength The replacementLength to set
	 */
	public void setReplacementLength(int replacementLength) {
		Assert.isTrue(replacementLength >= 0);
		fReplacementLength= replacementLength;
	}

	/**
	 * Gets the replacement string.
	 * @return Returns a String
	 */
	public String getReplacementString() {
		return fReplacementString;
	}

	/**
	 * Sets the replacement string.
	 * @param replacementString The replacement string to set
	 */
	public void setReplacementString(String replacementString) {
		fReplacementString= replacementString;
	}

	/**
	 * Sets the image.
	 * @param image The image to set
	 */
	public void setImage(Image image) {
		fImage= image;
	}

	/*
	 * @see ICompletionProposalExtension#isValidFor(IDocument, int)
	 */
	public boolean isValidFor(IDocument document, int offset) {
		return validate(document, offset, null);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 */
	public boolean validate(IDocument document, int offset, DocumentEvent event) {

		if (offset < fReplacementOffset)
			return false;
				
		/* 
		 * See http://dev.eclipse.org/bugs/show_bug.cgi?id=17667
		String word= fReplacementString;
		 */ 
		boolean validated= startsWith(document, offset, fDisplayString);	

		if (validated && event != null) {
			// adapt replacement range to document change
			int delta= (event.fText == null ? 0 : event.fText.length()) - event.fLength;
			fReplacementLength += delta;	
		}

		return validated;
	}
	
	/**
	 * Gets the proposal's relevance.
	 * @return Returns a int
	 */
	public int getRelevance() {
		return fRelevance;
	}

	/**
	 * Sets the proposal's relevance.
	 * @param relevance The relevance to set
	 */
	public void setRelevance(int relevance) {
		fRelevance= relevance;
	}

	/**
	 * Returns <code>true</code> if a words starts with the code completion prefix in the document,
	 * <code>false</code> otherwise.
	 */	
	protected boolean startsWith(IDocument document, int offset, String word) {
		int wordLength= word == null ? 0 : word.length();
		if (offset >  fReplacementOffset + wordLength)
			return false;
		
		try {
			int length= offset - fReplacementOffset;
			String start= document.get(fReplacementOffset, length);
			return word.substring(0, length).equalsIgnoreCase(start);
		} catch (BadLocationException x) {
		}
		
		return false;	
	}	

	private static boolean insertCompletion() {
		IPreferenceStore preference= JavaPlugin.getDefault().getPreferenceStore();
		return preference.getBoolean(ContentAssistPreference.INSERT_COMPLETION);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension1#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {

		IDocument document= viewer.getDocument();

		fToggleEating= (stateMask & SWT.CTRL) != 0;
		if (insertCompletion() ^ fToggleEating)
			fReplacementLength= offset - fReplacementOffset;
		
		apply(document, trigger, offset);
		fToggleEating= false;
	}

	private static Color getForegroundColor(StyledText text) {

		IPreferenceStore preference= JavaPlugin.getDefault().getPreferenceStore();
		RGB rgb= PreferenceConverter.getColor(preference, ContentAssistPreference.COMPLETION_REPLACEMENT_FOREGROUND);
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		return textTools.getColorManager().getColor(rgb);
	}

	private static Color getBackgroundColor(StyledText text) {

		IPreferenceStore preference= JavaPlugin.getDefault().getPreferenceStore();
		RGB rgb= PreferenceConverter.getColor(preference, ContentAssistPreference.COMPLETION_REPLACEMENT_BACKGROUND);
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		return textTools.getColorManager().getColor(rgb);
	}
	
	private void repairPresentation(ITextViewer viewer) {
		if (fRememberedStyleRange != null) {
			if (viewer instanceof ITextViewerExtension2) {
				// attempts to reduce the redraw area
				ITextViewerExtension2 viewer2= (ITextViewerExtension2) viewer;
				viewer2.invalidateTextPresentation(fRememberedStyleRange.start, fRememberedStyleRange.length);
			} else
				viewer.invalidateTextPresentation();
		}
	}

	private void updateStyle(ITextViewer viewer) {

		StyledText text= viewer.getTextWidget();
		if (text == null || text.isDisposed())
			return;

		IRegion visibleRegion= viewer.getVisibleRegion();			
		int caretOffset= text.getCaretOffset() + visibleRegion.getOffset();

		if (caretOffset >= fReplacementOffset + fReplacementLength) {
			repairPresentation(viewer); 
			return;
		}
			
		int offset= caretOffset - visibleRegion.getOffset();
		int length= fReplacementOffset + fReplacementLength - caretOffset;
	
		Color foreground= getForegroundColor(text);
		Color background= getBackgroundColor(text);

		repairPresentation(viewer);
		fRememberedStyleRange= new StyleRange(offset, length, foreground, background);
		text.setStyleRange(fRememberedStyleRange);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(ITextViewer, boolean)
	 */
	public void selected(ITextViewer viewer, boolean smartToggle) {
		if (!insertCompletion() ^ smartToggle)
			updateStyle(viewer);
		else {
			repairPresentation(viewer);
			fRememberedStyleRange= null;
		}
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(ITextViewer)
	 */
	public void unselected(ITextViewer viewer) {
		repairPresentation(viewer);
		fRememberedStyleRange= null;
	}

}