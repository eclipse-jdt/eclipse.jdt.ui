package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action to support run to line (i.e. where the cursor is)
 */
public class RunToLineAction extends Action implements IUpdate {

	protected JavaEditor fEditor;
	
	
	public RunToLineAction(JavaEditor editor) {
		super();
		
		setText(JavaEditorMessages.getString("RunToLine.label")); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("RunToLine.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("RunToLine.description")); //$NON-NLS-1$
		
		fEditor= editor;
		
		update();
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.RUN_TO_LINE_ACTION });					
	}

	protected IJavaDebugTarget getContext() {
		IDebugElement context = DebugUITools.getDebugContext();
		if (context != null) {
			context = context.getDebugTarget();
			if (context instanceof IJavaDebugTarget) {
				return (IJavaDebugTarget)context;
			}
		}		
		return null;
	}
	
	public void run() {
		try {
			IJavaDebugTarget target = getContext();
			if (target == null) {
				fEditor.getSite().getShell().getDisplay().beep();
				return;
			}
			ISelectionProvider sp= fEditor.getSelectionProvider();
			if (sp == null) {
				return;
			}
			ITextSelection ts= (ITextSelection) sp.getSelection();
			int lineNumber= ts.getStartLine() + 1;
			IJavaElement je= (IJavaElement) fEditor.getElementAt(ts.getOffset());
			IType type= null;
			if (je instanceof IType) {
				type= (IType) je;
			} else
				if (je instanceof IMember) {
					type= ((IMember) je).getDeclaringType();
				}
			if (type == null) {
				return;
			}
	
			IBreakpoint breakpoint= null;
			try {
				breakpoint= JDIDebugModel.createRunToLineBreakpoint(type, lineNumber, -1, -1);
			} catch (DebugException de) {
				errorDialog(de.getStatus());
				return;
			} 
			target.breakpointAdded(breakpoint);
			IThread[] threads= target.getThreads(); 
			for (int i= 0; i < threads.length; i++) {
				IThread thread= threads[i];
				if (thread.canResume()) {
					try {
						thread.resume();
					} catch (DebugException de) {

					}
					break;
				}
			}
		} catch(DebugException de) {
		}
	}

	protected void errorDialog(IStatus status) {
		Shell shell= fEditor.getSite().getShell();
		ErrorDialog.openError(shell, JavaEditorMessages.getString("RunToLine.error.title1"), JavaEditorMessages.getString("RunToLine.error.message1"), status); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * @see IUpdate
	 */
	public void update() {
		setEnabled(getContext() != null);
	}
	
}

