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

package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;

import org.eclipse.ui.contentassist.ContentAssistHandler;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;

/**
 * @since 3.0
 */
public class ControlContentAssistHelper {
	
	/**
	 * @param text the text field to install ContentAssist
	 * @param processor the <code>IContentAssistProcessor</code>
	 */
	public static void createTextContentAssistant(final Text text, IContentAssistProcessor processor) {
		ContentAssistHandler.createHandlerForText(text, createJavaContentAssistant(processor));
	}

	/**
	 * @param combo the text field to install ContentAssist
	 * @param processor the <code>IContentAssistProcessor</code>
	 */
	public static void createComboContentAssistant(final Combo combo, IContentAssistProcessor processor) {
		ContentAssistHandler.createHandlerForCombo(combo, createJavaContentAssistant(processor));
	}

	public static ContentAssistant createJavaContentAssistant(IContentAssistProcessor processor) {
		final ContentAssistant contentAssistant= new ContentAssistant();
		
		contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		
		ContentAssistPreference.configure(contentAssistant, JavaPlugin.getDefault().getPreferenceStore());
		contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		contentAssistant.setInformationControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true));
			}
		});
		
		return contentAssistant;
	}
	
}
