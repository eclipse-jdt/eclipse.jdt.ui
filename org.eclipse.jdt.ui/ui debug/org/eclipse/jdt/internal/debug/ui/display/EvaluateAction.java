/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui.display;


import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.eval.EvaluationManager;
import org.eclipse.jdt.debug.eval.IEvaluationEngine;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.internal.debug.ui.JDIModelPresentation;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IUpdate;

import com.sun.jdi.InvocationException;
import com.sun.jdi.ObjectReference;


/**
 * Action to do simple code evaluation. The evaluation
 * is done in the UI thread and the result is inserted into the text
 * directly.
 */
public abstract class EvaluateAction extends Action implements IUpdate, IEvaluationListener {
		
	protected IWorkbenchPart fWorkbenchPart;
	protected String fExpression;
	
	/**
	 * Indicates whether this action is used from within an editor.  If so,
	 * then this action is enabled only when the editor's input matches the
	 * editor input corresponding to the currently selected stack frame.
	 * If this flag is false, then this action is enabled whenever there is
	 * a stack frame selected in the UI.
	 */
	private boolean fUsedInEditor;
		
	public EvaluateAction(IWorkbenchPart workbenchPart, boolean usedInEditor) {
		super();
		fWorkbenchPart= workbenchPart;
		fUsedInEditor = usedInEditor;
	}
	
	/**
	 * Finds the currently selected stack frame in the UI
	 */
	protected IStackFrame getContext() {
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
				}
			}
		}
		return null;
	}
	
	public void run() {
		
		fExpression= null;
		
		IStackFrame stackFrame= getContext();
		if (stackFrame == null) {
			reportError(DisplayMessages.getString("Evaluate.error.message.stack_frame_context")); //$NON-NLS-1$
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
					if (dataDisplay != null && displayExpression())
						dataDisplay.displayExpression(fExpression);
					
					IEvaluationEngine engine = getEvauationEngine((IJavaDebugTarget)adapter.getDebugTarget(), project);
					engine.evaluate(fExpression, adapter, this);
					
				} catch (CoreException e) {
					reportError(e);
				}
			} else {
				reportError(DisplayMessages.getString("Evaluate.error.message.src_context")); //$NON-NLS-1$
			}
		} else {
			reportError(DisplayMessages.getString("Evaluate.error.message.eval_adapter")); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns an evaluation engine for the given debug target
	 * and Java project.
	 * 
	 * @param vm debug target on which the evaluation will be
	 *  performed
	 * @param project the context in which the evaluation will be
	 *  compiled
	 * @exception CoreException if creation of a new evaluation
	 *  engine is required and fails 
	 */
	protected IEvaluationEngine getEvauationEngine(IJavaDebugTarget vm, IJavaProject project) throws CoreException {
		IEvaluationEngine engine = EvaluationManager.getEvaluationEngine(vm);
		if (engine == null) {
			IPath outputLocation = project.getOutputLocation();
			IWorkspace workspace = project.getProject().getWorkspace();
			IResource res = workspace.getRoot().findMember(outputLocation);
			File dir = new File(res.getLocation().toOSString());
			engine= EvaluationManager.newLocalEvaluationEngine(project, vm, dir);
		}	
		return engine;
			
	}
	
	protected IJavaElement getJavaElement(IStackFrame stackFrame) {
		
		// Get the corresponding element.
		ILaunch launch = stackFrame.getLaunch();
		if (launch == null) {
			return null;
		}
		ISourceLocator locator= launch.getSourceLocator();
		if (locator == null)
			return null;
		
		Object sourceElement = locator.getSourceElement(stackFrame);
		if (sourceElement instanceof IJavaElement) {
			return (IJavaElement) sourceElement;
		}			
		return null;
	}
	
	/**
	 * @see IUpdate
	 */
	public void update() {
		boolean enabled = false;
		if (isValidStackFrame()) {
			ISelectionProvider provider = fWorkbenchPart.getSite().getSelectionProvider();
			if (provider != null)  {
				if (textHasContent(((ITextSelection)provider.getSelection()).getText())) {
					enabled = true;
				}
			}
		}
		setEnabled(enabled);
	}
	
	/**
	 * Returns true if the current stack frame context can be used for an
	 * evaluation, false otherwise.
	 */
	protected boolean isValidStackFrame() {
		IStackFrame stackFrame = getContext();
		if (stackFrame == null) {
			return false;
		}
		if (isUsedInEditor()) {
			return compareToEditorInput(stackFrame);
		} else {
			return true;
		}
	}
	
	/**
	 * Resolve an editor input from the source element of the stack frame
	 * argument, and return whether it's equal to the editor input for the
	 * editor that owns this action.
	 */
	protected boolean compareToEditorInput(IStackFrame stackFrame) {
		ILaunch launch = stackFrame.getLaunch();
		ISourceLocator sourceLocator = launch.getSourceLocator();
		Object sourceElement = sourceLocator.getSourceElement(stackFrame);
		JDIModelPresentation presentation = new JDIModelPresentation();
		IEditorInput sfEditorInput = presentation.getEditorInput(sourceElement);
		if (fWorkbenchPart instanceof IEditorPart) {
			return ((IEditorPart)fWorkbenchPart).getEditorInput().equals(sfEditorInput);
		}
		return false;
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
		ErrorDialog.openError(getShell(), "Error evaluating", null, status);
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
		
		String message= MessageFormat.format(DisplayMessages.getString("Evaluate.error.message.direct_exception"), new Object[] { exception.getClass() }); //$NON-NLS-1$
		if (exception.getMessage() != null)
			message= MessageFormat.format(DisplayMessages.getString("Evaluate.error.message.exception.pattern"), new Object[] { message, exception.getMessage() }); //$NON-NLS-1$
		reportError(message);
	}
	
	protected void reportProblems(IEvaluationResult result) {
		IMarker[] problems= result.getProblems();
		if (problems.length == 0)
			reportError(result.getException());
		else
			reportProblems(problems);
	}
	
	protected void reportProblems(IMarker[] problems) {
		
		String defaultMsg= DisplayMessages.getString("Evaluate.error.message.unqualified_error"); //$NON-NLS-1$
		
		String message= ""; //$NON-NLS-1$
		for (int i= 0; i < problems.length; i++) {
			IMarker problem= problems[i];
			if (problem.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
				String msg= problems[i].getAttribute(IMarker.MESSAGE, defaultMsg);
				if (i == 0) {
					message= msg;
				} else {
					message= MessageFormat.format(DisplayMessages.getString("Evaluate.error.problem_append_pattern"), new Object[] { message, msg }); //$NON-NLS-1$
				}
			}
		}
		
		if (message.length() != 0) {
			reportError(message);
		}
	}
	
	protected void reportWrappedException(Throwable exception) {
		if (exception instanceof com.sun.jdi.InvocationException) {
			InvocationException ie= (InvocationException) exception;
			ObjectReference ref= ie.exception();
			reportError(MessageFormat.format(DisplayMessages.getString("Evaluate.error.message.wrapped_exception"), new Object[] { ref.referenceType().name() })); //$NON-NLS-1$
		} else
			reportError(exception);
	}
	
	/**
	 * Returns whether to display the expression via
	 * the data display.
	 */
	protected boolean displayExpression() {
		return true;
	}
	
	protected boolean isUsedInEditor() {
		return fUsedInEditor;
	}
}
