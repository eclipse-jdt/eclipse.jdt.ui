/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IPreferencesConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/*
 * The page for setting java plugin preferences.
 */
public class JavaBasePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String PROP_JDK= "org.eclipse.jdt.ui.build.jdk.library";
	public static final String KEY_LIBRARY= "org.eclipse.jdt.ui.build.jdk.library.label";
	public static final String KEY_LINKING= "org.eclipse.jdt.ui.packages.linkselection";
	public static final String KEY_DESCRIPTION= "org.eclipse.jdt.ui.build.jdk.library.description";

	public JavaBasePreferencePage() {
		super(GRID);
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaPlugin.getResourceString(KEY_DESCRIPTION));
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(IPreferencesConstants.LINK_PACKAGES_TO_EDITOR, true);
	}

	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();
		FileFieldEditor editor= new FileFieldEditor(PROP_JDK, JavaPlugin.getResourceString(KEY_LIBRARY), true, parent);
		editor.setFileExtensions(new String[] {"*.jar", "*.zip"});
		addField(editor);

		JDKZipFieldEditor jdkEditor= new JDKZipFieldEditor(parent);
		jdkEditor.setFileExtensions(new String[] {"*.jar", "*.zip"});
		addField(jdkEditor);

		//fix for: 1G840WG: ITPJUI:WINNT - 'source for binaries' - inconsistent order 
		ZipFileFieldEditor zipEditor= new ZipFileFieldEditor(parent);
		jdkEditor.setZipEditor(zipEditor);
		addField(zipEditor);

		BooleanFieldEditor boolEditor= new BooleanFieldEditor(
			IPreferencesConstants.LINK_PACKAGES_TO_EDITOR,
			JavaPlugin.getResourceString(KEY_LINKING),
			parent
        );
		addField(boolEditor);
		
	}

	public void init(IWorkbench workbench) {
	}
	
	/**
	 * Gets the current setting for the default jdk (rt.jar)
	 */
	public static IPath getJDKPath() {
		IPreferenceStore pstore= JavaPlugin.getDefault().getPreferenceStore();
		String jdk= pstore.getString(JavaBasePreferencePage.PROP_JDK);
		if (jdk != null && !"".equals(jdk)) {
			return new Path(jdk);
		} else {
			return null;
		}
	}

	/**
	 * Gets the current settings for the jdk source attachment
	 * Returns IPath[2]: index 0: source, index 1: prefix
	 */	
	public static IPath[] getJDKSourceAttachment() {
		IPreferenceStore pstore= JavaPlugin.getDefault().getPreferenceStore();
		String source= pstore.getString(JDKZipFieldEditor.PROP_SOURCE);
		String prefix= pstore.getString(JDKZipFieldEditor.PROP_PREFIX);
		if (source != null && prefix != null && !"".equals(source)) {
			return new IPath[] { new Path(source), new Path(prefix) };
		}
		return null;
	}
}


