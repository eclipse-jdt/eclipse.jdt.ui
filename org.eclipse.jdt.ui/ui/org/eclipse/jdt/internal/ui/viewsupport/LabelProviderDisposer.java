/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;


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
	
	public void widgetDisposed(DisposeEvent e) {
		fLabelProvider.dispose();
	}
}


