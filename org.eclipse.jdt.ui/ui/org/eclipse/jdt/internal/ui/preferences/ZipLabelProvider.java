/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
  * Implementation of the <code>ILabelProvider</code>
  */

public class ZipLabelProvider extends LabelProvider {
	
	public Image getImage(Object element) {
		if (((ZipTreeNode)element).representsZipFile())
			return JavaPlugin.getDefault().getImageRegistry().get(JavaPluginImages.IMG_OBJS_JAR);
		else 
			return JavaPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	}

	public String getText(Object element) {
		if (element instanceof ZipTreeNode)
			return ((ZipTreeNode) element).getName();
		else return "";
	}
}


