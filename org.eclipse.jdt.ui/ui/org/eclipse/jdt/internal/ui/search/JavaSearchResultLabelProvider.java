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
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.JavaElementLabelProvider;


public class JavaSearchResultLabelProvider extends LabelProvider {

	public static final int SHOW_ELEMENT_CONTAINER= 1; // default
	public static final int SHOW_CONTAINER_ELEMENT= 2;
	public static final int SHOW_PATH= 3;

	private JavaElementLabelProvider fLabelProvider;
	private JavaTextLabelProvider fTextLabelProvider;
	
	// LRU Cache
	private IMarker fLastMarker;
	private IJavaElement fLastJavaElement;
	
	public static final JavaSearchResultLabelProvider INSTANCE= new JavaSearchResultLabelProvider();

	public JavaSearchResultLabelProvider() {
		fLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_OVERLAY_ICONS | JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_CONTAINER | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);
		fTextLabelProvider= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_CONTAINER | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);
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
					return "?: " + fLabelProvider.getText(fLastMarker.getResource()); //$NON-NLS-1$
			}
			else
				return ""; //$NON-NLS-1$
		}
		if (javaElement instanceof IImportDeclaration)
			javaElement= ((IImportDeclaration)javaElement).getParent().getParent();

		if (isAccurate) 
			return fTextLabelProvider.getTextLabel((IJavaElement)javaElement);
		else
			return "?: " + fTextLabelProvider.getTextLabel((IJavaElement)javaElement); //$NON-NLS-1$
	}

	public Image getImage(Object o) {
		IJavaElement javaElement= getJavaElement(o);
		if (javaElement == null)
			return null;
		return fLabelProvider.getImage(javaElement);
	}

	public void setOrder(int orderFlag) {
		if (orderFlag == SHOW_ELEMENT_CONTAINER) {
			fTextLabelProvider.turnOn(JavaElementLabelProvider.SHOW_CONTAINER);
			fTextLabelProvider.turnOff(JavaTextLabelProvider.SHOW_MEMBER_FULLY_QUALIFIED);
			fTextLabelProvider.turnOn(JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION);
			fTextLabelProvider.turnOff(JavaElementLabelProvider.SHOW_ROOT);
		}
		else if (orderFlag == SHOW_CONTAINER_ELEMENT) {
			fTextLabelProvider.turnOff(JavaElementLabelProvider.SHOW_CONTAINER);
			fTextLabelProvider.turnOn(JavaTextLabelProvider.SHOW_MEMBER_FULLY_QUALIFIED);
			fTextLabelProvider.turnOff(JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION);
			fTextLabelProvider.turnOff(JavaElementLabelProvider.SHOW_ROOT);			
		}
		else if (orderFlag == SHOW_PATH) {
			fTextLabelProvider.turnOff(JavaElementLabelProvider.SHOW_CONTAINER);
			fTextLabelProvider.turnOn(JavaTextLabelProvider.SHOW_MEMBER_FULLY_QUALIFIED);
			fTextLabelProvider.turnOff(JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION);
			fTextLabelProvider.turnOn(JavaElementLabelProvider.SHOW_ROOT);
			fTextLabelProvider.turnOff(JavaTextLabelProvider.SHOW_ROOT_POSTFIX);
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