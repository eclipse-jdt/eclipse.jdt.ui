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

import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * @author Thomas Mäder
 *
 */
public class ParticipantSelectionDialog extends SelectionDialog {

	private CheckboxTableViewer fViewer;
	
	static class ParticipantLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return getImage(element);
		}

		public String getColumnText(Object element, int columnIndex) {
			return getText(element);
		}
		
		public String getText(Object element) {
			return ((IConfigurationElement)element).getAttribute("label");
		}

	}
	
	protected ParticipantSelectionDialog(Shell parentShell) {
		super(parentShell);
		setTitle("Select Search Extensions");
		setMessage("Select the search extensions to use in java search");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		parent= (Composite) super.createDialogArea(parent);
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		result.setLayout(gl);

		Label message= createMessageArea(result);
		//message.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fViewer= CheckboxTableViewer.newCheckList(result, SWT.BORDER);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(60);
		gd.heightHint= convertHeightInCharsToPixels(15);
		fViewer.getControl().setLayoutData(gd);
		fViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return Platform.getPluginRegistry().getConfigurationElementsFor(JavaSearchPage.PARTICIPANT_EXTENSION_POINT);
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		
		fViewer.setLabelProvider(new ParticipantLabelProvider());
		
		fViewer.setInput(new Object());
		return result;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		setSelectionResult(fViewer.getCheckedElements());
		super.okPressed();
	}
	
	public void create() {
		super.create();

		List initialSelections= getInitialElementSelections();
		if (initialSelections.size() > 0) {
			fViewer.setCheckedElements(initialSelections.toArray());			
		}
	}		


}
