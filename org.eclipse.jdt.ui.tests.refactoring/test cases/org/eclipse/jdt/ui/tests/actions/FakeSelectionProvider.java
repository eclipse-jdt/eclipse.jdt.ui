package org.eclipse.jdt.ui.tests.actions;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

class FakeSelectionProvider implements ISelectionProvider {
		private Object[] fElems;
		FakeSelectionProvider(Object[] elements){
			fElems= elements;
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


