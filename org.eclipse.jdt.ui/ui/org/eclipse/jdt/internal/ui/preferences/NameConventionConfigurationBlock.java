/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/*
 * The page to configure name conventions
 */
public class NameConventionConfigurationBlock extends OptionsConfigurationBlock {

	private final static int FIELD= 1;
	private final static int STATIC= 2;
	private final static int ARGUMENT= 3;
	private final static int LOCAL= 4;

	// Preference store keys, see JavaCore.getOptions
	private static final String PREF_FIELD_PREFIXES= JavaCore.CODEASSIST_FIELD_PREFIXES; 
	private static final String PREF_FIELD_SUFFIXES= JavaCore.CODEASSIST_FIELD_SUFFIXES; 
	private static final String PREF_STATIC_FIELD_PREFIXES= JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES; 
	private static final String PREF_STATIC_FIELD_SUFFIXES= JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES; 
	private static final String PREF_ARGUMENT_PREFIXES= JavaCore.CODEASSIST_ARGUMENT_PREFIXES; 
	private static final String PREF_ARGUMENT_SUFFIXES= JavaCore.CODEASSIST_ARGUMENT_SUFFIXES; 
	private static final String PREF_LOCAL_PREFIXES= JavaCore.CODEASSIST_LOCAL_PREFIXES; 
	private static final String PREF_LOCAL_SUFFIXES= JavaCore.CODEASSIST_LOCAL_SUFFIXES; 

	private static class NameConventionEntry {
		public int kind;
		public String prefix;
		public String suffix;
		public String prefixkey;
		public String suffixkey;		
	}

	private class NameConventionInputDialog extends StatusDialog implements IDialogFieldListener {

		private StringDialogField fPrefixField;
		private StringDialogField fSuffixField;
		private NameConventionEntry fEntry;
		private DialogField fMessageField;
			
		public NameConventionInputDialog(Shell parent, String title, String message, NameConventionEntry entry) {
			super(parent);
			fEntry= entry;
			
			setTitle(title);

			fMessageField= new DialogField();
			fMessageField.setLabelText(message);
	
			fPrefixField= new StringDialogField();
			fPrefixField.setLabelText(PreferencesMessages.getString("NameConventionConfigurationBlock.dialog.prefix")); //$NON-NLS-1$
			fPrefixField.setDialogFieldListener(this);
			
			fSuffixField= new StringDialogField();
			fSuffixField.setLabelText(PreferencesMessages.getString("NameConventionConfigurationBlock.dialog.suffix")); //$NON-NLS-1$
			fSuffixField.setDialogFieldListener(this);			

			fPrefixField.setText(entry.prefix);
			fSuffixField.setText(entry.suffix);
		}
		
		public NameConventionEntry getResult() {
			NameConventionEntry res= new NameConventionEntry();
			res.prefix= fPrefixField.getText();
			res.suffix= fSuffixField.getText();			
			res.prefixkey= fEntry.prefixkey;
			res.suffixkey= fEntry.suffixkey;
			res.kind= 	fEntry.kind;
			return res;
		}
				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			
			Composite inner= new Composite(composite, SWT.NONE);
			GridLayout layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			layout.numColumns= 2;
			inner.setLayout(layout);
			
			fMessageField.doFillIntoGrid(inner, 2);
			fPrefixField.doFillIntoGrid(inner, 2);
			fSuffixField.doFillIntoGrid(inner, 2);
			
			LayoutUtil.setHorizontalGrabbing(fPrefixField.getTextControl(null));
			LayoutUtil.setWidthHint(fPrefixField.getTextControl(null), convertWidthInCharsToPixels(45));
			LayoutUtil.setWidthHint(fSuffixField.getTextControl(null), convertWidthInCharsToPixels(45));
			
			fPrefixField.postSetFocusOnDialogField(parent.getDisplay());
			
			applyDialogFont(composite);		
			return composite;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			// validate
			IStatus prefixStatus= validateIdentifiers(getTokens(fPrefixField.getText(), ","), true); //$NON-NLS-1$
			IStatus suffixStatus= validateIdentifiers(getTokens(fSuffixField.getText(), ","), false); //$NON-NLS-1$
			
			updateStatus(StatusUtil.getMoreSevere(suffixStatus, prefixStatus));
		}		
		
