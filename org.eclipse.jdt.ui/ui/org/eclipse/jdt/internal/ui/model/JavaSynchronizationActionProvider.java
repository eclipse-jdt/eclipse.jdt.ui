/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.team.core.mapping.IResourceMappingScope;
import org.eclipse.team.core.mapping.ISynchronizationContext;

import org.eclipse.team.ui.mapping.MergeActionHandler;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;

/**
 * Java-aware synchronization action provider which provides actions for
 * incoming refactorings.
 * 
 * @since 3.2
 */
public final class JavaSynchronizationActionProvider extends SynchronizationActionProvider {

	/** Delegate for refactoring action handlers */
	private final class RefactoringHandlerDelegate extends AbstractHandler {

		/** The delegate handler */
		private final IHandler fDelegateHandler;

		/**
		 * Creates a new synchronization handler delegate.
		 * 
		 * @param handler
		 *            the delegate handler
		 */
		public RefactoringHandlerDelegate(final IHandler handler) {
			Assert.isNotNull(handler);
			fDelegateHandler= handler;
		}

		/**
		 * {@inheritDoc}
		 */
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			return fDelegateHandler.execute(event);
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isEnabled() {
			return !hasRefactorings(getSynchronizationContext(), getSynchronizePageConfiguration()) && fDelegateHandler.isEnabled();
		}
	}

	/**
	 * Returns the currently selected refactorings.
	 * 
	 * @param context
	 *            the synchronization context
	 * @param configuration
	 *            the synchronize page configuration
	 * @return the selected refactorings, or the empty array
	 */
	public static RefactoringDescriptorProxy[] getRefactorings(final ISynchronizationContext context, final ISynchronizePageConfiguration configuration) {
		Assert.isNotNull(context);
		Assert.isNotNull(configuration);
		final Set set= new HashSet();
		final ISelection selection= configuration.getSite().getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structured= (IStructuredSelection) selection;
			if (!structured.isEmpty()) {
				final Object[] elements= structured.toArray();
				final IResourceMappingScope scope= context.getScope();
				for (int index= 0; index < elements.length; index++) {
					if (elements[index] instanceof RefactoringHistory) {
						final RefactoringHistory history= (RefactoringHistory) elements[index];
						final IResource resource= JavaModelProvider.getResource(history);
						if (resource != null && scope.contains(resource))
							set.addAll(Arrays.asList(history.getDescriptors()));
					} else if (elements[index] instanceof RefactoringDescriptorProxy) {
						final RefactoringDescriptorProxy proxy= (RefactoringDescriptorProxy) elements[index];
						final IResource resource= JavaModelProvider.getResource(proxy);
						if (resource != null && scope.contains(resource))
							set.add(elements[index]);
					}
				}
			}
		}
		return (RefactoringDescriptorProxy[]) set.toArray(new RefactoringDescriptorProxy[set.size()]);
	}

	/**
	 * Returns whether any refactorings from the given synchronization context
	 * are selected.
	 * 
	 * @param context
	 *            the synchronization context
	 * @param configuration
	 *            the synchronize page configuration
	 * @return <code>true</code> if any refactorings are selected,
	 *         <code>false</code> otherwise
	 */
	public static boolean hasRefactorings(final ISynchronizationContext context, final ISynchronizePageConfiguration configuration) {
		Assert.isNotNull(context);
		Assert.isNotNull(configuration);
		final ISelection selection= configuration.getSite().getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structured= (IStructuredSelection) selection;
			if (!structured.isEmpty()) {
				final Object[] elements= structured.toArray();
				final IResourceMappingScope scope= context.getScope();
				for (int index= 0; index < elements.length; index++) {
					if (elements[index] instanceof RefactoringHistory) {
						final RefactoringHistory history= (RefactoringHistory) elements[index];
						final IResource resource= JavaModelProvider.getResource(history);
						if (resource != null)
							return scope.contains(resource);
					} else if (elements[index] instanceof RefactoringDescriptorProxy) {
						final RefactoringDescriptorProxy proxy= (RefactoringDescriptorProxy) elements[index];
						final IResource resource= JavaModelProvider.getResource(proxy);
						if (resource != null)
							return scope.contains(resource);
					}
				}
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillContextMenu(final IMenuManager menu) {
		super.fillContextMenu(menu);
		menu.add(new AcceptRefactoringsAction(getSynchronizationContext(), getSynchronizePageConfiguration()));
	}

	/**
	 * {@inheritDoc}
	 */
	protected void initialize() {
		super.initialize();
		final ISynchronizePageConfiguration configuration= getSynchronizePageConfiguration();
		registerHandler(MERGE_ACTION_ID, new RefactoringHandlerDelegate(MergeActionHandler.getDefaultHandler(MERGE_ACTION_ID, configuration)));
		registerHandler(OVERWRITE_ACTION_ID, new RefactoringHandlerDelegate(MergeActionHandler.getDefaultHandler(OVERWRITE_ACTION_ID, configuration)));
		registerHandler(MARK_AS_MERGE_ACTION_ID, new RefactoringHandlerDelegate(MergeActionHandler.getDefaultHandler(MARK_AS_MERGE_ACTION_ID, configuration)));
	}
}
