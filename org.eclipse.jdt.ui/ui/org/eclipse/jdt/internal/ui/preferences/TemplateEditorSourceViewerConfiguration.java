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
package org.eclipse.jdt.internal.ui.preferences;

import java.util.Iterator;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.corext.template.TemplateVariable;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jdt.internal.ui.text.template.TemplateVariableProcessor;


public class TemplateEditorSourceViewerConfiguration extends JavaSourceViewerConfiguration {

	private static class TemplateVariableTextHover implements ITextHover {

		private TemplateVariableProcessor fProcessor;

		/**
		 * @param type
		 */
		public TemplateVariableTextHover(TemplateVariableProcessor processor) {
			fProcessor= processor;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
		 */
		public String getHoverInfo(ITextViewer textViewer, IRegion subject) {
			try {
				IDocument doc= textViewer.getDocument();
				int offset= subject.getOffset();
				if (offset >= 2 && "${".equals(doc.get(offset-2, 2))) { //$NON-NLS-1$
					String varName= doc.get(offset, subject.getLength());
					Iterator iter= fProcessor.getContextType().variableIterator();
					while (iter.hasNext()) {
						TemplateVariable var= (TemplateVariable) iter.next();
						if (varName.equals(var.getName())) {
							return var.getDescription();
						}
					}
				}				
			} catch (BadLocationException e) {
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
		 */
		public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
			if (textViewer != null) {
				return JavaWordFinder.findWord(textViewer.getDocument(), offset);
			}
			return null;	
		}
		
	} 
	
	private final TemplateVariableProcessor fProcessor;

	public TemplateEditorSourceViewerConfiguration(JavaTextTools tools, ITextEditor editor, TemplateVariableProcessor processor) {
		super(tools, editor, IJavaPartitions.JAVA_PARTITIONING);
		fProcessor= processor;
	}
	
	/*
	 * @see SourceViewerConfiguration#getContentAssistant(ISourceViewer)
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		IColorManager manager= textTools.getColorManager();					
		

		ContentAssistant assistant= new ContentAssistant();
		assistant.setContentAssistProcessor(fProcessor, IDocument.DEFAULT_CONTENT_TYPE);
			// Register the same processor for strings and single line comments to get code completion at the start of those partitions.
		assistant.setContentAssistProcessor(fProcessor, IJavaPartitions.JAVA_STRING);
		assistant.setContentAssistProcessor(fProcessor, IJavaPartitions.JAVA_CHARACTER);
		assistant.setContentAssistProcessor(fProcessor, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		assistant.setContentAssistProcessor(fProcessor, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		assistant.setContentAssistProcessor(fProcessor, IJavaPartitions.JAVA_DOC);

		assistant.enableAutoInsert(store.getBoolean(PreferenceConstants.CODEASSIST_AUTOINSERT));
		assistant.enableAutoActivation(store.getBoolean(PreferenceConstants.CODEASSIST_AUTOACTIVATION));
		assistant.setAutoActivationDelay(store.getInt(PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY));
		assistant.setProposalPopupOrientation(ContentAssistant.PROPOSAL_OVERLAY);
		assistant.setContextInformationPopupOrientation(ContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		Color background= getColor(store, PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND, manager);			
		assistant.setContextInformationPopupBackground(background);
		assistant.setContextSelectorBackground(background);
		assistant.setProposalSelectorBackground(background);

		Color foreground= getColor(store, PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND, manager);
		assistant.setContextInformationPopupForeground(foreground);
		assistant.setContextSelectorForeground(foreground);
		assistant.setProposalSelectorForeground(foreground);
		
		return assistant;
	}	

	private Color getColor(IPreferenceStore store, String key, IColorManager manager) {
		RGB rgb= PreferenceConverter.getColor(store, key);
		return manager.getColor(rgb);
	}
	
	/*
	 * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String, int)
	 * @since 2.1
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
		return new TemplateVariableTextHover(fProcessor);
	}

}