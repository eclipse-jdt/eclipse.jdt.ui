/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;


import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuListener;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

/*
 * A hierarchical view of a test run.
 * The contents of a test suite is shown
 * as a tree.
 */
class HierarchyRunView implements ITestRunView {
	
	class MenuListener implements IMenuListener {
		public void menuAboutToShow(IMenuManager manager){
			if (fTree.getSelectionCount() > 0) {
				final TreeItem treeItem= fTree.getSelection()[0];
				TestInfo testInfo= (TestInfo) treeItem.getData();
				if (testInfo.fStatus == TestRunnerViewPart.IS_SUITE) {
					manager.add(new Action("&Rerun Suite"){ 
						public void run(){
							Vector vector= new Vector();
							collectTestClasses(treeItem, vector);
							fRunViewContext.reRunTest((String[]) vector.toArray(new String[vector.size()]));
						} 
					});
				}
				if (testInfo.fStatus == TestRunnerViewPart.IS_SUITE) {			
					manager.add(new Action("&Goto File"){ 
						public void run(){
							String className= getTestLabel();
							int index= className.length();
							if ((index= className.indexOf('@')) > 0)
								className= className.substring(0, index);
							fRunViewContext.goToTest(className, 0);
						} 
					});
				} else {
					manager.add(new Action("&Goto File"){ 
						public void run(){ 
							fRunViewContext.goToTestMethod(getClassName(), getTestLabel());
						} 
					});	
				}
			}
		}		
	}	
	
	private Composite fTestTreePanel;
	private TestRunnerViewPart fRunViewContext;
	private String fProjectName;
	private String fTestName;
	private static final String fgName= "Hierarchy";
	private boolean fPressed= false;
	private Tree fTree;
	
	private final Image fOkIcon= TestRunnerViewPart.createImage("icons/ok.gif", getClass());
	private final Image fErrorIcon= TestRunnerViewPart.createImage("icons/error.gif", getClass());
	private final Image fFailureIcon= TestRunnerViewPart.createImage("icons/failure.gif", getClass());
	private final Image fHierarchyIcon= TestRunnerViewPart.createImage("icons/hierarchy.gif", getClass());
		
	public HierarchyRunView(CTabFolder tabFolder, TestRunnerViewPart context) {
		fRunViewContext= context;
		
		CTabItem hierarchyTab= new CTabItem(tabFolder, SWT.NONE);
		hierarchyTab.setText(fgName);
		fHierarchyIcon.setBackground(tabFolder.getBackground());
		hierarchyTab.setImage(fHierarchyIcon);
		
		fTestTreePanel= new Composite(tabFolder, SWT.NONE);
		GridLayout gridLayout= new GridLayout();
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		fTestTreePanel.setLayout(gridLayout);
		
		GridData gridData= new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		fTestTreePanel.setLayoutData(gridData);
		
		hierarchyTab.setControl(fTestTreePanel);
		hierarchyTab.setToolTipText("Test Hierarchy");
		
		fTree= new Tree(fTestTreePanel, SWT.V_SCROLL);
		
		fOkIcon.setBackground(fTestTreePanel.getBackground());
		fErrorIcon.setBackground(fTestTreePanel.getBackground());
		fFailureIcon.setBackground(fTestTreePanel.getBackground());
		
		setMenuListener();
		addListeners();
	}


	void disposeIcons() {
		if (fErrorIcon != null && !fErrorIcon.isDisposed()) {
			fErrorIcon.dispose();
		}
		if (fFailureIcon != null && !fFailureIcon.isDisposed()) {
			fFailureIcon.dispose();
		}
		if (fOkIcon != null && !fOkIcon.isDisposed()) {
			fOkIcon.dispose();
		}
		if (fHierarchyIcon != null && !fHierarchyIcon.isDisposed()) {
			fHierarchyIcon.dispose();
		}
	}
	
