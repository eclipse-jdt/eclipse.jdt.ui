/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.NotDefinedException;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;

/**
 * Reloads the active file-imported JUnit test run from its original source.
 */
public final class ReloadTestRunHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart activePart= HandlerUtil.getActivePart(event);
		if (!(activePart instanceof TestRunnerViewPart testRunnerView)) {
			return null;
		}

		TestRunSession testRunSession= testRunnerView.getTestRunSession();
		JUnitModel model= JUnitCorePlugin.getModel();
		if (testRunSession == null || model.getImportedTestRunSource(testRunSession) == null) {
			return null;
		}

		try {
			JUnitModel.reloadTestRunSession(testRunSession);
		} catch (CoreException e) {
			JUnitPlugin.log(e);
			String title;
			try {
				title= event.getCommand().getName();
			} catch (NotDefinedException exception) {
				title= JUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_error_title;
			}
			ErrorDialog.openError(testRunnerView.getSite().getShell(), title, e.getStatus().getMessage(), e.getStatus());
		}
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		Object activePart= HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_PART_NAME);
		boolean enabled= false;
		if (activePart instanceof TestRunnerViewPart testRunnerView) {
			TestRunSession testRunSession= testRunnerView.getTestRunSession();
			enabled= testRunSession != null
					&& JUnitCorePlugin.getModel().getImportedTestRunSource(testRunSession) != null;
		}
		setBaseEnabled(enabled);
	}
}
