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
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

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
class SaveParticipantConfigurationBlock implements IPreferenceAndPropertyConfigurationBlock {

	private final SaveParticipantDescriptor[] fRegisteredDescriptors;
	private SelectionButtonDialogField fEnableField;
	private final PreferencePage fPreferencePage;
	private final IScopeContext fContext;
	private ISaveParticipantPreferenceConfiguration fConfigurationBlock;

	public SaveParticipantConfigurationBlock(IScopeContext context, PreferencePage preferencePage) {
		fContext= context;
		Assert.isNotNull(context);
		fRegisteredDescriptors= JavaPlugin.getDefault().getSaveParticipantRegistry().getSaveParticipantDescriptors();

		fPreferencePage= preferencePage;
    }

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#createControl(org.eclipse.swt.widgets.Composite)
	 * @since 3.3
	 */
	public Control createControl(Composite parent) {
		if (fRegisteredDescriptors.length == 1) {
			IPostSaveListener listener= fRegisteredDescriptors[0].getPostSaveListener();
			boolean isActive= JavaPlugin.getDefault().getSaveParticipantRegistry().getEnabledPostSaveListeners(fContext).length == 1;
			
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			composite.setLayout(new GridLayout(2, false));
			
			fEnableField= new SelectionButtonDialogField(SWT.CHECK);
			fEnableField.setLabelText(Messages.format(PreferencesMessages.JavaEditorPreferencePage_saveParticipant_cleanup, listener.getName()));
			if (isActive) {
				fEnableField.setSelection(true);
			}
			fEnableField.doFillIntoGrid(composite, 2);
			
			fConfigurationBlock= fRegisteredDescriptors[0].getPreferenceConfiguration();
			final Control configControl;
			if (fConfigurationBlock != null) {
				IPreferencePageContainer container= fPreferencePage.getContainer();
				IWorkingCopyManager manager;
				if (container instanceof IWorkbenchPreferenceContainer) {
					manager= ((IWorkbenchPreferenceContainer)container).getWorkingCopyManager();
				} else {
					manager= new WorkingCopyManager(); // non shared
				}
				
				configControl= fConfigurationBlock.createControl(composite, manager);
				if (!isActive)
					setEnabled(configControl, false);
			} else {
				configControl= null;
			}
			
			fEnableField.setDialogFieldListener(new IDialogFieldListener() {
				public void dialogFieldChanged(DialogField field) {
					fContext.getNode(JavaUI.ID_PLUGIN).putBoolean(SaveParticipantRegistry.getPreferenceKey(fRegisteredDescriptors[0]), fEnableField.isSelected());
					if (configControl != null)
						setEnabled(configControl, fEnableField.isSelected());
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
	        	block.initialize(fContext);
        }
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#performDefaults()
	 */
	public void performDefaults() {
		if (fEnableField.isSelected()) {
			fContext.getNode(JavaUI.ID_PLUGIN).putBoolean(SaveParticipantRegistry.getPreferenceKey(fRegisteredDescriptors[0]), false);
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
	
	/**
	 * {@inheritDoc}
	 */
	public void enableProjectSettings() {
		String key= SaveParticipantRegistry.getPreferenceKey(fRegisteredDescriptors[0]);
		fContext.getNode(JavaUI.ID_PLUGIN).putBoolean(key, fEnableField.isSelected());
		
		fConfigurationBlock.enableProjectSettings();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void disableProjectSettings() {
		IEclipsePreferences node= fContext.getNode(JavaUI.ID_PLUGIN);
		for (int i= 0; i < fRegisteredDescriptors.length; i++) {
			node.remove(SaveParticipantRegistry.getPreferenceKey(fRegisteredDescriptors[i]));
		}
		
		fConfigurationBlock.disableProjectSettings();
	}
}
