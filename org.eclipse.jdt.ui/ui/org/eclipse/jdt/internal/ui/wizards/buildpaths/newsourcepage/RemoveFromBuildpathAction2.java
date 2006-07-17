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

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class RemoveFromBuildpathAction2 extends RemoveFromBuildpathAction implements IClasspathModifierAction {

	private final HintTextGroup fProvider;

	public RemoveFromBuildpathAction2(IClasspathModifierListener listener, HintTextGroup provider, IRunnableContext context) {
		super(null, context, listener, JavaPluginImages.DESC_ELCL_REMOVE_AS_SOURCE_FOLDER);
		
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_REMOVE_AS_SOURCE_FOLDER);
		
		fProvider= provider;
    }
	
	/**
	 * {@inheritDoc}
	 */
	protected void selectAndReveal(ISelection selection) {
	    fProvider.handleRemoveFromBP(((StructuredSelection)selection).toList(), false);
	}
}
