/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
// AW
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * A group context can be used to access the current selection,
 * the current input element and the common super type, if the
 * selection contains multiple elements.
 */
public class GroupContext {

	private IInputSelectionProvider fProvider;
	private Class fCommonSuperType;

	/**
	 * Creates a new <code>ContributorContext</code> based on the
	 * given selection provider and input element.
	 */
	public GroupContext(IInputSelectionProvider provider) {
		fProvider= provider;
		Assert.isNotNull(fProvider);
	} 
	 
	/**
	 * Returns the current selection.
	 */
	public ISelection getSelection() {
		return fProvider.getSelection();
	}
	
	/**
	 * Returns the current input element.
	 */
	public Object getInput() {
		return fProvider.getInput();
	}
	
	/**
	 * Returns the selection provider, or <code>null</code> if no
	 * selection provider is available.
	 */
	public ISelectionProvider getSelectionProvider() {
		return fProvider;
	} 
	
	/**
	 * Returns the common super type if the selection contains
	 * multiple elements.
	 */
	public Class getCommonSuperType() {
		if (fCommonSuperType != null)
			return fCommonSuperType;
			
		ISelection s= getSelection();
		if (s == null || s.isEmpty() || ! (s instanceof StructuredSelection))
			return null;
		
		StructuredSelection selection= (StructuredSelection)s;
		Iterator iter= selection.iterator();
		Object current= iter.next();
		fCommonSuperType= current.getClass();
		while (iter.hasNext()) {
			Object o= iter.next();
			Class clazz= o.getClass();
			
			if (clazz.equals(fCommonSuperType))
				continue;
				
			if (clazz.isInstance(current)) {
				fCommonSuperType= clazz;
				current= o;
				continue;
			}
			
			findCommonSuperType(o);
			current= o;
		}
		return fCommonSuperType;
	}
	
	private void findCommonSuperType(Object o) {
		while (fCommonSuperType != null) {
			if (fCommonSuperType.isInstance(o))
				break;
			fCommonSuperType= fCommonSuperType.getSuperclass();
		}
		Assert.isTrue(fCommonSuperType != null);
	}	 
}