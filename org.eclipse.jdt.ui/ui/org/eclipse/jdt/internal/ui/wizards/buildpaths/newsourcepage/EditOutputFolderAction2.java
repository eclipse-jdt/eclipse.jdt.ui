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
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;


public class EditOutputFolderAction2 extends EditOutputFolderAction {
	
	private final HintTextGroup fProvider;

	public EditOutputFolderAction2(IClasspathModifierListener listener, HintTextGroup provider, IRunnableContext context, ISetSelectionTarget selectionTarget) {
		super(listener, context, selectionTarget);
		
		fProvider= provider;
    }

	/**
	 * {@inheritDoc}
	 */
	protected void selectAndReveal(ISelection selection) {
		fProvider.handleEditOutputFolder(((StructuredSelection)selection).toList());
		
		super.selectAndReveal(selection);
	}
}
