package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.util.ResourceBundle;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class GotoErrorAction extends TextEditorAction {
	
	
	private boolean fForward;
	
	
	public GotoErrorAction(ResourceBundle bundle, String prefix, boolean forward) {
		super(bundle, prefix, null);
		fForward= forward;
		setImageDescriptor(forward ? JavaPluginImages.DESC_TOOL_GOTO_NEXT_ERROR : JavaPluginImages.DESC_TOOL_GOTO_PREV_ERROR);
	}
	
	/**
	 * @see Action#run()
	 */
	public void run() {
		CompilationUnitEditor e= (CompilationUnitEditor) getTextEditor();
		e.gotoError(fForward);
	}
	
	/**
	 * @see TextEditorAction#setEditor(ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {
		if (editor instanceof CompilationUnitEditor) 
			super.setEditor(editor);
	}
	
	/**
	 * @see TextEditorAction#update()
	 */
	public void update() {
		setEnabled(true);
	}
}