/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IWorkingCopy;

import org.eclipse.jdt.ui.IWorkingCopyProvider;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.ui.ProblemsLabelDecorator.ProblemsLabelChangedEvent;

import org.eclipse.jdt.internal.ui.util.SelectionUtil;

/**
 * Extends a  TreeViewer to allow more performance when showing error ticks.
 * A <code>ProblemItemMapper</code> is contained that maps all items in
 * the tree to underlying resource
 */
public class ProblemTreeViewer extends TreeViewer {

	protected ResourceToItemsMapper fResourceToItemsMapper;

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
		fResourceToItemsMapper= new ResourceToItemsMapper(this);
	}
	
	
	/*
	 * @see StructuredViewer#mapElement(Object, Widget)
	 */
	protected void mapElement(Object element, Widget item) {
		super.mapElement(element, item);
		if (item instanceof Item) {
			fResourceToItemsMapper.addToMap(element, (Item) item);
		}
	}

	/*
	 * @see StructuredViewer#unmapElement(Object, Widget)
	 */
	protected void unmapElement(Object element, Widget item) {
		if (item instanceof Item) {
			fResourceToItemsMapper.removeFromMap(element, (Item) item);
		}		
		super.unmapElement(element, item);
	}

	/*
	 * @see StructuredViewer#unmapAllElements()
	 */
	protected void unmapAllElements() {
		fResourceToItemsMapper.clearMap();
		super.unmapAllElements();
	}

	/*
	 * @see ContentViewer#handleLabelProviderChanged(LabelProviderChangedEvent)
	 */
	protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
		if (event instanceof ProblemsLabelChangedEvent) {
			ProblemsLabelChangedEvent e= (ProblemsLabelChangedEvent) event;
			if (!e.isMarkerChange() && !isShowingWorkingCopies()) {
				return;
			}
		}		
		
		Object[] changed= event.getElements();
		if (changed != null && !fResourceToItemsMapper.isEmpty()) {
			ArrayList others= new ArrayList();
			for (int i= 0; i < changed.length; i++) {
				Object curr= changed[i];
				if (curr instanceof IResource) {
					fResourceToItemsMapper.resourceChanged((IResource) curr);
				} else {
					others.add(curr);
				}
			}
			if (others.isEmpty()) {
				return;
			}
			event= new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource(), others.toArray());
		}
		super.handleLabelProviderChanged(event);
	}
	
	/**
	 * @see StructuredViewer#handleInvalidSelection(ISelection, ISelection)
	 */
	protected void handleInvalidSelection(ISelection invalidSelection, ISelection newSelection) {
		ISelection validNewSelection= getValidSelection(newSelection);
		if (getComparer() == null && isShowingWorkingCopies()) {
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
					List alreadySelected= SelectionUtil.toList(validNewSelection);
					if (alreadySelected != null && !alreadySelected.isEmpty())
						elementsToSelect.addAll(SelectionUtil.toList(validNewSelection));
					validNewSelection= new StructuredSelection(elementsToSelect);
				}
			}
		}
		if (validNewSelection != newSelection)
			setSelection(validNewSelection);
		super.handleInvalidSelection(invalidSelection, validNewSelection);
	}

	/*
	 * Returns a selection which does not contain non-existing
	 * Java elements. If all elements are ok then the original
	 * selection is returned unchanged.
	 */
	private ISelection getValidSelection(ISelection selection) {
		List selectedElements= SelectionUtil.toList(selection);
		if (selectedElements == null || selectedElements.isEmpty())
			return selection;

		boolean selectionChanged= false;
		List validElementsToSelect= new ArrayList(selectedElements.size());
		Iterator iter= selectedElements.iterator();
		while (iter.hasNext()) {
			Object element= iter.next();
			if (element instanceof IJavaElement) {
				IJavaElement je= (IJavaElement)element;
				if (je != null && je.exists()) 
					validElementsToSelect.add(je);
				else
					selectionChanged= true;
			} else {
				validElementsToSelect.add(element);
			}
		}
		if (selectionChanged)
			return new StructuredSelection(validElementsToSelect);
		else
			return selection;
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
		Object contentProvider= getContentProvider();
		return contentProvider instanceof IWorkingCopyProvider && ((IWorkingCopyProvider)contentProvider).providesWorkingCopies();
	}
}

