package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.ResourceAction;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.JDIDebugModel;

import org.eclipse.jdt.internal.debug.core.JDIDebugTarget;import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Action to support run to line (i.e. where the cursor is)
 */
public class RunToLineAction extends ResourceAction implements IUpdate {

	protected static final String ERROR_RUN_TO_LINE= "error.run_to_line.";

	protected JavaEditor fEditor;

	protected ResourceBundle fBundle;
	protected String fPrefix;

	public RunToLineAction(ResourceBundle bundle, String prefix, JavaEditor editor) {
		super(bundle, prefix);
		fEditor= editor;
		fBundle= bundle;
		fPrefix= prefix;
		
		update();
	}

	public void run() {
		try {
			JDIDebugTarget target= getContext();
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
			IJavaElement je= (IJavaElement) fEditor.getJavaSourceReferenceAt(ts.getOffset());
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
	
			IMarker breakpoint= null;
			try {
				breakpoint= JDIDebugModel.createRunToLineBreakpoint(type, lineNumber, -1, -1);
			} catch (DebugException de) {
				errorDialog(de.getStatus());
				return;
			} 
			target.breakpointAdded(breakpoint);
			IDebugElement[] threads= target.getChildren(); 
			for (int i= 0; i < threads.length; i++) {
				IThread thread= (IThread) threads[i];
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

	/**
	 * Resolves the debug target context to set the run to line
	 */
	protected JDIDebugTarget getContext() throws DebugException{
		JDIDebugTarget target= getContextFromUI();
		if (target == null) {
			target= getContextFromModel();
		}
		if (target == null) {
			return null;
		}
		IDebugElement[] threads= target.getChildren();
		boolean threadSuspended= false;
		for (int i= 0; i < threads.length; i++) {
			IThread thread= (IThread) threads[i];
			if (thread.canResume()) {
				threadSuspended=true;
				break;
			}
		}
		if (threadSuspended) {
			return target;
		}
		return null;
	}

	/**
	 * Resolves a debug target context from the model
	 */
	protected JDIDebugTarget getContextFromModel() throws DebugException {
		IDebugTarget[] dts= DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
		for (int i= 0; i < dts.length; i++) {
			JDIDebugTarget dt= (JDIDebugTarget)dts[i];
			if (getContextFromDebugTarget(dt) != null) {
				return dt;
			}
		}
		return null;
	}

	/**
	 * Resolves a debug target context from the model
	 */
	protected JDIDebugTarget getContextFromThread(IThread thread) throws DebugException {
		if (thread.isSuspended()) {
			return (JDIDebugTarget) thread.getDebugTarget();
		}
		return null;
	}

	/**
	 * Resolves a stack frame context from the UI
	 */
	protected JDIDebugTarget getContextFromUI() throws DebugException {
		
		IWorkbenchPage page= fEditor.getSite().getWorkbenchWindow().getActivePage();
		if (page == null)
			return null;
			
		IViewPart part= page.findView(IDebugUIConstants.ID_DEBUG_VIEW);
		if (part == null)
			return null;
			
		ISelectionProvider sp= part.getSite().getSelectionProvider();
		if (sp != null) {
			ISelection s= sp.getSelection();
			if (s instanceof IStructuredSelection) {
				IStructuredSelection ss= (IStructuredSelection) s;
				Object item= ss.getFirstElement();
				if (item instanceof IStackFrame) {
					return (JDIDebugTarget) ((IStackFrame) item).getDebugTarget();
				}
				if (item instanceof IThread) {
					return getContextFromThread((IThread) item);
				}
				if (item instanceof JDIDebugTarget) {
					return (JDIDebugTarget) item;
				}
			}
		}
		
		return null;
	}


	protected void errorDialog(IStatus status) {
		Shell shell= fEditor.getSite().getShell();
		String key= fPrefix + ERROR_RUN_TO_LINE + "title";
		String title= getString(fBundle, key, key);
		key= fPrefix + ERROR_RUN_TO_LINE + "message";
		String msg= getString(fBundle, key, key);
		ErrorDialog.openError(shell, title, msg, status);
	}
	
	/**
	 * @see IUpdate
	 */
	public void update() {
		try {
			setEnabled(getContext() != null);
		} catch (DebugException de) {
			setEnabled(false);
			JavaPlugin.log(de.getStatus());
		}
	}
	
	/**
	 * Resolves a stack frame context from the model
	 */
	protected JDIDebugTarget getContextFromDebugTarget(JDIDebugTarget dt) throws DebugException {
		if (!dt.isTerminated()) {
			IDebugElement[] threads= dt.getChildren();
			for (int i= 0; i < threads.length; i++) {
				IThread thread= (IThread) threads[i];
				if (thread.isSuspended()) {
					return dt;
				}
			}
		}
		return null;
	}

}

