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
package org.eclipse.jdt.internal.ui.model;

import org.eclipse.jface.action.Action;

import org.eclipse.team.core.mapping.ISynchronizationContext;

import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Action to accept pending refactorings to execute them on the local workspace.
 * 
 * @since 3.2
 */
public final class AcceptRefactoringsAction extends Action {

	/** The synchronize page configuration */
	private final ISynchronizePageConfiguration fConfiguration;

	/** The synchronization context */
	private final ISynchronizationContext fContext;

	/**
	 * Creates a new accept refactorings action.
	 * 
	 * @param context
	 *            the synchronization context
	 * @param configuration
	 *            the synchronize page configuration
	 */
	public AcceptRefactoringsAction(final ISynchronizationContext context, final ISynchronizePageConfiguration configuration) {
		fConfiguration= configuration;
		fContext= context;
		setText(ModelMessages.AcceptRefactoringsAction_title);
		setToolTipText(ModelMessages.AcceptRefactoringsAction_tool_tip);
		setDescription(ModelMessages.AcceptRefactoringsAction_description);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isEnabled() {
		return JavaSynchronizationActionProvider.hasRefactorings(fContext, fConfiguration);
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {
		// TODO: implement
	}
}