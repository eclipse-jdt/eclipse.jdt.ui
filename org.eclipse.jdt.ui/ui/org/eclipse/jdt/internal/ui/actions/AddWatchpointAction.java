package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.DebugJavaUtils;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
			IMarker breakpoint= JDIDebugModel.createWatchpoint(field, 0);
			DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(breakpoint);
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
				IBreakpointManager manager= DebugPlugin.getDefault().getBreakpointManager();
				IMarker[] breakpoints= manager.getBreakpoints();
				for (int i=0; i<breakpoints.length; i++) {
					IMarker breakpoint= breakpoints[i];
					IField breakpointField= DebugJavaUtils.getField(breakpoint);
					if (breakpointField != null && equalFields(breakpointField, field)) {
						return false;
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
		ICompilationUnit cu= JDIDebugModel.getCompilationUnit(field);
		return (breakpointField.getCompilationUnit().equals(cu) &&
		breakpointField.getElementName().equals(field.getElementName()) &&
		breakpointField.getDeclaringType().getElementName().equals(field.getDeclaringType().getElementName()));
	}
}

