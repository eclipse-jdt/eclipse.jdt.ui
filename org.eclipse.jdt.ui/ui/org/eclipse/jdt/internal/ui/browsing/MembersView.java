/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.action.IToolBarManager;
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

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.jdt.internal.ui.typehierarchy.MethodsViewerFilter;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyMessages;
import org.eclipse.jdt.internal.ui.viewsupport.ErrorTickImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;

public class MembersView extends JavaBrowsingPart {
	
	private MembersFilterAction fFilterActions[];
	private MethodsViewerFilter fMemberFilter;

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
		// fields
		String title= TypeHierarchyMessages.getString("MethodsViewer.hide_fields.label"); //$NON-NLS-1$
		String helpContext= IJavaHelpContextIds.FILTER_FIELDS_ACTION;
		MembersFilterAction hideFields= new MembersFilterAction(this, title, MethodsViewerFilter.FILTER_FIELDS, helpContext, false);
		hideFields.setDescription(TypeHierarchyMessages.getString("MethodsViewer.hide_fields.description")); //$NON-NLS-1$
		hideFields.setToolTipChecked(TypeHierarchyMessages.getString("MethodsViewer.hide_fields.tooltip.checked")); //$NON-NLS-1$
		hideFields.setToolTipUnchecked(TypeHierarchyMessages.getString("MethodsViewer.hide_fields.tooltip.unchecked")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideFields, "fields_co.gif"); //$NON-NLS-1$
		
		// static
		title= TypeHierarchyMessages.getString("MethodsViewer.hide_static.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_STATIC_ACTION;
		MembersFilterAction hideStatic= new MembersFilterAction(this, title, MethodsViewerFilter.FILTER_STATIC, helpContext, false);
		hideStatic.setDescription(TypeHierarchyMessages.getString("MethodsViewer.hide_static.description")); //$NON-NLS-1$
		hideStatic.setToolTipChecked(TypeHierarchyMessages.getString("MethodsViewer.hide_static.tooltip.checked")); //$NON-NLS-1$
		hideStatic.setToolTipUnchecked(TypeHierarchyMessages.getString("MethodsViewer.hide_static.tooltip.unchecked")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideStatic, "static_co.gif"); //$NON-NLS-1$
		
		// non-public
		title= TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_PUBLIC_ACTION;
		MembersFilterAction hideNonPublic= new MembersFilterAction(this, title, MethodsViewerFilter.FILTER_NONPUBLIC, helpContext, false);
		hideNonPublic.setDescription(TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.description")); //$NON-NLS-1$
		hideNonPublic.setToolTipChecked(TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.tooltip.checked")); //$NON-NLS-1$
		hideNonPublic.setToolTipUnchecked(TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.tooltip.unchecked")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideNonPublic, "public_co.gif"); //$NON-NLS-1$
		
		// order corresponds to order in toolbar
		fFilterActions= new MembersFilterAction[] { hideFields, hideStatic, hideNonPublic };
		
		return new ProblemTreeViewer(parent, SWT.MULTI);
	}

	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		fMemberFilter= new MethodsViewerFilter();
		getViewer().addFilter(fMemberFilter);
	}

	protected void fillToolBar(IToolBarManager tbm) {
		for (int i= 0; i < fFilterActions.length; i++)	
			tbm.add(fFilterActions[i]);
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
	
	/**
	 * Filters the members list
	 */	
	public void setMemberFilter(int filterProperty, boolean set) {
		if (set) {
			fMemberFilter.addFilter(filterProperty);
		} else {
			fMemberFilter.removeFilter(filterProperty);
		}
		for (int i= 0; i < fFilterActions.length; i++) {
			if (((MembersFilterAction)fFilterActions[i]).getFilterProperty() == filterProperty) {
				fFilterActions[i].setChecked(set);
			}
		}
		getViewer().refresh();
	}
	
}
