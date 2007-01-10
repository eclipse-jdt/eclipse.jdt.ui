/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.saveparticipant;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.preference.IPreferencePageContainer;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

public abstract class AbstractSaveParticipantPreferenceConfiguration implements ISaveParticipantPreferenceConfiguration {
	
	/**
     * Preference prefix that is appended to the id of {@link SaveParticipantDescriptor save participants}.
     * 
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     *
     * @see SaveParticipantDescriptor
     * @since 3.3
     */
    private static final String EDITOR_SAVE_PARTICIPANT_PREFIX= "editor_save_participant_";  //$NON-NLS-1$
	
	private SelectionButtonDialogField fEnableField;
	private Control fConfigControl;
	private IScopeContext fContext;

	/**
	 * The id of the post save listener managed by this configuration block, not null
	 */
	protected abstract String getPostSaveListenerId();
	
	/**
	 * The name of the post save listener managed by this configuration block, not null
	 */
	protected abstract String getPostSaveListenerName();
	
	protected Control createConfigControl(Composite composite, IPreferencePageContainer container) {
		//Default has no specific controls
	    return null;
    }
	
	/**
	 * {@inheritDoc}
	 */
	public Control createControl(Composite parent, IPreferencePageContainer container) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridData gridData= new GridData(SWT.FILL, SWT.TOP, true, false);
		composite.setLayoutData(gridData);
		GridLayout layout= new GridLayout(3, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		
		fEnableField= new SelectionButtonDialogField(SWT.CHECK);
		fEnableField.setLabelText(getPostSaveListenerName());
		fEnableField.doFillIntoGrid(composite, 3);
		
		Label label= new Label(composite, SWT.NONE);
		GridData data= new GridData(SWT.FILL, SWT.TOP, false, false);
		data.widthHint= new PixelConverter(composite).convertWidthInCharsToPixels(1);
		label.setLayoutData(data);
		
		fConfigControl= createConfigControl(composite, container);
		
		return composite;
	}

	/**
	 * {@inheritDoc}
	 */
	public void initialize(final IScopeContext context, IAdaptable element) {
		boolean enabled= isEnabled(context);
		fEnableField.setSelection(enabled);
		
		final ControlEnableState[] state= new ControlEnableState[1];
		if (fConfigControl != null && !enabled) {
			state[0]= ControlEnableState.disable(fConfigControl);
		}
		
		fEnableField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				context.getNode(JavaUI.ID_PLUGIN).putBoolean(getPreferenceKey(), fEnableField.isSelected());
				if (fConfigControl != null) {
					if (state[0] != null) {
						state[0].restore();
						state[0]= null;
					} else {
						state[0]= ControlEnableState.disable(fConfigControl);
					}
				}
			}
		});
		
		fContext= context;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {}
	
	/**
	 * {@inheritDoc}
	 */
	public void performDefaults() {
		String key= getPreferenceKey();
		boolean defaultEnabled= new DefaultScope().getNode(JavaUI.ID_PLUGIN).getBoolean(key, false);
		fContext.getNode(JavaUI.ID_PLUGIN).putBoolean(key, defaultEnabled);
		fEnableField.setSelection(defaultEnabled);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void performOk() {}
	
	/**
	 * {@inheritDoc}
	 */
	public void enableProjectSettings() {
		fContext.getNode(JavaUI.ID_PLUGIN).putBoolean(getPreferenceKey(), fEnableField.isSelected());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void disableProjectSettings() {
		fContext.getNode(JavaUI.ID_PLUGIN).remove(getPreferenceKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean hasSettingsInScope(IScopeContext context) {
    	return context.getNode(JavaUI.ID_PLUGIN).get(getPreferenceKey(), null) != null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isEnabled(IScopeContext context) {
		IEclipsePreferences node;
		if (hasSettingsInScope(context)) {
			node= context.getNode(JavaUI.ID_PLUGIN);
		} else {
			node= new InstanceScope().getNode(JavaUI.ID_PLUGIN);
		}
		IEclipsePreferences defaultNode= new DefaultScope().getNode(JavaUI.ID_PLUGIN);
		
		String key= getPreferenceKey();
		return node.getBoolean(key, defaultNode.getBoolean(key, false));
	}
	
	private String getPreferenceKey() {
		return EDITOR_SAVE_PARTICIPANT_PREFIX + getPostSaveListenerId();
    }
}
