/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IStorage;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;

/**
 * Standard label provider for IStorage objects.
 * Use this class when you want to present IStorage objects in a viewer.
 */
public class StorageLabelProvider extends LabelProvider {
	
	private Map fJarImageMap= new HashMap(10);

	/* (non-Javadoc)
	 * @see ILabelProvider#getImage
	 */
	public Image getImage(Object element) {
		if (element instanceof IStorage) 
			return getImageForJarEntry((IStorage)element);

		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see ILabelProvider#getText
	 */
	public String getText(Object element) {
		if (element instanceof IStorage)
			return ((IStorage)element).getName();

		return super.getText(element);
	}

	/* (non-Javadoc)
	 * 
	 * @see IBaseLabelProvider#dispose
	 */
	public void dispose() {
		if (fJarImageMap != null) {
			Iterator each= fJarImageMap.values().iterator();
			while (each.hasNext()) {
				Image image= (Image)each.next();
				image.dispose();
			}
			fJarImageMap= null;
		}
	}
	
	/*
	 * Gets and caches an image for a JarEntryFile.
	 * The image for a JarEntryFile is retrieved from the EditorRegistry.
	 */ 
	private Image getImageForJarEntry(IStorage element) {
		if (fJarImageMap == null)
			return null;
		
		String extension= element.getFullPath().getFileExtension();
		Image image= (Image)fJarImageMap.get(extension);
		if (image != null) 
			return image;
		IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
		ImageDescriptor desc= registry.getImageDescriptor(element.getName());
		image= desc.createImage();
		fJarImageMap.put(extension, image);
		return image;
	}
}
