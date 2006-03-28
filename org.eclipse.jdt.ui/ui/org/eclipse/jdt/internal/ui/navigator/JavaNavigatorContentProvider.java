/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.navigator.IExtensionStateConstants.Values;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;

public class JavaNavigatorContentProvider extends
		PackageExplorerContentProvider implements IPipelinedTreeContentProvider {

	public JavaNavigatorContentProvider() {
		super(false);
	}

	public JavaNavigatorContentProvider(boolean provideMembers) {
		super(provideMembers);
	}

	public static final String JDT_EXTENSION_ID = "org.eclipse.jdt.ui.javaContent"; //$NON-NLS-1$ 

	private IExtensionStateModel fStateModel;

	public void init(ICommonContentExtensionSite commonContentExtensionSite) {
		IExtensionStateModel stateModel= commonContentExtensionSite.getExtensionStateModel();
		IMemento memento= commonContentExtensionSite.getMemento();
		
		fStateModel = stateModel;
		// fManager = new WorkingSetModelManager(fStateModel, this);
		// expose the manager for the action provider
		// fStateModel.setProperty(WorkingSetModelManager.INSTANCE_KEY,
		// fManager);
		restoreState(memento);
		fStateModel.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (Values.IS_LAYOUT_FLAT.equals(event.getProperty())) {
					if (event.getNewValue() != null) {
						boolean newValue = ((Boolean) event.getNewValue())
								.booleanValue() ? true : false;
						setIsFlatLayout(newValue);
					}
				}

			}
		});

		IPreferenceStore store = PreferenceConstants.getPreferenceStore();
		boolean showCUChildren = store
				.getBoolean(PreferenceConstants.SHOW_CU_CHILDREN);
		setProvideMembers(showCUChildren);
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		super.inputChanged(viewer, oldInput, findInputElement(newInput));
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot)
			return super.getElements(JavaCore
					.create((IWorkspaceRoot) inputElement));
		return super.getElements(inputElement);
	}

	private Object findInputElement(Object newInput) {
		if (newInput instanceof IWorkspaceRoot) {
			return JavaCore.create((IWorkspaceRoot) newInput);
		}
		return newInput;
	}

	public void restoreState(IMemento memento) {

	}

	public void saveState(IMemento memento) {

	}

	public void getPipelinedChildren(Object parent, Set currentChildren) {
		Object[] children = getChildren(parent);
		for (Iterator iter = currentChildren.iterator(); iter.hasNext();)
			if (iter.next() instanceof IResource)
				iter.remove();
		currentChildren.addAll(Arrays.asList(children));
	}

	public void getPipelinedElements(Object input, Set currentElements) {
		Object[] children = getElements(input);

		for (Iterator iter = currentElements.iterator(); iter.hasNext();)
			if (iter.next() instanceof IResource)
				iter.remove();

		currentElements.addAll(Arrays.asList(children));
	}

	public Object getPipelinedParent(Object object, Object suggestedParent) {
		return getParent(object);
	}

	public PipelinedShapeModification interceptAdd(
			PipelinedShapeModification addModification) {
		// TODO Auto-generated method stub
		return null;
	}

	public PipelinedShapeModification interceptRemove(
			PipelinedShapeModification removeModification) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
		IJavaElement javaElement;
		Set interceptedElements = new HashSet();
		for (Iterator iter = refreshSynchronization.getRefreshTargets().iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof IResource) {
				if ((javaElement = JavaCore.create((IResource) element)) != null && javaElement.exists()) {
					iter.remove();
					interceptedElements.add(javaElement);
				}
			}
		}
		if (interceptedElements.size() > 0) {
			refreshSynchronization.getRefreshTargets().addAll(
					interceptedElements);
			return true;
		}
		return false;

	}

	public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
		// TODO Auto-generated method stub
		return false;
	}

	protected void postAdd(final Object parent, final Object element) {
		if (parent instanceof IJavaModel)
			super.postAdd(((IJavaModel) parent).getWorkspace(), element);
		else
			super.postAdd(parent, element);
	}

}
