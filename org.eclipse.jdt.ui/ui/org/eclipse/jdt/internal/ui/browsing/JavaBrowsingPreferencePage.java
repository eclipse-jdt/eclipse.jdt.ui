/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/*
 * The page for setting the Java Browsing preferences.
 */
public class JavaBrowsingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String LINK_VIEW_TO_EDITOR= "org.eclipse.jdt.ui.browsing.linktoeditor"; //$NON-NLS-1$
	public static final String OPEN_EDITOR_ON_SINGLE_CLICK= "org.eclipse.jdt.ui.browsing.openEditorOnSinglClick"; //$NON-NLS-1$
	public static final String STACK_VERTICALLY= "org.eclipse.jdt.ui.browsing.stackVertically"; //$NON-NLS-1$
	public static final String EDITOR_THRESHOLD= "org.eclipse.jdt.ui.browsing.editorThreshold"; //$NON-NLS-1$

	private IntegerFieldEditor fIntegerEditor;
	
	public JavaBrowsingPreferencePage() {
		super(GRID);

		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaBrowsingMessages.getString("JavaBrowsingPreferencePage.description")); //$NON-NLS-1$
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(LINK_VIEW_TO_EDITOR, true);
		store.setDefault(OPEN_EDITOR_ON_SINGLE_CLICK, true);
		store.setDefault(STACK_VERTICALLY, false);
		store.setDefault(EDITOR_THRESHOLD, 1);
	}

	public static boolean linkViewSelectionToEditor() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(LINK_VIEW_TO_EDITOR);
	}


	public static boolean openEditorOnSingleClick() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(OPEN_EDITOR_ON_SINGLE_CLICK);
	}

	public static boolean stackHorizontal() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return !store.getBoolean(STACK_VERTICALLY);
	}

	public static int editorThreshold() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getInt(EDITOR_THRESHOLD);
	}

	/*
	 * Method declared on PreferencePage
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.JAVA_BROWSING_PREFERENCE_PAGE);
	}
	
	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();

		BooleanFieldEditor boolEditor= new BooleanFieldEditor(
			LINK_VIEW_TO_EDITOR,
			JavaBrowsingMessages.getString("JavaBrowsingPreferencePage.linkViewToEditor"), //$NON-NLS-1$
			parent
        );
		addField(boolEditor);

		boolEditor= new BooleanFieldEditor(
			OPEN_EDITOR_ON_SINGLE_CLICK,
			JavaBrowsingMessages.getString("JavaBrowsingPreferencePage.openEditorOnSingleClick"), //$NON-NLS-1$
			parent
        );
		addField(boolEditor);

		boolEditor= new BooleanFieldEditor(
			STACK_VERTICALLY,
			JavaBrowsingMessages.getString("JavaBrowsingPreferencePage.stackVertically"), //$NON-NLS-1$
			parent
        );
		addField(boolEditor);
		
		fIntegerEditor= new IntegerFieldEditor(
			EDITOR_THRESHOLD,
			JavaBrowsingMessages.getString("JavaBrowsingPreferencePage.reuseEditors"), //$NON-NLS-1$
			parent);
		fIntegerEditor.setPreferencePage(this);
		fIntegerEditor.setTextLimit(2);
		fIntegerEditor.setErrorMessage(JavaBrowsingMessages.getString("JavaBrowsingPreferencePage.reuseEditorsError")); //$NON-NLS-1$
		fIntegerEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);
		fIntegerEditor.setValidRange(1, 99);
		addField(fIntegerEditor);
	}

	/*
	 * Method declared on IWorkbenchPreferencePage
	 */
	public void init(IWorkbench workbench) {
	}

	/*
	 * Method declared on IPreferencePage.
	 */
	public boolean okToLeave() {
		return fIntegerEditor.isValid();
	}

	/*
	 * Method declared on IPropertyChangeListener.
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (fIntegerEditor.isValid())
			super.setErrorMessage(null);
		else
			super.setErrorMessage(fIntegerEditor.getErrorMessage());
		setValid(fIntegerEditor.isValid());
	}

	/*
	 * Prevent clearing of error message if integer field loses focus
	 */
	public void setErrorMessage(String message) {
		if (message == null && !fIntegerEditor.isValid())
			return;
		else
			super.setErrorMessage(message);
	}

	/*
	 * Method declared on IPreferencePage.
	 */
	public boolean performOk() {
		if (!super.performOk())
			return false;
		updateJavaBrowsingPerspectives();
		return true;
	}

	private void updateJavaBrowsingPerspectives() {
		IWorkbenchWindow[] wbWindows= PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i= 0; i < wbWindows.length; i++) {
			IWorkbenchPage[] wbPages= wbWindows[i].getPages();
			for (int j= 0; j < wbPages.length; j++) {
				IWorkbenchPage wbPage= wbPages[j];
				if (JavaPlugin.ID_BROWSING_PERSPECTIVE.equals(wbPage.getPerspective().getId()))
					// XXX: See: http://bugs.eclipse.org/bugs/show_bug.cgi?id=9392
					wbPage.setEditorReuseThreshold(editorThreshold());
			}
		}
	}
}
