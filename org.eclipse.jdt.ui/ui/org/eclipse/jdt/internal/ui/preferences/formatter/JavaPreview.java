/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.MarginPainter;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.WhitespaceCharacterPainter;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.SimpleJavaSourceViewerConfiguration;


public class JavaPreview {

	private final class JavaSourcePreviewerUpdater {

	    final IPropertyChangeListener propertyListener= new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (fViewerConfiguration.affectsTextPresentation(event)) {
					fViewerConfiguration.handlePropertyChangeEvent(event);
					fSourceViewer.invalidateTextPresentation();
				}
			}
		};


		public JavaSourcePreviewerUpdater() {

		    fPreferenceStore.addPropertyChangeListener(propertyListener);

			fSourceViewer.getTextWidget().addDisposeListener(e -> fPreferenceStore.removePropertyChangeListener(propertyListener));
		}
	}

	protected final SimpleJavaSourceViewerConfiguration fViewerConfigurationStandard;
	protected final SimpleJavaSourceViewerConfiguration fViewerConfigurationModule;
	protected final Document fPreviewDocument;
	protected final SourceViewer fSourceViewer;
	protected final IPreferenceStore fPreferenceStore;

	protected final MarginPainter fMarginPainter;

	protected boolean fModuleInfoMode= false;
	protected SimpleJavaSourceViewerConfiguration fViewerConfiguration;
	protected Map<String, String> fWorkingValues;
	protected String fFormatterId;

	private String fPreviewText= ""; //$NON-NLS-1$
	private String fUneditedText;
	private int fCodeKind;
	private int fTabSize= 0;
	private WhitespaceCharacterPainter fWhitespaceCharacterPainter;
	private boolean fEditorMode;
	private boolean fUpdateScheduled;

	public JavaPreview(Map<String, String> workingValues, Composite parent) {
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		fPreviewDocument= new Document();
		fWorkingValues= workingValues;
		tools.setupJavaDocumentPartitioner( fPreviewDocument, IJavaPartitions.JAVA_PARTITIONING);

		PreferenceStore prioritizedSettings= new PreferenceStore();
		HashMap<String, String> complianceOptions= new HashMap<>();
		JavaModelUtil.setComplianceOptions(complianceOptions, JavaModelUtil.VERSION_LATEST);
		for (Entry<String, String> complianceOption : complianceOptions.entrySet()) {
			prioritizedSettings.setValue(complianceOption.getKey(), complianceOption.getValue());
		}

		IPreferenceStore[] chain= { prioritizedSettings, JavaPlugin.getDefault().getCombinedPreferenceStore() };
		fPreferenceStore= new ChainedPreferenceStore(chain);
		fSourceViewer= new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER, fPreferenceStore);
		fSourceViewer.setEditable(false);

		// Don't set caret to 'null' as this causes https://bugs.eclipse.org/293263