	private void setMenuListener() {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new MenuListener());
		Menu menu= menuMgr.createContextMenu(fTree);
		fTree.setMenu(menu);	
	}

	protected Tree getTree() {
		return fTree;
	}
	
	private String getTestLabel() {
		TreeItem treeItem= fTree.getSelection()[0];
		if(treeItem == null) 
			return "";
		return treeItem.getText();
	}

	private TestInfo getTestInfo() {
		TreeItem[] treeItems;
		if((treeItems= fTree.getSelection()).length == 0) 
			return null;
		TreeItem treeItem= treeItems[0];
		if(treeItem == null) 
			return null;
		return ((TestInfo) treeItem.getData());
	}	
	
	public String getClassName() {
		TestInfo testInfo= getTestInfo();
		if (testInfo == null) return null;
		return extractClassName(testInfo.fTestName);
	}
	
	public String getTestName() {
		TestInfo testInfo= getTestInfo();
		if (testInfo == null) 
			return null;
		return testInfo.fTestName;
	}
	
	private String extractClassName(String testNameString) {
		if (testNameString == null) 
			return null;
		int index= testNameString.indexOf('(');
		if (index < 0) 
			return testNameString;
		testNameString= testNameString.substring(index + 1);
		testNameString= testNameString.substring(0, testNameString.indexOf(')'));
		return testNameString;
	}		

	public String getName() {
		return fgName;
	}
	
	public void setSelectedTest(String testName) {
		if (testName == null) 
			return;
		TreeItem treeItem= findItemByTest(testName, fTree.getItems());
		if(treeItem == null) 
			return;
		fTree.setSelection(new TreeItem[]{treeItem});
		activate();
	}
	
	public void updateTest(String testName) {
		if (testName == null) 
			return;
		TreeItem treeItem= findItemByTest(testName, fTree.getItems());
		if(treeItem == null) 
			return;

		TestInfo testInfo= fRunViewContext.getTestInfo(testName);
		if(testInfo == null)
			return;
			
		treeItem.setData(testInfo);
	
		if(testInfo.fStatus != TestRunnerViewPart.IS_SUITE)
			treeItem.setImage(fOkIcon);	

		if (testInfo.fStatus == ITestRunListener.STATUS_FAILURE)
			treeItem.setImage(fFailureIcon);
		else if (testInfo.fStatus == ITestRunListener.STATUS_ERROR)
			treeItem.setImage(fErrorIcon);
	}
	
	public void activate() {
		try {
			testSelected();
		} catch (Exception e) {
		}
	}
	
	public void aboutToStart(){
		fTree.removeAll();
	}
	
	private TreeItem findItemByTest(String testName, TreeItem[] treeItem){
		if (testName == null) 
			return null;
		if(treeItem.length == 0) 
			return null;
		for(int index= 0; index < treeItem.length; index++) {
			TestInfo testInfo= (TestInfo) treeItem[index].getData();
			if(testInfo.fTestName.equals(testName)){
				return ((TreeItem)treeItem[index]);
			}
			else if(treeItem[index].getItemCount() > 0)
				if(null != (TreeItem)(findItemByTest(testName, treeItem[index].getItems())))
					return findItemByTest(testName, treeItem[index].getItems());
		}
		return null;
	}

	protected void testSelected() {
		fRunViewContext.handleTestSelected(getTestName());
	}

	private void collectTestClasses(TreeItem treeItem, Vector vector) {
		TreeItem[] items= treeItem.getItems();
		
		for (int i= 0; i < items.length; i++) {
			TestInfo testInfo= (TestInfo) items[i].getData();
			if (testInfo.fStatus == TestRunnerViewPart.IS_SUITE) 
				collectTestClasses(items[i], vector);
			else {
				String className= extractClassName(testInfo.fTestName);
				if (!vector.contains(className))
					vector.addElement(className);
			}
		}
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

		fTree.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				TestInfo testInfo= getTestInfo();
				if(testInfo == null) return;
				
				if (testInfo.fStatus == TestRunnerViewPart.IS_SUITE){
					String className= getTestLabel();
					int index= className.length();
					if ((index= className.indexOf('@')) > 0)
						className= className.substring(0, index);							
					fRunViewContext.goToTest(className, 0);
				}
				else
					fRunViewContext.goToTestMethod(getClassName(), getTestLabel());
			}
			public void mouseDown(MouseEvent e) {
				fPressed= true;
			}
			public void mouseUp(MouseEvent e) {
				fPressed= false;
			}
		});
		fTree.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				if (!(e.getSource() instanceof Tree)) 
					return;
				
				TreeItem[] treeItem= {((Tree) e.getSource()).getItem(new Point(e.x, e.y))};
				if (fPressed & (null != treeItem[0])) {
					fTree.setSelection(treeItem);
					activate();
				}
				// scroll
				if ((e.y < 1) & fPressed) {
					try {
						TreeItem tItem= treeItem[0].getParentItem();
						fTree.setSelection(new TreeItem[] { tItem });
						activate();
					} catch (Exception ex) {
					}
				}
			}
		});
	}
}