package org.eclipse.jdt.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.HoverTextControl;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IHoverControl;
import org.eclipse.jface.text.IHoverControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.internal.html.HoverBrowserControl;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.RuleBasedDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.internal.ui.text.JavaAnnotationHover;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.LineWrappingTextPresenter;
import org.eclipse.jdt.internal.ui.text.java.JavaAutoIndentStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProcessor;
import org.eclipse.jdt.internal.ui.text.java.JavaDoubleClickSelector;
import org.eclipse.jdt.internal.ui.text.java.JavaFormattingStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaReconcilingStrategy;
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
	
	/* 
	 * @see ISourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		IColorManager manager= getColorManager();
		PresentationReconciler reconciler= new PresentationReconciler();

		RuleBasedDamagerRepairer dr= new RuleBasedDamagerRepairer(getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr= new RuleBasedDamagerRepairer(getJavaDocScanner());
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_DOC);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_DOC);

		RuleBasedScanner scanner= new RuleBasedScanner();
		scanner.setDefaultReturnToken(new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_MULTI_LINE_COMMENT))));
		dr= new RuleBasedDamagerRepairer(scanner);		
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT);

		scanner= new RuleBasedScanner();
		scanner.setDefaultReturnToken(new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT))));
		dr= new RuleBasedDamagerRepairer(scanner);		
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_SINGLE_LINE_COMMENT);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_SINGLE_LINE_COMMENT);
		
		return reconciler;
	}

	/*
	 * @see SourceViewerConfiguration#getContentAssistant(ISourceViewer)
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		
		ContentAssistant assistant= new ContentAssistant();
		assistant.setContentAssistProcessor(new JavaCompletionProcessor(getEditor()), IDocument.DEFAULT_CONTENT_TYPE);
		assistant.setContentAssistProcessor(new JavaDocCompletionProcessor(getEditor()), JavaPartitionScanner.JAVA_DOC);
		
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);
		assistant.setProposalPopupOrientation(assistant.PROPOSAL_OVERLAY);
		assistant.setContextInformationPopupOrientation(assistant.CONTEXT_INFO_ABOVE);
		assistant.setHoverControlCreator(getHoverControlCreator(sourceViewer));
		
		Color background= getColorManager().getColor(new RGB(254, 241, 233));
		assistant.setContextInformationPopupBackground(background);
		assistant.setContextSelectorBackground(background);
		assistant.setProposalSelectorBackground(background);
		
		return assistant;
	}
	
	/*
	 * @see SourceViewerConfiguration#getReconciler(ISourceViewer)
	 */
	public IReconciler getReconciler(ISourceViewer sourceViewer) {

		if (getEditor() != null && getEditor().isEditable()) {
			MonoReconciler reconciler= new MonoReconciler(new JavaReconcilingStrategy(getEditor()), false);
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
		return new JavaDoubleClickSelector();
	}

	/*
	 * @see SourceViewerConfiguration#getDefaultPrefix(ISourceViewer, String)
	 */
	public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "//", "" };
	}

	/*
	 * @see SourceViewerConfiguration#getIndentPrefixes(ISourceViewer, String)
	 */
	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] {"\t", "    ", ""}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/*
	 * @see SourceViewerConfiguration#getTabWidth(ISourceViewer)
	 */
	public int getTabWidth(ISourceViewer sourceViewer) {
		return 4;
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
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, JavaPartitionScanner.JAVA_DOC, JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT, JavaPartitionScanner.JAVA_SINGLE_LINE_COMMENT };
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
	 */
	public IHoverControlCreator getHoverControlCreator(ISourceViewer sourceViewer) {
		return new IHoverControlCreator() {
			public IHoverControl createHoverControl(Shell parent) {
				if (true)
					return new HoverTextControl(parent, new LineWrappingTextPresenter());
				else
					return new HoverBrowserControl(parent);
			}
		};
	}
	
	/*
	 * @see SourceViewerConfiguration#getInformationPresenter(ISourceViewer)
	 */
	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
		InformationPresenter presenter= new InformationPresenter(getHoverControlCreator(sourceViewer));
		IInformationProvider provider= new JavaTextHover(getEditor());
		presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		presenter.setInformationProvider(provider, JavaPartitionScanner.JAVA_DOC);
		return presenter;
	}
}
