/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;

import org.eclipse.core.resources.IStorage;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

public class JavaUILabelProvider extends LabelProvider {
	
	private JavaElementImageProvider fImageLabelProvider;
	private StorageLabelProvider fStorageLabelProvider;
	
	private IAdornmentProvider[] fAdornmentProviders;

	private int fImageFlags;
	private int fTextFlags;
	
	/**
	 * Creates a new label provider with default flags.
	 */
	public JavaUILabelProvider() {
		this(JavaElementLabels.M_PARAMETER_TYPES, JavaElementImageProvider.OVERLAY_ICONS, null);
	}

	/**
	 * @param textFlags Flags defined in <code>JavaElementLabels</code>.
	 * @param imageFlags Flags defined in <code>JavaElementImageProvider</code>.
	 */
	public JavaUILabelProvider(int textFlags, int imageFlags, IAdornmentProvider[] adormentProviders) {
		fImageLabelProvider= new JavaElementImageProvider();
		fAdornmentProviders= adormentProviders; 
		
		fStorageLabelProvider= new StorageLabelProvider();
		fImageFlags= imageFlags;
		fTextFlags= textFlags;
	}
	
	/**
	 * Sets the textFlags.
	 * @param textFlags The textFlags to set
	 */
	public void setTextFlags(int textFlags) {
		fTextFlags= textFlags;
	}

	/**
	 * Sets the imageFlags 
	 * @param imageFlags The imageFlags to set
	 */
	public void setImageFlags(int imageFlags) {
		fImageFlags= imageFlags;
	}
	
	/**
	 * Gets the image flags.
	 * Can be overwriten by super classes.
	 * @return Returns a int
	 */
	public int getImageFlags() {
		return fImageFlags;
	}

	/**
	 * Gets the text flags. Can be overwriten by super classes.
	 * @return Returns a int
	 */
	public int getTextFlags() {
		return fTextFlags;
	}	

	public Image getImage(Object element) {
		int imageFlags= getImageFlags();
		if (fAdornmentProviders != null) {
			for (int i= 0; i < fAdornmentProviders.length; i++) {
				imageFlags |= fAdornmentProviders[i].computeAdornmentFlags(element);
			}
		}
		
		Image result= fImageLabelProvider.getImageLabel(element, imageFlags);
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
		String text= JavaElementLabels.getTextLabel(element, getTextFlags());
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
		if (fAdornmentProviders != null) {
			for (int i= 0; i < fAdornmentProviders.length; i++) {
				fAdornmentProviders[i].dispose();
			}
		}
		
		fStorageLabelProvider.dispose();
		fImageLabelProvider.dispose();
	}
	
	public static IAdornmentProvider[] getAdornmentProviders(boolean errortick, IAdornmentProvider extra) {
		if (errortick) {
			if (extra == null) {
				return new IAdornmentProvider[] { new ErrorTickAdornmentProvider() };
			} else {
				return new IAdornmentProvider[] { new ErrorTickAdornmentProvider(), extra };
			}
		}
		if (extra != null) {
			return new IAdornmentProvider[] { extra };
		}
		return null;
	}


}
