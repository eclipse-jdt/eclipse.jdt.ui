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

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.action.Action;

/**
 * Copies a test failure stack trace to the clipboard.
 */
public class CopyTraceAction extends Action {
	private String fTrace;
	private FailureTraceView fView;
	
	/**
	 * Constructor for CopyTraceAction.
	 */
	public CopyTraceAction(FailureTraceView view, String trace) {
		super("Copy Trace"); 
		fTrace= trace;
		if (fTrace == null)
			fTrace= "";
		fView= view;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		Clipboard clipboard= new Clipboard(fView.getComposite().getDisplay());		
		clipboard.setContents(
			new String[]{ convertLineTerminators(fTrace) }, 
			new Transfer[]{ plainTextTransfer });
	}

	private String convertLineTerminators(String in) {
		StringWriter stringWriter= new StringWriter();
		PrintWriter printWriter= new PrintWriter(stringWriter);
		StringReader stringReader= new StringReader(in);
		BufferedReader bufferedReader= new BufferedReader(stringReader);		
		String line;
		try {
			while ((line= bufferedReader.readLine()) != null) {
				printWriter.println(line);
			}
		} catch (IOException e) {
			return in; // return the trace unfiltered
		}
		return stringWriter.toString();
	}
}
