package org.eclipse.jdt.ui.tests.refactoring.infra;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

public class MockSelectionProvider implements ISelectionProvider {
		private ISelection fSelection;
		public MockSelectionProvider(Object[] elements){
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


