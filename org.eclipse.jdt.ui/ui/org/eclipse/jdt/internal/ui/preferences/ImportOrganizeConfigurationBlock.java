/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.JavaConventions;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/*
 * The page for setting the organize import settings
 */
public class ImportOrganizeConfigurationBlock extends OptionsConfigurationBlock {

	private static final Key PREF_IMPORTORDER= getJDTUIKey(PreferenceConstants.ORGIMPORTS_IMPORTORDER);
	private static final Key PREF_ONDEMANDTHRESHOLD= getJDTUIKey(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD);
	private static final Key PREF_IGNORELOWERCASE= getJDTUIKey(PreferenceConstants.ORGIMPORTS_IGNORELOWERCASE);
	
	private static final String DIALOGSETTING_LASTLOADPATH= JavaUI.ID_PLUGIN + ".importorder.loadpath"; //$NON-NLS-1$
	private static final String DIALOGSETTING_LASTSAVEPATH= JavaUI.ID_PLUGIN + ".importorder.savepath"; //$NON-NLS-1$

	private static Key[] getAllKeys() {
		return new Key[] {
			PREF_IMPORTORDER, PREF_ONDEMANDTHRESHOLD, PREF_IGNORELOWERCASE
		};	
	}	
	
	private static class ImportOrganizeLabelProvider extends LabelProvider {
		
		private static final Image PCK_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_PACKAGE);

