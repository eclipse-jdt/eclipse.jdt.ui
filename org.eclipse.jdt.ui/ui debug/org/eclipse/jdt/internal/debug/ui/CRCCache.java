package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import java.io.IOException;import java.io.InputStream;import java.util.HashMap;import java.util.zip.CRC32;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IResourceChangeEvent;import org.eclipse.core.resources.IResourceChangeListener;import org.eclipse.core.resources.IResourceDelta;import org.eclipse.core.resources.IResourceDeltaVisitor;import org.eclipse.core.resources.ResourcesPlugin;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;

public class CRCCache implements IResourceChangeListener {
	
	protected static CRCCache fgDefault;
	
	protected HashMap fCache = new HashMap(20);	
	protected HashMap fFileCache = new HashMap(20);	
	
	public CRCCache() {
		super();
		fgDefault = this;
	}
	
	public void startup() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}
	
	public void shutdown() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		fCache.clear();
		fFileCache.clear();
	}
	
	public static CRCCache getDefault() {
		return fgDefault;
	}
	
	public int getCRC(IType type) {
		Integer crc = (Integer)fCache.get(type);
		if (crc == null) {
			try {
				crc = computeCRC(type);
			} catch (CoreException e) {
				crc = new Integer(0);
			}
			fCache.put(type, crc);
		}
		return crc.intValue();
	}
	
	protected Integer computeCRC(IType type) throws CoreException {
		byte[] bytes = null;
		if (type.isBinary()) {
			IPackageFragmentRoot root = (IPackageFragmentRoot)type.getPackageFragment().getParent();
			if (root.isArchive()) {
				// in a jar
				bytes = new byte[0];
			} else {
				// in a file
				IFile file = (IFile)type.getClassFile().getUnderlyingResource();
				bytes = getBytes(file.getContents());
				fFileCache.put(file, type);
			}
		} else {
			// source type - find binary
			IFile file = getBinaryFile(type);
			bytes = getBytes(file.getContents());
			fFileCache.put(file, type);
		}
		CRC32 crc = new CRC32();
		crc.update(bytes);
		return new Integer((int)crc.getValue());
		
	}


	protected IFile getBinaryFile(IType sourceType) throws JavaModelException {
	
		IJavaProject project = sourceType.getJavaProject();

		IPath path = project.getOutputLocation()
					.addTrailingSeparator()
					.append(sourceType.getPackageFragment().getElementName());
		IType enclosingType = sourceType;
		String binaryName = sourceType.getElementName();
		while ((enclosingType = enclosingType.getDeclaringType()) != null){
			binaryName = enclosingType.getElementName() + '$' + binaryName;
		}
		path = path.addTrailingSeparator()
				.append(binaryName)
				.addFileExtension("class");
		return project.getProject().getFile(path);
	}

	protected byte[] getBytes(InputStream stream) {
		byte[] bytes = new byte[0];
		try {
			int avail = stream.available();
			while (avail > 0) {
				byte[] temp = new byte[bytes.length + avail];
				System.arraycopy(bytes, 0, temp, 0, bytes.length);
				stream.read(temp, bytes.length, avail);
				bytes = temp;
			}
		} catch (IOException e) {
		}
		return bytes;
	}
	
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDeltaVisitor v = new CRCVisitor(); 
		try {
			v.visit(event.getDelta());
		} catch (CoreException e) {
		}
	}
	
	class CRCVisitor implements IResourceDeltaVisitor {
		public boolean visit(IResourceDelta delta) {
			IResource r = delta.getResource();
			if (r.getType() == IResource.FILE) {
				IType type = (IType)fFileCache.get(r);
				if (type != null) {
					fCache.remove(type);
				}
				return false;
			} else {
				return true;
			}
		}
	}
}
