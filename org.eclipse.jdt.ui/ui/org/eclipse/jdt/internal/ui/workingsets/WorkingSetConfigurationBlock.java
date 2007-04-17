/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import com.ibm.icu.text.Collator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;


public class WorkingSetConfigurationBlock {
	
	private static final String WORKINGSET_SELECTION_HISTORY= "workingset_selection_history"; //$NON-NLS-1$
	private static final int MAX_HISTORY_SIZE= 5;
	
	private Label fLabel;
	private Combo fWorkingSetCombo;
	private Button fConfigure;
	private IWorkingSet[] fSelectedWorkingSets;
	private String[] fWorkingSetIDs;
	private String fMessage;
	private Button fEnableButton;
	private ArrayList fSelectionHistory;
	private final IDialogSettings fSettings;
	private final String fEnableButtonText;

	/**
	 * @param preSelectedWorkingSets the working sets which are selected when showning the block, not <b>null</b>
	 * @param compatibleWorkingSetIds only working sets with an id in compatibleWorkingSetIds can be selected, not <b>null</b>
	 * @param enableButtonText the text shown for the enable button, not <b>null</b>
	 * @param settings to store/load the selection history, not <b>null</b>
	 */
	public WorkingSetConfigurationBlock(IWorkingSet[] preSelectedWorkingSets, String[] compatibleWorkingSetIds, String enableButtonText, IDialogSettings settings) {
		Assert.isNotNull(preSelectedWorkingSets);
		Assert.isNotNull(compatibleWorkingSetIds);
		Assert.isNotNull(enableButtonText);
		Assert.isNotNull(settings);
		
		fEnableButtonText= enableButtonText;
		fSelectedWorkingSets= preSelectedWorkingSets;
		fWorkingSetIDs= compatibleWorkingSetIds;
		fSettings= settings;
		fSelectionHistory= loadSelectionHistory(settings, compatibleWorkingSetIds);
	}

	/**
	 * @param message the message to show to the user in the working set selection dialog
	 */
	public void setDialogMessage(String message) {
		fMessage= message;
	}
	
	/**
	 * @return the selected working sets, not <b>null</b>
	 */
	public IWorkingSet[] getSelectedWorkingSets() {
		if (fEnableButton.getSelection()) {
			return fSelectedWorkingSets;
		} else {
			return new IWorkingSet[0];
		}
	}

