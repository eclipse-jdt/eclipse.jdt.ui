package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class AppearancePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String PREF_METHOD_RETURNTYPE= JavaUI.ID_PLUGIN + ".methodreturntype"; //$NON-NLS-1$
	public static final String PREF_OVERRIDE_INDICATOR= JavaUI.ID_PLUGIN + ".overrideindicator"; //$NON-NLS-1$
	public static final String PREF_COMPRESS_PACKAGE_NAMES= JavaUI.ID_PLUGIN + ".compresspackagenames"; //$NON-NLS-1$
	public static final String PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW= "PackagesView.pkgNamePatternForPackagesView"; //$NON-NLS-1$

	public static boolean showMethodReturnType() {
		return JavaPlugin.getDefault().getPreferenceStore().getBoolean(PREF_METHOD_RETURNTYPE);
	}
	
	public static boolean showOverrideIndicators() {
		return JavaPlugin.getDefault().getPreferenceStore().getBoolean(PREF_OVERRIDE_INDICATOR);
	}	

	public static String getPkgNamePatternForPackagesView() {
		if (! isCompressingEnabled())
			return "";
		return JavaPlugin.getDefault().getPreferenceStore().getString(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW);
	}

	public static boolean isCompressingPkgNameInPackagesView() {
		return isCompressingEnabled() && getPkgNamePatternForPackagesView().length() > 0;
	}

	private static boolean isCompressingEnabled() {
		return JavaPlugin.getDefault().getPreferenceStore().getBoolean(PREF_COMPRESS_PACKAGE_NAMES);
	}
	
	/**
	 * Initializes the current options (read from preference store)
	 */
	public static void initDefaults(IPreferenceStore prefs) {
		prefs.setDefault(PREF_COMPRESS_PACKAGE_NAMES, false);
		prefs.setDefault(PREF_METHOD_RETURNTYPE, false);
		prefs.setDefault(PREF_OVERRIDE_INDICATOR, true);
		prefs.setDefault(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW, ""); //$NON-NLS-1$
	}
	
	private SelectionButtonDialogField fShowMethodReturnType;
	private SelectionButtonDialogField fShowOverrideIndicator;
	private SelectionButtonDialogField fCompressPackageNames;
	private StringDialogField fPackageNamePattern;
			
	public AppearancePreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("AppearancePreferencePage.description")); //$NON-NLS-1$
	
		IDialogFieldListener listener= new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				doDialogFieldChanged(field);
			}
		};
	
		fShowMethodReturnType= new SelectionButtonDialogField(SWT.CHECK);
		fShowMethodReturnType.setDialogFieldListener(listener);
		fShowMethodReturnType.setLabelText(JavaUIMessages.getString("AppearancePreferencePage.methodreturntype.label")); //$NON-NLS-1$
		
		fShowOverrideIndicator= new SelectionButtonDialogField(SWT.CHECK);
		fShowOverrideIndicator.setDialogFieldListener(listener);
		fShowOverrideIndicator.setLabelText(JavaUIMessages.getString("AppearancePreferencePage.overrideindicator.label")); //$NON-NLS-1$

		fCompressPackageNames= new SelectionButtonDialogField(SWT.CHECK);
		fCompressPackageNames.setDialogFieldListener(listener);
		fCompressPackageNames.setLabelText("Co&mpress package name segments (except for the last one)"); 

		fPackageNamePattern= new StringDialogField();
		fPackageNamePattern.setDialogFieldListener(listener);
		fPackageNamePattern.setLabelText(JavaUIMessages.getString("AppearancePreferencePage.pkgNamePattern.label")); //$NON-NLS-1$
	}	

	private void initFields() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fShowMethodReturnType.setSelection(prefs.getBoolean(PREF_METHOD_RETURNTYPE));
		fShowOverrideIndicator.setSelection(prefs.getBoolean(PREF_OVERRIDE_INDICATOR));
		fPackageNamePattern.setText(prefs.getString(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW));
		fCompressPackageNames.setSelection(prefs.getBoolean(PREF_COMPRESS_PACKAGE_NAMES));
		fPackageNamePattern.setEnabled(fCompressPackageNames.isSelected());
	}
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.APPEARANCE_PREFERENCE_PAGE);
	}	

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		int nColumns= 1;
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= nColumns;
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(layout);
				
		fShowMethodReturnType.doFillIntoGrid(composite, nColumns);
		fShowOverrideIndicator.doFillIntoGrid(composite, nColumns);
		new Separator().doFillIntoGrid(composite, nColumns);
		fCompressPackageNames.doFillIntoGrid(composite, nColumns);
		fPackageNamePattern.doFillIntoGrid(composite, 2);
		LayoutUtil.setWidthHint(fPackageNamePattern.getLabelControl(null), convertWidthInCharsToPixels(80));
		
		initFields();
		
		return composite;
	}
	
	private void doDialogFieldChanged(DialogField field) {
		if (field == fCompressPackageNames)
			fPackageNamePattern.setEnabled(fCompressPackageNames.isSelected());
	
		updateStatus(getValidationStatus());
	}
	
	private IStatus getValidationStatus(){
		if (fCompressPackageNames.isSelected() && fPackageNamePattern.getText().equals(""))
			return new StatusInfo(IStatus.ERROR, "Enter a package name compression pattern");
		else	
			return new StatusInfo();
	}
	
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
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
		prefs.setValue(PREF_METHOD_RETURNTYPE, fShowMethodReturnType.isSelected());
		prefs.setValue(PREF_OVERRIDE_INDICATOR, fShowOverrideIndicator.isSelected());
		prefs.setValue(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW, fPackageNamePattern.getText());
		prefs.setValue(PREF_COMPRESS_PACKAGE_NAMES, fCompressPackageNames.isSelected());
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}	
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fShowMethodReturnType.setSelection(prefs.getDefaultBoolean(PREF_METHOD_RETURNTYPE));
		fShowOverrideIndicator.setSelection(prefs.getDefaultBoolean(PREF_OVERRIDE_INDICATOR));
		fPackageNamePattern.setText(prefs.getDefaultString(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW));
		fCompressPackageNames.setSelection(prefs.getDefaultBoolean(PREF_COMPRESS_PACKAGE_NAMES));
		super.performDefaults();
	}
}

