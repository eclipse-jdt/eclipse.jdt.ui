/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui.display;


import java.util.ResourceBundle;import org.eclipse.core.resources.IMarker;import org.eclipse.debug.core.DebugException;import org.eclipse.swt.widgets.Display;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.jdt.debug.core.IJavaEvaluationResult;import org.eclipse.jdt.debug.core.IJavaValue;


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
