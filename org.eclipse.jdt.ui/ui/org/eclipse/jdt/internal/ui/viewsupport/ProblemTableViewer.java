package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableViewer;

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

}


