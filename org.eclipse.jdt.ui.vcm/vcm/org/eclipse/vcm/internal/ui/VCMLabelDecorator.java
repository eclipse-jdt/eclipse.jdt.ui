/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
package org.eclipse.vcm.internal.ui;

import java.util.ArrayList;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IResourceChangeEvent;import org.eclipse.core.resources.IResourceChangeListener;import org.eclipse.core.resources.IResourceDelta;import org.eclipse.core.resources.IResourceDeltaVisitor;import org.eclipse.core.resources.IResourceVisitor;import org.eclipse.core.resources.ResourcesPlugin;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.jface.util.IPropertyChangeListener;import org.eclipse.jface.util.PropertyChangeEvent;import org.eclipse.jface.viewers.ILabelDecorator;import org.eclipse.jface.viewers.LabelProvider;import org.eclipse.jface.viewers.LabelProviderChangedEvent;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Shell;import org.eclipse.vcm.internal.core.base.IResourceEdition;import org.eclipse.vcm.internal.core.base.ISharingManager;import org.eclipse.vcm.internal.core.base.ISyncInfo;import org.eclipse.vcm.internal.core.base.ITeamStream;import org.eclipse.vcm.internal.core.base.VCMPlugin;import org.eclipse.vcm.internal.ui.IWorkbenchVCMConstants;import org.eclipse.vcm.internal.ui.WorkbenchVCMPlugin;

/**
 * This label decorator adds Version information to resources.
 */
