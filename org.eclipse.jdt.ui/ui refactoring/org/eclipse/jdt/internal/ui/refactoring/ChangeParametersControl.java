/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.JavaConventions;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;

/**
 * A special control to edit and reorder method parameters.
 */
public class ChangeParametersControl extends Composite {

	private static class ParameterInfoContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			return ((List) inputElement).toArray();
		}
		public void dispose() {
			// do nothing
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
		}
	}

	private static class ParameterInfoLabelProvider extends LabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		public String getColumnText(Object element, int columnIndex) {
			ParameterInfo info= (ParameterInfo) element;
			if (columnIndex == TYPE_PROP)
				return info.getType();
			if (columnIndex == NEWNAME_PROP)
				return info.getNewName();
			if (columnIndex == DEFAULT_PROP)
				return info.getDefaultValue();
			Assert.isTrue(false);
			return ""; //$NON-NLS-1$
		}
	}

	private class ReorderParametersCellModifier implements ICellModifier {
		public boolean canModify(Object element, String property) {
			if (!(element instanceof ParameterInfo))
				return false;
			if (property.equals(PROPERTIES[NEWNAME_PROP]))
				return true;
			return (((ParameterInfo)element).isAdded());
		}
		public Object getValue(Object element, String property) {
			if (!(element instanceof ParameterInfo))
				return null;
			if (property.equals(PROPERTIES[TYPE_PROP]))
				return ((ParameterInfo) element).getType();
			else if (property.equals(PROPERTIES[NEWNAME_PROP]))
				return ((ParameterInfo) element).getNewName();
			else if (property.equals(PROPERTIES[DEFAULT_PROP]))
				return ((ParameterInfo) element).getDefaultValue();
			Assert.isTrue(false);
			return null;
		}
		public void modify(Object element, String property, Object value) {
			if (!(element instanceof TableItem))
				return;
			Object data= ((TableItem) element).getData();
			if (!(data instanceof ParameterInfo))
				return;
			ParameterInfo parameterInfo= (ParameterInfo) data;
			if (property.equals(PROPERTIES[NEWNAME_PROP])) 
				parameterInfo.setNewName((String) value);
			else if (property.equals(PROPERTIES[DEFAULT_PROP]))
				parameterInfo.setDefaultValue((String) value);
			else if (property.equals(PROPERTIES[TYPE_PROP]))
				parameterInfo.setType((String) value);
 			else 
 				Assert.isTrue(false);
			fListener.parameterChanged(parameterInfo);
			fTableViewer.update(parameterInfo, new String[] { property });
		}
	};

	private static final String[] PROPERTIES= { "type", "new", "default" }; //$NON-NLS-2$ //$NON-NLS-1$
	private static final int TYPE_PROP= 0;
	private static final int NEWNAME_PROP= 1;
	private static final int DEFAULT_PROP= 2;

	private static final int ROW_COUNT= 10;

	private final boolean fEditable;
	private final boolean fCanAddParameters;
	private final ParameterListChangeListener fListener;

	private Button fUpButton;
	private Button fDownButton;
	private TableViewer fTableViewer;
	private Button fEditButton;
	private Button fAddButton;
	private Button fRemoveButton;
	private List fParameterInfos;

	public ChangeParametersControl(Composite parent, int style, String label, ParameterListChangeListener listener, boolean editable, boolean canAddParameters) {
		super(parent, style);
		Assert.isNotNull(listener);
		fListener= listener;
		fEditable= editable;
		fCanAddParameters= canAddParameters;
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		setLayout(layout);

		Label tableLabel= new Label(this, SWT.NONE);
		GridData labelGd= new GridData();
		labelGd.horizontalSpan= 2;
		tableLabel.setLayoutData(labelGd);
		tableLabel.setText(label); //$NON-NLS-1$

		createParameterList(this);
		createButtonComposite(this);
	}

	public void setInput(List parameterInfos) {
		Assert.isNotNull(parameterInfos);
		fParameterInfos= parameterInfos;
		fTableViewer.setInput(fParameterInfos);
		if (fParameterInfos.size() > 0)
			fTableViewer.setSelection(new StructuredSelection(fParameterInfos.get(0)));
	}

	// ---- Parameter table -----------------------------------------------------------------------------------

	private void createParameterList(Composite parent) {
		TableLayoutComposite layouter= new TableLayoutComposite(parent, SWT.NULL);
		addColumnLayoutData(layouter);
		
		Table table= new Table(layouter, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableColumn tc;
		tc= new TableColumn(table, SWT.NONE, TYPE_PROP);
		tc.setResizable(true);
		tc.setText(RefactoringMessages.getString("ChangeParametersControl.table.type")); //$NON-NLS-1$
		
		tc= new TableColumn(table, SWT.NONE, NEWNAME_PROP);
		tc.setResizable(true);
		tc.setText(RefactoringMessages.getString("ChangeParametersControl.table.name")); //$NON-NLS-1$

		if (fCanAddParameters){
			tc= new TableColumn(table, SWT.NONE, DEFAULT_PROP);
			tc.setResizable(true);
			tc.setText("Default value");
		}	
		
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.heightHint= table.getGridLineWidth() + table.getItemHeight() * ROW_COUNT;
		gd.widthHint= 40;
		layouter.setLayoutData(gd);

		fTableViewer= new TableViewer(table);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.setContentProvider(new ParameterInfoContentProvider());
		fTableViewer.setLabelProvider(new ParameterInfoLabelProvider());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsEnabledState();
			}
		});

		if (fEditable)
			addCellEditors();
	}

	private void addColumnLayoutData(TableLayoutComposite layouter) {
		if (fCanAddParameters){
			layouter.addColumnData(new ColumnWeightData(33, true));
			layouter.addColumnData(new ColumnWeightData(33, true));
			layouter.addColumnData(new ColumnWeightData(34, true));
		} else {
			layouter.addColumnData(new ColumnWeightData(50, true));
			layouter.addColumnData(new ColumnWeightData(50, true));
		}	
	}

	private ParameterInfo[] getSelectedItems() {
		ISelection selection= fTableViewer.getSelection();
		if (selection == null)
			return new ParameterInfo[0];

		if (!(selection instanceof IStructuredSelection))
			return new ParameterInfo[0];

		List selected= ((IStructuredSelection) selection).toList();
		return (ParameterInfo[]) selected.toArray(new ParameterInfo[selected.size()]);
	}

	// ---- Button bar --------------------------------------------------------------------------------------

	private void createButtonComposite(Composite parent) {
		Composite buttonComposite= new Composite(parent, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		buttonComposite.setLayout(gl);

		fUpButton= createButton(buttonComposite, RefactoringMessages.getString("ChangeParametersControl.buttons.move_up"), true); //$NON-NLS-1$
		fDownButton= createButton(buttonComposite, RefactoringMessages.getString("ChangeParametersControl.buttons.move_down"), false); //$NON-NLS-1$
		if (fEditable)
			fEditButton= createEditButton(buttonComposite);
		if(fCanAddParameters){
			fAddButton= createAddButton(buttonComposite);	
			fRemoveButton= createRemoveButton(buttonComposite);
		}	
		updateButtonsEnabledState();
	}

	private void updateButtonsEnabledState() {
		fUpButton.setEnabled(canMove(true));
		fDownButton.setEnabled(canMove(false));
		if (fEditButton != null)
			fEditButton.setEnabled(fTableViewer.getTable().getSelectionIndices().length == 1);
		if (fAddButton != null)
			fAddButton.setEnabled(true);	
		if (fRemoveButton != null)
			fRemoveButton.setEnabled(areAllSelectedNew());
	}

	private boolean areAllSelectedNew() {
		ParameterInfo[] selected= getSelectedItems();
		for (int i = 0; i < selected.length; i++) {
			if (! selected[i].isAdded())
				return false;
				
		}
		return true;
	}


	private Button createEditButton(Composite buttonComposite) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.getString("ChangeParametersControl.buttons.edit")); //$NON-NLS-1$
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ISelection selection= fTableViewer.getSelection();
				try {
					ParameterInfo[] selected= getSelectedItems();
					Assert.isTrue(selected.length == 1);
					ParameterInfo parameterInfo= selected[0];
					String key= RefactoringMessages.getString("ChangeParametersControl.inputdialog.message"); //$NON-NLS-1$
					String message= MessageFormat.format(key, new String[] { parameterInfo.getOldName()});
					IInputValidator validator= createParameterNameValidator(parameterInfo.getOldName());
					InputDialog dialog= new InputDialog(getShell(), RefactoringMessages.getString("ChangeParametersControl.inputDialog.title"), message, parameterInfo.getNewName(), validator); //$NON-NLS-1$
					if (dialog.open() == InputDialog.CANCEL) {
						fTableViewer.setSelection(selection);
						return;
					}
					parameterInfo.setNewName(dialog.getValue());
					fListener.parameterChanged(parameterInfo);
					fTableViewer.update(parameterInfo, new String[] { PROPERTIES[NEWNAME_PROP] });
				} finally {
					fTableViewer.refresh();
					fTableViewer.getControl().setFocus();
					fTableViewer.setSelection(selection);
				}
			}
		});
		return button;
	}

	private Button createAddButton(Composite buttonComposite) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText("Add");
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fParameterInfos.add(ParameterInfo.createInfoForAddedParameter());
				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				fTableViewer.getTable().setSelection(fParameterInfos.size() - 1);
				fListener.parameterListChanged();
				updateButtonsEnabledState();
			}
		});	
		return button;
	}

	private Button createRemoveButton(Composite buttonComposite) {
		final Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText("Remove");
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Assert.isTrue(areAllSelectedNew());
				ParameterInfo[] selected= getSelectedItems();
				for (int i= 0; i < selected.length; i++) {
					fParameterInfos.remove(selected[i]);
				}
				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				fListener.parameterListChanged();
				button.setEnabled(false);
			}
		});	
		return button;
	}

	private Button createButton(Composite buttonComposite, String text, final boolean up) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ISelection savedSelection= fTableViewer.getSelection();
				if (savedSelection == null)
					return;
				ParameterInfo[] selection= getSelectedItems();
				if (selection.length == 0)
					return;

				if (up)
					moveUp(selection);
				else
					moveDown(selection);

				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				fTableViewer.setSelection(savedSelection);
				fListener.parameterListChanged();
			}
		});
		return button;
	}

	//---- editing -----------------------------------------------------------------------------------------------

	private static IInputValidator createParameterNameValidator(final String oldName){
		return new IInputValidator(){
	        public String isValid(String newText) {
	        	if (newText.equals("")) //$NON-NLS-1$
	        		return ""; //$NON-NLS-1$
	        	if (newText.equals(oldName))
	        		return ""; //$NON-NLS-1$
	        	IStatus status= JavaConventions.validateFieldName(newText);
	        	if (status.getSeverity() == IStatus.ERROR)
	        		return status.getMessage();
	            return null;
	        }
		};
	}

	private void addCellEditors() {
		Table table= fTableViewer.getTable();
		final CellEditor editors[]= new CellEditor[PROPERTIES.length];

		class AutoApplyTextCellEditor extends TextCellEditor {
			public AutoApplyTextCellEditor(Composite parent) {
				super(parent);
			}
			public void fireApplyEditorValue() {
				super.fireApplyEditorValue();
			}
		};

		editors[TYPE_PROP]= new AutoApplyTextCellEditor(table);
		editors[TYPE_PROP].getControl().addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				((AutoApplyTextCellEditor) editors[TYPE_PROP]).fireApplyEditorValue();
			}
		});

		editors[NEWNAME_PROP]= new AutoApplyTextCellEditor(table);
		editors[NEWNAME_PROP].getControl().addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				((AutoApplyTextCellEditor) editors[NEWNAME_PROP]).fireApplyEditorValue();
			}
		});
		editors[DEFAULT_PROP]= new AutoApplyTextCellEditor(table);
		editors[DEFAULT_PROP].getControl().addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				((AutoApplyTextCellEditor) editors[DEFAULT_PROP]).fireApplyEditorValue();
			}
		});

		fTableViewer.setCellEditors(editors);
		fTableViewer.setColumnProperties(PROPERTIES);
		fTableViewer.setCellModifier(new ReorderParametersCellModifier());
	}

	//---- change order ----------------------------------------------------------------------------------------

	private void moveUp(ParameterInfo[] selection) {
		moveUp(fParameterInfos, Arrays.asList(selection));
	}

	private void moveDown(ParameterInfo[] selection) {
		Collections.reverse(fParameterInfos);
		moveUp(fParameterInfos, Arrays.asList(selection));
		Collections.reverse(fParameterInfos);
	}

	private static void moveUp(List elements, List move) {
		List res= new ArrayList(elements.size());
		Object floating= null;
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object curr= iter.next();
			if (move.contains(curr)) {
				res.add(curr);
			} else {
				if (floating != null)
					res.add(floating);
				floating= curr;
			}
		}
		if (floating != null) {
			res.add(floating);
		}
		elements.clear();
		for (Iterator iter= res.iterator(); iter.hasNext();) {
			elements.add(iter.next());
		}
	}

	private boolean canMove(boolean up) {
		if (fParameterInfos == null || fParameterInfos.size() == 0)
			return false;
		int[] indc= fTableViewer.getTable().getSelectionIndices();
		int invalid= up ? 0 : fParameterInfos.size() - 1;
		if (indc.length == 0)
			return false;
		for (int i= 0; i < indc.length; i++) {
			if (indc[i] == invalid)
				return false;
		}
		return true;
	}
}