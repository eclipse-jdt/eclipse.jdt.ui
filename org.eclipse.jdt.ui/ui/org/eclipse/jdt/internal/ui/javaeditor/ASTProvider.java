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

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Provides ASTs for clients with the option to cache it.
 * 
 * @since 3.0
 */
public final class ASTProvider {

	private static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jdt.ui/debug/ASTProvider"));  //$NON-NLS-1$//$NON-NLS-2$

	
	private static final String AST_DISPOSED= "org.eclipse.jdt.internal.ui.astDisposed"; //$NON-NLS-1$
	private static final String DEBUG_PREFIX= "ASTProvider > "; //$NON-NLS-1$
	
	
	private IJavaElement fReconcilingJavaElement;
	private IJavaElement fActiveJavaElement;
	private CompilationUnit fAST;
	private Object fReconcileLock= new Object();
	private Object fWaitLock= new Object();
	private boolean fIsReconciling;

	
	/**
	 * Creates a new AST provider. 
	 */
	public ASTProvider() {
	}
	
	boolean isDisposed(CompilationUnit ast) {
		Assert.isNotNull(ast);
		
		return ((Boolean)ast.getProperty(AST_DISPOSED)).booleanValue(); 
	}

	void aboutToBeReconciled(IJavaElement javaElement) {
		
		if (DEBUG)
			System.out.println(DEBUG_PREFIX + "about to reconcile: " + toString(javaElement)); //$NON-NLS-1$

		synchronized (fReconcileLock) {
			fIsReconciling= true;
			fReconcilingJavaElement= javaElement;
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
	 * Returns a string for the given Java element used for debugging.
	 * 
	 * @param ast the compilation unit AST
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

	private synchronized void cache(CompilationUnit ast, IJavaElement javaElement) {
		
		if (fAST != null)
			disposeAST();
		
		if (DEBUG && ast != null)
			System.out.println(DEBUG_PREFIX + "caching AST:" + toString(ast)); //$NON-NLS-1$

		fAST= ast;
		fActiveJavaElement= javaElement;
		
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

	private boolean isReconciling(IJavaElement javaElement) {
		synchronized (fReconcileLock) {
			return javaElement.equals(fReconcilingJavaElement) && fIsReconciling;		
		}
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
			if (javaElement != fReconcilingJavaElement) {
				
				if (DEBUG)
					System.out.println(DEBUG_PREFIX + "  ignoring AST of outdated editor"); //$NON-NLS-1$
				
				return;
			}
			
			if (ast != null && javaElement != null)
				cache(ast, javaElement);
		}
		
	}

}
