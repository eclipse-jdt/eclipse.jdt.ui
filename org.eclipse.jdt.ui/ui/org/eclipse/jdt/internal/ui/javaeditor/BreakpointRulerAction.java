package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IDebugConstants;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.IVerticalRuler;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerRulerAction;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaDebugConstants;
import org.eclipse.jdt.debug.core.JDIDebugModel;

import org.eclipse.jdt.internal.debug.ui.BreakpointLocationVerifier;
import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 *
 */
public class BreakpointRulerAction extends MarkerRulerAction {	
		
	
	public BreakpointRulerAction(IVerticalRuler ruler, ITextEditor editor) {
		super(JavaEditorMessages.getResourceBundle(), "ManageBreakpoints.", ruler, editor, IDebugConstants.BREAKPOINT_MARKER, false); //$NON-NLS-1$
	}
	
	
	/**
	 * Checks whether the element the breakpoint refers to is shown in this editor
	 */
	protected boolean breakpointElementInEditor(IBreakpointManager manager, IMarker marker) {
		return true;
	}
	
	/**
	 * @see MarkerRulerAction#getMarkers
	 */
	protected List getMarkers() {
		
		List breakpoints= new ArrayList();
		
		IResource resource= getResource();
		IDocument document= getDocument();
		AbstractMarkerAnnotationModel model= getAnnotationModel();
		
		if (model != null) {
			try {
				
				IMarker[] markers= null;
				if (resource instanceof IFile)
					markers= resource.findMarkers(IDebugConstants.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);
				else {
					IWorkspaceRoot root= JavaPlugin.getWorkspace().getRoot();
					//fix for: 1GEUMGZ
					markers= root.findMarkers(IDebugConstants.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);
				}
				
				if (markers != null) {
					IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
					for (int i= 0; i < markers.length; i++) {
						if (breakpointManager.isRegistered(markers[i]) && 
								breakpointElementInEditor(breakpointManager, markers[i]) && 
								includesRulerLine(model.getMarkerPosition(markers[i]), document))
							breakpoints.add(markers[i]);
					}
				}
			} catch (CoreException x) {
				JavaPlugin.logErrorStatus(JavaEditorMessages.getString("BreakpointRulerAction.error.retrieving.message"), x.getStatus()); //$NON-NLS-1$
			}
		}
		return breakpoints;
	}
	
	/**
	 * @see MarkerRulerAction#addMarker
	 */
	protected void addMarker() {
		
		IEditorInput editorInput= getTextEditor().getEditorInput();
		
		IDocument document= getDocument();
		int rulerLine= getVerticalRuler().getLineOfLastMouseButtonActivity();
		IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
		
		try {
			BreakpointLocationVerifier bv = new BreakpointLocationVerifier();
			int lineNumber = bv.getValidBreakpointLocation(document, rulerLine);
			if (lineNumber > 0) {
				
				IRegion line= document.getLineInformation(lineNumber - 1);
				
				IType type = null;
				if (editorInput instanceof ClassFileEditorInput) {
					ClassFileEditorInput input= (ClassFileEditorInput) editorInput;
					type = input.getClassFile().getType();
				} else if (editorInput instanceof IFileEditorInput) {
					IFileEditorInput input= (IFileEditorInput) editorInput;
					ICompilationUnit cu = (ICompilationUnit) JavaCore.create(input.getFile());
					IJavaElement e = cu.getElementAt(line.getOffset());
					if (e instanceof IType) 
						type = (IType)e;
					else if (e != null && e instanceof IMember) {
						type = ((IMember)e).getDeclaringType();
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
			Shell shell= getTextEditor().getSite().getShell();
			ErrorDialog.openError(shell, JavaEditorMessages.getString("BreakpointRulerAction.error.adding.title1"), JavaEditorMessages.getString("BreakpointRulerAction.error.adding.message1"), e.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
		} catch (CoreException e) {
			Shell shell= getTextEditor().getSite().getShell();
			ErrorDialog.openError(shell, JavaEditorMessages.getString("BreakpointRulerAction.error.adding.title2"), JavaEditorMessages.getString("BreakpointRulerAction.error.adding.message2"), e.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
		} catch (BadLocationException e) {
			Shell shell= getTextEditor().getSite().getShell();
			ErrorDialog.openError(shell, JavaEditorMessages.getString("BreakpointRulerAction.error.adding.title3"), JavaEditorMessages.getString("BreakpointRulerAction.error.adding.message3"), null); //$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	/**
	 * @see MarkerRulerAction#removeMarkers
	 */
	protected void removeMarkers(List markers) {
		IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
		try {
			
			Iterator e= markers.iterator();
			while (e.hasNext()) {
				breakpointManager.removeBreakpoint((IMarker) e.next(), true);
			}
			
		} catch (CoreException e) {
			Shell shell= getTextEditor().getSite().getShell();
			ErrorDialog.openError(shell, JavaEditorMessages.getString("BreakpointRulerAction.error.removing.title1"), JavaEditorMessages.getString("BreakpointRulerAction.errro.removing.message1"), e.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
		}
	}
}