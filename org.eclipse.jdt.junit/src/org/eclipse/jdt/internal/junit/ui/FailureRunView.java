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
package org.eclipse.jdt.internal.junit.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.eclipse.jdt.junit.ITestRunListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;


/**
 * A view presenting the failed tests in a table.
 */
class FailureRunView implements ITestRunView, IMenuListener {
	private Table fTable;
	private TestRunnerViewPart fRunnerViewPart;
	private final Clipboard fClipboard;	
	private boolean fMoveSelection= false;
	
	private final Image fErrorIcon= TestRunnerViewPart.createImage("obj16/testerr.gif"); //$NON-NLS-1$
	private final Image fFailureIcon= TestRunnerViewPart.createImage("obj16/testfail.gif"); //$NON-NLS-1$
	private final Image fFailureTabIcon= TestRunnerViewPart.createImage("obj16/failures.gif"); //$NON-NLS-1$

	public FailureRunView(CTabFolder tabFolder, Clipboard clipboard, TestRunnerViewPart runner) {
		fRunnerViewPart= runner;
		fClipboard= clipboard;
		
		CTabItem failureTab= new CTabItem(tabFolder, SWT.NONE);
		failureTab.setText(getName());
		failureTab.setImage(fFailureTabIcon);

		Composite composite= new Composite(tabFolder, SWT.NONE);
		GridLayout gridLayout= new GridLayout();
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		composite.setLayout(gridLayout);
		
		GridData gridData= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		composite.setLayoutData(gridData);	
			
		fTable= new Table(composite, SWT.NONE);
		gridLayout= new GridLayout();
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		fTable.setLayout(gridLayout);
		
		gridData= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		fTable.setLayoutData(gridData);
		
		failureTab.setControl(composite);
		failureTab.setToolTipText(JUnitMessages.getString("FailureRunView.tab.tooltip")); //$NON-NLS-1$
		
		initMenu();
		addListeners();	
	}

	private void disposeIcons() {
		fErrorIcon.dispose();
		fFailureIcon.dispose();
		fFailureTabIcon.dispose();
	}

