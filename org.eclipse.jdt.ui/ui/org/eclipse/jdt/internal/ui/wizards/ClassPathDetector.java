package org.eclipse.jdt.internal.ui.wizards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
  */
public class ClassPathDetector implements IResourceVisitor {
		
	private HashSet fSourceFolders;
	private HashSet fJARFiles;
	private HashSet fClassFolders;
		
	private IProject fProject;
		
	private IPath fResultOutputFolder;
	private IClasspathEntry[] fResultClasspath;
		
	public ClassPathDetector(IProject project) throws CoreException {
		fSourceFolders= new HashSet();
		fJARFiles= new HashSet(10);
		fClassFolders= new HashSet(100);
		fProject= project;
			
		project.accept(this);
			
		fResultClasspath= null;
		fResultOutputFolder= null;
			
		detectClasspath();
	}
	
	
	private boolean isNested(IPath path, Iterator iter) {
		while (iter.hasNext()) {
			IPath other= (IPath) iter.next();
			if (other.isPrefixOf(path)) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Method detectClasspath.
	 */
	private void detectClasspath() {
		ArrayList cpEntries= new ArrayList();
			
		for (Iterator iter= fSourceFolders.iterator(); iter.hasNext();) {
			IPath path= (IPath) iter.next();
			ArrayList excluded= new ArrayList();
			for (Iterator inner= fSourceFolders.iterator(); inner.hasNext();) {
				IPath other= (IPath) inner.next();
				if (!path.equals(other) && path.isPrefixOf(other)) {
					IPath pathToExclude= other.removeFirstSegments(path.segmentCount()).addTrailingSeparator();
					excluded.add(pathToExclude);
				}
			}
			IPath[] excludedPaths= (IPath[]) excluded.toArray(new IPath[excluded.size()]);
			IClasspathEntry entry= JavaCore.newSourceEntry(path, excludedPaths);
			cpEntries.add(entry);
		}
			
		for (Iterator iter= fJARFiles.iterator(); iter.hasNext();) {
			IPath path= (IPath) iter.next();
			if (!isNested(path, fSourceFolders.iterator())) {
				IClasspathEntry entry= JavaCore.newLibraryEntry(path, null, null);
				cpEntries.add(entry);	
			}
		}
			
		if (cpEntries.isEmpty()) {
			return;
		}
		IClasspathEntry[] jreEntries= PreferenceConstants.getDefaultJRELibrary();
		for (int i= 0; i < jreEntries.length; i++) {
			cpEntries.add(jreEntries[i]);
		}

		IClasspathEntry[] entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
		IPath outputLocation;

		IPath projPath= fProject.getFullPath();
		if (fSourceFolders.size() == 1 && entries[0].getPath().equals(projPath)) {
			outputLocation= projPath;
		} else {
			outputLocation= projPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
		} 			

		if (!JavaConventions.validateClasspath(JavaCore.create(fProject), entries, outputLocation).isOK()) {
			return;
		}
			
		fResultClasspath= entries;
		fResultOutputFolder= outputLocation;
	}

	private boolean visitCompilationUnit(IFile file) throws JavaModelException {
		ICompilationUnit cu= JavaCore.createCompilationUnitFrom(file);
		if (cu != null) {
			ICompilationUnit workingCopy= null;
			try {
				workingCopy= (ICompilationUnit) cu.getWorkingCopy();
				synchronized(workingCopy) {
					workingCopy.reconcile();
				}
				IPath packPath= file.getParent().getFullPath();
				IPackageDeclaration[] decls= workingCopy.getPackageDeclarations();
				if (decls.length == 0) {
					fSourceFolders.add(packPath);
				} else {
					IPath relpath= new Path(decls[0].getElementName().replace('.', '/'));
					int remainingSegments= packPath.segmentCount() - relpath.segmentCount();
					if (remainingSegments >= 0) {
						IPath common= packPath.removeFirstSegments(remainingSegments);
						if (common.equals(relpath)) {
							IPath prefix= packPath.uptoSegment(remainingSegments);
							fSourceFolders.add(prefix);
						}
					}
				}						
			} finally {
				if (workingCopy != null) {
					workingCopy.destroy();
				}
			}
		}
		return true;
	}
		
	private boolean visitClassFile(IFile file) {
		/*IClassFileReader reader= ToolFactory.createDefaultClassFileReader(file.getLocation().toOSString(), 0);
		char[] className= reader.getClassName();
		String str= new String(className);
		*/
		return true;
	}	
		
	public boolean visit(IResource resource) throws CoreException {
		if (resource.getType() == IResource.FILE) {
			IFile file= (IFile) resource;
			String extension= resource.getFileExtension();
			if ("java".equalsIgnoreCase(extension)) { //$NON-NLS-1$
				return visitCompilationUnit(file);
			} else if ("class".equalsIgnoreCase(extension)) { //$NON-NLS-1$
				return visitClassFile(file);
			} else if ("jar".equalsIgnoreCase(extension)) { //$NON-NLS-1$
				fJARFiles.add(file.getFullPath());
				return false;
			}
		}
		return true;
	}
		
	public IPath getOutputLocation() {
		return fResultOutputFolder;
	}
		
	public IClasspathEntry[] getClasspath() {
		return fResultClasspath;
	}
}
	

