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

import com.ibm.icu.text.Collator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.ControlEnableState;
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
	
	private static class ConfigurationDescription {
		
		SelectionButtonDialogField fEnableField;
		ISaveParticipantPreferenceConfiguration fPreferenceConfiguration;
		SaveParticipantDescriptor fDescriptior;
		
		public ConfigurationDescription(SaveParticipantDescriptor descriptor, ISaveParticipantPreferenceConfiguration preferenceConfiguration, SelectionButtonDialogField enableField) {
			fDescriptior= descriptor;
			fPreferenceConfiguration= preferenceConfiguration;
			fEnableField= enableField;
		}
	}
	
	private final PreferencePage fPreferencePage;
	private final IScopeContext fContext;
	private ConfigurationDescription[] fConfigurationDescriptions;
	
	public SaveParticipantConfigurationBlock(IScopeContext context, PreferencePage preferencePage) {
		Assert.isNotNull(context);
		Assert.isNotNull(preferencePage);
		
		fContext= context;
		fPreferencePage= preferencePage;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#createControl(org.eclipse.swt.widgets.Composite)
	 * @since 3.3
	 */
	public Control createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		composite.setLayout(new GridLayout(2, false));
		
		SaveParticipantRegistry registry= JavaPlugin.getDefault().getSaveParticipantRegistry();
		SaveParticipantDescriptor[] descriptors= registry.getSaveParticipantDescriptors();
		
		fConfigurationDescriptions= new ConfigurationDescription[descriptors.length];
		if (descriptors.length == 0)
			return composite;
		
		Arrays.sort(descriptors, new Comparator() {
			public int compare(Object o1, Object o2) {
				SaveParticipantDescriptor d1= (SaveParticipantDescriptor)o1;
				SaveParticipantDescriptor d2= (SaveParticipantDescriptor)o2;
				return Collator.getInstance().compare(d1.getPostSaveListener().getName(), d2.getPostSaveListener().getName());
			}
		});
		
		IPostSaveListener[] listeners= registry.getEnabledPostSaveListeners(fContext);
		HashSet enabledListeners= new HashSet();
		for (int i= 0; i < listeners.length; i++) {
			enabledListeners.add(listeners[i].getId());
		}
		
		for (int i= 0; i < descriptors.length; i++) {
			final SaveParticipantDescriptor descriptor= descriptors[i];
			
			boolean isActive= enabledListeners.contains(descriptor.getId());
			
			SelectionButtonDialogField enableField= new SelectionButtonDialogField(SWT.CHECK);
			enableField.setLabelText(Messages.format(PreferencesMessages.JavaEditorPreferencePage_saveParticipant_cleanup, descriptor.getPostSaveListener().getName()));
			if (isActive) {
				enableField.setSelection(true);
			}
			enableField.doFillIntoGrid(composite, 2);
			
			ISaveParticipantPreferenceConfiguration preferenceConfiguration= descriptor.getPreferenceConfiguration();
			final Control configControl;
			final ControlEnableState[] state= new ControlEnableState[1];
			if (preferenceConfiguration != null) {
				IPreferencePageContainer container= fPreferencePage.getContainer();
				IWorkingCopyManager manager;
				if (container instanceof IWorkbenchPreferenceContainer) {
					manager= ((IWorkbenchPreferenceContainer)container).getWorkingCopyManager();
				} else {
					manager= new WorkingCopyManager(); // non shared
				}
				
				configControl= preferenceConfiguration.createControl(composite, manager);
				if (!isActive) {
					state[0]= ControlEnableState.disable(configControl);
				}
			} else {
				configControl= null;
			}
			
			enableField.setDialogFieldListener(new IDialogFieldListener() {
				public void dialogFieldChanged(DialogField field) {
					fContext.getNode(JavaUI.ID_PLUGIN).putBoolean(SaveParticipantRegistry.getPreferenceKey(descriptor), ((SelectionButtonDialogField)field).isSelected());
					if (configControl != null) {
						if (state[0] != null) {
							state[0].restore();
							state[0]= null;
						} else {
							state[0]= ControlEnableState.disable(configControl);
						}
					}
				}
			});
			
			fConfigurationDescriptions[i]= new ConfigurationDescription(descriptor, preferenceConfiguration, enableField);
		}
		
		return composite;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#dispose()
	 */
	public void dispose() {
		for (int i= 0; i < fConfigurationDescriptions.length; i++) {
			ISaveParticipantPreferenceConfiguration block= fConfigurationDescriptions[i].fPreferenceConfiguration;
			if (block != null)
				block.dispose();
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#initialize()
	 */
	public void initialize() {
		for (int i= 0; i < fConfigurationDescriptions.length; i++) {
			ISaveParticipantPreferenceConfiguration block= fConfigurationDescriptions[i].fPreferenceConfiguration;
			if (block != null)
				block.initialize(fContext);
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#performDefaults()
	 */
	public void performDefaults() {
		IEclipsePreferences defaultNode= new DefaultScope().getNode(JavaUI.ID_PLUGIN);
		IEclipsePreferences node= fContext.getNode(JavaUI.ID_PLUGIN);
		
		for (int i= 0; i < fConfigurationDescriptions.length; i++) {
			ConfigurationDescription configurationDescription= fConfigurationDescriptions[i];
			
			String key= SaveParticipantRegistry.getPreferenceKey(configurationDescription.fDescriptior);
			boolean enabled= defaultNode.getBoolean(key, false);
			node.putBoolean(key, enabled);
			configurationDescription.fEnableField.setSelection(enabled);
			
			ISaveParticipantPreferenceConfiguration block= configurationDescription.fPreferenceConfiguration;
			if (block != null)
				block.performDefaults();
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#performOk()
	 */
	public void performOk() {
		for (int i= 0; i < fConfigurationDescriptions.length; i++) {
			ISaveParticipantPreferenceConfiguration block= fConfigurationDescriptions[i].fPreferenceConfiguration;
			if (block != null)
				block.performOk();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void enableProjectSettings() {
		IEclipsePreferences node= fContext.getNode(JavaUI.ID_PLUGIN);
		
		for (int i= 0; i < fConfigurationDescriptions.length; i++) {
			ConfigurationDescription configurationDescription= fConfigurationDescriptions[i];
			
			node.putBoolean(SaveParticipantRegistry.getPreferenceKey(configurationDescription.fDescriptior), configurationDescription.fEnableField.isSelected());
			
			ISaveParticipantPreferenceConfiguration block= configurationDescription.fPreferenceConfiguration;
			if (block != null)
				block.enableProjectSettings();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void disableProjectSettings() {
		IEclipsePreferences node= fContext.getNode(JavaUI.ID_PLUGIN);
		
		for (int i= 0; i < fConfigurationDescriptions.length; i++) {
			ConfigurationDescription configurationDescription= fConfigurationDescriptions[i];
			
			node.remove(SaveParticipantRegistry.getPreferenceKey(configurationDescription.fDescriptior));
			
			ISaveParticipantPreferenceConfiguration block= configurationDescription.fPreferenceConfiguration;
			if (block != null)
				block.disableProjectSettings();
		}
	}
}
