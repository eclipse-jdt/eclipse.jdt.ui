package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A Subclass of tree viewer to allow fast updates of error ticks.
 */
public class PackageViewer extends TreeViewer implements ISeverityListener {
	private HashMap fPathToWidget;
		
	public PackageViewer(Composite parent) {
		super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		fPathToWidget= new HashMap();
	}
	
	public void updateIcons(Collection changedElements) {
		ILabelProvider provider= (ILabelProvider)getLabelProvider();
		Iterator elements= changedElements.iterator();
		while (elements.hasNext()) {
			IPath path= (IPath)elements.next();
			Object o= fPathToWidget.get(path);
			if (o instanceof List) {
				List list= (List)o;
				for (int i= 0; i < list.size(); i++) {
					Object o2= list.get(i);
					if (o2 instanceof Item) {
						refreshIcon(provider, (Item)o2);
					}
				}
			} else if (o instanceof Item) {
				refreshIcon(provider, (Item)o);
			}
		}
	}
	
	protected void refreshIcon(ILabelProvider provider, Item item) {
		// defensive code. Multithread issue?
		if (item.isDisposed())
			return;
		Object data= item.getData();
		Image image= provider.getImage(data);
		if (image != null) {
			item.setImage(image);
		}
	}
	
	protected void mapElement(Object element, Widget widget) {
		super.mapElement(element, widget);
		if (element instanceof IJavaElement) {
			try {
				IResource res= ((IJavaElement)element).getCorrespondingResource();
				if (res != null)
					addToMap(res.getFullPath(), element, widget);
			} catch (JavaModelException e) {
			}
		}
	}
	protected void unmapElement(Object element) {
		super.unmapElement(element);
		if (element instanceof IJavaElement) {
			try {
				IResource res= ((IJavaElement)element).getCorrespondingResource();
				if (res != null)
					removeFromMap(res.getFullPath(), element);
			} catch (JavaModelException e) {
			}
		}
	}
	protected void unmapAllElements() {
		super.unmapAllElements();
		fPathToWidget.clear();
	}
	
	
	public void severitiesChanged(final Set changed) {
		Control ctrl= getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().asyncExec(new Runnable() {
				public void run() {
					updateIcons(changed);
				}
			});
		}
	}

	protected void addToMap(IPath path, Object element, Widget w) {
		Object existingMapping= fPathToWidget.get(path);
		if (existingMapping instanceof List) {
			List list= (List)existingMapping;
			list.add(w);
		} else if (existingMapping instanceof Widget) {
			ArrayList list= new ArrayList(2);
			list.add(existingMapping);
			list.add(w);
			fPathToWidget.put(path, list);
		} else {
			fPathToWidget.put(path, w);
		}
	}
	
	protected void removeFromMap(IPath path, Object element) {
		Object existingMapping= fPathToWidget.get(path);
		if (existingMapping instanceof List) {
			List list= (List)existingMapping;
			for (int i= 0; i < list.size(); i++) {
				Widget w= (Widget)list.get(i);
				// defensive code. Multithread issue?
				if (! w.isDisposed()) {
					Object data= w.getData();
					if (data == null || data.equals(element)) {
						list.remove(w);
						break;
					}
				}
			}
		} else if (existingMapping instanceof Widget) {
			fPathToWidget.remove(path);
		}
	}
}