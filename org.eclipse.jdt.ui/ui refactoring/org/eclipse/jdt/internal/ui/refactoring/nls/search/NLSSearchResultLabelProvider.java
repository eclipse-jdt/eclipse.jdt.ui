/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.search.ui.ISearchResultViewEntry;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.search.IJavaSearchUIConstants;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class NLSSearchResultLabelProvider extends LabelProvider {

	private JavaElementLabelProvider fImageProvider;
	private JavaElementLabelProvider fTextProvider;
	
	// LRU Cache
	private IMarker fLastMarker;
	private IJavaElement fLastJavaElement;
	
	public static final NLSSearchResultLabelProvider INSTANCE= new NLSSearchResultLabelProvider();

	public NLSSearchResultLabelProvider() {
		fImageProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_OVERLAY_ICONS | JavaElementLabelProvider.SHOW_SMALL_ICONS);
		fTextProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_ROOT | JavaElementLabelProvider.SHOW_QUALIFIED);
	}	

	public String getText(Object o) {
		fLastMarker= null;
		IMarker marker= getMarker(o);
		if (marker == null)
			return ""; //$NON-NLS-1$
		IJavaElement javaElement= getJavaElement(marker);
		if (javaElement == null)
			return marker.getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
		if (javaElement instanceof IImportDeclaration)
			return fTextProvider.getText(((IImportDeclaration)javaElement).getParent().getParent());
		return fTextProvider.getText((IJavaElement)javaElement);
	}

	public Image getImage(Object o) {
		IMarker marker= getMarker(o);
		if (marker == null)
			return null;
		IJavaElement javaElement= getJavaElement(marker);
		if (javaElement == null)
			return fImageProvider.getImage(marker.getResource());
		return fImageProvider.getImage((IJavaElement)javaElement);
	}

	private IMarker getMarker(Object o) {
		if (!(o instanceof ISearchResultViewEntry))
			return null;
		IMarker marker= ((ISearchResultViewEntry)o).getSelectedMarker();
		if (marker == null || !marker.exists())
			return null;
		return marker;
	}
	
	private IJavaElement getJavaElement(IMarker marker) {
		if (fLastMarker != marker) {
			try {
				fLastJavaElement= JavaCore.create((String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));
			} catch (CoreException ex) {
				ExceptionHandler.handle(ex, NLSSearchMessages.getString("Search.Error.createJavaElement.title"), NLSSearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-2$ //$NON-NLS-1$
				fLastJavaElement= null;
			}
			fLastMarker= marker;
		}
		return fLastJavaElement;
	}
}