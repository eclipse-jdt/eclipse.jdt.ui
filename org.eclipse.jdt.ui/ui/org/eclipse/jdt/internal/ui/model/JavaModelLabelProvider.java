/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.model;

import org.eclipse.swt.graphics.Image;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryControlConfiguration;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryLabelProvider;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * Label provider for Java models.
 * 
 * @since 3.2
 */
public final class JavaModelLabelProvider extends AppearanceAwareLabelProvider {

	/** The refactoring history label provider */
	private final RefactoringHistoryLabelProvider fHistoryLabelProvider= new RefactoringHistoryLabelProvider(new RefactoringHistoryControlConfiguration(null, true, false));

	/** The project settings image, or <code>null</code> */
	private Image fSettingsImage= null;

	/**
	 * Creates a new java model label provider.
	 */
	public JavaModelLabelProvider() {
		super(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS);
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		super.dispose();
		if (fSettingsImage != null && !fSettingsImage.isDisposed()) {
			fSettingsImage.dispose();
			fSettingsImage= null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Image getImage(final Object element) {
		Image image= super.getImage(element);
		if (image == null) {
			if (element instanceof RefactoringHistory)
				image= fHistoryLabelProvider.getImage(element);
			else if (element instanceof RefactoringDescriptorProxy)
				image= fHistoryLabelProvider.getImage(element);
			else if (element instanceof JavaProjectSettings) {
				if (fSettingsImage == null || fSettingsImage.isDisposed())
					fSettingsImage= JavaPluginImages.DESC_OBJS_LIBRARY.createImage();
				image= fSettingsImage;
			}
			return decorateImage(image, element);
		}
		return image;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getText(final Object element) {
		String text= super.getText(element);
		if (text == null || "".equals(text)) { //$NON-NLS-1$
			if (element instanceof RefactoringHistory)
				text= fHistoryLabelProvider.getText(element);
			else if (element instanceof RefactoringDescriptorProxy)
				text= fHistoryLabelProvider.getText(element);
			else if (element instanceof JavaProjectSettings) {
				text= ModelMessages.JavaModelLabelProvider_project_preferences_label;
			}
			return decorateText(text, element);
		}
		return text;
	}
}