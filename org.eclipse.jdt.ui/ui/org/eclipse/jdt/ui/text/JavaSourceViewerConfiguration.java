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
package org.eclipse.jdt.ui.text;

import java.util.Vector;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ExtendedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.PreferencesAdapter;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.AbstractJavaScanner;
import org.eclipse.jdt.internal.ui.text.CompoundContentAssistProcessor;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.JavaAnnotationHover;
import org.eclipse.jdt.internal.ui.text.JavaCommentScanner;
import org.eclipse.jdt.internal.ui.text.JavaCompositeReconcilingStrategy;
import org.eclipse.jdt.internal.ui.text.JavaElementProvider;
import org.eclipse.jdt.internal.ui.text.JavaOutlineInformationControl;
import org.eclipse.jdt.internal.ui.text.JavaReconciler;
import org.eclipse.jdt.internal.ui.text.SingleTokenJavaScanner;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingStrategy;
import org.eclipse.jdt.internal.ui.text.comment.DefaultTextMeasurement;
import org.eclipse.jdt.internal.ui.text.java.JavaAutoIndentStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaCodeScanner;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProcessor;
import org.eclipse.jdt.internal.ui.text.java.JavaDoubleClickSelector;
import org.eclipse.jdt.internal.ui.text.java.JavaFormattingStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaStringAutoIndentStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaStringDoubleClickSelector;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaEditorTextHoverDescriptor;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaEditorTextHoverProxy;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaInformationProvider;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAutoIndentStrategy;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocCompletionProcessor;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocScanner;
import org.eclipse.jdt.internal.ui.text.spelling.WordCompletionProcessor;
import org.eclipse.jdt.internal.ui.typehierarchy.HierarchyInformationControl;


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
	 * @deprecated As of 3.0 replaced by {@link ExtendedTextEditorPreferenceConstants#EDITOR_TAB_WIDTH}
	 */
	public final static String PREFERENCE_TAB_WIDTH= PreferenceConstants.EDITOR_TAB_WIDTH;

	/** 
	 * Preference key for inserting spaces rather than tabs.
	 * 
	 * @since 2.0
	 */
	public final static String SPACES_FOR_TABS= PreferenceConstants.EDITOR_SPACES_FOR_TABS;

	
	private JavaTextTools fJavaTextTools;
	private ITextEditor fTextEditor;
	private String fDocumentPartitioning;
	

	/**
	 * The Java source code scanner
	 *
	 * @since 3.0
	 */
	private AbstractJavaScanner fCodeScanner;
	/** 
	 * The Java multiline comment scanner 
	 *
	 * @since 3.0
	 */
	private AbstractJavaScanner fMultilineCommentScanner;
	/**
	 * The Java singleline comment scanner
	 *
	 * @since 3.0
	 */
	private AbstractJavaScanner fSinglelineCommentScanner;
	/**
	 * The Java string scanner
	 *
	 * @since 3.0
	 */
	private AbstractJavaScanner fStringScanner;
	/**
	 * The JavaDoc scanner
	 *
	 * @since 3.0
	 */
	private AbstractJavaScanner fJavaDocScanner;
	/**
	 * The new preference store
	 * <p>Note that this is work in progress and still subject to change.</p>
	 *
	 * @since 3.0
	 */
	private IPreferenceStore fNewPreferenceStore;
	/**
	 * The color manager
	 *
	 * @since 3.0
	 */
	private IColorManager fColorManager;
	
	/**
	 * Creates a new Java source viewer configuration for viewers in the given editor 
	 * using the given preference store, the color manager and the specified document partitioning.
	 * <p>
	 * Creates a Java source viewer configuration in the new setup without text tools. Clients are
	 * allowed to call {@link JavaSourceViewerConfiguration#setNewPreferenceStore(IPreferenceStore)} and
	 * {@link JavaSourceViewerConfiguration#handlePropertyChangeEvent(PropertyChangeEvent)}
	 * and disallowed to call {@link JavaSourceViewerConfiguration#getPreferenceStore()} on the resulting
	 * Java source viewer configuration.</p>
	 * <p>
	 * XXX: Note that this is work in progress and API is still subject to change.
	 * </p>
	 *
	 * @param colorManager the color manager
	 * @param newPreferenceStore the new preference store, can be read-only
	 * @param editor the editor in which the configured viewer(s) will reside
	 * @param partitioning the document partitioning for this configuration
	 * 
	 * @since 3.0
	 */
	public JavaSourceViewerConfiguration(IColorManager colorManager, IPreferenceStore newPreferenceStore, ITextEditor editor, String partitioning) {
		fColorManager= colorManager;
		setNewPreferenceStore(newPreferenceStore);
		fTextEditor= editor;
		fDocumentPartitioning= partitioning;
	}

	/**
	 * Creates a new Java source viewer configuration for viewers in the given editor 
	 * using the given Java tools and the specified document partitioning.
	 *
	 * @param tools the Java text tools to be used
	 * @param editor the editor in which the configured viewer(s) will reside
	 * @param partitioning the document partitioning for this configuration
	 * @see JavaTextTools
	 * @since 3.0
	 */
	public JavaSourceViewerConfiguration(JavaTextTools tools, ITextEditor editor, String partitioning) {
		fJavaTextTools= tools;
		fColorManager= tools.getColorManager();
		fNewPreferenceStore= createCombinedPreferenceStore();
		fCodeScanner= (AbstractJavaScanner) fJavaTextTools.getCodeScanner();
		fMultilineCommentScanner= (AbstractJavaScanner) fJavaTextTools.getMultilineCommentScanner();
		fSinglelineCommentScanner= (AbstractJavaScanner) fJavaTextTools.getSinglelineCommentScanner();
		fStringScanner= (AbstractJavaScanner) fJavaTextTools.getStringScanner();
		fJavaDocScanner= (AbstractJavaScanner) fJavaTextTools.getJavaDocScanner();
		fTextEditor= editor;
		fDocumentPartitioning= partitioning;
	}

	/**
	 * Creates a new Java source viewer configuration for viewers in the given editor 
	 * using the given Java tools.
	 *
	 * @param tools the Java text tools to be used
	 * @param editor the editor in which the configured viewer(s) will reside
	 * @see JavaTextTools
	 */
	public JavaSourceViewerConfiguration(JavaTextTools tools, ITextEditor editor) {
		this(tools, editor, null);
	}
	
	/**
	 * Returns the Java source code scanner for this configuration.
	 *
	 * @return the Java source code scanner
	 */
	protected RuleBasedScanner getCodeScanner() {
		return fCodeScanner;
	}
	
	/**
	 * Returns the Java multi-line comment scanner for this configuration.
	 *
	 * @return the Java multi-line comment scanner
	 * @since 2.0
	 */
	protected RuleBasedScanner getMultilineCommentScanner() {
		return fMultilineCommentScanner;
	}
	
	/**
	 * Returns the Java single-line comment scanner for this configuration.
	 *
	 * @return the Java single-line comment scanner
	 * @since 2.0
	 */
	protected RuleBasedScanner getSinglelineCommentScanner() {
		return fSinglelineCommentScanner;
	}
	
	/**
	 * Returns the Java string scanner for this configuration.
	 *
	 * @return the Java string scanner
	 * @since 2.0
	 */
	protected RuleBasedScanner getStringScanner() {
		return fStringScanner;
	}
	
	/**
	 * Returns the JavaDoc scanner for this configuration.
	 *
	 * @return the JavaDoc scanner
	 */
	protected RuleBasedScanner getJavaDocScanner() {
		return fJavaDocScanner;
	}
	
	/**
	 * Returns the color manager for this configuration.
	 *
	 * @return the color manager
	 */
	protected IColorManager getColorManager() {
		return fColorManager;
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
	 * <p>
	 * Clients are not allowed to call this method if the new setup without
	 * text tools is in use.
	 * @see JavaSourceViewerConfiguration#JavaSourceViewerConfiguration(IColorManager, IPreferenceStore, ITextEditor, String)</p>
	 * <p>
	 * FIXME: deprecate when new API is stabilized.
	 * </p> 
	 * 
	 * @return the preference store used to initialize this configuration
	 * @since 2.0
	 */
	public IPreferenceStore getPreferenceStore() {
		Assert.isTrue(!isNewSetup());
		return fJavaTextTools.getPreferenceStore();
	}
	
	/**
	 * Returns the new preference store used by this configuration to initialize
	 * the individual bits and pieces.
	 * <p>
	 * XXX: Note that this is work in progress and API is still subject to change.
	 * </p>
	 * 
	 * @return the new preference store used to initialize this configuration
	 * 
	 * @since 3.0
	 */
	public IPreferenceStore getNewPreferenceStore() {
		return fNewPreferenceStore;
	}
	
	/**
	 * Sets the preference store used by this configuration to initialize
	 * the individual bits and pieces to the given preference store.
	 * <p>
	 * Clients are not allowed to call this method if the old setup with
	 * text tools is in use.
	 * @see JavaSourceViewerConfiguration#JavaSourceViewerConfiguration(IColorManager, IPreferenceStore, ITextEditor, String)</p>
	 * <p>
	 * XXX: Note that this is work in progress and API is still subject to change.
	 * </p>
	 * 
	 * @param preferenceStore the preference store to initialize this configuration
	 * 
	 * @since 3.0
	 */
	public void setNewPreferenceStore(IPreferenceStore preferenceStore) {
		Assert.isTrue(isNewSetup());
		
		fNewPreferenceStore= preferenceStore;
		initializeScanners();
	}
	
	/**
	 * @return <code>true</code> iff the new setup without text tools is in use.
	 * 
	 * @since 3.0
	 */
	private boolean isNewSetup() {
		return fJavaTextTools == null;
	}

	/**
	 * Creates and returns a new preference store which combines the preference
	 * stores from the text tools.
	 *
	 * @param tools Text tools
	 * @return the combined preference store
	 * 
	 * @since 3.0
	 */
	private IPreferenceStore createCombinedPreferenceStore() {
		Assert.isTrue(!isNewSetup());
		if (fJavaTextTools.getCorePreferenceStore() == null)
			return fJavaTextTools.getPreferenceStore();
		
		return new ChainedPreferenceStore(new IPreferenceStore[] { fJavaTextTools.getPreferenceStore(), new PreferencesAdapter(fJavaTextTools.getCorePreferenceStore()) });
	}

	/**
	 * Initializes the scanners.
	 * 
	 * @since 3.0 
	 */
	private void initializeScanners() {
		Assert.isTrue(isNewSetup());
		fCodeScanner= new JavaCodeScanner(getColorManager(), getNewPreferenceStore());
		fMultilineCommentScanner= new JavaCommentScanner(getColorManager(), getNewPreferenceStore(), IJavaColorConstants.JAVA_MULTI_LINE_COMMENT);
		fSinglelineCommentScanner= new JavaCommentScanner(getColorManager(), getNewPreferenceStore(), IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT);
		fStringScanner= new SingleTokenJavaScanner(getColorManager(), getNewPreferenceStore(), IJavaColorConstants.JAVA_STRING);
		fJavaDocScanner= new JavaDocScanner(getColorManager(), getNewPreferenceStore());
	}

	/*
	 * @see SourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		PresentationReconciler reconciler= new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr= new DefaultDamagerRepairer(getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr= new DefaultDamagerRepairer(getJavaDocScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_DOC);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_DOC);

		dr= new DefaultDamagerRepairer(getMultilineCommentScanner());		
		reconciler.setDamager(dr, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);

		dr= new DefaultDamagerRepairer(getSinglelineCommentScanner());		
		reconciler.setDamager(dr, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		
		dr= new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_STRING);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_STRING);
		
		dr= new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_CHARACTER);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_CHARACTER);

		
		return reconciler;
	}

	/*
	 * @see SourceViewerConfiguration#getContentAssistant(ISourceViewer)
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		
		if (getEditor() != null) {
		
			ContentAssistant assistant= new ContentAssistant();
			assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
			
			assistant.setRestoreCompletionProposalSize(getSettings("completion_proposal_size")); //$NON-NLS-1$
			
			IContentAssistProcessor javaProcessor= new JavaCompletionProcessor(getEditor());
			assistant.setContentAssistProcessor(javaProcessor, IDocument.DEFAULT_CONTENT_TYPE);
			
			// Register the java processor for single line comments to get the NLS template working inside comments
			IContentAssistProcessor wordProcessor= new WordCompletionProcessor();
			CompoundContentAssistProcessor compoundProcessor= new CompoundContentAssistProcessor();
			compoundProcessor.add(javaProcessor);
			compoundProcessor.add(wordProcessor);
			
			assistant.setContentAssistProcessor(compoundProcessor, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
			
			assistant.setContentAssistProcessor(wordProcessor, IJavaPartitions.JAVA_STRING);
			assistant.setContentAssistProcessor(wordProcessor, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);

			assistant.setContentAssistProcessor(new JavaDocCompletionProcessor(getEditor()), IJavaPartitions.JAVA_DOC);
			
			ContentAssistPreference.configure(assistant, getNewPreferenceStore());
			
			assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
			assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
					
			return assistant;
		}
		
		return null;
	}
	
	/*
	 * @see SourceViewerConfiguration#getReconciler(ISourceViewer)
	 */
	public IReconciler getReconciler(ISourceViewer sourceViewer) {

		final ITextEditor editor= getEditor();
		if (editor != null && editor.isEditable()) {
			
			JavaCompositeReconcilingStrategy strategy= new JavaCompositeReconcilingStrategy(editor, getConfiguredDocumentPartitioning(sourceViewer));
			JavaReconciler reconciler= new JavaReconciler(editor, strategy, false);
			reconciler.setIsIncrementalReconciler(false);
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
		if (IJavaPartitions.JAVA_DOC.equals(contentType) ||
				IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(contentType))
			return new JavaDocAutoIndentStrategy(getConfiguredDocumentPartitioning(sourceViewer));
		else if (IJavaPartitions.JAVA_STRING.equals(contentType))
			return new JavaStringAutoIndentStrategy(getConfiguredDocumentPartitioning(sourceViewer));
		return new JavaAutoIndentStrategy(getConfiguredDocumentPartitioning(sourceViewer));
	}

	/*
	 * @see SourceViewerConfiguration#getDoubleClickStrategy(ISourceViewer, String)
	 */
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (IJavaPartitions.JAVA_DOC.equals(contentType) ||
				IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(contentType) ||
				IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(contentType))
			return new DefaultTextDoubleClickStrategy();
		else if (IJavaPartitions.JAVA_STRING.equals(contentType) ||
				IJavaPartitions.JAVA_CHARACTER.equals(contentType))
			return new JavaStringDoubleClickSelector(getConfiguredDocumentPartitioning(sourceViewer));
		return new JavaDoubleClickSelector();
	}

	/*
	 * @see SourceViewerConfiguration#getDefaultPrefixes(ISourceViewer, String)
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
				
		int tabWidth= CodeFormatterUtil.getTabWidth();
		boolean useSpaces= getNewPreferenceStore().getBoolean(SPACES_FOR_TABS);
		
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
		IPreferenceStore store= getNewPreferenceStore();
		
		if (store.contains(ExtendedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH))
			return store.getInt(ExtendedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
		else
			// backwards compatibility code
			return store.getInt(PREFERENCE_TAB_WIDTH);
	}

	/*
	 * @see SourceViewerConfiguration#getAnnotationHover(ISourceViewer)
	 */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new JavaAnnotationHover(JavaAnnotationHover.VERTICAL_RULER_HOVER);
	}

	/*
	 * @see SourceViewerConfiguration#getOverviewRulerAnnotationHover(ISourceViewer)
	 * @since 3.0
	 */
	public IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer) {
		return new JavaAnnotationHover(JavaAnnotationHover.OVERVIEW_RULER_HOVER);
	}

	/*
	 * @see SourceViewerConfiguration#getConfiguredTextHoverStateMasks(ISourceViewer, String)
	 * @since 2.1
	 */
	public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
		JavaEditorTextHoverDescriptor[] hoverDescs= JavaPlugin.getDefault().getJavaEditorTextHoverDescriptors();
		int stateMasks[]= new int[hoverDescs.length];
		int stateMasksLength= 0;		
		for (int i= 0; i < hoverDescs.length; i++) {
			if (hoverDescs[i].isEnabled()) {
				int j= 0;
				int stateMask= hoverDescs[i].getStateMask();
				while (j < stateMasksLength) {
					if (stateMasks[j] == stateMask)
						break;
					j++;
				}
				if (j == stateMasksLength)
					stateMasks[stateMasksLength++]= stateMask;
			}
		}
		if (stateMasksLength == hoverDescs.length)
			return stateMasks;
		
		int[] shortenedStateMasks= new int[stateMasksLength];
		System.arraycopy(stateMasks, 0, shortenedStateMasks, 0, stateMasksLength);
		return shortenedStateMasks;
	}
	
	/*
	 * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String, int)
	 * @since 2.1
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
		JavaEditorTextHoverDescriptor[] hoverDescs= JavaPlugin.getDefault().getJavaEditorTextHoverDescriptors();
		int i= 0;
		while (i < hoverDescs.length) {
			if (hoverDescs[i].isEnabled() &&  hoverDescs[i].getStateMask() == stateMask)
				return new JavaEditorTextHoverProxy(hoverDescs[i], getEditor());
			i++;
		}

		return null;
	}

	/*
	 * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String)
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return getTextHover(sourceViewer, contentType, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
	}
	
	/*
	 * @see SourceViewerConfiguration#getConfiguredContentTypes(ISourceViewer)
	 */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { 
			IDocument.DEFAULT_CONTENT_TYPE, 
			IJavaPartitions.JAVA_DOC, 
			IJavaPartitions.JAVA_MULTI_LINE_COMMENT, 
			IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
			IJavaPartitions.JAVA_STRING,
			IJavaPartitions.JAVA_CHARACTER
		};
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		if (fDocumentPartitioning != null)
			return fDocumentPartitioning;
		return super.getConfiguredDocumentPartitioning(sourceViewer);
	}
	
	/*
	 * @see SourceViewerConfiguration#getContentFormatter(ISourceViewer)
	 */
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
		final MultiPassContentFormatter formatter= new MultiPassContentFormatter(getConfiguredDocumentPartitioning(sourceViewer), IDocument.DEFAULT_CONTENT_TYPE);
		
		formatter.setMasterStrategy(new JavaFormattingStrategy());
		DefaultTextMeasurement textMeasurement= new DefaultTextMeasurement(sourceViewer.getTextWidget());
		formatter.setSlaveStrategy(new CommentFormattingStrategy(textMeasurement), IJavaPartitions.JAVA_DOC);
		formatter.setSlaveStrategy(new CommentFormattingStrategy(textMeasurement), IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		formatter.setSlaveStrategy(new CommentFormattingStrategy(textMeasurement), IJavaPartitions.JAVA_MULTI_LINE_COMMENT);		
		
		return formatter;
	}
	
	/*
	 * @see SourceViewerConfiguration#getInformationControlCreator(ISourceViewer)
	 * @since 2.0
	 */
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true));
			}
		};
	}

	/**
	 * Returns the information presenter control creator. The creator is a factory creating the
	 * presenter controls for the given source viewer. This implementation always returns a creator
	 * for <code>DefaultInformationControl</code> instances.
	 * 
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @return an information control creator
	 * @since 2.1
	 */
	private IInformationControlCreator getInformationPresenterControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle= SWT.RESIZE;
				int style= SWT.V_SCROLL | SWT.H_SCROLL;
				return new DefaultInformationControl(parent, shellStyle, style, new HTMLTextPresenter(false));
				// return new HoverBrowserControl(parent);
			}
		};
	}

	/**
	 * Returns the outline presenter control creator. The creator is a factory creating outline
	 * presenter controls for the given source viewer. This implementation always returns a creator
	 * for <code>JavaOutlineInformationControl</code> instances.
	 * 
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @return an information control creator
	 * @since 2.1
	 */
	private IInformationControlCreator getOutlinePresenterControlCreator(ISourceViewer sourceViewer, final String commandId) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle= SWT.RESIZE;
				int treeStyle= SWT.V_SCROLL | SWT.H_SCROLL;
				return new JavaOutlineInformationControl(parent, shellStyle, treeStyle, commandId);
			}
		};
	}
	
	private IInformationControlCreator getHierarchyPresenterControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle= SWT.RESIZE;
				int treeStyle= SWT.V_SCROLL | SWT.H_SCROLL;
				return new HierarchyInformationControl(parent, shellStyle, treeStyle);
			}
		};
	}	
	
	/*
	 * @see SourceViewerConfiguration#getInformationPresenter(ISourceViewer)
	 * @since 2.0
	 */
	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
		InformationPresenter presenter= new InformationPresenter(getInformationPresenterControlCreator(sourceViewer));
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		IInformationProvider provider= new JavaInformationProvider(getEditor());
		presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_DOC);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_CHARACTER);
		presenter.setSizeConstraints(60, 10, true, true);
		return presenter;
	}

	/**
	 * Returns the outline presenter which will determine and shown
	 * information requested for the current cursor position.
	 *
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @param doCodeResolve a boolean which specifies whether code resolve should be used to compute the Java element 
	 * @return an information presenter
	 * @since 2.1
	 */
	public IInformationPresenter getOutlinePresenter(ISourceViewer sourceViewer, boolean doCodeResolve) {
		InformationPresenter presenter;
		if (doCodeResolve)
			presenter= new InformationPresenter(getOutlinePresenterControlCreator(sourceViewer, IJavaEditorActionDefinitionIds.OPEN_STRUCTURE));
		else
			presenter= new InformationPresenter(getOutlinePresenterControlCreator(sourceViewer, IJavaEditorActionDefinitionIds.SHOW_OUTLINE));
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
		IInformationProvider provider= new JavaElementProvider(getEditor(), doCodeResolve);
		presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_DOC);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_STRING);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_CHARACTER);
		presenter.setSizeConstraints(20, 20, true, false);
		presenter.setRestoreInformationControlBounds(getSettings("outline_presenter_bounds"), true, true); //$NON-NLS-1$
		return presenter;
	}
	
	private IDialogSettings getSettings(String sectionName) {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings().getSection(sectionName);
		if (settings == null)
			settings= JavaPlugin.getDefault().getDialogSettings().addNewSection(sectionName);
		
		return settings;
	}
	
	public IInformationPresenter getHierarchyPresenter(ISourceViewer sourceViewer, boolean doCodeResolve) {
		InformationPresenter presenter= new InformationPresenter(getHierarchyPresenterControlCreator(sourceViewer));
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
		IInformationProvider provider= new JavaElementProvider(getEditor(), doCodeResolve);
		presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_DOC);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_STRING);
		presenter.setInformationProvider(provider, IJavaPartitions.JAVA_CHARACTER);
		presenter.setSizeConstraints(50, 20, true, false);
		presenter.setRestoreInformationControlBounds(getSettings("hierarchy_presenter_bounds"), true, true); //$NON-NLS-1$
		return presenter;
	}

	/**
	 * Determines whether the preference change encoded by the given event
	 * changes the behavior of one of its contained components.
	 * <p>
	 * XXX: Note that this is work in progress and API is still subject to change.
	 * </p>
	 * 
	 * @param event the event to be investigated
	 * @return <code>true</code> if event causes a behavioral change
	 * @since 3.0
	 */
	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		return  fCodeScanner.affectsBehavior(event)
			|| fMultilineCommentScanner.affectsBehavior(event)
			|| fSinglelineCommentScanner.affectsBehavior(event)
			|| fStringScanner.affectsBehavior(event)
			|| fJavaDocScanner.affectsBehavior(event);
	}
	
	/**
	 * Adapts the behavior of the contained components to the change
	 * encoded in the given event.
	 * <p>
	 * Clients are not allowed to call this method if the old setup with
	 * text tools is in use.
	 * @see JavaSourceViewerConfiguration#JavaSourceViewerConfiguration(IColorManager, IPreferenceStore, ITextEditor, String)</p>
	 * <p>
	 * XXX: Note that this is work in progress and API is still subject to change.
	 * </p>
	 * 
	 * @param event the event to which to adapt
	 * @since 3.0
	 */
	public void handlePropertyChangeEvent(PropertyChangeEvent event) {
		Assert.isTrue(isNewSetup());
		if (fCodeScanner.affectsBehavior(event))
			fCodeScanner.adaptToPreferenceChange(event);
		if (fMultilineCommentScanner.affectsBehavior(event))
			fMultilineCommentScanner.adaptToPreferenceChange(event);
		if (fSinglelineCommentScanner.affectsBehavior(event))
			fSinglelineCommentScanner.adaptToPreferenceChange(event);
		if (fStringScanner.affectsBehavior(event))
			fStringScanner.adaptToPreferenceChange(event);
		if (fJavaDocScanner.affectsBehavior(event))
			fJavaDocScanner.adaptToPreferenceChange(event);
	}
}
