/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.text.Assert;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;


/**
 * Provides a shared AST for clients.
 * 
 * @since 3.0
 */
public final class ASTProvider {

	/**
	 * Tells whether this class is in debug mode.
	 * @since 3.0
	 */
	private static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jdt.ui/debug/ASTProvider"));  //$NON-NLS-1$//$NON-NLS-2$

	
	/**
	 * Internal activation listener.
	 * 
	 * @since 3.0
	 */
	private class ActivationListener implements IPartListener2, IWindowListener {

		
		/*
		 * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partActivated(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partBroughtToTop(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partClosed(IWorkbenchPartReference ref) {
			if (isActiveEditor(ref))
				activeJavaEditorChanged(null);
		}
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partDeactivated(IWorkbenchPartReference ref) {
		}
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partOpened(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partHidden(IWorkbenchPartReference ref) {
		}
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partVisible(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partInputChanged(IWorkbenchPartReference ref) {
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
		 */
		public void windowActivated(IWorkbenchWindow window) {
			IWorkbenchPartReference ref= window.getPartService().getActivePartReference();
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				activeJavaEditorChanged(ref.getPart(true));
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
		 */
		public void windowDeactivated(IWorkbenchWindow window) {
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
		 */
		public void windowClosed(IWorkbenchWindow window) {
			if (fActiveEditor != null && fActiveEditor.getSite() != null && window == fActiveEditor.getSite().getWorkbenchWindow()) 
				activeJavaEditorChanged(null);
			window.getPartService().removePartListener(this);
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
		 */
		public void windowOpened(IWorkbenchWindow window) {
			window.getPartService().addPartListener(this);
		}
		
		private boolean isActiveEditor(IWorkbenchPartReference ref) {
			return ref != null && isActiveEditor(ref.getPart(false));
		}
		
		private boolean isActiveEditor(IWorkbenchPart part) {
			return part != null && (part == fActiveEditor);
		}
		
		private boolean isJavaEditor(IWorkbenchPartReference ref) {
			if (ref == null)
				return false;
			
			String id= ref.getId();
			return JavaUI.ID_CF_EDITOR.equals(id) || JavaUI.ID_CU_EDITOR.equals(id); 
		}
	}
	
	public static final int AST_LEVEL= AST.JLS2;
	
	private static final String AST_DISPOSED= "org.eclipse.jdt.internal.ui.astDisposed"; //$NON-NLS-1$
	private static final String DEBUG_PREFIX= "ASTProvider > "; //$NON-NLS-1$
	
	
	private IJavaElement fReconcilingJavaElement;
	private IJavaElement fActiveJavaElement;
	private CompilationUnit fAST;
	private ActivationListener fActivationListener;
	private Object fReconcileLock= new Object();
	private Object fWaitLock= new Object();
	private boolean fIsReconciling;
	private IWorkbench fWorkbench;
	private IWorkbenchPart fActiveEditor;
	
	/**
	 * Creates a new AST provider. 
	 */
	public ASTProvider() {
		install();
	}
	
	/**
	 * Installs this AST provider.
	 */
	void install() {
		// Create and register activation listener
		fActivationListener= new ActivationListener();
		
		/*
		 * XXX: Don't in-line this field unless the following bug has been fixed:
		 *      https://bugs.eclipse.org/bugs/show_bug.cgi?id=55246
		 */
		fWorkbench= PlatformUI.getWorkbench();
		
		fWorkbench.addWindowListener(fActivationListener);
		
		// Ensure existing windows get connected
		IWorkbenchWindow[] windows= fWorkbench.getWorkbenchWindows();
		for (int i= 0, length= windows.length; i < length; i++)
			windows[i].getPartService().addPartListener(fActivationListener);
	}
	
	private void activeJavaEditorChanged(IWorkbenchPart editor) {
		fActiveEditor= editor;

		IJavaElement javaElement= null;
		if (editor != null)
			javaElement= ((JavaEditor)editor).getInputJavaElement();
		
		if (DEBUG)
			System.out.println(DEBUG_PREFIX + "active editor is: " + toString(javaElement)); //$NON-NLS-1$

		synchronized (fReconcileLock) {
			if (fIsReconciling && !fReconcilingJavaElement.equals(javaElement)) {
				fIsReconciling= false;
				fReconcilingJavaElement= null;
			}
		}

		cache(null, javaElement);
	}

	/**
	 * Returns whether the given compilation unit AST is
	 * cached by this AST provided.
	 * 
	 * @param ast the compilation unit AST
	 * @return <code>true</code if the given AST is the cached one
	 */
	public boolean isCached(CompilationUnit ast) {
		return ast != null && fAST == ast; 
	}

	/**
	 * Informs that reconciling for the given element is about to be started.
	 * 
	 * @param javaElement the Java element
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#aboutToBeReconciled()
	 */
	void aboutToBeReconciled(IJavaElement javaElement) {

		if (DEBUG)
			System.out.println(DEBUG_PREFIX + "about to reconcile: " + toString(javaElement)); //$NON-NLS-1$

		if (javaElement == null)
			return;
		
		synchronized (fReconcileLock) {
			fIsReconciling= true;
			fReconcilingJavaElement= javaElement;
		}
		cache(null, javaElement);
	}
	
	/**
	 * Disposes the cached AST.
	 */
	private synchronized void disposeAST() {
		
		if (fAST == null)
			return;
		
		if (DEBUG)
			System.out.println(DEBUG_PREFIX + "disposing AST: " + toString(fAST)); //$NON-NLS-1$
		
		fAST.setProperty(AST_DISPOSED, Boolean.TRUE);
		fAST= null;
		
		cache(null, null);
	}

	/**
	 * Returns a string for the given Java element used for debugging.
	 * 
	 * @param javaElement the compilation unit AST
	 * @return a string used for debugging
	 */
	private String toString(IJavaElement javaElement) {
		if (javaElement == null)
			return "null"; //$NON-NLS-1$
		else
			return javaElement.getElementName();
		
	}
	
	/**
	 * Returns a string for the given AST used for debugging.
	 * 
	 * @param ast the compilation unit AST
	 * @return a string used for debugging
	 */
	private String toString(CompilationUnit ast) {
		if (ast == null)
			return "null"; //$NON-NLS-1$
		
		List types= ast.types();
		if (types != null && types.size() > 0)
			return ((AbstractTypeDeclaration)types.get(0)).getName().getIdentifier();
		else
			return "AST without any type"; //$NON-NLS-1$
	}

	/**
	 * Caches the given compilation unit AST for the given Java element.
	 * 
	 * @param ast
	 * @param javaElement
	 */
	private synchronized void cache(CompilationUnit ast, IJavaElement javaElement) {
		
		if (DEBUG && (javaElement != null || ast != null)) // don't report call from disposeAST()
			System.out.println(DEBUG_PREFIX + "caching AST:" + toString(ast) + " for: " + toString(javaElement)); //$NON-NLS-1$ //$NON-NLS-2$

		if (fAST != null)
			disposeAST();

		fAST= ast;
		
		fActiveJavaElement= javaElement;
		
		// Signal AST change
		synchronized (fWaitLock) {
			fWaitLock.notifyAll();
		}
	}

	/**
	 * Returns a shared compilation unit AST for the given
	 * Java element.
	 * <p>
	 * Clients are not allowed to modify the AST and must
	 * synchronize all access to its nodes.
	 * </p>
	 * 
	 * @param je				the Java element
	 * @param wait				<code>true</code> if the client wants to wait for the result,
	 * 								<code>null</code> will be returned if the AST is not ready and
	 * 								the client does not want to wait
	 * @param progressMonitor	the progress monitor
	 * @return					the AST or <code>null</code> if the AST is not available
	 */
	public CompilationUnit getAST(IJavaElement je, boolean wait, IProgressMonitor progressMonitor) {
		Assert.isTrue(je != null && (je.getElementType() == IJavaElement.CLASS_FILE || je.getElementType() == IJavaElement.COMPILATION_UNIT));
		
		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;
		
		synchronized (this) {
			if (je.equals(fActiveJavaElement)) {
				if (fAST != null || !wait)
					return fAST;
			}
		}
		if (isReconciling(je)) {
			try {
				final IJavaElement activeElement= fReconcilingJavaElement;
				
				// Wait for AST
				synchronized (fWaitLock) {
					if (DEBUG)
						System.out.println(DEBUG_PREFIX + "waiting for AST for: " + je.getElementName()); //$NON-NLS-1$
					
					fWaitLock.wait();
				}
				
				// Check whether active element is still valid
				synchronized (this) {
					if (activeElement == fActiveJavaElement) {
						if (DEBUG)
							System.out.println(DEBUG_PREFIX + "...got AST for: " + je.getElementName()); //$NON-NLS-1$
						
						return fAST;
					}
				}
				return getAST(je, wait, progressMonitor);
			} catch (InterruptedException e) {
			}
		} else if (!wait)
			return null;
		
		CompilationUnit ast= createAST(je, progressMonitor);
		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;
		
		if (DEBUG)
			System.out.println(DEBUG_PREFIX + "created AST for: " + je.getElementName()); //$NON-NLS-1$

		if (ast != null && je.equals(fActiveJavaElement))
			cache(ast, je);
		
		return ast;
	}

	/**
	 * Tells whether a reconciler is reconciling the
	 * given Java element.
	 * 
	 * @param javaElement the Java element
	 * @return
	 */
	private boolean isReconciling(IJavaElement javaElement) {
		synchronized (fReconcileLock) {
			return javaElement.equals(fReconcilingJavaElement) && fIsReconciling;		
		}
	}
	
	/**
	 * Creates a new compilation unit AST.
	 * 
	 * @param je the Java element for which to create the AST
	 * @param progressMonitor the progress monitor
	 * @return
	 * @throws	IllegalStateException if the settings provided are
	 * 					insufficient, contradictory, or otherwise unsupported
	 */
	private CompilationUnit createAST(IJavaElement je, IProgressMonitor progressMonitor) {
		ASTParser parser = ASTParser.newParser(AST_LEVEL);
		parser.setResolveBindings(true);
		
		if (je.getElementType() == IJavaElement.COMPILATION_UNIT)
			parser.setSource((ICompilationUnit)je);
		else if (je.getElementType() == IJavaElement.CLASS_FILE)
			parser.setSource((IClassFile)je);
		
		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		try {
			CompilationUnit root= (CompilationUnit)parser.createAST(progressMonitor);
			// mark as unmodifiable
			ASTNodes.setFlagsToAST(root, ASTNode.PROTECT);

			return root;
		} catch (IllegalStateException ex) {
			return null;
		}
	}
		
	/**
	 * Disposes this AST provider.
	 */
	public void dispose() {

		// Dispose activation listener
		fWorkbench.removeWindowListener(fActivationListener);
		fActivationListener= null;
		
		disposeAST();
		
		synchronized (fWaitLock) {
			fWaitLock.notify();
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#reconciled(org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	void reconciled(CompilationUnit ast, IJavaElement javaElement) {
		
		if (DEBUG)
			System.out.println(DEBUG_PREFIX + "reconciled AST: " + toString(ast)); //$NON-NLS-1$

		synchronized (fReconcileLock) {
		
			fIsReconciling= false;
			if (javaElement == null || !javaElement.equals(fReconcilingJavaElement)) {
				
				if (DEBUG)
					System.out.println(DEBUG_PREFIX + "  ignoring AST of out-dated editor"); //$NON-NLS-1$

				// Signal - threads might wait for wrong element
				synchronized (fWaitLock) {
					fWaitLock.notifyAll();
				}

				return;
			}
			
			cache(ast, javaElement);
		}
	}
}
