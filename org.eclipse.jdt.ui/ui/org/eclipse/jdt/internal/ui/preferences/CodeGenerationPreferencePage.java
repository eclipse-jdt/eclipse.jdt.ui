/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/*
 * The page to configure the code formatter options.
 */
public class CodeGenerationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IStatusChangeListener {

	private static final String PREF_USE_GETTERSETTER_PREFIX= PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX;
	private static final String PREF_USE_GETTERSETTER_SUFFIX= PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX;

	private static final String PREF_JAVADOC_STUBS= PreferenceConstants.CODEGEN__JAVADOC_STUBS;
	private static final String PREF_NON_JAVADOC_COMMENTS= PreferenceConstants.CODEGEN__NON_JAVADOC_COMMENTS;
	private static final String PREF_FILE_COMMENTS= PreferenceConstants.CODEGEN__FILE_COMMENTS;

	private SelectionButtonDialogField fUseGetterSetterPrefix;
	private SelectionButtonDialogField fUseGetterSetterSuffix;
	
	private SelectionButtonDialogField fCreateJavaDocComments;
	private SelectionButtonDialogField fCreateNonJavadocComments;
	private SelectionButtonDialogField fCreateFileComments;

	private NameConventionConfigurationBlock fConfigurationBlock;

	public CodeGenerationPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(PreferencesMessages.getString("CodeGenerationPreferencePage.description")); //$NON-NLS-1$
		
		// only used when page is shown programatically
		setTitle(PreferencesMessages.getString("CodeGenerationPreferencePage.title"));		
		
		fConfigurationBlock= new NameConventionConfigurationBlock(this, null);

		fUseGetterSetterPrefix= new SelectionButtonDialogField(SWT.CHECK);
		fUseGetterSetterPrefix.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.prefix.checkbox")); //$NON-NLS-1$
		
		fUseGetterSetterSuffix= new SelectionButtonDialogField(SWT.CHECK);
		fUseGetterSetterSuffix.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.suffix.checkbox")); //$NON-NLS-1$
	
		fCreateJavaDocComments= new SelectionButtonDialogField(SWT.CHECK);
		fCreateJavaDocComments.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.javadoc_comment.label")); //$NON-NLS-1$
	
		fCreateNonJavadocComments= new SelectionButtonDialogField(SWT.CHECK);
		fCreateNonJavadocComments.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.see_comment.label"));				 //$NON-NLS-1$
	
		fCreateJavaDocComments.attachDialogField(fCreateNonJavadocComments);

		fCreateFileComments= new SelectionButtonDialogField(SWT.CHECK);
		fCreateFileComments.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.file_comment.label"));				 //$NON-NLS-1$

		IPreferenceStore prefs= PreferenceConstants.getPreferenceStore();
		fUseGetterSetterSuffix.setSelection(prefs.getBoolean(PREF_USE_GETTERSETTER_SUFFIX));
		fUseGetterSetterPrefix.setSelection(prefs.getBoolean(PREF_USE_GETTERSETTER_PREFIX));
		fCreateJavaDocComments.setSelection(prefs.getBoolean(PREF_JAVADOC_STUBS));
		fCreateNonJavadocComments.setSelection(prefs.getBoolean(PREF_NON_JAVADOC_COMMENTS));
		fCreateFileComments.setSelection(prefs.getBoolean(PREF_FILE_COMMENTS));
	}

	/*
	 * @see IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */	
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.CODEFORMATTER_PREFERENCE_PAGE);
	}	

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		result.setLayout(layout);
	
		int horizontalIndent= convertWidthInCharsToPixels(4);

		Group javaDocGroup= new Group(result, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		javaDocGroup.setLayout(layout);
		javaDocGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		javaDocGroup.setText(PreferencesMessages.getString("CodeGenerationPreferencePage.comments.label")); //$NON-NLS-1$
		fCreateJavaDocComments.doFillIntoGrid(javaDocGroup, 2);
		fCreateNonJavadocComments.doFillIntoGrid(javaDocGroup, 2);
		LayoutUtil.setHorizontalIndent(fCreateNonJavadocComments.getSelectionButton(null), horizontalIndent);
		fCreateFileComments.doFillIntoGrid(javaDocGroup, 2);
	
/*		Group getterSetterGroup= new Group(result, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		getterSetterGroup.setLayout(layout);
		getterSetterGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		getterSetterGroup.setText(PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.label")); //$NON-NLS-1$

		fUseGetterSetterPrefix.doFillIntoGrid(getterSetterGroup, 2);
		fUseGetterSetterSuffix.doFillIntoGrid(getterSetterGroup, 2);
*/		
		Control control= fConfigurationBlock.createContents(result);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		return result;
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		prefs.setValue(PREF_USE_GETTERSETTER_SUFFIX, fUseGetterSetterSuffix.isSelected());
		prefs.setValue(PREF_USE_GETTERSETTER_PREFIX, fUseGetterSetterPrefix.isSelected());
		prefs.setValue(PREF_JAVADOC_STUBS, fCreateJavaDocComments.isSelected());
		prefs.setValue(PREF_NON_JAVADOC_COMMENTS, fCreateNonJavadocComments.isSelected());
		prefs.setValue(PREF_FILE_COMMENTS, fCreateFileComments.isSelected());
		JavaPlugin.getDefault().savePluginPreferences();		
		
		if (!fConfigurationBlock.performOk(true)) {
			return false;
		}	
		return super.performOk();
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();

		fUseGetterSetterSuffix.setSelection(prefs.getDefaultBoolean(PREF_USE_GETTERSETTER_SUFFIX));
		fUseGetterSetterPrefix.setSelection(prefs.getDefaultBoolean(PREF_USE_GETTERSETTER_PREFIX));
		fCreateJavaDocComments.setSelection(prefs.getDefaultBoolean(PREF_JAVADOC_STUBS));
		fCreateNonJavadocComments.setSelection(prefs.getDefaultBoolean(PREF_NON_JAVADOC_COMMENTS));		
		fCreateFileComments.setSelection(prefs.getDefaultBoolean(PREF_FILE_COMMENTS));
		
		fConfigurationBlock.performDefaults();
		super.performDefaults();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);		
	}

}



