/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;


/**
 * A view presenting the test failures as a list.
 */
class FailureRunView implements ITestRunView {
	private Table fTable;
	private TestRunnerViewPart fRunViewContext;
	private boolean fPressed= false;
	private static final String fgName= "Failures";
	
	private final Image fErrorIcon= TestRunnerViewPart.createImage("icons/error.gif", getClass());
	private final Image fFailureIcon= TestRunnerViewPart.createImage("icons/failure.gif", getClass());
	
	public FailureRunView(CTabFolder tabFolder, TestRunnerViewPart context) {
		fRunViewContext= context;
		
		CTabItem failureTab= new CTabItem(tabFolder, SWT.NONE);
		failureTab.setText(fgName);
		fFailureIcon.setBackground(tabFolder.getBackground());
		failureTab.setImage(fFailureIcon);

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
		failureTab.setToolTipText("Failures and Errors");
		
		fTable.setToolTipText("Failure - grey X; Error - red X");
		fErrorIcon.setBackground(fTable.getBackground());
		fFailureIcon.setBackground(fTable.getBackground());
		
		setMenuListener();
		addListeners();	
	}


	public void dispose() {
		if (fErrorIcon != null && !fErrorIcon.isDisposed()) {
			fErrorIcon.dispose();
		}
		if (fFailureIcon != null && !fFailureIcon.isDisposed()) {
			fFailureIcon.dispose();
		}
	}

	private void setMenuListener() {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager){
				if (fTable.getSelectionCount() > 0) {
					manager.add(new Action("&Rerun Suite"){ 
						public void run(){ 
							fRunViewContext.reRunTest(new String[] {getClassName()});
						} 
					});
					manager.add(new Action("&Goto File"){ public void run(){ fRunViewContext.goToTestMethod(getClassName(), getMethodName());} });
				}
			}		
		});
		Menu menu= menuMgr.createContextMenu(fTable);
		fTable.setMenu(menu);
	}
	
	public String getName() {
		return fgName;
	}
	
	public String getTestName() {
		int index= fTable.getSelectionIndex();
		if (index == -1)
			return null;
		return fTable.getItem(index).getText();
	}
	
	public String getClassName() {
		int index= fTable.getSelectionIndex();
		if (index == -1)
			return null;
		String className= fTable.getItem(index).getText();
		className= className.substring(className.indexOf('(') + 1);
		className= className.substring(0, className.indexOf(')'));
		return className;
	}
	
	public String getMethodName() {
		int index= fTable.getSelectionIndex();
		if (index == -1)
			return null;
		String methodName= fTable.getItem(index).getText();
		methodName= methodName.substring(0, methodName.indexOf('('));
		return methodName;		
	}
	
	public void setSelectedTest(String testName){
		Iterator iter= Arrays.asList(fTable.getItems()).iterator();
		TableItem tableItem;
		TestInfo testInfo= fRunViewContext.getTestInfo(testName);
		while (iter.hasNext()) {
			tableItem= (TableItem)iter.next();

			if (tableItem.getText().equals(testName)){
				fTable.setSelection(new TableItem[] { tableItem });
				fTable.showItem(tableItem);
			}
		}
	}
	
	public void updateTest(String testName){
		if (testName == null) return;
		TestInfo testInfo= fRunViewContext.getTestInfo(testName);
		TableItem tableItem= findItemByTest(testName);
		
		if(testInfo == null || testInfo.fTrace == null) { 
			if(tableItem != null)
				fTable.remove(fTable.indexOf(tableItem));
			return;
		}
		if (tableItem == null)
			tableItem= new TableItem(fTable, SWT.NONE);

		tableItem.setText(testName);
		if (testInfo.fStatus == ITestRunListener.STATUS_FAILURE)
			tableItem.setImage(fFailureIcon);
		else
			tableItem.setImage(fErrorIcon);
			
		tableItem.setData(testInfo);
	}

	private TableItem findItemByTest(String testName) {
		TableItem[] items= fTable.getItems();
		for (int i=0; i < items.length; i++) {
			if (items[i].getText().equals(testName))
				return items[i];
		}
		return null;
	}


	public void activate() {
		try {
			testSelected();
		} catch (Exception e) {
		}
	}

	public void aboutToStart() {
		fTable.removeAll();
	}

	protected void testSelected() {
		fRunViewContext.handleTestSelected(getTestName());
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
		fTable.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e){
				fRunViewContext.goToTestMethod(getClassName(), getMethodName());
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
}