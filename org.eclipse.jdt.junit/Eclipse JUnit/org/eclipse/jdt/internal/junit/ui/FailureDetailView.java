/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * A view that shows a stack trace of a failed test.
 */
class FailureDetailView {
	private Table fDetailView;
	private TestRunnerViewPart fTestRunner;
	private String fTrace;
	
	private final Image fStackIcon= TestRunnerViewPart.createImage("icons/stckframe_obj.gif", getClass());
	private final Image fExceptionIcon= TestRunnerViewPart.createImage("icons/exc_catch.gif", getClass());
	private final Image fInfoIcon= TestRunnerViewPart.createImage("icons/info_obj.gif", getClass());

	public FailureDetailView(Composite parent, TestRunnerViewPart testRunner) {
		fDetailView= new Table(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		fTestRunner= testRunner;
		
		fDetailView.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e){
				if(fDetailView.getSelection().length != 0)
					fTestRunner.goToFile(fDetailView.getSelection()[0].getText());
			}
		});
		setMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager){
				if (fDetailView.getSelectionCount() > 0)
					manager.add(new Action("&Goto File"){ public void run(){ fTestRunner.goToFile(fDetailView.getSelection()[0].getText());} });
			}				
		});
		fStackIcon.setBackground(fDetailView.getBackground());
		fExceptionIcon.setBackground(fDetailView.getBackground());
		fInfoIcon.setBackground(fDetailView.getBackground());
	}
	
	private void setMenuListener(IMenuListener menuListener) {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(menuListener);
		Menu menu= menuMgr.createContextMenu(fDetailView);
		fDetailView.setMenu(menu);		
	}
	
	public void dispose(){
		if (fExceptionIcon != null && !fExceptionIcon.isDisposed()) {
			fExceptionIcon.dispose();
		}
		if (fStackIcon != null && !fStackIcon.isDisposed()) {
			fStackIcon.dispose();
		}
		if (fInfoIcon != null && !fInfoIcon.isDisposed()) {
			fInfoIcon.dispose();
		}
	}
	
	/**
	 * Returns the composite used to present the trace
	 */
	protected Composite getComposite(){
		return fDetailView;
	}
	
	/**
	 * Shows a TestFailure
	 */
	protected void showFailure(String trace) {
		if(trace == null || trace.trim().equals("")) {
			clear();
			return;
		}
	
		if(trace.trim().equals(fTrace)) return;
		fTrace= trace.trim();
		fDetailView.removeAll();
		
		int start= 0;
		int end= trace.indexOf('\n', start);
		
		TableItem tableItem= new TableItem(fDetailView, SWT.NONE);
		String itemLabel= trace.substring(start,end).replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
		tableItem.setText(itemLabel);
		tableItem.setImage(fExceptionIcon);
		start= end + 1;
		end= trace.indexOf('\n', start);
		
		while(end != -1){
			tableItem= new TableItem(fDetailView, SWT.NONE);
			itemLabel= trace.substring(start,end).replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
			tableItem.setText(itemLabel);
			if(!itemLabel.trim().equals("")) {
				tableItem.setImage(fStackIcon);
			}
			start= end + 1;
			end= trace.indexOf('\n', start);
		}
	}
	
	protected void setInformation(String text){
		clear();
		TableItem tableItem= new TableItem(fDetailView, SWT.NONE);
		tableItem.setImage(fInfoIcon);
		tableItem.setText(text);
	}
	
	protected void clear() {
		fDetailView.removeAll();
		fTrace= null;
	}
}