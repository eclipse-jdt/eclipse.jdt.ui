/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.search.ui.ISearchResultViewEntry;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.JavaElementLabelProvider;


public class JavaSearchResultLabelProvider extends LabelProvider {

	private JavaElementLabelProvider fLabelProvider;
	
	// LRU Cache
	private IMarker fLastMarker;
	private IJavaElement fLastJavaElement;
	
	public static final JavaSearchResultLabelProvider INSTANCE= new JavaSearchResultLabelProvider();

	public JavaSearchResultLabelProvider() {
		fLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_OVERLAY_ICONS | JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_CONTAINER | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);
	}	

	public String getText(Object o) {
		fLastMarker= null;
		IJavaElement javaElement= getJavaElement(o);
		if (javaElement == null)
			return ""; //$NON-NLS-1$
		if (javaElement instanceof IImportDeclaration)
			return fLabelProvider.getText(((IImportDeclaration)javaElement).getParent().getParent());
		return fLabelProvider.getText((IJavaElement)javaElement);
	}

	public Image getImage(Object o) {
		IJavaElement javaElement= getJavaElement(o);
		if (javaElement == null)
			return null;
		return fLabelProvider.getImage((IJavaElement)javaElement);
	}

	private IJavaElement getJavaElement(Object o) {
		if (!(o instanceof ISearchResultViewEntry))
			return null;
		IMarker marker= ((ISearchResultViewEntry)o).getSelectedMarker();
		if (marker == null || !marker.exists())
			return null;
		return getJavaElement(marker);
	}
	
	private IJavaElement getJavaElement(IMarker marker) {
		if (fLastMarker != marker) {
			try {
				fLastJavaElement= JavaCore.create((String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));
			} catch (CoreException ex) {
				ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.createJavaElement.title"), SearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-2$ //$NON-NLS-1$
				fLastJavaElement= null;
			}
			fLastMarker= marker;
		}
		return fLastJavaElement;
	}
}