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
						showProblems(result);
					if (value != null)
						insertResult(value);
				}
			});
		}
	}
	
	protected void showProblems(IJavaEvaluationResult result) {
		IMarker[] problems= result.getProblems();
		if (problems.length == 0) {
			Throwable throwable= result.getException();
			Status status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, throwable.getMessage(), throwable);
			reportError(status);
		} else
			showProblems(problems);
	}
	
	protected void showProblems(IMarker[] problems) {
		
		String defaultMsg= getErrorResourceString("unqualified");
		String position= getErrorResourceString("position");
		
		StringBuffer buffer= new StringBuffer();
		for (int i= 0; i < problems.length; i++) {
			if (i > 0) buffer.append('\n');
			buffer.append(position);
			buffer.append(' ');
			buffer.append(MarkerUtilities.getCharStart(problems[i]));
			buffer.append(' ');
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
			reportError(x.getStatus());
		}
		
		IDataDisplay dataDisplay= getDataDisplay(fWorkbenchPart);
		if (dataDisplay != null)
			dataDisplay.display(fExpression, resultString.toString());
	}
	
	protected IDataDisplay getDataDisplay(IWorkbenchPart workbenchPart) {
		
		Object value= workbenchPart.getAdapter(IDataDisplay.class);
		if (value instanceof IDataDisplay)
			return (IDataDisplay) value;
		
		return null;
	}
}
