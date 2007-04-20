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

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.SelectionDialog;

public class SimpleWorkingSetSelectionDialog extends SelectionDialog {
	
	private static class WorkingSetLabelProvider extends LabelProvider {
		
		private Map fIcons;
		
		public WorkingSetLabelProvider() {
			fIcons= new Hashtable();
		}
		
		public void dispose() {
			Iterator iterator= fIcons.values().iterator();
			while (iterator.hasNext()) {
				Image icon= (Image)iterator.next();
				icon.dispose();
			}
			super.dispose();
		}
		
		public Image getImage(Object object) {
			Assert.isTrue(object instanceof IWorkingSet);
			IWorkingSet workingSet= (IWorkingSet)object;
			ImageDescriptor imageDescriptor= workingSet.getImageDescriptor();
			if (imageDescriptor == null)
				return null;
			
			Image icon= (Image)fIcons.get(imageDescriptor);
			if (icon == null) {
				icon= imageDescriptor.createImage();
				fIcons.put(imageDescriptor, icon);
			}
			
			return icon;
		}
		
		public String getText(Object object) {
			Assert.isTrue(object instanceof IWorkingSet);
			IWorkingSet workingSet= (IWorkingSet)object;
			return workingSet.getName();
		}
		
	}
	
	private class Filter extends ViewerFilter {
		
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return isCompatible((IWorkingSet)element);
		}
				
		private boolean isCompatible(IWorkingSet set) {
			if (!set.isSelfUpdating() || set.isAggregateWorkingSet())
				return false;
			
			if (!set.isVisible())
				return false;
			
			return true;
		}
		
	}
	
	private final IWorkingSet[] fWorkingSets;
	private final IWorkingSet[] fInitialSelection;
	
	private CheckboxTableViewer fTableViewer;
	private IWorkingSet[] fCheckedElements;
	
	private Button fSelectAll;
	private Button fDeselectAll;

	public SimpleWorkingSetSelectionDialog(Shell shell, IWorkingSet[] allWorkingSet, IWorkingSet[] initialSelection) {
		super(shell);
		setTitle(WorkingSetMessages.SimpleWorkingSetSelectionDialog_SimpleSelectWorkingSetDialog_title);
		setHelpAvailable(false);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fWorkingSets= allWorkingSet;
		fInitialSelection= initialSelection;
		fCheckedElements= fInitialSelection;
	}
	
	protected final Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		composite.setFont(parent.getFont());

		createMessageArea(composite);
		Composite inner= new Composite(composite, SWT.NONE);
		inner.setFont(composite.getFont());
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		inner.setLayout(layout);
		
		Composite tableComposite= new Composite(inner, SWT.NONE);
		tableComposite.setFont(composite.getFont());
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		tableComposite.setLayout(layout);
		
		fTableViewer= createTableViewer(tableComposite);
		createRightButtonBar(inner);
		
		createBottomButtonBar(composite);
		
		return composite;
	}

	public IWorkingSet[] getSelection() {
		return fCheckedElements;
	}
	
	protected CheckboxTableViewer createTableViewer(Composite parent) {
		CheckboxTableViewer result= CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.MULTI);
		result.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				checkedStateChanged();
			}
		});
		GridData data= new GridData(GridData.FILL_BOTH);
		data.heightHint= convertHeightInCharsToPixels(20);
		data.widthHint= convertWidthInCharsToPixels(50);
		result.getTable().setLayoutData(data);
		result.getTable().setFont(parent.getFont());

		result.addFilter(createTableFilter());
		result.setLabelProvider(createTableLabelProvider());
		result.setSorter(createTableSorter());
		result.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object element) {
				return (Object[])element;
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		
		result.setInput(fWorkingSets);
		result.setCheckedElements(fInitialSelection);
		
		return result;
	}

	protected ViewerSorter createTableSorter() {
		return new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				IWorkingSet w1= (IWorkingSet)e1;
				IWorkingSet w2= (IWorkingSet)e2;
				return Collator.getInstance().compare(w1.getLabel(), w2.getLabel());
			}
		};
	}

	protected LabelProvider createTableLabelProvider() {
		return new WorkingSetLabelProvider();
	}

	protected ViewerFilter createTableFilter() {
		return new Filter();
	}
	
	protected void createRightButtonBar(Composite parent) {
		Composite buttons= new Composite(parent, SWT.NONE);
		buttons.setFont(parent.getFont());
		buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		buttons.setLayout(layout);

		createButtonsForRightButtonBar(buttons);
	}

	protected void createButtonsForRightButtonBar(Composite bar) {
		fSelectAll= new Button(bar, SWT.PUSH);
		fSelectAll.setText(WorkingSetMessages.SimpleWorkingSetSelectionDialog_SelectAll_button); 
		fSelectAll.setFont(bar.getFont());
		setButtonLayoutData(fSelectAll);
		fSelectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll();
			}
		});
		
		fDeselectAll= new Button(bar, SWT.PUSH);
		fDeselectAll.setText(WorkingSetMessages.SimpleWorkingSetSelectionDialog_DeselectAll_button); 
		fDeselectAll.setFont(bar.getFont());
		setButtonLayoutData(fDeselectAll);
		fDeselectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deselectAll();
			}
		});
	}
	
	protected void createBottomButtonBar(Composite parent) {
	}
	
	protected void checkedStateChanged() {
		List elements= Arrays.asList(fTableViewer.getCheckedElements());
		fCheckedElements= (IWorkingSet[])elements.toArray(new IWorkingSet[elements.size()]);
	}
	
	protected void selectAll() {
		fTableViewer.setAllChecked(true);
		checkedStateChanged();
	}
	
	protected void deselectAll() {
		fTableViewer.setAllChecked(false);
		checkedStateChanged();
	}
}