//		fSourceViewer.getTextWidget().setCaret(null);

		fViewerConfigurationStandard= new SimpleJavaSourceViewerConfiguration(tools.getColorManager(), fPreferenceStore, null, IJavaPartitions.JAVA_PARTITIONING, true, false);
		fViewerConfigurationModule= new SimpleJavaSourceViewerConfiguration(tools.getColorManager(), fPreferenceStore, null, IJavaPartitions.JAVA_PARTITIONING, true, true);
		fViewerConfiguration= fViewerConfigurationStandard;
		fSourceViewer.configure(fViewerConfiguration);
		fSourceViewer.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));

		fMarginPainter= new MarginPainter(fSourceViewer);
		final RGB rgb= PreferenceConverter.getColor(fPreferenceStore, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR);
		fMarginPainter.setMarginRulerColor(tools.getColorManager().getColor(rgb));
		fSourceViewer.addPainter(fMarginPainter);

		new JavaSourcePreviewerUpdater();
		fSourceViewer.setDocument(fPreviewDocument);

		fSourceViewer.getTextWidget().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL) {
					if (e.keyCode == 'z') {
						fSourceViewer.getUndoManager().undo();
					} else if (e.keyCode == 'y') {
						fSourceViewer.getUndoManager().redo();
					}
				}
			}
		});
	}

	public Control getControl() {
		return fSourceViewer.getControl();
	}

	public void update() {
		if (fUpdateScheduled)
			return;
		fUpdateScheduled= true;
		Display.getDefault().asyncExec(() -> {
			doUpdate();
			fUpdateScheduled= false;
		});
	}

	private void doUpdate() {
		if (fWorkingValues == null) {
			fPreviewDocument.set(""); //$NON-NLS-1$
			return;
		}
		final StyledText widget= fSourceViewer.getTextWidget();

		// update the print margin
		final String value= fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT);
		final int lineWidth= getPositiveIntValue(value, 0);
		fMarginPainter.setMarginRulerColumn(lineWidth);

		// update the tab size
		final int tabSize= JavaCore.SPACE.equals(fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR))
				? getPositiveIntValue(fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE), 0)
				: getPositiveIntValue(fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE), 0);
		if (tabSize != fTabSize)
			widget.setTabs(tabSize);
		fTabSize= tabSize;

		boolean moduleInfoMode= fCodeKind == CodeFormatter.K_MODULE_INFO;
		if (fModuleInfoMode != moduleInfoMode) {
			fModuleInfoMode= moduleInfoMode;
			fViewerConfiguration= fModuleInfoMode ? fViewerConfigurationModule : fViewerConfigurationStandard;
			fSourceViewer.unconfigure();
			fSourceViewer.configure(fViewerConfiguration);
		}

		final int height= widget.getClientArea().height;
		final int top0= widget.getTopPixel();

		final int totalPixels0= getHeightOfAllLines(widget);
		final int topPixelRange0= totalPixels0 > height ? totalPixels0 - height : 0;

		widget.setRedraw(false);
		if (fEditorMode) {
			fPreviewDocument.set(fPreviewText);
		} else {
			doFormatPreview();
			fSourceViewer.getUndoManager().reset();
		}
		fSourceViewer.setSelection(null);

		final int totalPixels1= getHeightOfAllLines(widget);
		final int topPixelRange1= totalPixels1 > height ? totalPixels1 - height : 0;

		final int top1= topPixelRange0 > 0 ? (int)(topPixelRange1 * top0 / (double)topPixelRange0) : 0;
		widget.setTopPixel(top1);
		widget.setRedraw(true);

		widget.setCursor(widget.getDisplay().getSystemCursor(fEditorMode ? SWT.CURSOR_IBEAM : SWT.CURSOR_ARROW));
	}

	private int getHeightOfAllLines(StyledText styledText) {
		int height= 0;
		int lineCount= styledText.getLineCount();
		for (int i= 0; i < lineCount; i++)
			height= height + styledText.getLineHeight(styledText.getOffsetAtLine(i));
		return height;
	}

	protected void doFormatPreview() {
		fPreviewDocument.set(fPreviewText);

		String delimiter= TextUtilities.getDefaultLineDelimiter(fPreviewDocument);
		Map<String, String> prefs= fWorkingValues;
		if (fFormatterId != null) {
			prefs= new HashMap<>(fWorkingValues);
			prefs.put(JavaCore.JAVA_FORMATTER, fFormatterId);
		}
		fSourceViewer.setRedraw(false);
		try {
			TextEdit edit= CodeFormatterUtil.reformat(fCodeKind + CodeFormatter.F_INCLUDE_COMMENTS, fPreviewText, 0, delimiter, prefs);
			if (edit != null)
				edit.apply(fPreviewDocument);
		} catch (Exception e) {
			JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR,
					FormatterMessages.JavaPreview_formatter_exception, e));
		} finally {
			fSourceViewer.setRedraw(true);
		}
	}

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

	public Map<String, String> getWorkingValues() {
		return fWorkingValues;
	}


	public void setWorkingValues(Map<String, String> workingValues) {
		fWorkingValues= workingValues;
	}

	public void setPreviewText(String previewText, int codeKind) {
		if (previewText.equals(fUneditedText))
			return;
		fPreviewText= previewText;
		fUneditedText= previewText;
		fCodeKind= codeKind;
		update();
	}

	public void showInvisibleCharacters(boolean enable) {
		if (enable) {
			if (fWhitespaceCharacterPainter == null) {
				fWhitespaceCharacterPainter= new WhitespaceCharacterPainter(fSourceViewer);
				fSourceViewer.addPainter(fWhitespaceCharacterPainter);
			}
		} else {
			fSourceViewer.removePainter(fWhitespaceCharacterPainter);
			fWhitespaceCharacterPainter= null;
		}
	}

	public void setEditorMode(boolean editorMode) {
		if (fEditorMode && !editorMode) {
			fPreviewText= fPreviewDocument.get();
		}
		fEditorMode= editorMode;
		update();
		fSourceViewer.setEditable(editorMode);
	}

	public void setFormatterId(String formatterId) {
		fFormatterId= formatterId;
	}
}