	private void initMenu() {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this);
		Menu menu= menuMgr.createContextMenu(fTable);
		fTable.setMenu(menu);
	}
	
	public String getName() {
		return JUnitMessages.getString("FailureRunView.tab.title"); //$NON-NLS-1$
	}
	
	public String getSelectedTestId() {
		int index= fTable.getSelectionIndex();
		if (index == -1)
			return null;
		return getTestInfo(fTable.getItem(index)).getTestId();
	}
	
	public String getAllFailedTestNames() {
		StringBuffer trace= new StringBuffer();
		String lineDelim= System.getProperty("line.separator", "\n");  //$NON-NLS-1$//$NON-NLS-2$
		for (int i= 0; i < fTable.getItemCount(); i++) {
			TestRunInfo testInfo= getTestInfo(fTable.getItem(i));
			trace.append(testInfo.getTestName()).append(lineDelim);
			String failureTrace= testInfo.getTrace();
			if (failureTrace != null) {
				StringReader stringReader= new StringReader(failureTrace);
				BufferedReader bufferedReader= new BufferedReader(stringReader);
				String line;
				try {
					while ((line= bufferedReader.readLine()) != null) 
						trace.append(line+lineDelim);
				} catch (IOException e) {
					trace.append(lineDelim);
				}	
			}
		}
		return trace.toString();
	}
	
	private String getClassName() {
		TableItem item= getSelectedItem();
		TestRunInfo info= getTestInfo(item);
		return info.getClassName();
	}
	
	private String getMethodName() {
		TableItem item= getSelectedItem();
		TestRunInfo info= getTestInfo(item);
		return info.getTestMethodName();
	}
	
	public void menuAboutToShow(IMenuManager manager){
		if (fTable.getSelectionCount() > 0) {
			String className= getClassName();
			String methodName= getMethodName();
			if (className != null) {
				manager.add(new OpenTestAction(fRunnerViewPart, className, methodName));
				manager.add(new RerunAction(fRunnerViewPart, getSelectedTestId(), className, methodName));
				manager.add(new CopyFailureListAction(fRunnerViewPart, FailureRunView.this, fClipboard));
			}
		}
	}		
	
	private TableItem getSelectedItem() {
		int index= fTable.getSelectionIndex();
		if (index == -1)
			return null;
		return fTable.getItem(index);
	}
	
	public void setSelectedTest(String testId){
		TableItem[] items= fTable.getItems();
		for (int i= 0; i < items.length; i++) {
			TableItem tableItem= items[i];
			TestRunInfo info= getTestInfo(tableItem);		
			if (info.getTestId().equals(testId)){
				fTable.setSelection(new TableItem[] { tableItem });
				fTable.showItem(tableItem);
				return;
			}
		}
	}

	private TestRunInfo getTestInfo(TableItem item) {
		return (TestRunInfo)item.getData(); 	
	}
	
	public void setFocus() {
		fTable.setFocus();
	}
	
	public void endTest(String testId){
		TestRunInfo testInfo= fRunnerViewPart.getTestInfo(testId);
		if(testInfo == null || testInfo.getStatus() == ITestRunListener.STATUS_OK) 
			return;

		TableItem tableItem= new TableItem(fTable, SWT.NONE);
		updateTableItem(testInfo, tableItem);
		fTable.showItem(tableItem);
	}

	private void updateTableItem(TestRunInfo testInfo, TableItem tableItem) {
		String label= JUnitMessages.getFormattedString("FailureRunView.labelfmt", new String[] { testInfo.getTestMethodName(), testInfo.getClassName() }); //$NON-NLS-1$
		tableItem.setText(label);
		if (testInfo.getStatus() == ITestRunListener.STATUS_FAILURE)
			tableItem.setImage(fFailureIcon);
		else
			tableItem.setImage(fErrorIcon);
		tableItem.setData(testInfo);
	}

	private TableItem findItem(String testId) {
		TableItem[] items= fTable.getItems();
		for (int i= 0; i < items.length; i++) {
			TestRunInfo info= getTestInfo(items[i]);
			if (info.getTestId().equals(testId))
				return items[i];
		}
		return null;
	}

	public void activate() {
		fMoveSelection= false;
		testSelected();
	}

	public void aboutToStart() {
		fMoveSelection= false;
		fTable.removeAll();
	}

	private void testSelected() {
		fRunnerViewPart.handleTestSelected(getSelectedTestId());
	}
	
	private void addListeners() {
		fTable.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				activate();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				activate();
			}
		});
		
		fTable.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});

		fTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e){
				handleDoubleClick(e);
			}
			public void mouseDown(MouseEvent e) {
				activate();
			}
			public void mouseUp(MouseEvent e) {
				activate();
			}
		});
	}
	
	void handleDoubleClick(MouseEvent e) {
		if (fTable.getSelectionCount() > 0) 
			new OpenTestAction(fRunnerViewPart, getClassName(), getMethodName()).run();
	}
	
	public void newTreeEntry(String treeEntry) {
	}
	
	/*
	 * @see ITestRunView#testStatusChanged(TestRunInfo)
	 */
	public void testStatusChanged(TestRunInfo info) {
		TableItem item= findItem(info.getTestId());
		if (item != null) {
			if (info.getStatus() == ITestRunListener.STATUS_OK) {
				item.dispose();
				return;
			}
			updateTableItem(info, item);
		} 
		if (item == null && info.getStatus() != ITestRunListener.STATUS_OK) {
			item= new TableItem(fTable, SWT.NONE);
			updateTableItem(info, item);
		}
		if (item != null)
			fTable.showItem(item);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.ui.ITestRunView#startTest(java.lang.String)
	 */
	public void startTest(String testId) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.ui.ITestRunView#selectNext()
	 */
	public void selectNext() {
		if (fTable.getItemCount() == 0)
			return;
			
		int index= fTable.getSelectionIndex();
		if (index == -1)
			index= 0;
		
		if (fMoveSelection)
			index= Math.min(fTable.getItemCount()-1, index+1);
		else
			fMoveSelection= true;
			
		selectTest(index);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.ui.ITestRunView#selectPrevious()
	 */
	public void selectPrevious() {
		if (fTable.getItemCount() == 0)
			return;
			
		int index= fTable.getSelectionIndex();
		if (index == -1)
			index= fTable.getItemCount()-1;
			
		if (fMoveSelection)
			index= Math.max(0, index-1);
		else
			fMoveSelection= true;
			
		selectTest(index);
	}

	private void selectTest(int index) {
		TableItem item= fTable.getItem(index);
		TestRunInfo info= getTestInfo(item);
		fRunnerViewPart.showTest(info);
	}

	public void aboutToEnd() {
	}
}
