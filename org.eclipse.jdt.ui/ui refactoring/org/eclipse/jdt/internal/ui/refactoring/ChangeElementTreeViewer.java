/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;

public class ChangeElementTreeViewer extends CheckboxTreeViewer {
	
	public ChangeElementTreeViewer(Composite parent) {
		super(parent, SWT.BORDER | SWT.FLAT);
		addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event){
				ChangeElement element= (ChangeElement)event.getElement();
				boolean checked= event.getChecked();
				
				element.setActive(checked);
				setSubtreeChecked(element, checked);
				setSubtreeGrayed(element, false);
				ChangeElement parent= element.getParent();
				if (parent != null) {
					if (checked) {
						while(parent != null) {
							setChecked(parent, checked);
							boolean grayed= parent.getActive() == ChangeElement.PARTLY_ACTIVE ? true : false;
							setGrayed(parent, grayed);
							parent= parent.getParent();
						}
					} else {
						setParentsGrayed(parent, true);
					}
				}				
			}
		});
		addTreeListener(new ITreeViewerListener() {
			public void treeCollapsed(TreeExpansionEvent event) {
			}
			public void treeExpanded(TreeExpansionEvent event) {
				ChangeElement element= (ChangeElement)event.getElement();
				initializeChildren(element);
			}
		});
	}
	
	protected void inputChanged(Object input, Object oldInput) {
		super.inputChanged(input, oldInput);
		initializeChildren((ChangeElement)input);
	}
	
	public void expandToLevel(Object element, int level) {
		super.expandToLevel(element, level);
		initializeChildren((ChangeElement)element);
	}
	
	private void initializeChildren(ChangeElement element) {
		if (element == null)
			return;
		ChangeElement[] children= element.getChildren();
		if (children == null)
			return;
		for (int i= 0; i < children.length; i++) {
			ChangeElement child= children[i];
			int state= child.getActive();
			boolean checked= state == ChangeElement.INACTIVE ? false : true;
			if (checked)
				setChecked(child, checked);
			boolean grayed= state == ChangeElement.PARTLY_ACTIVE ? true : false;
			if (grayed)
				setGrayed(child, grayed);
		}
	}
	
	private void setSubtreeGrayed(Object element, boolean grayed) {
		Widget widget= findItem(element);
		if (widget instanceof TreeItem) {
			TreeItem item= (TreeItem)widget;
			if (item.getGrayed() != grayed) {
				item.setGrayed(grayed);
				grayChildren(getChildren(item), grayed);
			}
		}
	}
	
	private void grayChildren(Item[] items, boolean grayed) {
		for (int i= 0; i < items.length; i++) {
			Item element= items[i];
			if (element instanceof TreeItem) {
				TreeItem item= (TreeItem)element;
				if (item.getGrayed() != grayed) {
					item.setGrayed(grayed);
					grayChildren(getChildren(item), grayed);
				}
			}
		}
	}
}