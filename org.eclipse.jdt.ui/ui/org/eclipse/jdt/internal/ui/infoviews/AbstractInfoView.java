/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.infoviews;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.search.SearchUtil;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Abstract class for views which show information for a given element.
 * 
 * @since 3.0
 */
abstract class AbstractInfoView extends ViewPart implements ISelectionListener {


	/** JavaElementLabels flags used for the title */
	private static final int TITLE_LABEL_FLAGS= 0;
	/** JavaElementLabels flags used for the tool tip text */
	private static final int TOOLTIP_LABEL_FLAGS= JavaElementLabels.DEFAULT_QUALIFIED | JavaElementLabels.ROOT_POST_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH |
			JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.M_EXCEPTIONS | 
			JavaElementLabels.F_APP_TYPE_SIGNATURE;

	/** The current input. */
	protected IJavaElement fCurrentInput;

	/*
	 * @see IPartListener2
	 */
	private IPartListener2 fPartListener= new IPartListener2() {
		public void partVisible(IWorkbenchPartReference ref) {
			if (ref.getId().equals(getSite().getId())) {
				IWorkbenchPart activePart= ref.getPage().getActivePart();
				if (activePart != null)
					selectionChanged(activePart, ref.getPage().getSelection());
				startListeningForSelectionChanges();
			}
		}
		public void partHidden(IWorkbenchPartReference ref) {
			if (ref.getId().equals(getSite().getId()))
				stopListeningForSelectionChanges();
		}
		public void partInputChanged(IWorkbenchPartReference ref) {
			if (!ref.getId().equals(getSite().getId()))
				setInputFrom(ref.getPart(false));
		}
		public void partActivated(IWorkbenchPartReference ref) {
		}
		public void partBroughtToTop(IWorkbenchPartReference ref) {
		}
		public void partClosed(IWorkbenchPartReference ref) {
		}
		public void partDeactivated(IWorkbenchPartReference ref) {
		}
		public void partOpened(IWorkbenchPartReference ref) {
		}
	};


	/**
	 * Set the input of this view.
	 *  
	 * @param input the input object
	 * @return	<code>true</code> if the input was set successfully 
	 */
	abstract protected boolean setInput(Object input);

	/**
	 * Create the part control.
	 * 
 	 * @param parent the parent control
	 * @see IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	abstract protected void internalCreatePartControl(Composite parent);

	/**
	 * Set the view's foreground color.
	 * 
	 * @param color the SWT color
	 */
	abstract protected void setForeground(Color color);

	/**
	 * Set the view's background color.
	 * 
	 * @param color the SWT color
	 */
	abstract protected void setBackground(Color color);

	/*
	 * @see IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public final void createPartControl(Composite parent) {
		internalCreatePartControl(parent);
		setInfoColor();
		getSite().getWorkbenchWindow().getPartService().addPartListener(fPartListener);
	}
	
	/**
	 * Sets the foreground and background color to the corresponding SWT info color.
	 */
	private void setInfoColor() {
		if (getSite().getShell().isDisposed())
			return;
		
		Display display= getSite().getShell().getDisplay();
		if (display == null || display.isDisposed())
			return;

		setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	}
	
	/**
	 * Start to listen for selection changes.
	 */
	protected void startListeningForSelectionChanges() {
		getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(this);
	}

	/**
	 * Stop to listen for selection changes.
	 */
	protected void stopListeningForSelectionChanges() {
		getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(this);
	}

	/*
	 * @see ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part.equals(this))
			return;

		setInputFrom(part);
	}

	/**
	 * Finds and returns the Java element selected in the given part.
	 * 
	 * @param part the workbench part for which to find the selected Java element
	 * @return the selected Java element
	 */
	private IJavaElement findSelectedJavaElement(IWorkbenchPart part) {
		Object element;
		try {
			IStructuredSelection sel= SelectionConverter.getStructuredSelection(part);
			element= SelectionUtil.getSingleElement(sel);
		} catch (JavaModelException e) {
			return null;
		}
			
		return findJavaElement(element);
	}

	private static IJavaElement findJavaElement(Object element) {

		if (SearchUtil.isISearchResultViewEntry(element)) {
			IJavaElement je= SearchUtil.getJavaElement(element);
			if (je != null)
				return je;
			element= SearchUtil.getResource(element);
		}
	
		IJavaElement je= null;
		if (element instanceof IAdaptable)
			je= (IJavaElement)((IAdaptable)element).getAdapter(IJavaElement.class);
			
		if (je != null && je.getElementType() == IJavaElement.COMPILATION_UNIT)
			je= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)je);
			
		return je;
	}


	/**
	 * Finds and returns the type for the given CU.
	 * 
	 * @param cu	the compilation unit
	 * @return	the type with same name as the given CU or the first type in the CU 
	 */
	protected IType getTypeForCU(ICompilationUnit cu) {
		
		if (cu == null || !cu.exists())
			return null;
		
		cu= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		
		// Use primary type if possible
		IType primaryType= cu.findPrimaryType();
		if (primaryType != null)
			return primaryType;

		// Use first top-level type
		try {
			IType[] types= cu.getTypes();
			if (types.length > 0)
				return types[0];
			else
				return null;
		} catch (JavaModelException ex) {
			return null;
		}
	}	

	/**
	 * Sets this view's input based on the selection in the given part.
	 * 
	 * @param part the part from which to get the selected Java element
	 */
	private void setInputFrom(IWorkbenchPart part) {
		IJavaElement je= findSelectedJavaElement(part);
		
		if (fCurrentInput != null && fCurrentInput.equals(je))
			return;
		
		if (!setInput(je))
			return;

		fCurrentInput= je;
		
		setTitle(getSite().getRegisteredName() + " (" + JavaElementLabels.getElementLabel(je, TITLE_LABEL_FLAGS) + ")");  //$NON-NLS-1$//$NON-NLS-2$
		setTitleToolTip(JavaElementLabels.getElementLabel(je, TOOLTIP_LABEL_FLAGS));  //$NON-NLS-1$//$NON-NLS-2$
	}

	/*
	 * @see IWorkbenchPart#dispose()
	 */
	final public void dispose() {
		getSite().getWorkbenchWindow().getPartService().removePartListener(fPartListener);
		internalDispose();
	}

	/*
	 * @see IWorkbenchPart#dispose()
	 */
	protected void internalDispose() {
	}
}
