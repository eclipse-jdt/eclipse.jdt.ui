/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jdt.ui.JavaUI;

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
		IContentProvider provider= getContentProvider();
		if (source != null && provider instanceof BaseJavaElementContentProvider) {
			BaseJavaElementContentProvider javaProvider= (BaseJavaElementContentProvider)provider;
			Object[] mapped= javaProvider.getCorrespondingJavaElements(source, false);
			super.handleLabelProviderChanged(new LabelProviderChangedEvent((IBaseLabelProvider)event.getSource(), mapped));	
			return;
		} 
		super.handleLabelProviderChanged(event);
	}
	
	/**
	 * @see StructuredViewer#handleInvalidSelection(ISelection, ISelection)
	 */
	protected void handleInvalidSelection(ISelection invalidSelection, ISelection newSelection) {
		if (isShowingWorkingCopies()) {
			// Convert to and from working copies
			if (invalidSelection instanceof IStructuredSelection) {
				IStructuredSelection structSel= (IStructuredSelection)invalidSelection;
				List elementsToSelect= new ArrayList(structSel.size());
				Iterator iter= structSel.iterator();
				while (iter.hasNext()) {
					Object element= iter.next();
					if (element instanceof IJavaElement) {
						IJavaElement je= convertToValidElement((IJavaElement)element);
						if (je != null)
							elementsToSelect.add(je);
					}
				}
				if (!elementsToSelect.isEmpty()) {
					List alreadySelected= SelectionUtil.toList(newSelection);
					if (alreadySelected != null && !alreadySelected.isEmpty())
						elementsToSelect.addAll(SelectionUtil.toList(newSelection));
					newSelection= new StructuredSelection(elementsToSelect);
					setSelection(newSelection);
				}
			}
		}
		super.handleInvalidSelection(invalidSelection, newSelection);
	}

	/**
	 * Converts a working copy (element) to a cu (element)
	 * or vice-versa.
	 * 
	 * @return the converted Java element or <code>null</code> if the conversion fails
	 */
	private IJavaElement convertToValidElement(IJavaElement je) {
		ICompilationUnit cu= (ICompilationUnit)je.getAncestor(IJavaElement.COMPILATION_UNIT);
		IJavaElement convertedJE= null;
		if (cu == null)
			return null;

		if (cu.isWorkingCopy())
			convertedJE= cu.getOriginal(je);
		else {
			IWorkingCopy wc= (IWorkingCopy)cu.findSharedWorkingCopy(JavaUI.getBufferFactory());
			if (wc != null) {
				IJavaElement[] matches= wc.findElements(je);
				if (matches != null && matches.length > 0)
					convertedJE= matches[0];
			}
		}
		if (convertedJE != null && convertedJE.exists())
			return convertedJE;
		else
			return null;
	}

	/**
	 * Answers whether this viewer shows working copies or not.
	 * 
	 * @return <code>true</code> if this viewer shows working copies
	 */
	private boolean isShowingWorkingCopies() {
		IContentProvider contentProvider= getContentProvider();
		return contentProvider instanceof BaseJavaElementContentProvider
			&& ((BaseJavaElementContentProvider)contentProvider).getProvideWorkingCopy();
	}

	/**
	 * @see AbstractTreeViewer#doUpdateItem(Item, Object)
	 */
	protected void doUpdateItem(Item item, Object element) {
		super.doUpdateItem(item, element);
		// experiment with greying out read only elements
		if (item instanceof TreeItem && element instanceof IJavaElement) {
			IJavaElement jelement= (IJavaElement)element;
			if (jelement.isReadOnly())
				((TreeItem)item).setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		}
	}
}

