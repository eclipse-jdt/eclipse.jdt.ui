/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jun 10, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.Arrays;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

/**
 * @author tma
 * 
 * To change the template for this generated type comment go to Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FiltersDialog extends SelectionStatusDialog {

	private CheckboxTableViewer listViewer;
	private JavaSearchResultPage fPage;
	private Button fLimitElementsCheckbox;
	private Text fLimitElementsField;
	
	private int fLimitElementCount= 1000;
	private boolean fLimitElements= false;

	public FiltersDialog(JavaSearchResultPage page) {
		super(page.getSite().getShell());
		setTitle(org.eclipse.jdt.internal.ui.search.SearchMessages.getString("FiltersDialog.title")); //$NON-NLS-1$
		setStatusLineAboveButtons(true);
		fPage = page;
	}

	public MatchFilter[] getEnabledFilters() {
		Object[] result = getResult();
		MatchFilter[] filters = new MatchFilter[result.length];
		System.arraycopy(result, 0, filters, 0, filters.length);
		return filters;
	}

	public boolean isLimitEnabled() {
		return fLimitElements;
	}

	/**
	 * @return returns the number of entries to limit the filters entry to
	 */
	public int getElementLimit() {
		return fLimitElementCount;
	}

	/*
	 * (non-Javadoc) Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite composite) {
		Composite parent = (Composite) super.createDialogArea(composite);

		createTableLimit(parent);
		// Create list viewer
		Label l= new Label(parent, SWT.NONE);
		l.setText(org.eclipse.jdt.internal.ui.search.SearchMessages.getString("FiltersDialog.filters.label")); //$NON-NLS-1$
		
		Table table = new Table(parent, SWT.CHECK | SWT.BORDER);
		listViewer = new CheckboxTableViewer(table);

		GridData data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);

		listViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				// Return the features's label.
				return ((MatchFilter) element).getName();
			}
		});

		// Set the content provider
		ArrayContentProvider cp = new ArrayContentProvider();
		listViewer.setContentProvider(cp);
		listViewer.setInput(MatchFilter.allFilters());
		listViewer.setCheckedElements(fPage.getMatchFilters());

		l= new Label(parent, SWT.NONE);
		l.setText(org.eclipse.jdt.internal.ui.search.SearchMessages.getString("FiltersDialog.description.label")); //$NON-NLS-1$
		final Text description = new Text(parent, SWT.LEFT | SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = convertHeightInCharsToPixels(3);
		description.setLayoutData(data);
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object selectedElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (selectedElement != null)
					description.setText(((MatchFilter) selectedElement).getDescription());
				else
					description.setText(""); //$NON-NLS-1$
			}
		});
		return parent;
	}


	private void createTableLimit(Composite ancestor) {
		Composite parent = new Composite(ancestor, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.numColumns = 2;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		parent.setLayout(gl);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		parent.setLayoutData(gd);

		fLimitElementsCheckbox = new Button(parent, SWT.CHECK);
		fLimitElementsCheckbox.setText(org.eclipse.jdt.internal.ui.search.SearchMessages.getString("FiltersDialog.limit.label"));  //$NON-NLS-1$
		fLimitElementsCheckbox.setLayoutData(new GridData());

		fLimitElementsField = new Text(parent, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(6);
		fLimitElementsField.setLayoutData(gd);

		applyDialogFont(parent);

		fLimitElementsCheckbox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLimitValueEnablement();
			}

		});

		fLimitElementsField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				validateText();
			}
		});
		initLimit();
	}

	private void initLimit() {
		boolean limit = fPage.limitElements();
		int count = fPage.getElementLimit();
		fLimitElementsCheckbox.setSelection(limit);
		fLimitElementsField.setText(String.valueOf(count));

		updateLimitValueEnablement();
	}

	private void updateLimitValueEnablement() {
		fLimitElementsField.setEnabled(fLimitElementsCheckbox.getSelection());
	}

	protected void validateText() {
		String text = fLimitElementsField.getText();
		int value = -1;
		try {
			value = Integer.valueOf(text).intValue();
		} catch (NumberFormatException e) {

		}
		if (fLimitElementsCheckbox.getSelection() && value <= 0)
			updateStatus(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, org.eclipse.jdt.internal.ui.search.SearchMessages.getString("FiltersDialog.limit.error"), null)); //$NON-NLS-1$
		else
			updateStatus(new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null)); //$NON-NLS-1$
	}

	protected void computeResult() {
		fLimitElementCount= Integer.valueOf(fLimitElementsField.getText()).intValue();
		fLimitElements= fLimitElementsCheckbox.getSelection();

		setResult(Arrays.asList(listViewer.getCheckedElements()));
	}
}