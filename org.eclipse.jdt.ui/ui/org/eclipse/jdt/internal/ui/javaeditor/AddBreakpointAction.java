package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ResourceBundle;import org.eclipse.core.resources.IMarker;import org.eclipse.core.runtime.CoreException;import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.IDebugConstants;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.debug.core.IJavaDebugConstants;import org.eclipse.jdt.debug.core.JDIDebugModel;import org.eclipse.jdt.internal.debug.ui.BreakpointLocationVerifier;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.text.BadLocationException;import org.eclipse.jface.text.IDocument;import org.eclipse.jface.text.IRegion;import org.eclipse.jface.text.ITextSelection;import org.eclipse.jface.viewers.ISelection;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.IEditorInput;import org.eclipse.ui.IFileEditorInput;import org.eclipse.ui.texteditor.AddMarkerAction;


/**
 * Action for setting breakpoints for a given text selection.
 */
public class AddBreakpointAction extends AddMarkerAction {
	
	protected static final String ERROR_ADD_BREAKPOINT= "error.add_breakpoint.";
	private JavaEditor fJavaEditor;

	public AddBreakpointAction(ResourceBundle bundle, String prefix, JavaEditor editor) {
		super(bundle, prefix, editor, IDebugConstants.BREAKPOINT_MARKER, false);
		fJavaEditor= editor;
	}
	
	/**
	 * @see Action#actionPerformed
	 */
	public void run() {
		if (fJavaEditor != null) 
			createMarker(fJavaEditor.getEditorInput());
	}
	/**
	 * Creates a breakpoint marker.
	 */
	protected IMarker createMarker(IEditorInput editorInput) {
		ISelection s= fJavaEditor.getSelectionProvider().getSelection();
		if (!s.isEmpty()) {
			ITextSelection selection= (ITextSelection) s;
			try {
				IDocument document= fJavaEditor.getDocumentProvider().getDocument(editorInput);
				BreakpointLocationVerifier bv = new BreakpointLocationVerifier();
				int lineNumber = bv.getValidBreakpointLocation(document, selection.getStartLine());
				if (lineNumber > 0) {
					
					IRegion line= document.getLineInformation(lineNumber - 1);
					
					IType type = null;
					if (editorInput instanceof ClassFileEditorInput) {
						
						ClassFileEditorInput input= (ClassFileEditorInput) editorInput;
						type = input.getClassFile().getType();
					
					} else if (editorInput instanceof IFileEditorInput) {
						
						IFileEditorInput input= (IFileEditorInput) editorInput;
						IJavaElement element= JavaCore.create(input.getFile());
						if (element instanceof ICompilationUnit) {
							ICompilationUnit cu = (ICompilationUnit) element;
							IJavaElement e = cu.getElementAt(line.getOffset());
							if (e instanceof IType)
								type = (IType)e;
							else if (e != null && e instanceof IMember)
								type = ((IMember) e).getDeclaringType();
						}
					}
					if (type != null) {
						if (!EditorUtility.isDuplicateBreakpoint(JDIDebugModel.getPluginIdentifier(), IJavaDebugConstants.JAVA_LINE_BREAKPOINT, type, lineNumber)) {
							IMarker breakpoint = JDIDebugModel.createLineBreakpoint(type, lineNumber, line.getOffset(), line.getOffset() + line.getLength(), 0);
							DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(breakpoint);
						}
					}
					
				}
			} catch (DebugException e) {
				Shell shell= fJavaEditor.getSite().getShell();
				String title= getString(getResourceBundle(), getResourceKeyPrefix()+ERROR_ADD_BREAKPOINT+"title", "Add Breakpoint");
				String msg= getString(getResourceBundle(), getResourceKeyPrefix()+ERROR_ADD_BREAKPOINT+"message", "Cannot add breakpoint");
				ErrorDialog.openError(shell, title, msg, e.getStatus());
			} catch (CoreException e) {
				Shell shell= fJavaEditor.getSite().getShell();
				String title= getString(getResourceBundle(), getResourceKeyPrefix()+ERROR_ADD_BREAKPOINT+"title", "Add Breakpoint");
				String msg= getString(getResourceBundle(), getResourceKeyPrefix()+ERROR_ADD_BREAKPOINT+"message", "Cannot add breakpoint");
				ErrorDialog.openError(shell, title, msg, e.getStatus());
			} catch (BadLocationException e) {
				Shell shell= fJavaEditor.getSite().getShell();
				String title= getString(getResourceBundle(), getResourceKeyPrefix()+ERROR_ADD_BREAKPOINT+"title", "Add Breakpoint");
				String msg= getString(getResourceBundle(), getResourceKeyPrefix()+ERROR_ADD_BREAKPOINT+"message", "Cannot add breakpoint");
				ErrorDialog.openError(shell, title, msg, null);
			}

		}
		return null;
	}
}
