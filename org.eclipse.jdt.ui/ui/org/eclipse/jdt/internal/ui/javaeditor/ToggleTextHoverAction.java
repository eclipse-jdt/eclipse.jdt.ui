package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ResourceBundle;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.ui.texteditor.ITextEditor;import org.eclipse.ui.texteditor.TextEditorAction;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.text.java.hover.JavaTextHover;


/**
 * A toolbar action which toggles the enabling state of the 
 * editor's text hover.
 */
public class ToggleTextHoverAction extends TextEditorAction {
	
	
	private IPreferenceStore fStore;
	private boolean fIsEnabled;
	
	private String fToolTipChecked;
	private String fToolTipUnchecked;
	
	
	/**
	 * Constructs and updates the action.
	 */
	public ToggleTextHoverAction(ResourceBundle bundle, String prefix) {
		super(bundle, prefix, null);
		
		JavaPluginImages.setImageDescriptors(this, "tool16", "format_edit.gif");
		
		fToolTipChecked= getString(bundle, prefix + "tooltip.checked", prefix + "tooltip.checked");
		fToolTipUnchecked= getString(bundle, prefix + "tooltip.unchecked", prefix + "tooltip.unchecked");
	}
	
	private IPreferenceStore getStore() {
		if (fStore == null)
			fStore= JavaPlugin.getDefault().getPreferenceStore();
		return fStore;
	}
	
	private String getToolTipText(boolean checked) {
		return checked ? fToolTipChecked : fToolTipUnchecked;
	}
	
	/**
	 * @see IAction#actionPerformed
	 */
	public void run() {
		fIsEnabled= !fIsEnabled;
		getStore().setValue(IPreferencesConstants.EDITOR_SHOW_HOVER, fIsEnabled);
		setChecked(fIsEnabled);
		setToolTipText(getToolTipText(fIsEnabled));
	}
	
	/**
	 * @see TextEditorAction#update
	 */
	public void update() {
		fIsEnabled= getStore().getBoolean(IPreferencesConstants.EDITOR_SHOW_HOVER);
		setChecked(fIsEnabled);
		setToolTipText(getToolTipText(fIsEnabled));
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