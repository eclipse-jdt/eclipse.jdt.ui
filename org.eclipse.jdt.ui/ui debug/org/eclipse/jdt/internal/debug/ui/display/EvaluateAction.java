/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui.display;


import com.sun.jdi.InvocationException;import com.sun.jdi.ObjectReference;import java.util.ResourceBundle;import org.eclipse.core.resources.IMarker;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.model.IDebugElement;import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.core.model.ISourceLocator;import org.eclipse.debug.core.model.IStackFrame;import org.eclipse.debug.core.model.IThread;import org.eclipse.debug.ui.IDebugUIConstants;import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.text.ITextSelection;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.ui.IViewPart;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.ui.texteditor.IUpdate;import org.eclipse.ui.texteditor.ResourceAction;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.debug.core.IJavaEvaluationListener;
import org.eclipse.jdt.debug.core.IJavaEvaluationResult;import org.eclipse.jdt.debug.core.IJavaStackFrame;import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Action to do simple code evaluation. The evaluation
 * is done in the UI thread and the result is inserted into the text
 * directly.
 */
public abstract class EvaluateAction extends ResourceAction implements IUpdate, IJavaEvaluationListener {
	
	protected ResourceBundle fResourceBundle;
	protected String fPrefix;
	
	protected IWorkbenchPart fWorkbenchPart;
	protected String fExpression;
	
	
	public EvaluateAction(ResourceBundle bundle, String prefix, IWorkbenchPart workbenchPart) {
		super(bundle, prefix);
		
		fPrefix= prefix;
		fResourceBundle= bundle;
		fWorkbenchPart= workbenchPart;
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
			} catch(DebugException x) {
				JavaPlugin.log(x.getStatus());
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
		} catch(DebugException x) {
			JavaPlugin.log(x.getStatus());
		}
		return null;
	}
	
	/**
	 * Resolves a stack frame context from the UI
	 */
	protected IStackFrame getContextFromUI() {
		IWorkbenchPage page = fWorkbenchPart.getSite().getWorkbenchWindow().getActivePage();
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
		
		fExpression= null;
		
		IStackFrame stackFrame= getContext();
		if (stackFrame == null) {
			reportError(getErrorResourceString("nosfcontext"));
			return;
		}
		
		IJavaStackFrame adapter= (IJavaStackFrame) stackFrame.getAdapter(IJavaStackFrame.class);
		if (adapter != null) {
			
			
			IJavaElement javaElement= getJavaElement(stackFrame);
			if (javaElement != null) {
				IJavaProject project = javaElement.getJavaProject();
				try {
					
					ITextSelection selection = (ITextSelection) fWorkbenchPart.getSite().getSelectionProvider().getSelection();
					fExpression= selection.getText();
					
					IDataDisplay dataDisplay= getDataDisplay();
					if (dataDisplay != null)
						dataDisplay.displayExpression(fExpression);
						
					adapter.evaluate(fExpression, this, project);
					
				} catch (DebugException e) {
					reportError(e);
				}
			} else {
				reportError(getErrorResourceString("nosrccontext"));
			}
		} else {
			reportError(getErrorResourceString("noevaladapter"));
		}
	}
	
	protected IJavaElement getJavaElement(IStackFrame stackFrame) {
		
		// Get the corresponding element.
		ISourceLocator locator= stackFrame.getSourceLocator();
		if (locator == null)
			return null;
		
		Object sourceElement = locator.getSourceElement(stackFrame);
		if (sourceElement instanceof IType)
			return (IType) sourceElement;
		
		return null;
	}
	
	/**
	 * @see IUpdate
	 */
	public void update() {
		if (fWorkbenchPart instanceof ClassFileEditor) {
			setEnabled(false);
			return;
		}
		setEnabled(getContext() != null && 
			textHasContent(((ITextSelection)fWorkbenchPart.getSite().getSelectionProvider().getSelection()).getText()));
	}
	
	protected Shell getShell() {
		return fWorkbenchPart.getSite().getShell();
	}
	
	protected IDataDisplay getDataDisplay() {
		
		Object value= fWorkbenchPart.getAdapter(IDataDisplay.class);
		if (value instanceof IDataDisplay)
			return (IDataDisplay) value;
		
		return null;
	}	
	
	protected boolean textHasContent(String text) {
		int length= text.length();
		if (length > 0) {
			for (int i= 0; i < length; i++) {
				if (Character.isLetterOrDigit(text.charAt(i))) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected void reportError(String message) {
		Status status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, message, null);
		reportError(status);
	}
	
	protected void reportError(IStatus status) {
		ErrorDialog.openError(getShell(), getErrorResourceString("errorevaluating"), null, status);
	}
	
	protected void reportError(Throwable exception) {
		if (exception instanceof DebugException) {
			DebugException de = (DebugException)exception;
			Throwable t= de.getStatus().getException();
			if (t != null) {
				reportWrappedException(t);
				return;
			}
		}
		
		if (exception instanceof CoreException) {
			CoreException ce= (CoreException) exception;
			reportError(ce.getStatus());
			return;
		}
		
		String message = getErrorResourceString("exceptionevaluating");
		message += " " + exception.getClass();
		if (exception.getMessage() != null) {
			message += " - " + exception.getMessage();
		}
		reportError(message);
	}
	
	protected void reportProblems(IJavaEvaluationResult result) {
		IMarker[] problems= result.getProblems();
		if (problems.length == 0)
			reportError(result.getException());
		else
			reportProblems(problems);
	}
	
	protected void reportProblems(IMarker[] problems) {
		
		String defaultMsg= getErrorResourceString("unqualified");
		
		StringBuffer buffer= new StringBuffer();
		for (int i= 0; i < problems.length; i++) {
			if (i > 0) buffer.append('\n');
			buffer.append(problems[i].getAttribute(IMarker.MESSAGE, defaultMsg));
		}
		
		reportError(buffer.toString());
	}
	
	protected void reportWrappedException(Throwable exception) {
		if (exception instanceof com.sun.jdi.InvocationException) {
			InvocationException ie= (InvocationException) exception;
			ObjectReference ref= ie.exception();
			String message = getErrorResourceString("exceptionevaluating");
			reportError(message + " " + ref.referenceType().name());
		} else
			reportError(exception);
	}
	
	protected String getErrorResourceString(String key) {
		String s= fPrefix + "error." + key;
		return getString(fResourceBundle, s, s);
	}
}
