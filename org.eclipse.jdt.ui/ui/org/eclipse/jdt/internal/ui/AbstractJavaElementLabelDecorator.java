/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;

public abstract class AbstractJavaElementLabelDecorator implements ILightweightLabelDecorator {

	private class DecoratorElementChangeListener implements IElementChangedListener {

		@Override
		public void elementChanged(ElementChangedEvent event) {
			List<IJavaElement> changed= new ArrayList<>();
			processDelta(event.getDelta(), changed);
			if (changed.isEmpty())
				return;

			fireChange(changed.toArray(new IJavaElement[changed.size()]));
		}

	}

	private ListenerList<ILabelProviderListener> fListeners;
	private IElementChangedListener fChangeListener;

	@Override
	public void addListener(ILabelProviderListener listener) {
		if (fChangeListener == null) {
			fChangeListener= new DecoratorElementChangeListener();
			JavaCore.addElementChangedListener(fChangeListener);
		}

		if (fListeners == null) {
			fListeners= new ListenerList<>();
		}

		fListeners.add(listener);
	}

	@Override
	public void dispose() {
		if (fChangeListener != null) {
			JavaCore.removeElementChangedListener(fChangeListener);
			fChangeListener= null;
		}
		if (fListeners != null) {
			for (ILabelProviderListener listener : fListeners) {
				fListeners.remove(listener);
			}
			fListeners= null;
		}
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		if (fListeners == null)
			return;

		fListeners.remove(listener);

		if (fListeners.isEmpty() && fChangeListener != null) {
			JavaCore.removeElementChangedListener(fChangeListener);
			fChangeListener= null;
		}
	}

	private void fireChange(IJavaElement[] elements) {
		if (fListeners != null && !fListeners.isEmpty()) {
			LabelProviderChangedEvent event= new LabelProviderChangedEvent(this, elements);
			for (ILabelProviderListener listener : fListeners) {
				listener.labelProviderChanged(event);
			}
		}
	}

	@Override
	public abstract void decorate(Object element, IDecoration decoration);

	protected abstract void processDelta(IJavaElementDelta delta, List<IJavaElement> result);

	protected boolean processChildrenDelta(IJavaElementDelta delta, List<IJavaElement> result) {
		for (IJavaElementDelta child : delta.getAffectedChildren()) {
			processDelta(child, result);
		}
		return false;
	}

}
