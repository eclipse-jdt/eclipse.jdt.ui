/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.jdt.internal.junit.runner.ITestRunListener;

/*
 * A view that shows the contents of a test suite
 * as a tree.
 */
class HierarchyRunView implements ITestRunView, IMenuListener {
	/**
	 * The tree widget
	 */
	private Tree fTree;
	
	public static final int IS_SUITE= -1;	
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
	 * Maps test names to TreeItems. 
	 * If there is one treeItem for a test then the
	 * value of the map corresponds to the item, otherwise
	 * there is a list of tree items.
	 */
	private Map fTreeItemMap= new HashMap();
	
	private TestRunnerViewPart fTestRunnerPart;
	
	private boolean fPressed= false;
	
	private final Image fOkIcon= TestRunnerViewPart.createImage("icons/ok.gif", getClass());
	private final Image fErrorIcon= TestRunnerViewPart.createImage("icons/error.gif", getClass());
	private final Image fFailureIcon= TestRunnerViewPart.createImage("icons/failure.gif", getClass());
	private final Image fHierarchyIcon= TestRunnerViewPart.createImage("icons/hierarchy.gif", getClass());
	private final Image fTestIcon= TestRunnerViewPart.createImage("icons/testIcon.gif", getClass());
		
	public HierarchyRunView(CTabFolder tabFolder, TestRunnerViewPart runner) {
		fTestRunnerPart= runner;
		
		CTabItem hierarchyTab= new CTabItem(tabFolder, SWT.NONE);
		hierarchyTab.setText(getName());
		fHierarchyIcon.setBackground(tabFolder.getBackground());
		hierarchyTab.setImage(fHierarchyIcon);
		
		Composite testTreePanel= new Composite(tabFolder, SWT.NONE);
		GridLayout gridLayout= new GridLayout();
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		testTreePanel.setLayout(gridLayout);
		
		GridData gridData= new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		testTreePanel.setLayoutData(gridData);
		
		hierarchyTab.setControl(testTreePanel);
		hierarchyTab.setToolTipText("Test Hierarchy");
		
		fTree= new Tree(testTreePanel, SWT.V_SCROLL);
		gridData= new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		fTree.setLayoutData(gridData);

		fOkIcon.setBackground(testTreePanel.getBackground());
		fErrorIcon.setBackground(testTreePanel.getBackground());
		fFailureIcon.setBackground(testTreePanel.getBackground());
		
		initMenu();
		addListeners();
	}


	void disposeIcons() {
		if (fErrorIcon != null && !fErrorIcon.isDisposed()) 
			fErrorIcon.dispose();
		if (fFailureIcon != null && !fFailureIcon.isDisposed()) 
			fFailureIcon.dispose();
		if (fOkIcon != null && !fOkIcon.isDisposed()) 
			fOkIcon.dispose();
		if (fHierarchyIcon != null && !fHierarchyIcon.isDisposed()) 
			fHierarchyIcon.dispose();
		if (fTestIcon != null && !fTestIcon.isDisposed()) 
			fTestIcon.dispose();
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
			return "";
		return treeItem.getText();
	}

	private TestRunInfo getTestInfo() {
		TreeItem[] treeItems= fTree.getSelection();
		if(treeItems.length == 0) 
			return null;
		return ((TestRunInfo)treeItems[0].getData());
	}	
	
	public String getClassName() {
		TestRunInfo testInfo= getTestInfo();
		if (testInfo == null) 
			return null;
		return extractClassName(testInfo.fTestName);
	}
	
	public String getTestName() {
		TestRunInfo testInfo= getTestInfo();
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
		return testNameString.substring(0, testNameString.indexOf(')'));
	}		

	public String getName() {
		return "Hierarchy";
	}
	
	public void setSelectedTest(String testName) {
		TreeItem treeItem= findFirstItem(testName);
		fTree.setSelection(new TreeItem[]{treeItem});
	}
	
	public void endTest(String testName) {	
		TreeItem treeItem= findFirstNotRunItem(testName);
		// workaround for bug 8657
		if (treeItem == null)  
			return;
			
		TestRunInfo testInfo= fTestRunnerPart.getTestInfo(testName);
			
		updateItem(treeItem, testInfo);
			
		if (testInfo.fTrace != null)
			fTree.showItem(treeItem);
	}

	private void updateItem(TreeItem treeItem, TestRunInfo testInfo) {
		treeItem.setData(testInfo);
		if(testInfo.fStatus == ITestRunListener.STATUS_OK)
			treeItem.setImage(fOkIcon);	
		else if (testInfo.fStatus == ITestRunListener.STATUS_FAILURE)
			treeItem.setImage(fFailureIcon);
		else if (testInfo.fStatus == ITestRunListener.STATUS_ERROR)
			treeItem.setImage(fErrorIcon);
	}

