/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.provider.ThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentServiceListener;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.jdt.core.IJavaProject;

public class MockPluginView extends ViewPart implements INavigatorContentServiceListener {

	private static final String VIEWER_ID = "org.eclipse.jdt.tests.ui.model.mockViewer";
	private static final String JAVA_CONTENT_PROVIDER_ID = "org.eclipse.jdt.ui.javaModelContent";

	private CommonViewer fViewer;
	private INavigatorContentExtension fExtension;
	private ISynchronizationContext fContext;

	public MockPluginView() {
		// Nothing to do
	}

	public void createPartControl(Composite parent) {
		fViewer = new CommonViewer(VIEWER_ID, parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		// Only enable the Java model content
		fViewer.getNavigatorContentService().bindExtensions(new String[] { JAVA_CONTENT_PROVIDER_ID }, true);
		fViewer.getNavigatorContentService().getActivationService().activateExtensions(new String[] { JAVA_CONTENT_PROVIDER_ID }, true);
		fViewer.getNavigatorContentService().addListener(this);
		if (fContext != null)
			setInput(fContext);
	}

	public void setFocus() {
		// Nothing to do
	}

	public void setInput(Object input) {
		fViewer.setInput(input);
	}

	public ITreeContentProvider getContentProvider() {
		return (ITreeContentProvider)fViewer.getContentProvider();
	}

	public void onLoad(INavigatorContentExtension anExtension) {
		this.fExtension = anExtension;
		setContext(fContext);
	}

	public void setProject(IJavaProject project) throws CoreException {
		ISynchronizationContext context = createContext(project);
		setContext(context);
	}

	private void setContext(ISynchronizationContext context) {
		if (fContext != context) {
			if (fContext != null) {
				fContext.dispose();
			}
			fContext = context;
		}
		if (fExtension != null) {
			fExtension.getStateModel().setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE, context.getScope());
			fExtension.getStateModel().setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT, context);
		}
		setInput(fContext);
	}

	public void dispose() {
		super.dispose();
		if (fContext != null) {
			fContext.dispose();
		}
	}

	private ISynchronizationContext createContext(IJavaProject project) throws CoreException {
		ResourceDiffTree tree = new ResourceDiffTree();
		SynchronizationScopeManager manager = new SynchronizationScopeManager("Java Model Tests",
				getResourceMappings(project),
				ResourceMappingContext.LOCAL_CONTEXT,
				true);
		manager.initialize(new NullProgressMonitor());
		SynchronizationContext context = new SynchronizationContext(manager, ISynchronizationContext.THREE_WAY, tree) {
			public void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException {
				// Nothing to do
			}
		};
		return context;
	}

	private ResourceMapping[] getResourceMappings(IJavaProject project) {
		ResourceMapping mapping = (ResourceMapping)project.getResource().getAdapter(ResourceMapping.class);
		return new ResourceMapping[] { mapping };
	}

	private IResource getResource(IProject project, String path) {
		if (path.endsWith("/"))
			return project.getFolder(path);
		return project.getFile(path);
	}

	private ResourceDiff createResourceDiff(IProject project, String path, int kind) {
		final IResource resource = getResource(project, path);
		ResourceDiff diff = new ResourceDiff(resource, kind, 0, new FileRevision() {
			public String getName() {
				return resource.getName();
			}
			public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
				return new IStorage() {
					public Object getAdapter(Class adapter) {
						return null;
					}
					public boolean isReadOnly() {
						return true;
					}
					public String getName() {
						return resource.getName();
					}
					public IPath getFullPath() {
						return resource.getFullPath();
					}
					public InputStream getContents() throws CoreException {
						return new ByteArrayInputStream("".getBytes());
					}
				};
			}
			public boolean isPropertyMissing() {
				return false;
			}
			public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
				return this;
			}}, null);
		return diff;
	}

	public void addOutgoingDeletion(IProject project, String path) {
		IDiff diff = createOutgoingDeletion(project, path);
		add(diff);
	}

	private IDiff createOutgoingDeletion(IProject project, String path) {
		ResourceDiff diff= createResourceDiff(project, path, IDiff.REMOVE);
		return new ThreeWayDiff(diff, null);
	}

	public void addIncomingAddition(IProject project, String path){
		IDiff diff = createIncomingAddition(project,path);
		add(diff);
	}

	private IDiff createIncomingAddition(IProject project, String path) {
		ResourceDiff diff= createResourceDiff(project, path, IDiff.ADD);
		return new ThreeWayDiff(null, diff);
	}

	public void addOutgoingChange(IProject project, String path){
		IDiff diff= createOutgoingChange(project,path);
		add(diff);
	}
	
	private IDiff createOutgoingChange(IProject project, String path) {
		ResourceDiff diff= createResourceDiff(project, path, IDiff.CHANGE);
		return new ThreeWayDiff(diff, null);
	}
	
	private void add(IDiff diff) {
		((ResourceDiffTree)fContext.getDiffTree()).add(diff);
	}

}
