/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.resources.IStorage;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

public class JavaUILabelProvider extends LabelProvider {
	
	private JavaElementImageProvider fImageLabelProvider;
	private StorageLabelProvider fStorageLabelProvider;

	private int fImageFlags;
	private int fTextFlags;
	
	/**
	 * Creates a new label provider with default flags.
	 */
	public JavaUILabelProvider() {
		this(JavaElementLabels.M_PARAMETER_TYPES, JavaElementImageProvider.OVERLAY_ICONS);
	}

	/**
	 * @param textFlags Flags defined in <code>JavaElementLabels</code>.
	 * @param imageFlags Flags defined in <code>JavaElementImageProvider</code>.
	 */
	public JavaUILabelProvider(int textFlags, int imageFlags, JavaElementImageProvider imageLabelProvider) {
		fImageLabelProvider= imageLabelProvider;
		fStorageLabelProvider= new StorageLabelProvider();
		fImageFlags= imageFlags;
		fTextFlags= textFlags;
	}

	/**
	 * @param textFlags Flags defined in <code>JavaElementLabels</code>.
	 * @param imageFlags Flags defined in <code>JavaElementImageProvider</code>.
	 */	
	public JavaUILabelProvider(int textFlags, int imageFlags) {
		this(textFlags, imageFlags, new JavaElementImageProvider());
	}
	
	public JavaUILabelProvider(JavaElementImageProvider imageLabelProvider) {
		this(JavaElementLabels.M_PARAMETER_TYPES, JavaElementImageProvider.OVERLAY_ICONS, imageLabelProvider);

	}		
	
	/**
	 * Sets the text flags to use. Valid flags are defined in <code>JavaElementLabels</code>.
	 */
	public void setTextFlags(int flags) {
		fTextFlags= flags;
	}
	
	/**
	 * Sets the image flags to use. Valid flags are defined in <code>JavaElementImageProvider</code>.
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

		return result;
	}

	/* (non-Javadoc)
	 * @see ILabelProvider#getText
	 */
	public String getText(Object element) {
		String text= JavaElementLabels.getTextLabel(element, fTextFlags);
		if (text.length() > 0) {
			return text;
		}

		if (element instanceof IStorage)
			return fStorageLabelProvider.getText(element);

		return text;
	}

	/* (non-Javadoc)
	 * 
	 * @see IBaseLabelProvider#dispose
	 */
	public void dispose() {
		fStorageLabelProvider.dispose();
		fImageLabelProvider.dispose();
	}
}
