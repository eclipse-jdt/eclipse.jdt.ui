/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.jdt.junit.ITestRunListener;


/**
 * A view presenting the failed tests in a table.
 */
class FailureRunView implements ITestRunView, IMenuListener {
	private Table fTable;
	private TestRunnerViewPart fRunnerViewPart;
	private boolean fPressed= false;
	
	private final Image fErrorIcon= TestRunnerViewPart.createImage("obj16/testerr.gif"); //$NON-NLS-1$
	private final Image fFailureIcon= TestRunnerViewPart.createImage("obj16/testfail.gif"); //$NON-NLS-1$
	private final Image fFailureTabIcon= TestRunnerViewPart.createImage("obj16/failures.gif"); //$NON-NLS-1$
	
	public FailureRunView(CTabFolder tabFolder, TestRunnerViewPart runner) {
		fRunnerViewPart= runner;
		
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

	void disposeIcons() {
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
	
	public String getTestName() {
		int index= fTable.getSelectionIndex();
		if (index == -1)
			return null;
		return fTable.getItem(index).getText();
	}
	
	private String getClassName() {
		String className= getSelectedText();
		className= className.substring(className.indexOf('(') + 1);
		return className.substring(0, className.indexOf(')'));
	}
	
	private String getMethodName() {
		String methodName= getSelectedText();
		return methodName.substring(0, methodName.indexOf('('));
	}

	public void menuAboutToShow(IMenuManager manager){
		if (fTable.getSelectionCount() > 0) {
			String className= getClassName();
			String methodName= getMethodName();
			if (className != null) {
				manager.add(new OpenTestAction(fRunnerViewPart, className, methodName));
				manager.add(new RerunAction(fRunnerViewPart, className, methodName));
			}
		}
	}		
	
	private String getSelectedText() {
		int index= fTable.getSelectionIndex();
		if (index == -1)
			return null;
		return fTable.getItem(index).getText();
	}
	
	public void setSelectedTest(String testName){
		TableItem[] items= fTable.getItems();
		for (int i= 0; i < items.length; i++) {
			TableItem tableItem= items[i]; 			
			if (tableItem.getText().equals(testName)){
				fTable.setSelection(new TableItem[] { tableItem });
				fTable.showItem(tableItem);
				return;
			}
		}
	}
	
	public void setFocus() {
		fTable.setFocus();
	}
	
	public void endTest(String testName){
		TestRunInfo testInfo= fRunnerViewPart.getTestInfo(testName);
		if(testInfo == null || testInfo.fStatus == ITestRunListener.STATUS_OK) 
			return;

		TableItem tableItem= new TableItem(fTable, SWT.NONE);
		updateTableItem(testInfo, tableItem);
		fTable.showItem(tableItem);
	}

	private void updateTableItem(TestRunInfo testInfo, TableItem tableItem) {
		tableItem.setText(testInfo.fTestName);
		if (testInfo.fStatus == ITestRunListener.STATUS_FAILURE)
			tableItem.setImage(fFailureIcon);
		else
			tableItem.setImage(fErrorIcon);
		tableItem.setData(testInfo);
	}

	private TableItem findItemByTest(String testName) {
		TableItem[] items= fTable.getItems();
		for (int i= 0; i < items.length; i++) {
			if (items[i].getText().equals(testName))
				return items[i];
		}
		return null;
	}

	public void activate() {
		testSelected();
	}

	public void aboutToStart() {
		fTable.removeAll();
	}

	protected void testSelected() {
		fRunnerViewPart.handleTestSelected(getTestName());
	}
	
	protected void addListeners() {
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

		fTable.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e){
				handleDoubleClick(e);
			}
			public void mouseDown(MouseEvent e) {
				fPressed= true;
				activate();
			}
			public void mouseUp(MouseEvent e) {
				fPressed= false;
				activate();
			}
		});
		
		fTable.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				TableItem tableItem= ((Table) e.getSource()).getItem(new Point(e.x, e.y));
				if (fPressed & (null != tableItem)) {
					fTable.setSelection(new TableItem[] { tableItem });
					activate();
				}
				// scrolling up and down
				if ((e.y + 1 > fTable.getBounds().height)
					& fPressed
					& (fTable.getSelectionIndex() != fTable.getItemCount() - 1)) {
					fTable.setTopIndex(fTable.getTopIndex() + 1);
					fTable.setSelection(fTable.getSelectionIndex() + 1);
					activate();
				}
				if ((e.y - 1 < 0) & fPressed & (fTable.getTopIndex() != 0)) {
					fTable.setTopIndex(fTable.getTopIndex() - 1);
					fTable.setSelection(fTable.getSelectionIndex() - 1);
					activate();
				}
			}
		});
	}
	
	public void handleDoubleClick(MouseEvent e) {
		if (fTable.getSelectionCount() > 0) 
			new OpenTestAction(fRunnerViewPart, getClassName(), getMethodName()).run();
	}
	
	public void newTreeEntry(String treeEntry) {
	}
	
	/*
	 * @see ITestRunView#testStatusChanged(TestRunInfo)
	 */
	public void testStatusChanged(TestRunInfo info) {
		TableItem item= findItemByTest(info.fTestName);
		if (item != null) {
			if (info.fStatus == ITestRunListener.STATUS_OK) {
				item.dispose();
				return;
			}
			updateTableItem(info, item);
		} 
		if (item == null && info.fStatus != ITestRunListener.STATUS_OK) {
			item= new TableItem(fTable, SWT.NONE);
			updateTableItem(info, item);
		}
		if (item != null)
			fTable.showItem(item);
	}
}