/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.PropagatingFontFieldEditor;


/**
 * A preference page to set the font used in the Java editor.
 * This preference page uses the key <code>"JavaEditorPreferencePage.description"</code> 
 * to look up the page description and <code>"JavaEditorPreferencePage.fontEditor"</code>
 * for the font editor description.
 */
public class JavaEditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	/**
	 * Create the preference page.
	 */
	public JavaEditorPreferencePage() {
		super(GRID);
		
		setDescription(JavaUIMessages.getString("JavaEditorPreferencePage.description"));		 //$NON-NLS-1$
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
	}
	
	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		// added for 1GEUGE6: ITPJUI:WIN2000 - Help is the same on all preference pages
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE));
	}	
	
	
	/*
	 * @see FieldEditorPreferencePage#createFieldEditors
	 */
	public void createFieldEditors() {
		addField(new PropagatingFontFieldEditor(AbstractTextEditor.PREFERENCE_FONT, JavaUIMessages.getString("JavaEditorPreferencePage.font"), getFieldEditorParent())); //$NON-NLS-1$
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}
	
	public static void initDefaults(IPreferenceStore store) {
		PropagatingFontFieldEditor.startPropagate(store, JFaceResources.TEXT_FONT);
	}	
}