public class VCMLabelDecorator
	extends LabelProvider
	implements ILabelDecorator, IResourceChangeListener {
	//the shell this decorator is in -- needed to know UI thread for updates.
	private Shell shell;

	//a dummy core exception used to short-circuit a resource visit
	private static final CoreException CORE_EXCEPTION=
		new CoreException(new Status(IStatus.OK, WorkbenchVCMPlugin.ID, 1, "", null));

	private String outgoingChangePrefix;

	private static final ISharingManager manager=
		VCMPlugin.getProvider().getSharingManager();

	/**
	 * Creates a new decorator with the given shell.  The shell is
	 * needed for determining the UI display for updates.
	 */
	public VCMLabelDecorator(Shell shell) {
		this.shell= shell;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
			this,
			IResourceChangeEvent.POST_AUTO_BUILD);

		//setup outgoing change prefix
		outgoingChangePrefix=
			WorkbenchVCMPlugin.getDefault().getPreferenceStore().getString(
				IWorkbenchVCMConstants.PREF_OUTGOING_CHANGE_PREFIX);
		WorkbenchVCMPlugin
			.getDefault()
			.getPreferenceStore()
			.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event
					.getProperty()
					.equals(IWorkbenchVCMConstants.PREF_OUTGOING_CHANGE_PREFIX)) {
					outgoingChangePrefix= (String) event.getNewValue();
					fireLabelProviderChanged(
						new LabelProviderChangedEvent(VCMLabelDecorator.this, null));
				}
			}
		});

	}

	/**
	 * @see ILabelDecorator#decorateImage(Image, Object)
	 */
	public Image decorateImage(Image image, Object object) {
		return image;
	}
	/**
	 * @see ILabelDecorator#decorateText(String, Object)
	 */
	public String decorateText(String text, Object o) {
		IResource resource= getResource(o);
		if (resource == null) {
			//don't annotate things we don't know about
			return text;
		}
		StringBuffer result= new StringBuffer();

		//append outgoing state
		if (isOutgoing(resource)) {
			result.append(outgoingChangePrefix);
		}

		//append original text
		result.append(text);

		if (resource.getType() == IResource.FILE) {
			IFile file= (IFile) resource;

			//append base state and dirty state
			IResourceEdition base= manager.getBaseVersion(file);
			if (base != null) {
				boolean dirty= !manager.isClean(file);
				result.append(" ");
				if (dirty)
					result.append("<");
				result.append(base.getVersionName());
				if (dirty)
					result.append(">");
			}
		} else
			if (resource.getType() == IResource.PROJECT) {
				//append stream name
				IProject project= (IProject) resource;
				ITeamStream stream= manager.getSharing(project);
				if (stream != null) {
					result.append(" (");
					result.append(stream.getName());
					result.append(")");
				}
			}
		return result.toString();
	}
	/**
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}
	/**
	 * This method was added to avoid compile errors in VA/J.
	 * VA/J incorrectly doesn't allow inner classes to refer to protected 
	 * members in their containing type's supertypes
	 */
	void fireLabelUpdates(final LabelProviderChangedEvent[] events) {
		for (int i= 0; i < events.length; i++) {
			fireLabelProviderChanged(events[i]);
		}
	}
	/**
	 * Generates label change events for the entire subtree rooted
	 * at the given resource.
	 */
	private void generateEventsForSubtree(
		IResource parent,
		final ArrayList events) {
		try {
			parent.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) {
					events.add(VCMLabelDecorator.this.createLabelEvent(resource));
					return true;
				}
			});
		} catch (CoreException e) {
			//this is never thrown in the above visitor
		}
	}
	/**
	 * Returns the resource for the given input object, or
	 * null if there is no resource associated with it.
	 */
	private IResource getResource(Object object) {
		if (object instanceof IResource) {
			return (IResource) object;
		}
		if (object instanceof IAdaptable) {
			return (IResource) ((IAdaptable) object).getAdapter(IResource.class);
		}
		return null;
	}
	/**
	 * Returns true if the given object is an outgoing change, and
	 * false otherwise.
	 */
	private boolean isOutgoing(Object object) {
		if (object instanceof IResource) {
			//determine if there was an outgoing change to the resource
			//by checking for dirty children.
			try {
				((IResource) object).accept(new IResourceVisitor() {
					public boolean visit(IResource resource) throws CoreException {
						//don't care about unshared things.
						if (manager.getSharing(resource) == null) {
							return false;
						}
						if (!manager.isManaged(resource)) {
							//it could be ignored, in which case it's not an interesting change
							if (manager.getIgnored(new IResource[] { resource })[0]) {
								return false;
							} else {
								//it must be a new resource -- show as a change
								throw CORE_EXCEPTION;
							}
						}
						if (manager.getSyncInfo(resource).getOutgoingChange() != ISyncInfo.NO_CHANGE) {
							//there is a change in some child
							throw CORE_EXCEPTION;
						}
						//no change -- keep looking in children
						return true;
					}
				}, IResource.DEPTH_INFINITE, true);
			} catch (CoreException e) {
				//if our exception was caught, we know there's a dirty child
				return e == CORE_EXCEPTION;
			}
		}
		return false;
	}

	/**
	 * Process a resource delta.  Returns all label provider changed
	 * events that were generated by this delta.
	 */
	protected LabelProviderChangedEvent[] processDelta(IResourceDelta delta) {
		final ArrayList events= new ArrayList();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource= delta.getResource();
					//skip workspace root
					if (resource.getType() == IResource.ROOT) {
						return true;
					}
					//don't care about deletions
					if (delta.getKind() == IResourceDelta.REMOVED) {
						return false;
					}
					//ignore subtrees that aren't shared
					if (manager.getSharing(resource) == null) {
						return false;
					}
					//ignore subtrees that are ignored by VCM
					if (!manager.isManaged(resource)
						&& manager.getIgnored(new IResource[] { resource })[0]) {
						return false;
					}
					//if project sharing info has changed, need to update whole tree
					if (resource.getType() == IResource.PROJECT
						&& ((delta.getFlags() & IResourceDelta.SYNC) != 0)) {
						generateEventsForSubtree(resource, events);
						return false;
					}
					//chances are the VCM outgoing bit needs to be updated
					//if any child has changed.
					events.add(
						VCMLabelDecorator.this.createLabelEvent(delta.getResource()));
					return true;
				}
			});
		} catch (CoreException e) {
			WorkbenchVCMPlugin.log(e.getStatus());
		}
		//convert event list to array
		LabelProviderChangedEvent[] result=
			new LabelProviderChangedEvent[events.size()];
		events.toArray(result);
		return result;
	}
	/**
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		//first collect the label change events
		final LabelProviderChangedEvent[] events= processDelta(event.getDelta());

		//now post the change events to the UI thread
		if (events.length > 0 && shell != null && !shell.isDisposed()) {
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					fireLabelUpdates(events);
				}
			});
		}
	}
	
	/**
 	 * Returns the change event to be fired for updates to the given resource.
 	 */
	protected LabelProviderChangedEvent createLabelEvent(IResource resource) {
		return new LabelProviderChangedEvent(this, resource);
	}


}