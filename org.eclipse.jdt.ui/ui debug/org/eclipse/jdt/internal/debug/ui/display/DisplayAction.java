/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui.display;

import java.util.ResourceBundle;import org.eclipse.core.resources.IMarker;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.debug.core.DebugException;import org.eclipse.swt.widgets.Display;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.ui.texteditor.MarkerUtilities;import org.eclipse.jdt.debug.core.IJavaEvaluationResult;import org.eclipse.jdt.debug.core.IJavaValue;import org.eclipse.jdt.internal.ui.JavaPlugin;



/**
 * Displays the result of an evaluation in the display view
 */
public class DisplayAction extends EvaluateAction {
	
	public DisplayAction(ResourceBundle bundle, String prefix, IWorkbenchPart workbenchPart) {
		super(bundle, prefix, workbenchPart);
	}
	
	public void evaluationComplete(final IJavaEvaluationResult result) {
		
		final IJavaValue value= result.getValue();
		
		if (result.hasProblems() || value != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (result.hasProblems())
						reportProblems(result);
					if (value != null)
						insertResult(value);
				}
			});
		}
	}
	
	protected void reportProblems(IJavaEvaluationResult result) {
		IMarker[] problems= result.getProblems();
		if (problems.length == 0)
			reportError(result.getException());
		else
			reportProblems(problems);
	}
	
	protected void reportProblems(IMarker[] problems) {
		
		String defaultMsg= getErrorResourceString("unqualified");
		
		StringBuffer buffer= new StringBuffer();
		for (int i= 0; i < problems.length; i++) {
			if (i > 0) buffer.append('\n');
			buffer.append(problems[i].getAttribute(IMarker.MESSAGE, defaultMsg));
		}
		
		reportError(buffer.toString());
	}
		
	protected void insertResult(IJavaValue result) {
		
		StringBuffer resultString= new StringBuffer();
		try {
			String sig= result.getSignature();
			if ("V".equals(sig)) {
				resultString.append(' ');
				resultString.append(getErrorResourceString("noreturn"));
			} else {
				if (sig != null) {
					resultString.append(" (");
					resultString.append(result.getReferenceTypeName());
					resultString.append(") ");
				} else {
					resultString.append(' ');
				}  
				resultString.append(result.evaluateToString());
			}
		} catch(DebugException x) {
			reportError(x);
		}
		
		IDataDisplay dataDisplay= getDataDisplay();
		if (dataDisplay != null)
			dataDisplay.displayExpressionValue(resultString.toString());
	}
}
