/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.SelectAllAction;
import org.eclipse.jdt.internal.ui.filters.NonJavaElementFilter;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;

public class TypesView extends JavaBrowsingPart {

	private SelectAllAction fSelectAllAction;

	/**
	 * Creates and returns the label provider for this part.
	 * 
	 * @return	the label provider
	 * @see	ILabelProvider
	 */
	protected ILabelProvider createLabelProvider() {
		return new AppearanceAwareLabelProvider(
						AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS,
						AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS,
						new ILabelDecorator[] { new TopLevelTypeProblemsLabelDecorator(null)});
	}

	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		super.addFilters();
		getViewer().addFilter(new NonJavaElementFilter());
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
		else if (element instanceof IType) {
			IType type= (IType)element;
			return type.getDeclaringType() == null && isValidElement(type.getCompilationUnit());
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
			case IJavaElement.TYPE:
				IType type= ((IType)je).getDeclaringType();
				if (type == null)
					type= (IType)je;
				return getSuitableJavaElement(type);
			case IJavaElement.COMPILATION_UNIT:
				return getTypeForCU((ICompilationUnit)je);
			case IJavaElement.CLASS_FILE:
				try {
					return findElementToSelect(((IClassFile)je).getType());
				} catch (JavaModelException ex) {
					return null;
				}
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.IMPORT_DECLARATION:
			case IJavaElement.PACKAGE_DECLARATION:
				return findElementToSelect(je.getParent());
			default:
				if (je instanceof IMember)
					return findElementToSelect(((IMember)je).getDeclaringType());
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
	
	protected void createActions() {
		super.createActions();
		fSelectAllAction= new SelectAllAction((TableViewer)getViewer());
	}

	protected void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		
		// Add selectAll action handlers.
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.SELECT_ALL, fSelectAllAction);
	}
	
	/**
	 * Handles selection of compound element in Packages view.
	 * 
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.uiIWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 * @since 2.1
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!needsToProcessSelectionChanged(part, selection))
			return;

		// above call ensure structured selection
		IStructuredSelection sel= (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object selectedElement= sel.getFirstElement();
			if (selectedElement instanceof CompoundElement){
				IPackageFragment[] fragments= ((CompoundElement)selectedElement).getFragments();
				List selectedElements= Arrays.asList(fragments);
				if (selectedElements.size() > 1)
					adjustInput(part, selectedElements);
				else if (selectedElements.size() == 1)
					super.selectionChanged(part, new StructuredSelection(selectedElements.get(0)));
				else
					Assert.isLegal(false);
				return;
			}				

//			This code would be used once we support multiple selection:			
//			List selectedElements= new ArrayList();		
//			if (sel.size() > 0) {
//				Iterator iter= sel.iterator();
//				while (iter.hasNext()) {
//					Object element= iter.next();
//					if(element instanceof CompoundElement){
//						IPackageFragment[] fragments= ((CompoundElement)element).getFragments();
//						selectedElements.addAll(Arrays.asList(fragments));
//					}
//					else if (element instanceof IPackageFragment) {
//						selectedElements.add(element);
//				}
//				adjustInput(part, selectedElements);
//				return;
//
		}
		super.selectionChanged(part, selection);
	}

	private void adjustInput(IWorkbenchPart part, List selectedElements) {
		Object currentInput= getViewer().getInput();
		if (!selectedElements.equals(currentInput))
			setInput(selectedElements);
	}
}