	/**
	 * Add this block to the <code>parent</parent>
	 * @param parent the parent to add the block to, not <b>null</b>
	 */
	public void createContent(final Composite parent) {
		int numColumn= 3;
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		composite.setLayout(new GridLayout(numColumn, false));
		
		fEnableButton= new Button(composite, SWT.CHECK);
		fEnableButton.setText(fEnableButtonText);
		GridData enableData= new GridData(SWT.FILL, SWT.CENTER, true, false);
		enableData.horizontalSpan= numColumn;
		fEnableButton.setLayoutData(enableData);
		fEnableButton.setSelection(fSelectedWorkingSets.length > 0);
					
		fLabel= new Label(composite, SWT.NONE);
		fLabel.setText(WorkingSetMessages.WorkingSetConfigurationBlock_WorkingSetText_name);
		
		fWorkingSetCombo= new Combo(composite, SWT.READ_ONLY | SWT.BORDER);
		GridData textData= new GridData(SWT.FILL, SWT.CENTER, true, false);
		textData.horizontalSpan= numColumn - 2;
		textData.horizontalIndent= 0;
		fWorkingSetCombo.setLayoutData(textData);
		
		fConfigure= new Button(composite, SWT.PUSH);
		fConfigure.setText(WorkingSetMessages.WorkingSetConfigurationBlock_SelectWorkingSet_button);
		GridData configureData= new GridData(SWT.LEFT, SWT.CENTER, false, false);
		configureData.widthHint= getButtonWidthHint(fConfigure);
		fConfigure.setLayoutData(configureData);
		fConfigure.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
				IWorkingSet[] workingSets= manager.getWorkingSets();
				SimpleWorkingSetSelectionDialog dialog= new SimpleWorkingSetSelectionDialog(parent.getShell(), workingSets, fWorkingSetIDs);
				dialog.setSelection(fSelectedWorkingSets);
				if (fMessage != null)
					dialog.setMessage(fMessage);

				if (dialog.open() == Window.OK) {
					IWorkingSet[] result= dialog.getSelection();
					if (result != null && result.length > 0) {
						fSelectedWorkingSets= result;
						manager.addRecentWorkingSet(result[0]);
					} else {
						fSelectedWorkingSets= new IWorkingSet[0];
					}
					updateWorkingSetSelection();
				}
			}
		});
		
		fEnableButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateEnableState(fEnableButton.getSelection());
			}
		});
		updateEnableState(fEnableButton.getSelection());
		
		fWorkingSetCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateSelectedWorkingSets();
			}
		});
		
		fWorkingSetCombo.setItems(getHistoryEntries());
		if (fSelectedWorkingSets.length == 0 && fSelectionHistory.size() > 0) {
			fWorkingSetCombo.select(historyIndex((String)fSelectionHistory.get(0)));
			updateSelectedWorkingSets();
		} else {
			updateWorkingSetSelection();
		}
	}
	
	private void updateEnableState(boolean enabled) {
		fLabel.setEnabled(enabled);
		fWorkingSetCombo.setEnabled(enabled);
		fConfigure.setEnabled(enabled);
	}
	
	private void updateWorkingSetSelection() {
		StringBuffer buf= new StringBuffer();
		
		if (fSelectedWorkingSets.length > 0) {
			buf.append(fSelectedWorkingSets[0].getLabel());
			for (int i= 1; i < fSelectedWorkingSets.length; i++) {
				IWorkingSet ws= fSelectedWorkingSets[i];
				buf.append(',').append(' ');
				buf.append(ws.getLabel());
			}
		}
			
		String currentSelection= buf.toString();
		int index= historyIndex(currentSelection);
		if (index >= 0) {
			historyInsert(currentSelection);
			fWorkingSetCombo.select(index);
		} else {
			historyInsert(currentSelection);
			fWorkingSetCombo.setItems(getHistoryEntries());
			fWorkingSetCombo.select(historyIndex(currentSelection));
		}
	}

	private String[] getHistoryEntries() {
		String[] history= (String[])fSelectionHistory.toArray(new String[fSelectionHistory.size()]);
		Arrays.sort(history, new Comparator() {
			public int compare(Object o1, Object o2) {
				return Collator.getInstance().compare(o1, o2);
			}
		});
		return history;
	}

	private void historyInsert(String entry) {
		fSelectionHistory.remove(entry);
		fSelectionHistory.add(0, entry);
		storeSelectionHistory(fSettings);
	}

	private int historyIndex(String entry) {
		for (int i= 0; i < fWorkingSetCombo.getItemCount(); i++) {
			if (fWorkingSetCombo.getItem(i).equals(entry))
				return i;
		}
		
		return -1;
	}
	
	private void updateSelectedWorkingSets() {
		String item= fWorkingSetCombo.getItem(fWorkingSetCombo.getSelectionIndex());
		String[] workingSetNames= item.split(", "); //$NON-NLS-1$
		
		IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
		fSelectedWorkingSets= new IWorkingSet[workingSetNames.length];
		for (int i= 0; i < workingSetNames.length; i++) {					
			IWorkingSet set= workingSetManager.getWorkingSet(workingSetNames[i]);
			Assert.isNotNull(set);
			fSelectedWorkingSets[i]= set;
		}
	}
	
	private void storeSelectionHistory(IDialogSettings settings) {
		String[] history;
		if (fSelectionHistory.size() > MAX_HISTORY_SIZE) {
			List subList= fSelectionHistory.subList(0, MAX_HISTORY_SIZE);
			history= (String[])subList.toArray(new String[subList.size()]);
		} else {
			history= (String[])fSelectionHistory.toArray(new String[fSelectionHistory.size()]);
		}
		settings.put(WORKINGSET_SELECTION_HISTORY, history);
	}
	
	private ArrayList loadSelectionHistory(IDialogSettings settings, String[] compatibleWorkingSetIds) {
		String[] strings= settings.getArray(WORKINGSET_SELECTION_HISTORY);
		if (strings == null || strings.length == 0)
			return new ArrayList();
		
		ArrayList result= new ArrayList();
		
		IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
		for (int i= 0; i < strings.length; i++) {
			String[] workingSetNames= strings[i].split(", "); //$NON-NLS-1$
			boolean valid= true;
			for (int j= 0; j < workingSetNames.length && valid; j++) {				
				IWorkingSet workingSet= workingSetManager.getWorkingSet(workingSetNames[j]);
				if (workingSet == null) {
					valid= false;
				} else {
					if (!contains(compatibleWorkingSetIds, workingSet.getId()))
						valid= false;
				}
			}
			if (valid) {
				result.add(strings[i]);
			}
		}
		
		return result;
	}

	private boolean contains(String[] compatibleWorkingSetIds, String id) {
		for (int i= 0; i < compatibleWorkingSetIds.length; i++) {
			if (compatibleWorkingSetIds[i].equals(id))
				return true;
		}
		
		return false;
	}
	
	private static int getButtonWidthHint(Button button) {
		button.setFont(JFaceResources.getDialogFont());
		
		GC gc = new GC(button);
		gc.setFont(button.getFont());
		FontMetrics fontMetrics= gc.getFontMetrics();
		gc.dispose();
		
		int widthHint= Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);
		return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}
}