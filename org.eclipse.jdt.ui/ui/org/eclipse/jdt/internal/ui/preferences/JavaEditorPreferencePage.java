/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.WorkbenchChainedTextFontFieldEditor;

import org.eclipse.jdt.ui.text.IJavaColorConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

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
		addField(new WorkbenchChainedTextFontFieldEditor(AbstractTextEditor.PREFERENCE_FONT, JavaUIMessages.getString("JavaEditorPreferencePage.font"), getFieldEditorParent())); //$NON-NLS-1$
		addField(new ColorFieldEditor(IJavaColorConstants.JAVA_DEFAULT, "Java code default:", getFieldEditorParent()));
		addField(new ColorFieldEditor(IJavaColorConstants.JAVA_MULTI_LINE_COMMENT, "Multi-line comments:", getFieldEditorParent()));
		addField(new ColorFieldEditor(IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT, "Single-line comments:", getFieldEditorParent()));
		addField(new ColorFieldEditor(IJavaColorConstants.JAVA_KEYWORD, "Keywords:", getFieldEditorParent()));
		addField(new ColorFieldEditor(IJavaColorConstants.JAVA_TYPE, "Built-in types:", getFieldEditorParent()));
		addField(new ColorFieldEditor(IJavaColorConstants.JAVA_STRING, "Strings:", getFieldEditorParent()));
		addField(new ColorFieldEditor(IJavaColorConstants.JAVADOC_DEFAULT, "JavaDoc default:", getFieldEditorParent()));
		addField(new ColorFieldEditor(IJavaColorConstants.JAVADOC_KEYWORD, "JavaDoc keywords:", getFieldEditorParent()));
		addField(new ColorFieldEditor(IJavaColorConstants.JAVADOC_TAG, "HTML tags in JavaDoc", getFieldEditorParent()));
		addField(new ColorFieldEditor(IJavaColorConstants.JAVADOC_LINK, "Links in JavaDoc", getFieldEditorParent()));
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}
	
	
	public static void initDefaults(IPreferenceStore store) {
		
		WorkbenchChainedTextFontFieldEditor.startPropagate(store, JFaceResources.TEXT_FONT);
		
		Display display= Display.getDefault();
		Color color= display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		PreferenceConverter.setDefault(store,  AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND, color.getRGB());
		
		color= display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		PreferenceConverter.setDefault(store,  AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, color.getRGB());		
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_MULTI_LINE_COMMENT, new RGB(63, 127, 95));
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT, new RGB(63, 127, 95));
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_KEYWORD, new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_TYPE, new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_STRING, new RGB(42, 0, 255));
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVA_DEFAULT, new RGB(0, 0, 0));
		
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_KEYWORD, new RGB(127, 159, 191));
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_TAG, new RGB(127, 127, 159));
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_LINK, new RGB(63, 63, 191));
		PreferenceConverter.setDefault(store, IJavaColorConstants.JAVADOC_DEFAULT, new RGB(63, 95, 191));
	}	
}
