package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ResourceBundle;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.ui.texteditor.ITextEditor;import org.eclipse.ui.texteditor.TextEditorAction;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover;


/**
 * A toolbar action which toggles the enabling state of the 
 * editor's text hover.
 */
public class ToggleTextHoverAction extends TextEditorAction {
	
	
	private IPreferenceStore fStore;
	private boolean fIsEnabled;
	
	
	/**
	 * Constructs and updates the action.
	 */
	public ToggleTextHoverAction(ResourceBundle bundle, String prefix) {
		super(bundle, prefix, null);
		JavaPluginImages.setImageDescriptors(this, "tool16", "format_edit.gif");
	}
	
	private IPreferenceStore getStore() {
		if (fStore == null)
			fStore= JavaPlugin.getDefault().getPreferenceStore();
		return fStore;
	}
	
	/**
	 * @see IAction#actionPerformed
	 */
	public void run() {
		fIsEnabled= !fIsEnabled;
		getStore().setValue(JavaTextHover.ENABLED, fIsEnabled);
		setChecked(fIsEnabled);
	}
	
	/**
	 * @see TextEditorAction#update
	 */
	public void update() {
		fIsEnabled= getStore().getBoolean(JavaTextHover.ENABLED);
		setChecked(fIsEnabled);
		setEnabled(true);
	}
	
	/**
	 * @see TextEditorAction#setEditor(ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {		
		super.setEditor(editor);
		update();
	}
}