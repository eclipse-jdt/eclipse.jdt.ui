package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
 
import java.util.ResourceBundle;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IJavaEvaluationListener;
import org.eclipse.jdt.debug.core.IJavaStackFrame;import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Action to do simple code evaluation. The evaluation
 * is done in the UI thread and the result is inserted into the text
 * directly.
 */
public abstract class EvaluateAction extends ResourceAction implements IUpdate, IJavaEvaluationListener {

	JavaEditor fEditor;
	ITextSelection fSelection;

	public EvaluateAction(ResourceBundle bundle, String prefix, JavaEditor editor) {
		super(bundle, prefix);
		fEditor= editor;
	}
	/**
	 * Resolves the stack frame context for the evaluation
	 */
	protected IStackFrame getContext() {
		IStackFrame frame= getContextFromUI();
		if (frame == null) {
			frame= getContextFromModel();
		}
		return frame;
	}
	/**
	 * Resolves a stack frame context from the model
	 */
	protected IStackFrame getContextFromDebugTarget(IDebugTarget dt) {
		if (!dt.isTerminated()) {
			try {
				IDebugElement[] threads= dt.getChildren();
				for (int i= 0; i < threads.length; i++) {
					IThread thread= (IThread)threads[i];
					if (thread.isSuspended()) {
						return thread.getTopStackFrame();
					}
				}
			} catch(DebugException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
		return null;
	}
	/**
	 * Resolves a stack frame context from the model
	 */
	protected IStackFrame getContextFromModel() {
		IDebugTarget[] dts= DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
		for (int i= 0; i < dts.length; i++) {
			IDebugTarget dt= dts[i];
			IStackFrame frame = getContextFromDebugTarget(dt);
			if (frame != null) {
				return frame;
			}
		}
		return null;
	}
	/**
	 * Resolves a stack frame context from the model
	 */
	protected IStackFrame getContextFromThread(IThread thread) {
		try {
			if (thread.isSuspended()) {
				return thread.getTopStackFrame();
			}
		} catch(DebugException e) {
			JavaPlugin.log(e.getStatus());
		}
		return null;
	}
	/**
	 * Resolves a stack frame context from the UI
	 */
	protected IStackFrame getContextFromUI() {
		IWorkbenchPage page = fEditor.getSite().getWorkbenchWindow().getActivePage();
		if (page == null) {
			return null;
		}
		IViewPart part = page.findView(IDebugUIConstants.ID_DEBUG_VIEW);
		if (part != null) {
			ISelectionProvider sp = part.getSite().getSelectionProvider();
			if (sp != null) {
				ISelection s = sp.getSelection();
				if (s instanceof IStructuredSelection && !s.isEmpty()) {
					IStructuredSelection ss = (IStructuredSelection)s;
					Object item = ss.getFirstElement();
					if (item instanceof IStackFrame) {
						return (IStackFrame)item;
					}
					if (item instanceof IThread) {
						return getContextFromThread((IThread)item);
					}
					if (item instanceof IDebugTarget) {
						return getContextFromDebugTarget((IDebugTarget)item);
					}
				}
			}
		}
		return null;
	}
	public void run() {
		IStackFrame stackFrame= getContext();
		if (stackFrame == null) {
			System.out.println("No stack frame context");
			return;
		}
		
		IJavaStackFrame adapter= (IJavaStackFrame) stackFrame.getAdapter(IJavaStackFrame.class);
		if (adapter != null) {
			fSelection = (ITextSelection) fEditor.getSelectionProvider().getSelection();
			IJavaProject project = ((IJavaElement) fEditor.getJavaSourceReferenceAt(fSelection.getOffset())).getJavaProject();
			try {
				String eval= fSelection.getText();
				adapter.evaluate(eval, this, project);

			} catch (DebugException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("No evaluation adapter");
		}
	}
	/**
	 * @see IUpdate
	 */
	public void update() {
		setEnabled(getContext() != null &&  !fEditor.getSelectionProvider().getSelection().isEmpty());
	}
}
