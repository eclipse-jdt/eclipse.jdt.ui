/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.ui;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.ui.progress.WorkbenchJob;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.ImageImageDescriptor;

/**
 * LabelDecorator that decorates an method's image with override or implements overlays.
 * The viewer using this decorator is responsible for updating the images on element changes.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OverrideIndicatorLabelDecorator implements ILabelDecorator, ILightweightLabelDecorator {

	/** Refreshes the labels in the UI thread once a build is done. */
	private final class UpdateJob extends WorkbenchJob {

		UpdateJob() {
			super("Java override indicator decoration update"); //$NON-NLS-1$
			setSystem(true);
			setPriority(DECORATE);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			// the next build already started: keep the pending elements, its POST_BUILD reschedules
			if (!fBuildRunning.get()) {
				fireLabelProviderChanged();
			}
			return Status.OK_STATUS;
		}
	}

	private ImageDescriptorRegistry fRegistry;
	private boolean fUseNewRegistry= false;

	private static final int MAX_PENDING_ELEMENTS= 512;

	private ListenerList<ILabelProviderListener> fListeners;
	private IResourceChangeListener fBuildListener;
	private final UpdateJob fUpdateJob= new UpdateJob();

	/**
	 * Whether a build is currently running. A flag rather than a nesting count: a PRE_BUILD whose
	 * POST_BUILD never arrives would leave a count permanently above zero and suppress every
	 * indicator from then on, while a flag is cleared again by the next build.
	 */
	private final AtomicBoolean fBuildRunning= new AtomicBoolean();

	/** Elements whose indicator was not computed because a build was running. */
	private final Set<Object> fPendingElements= ConcurrentHashMap.newKeySet();

	/** Set when there were too many pending elements to track them individually. */
	private final AtomicBoolean fRefreshAll= new AtomicBoolean();

	/**
	 * Creates a decorator. The decorator creates an own image registry to cache
	 * images.
	 */
	public OverrideIndicatorLabelDecorator() {
		this(null);
		fUseNewRegistry= true;
	}

	/*
	 * Creates decorator with a shared image registry.
	 *
	 * @param registry The registry to use or <code>null</code> to use the Java plugin's
	 * image registry.
	 */
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 *
	 * @param registry The registry to use.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public OverrideIndicatorLabelDecorator(ImageDescriptorRegistry registry) {
		fRegistry= registry;
	}

	private ImageDescriptorRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry= fUseNewRegistry ? new ImageDescriptorRegistry() : JavaPlugin.getImageDescriptorRegistry();
		}
		return fRegistry;
	}


	@Override
	public String decorateText(String text, Object element) {
		return text;
	}

	@Override
	public Image decorateImage(Image image, Object element) {
		if (image == null)
			return null;

		int adornmentFlags= computeAdornmentFlags(element);
		if (adornmentFlags != 0) {
			ImageDescriptor baseImage= new ImageImageDescriptor(image);
			Rectangle bounds= image.getBounds();
			return getRegistry().get(new JavaElementImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width, bounds.height)));
		}
		return image;
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param element The element to decorate
	 * @return Resulting decorations (combination of JavaElementImageDescriptor.IMPLEMENTS
	 * and JavaElementImageDescriptor.OVERRIDES)
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public int computeAdornmentFlags(Object element) {
		return JavaCore.callReadOnly(() -> {
			if (element instanceof IMethod) {
				try {
					IMethod method= (IMethod) element;
					if (!method.getJavaProject().isOnClasspath(method)) {
						return 0;
					}
					int flags= method.getFlags();
					if (!method.isConstructor() && !Flags.isPrivate(flags) && !Flags.isStatic(flags)) {
						int res= getOverrideIndicators(method);
						if (res != 0 && Flags.isSynchronized(flags)) {
							return res | JavaElementImageDescriptor.SYNCHRONIZED;
						}
						return res;
					}
				} catch (JavaModelException e) {
					if (!e.isDoesNotExist()) {
						JavaPlugin.log(e);
					}
				}
			}
			return 0;
		});
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 *
	 * @param method The element to decorate
	 * @return Resulting decorations (combination of JavaElementImageDescriptor.IMPLEMENTS and
	 *         JavaElementImageDescriptor.OVERRIDES)
	 * @throws JavaModelException if accessing a Java Model element fails
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected int getOverrideIndicators(IMethod method) throws JavaModelException {
		CompilationUnit astRoot= SharedASTProviderCore.getAST(method.getTypeRoot(), SharedASTProviderCore.WAIT_ACTIVE_ONLY, null);
		if (astRoot != null) {
			int res= findInHierarchyWithAST(astRoot, method);
			if (res != -1) {
				return res;
			}
		}

		IType type= method.getDeclaringType();

		if (fBuildRunning.get() && !SuperTypeHierarchyCache.hasInCache(type)) {
			// a running build invalidates the cache continuously, so a hierarchy built here is wasted
			recordSkippedElement(method);
			return 0;
		}

		MethodOverrideTester methodOverrideTester= SuperTypeHierarchyCache.getMethodOverrideTester(type);
		IMethod defining= methodOverrideTester.findOverriddenMethod(method, true);
		if (defining != null) {
			if (JdtFlags.isAbstract(defining)) {
				return JavaElementImageDescriptor.IMPLEMENTS;
			} else {
				return JavaElementImageDescriptor.OVERRIDES;
			}
		}
		return 0;
	}

	private int findInHierarchyWithAST(CompilationUnit astRoot, IMethod method) throws JavaModelException {
		ASTNode node= NodeFinder.perform(astRoot, method.getNameRange());
		if (node instanceof SimpleName && node.getParent() instanceof MethodDeclaration) {
			IMethodBinding binding= ((MethodDeclaration) node.getParent()).resolveBinding();
			if (binding != null) {
				IMethodBinding defining= Bindings.findOverriddenMethod(binding, true);
				if (defining != null) {
					if (JdtFlags.isAbstract(defining)) {
						return JavaElementImageDescriptor.IMPLEMENTS;
					} else {
						return JavaElementImageDescriptor.OVERRIDES;
					}
				}
				return 0;
			}
		}
		return -1;
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 *
	 * @param type The declaring type of the method to decorate.
	 * @param hierarchy The type hierarchy of the declaring type.
	 * @param name The name of the method to find.
	 * @param paramTypes The parameter types of the method to find.
	 * @return The resulting decoration.
	 * @throws JavaModelException if accessing a Java Model element fails
	 * @deprecated Not used anymore. This method is not accurate for methods in generic types.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@Deprecated
	protected int findInHierarchy(IType type, ITypeHierarchy hierarchy, String name, String[] paramTypes) throws JavaModelException {
		IType superClass= hierarchy.getSuperclass(type);
		if (superClass != null) {
			IMethod res= JavaModelUtil.findMethodInHierarchy(hierarchy, superClass, name, paramTypes, false);
			if (res != null && !Flags.isPrivate(res.getFlags()) && JavaModelUtil.isVisibleInHierarchy(res, type.getPackageFragment())) {
				if (JdtFlags.isAbstract(res)) {
					return JavaElementImageDescriptor.IMPLEMENTS;
				} else {
					return JavaElementImageDescriptor.OVERRIDES;
				}
			}
		}
		for (IType intf : hierarchy.getSuperInterfaces(type)) {
			IMethod res= JavaModelUtil.findMethodInHierarchy(hierarchy, intf, name, paramTypes, false);
			if (res != null) {
				if (JdtFlags.isAbstract(res)) {
					return JavaElementImageDescriptor.IMPLEMENTS;
				} else {
					return JavaElementImageDescriptor.OVERRIDES;
				}
			}
		}
		return 0;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		if (fListeners == null) {
			fListeners= new ListenerList<>();
		}
		fListeners.add(listener);
		if (fBuildListener == null) {
			fBuildListener= this::handleBuildEvent;
			ResourcesPlugin.getWorkspace().addResourceChangeListener(fBuildListener, IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD);
		}
	}

	@Override
	public void dispose() {
		removeBuildListener();
		fUpdateJob.cancel();
		fPendingElements.clear();
		if (fRegistry != null && fUseNewRegistry) {
			fRegistry.dispose();
		}
		if (fListeners != null) {
			fListeners.clear();
		}
	}

	private void recordSkippedElement(IMethod method) {
		if (fPendingElements.size() < MAX_PENDING_ELEMENTS) {
			fPendingElements.add(method);
		} else {
			fRefreshAll.set(true);
		}
		// the build may have finished while recording, its POST_BUILD then saw nothing pending
		if (!fBuildRunning.get()) {
			fUpdateJob.schedule(100);
		}
	}

	private void handleBuildEvent(IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_BUILD) {
			fBuildRunning.set(true);
		} else if (event.getType() == IResourceChangeEvent.POST_BUILD) {
			fBuildRunning.set(false);
			if (fRefreshAll.get() || !fPendingElements.isEmpty()) {
				fUpdateJob.schedule(100);
			}
		}
	}

	private void removeBuildListener() {
		if (fBuildListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fBuildListener);
			fBuildListener= null;
			fBuildRunning.set(false);
			fRefreshAll.set(false);
			fPendingElements.clear();
		}
	}

	private void fireLabelProviderChanged() {
		boolean refreshAll= fRefreshAll.getAndSet(false);
		// drain instead of clear: a build starting again in the meantime must not lose an element
		Object[] elements= fPendingElements.toArray();
		for (Object element : elements) {
			fPendingElements.remove(element);
		}

		if (fListeners == null || fListeners.isEmpty()) {
			return;
		}
		if (!refreshAll && elements.length == 0) {
			return;
		}
		LabelProviderChangedEvent event= refreshAll ? new LabelProviderChangedEvent(this) : new LabelProviderChangedEvent(this, elements);
		for (ILabelProviderListener listener : fListeners) {
			listener.labelProviderChanged(event);
		}
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		if (fListeners == null) {
			return;
		}
		fListeners.remove(listener);
		if (fListeners.isEmpty()) {
			removeBuildListener();
		}
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		int adornmentFlags= computeAdornmentFlags(element);
		if ((adornmentFlags & JavaElementImageDescriptor.IMPLEMENTS) != 0) {
			if ((adornmentFlags & JavaElementImageDescriptor.SYNCHRONIZED) != 0) {
				decoration.addOverlay(JavaPluginImages.DESC_OVR_SYNCH_AND_IMPLEMENTS);
			} else {
				decoration.addOverlay(JavaPluginImages.DESC_OVR_IMPLEMENTS);
			}
		} else if ((adornmentFlags & JavaElementImageDescriptor.OVERRIDES) != 0) {
			if ((adornmentFlags & JavaElementImageDescriptor.SYNCHRONIZED) != 0) {
				decoration.addOverlay(JavaPluginImages.DESC_OVR_SYNCH_AND_OVERRIDES);
			} else {
				decoration.addOverlay(JavaPluginImages.DESC_OVR_OVERRIDES);
			}
		}
	}

}
