/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

/**
 * A command handler to show a java element in the package view.
 *
 * @since 3.2
 */
public class ShowElementInPackageViewHandler extends AbstractHandler {

	private static final String PARAM_ID_ELEMENT_REF= "elementRef"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IJavaElement javaElement= (IJavaElement) event.getObjectParameterForExecution(PARAM_ID_ELEMENT_REF);

		PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
		view.tryToReveal(javaElement);

		return null;
	}

}
