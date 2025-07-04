/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

/**
 * The {@link CoreASTProvider} provides access to the {@link CompilationUnit AST root} used by
 * the current active Java editor.
 *
 * The {@link CoreASTProvider} contains all methods/functionality that are
 * not dependent on the UI, from org.eclipse.jdt.internal.ui.javaeditor.ASTProvider
 * for the purpose of reuse by non-UI bundles.
 *
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 * </p>
 *
 * @since 1.10
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class CoreASTProvider {

	private static CoreASTProvider instance = new CoreASTProvider();

	public static final String DEBUG_PREFIX= "ASTProvider > "; //$NON-NLS-1$

	private ITypeRoot fActiveJavaElement;
	private CompilationUnit fAST;
	private ReconcileState fReconcileState= new ReconcileState();

	/**
	 * Wait flag class.
	 */
	public static final class WAIT_FLAG {

		private String fName;

		private WAIT_FLAG(String name) {
			fName= name;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return fName;
		}
	}

	/**
	 * Wait flag indicating that a client requesting an AST
	 * wants to wait until an AST is ready.
	 * <p>
	 * An AST will be created by this AST provider if the shared
	 * AST is not for the given Java element.
	 * </p>
	 */
	public static final WAIT_FLAG WAIT_YES= new WAIT_FLAG("wait yes"); //$NON-NLS-1$

	/**
	 * Wait flag indicating that a client requesting an AST
	 * only wants to wait for the shared AST of the active editor.
	 * <p>
	 * No AST will be created by the AST provider.
	 * </p>
	 */
	public static final WAIT_FLAG WAIT_ACTIVE_ONLY= new WAIT_FLAG("wait active only"); //$NON-NLS-1$

	/**
	 * Wait flag indicating that a client requesting an AST
	 * only wants the already available shared AST.
	 * <p>
	 * No AST will be created by the AST provider.
	 * </p>
	 */
	public static final WAIT_FLAG WAIT_NO= new WAIT_FLAG("don't wait"); //$NON-NLS-1$

	/**
	 * Returns a shared compilation unit AST for the given Java element.
	 * <p>
	 * Clients are not allowed to modify the AST and must synchronize all access to its nodes.
	 * </p>
	 *
	 * @param input the Java element, must not be <code>null</code>
	 * @param waitFlag org.eclipse.jdt.ui.SharedASTProvider#WAIT_YES,
	 * org.eclipse.jdt.ui.SharedASTProvider#WAIT_NO or
	 * org.eclipse.jdt.ui.SharedASTProvider#WAIT_ACTIVE_ONLY
	 * @param progressMonitor the progress monitor or <code>null</code>
	 * @return the AST or <code>null</code> if the AST is not available
	 */
	public CompilationUnit getAST(final ITypeRoot input, WAIT_FLAG waitFlag, IProgressMonitor progressMonitor) {
		if (input == null || waitFlag == null)
			throw new IllegalArgumentException("input or wait flag are null"); //$NON-NLS-1$

		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		boolean isActiveElement;
		synchronized (this) {
			isActiveElement= input.equals(fActiveJavaElement);
			if (isActiveElement) {
				if (fAST != null) {
					if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
						System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "returning cached AST:" + toString(fAST) + " for: " + input.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

					return fAST;
				}
				if (waitFlag == CoreASTProvider.WAIT_NO) {
					if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
						System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "returning null (WAIT_NO) for: " + input.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$

					return null;

				}
			}
		}

		final boolean canReturnNull= waitFlag == CoreASTProvider.WAIT_NO || (waitFlag == CoreASTProvider.WAIT_ACTIVE_ONLY && (!isActiveElement || fAST != null));
		boolean isReconciling= false;
		final ITypeRoot activeElement;
		Runnable finishReconciling;
		if (isActiveElement) {
			synchronized (fReconcileState) {
				activeElement= fReconcileState.fReconcilingJavaElement;
				isReconciling= fReconcileState.isReconciling(input);
				finishReconciling = fReconcileState.fFinishReconciling;
				if (!isReconciling && !canReturnNull)
					aboutToBeReconciled(input);
			}
		} else {
			activeElement= null;
			finishReconciling = null;
		}

		if (isReconciling) {
			try {
				if (finishReconciling!=null) {
					finishReconciling.run();
				}
				// Wait for AST
				synchronized (fReconcileState) {
					long deadline = System.currentTimeMillis()+TimeUnit.SECONDS.toMillis(30);
					if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
						System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "waiting for AST for: " + input.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$
					while (fReconcileState.isReconciling(input)) {
						TimeUnit.SECONDS.timedWait(fReconcileState, 1);
						if (System.currentTimeMillis()> deadline) {
							ILog.get().error("Waited more than 30 seconds for reconcilation to complete! Proceed without waiting for completion!"); //$NON-NLS-1$
						}
					}
				}

				// Check whether active element is still valid
				synchronized (this) {
					if (activeElement == fActiveJavaElement && fAST != null) {
						if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
							System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "...got AST: " + toString(fAST) + " for: " + input.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

						return fAST;
					}
				}
				return getAST(input, waitFlag, progressMonitor);
			} catch (InterruptedException e) {
				return null; // thread has been interrupted don't compute AST
			}
		} else if (canReturnNull)
			return null;


		CompilationUnit ast= null;
		try {
			ast= createAST(input, progressMonitor);
			if (progressMonitor != null && progressMonitor.isCanceled()) {
				ast= null;
				if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
					System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "Ignore created AST for: " + input.getElementName() + " - operation has been cancelled"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		} finally {
			if (isActiveElement) {
				if (fAST != null) {
					// in the meantime, reconcile created a new AST. Return that one
					if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
						System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "Ignore created AST for " + input.getElementName() + " - AST from reconciler is newer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					reconciled(fAST, input, null);
					return fAST;
				} else
					reconciled(ast, input, null);
			}
		}
		return ast;
	}

	/**
	 * Informs that reconciling for the given element is about to be started.
	 *
	 * @param javaElement the Java element
	 * See org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#aboutToBeReconciled()
	 */
	public void aboutToBeReconciled(ITypeRoot javaElement) {
		aboutToBeReconciled(javaElement, null);
	}

	/**
	 * Informs that reconciling for the given element is about to be started.
	 *
	 * @param javaElement the Java element
	 * @param finishReconciling Runnable to be run.
	 * see org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#aboutToBeReconciled(JavaReconciler)
	 * @since 1.16
	 */
	public void aboutToBeReconciled(ITypeRoot javaElement, Runnable finishReconciling) {

		if (javaElement == null)
			return;

		if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
			System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "about to reconcile: " + toString(javaElement)); //$NON-NLS-1$ //$NON-NLS-2$
		synchronized (fReconcileState) {
			fReconcileState.fReconcilingJavaElement= javaElement;
			fReconcileState.fIsReconciling= true;
			fReconcileState.fFinishReconciling = finishReconciling;
			cache(null, javaElement);
			fReconcileState.notifyAll();
		}
	}

	/**
	 * Creates a new compilation unit AST.
	 *
	 * @param input the Java element for which to create the AST
	 * @param progressMonitor the progress monitor
	 * @return AST
	 */
	private static CompilationUnit createAST(final ITypeRoot input, final IProgressMonitor progressMonitor) {
		if (!hasSource(input))
			return null;

		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		final ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(IASTSharedValues.SHARED_AST_STATEMENT_RECOVERY);
		parser.setBindingsRecovery(IASTSharedValues.SHARED_BINDING_RECOVERY);
		parser.setSource(input);

		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		final CompilationUnit root[]= new CompilationUnit[1];

		SafeRunner.run(new ISafeRunnable() {
			@Override
			public void run() {
				try {
					if (progressMonitor != null && progressMonitor.isCanceled())
						return;
					if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
						System.err.println(getThreadName() + " - " + DEBUG_PREFIX + "creating AST for: " + input.getElementName()); //$NON-NLS-1$ //$NON-NLS-2$
					root[0]= (CompilationUnit)parser.createAST(progressMonitor);

					//mark as unmodifiable
					ASTNodes.setFlagsToAST(root[0], ASTNode.PROTECT);
				} catch (OperationCanceledException ex) {
					return;
				}
			}
			@Override
			public void handleException(Throwable ex) {
				IStatus status= new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, IStatus.OK, "Error in JDT Core during AST creation", ex);  //$NON-NLS-1$
				ILog.of(CoreASTProvider.class).log(status);
			}
		});
		return root[0];
	}

	/**
	 * Update internal structures after reconcile.
	 *
	 * @param ast the compilation unit AST or <code>null</code> if the working copy was consistent or
	 *            reconciliation has been cancelled
	 * @param javaElement the Java element for which the AST was built
	 * @param progressMonitor the progress monitor
	 * See org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener#reconciled(CompilationUnit,
	 *      boolean, IProgressMonitor)
	 */
	public void reconciled(CompilationUnit ast, ITypeRoot javaElement, IProgressMonitor progressMonitor) {
		if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
			System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "reconciled: " + toString(javaElement) + ", AST: " + toString(ast)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		synchronized (fReconcileState) {
			fReconcileState.fIsReconciling= false;
			fReconcileState.fFinishReconciling= null;
			if (javaElement == null || !javaElement.equals(fReconcileState.fReconcilingJavaElement)) {

				if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
					System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "  ignoring AST of out-dated editor"); //$NON-NLS-1$ //$NON-NLS-2$

				fReconcileState.notifyAll();

				return;
			}
			cache(ast, javaElement);
		}
	}

	/**
	 * Caches the given compilation unit AST for the given Java element.
	 *
	 * @param ast the ast
	 * @param javaElement the java element
	 */
	public synchronized void cache(CompilationUnit ast, ITypeRoot javaElement) {

		if (fActiveJavaElement != null && !fActiveJavaElement.equals(javaElement)) {
			if (JavaManipulationPlugin.DEBUG_AST_PROVIDER && javaElement != null) // don't report call from disposeAST()
				System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "don't cache AST for inactive: " + toString(javaElement)); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}

		if (JavaManipulationPlugin.DEBUG_AST_PROVIDER && (javaElement != null || ast != null)) // don't report call from disposeAST()
			System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "caching AST: " + toString(ast) + " for: " + toString(javaElement)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (fAST != null)
			disposeAST();

		fAST= ast;
	}

	/**
	 * Checks whether the given Java element has accessible source.
	 *
	 * @param je the Java element to test
	 * @return <code>true</code> if the element has source
	 */
	private static boolean hasSource(ITypeRoot je) {
		if (je == null || !je.exists())
			return false;

		try {
			return je.getBuffer() != null;
		} catch (JavaModelException ex) {
			IStatus status= new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, IStatus.OK, "Error in JDT Core during AST creation", ex);  //$NON-NLS-1$
			ILog.of(CoreASTProvider.class).log(status);
		}
		return false;
	}

	/**
	 * Disposes the cached AST.
	 */
	public synchronized void disposeAST() {

		if (fAST == null)
			return;

		if (JavaManipulationPlugin.DEBUG_AST_PROVIDER)
			System.out.println(getThreadName() + " - " + DEBUG_PREFIX + "disposing AST: " + toString(fAST) + " for: " + toString(fActiveJavaElement)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		fAST= null;

		cache(null, null);
	}

	/**
	 * Returns a string for the given Java element used for debugging.
	 *
	 * @param javaElement the compilation unit AST
	 * @return a string used for debugging
	 */
	public String toString(ITypeRoot javaElement) {
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

		List<AbstractTypeDeclaration> types= ast.types();
		if (types != null && types.size() > 0)
			return types.get(0).getName().getIdentifier() + "(" + ast.hashCode() + ")"; //$NON-NLS-1$//$NON-NLS-2$
		else
			return "AST without any type"; //$NON-NLS-1$
	}

	/**
	 * @return The name of the current thread if not null.
	 * Otherwise this returns the string representation of the
	 * current thread.
	 */
	public static String getThreadName() {
		String name= Thread.currentThread().getName();
		if (name != null)
			return name;
		else
			return Thread.currentThread().toString();
	}

	/**
	 * @return The singleton instance of this class.
	 */
	public static CoreASTProvider getInstance() {
		return instance;
	}

	private CoreASTProvider() {
		// Prevent instantiation.
	}

	/**
	 * @return Whether the current java element is being reconciled.
	 */
	public boolean isReconciling () {
		synchronized (fReconcileState) {
			return fReconcileState.fIsReconciling;
		}
	}

	/**
	 * @return The java element currently being reconciled.
	 */
	public ITypeRoot getReconcilingJavaElement () {
		synchronized (fReconcileState) {
			return fReconcileState.fReconcilingJavaElement;
		}
	}

	/**
	 * @return The active java element.
	 */
	public synchronized ITypeRoot getActiveJavaElement () {
		return fActiveJavaElement;
	}

	/**
	 * Set the active java element that is currently active.
	 * @param activeJavaElement the java element.
	 */
	public synchronized void setActiveJavaElement (ITypeRoot activeJavaElement) {
		fActiveJavaElement = activeJavaElement;
	}

	/**
	 * @return The compilation unit's cached AST.
	 */
	public CompilationUnit getCachedAST () {
		return fAST;
	}

	/**
	 * Notify all waiting threads that the AST has changed.
	 */
	public void waitLockNotifyAll () {
		synchronized (fReconcileState) {
			fReconcileState.notifyAll();
		}
	}

	/**
	 * Clear the reconciliation state.
	 */
	public void clearReconciliation () {
		synchronized (fReconcileState) {
			fReconcileState.fIsReconciling = false;
			fReconcileState.fReconcilingJavaElement = null;
			fReconcileState.fFinishReconciling = null;
			fReconcileState.notifyAll();
		}
	}

	/**
	 * Check if the given java element needs to clear the reconciliation
	 * @since 1.24
	 */
	public void clearReconciliation(ITypeRoot javaElement) {
		synchronized (fReconcileState) {
			if (fReconcileState.fIsReconciling && (fReconcileState.fReconcilingJavaElement == null || !fReconcileState.fReconcilingJavaElement.equals(javaElement))
					|| javaElement == null) {
				clearReconciliation();
			}
		}
	}

	private static final class ReconcileState {
		ITypeRoot fReconcilingJavaElement;
		boolean fIsReconciling;
		Runnable fFinishReconciling;

		/**
		 * Tells whether the given Java element is the one reported as currently being reconciled.
		 *
		 * @param javaElement the Java element
		 * @return <code>true</code> if reported as currently being reconciled
		 */
		private boolean isReconciling(ITypeRoot javaElement) {
			return javaElement != null && javaElement.equals(fReconcilingJavaElement) && fIsReconciling;
		}
	}

}
