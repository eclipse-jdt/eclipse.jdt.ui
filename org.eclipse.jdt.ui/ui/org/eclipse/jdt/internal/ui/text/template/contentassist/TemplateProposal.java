/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.template.contentassist;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension7;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.template.java.JavaDocContext;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.IndentUtil;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * A template proposal.
 */
public class TemplateProposal
		implements IJavaCompletionProposal, ICompletionProposalExtension2, ICompletionProposalExtension3, ICompletionProposalExtension4, ICompletionProposalExtension6, ICompletionProposalExtension7 {

	private final Template fTemplate;
	private final TemplateContext fContext;
	private final Image fImage;
	private final IRegion fRegion;
	private int fRelevance;
	private boolean fIsSubstringMatch;

	private IRegion fSelectedRegion; // initialized by apply()
	private StyledString fDisplayString;
	private InclusivePositionUpdater fUpdater;

	/**
	 * Creates a template proposal with a template and its context.
	 *
	 * @param template  the template
	 * @param context   the context in which the template was requested.
	 * @param region	the region this proposal is applied to
	 * @param image     the icon of the proposal.
	 */
	public TemplateProposal(Template template, TemplateContext context, IRegion region, Image image) {
		Assert.isNotNull(template);
		Assert.isNotNull(context);
		Assert.isNotNull(region);

		fTemplate= template;
		fContext= context;
		fImage= image;
		fRegion= region;

		fDisplayString= null;

		fRelevance= computeRelevance();
	}

	/**
	 * Computes the relevance to match the relevance values generated by the
	 * core content assistant.
	 *
	 * @return a sensible relevance value.
	 */
	private int computeRelevance() {
		// see org.eclipse.jdt.internal.codeassist.RelevanceConstants
		final int R_DEFAULT= 30;
		final int R_INTERESTING= 5;
		final int R_CASE= 10;
		final int R_NON_RESTRICTED= 3;
		final int R_EXACT_NAME = 4;
		final int R_INLINE_TAG = 31;

		int base= R_DEFAULT + R_INTERESTING + R_NON_RESTRICTED;
		boolean isSubstring= false;
		try {
			if (fContext instanceof DocumentTemplateContext) {
				DocumentTemplateContext templateContext= (DocumentTemplateContext) fContext;
				IDocument document= templateContext.getDocument();

				String content= document.get(fRegion.getOffset(), fRegion.getLength());
				String templateName= fTemplate.getName();
				if (content.length() > 0 && templateName.startsWith(content))
					base += R_CASE;
				if (templateName.equalsIgnoreCase(content))
					base += R_EXACT_NAME;
				if (fContext instanceof JavaDocContext)
					base += R_INLINE_TAG;

				String templateNameLC= templateName.toLowerCase();
				String contentLC= content.toLowerCase();
				isSubstring= content.length() > 0 && !templateNameLC.startsWith(contentLC) && templateNameLC.contains(contentLC);
			}
		} catch (BadLocationException e) {
			// ignore - not a case sensitive match then
		}

		// see CompletionProposalCollector.computeRelevance
		// just under keywords, but better than packages
		final int TEMPLATE_RELEVANCE= 1;
		int rel= base * 16 + TEMPLATE_RELEVANCE;
		return isSubstring ? rel - 400 : rel;
	}

	/**
	 * Returns the template of this proposal.
	 *
	 * @return the template of this proposal
	 * @since 3.1
	 */
	public final Template getTemplate() {
		return fTemplate;
	}

	/**
	 * Returns the context in which the template was requested.
	 *
	 * @return the context in which the template was requested
	 * @since 3.1
	 */
	protected final TemplateContext getContext() {
		return fContext;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @deprecated This method is no longer called by the framework and clients should overwrite
	 *             {@link #apply(ITextViewer, char, int, int)} instead
	 */
	@Deprecated
	@Override
	public final void apply(IDocument document) {
		// not called anymore
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {

		IDocument document= viewer.getDocument();
		try {
			fContext.setReadOnly(false);
			int start;
			TemplateBuffer templateBuffer;
			try {
				beginCompoundChange(viewer);

				int oldReplaceOffset= getReplaceOffset();
				try {
					// this may already modify the document (e.g. add imports)
					templateBuffer= fContext.evaluate(fTemplate);
				} catch (TemplateException e1) {
					fSelectedRegion= fRegion;
					return;
				}

				start= getReplaceOffset();
				int shift= start - oldReplaceOffset;
				int end= Math.max(getReplaceEndOffset(), offset + shift);

				// insert template string
				if (end > document.getLength())
					end= offset;
				String templateString= templateBuffer.getString();
				document.replace(start, end - start, templateString);
			} finally {
				endCompoundChange(viewer);
			}

			// translate positions
			LinkedModeModel model= new LinkedModeModel();
			TemplateVariable[] variables= templateBuffer.getVariables();

			MultiVariableGuess guess= fContext instanceof JavaContext ? ((JavaContext) fContext).getMultiVariableGuess() : null;

			boolean hasPositions= false;
			for (TemplateVariable variable : variables) {
				if (variable.isUnambiguous())
					continue;

				LinkedPositionGroup group= new LinkedPositionGroup();

				int[] offsets= variable.getOffsets();
				int length= variable.getLength();

				LinkedPosition first;
				if (guess != null && variable instanceof MultiVariable) {
					first= new VariablePosition(document, offsets[0] + start, length, guess, (MultiVariable) variable);
					guess.addSlave((VariablePosition) first);
				} else {
					String[] values= variable.getValues();
					ICompletionProposal[] proposals= new ICompletionProposal[values.length];
					for (int j= 0; j < values.length; j++) {
						ensurePositionCategoryInstalled(document, model);
						Position pos= new Position(offsets[0] + start, length);
						document.addPosition(getCategory(), pos);
						proposals[j]= new PositionBasedCompletionProposal(values[j], pos, length);
					}

					if (proposals.length > 1)
						first= new ProposalPosition(document, offsets[0] + start, length, proposals);
					else
						first= new LinkedPosition(document, offsets[0] + start, length);
				}

				for (int j= 0; j != offsets.length; j++)
					if (j == 0)
						group.addPosition(first);
					else
						group.addPosition(new LinkedPosition(document, offsets[j] + start, length));

				model.addGroup(group);
				hasPositions= true;
			}

			if (hasPositions) {
				model.forceInstall();
				JavaEditor editor= getJavaEditor();
				if (editor != null) {
					model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
				}

				LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
				ui.setExitPosition(viewer, getCaretOffset(templateBuffer) + start, 0, Integer.MAX_VALUE);
				ui.enter();

				fSelectedRegion= ui.getSelectedRegion();
			} else {
				fSelectedRegion= new Region(getCaretOffset(templateBuffer) + start, 0);
			}

		} catch (BadLocationException | BadPositionCategoryException e) {
			JavaPlugin.log(e);
			openErrorDialog(viewer.getTextWidget().getShell(), e);
			fSelectedRegion= fRegion;
		}

	}

	private void endCompoundChange(ITextViewer viewer) {
		if (viewer instanceof ITextViewerExtension) {
			ITextViewerExtension extension= (ITextViewerExtension) viewer;
			IRewriteTarget target= extension.getRewriteTarget();
			target.endCompoundChange();
		}
	}

	private void beginCompoundChange(ITextViewer viewer) {
		if (viewer instanceof ITextViewerExtension) {
			ITextViewerExtension extension= (ITextViewerExtension) viewer;
			IRewriteTarget target= extension.getRewriteTarget();
			target.beginCompoundChange();
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

	private void ensurePositionCategoryInstalled(final IDocument document, LinkedModeModel model) {
		if (!document.containsPositionCategory(getCategory())) {
			document.addPositionCategory(getCategory());
			fUpdater= new InclusivePositionUpdater(getCategory());
			document.addPositionUpdater(fUpdater);

			model.addLinkingListener(new ILinkedModeListener() {

				/*
				 * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
				 */
				@Override
				public void left(LinkedModeModel environment, int flags) {
					ensurePositionCategoryRemoved(document);
				}

				@Override
				public void suspend(LinkedModeModel environment) {}
				@Override
				public void resume(LinkedModeModel environment, int flags) {}
			});
		}
	}

	private void ensurePositionCategoryRemoved(IDocument document) {
		if (document.containsPositionCategory(getCategory())) {
			try {
				document.removePositionCategory(getCategory());
			} catch (BadPositionCategoryException e) {
				// ignore
			}
			document.removePositionUpdater(fUpdater);
		}
	}

	private String getCategory() {
		return "TemplateProposalCategory_" + toString(); //$NON-NLS-1$
	}

	private int getCaretOffset(TemplateBuffer buffer) {

	    TemplateVariable[] variables= buffer.getVariables();
		for (TemplateVariable variable : variables) {
			if (GlobalTemplateVariables.Cursor.NAME.equals(variable.getType()))
				return variable.getOffsets()[0];
		}

		return buffer.getString().length();
	}

	/**
	 * Returns the offset of the range in the document that will be replaced by
	 * applying this template.
	 *
	 * @return the offset of the range in the document that will be replaced by
	 *         applying this template
	 */
	protected final int getReplaceOffset() {
		int start;
		if (fContext instanceof DocumentTemplateContext) {
			DocumentTemplateContext docContext = (DocumentTemplateContext)fContext;
			start= docContext.getStart();
		} else {
			start= fRegion.getOffset();
		}
		return start;
	}

	/**
	 * Returns the end offset of the range in the document that will be replaced
	 * by applying this template.
	 *
	 * @return the end offset of the range in the document that will be replaced
	 *         by applying this template
	 */
	protected final int getReplaceEndOffset() {
		int end;
		if (fContext instanceof DocumentTemplateContext) {
			DocumentTemplateContext docContext = (DocumentTemplateContext)fContext;
			end= docContext.getEnd();
		} else {
			end= fRegion.getOffset() + fRegion.getLength();
		}
		return end;
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	@Override
	public Point getSelection(IDocument document) {
		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	@Override
	public String getAdditionalProposalInfo() {
	    try {
		    fContext.setReadOnly(true);
			TemplateBuffer templateBuffer;
			try {
				templateBuffer= fContext.evaluate(fTemplate);
			} catch (TemplateException e) {
				return null;
			}

			IDocument document= new Document(templateBuffer.getString());
			IndentUtil.indentLines(document, new LineRange(0, document.getNumberOfLines()), null, null);
			return document.get();

	    } catch (BadLocationException e) {
			handleException(JavaPlugin.getActiveWorkbenchShell(), new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "", e))); //$NON-NLS-1$
			return null;
		}
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	@Override
	public String getDisplayString() {
		return getStyledDisplayString().getString();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension6#getStyledDisplayString()
	 * @since 3.4
	 */
	@Override
	public StyledString getStyledDisplayString() {
		if (fDisplayString == null) {
			String[] arguments= new String[] { fTemplate.getName(), fTemplate.getDescription() };
			String decorated= Messages.format(TemplateContentAssistMessages.TemplateProposal_displayString, arguments);
			StyledString string= new StyledString(fTemplate.getName(), StyledString.COUNTER_STYLER);
			fDisplayString= StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, string);
		}
		return fDisplayString;
	}

	public void setDisplayString(StyledString displayString) {
		fDisplayString= displayString;
	}

	@Override
	public StyledString getStyledDisplayString(IDocument document, int offset, final BoldStylerProvider boldStylerProvider) {
		StyledString styledDisplayString= new StyledString();
		styledDisplayString.append(getStyledDisplayString());
		int start= getPrefixCompletionStart(document, offset);
		int patternLength= offset - start;
		try {
			String pattern= document.get(start, patternLength);
			if (!pattern.isEmpty()) {
				Styler styler= new Styler() {
					@Override
					public void applyStyles(TextStyle textStyle) {
						textStyle.foreground= JFaceResources.getColorRegistry().get(JFacePreferences.COUNTER_COLOR);
						textStyle.font= boldStylerProvider.getBoldFont();
					}
				};
				String displayString= styledDisplayString.getString();
				boolean hasBracket= fContext instanceof JavaDocContext && displayString.indexOf('<') == 0;
				if (hasBracket) {
					displayString= displayString.substring(1);
					if (pattern.indexOf('<') == 0) {
						pattern= pattern.substring(1);
						Strings.markMatchingRegions(styledDisplayString, 0, new int[] { 0, 1 }, styler);
					}
				}
				int matchRule= SearchPattern.R_PREFIX_MATCH;
				if (JavaManipulationPlugin.CODEASSIST_SUBSTRING_MATCH_ENABLED && CharOperation.substringMatch(pattern, displayString)) {
					matchRule= SearchPattern.R_SUBSTRING_MATCH;
				}
				int[] matchingRegions= SearchPattern.getMatchingRegions(pattern, displayString, matchRule);
				if (hasBracket && matchingRegions != null) {
					for (int i= 0; i < matchingRegions.length; i+= 2) {
						matchingRegions[i]++;
					}
				}
				Strings.markMatchingRegions(styledDisplayString, 0, matchingRegions, styler);
			}
		} catch (BadLocationException e) {
			// return styledDisplayString
		}
		return styledDisplayString;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	@Override
	public Image getImage() {
		return fImage;
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	private void openErrorDialog(Shell shell, Exception e) {
		MessageDialog.openError(shell, TemplateContentAssistMessages.TemplateEvaluator_error_title, e.getMessage());
	}

	private void handleException(Shell shell, CoreException e) {
		ExceptionHandler.handle(e, shell, TemplateContentAssistMessages.TemplateEvaluator_error_title, null);
	}

	/*
	 * @see IJavaCompletionProposal#getRelevance()
	 */
	@Override
	public int getRelevance() {
		return fIsSubstringMatch ? fRelevance - 400 : fRelevance;
	}

	public void setRelevance(int relevance) {
		fRelevance= relevance;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getInformationControlCreator()
	 */
	@Override
	public IInformationControlCreator getInformationControlCreator() {
		int orientation;
		JavaEditor editor= getJavaEditor();
		if (editor != null)
			orientation= editor.getOrientation();
		else
			orientation= SWT.LEFT_TO_RIGHT;
		return new TemplateInformationControlCreator(orientation);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer, boolean)
	 */
	@Override
	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(org.eclipse.jface.text.ITextViewer)
	 */
	@Override
	public void unselected(ITextViewer viewer) {
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 */
	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		try {
			int replaceOffset= getReplaceOffset();
			if (offset >= replaceOffset) {
				String content= document.get(replaceOffset, offset - replaceOffset).toLowerCase();
				String templateName= fTemplate.getName().toLowerCase();
				boolean isSubstringEnabled= JavaManipulationPlugin.CODEASSIST_SUBSTRING_MATCH_ENABLED;
				boolean valid= false;
				fIsSubstringMatch= false;
				if (templateName.startsWith(content)) {
					valid= true;
				} else if (isSubstringEnabled && templateName.contains(content)) {
					valid= true;
					fIsSubstringMatch= true;
				}
				if (!valid && fContext instanceof JavaDocContext && templateName.startsWith("<")) { //$NON-NLS-1$
					if (templateName.startsWith(content, 1)) {
						valid= true;
					} else if (isSubstringEnabled && CharOperation.substringMatch(content.indexOf('<') == 0 ? content.substring(1) : content, templateName.substring(1))) {
						valid= true;
						fIsSubstringMatch= true;
					}
				}
				return valid;
			}
		} catch (BadLocationException e) {
			// concurrent modification - ignore
		}
		return false;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getReplacementString()
	 */
	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		// bug 114360 - don't make selection templates prefix-completable
		if (isSelectionTemplate())
			return ""; //$NON-NLS-1$
		return fTemplate.getName();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getReplacementOffset()
	 */
	@Override
	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return getReplaceOffset();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	@Override
	public boolean isAutoInsertable() {
		if (isSelectionTemplate())
			return false;
		return fTemplate.isAutoInsertable();
	}

	/**
	 * Returns <code>true</code> if the proposal has a selection, e.g. will wrap some code.
	 *
	 * @return <code>true</code> if the proposals completion length is non zero
	 * @since 3.2
	 */
	private boolean isSelectionTemplate() {
		if (fContext instanceof DocumentTemplateContext) {
			DocumentTemplateContext ctx= (DocumentTemplateContext) fContext;
			if (ctx.getCompletionLength() > 0)
				return true;
		}
		return false;
	}
}
