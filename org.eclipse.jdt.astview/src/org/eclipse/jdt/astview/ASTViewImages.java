/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.astview;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;

public class ASTViewImages {

	private static URL fgIconBaseURL= null;
	
	static {
		fgIconBaseURL= ASTViewPlugin.getDefault().getBundle().getEntry("/icons/"); //$NON-NLS-1$
	}
	
	public static final String COLLAPSE= "collapseall.gif"; //$NON-NLS-1$
	public static final String EXPAND= "expandall.gif"; //$NON-NLS-1$
	public static final String LINK_WITH_EDITOR= "synced.gif"; //$NON-NLS-1$

	public static final String SETFOCUS= "setfocus.gif"; //$NON-NLS-1$
	public static final String REFRESH= "refresh.gif"; //$NON-NLS-1$

	
	//---- Helper methods to access icons on the file system --------------------------------------

	public static void setImageDescriptors(IAction action, String type) {
		
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(makeIconFileURL("d", type)); //$NON-NLS-1$
			if (id != null)
				action.setDisabledImageDescriptor(id);
		} catch (MalformedURLException e) {
		}
	
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(makeIconFileURL("c", type)); //$NON-NLS-1$
			if (id != null)
				action.setHoverImageDescriptor(id);
		} catch (MalformedURLException e) {
		}
	
		action.setImageDescriptor(create("e", type)); //$NON-NLS-1$
	}
	
	
	private static ImageDescriptor create(String path, String name) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(path, name));
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	private static URL makeIconFileURL(String path, String name) throws MalformedURLException {
		StringBuffer buffer= new StringBuffer(path);
		buffer.append('/');
		buffer.append(name);
		return new URL(fgIconBaseURL, buffer.toString());
	}	
}
