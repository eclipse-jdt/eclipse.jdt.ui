/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.jdt.astview;

import java.net.URL;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;

public class ASTViewImages {

	private static final IPath ICONS_PATH= new Path("$nl$/icons"); //$NON-NLS-1$

	public static final String COLLAPSE= "collapseall.png"; //$NON-NLS-1$
	public static final String EXPAND= "expandall.png"; //$NON-NLS-1$
	public static final String LINK_WITH_EDITOR= "synced.png"; //$NON-NLS-1$

	public static final String SETFOCUS= "setfocus.png"; //$NON-NLS-1$
	public static final String REFRESH= "refresh.png"; //$NON-NLS-1$
	public static final String CLEAR= "clear.png"; //$NON-NLS-1$

	public static final String ADD_TO_TRAY= "add.png"; //$NON-NLS-1$

	//---- Helper methods to access icons on the file system --------------------------------------

	public static void setImageDescriptors(IAction action, String type) {
		ImageDescriptor id= create("d", type); //$NON-NLS-1$
		if (id != null)
			action.setDisabledImageDescriptor(id);

		id= create("e", type); //$NON-NLS-1$
		if (id != null) {
			action.setImageDescriptor(id);
		} else {
			action.setImageDescriptor(ImageDescriptor.getMissingImageDescriptor());
		}
	}

	private static ImageDescriptor create(String prefix, String name) {
		IPath path= ICONS_PATH.append(prefix).append(name);
		return createImageDescriptor(ASTViewPlugin.getDefault().getBundle(), path);
	}

	/*
	 * Since 3.1.1. Load from icon paths with $NL$
	 */
	public static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path) {
		URL url= FileLocator.find(bundle, path, null);
		if (url != null) {
			return ImageDescriptor.createFromURL(url);
		}
		return null;
	}

	private ASTViewImages() {
	}
}
