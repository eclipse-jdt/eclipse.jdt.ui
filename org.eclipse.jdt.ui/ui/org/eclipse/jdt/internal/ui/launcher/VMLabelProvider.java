/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ILabelProviderListener;import org.eclipse.jface.viewers.ITableLabelProvider;import org.eclipse.swt.graphics.Image;

public class VMLabelProvider implements ITableLabelProvider {

	/**
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}

	/**
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/**
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/**
	 * @see ITableLabelProvider#getColumnText(Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof IVMInstall) {			IVMInstall vm= (IVMInstall)element;			switch(columnIndex) {				case 0: 					return vm.getVMInstallType().getName();				case 1:					return vm.getName();				case 2:					return vm.getInstallLocation().getAbsolutePath();			}
		}
		return element.toString();
	}

	/**
	 * @see ITableLabelProvider#getColumnImage(Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

}
