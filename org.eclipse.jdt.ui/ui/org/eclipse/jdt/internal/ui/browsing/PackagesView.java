/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.jdt.internal.ui.actions.SelectAllAction;
import org.eclipse.jdt.internal.ui.filters.NonJavaElementFilter;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.LibraryFilter;

public class PackagesView extends JavaBrowsingPart {

	private SelectAllAction fSelectAllAction;

	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		super.addFilters();
		getViewer().addFilter(new NonJavaElementFilter());
		getViewer().addFilter(new LibraryFilter());
	}

	protected ILabelProvider createLabelProvider() {
		return new AppearanceAwareLabelProvider(
						AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED,
						AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS,
						AppearanceAwareLabelProvider.getDecorators(true, null)
						);
	}

	/**
	 * Returns the context ID for the Help system
	 * 
	 * @return	the string used as ID for the Help context
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.PACKAGES_BROWSING_VIEW;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	protected boolean isValidInput(Object element) {
		if (element instanceof IJavaProject || (element instanceof IPackageFragmentRoot && ((IJavaElement)element).getElementName() != IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH))
			try {
				IJavaProject jProject= ((IJavaElement)element).getJavaProject();
				if (jProject != null)
					return jProject.getProject().hasNature(JavaCore.NATURE_ID);
			} catch (CoreException ex) {
				return false;
			}
		return false;
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

	protected void createActions() {
		super.createActions();
		fSelectAllAction= new SelectAllAction((TableViewer)getViewer());
	}

	protected void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		
		// Add selectAll action handlers.
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.SELECT_ALL, fSelectAllAction);
	}
}
