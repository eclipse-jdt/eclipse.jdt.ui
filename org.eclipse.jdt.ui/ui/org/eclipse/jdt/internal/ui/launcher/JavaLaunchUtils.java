/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * This class serves as a location for utility methods.
 */
public class JavaLaunchUtils {
	
	private static ResourceBundle fgResourceBundle;
	
	static class ArgumentParser {
		private String fArgs;
		private int fIndex= 0;
		private int ch= -1;
		
		public ArgumentParser(String args) {
			fArgs= args;
		}
		
		private int getNext() {
			if (fIndex < fArgs.length())
				return fArgs.charAt(fIndex++);
			return -1;
		}
		
		public String[] parseArguments() {
			StringBuffer buf;
			List v= new ArrayList();
			
			ch= getNext();
			while (ch > 0) {
				while (Character.isWhitespace((char)ch))
					ch= getNext();	
				
				if (ch == '"') {
					v.add(parseString());
				} else {
					v.add(parseToken());
				}
			}
	
			String[] result= new String[v.size()];
			v.toArray(result);
			return result;
		}
		
		public String parseString() {
			StringBuffer buf= new StringBuffer();
			buf.append((char)ch);
			ch= getNext();
			while (ch > 0 && ch != '"') {
				buf.append((char)ch);
				ch= getNext();
			}
			if (ch > 0)
				buf.append((char)ch);
			ch= getNext();
				
			return buf.toString();
		}
		
		public String parseToken() {
			StringBuffer buf= new StringBuffer();
			
			while (ch > 0 && !Character.isWhitespace((char)ch)) {
				if (ch == '"')
					buf.append(parseString());
				else {
					buf.append((char)ch);
					ch= getNext();
				}
			}
			return buf.toString();
		}
	}

	public static String[] parseArguments(String args) {
		if (args == null)
			return new String[0];
		ArgumentParser parser= new ArgumentParser(args);
		String[] res= parser.parseArguments();
		
		return res;
	}

	/**
	 * Utility method with conventions
	 */
	public static void errorDialog(Shell shell, String resourcePrefix, IStatus s) {
		String message= getResourceString(resourcePrefix+"message");
		String title= getResourceString(resourcePrefix+"title");
		ErrorDialog.openError(shell, title, message, s);
	}
	
	/**
	 * Utility method
	 */
	public static String getResourceString(String key) {
		if (fgResourceBundle == null) {
			fgResourceBundle= getResourceBundle();
		}
		if (fgResourceBundle != null) {
			return getResourceBundle().getString(key);
		} else {
			return "!" + key + "!";
		}
	}
	/**
	 * Returns the resource bundle used by all parts of the launcher package.
	 */
	public static ResourceBundle getResourceBundle() {
		try {
			return ResourceBundle.getBundle("org.eclipse.jdt.internal.ui.launcher.LauncherResources");
		} catch (MissingResourceException e) {
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), "Error", e.toString());
		}		
		return null;
	}
	private static IType findInPackage(IPackageFragment pkg, String name) {
		IClassFile cf= pkg.getClassFile(name + ".class");
		try {
			if (cf.exists())
				return cf.getType();
		} catch (JavaModelException e) {
		}
		ICompilationUnit cu= pkg.getCompilationUnit(name + ".java");
		if (cu.exists())
			return cu.getType(name);
		try {
			ICompilationUnit[] cus= pkg.getCompilationUnits();
			for (int i= 0; i < cus.length; i++) {
				IType[] types= cus[i].getAllTypes();
				for (int j= 0; j < types.length; j++) {
					if (name.equals(types[j].getTypeQualifiedName()))
						return types[j];
				}
			}
		} catch (JavaModelException e) {
		}
		return null;
	}
	private static IPackageFragment findPackage(String name) {
		String pathString= name.replace('.', '/');
		return (IPackageFragment) locateElement(pathString);
	}
	static IType locateType(String name) {
		int lastDot= name.lastIndexOf('.');
		String pkgName= "";
		String typeName= name;
		if (lastDot >= 0) {
			pkgName= name.substring(0, lastDot);
			typeName= name.substring(lastDot + 1);
		}
		IPackageFragment pkg= findPackage(pkgName);
		if (pkg != null)
			return findInPackage(pkg, typeName);
		return null;

	}
	
	private static Object locateElement(IProject p, String fileName) {
		IJavaProject project= JavaCore.create(p);
		Object element= findInProject(project, fileName);
		if (element != null)
			return element;
		return null;
	}
	
	private static Object locateElement(String fileName) {
		IProject[] projects= JavaPlugin.getWorkspace().getRoot().getProjects();
		for (int i= 0; i < projects.length; i++) {
			Object element= locateElement(projects[i], fileName);
			if (element != null)
				return element;
		}
		return null;
	}
	
	private static Object findInProject(IJavaProject project, String fileName) {
		try {
			IJavaElement element= project.findElement(new Path(fileName));
			if (element instanceof ICompilationUnit) {
				return element.getCorrespondingResource();
			} else {
				return element;
			}
		} catch (JavaModelException x) {
		}
		return null;
	}

}