/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import java.util.ArrayList;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jface.viewers.IStructuredContentProvider;import org.eclipse.jface.viewers.Viewer;

public class VMContentProvider implements IStructuredContentProvider {

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
			IVMInstallType[] vmTypes= (IVMInstallType[])inputElement;			ArrayList vms= new ArrayList();			for (int i= 0; i < vmTypes.length; i++) {				IVMInstall[] vmInstalls= vmTypes[i].getVMInstalls();				for (int j= 0; j < vmInstalls.length; j++) 					vms.add(vmInstalls[j]);			}
			IVMInstall[] result= new IVMInstall[vms.size()];			return vms.toArray(result);		}		
		return new Object[0];
	}

}
