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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatterExtension2;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;

/**
 * @author sib
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class JavaPreview {
	
	
	private class JavaSourcePreviewerUpdater {
		
		public JavaSourcePreviewerUpdater(final SourceViewer viewer, final JavaTextTools javaTextTools) {
			Assert.isNotNull(viewer);
			Assert.isNotNull(javaTextTools);
			final IPropertyChangeListener fontChangeListener= new IPropertyChangeListener() {
				/*
				 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
				 */
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(PreferenceConstants.EDITOR_TEXT_FONT)) {
						Font font= JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
						viewer.getTextWidget().setFont(font);
					}
				}
			};
			final IPreferenceStore preferenceStore= javaTextTools.getPreferenceStore();
			final IPropertyChangeListener propertyChangeListener= new IPropertyChangeListener() {
				/*
				 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
				 */
				public void propertyChange(PropertyChangeEvent event) {
					if (javaTextTools.affectsBehavior(event))
						viewer.invalidateTextPresentation();
				}
			};
			viewer.getTextWidget().addDisposeListener(new DisposeListener() {
				/*
				 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
				 */
				public void widgetDisposed(DisposeEvent e) {
					preferenceStore.removePropertyChangeListener(propertyChangeListener);
					JFaceResources.getFontRegistry().removeListener(fontChangeListener);
				}
			});
			JFaceResources.getFontRegistry().addListener(fontChangeListener);
			preferenceStore.addPropertyChangeListener(propertyChangeListener);
		}
	}
	
	
//	private static final String PREVIEW_FONT =	"org.eclipse.jdt.internal.ui.preferences.code_formatter_preview_font";
//	private static final double PREVIEW_FONT_SCALE_FACTOR = 0.8;
	
	private final JavaTextTools fTextTools;
	private final JavaSourceViewerConfiguration fViewerConfiguration;
	private final Document fPreviewDocument;
//	private final FontRegistry fFontRegistry;

	private SourceViewer fSourceViewer;
	protected Map fWorkingValues;
	private String fPreviewText;
	
	
	/**
	 * Create a new Java preview
	 */
	
	public JavaPreview(Map workingValues) {
		fTextTools= JavaPlugin.getDefault().getJavaTextTools();
		fViewerConfiguration= new JavaSourceViewerConfiguration( fTextTools, null, IJavaPartitions.JAVA_PARTITIONING);
		fPreviewDocument= new Document();
//		fFontRegistry= new FontRegistry();
		fWorkingValues= workingValues;
		fTextTools.setupJavaDocumentPartitioner( fPreviewDocument, IJavaPartitions.JAVA_PARTITIONING);	
//		initializePreviewFont();
	}
	
//	private void initializePreviewFont() {
//		FontData[] fontData = JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT).getFontData();
//		fontData[0].setHeight((int) (PREVIEW_FONT_SCALE_FACTOR * fontData[0].height));
//		fFontRegistry.put(PREVIEW_FONT, fontData);
//	}

	public Control createContents(Composite parent) {
		if (fSourceViewer != null) 
			return null;
		
		fSourceViewer= new JavaSourceViewer(parent, null, null, false, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fSourceViewer.configure(fViewerConfiguration);
		fSourceViewer.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		
//		fSourceViewer.getTextWidget().setFont(fFontRegistry.get(PREVIEW_FONT));
		new JavaSourcePreviewerUpdater(fSourceViewer, fTextTools);
		fSourceViewer.setDocument(fPreviewDocument);
		update();
		return fSourceViewer.getControl();
	}
	
	
	public void update() {
		if (fWorkingValues == null)
			return;

		final StyledText text = fSourceViewer.getTextWidget();
		final int top0= fSourceViewer.getTopIndex();
		final int range0= text.getLineCount() - (fSourceViewer.getBottomIndex()- fSourceViewer.getTopIndex());
		
		if (fPreviewText == null) {
			fPreviewDocument.set(""); //$NON-NLS-1$
			fSourceViewer.getControl().setEnabled(false);
			return;
		}
		
		if (!fSourceViewer.getControl().getEnabled())
			fSourceViewer.getControl().setEnabled(true);
		
		
		fSourceViewer.setRedraw(false);
		
		final Point selection = fSourceViewer.getSelectedRange();		
//		fTextTools.setupJavaDocumentPartitioner( fPreviewDocument, IJavaPartitions.JAVA_PARTITIONING);

		fPreviewDocument.set(fPreviewText);
		final IFormattingContext context = new CommentFormattingContext();
		try {
			final IContentFormatter formatter =	fViewerConfiguration.getContentFormatter(fSourceViewer);
			if (formatter instanceof IContentFormatterExtension2) {
				final IContentFormatterExtension2 extension = (IContentFormatterExtension2) formatter;
				context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, fWorkingValues);
				context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.valueOf(true));
				extension.format(fPreviewDocument, context);
			} else
				formatter.format(fPreviewDocument, new Region(0, fPreviewDocument.getLength()));
		} finally {
			fSourceViewer.setSelectedRange(selection.x, selection.y);
			context.dispose();
			int range1 = text.getLineCount() - (fSourceViewer.getBottomIndex() - fSourceViewer.getTopIndex());
			fSourceViewer.setRedraw(true);
			
			/** 
			 * TODO: This doesn't work when it is called while redraw is disabled, so it is here. 
			 * But this causes some flickering. Any hints?
			 */
			fSourceViewer.setTopIndex((int)Math.round((top0 * range1) / (double)range0));
		}
	}

	
	public final Map getWorkingValues() {
		return fWorkingValues;
	}
	
	
	public final void setWorkingValues(Map workingValues) {
		fWorkingValues= workingValues;
	}

	
	public final String getPreviewText() {
		return fPreviewText;
	}

	
	public final void setPreviewText(String previewText) {
		fPreviewText= previewText;
	}
}
