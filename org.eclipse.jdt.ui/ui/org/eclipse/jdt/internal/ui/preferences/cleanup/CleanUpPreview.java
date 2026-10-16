/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatterExtension;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.ui.forms.FormColors;

import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.IndentAction;
import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;
import org.eclipse.jdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.jdt.internal.ui.text.LineComparator;
import org.eclipse.jdt.internal.ui.text.java.JavaFormattingContext;


public class CleanUpPreview extends JavaPreview {

	private static final int HIGHLIGHT_BLEND_PERCENT= 30;

	private ICleanUpConfigurationUI fPage;
	private boolean fFormat;
	private boolean fCorrectIndentation;
	private final Document fPreviousDocument= new Document();
	private int fFirstChangeStartLine= -1;
	private int fFirstChangeEndLine= -1;

	public CleanUpPreview(Composite parent, ICleanUpConfigurationUI page) {
		super(JavaCore.getDefaultOptions(), parent);
		fPage= page;
		fFormat= false;
	}

	public void setFormat(boolean enable) {
		fFormat= enable;
	}

	public void setCorrectIndentation(boolean enabled) {
		fCorrectIndentation= enabled;
	}

	@Override
	protected void doFormatPreview() {
		format(fPage.getPreview());
		highlightChangedLines();
	}

	@Override
	protected void doAfterUpdate() {
		if (fFirstChangeStartLine < 0)
			return;
		try {
			IRegion first= fPreviewDocument.getLineInformation(fFirstChangeStartLine);
			IRegion last= fPreviewDocument.getLineInformation(fFirstChangeEndLine);
			fSourceViewer.revealRange(first.getOffset(), last.getOffset() + last.getLength() - first.getOffset());
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	private void highlightChangedLines() {
		StyledText widget= fSourceViewer.getTextWidget();
		widget.setLineBackground(0, widget.getLineCount(), null);
		fFirstChangeStartLine= -1;
		fFirstChangeEndLine= -1;
		if (fPreviousDocument.getLength() > 0) {
			RangeDifference[] differences= RangeDifferencer.findDifferences(new LineComparator(fPreviousDocument), new LineComparator(fPreviewDocument));
			Color color= new Color(FormColors.blend(widget.getSelectionBackground().getRGB(), widget.getBackground().getRGB(), HIGHLIGHT_BLEND_PERCENT));
			for (RangeDifference difference : differences) {
				if (difference.kind() != RangeDifference.CHANGE)
					continue;
				int start= difference.rightStart();
				int length= difference.rightLength();
				if (length == 0) {
					// pure deletion: mark the line that now takes the place of the removed ones
					start= Math.min(start, widget.getLineCount() - 1);
					length= 1;
				}
				widget.setLineBackground(start, length, color);
				if (fFirstChangeStartLine < 0) {
					fFirstChangeStartLine= start;
					fFirstChangeEndLine= start + length - 1;
				}
			}
		}
		fPreviousDocument.set(fPreviewDocument.get());
	}

	private void format(String text) {
        if (text == null) {
            fPreviewDocument.set(""); //$NON-NLS-1$
            return;
        }
        fPreviewDocument.set(text);

        if (!fFormat) {
        	if (!fCorrectIndentation)
        		return;

        	fSourceViewer.setRedraw(false);
        	try {
        		IndentAction.indent(fPreviewDocument, null);
        	} catch (BadLocationException e) {
				JavaPlugin.log(e);
			} finally {
        		fSourceViewer.setRedraw(true);
        	}

			return;
        }

		fSourceViewer.setRedraw(false);
		final IFormattingContext context = new JavaFormattingContext();
		try {
			final IContentFormatter formatter =	fViewerConfiguration.getContentFormatter(fSourceViewer);
			if (formatter instanceof IContentFormatterExtension) {
				final IContentFormatterExtension extension = (IContentFormatterExtension) formatter;
				context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, JavaCore.getOptions());
				context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.TRUE);
				extension.format(fPreviewDocument, context);
			} else
				formatter.format(fPreviewDocument, new Region(0, fPreviewDocument.getLength()));
		} catch (Exception e) {
			final IStatus status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR,
				MultiFixMessages.CleanUpRefactoringWizard_formatterException_errorMessage, e);
			JavaPlugin.log(status);
		} finally {
		    context.dispose();
		    fSourceViewer.setRedraw(true);
		}
	}

    @Override
	public void setWorkingValues(Map<String, String> workingValues) {
    	//Don't change the formatter settings
    }

}