		private IStatus validateIdentifiers(String[] values, boolean prefix) {
			for (int i= 0; i < values.length; i++) {
				String val= values[i];
				if (val.length() == 0) {
					if (prefix) {
						return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("NameConventionConfigurationBlock.error.emptyprefix")); //$NON-NLS-1$
					} else {
						return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("NameConventionConfigurationBlock.error.emptysuffix")); //$NON-NLS-1$
					}							
				}
				String name= prefix ? val + "x" : "x" + val; //$NON-NLS-2$ //$NON-NLS-1$
				IStatus status= JavaConventions.validateFieldName(name);
				if (status.matches(IStatus.ERROR)) {
					if (prefix) {
						return new StatusInfo(IStatus.ERROR, PreferencesMessages.getFormattedString("NameConventionConfigurationBlock.error.invalidprefix", val)); //$NON-NLS-1$
					} else {
						return new StatusInfo(IStatus.ERROR, PreferencesMessages.getFormattedString("NameConventionConfigurationBlock.error.invalidsuffix", val)); //$NON-NLS-1$
					}
				}
			}
			return new StatusInfo();
		}		
	
		/*
		 * @see org.eclipse.jface.window.Window#configureShell(Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			//WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.IMPORT_ORGANIZE_INPUT_DIALOG);
		}
	}	
	
	private static class NameConventionLabelProvider extends LabelProvider implements ITableLabelProvider {
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return getColumnImage(element, 0);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return getColumnText(element, 0);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 0) {
				return null;
			}
			
			NameConventionEntry entry= (NameConventionEntry) element;
			ImageDescriptorRegistry registry= JavaPlugin.getImageDescriptorRegistry();
			switch (entry.kind) {
				case FIELD:
					return registry.get(JavaPluginImages.DESC_FIELD_PUBLIC);
				case STATIC:
					return registry.get(new JavaElementImageDescriptor(JavaPluginImages.DESC_FIELD_PUBLIC, JavaElementImageDescriptor.STATIC, JavaElementImageProvider.SMALL_SIZE));
				case ARGUMENT:
					return registry.get(JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE);
				default:
					return registry.get(JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE);
			}
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			NameConventionEntry entry= (NameConventionEntry) element;
			if (columnIndex == 0) {
				switch (entry.kind) {
					case FIELD:
						return PreferencesMessages.getString("NameConventionConfigurationBlock.field.label"); //$NON-NLS-1$
					case STATIC:
						return PreferencesMessages.getString("NameConventionConfigurationBlock.static.label"); //$NON-NLS-1$
					case ARGUMENT:
						return PreferencesMessages.getString("NameConventionConfigurationBlock.arg.label"); //$NON-NLS-1$
					default:
						return PreferencesMessages.getString("NameConventionConfigurationBlock.local.label"); //$NON-NLS-1$
				}
			} else if (columnIndex == 1) {
				return entry.prefix;
			} else {
				return entry.suffix;
			}
		}
	}
	
	private class NameConventionAdapter implements IListAdapter, IDialogFieldListener {

		private boolean canEdit(ListDialogField field) {
			return field.getSelectedElements().size() == 1;
		}

		public void customButtonPressed(ListDialogField field, int index) {
			doEditButtonPressed();
		}

		public void selectionChanged(ListDialogField field) {
			field.enableButton(0, canEdit(field));
		}
			
		public void doubleClicked(ListDialogField field) {
			if (canEdit(field)) {
				doEditButtonPressed();
			}
		}

		public void dialogFieldChanged(DialogField field) {
			validateSettings(null, null);
		}	
	}
		
	private ListDialogField fNameConventionList;
	private SelectionButtonDialogField fUseKeywordThisBox;
	private SelectionButtonDialogField fUseIsForBooleanGettersBox;
	private static final String PREF_KEYWORD_THIS= PreferenceConstants.CODEGEN_KEYWORD_THIS;
	private static final String PREF_IS_FOR_GETTERS= PreferenceConstants.CODEGEN_IS_FOR_GETTERS;
	private static final String PREF_EXCEPTION_NAME= PreferenceConstants.CODEGEN_EXCEPTION_VAR_NAME;
	
	private StringDialogField fExceptionName;

	
	public NameConventionConfigurationBlock(IStatusChangeListener context, IJavaProject project) {
		super(context, project);
		
		NameConventionAdapter adapter=  new NameConventionAdapter();
		String[] buttons= new String[] {
			/* 0 */ PreferencesMessages.getString("NameConventionConfigurationBlock.list.edit.button") //$NON-NLS-1$
		};
		fNameConventionList= new ListDialogField(adapter, buttons, new NameConventionLabelProvider());
		fNameConventionList.setDialogFieldListener(adapter);
		fNameConventionList.setLabelText(PreferencesMessages.getString("NameConventionConfigurationBlock.list.label")); //$NON-NLS-1$
		
		String[] columnsHeaders= new String[] {
			PreferencesMessages.getString("NameConventionConfigurationBlock.list.name.column"), //$NON-NLS-1$
			PreferencesMessages.getString("NameConventionConfigurationBlock.list.prefix.column"), //$NON-NLS-1$
			PreferencesMessages.getString("NameConventionConfigurationBlock.list.suffix.column"),			 //$NON-NLS-1$
		};
		ColumnLayoutData[] data= new ColumnLayoutData[] {
			new ColumnWeightData(3),
			new ColumnWeightData(2),
			new ColumnWeightData(2)
		};
		
		fNameConventionList.setTableColumns(new ListDialogField.ColumnsDescription(data, columnsHeaders, true));
		unpackEntries();
		if (fNameConventionList.getSize() > 0) {
			fNameConventionList.selectFirstElement();
		} else {
			fNameConventionList.enableButton(0, false);
		}

		IPreferenceStore preferenceStore= PreferenceConstants.getPreferenceStore();
		
		fExceptionName= new StringDialogField();
		fExceptionName.setLabelText(PreferencesMessages.getString("NameConventionConfigurationBlock.exceptionname.label")); //$NON-NLS-1$
		fExceptionName.setText(preferenceStore.getString(PREF_EXCEPTION_NAME));
		
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=38879
		fUseKeywordThisBox= new SelectionButtonDialogField(SWT.CHECK | SWT.WRAP);		
		fUseKeywordThisBox.setLabelText(PreferencesMessages.getString("NameConventionConfigurationBlock.keywordthis.label")); //$NON-NLS-1$
		fUseKeywordThisBox.setSelection(preferenceStore.getBoolean(PREF_KEYWORD_THIS));
		
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=39044
		fUseIsForBooleanGettersBox= new SelectionButtonDialogField(SWT.CHECK | SWT.WRAP);		
		fUseIsForBooleanGettersBox.setLabelText(PreferencesMessages.getString("NameConventionConfigurationBlock.isforbooleangetters.label")); //$NON-NLS-1$
		fUseIsForBooleanGettersBox.setSelection(preferenceStore.getBoolean(PREF_IS_FOR_GETTERS));		
	}
	
	protected String[] getAllKeys() {
		return new String[] {
			PREF_FIELD_PREFIXES, PREF_FIELD_SUFFIXES, PREF_STATIC_FIELD_PREFIXES, PREF_STATIC_FIELD_SUFFIXES,
			PREF_ARGUMENT_PREFIXES, PREF_ARGUMENT_SUFFIXES, PREF_LOCAL_PREFIXES, PREF_LOCAL_SUFFIXES
		};	
	}	

	protected Control createContents(Composite parent) {
		setShell(parent.getShell());
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(layout);

		fNameConventionList.doFillIntoGrid(composite, 4);
		LayoutUtil.setHorizontalSpan(fNameConventionList.getLabelControl(null), 2);
		Table table= fNameConventionList.getTableViewer().getTable();
		GridData data= (GridData)fNameConventionList.getListControl(null).getLayoutData();
		data.heightHint= SWTUtil.getTableHeightHint(table, 5);
		data.grabExcessHorizontalSpace= true;
		data.verticalAlignment= GridData.BEGINNING;
		data.grabExcessVerticalSpace= false;
		
		data= (GridData)fNameConventionList.getButtonBox(null).getLayoutData();
		data.grabExcessVerticalSpace= false;
		data.verticalAlignment= GridData.BEGINNING;
		
		fUseKeywordThisBox.doFillIntoGrid(composite, 3);
		fUseIsForBooleanGettersBox.doFillIntoGrid(composite, 3);

		fExceptionName.doFillIntoGrid(composite, 2);
		DialogField.createEmptySpace(composite);
		
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#validateSettings(java.lang.String, java.lang.String)
	 */
	protected void validateSettings(String changedKey, String newValue) {
		// no validation
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#updateControls()
	 */
	protected void updateControls() {
		unpackEntries();
	}	
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getFullBuildDialogStrings(boolean)
	 */
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null; // no build required
	}
	
	private void createEntry(List list, String prefixKey, String suffixKey, int kind) {
		NameConventionEntry entry= new NameConventionEntry();
		entry.kind= kind;
		entry.suffixkey= suffixKey;
		entry.prefixkey= prefixKey;	
		entry.suffix= (String) fWorkingValues.get(suffixKey);
		entry.prefix= (String) fWorkingValues.get(prefixKey);
		list.add(entry);
	}
	
	private void unpackEntries() {
		ArrayList list= new ArrayList(4);
		createEntry(list, PREF_FIELD_PREFIXES, PREF_FIELD_SUFFIXES,  FIELD);
		createEntry(list, PREF_STATIC_FIELD_PREFIXES, PREF_STATIC_FIELD_SUFFIXES, STATIC);
		createEntry(list, PREF_ARGUMENT_PREFIXES, PREF_ARGUMENT_SUFFIXES, ARGUMENT);
		createEntry(list, PREF_LOCAL_PREFIXES, PREF_LOCAL_SUFFIXES, LOCAL);
		fNameConventionList.setElements(list);
	}
	
	private void packEntries() {
		for (int i= 0; i < fNameConventionList.getSize(); i++) {
			NameConventionEntry entry= (NameConventionEntry) fNameConventionList.getElement(i);
			fWorkingValues.put(entry.suffixkey, entry.suffix);
			fWorkingValues.put(entry.prefixkey, entry.prefix);
		}
	}
		
	private void doEditButtonPressed() {
		NameConventionEntry entry= (NameConventionEntry) fNameConventionList.getSelectedElements().get(0);

		String title;
		String message;
		switch (entry.kind) {
			case FIELD:
				title= PreferencesMessages.getString("NameConventionConfigurationBlock.field.dialog.title"); //$NON-NLS-1$
				message= PreferencesMessages.getString("NameConventionConfigurationBlock.field.dialog.message"); //$NON-NLS-1$
				break;
			case STATIC:
				title= PreferencesMessages.getString("NameConventionConfigurationBlock.static.dialog.title"); //$NON-NLS-1$
				message= PreferencesMessages.getString("NameConventionConfigurationBlock.static.dialog.message"); //$NON-NLS-1$
				break;
			case ARGUMENT:
				title= PreferencesMessages.getString("NameConventionConfigurationBlock.arg.dialog.title"); //$NON-NLS-1$
				message= PreferencesMessages.getString("NameConventionConfigurationBlock.arg.dialog.message"); //$NON-NLS-1$
				break;
			default:
				title= PreferencesMessages.getString("NameConventionConfigurationBlock.local.dialog.title"); //$NON-NLS-1$
				message= PreferencesMessages.getString("NameConventionConfigurationBlock.local.dialog.message"); //$NON-NLS-1$
		}
		
		NameConventionInputDialog dialog= new NameConventionInputDialog(getShell(), title, message, entry);
		if (dialog.open() == Window.OK) {
			fNameConventionList.replaceElement(entry, dialog.getResult());
		}
	}		

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#performDefaults()
	 */
	public void performDefaults() {
		super.performDefaults();
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fExceptionName.setText(prefs.getDefaultString(PREF_EXCEPTION_NAME));
		fUseKeywordThisBox.setSelection(prefs.getDefaultBoolean(PREF_KEYWORD_THIS));
		fUseIsForBooleanGettersBox.setSelection(prefs.getDefaultBoolean(PREF_IS_FOR_GETTERS));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#performOk(boolean)
	 */
	public boolean performOk(boolean enabled) {
		IPreferenceStore prefs= PreferenceConstants.getPreferenceStore();
		prefs.setValue(PREF_EXCEPTION_NAME, fExceptionName.getText());
		prefs.setValue(PREF_KEYWORD_THIS, fUseKeywordThisBox.isSelected());
		prefs.setValue(PREF_IS_FOR_GETTERS, fUseIsForBooleanGettersBox.isSelected());
		JavaPlugin.getDefault().savePluginPreferences();
				
		packEntries();
		return super.performOk(enabled);
	}

}


