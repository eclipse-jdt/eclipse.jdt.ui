/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.ui.refactoring.ChangePreviewViewerInput;
import org.eclipse.ltk.ui.refactoring.IChangePreviewViewer;

import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.IPropertiesFilePartitions;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileDocumentSetupParticipant;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileSourceViewerConfiguration;
import org.eclipse.jdt.internal.ui.util.ViewerPane;

/**
 * Change preview viewer for <code>CreateTextFileChange</code> objects.
 */
public final class CreateTextFileChangePreviewViewer implements IChangePreviewViewer {

	private static class CreateTextFilePreviewer extends ViewerPane {

		private ImageDescriptor fDescriptor;

		private Image fImage;

		public CreateTextFilePreviewer(Composite parent, int style) {
			super(parent, style);
			addDisposeListener(e -> disposeImage());
		}

		/*package*/ void disposeImage() {
			if (fImage != null) {
				fImage.dispose();
			}
		}

		public void setImageDescriptor(ImageDescriptor imageDescriptor) {
			fDescriptor= imageDescriptor;
		}

		@Override
		public void setText(String text) {
			super.setText(text);
			Image current= null;
			if (fDescriptor != null) {
				current= fImage;
				fImage= fDescriptor.createImage();
			} else {
				current= fImage;
				fImage= null;
			}
			setImage(fImage);
			if (current != null) {
				current.dispose();
			}
		}

	}

	private static class FileChangeSourceViewer extends SourceViewer {

		private final Map<String, Color> customColors= new HashMap<>();

		public FileChangeSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
			super(parent, ruler, styles);
			setColors();
			StyledText textWidget= getTextWidget();
			textWidget.setEditable(false);
			textWidget.setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		}

		@Override
		protected void handleDispose() {
			customColors.clear();
			super.handleDispose();
		}

		private void setColors() {
			IPreferenceStore store= EditorsUI.getPreferenceStore();
			StyledText styledText= getTextWidget();
			setColor(styledText, store,
					AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND,
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT));
			setColor(styledText, store,
					AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND,
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT));
			setColor(styledText, store,
					AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND,
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT));
			setColor(styledText, store,
					AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND,
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT));
		}

		private void setColor(StyledText styledText, IPreferenceStore store,
				String key, boolean useDefault) {
			Color newColor= useDefault
					? null
					: createColor(styledText.getDisplay(), store, key);
			switch (key) {
				case AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND:
					styledText.setForeground(newColor);
					break;
				case AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND:
					styledText.setBackground(newColor);
					break;
				case AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND:
					styledText.setSelectionForeground(newColor);
					break;
				case AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND:
					styledText.setSelectionBackground(newColor);
					break;
				default:
					return;
			}
			customColors.remove(key);
			customColors.put(key, newColor);
		}

		private static Color createColor(Display display, IPreferenceStore store,
				String key) {
			RGB rgb= null;
			if (store.contains(key)) {
				if (store.isDefault(key)) {
					rgb= PreferenceConverter.getDefaultColor(store, key);
				} else {
					rgb= PreferenceConverter.getColor(store, key);
				}
				if (rgb != null) {
					return new Color(display, rgb);
				}
			}
			return null;
		}
	}

	private CreateTextFilePreviewer fPane;

	private SourceViewer fSourceViewer;

	@Override
	public void createControl(Composite parent) {
		fPane= new CreateTextFilePreviewer(parent, SWT.BORDER | SWT.FLAT);
		Dialog.applyDialogFont(fPane);

		fSourceViewer= new FileChangeSourceViewer(fPane, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		fPane.setContent(fSourceViewer.getControl());
	}

	@Override
	public Control getControl() {
		return fPane;
	}

	public void refresh() {
		fSourceViewer.refresh();
	}


	@Override
	public void setInput(ChangePreviewViewerInput input) {
		Change change= input.getChange();
		if (change != null) {
			Object element= change.getModifiedElement();
			if (element instanceof IAdaptable) {
				IAdaptable adaptable= (IAdaptable) element;
				IWorkbenchAdapter workbenchAdapter= adaptable.getAdapter(IWorkbenchAdapter.class);
				if (workbenchAdapter != null) {
					fPane.setImageDescriptor(workbenchAdapter.getImageDescriptor(element));
				} else {
					fPane.setImageDescriptor(null);
				}
			} else {
				fPane.setImageDescriptor(null);
			}
		}
		if (!(change instanceof CreateTextFileChange)) {
			fSourceViewer.setInput(null);
			fPane.setText(""); //$NON-NLS-1$
			return;
		}
		CreateTextFileChange textFileChange= (CreateTextFileChange) change;
		fPane.setText(textFileChange.getName());
		IDocument document= new Document(textFileChange.getPreview());
		// This is a temporary work around until we get the
		// source viewer registry.
		fSourceViewer.unconfigure();
		String textType= textFileChange.getTextType();
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		boolean nomatch= false;
		if (textType != null) switch (textType) {
		case "java": //$NON-NLS-1$
			textTools.setupJavaDocumentPartitioner(document);
			fSourceViewer.configure(new JavaSourceViewerConfiguration(textTools.getColorManager(), store, null, null));
			fSourceViewer.getTextWidget().setOrientation(SWT.LEFT_TO_RIGHT);
			break;
		case "properties": //$NON-NLS-1$
			PropertiesFileDocumentSetupParticipant.setupDocument(document);
			fSourceViewer.configure(new PropertiesFileSourceViewerConfiguration(textTools.getColorManager(), store, null, IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING));
			fSourceViewer.getTextWidget().setOrientation(SWT.LEFT_TO_RIGHT);
			break;
		default:
			nomatch= true;
			break;
		}
		if (nomatch) {
			fSourceViewer.configure(new SourceViewerConfiguration());
			fSourceViewer.getTextWidget().setOrientation(fSourceViewer.getTextWidget().getParent().getOrientation());
		}
		fSourceViewer.setInput(document);
	}
}
