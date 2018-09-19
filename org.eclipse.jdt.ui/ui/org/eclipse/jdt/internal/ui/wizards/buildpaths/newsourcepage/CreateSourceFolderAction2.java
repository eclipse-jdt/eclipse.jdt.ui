/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.part.ISetSelectionTarget;

public class CreateSourceFolderAction2 extends CreateSourceFolderAction {

	private final HintTextGroup fProvider;

	public CreateSourceFolderAction2(HintTextGroup provider, IRunnableContext context, ISetSelectionTarget selectionTarget) {
		super(context, selectionTarget);

		fProvider= provider;
    }

	@Override
	protected void selectAndReveal(ISelection selection) {
	    fProvider.handleFolderCreation(((StructuredSelection)selection).toList());

	    super.selectAndReveal(selection);
	}
}