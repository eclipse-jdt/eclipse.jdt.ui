package org.eclipse.jdt.internal.ui.util;


import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public class SelectionUtil {

	public static List toList(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			return ((IStructuredSelection)selection).toList();
		}
		return null;
	}
	
	/**
	 * Returns the elements of the given selection. 
	 * Returns an empty array if the selection is empty or if 
	 * the given selection is not of type <code>IStructuredSelection</code>.
	 *
	 * @param selection the selection
	 * @return the selected elements
	 *
	 */
	public static Object[] toArray(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return new Object[0];
		}
		return ((IStructuredSelection)selection).toArray();
	}
	
	/**
	 * Returns the elements of the given selection. 
	 * Returns an empty array if the selection is empty or if 
	 * the given selection is not of type <code>IStructuredSelection</code>.
	 *
	 * @param selection the selection
	 * @return the selected elements
	 *
	 */
	public static Object getFirstElement(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}
		return ((IStructuredSelection)selection).getFirstElement();
	}

}