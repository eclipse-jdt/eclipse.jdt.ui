/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.ui.actions;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;
import org.eclipse.jdt.bcoview.internal.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.plugin.AbstractUIPlugin;


/**
 * Default action which could be used as template for "toggle" action. Action image, text and
 * tooltip will be initialized by default. To use it, register IPropertyChangeListener and check for
 * IAction.CHECKED event name.
 */
public abstract class DefaultToggleAction extends Action implements IPropertyChangeListener {

	private static final String ACTION = "action"; //$NON-NLS-1$

	boolean avoidUpdate;

	private final IPreferenceStore store;

	public DefaultToggleAction(final String id) {
		this(id, true);
	}

	public DefaultToggleAction(final String id, final boolean addPreferenceListener) {
		super();
		setId(id);
		init();

		IPreferenceStore prefStore = BytecodeOutlinePlugin.getDefault().getPreferenceStore();

		boolean isChecked = prefStore.getBoolean(id);
		setChecked(isChecked);
		if (addPreferenceListener) {
			this.store = prefStore;
			prefStore.addPropertyChangeListener(this);
		} else {
			this.store = null;
		}
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		if (store == null) {
			return;
		}
		String id = getId();
		if (!id.equals(event.getProperty())) {
			return;
		}
		boolean isChecked = store.getBoolean(id);
		setChecked(isChecked);
		// The action state can be changed from preference page (therefore run()), but...
		// see http://forge.objectweb.org/tracker/?func=detail&atid=100023&aid=311888&group_id=23
		// this causes multiple unneeded re-syncs of the compare editor
		if (!avoidUpdate) {
			run(isChecked);
		}
	}

	public void dispose() {
		if (store != null) {
			store.removePropertyChangeListener(this);
		}
	}

	private void init() {
		String myId = getId();
		if (myId != null && myId.startsWith("diff_")) { //$NON-NLS-1$
			myId = myId.substring("diff_".length()); //$NON-NLS-1$
		}
		setImageDescriptor(AbstractUIPlugin
				.imageDescriptorFromPlugin(
						BytecodeOutlinePlugin.getDefault().getBundle().getSymbolicName(),
						Messages.getResourceString(ACTION + "_" + myId + "_" + IMAGE))); //$NON-NLS-1$ //$NON-NLS-2$

		setText(Messages.getResourceString(ACTION + "_" + myId + "_" + TEXT)); //$NON-NLS-1$ //$NON-NLS-2$
		setToolTipText(Messages.getResourceString(ACTION + "_" + myId + "_" + TOOL_TIP_TEXT)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public final void run() {
		boolean isChecked = isChecked();
		avoidUpdate = true;
		// compare dialog: we use store as global variables to remember the state
		BytecodeOutlinePlugin.getDefault().getPreferenceStore().setValue(getId(), isChecked);
		avoidUpdate = false;
		run(isChecked);
	}

	public abstract void run(boolean newState);
}
