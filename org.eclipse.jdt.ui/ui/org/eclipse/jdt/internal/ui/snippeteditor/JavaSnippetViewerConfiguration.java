package org.eclipse.jdt.internal.ui.snippeteditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.rules.RuleBasedDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.java.JavaAutoIndentStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaDoubleClickSelector;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocCompletionProcessor;


/**
 *  The source viewer configuration for the Java snippet editor.
 */
public class JavaSnippetViewerConfiguration extends SourceViewerConfiguration {
	
	private JavaSnippetEditor fEditor;
	private JavaTextTools fJavaTextTools;
	
	/**
	 * Constructor.
	 */
	public JavaSnippetViewerConfiguration(JavaTextTools tools, JavaSnippetEditor editor) {
		fJavaTextTools= tools;
		fEditor= editor;
	}
	
	protected RuleBasedScanner getCodeScanner() {
		return fJavaTextTools.getCodeScanner();
	}
	
	protected RuleBasedScanner getJavaDocScanner() {
		return fJavaTextTools.getJavaDocScanner();
	}
	
	protected IColorManager getColorManager() {
		return fJavaTextTools.getColorManager();
	}

	/**
	 * @see ISourceViewerConfiguration#getPresentationReconciler
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourcePart) {
		IColorManager manager= getColorManager();
		PresentationReconciler reconciler= new PresentationReconciler();

		RuleBasedDamagerRepairer dr= new RuleBasedDamagerRepairer(
			getCodeScanner(),
			new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_DEFAULT))
		);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr= new RuleBasedDamagerRepairer(
			getJavaDocScanner(),
			new TextAttribute(manager.getColor(IJavaColorConstants.JAVADOC_DEFAULT))
		);
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_DOC);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_DOC);

		dr= new RuleBasedDamagerRepairer(
			null,
			new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_MULTI_LINE_COMMENT))
		);		
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_MULTILINE_COMMENT);

		return reconciler;
	}
	
		
	/**
	 * @see ISourceViewerConfiguration#getConfiguredContentTypes
	 */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, JavaPartitionScanner.JAVA_DOC, JavaPartitionScanner.JAVA_MULTILINE_COMMENT };
	}

	/**
	 * @see ISourceViewerConfiguration#getContentAssistant
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

		ContentAssistant assistant= new ContentAssistant();
		assistant.setContentAssistProcessor(new JavaSnippetCompletionProcessor(fEditor), IDocument.DEFAULT_CONTENT_TYPE);
		assistant.setContentAssistProcessor(new JavaDocCompletionProcessor(fEditor), JavaPartitionScanner.JAVA_DOC);

		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);
		assistant.setProposalPopupOrientation(assistant.PROPOSAL_OVERLAY);
		assistant.setContextInformationPopupOrientation(assistant.CONTEXT_INFO_ABOVE);
		assistant.setContextInformationPopupBackground(getColorManager().getColor(new RGB(150, 150, 0)));

		return assistant;
	}

	/**
	 * @see SourceViewerConfiguration#getReconciler(Object)
	 */
	public IReconciler getReconciler(ISourceViewer sourcePart) {
		return null;
	}
	
	/**
	 * @see ISourceViewerConfiguration#getAutoIndentStrategy
	 */
	public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourcePart, String contentType) {
		return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? new JavaAutoIndentStrategy() : new DefaultAutoIndentStrategy());
	}
	
	/**
	 * @see ISourceViewerConfiguration#getDoubleClickStrategy
	 */
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourcePart, String contentType) {
		return new JavaDoubleClickSelector();
	}
	
	/**
	 * @see ISourceViewerConfiguration#getDefaultPrefix
	 */
	public String getDefaultPrefix(ISourceViewer sourcePart, String contentType) {
		return (contentType == null ? "//" : null);
	}
	
	/**
	 * @see ISourceViewerConfiguration#getIndentPrefixes
	 */
	public String[] getIndentPrefixes(ISourceViewer sourcePart, String contentType) {
		return new String[] { "\t", "    " };
	}
		
	/**
	 * @see ISourceViewerConfiguration#getTabWidth
	 */
	public int getTabWidth(ISourceViewer sourcePart) {
		return 4;
	}
}