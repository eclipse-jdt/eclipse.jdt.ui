/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.DecoratingLabelProvider;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import org.eclipse.search.ui.ISearchResultViewEntry;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.StandardJavaUILabelProvider;


class JavaSearchResultLabelProvider extends DecoratingLabelProvider {
	public static final int SHOW_ELEMENT_CONTAINER= 1; // default
	public static final int SHOW_CONTAINER_ELEMENT= 2;
	public static final int SHOW_PATH= 3;
	public static final String POTENTIAL_MATCH= SearchMessages.getString("JavaSearchResultLabelProvider.potentialMatch"); //$NON-NLS-1$
	
	// Cache
	private IMarker fLastMarker;
	private IJavaElement fLastJavaElement;
	private StringBuffer fBufffer= new StringBuffer(50);
	

	JavaSearchResultLabelProvider() {
		super(
			new StandardJavaUILabelProvider(
				StandardJavaUILabelProvider.DEFAULT_TEXTFLAGS,
				StandardJavaUILabelProvider.DEFAULT_IMAGEFLAGS,
				null)
			, null);
		setLabelDecorator(PlatformUI.getWorkbench().getDecoratorManager());
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
		return SearchUtil.getJavaElement(marker);
	}

	private IMarker getMarker(Object o) {
		if (!(o instanceof ISearchResultViewEntry))
			return null;
		return ((ISearchResultViewEntry)o).getSelectedMarker();
	}
	
	private IJavaElement getJavaElement(IMarker marker) {
		if (fLastMarker != marker) {
			try {
				fLastJavaElement= (IJavaElement)marker.getAttribute("je");
			} catch (CoreException ex) {
				fLastJavaElement= null;
			}
//			String handle;
//			try {
//				handle= (String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID);
//			} catch (CoreException ex) {
//				ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.javaElementAccess.title"), SearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
//				handle= null;
//			}
//			
//			if (handle != null) {
//				fLastJavaElement= JavaCore.create(handle);
//				if (marker.getAttribute("isWorkingCopy", false)) {
//					ICompilationUnit cu= getCompilationUnit(fLastJavaElement);
//					// Find working copy element
//					IWorkingCopy[] workingCopies= getWorkingCopies();
//					int i= 0;
//					while (i < workingCopies.length) {
//						if (workingCopies[i].getOriginalElement().equals(cu)) {
//							try {
//								fLastJavaElement= findInWorkingCopy(workingCopies[i], fLastJavaElement, true);
//								if (fLastJavaElement != null && !fLastJavaElement.exists())
//									fLastJavaElement= cu.getElementAt(marker.getAttribute(IMarker.CHAR_START, 0));
//							} catch(JavaModelException ex) {
//								fLastJavaElement= null;
//							}
//							break;
//						}
//						i++;
//					}
//				}
//			}
//			else
//				fLastJavaElement= null;
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

	private IWorkingCopy[] getWorkingCopies() {
		IWorkingCopyManager wcManager= JavaPlugin.getDefault().getWorkingCopyManager();
		IEditorPart[] editorParts= JavaPlugin.getDefault().getDirtyEditors();
		ArrayList workingCopies= new ArrayList(editorParts.length);
		for (int i= 0; i < editorParts.length; i++) {
			IWorkingCopy workingCopy= wcManager.getWorkingCopy(editorParts[i].getEditorInput());
			if (workingCopy != null)
				workingCopies.add(workingCopy);
		}
		return (IWorkingCopy[])workingCopies.toArray(new IWorkingCopy[workingCopies.size()]);
	}

	/** 
	 * Returns the working copy of the given java element.
	 * @param javaElement the javaElement for which the working copyshould be found
	 * @param reconcile indicates whether the working copy must be reconcile prior to searching it
	 * @return the working copy of the given element or <code>null</code> if none
	 */	
	public IJavaElement findInWorkingCopy(IWorkingCopy workingCopy, IJavaElement element, boolean reconcile) throws JavaModelException {
		if (workingCopy != null) {
			if (reconcile) {
				synchronized (workingCopy) {
					workingCopy.reconcile(null);
					return SearchUtil.findInCompilationUnit((ICompilationUnit)workingCopy, element);
				}
			} else {
					return SearchUtil.findInCompilationUnit((ICompilationUnit)workingCopy, element);
			}
		}
		return null;
	}

	
	/**
	 * Returns the compilation unit for the given java element.
	 * 
	 * @param	element the java element whose compilation unit is searched for
	 * @return	the compilation unit of the given java element
	 */
	private ICompilationUnit getCompilationUnit(IJavaElement element) {
		if (element == null)
			return null;
			
		if (element instanceof IMember)
			return ((IMember) element).getCompilationUnit();
		
		int type= element.getElementType();
		if (IJavaElement.COMPILATION_UNIT == type)
			return (ICompilationUnit) element;
		if (IJavaElement.CLASS_FILE == type)
			return null;
			
		return getCompilationUnit(element.getParent());
	}
}