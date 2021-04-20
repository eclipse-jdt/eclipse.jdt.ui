/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.graphics.Font;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;

import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

/**
 * Handles Java editor font changes for Java source preview viewers.
 *
 * @since 3.0
 */
public class JavaSourcePreviewerUpdater {

	/**
	 * Creates a Java source preview updater for the given viewer, configuration and preference store.
	 *
	 * @param viewer the viewer
	 * @param configuration the configuration
	 * @param preferenceStore the preference store
	 */
	public JavaSourcePreviewerUpdater(final SourceViewer viewer, final JavaSourceViewerConfiguration configuration, final IPreferenceStore preferenceStore) {
		Assert.isNotNull(viewer);
		Assert.isNotNull(configuration);
		Assert.isNotNull(preferenceStore);
		final IPropertyChangeListener fontChangeListener= event -> {
			if (PreferenceConstants.EDITOR_TEXT_FONT.equals(event.getProperty())) {
				Font font= JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
				viewer.getTextWidget().setFont(font);
			}
		};
		final IPropertyChangeListener propertyChangeListener= event -> {
			if (configuration.affectsTextPresentation(event)) {
				configuration.handlePropertyChangeEvent(event);
				viewer.invalidateTextPresentation();
			}
		};
		viewer.getTextWidget().addDisposeListener(e -> {
			preferenceStore.removePropertyChangeListener(propertyChangeListener);
			JFaceResources.getFontRegistry().removeListener(fontChangeListener);
		});
		JFaceResources.getFontRegistry().addListener(fontChangeListener);
		preferenceStore.addPropertyChangeListener(propertyChangeListener);
	}
}
