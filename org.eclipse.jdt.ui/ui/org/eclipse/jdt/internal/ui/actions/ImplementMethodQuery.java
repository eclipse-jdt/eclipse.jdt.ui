package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.codemanipulation.IImplementMethodQuery;
import org.eclipse.jdt.internal.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class ImplementMethodQuery implements IImplementMethodQuery {

	private static class ImplementMethodContentProvider implements ITreeContentProvider {

		private Object[] fTypes;
		private IMethod[] fMethods;
		private final Object[] fEmpty= new Object[0];

		/**
		 * Constructor for ImplementMethodContentProvider.
		 */
		public ImplementMethodContentProvider(IMethod[] methods, Object[] types) {
			fMethods= methods;
			fTypes= types;
		}
		
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IType) {
				ArrayList result= new ArrayList(fMethods.length);
				for (int i= 0; i < fMethods.length; i++) {
					if (fMethods[i].getDeclaringType().equals(parentElement)) {
						result.add(fMethods[i]);
					}
				}
				return result.toArray();
			}
			return fEmpty;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMethod) {
				return ((IMethod)element).getDeclaringType();
			}
			return null;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fTypes;
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	private static class ImplementMethodSorter extends ViewerSorter {

		private IType[] fAllTypes;

		public ImplementMethodSorter(ITypeHierarchy typeHierarchy) {
			IType curr= typeHierarchy.getType();
			IType[] superTypes= typeHierarchy.getAllSupertypes(curr);
			fAllTypes= new IType[superTypes.length + 1];
			fAllTypes[0]= curr;
			System.arraycopy(superTypes, 0, fAllTypes, 1, superTypes.length);
		}

		/*
		 * @see ViewerSorter#compare(Viewer, Object, Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof IType && e2 instanceof IType) {
				if (e1.equals(e2)) {
					return 0;
				}
				for (int i= 0; i < fAllTypes.length; i++) {
					IType curr= fAllTypes[i];
					if (curr.equals(e1)) {
						return -1;
					}
					if (curr.equals(e2)) {
						return 1;
					}	
				}
			}
			return 0;
		}

	}
	
	private class ImplementMethodValidator implements ISelectionValidator {
		/*
		 * @see ISelectionValidator#validate(Object[])
		 */
		public IStatus validate(Object[] selection) {
			int count= 0;
			for (int i= 0; i < selection.length; i++) {
				if (selection[i] instanceof IMethod) {
					count++;
				}
			}
			if (count == 0 && !fEmptySelectionAllowed) {
				return new StatusInfo(IStatus.ERROR, "");
			}
			if (count == 1) {
				return new StatusInfo(IStatus.INFO, count + " method selected.");
			} else {
				return new StatusInfo(IStatus.INFO, count + " methods selected.");
			}
		}

	}
	
	private boolean fEmptySelectionAllowed;
	private Shell fShell;
	
	public ImplementMethodQuery(Shell shell, boolean emptySelectionAllowed) {
		fShell= shell;
		fEmptySelectionAllowed= emptySelectionAllowed;
	}

	/*
	 * @see IImplementMethodQuery#select(IMethod[], IMethod[], ITypeHierarchy)
	 */
	public IMethod[] select(IMethod[] methods, IMethod[] defaultSelected, ITypeHierarchy typeHierarchy) {
		HashSet types= new HashSet(methods.length);
		for (int i= 0; i < methods.length; i++) {
			types.add(methods[i].getDeclaringType());
		}
		Object[] typesArrays= types.toArray();
		
		ViewerSorter sorter= new ImplementMethodSorter(typeHierarchy);
		sorter.sort(null, typesArrays);

		HashSet expanded= new HashSet(defaultSelected.length); 
		for (int i= 0; i < defaultSelected.length; i++) {
			expanded.add(defaultSelected[i].getDeclaringType());
		}

		if (expanded.isEmpty() && typesArrays.length > 0) {
			expanded.add(typesArrays[0]);
		}
		
		ILabelProvider lprovider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ITreeContentProvider cprovider= new ImplementMethodContentProvider(methods, typesArrays);
		
		CheckedTreeSelectionDialog dialog= new CheckedTreeSelectionDialog(fShell, lprovider, cprovider);
		dialog.setValidator(new ImplementMethodValidator());
		dialog.setTitle("Implement Methods");
		dialog.setMessage("&Select methods to implement:");
		dialog.setInitialSelections(defaultSelected);
		dialog.setExpandedElements(expanded.toArray());
		dialog.setContainerMode(true);
		dialog.setSize(60, 25);
		dialog.setInput(this); // input does not matter
		if (dialog.open() == dialog.OK) {
			Object[] checkedElements= dialog.getResult();
			ArrayList result= new ArrayList(checkedElements.length);
			for (int i= 0; i < checkedElements.length; i++) {
				Object curr= checkedElements[i];
				if (curr instanceof IMethod) {
					result.add(curr);
				}
			}
			return (IMethod[]) result.toArray(new IMethod[result.size()]);
		}
		return null;
	}
}

