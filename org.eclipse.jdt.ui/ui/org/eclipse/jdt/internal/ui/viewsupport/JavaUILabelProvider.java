/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.resources.IStorage;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IJavaElement;

public class JavaUILabelProvider extends LabelProvider {
	
	private JavaElementImageProvider fImageLabelProvider;
	private WorkbenchLabelProvider fWorkbenchLabelProvider;	
	private StorageLabelProvider fStorageLabelProvider;

	private int fImageFlags;
	private int fTextFlags;
	
	/**
	 * Creates a new label provider with default flags.
	 */
	public JavaUILabelProvider() {
		this(JavaElementLabels.M_PARAMETER_TYPES, JavaElementImageProvider.OVERLAY_ICONS);
	}

	public JavaUILabelProvider(int textFlags, int imageFlags, JavaElementImageProvider imageLabelProvider) {
		fImageLabelProvider= imageLabelProvider;
		fWorkbenchLabelProvider= new WorkbenchLabelProvider();
		fStorageLabelProvider= new StorageLabelProvider();
		fImageFlags= imageFlags;
		fTextFlags= textFlags;
	}
	
	public JavaUILabelProvider(int textFlags, int imageFlags) {
		this(textFlags, imageFlags, new JavaElementImageProvider());
	}
	
	public JavaUILabelProvider(JavaElementImageProvider imageLabelProvider) {
		this(JavaElementLabels.M_PARAMETER_TYPES, JavaElementImageProvider.OVERLAY_ICONS, imageLabelProvider);

	}		
	
	/**
	 * Sets the text flags to use
	 */
	public void setTextFlags(int flags) {
		fTextFlags= flags;
	}
	
	/**
	 * Sets the text flags to use
	 */
	public void setImageFlags(int flags) {
		fImageFlags= flags;
	}	
	
	/* (non-Javadoc)
	 * @see ILabelProvider#getImage
	 */
	public Image getImage(Object element) {
		Image result= fImageLabelProvider.getImageLabel(element, fImageFlags);
		if (result != null) {
			return result;
		}

		if (element instanceof IStorage) 
			return fStorageLabelProvider.getImage(element);

		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see ILabelProvider#getText
	 */
	public String getText(Object element) {

		if (element instanceof IJavaElement) {
			return JavaElementLabels.getElementLabel((IJavaElement) element, fTextFlags);
		}
	
		String text= fWorkbenchLabelProvider.getText(element);
		if (text.length() > 0) {
			return text;
		}

		if (element instanceof IStorage)
			return fStorageLabelProvider.getText(element);

		return super.getText(element);
	}

	/* (non-Javadoc)
	 * 
	 * @see IBaseLabelProvider#dispose
	 */
	public void dispose() {
		fWorkbenchLabelProvider.dispose();
		fStorageLabelProvider.dispose();
	}
}
