/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.IPostSaveListener;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.ISaveParticipantPreferenceConfiguration;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.SaveParticipantDescriptor;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.SaveParticipantRegistry;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
 * Configures Java Editor save participants.
 * 
 * @since 3.3
 */
class SaveParticipantConfigurationBlock implements IPreferenceConfigurationBlock {

	private final OverlayPreferenceStore fStore;
	private SaveParticipantDescriptor[] fRegisteredDescriptors;
	private SelectionButtonDialogField fEnableField;
	private final PreferencePage fPreferencePage;

	public SaveParticipantConfigurationBlock(OverlayPreferenceStore store, PreferencePage preferencePage) {
		Assert.isNotNull(store);
		fStore= store;
		fRegisteredDescriptors= JavaPlugin.getDefault().getSaveParticipantRegistry().getSaveParticipantDescriptors();

		fPreferencePage= preferencePage;
		fStore.addKeys(createOverlayStoreKeys());
    }
	
	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		OverlayPreferenceStore.OverlayKey[] result= new OverlayPreferenceStore.OverlayKey[fRegisteredDescriptors.length];
		for (int i= 0; i < fRegisteredDescriptors.length; i++) {
	        result[i]= new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, getPreferenceKey(fRegisteredDescriptors[i])); 
        }
		return result;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#createControl(org.eclipse.swt.widgets.Composite)
	 * @since 3.3
	 */
	public Control createControl(Composite parent) {
		if (fRegisteredDescriptors.length == 1) {
			IPostSaveListener listener= fRegisteredDescriptors[0].getPostSaveListener();
			boolean isActive= JavaPlugin.getDefault().getSaveParticipantRegistry().getEnabledPostSaveListeners().length == 1;
			
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			composite.setLayout(new GridLayout(2, false));
			
			fEnableField= new SelectionButtonDialogField(SWT.CHECK);
			fEnableField.setLabelText(Messages.format(PreferencesMessages.JavaEditorPreferencePage_saveParticipant_cleanup, listener.getName()));
			if (isActive) {
				fEnableField.setSelection(true);
			}
			fEnableField.doFillIntoGrid(composite, 2);
			
			ISaveParticipantPreferenceConfiguration configurationBlock= fRegisteredDescriptors[0].getPreferenceConfiguration();
			final Control configControl;
			if (configurationBlock != null) {
				configControl= configurationBlock.createControl(composite, fPreferencePage);
				if (!isActive)
					setEnabled(configControl, false);
			} else {
				configControl= null;
			}
			
			fEnableField.setDialogFieldListener(new IDialogFieldListener() {
				public void dialogFieldChanged(DialogField field) {
					if (fEnableField.isSelected()) {
						fStore.setValue(getPreferenceKey(fRegisteredDescriptors[0]), true);
						if (configControl != null)
							setEnabled(configControl, true);
					} else {
						fStore.setValue(getPreferenceKey(fRegisteredDescriptors[0]), false);
						if (configControl != null)
							setEnabled(configControl, false);
					}
                }
			});
			
			return composite;
		} else {
			Assert.isTrue(false, "TODO"); //$NON-NLS-1$
		}
		return null;
	}

	private void setEnabled(Control control, boolean enable) {
		control.setEnabled(enable);
		if (control instanceof Composite) {
			Control[] children= ((Composite)control).getChildren();
			for (int i= 0; i < children.length; i++) {
	            setEnabled(children[i], enable);
            }
		}
    }

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#dispose()
	 */
	public void dispose() {
		for (int i= 0; i < fRegisteredDescriptors.length; i++) {
	        ISaveParticipantPreferenceConfiguration block= fRegisteredDescriptors[i].getPreferenceConfiguration();
	        if (block != null)
	        	block.dispose();
        }
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#initialize()
	 */
	public void initialize() {
		for (int i= 0; i < fRegisteredDescriptors.length; i++) {
	        ISaveParticipantPreferenceConfiguration block= fRegisteredDescriptors[i].getPreferenceConfiguration();
	        if (block != null)
	        	block.initialize();
        }
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#performDefaults()
	 */
	public void performDefaults() {
		if (fEnableField.isSelected()) {
			fStore.setValue(getPreferenceKey(fRegisteredDescriptors[0]), false);
			fEnableField.setSelection(false);
		}
		
		for (int i= 0; i < fRegisteredDescriptors.length; i++) {
	        ISaveParticipantPreferenceConfiguration block= fRegisteredDescriptors[i].getPreferenceConfiguration();
	        if (block != null)
	        	block.performDefaults();
        }
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#performOk()
	 */
	public void performOk() {
		for (int i= 0; i < fRegisteredDescriptors.length; i++) {
	        ISaveParticipantPreferenceConfiguration block= fRegisteredDescriptors[i].getPreferenceConfiguration();
	        if (block != null)
	        	block.performOk();
        }
	}
	
	private String getPreferenceKey(SaveParticipantDescriptor descriptor) {
		return SaveParticipantRegistry.EDITOR_SAVE_PARTICIPANT_PREFIX + descriptor.getId();
	}

}
