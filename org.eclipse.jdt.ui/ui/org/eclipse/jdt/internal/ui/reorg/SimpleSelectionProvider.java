package org.eclipse.jdt.internal.ui.reorg;

import java.util.Collection;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.internal.corext.Assert;

public class SimpleSelectionProvider implements ISelectionProvider {
	private Object[] fElems;

	public SimpleSelectionProvider(Collection collection) {
		this(collection.toArray());
	}
	
	public SimpleSelectionProvider(Object[] elements) {
		Assert.isNotNull(elements);
		fElems = elements;
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
	}

	public ISelection getSelection() {
		return new StructuredSelection(fElems);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
	}

	public void setSelection(ISelection selection) {
	}

}