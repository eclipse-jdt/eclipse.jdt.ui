/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ContentAssistant;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;


public class JavaCorrectionAssistant extends ContentAssistant {

	/**
	 * Constructor for CorrectionAssistant.
	 */
	public JavaCorrectionAssistant(IEditorPart editor) {
		super();
		JavaCorrectionProcessor processor= new JavaCorrectionProcessor(editor); 
		
		setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		setContentAssistProcessor(processor, JavaPartitionScanner.JAVA_STRING);
	
	
		enableAutoActivation(false);
		enableAutoInsert(false);
		
		setContextInformationPopupOrientation(CONTEXT_INFO_ABOVE);
		setInformationControlCreator(getInformationControlCreator());

		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		IColorManager manager= textTools.getColorManager();

		IPreferenceStore store=  JavaPlugin.getDefault().getPreferenceStore();

		Color c= getColor(store, ContentAssistPreference.PROPOSALS_FOREGROUND, manager);
		setProposalSelectorForeground(c);
		
		c= getColor(store, ContentAssistPreference.PROPOSALS_BACKGROUND, manager);
		setProposalSelectorBackground(c);
	}
	
	private IInformationControlCreator getInformationControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, new HTMLTextPresenter());
			}
		};
	}	
	
	private static Color getColor(IPreferenceStore store, String key, IColorManager manager) {
		RGB rgb= PreferenceConverter.getColor(store, key);
		return manager.getColor(rgb);
	}

}
