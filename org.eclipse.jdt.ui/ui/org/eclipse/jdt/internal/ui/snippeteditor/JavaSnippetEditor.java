package org.eclipse.jdt.internal.ui.snippeteditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import java.util.ArrayList;import java.util.List;import java.util.ResourceBundle;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IMarker;import org.eclipse.core.resources.IProject;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IStatus;import org.eclipse.debug.core.DebugEvent;import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.IDebugEventListener;import org.eclipse.debug.core.ILaunchManager;import org.eclipse.debug.core.ILauncher;import org.eclipse.debug.core.model.IDebugElement;import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.ui.DebugUITools;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.eval.IEvaluationContext;import org.eclipse.jdt.debug.core.IJavaEvaluationListener;import org.eclipse.jdt.debug.core.IJavaEvaluationResult;import org.eclipse.jdt.debug.core.IJavaStackFrame;import org.eclipse.jdt.debug.core.IJavaThread;import org.eclipse.jdt.debug.core.IJavaValue;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.text.java.ResultCollector;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jdt.ui.text.JavaTextTools;import org.eclipse.jface.action.Action;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.text.BadLocationException;import org.eclipse.jface.text.IDocument;import org.eclipse.jface.text.ITextSelection;import org.eclipse.jface.text.source.ISourceViewer;import org.eclipse.jface.util.Assert;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.IEditorSite;import org.eclipse.ui.IFileEditorInput;import org.eclipse.ui.part.EditorActionBarContributor;import org.eclipse.ui.part.FileEditorInput;import org.eclipse.ui.texteditor.AbstractTextEditor;import org.eclipse.ui.texteditor.ITextEditorActionConstants;import org.eclipse.ui.texteditor.MarkerUtilities;import org.eclipse.ui.texteditor.TextOperationAction;

/**
 * An editor for Java snippets.
 */
public class JavaSnippetEditor extends AbstractTextEditor implements IDebugEventListener, IJavaEvaluationListener {			

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
	
	private int fSnippetStart;
	private int fSnippetEnd;
	
	/**
	 * Default constructor.
	 */
	public JavaSnippetEditor() {
		super();
		setDocumentProvider(JavaPlugin.getDefault().getSnippetDocumentProvider());
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		setSourceViewerConfiguration(new JavaSnippetViewerConfiguration(textTools, this));		
		fSnippetStateListeners= new ArrayList(1);
		DebugPlugin.getDefault().addDebugEventListener(this);
	}
	
	public void dispose() {
		DebugPlugin.getDefault().removeDebugEventListener(this);
		shutDownVM();
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
		setAction("Display", new DisplayAction(this, "D&isplay"));		
		setAction("Run", new RunAction(this, "R&un"));
		setAction("Inspect", new InspectAction(this, "I&nspect"));
		
		Action a= new StopAction(this, "Restart");
		a.setEnabled(false);
		setAction("Stop", a);

		setAction("RunInPackage", new RunInPackageAction(this, "Run in Package..."));
		setAction("Import", new ImportAction(this, "Import"));
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
		fResultMode= resultMode;
		
		if (classPathHasChanged()) {
			// need to relaunch VM
		};
			
		fVM = ScrapbookLauncher.getDefault().getDebugTarget(getPage());
		if (fVM == null) {
			launchVM();
			fVM = ScrapbookLauncher.getDefault().getDebugTarget(getPage());
		}
		fireEvalStateChanged();

		ITextSelection selection= (ITextSelection) getSelectionProvider().getSelection();
		String snippet= selection.getText();
		fSnippetStart= selection.getOffset();
		fSnippetEnd= fSnippetStart + selection.getLength();
		
		if (snippet.length() == 0)
			return;
		evaluationStarts();
		evaluate(snippet);
	}	
	
	public void setPackage(String packageName) {
		IEvaluationContext ec= getEvaluationContext();
		ec.setPackageName(packageName);
	}
			
	protected IEvaluationContext getEvaluationContext() {
		if (fEvaluationContext == null) {
			IJavaProject project= getJavaProject();
			fEvaluationContext= project.newEvaluationContext();
		}
		return fEvaluationContext;
	}
	
