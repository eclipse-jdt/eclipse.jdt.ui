/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

/**
 * A view that shows a stack trace of a failed test.
 */
class FailureTraceView implements IMenuListener {
	private static final String FRAME_PREFIX= "at ";
	private Table fTable;
	private TestRunnerViewPart fTestRunner;
	private String fInputTrace;
	
	private final Image fStackIcon= TestRunnerViewPart.createImage("obj16/stkfrm_obj.gif"); //$NON-NLS-1$
	private final Image fExceptionIcon= TestRunnerViewPart.createImage("obj16/exc_catch.gif"); //$NON-NLS-1$

	public FailureTraceView(Composite parent, TestRunnerViewPart testRunner) {
		fTable= new Table(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		fTestRunner= testRunner;
		
		fTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e){
				handleDoubleClick(e);
			}
		});
		
		initMenu();
		
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});
	}
	
	void handleDoubleClick(MouseEvent e) {
		if(fTable.getSelection().length != 0) {
			Action a= createOpenEditorAction(getSelectedText());
			if (a != null)
				a.run();
		}
	}
	
	private void initMenu() {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this);
		Menu menu= menuMgr.createContextMenu(fTable);
		fTable.setMenu(menu);		
	}
	
	public void menuAboutToShow(IMenuManager manager) {
		if (fTable.getSelectionCount() > 0) {
			Action a= createOpenEditorAction(getSelectedText());
			if (a != null)
				manager.add(a);
		}
		manager.add(new CopyTraceAction(FailureTraceView.this));
	}

	public String getTrace() {
		return fInputTrace;
	}
	
	private String getSelectedText() {
		return fTable.getSelection()[0].getText();
	}				

	private Action createOpenEditorAction(String traceLine) {
		try { 
			//TODO: works for JDK stack trace only
			String testName= traceLine;
			testName= testName.substring(testName.indexOf(FRAME_PREFIX)); //$NON-NLS-1$
			testName= testName.substring(FRAME_PREFIX.length(), testName.indexOf('(')).trim();
			testName= testName.substring(0, testName.lastIndexOf('.'));
			
			String lineNumber= traceLine;
			lineNumber= lineNumber.substring(lineNumber.indexOf(':') + 1, lineNumber.indexOf(")")); //$NON-NLS-1$
			int line= Integer.valueOf(lineNumber).intValue();
			return new OpenEditorAtLineAction(fTestRunner, testName, line);
		} catch(NumberFormatException e) {
		}
		catch(IndexOutOfBoundsException e) {	
		}	
		return null;
	}
	
	void disposeIcons(){
		if (fExceptionIcon != null && !fExceptionIcon.isDisposed()) 
			fExceptionIcon.dispose();
		if (fStackIcon != null && !fStackIcon.isDisposed()) 
			fStackIcon.dispose();
	}
	
	/**
	 * Returns the composite used to present the trace
	 */
	public Composite getComposite(){
		return fTable;
	}
	
	/**
	 * Refresh the table from the the trace.
	 */
	public void refresh() {
		updateTable(fInputTrace);
	}
	
	/**
	 * Shows a TestFailure
	 */
	public void showFailure(String trace) {	
		if (fInputTrace == trace)
			return;
		fInputTrace= trace;
		updateTable(trace);
	}

	private void updateTable(String trace) {
		if(trace == null || trace.trim().equals("")) { //$NON-NLS-1$
			clear();
			return;
		}
		trace= trace.trim();
		fTable.setRedraw(false);
		fTable.removeAll();
		fillTable(filterStack(trace));
		fTable.setRedraw(true);
	}

	protected void fillTable(String trace) {
		StringReader stringReader= new StringReader(trace);
		BufferedReader bufferedReader= new BufferedReader(stringReader);
		String line;

		try {	
			// first line contains the thrown exception
			line= bufferedReader.readLine();
			if (line == null)
				return;
				
			TableItem tableItem= new TableItem(fTable, SWT.NONE);
			String itemLabel= line.replace('\t', ' ');
			tableItem.setText(itemLabel);
			tableItem.setImage(fExceptionIcon);
			
			// the stack frames of the trace
			while ((line= bufferedReader.readLine()) != null) {
				itemLabel= line.replace('\t', ' ');
				if (itemLabel.trim().length() > 0) {
					tableItem= new TableItem(fTable, SWT.NONE);
					// heuristic for detecting a stack frame - works for JDK
					if ((itemLabel.indexOf(" at ") >= 0)) { //$NON-NLS-1$
						tableItem.setImage(fStackIcon);
					}
					tableItem.setText(itemLabel);
				}	
			}
		} catch (IOException e) {
			TableItem tableItem= new TableItem(fTable, SWT.NONE);
			tableItem.setText(trace);
		}			
	}
	
	/**
	 * Shows other information than a stack trace.
	 */
	public void setInformation(String text) {
		clear();
		TableItem tableItem= new TableItem(fTable, SWT.NONE);
		tableItem.setText(text);
	}
	
	/**
	 * Clears the non-stack trace info
	 */
	public void clear() {
		fTable.removeAll();
		fInputTrace= null;
	}
	
	private String filterStack(String stackTrace) {	
		if (!JUnitPreferencePage.getFilterStack() || stackTrace == null) 
			return stackTrace;
			
		StringWriter stringWriter= new StringWriter();
		PrintWriter printWriter= new PrintWriter(stringWriter);
		StringReader stringReader= new StringReader(stackTrace);
		BufferedReader bufferedReader= new BufferedReader(stringReader);	
			
		String line;
		String[] patterns= JUnitPreferencePage.getFilterPatterns();
		try {	
			while ((line= bufferedReader.readLine()) != null) {
				if (!filterLine(patterns, line))
					printWriter.println(line);
			}
		} catch (IOException e) {
			return stackTrace; // return the stack unfiltered
		}
		return stringWriter.toString();
	}
	
	private boolean filterLine(String[] patterns, String line) {
		for (int i= 0; i < patterns.length; i++) {
			if (line.indexOf(patterns[i]) > 0)
				return true;
		}		
		return false;
	}
}
