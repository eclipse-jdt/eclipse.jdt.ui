/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.jarpackager.LibraryFilter;
import org.eclipse.jdt.internal.ui.packageview.EmptyInnerPackageFilter;
import org.eclipse.jdt.internal.ui.packageview.EmptyPackageFilter;
import org.eclipse.jdt.internal.ui.packageview.JavaElementPatternFilter;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class PackagesView extends JavaBrowsingPart {

	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		getViewer().addFilter(new EmptyInnerPackageFilter());
		getViewer().addFilter(new NonJavaElementFilter());
		getViewer().addFilter(new LibraryFilter());
		JavaElementPatternFilter patternFilter= new JavaElementPatternFilter();
		patternFilter.setPatterns(new String[] {"CVS", "*.CVS"});
		getViewer().addFilter(patternFilter);
	}

	/**
	 * Returns the context ID for the Help system
	 * 
	 * @return	the string used as ID for the Help context
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.PACKAGES_VIEW;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	protected boolean isValidInput(Object element) {
		return element instanceof IJavaProject
			|| (element instanceof IPackageFragmentRoot && ((IJavaElement)element).getElementName() != IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH);
	}

	/*
	 * Gets suitable input.
	 */

	/**
	 * Answers if the given <code>element</code> is a valid
	 * element for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid element
	 */
	protected boolean isValidElement(Object element) {
		if (element instanceof IPackageFragment) {
			IJavaElement parent= ((IPackageFragment)element).getParent();
			if (parent != null)
				return super.isValidElement(parent) || super.isValidElement(parent.getJavaProject());
		}
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
			case IJavaElement.PACKAGE_FRAGMENT:
				return je;
			case IJavaElement.COMPILATION_UNIT:
				return ((ICompilationUnit)je).getParent();
			case IJavaElement.CLASS_FILE:
				return ((IClassFile)je).getParent();
			case IJavaElement.TYPE:
				return ((IType)je).getPackageFragment();
			default:
				return findElementToSelect(je.getParent());
		}
	}
}
