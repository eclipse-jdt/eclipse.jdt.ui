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

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Copies a test failure stack trace to the clipboard.
 */
public class CopyTraceAction extends Action {
	private FailureTraceView fView;
	
	private Clipboard fClipboard;

	/**
	 * Constructor for CopyTraceAction.
	 */
	public CopyTraceAction(FailureTraceView view) {
		super(JUnitMessages.getString("CopyTrace.action.label"));  //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJUnitHelpContextIds.COPYTRACE_ACTION);
		fView= view;
		fClipboard= new Clipboard(fView.getComposite().getDisplay());
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		String trace= fView.getTrace();
		if (trace == null)
			trace= ""; //$NON-NLS-1$
		
		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		try{
			fClipboard.setContents(
				new String[]{ convertLineTerminators(trace) }, 
				new Transfer[]{ plainTextTransfer });
		}  catch (SWTError e){
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) 
				throw e;
			if (MessageDialog.openQuestion(fView.getComposite().getShell(), JUnitMessages.getString("CopyTraceAction.problem"), JUnitMessages.getString("CopyTraceAction.clipboard_busy")))  //$NON-NLS-1$ //$NON-NLS-2$
				run();
		}
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
