/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.internal.ui.util.JdtHackFinder;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.internal.ui.util.TwoArrayQuickSort;

/**
 * A selection widget that consists of a list and a text entry field. The list
 * of elements presented are limited to the pattern entered into the text entry
 * field.
 */
public class SelectionList extends Composite {

	// State
	private Object[] fElements;
	private ILabelProvider fRenderer;
	private boolean fIgnoreCase;
	
	// Implementation details
	private String[] fRenderedStrings;
	private int[] fFilteredElements;	
	private String fRememberedMatchText;

	// SWT widgets
	private Table fList;
	private Text fText;
	
	/**
	 * Creates new instance of the widget.
	 */
	public SelectionList(Composite parent, int style, ILabelProvider renderer, boolean ignoreCase) {
		super(parent, SWT.NONE);
		fRenderer= renderer;
		fIgnoreCase= ignoreCase;
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0; layout.marginWidth= 0;
		JdtHackFinder.fixme("1G9V58A: ITPUI:WIN2000 - Dialog.convert* methods should be static");
		setLayout(layout);
		createText();
		createList(style);
	}

	/**
	 * Sets the list of elements presented in the widget.
	 */
	public void setElements(List elements, boolean refilter) {
		// We copy the list since we sort it.
		if (elements == null)
			fElements= new Object[0];
		else 
			fElements= elements.toArray();
		fFilteredElements= new int[fElements.length+1];
		fRenderedStrings= renderStrings();
		if (refilter)
			filter(true);		
	}

	/**
	 * Returns <code>true</code> when the list of elements is empty.
	 */
	public boolean isEmptyList() {
		return fElements == null || fElements.length == 0;
	}
	 
	/**
	 * Returns a list of selected elements. Note that the type of the elements
	 * returned in the list are the same as the ones passed to the selection list
	 * via <code>setElements</code>. The list doesn't contain the rendered strings.
	 */
	public List getSelection() {
		if (fList == null || fList.isDisposed() || fList.getSelectionCount() == 0)
			return new ArrayList(0);
		int[] listSelection= fList.getSelectionIndices();
		List selected= new ArrayList(listSelection.length);
		for (int i= 0; i < listSelection.length; i++) {
			selected.add(fElements[fFilteredElements[listSelection[i]]]);
		}
		return selected;
	}
	
	/**
	 * Returns the selection indices.
	 */
	public int[] getSelectionIndices() {
		return fList.getSelectionIndices();
	}
	 
	/**
	 * Selects the elements in the list determined by the given
	 * selection indices.
	 */
	protected void setSelection(int[] selection) {
		fList.setSelection(selection);
	}
	
	/**
	 * Adds a selection change listener to this widget.
	 */
	public void addSelectionListener(SelectionListener listener) {
		fList.addSelectionListener(listener);
	}
	
	/**
	 * Removes a selection change listener to this widget.
	 */
	public void removeSelectionListener(SelectionListener listener) {
		fList.removeSelectionListener(listener);
	}
	/**
	 * Sets the filter pattern. Current only prefix filter pattern are supported.
	 */
	public void setFilter(String pattern, boolean refilter) {
		fText.setText(pattern);
		if (refilter)
			filter(true);
	}
	
	/**
	 * Returns the currently used filter text.
	 */
	public String getFilter() {
		return fText.getText();
	}
	 
	/**
	 * Select the pattern text.
	 */
	public void selectFilterText() {
		fText.selectAll();
	}

	/*
	 * Non Java-doc
	 */
	public boolean setFocus() {
		return fText.setFocus();
	}
	
	/*
	 * Non Java-doc
	 */
	public void setFont(Font font) {
		super.setFont(font);
		fText.setFont(font);
		fList.setFont(font);
	}
	
	/* 
	 * Non Java-doc
	 */
	public void setEnabled(boolean enable) {
		super.setEnabled(enable);
		fText.setEnabled(enable);
		fList.setEnabled(enable);
	}
	 
	/**
	 * Filters the list of elements according to the pattern entered
	 * into the text entry field.
	 */
	public void filter(boolean forceUpdate) {
		int k= 0;
		String text= fText.getText();
		if (!forceUpdate && text.equals(fRememberedMatchText))
			return;
		fRememberedMatchText= text;
		StringMatcher matcher= new StringMatcher(text+"*", fIgnoreCase, false);
		for (int i= 0; i < fElements.length; i++) {
			if (matcher.match(fRenderedStrings[i])) {
				fFilteredElements[k]= i;
				k++;
			}
		}
		fFilteredElements[k]= -1;
		updateListWidget(fFilteredElements, k);
	}

	private void createText() {
		fText= new Text(this, SWT.BORDER);
		GridData spec= new GridData();
		spec.grabExcessVerticalSpace= false;
		spec.grabExcessHorizontalSpace= true;
		spec.horizontalAlignment= spec.FILL;
		spec.verticalAlignment= spec.BEGINNING;
		fText.setLayoutData(spec);
		Listener l= new Listener() {
			public void handleEvent(Event evt) {
				filter(false);
			}
		};
		fText.addListener(SWT.Modify, l);
	}
	
	private void createList(int style) {
		fList= new Table(this, style);
		fList.setLayoutData(new GridData(GridData.FILL_BOTH));
		fList.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fRenderer.dispose();
			}
		});
	}
	
	private String[] renderStrings() {
		String[] strings= new String[fElements.length];
		for (int i= 0; i < strings.length; i++) {
			strings[i]= fRenderer.getText(fElements[i]);
		}
		TwoArrayQuickSort.sort(strings, fElements, fIgnoreCase);
		return strings;
	}	
	
	private void updateListWidget(int[] indices, int size) {
		if (fList == null || fList.isDisposed())
			return;
		fList.setRedraw(false);
		int itemCount= fList.getItemCount();
		int oldSelectionIndex= fList.getSelectionIndex();
		boolean fireSelectionChanged= false;
		if (size < itemCount) {
			fList.remove(0, itemCount-size-1);
		}
		Display d= fList.getDisplay();
		TableItem[] items= fList.getItems();
		for (int i= 0; i < size; i++) {
			TableItem ti= null;
			if (i < itemCount) {
				ti= items[i];
			} else {
				ti= new TableItem(fList, i);
			}
			ti.setText(fRenderedStrings[indices[i]]);
			Image img= fRenderer.getImage(fElements[indices[i]]);
			if (img != null)
				ti.setImage(img);
		}
		if (fList.getItemCount() > 0) {
			fList.setSelection(0);
			if (oldSelectionIndex != -1)
				fireSelectionChanged= true;
		}	
		fList.setRedraw(true);
		if (fireSelectionChanged) {
			Event event= new Event();
			fList.notifyListeners(SWT.Selection, event);
		}
	}			
}