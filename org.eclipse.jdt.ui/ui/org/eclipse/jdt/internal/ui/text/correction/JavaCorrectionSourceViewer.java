package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

public class JavaCorrectionSourceViewer extends SourceViewer {

	/** 
	 * Text operation code for requesting correction assist to show correction
	 * proposals for the current position. 
	 */
	public static final int CORRECTIONASSIST_PROPOSALS= 50;

	private JavaCorrectionAssistant fCorrectionAssistant;
	
	private CompilationUnitEditor fEditor;

	public JavaCorrectionSourceViewer(Composite parent, IVerticalRuler ruler, int styles, CompilationUnitEditor editor) {
		super(parent, ruler, styles);
		fEditor= editor;
	}

	/*
	 * @see ITextOperationTarget#doOperation(int)
	 */
	public void doOperation(int operation) {
		if (getTextWidget() == null)
			return;

		if (operation == CORRECTIONASSIST_PROPOSALS) {
			fCorrectionAssistant.showPossibleCompletions();
			return;
		}
		super.doOperation(operation);
	}

	/*
	 * @see ITextOperationTarget#canDoOperation(int)
	 */
	public boolean canDoOperation(int operation) {
		if (operation == CORRECTIONASSIST_PROPOSALS) {
			return true;
		}
		return super.canDoOperation(operation);
	}

	/*
	 * @see ISourceViewer#configure(SourceViewerConfiguration)
	 */
	public void configure(SourceViewerConfiguration configuration) {
		super.configure(configuration);
		fCorrectionAssistant= new JavaCorrectionAssistant(fEditor);
		fCorrectionAssistant.install(this);
	}

	/*
	 * @see TextViewer#handleDispose()
	 */
	protected void handleDispose() {
		if (fCorrectionAssistant != null) {
			fCorrectionAssistant.uninstall();
			fCorrectionAssistant= null;
		}
		super.handleDispose();
	}

};