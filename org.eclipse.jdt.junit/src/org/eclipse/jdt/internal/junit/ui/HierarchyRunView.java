/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids - sdavids@gmx.de bug 26754 
*******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jdt.junit.ITestRunListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/*
 * A view that shows the contents of a test suite
 * as a tree.
 */
class HierarchyRunView implements ITestRunView, IMenuListener {
	
	/**
	 * The tree widget
	 */
	private Tree fTree;
	
	private TreeItem fCachedParent;
	private TreeItem[] fCachedItems;
	
	/**
	 * Helper used to resurrect test hierarchy
	 */
	private static class SuiteInfo {
		public int fTestCount;
		public TreeItem fTreeItem;
		
		public SuiteInfo(TreeItem treeItem, int testCount){
			fTreeItem= treeItem;
			fTestCount= testCount;
		}
	}
	/**
	 * Vector of SuiteInfo items
	 */
	private Vector fSuiteInfos= new Vector();
	/**
	 * Maps test Ids to TreeItems. 
	 */
	private Map fTreeItemMap= new HashMap();
	
	private TestRunnerViewPart fTestRunnerPart;
	
	private final Image fOkIcon= TestRunnerViewPart.createImage("obj16/testok.gif"); //$NON-NLS-1$
	private final Image fErrorIcon= TestRunnerViewPart.createImage("obj16/testerr.gif"); //$NON-NLS-1$
	private final Image fFailureIcon= TestRunnerViewPart.createImage("obj16/testfail.gif"); //$NON-NLS-1$
	private final Image fHierarchyIcon= TestRunnerViewPart.createImage("obj16/testhier.gif"); //$NON-NLS-1$
	private final Image fSuiteIcon= TestRunnerViewPart.createImage("obj16/tsuite.gif"); //$NON-NLS-1$
	private final Image fSuiteErrorIcon= TestRunnerViewPart.createImage("obj16/tsuiteerror.gif"); //$NON-NLS-1$
	private final Image fSuiteFailIcon= TestRunnerViewPart.createImage("obj16/tsuitefail.gif"); //$NON-NLS-1$
	private final Image fTestIcon= TestRunnerViewPart.createImage("obj16/test.gif"); //$NON-NLS-1$
	private final Image fTestRunningIcon= TestRunnerViewPart.createImage("obj16/testrun.gif"); //$NON-NLS-1$
		
	public HierarchyRunView(CTabFolder tabFolder, TestRunnerViewPart runner) {
		fTestRunnerPart= runner;
		
		CTabItem hierarchyTab= new CTabItem(tabFolder, SWT.NONE);
		hierarchyTab.setText(getName());
		hierarchyTab.setImage(fHierarchyIcon);
		
		Composite testTreePanel= new Composite(tabFolder, SWT.NONE);
		GridLayout gridLayout= new GridLayout();
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		testTreePanel.setLayout(gridLayout);
		
		GridData gridData= new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		testTreePanel.setLayoutData(gridData);
		
		hierarchyTab.setControl(testTreePanel);
		hierarchyTab.setToolTipText(JUnitMessages.getString("HierarchyRunView.tab.tooltip")); //$NON-NLS-1$
		
		fTree= new Tree(testTreePanel, SWT.V_SCROLL);
		gridData= new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		fTree.setLayoutData(gridData);
		
		initMenu();
		addListeners();
	}

	private void disposeIcons() {
		fErrorIcon.dispose();
		fFailureIcon.dispose();
		fOkIcon.dispose();
		fHierarchyIcon.dispose();
		fTestIcon.dispose();
		fTestRunningIcon.dispose();
		fSuiteIcon.dispose();
		fSuiteErrorIcon.dispose();
		fSuiteFailIcon.dispose(); 
	}
	
