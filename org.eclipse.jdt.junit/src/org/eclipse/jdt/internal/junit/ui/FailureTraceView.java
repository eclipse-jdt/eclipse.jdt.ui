/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de bug 37333, 26653 
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IOpenEventListener;
import org.eclipse.jface.util.OpenStrategy;

/**
 * A view that shows a stack trace of a failed test.
 */
class FailureTraceView implements IMenuListener {
    private final Image fStackIcon= TestRunnerViewPart.createImage("obj16/stkfrm_obj.gif"); //$NON-NLS-1$
    private final Image fExceptionIcon= TestRunnerViewPart.createImage("obj16/exc_catch.gif"); //$NON-NLS-1$
    
    private static final String FRAME_PREFIX= "at "; //$NON-NLS-1$
	private Table fTable;
	private TestRunnerViewPart fTestRunner;
	private String fInputTrace;
	private final Clipboard fClipboard;
    private TestRunInfo fFailure;
    private CompareResultsAction fCompareAction;

	public FailureTraceView(Composite parent, Clipboard clipboard, TestRunnerViewPart testRunner, ToolBar toolBar) {
		Assert.isNotNull(clipboard);
		
		// fill the failure trace viewer toolbar
		ToolBarManager failureToolBarmanager= new ToolBarManager(toolBar);
		failureToolBarmanager.add(new EnableStackFilterAction(this));			
		fCompareAction = new CompareResultsAction(this);
		fCompareAction.setEnabled(false);
        failureToolBarmanager.add(fCompareAction);			
		failureToolBarmanager.update(true);
		
		fTable= new Table(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		fTestRunner= testRunner;
		fClipboard= clipboard;
		
		OpenStrategy handler = new OpenStrategy(fTable);
		handler.addOpenListener(new IOpenEventListener() {
			public void handleOpen(SelectionEvent e) {
				if (fTable.getSelectionIndex() == 0 && fFailure.isComparisonFailure()) {
					(new CompareResultsAction(FailureTraceView.this)).run();
				}
				if (fTable.getSelection().length != 0) {
					Action a = createOpenEditorAction(getSelectedText());
					if (a != null)
						a.run();
				}
			}
		});
		
		initMenu();
		
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});
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
			manager.add(new CopyTraceAction(FailureTraceView.this, fClipboard));
		}
		if (fFailure.isComparisonFailure()) 
			manager.add(new CompareResultsAction(FailureTraceView.this));
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
			int innerSeparatorIndex= testName.indexOf('$');
			if (innerSeparatorIndex != -1)
				testName= testName.substring(0, innerSeparatorIndex);
			
			String lineNumber= traceLine;
			lineNumber= lineNumber.substring(lineNumber.indexOf(':') + 1, lineNumber.indexOf(')'));
			int line= Integer.valueOf(lineNumber).intValue();
			//fix for bug 37333	
			String cuName= traceLine.substring(traceLine.indexOf('(') + 1, traceLine.indexOf(':'));
			return new OpenEditorAtLineAction(fTestRunner, cuName, testName, line);
		} catch(NumberFormatException e) {
		}
		catch(IndexOutOfBoundsException e) {	
		}	
		return null;
	}
	
	private void disposeIcons(){
		if (fExceptionIcon != null && !fExceptionIcon.isDisposed()) 
			fExceptionIcon.dispose();
		if (fStackIcon != null && !fStackIcon.isDisposed()) 
			fStackIcon.dispose();
	}
	
	/**
	 * Returns the composite used to present the trace
	 */
	Composite getComposite(){
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
	 * @param failure the failed test
	 */
	public void showFailure(TestRunInfo failure) {	
	    fFailure= failure;
	    String trace= ""; //$NON-NLS-1$
	    updateEnablement(failure);
	    if (failure != null) 
	        trace= failure.getTrace();
		if (fInputTrace == trace)
			return;
		fInputTrace= trace;
		updateTable(trace);
	}

	public void updateEnablement(TestRunInfo failure) {
		fCompareAction.setEnabled(failure != null && failure.isComparisonFailure());
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

	private void fillTable(String trace) {
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
				tableItem= new TableItem(fTable, SWT.NONE);
				// heuristic for detecting a stack frame - works for JDK
				if ((itemLabel.indexOf(" at ") >= 0)) { //$NON-NLS-1$
					tableItem.setImage(fStackIcon);
				}
				tableItem.setText(itemLabel);
			}
		} catch (IOException e) {
			TableItem tableItem= new TableItem(fTable, SWT.NONE);
			tableItem.setText(trace);
		}			
	}
	
	/**
	 * Shows other information than a stack trace.
	 * @param text the informational message to be shown
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
		String pattern;
		int len;
		for (int i= (patterns.length - 1); i >= 0; --i) {
			pattern= patterns[i];
			len= pattern.length() - 1;
			if (pattern.charAt(len) == '*') {
				//strip trailing * from a package filter
				pattern= pattern.substring(0, len);
			} else if (Character.isUpperCase(pattern.charAt(0))) {
				//class in the default package
				pattern= FRAME_PREFIX + pattern + '.';
			} else {
				//class names start w/ an uppercase letter after the .
				final int lastDotIndex= pattern.lastIndexOf('.');
				if ((lastDotIndex != -1) && (lastDotIndex != len) && Character.isUpperCase(pattern.charAt(lastDotIndex + 1)))
					pattern += '.'; //append . to a class filter
			}

			if (line.indexOf(pattern) > 0)
				return true;
		}		
		return false;
	}

    public TestRunInfo getFailedTest() {
        return fFailure;
    }

    public Shell getShell() {
        return fTable.getShell();
    }
}
