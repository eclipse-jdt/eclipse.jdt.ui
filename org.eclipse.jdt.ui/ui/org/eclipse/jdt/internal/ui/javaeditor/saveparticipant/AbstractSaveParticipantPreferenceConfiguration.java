/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.saveparticipant;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jface.preference.IPreferencePageContainer;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

public abstract class AbstractSaveParticipantPreferenceConfiguration implements ISaveParticipantPreferenceConfiguration {

	/**
     * Preference prefix that is prepended to the id of {@link SaveParticipantDescriptor save participants}.
     *
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     *
     * @see SaveParticipantDescriptor
     * @since 3.3
     */
    public static final String EDITOR_SAVE_PARTICIPANT_PREFIX= SaveParticipantPreferenceConfigurationConstants.EDITOR_SAVE_PARTICIPANT_PREFIX;

	private SelectionButtonDialogField fEnableField;
	private IScopeContext fContext;

	/**
	 * @return id of the post save listener managed by this configuration block
	 */
	protected abstract String getPostSaveListenerId();

	/**
	 * @return human readable name of the post save listener managed by this configuration block
	 */
	protected abstract String getPostSaveListenerName();

	/**
	 * Subclasses can add specific controls
	 *
	 * @param parent the parent to use to add the control to
	 * @param container the container showing the preferences
	 */
	protected void createConfigControl(Composite parent, IPreferencePageContainer container) {
		//Default has no specific controls
	}

	@Override
	public Control createControl(Composite parent, IPreferencePageContainer container) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);

		fEnableField= new SelectionButtonDialogField(SWT.CHECK);
		fEnableField.setLabelText(getPostSaveListenerName());
		fEnableField.doFillIntoGrid(composite, 1);

		createConfigControl(composite, container);

		return composite;
	}

	@Override
	public void initialize(final IScopeContext context, IAdaptable element) {
		boolean enabled= isEnabled(context);
		fEnableField.setSelection(enabled);

		fEnableField.setDialogFieldListener(field -> {
			fContext.getNode(JavaUI.ID_PLUGIN).putBoolean(getPreferenceKey(), fEnableField.isSelected());
			enabled(fEnableField.isSelected());
		});

		fContext= context;

		enabled(enabled);
	}

	@Override
	public void dispose() {}

	@Override
	public void performDefaults() {
		String key= getPreferenceKey();
		boolean defaultEnabled;
		if (ProjectScope.SCOPE.equals(fContext.getName())) {
			defaultEnabled= InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN).getBoolean(key, false);
		} else {
			defaultEnabled= DefaultScope.INSTANCE.getNode(JavaUI.ID_PLUGIN).getBoolean(key, false);
		}
		fContext.getNode(JavaUI.ID_PLUGIN).putBoolean(key, defaultEnabled);
		fEnableField.setSelection(defaultEnabled);

		enabled(defaultEnabled);
	}

	@Override
	public void performOk() {}

	@Override
	public void enableProjectSettings() {
		fContext.getNode(JavaUI.ID_PLUGIN).putBoolean(getPreferenceKey(), fEnableField.isSelected());
	}

	@Override
	public void disableProjectSettings() {
		fContext.getNode(JavaUI.ID_PLUGIN).remove(getPreferenceKey());
	}

	@Override
	public boolean hasSettingsInScope(IScopeContext context) {
    	return context.getNode(JavaUI.ID_PLUGIN).get(getPreferenceKey(), null) != null;
	}

	@Override
	public boolean isEnabled(IScopeContext context) {
		IEclipsePreferences node;
		if (hasSettingsInScope(context)) {
			node= context.getNode(JavaUI.ID_PLUGIN);
		} else {
			node= InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN);
		}
		IEclipsePreferences defaultNode= DefaultScope.INSTANCE.getNode(JavaUI.ID_PLUGIN);

		String key= getPreferenceKey();
		return node.getBoolean(key, defaultNode.getBoolean(key, false));
	}

	/**
	 * @param enabled true if this save action has been enabled by user, false otherwise
	 */
	protected void enabled(boolean enabled) {
	}

	private String getPreferenceKey() {
		return EDITOR_SAVE_PARTICIPANT_PREFIX + getPostSaveListenerId();
    }
}
