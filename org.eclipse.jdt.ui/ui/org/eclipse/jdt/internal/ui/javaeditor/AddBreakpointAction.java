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
	
	private JavaEditor fJavaEditor;

	public AddBreakpointAction(JavaEditor editor) {
		super(JavaEditorMessages.getResourceBundle(), "AddBreakpoint.", editor, IDebugConstants.BREAKPOINT_MARKER, false); //$NON-NLS-1$
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
						if (!EditorUtility.isDuplicateLineBreakpoint(JDIDebugModel.getPluginIdentifier(), IJavaDebugConstants.JAVA_LINE_BREAKPOINT, type, lineNumber)) {
							IMarker breakpoint = JDIDebugModel.createLineBreakpoint(type, lineNumber, line.getOffset(), line.getOffset() + line.getLength(), 0);
							DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(breakpoint);
						}
					}
					
				}
			} catch (DebugException e) {
				Shell shell= fJavaEditor.getSite().getShell();
				ErrorDialog.openError(shell, JavaEditorMessages.getString("AddBreakpoint.error.title1"), JavaEditorMessages.getString("AddBreakpoint.error.message1"), e.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
			} catch (CoreException e) {
				Shell shell= fJavaEditor.getSite().getShell();
				ErrorDialog.openError(shell, JavaEditorMessages.getString("AddBreakpoint.error.title2"), JavaEditorMessages.getString("AddBreakpoint.error.message2"), e.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
			} catch (BadLocationException e) {
				Shell shell= fJavaEditor.getSite().getShell();
				ErrorDialog.openError(shell, JavaEditorMessages.getString("AddBreakpoint.error.title3"), JavaEditorMessages.getString("AddBreakpoint.error.message3"), null); //$NON-NLS-2$ //$NON-NLS-1$
			}

		}
		return null;
	}
}