	private void initMenu() {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this);
		Menu menu= menuMgr.createContextMenu(fTree);
		fTree.setMenu(menu);	
	}

	private String getTestLabel() {
		TreeItem treeItem= fTree.getSelection()[0];
		if(treeItem == null) 
			return ""; //$NON-NLS-1$
		return treeItem.getText();
	}

	private TestRunInfo getTestInfo() {
		TreeItem[] treeItems= fTree.getSelection();
		if(treeItems.length == 0) 
			return null;
		return ((TestRunInfo)treeItems[0].getData());
	}	
	
	private boolean isSuiteSelected() {
		TreeItem[] treeItems= fTree.getSelection();
		if(treeItems.length != 1) 
			return false;
		return treeItems[0].getItemCount() > 0;
	}	
	
	private String getClassName() {
		TestRunInfo testInfo= getTestInfo();
		if (testInfo == null) 
			return null;
		return extractClassName(testInfo.getTestName());
	}
	
	public String getSelectedTestId() {
		TestRunInfo testInfo= getTestInfo();
		if (testInfo == null) 
			return null;
		return testInfo.getTestId();
	}
		
	private String extractClassName(String testNameString) {
		if (testNameString == null) 
			return null;
		int index= testNameString.indexOf('(');
		if (index < 0) 
			return testNameString;
		testNameString= testNameString.substring(index + 1);
		return testNameString.substring(0, testNameString.indexOf(')'));
	}		

	public String getName() {
		return JUnitMessages.getString("HierarchyRunView.tab.title"); //$NON-NLS-1$
	}
	
	public void setSelectedTest(String testId) {
		TreeItem treeItem= findTreeItem(testId);
		if (treeItem != null)
			fTree.setSelection(new TreeItem[]{treeItem});
	}
	
	public void startTest(String testId) {	
		TreeItem treeItem= findTreeItem(testId);
		if (treeItem == null)  
			return;
		setCurrentItem(treeItem);		
	}

	private void setCurrentItem(TreeItem treeItem) {
		treeItem.setImage(fTestRunningIcon);
	}

	public void endTest(String testId) {	
		TreeItem treeItem= findTreeItem(testId);
		if (treeItem == null)  
			return;
			
		TestRunInfo testInfo= fTestRunnerPart.getTestInfo(testId);
			
		updateItem(treeItem, testInfo);
		
		if (fTestRunnerPart.isAutoScroll()) {
			fTree.showItem(treeItem);
			cacheItems(treeItem);
			collapseIfOK(treeItem);
		} 
	}

	private void cacheItems(TreeItem treeItem) {
		TreeItem parent= treeItem.getParentItem();
		if (parent == fCachedParent)
			return;
		fCachedItems= parent.getItems();
		fCachedParent= parent;	
	}

	private void collapseIfOK(TreeItem treeItem) {
		TreeItem parent= treeItem.getParentItem();
		if (parent != null) {
			TreeItem[] items= null;
			if (parent == fCachedParent)
				items= fCachedItems;
			else 
				items= parent.getItems();
				
			if (isLast(treeItem, items)) {
				boolean ok= true;
				for (int i= 0; i < items.length; i++) {
					if (!(getTestRunInfo(items[i]).getStatus() == ITestRunListener.STATUS_OK)) {
						ok= false;
						break;
					}
				}
				if (ok) {
					parent.setExpanded(false);
					collapseIfOK(parent);
				}
			}
		}
	}

	private boolean isLast(TreeItem treeItem, TreeItem[] items) {
		return items[items.length-1] == treeItem;
	}

	private void updateItem(TreeItem treeItem, TestRunInfo testInfo) {
		treeItem.setData(testInfo);
		if(testInfo.getStatus() == ITestRunListener.STATUS_OK) {
			treeItem.setImage(fOkIcon);	
			return;
		}
		
		if (testInfo.getStatus() == ITestRunListener.STATUS_FAILURE) 
			treeItem.setImage(fFailureIcon);
		else if (testInfo.getStatus() == ITestRunListener.STATUS_ERROR)
			treeItem.setImage(fErrorIcon);
		propagateStatus(treeItem, testInfo.getStatus());	
	}

	private void propagateStatus(TreeItem item, int status) {
		TreeItem parent= item.getParentItem();
		TestRunInfo testRunInfo= getTestRunInfo(item);
		
		if (parent == null)
			return;
		Image parentImage= parent.getImage();
		
		if (status == ITestRunListener.STATUS_FAILURE) {
			if (parentImage == fSuiteErrorIcon || parentImage == fSuiteFailIcon) 
				return;
			parent.setImage(fSuiteFailIcon);
			testRunInfo.setStatus(ITestRunListener.STATUS_FAILURE);
		} else {
			if (parentImage == fSuiteErrorIcon) 
				return;
			parent.setImage(fSuiteErrorIcon);
			testRunInfo.setStatus(ITestRunListener.STATUS_ERROR);
		}
		propagateStatus(parent, status);
	}
	
	private TestRunInfo getTestRunInfo(TreeItem item) {
		return (TestRunInfo)item.getData();
	}

	public void activate() {
		testSelected();
	}
	
	public void setFocus() {
		fTree.setFocus();
	}

	public void aboutToStart() {
		fTree.removeAll();
		fSuiteInfos.removeAllElements();
		fTreeItemMap= new HashMap();
		fCachedParent= null;
		fCachedItems= null;
	}
	
	private void testSelected() {
		fTestRunnerPart.handleTestSelected(getSelectedTestId());
	}
	
	private void addListeners() {
		fTree.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				activate();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				activate();
			}
		});
		
		fTree.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});

		fTree.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				handleDoubleClick(e);
			}
		});
	}
	
	void handleDoubleClick(MouseEvent e) {
		TestRunInfo testInfo= getTestInfo();
		String testLabel= testInfo.getTestName();
		OpenTestAction action= null;
		
		if (isSuiteSelected()) 	
			action= new OpenTestAction(fTestRunnerPart, testLabel);
		else 
			action= new OpenTestAction(fTestRunnerPart, getClassName(), getTestLabel());

		if (action != null && action.isEnabled())
			action.run();													
	}

	public void menuAboutToShow(IMenuManager manager) {
		if (fTree.getSelectionCount() > 0) {
			TreeItem treeItem= fTree.getSelection()[0];
			TestRunInfo testInfo= (TestRunInfo) treeItem.getData();
			String testLabel= testInfo.getTestName();
			if (isSuiteSelected()) {	
				manager.add(new OpenTestAction(fTestRunnerPart, testLabel));
			} else {
				manager.add(new OpenTestAction(fTestRunnerPart, getClassName(), getTestLabel()));
				manager.add(new RerunAction(fTestRunnerPart, getSelectedTestId(), getClassName(), getTestLabel()));
			}
		}
	}	
	
	public void newTreeEntry(String treeEntry) {
		// format: testId","testName","isSuite","testcount
		int index0= treeEntry.indexOf(',');
		StringBuffer testName= new StringBuffer(100);
		int index1= scanTestName(treeEntry, index0+1, testName);
		int index2= treeEntry.indexOf(',', index1+1);
		String label= testName.toString().trim();
		String id= treeEntry.substring(0, index0);
		TestRunInfo testInfo= new TestRunInfo(id, label);
		int index3;
		if((index3= label.indexOf('(')) > 0)
			label= label.substring(0, index3);
		if((index3= label.indexOf('@')) > 0)
			label= label.substring(0, index3);
		
		String isSuite= treeEntry.substring(index1+1, index2);
		int testCount= Integer.parseInt(treeEntry.substring(index2+1));
		TreeItem treeItem;
	
		while((fSuiteInfos.size() > 0) && (((SuiteInfo) fSuiteInfos.lastElement()).fTestCount == 0))	{
			fSuiteInfos.removeElementAt(fSuiteInfos.size()-1);
		}
	
		if(fSuiteInfos.size() == 0){
			treeItem= new TreeItem(fTree, SWT.NONE);
			treeItem.setImage(fSuiteIcon);
			fSuiteInfos.addElement(new SuiteInfo(treeItem, testCount));
		} else if(isSuite.equals("true")) { //$NON-NLS-1$
			treeItem= new TreeItem(((SuiteInfo) fSuiteInfos.lastElement()).fTreeItem, SWT.NONE);
			treeItem.setImage(fHierarchyIcon);
			((SuiteInfo)fSuiteInfos.lastElement()).fTestCount -= 1;
			fSuiteInfos.addElement(new SuiteInfo(treeItem, testCount));
		} else {
			treeItem= new TreeItem(((SuiteInfo) fSuiteInfos.lastElement()).fTreeItem, SWT.NONE);
			treeItem.setImage(fTestIcon);
			((SuiteInfo)fSuiteInfos.lastElement()).fTestCount -= 1;
			mapTest(testInfo, treeItem);
		}
		treeItem.setText(label);
		treeItem.setData(testInfo);
	}
	
	private int scanTestName(String s, int start, StringBuffer testName) {
		boolean inQuote= false;
		int i= start;
		for (; i < s.length(); i++) {
			char c= s.charAt(i);
			if (c == '\\' && !inQuote) {
				inQuote= true;
				continue;
			} else if (inQuote) {
				inQuote= false;
				testName.append(c);
			} else if (c == ',')
				break;
			else
				testName.append(c);
		}
		return i;
	}


	private void mapTest(TestRunInfo info, TreeItem item) {
		fTreeItemMap.put(info.getTestId(), item);
	}
	
	private TreeItem findTreeItem(String testId) {
		Object o= fTreeItemMap.get(testId);
		if (o instanceof TreeItem) 
			return (TreeItem)o;
		return null;
	}
	
	/*
	 * @see ITestRunView#testStatusChanged(TestRunInfo, int)
	 */
	public void testStatusChanged(TestRunInfo newInfo) {
		Object o= fTreeItemMap.get(newInfo.getTestId());
		if (o instanceof TreeItem) {
			updateItem((TreeItem)o, newInfo);
			return;
		}
	}
}
