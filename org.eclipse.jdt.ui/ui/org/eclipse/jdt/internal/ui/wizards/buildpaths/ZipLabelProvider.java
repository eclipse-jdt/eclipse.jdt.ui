/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;

class ZipLabelProvider extends LabelProvider {
	
	private final Image IMG_JAR= JavaPlugin.getDefault().getImageRegistry().get(JavaPluginImages.IMG_OBJS_JAR);
	private final Image IMG_FOLDER= JavaPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	
	public Image getImage(Object element) {
		if (element == null || !(element instanceof ZipTreeNode))
			return super.getImage(element);
		if (((ZipTreeNode)element).representsZipFile())
			return IMG_JAR;
		else 
			return IMG_FOLDER;
	}

	public String getText(Object element) {
		if (element == null || !(element instanceof ZipTreeNode))
			return super.getText(element);
		return ((ZipTreeNode) element).getName();
	}
}


