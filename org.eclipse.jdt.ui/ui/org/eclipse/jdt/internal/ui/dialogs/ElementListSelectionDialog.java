/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;

/**
 * A class to select one or more elements out of an indexed property
 */
public class ElementListSelectionDialog extends AbstractElementListSelectionDialog {
	
	private List fElements;
	
	/**
	 * Constructs a list selection dialog.
	 * @param renderer The label renderer used
	 * @param ignoreCase Decides if the match string ignores lower/upppr case
	 * @param multipleSelection Allow multiple selection	 
	 */	
	public ElementListSelectionDialog(Shell parent,	ILabelProvider renderer)
	{
		super(parent, renderer);
	}

	/**
	 * Sets the elements presented by this dialog.
	 */
	public void setElements(List elements) {
		fElements= elements;	
	}

	/**
	 * Sets the elements presented by this dialog.
	 * Convenience method.
	 */
	public void setElements(Object[] elements) {
		fElements= Arrays.asList(elements);
	}
	
	/*
	 * @private
	 */
	protected void computeResult() {
		setResult(getWidgetSelection());
	}

	/*
	 * @private
	 */	
	protected Control createDialogArea(Composite parent) {
		Control result= super.createDialogArea(parent);
		
		setSelectionListElements(fElements, false);
      	//a little trick to make the window come up faster
      	String initialFilter= null;
      	if (getPrimaryInitialSelection() instanceof String)
			initialFilter= (String)getPrimaryInitialSelection();
      	if (initialFilter != null)
      		setFilter(initialFilter, true);
      	else
      		refilter();
      				
		return result;
	}
	
	public Object[] getSelectedElements() {
		return getResult();
	}
	
	public Object getSelectedElement() {
		return getPrimaryResult();
	}	
}