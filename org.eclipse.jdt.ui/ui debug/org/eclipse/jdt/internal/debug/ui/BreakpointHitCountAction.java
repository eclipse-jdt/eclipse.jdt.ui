package org.eclipse.jdt.internal.debug.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.util.Iterator;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.jdt.debug.core.IJavaDebugConstants;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class BreakpointHitCountAction extends Action implements IViewActionDelegate {

	private final static String PREFIX= "breakpoint_hit_count_action.";
	private final static String ERROR= PREFIX + "error.";
	private static final String DIALOG_TITLE= PREFIX + "dialog.title";
	private static final String DIALOG_MESSAGE= PREFIX + "dialog.message";
	private static final String DIALOG_INVALID= PREFIX + "dialog.invalid";

	private static final String INITIAL_VALUE= "0";

	protected IStructuredSelection fCurrentSelection;

	/**
	 * A dialog that sets the focus to the text area.
	 */
	class HitCountDialog extends InputDialog {
		protected  HitCountDialog(Shell parentShell,
									String dialogTitle,
									String dialogMessage,
									String initialValue,
									IInputValidator validator) {
			super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
		}
		
		public int open() {
			Display display= JavaPlugin.getActiveWorkbenchWindow().getShell().getDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					getText().setFocus();
				}
			});
			
			return super.open();
		}
	}
	
	public BreakpointHitCountAction() {
		setEnabled(false);
	}

	/**
	 * Returns the plugin's breakpoint manager
	 */
	protected IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}

	/**
	 * @see IActionDelegate
	 */
	public void run(IAction action) {
		IStructuredSelection selection= getStructuredSelection();
		Iterator enum= selection.iterator();
		if (!enum.hasNext()) {
			return;
		}

		while (enum.hasNext()) {
			IMarker breakpoint= (IMarker)enum.next();
			int newHitCount= hitCountDialog(breakpoint);
			if (newHitCount != -1) {				
				try {
					JDIDebugModel.setHitCount(breakpoint, newHitCount);
					getBreakpointManager().setEnabled(breakpoint, true);
				} catch (CoreException ce) {
					DebugUIUtils.logError(ce);
				}
			}
		}
	}

	/**
	 * @see IAction
	 */
	public void run() {
		run(null);
	}

	protected int hitCountDialog(IMarker marker) {
		String title= DebugUIUtils.getResourceString(DIALOG_TITLE);
		String message= DebugUIUtils.getResourceString(DIALOG_MESSAGE);
		IInputValidator validator= new IInputValidator() {
			int hitCount= -1;
			public String isValid(String value) {
				try {
					hitCount= Integer.valueOf(value.trim()).intValue();
				} catch (NumberFormatException nfe) {
					hitCount= -1;
				}
				if (hitCount < 0) {
					String msg= DebugUIUtils.getResourceString(DIALOG_INVALID);
					return msg;
				}
				//no error
				return null;
			}
		};

		int currentHitCount= JDIDebugModel.getHitCount(marker);
		String initialValue;
		if (currentHitCount > 0) {
			initialValue= Integer.toString(currentHitCount);
		} else {
			initialValue= INITIAL_VALUE;
		}
		Shell activeShell= JavaPlugin.getActiveWorkbenchWindow().getShell();
		InputDialog dialog= new HitCountDialog(activeShell, title, message, initialValue, validator);
		if (dialog.open() != dialog.OK) {
			return -1;
		}

		return Integer.parseInt(dialog.getValue().trim());
	}

	/**
	 * @see IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fCurrentSelection= (IStructuredSelection)sel;
			Object[] elements= fCurrentSelection.toArray();
			action.setEnabled(elements.length == 1 && isEnabledFor(elements[0]));
		}
	}

	/**
	 * @see IViewActionDelegate
	 */
	public void init(IViewPart view) {
	}

	protected IStructuredSelection getStructuredSelection() {
		return fCurrentSelection;
	}

	public boolean isEnabledFor(Object element) {
		try {
			return element instanceof IMarker && ((IMarker)element).isSubtypeOf(IJavaDebugConstants.JAVA_LINE_BREAKPOINT);
		} catch (CoreException ce) {
			return false;
		}
	}

}
