package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ResourceBundle;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.text.IRegion;import org.eclipse.ui.texteditor.ITextEditor;import org.eclipse.ui.texteditor.TextEditorAction;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * A toolbar action which toggles the presentation model of the
 * connected text editor. The editor shows either the highlight range
 * only or always the whole document.
 */
public class TogglePresentationAction extends TextEditorAction {
		
	private String fToolTipChecked;
	private String fToolTipUnchecked;

	/**
	 * Constructs and updates the action.
	 */
	public TogglePresentationAction(ResourceBundle bundle, String prefix) {
		super(bundle, prefix, null);
		
		JavaPluginImages.setImageDescriptors(this, "tool16", "segment_edit.gif");
		
		fToolTipChecked= getString(bundle, prefix + "tooltip.checked", prefix + "tooltip.checked");
		fToolTipUnchecked= getString(bundle, prefix + "tooltip.unchecked", prefix + "tooltip.unchecked");
		
		update();
	}
	
	/**
	 * @see IAction#actionPerformed
	 */
	public void run() {
		
		ITextEditor editor= getTextEditor();
		if (editor == null)
			return;
		
		IRegion remembered= editor.getHighlightRange();
		editor.resetHighlightRange();
		
		boolean showAll= !editor.showsHighlightRangeOnly();
		setChecked(showAll);
		setToolTipText(getToolTipText(showAll));
		
		editor.showHighlightRangeOnly(showAll);
		if (remembered != null)
			editor.setHighlightRange(remembered.getOffset(), remembered.getLength(), true);
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(IPreferencesConstants.EDITOR_SHOW_SEGMENTS, showAll);
	}
	
	private String getToolTipText(boolean checked) {
		return checked ? fToolTipChecked : fToolTipUnchecked;
	}
	
	/**
	 * @see TextEditorAction#update
	 */
	public void update() {
		ITextEditor editor= getTextEditor();
		boolean checked= (editor != null && editor.showsHighlightRangeOnly());
		setChecked(checked);
		setToolTipText(getToolTipText(checked));
		setEnabled(true);
	}
	
	/**
	 * @see TextEditorAction#setEditor(ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {
		
		super.setEditor(editor);
		
		if (editor != null) {
			
			boolean showSegments= JavaPlugin.getDefault().getPreferenceStore().getBoolean(IPreferencesConstants.EDITOR_SHOW_SEGMENTS);
			
			if (isChecked() != showSegments) {
				setChecked(showSegments);
				setToolTipText(getToolTipText(showSegments));
			}
			
			if (editor.showsHighlightRangeOnly() != showSegments) {
				IRegion remembered= editor.getHighlightRange();
				editor.resetHighlightRange();
				editor.showHighlightRangeOnly(showSegments);
				if (remembered != null)
					editor.setHighlightRange(remembered.getOffset(), remembered.getLength(), true);
			}
		}
	}
}