/*******************************************************************************
 * Copyright (c) 2017 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer.RequiredProjectWrapper;

public class WithoutTestCodeDecorator implements ILightweightLabelDecorator {

	public WithoutTestCodeDecorator() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		// no action required
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// no action required
	}

	@Override
	public void dispose() {
		// no action required
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof ClassPathContainer) {
			ClassPathContainer classPathContainer= (ClassPathContainer) element;
			IClasspathEntry classpathEntry= classPathContainer.getClasspathEntry();
			decorateClassPathEntry(classpathEntry, decoration);
		} else if (element instanceof RequiredProjectWrapper) {
			RequiredProjectWrapper requiredProjectWrapper= (RequiredProjectWrapper) element;
			IClasspathEntry classpathEntry= requiredProjectWrapper.getClasspathEntry();
			decorateClassPathEntry(classpathEntry, decoration);
		} else {
			IClasspathEntry classpathEntry= determineClassPathEntry(element);
			decorateClassPathEntry(classpathEntry, decoration);
		}

	}

	private static IClasspathEntry determineClassPathEntry(Object element) {
		if (element instanceof IJavaElement) {
			IPackageFragmentRoot packageFragmentRoot= (IPackageFragmentRoot) ((IJavaElement) element).getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (packageFragmentRoot != null) {
				try {
					return packageFragmentRoot.getResolvedClasspathEntry();
				} catch (JavaModelException e) {
					return null;
				}
			}
		}
		return null;
	}
	private void decorateClassPathEntry(IClasspathEntry classpathEntry, IDecoration decoration) {
		if (classpathEntry == null) {
			return;
		}
		if (classpathEntry.isWithoutTestCode()) {
			decoration.addSuffix(JavaUIMessages.WithoutTestCodeDecorator_suffix_withoutTestCode);
		}
	}
}
