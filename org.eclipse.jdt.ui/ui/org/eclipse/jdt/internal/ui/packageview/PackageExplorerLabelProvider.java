/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;


import org.eclipse.core.resources.IStorage;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.MarkerErrorTickProvider;
import org.eclipse.jdt.internal.ui.viewsupport.StorageLabelProvider;

/**
 * Standard label provider for Java elements used by the PackageExplorerPart
 * Use this class when you want to present the Java elements in a viewer.
 * <p>
 * The implementation also handles non-Java elements by forwarding the requests to an 
 * internal <code>WorkbenchLabelProvider</code>.
 * </p>
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @see org.eclipse.ui.model.WorkbenchLabelProvider
 */
class PackageExplorerLabelProvider extends LabelProvider {

	private JavaElementImageProvider fImageLabelProvider;
	private WorkbenchLabelProvider fWorkbenchLabelProvider;	
	private StorageLabelProvider fStorageLabelProvider;

	private int fImageFlags;
	private int fTextFlags;

	/**
	 * Create new JavaElementLabelProvider for the PackageExplorerPart
	 */
	public PackageExplorerLabelProvider() {
		fWorkbenchLabelProvider= new WorkbenchLabelProvider();
		fStorageLabelProvider= new StorageLabelProvider();
		fImageLabelProvider= new JavaElementImageProvider();
		fImageLabelProvider.setErrorTickProvider(new MarkerErrorTickProvider());
		fImageFlags= JavaElementImageProvider.OVERLAY_ICONS | JavaElementImageProvider.SMALL_ICONS;
		fTextFlags= JavaElementLabels.ROOT_VARIABLE | JavaElementLabels.M_PARAMETER_TYPES;
		if (WorkInProgressPreferencePage.isCompressingPkgNameInPackagesView())
			fTextFlags |= JavaElementLabels.P_COMPRESSED;
	}

	void setCompressingPkgNameInPackagesView(boolean state) {
		if (state)
			fTextFlags |= JavaElementLabels.P_COMPRESSED;
		else
			fTextFlags &= ~JavaElementLabels.P_COMPRESSED;
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
