/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

public class FilterDialog extends SelectionDialog {

	private String[] fEnabledFilterIds;

	private static MatchFilter[] fgBuiltInFilters= new MatchFilter[] {
			new ReadFilter(),
			new WriteFilter(),
			new ImportFilter()
		};
;

	private CheckboxTableViewer fCheckBoxList;
	
	private JavaSearchResultPage fPage;

	private Stack fFilterDescriptorChangeHistory;

	
	/**
	 * Creates a dialog to customize Java element filters.
	 * 
	 * @param shell the parent shell
	 * @param viewId the id of the view
	 * @param enablePatterns <code>true</code> if pattern filters are enabled
	 * @param patterns the filter patterns
	 * @param enabledFilterIds the Ids of the enabled filters
	 */
	public FilterDialog(
			Shell shell,
			JavaSearchResultPage page) {

		super(shell);
		
		fPage= page;

		fFilterDescriptorChangeHistory= new Stack();
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	protected void configureShell(Shell shell) {
		setTitle("Match Filters");
		setMessage("Select Match Filters");
		super.configureShell(shell);
		WorkbenchHelp.setHelp(shell, IJavaHelpContextIds.CUSTOM_FILTERS_DIALOG);
	}

	/**
	 * Overrides method in Dialog
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(Composite)
	 */	
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);
		// create a composite with standard margins and spacing
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());
		Composite group= composite;
		
		createCheckBoxList(group);
				
		applyDialogFont(parent);		
		return parent;
	}

	private void createCheckBoxList(Composite parent) {
		// Filler
		new Label(parent, SWT.NONE);
		
		Label info= new Label(parent, SWT.LEFT);
		info.setText("info text");  //$NON-NLS-1$
		
		fCheckBoxList= CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		GridData data= new GridData(GridData.FILL_BOTH);
		data.heightHint= fCheckBoxList.getTable().getItemHeight() * 10;
		fCheckBoxList.getTable().setLayoutData(data);

		fCheckBoxList.setLabelProvider(createLabelPrivder());
		fCheckBoxList.setContentProvider(new ArrayContentProvider());

		fCheckBoxList.setInput(fgBuiltInFilters);
		setInitialSelections(fPage.getMatchFilters());
		
		List initialSelection= getInitialElementSelections();
		if (initialSelection != null && !initialSelection.isEmpty())
			checkInitialSelections();

		// Description
		info= new Label(parent, SWT.LEFT);
		info.setText("Filter Description");
		final Text description= new Text(parent, SWT.LEFT | SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint= convertHeightInCharsToPixels(3);
		description.setLayoutData(data);
		fCheckBoxList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection= event.getSelection();
				if (selection instanceof IStructuredSelection) {
					Object selectedElement= ((IStructuredSelection)selection).getFirstElement();
					if (selectedElement instanceof MatchFilter)
						description.setText(((MatchFilter)selectedElement).getDescription());
				}
			}
		});
		fCheckBoxList.addCheckStateListener(new ICheckStateListener() {
			/*
			 * @see org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers.CheckStateChangedEvent)
			 */
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element= event.getElement();
				if (element instanceof MatchFilter) {
					// renew if already touched
					if (fFilterDescriptorChangeHistory.contains(element))
						fFilterDescriptorChangeHistory.remove(element);
					fFilterDescriptorChangeHistory.push(element);
				}
			}});

		addSelectionButtons(parent);
	}

	private void addSelectionButtons(Composite composite) {
		Composite buttonComposite= new Composite(composite, SWT.RIGHT);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		buttonComposite.setLayout(layout);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace= true;
		composite.setData(data);

		// Select All button
		String label= "Select All";
		Button selectButton= createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, label, false);
		SWTUtil.setButtonDimensionHint(selectButton);
		SelectionListener listener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fCheckBoxList.setAllChecked(true);
				fFilterDescriptorChangeHistory.clear();
				for (int i= 0; i < fgBuiltInFilters.length; i++)
					fFilterDescriptorChangeHistory.push(fgBuiltInFilters[i]);
			}
		};
		selectButton.addSelectionListener(listener);

		// De-select All button
		label= "Deselect All";
		Button deselectButton= createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, label, false);
		SWTUtil.setButtonDimensionHint(deselectButton);
		listener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fCheckBoxList.setAllChecked(false);
				fFilterDescriptorChangeHistory.clear();
				for (int i= 0; i < fgBuiltInFilters.length; i++)
					fFilterDescriptorChangeHistory.push(fgBuiltInFilters[i]);
			}
		};
		deselectButton.addSelectionListener(listener);
	}

	private void checkInitialSelections() {
		Iterator itemsToCheck= getInitialElementSelections().iterator();
		while (itemsToCheck.hasNext())
			fCheckBoxList.setChecked(itemsToCheck.next(),true);
	}

	protected void okPressed() {
		if (fgBuiltInFilters != null) {
			ArrayList result= new ArrayList();
			for (int i= 0; i < fgBuiltInFilters.length; ++i) {
				if (fCheckBoxList.getChecked(fgBuiltInFilters[i]))
					result.add(fgBuiltInFilters[i]);
			}
			setResult(result);
		}
		super.okPressed();
	}
	
	private ILabelProvider createLabelPrivder() {
		return 
			new LabelProvider() {
				public Image getImage(Object element) {
					return null;
				}
				public String getText(Object element) {
					if (element instanceof MatchFilter)
						return ((MatchFilter)element).getName();
					else
						return null;
				}
			};
	}

	// ---------- result handling ----------


	/**
	 * @return a stack with the filter descriptor check history
	 * @since 3.0
	 */
	public Stack getFilterDescriptorChangeHistory() {
		return fFilterDescriptorChangeHistory;
	}


}