	public IJavaProject getJavaProject() {
		if (fJavaProject == null) {
			try {
				fJavaProject = findJavaProject();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return fJavaProject;
	}
	
	public void shutDownVM() {
		// The real shut down
		if (fVM != null) {
			try {
				fVM.terminate();
			} catch (DebugException e) {
				ErrorDialog.openError(getShell(), "Can't shutdown VM", null, e.getStatus());
				return;
			}
			DebugPlugin.getDefault().getLaunchManager().deregisterLaunch(fVM.getLaunch());
			fVM= null;
			fThread = null;
			fEvaluationContext= null;
			Action action= (Action)getAction("Stop");
			action.setEnabled(false);
			fireEvalStateChanged();
		}
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
		List v= new ArrayList(fSnippetStateListeners);
		for (int i= 0; i < v.size(); i++) {
			ISnippetStateChangedListener l= (ISnippetStateChangedListener) v.get(i);
			l.snippetStateChanged(this);
		}
	}
	
	void evaluate(final String snippet) {
		if (getThread() == null) {
			// repost - wait for our main thread to suspend
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
			Runnable r = new Runnable() {
				public void run() {
					evaluate(snippet);
				}
			};
			getSite().getShell().getDisplay().asyncExec(r);
			return;
		}
		try {
			getThread().evaluate(snippet, this, getEvaluationContext());
		} catch (DebugException e) {
			ErrorDialog.openError(getShell(), "Problems evaluating expression", null, e.getStatus());
		} finally {
			evaluationEnds();
		}
	}

	public void evaluationComplete(final IJavaEvaluationResult result) {
		Runnable r = new Runnable() {
			public void run() {
				evaluationEnds();
				if (result.hasProblems()) {
					IMarker[] problems = result.getProblems();
					for (int i = 0; i < problems.length; i++) {
						showProblem(problems[i]);
					}
					return;
				}
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
						DebugUITools.inspect(result.getSnippet(), value);
						break;
					case RESULT_RUN:
						// no action
						break;
					}
				}
			}
		};
		getVerticalRuler().getControl().getDisplay().asyncExec(r);
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
			ErrorDialog.openError(getShell(), "Error during evaluation", null, status);
	}
	
	public void displayResult(IJavaValue result) {
		String resultString= null;
		try {
			if (result.getSignature() == null) 
				resultString= "(No explicit return value)";
			else 
				resultString= " ("+result.getReferenceTypeName()+") "+result.evaluateToString();
		} catch(DebugException e) {
			ErrorDialog.openError(getShell(), "Problems evaluating toString of expression", null, e.getStatus());
		}
			
		try {
			getSourceViewer().getDocument().replace(fSnippetEnd, 0, resultString);
		} catch (BadLocationException e) {
		}
		
		selectAndReveal(fSnippetEnd, resultString.length());
	}
	
	public void showProblem(IMarker problem) {
		evaluationEnds();
		int estart= MarkerUtilities.getCharStart(problem)+fSnippetStart;
		String message= "Unqualified Error";
		message= problem.getAttribute(IMarker.MESSAGE, message);
		try {
			getSourceViewer().getDocument().replace(estart, 0, message);
		} catch (BadLocationException e) {
		}
		selectAndReveal(estart, message.length());
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
			MessageDialog.openError(getShell(), "Warning", "The class path of the Project has changed. Restarting the evaluation context.");
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
	
	void evaluationStarts() {
		fEvaluating= true;
		fireEvalStateChanged();
		showStatus("Evaluating...");
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
		return getSite().getWorkbenchWindow().getShell();
	}
	
	public void handleDebugEvent(DebugEvent e) {
		Object source = e.getSource();
		if (source instanceof IDebugElement) {
			IDebugElement de = (IDebugElement)source;
			if (de.getDebugTarget().equals(fVM)) {
				if (de instanceof IDebugTarget) {
					if (e.getKind() == DebugEvent.TERMINATE) {
						shutDownVM();
					}
				}
			}
		}
	}
	
	protected IJavaThread getThread() {
		try {
			if (fThread == null) {
				IDebugElement[] threads = fVM.getChildren();
				for (int i = 0; i < threads.length; i++) {
					IJavaThread thread = (IJavaThread)threads[i];
					if (thread.isSuspended() && thread.getChildren().length == 1) {
						IJavaStackFrame frame = (IJavaStackFrame)thread.getTopStackFrame();
						if (frame.getMethodName().equals("main")) {
							fThread = thread;
							break;
						}
					}
				}
			}
		} catch(DebugException e) {
			JavaPlugin.log(e.getStatus());
			return null;
		}
		return fThread;
	}
	
	protected void launchVM() {
		ILauncher launcher = ScrapbookLauncher.getLauncher();
		launcher.launch(new Object[] {getPage()}, ILaunchManager.DEBUG_MODE);
	}
	
	protected IFile getPage() {
		return ((FileEditorInput)getEditorInput()).getFile();
	}
}