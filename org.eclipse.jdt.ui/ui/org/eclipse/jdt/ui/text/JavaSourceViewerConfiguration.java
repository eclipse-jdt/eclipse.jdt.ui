/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.text;

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.text.JavaAnnotationHover;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.JavaReconciler;
import org.eclipse.jdt.internal.ui.text.java.JavaAutoIndentStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProcessor;
import org.eclipse.jdt.internal.ui.text.java.JavaDoubleClickSelector;
import org.eclipse.jdt.internal.ui.text.java.JavaFormattingStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaReconcilingStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaStringDoubleClickSelector;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaInformationProvider;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAutoIndentStrategy;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocCompletionProcessor;


/**
 * Configuration for a source viewer which shows Java code.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class JavaSourceViewerConfiguration extends SourceViewerConfiguration {
	
	/** 
	 * Preference key used to look up display tab width.
	 * 
	 * @since 2.0
	 */
	public final static String PREFERENCE_TAB_WIDTH= "org.eclipse.jdt.ui.editor.tab.width"; //$NON-NLS-1$
	/** 
	 * Preference key for inserting spaces rather than tabs.
	 * 
	 * @since 2.0
	 */
	public final static String SPACES_FOR_TABS= "spacesForTabs"; //$NON-NLS-1$

	
	private JavaTextTools fJavaTextTools;
	private ITextEditor fTextEditor;
	
	/**
	 * Creates a new Java source viewer configuration for viewers in the given editor 
	 * using the given Java tools.
	 *
	 * @param tools the Java tools to be used
	 * @param editor the editor in which the configured viewer(s) will reside
	 */
	public JavaSourceViewerConfiguration(JavaTextTools tools, ITextEditor editor) {
		fJavaTextTools= tools;
		fTextEditor= editor;
	}
	
	/**
	 * Returns the Java source code scanner for this configuration.
	 *
	 * @return the Java source code scanner
	 */
	protected RuleBasedScanner getCodeScanner() {
		return fJavaTextTools.getCodeScanner();
	}
	
	/**
	 * Returns the Java multiline comment scanner for this configuration.
	 *
	 * @return the Java multiline comment scanner
	 * 
	 * @since 2.0
	 */
	protected RuleBasedScanner getMultilineCommentScanner() {
		return fJavaTextTools.getMultilineCommentScanner();
	}
	
	/**
	 * Returns the Java singleline comment scanner for this configuration.
	 *
	 * @return the Java singleline comment scanner
	 * 
	 * @since 2.0
	 */
	protected RuleBasedScanner getSinglelineCommentScanner() {
		return fJavaTextTools.getSinglelineCommentScanner();
	}
	
	/**
	 * Returns the Java string scanner for this configuration.
	 *
	 * @return the Java string scanner
	 * 
	 * @since 2.0
	 */
	protected RuleBasedScanner getStringScanner() {
		return fJavaTextTools.getStringScanner();
	}
	
	/**
	 * Returns the JavaDoc scanner for this configuration.
	 *
	 * @return the JavaDoc scanner
	 */
	protected RuleBasedScanner getJavaDocScanner() {
		return fJavaTextTools.getJavaDocScanner();
	}
	
	/**
	 * Returns the color manager for this configuration.
	 *
	 * @return the color manager
	 */
	protected IColorManager getColorManager() {
		return fJavaTextTools.getColorManager();
	}
	
	/**
	 * Returns the editor in which the configured viewer(s) will reside.
	 *
	 * @return the enclosing editor
	 */
	protected ITextEditor getEditor() {
		return fTextEditor;
	}
	
	/**
	 * Returns the preference store used by this configuration to initialize
	 * the individual bits and pieces.
	 * 
	 * @return the preference store used to initialize this configuration
	 * 
	 * @since 2.0
	 */
	protected IPreferenceStore getPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}
	
	/* 
	 * @see ISourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		PresentationReconciler reconciler= new PresentationReconciler();

		DefaultDamagerRepairer dr= new DefaultDamagerRepairer(getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr= new DefaultDamagerRepairer(getJavaDocScanner());
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_DOC);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_DOC);

		dr= new DefaultDamagerRepairer(getMultilineCommentScanner());		
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT);

		dr= new DefaultDamagerRepairer(getSinglelineCommentScanner());		
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_SINGLE_LINE_COMMENT);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_SINGLE_LINE_COMMENT);
		
		dr= new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_STRING);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_STRING);
		
		return reconciler;
	}

	/*
	 * @see SourceViewerConfiguration#getContentAssistant(ISourceViewer)
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		
		if (getEditor() != null) {
		
			ContentAssistant assistant= new ContentAssistant();
			
			IContentAssistProcessor processor= new JavaCompletionProcessor(getEditor());
			assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
				// Register the same processor for strings and single line comments to get code completion at the start of those partitions.
			assistant.setContentAssistProcessor(processor, JavaPartitionScanner.JAVA_STRING);
			assistant.setContentAssistProcessor(processor, JavaPartitionScanner.JAVA_SINGLE_LINE_COMMENT);
			
			assistant.setContentAssistProcessor(new JavaDocCompletionProcessor(getEditor()), JavaPartitionScanner.JAVA_DOC);
			
			ContentAssistPreference.configure(assistant, getPreferenceStore());
			
			assistant.setContextInformationPopupOrientation(assistant.CONTEXT_INFO_ABOVE);
			assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
					
			return assistant;
		}
		
		return null;
	}
	
	/*
	 * @see SourceViewerConfiguration#getReconciler(ISourceViewer)
	 */
	public IReconciler getReconciler(ISourceViewer sourceViewer) {

		if (getEditor() != null && getEditor().isEditable()) {
			JavaReconciler reconciler= new JavaReconciler(getEditor(), new JavaReconcilingStrategy(getEditor()), false);
			reconciler.setProgressMonitor(new NullProgressMonitor());
			reconciler.setDelay(500);
			return reconciler;
		}
		
		return null;
	}

	/*
	 * @see SourceViewerConfiguration#getAutoIndentStrategy(ISourceViewer, String)
	 */
	public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourceViewer, String contentType) {
		if (JavaPartitionScanner.JAVA_DOC.equals(contentType) ||
				JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT.equals(contentType))
			return new JavaDocAutoIndentStrategy();
		return new JavaAutoIndentStrategy();
	}

	/*
	 * @see SourceViewerConfiguration#getDoubleClickStrategy(ISourceViewer, String)
	 */
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (JavaPartitionScanner.JAVA_DOC.equals(contentType) ||
				JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT.equals(contentType) ||
				JavaPartitionScanner.JAVA_SINGLE_LINE_COMMENT.equals(contentType))
			return new DefaultTextDoubleClickStrategy();
		else if (JavaPartitionScanner.JAVA_STRING.equals(contentType))
			return new JavaStringDoubleClickSelector();
		return new JavaDoubleClickSelector();
	}

	/*
	 * @see SourceViewerConfiguration#getDefaultPrefix(ISourceViewer, String)
	 * @since 2.0
	 */
	public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "//", "" }; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see SourceViewerConfiguration#getIndentPrefixes(ISourceViewer, String)
	 */
	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {

		Vector vector= new Vector();

		// prefix[0] is either '\t' or ' ' x tabWidth, depending on useSpaces
				
		Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
		int tabWidth= preferences.getInt(JavaCore.FORMATTER_TAB_SIZE);
		boolean useSpaces= getPreferenceStore().getBoolean(SPACES_FOR_TABS);
		
		for (int i= 0; i <= tabWidth; i++) {
		    StringBuffer prefix= new StringBuffer();

			if (useSpaces) {
			    for (int j= 0; j + i < tabWidth; j++)
			    	prefix.append(' ');
		    	
				if (i != 0)
		    		prefix.append('\t');				
			} else {    
			    for (int j= 0; j < i; j++)
			    	prefix.append(' ');
		    	
				if (i != tabWidth)
		    		prefix.append('\t');
			}
			
			vector.add(prefix.toString());
		}

		vector.add(""); //$NON-NLS-1$
		
		return (String[]) vector.toArray(new String[vector.size()]);
	}

	/*
	 * @see SourceViewerConfiguration#getTabWidth(ISourceViewer)
	 */
	public int getTabWidth(ISourceViewer sourceViewer) {
		return getPreferenceStore().getInt(PREFERENCE_TAB_WIDTH);
	}

	/*
	 * @see SourceViewerConfiguration#getAnnotationHover(ISourceViewer)
	 */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new JavaAnnotationHover();
	}

	/*
	 * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String)
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new JavaTextHover(getEditor());
	}
	
	/*
	 * @see SourceViewerConfiguration#getConfiguredContentTypes(ISourceViewer)
	 */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { 
			IDocument.DEFAULT_CONTENT_TYPE, 
			JavaPartitionScanner.JAVA_DOC, 
			JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT, 
			JavaPartitionScanner.JAVA_SINGLE_LINE_COMMENT,
			JavaPartitionScanner.JAVA_STRING
		};
	}
	
	/*
	 * @see SourceViewerConfiguration#getContentFormatter(ISourceViewer)
	 */
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
		
		ContentFormatter formatter= new ContentFormatter();
		IFormattingStrategy strategy= new JavaFormattingStrategy(sourceViewer);
		
		formatter.setFormattingStrategy(strategy, IDocument.DEFAULT_CONTENT_TYPE);
		formatter.enablePartitionAwareFormatting(false);		
		formatter.setPartitionManagingPositionCategories(fJavaTextTools.getPartitionManagingPositionCategories());
		
		return formatter;
	}
	
	/*
	 * @see SourceViewerConfiguration#getHoverControlCreator(ISourceViewer)
	 * @since 2.0
	 */
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		return getInformationControlCreator(sourceViewer, true);
	}
	
	private IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer, final boolean cutDown) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int style= cutDown ? SWT.NONE : (SWT.V_SCROLL | SWT.H_SCROLL);
				return new DefaultInformationControl(parent, style, new HTMLTextPresenter(cutDown));
				// return new HoverBrowserControl(parent);
			}
		};
	}
	
	/*
	 * @see SourceViewerConfiguration#getInformationPresenter(ISourceViewer)
	 * @since 2.0
	 */
	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
		InformationPresenter presenter= new InformationPresenter(getInformationControlCreator(sourceViewer, false));
		IInformationProvider provider= new JavaInformationProvider(getEditor());
		presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		presenter.setInformationProvider(provider, JavaPartitionScanner.JAVA_DOC);
		presenter.setSizeConstraints(60, 10, true, true);		
		return presenter;
	}
}
