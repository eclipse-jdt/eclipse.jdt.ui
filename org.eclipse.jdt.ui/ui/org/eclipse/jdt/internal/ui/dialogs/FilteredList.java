/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.internal.core.refactoring.util.Selection;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.internal.ui.util.TypeInfo;

/**
 * A composite widget which holds a list of elements. The elements
 * can be filtered. Duplicates can be eliminated if desired.
 */
public class FilteredList extends Composite {
	private Table fList;
	private ILabelProvider fRenderer;
	private boolean fMatchEmtpyString= true;
	private boolean fIgnoreCase;
	private boolean fAllowDuplicates;
	private String fFilter= ""; //$NON-NLS-1$
	private TwoArrayQuickSorter fSorter;
	
	private Object[] fElements= new Object[0];
	private Label[] fLabels;

	private int[] fFoldedIndices;
	private int fFoldedCount;
	
	private int[] fFilteredIndices;
	private int fFilteredCount;

	private static class Label {
		public final String string;
		public final Image image;

		public Label(String string, Image image) {
			this.string= string;
			this.image= image;
		}
		
		public boolean equals(Label label) {
			if (label == null)
				return false;
				
			return			
				string.equals(label.string) &&
				image.equals(label.image);
		}
	}

	private static class LabelComparator implements Comparator {
		private boolean fIgnoreCase;
	
		LabelComparator(boolean ignoreCase) {
			fIgnoreCase= ignoreCase;
		}
	
		public int compare(Object left, Object right) {
			Label leftLabel= (Label) left;
			Label rightLabel= (Label) right;
				
			int value= fIgnoreCase
				? leftLabel.string.compareToIgnoreCase(rightLabel.string)
				: leftLabel.string.compareTo(rightLabel.string);

			if (value != 0)
				return value;

			// XXX works only for max. two image types
			return leftLabel.image.equals(rightLabel.image) ? 0 : 1;
		}
		
	}	
	
	/**
	 * Creates new instance of the widget.
	 */
	public FilteredList(Composite parent, int style, ILabelProvider renderer,
		boolean ignoreCase, boolean allowDuplicates, boolean matchEmptyString)
	{
		super(parent, SWT.NONE);

		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		setLayout(layout);
		
		fList= new Table(this, style);
		fList.setLayoutData(new GridData(GridData.FILL_BOTH));
		fList.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fRenderer.dispose();
			}
		});
		
		fRenderer= renderer;
		fIgnoreCase= ignoreCase;		
		fSorter= new TwoArrayQuickSorter(new LabelComparator(ignoreCase));
		fAllowDuplicates= allowDuplicates;
		fMatchEmtpyString= matchEmptyString;
	}
	/**
	 * Sets the list of elements presented in the widget.
	 */
	public void setElements(List elements) {
		// copy list for sorting
		if (elements == null)
			fElements= new Object[0];
		else 
			fElements= elements.toArray();
			
		int length= fElements.length;

		// fill labels			
		fLabels= new Label[length];		
		for (int i= 0; i != length; i++)
			fLabels[i]= new Label(
				fRenderer.getText(fElements[i]),
				fRenderer.getImage(fElements[i]));

		fSorter.sort(fLabels, fElements);
		
		fFoldedIndices= new int[length];
		fFoldedCount= fold();

		fFilteredIndices= new int[length];	
		fFilteredCount= filter();

		updateListWidget();
	}

	/**
	 * Returns <code>true</code> if the list is empty, <code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return (fElements == null) || (fElements.length == 0);
	}

	public void addSelectionListener(SelectionListener listener) {
		fList.addSelectionListener(listener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		fList.removeSelectionListener(listener);
	}	

	public void setSelection(int[] selection) {
		fList.setSelection(selection);
	}
	
	public int[] getSelectionIndices() {
		return fList.getSelectionIndices();
	}
	
	// convenience
	public int getSelectionIndex() {
		return fList.getSelectionIndex();		
	}
	
	/**
	 * Returns a list of selected elements. Note that the type of the elements
	 * returned in the list are the same as the ones passed to the selection list
	 * via <code>setElements</code>. The list doesn't contain the rendered strings.
	 */
	public List getSelectionAsList() {
		if (fList.isDisposed() || (fList.getSelectionCount() == 0))
			return new ArrayList(0);
			
		int[] listSelection= fList.getSelectionIndices();
		List selected= new ArrayList(listSelection.length);
		for (int i= 0; i < listSelection.length; i++)
			selected.add(fElements[fFilteredIndices[listSelection[i]]]);
		
		return selected;
	}

	/**
	 * Sets the filter pattern. Current only prefix filter pattern are supported.
	 * @param filter the filter pattern.
	 */
	public void setFilter(String filter) {
		fFilter= filter;

		fFilteredCount= filter();			
		updateListWidget();
	}
	
	/**
	 * Returns the filter pattern.
	 * @return the filter pattern.
	 */
	public String getFilter() {
		return fFilter;
	}

	/**
	 * Returns all elements which are folded together.
	 * @param index the selected index.
	 * @return an array of elements folded together at the index.
	 */
	public Object[] getFoldedElements(int index) {
		index= fFilteredIndices[index];

		int start= fFoldedIndices[index];			
		int count= (index == fFoldedCount - 1)
			? fElements.length - start
			: fFoldedIndices[index + 1] - start;
			
		Object[] elements= new Object[count];
		for (int i= 0; i != count; i++)
			elements[i]= fElements[start + i];			
				
		return elements;
	}

	private int fold() {
		int length= fElements.length;
		
		if (fAllowDuplicates) {
			for (int i= 0; i != length; i++)			
				fFoldedIndices[i]= i; // identity mapping
				
			return length;			
		
		} else {
			int k= 0;
			Label last= null;
			for (int i= 0; i != length; i++) {
				Label current= fLabels[i];
				if (! current.equals(last)) {
					fFoldedIndices[k]= i;
					k++;
					last= current;
				}
			}
			return k;
		}
	}

	/**
	 * Filters the list  with the filter pattern.
	 */
	private int filter() {
		if (((fFilter == null) || (fFilter.length() == 0)) && !fMatchEmtpyString)
			return 0;
		
		StringMatcher matcher= new StringMatcher(fFilter + "*", fIgnoreCase, false); //$NON-NLS-1$

		int k= 0;
		for (int i= 0; i != fFoldedCount; i++) {
			int j = fFoldedIndices[i];
			if (matcher.match(fLabels[j].string))
				fFilteredIndices[k++]= i;
		}			
						
		return k;
	}	

	/**
	 * Updates the list widget.
	 */	 
	private void updateListWidget() {
		if (fList.isDisposed())
			return;
			
		fList.setRedraw(false);
		
		// resize table
		int itemCount= fList.getItemCount();
		if (fFilteredCount < itemCount)
			fList.remove(0, itemCount - fFilteredCount - 1);
		else if (fFilteredCount > itemCount)
			for (int i= 0; i != fFilteredCount - itemCount; i++)
				new TableItem(fList, SWT.NONE);

		// fill table
		TableItem[] items= fList.getItems();
		for (int i= 0; i != fFilteredCount; i++) {
			TableItem item= items[i];
			Label label= fLabels[fFoldedIndices[fFilteredIndices[i]]];
			
			item.setText(label.string);
			item.setImage(label.image);
		}
		// select first item if any
		if (fList.getItemCount() > 0)
			fList.setSelection(0);
					
		fList.setRedraw(true);		
		fList.notifyListeners(SWT.Selection, new Event());
	}
	
}