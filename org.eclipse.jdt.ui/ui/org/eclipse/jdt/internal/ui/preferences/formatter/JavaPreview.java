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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.MarginPainter;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.texteditor.ExtendedTextEditorPreferenceConstants;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;


public abstract class JavaPreview {
    
	
	private final class JavaSourcePreviewerUpdater {
	    
	    final IPropertyChangeListener fontListener= new IPropertyChangeListener() {
	        public void propertyChange(PropertyChangeEvent event) {
	            if (event.getProperty().equals(PreferenceConstants.EDITOR_TEXT_FONT)) {
					final Font font= JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
					fSourceViewer.getTextWidget().setFont(font);
					if (fMarginPainter != null) {
						fMarginPainter.initialize();
					}
				}
			}
		};
		
	    final IPropertyChangeListener propertyListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (fTextTools.affectsBehavior(event))
					fSourceViewer.invalidateTextPresentation();
			}
		};
		
		
		public JavaSourcePreviewerUpdater() {
			
		    JFaceResources.getFontRegistry().addListener(fontListener);
			fTextTools.getPreferenceStore().addPropertyChangeListener(propertyListener);
			
			fSourceViewer.getTextWidget().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					JFaceResources.getFontRegistry().removeListener(fontListener);
					fTextTools.getPreferenceStore().removePropertyChangeListener(propertyListener);
				}
			});
		}
	}
	
	protected final JavaSourceViewerConfiguration fViewerConfiguration;
	protected final Document fPreviewDocument;
	protected final JavaTextTools fTextTools;
	protected final SourceViewer fSourceViewer;
	
	protected final MarginPainter fMarginPainter;
	
	protected Map fWorkingValues;

	private int fTabSize= 0;
	
	/**
	 * Create a new Java preview
	 */
	
	public JavaPreview(Map workingValues, Composite parent) {
		fTextTools= JavaPlugin.getDefault().getJavaTextTools();
		fViewerConfiguration= new JavaSourceViewerConfiguration( fTextTools, null, IJavaPartitions.JAVA_PARTITIONING);
		fPreviewDocument= new Document();
		fWorkingValues= workingValues;
		fTextTools.setupJavaDocumentPartitioner( fPreviewDocument, IJavaPartitions.JAVA_PARTITIONING);	
		
		fSourceViewer= new JavaSourceViewer(parent, null, null, false, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fSourceViewer.configure(fViewerConfiguration);
		fSourceViewer.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		
		fMarginPainter= new MarginPainter(fSourceViewer);
		final IPreferenceStore prefStore= JavaPlugin.getDefault().getPreferenceStore();
		final RGB rgb= PreferenceConverter.getColor(prefStore, ExtendedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR);
		fMarginPainter.setMarginRulerColor(fTextTools.getColorManager().getColor(rgb));
		fSourceViewer.addPainter(fMarginPainter);
		
		new JavaSourcePreviewerUpdater();
		fSourceViewer.setDocument(fPreviewDocument);
	}
	
	public Control getControl() {
	    return fSourceViewer.getControl();
	}
	
	
	public void update() {
		if (fWorkingValues == null) {
		    fPreviewDocument.set(""); //$NON-NLS-1$
		    return;
		}
		
		// update the print margin
		final String value= (String)fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT);
		final int lineWidth= getPositiveIntValue(value, 0);
		fMarginPainter.setMarginRulerColumn(lineWidth);
		
		// update the tab size
		final int tabSize= getPositiveIntValue((String) fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE), 0);
		if (tabSize != fTabSize) fSourceViewer.getTextWidget().setTabs(tabSize);
		fTabSize= tabSize;
		
		final StyledText widget= (StyledText)fSourceViewer.getControl();
		final int height= widget.getClientArea().height;
		final int top0= widget.getTopPixel();
		
		final int totalPixels0= widget.getLineCount() * widget.getLineHeight();
		final int topPixelRange0= totalPixels0 > height ? totalPixels0 - height : 0;
		
		widget.setRedraw(false);
		doFormatPreview();
		fSourceViewer.setSelection(null);
		
		final int totalPixels1= widget.getLineCount() * widget.getLineHeight();
		final int topPixelRange1= totalPixels1 > height ? totalPixels1 - height : 0;

		final int top1= topPixelRange0 > 0 ? (int)(topPixelRange1 * top0 / (double)topPixelRange0) : 0;
		widget.setTopPixel(top1);
		widget.setRedraw(true);
	}
	
	protected abstract void doFormatPreview();

	
	private static int getPositiveIntValue(String string, int defaultValue) {
	    try {
	        int i= Integer.parseInt(string);
	        if (i >= 0) {
	            return i;
	        }
	    } catch (NumberFormatException e) {
	    }
	    return defaultValue;
	}		
	

	
	public final Map getWorkingValues() {
		return fWorkingValues;
	}
	
	
	public final void setWorkingValues(Map workingValues) {
		fWorkingValues= workingValues;
	}
}
