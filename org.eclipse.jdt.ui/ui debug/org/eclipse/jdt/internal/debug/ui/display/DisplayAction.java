package org.eclipse.jdt.internal.debug.ui.display;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.text.MessageFormat;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Displays the result of an evaluation in the display view
 */
public class DisplayAction extends EvaluateAction {
	
	public DisplayAction(IWorkbenchPart workbenchPart, boolean usedInEditor) {
		super(workbenchPart, usedInEditor);
		
		setText(DisplayMessages.getString("Display.label")); //$NON-NLS-1$
		setToolTipText(DisplayMessages.getString("Display.tooltip")); //$NON-NLS-1$
		setDescription(DisplayMessages.getString("Display.description")); //$NON-NLS-1$
		JavaPluginImages.setToolImageDescriptors(this, "disp_sbook.gif"); //$NON-NLS-1$
	}
	
	public void evaluationComplete(final IEvaluationResult result) {
		
		final IJavaValue value= result.getValue();
		
		if (result.hasProblems() || value != null) {
			Display display= Display.getDefault();
			if (display.isDisposed()) {
				return;
			}
			display.asyncExec(new Runnable() {
				public void run() {
					if (result.hasProblems()) {
						reportProblems(result);
						IDataDisplay dataDisplay= getDataDisplay();
						if (dataDisplay != null) {
							dataDisplay.displayExpressionValue(DisplayMessages.getString("DisplayAction.(evaluation_failed)_1")); //$NON-NLS-1$
						}
					}
					if (value != null) {
						insertResult(value, result.getThread());
					}
				}
			});
		}
	}
	
	protected void insertResult(IJavaValue result, IJavaThread thread) {
		
		String resultString= " "; //$NON-NLS-1$
		try {
			String sig= null;
			IJavaType type= result.getJavaType();
			if (type != null) {
				sig= type.getSignature();
			}
			if ("V".equals(sig)) { //$NON-NLS-1$
				resultString= DisplayMessages.getString("Display.no_result_value"); //$NON-NLS-1$
			} else {
				if (sig != null) {
					resultString= MessageFormat.format(DisplayMessages.getString("Display.type_name_pattern"), new Object[] { result.getReferenceTypeName() }); //$NON-NLS-1$
				}
				resultString= MessageFormat.format(DisplayMessages.getString("Display.result_pattern"), new Object[] { resultString, result.evaluateToString(thread) }); //$NON-NLS-1$
			}
		} catch(DebugException x) {
			reportError(x);
		}
		
		IDataDisplay dataDisplay= getDataDisplay();
		if (dataDisplay != null) {
			dataDisplay.displayExpressionValue(resultString);
		}
	}
}