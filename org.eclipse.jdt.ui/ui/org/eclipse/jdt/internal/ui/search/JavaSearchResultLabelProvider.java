/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

import org.eclipse.search.ui.ISearchResultViewEntry;

import org.eclipse.search.internal.ui.SearchPlugin;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.StandardJavaUILabelProvider;


public class JavaSearchResultLabelProvider extends DecoratingLabelProvider {
	public static final int SHOW_ELEMENT_CONTAINER= 1; // default
	public static final int SHOW_CONTAINER_ELEMENT= 2;
	public static final int SHOW_PATH= 3;
	public static final String POTENTIAL_MATCH= SearchMessages.getString("JavaSearchResultLabelProvider.potentialMatch"); //$NON-NLS-1$
	private int fTextFlags= 0;
	
	// Cache
	private IMarker fLastMarker;
	private IJavaElement fLastJavaElement;
	private StringBuffer fBufffer= new StringBuffer(50);
	
	public static final JavaSearchResultLabelProvider INSTANCE= new JavaSearchResultLabelProvider();
	public JavaSearchResultLabelProvider() {
		super(getJavaElementLabelProvider(), getDecoratorManager());
	}	

	private static ILabelDecorator getDecoratorManager() {
		if (getSite() != null)
			return getSite().getDecoratorManager();
		else
			return null;
	}
	
	private static ILabelProvider getJavaElementLabelProvider() {	
		return
			new StandardJavaUILabelProvider(
				StandardJavaUILabelProvider.DEFAULT_TEXTFLAGS,
				StandardJavaUILabelProvider.DEFAULT_IMAGEFLAGS,
				null
			);
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
					return super.getText(fLastMarker.getResource());
				else
					return super.getText(fLastMarker.getResource()) + POTENTIAL_MATCH;
			}
			else
				return ""; //$NON-NLS-1$
		}
		if (javaElement instanceof IImportDeclaration)
			javaElement= ((IImportDeclaration)javaElement).getParent().getParent();
		fBufffer.setLength(0);
		if (!isAccurate) 
			return super.getText(javaElement) + POTENTIAL_MATCH;
		else
			return super.getText(javaElement);
	}
	public Image getImage(Object o) {
		IJavaElement javaElement= getJavaElement(o);
		if (javaElement == null)
			return null;
		return super.getImage(javaElement);
	}
	public void setOrder(int orderFlag) {
		int flags= StandardJavaUILabelProvider.DEFAULT_TEXTFLAGS;
		if (orderFlag == SHOW_ELEMENT_CONTAINER)
			flags |= JavaElementLabels.F_POST_QUALIFIED | JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.I_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
							| JavaElementLabels.T_POST_QUALIFIED | JavaElementLabels.D_POST_QUALIFIED | JavaElementLabels.CF_POST_QUALIFIED  | JavaElementLabels.CU_POST_QUALIFIED;
			
		else if (orderFlag == SHOW_CONTAINER_ELEMENT)
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
		else if (orderFlag == SHOW_PATH) {
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
			flags |= JavaElementLabels.PREPEND_ROOT_PATH;
		}
		((StandardJavaUILabelProvider)getLabelProvider()).setTextFlags(flags);
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

	private static IWorkbenchPartSite getSite() {
		IWorkbenchPage page= SearchPlugin.getActivePage();
		if (page != null) {
			IWorkbenchPart part= page.getActivePart();
			if (part != null)
				return part.getSite();
		}
		return null;
	}		
}