	public void activate() {
		testSelected();
	}
	
	public void aboutToStart() {
		fTree.removeAll();
		fSuiteInfos.removeAllElements();
		fTreeItemMap= new HashMap();
	}
	
	protected void testSelected() {
		fTestRunnerPart.handleTestSelected(getTestName());
	}
	
	public void menuAboutToShow(IMenuManager manager) {
		if (fTree.getSelectionCount() > 0) {
			final TreeItem treeItem= fTree.getSelection()[0];
			TestRunInfo testInfo= (TestRunInfo) treeItem.getData();
			if (testInfo.fStatus == IS_SUITE) {	
				String className= getTestLabel();
				int index= className.length();
				if ((index= className.indexOf('@')) > 0)
					className= className.substring(0, index);
				manager.add(new OpenTestAction(fTestRunnerPart, className));
			} else {
				manager.add(new OpenTestAction(fTestRunnerPart, getClassName(), getTestLabel()));
				manager.add(new RerunAction(fTestRunnerPart, getClassName(), getTestLabel()));
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
				handleDoubleClick(e);
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
	
	void handleDoubleClick(MouseEvent e) {
		TestRunInfo testInfo= getTestInfo();
		if(testInfo == null) 
			return;
		
		if ((testInfo.fStatus == IS_SUITE)) {
			String className= getTestLabel();
			int index= className.length();
			if ((index= className.indexOf('@')) > 0)
				className= className.substring(0, index);
			(new OpenTestAction(fTestRunnerPart, className)).run();							
		}
		else {
			(new OpenTestAction(fTestRunnerPart, getClassName(), getTestLabel())).run();
		}							
	}
	
	public void newTreeEntry(String treeEntry) {
		int index0= treeEntry.indexOf(',');
		int index1= treeEntry.lastIndexOf(',');
		String label= treeEntry.substring(0, index0).trim();
		TestRunInfo testInfo= new TestRunInfo(label);
		//fTestInfo.addElement(testInfo);
		int index2;
		if((index2= label.indexOf('(')) > 0)
			label= label.substring(0, index2);
		if((index2= label.indexOf('@')) > 0)
			label= label.substring(0, index2);
		
		String isSuite= treeEntry.substring(index0+1, index1);
		int testCount= Integer.parseInt(treeEntry.substring(index1+1));
		TreeItem treeItem;
	
		while((fSuiteInfos.size() > 0) && (((SuiteInfo) fSuiteInfos.lastElement()).fTestCount == 0))	{
			fSuiteInfos.removeElementAt(fSuiteInfos.size()-1);
		}

		if(fSuiteInfos.size() == 0){
			testInfo.fStatus= IS_SUITE;
			treeItem= new TreeItem(fTree, SWT.NONE);
			treeItem.setImage(fHierarchyIcon);
			fSuiteInfos.addElement(new SuiteInfo(treeItem, testCount));
		} else if(isSuite.equals("true")) {
			testInfo.fStatus= IS_SUITE;
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
	
	private void mapTest(TestRunInfo info, TreeItem item) {
		String test= info.fTestName;
		Object o= fTreeItemMap.get(test);
		if (o == null) {
			fTreeItemMap.put(test, item);
			return;
		}
		if (o instanceof TreeItem) {
			List list= new ArrayList();
			list.add(o);
			list.add(item);
			fTreeItemMap.put(test, list);
			return;
		}
		if (o instanceof List) {
			((List)o).add(item);
		}
	}
	
	private TreeItem findFirstNotRunItem(String testName) {
		Object o= fTreeItemMap.get(testName);
		if (o instanceof TreeItem) 
			return (TreeItem)o;
		if (o instanceof List) {
			List l= (List)o;
			for (int i= 0; i < l.size(); i++) {
				TreeItem item= (TreeItem)l.get(i);
				if (item.getImage() == fTestIcon)
					return item;
			}
			return null;
		}
		return null;
	}
	
	private TreeItem findFirstItem(String testName) {
		Object o= fTreeItemMap.get(testName);
		if (o instanceof TreeItem) 
			return (TreeItem)o;
		if (o instanceof List) {
			return (TreeItem)((List)o).get(0);
		}
		return null;
	}
	/*
	 * @see ITestRunView#testStatusChanged(TestRunInfo, int)
	 */
	public void testStatusChanged(TestRunInfo newInfo) {
		Object o= fTreeItemMap.get(newInfo.fTestName);
		if (o instanceof TreeItem) {
			updateItem((TreeItem)o, newInfo);
			return;
		}
		if (o instanceof List) {
			List l= (List)o;
			for (int i= 0; i < l.size(); i++) 
				updateItem((TreeItem)l.get(i), newInfo);
		}		
	}
}