		public Image getImage(Object element) {
			return PCK_ICON;
		}
	}
	
	private class ImportOrganizeAdapter implements IListAdapter, IDialogFieldListener {

		private boolean canEdit(ListDialogField field) {
			return field.getSelectedElements().size() == 1;
		}

        public void customButtonPressed(ListDialogField field, int index) {
        	doButtonPressed(index);
        }

        public void selectionChanged(ListDialogField field) {
			fOrderListField.enableButton(IDX_EDIT, canEdit(field));
        }

        public void dialogFieldChanged(DialogField field) {
        	updateModel(field);
        }
        
        public void doubleClicked(ListDialogField field) {
        	if (canEdit(field)) {
				doButtonPressed(IDX_EDIT);
        	}
        }
	}
	
	private static final int IDX_ADD= 0;
	private static final int IDX_EDIT= 1;
	private static final int IDX_REMOVE= 2;
	private static final int IDX_UP= 4;
	private static final int IDX_DOWN= 5;
	private static final int IDX_LOAD= 7;
	private static final int IDX_SAVE= 8;

	private ListDialogField fOrderListField;
	private StringDialogField fThresholdField;
	private SelectionButtonDialogField fIgnoreLowerCaseTypesField;
	
	private PixelConverter fPixelConverter;
	
	public ImportOrganizeConfigurationBlock(IStatusChangeListener context, IProject project) {
		super(context, project, getAllKeys());
	
		String[] buttonLabels= new String[] { 
			/* 0 */  PreferencesMessages.getString("ImportOrganizeConfigurationBlock.order.add.button"), //$NON-NLS-1$
			/* 1 */  PreferencesMessages.getString("ImportOrganizeConfigurationBlock.order.edit.button"), //$NON-NLS-1$
			/* 2 */  PreferencesMessages.getString("ImportOrganizeConfigurationBlock.order.remove.button"), //$NON-NLS-1$
			/* 3 */  null,
			/* 4 */  PreferencesMessages.getString("ImportOrganizeConfigurationBlock.order.up.button"), //$NON-NLS-1$
			/* 5 */  PreferencesMessages.getString("ImportOrganizeConfigurationBlock.order.down.button"), //$NON-NLS-1$
			/* 6 */  null,
			/* 7 */  PreferencesMessages.getString("ImportOrganizeConfigurationBlock.order.load.button"), //$NON-NLS-1$					
			/* 8 */  PreferencesMessages.getString("ImportOrganizeConfigurationBlock.order.save.button") //$NON-NLS-1$			
		};
				
		ImportOrganizeAdapter adapter= new ImportOrganizeAdapter();
		
		fOrderListField= new ListDialogField(adapter, buttonLabels, new ImportOrganizeLabelProvider());
		fOrderListField.setDialogFieldListener(adapter);
		fOrderListField.setLabelText(PreferencesMessages.getString("ImportOrganizeConfigurationBlock.order.label")); //$NON-NLS-1$
		fOrderListField.setUpButtonIndex(IDX_UP);
		fOrderListField.setDownButtonIndex(IDX_DOWN);
		fOrderListField.setRemoveButtonIndex(IDX_REMOVE);
		
		fOrderListField.enableButton(IDX_EDIT, false);
		
		fThresholdField= new StringDialogField();
		fThresholdField.setDialogFieldListener(adapter);
		fThresholdField.setLabelText(PreferencesMessages.getString("ImportOrganizeConfigurationBlock.threshold.label")); //$NON-NLS-1$
	
		fIgnoreLowerCaseTypesField= new SelectionButtonDialogField(SWT.CHECK);
		fIgnoreLowerCaseTypesField.setDialogFieldListener(adapter);
		fIgnoreLowerCaseTypesField.setLabelText(PreferencesMessages.getString("ImportOrganizeConfigurationBlock.ignoreLowerCase.label")); //$NON-NLS-1$
	
		updateControls();
	}
	

	
	protected Control createContents(Composite parent) {
		setShell(parent.getShell());
		
		fPixelConverter= new PixelConverter(parent);
	
		Composite composite= new Composite(parent, SWT.NONE);
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		
		composite.setLayout(layout);
		
		fOrderListField.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalSpan(fOrderListField.getLabelControl(null), 2);
		LayoutUtil.setWidthHint(fOrderListField.getLabelControl(null), fPixelConverter.convertWidthInCharsToPixels(40));
		LayoutUtil.setHorizontalGrabbing(fOrderListField.getListControl(null));
		
		fThresholdField.doFillIntoGrid(composite, 2);
		((GridData) fThresholdField.getTextControl(null).getLayoutData()).grabExcessHorizontalSpace= false;
		
		fIgnoreLowerCaseTypesField.doFillIntoGrid(composite, 2);
		
		Dialog.applyDialogFont(composite);
		return composite;
	}
		
	private void doThresholdChanged() {
		StatusInfo status= new StatusInfo();
		String thresholdString= fThresholdField.getText();
		try {
			int threshold= Integer.parseInt(thresholdString);
			if (threshold < 0) {
				status.setError(PreferencesMessages.getString("ImportOrganizeConfigurationBlock.error.invalidthreshold")); //$NON-NLS-1$
			}
		} catch (NumberFormatException e) {
			status.setError(PreferencesMessages.getString("ImportOrganizeConfigurationBlock.error.invalidthreshold")); //$NON-NLS-1$
		}
		updateStatus(status);
	}
	
	private void doButtonPressed(int index) {
		if (index == IDX_ADD) { // add new
			List existing= fOrderListField.getElements();
			ImportOrganizeInputDialog dialog= new ImportOrganizeInputDialog(getShell(), existing);
			if (dialog.open() == Window.OK) {
				List selectedElements= fOrderListField.getSelectedElements();
				if (selectedElements.size() == 1) {
					int insertionIndex= fOrderListField.getIndexOfElement(selectedElements.get(0)) + 1;
					fOrderListField.addElement(dialog.getResult(), insertionIndex);
				} else {
					fOrderListField.addElement(dialog.getResult());
				}
			}
		} else if (index == IDX_EDIT) { // edit
			List selected= fOrderListField.getSelectedElements();
			if (selected.isEmpty()) {
				return;
			}
			String editedEntry= (String) selected.get(0);
			
			List existing= fOrderListField.getElements();
			existing.remove(editedEntry);
			
			ImportOrganizeInputDialog dialog= new ImportOrganizeInputDialog(getShell(), existing);
			dialog.setInitialString(editedEntry);
			if (dialog.open() == Window.OK) {
				fOrderListField.replaceElement(editedEntry, dialog.getResult());
			}
		} else if (index == IDX_LOAD) { // load
			List order= loadImportOrder();
			if (order != null) {
				fOrderListField.setElements(order);
			}
		} else if (index == IDX_SAVE) { // save
			saveImportOrder(fOrderListField.getElements());
		}		
	}
	
	
	/*
	 * The import order file is a property file. The keys are
	 * "0", "1" ... last entry. The values must be valid package names.
	 */
	private List loadFromProperties(Properties properties) {
		ArrayList res= new ArrayList();
		int nEntries= properties.size();
		for (int i= 0 ; i < nEntries; i++) {
			String curr= properties.getProperty(String.valueOf(i));
			if (curr != null) {
				if (!JavaConventions.validatePackageName(curr).matches(IStatus.ERROR)) {
					res.add(curr);
				} else {
					return null;
				}
			} else {
				return res;
			}
		}
		return res;
	}
	
	private List loadImportOrder() {
		IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		FileDialog dialog= new FileDialog(getShell(), SWT.OPEN);
		dialog.setText(PreferencesMessages.getString("ImportOrganizeConfigurationBlock.loadDialog.title")); //$NON-NLS-1$)
		dialog.setFilterExtensions(new String[] {"*.importorder", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		String lastPath= dialogSettings.get(DIALOGSETTING_LASTLOADPATH);
		if (lastPath != null) {
			dialog.setFilterPath(lastPath);
		}
		String fileName= dialog.open();
		if (fileName != null) {
			dialogSettings.put(DIALOGSETTING_LASTLOADPATH, dialog.getFilterPath());
					
			Properties properties= new Properties();
			FileInputStream fis= null;
			try {
				fis= new FileInputStream(fileName);
				properties.load(fis);
				List res= loadFromProperties(properties);
				if (res != null) {
					return res;
				}
			} catch (IOException e) {
				JavaPlugin.log(e);
			} finally {
				if (fis != null) {
					try { fis.close(); } catch (IOException e) {}
				}
			}
			String title= PreferencesMessages.getString("ImportOrganizeConfigurationBlock.loadDialog.error.title"); //$NON-NLS-1$
			String message= PreferencesMessages.getString("ImportOrganizeConfigurationBlock.loadDialog.error.message"); //$NON-NLS-1$
			MessageDialog.openError(getShell(), title, message);
		}
		return null;
	}
	
	private void saveImportOrder(List elements) {
		IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		FileDialog dialog= new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(PreferencesMessages.getString("ImportOrganizeConfigurationBlock.saveDialog.title")); //$NON-NLS-1$)
		dialog.setFilterExtensions(new String[] {"*.importorder", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFileName("example.importorder"); //$NON-NLS-1$
		String lastPath= dialogSettings.get(DIALOGSETTING_LASTSAVEPATH);
		if (lastPath != null) {
			dialog.setFilterPath(lastPath);
		}
		String fileName= dialog.open();
		if (fileName != null) {
			dialogSettings.put(DIALOGSETTING_LASTSAVEPATH, dialog.getFilterPath());
			
			Properties properties= new Properties();
			for (int i= 0; i < elements.size(); i++) {
				properties.setProperty(String.valueOf(i), (String) elements.get(i));
			}
			FileOutputStream fos= null;
			try {
				fos= new FileOutputStream(fileName);
				properties.store(fos, "Organize Import Order"); //$NON-NLS-1$
			} catch (IOException e) {
				JavaPlugin.log(e);
				String title= PreferencesMessages.getString("ImportOrganizeConfigurationBlock.saveDialog.error.title"); //$NON-NLS-1$
				String message= PreferencesMessages.getString("ImportOrganizeConfigurationBlock.saveDialog.error.message"); //$NON-NLS-1$
				MessageDialog.openError(getShell(), title, message);				
			} finally {
				if (fos != null) {
					try { fos.close(); } catch (IOException e) {}
				}
			}
		}
	}

	private void updateStatus(IStatus status) {
		fContext.statusChanged(status);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#validateSettings(java.lang.String, java.lang.String)
	 */
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		// no validation
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#updateControls()
	 */
	protected void updateControls() {
		String[] importOrder= getImportOrderPreference();
		int threshold= getImportNumberThreshold();
		boolean ignoreLowerCase= Boolean.valueOf(getValue(PREF_IGNORELOWERCASE)).booleanValue();
		
		fOrderListField.removeAllElements();
		for (int i= 0; i < importOrder.length; i++) {
			fOrderListField.addElement(importOrder[i]);
		}
		fThresholdField.setText(String.valueOf(threshold));
		fIgnoreLowerCaseTypesField.setSelection(ignoreLowerCase);
	}	
	
	
	protected final void updateModel(DialogField field) {
		if (field == fOrderListField) {
	  		setValue(PREF_IMPORTORDER, packOrderList(fOrderListField.getElements()));
		} else if (field == fThresholdField) {
	  		setValue(PREF_ONDEMANDTHRESHOLD, fThresholdField.getText());
	  		
	  		doThresholdChanged();
	  		
		} else if (field == fIgnoreLowerCaseTypesField) {
	  		setValue(PREF_IGNORELOWERCASE, fIgnoreLowerCaseTypesField.isSelected());
		}
	}
	
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getFullBuildDialogStrings(boolean)
	 */
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null; // no build required
	}

	private static String[] unpackOrderList(String str) {
		StringTokenizer tok= new StringTokenizer(str, ";"); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			res[i]= tok.nextToken();
		}
		return res;
	}
	
	private static String packOrderList(List orderList) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < orderList.size(); i++) {
			buf.append((String) orderList.get(i));
			buf.append(';');
		}
		return buf.toString();
	}
	
	private String[] getImportOrderPreference() {
		String str= getValue(PREF_IMPORTORDER);
		if (str != null) {
			return unpackOrderList(str);
		}
		return new String[0];
	}
	
	private int getImportNumberThreshold() {
		String thresholdStr= getValue(PREF_ONDEMANDTHRESHOLD);
		try {
			int threshold= Integer.parseInt(thresholdStr);
			if (threshold < 0) {
				threshold= Integer.MAX_VALUE;
			}
			return threshold;
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}


}


