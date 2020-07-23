/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * The properties file correction assistant.
 *
 * @since 3.7
 */
public class PropertiesCorrectionAssistant extends QuickAssistAssistant {

	private ITextEditor fEditor;

	public PropertiesCorrectionAssistant(ITextEditor editor) {
		super();
		Assert.isNotNull(editor);
		fEditor= editor;

		setQuickAssistProcessor(new PropertiesCorrectionProcessor(this));
		enableColoredLabels(PlatformUI.getPreferenceStore().getBoolean(IWorkbenchPreferenceConstants.USE_COLORED_LABELS));

		setInformationControlCreator(getInformationControlCreator());
	}

	private IInformationControlCreator getInformationControlCreator() {
		return parent -> new DefaultInformationControl(parent, JavaPlugin.getAdditionalInfoAffordanceString());
	}

	public IEditorPart getEditor() {
		return fEditor;
	}
}
