/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.jarpackager.LibraryFilter;
import org.eclipse.jdt.internal.ui.packageview.EmptyInnerPackageFilter;
import org.eclipse.jdt.internal.ui.packageview.EmptyPackageFilter;
import org.eclipse.jdt.internal.ui.viewsupport.ErrorTickImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class TypesView extends JavaBrowsingPart {

	/**
	 * Creates and returns the label provider for this part.
	 * 
	 * @return	the label provider
	 * @see	ILabelProvider
	 */
	protected ILabelProvider createLabelProvider() {
		return new JavaUILabelProvider(
						JavaElementLabels.ROOT_VARIABLE | JavaElementLabels.M_PARAMETER_TYPES,
						JavaElementImageProvider.OVERLAY_ICONS,
						new ErrorTickImageProvider());
	}

	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		getViewer().addFilter(new EmptyInnerPackageFilter());
		getViewer().addFilter(new EmptyPackageFilter());
		getViewer().addFilter(new NonJavaElementFilter());
		getViewer().addFilter(new LibraryFilter());
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	protected boolean isValidInput(Object element) {
		return element instanceof IPackageFragment;
		//|| element instanceof ICompilationUnit || element instanceof IClassFile;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * element for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid element
	 */
	protected boolean isValidElement(Object element) {
		if (element instanceof ICompilationUnit)
			return super.isValidElement(((ICompilationUnit)element).getParent());
		else if (element instanceof IType)
			return isValidElement(((IType)element).getCompilationUnit());
		return false;
	}

	/**
	 * Finds the element which has to be selected in this part.
	 * 
	 * @param je	the Java element which has the focus
	 */
	protected IJavaElement findElementToSelect(IJavaElement je) {
		if (je == null)
			return null;

		switch (je.getElementType()) {
			case IJavaElement.TYPE:
				IType type= ((IType)je).getDeclaringType();
				if (type == null)
					type= (IType)je;
				return getSuitableJavaElement(type);
			case IJavaElement.COMPILATION_UNIT:
				try {
					IType[] types= ((ICompilationUnit)getSuitableJavaElement(je)).getTypes();
					if (types.length > 0)
						return types[0];
					else
						return null;
				} catch (JavaModelException ex) {
					return null;
				}
			case IJavaElement.CLASS_FILE:
				try {
					return ((IClassFile)je).getType();
				} catch (JavaModelException ex) {
					return null;
				}
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.IMPORT_DECLARATION:
			case IJavaElement.PACKAGE_DECLARATION:
				return findElementToSelect(je.getParent());
			default:
				if (je instanceof IMember) {
					je= ((IMember)je).getDeclaringType();
					return getSuitableJavaElement(je);
				}
				return null; 
		}
	}

	/**
	 * Returns the context ID for the Help system
	 * 
	 * @return	the string used as ID for the Help context
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.TYPES_VIEW;
	}
}
