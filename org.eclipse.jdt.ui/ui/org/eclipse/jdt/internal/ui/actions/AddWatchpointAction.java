package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.ui.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;

public class AddWatchpointAction extends JavaUIAction implements IUpdate {

	private ISelectionProvider fSelectionProvider;
		
	public AddWatchpointAction(ISelectionProvider selectionProvider) {
		super("Add &Watchpoint");
		setDescription("Add a watchpoint");
		setToolTipText("Add Watchpoint");
		fSelectionProvider= selectionProvider;
		
		WorkbenchHelp.setHelp(this, new Object[] {IJavaHelpContextIds.ADD_WATCHPOINT_ACTION });
	}

	/**
	 * @see Action#run()
	 */
	public void run() {
		ISelection selection= fSelectionProvider.getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IField field= (IField)((IStructuredSelection)selection).getFirstElement();
		try {
			JDIDebugModel.createWatchpoint(field, 0);
		} catch (DebugException x) {
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JavaUIMessages.getString("AddWatchpointAction.errorTitle"), x.getMessage());
		}
	}

	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		setEnabled(canActionBeAdded());
	}
	
	/**
	 * Returns whether this action can be added for the given selection
	 */
	public boolean canActionBeAdded() {
		ISelection selection= fSelectionProvider.getSelection();
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			IStructuredSelection structSel= (IStructuredSelection)selection;
			Object obj= structSel.getFirstElement();
			if (structSel.size() == 1 && obj instanceof IField) {
				IField field = (IField) obj;
				IBreakpoint[] breakpoints= getBreakpointManager().getBreakpoints();
				for (int i=0; i<breakpoints.length; i++) {
					IBreakpoint breakpoint= breakpoints[i];
					if (breakpoint instanceof IJavaWatchpoint) {
						IField breakpointField= null;
						try {
							breakpointField = ((IJavaWatchpoint) breakpoint).getField();
						} catch (CoreException e) {
							return false;
						}
						if (breakpointField != null && equalFields(breakpointField, field)) {
							return false;
						}
					}
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Compare two fields for <code>canActionBeAdded()</code>. The default <code>equals()</code>
	 * method for <code>IField</code> doesn't give the comparison desired.
	 */
	private boolean equalFields(IField breakpointField, IField field) {
		return (breakpointField.getElementName().equals(field.getElementName()) &&
		breakpointField.getDeclaringType().getElementName().equals(field.getDeclaringType().getElementName()));
	}
	
	private IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}
}

