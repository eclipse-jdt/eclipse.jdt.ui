package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
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
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class AppearancePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	// need to be 
	public static final String PREF_METHOD_RETURNTYPE= JavaUI.ID_PLUGIN + ".methodreturntype"; //$NON-NLS-1$
	public static final String PREF_OVERRIDE_INDICATOR= JavaUI.ID_PLUGIN + ".overrideindicator"; //$NON-NLS-1$

	public static boolean showMethodReturnType() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		return prefs.getBoolean(PREF_METHOD_RETURNTYPE);
	}
	
	public static boolean showOverrideIndicators() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		return prefs.getBoolean(PREF_OVERRIDE_INDICATOR);
	}	
	
	/**
	 * Initializes the current options (read from preference store)
	 */
	public static void initDefaults(IPreferenceStore prefs) {
		prefs.setDefault(PREF_METHOD_RETURNTYPE, false);
		prefs.setDefault(PREF_OVERRIDE_INDICATOR, true);
	}
	
	private SelectionButtonDialogField fShowMethodReturnType;
	private SelectionButtonDialogField fShowOverrideIndicator;
	
		
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
	}
	
	private void initFields() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fShowMethodReturnType.setSelection(prefs.getBoolean(PREF_METHOD_RETURNTYPE));
		fShowOverrideIndicator.setSelection(prefs.getBoolean(PREF_OVERRIDE_INDICATOR));
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
		int nColumns= 1;
		
		MGridLayout layout= new MGridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= nColumns;
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(layout);
				
		fShowMethodReturnType.doFillIntoGrid(composite, nColumns);
		fShowOverrideIndicator.doFillIntoGrid(composite, nColumns);
			
		initFields();
		
		return composite;
	}
	
	private void doDialogFieldChanged(DialogField field) {
		// no validation needed
		updateStatus(new StatusInfo());
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
		return super.performOk();
	}	
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fShowMethodReturnType.setSelection(prefs.getDefaultBoolean(PREF_METHOD_RETURNTYPE));
		fShowOverrideIndicator.setSelection(prefs.getDefaultBoolean(PREF_OVERRIDE_INDICATOR));
		super.performDefaults();
	}	

}

