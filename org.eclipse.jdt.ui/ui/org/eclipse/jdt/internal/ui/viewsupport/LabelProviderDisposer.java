package org.eclipse.jdt.internal.ui.viewsupport;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

import org.eclipse.jface.viewers.ILabelProvider;

/**
 * Helper class to manage images that should be disposed when a control is disposed
 * contol.addWidgetListener(new LabelProviderDisposer(myLabelProvider));
 */
public class LabelProviderDisposer implements DisposeListener {
	
	private ILabelProvider fLabelProvider;
		
	public LabelProviderDisposer(ILabelProvider labelProvider) {
		fLabelProvider= labelProvider;
	}
	
	/**
	 * @see WidgetListener#widgetDisposed
	 */
	public void widgetDisposed(DisposeEvent e) {
		fLabelProvider.dispose();
	}
}


