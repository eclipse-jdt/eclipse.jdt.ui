/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
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
	public static final String KEY_LINK_MOVE_CU_IN_PACKAGES_TO_REFACTORING= "org.eclipse.jdt.ui.packages.linkMoveCuToRefactoring";
	public static final String KEY_LINK_RENAME_PACKAGE_IN_PACKAGES_TO_REFACTORING= "org.eclipse.jdt.ui.packages.linkRenamePackageToRefactoring";

	public static final String JDKLIB_VARIABLE= "JDK_LIBRARY";

	public JavaBasePreferencePage() {
		super(GRID);
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaPlugin.getResourceString(KEY_DESCRIPTION));
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(IPreferencesConstants.LINK_PACKAGES_TO_EDITOR, true);
		store.setDefault(IPreferencesConstants.LINK_MOVE_CU_IN_PACKAGES_TO_REFACTORING, false);
		store.setDefault(IPreferencesConstants.LINK_RENAME_PACKAGE_IN_PACKAGES_TO_REFACTORING, false);
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
		
		/* not active yet
		boolEditor= new BooleanFieldEditor(
			IPreferencesConstants.LINK_MOVE_CU_IN_PACKAGES_TO_REFACTORING,
			JavaPlugin.getResourceString(KEY_LINK_MOVE_CU_IN_PACKAGES_TO_REFACTORING),
			parent
		);
		addField(boolEditor);
		
		boolEditor= new BooleanFieldEditor(
			IPreferencesConstants.LINK_RENAME_PACKAGE_IN_PACKAGES_TO_REFACTORING,
			JavaPlugin.getResourceString(KEY_LINK_RENAME_PACKAGE_IN_PACKAGES_TO_REFACTORING),
			parent
		);
		addField(boolEditor);
		*/
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
	
	
	// ------------- shared jdk variable
	
	
	public static void initSharedJDKVariable() throws JavaModelException {
		updateJDKLibraryEntry();
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(JavaBasePreferencePage.PROP_JDK)) {
					try {
						updateJDKLibraryEntry();
					} catch (JavaModelException e) {
						JavaPlugin.log(e.getStatus());
					}
				}
			}
		});
	}
		
	
	private static IJavaProject getJavaProject() {
		// workaround (1GDKGEO: ITPJUI:WINNT - no JavaCore.newVariableEntry)
		return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProject("p");
	}
	
	
	private static void updateJDKLibraryEntry() throws JavaModelException {
		IPath jdkPath= JavaBasePreferencePage.getJDKPath();
		if (jdkPath == null) {
			jdkPath= new Path("");
		}
		IClasspathEntry entry= getJavaProject().newLibraryEntry(jdkPath);
		JavaCore.setClasspathVariable(JDKLIB_VARIABLE, entry);
	}
	

}


