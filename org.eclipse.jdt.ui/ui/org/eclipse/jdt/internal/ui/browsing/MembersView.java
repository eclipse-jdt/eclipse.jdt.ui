/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.jarpackager.LibraryFilter;
import org.eclipse.jdt.internal.ui.packageview.EmptyInnerPackageFilter;
import org.eclipse.jdt.internal.ui.packageview.EmptyPackageFilter;
import org.eclipse.jdt.internal.ui.typehierarchy.MethodsViewerFilter;
import org.eclipse.jdt.internal.ui.viewsupport.ErrorTickImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class MembersView extends JavaBrowsingPart {

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
	 * Returns the context ID for the Help system
	 * 
	 * @return	the string used as ID for the Help context
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.MEMBERS_VIEW;
	}

	/**
	 * Creates the the viewer of this part.
	 * 
	 * @param parent	the parent for the viewer
	 */
	protected StructuredViewer createViewer(Composite parent) {
		return new ProblemTreeViewer(parent, SWT.MULTI);
	}

	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		getViewer().addFilter(new MethodsViewerFilter());
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	protected boolean isValidInput(Object element) {
		if (element instanceof IType) {
			IType type= (IType)element;
			return type.isBinary() || type.getDeclaringType() == null;
		}
		return false;
	}
	
	/**
	 * Answers if the given <code>element</code> is a valid
	 * element for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid element
	 */
	protected boolean isValidElement(Object element) {
		if (element instanceof IMember)
			return super.isValidElement(((IMember)element).getDeclaringType());
		else if (element instanceof IImportContainer || element instanceof IImportDeclaration) {
			IJavaElement parent= ((IJavaElement)element).getParent();
			if (parent.getElementType() == IJavaElement.CLASS_FILE) {
				IType type;
				try {
					type= ((IClassFile)parent).getType();
				} catch (JavaModelException ex) {
					return false;
				}
				return isValidElement(type);
			}
			else if (parent.getElementType() == IJavaElement.COMPILATION_UNIT) {
				IType[] types;
				try {
					types= ((ICompilationUnit)parent).getAllTypes();
				} catch (JavaModelException ex) {
					return false;
				}
				for (int i= 0; i < types.length; i++) {
					boolean result= isValidElement(types[i]);
					if (result)
						return true;
				}
			}
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
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
			case IJavaElement.TYPE:
			case IJavaElement.PACKAGE_DECLARATION:
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.IMPORT_DECLARATION:
				je= getSuitableJavaElement(je);
				if (je != null)
					return je;
			default:
				return null; 
		}
	}

	/**
	 * Finds the closest Java element which can be used as input for
	 * this part and has the given Java element as child
	 * 
	 * @param 	je 	the Java element for which to search the closest input
	 * @return	the closest Java element used as input for this part
	 */
	protected IJavaElement findInputForJavaElement(IJavaElement je) {
		if (je == null)
			return null;
			
		switch (je.getElementType()) {
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
			case IJavaElement.TYPE:
				IType type= ((IMember)je).getDeclaringType();
				if (type == null)
					return je;
				else
					return findInputForJavaElement(type);
			case IJavaElement.IMPORT_DECLARATION:
				return findInputForJavaElement(je.getParent());
			case IJavaElement.PACKAGE_DECLARATION:
			case IJavaElement.IMPORT_CONTAINER:
				IJavaElement parent= je.getParent();
				if (parent instanceof ICompilationUnit) {
					IType[] types;
					try {
						types= ((ICompilationUnit)parent).getAllTypes();
					} catch (JavaModelException ex) {
						return null;
					}
					if (types.length > 0)
						return types[0];
					else
						return null;
				}
				else if (parent instanceof IClassFile)
					try {
						return ((IClassFile)parent).getType();
					} catch (JavaModelException ex) {
						// no input
					}
				return null;
		}
		return super.findInputForJavaElement(je);
	}
}
