/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.resources.IStorage;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.ui.ProblemsLabelDecorator;

public class JavaUILabelProvider extends LabelProvider {
	
	protected JavaElementImageProvider fImageLabelProvider;
	protected StorageLabelProvider fStorageLabelProvider;
	
	protected ILabelDecorator[] fLabelDecorators;

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
	public JavaUILabelProvider(int textFlags, int imageFlags, ILabelDecorator[] labelDecorators) {
		fImageLabelProvider= new JavaElementImageProvider();
		fLabelDecorators= labelDecorators; 
		
		fStorageLabelProvider= new StorageLabelProvider();
		fImageFlags= imageFlags;
		fTextFlags= textFlags;
	}
	
	/**
	 * Sets the textFlags.
	 * @param textFlags The textFlags to set
	 */
	public final void setTextFlags(int textFlags) {
		fTextFlags= textFlags;
	}

	/**
	 * Sets the imageFlags 
	 * @param imageFlags The imageFlags to set
	 */
	public final void setImageFlags(int imageFlags) {
		fImageFlags= imageFlags;
	}
	
	/**
	 * Gets the image flags.
	 * Can be overwriten by super classes.
	 * @return Returns a int
	 */
	public final int getImageFlags() {
		return fImageFlags;
	}

	/**
	 * Gets the text flags.
	 * @return Returns a int
	 */
	public final int getTextFlags() {
		return fTextFlags;
	}
	
	/**
	 * Evaluates the image flags for a element.
	 * Can be overwriten by super classes.
	 * @return Returns a int
	 */
	protected int evaluateImageFlags(Object element) {
		return getImageFlags();
	}

	/**
	 * Evaluates the text flags for a element. Can be overwriten by super classes.
	 * @return Returns a int
	 */
	protected int evaluateTextFlags(Object element) {
		return getTextFlags();
	}
	

	/* (non-Javadoc)
	 * @see ILabelProvider#getImage
	 */
	public Image getImage(Object element) {
		Image result= fImageLabelProvider.getImageLabel(element, evaluateImageFlags(element));
		if (result == null && (element instanceof IStorage)) {
			result= fStorageLabelProvider.getImage(element);
		}
		if (fLabelDecorators != null && result != null) {
			for (int i= 0; i < fLabelDecorators.length; i++) {
				result= fLabelDecorators[i].decorateImage(result, element);
			}
		}			
		return result;
	}

	/* (non-Javadoc)
	 * @see ILabelProvider#getText
	 */
	public String getText(Object element) {
		String result= JavaElementLabels.getTextLabel(element, evaluateTextFlags(element));
		if (result.length() == 0 && (element instanceof IStorage)) {
			result= fStorageLabelProvider.getText(element);
		}
		if (fLabelDecorators != null && result.length() > 0) {
			for (int i= 0; i < fLabelDecorators.length; i++) {
				result= fLabelDecorators[i].decorateText(result, element);
			}
		}			
		return result;
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#dispose
	 */
	public void dispose() {
		if (fLabelDecorators != null) {
			for (int i= 0; i < fLabelDecorators.length; i++) {
				fLabelDecorators[i].dispose();
			}
			fLabelDecorators= null;
		}
		fStorageLabelProvider.dispose();
		fImageLabelProvider.dispose();
	}
	
	/* (non-Javadoc)
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
		if (fLabelDecorators != null) {
			for (int i= 0; i < fLabelDecorators.length; i++) {
				fLabelDecorators[i].addListener(listener);
			}
		}
		super.addListener(listener);	
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return true;	
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
		if (fLabelDecorators != null) {
			for (int i= 0; i < fLabelDecorators.length; i++) {
				fLabelDecorators[i].removeListener(listener);
			}
		}
		super.removeListener(listener);	
	}
	
	public static ILabelDecorator[] getDecorators(boolean errortick, ILabelDecorator extra) {
		if (errortick) {
			if (extra == null) {
				return new ILabelDecorator[] { new ProblemsLabelDecorator(null) };
			} else {
				return new ILabelDecorator[] { new ProblemsLabelDecorator(null), extra };
			}
		}
		if (extra != null) {
			return new ILabelDecorator[] { extra };
		}
		return null;
	}	

}
