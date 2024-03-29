/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import org.eclipse.jdt.internal.ui.util.SWTUtil;


/**
 * A tree with a button bar.
 * Typical buttons are 'Add', 'Remove', 'Up' and 'Down'.
 * Tree model is independent of widget creation.
 * DialogFields controls are: Label, Tree, and Composite containing buttons.
 *
 * @param <E> the type of the root elements
 */
public class TreeListDialogField<E> extends DialogField {

	protected TreeViewer fTree;
	protected ILabelProvider fLabelProvider;
	protected TreeViewerAdapter fTreeViewerAdapter;
	protected List<E> fElements;
	protected ViewerComparator fViewerComparator;

	protected String[] fButtonLabels;
	private Button[] fButtonControls;

	private boolean[] fButtonsEnabled;

	private int fRemoveButtonIndex;
	private int fUpButtonIndex;
	private int fDownButtonIndex;

	private Label fLastSeparator;

	private Tree fTreeControl;
	private Composite fButtonsControl;
	private ISelection fSelectionWhenEnabled;

	private ITreeListAdapter<E> fTreeAdapter;

	private Object fParentElement;
	private int fTreeExpandLevel;

	/**
	 * @param adapter Can be <code>null</code>.
	 */
	public TreeListDialogField(ITreeListAdapter<E> adapter, String[] buttonLabels, ILabelProvider lprovider) {
		super();
		fTreeAdapter= adapter;

		fLabelProvider= lprovider;
		fTreeViewerAdapter= new TreeViewerAdapter();
		fParentElement= this;

		fElements= new ArrayList<>(10);

		fButtonLabels= buttonLabels;
		if (fButtonLabels != null) {
			int nButtons= fButtonLabels.length;
			fButtonsEnabled= new boolean[nButtons];
			for (int i= 0; i < nButtons; i++) {
				fButtonsEnabled[i]= true;
			}
		}

		fTree= null;
		fTreeControl= null;
		fButtonsControl= null;

		fRemoveButtonIndex= -1;
		fUpButtonIndex= -1;
		fDownButtonIndex= -1;

		fTreeExpandLevel= 0;
	}

	/**
	* Sets the index of the 'remove' button in the button label array passed in
	* the constructor. The behavior of the button marked as the 'remove' button
	* will then be handled internally. (enable state, button invocation
	* behavior)
	*/
	public void setRemoveButtonIndex(int removeButtonIndex) {
		Assert.isTrue(removeButtonIndex < fButtonLabels.length);
		fRemoveButtonIndex= removeButtonIndex;
	}

	/**
	* Sets the index of the 'up' button in the button label array passed in the
	* constructor. The behavior of the button marked as the 'up' button will
	* then be handled internally.
	* (enable state, button invocation behavior)
	*/
	public void setUpButtonIndex(int upButtonIndex) {
		Assert.isTrue(upButtonIndex < fButtonLabels.length);
		fUpButtonIndex= upButtonIndex;
	}

	/**
	* Sets the index of the 'down' button in the button label array passed in
	* the constructor. The behavior of the button marked as the 'down' button
	* will then be handled internally. (enable state, button invocation
	* behavior)
	*/
	public void setDownButtonIndex(int downButtonIndex) {
		Assert.isTrue(downButtonIndex < fButtonLabels.length);
		fDownButtonIndex= downButtonIndex;
	}

	/**
	* Sets the viewer comparator.
	* @param viewerComparator The viewer comparator to set
	*/
	public void setViewerComparator(ViewerComparator viewerComparator) {
		fViewerComparator= viewerComparator;
	}

	public void setTreeExpansionLevel(int level) {
		fTreeExpandLevel= level;
		if (isOkToUse(fTreeControl) && fTreeExpandLevel > 0) {
			fTree.expandToLevel(level);
		}
	}

	// ------ adapter communication

	private void buttonPressed(int index) {
		if (!managedButtonPressed(index) && fTreeAdapter != null) {
			fTreeAdapter.customButtonPressed(this, index);
		}
	}

	/**
	* Checks if the button pressed is handled internally
	* @return Returns true if button has been handled.
	*/
	protected boolean managedButtonPressed(int index) {
		if (index == fRemoveButtonIndex) {
			remove();
		} else if (index == fUpButtonIndex) {
			up();
		} else if (index == fDownButtonIndex) {
			down();
		} else {
			return false;
		}
		return true;
	}

	// ------ layout helpers

	/*
	* @see DialogField#doFillIntoGrid
	*/
	@Override
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {
		PixelConverter converter= new PixelConverter(parent);

		assertEnoughColumns(nColumns);

		Label label= getLabelControl(parent);
		GridData gd= gridDataForLabel(1);
		gd.verticalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);

