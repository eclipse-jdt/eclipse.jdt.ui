/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener;

/**
 * Provides ASTs for clients with the option to cache it.
 * 
 * @since 3.0
 */
public final class ASTProvider implements IJavaReconcilingListener {

	private static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jdt.ui/debug/ASTProvider"));  //$NON-NLS-1$//$NON-NLS-2$

	
	private class ActivationListener implements IPartListener2, IWindowListener {
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partActivated(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				setActiveEditor(ref.getPart(true));
		}
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partBroughtToTop(IWorkbenchPartReference ref) {
			if (isJavaEditor(ref) && !isActiveEditor(ref))
				setActiveEditor(ref.getPart(true));
		}
		
		/*
		 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
		 */
		public void partClosed(IWorkbenchPartReference ref) {
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
				setActiveEditor(ref.getPart(true));
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
				setActiveEditor(ref.getPart(true));
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
				setActiveEditor(ref.getPart(true));
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
			synchronized (fReconcileListenerLock) {
				return part != null && part == fActiveEditor;
			}
		}
		
		private boolean isJavaEditor(IWorkbenchPartReference ref) {
			if (ref == null)
				return false;
			
			String id= ref.getId();
			return JavaUI.ID_CF_EDITOR.equals(id) || JavaUI.ID_CU_EDITOR.equals(id); 
		}
	}
	
	
	private static final String AST_DISPOSED= "org.eclipse.jdt.internal.ui.astDisposed"; //$NON-NLS-1$
	private static final String DEBUG_PREFIX= "ASTProvider > "; //$NON-NLS-1$
	
	
	private JavaEditor fActiveEditor;
	private JavaEditor fReconcilingEditor;
	private IJavaElement fActiveJavaElement;
	private boolean fActiveIsCUEditor;
	private CompilationUnit fAST;
	private ActivationListener fActivationListener;
	private Object fReconcileListenerLock= new Object();
	private Object fWaitLock= new Object();
	private boolean fIsReconciling;
	private IWorkbench fWorkbench;

	
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
	}
	
	private void setActiveEditor(IWorkbenchPart editor) {
		synchronized (fReconcileListenerLock) {
			if (fActiveIsCUEditor)
				((CompilationUnitEditor)fActiveEditor).removeReconcileListener(this);
			
			fActiveEditor= (JavaEditor)editor;
			fActiveIsCUEditor=  editor instanceof CompilationUnitEditor;
			
			if (fActiveIsCUEditor)
				((CompilationUnitEditor)fActiveEditor).addReconcileListener(this);
		}
	}
	
	boolean isDisposed(CompilationUnit ast) {
		Assert.isNotNull(ast);
		
		return ((Boolean)ast.getProperty(AST_DISPOSED)).booleanValue(); 
	}

	public void aboutToBeReconciled() {
		synchronized (fReconcileListenerLock) {
			fIsReconciling= true;
			fReconcilingEditor= fActiveEditor;
		}
	}
	
	synchronized void disposeAST() {
		
		if (fAST == null)
			return;
		
		if (DEBUG)
			System.out.println(DEBUG_PREFIX + "disposing AST: " + toString(fAST)); //$NON-NLS-1$
		
		fAST.setProperty(AST_DISPOSED, Boolean.TRUE);
		fAST= null;
		
		cache(null, null);
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

	private synchronized void cache(CompilationUnit ast, IJavaElement je) {
		
		if (fAST != null)
			disposeAST();
		
		if (DEBUG && ast != null)
			System.out.println(DEBUG_PREFIX + "caching AST:" + toString(ast)); //$NON-NLS-1$

		fAST= ast;
		fActiveJavaElement= je;
		
		if (fAST != null)
			fAST.setProperty(AST_DISPOSED, Boolean.FALSE);
		
		// Signal arrival of AST
		synchronized (fWaitLock) {
			fWaitLock.notifyAll();
		}
	}
	
	public CompilationUnit getAST(IJavaElement je, boolean wait, boolean cacheAST, IProgressMonitor progressMonitor) {
		Assert.isTrue(je != null && (je.getElementType() == IJavaElement.CLASS_FILE || je.getElementType() == IJavaElement.COMPILATION_UNIT));
		
		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;
		
		boolean inCache= false;
		
		synchronized (this) {
			inCache= je.equals(fActiveJavaElement);
			if (inCache) {
				if (fAST != null || !wait)
					return fAST;
			}
		}
		if (inCache && fIsReconciling) {
			try {
				final IJavaElement activeElement= fActiveJavaElement;
				
				// Wait for AST
				synchronized (fWaitLock) {
					if (DEBUG)
						System.out.println(DEBUG_PREFIX + "waiting for AST for: " + je.getElementName()); //$NON-NLS-1$
					
					fWaitLock.wait();
				}
				
				// Check whether active element is still valid
				synchronized (this) {
					if (activeElement == fActiveJavaElement)
						return fAST;
				}
				return getAST(je, wait, cacheAST, progressMonitor);
			} catch (InterruptedException e) {
			}
		} else if (!wait)
			return null;
		
		CompilationUnit ast= createAST(je, progressMonitor);
		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;
		
		if (DEBUG)
			System.out.println(DEBUG_PREFIX + "created AST for: " + je.getElementName()); //$NON-NLS-1$
		
		if (cacheAST && ast != null)
			cache(ast, je);
		
		return ast;
	}

	/**
	 * @param je
	 * @param progressMonitor
	 * @return
	 * @exception IllegalStateException if the settings provided
	 * are insufficient, contradictory, or otherwise unsupported
	 */
	private CompilationUnit createAST(IJavaElement je, IProgressMonitor progressMonitor) {
		ASTParser parser = ASTParser.newParser(AST.LEVEL_2_0);
		parser.setResolveBindings(true);
		
		if (je.getElementType() == IJavaElement.COMPILATION_UNIT)
			parser.setSource((ICompilationUnit)je);
		else if (je.getElementType() == IJavaElement.CLASS_FILE)
			parser.setSource((IClassFile)je);
		
		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		return (CompilationUnit)parser.createAST(progressMonitor);
	}
	
	/**
	 * Dispose this AST provider.
	 */
	public void dispose() {

		// Dispose activation listener
		fWorkbench.removeWindowListener(fActivationListener);
		fActivationListener= null;
		
		synchronized (fReconcileListenerLock) {
			fActiveEditor= null;
		}
		
		fActiveIsCUEditor= false;
		
		disposeAST();
		
		synchronized (fWaitLock) {
			fWaitLock.notify();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#reconciled(org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	public void reconciled(CompilationUnit ast) {
		
		if (DEBUG)
			System.out.println(DEBUG_PREFIX + "reconciled AST: " + toString(ast)); //$NON-NLS-1$
		
		synchronized (fReconcileListenerLock) {
			fIsReconciling= false;
			if (fActiveEditor != fReconcilingEditor) {
				
				if (DEBUG)
					System.out.println(DEBUG_PREFIX + "  ignoring AST of outdated editor"); //$NON-NLS-1$
				
				return;
			}
			
			if (ast != null && fActiveEditor != null)
				cache(ast, fActiveEditor.getInputJavaElement());
		}
		
	}
}
