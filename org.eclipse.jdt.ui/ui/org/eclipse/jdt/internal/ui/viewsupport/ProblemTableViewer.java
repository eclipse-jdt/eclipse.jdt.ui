package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.Set;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

/**
 * Extends a  TableViewer to allow more performance when showing error ticks.
 * A <code>ProblemItemMapper</code> is contained that maps all items in
 * the tree to underlying resource
 */
public class ProblemTableViewer extends TableViewer implements IProblemChangedListener {

	private ProblemItemMapper fProblemItemMapper;

	/**
	 * Constructor for ProblemTableViewer.
	 * @param parent
	 */
	public ProblemTableViewer(Composite parent) {
		super(parent);
		initMapper();
	}

	/**
	 * Constructor for ProblemTableViewer.
	 * @param parent
	 * @param style
	 */
	public ProblemTableViewer(Composite parent, int style) {
		super(parent, style);
		initMapper();
	}

	/**
	 * Constructor for ProblemTableViewer.
	 * @param table
	 */
	public ProblemTableViewer(Table table) {
		super(table);
		initMapper();
	}

	private void initMapper() {
		fProblemItemMapper= new ProblemItemMapper();
	}
	
	/*
	 * @see IProblemChangedListener#problemsChanged
	 */
	public void problemsChanged(final Set changed) {
		Control control= getControl();
		if (control != null && !control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					fProblemItemMapper.problemsChanged(changed, (ILabelProvider)getLabelProvider());
				}
			});
		}
	}
	
	/*
	 * @see StructuredViewer#mapElement(Object, Widget)
	 */
	protected void mapElement(Object element, Widget item) {
		super.mapElement(element, item);
		if (item instanceof Item) {
			fProblemItemMapper.addToMap(element, (Item) item);
		}
	}

	/*
	 * @see StructuredViewer#unmapElement(Object, Widget)
	 */
	protected void unmapElement(Object element, Widget item) {
		if (item instanceof Item) {
			fProblemItemMapper.removeFromMap(element, (Item) item);
		}		
		super.unmapElement(element, item);
	}
	
	/*
	 * @see ContentViewer#handleLabelProviderChanged(LabelProviderChangedEvent)
	 */
	protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
		Object[] source= event.getElements();
		if (source == null) {
			super.handleLabelProviderChanged(event);
			return;
		}
		
		// map the event to the Java elements if possible
		// this does not handle the ambiguity of default packages
		Object[] mapped= new Object[source.length];
		for (int i= 0; i < source.length; i++) {
			Object o= source[i];
			// needs to handle the case of:
			// default package
			// package fragment root on project
			if (o instanceof IResource) {
				IResource r= (IResource)o;
				IJavaElement element= JavaCore.create(r);
				if (element != null) 
					mapped[i]= element;
				else
					mapped[i]= o;
			} else {
				mapped[i]= o;
			}
		}
		super.handleLabelProviderChanged(new LabelProviderChangedEvent((IBaseLabelProvider)event.getSource(), mapped));	
	}
}
