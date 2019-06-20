/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.LegacyHandlerSubmissionExpression;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;

import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

public class CorrectionCommandInstaller {

	private List<IHandlerActivation> fCorrectionHandlerActivations;

	public CorrectionCommandInstaller() {
		fCorrectionHandlerActivations= null;
	}

	public void registerCommands(CompilationUnitEditor editor) {
		IWorkbench workbench= PlatformUI.getWorkbench();
		ICommandService commandService= workbench.getAdapter(ICommandService.class);
		IHandlerService handlerService= workbench.getAdapter(IHandlerService.class);
		if (commandService == null || handlerService == null) {
			return;
		}

		if (fCorrectionHandlerActivations != null) {
			JavaPlugin.logErrorMessage("correction handler activations not released"); //$NON-NLS-1$
		}
		fCorrectionHandlerActivations= new ArrayList<>();

		for (String id : (Collection<String>)commandService.getDefinedCommandIds()) {
			if (id.startsWith(ICommandAccess.COMMAND_ID_PREFIX)) {
				boolean isAssist= id.endsWith(ICommandAccess.ASSIST_SUFFIX);
				CorrectionCommandHandler handler= new CorrectionCommandHandler(editor, id, isAssist);
				IHandlerActivation activation= handlerService.activateHandler(id, handler, new LegacyHandlerSubmissionExpression(null, null, editor.getSite()));
				fCorrectionHandlerActivations.add(activation);
			}
		}
	}

	public void deregisterCommands() {
		IHandlerService handlerService= PlatformUI.getWorkbench().getAdapter(IHandlerService.class);
		if (handlerService != null && fCorrectionHandlerActivations != null) {
			handlerService.deactivateHandlers(fCorrectionHandlerActivations);
			fCorrectionHandlerActivations= null;
		}
	}

}
