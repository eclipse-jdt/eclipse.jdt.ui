/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.ISeverityListener;
import org.eclipse.jdt.internal.ui.viewsupport.SeverityItemMapper;

/**
 * A Subclass of tree viewer to allow fast updates of error ticks.
 */
public class PackageViewer extends TreeViewer implements ISeverityListener {
	private SeverityItemMapper fSeverityItemMapper;
		
	public PackageViewer(Composite parent) {
		super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		fSeverityItemMapper= new SeverityItemMapper();
	}
	
	/**
	 * @see ISeverityListener#severitiesChanged
	 */
	public void severitiesChanged(final Set changed) {
		Control control= getControl();
		if (control != null && !control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					fSeverityItemMapper.severitiesChanged(changed, (ILabelProvider)getLabelProvider());
				}
			});
		}
	}
	
	private IResource getMappingResource(IJavaElement element) {
		try {
			return ((IJavaElement)element).getCorrespondingResource();
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}			
		return null;
	}
	
	/**
	 * @see TreeViewer#mapElement
	 */
	protected void mapElement(Object element, Widget widget) {
		super.mapElement(element, widget);
		if (element instanceof IJavaElement) {
			IResource res= getMappingResource((IJavaElement)element);
			if (res != null && widget instanceof Item) {
				fSeverityItemMapper.addToMap(res, (Item)widget);
			}
		}
	}

	/**
	 * @see TreeViewer#unmapElement
	 */	
	protected void unmapElement(Object element) {
		super.unmapElement(element);
		if (element instanceof IJavaElement) {
			IResource res= getMappingResource((IJavaElement)element);
			if (res != null) {
				fSeverityItemMapper.removeFromMap(res, element);
			}
		}
	}

	/**
	 * @see TreeViewer#unmapAllElements
	 */	
	protected void unmapAllElements() {
		super.unmapAllElements();
		fSeverityItemMapper.clearMap();
	}
	
}