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

import org.eclipse.jdt.ui.PreferenceConstants;

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

	private static final String SHOW_CU_CHILDREN= PreferenceConstants.SHOW_CU_CHILDREN;
	private static final String PREF_METHOD_RETURNTYPE= PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE;
	private static final String PREF_COMPRESS_PACKAGE_NAMES= PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES;
	private static final String PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW= PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW;
	private static final String STACK_BROWSING_VIEWS_VERTICALLY= PreferenceConstants.BROWSING_STACK_VERTICALLY;
	private static final String PREF_FOLD_PACKAGES_IN_PACKAGE_EXPLORER= PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER;


	/**
	 * @deprecated Inline to remove reference to preference page
	 */
	public static boolean showMethodReturnType() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE);
	}

	/**
	 * @deprecated Inline to remove reference to preference page
	 */
	public static boolean showOverrideIndicators() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_OVERRIDE_INDICATOR);
	}	

	/**
	 * @deprecated Inline to remove reference to preference page
	 * /
	public static boolean arePackagesFoldedInHierarchicalLayout(){
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER);	
	}

	/**
	 * @deprecated Inline to remove reference to preference page
	 */
	public static String getPkgNamePatternForPackagesView() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		if (!store.getBoolean(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES))
			return ""; //$NON-NLS-1$
		return store.getString(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW);
	}
	
	private SelectionButtonDialogField fShowMethodReturnType;
	private SelectionButtonDialogField fCompressPackageNames;
	private SelectionButtonDialogField fStackBrowsingViewsVertically;
	private SelectionButtonDialogField fShowMembersInPackageView;
	private StringDialogField fPackageNamePattern;
	private SelectionButtonDialogField fFoldPackagesInPackageExplorer;
	
	

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
		
		fShowMembersInPackageView= new SelectionButtonDialogField(SWT.CHECK);
		fShowMembersInPackageView.setDialogFieldListener(listener);
		fShowMembersInPackageView.setLabelText(JavaUIMessages.getString("AppearancePreferencePage.showMembersInPackagesView")); //$NON-NLS-1$

		fStackBrowsingViewsVertically= new SelectionButtonDialogField(SWT.CHECK);
		fStackBrowsingViewsVertically.setDialogFieldListener(listener);
		fStackBrowsingViewsVertically.setLabelText(JavaUIMessages.getString("AppearancePreferencePage.stackViewsVerticallyInTheJavaBrowsingPerspective")); //$NON-NLS-1$

		fFoldPackagesInPackageExplorer= new SelectionButtonDialogField(SWT.CHECK);
		fFoldPackagesInPackageExplorer.setDialogFieldListener(listener);
		fFoldPackagesInPackageExplorer.setLabelText(JavaUIMessages.getString("AppearancePreferencePage.foldEmptyPackages")); //$NON-NLS-1$

		fCompressPackageNames= new SelectionButtonDialogField(SWT.CHECK);
		fCompressPackageNames.setDialogFieldListener(listener);
		fCompressPackageNames.setLabelText(JavaUIMessages.getString("AppearancePreferencePage.pkgNamePatternEnable.label")); //$NON-NLS-1$

		fPackageNamePattern= new StringDialogField();
		fPackageNamePattern.setDialogFieldListener(listener);
		fPackageNamePattern.setLabelText(JavaUIMessages.getString("AppearancePreferencePage.pkgNamePattern.label")); //$NON-NLS-1$
	}	

	private void initFields() {
		IPreferenceStore prefs= getPreferenceStore();
		fShowMethodReturnType.setSelection(prefs.getBoolean(PREF_METHOD_RETURNTYPE));
		fShowMembersInPackageView.setSelection(prefs.getBoolean(SHOW_CU_CHILDREN));
		fStackBrowsingViewsVertically.setSelection(prefs.getBoolean(STACK_BROWSING_VIEWS_VERTICALLY));
		fPackageNamePattern.setText(prefs.getString(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW));
		fCompressPackageNames.setSelection(prefs.getBoolean(PREF_COMPRESS_PACKAGE_NAMES));
		fPackageNamePattern.setEnabled(fCompressPackageNames.isSelected());
		fFoldPackagesInPackageExplorer.setSelection(prefs.getBoolean(PREF_FOLD_PACKAGES_IN_PACKAGE_EXPLORER));
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
		fShowMembersInPackageView.doFillIntoGrid(composite, nColumns);				
		fFoldPackagesInPackageExplorer.doFillIntoGrid(composite, nColumns);

		new Separator().doFillIntoGrid(composite, nColumns);
		
		fCompressPackageNames.doFillIntoGrid(composite, nColumns);
		fPackageNamePattern.doFillIntoGrid(composite, 2);
		LayoutUtil.setHorizontalGrabbing(fPackageNamePattern.getTextControl(null));
		LayoutUtil.setWidthHint(fPackageNamePattern.getLabelControl(null), convertWidthInCharsToPixels(65));
		
		new Separator().doFillIntoGrid(composite, nColumns);
		fStackBrowsingViewsVertically.doFillIntoGrid(composite, nColumns);
		
		
		DialogField field= new DialogField();
		field.setLabelText(JavaUIMessages.getString("AppearancePreferencePage.preferenceOnlyEffectiveForNewPerspectives")); //$NON-NLS-1$
		field.doFillIntoGrid(composite, 2);
		
		initFields();
		
		return composite;
	}
	
	private void doDialogFieldChanged(DialogField field) {
		if (field == fCompressPackageNames)
			fPackageNamePattern.setEnabled(fCompressPackageNames.isSelected());
	
		updateStatus(getValidationStatus());
	}
	
	private IStatus getValidationStatus(){
		if (fCompressPackageNames.isSelected() && fPackageNamePattern.getText().equals("")) //$NON-NLS-1$
			return new StatusInfo(IStatus.ERROR, JavaUIMessages.getString("AppearancePreferencePage.packageNameCompressionPattern.error.isEmpty")); //$NON-NLS-1$
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
		IPreferenceStore prefs= getPreferenceStore();
		prefs.setValue(PREF_METHOD_RETURNTYPE, fShowMethodReturnType.isSelected());
		prefs.setValue(SHOW_CU_CHILDREN, fShowMembersInPackageView.isSelected());
		prefs.setValue(STACK_BROWSING_VIEWS_VERTICALLY, fStackBrowsingViewsVertically.isSelected());
		prefs.setValue(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW, fPackageNamePattern.getText());
		prefs.setValue(PREF_COMPRESS_PACKAGE_NAMES, fCompressPackageNames.isSelected());
		prefs.setValue(PREF_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, fFoldPackagesInPackageExplorer.isSelected());
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}	
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore prefs= getPreferenceStore();
		fShowMethodReturnType.setSelection(prefs.getDefaultBoolean(PREF_METHOD_RETURNTYPE));
		fShowMembersInPackageView.setSelection(prefs.getDefaultBoolean(SHOW_CU_CHILDREN));
		fStackBrowsingViewsVertically.setSelection(prefs.getDefaultBoolean(STACK_BROWSING_VIEWS_VERTICALLY));
		fPackageNamePattern.setText(prefs.getDefaultString(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW));
		fCompressPackageNames.setSelection(prefs.getDefaultBoolean(PREF_COMPRESS_PACKAGE_NAMES));
		fFoldPackagesInPackageExplorer.setSelection(prefs.getDefaultBoolean(PREF_FOLD_PACKAGES_IN_PACKAGE_EXPLORER));
		super.performDefaults();
	}
}

