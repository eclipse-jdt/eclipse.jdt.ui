package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class ProblemTreeViewer extends TreeViewer implements IProblemChangedListener {

	private ProblemItemMapper fSeverityItemMapper;

	/**
	 * @see TreeViewer#TreeViewer(Composite)
	 */
	public ProblemTreeViewer(Composite parent) {
		super(parent);
		initMapper();
	}

	/**
	 * @see TreeViewer#TreeViewer(Composite, int)
	 */
	public ProblemTreeViewer(Composite parent, int style) {
		super(parent, style);
		initMapper();
	}

	/**
	 * @see TreeViewer#TreeViewer(Tree)
	 */
	public ProblemTreeViewer(Tree tree) {
		super(tree);
		initMapper();
	}
	
	private void initMapper() {
		fSeverityItemMapper= new ProblemItemMapper();
	}
	
	
	/**
	 * @see IProblemChangedListener#problemsChanged
	 */
	public void problemsChanged(final Set changed) {
		Control control= getControl();
		if (control != null && !control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					fSeverityItemMapper.problemsChanged(changed, (ILabelProvider)getLabelProvider());
				}
			});
		}
	}
	
	/**
	 * Called to get the underlying resource of an element that can show
	 * error markers.
	 * If the element can not show error markers, return null.
	 */
	protected IResource getUnderlyingResource(Object obj) throws CoreException {
		if (obj instanceof IJavaElement) {
			IJavaElement elem= (IJavaElement)obj;
			
			int type= elem.getElementType();
			if (type < IJavaElement.TYPE) {
				return elem.getCorrespondingResource();
			} else {
				IJavaElement cu= JavaModelUtility.findElementOfKind(elem, IJavaElement.COMPILATION_UNIT);
				if (cu != null) {
					return cu.getCorrespondingResource();
				}
			}
		}
		return null;
	}	
	
	/**
	 * @see TreeViewer#mapElement
	 */
	protected void mapElement(Object element, Widget widget) {
		super.mapElement(element, widget);
		try {
			IResource res= getUnderlyingResource(element);
			if (res != null && widget instanceof Item) {
				fSeverityItemMapper.addToMap(res, (Item)widget);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
		}			
	}

	/**
	 * @see TreeViewer#unmapElement
	 */	
	protected void unmapElement(Object element) {
		super.unmapElement(element);
		try {
			IResource res= getUnderlyingResource(element);
			if (res != null) {
				fSeverityItemMapper.removeFromMap(res, element);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
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

