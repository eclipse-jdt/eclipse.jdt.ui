package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.util.Collection;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.internal.corext.Assert;

public class MockSelectionProvider implements ISelectionProvider {
	private ISelection fSelection;

	public MockSelectionProvider(Collection collection) {
		this(collection.toArray());
	}
	
	public MockSelectionProvider(Object[] elements) {
		Assert.isNotNull(elements);
		fSelection= new StructuredSelection(elements);
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
	}

	public ISelection getSelection() {
		return fSelection;
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
	}

	public void setSelection(ISelection selection) {
	}

}


