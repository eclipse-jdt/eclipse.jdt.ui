/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public class SelectionUtil {

	public static List toList(ISelection selection) {
		if (selection instanceof IStructuredSelection) 
			return ((IStructuredSelection)selection).toList();
		return null;
	}

	/**
	 * Returns the selected element if the selection
	 * consists of a single element only. 
	 *
	 * @param selection the selection
	 * @return the selected first element or null
	 *
	 */
	public static Object getSingleElement(ISelection s) {
		if (!(s instanceof IStructuredSelection))
			return null;
		IStructuredSelection selection= (IStructuredSelection)s;
		if (selection.size() != 1)
			return null;
	
		return selection.getFirstElement();		
	}
}