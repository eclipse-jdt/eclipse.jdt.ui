package org.eclipse.jdt.internal.debug.ui.display;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Places the result of an evaluation in the debug inspector
 */
public class InspectAction extends EvaluateAction {

	public InspectAction(IWorkbenchPart workbenchPart, boolean usedInEditor) {
		super(workbenchPart, usedInEditor);
		
		setText(DisplayMessages.getString("Inspect.label")); //$NON-NLS-1$
		setToolTipText(DisplayMessages.getString("Inspect.tooltip")); //$NON-NLS-1$
		setDescription(DisplayMessages.getString("Inspect.description")); //$NON-NLS-1$
		JavaPluginImages.setToolImageDescriptors(this, "insp_sbook.gif"); //$NON-NLS-1$
	}
	
	public void evaluationComplete(final IEvaluationResult res) {
		final IValue value= res.getValue();
		if (res.hasProblems() || value != null) {
			Display display= Display.getDefault();
			if (display.isDisposed()) {
				return;
			}
			display.asyncExec(new Runnable() {
				public void run() {
					if (res.hasProblems()) {
						reportProblems(res);
					} 
					if (value != null) {
						DebugUITools.inspect(res.getSnippet().trim(), value);
					}
				}
			});
		}
	}
	
	/**
	 * Returns whether to display the expression via
	 * the data display.
	 */
	protected boolean displayExpression() {
		return false;
	}
}
