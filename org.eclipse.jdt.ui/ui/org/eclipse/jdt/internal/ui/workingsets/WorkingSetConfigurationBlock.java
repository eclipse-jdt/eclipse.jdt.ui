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

import org.eclipse.core.runtime.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;


public class WorkingSetConfigurationBlock {
	
	private Label fLabel;
	private Text fText;
	private Button fConfigure;
	private IWorkingSet[] fSelectedWorkingSets;
	private String[] fWorkingSetIDs;
	private String fMessage;

	/**
	 * @param preSelectedWorkingSets the working sets which are selected when showning the block, not <b>null</b>
	 * @param compatibleWorkingSetIds only working sets with an id in compatibleWorkingSetIds can be selected, not <b>null</b>
	 */
	public WorkingSetConfigurationBlock(IWorkingSet[] preSelectedWorkingSets, String[] compatibleWorkingSetIds) {
		Assert.isNotNull(preSelectedWorkingSets);
		Assert.isNotNull(compatibleWorkingSetIds);
		
		fSelectedWorkingSets= preSelectedWorkingSets;
		fWorkingSetIDs= compatibleWorkingSetIds;
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
		return fSelectedWorkingSets;
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
					
		fLabel= new Label(composite, SWT.NONE);
		fLabel.setText(WorkingSetMessages.WorkingSetConfigurationBlock_WorkingSetText_name);
		
		fText= new Text(composite, SWT.READ_ONLY | SWT.BORDER);
		GridData textData= new GridData(SWT.FILL, SWT.CENTER, true, false);
		textData.horizontalSpan= numColumn - 2;
		textData.horizontalIndent= 0;
		fText.setLayoutData(textData);
		
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
		
		updateWorkingSetSelection();
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
		
		fText.setText(buf.toString());
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