/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;

import java.io.ByteArrayOutputStream;import java.io.PrintStream;import java.lang.reflect.InvocationTargetException;import java.util.*;import org.eclipse.core.resources.*;import org.eclipse.core.runtime.*;import org.eclipse.debug.core.*;import org.eclipse.debug.core.model.IDebugElement;import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.ui.DebugUITools;import org.eclipse.jdt.core.*;import org.eclipse.jdt.core.eval.IEvaluationContext;import org.eclipse.jdt.debug.core.*;import org.eclipse.jdt.internal.ui.IJavaUIStatus;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.text.java.ResultCollector;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jdt.ui.text.JavaTextTools;import org.eclipse.jface.action.Action;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.dialogs.*;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.text.*;import org.eclipse.jface.text.source.ISourceViewer;import org.eclipse.jface.util.Assert;import org.eclipse.swt.custom.BusyIndicator;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.*;import org.eclipse.ui.part.EditorActionBarContributor;import org.eclipse.ui.part.FileEditorInput;import org.eclipse.ui.texteditor.*;import com.sun.jdi.InvocationException;import com.sun.jdi.ObjectReference;

/**
 * An editor for Java snippets.
 */
public class JavaSnippetEditor extends AbstractTextEditor implements IDebugEventListener, IJavaEvaluationListener {			

	public static final String PREFIX = "SnippetEditor.";
	public static final String ERROR = PREFIX + "error.";
	public static final String EVALUATING = PREFIX + "evaluating";
	
	public static final String PACKAGE_CONTEXT = PREFIX + "package";

	private final static String TAG= "input_element";
	
	final static int RESULT_DISPLAY= 1;
	final static int RESULT_RUN= 2;
	final static int RESULT_INSPECT= 3;
	
	private IJavaProject fJavaProject;
	private IEvaluationContext fEvaluationContext;
	private IDebugTarget fVM;
	private String[] fLaunchedClassPath;
	private List fSnippetStateListeners;	
	private int fResultMode; // one of the RESULT_* constants from above
	private boolean fEvaluating;
	private IJavaThread fThread;
	private int fAttempts= 0;
	
	private int fSnippetStart;
	private int fSnippetEnd;
	
	private String fPackageName= null;
	
