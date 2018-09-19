/*******************************************************************************
 * Copyright (c) 2017 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Determines whether a package has only a single child as well as retrieves that child if it
 * exists. Also provides all package children of a package.
 *
 * <p>
 * For this class, the parent of a package is its hierarchical parent package, not the package root.
 * E.g. for packages {@code a}, {@code a.b.} and {@code a.b.c}, the parent of {@code a.b.c} is
 * {@code a.b} and the parent of {@code a.b} is {@code a}. The package {@code a} has no parent.
 * </p>
 *
 * <p>
 * A single query runs in constant time. Preparing for queries runs in time linear to the number of
 * packages in the package root. The first query on this object will run the preparation step.
 * </p>
 *
 * <p>
 * Not thread safe.
 * </p>
 *
 * @see #getDirectChildren(IPackageFragment)
 */
public class PackageCache {

	/**
	 * Caches the children of a package in a package root. The cache for a package root is built on the
	 * first query.
	 */
	static class PerRootCache {

		private final Map<IPackageFragmentRoot, PackageCache> packageCaches= new HashMap<>();

		boolean hasSingleChild(IPackageFragment packageFragment) throws JavaModelException {
			PackageCache packagesOfRoot= getPackageCache(packageFragment);
			return packagesOfRoot.hasSingleChild(packageFragment);
		}

		IPackageFragment getSingleChild(IPackageFragment packageFragment) throws JavaModelException {
			PackageCache packagesOfRoot= getPackageCache(packageFragment);
			return packagesOfRoot.getSingleChild(packageFragment);
		}

		List<IPackageFragment> getDirectChildren(IPackageFragment packageFragment) throws JavaModelException {
			PackageCache packagesOfRoot= getPackageCache(packageFragment);
			return packagesOfRoot.getDirectChildren(packageFragment);
		}

		private PackageCache getPackageCache(IPackageFragment packageFragment) {
			IPackageFragmentRoot packageRoot= (IPackageFragmentRoot) packageFragment.getParent();
			PackageCache packageCache= getPackageCache(packageRoot);
			return packageCache;
		}

		private PackageCache getPackageCache(IPackageFragmentRoot root) {
			PackageCache packageCacheOfRoot;
			synchronized (packageCaches) {
				packageCacheOfRoot= packageCaches.get(root);
				if (packageCacheOfRoot == null) {
					packageCacheOfRoot= new PackageCache(root);
					packageCaches.put(root, packageCacheOfRoot);
				}
			}
			return packageCacheOfRoot;
		}

		/**
		 * Can be called from a different (not only UI) thread.
		 */
		void clear() {
			synchronized (packageCaches) {
				packageCaches.clear();
			}
		}
	}


	private final IPackageFragmentRoot packageRoot;

	/**
	 * Key is {@link IPackageFragment#getElementName()}, value is the list of the direct children
	 * packages.
	 */
	private final Map<String, List<IPackageFragment>> packagesCache;

	private boolean initialized;

	/**
	 * @param packageRoot The package root for packages of which the queries will be issued.
	 */
	public PackageCache(IPackageFragmentRoot packageRoot) {
		this.packageRoot= packageRoot;
		packagesCache= new HashMap<>();
		initialized= false;
	}

	/**
	 * @return {@code true} iff the specified fragment has exactly one child.
	 *
	 * @param packageFragment The fragment for which to check.
	 * @throws JavaModelException If accessing the packages in the package root fails.
	 *
	 * @see #getSingleChild(IPackageFragment)
	 */
	public boolean hasSingleChild(IPackageFragment packageFragment) throws JavaModelException {
		IPackageFragment singleChild= getSingleChild(packageFragment);
		boolean hasSingleChild= singleChild != null;
		return hasSingleChild;
	}

	/**
	 * @return The single child of the specified package or {@code null} if the package does not have
	 *         exactly one child.
	 *
	 * @param packageFragment The single child of this fragment will be retrieved.
	 * @throws JavaModelException If accessing the packages in the package root fails.
	 *
	 * @see #getDirectChildren(IPackageFragment)
	 */
	public IPackageFragment getSingleChild(IPackageFragment packageFragment) throws JavaModelException {
		List<IPackageFragment> children= getDirectChildren(packageFragment);
		boolean hasSingleChild= children.size() == 1;
		if (hasSingleChild) {
			IPackageFragment singleChild= children.get(0);
			return singleChild;
		}
		return null;
	}

	/**
	 * <b>Example:</b> The following holds for a package root folder {@code src}, with packages:
	 *
	 * <pre>
	 *   a
	 *   |
	 *   |-- b
	 *       |
	 *       |-- c
	 *       |   |
	 *       |   |-- d
	 *       |
	 *       |-- e
	 *
	 *   f
	 *   |
	 *   |-- g
	 * </pre>
	 * <p>
	 * The packages that have a single child are {@code a}, {@code a.b.c} and {@code f}. Their children
	 * are {@code a.b}, {@code a.b.c.d} and {@code f.g}, respectively.
	 * </p>
	 * <p>
	 * Package {@code a.b} has children {@code a.b.c} and {@code a.b.e}. Packages {@code a.b.c.d},
	 * {@code a.b.e} and {@code f.g} have no children.
	 * </p>
	 * <p>
	 * All packages are {@code a}, {@code a.b}, {@code a.b.c}, {@code a.b.c.d}, {@code a.b.e}, {@code f}
	 * and {@code f.g}.
	 * </p>
	 *
	 * @return The direct children of the specified package. Never {@code null}.
	 *
	 * @param packageFragment The direct children of this fragment will be retrieved.
	 * @throws JavaModelException If accessing the packages in the package root fails.
	 */
	public List<IPackageFragment> getDirectChildren(IPackageFragment packageFragment) throws JavaModelException {
		initialize();
		String packageName= packageFragment.getElementName();
		List<IPackageFragment> childrenOfPackage= packagesCache.get(packageName);
		if (childrenOfPackage == null) {
			return Collections.EMPTY_LIST;
		}
		return Collections.unmodifiableList(childrenOfPackage);
	}

	private void initialize() throws JavaModelException {
		if (!initialized) {
			collectChildrenOfPackages();
			initialized= true;
		}
	}

	/**
	 * Prepares for queries.
	 *
	 * @throws JavaModelException If accessing the packages in the package root fails.
	 */
	private void collectChildrenOfPackages() throws JavaModelException {
		packagesCache.clear();

		IJavaElement[] allPackages= packageRoot.getChildren();

		for (IJavaElement child : allPackages) {
			IPackageFragment currentPackage= (IPackageFragment) child;

			String packageName= currentPackage.getElementName();

			int index= packageName.lastIndexOf('.');
			boolean hasParentPackage= index != -1;
			if (hasParentPackage) {
				String parentName= packageName.substring(0, index);

				List<IPackageFragment> siblingsOfCurrentPackage= packagesCache.get(parentName);
				if (siblingsOfCurrentPackage == null) {
					siblingsOfCurrentPackage= new ArrayList<>();
					packagesCache.put(parentName, siblingsOfCurrentPackage);
				}
				siblingsOfCurrentPackage.add(currentPackage);
			}
		}
	}
}
