package org.eclipse.jdt.internal.ui.preferences;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaConventions;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class CodeGenerationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String PREF_USE_GETTERSETTER_PREFIX= PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX;
	private static final String PREF_GETTERSETTER_PREFIX= PreferenceConstants.CODEGEN_GETTERSETTER_PREFIX;

	private static final String PREF_USE_GETTERSETTER_SUFFIX= PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX;
	private static final String PREF_GETTERSETTER_SUFFIX= PreferenceConstants.CODEGEN_GETTERSETTER_SUFFIX;

	private static final String PREF_JAVADOC_STUBS= PreferenceConstants.CODEGEN__JAVADOC_STUBS;
	private static final String PREF_NON_JAVADOC_COMMENTS= PreferenceConstants.CODEGEN__NON_JAVADOC_COMMENTS;
	private static final String PREF_FILE_COMMENTS= PreferenceConstants.CODEGEN__FILE_COMMENTS;

	/**
	 * @deprecated Inline to avoid reference to preference page
	 */
	public static String[] getGetterStetterPrefixes() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		if (prefs.getBoolean(PREF_USE_GETTERSETTER_PREFIX)) {
			String str= prefs.getString(PREF_GETTERSETTER_PREFIX);
			if (str != null) {
				return unpackOrderList(str);
			}
		}
		return new String[0];
	}

	/**
	 * @deprecated Inline to avoid reference to preference page
	 */	
	public static String[] getGetterStetterSuffixes() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		if (prefs.getBoolean(PREF_USE_GETTERSETTER_SUFFIX)) {
			String str= prefs.getString(PREF_GETTERSETTER_SUFFIX);
			if (str != null) {
				return unpackOrderList(str);
			}
		}
		return new String[0];
	}	

	/**
	 * @deprecated Inline to avoid reference to preference page
	 */	
	public static boolean doCreateComments() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		return prefs.getBoolean(PREF_JAVADOC_STUBS);
	}

	/**
	 * @deprecated Inline to avoid reference to preference page
	 */	
	public static boolean doNonJavaDocSeeComments() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		return prefs.getBoolean(PREF_NON_JAVADOC_COMMENTS);
	}

	/**
	 * @deprecated Inline to avoid reference to preference page
	 */
	public static boolean doFileComments() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		return prefs.getBoolean(PREF_FILE_COMMENTS);
	}			
	
	

	private static String[] unpackOrderList(String str) {
		StringTokenizer tok= new StringTokenizer(str, ","); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			res[i]= tok.nextToken().trim();
		}
		return res;
	}
	
	private SelectionButtonDialogField fUseGetterSetterPrefix;
	private SelectionButtonDialogField fUseGetterSetterSuffix;
	private StringDialogField fGetterSetterPrefix;
	private StringDialogField fGetterSetterSuffix;
	
	private SelectionButtonDialogField fCreateJavaDocComments;
	private SelectionButtonDialogField fCreateNonJavadocComments;
	private SelectionButtonDialogField fCreateFileComments;
	
	private IStatus fGetterSetterPrefixStatus;
	private IStatus fGetterSetterSuffixStatus;
		
	public CodeGenerationPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(PreferencesMessages.getString("CodeGenerationPreferencePage.description")); //$NON-NLS-1$
	
		IDialogFieldListener listener= new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				doDialogFieldChanged(field);
			}
		};
		
		fGetterSetterPrefixStatus= new StatusInfo();
		fGetterSetterSuffixStatus= new StatusInfo();
	
		fUseGetterSetterPrefix= new SelectionButtonDialogField(SWT.CHECK);
		fUseGetterSetterPrefix.setDialogFieldListener(listener);
		fUseGetterSetterPrefix.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.prefix.checkbox")); //$NON-NLS-1$
		
		fGetterSetterPrefix= new StringDialogField();
		fGetterSetterPrefix.setDialogFieldListener(listener);
		fGetterSetterPrefix.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.prefix.list")); //$NON-NLS-1$
		
		fUseGetterSetterSuffix= new SelectionButtonDialogField(SWT.CHECK);
		fUseGetterSetterSuffix.setDialogFieldListener(listener);
		fUseGetterSetterSuffix.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.suffix.checkbox")); //$NON-NLS-1$

		fGetterSetterSuffix= new StringDialogField();
		fGetterSetterSuffix.setDialogFieldListener(listener);
		fGetterSetterSuffix.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.suffix.list")); //$NON-NLS-1$
		
		fCreateJavaDocComments= new SelectionButtonDialogField(SWT.CHECK);
		fCreateJavaDocComments.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.javadoc_comment.label")); //$NON-NLS-1$
		
		fCreateNonJavadocComments= new SelectionButtonDialogField(SWT.CHECK);
		fCreateNonJavadocComments.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.see_comment.label"));				 //$NON-NLS-1$
	
		fCreateFileComments= new SelectionButtonDialogField(SWT.CHECK);
		fCreateFileComments.setLabelText(PreferencesMessages.getString("CodeGenerationPreferencePage.file_comment.label"));				 //$NON-NLS-1$
	}
	
	private void initFields() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fUseGetterSetterSuffix.setSelection(prefs.getBoolean(PREF_USE_GETTERSETTER_SUFFIX));
		String str= prefs.getString(PREF_GETTERSETTER_SUFFIX);
		if (str == null) {
			str= ""; //$NON-NLS-1$
		}
		fGetterSetterSuffix.setText(str);
		fUseGetterSetterPrefix.setSelection(prefs.getBoolean(PREF_USE_GETTERSETTER_PREFIX));
		str= prefs.getString(PREF_GETTERSETTER_PREFIX);
		if (str == null) {
			str= ""; //$NON-NLS-1$
		}
		fGetterSetterPrefix.setText(str);
		fCreateJavaDocComments.setSelection(prefs.getBoolean(PREF_JAVADOC_STUBS));
		fCreateNonJavadocComments.setSelection(prefs.getBoolean(PREF_NON_JAVADOC_COMMENTS));
		fCreateFileComments.setSelection(prefs.getBoolean(PREF_FILE_COMMENTS));
	}
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.CODE_MANIPULATION_PREFERENCE_PAGE);
	}	

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= 0;
		layout.verticalSpacing= convertVerticalDLUsToPixels(10);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
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
		fCreateFileComments.doFillIntoGrid(javaDocGroup, 2);
		
		Group getterSetterGroup= new Group(result, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		getterSetterGroup.setLayout(layout);
		getterSetterGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		getterSetterGroup.setText(PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.label")); //$NON-NLS-1$

		fUseGetterSetterPrefix.doFillIntoGrid(getterSetterGroup, 2);
		fGetterSetterPrefix.doFillIntoGrid(getterSetterGroup, 2);
		LayoutUtil.setHorizontalIndent(fGetterSetterPrefix.getLabelControl(null), horizontalIndent);
		LayoutUtil.setHorizontalGrabbing(fGetterSetterPrefix.getTextControl(null));
		
		fUseGetterSetterSuffix.doFillIntoGrid(getterSetterGroup, 2);
		fGetterSetterSuffix.doFillIntoGrid(getterSetterGroup, 2);		
		LayoutUtil.setHorizontalIndent(fGetterSetterSuffix.getLabelControl(null), horizontalIndent);
		
		initFields();
		
		return result;
	}
	
	private void doDialogFieldChanged(DialogField field) {
		if (field == fGetterSetterPrefix || field == fUseGetterSetterPrefix) {
			fGetterSetterPrefix.setEnabled(fUseGetterSetterPrefix.isSelected());
			if (fUseGetterSetterPrefix.isSelected()) {
				String[] prefixes= unpackOrderList(fGetterSetterPrefix.getText());
				fGetterSetterPrefixStatus= validateIdentifiers(prefixes, true);
			} else {
				fGetterSetterPrefixStatus= new StatusInfo();
			}
		}
		if (field == fGetterSetterSuffix || field == fUseGetterSetterSuffix) {
			fGetterSetterSuffix.setEnabled(fUseGetterSetterSuffix.isSelected());
			if (fUseGetterSetterSuffix.isSelected()) {
				String[] prefixes= unpackOrderList(fGetterSetterSuffix.getText());
				fGetterSetterSuffixStatus= validateIdentifiers(prefixes, false);
			} else {
				fGetterSetterSuffixStatus= new StatusInfo();
			}
		}		
		
		updateStatus(StatusUtil.getMoreSevere(fGetterSetterPrefixStatus, fGetterSetterSuffixStatus));
	}
	
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}		
	
	private IStatus validateIdentifiers(String[] values, boolean prefix) {
		for (int i= 0; i < values.length; i++) {
			String val= values[i];
			if (val.length() == 0) {
				if (prefix) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.error.emptyprefix")); //$NON-NLS-1$
				} else {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("CodeGenerationPreferencePage.gettersetter.error.emptysuffix")); //$NON-NLS-1$
				}							
			}
			String name= prefix ? val + "x" : "x" + val; //$NON-NLS-2$ //$NON-NLS-1$
			IStatus status= JavaConventions.validateFieldName(name);
			if (status.matches(IStatus.ERROR)) {
				if (prefix) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getFormattedString("CodeGenerationPreferencePage.gettersetter.error.invalidprefix", val)); //$NON-NLS-1$
				} else {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getFormattedString("CodeGenerationPreferencePage.gettersetter.error.invalidsuffix", val)); //$NON-NLS-1$
				}
			}
		}
		return new StatusInfo();
	
	}


	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		prefs.setValue(PREF_USE_GETTERSETTER_SUFFIX, fUseGetterSetterSuffix.isSelected());
		prefs.setValue(PREF_GETTERSETTER_SUFFIX, fGetterSetterSuffix.getText());
		prefs.setValue(PREF_USE_GETTERSETTER_PREFIX, fUseGetterSetterPrefix.isSelected());
		prefs.setValue(PREF_GETTERSETTER_PREFIX, fGetterSetterPrefix.getText());
		prefs.setValue(PREF_JAVADOC_STUBS, fCreateJavaDocComments.isSelected());
		prefs.setValue(PREF_NON_JAVADOC_COMMENTS, fCreateNonJavadocComments.isSelected());
		prefs.setValue(PREF_FILE_COMMENTS, fCreateFileComments.isSelected());
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}	
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fUseGetterSetterSuffix.setSelection(prefs.getDefaultBoolean(PREF_USE_GETTERSETTER_SUFFIX));
		String str= prefs.getDefaultString(PREF_GETTERSETTER_SUFFIX);
		if (str == null) {
			str= ""; //$NON-NLS-1$
		}
		fGetterSetterSuffix.setText(str);
		fUseGetterSetterPrefix.setSelection(prefs.getDefaultBoolean(PREF_USE_GETTERSETTER_PREFIX));
		str= prefs.getDefaultString(PREF_GETTERSETTER_PREFIX);
		if (str == null) {
			str= ""; //$NON-NLS-1$
		}
		fGetterSetterPrefix.setText(str);
		fCreateJavaDocComments.setSelection(prefs.getDefaultBoolean(PREF_JAVADOC_STUBS));
		fCreateNonJavadocComments.setSelection(prefs.getDefaultBoolean(PREF_NON_JAVADOC_COMMENTS));		
		fCreateFileComments.setSelection(prefs.getDefaultBoolean(PREF_FILE_COMMENTS));
		super.performDefaults();
	}	

}