	/**
	 * Default constructor.
	 */
	public JavaSnippetEditor() {
		super();
		setDocumentProvider(JavaPlugin.getDefault().getSnippetDocumentProvider());
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		setSourceViewerConfiguration(new JavaSnippetViewerConfiguration(textTools, this));		
		fSnippetStateListeners= new ArrayList(4);
	}
	
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		fPackageName = getPage().getPersistentProperty(new QualifiedName(JavaPlugin.getPluginId(), PACKAGE_CONTEXT));
	}
		
	public void dispose() {
		shutDownVM();
		fSnippetStateListeners= Collections.EMPTY_LIST;
		super.dispose();
	}
	/** 
	 * Returns the editor's resource bundle.
	 *
	 * @return the editor's resource bundle
	 */
	protected ResourceBundle getResourceBundle() {
		return JavaPlugin.getDefault().getResourceBundle();
	}
	
	/**
	 * Convenience method for safely accessing resources.
	 */
	protected String getResourceString(String key) {
		return JavaPlugin.getDefault().getResourceString(key);
	}
	
	/**
	 * @see AbstractTextEditor#createActions
	 */
	protected void createActions() {
		super.createActions();
		setAction("Display", new DisplayAction(this));		
		setAction("Run", new RunAction(this));
		setAction("Inspect", new InspectAction(this));
		
		Action a= new StopAction(this);
		a.setEnabled(false);
		setAction("Stop", a);

		setAction("RunInPackage", new RunInPackageAction(this));
		setAction("ContentAssistProposal", new TextOperationAction(getResourceBundle(), "Editor.ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS));			
		setAction("OpenOnSelection", new SnippetOpenOnSelectionAction(this, getResourceBundle(), "Editor.OpenOnSelection."));			
	}
	
	/**
	 * @see IEditorPart#saveState(IMemento)
	 */
	/*public void saveState(IMemento memento) {
		IFile file= (IFile) getEditorInput();
		memento.putString(TAG, file.getFullPath().toString());
	}*/
	
	/**
	 * @see AbstractTextEditor#editorContextMenuAboutToShow(MenuManager)
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addGroup(menu, ITextEditorActionConstants.GROUP_EDIT, IContextMenuConstants.GROUP_GENERATE);		
		addGroup(menu, ITextEditorActionConstants.GROUP_FIND, IContextMenuConstants.GROUP_SEARCH);		
		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "ContentAssistProposal");
		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "OpenOnSelection");
		addAction(menu, IContextMenuConstants.GROUP_SEARCH, "Display");
		addAction(menu, IContextMenuConstants.GROUP_SEARCH, "Run");
		addAction(menu, IContextMenuConstants.GROUP_SEARCH, "Inspect");
	}

	public boolean isVMLaunched() {
		return fVM != null;
	}
	
	public boolean isEvaluating() {
		return fEvaluating;
	}
	
	public void evalSelection(final int resultMode) {
		if (isEvaluating()) {
			return;
		}
		evaluationStarts();
		fireEvalStateChanged();
		
		fResultMode= resultMode;
		buildAndLaunch();
		
		if (fVM == null) {
			evaluationEnds();
			return;
		}
		fireEvalStateChanged();

		ITextSelection selection= (ITextSelection) getSelectionProvider().getSelection();
		String snippet= selection.getText();
		if (snippet.length() == 0) {
			evaluationEnds();
			return;
		}
		fSnippetStart= selection.getOffset();
		fSnippetEnd= fSnippetStart + selection.getLength();
		
		evaluate(snippet);			
	}	
	
	
	protected void buildAndLaunch() {
		boolean build = !getJavaProject().getProject().getWorkspace().isAutoBuilding()
			|| !getJavaProject().hasBuildState();
		
		if (build) {
			IRunnableWithProgress r= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					try {
						getJavaProject().getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, pm);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			try {
				new ProgressMonitorDialog(getShell()).run(true, false, r);		
			} catch (InterruptedException e) {
				evaluationEnds();
				return;
			} catch (InvocationTargetException e) {
				evaluationEnds();
				return;
			}
		}

		boolean cpChange = classPathHasChanged();
		boolean launch = fVM == null || cpChange;
				
		if (cpChange) {
			shutDownVM();
		};
	
		if (launch) {
			launchVM();
			fVM = ScrapbookLauncher.getDefault().getDebugTarget(getPage());
		}
	}
	
	public void setPackage(String packageName) {
		fPackageName= packageName;
		// persist
		try {
			getPage().setPersistentProperty(new QualifiedName(JavaPlugin.getPluginId(), PACKAGE_CONTEXT), packageName);
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), JavaPlugin.getResourceString(ERROR + "packagecontext"), null, e.getStatus());
		}
	}
	
	public String getPackage() {
		return fPackageName;
	}
			
	protected IEvaluationContext getEvaluationContext() {
		if (fEvaluationContext == null) {
			IJavaProject project= getJavaProject();
			fEvaluationContext= project.newEvaluationContext();
		}
		if (fPackageName != null) {		
			fEvaluationContext.setPackageName(fPackageName);
		}
		return fEvaluationContext;
	}
	
	public IJavaProject getJavaProject() {
		if (fJavaProject == null) {
			try {
				fJavaProject = findJavaProject();
			} catch (JavaModelException e) {
				showError(e.getStatus());
			}
		}
		return fJavaProject;
	}
	
	public void shutDownVM() {
		DebugPlugin.getDefault().removeDebugEventListener(this);

		// The real shut down
		IDebugTarget target= fVM;
		if (fVM != null) {
			try {
				IMarker bp = ScrapbookLauncher.getDefault().getMagicBreakpoint(fVM);
				if (bp != null) {
					fVM.breakpointRemoved(bp, null);
				}
				if (getThread() != null) {
					getThread().resume();
				}
				fVM.terminate();
			} catch (DebugException e) {
				ErrorDialog.openError(getShell(), JavaPlugin.getResourceString(ERROR + "cantshutdown"), null, e.getStatus());
				return;
			}
			vmTerminated();
			ScrapbookLauncher.getDefault().cleanup(target);
		}
	}
	
	/**
	 * The VM has termianted, update state
	 */
	protected void vmTerminated() {
		DebugPlugin.getDefault().getLaunchManager().deregisterLaunch(fVM.getLaunch());
		fVM= null;
		fThread= null;
		fEvaluationContext = null;
		fLaunchedClassPath = null;
		fireEvalStateChanged();
	}
	
	public void addSnippetStateChangedListener(ISnippetStateChangedListener listener) {
		if (!fSnippetStateListeners.contains(listener))
			fSnippetStateListeners.add(listener);
	}
	
	public void removeSnippetStateChangedListener(ISnippetStateChangedListener listener) {
		if (fSnippetStateListeners != null)
			fSnippetStateListeners.remove(listener);
	}

	public void fireEvalStateChanged() {
		Runnable r= new Runnable() {
			public void run() {			
				List v= new ArrayList(fSnippetStateListeners);
				for (int i= 0; i < v.size(); i++) {
					ISnippetStateChangedListener l= (ISnippetStateChangedListener) v.get(i);
					l.snippetStateChanged(JavaSnippetEditor.this);
				}
			}
		};
		getShell().getDisplay().asyncExec(r);
	}
	
	void evaluate(final String snippet) {
		if (fAttempts < 200 && getThread() == null) {
			// wait for our main thread to suspend
			fAttempts++;
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
			Runnable r = new Runnable() {
				public void run() {
					evaluate(snippet);
				}
			};
			getShell().getDisplay().asyncExec(r);
			return;
		}
		if (getThread() == null) {
			IStatus status = new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaUIStatus.INTERNAL_ERROR, 
				JavaPlugin.getResourceString(ERROR + "noexecutioncontext"), null);
			ErrorDialog.openError(getShell(), JavaPlugin.getResourceString(ERROR + "problemseval"), null, status);
			evaluationEnds();
			return;
		}
		try {
			getThread().evaluate(snippet, JavaSnippetEditor.this, getEvaluationContext());
		} catch (DebugException e) {
			ErrorDialog.openError(getShell(), JavaPlugin.getResourceString(ERROR + "problemseval"), null, e.getStatus());
			evaluationEnds();
		}
	}

	public void evaluationComplete(final IJavaEvaluationResult result) {
		Runnable r = new Runnable() {
			public void run() {
				if (result.hasProblems()) {
					IMarker[] problems = result.getProblems();
					int count= problems.length;
					if (count == 0) {
						showException(result.getException());
					} else {
						for (int i = 0; i < count; i++) {
							showProblem(problems[i]);
						}
					}
				} else {
					final IJavaValue value = result.getValue();
					if (value != null) {
						switch (fResultMode) {
						case RESULT_DISPLAY:
							Runnable r = new Runnable() {
								public void run() {
									displayResult(value);
								}
							};
							getSite().getShell().getDisplay().asyncExec(r);
							break;
						case RESULT_INSPECT:
							String snippet = result.getSnippet().trim();
							int snippetLength = snippet.length();
							if (snippetLength > 30) {
								snippet = snippet.substring(0, 15) + "..." + snippet.substring(snippetLength - 15, snippetLength); 
							}
							snippet = snippet.replace('\n', ' ');
							snippet = snippet.replace('\r', ' ');
							snippet = snippet.replace('\t', ' ');
							DebugUITools.inspect(snippet, value);
							break;
						case RESULT_RUN:
							// no action
							break;
						}
					}
				}
				evaluationEnds();
			}
		};
		Control control= getVerticalRuler().getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(r);
		}
	}
	
	public void codeComplete(ResultCollector collector) throws JavaModelException {
		IDocument d= getSourceViewer().getDocument();
		ITextSelection selection= (ITextSelection)getSelectionProvider().getSelection();
		int start= selection.getOffset();
		String snippet= d.get();	
		IEvaluationContext e= getEvaluationContext();
		if (e != null) 
			e.codeComplete(snippet, start, collector);
	}
		 
	public IJavaElement[] codeResolve() throws JavaModelException {
		IDocument d= getSourceViewer().getDocument();
		ITextSelection selection= (ITextSelection) getSelectionProvider().getSelection();
		int start= selection.getOffset();
		int len= selection.getLength();
		
		String snippet= d.get();	
		IEvaluationContext e= getEvaluationContext();
		if (e != null) 
			return e.codeSelect(snippet, start, len);
		return null;
	}	
	public void showError(IStatus status) {
		evaluationEnds();
		if (!status.isOK())
			ErrorDialog.openError(getShell(), JavaPlugin.getResourceString(ERROR + "erroreval"), null, status);
	}
	
	public void displayResult(IJavaValue result) {
		StringBuffer resultString= new StringBuffer();
		try {
			String sig= result.getSignature();
			if ("V".equals(sig)) {
				resultString.append(' ');
				resultString.append(JavaPlugin.getResourceString(ERROR + "noreturn"));
			} else {
				if (sig != null) {
					resultString.append(" (");
					resultString.append(result.getReferenceTypeName());
					resultString.append(") ");
				} else {
					resultString.append(' ');
				}   
				resultString.append(result.evaluateToString());
			}
		} catch(DebugException e) {
			ErrorDialog.openError(getShell(), JavaPlugin.getResourceString(ERROR + "evaltostring"), null, e.getStatus());
		}
			
		try {
			getSourceViewer().getDocument().replace(fSnippetEnd, 0, resultString.toString());
		} catch (BadLocationException e) {
		}
		
		selectAndReveal(fSnippetEnd, resultString.length());
	}
	
	protected void showProblem(IMarker problem) {
		int estart= MarkerUtilities.getCharStart(problem)+fSnippetStart;
		String message= JavaPlugin.getResourceString(ERROR + "unqualified");
		message= problem.getAttribute(IMarker.MESSAGE, message);
		try {
			getSourceViewer().getDocument().replace(estart, 0, message);
		} catch (BadLocationException e) {
		}
		selectAndReveal(estart, message.length());
	}
	
	protected void showException(Throwable exception) {
		if (exception instanceof DebugException) {
			DebugException de = (DebugException)exception;
			Throwable t= de.getStatus().getException();
			if (t != null) {
				// show underlying exception
				showUnderlyingException(t);
				return;
			}
		}
		ByteArrayOutputStream bos= new ByteArrayOutputStream();
		PrintStream ps= new java.io.PrintStream(bos, true);
		exception.printStackTrace(ps);
		try {
			getSourceViewer().getDocument().replace(fSnippetEnd, 0, bos.toString());
		} catch (BadLocationException e) {
		}
		selectAndReveal(fSnippetEnd, bos.size());
	}
	
	protected void showUnderlyingException(Throwable t) {
		if (t instanceof com.sun.jdi.InvocationException) {
			InvocationException ie= (InvocationException)t;
			ObjectReference ref= ie.exception();
			String eName= ref.referenceType().name();
			String message = getResourceString(ERROR + "exceptioneval") + eName;
			try {
				getSourceViewer().getDocument().replace(fSnippetEnd, 0, message);
			} catch (BadLocationException e) {
			}
			selectAndReveal(fSnippetEnd, message.length());
		} else {
			showException(t);
		}
	}
	
	IJavaProject findJavaProject() throws JavaModelException {
		Object input= getEditorInput();
		if (input instanceof IFileEditorInput) {
			IFileEditorInput file= (IFileEditorInput)input;
			IProject p= file.getFile().getProject();
			return JavaCore.create(p);
		}
		Assert.isTrue(false, "no Java project found for snippet");
		return null;
	}
		
	boolean classPathHasChanged() {
		String[] classpath= getClassPath(getJavaProject());
		if (fLaunchedClassPath != null && !classPathsEqual(fLaunchedClassPath, classpath)) {
			MessageDialog.openError(getShell(), JavaPlugin.getResourceString(ERROR + "warningdialogtitle"), JavaPlugin.getResourceString(ERROR + "cpchanged"));
			return true;
		}
		return false;
	}
	
	boolean classPathsEqual(String[] path1, String[] path2) {
		if (path1.length != path2.length)
			return false;
		for (int i= 0; i < path1.length; i++) {
			if (!path1[i].equals(path2[i]))
				return false;
		}
		return true;
	}
		
	synchronized void evaluationStarts() {
		if (fThread != null) {
			try {
				fThread.resume();
				fThread = null;
			} catch (DebugException e) {
				// XXX: error
			}
		}		
		fEvaluating = true;
		fAttempts = 0;
		fireEvalStateChanged();
		showStatus(JavaPlugin.getResourceString(EVALUATING));
		getSourceViewer().setEditable(false);
	}
	
	void evaluationEnds() {
		fEvaluating= false;
		fireEvalStateChanged();
		showStatus("");
		getSourceViewer().setEditable(true);
	}
	
	void showStatus(String message) {
		IEditorSite site=(IEditorSite)getSite();
		EditorActionBarContributor contributor= (EditorActionBarContributor)site.getActionBarContributor();
		contributor.getActionBars().getStatusLineManager().setMessage(message);
	}
	
	String[] getClassPath(IJavaProject project) {
		try {
			return JavaRuntime.computeDefaultRuntimeClassPath(project);
		} catch (CoreException e) {
			return new String[0];
		}
	}
	
	protected Shell getShell() {
		return getSite().getShell();
	}
	
	public void handleDebugEvent(DebugEvent e) {
		Object source = e.getSource();
		if (source instanceof IDebugElement) {
			IDebugElement de = (IDebugElement)source;
			if (de.getElementType() == IDebugElement.DEBUG_TARGET) {
				if (de.getDebugTarget().equals(fVM)) {
					if (e.getKind() == DebugEvent.TERMINATE) {
						Runnable r = new Runnable() {
							public void run() {
								vmTerminated();
							}
						};
						getShell().getDisplay().asyncExec(r);
					}
				}
			} else if (de instanceof IJavaThread) {
				if (e.getKind() == DebugEvent.SUSPEND) {
					IJavaThread jt = (IJavaThread)de;
					try {
						IJavaStackFrame f= (IJavaStackFrame)jt.getTopStackFrame();
						if (f != null) {
							if (e.getDetail() == DebugEvent.STEP_END && f.getLineNumber() == 9 && f.getDeclaringTypeName().equals("org.eclipse.jdt.internal.ui.snippeteditor.ScrapbookMain1")) {
								fThread = jt;
							} else if (e.getDetail() == DebugEvent.BREAKPOINT && jt.getBreakpoint().equals(ScrapbookLauncher.getDefault().getMagicBreakpoint(jt.getDebugTarget()))) {
								jt.stepOver();
							}
						}
					} catch (DebugException ex) {
						// XXX : error
					}
				}
			}
		}
	}
	
	protected IJavaThread getThread() {
		return fThread;
	}
	
	protected void launchVM() {
		DebugPlugin.getDefault().addDebugEventListener(JavaSnippetEditor.this);
		fLaunchedClassPath = getClassPath(getJavaProject());
		final ILauncher launcher = ScrapbookLauncher.getLauncher();
		Runnable r = new Runnable() {
			public void run() {
				launcher.launch(new Object[] {getPage()}, ILaunchManager.DEBUG_MODE);
			}
		};
		BusyIndicator.showWhile(getShell().getDisplay(), r);
	}
	
	protected IFile getPage() {
		return ((FileEditorInput)getEditorInput()).getFile();
	}
	
	/**
	 * Updates all selection dependent actions.
	 */
	protected void updateSelectionDependentActions() {
		super.updateSelectionDependentActions();
		fireEvalStateChanged();
	}
}