package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ResourceBundle;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.ui.texteditor.ITextEditor;import org.eclipse.ui.texteditor.TextEditorAction;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * A toolbar action which toggles the enabling state of the 
 * editor's text hover.
 */
public class ToggleTextHoverAction extends TextEditorAction implements IPropertyChangeListener {
	
	
	private IPreferenceStore fStore;
	
	private String fToolTipChecked;
	private String fToolTipUnchecked;
	
	/**
	 * Constructs and updates the action.
	 */
	public ToggleTextHoverAction() {
		super(JavaEditorMessages.getResourceBundle(), "ToggleTextHover", null); //$NON-NLS-1$
		
		JavaPluginImages.setToolImageDescriptors(this, "jdoc_hover_edit.gif"); //$NON-NLS-1$
		
		fToolTipChecked= JavaEditorMessages.getString("ToggleTextHover.tooltip.checked"); //$NON-NLS-1$
		fToolTipUnchecked= JavaEditorMessages.getString("ToggleTextHover.tooltip.unchecked"); //$NON-NLS-1$
	
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.TOGGLE_TEXTHOVER_ACTION);	

		boolean showHover= getStore().getBoolean(IPreferencesConstants.EDITOR_SHOW_HOVER);
		setChecked(showHover);
		
		getStore().addPropertyChangeListener(this);
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
		boolean showHover= !getStore().getBoolean(IPreferencesConstants.EDITOR_SHOW_HOVER);
		getStore().setValue(IPreferencesConstants.EDITOR_SHOW_HOVER, showHover);
		setChecked(showHover);
		setToolTipText(getToolTipText(showHover));
	}
	
	/**
	 * @see TextEditorAction#update
	 */
	public void update() {
		boolean showHover= getStore().getBoolean(IPreferencesConstants.EDITOR_SHOW_HOVER);
		setChecked(showHover);
		setToolTipText(getToolTipText(showHover));
		setEnabled(true);
	}
	
	/**
	 * @see TextEditorAction#setEditor(ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {		
		super.setEditor(editor);
		update();
	}

	/**
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(IPreferencesConstants.EDITOR_SHOW_HOVER))
			update();
	}

}