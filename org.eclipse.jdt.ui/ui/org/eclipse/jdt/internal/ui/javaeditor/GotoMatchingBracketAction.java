package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;


public class GotoMatchingBracketAction extends Action {

	public final static String GOTO_MATCHING_BRACKET= "GotoMatchingBracket"; //$NON-NLS-1$
	
	private final CompilationUnitEditor fEditor;
	
	public GotoMatchingBracketAction(CompilationUnitEditor editor) {
		super(JavaEditorMessages.getString("GotoMatchingBracket.label"));
		Assert.isNotNull(editor);
		fEditor= editor;
		setEnabled(null != SelectionConverter.getInputAsCompilationUnit(fEditor));
	}
	
	public void run() {
		fEditor.gotoMatchingBracket();
	}
	
}