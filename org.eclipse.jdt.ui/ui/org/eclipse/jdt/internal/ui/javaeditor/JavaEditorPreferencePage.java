package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.internal.ui.IPreferencesConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/*
 * The page for setting java editor preferences.
 */
public class JavaEditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	protected final static String HIGHLIGHT_KEYWORDS= "Editor.highlight_keywords";
	protected final static String HIGHLIGHT_TYPES= "Editor.highlight_types";
	protected final static String HIGHLIGHT_STRINGS= "Editor.highlight_strings";
	protected final static String HIGHLIGHT_COMMENTS= "Editor.highlight_comments";
	
	protected final static String AUTOINDENT= "Editor.autoindent";
	protected final static String AUTOINDENT_OFF= "Editor.autoindent_off";
	protected final static String AUTOINDENT_NORMAL= "Editor.autoindent_normal";
	protected final static String AUTOINDENT_SMART= "Editor.autoindent_smart";
	
	protected final static String MODEL_RECONCILER_DELAY= "Editor.model_reconciler_delay";
	
	
	
	public JavaEditorPreferencePage() {
		super(FLAT);
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
	}
	
	protected String getString(ResourceBundle bundle, String key, String dflt) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException x) {
			return dflt;
		}
	}
	
	/**
	 * @see FieldEditorPreferencePage#createFieldEditors
	 */
	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();
		
		ResourceBundle bndl= JavaPlugin.getResourceBundle();
		
		addField(new BooleanFieldEditor(IPreferencesConstants.HIGHLIGHT_KEYWORDS, getString(bndl, HIGHLIGHT_KEYWORDS, HIGHLIGHT_KEYWORDS), parent));
		addField(new BooleanFieldEditor(IPreferencesConstants.HIGHLIGHT_TYPES, getString(bndl, HIGHLIGHT_TYPES, HIGHLIGHT_TYPES), parent));
		addField(new BooleanFieldEditor(IPreferencesConstants.HIGHLIGHT_STRINGS, getString(bndl, HIGHLIGHT_STRINGS, HIGHLIGHT_STRINGS), parent));
		addField(new BooleanFieldEditor(IPreferencesConstants.HIGHLIGHT_COMMENTS, getString(bndl, HIGHLIGHT_COMMENTS, HIGHLIGHT_COMMENTS), parent));
		
		String[][] keys= new String[][] {
			{ getString(bndl, AUTOINDENT_OFF, AUTOINDENT_OFF), IPreferencesConstants.AUTOINDENT_OFF },
			{ getString(bndl, AUTOINDENT_NORMAL, AUTOINDENT_NORMAL), IPreferencesConstants.AUTOINDENT_NORMAL },
	 		{ getString(bndl, AUTOINDENT_SMART, AUTOINDENT_SMART), IPreferencesConstants.AUTOINDENT_SMART }
		};	
		addField(new RadioGroupFieldEditor(IPreferencesConstants.AUTOINDENT, getString(bndl, AUTOINDENT, AUTOINDENT), 1, keys, parent));
		
		addField(new IntegerFieldEditor(IPreferencesConstants.MODEL_RECONCILER_DELAY, getString(bndl, MODEL_RECONCILER_DELAY, MODEL_RECONCILER_DELAY), parent));
	}
	
	/**
	 * @see IWorkbenchPreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}
}
