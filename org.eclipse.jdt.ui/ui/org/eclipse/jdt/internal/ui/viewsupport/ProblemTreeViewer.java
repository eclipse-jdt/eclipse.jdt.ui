package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Extends a  TreeViewer to allow more performance when showing error ticks.
 * A <code>ProblemItemMapper</code> is contained that maps all items in
 * the tree to underlying resource
 */
public class ProblemTreeViewer extends TreeViewer implements IProblemChangedListener {

	private ProblemItemMapper fProblemItemMapper;

	/*
	 * @see TreeViewer#TreeViewer(Composite)
	 */
	public ProblemTreeViewer(Composite parent) {
		super(parent);
		initMapper();
	}

	/*
	 * @see TreeViewer#TreeViewer(Composite, int)
	 */
	public ProblemTreeViewer(Composite parent, int style) {
		super(parent, style);
		initMapper();
	}

	/*
	 * @see TreeViewer#TreeViewer(Tree)
	 */
	public ProblemTreeViewer(Tree tree) {
		super(tree);
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
	 * @see StructuredViewer#associate(Object, Item)
	 */
	protected void associate(Object element, Item item) {
		if (item.getData() != element) {
			fProblemItemMapper.addToMap(element, item);	
		}
		super.associate(element, item);		
	}

	/*
	 * @see StructuredViewer#disassociate(Item)
	 */
	protected void disassociate(Item item) {
		fProblemItemMapper.removeFromMap(item.getData(), item);
		super.disassociate(item);	
	}

}