		Control list= getTreeControl(parent);
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= false;
		gd.verticalAlignment= GridData.FILL;
		gd.grabExcessVerticalSpace= true;
		gd.horizontalSpan= nColumns - 2;
		gd.widthHint= converter.convertWidthInCharsToPixels(50);
		gd.heightHint= converter.convertHeightInCharsToPixels(6);

		list.setLayoutData(gd);

		Composite buttons= getButtonBox(parent);
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= false;
		gd.verticalAlignment= GridData.FILL;
		gd.grabExcessVerticalSpace= true;
		gd.horizontalSpan= 1;
		buttons.setLayoutData(gd);

		return new Control[] { label, list, buttons };
	}

	/*
	* @see DialogField#getNumberOfControls
	*/
	@Override
	public int getNumberOfControls() {
		return 3;
	}

	/**
	* Sets the minimal width of the buttons. Must be called after widget creation.
	*/
	public void setButtonsMinWidth(int minWidth) {
		if (fLastSeparator != null) {
			((GridData) fLastSeparator.getLayoutData()).widthHint= minWidth;
		}
	}

	// ------ UI creation

	/**
	* Returns the tree control. When called the first time, the control will be
	* created.
	* @param parent The parent composite when called the first time, or <code>null</code>
	* after.
	*/
	public Control getTreeControl(Composite parent) {
		if (fTreeControl == null) {
			assertCompositeNotNull(parent);

			fTree= createTreeViewer(parent);

			fTreeControl= (Tree) fTree.getControl();
			fTreeControl.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					handleKeyPressed(e);
				}
			});
			fTree.setContentProvider(fTreeViewerAdapter);
			if(fLabelProvider instanceof IStyledLabelProvider) {
				fTree.setLabelProvider(new DelegatingStyledCellLabelProvider((IStyledLabelProvider) fLabelProvider));
			} else {
				fTree.setLabelProvider(fLabelProvider);
			}
			fTree.addSelectionChangedListener(fTreeViewerAdapter);
			fTree.addDoubleClickListener(fTreeViewerAdapter);

			fTree.setInput(fParentElement);
			fTree.expandToLevel(fTreeExpandLevel);

			if (fViewerComparator != null) {
				fTree.setComparator(fViewerComparator);
			}

			fTreeControl.setEnabled(isEnabled());
			if (fSelectionWhenEnabled != null) {
				postSetSelection(fSelectionWhenEnabled);
			}
		}
		return fTreeControl;
	}

	/**
	 * @return the internally used tree viewer, or <code>null</code> if the UI has not been created yet
	 */
	public TreeViewer getTreeViewer() {
		return fTree;
	}

	/**
	 * @param idx the index of the button
	 * @return the button control, or <code>null</code> if the UI has not been created yet
	 */
	public Button getButton(int idx) {
		return fButtonControls == null ? null : fButtonControls[idx];
	}

	/*
	* Subclasses may override to specify a different style.
	*/
	protected int getTreeStyle() {
		int style= SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL;
		return style;
	}

	protected TreeViewer createTreeViewer(Composite parent) {
		Tree tree= new Tree(parent, getTreeStyle());
		tree.setFont(parent.getFont());
		return new TreeViewer(tree);
	}

	protected Button createButton(Composite parent, String label, SelectionListener listener) {
		Button button= new Button(parent, SWT.PUSH);
		button.setFont(parent.getFont());
		button.setText(label);
		button.addSelectionListener(listener);
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.grabExcessHorizontalSpace= true;
		gd.verticalAlignment= GridData.BEGINNING;
		gd.widthHint= SWTUtil.getButtonWidthHint(button);

		button.setLayoutData(gd);
		return button;
	}

	private Label createSeparator(Composite parent) {
		Label separator= new Label(parent, SWT.NONE);
		separator.setFont(parent.getFont());
		separator.setVisible(false);
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.verticalAlignment= GridData.BEGINNING;
		gd.heightHint= 4;
		separator.setLayoutData(gd);
		return separator;
	}

	/**
	* Returns the composite containing the buttons. When called the first time, the control
	* will be created.
	* @param parent The parent composite when called the first time, or <code>null</code>
	* after.
	*/
	public Composite getButtonBox(Composite parent) {
		if (fButtonsControl == null) {
			assertCompositeNotNull(parent);

			SelectionListener listener= new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					doButtonSelected(e);
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					doButtonSelected(e);
				}
			};

			Composite contents= new Composite(parent, SWT.NONE);
			contents.setFont(parent.getFont());

			GridLayout layout= new GridLayout();
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			contents.setLayout(layout);

			if (fButtonLabels != null) {
				fButtonControls= new Button[fButtonLabels.length];
				for (int i= 0; i < fButtonLabels.length; i++) {
					String currLabel= fButtonLabels[i];
					if (currLabel != null) {
						fButtonControls[i]= createButton(contents, currLabel, listener);
						fButtonControls[i].setEnabled(isEnabled() && fButtonsEnabled[i]);
					} else {
						fButtonControls[i]= null;
						createSeparator(contents);
					}
				}
			}

			fLastSeparator= createSeparator(contents);

			updateButtonState();
			fButtonsControl= contents;
		}

		return fButtonsControl;
	}

	private void doButtonSelected(SelectionEvent e) {
		if (fButtonControls != null) {
			for (int i= 0; i < fButtonControls.length; i++) {
				if (e.widget == fButtonControls[i]) {
					buttonPressed(i);
					return;
				}
			}
		}
	}

	/**
	* Handles key events in the table viewer. Specifically
	* when the delete key is pressed.
	*/
	protected void handleKeyPressed(KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0) {
			if (fRemoveButtonIndex != -1 && isButtonEnabled(fTree.getSelection(), fRemoveButtonIndex)) {
				managedButtonPressed(fRemoveButtonIndex);
				return;
			}
		}
		fTreeAdapter.keyPressed(this, event);
	}

	// ------ enable / disable management

	/*
	* @see DialogField#dialogFieldChanged
	*/
	@Override
	public void dialogFieldChanged() {
		super.dialogFieldChanged();
		updateButtonState();
	}

	/*
	* Updates the enable state of the all buttons
	*/
	protected void updateButtonState() {
		if (fButtonControls != null && isOkToUse(fTreeControl) && fTreeControl.isEnabled()) {
			ISelection sel= fTree.getSelection();
			for (int i= 0; i < fButtonControls.length; i++) {
				Button button= fButtonControls[i];
				if (isOkToUse(button)) {
					button.setEnabled(isButtonEnabled(sel, i));
				}
			}
		}
	}


	protected boolean containsAttributes(List<Object> selected) {
		return !fElements.containsAll(selected);
	}


	protected boolean getManagedButtonState(ISelection sel, int index) {
		List<Object> selected= getSelectedElements();
		boolean hasAttributes= containsAttributes(selected);
		if (index == fRemoveButtonIndex) {
			return !selected.isEmpty() && !hasAttributes;
		} else if (index == fUpButtonIndex) {
			return !sel.isEmpty() && !hasAttributes && canMoveUp(selected);
		} else if (index == fDownButtonIndex) {
			return !sel.isEmpty() && !hasAttributes && canMoveDown(selected);
		}
		return true;
	}

	/*
	* @see DialogField#updateEnableState
	*/
	@Override
	protected void updateEnableState() {
		super.updateEnableState();

		boolean enabled= isEnabled();
		if (isOkToUse(fTreeControl)) {
			if (!enabled) {
				if (fSelectionWhenEnabled == null) {
					fSelectionWhenEnabled= fTree.getSelection();
					selectElements(null);
				}
			} else if (fSelectionWhenEnabled != null) {
					selectElements(fSelectionWhenEnabled);
			}
			fTreeControl.setEnabled(enabled);
		}
		updateButtonState();
	}

	/**
	* Sets a button enabled or disabled.
	*/
	public void enableButton(int index, boolean enable) {
		if (fButtonsEnabled != null && index < fButtonsEnabled.length) {
			fButtonsEnabled[index]= enable;
			updateButtonState();
		}
	}

	private boolean isButtonEnabled(ISelection sel, int index) {
		boolean extraState= getManagedButtonState(sel, index);
		return isEnabled() && extraState && fButtonsEnabled[index];
	}

	// ------ model access

	/**
	* Sets the elements shown in the list.
	*/
	public void setElements(List<E> elements) {
		fElements= new ArrayList<>(elements);
		refresh();
		if (isOkToUse(fTreeControl)) {
			fTree.expandToLevel(fTreeExpandLevel);
		}
		dialogFieldChanged();
	}

	/**
	* Gets the elements shown in the list.
	* The list returned is a copy, so it can be modified by the user.
	*/
	public List<E> getElements() {
		return new ArrayList<>(fElements);
	}

	/**
	* Gets the element shown at the given index.
	*/
	public E getElement(int index) {
		return fElements.get(index);
	}

	/**
	* Gets the index of an element in the list or -1 if element is not in list.
    */
	public int getIndexOfElement(Object elem) {
		return fElements.indexOf(elem);
	}

	/**
	* Replace an element.
	*/
	public void replaceElement(E oldElement, E newElement) throws IllegalArgumentException {
		int idx= fElements.indexOf(oldElement);
		if (idx != -1) {
			fElements.set(idx, newElement);
			if (isOkToUse(fTreeControl)) {
				List<Object> selected= getSelectedElements();
				if (selected.remove(oldElement)) {
					selected.add(newElement);
				}
				boolean isExpanded= fTree.getExpandedState(oldElement);
				fTree.remove(oldElement);
				fTree.add(fParentElement, newElement);
				if (isExpanded) {
					fTree.expandToLevel(newElement, fTreeExpandLevel);
				}
				selectElements(new StructuredSelection(selected));
			}
			dialogFieldChanged();
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	* Adds an element at the end of the tree list.
	*/
	public boolean addElement(E element) {
		if (fElements.contains(element)) {
			return false;
		}
		fElements.add(element);
		if (isOkToUse(fTreeControl)) {
			fTree.add(fParentElement, element);
			fTree.expandToLevel(element, fTreeExpandLevel);
		}
		dialogFieldChanged();
		return true;
	}

	/**
	* Adds elements at the end of the tree list.
	*/
	public boolean addElements(List<E> elements) {
		int nElements= elements.size();

		if (nElements > 0) {
			// filter duplicated
			ArrayList<E> elementsToAdd= new ArrayList<>(nElements);

			for (int i= 0; i < nElements; i++) {
				E elem= elements.get(i);
				if (!fElements.contains(elem)) {
					elementsToAdd.add(elem);
				}
			}
			if (!elementsToAdd.isEmpty()) {
				fElements.addAll(elementsToAdd);
				if (isOkToUse(fTreeControl)) {
					fTree.add(fParentElement, elementsToAdd.toArray());
					for (E element : elementsToAdd) {
						fTree.expandToLevel(element, fTreeExpandLevel);
					}
				}
				dialogFieldChanged();
				return true;
			}
		}
		return false;
	}

	/**
	* Adds an element at a position.
	*/
	public void insertElementAt(E element, int index) {
		if (fElements.contains(element)) {
			return;
		}
		fElements.add(index, element);
		if (isOkToUse(fTreeControl)) {
			fTree.add(fParentElement, element);
			if (fTreeExpandLevel != -1) {
				fTree.expandToLevel(element, fTreeExpandLevel);
			}
		}

		dialogFieldChanged();
	}

	/**
	* Adds an element at a position.
	*/
	public void removeAllElements() {
		if (fElements.size() > 0) {
			fElements.clear();
			refresh();
			dialogFieldChanged();
		}
	}

	/**
	* Removes an element from the list.
	*/
	public void removeElement(E element) throws IllegalArgumentException {
		if (fElements.remove(element)) {
			if (isOkToUse(fTreeControl)) {
				fTree.remove(element);
			}
			dialogFieldChanged();
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	* Removes elements from the list.
	*/
	public void removeElements(List<?> elements) {
		if (elements.size() > 0) {
			fElements.removeAll(elements);
			if (isOkToUse(fTreeControl)) {
				fTree.remove(elements.toArray());
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
		if (isOkToUse(fTreeControl)) {
			fTree.setSelection(selection, true);
		}
	}

	public void selectFirstElement() {
		Object element= null;
		if (fViewerComparator != null) {
			Object[] arr= fElements.toArray();
			fViewerComparator.sort(fTree, arr);
			if (arr.length > 0) {
				element= arr[0];
			}
		} else {
			if (fElements.size() > 0) {
				element= fElements.get(0);
			}
		}
		if (element != null) {
			selectElements(new StructuredSelection(element));
		}
	}

	public void postSetSelection(final ISelection selection) {
		if (isOkToUse(fTreeControl)) {
			Display d= fTreeControl.getDisplay();
			d.asyncExec(() -> {
				if (isOkToUse(fTreeControl)) {
					selectElements(selection);
				}
			});
		}
	}

	/**
	* Refreshes the tree.
	*/
	@Override
	public void refresh() {
		super.refresh();
		if (isOkToUse(fTreeControl)) {
			fTree.refresh();
		}
	}

	/**
	* Refreshes the tree.
	*/
	public void refresh(Object element) {
		if (isOkToUse(fTreeControl)) {
			fTree.refresh(element);
		}
	}

	/**
	* Updates the element.
	*/
	public void update(Object element) {
		if (isOkToUse(fTreeControl)) {
			fTree.update(element, null);
		}
	}

	// ------- list maintenance

	private List<E> moveUp(List<E> elements, List<E> move) {
		int nElements= elements.size();
		List<E> res= new ArrayList<>(nElements);
		E floating= null;
		for (E curr : elements) {
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

	private void moveUp(List<E> toMoveUp) {
		if (toMoveUp.size() > 0) {
			setElements(moveUp(fElements, toMoveUp));
			fTree.reveal(toMoveUp.get(0));
		}
	}

	private void moveDown(List<E> toMoveDown) {
		if (toMoveDown.size() > 0) {
			setElements(reverse(moveUp(reverse(fElements), toMoveDown)));
			fTree.reveal(toMoveDown.get(toMoveDown.size() - 1));
		}
	}

	private List<E> reverse(List<E> p) {
		List<E> reverse= new ArrayList<>(p);
		Collections.reverse(reverse);
		return reverse;
	}

	private void remove() {
		removeElements(getSelectedElements());
	}

	private void up() {
		moveUp(getSelectedRootElements());
	}

	private void down() {
		moveDown(getSelectedRootElements());
	}

	private boolean canMoveUp(List<Object> selectedElements) {
		if (isOkToUse(fTreeControl)) {
			int nSelected= selectedElements.size();
			int nElements= fElements.size();
			for (int i= 0; i < nElements && nSelected > 0; i++) {
				if (!selectedElements.contains(fElements.get(i))) {
					return true;
				}
				nSelected--;
			}
		}
		return false;
	}

	private boolean canMoveDown(List<Object> selectedElements) {
		if (isOkToUse(fTreeControl)) {
			int nSelected= selectedElements.size();
			for (int i= fElements.size() - 1; i >= 0 && nSelected > 0; i--) {
				if (!selectedElements.contains(fElements.get(i))) {
					return true;
				}
				nSelected--;
			}
		}
		return false;
	}

	/**
	* Returns the selected elements.
	*/
	public List<Object> getSelectedElements() {
		ArrayList<Object> result= new ArrayList<>();
		if (isOkToUse(fTreeControl)) {
			ISelection selection= fTree.getSelection();
			if (selection instanceof IStructuredSelection) {
				Iterator<?> iter= ((IStructuredSelection)selection).iterator();
				while (iter.hasNext()) {
					result.add(iter.next());
				}
			}
		}
		return result;
	}

	public List<E> getSelectedRootElements() {
		ArrayList<E> result= new ArrayList<>();
		if (isOkToUse(fTreeControl)) {
			ISelection selection= fTree.getSelection();
			if (selection instanceof IStructuredSelection) {
				Iterator<?> iter= ((IStructuredSelection)selection).iterator();
				while (iter.hasNext()) {
					Object element= iter.next();
					if (fElements.contains(element)) {
						@SuppressWarnings("unchecked")
						E rootElement= (E) element;
						result.add(rootElement);
					}
				}
			}
		}
		return result;
	}

	public void expandElement(Object element, int level) {
		if (isOkToUse(fTreeControl)) {
			fTree.expandToLevel(element, level);
		}
	}


	// ------- TreeViewerAdapter

	private class TreeViewerAdapter implements ITreeContentProvider, ISelectionChangedListener, IDoubleClickListener {

		private final Object[] NO_ELEMENTS= new Object[0];

		// ------- ITreeContentProvider Interface ------------

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// will never happen
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object obj) {
			return fElements.toArray();
		}

		@Override
		public Object[] getChildren(Object element) {
			if (fTreeAdapter != null) {
				return fTreeAdapter.getChildren(TreeListDialogField.this, element);
			}
			return NO_ELEMENTS;
		}

		@Override
		public Object getParent(Object element) {
			if (!fElements.contains(element) && fTreeAdapter != null) {
				return fTreeAdapter.getParent(TreeListDialogField.this, element);
			}
			return fParentElement;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (fTreeAdapter != null) {
				return fTreeAdapter.hasChildren(TreeListDialogField.this, element);
			}
			return false;
		}

		// ------- ISelectionChangedListener Interface ------------

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			doListSelected();
		}

		@Override
		public void doubleClick(DoubleClickEvent event) {
			doDoubleClick();
		}

	}

	private void doListSelected() {
		updateButtonState();
		if (fTreeAdapter != null) {
			fTreeAdapter.selectionChanged(this);
		}
	}

	private void doDoubleClick() {
		if (fTreeAdapter != null) {
			fTreeAdapter.doubleClicked(this);
		}
	}



}
