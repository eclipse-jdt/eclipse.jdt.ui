/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.ui.jarpackager.IManifestProvider;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

/**
 * A manifest provider creates manifest files.
 */
public class ManifestProvider implements IManifestProvider {

	// Constants
	private static final String SEALED_VALUE= "true"; //$NON-NLS-1$
	private static final String UNSEALED_VALUE= "false"; //$NON-NLS-1$

	/**
	 * Creates a manifest as defined by the <code>JarPackage</code>.
	 *
	 * @param	jarPackage	the JAR package specification
	 * @return the manifest
	 */
	@Override
	public Manifest create(JarPackageData jarPackage) throws CoreException {
		Assert.isNotNull(jarPackage);
		if (jarPackage.isManifestGenerated())
			return createGeneratedManifest(jarPackage);

		try {
			return createSuppliedManifest(jarPackage);
		} catch (IOException ex) {
			throw JarPackagerUtil.createCoreException(ex.getLocalizedMessage(), ex);
		}
	}

	/**
	 * Creates a default manifest.
	 *
	 * @param manifestVersion	the version of the manifest
	 * @return the manifest
	 */
	@Override
	public Manifest createDefault(String manifestVersion) {
		Manifest manifest= new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, manifestVersion);
		return manifest;
	}

	/**
	 * Hook for subclasses to add additional manifest entries.
	 *
	 * @param	manifest	the manifest to which the entries should be added
	 * @param	jarPackage	the JAR package specification
	 */
	protected void putAdditionalEntries(Manifest manifest, JarPackageData jarPackage) {
	}

	private Manifest createGeneratedManifest(JarPackageData jarPackage) {
		Manifest manifest= new Manifest();
		putVersion(manifest, jarPackage);
		putSealing(manifest, jarPackage);
		putMainClass(manifest, jarPackage);
		putAdditionalEntries(manifest, jarPackage);
		return manifest;
	}

	private void putVersion(Manifest manifest, JarPackageData jarPackage) {
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, jarPackage.getManifestVersion());
	}

	private void putSealing(Manifest manifest, JarPackageData jarPackage) {
		if (jarPackage.isJarSealed()) {
			manifest.getMainAttributes().put(Attributes.Name.SEALED, SEALED_VALUE);
			IPackageFragment[] packages= jarPackage.getPackagesToUnseal();
			if (packages != null) {
				for (IPackageFragment p : packages) {
					Attributes attributes= new Attributes();
					attributes.put(Attributes.Name.SEALED, UNSEALED_VALUE);
					manifest.getEntries().put(getInManifestFormat(p), attributes);
				}
			}
		}
		else {
			IPackageFragment[] packages= jarPackage.getPackagesToSeal();
			if (packages != null)
				for (IPackageFragment p : packages) {
					Attributes attributes= new Attributes();
					attributes.put(Attributes.Name.SEALED, SEALED_VALUE);
					manifest.getEntries().put(getInManifestFormat(p), attributes);
			}
		}
	}

	private void putMainClass(Manifest manifest, JarPackageData jarPackage) {
		if (jarPackage.getManifestMainClass() != null && jarPackage.getManifestMainClass().getFullyQualifiedName().length() > 0)
			manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, jarPackage.getManifestMainClass().getFullyQualifiedName());
	}

	private String getInManifestFormat(IPackageFragment packageFragment) {
		String name= packageFragment.getElementName();
		return name.replace('.', '/') + '/';
	}

	private Manifest createSuppliedManifest(JarPackageData jarPackage) throws CoreException, IOException {
		try (InputStream stream= jarPackage.getManifestFile().getContents(false);) {
			return new Manifest(stream);
		}
	}
}
