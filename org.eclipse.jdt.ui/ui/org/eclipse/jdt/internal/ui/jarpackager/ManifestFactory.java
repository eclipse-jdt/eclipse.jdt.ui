/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.jarpackager;import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.jface.util.Assert;import org.eclipse.jdt.core.IPackageFragment;

/**
 * This factory creates manifest files.
 */
public class ManifestFactory {

	private static final ManifestFactory fgSingleton= new ManifestFactory();

	// Constants
	private static final String SEALED_VALUE= "true";
	private static final String UNSEALED_VALUE= "false";
	
	private ManifestFactory() {
	}

	/**
	 * Returns the sole instance of this factory.
	 * 
	 * @return	the sole factory instance
	 */	
	public static ManifestFactory getInstance() {
		return fgSingleton;
	}
	/**
	 * Creates a manifest as defined by the <code>JarPackage</code>.
	 * 
	 * @param	jarPackage	the JAR package specification
	 */	
	public Manifest create(JarPackage jarPackage) throws IOException, CoreException {
		Assert.isNotNull(jarPackage);
		if (jarPackage.isManifestGenerated())
			return createGeneratedManifest(jarPackage);
		else
			return createSuppliedManifest(jarPackage);
	}
	/**
	 * Creates a default manifest.
	 * 
	 * @param manifestVersion	the version of the manifest
	 */	
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
	protected void putAdditionalEntries(Manifest manifest, JarPackage jarPackage) {
	}

	private Manifest createGeneratedManifest(JarPackage jarPackage) {
		Manifest manifest= new Manifest();
		putVersion(manifest, jarPackage);
		putSealing(manifest, jarPackage);
		putMainClass(manifest, jarPackage);
		putDownloadExtension(manifest, jarPackage);
		putAdditionalEntries(manifest, jarPackage);
		return manifest;
	}

	private void putVersion(Manifest manifest, JarPackage jarPackage) {
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, jarPackage.getManifestVersion());
	}
		
	private void putSealing(Manifest manifest, JarPackage jarPackage) {
		if (jarPackage.isJarSealed()) {
			manifest.getMainAttributes().put(Attributes.Name.SEALED, SEALED_VALUE);
			IPackageFragment[] packages= jarPackage.getPackagesToUnseal();
			if (packages != null) {
				for (int i= 0; i < packages.length; i++) {
					Attributes attributes= new Attributes();
					attributes.put(Attributes.Name.SEALED, UNSEALED_VALUE);
					manifest.getEntries().put(getInManifestFormat(packages[i]), attributes);
				}
			}
		}
		else {
			IPackageFragment[] packages= jarPackage.getPackagesToSeal();
			if (packages != null)
				for (int i= 0; i < packages.length; i++) {
					Attributes attributes= new Attributes();
					attributes.put(Attributes.Name.SEALED, SEALED_VALUE);
					manifest.getEntries().put(getInManifestFormat(packages[i]), attributes);
				}
		}
	}
	
	private void putMainClass(Manifest manifest, JarPackage jarPackage) {
		if (jarPackage.getMainClass() != null && jarPackage.getMainClass().getFullyQualifiedName().length() > 0)
			manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, jarPackage.getMainClass().getFullyQualifiedName());
	}
	
	private void putDownloadExtension(Manifest manifest, JarPackage jarPackage) {
		if (jarPackage.getDownloadExtensionsPath() != null && jarPackage.getDownloadExtensionsPath().length() > 0)
			manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, jarPackage.getDownloadExtensionsPath());
	}

	private String getInManifestFormat(IPackageFragment packageFragment) {
		String name= packageFragment.getElementName();
		return name.replace('.', '/') + '/';
	}

	private Manifest createSuppliedManifest(JarPackage jarPackage) throws IOException, CoreException {		IFile manifestFile= jarPackage.getManifestFile();		if (manifestFile.isLocal(IResource.DEPTH_ZERO))			manifestFile.setLocal(true, IResource.DEPTH_ZERO, new NullProgressMonitor());		Manifest manifest;		// No need to use buffer here because Manifest(...) does		InputStream stream= jarPackage.getManifestFile().getContents(false);		try {			manifest= new Manifest(stream);		} finally {			if (stream != null)				stream.close();		}
		return manifest;
	}
}