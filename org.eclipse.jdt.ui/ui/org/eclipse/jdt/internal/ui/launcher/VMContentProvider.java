package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jface.viewers.ITreeContentProvider;import org.eclipse.jface.viewers.Viewer;

public class VMContentProvider implements ITreeContentProvider {

	/**
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/**
	 * @see IContentProvider#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IVMInstallType[]) {
			return (IVMInstallType[])inputElement;
		}
		return new Object[0];
	}

	/**
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	/**
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof IVMInstall) 
			return ((IVMInstall)element).getVMInstallType();
		return null;
	}

	/**
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IVMInstallType) {
			return ((IVMInstallType)parentElement).getVMInstalls();
		}	
		return new Object[0];
	}

}
