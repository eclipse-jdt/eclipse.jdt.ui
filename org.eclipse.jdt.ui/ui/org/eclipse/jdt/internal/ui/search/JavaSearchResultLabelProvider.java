/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.search.ui.ISearchResultViewEntry;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;


public class JavaSearchResultLabelProvider extends LabelProvider {

	public static final int SHOW_ELEMENT_CONTAINER= 1; // default
	public static final int SHOW_CONTAINER_ELEMENT= 2;
	public static final int SHOW_PATH= 3;
	public static final String POTENTIAL_MATCH= SearchMessages.getString("JavaSearchResultLabelProvider.potentialMatch"); //$NON-NLS-1$

	private JavaElementLabelProvider fLabelProvider;
	private int fTextFlags= 0;
	
	// Cache
	private IMarker fLastMarker;
	private IJavaElement fLastJavaElement;
	private StringBuffer fBufffer= new StringBuffer(50);
	
	public static final JavaSearchResultLabelProvider INSTANCE= new JavaSearchResultLabelProvider();

	public JavaSearchResultLabelProvider() {
		fLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_OVERLAY_ICONS | JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_ROOT | JavaElementLabelProvider.SHOW_QUALIFIED);
	}	

	public String getText(Object o) {
		fLastMarker= null;
		IJavaElement javaElement= getJavaElement(o); // sets fLastMarker as side effect
		boolean isAccurate= true;
		if (fLastMarker != null && (fLastMarker.getAttribute(IJavaSearchUIConstants.ATT_ACCURACY, -1) == IJavaSearchResultCollector.POTENTIAL_MATCH))
			isAccurate= false;
		if (javaElement == null) {
			if (fLastMarker != null) {
				if (isAccurate) 
					return fLabelProvider.getText(fLastMarker.getResource());
				else
					return fLabelProvider.getText(fLastMarker.getResource()) + POTENTIAL_MATCH;
			}
			else
				return ""; //$NON-NLS-1$
		}
		if (javaElement instanceof IImportDeclaration)
			javaElement= ((IImportDeclaration)javaElement).getParent().getParent();

		fBufffer.setLength(0);
		JavaElementLabels.getElementLabel(javaElement, fTextFlags, fBufffer);
		if (!isAccurate) 
			fBufffer.append(POTENTIAL_MATCH);
		return fBufffer.toString();
	}

	public Image getImage(Object o) {
		IJavaElement javaElement= getJavaElement(o);
		if (javaElement == null)
			return null;
		return fLabelProvider.getImage(javaElement);
	}

	public void setOrder(int orderFlag) {
		if (orderFlag == SHOW_ELEMENT_CONTAINER)
			fTextFlags = JavaElementLabels.F_POST_QUALIFIED | JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.I_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
							| JavaElementLabels.T_POST_QUALIFIED | JavaElementLabels.D_POST_QUALIFIED | JavaElementLabels.CF_POST_QUALIFIED  | JavaElementLabels.CU_POST_QUALIFIED;
			
		else if (orderFlag == SHOW_CONTAINER_ELEMENT)
			fTextFlags= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
		else if (orderFlag == SHOW_PATH) {
			fTextFlags= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
			fTextFlags |= JavaElementLabels.PREPEND_ROOT_PATH;
		}
	}

	private IJavaElement getJavaElement(Object o) {
		if (o instanceof IJavaElement)
			return (IJavaElement)o;
		if (!(o instanceof ISearchResultViewEntry))
			return null;
		IMarker marker= getMarker(o);
		if (marker == null || !marker.exists())
			return null;
		return getJavaElement(marker);
	}

	private IMarker getMarker(Object o) {
		if (!(o instanceof ISearchResultViewEntry))
			return null;
		return ((ISearchResultViewEntry)o).getSelectedMarker();
	}
	
	private IJavaElement getJavaElement(IMarker marker) {
		if (fLastMarker != marker) {
			String handle;
			try {
				handle= (String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID);
			} catch (CoreException ex) {
				ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.javaElementAccess.title"), SearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
				handle= null;
			}
			
			if (handle != null)	
				fLastJavaElement= JavaCore.create(handle);
			else
				fLastJavaElement= null;
			fLastMarker= marker;
		}
		return fLastJavaElement;
	}
	
	private boolean handleContainsWrongCU(String handle, String resourceName) {
		int start= handle.indexOf('{');
		int end= handle.indexOf(".java"); //$NON-NLS-1$
		if (start >= end || start == -1)
			return false;
		String name= handle.substring(start + 1, end + 5);
		return !name.equals(resourceName);
	}
	
	private String computeFixedHandle(String handle, String resourceName) {
		int start= handle.indexOf('{');
		int end= handle.indexOf(".java"); //$NON-NLS-1$
		handle= handle.substring(0, start + 1) + resourceName + handle.substring(end + 5);
		return handle;
	}
}