package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ResourceBundle;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.text.IRegion;import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.ui.texteditor.ITextEditor;import org.eclipse.ui.texteditor.TextEditorAction;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * A toolbar action which toggles the presentation model of the
 * connected text editor. The editor shows either the highlight range
 * only or always the whole document.
 */
public class TogglePresentationAction extends TextEditorAction implements IPropertyChangeListener {
		
	private IPreferenceStore fStore;

	private String fToolTipChecked;
	private String fToolTipUnchecked;
	
	
	/**
	 * Constructs and updates the action.
	 */
	public TogglePresentationAction() {
		super(JavaEditorMessages.getResourceBundle(), "TooglePresentation.", null); //$NON-NLS-1$
		
		JavaPluginImages.setToolImageDescriptors(this, "segment_edit.gif"); //$NON-NLS-1$
		
		fToolTipChecked= JavaEditorMessages.getString("TogglePresentation.tooltip.checked"); //$NON-NLS-1$
		fToolTipUnchecked= JavaEditorMessages.getString("TogglePresentation.tooltip.unchecked"); //$NON-NLS-1$
		
		update();
		WorkbenchHelp.setHelp(this,	IJavaHelpContextIds.TOGGLE_PRESENTATION_ACTION);
		
		getStore().addPropertyChangeListener(this);
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
		
		getStore().setValue(IPreferencesConstants.EDITOR_SHOW_SEGMENTS, showAll);
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
			
			boolean showSegments= getStore().getBoolean(IPreferencesConstants.EDITOR_SHOW_SEGMENTS);			
			setChecked(showSegments);
			setToolTipText(getToolTipText(showSegments));
			
			if (editor.showsHighlightRangeOnly() != showSegments) {
				IRegion remembered= editor.getHighlightRange();
				editor.resetHighlightRange();
				editor.showHighlightRangeOnly(showSegments);
				if (remembered != null)
					editor.setHighlightRange(remembered.getOffset(), remembered.getLength(), true);
			}
		}
	}

	/**
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(IPreferencesConstants.EDITOR_SHOW_SEGMENTS))
			update();
	}

}