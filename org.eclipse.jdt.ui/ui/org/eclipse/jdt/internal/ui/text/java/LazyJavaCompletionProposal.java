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

package org.eclipse.jdt.internal.ui.text.java;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.CompletionProposal;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jdt.ui.text.java.CompletionProposalLabelProvider;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class LazyJavaCompletionProposal implements IJavaCompletionProposal, ICompletionProposalExtension, ICompletionProposalExtension2, ICompletionProposalExtension3 {
	/**
	 * A class to simplify tracking a reference position in a document.
	 */
	static final class ReferenceTracker {
	
		/** The reference position category name. */
		private static final String CATEGORY= "reference_position"; //$NON-NLS-1$
		/** The position updater of the reference position. */
		private final IPositionUpdater fPositionUpdater= new DefaultPositionUpdater(CATEGORY);
		/** The reference position. */
		private final Position fPosition= new Position(0);
	
		/**
		 * Called before document changes occur. It must be followed by a call to postReplace().
		 *
		 * @param document the document on which to track the reference position.
		 *
		 */
		public void preReplace(IDocument document, int offset) throws BadLocationException {
			fPosition.setOffset(offset);
			try {
				document.addPositionCategory(CATEGORY);
				document.addPositionUpdater(fPositionUpdater);
				document.addPosition(CATEGORY, fPosition);
	
			} catch (BadPositionCategoryException e) {
				// should not happen
				JavaPlugin.log(e);
			}
		}
	
		/**
		 * Called after the document changed occured. It must be preceded by a call to preReplace().
		 *
		 * @param document the document on which to track the reference position.
		 */
		public int postReplace(IDocument document) {
			try {
				document.removePosition(CATEGORY, fPosition);
				document.removePositionUpdater(fPositionUpdater);
				document.removePositionCategory(CATEGORY);
	
			} catch (BadPositionCategoryException e) {
				// should not happen
				JavaPlugin.log(e);
			}
			return fPosition.getOffset();
		}
	}

	private boolean fDisplayStringComputed;
	private String fDisplayString;
	
	private boolean fReplacementStringComputed;
	private String fReplacementString;
	
	private boolean fReplacementOffsetComputed;
	private int fReplacementOffset;
	
	private boolean fReplacementLengthComputed;
	private int fReplacementLength;
	
	private boolean fCursorPositionComputed;
	private int fCursorPosition;
	
	private boolean fImageComputed;
	private Image fImage;
	
	private boolean fContextInformationComputed;
	private IContextInformation fContextInformation;
	
	private boolean fProposalInfoComputed;
	private ProposalInfo fProposalInfo;
	
	private boolean fTriggerCharactersComputed;
	private char[] fTriggerCharacters;
	
	private boolean fSortStringComputed;
	private String fSortString;

	private boolean fRelevanceComputed;
	private int fRelevance;
	
	protected final CompletionProposal fProposal;
	private StyleRange fRememberedStyleRange;
	private boolean fToggleEating;
	private ITextViewer fTextViewer;

	public LazyJavaCompletionProposal(CompletionProposal proposal) {
		Assert.isNotNull(proposal);
		fProposal= proposal;
	}

	/*
	 * @see ICompletionProposalExtension#getTriggerCharacters()
	 */
	public final char[] getTriggerCharacters() {
		if (!fTriggerCharactersComputed)
			setTriggerCharacters(computeTriggerCharacters());
		return fTriggerCharacters;
	}
	
	protected char[] computeTriggerCharacters() {
		return new char[0];
	}

	/**
	 * Sets the trigger characters.
	 * @param triggerCharacters The set of characters which can trigger the application of this completion proposal
	 */
	public final void setTriggerCharacters(char[] triggerCharacters) {
		fTriggerCharactersComputed= true;
		fTriggerCharacters= triggerCharacters;
	}

	/**
	 * Sets the proposal info.
	 * @param proposalInfo The additional information associated with this proposal or <code>null</code>
	 */
	public final void setProposalInfo(ProposalInfo proposalInfo) {
		fProposalInfoComputed= true;
		fProposalInfo= proposalInfo;
	}

	/**
	 * Returns the additional proposal info, or <code>null</code> if none
	 * exists.
	 *
	 * @return the additional proposal info, or <code>null</code> if none
	 *         exists
	 */
	public final ProposalInfo getProposalInfo() {
		if (!fProposalInfoComputed)
			setProposalInfo(computeProposalInfo());
		return fProposalInfo;
	}

	protected ProposalInfo computeProposalInfo() {
		return null;
	}

	/**
	 * Sets the cursor position relative to the insertion offset. By default this is the length of the completion string
	 * (Cursor positioned after the completion)
	 * @param cursorPosition The cursorPosition to set
	 */
	public final void setCursorPosition(int cursorPosition) {
		Assert.isTrue(cursorPosition >= 0);
		fCursorPositionComputed= true;
		fCursorPosition= cursorPosition;
	}
	
	protected final int getCursorPosition() {
		if (!fCursorPositionComputed)
			setCursorPosition(computeCursorPosition());
		return fCursorPosition;
	}

	protected int computeCursorPosition() {
		return getReplacementString().length();
	}

	/*
	 * @see ICompletionProposal#apply
	 */
	public final void apply(IDocument document) {
		// not used any longer
		apply(document, (char) 0, getReplacementOffset() + getReplacementLength());
	}
	
	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension#apply(org.eclipse.jface.text.IDocument, char, int)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		try {
			// patch replacement length
			int delta= offset - (getReplacementOffset() + getReplacementLength());
			if (delta > 0)
				setReplacementLength(getReplacementLength() + delta);
	
			boolean isSmartTrigger= isSmartTrigger(trigger);
	
			String replacement;
			if (isSmartTrigger || trigger == (char) 0) {
				replacement= getReplacementString();
			} else {
				StringBuffer buffer= new StringBuffer(getReplacementString());
	
				// fix for PR #5533. Assumes that no eating takes place.
				if ((getCursorPosition() > 0 && getCursorPosition() <= buffer.length() && buffer.charAt(getCursorPosition() - 1) != trigger)) {
					buffer.insert(getCursorPosition(), trigger);
					setCursorPosition(getCursorPosition() + 1);
				}
	
				replacement= buffer.toString();
				setReplacementString(replacement);
			}
	
			// reference position just at the end of the document change.
			int referenceOffset= getReplacementOffset() + getReplacementLength();
			final ReferenceTracker referenceTracker= new JavaMethodCompletionProposal.ReferenceTracker();
			referenceTracker.preReplace(document, referenceOffset);
	
			replace(document, getReplacementOffset(), getReplacementLength(), replacement);
	
			referenceOffset= referenceTracker.postReplace(document);
			setReplacementOffset(referenceOffset - (replacement == null ? 0 : replacement.length()));
	
			// PR 47097
			if (isSmartTrigger)
				handleSmartTrigger(document, trigger, referenceOffset);
	
		} catch (BadLocationException x) {
			// ignore
		}
	}

	private boolean isSmartTrigger(char trigger) {
		return trigger == ';' && JavaPlugin.getDefault().getCombinedPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_SEMICOLON)
				|| trigger == '{' && JavaPlugin.getDefault().getCombinedPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_OPENING_BRACE);
	}

	private void handleSmartTrigger(IDocument document, char trigger, int referenceOffset) throws BadLocationException {
		DocumentCommand cmd= new DocumentCommand() {
		};
		
		cmd.offset= referenceOffset;
		cmd.length= 0;
		cmd.text= Character.toString(trigger);
		cmd.doit= true;
		cmd.shiftsCaret= true;
		cmd.caretOffset= getReplacementOffset() + getCursorPosition();
		
		SmartSemicolonAutoEditStrategy strategy= new SmartSemicolonAutoEditStrategy(IJavaPartitions.JAVA_PARTITIONING);
		strategy.customizeDocumentCommand(document, cmd);
		
		replace(document, cmd.offset, cmd.length, cmd.text);
		setCursorPosition(cmd.caretOffset - getReplacementOffset() + cmd.text.length());
	}
	
	protected final void replace(IDocument document, int offset, int length, String string) throws BadLocationException {
		if (!document.get(offset, length).equals(string))
			document.replace(offset, length, string);
	}
	
	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension1#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {

		IDocument document= viewer.getDocument();
		if (fTextViewer == null)
			fTextViewer= viewer;

		// don't eat if not in preferences, XOR with modifier key 1 (Ctrl)
		// but: if there is a selection, replace it!
		Point selection= viewer.getSelectedRange();
		fToggleEating= (stateMask & SWT.MOD1) != 0;
		int newLength= selection.x + selection.y - getReplacementOffset();
		if ((insertCompletion() ^ fToggleEating) && newLength >= 0)
			setReplacementLength(newLength);

		apply(document, trigger, offset);
		fToggleEating= false;
	}

	/*
	 * @see ICompletionProposal#getSelection
	 */
	public Point getSelection(IDocument document) {
		return new Point(getReplacementOffset() + getCursorPosition(), 0);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public final IContextInformation getContextInformation() {
		if (!fContextInformationComputed)
			setContextInformation(computeContextInformation());
		return fContextInformation;
	}

	protected IContextInformation computeContextInformation() {
		return null;
	}

	/**
	 * Sets the context information.
	 * @param contextInformation The context information associated with this proposal
	 */
	public final void setContextInformation(IContextInformation contextInformation) {
		fContextInformationComputed= true;
		fContextInformation= contextInformation;
	}
	
	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public final String getDisplayString() {
		if (!fDisplayStringComputed) {
			fDisplayStringComputed= true;
			fDisplayString= computeDisplayString();
		}
		return fDisplayString;
	}

	protected String computeDisplayString() {
		return new CompletionProposalLabelProvider().createLabel(fProposal);
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public final String getAdditionalProposalInfo() {
		if (getProposalInfo() != null) {
			return getProposalInfo().getInfo();
		}
		return null;
	}

	/*
	 * @see ICompletionProposalExtension#getContextInformationPosition()
	 */
	public final int getContextInformationPosition() {
		if (getContextInformation() == null)
			return getReplacementOffset() - 1;
		return getReplacementOffset() + getCursorPosition();
	}

	/**
	 * Gets the replacement offset.
	 * @return Returns a int
	 */
	public final int getReplacementOffset() {
		if (!fReplacementOffsetComputed)
			setReplacementOffset(fProposal.getReplaceStart());
		return fReplacementOffset;
	}

	/**
	 * Sets the replacement offset.
	 * @param replacementOffset The replacement offset to set
	 */
	public final void setReplacementOffset(int replacementOffset) {
		Assert.isTrue(replacementOffset >= 0);
		fReplacementOffsetComputed= true;
		fReplacementOffset= replacementOffset;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getCompletionOffset()
	 */
	public final int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return getReplacementOffset();
	}

	/**
	 * Gets the replacement length.
	 * @return Returns a int
	 */
	public final int getReplacementLength() {
		if (!fReplacementLengthComputed)
			setReplacementLength(fProposal.getReplaceEnd() - fProposal.getReplaceStart());
		return fReplacementLength;
	}

	/**
	 * Sets the replacement length.
	 * @param replacementLength The replacementLength to set
	 */
	public final void setReplacementLength(int replacementLength) {
		Assert.isTrue(replacementLength >= 0);
		fReplacementLengthComputed= true;
		fReplacementLength= replacementLength;
	}

	/**
	 * Gets the replacement string.
	 * @return Returns a String
	 */
	public final String getReplacementString() {
		if (!fReplacementStringComputed)
			setReplacementString(computeReplacementString());
		return fReplacementString;
	}

	protected String computeReplacementString() {
		return String.valueOf(fProposal.getCompletion());
	}

	/**
	 * Sets the replacement string.
	 * @param replacementString The replacement string to set
	 */
	public final void setReplacementString(String replacementString) {
		Assert.isNotNull(replacementString);
		fReplacementStringComputed= true;
		fReplacementString= replacementString;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getReplacementText()
	 */
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return getReplacementString();
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public final Image getImage() {
		if (!fImageComputed)
			setImage(computeImage());
		return fImage;
	}

	protected Image computeImage() {
		return JavaPlugin.getImageDescriptorRegistry().get(new CompletionProposalLabelProvider().createImageDescriptor(fProposal));
	}

	/**
	 * Sets the image.
	 * @param image The image to set
	 */
	public final void setImage(Image image) {
		fImageComputed= true;
		fImage= image;
	}

	/*
	 * @see ICompletionProposalExtension#isValidFor(IDocument, int)
	 */
	public final boolean isValidFor(IDocument document, int offset) {
		return validate(document, offset, null);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 */
	public boolean validate(IDocument document, int offset, DocumentEvent event) {

		if (offset < getReplacementOffset())
			return false;

		/*
		 * See http://dev.eclipse.org/bugs/show_bug.cgi?id=17667
		String word= fReplacementString;
		 */
		boolean validated= startsWith(document, offset, getDisplayString()); // TODO remove early display string reference

		if (validated && event != null) {
			// adapt replacement range to document change
			int delta= (event.fText == null ? 0 : event.fText.length()) - event.fLength;
			final int newLength= Math.max(getReplacementLength() + delta, 0);
			setReplacementLength(newLength);
		}

		return validated;
	}

	/**
	 * Gets the proposal's relevance.
	 * @return Returns a int
	 */
	public final int getRelevance() {
		if (!fRelevanceComputed)
			setRelevance(computeRelevance());
		return fRelevance;
	}

	/**
	 * Sets the proposal's relevance.
	 * @param relevance The relevance to set
	 */
	public final void setRelevance(int relevance) {
		fRelevanceComputed= true;
		fRelevance= relevance;
	}

	/**
	 * Returns <code>true</code> if a words starts with the code completion prefix in the document,
	 * <code>false</code> otherwise.
	 */
	protected final boolean startsWith(IDocument document, int offset, String word) {
		int wordLength= word == null ? 0 : word.length();
		if (offset >  getReplacementOffset() + wordLength)
			return false;

		try {
			int length= offset - getReplacementOffset();
			String start= document.get(getReplacementOffset(), length);
			return word.substring(0, length).equalsIgnoreCase(start);
		} catch (BadLocationException x) {
		}

		return false;
	}

	private static boolean insertCompletion() {
		IPreferenceStore preference= JavaPlugin.getDefault().getPreferenceStore();
		return preference.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
	}

	private static Color getForegroundColor(StyledText text) {

		IPreferenceStore preference= JavaPlugin.getDefault().getPreferenceStore();
		RGB rgb= PreferenceConverter.getColor(preference, PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND);
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		return textTools.getColorManager().getColor(rgb);
	}

	private static Color getBackgroundColor(StyledText text) {

		IPreferenceStore preference= JavaPlugin.getDefault().getPreferenceStore();
		RGB rgb= PreferenceConverter.getColor(preference, PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND);
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		return textTools.getColorManager().getColor(rgb);
	}

	private void repairPresentation(ITextViewer viewer) {
		if (fRememberedStyleRange != null) {
			 if (viewer instanceof ITextViewerExtension2) {
			 	// attempts to reduce the redraw area
			 	ITextViewerExtension2 viewer2= (ITextViewerExtension2) viewer;

			 	if (viewer instanceof ITextViewerExtension5) {

			 		ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
			 		IRegion modelRange= extension.widgetRange2ModelRange(new Region(fRememberedStyleRange.start, fRememberedStyleRange.length));
			 		if (modelRange != null)
			 			viewer2.invalidateTextPresentation(modelRange.getOffset(), modelRange.getLength());

			 	} else {
					viewer2.invalidateTextPresentation(fRememberedStyleRange.start + viewer.getVisibleRegion().getOffset(), fRememberedStyleRange.length);
			 	}

			} else
				viewer.invalidateTextPresentation();
		}
	}

	private void updateStyle(ITextViewer viewer) {

		StyledText text= viewer.getTextWidget();
		if (text == null || text.isDisposed())
			return;

		int widgetCaret= text.getCaretOffset();

		int modelCaret= 0;
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
			modelCaret= extension.widgetOffset2ModelOffset(widgetCaret);
		} else {
			IRegion visibleRegion= viewer.getVisibleRegion();
			modelCaret= widgetCaret + visibleRegion.getOffset();
		}

		if (modelCaret >= getReplacementOffset() + getReplacementLength()) {
			repairPresentation(viewer);
			return;
		}

		int offset= widgetCaret;
		int length= getReplacementOffset() + getReplacementLength() - modelCaret;

		Color foreground= getForegroundColor(text);
		Color background= getBackgroundColor(text);

		StyleRange range= text.getStyleRangeAtOffset(offset);
		int fontStyle= range != null ? range.fontStyle : SWT.NORMAL;

		repairPresentation(viewer);
		fRememberedStyleRange= new StyleRange(offset, length, foreground, background, fontStyle);
		if (range != null) {
			fRememberedStyleRange.strikeout= range.strikeout;
			fRememberedStyleRange.underline= range.underline;
		}

		// http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
		try {
			text.setStyleRange(fRememberedStyleRange);
		} catch (IllegalArgumentException x) {
			// catching exception as offset + length might be outside of the text widget
			fRememberedStyleRange= null;
		}
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

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getInformationControlCreator()
	 */
	public IInformationControlCreator getInformationControlCreator() {
		return null;
	}

	protected int computeRelevance() {
		final int baseRelevance= fProposal.getRelevance() * 16;
		switch (fProposal.getKind()) {
			case CompletionProposal.PACKAGE_REF:
				return baseRelevance + 0;
			case CompletionProposal.LABEL_REF:
				return baseRelevance + 1;
			case CompletionProposal.KEYWORD:
				return baseRelevance + 2;
			case CompletionProposal.TYPE_REF:
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
				return baseRelevance + 3;
			case CompletionProposal.METHOD_REF:
			case CompletionProposal.METHOD_NAME_REFERENCE:
			case CompletionProposal.METHOD_DECLARATION:
			case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
				return baseRelevance + 4;
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
				return baseRelevance + 4 /* + 99 */;
			case CompletionProposal.FIELD_REF:
				return baseRelevance + 5;
			case CompletionProposal.LOCAL_VARIABLE_REF:
			case CompletionProposal.VARIABLE_DECLARATION:
				return baseRelevance + 6;
			default:
				return baseRelevance;
		}
	}
	
	public final String getSortString() {
		if (!fSortStringComputed)
			setSortString(computeSortString());
		return fSortString;
	}

	protected final void setSortString(String string) {
		fSortStringComputed= true;
		fSortString= string;
	}

	protected String computeSortString() {
		return getDisplayString();
	}

	protected final ITextViewer getTextViewer() {
		return fTextViewer;
	}

	protected final boolean isToggleEating() {
		return fToggleEating;
	}
}
