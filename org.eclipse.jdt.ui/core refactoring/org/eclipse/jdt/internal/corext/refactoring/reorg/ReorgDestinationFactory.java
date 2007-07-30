/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;

public class ReorgDestinationFactory {
	
	static final class ResourceDestination implements IReorgDestination {
		
		private final IResource fDestination;

		private ResourceDestination(IResource destination) {
			Assert.isNotNull(destination);
			fDestination= destination;
		}

		/**
		 * {@inheritDoc}
		 */
		public Object getDestination() {
			return getResource();
		}

		public IResource getResource() {
			return fDestination;
		}
		
	}
	
	static final class JavaElementDestination implements IReorgDestination {
		
		private final IJavaElement fDestination;

		private JavaElementDestination(IJavaElement destination) {
			Assert.isNotNull(destination);
			fDestination= destination;
		}

		/**
		 * {@inheritDoc}
		 */
		public Object getDestination() {
			return getJavaElement();
		}
		
		public IJavaElement getJavaElement() {
			return fDestination;
		}
		
	}
	
	/**
	 * Wrap the given object into a destination
	 * @param destination the object to wrap
	 * @return a reorg destination if possible reorg destination or <b>null</b> otherwise
	 */
	public static IReorgDestination createDestination(Object destination) {
		if (destination instanceof IJavaElement) {
			return new JavaElementDestination((IJavaElement) destination);
		} if (destination instanceof IResource) {
			return new ResourceDestination((IResource) destination);
		}
		
		return null;
	}

}
