/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;


import java.util.ArrayList;import java.util.Iterator;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.events.SelectionListener;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;import org.eclipse.jface.viewers.ColumnWeightData;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ILabelProviderListener;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredContentProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.ITableLabelProvider;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.TableLayout;import org.eclipse.jface.viewers.TableViewer;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.viewers.ViewerSorter;import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class ListDialogField extends DialogField {
	
	public static final int UPDOWN= 1;
	
	protected TableViewer fTable;
	protected WrappedTableLabelProvider fWrappedLabelProvider;
	protected ListViewerAdapter fListViewerAdapter;
	protected List fElements;

	private String[] fCustomButtonLabels;
	private Button[] fCustomButtons;
	private boolean[] fCustomButtonsEnabled;
	
	private Button fRemoveButton;
	private Button fUpButton;
	private Button fDownButton;
	private Button fCheckAllButton;
	private Button fUncheckAllButton;	
	
	private String fRemoveButtonLabel, fUpButtonLabel, fDownButtonLabel;
	private boolean fRemoveButtonEnabled;
	
	private Label fLastSeparator;
	
	private int fConfiguration;
	
	private Table fTableControl;
	private Composite fButtonsControl;
	private ISelection fSelectionWhenEnabled;
	
	private IListAdapter fListAdapter;
	
	private Object fParentElement;
	
	/**
	 * Create a table without custom and remove buttons
	 */
	public ListDialogField(ILabelProvider lprovider, int config) {
		this(null, null, lprovider, config);
	}
	
	/**
	 * Create a table with custom buttons
	 */	
	public ListDialogField(IListAdapter adapter, String[] customButtonLabels, ILabelProvider lprovider, int config) {
		super();
		fListAdapter= adapter;
		fConfiguration= config;
		fWrappedLabelProvider= new WrappedTableLabelProvider(lprovider);
		fListViewerAdapter= new ListViewerAdapter();
		fParentElement= this;

		fElements= new ArrayList(10);
					
		fCustomButtonLabels= customButtonLabels;
		if (fCustomButtonLabels != null) {
			int nCustomButtons= fCustomButtonLabels.length;
			fCustomButtonsEnabled= new boolean[nCustomButtons];
			for (int i= 0; i < nCustomButtons; i++) {
				fCustomButtonsEnabled[i]= true;
			}
		}	
		
		// default labels
		fRemoveButtonLabel= "!Remove!";
		fUpButtonLabel= "!Up!";
		fDownButtonLabel= "!Down!";
		
		fRemoveButtonEnabled= true;
		
		fTable= null;
		fTableControl= null;
		fButtonsControl= null;
	}
			
	public void setRemoveButtonLabel(String removeButtonLabel) {
		fRemoveButtonLabel= removeButtonLabel;
	}
	
	public void setUpButtonLabel(String upButtonLabel) {
		fUpButtonLabel= upButtonLabel;
	}
	
	public void setDownButtonLabel(String downButtonLabel) {
		fDownButtonLabel= downButtonLabel;
	}
	
		
	// ------ configuration
	

	private boolean hasUpDown() {
		return ((fConfiguration & UPDOWN) != 0);
	}	
	
	// ------ adapter communication
	
	protected void customButtonPressed(int index) {
		fListAdapter.customButtonPressed(this, index);
	}

	// ------ layout helpers
	
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {
		assertEnoughColumns(nColumns);
		
		Label label= getLabelControl(parent);
		MGridData gd= gridDataForLabel(1);
		gd.verticalAlignment= gd.BEGINNING;
		label.setLayoutData(gd);
		
		Control list= getListControl(parent);
		gd= new MGridData();
		gd.horizontalAlignment= gd.FILL;
		gd.grabExcessHorizontalSpace= true;
		gd.verticalAlignment= gd.FILL;
		gd.grabExcessVerticalSpace= true;
		gd.grabColumn= 0;
		gd.horizontalSpan= nColumns - 2;
		gd.widthHint= 200;
		gd.heightHint= 200;
		list.setLayoutData(gd);
		
		Composite buttons= getButtonBox(parent);
		gd= new MGridData();
		gd.horizontalAlignment= gd.FILL;
		gd.grabExcessHorizontalSpace= false;
		gd.verticalAlignment= gd.FILL;
		gd.grabExcessVerticalSpace= false;
		gd.horizontalSpan= 1;
		buttons.setLayoutData(gd);
		
		return new Control[] { label, list, buttons };
	}
	
	public int getNumberOfControls() {
		return 3;	
	}
	
	public void setButtonsMinWidth(int minWidth) {
		if (fLastSeparator != null) {
			((MGridData)fLastSeparator.getLayoutData()).widthHint= minWidth;
		}
	}
	
	
	// ------ ui creation
	
	public Control getListControl(Composite parent) {
		if (fTableControl == null) {
			assertCompositeNotNull(parent);
						
			fTable= createTableViewer(parent);
			fTable.setContentProvider(fListViewerAdapter);
			fTable.setLabelProvider(fWrappedLabelProvider);
			fTable.addSelectionChangedListener(fListViewerAdapter);	
			
			fTableControl= (Table)fTable.getControl();
			
			// Add a table column.
			TableLayout tableLayout= new TableLayout();
			tableLayout.addColumnData(new ColumnWeightData(100));
			TableColumn tc= new TableColumn(fTableControl, SWT.NONE);
			tc.setResizable(false);
			fTableControl.setLayout(tableLayout);
			
			fTable.setInput(fParentElement);
			
			fTableControl.setEnabled(isEnabled());
			if (fSelectionWhenEnabled != null) {
				postSetSelection(fSelectionWhenEnabled);
			}		
		}
		return fTableControl;
	}
	
	public TableViewer getTableViewer() {
		return fTable;
	}
	
	/* 
	 * subclasses may override to specify a different style
	 */
	protected int getListStyle(){
		return SWT.BORDER + SWT.MULTI + SWT.H_SCROLL + SWT.V_SCROLL;
	}
	
	protected TableViewer createTableViewer(Composite parent) {
		Table table= new Table(parent, getListStyle());
		return new TableViewer(table);
	}	
	
	protected Button createButton(Composite parent, String label, SelectionListener listener) {
		Button button= new Button(parent, SWT.PUSH);
		button.setText(label);
		button.addSelectionListener(listener);
		MGridData gd= new MGridData();
		gd.horizontalAlignment= gd.FILL;
		gd.grabExcessHorizontalSpace= true;
		gd.verticalAlignment= gd.BEGINNING;
		button.setLayoutData(gd);
		return button;
	}
	
	private Label createSeparator(Composite parent) {
		Label separator= new Label(parent, SWT.NONE);
		separator.setVisible(false);
		MGridData gd= new MGridData();
		gd.horizontalAlignment= gd.FILL;
		gd.verticalAlignment= gd.BEGINNING;
		gd.heightHint= 4;
		separator.setLayoutData(gd);
		return separator;
	}			
	
	public Composite getButtonBox(Composite parent) {
		if (fButtonsControl == null) {
			assertCompositeNotNull(parent);
			
			SelectionListener listener= new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					doButtonSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					doButtonSelected(e);
				}
			};
			
			Composite contents= new Composite(parent, SWT.NULL);
			MGridLayout layout= new MGridLayout();
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			contents.setLayout(layout);
			
			if (fCustomButtonLabels != null) {
				fCustomButtons= new Button[fCustomButtonLabels.length];
				for (int i= 0; i < fCustomButtonLabels.length; i++) {
					String currLabel= fCustomButtonLabels[i];
					if (currLabel != null) {
						fCustomButtons[i]= createButton(contents, currLabel, listener);
						fCustomButtons[i].setEnabled(isEnabled() && fCustomButtonsEnabled[i]);
					} else {
						fCustomButtons[i]= null;
						createSeparator(contents);
					}
				}
				createSeparator(contents);			
				fRemoveButton= createButton(contents, fRemoveButtonLabel, listener);
				fRemoveButton.setEnabled(isEnabled() && fRemoveButtonEnabled);
				
				createSeparator(contents);
			}
			
			if (hasUpDown()) {
				fUpButton= createButton(contents, fUpButtonLabel, listener);
				fDownButton= createButton(contents, fDownButtonLabel, listener);
				createSeparator(contents);
			}
			
			createExtraButtons(contents);
						
			fLastSeparator= createSeparator(contents);	
	
			updateButtonState();
			fButtonsControl= contents;
		}
		
		return fButtonsControl;
	}
	
	protected void createExtraButtons(Composite parent) {
	}
	
	
	private void doButtonSelected(SelectionEvent e) {
		if (fCustomButtonLabels != null) {
			for (int i= 0; i < fCustomButtons.length; i++) {
				if (e.widget == fCustomButtons[i]) {
					customButtonPressed(i);
					return;
				}
			}
			if (e.widget == fRemoveButton) {
				remove();
				return;
			}
		}
		if (hasUpDown()) {
			if (e.widget == fUpButton) {
				up();
				return;
			}
			if (e.widget == fDownButton) {
				down();
				return;
			}
		}
	}	
	
	// ------ enable / disable management
	
	public void dialogFieldChanged() {
		super.dialogFieldChanged();
		updateButtonState();
	}
	
	
	/*
	 * sets the enable state of the remove, up and down buttons
	 */ 
	protected void updateButtonState() {
		if (fTable != null) {
			ISelection sel= fTable.getSelection();
			boolean enabled= !sel.isEmpty() && isEnabled();
			if (isOkToUse(fRemoveButton)) {
				fRemoveButton.setEnabled(enabled && fRemoveButtonEnabled);
			}
			
			if (hasUpDown()) {
				if (isOkToUse(fUpButton)) {
					fUpButton.setEnabled(enabled && canMoveUp());
				}
				if (isOkToUse(fDownButton)) {
					fDownButton.setEnabled(enabled && canMoveDown());
				}
			}
		}
	}
	
	protected void updateEnableState() {
		super.updateEnableState();
		
		boolean enabled= isEnabled();
		if (isOkToUse(fTableControl)) {
			if (!enabled) {
				fSelectionWhenEnabled= fTable.getSelection();
				selectElements(null);
			} else {
				selectElements(fSelectionWhenEnabled);
				fSelectionWhenEnabled= null;
			}
			fTableControl.setEnabled(enabled);
		}
		if (fCustomButtons != null) {
			for (int i= 0; i < fCustomButtons.length; i++) {
				Button button= fCustomButtons[i];
				if (isOkToUse(button)) {
					button.setEnabled(enabled && fCustomButtonsEnabled[i]);
				}
			}
		}
	
		updateButtonState();
	}
	
	public void enableCustomButton(int index, boolean enable) {
		if (fCustomButtonsEnabled != null && index < fCustomButtonsEnabled.length) {
			fCustomButtonsEnabled[index]= enable;
			if (fCustomButtons != null) {
				Button button= fCustomButtons[index];
				if (isOkToUse(button)) {
					button.setEnabled(isEnabled() && enable);
				}
			}
		}
	}
	
	public void enableRemoveButton(boolean enable) {
		fRemoveButtonEnabled= enable;
		if (isOkToUse(fRemoveButton)) {
			fRemoveButton.setEnabled(isEnabled() && enable);
		}
	}
		
	
	// ------ model access
	
	/**
	 * Sets the elements shown in the list
	 */
	public void setElements(List elements) {
		fElements= elements;
		if (fTable != null) {
			fTable.refresh();
		}
		dialogFieldChanged();
	}

	/**
	 * Gets the elements shown in the list.
	 * The list returned is a copy, so it can be changed by the user
	 * 
	 */	
	public List getElements() {
		return new ArrayList(fElements);
	}

	/**
	 * Gets the elements shown at the given index
	 */		
	public Object getElement(int index) {
		return fElements.get(index);
	}	

	/**
	 * Replace an element
	 */		
	public void replaceElement(Object oldElement, Object newElement) throws IllegalArgumentException { 
		int idx= fElements.indexOf(oldElement);
		if (idx != -1) {
			if (oldElement.equals(newElement) || fElements.contains(newElement)) {
				return;
			}
			fElements.set(idx, newElement);
			if (fTable != null) {
				fTable.refresh();
			}
			dialogFieldChanged();
		} else {
			throw new IllegalArgumentException();
		}
	}	

	/**
	 * Adds an element at the end of the list
	 */		
	public void addElement(Object element) {		
		if (fElements.contains(element)) {
			return;
		}
		fElements.add(element);
		if (fTable != null) {
			fTable.add(element);
		}
		dialogFieldChanged();
	}

	/**
	 * Adds elements at the end of the list
	 */	
	public void addElements(List elements) {
		int nElements= elements.size();
		
		if (nElements > 0) {
			// filter duplicated
			ArrayList elementsToAdd= new ArrayList(nElements);
			
			for (int i= 0; i < nElements; i++) {
				Object elem= elements.get(i);
				if (!fElements.contains(elem)) {
					elementsToAdd.add(elem);
				}	
			}
			fElements.addAll(elementsToAdd);
			if (fTable != null) {
				fTable.add(elementsToAdd.toArray());
			}
			dialogFieldChanged();
		}
	}	

	/**
	 * Adds an element at a position
	 */		
	public void insertElementAt(Object element, int index) {
		if (fElements.contains(element)) {
			return;
		}
		fElements.add(index, element);
		if (fTable != null) {
			fTable.add(element);
		}
		
		dialogFieldChanged();
	}	


	/**
	 * Adds an element at a position
	 */	
	public void removeAllElements() {
		if (fElements.size() > 0) {
			fElements.clear();
			if (fTable != null) {
				fTable.refresh();
			}
			dialogFieldChanged();
		}
	}
		
	/**
	 * Removes an element from the list
	 */		
	public void removeElement(Object element) throws IllegalArgumentException {
		if (fElements.remove(element)) {
			if (fTable != null) {
				fTable.remove(element);
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Removes elements from the list
	 */		
	public void removeElements(List elements) {
		if (elements.size() > 0) {
			fElements.removeAll(elements);
			if (fTable != null) {
				fTable.remove(elements.toArray());
			}
			dialogFieldChanged();
		}
	}

	/**
	 * Gets the number of elements
	 */		
	public int getSize() {
		return fElements.size();
	}
	

	public void selectElements(ISelection selection) {
		fSelectionWhenEnabled= selection;
		if (fTable != null) {
			fTable.setSelection(selection);
		}
	}
		
	public void postSetSelection(final ISelection selection) {
		if (isOkToUse(fTableControl)) {
			Display d= fTableControl.getDisplay();
			d.asyncExec(new Runnable() {
				public void run() {
					if (isOkToUse(fTableControl)) {
						selectElements(selection);
					}
				}
			});
		}
	}
	
	public void refresh() {
		fTable.refresh();
	}
	
	// ------- list maintenance
	
	/** 
	 * returns a Vector where the given elements are moved up one position
	 */
	private List moveUp(List elements, List move) {
		int nElements= elements.size();
		List res= new ArrayList(nElements);
		Object floating= null;
		for (int i= 0; i < nElements; i++) {
			Object curr= elements.get(i);
			if (move.contains(curr)) {
				res.add(curr);
			} else {
				if (floating != null) {
					res.add(floating);
				}
				floating= curr;
			}
		}
		if (floating != null) {
			res.add(floating);
		}
		return res;
	}	
	
	private void moveUp(List toMoveUp) {
		setElements(moveUp(fElements, toMoveUp));
		if (toMoveUp.size() > 0) {
			fTable.reveal(toMoveUp.get(0));
		}
	}
	
	private void moveDown(List toMoveDown) {
		setElements(reverse(moveUp(reverse(fElements), toMoveDown)));
		if (toMoveDown.size() > 0) {
			fTable.reveal(toMoveDown.get(toMoveDown.size() - 1));
		}
	}
	
	private List reverse(List p) {
		List reverse= new ArrayList(p.size());
		for (int i= p.size()-1; i >= 0; i--) {
			reverse.add(p.get(i));
		}
		return reverse;
	}
	
	
	private void remove() {
		removeElements(getSelectedElements());
	}
	
	private void up() {
		moveUp(getSelectedElements());
	}
	
	private void down() {
		moveDown(getSelectedElements());
	}
	
	private boolean canMoveUp() {
		if (isOkToUse(fTableControl)) {
			int[] indc= fTableControl.getSelectionIndices();
			for (int i= 0; i < indc.length; i++) {
				if (indc[i] != i) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean canMoveDown() {
		if (isOkToUse(fTableControl)) {
			int[] indc= fTableControl.getSelectionIndices();
			int k= fElements.size() - 1;
			for (int i= indc.length - 1; i >= 0 ; i--, k--) {
				if (indc[i] != k) {
					return true;
				}
			}
		}
		return false;
	}	
	

	public List getSelectedElements() {
		ISelection selection= fTable.getSelection();		
		List result= new ArrayList();
		if (selection instanceof IStructuredSelection) {
			Iterator iter= ((IStructuredSelection)selection).iterator();
			while (iter.hasNext()) {
				result.add(iter.next());
			}
		}
		return result;
	}
	
	
	
	
	// ------- ListViewerAdapter
	
	private class ListViewerAdapter implements IStructuredContentProvider, ISelectionChangedListener {

		// ------- ITableContentProvider Interface ------------
	
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// will never happen
		}
		
		public boolean isDeleted(Object element) {
			return false;
		}
	
		public void dispose() {
		}
		
		public Object[] getElements(Object obj) {
			return fElements.toArray();
		}
	
		// ------- ISelectionChangedListener Interface ------------
		
		public void selectionChanged(SelectionChangedEvent event) {
			doListSelected(event);
		}

	}
	
	
	private void doListSelected(SelectionChangedEvent event) {
		updateButtonState();
		if (fListAdapter != null) {
			fListAdapter.selectionChanged(this);
		}
	}
	
	
	private class WrappedTableLabelProvider implements ITableLabelProvider {

		private ILabelProvider fLabelProvider;

		public WrappedTableLabelProvider(ILabelProvider lprovider) {
			fLabelProvider= lprovider;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return fLabelProvider.getImage(element);
		}

		public String getColumnText(Object element, int columnIndex) {
			return fLabelProvider.getText(element);
		}
		
		/*public Image getColumnImage(Viewer viewer, Object element, int columnIndex) {
			org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("To be removed");
			return fLabelProvider.getImage(element);
		}

		public String getColumnText(Viewer viewer, Object element, int columnIndex) {
			org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("To be removed");
			return fLabelProvider.getText(element);
		}*/
		
		public void addListener(ILabelProviderListener listener) {
			fLabelProvider.addListener(listener);
		}
		
		public void removeListener(ILabelProviderListener listener) {
			fLabelProvider.removeListener(listener);
		}
	
		public void dispose() {
			fLabelProvider.dispose();
		}

		public boolean isLabelProperty(Object element, String property) {
			return fLabelProvider.isLabelProperty(element, property);
		}	
	}
	
}