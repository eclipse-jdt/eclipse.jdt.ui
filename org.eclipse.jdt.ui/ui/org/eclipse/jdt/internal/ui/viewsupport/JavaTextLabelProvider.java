/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.IPath;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;


/**
 * Default strategy of the Java plugin for the construction of Java element UI labels.
 */
public class JavaTextLabelProvider {	
	
	protected int fFlags;
	protected ResourceBundle fResourceBundle;
		
	public JavaTextLabelProvider(int flags) {
		fFlags= flags;
		fResourceBundle= JavaPlugin.getResourceBundle();
	}
	
	
	public void turnOn(int flags) {
		fFlags |= flags;
	}
	
	public void turnOff(int flags) {
		fFlags &= (~flags);
	}

	protected final boolean showReturnTypes() {
		return (fFlags & JavaElementLabelProvider.SHOW_RETURN_TYPE) != 0;
	}
	
	protected final boolean showParameters() {
		return (fFlags & JavaElementLabelProvider.SHOW_PARAMETERS) != 0;
	}
			
	protected final boolean showContainer() {
		return (fFlags & JavaElementLabelProvider.SHOW_CONTAINER) != 0;
	}
	
	protected final boolean showVariable() {
		return (fFlags & JavaElementLabelProvider.SHOW_VARIABLE) != 0;
	}	
	
	protected final boolean showRoot() {
		return (fFlags & JavaElementLabelProvider.SHOW_ROOT) != 0;
	}	

	protected final boolean showContainerQualification() {
		return (fFlags & JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION) != 0;
	}
	
	protected final boolean showPostfixQualification() {
		return (fFlags & JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION) != 0;
	}
	protected final boolean showType() {
		return (fFlags & JavaElementLabelProvider.SHOW_TYPE) != 0;
	}
	
	protected void renderName(IJavaElement element, StringBuffer buf) {
		switch (element.getElementType()) {
			case IJavaElement.IMPORT_CONTAINER:
				buf.append(fResourceBundle.getString("ImportContainer.label"));
				break;
			case IJavaElement.INITIALIZER:
				buf.append(fResourceBundle.getString("Initializer.label"));
				break;
			case IJavaElement.PACKAGE_FRAGMENT:
				renderPackageFragment((IPackageFragment)element, buf);
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				renderPackageFragmentRoot((IPackageFragmentRoot)element, buf);
				break;			
			case IJavaElement.COMPILATION_UNIT:
			case IJavaElement.CLASS_FILE:
				if (showContainerQualification()) {
					IPackageFragment pack= (IPackageFragment)element.getParent();
					renderQualified(element.getElementName(), pack, buf);
				} else {
					buf.append(element.getElementName());
				}
				break;
			case IJavaElement.TYPE:
				if (showContainerQualification()) {
					IType type= (IType)element;
					IJavaElement container= type.getDeclaringType();
					if (container != null) {
						renderInnerType(type, buf);
					} else {
						renderQualified(type.getElementName(), type.getPackageFragment(), buf);
					}
				} else {
					buf.append(element.getElementName());
				}
				break;			
			case IJavaElement.METHOD:
				renderMethod((IMethod)element, buf);
				break;
			case IJavaElement.FIELD:
				renderField((IField)element, buf);
				break;			
			default:
				buf.append(element.getElementName());
		}
	}
	
	protected void renderPackageFragment(IPackageFragment element, StringBuffer buf) {
		String name= element.getElementName();
		if ("".equals(name)) {
			buf.append(fResourceBundle.getString("DefaultPackage.label"));
		} else {
			buf.append(name);
		}
		if (showContainer()) {
			IPackageFragmentRoot parent= JavaModelUtility.getPackageFragmentRoot(element);
			buf.append(" - ");
			String container= parent.getElementName();
			// The default package fragment root of a project doesn't have a name.
			if (container.length() == 0) {
				container= parent.getParent().getElementName();
			}
			buf.append(container);
		} 
	}
	
	protected void renderPackageFragmentRoot(IPackageFragmentRoot root, StringBuffer buf) {
		String name= root.getElementName();
		if (showVariable() && root.isArchive()) {
			try {
				IClasspathEntry rawEntry= JavaModelUtility.getRawClasspathEntry(root);
				if (rawEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
					buf.append(rawEntry.getPath().makeRelative());
					buf.append(" - ");
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
		buf.append(root.getElementName());
	}
	
	protected void renderQualified(String elementName, IPackageFragment pack, StringBuffer buf) {
		if (showPostfixQualification()) {
			buf.append(elementName);
			buf.append(" - ");
			renderName(pack, buf);
		} else {
			if (!pack.isDefaultPackage()) {
				buf.append(pack.getElementName());
				buf.append('.');
			}
			buf.append(elementName);
		}
	}
	
	protected void renderInnerType(IType type, StringBuffer buf) {
		if (showPostfixQualification()) {
			buf.append(type.getElementName());
			buf.append(" - ");
			renderName(type.getDeclaringType(), buf);
		} else {
			buf.append(JavaModelUtility.getFullyQualifiedName(type));
		}
	}	
	
	protected void renderMethod(IMethod method, StringBuffer buf) {
		try {
			if (showReturnTypes() && !method.isConstructor()) {
				buf.append(Signature.getSimpleName(Signature.toString(method.getReturnType())));
				buf.append(' ');
			}
			
			buf.append(method.getElementName());
			
			if (showParameters()) {
				buf.append('(');
				
				String[] types= method.getParameterTypes();
				if (types.length > 0) {
					buf.append(Signature.getSimpleName(Signature.toString(types[0])));
				}
				for (int i= 1; i < types.length; i++) {
					buf.append(", ");
					buf.append(Signature.getSimpleName(Signature.toString(types[i])));
				}
				
				buf.append(')');
			}
			
			if (showContainer()) {
				buf.append(" - ");
				renderName(method.getDeclaringType(), buf);
			}
			
		} catch (JavaModelException e) {
		}
	}
	
	protected void renderField(IField field, StringBuffer buf) {
		try {
			buf.append(field.getElementName());
			
			if (showContainer()) {
				buf.append(" - ");
				renderName(field.getDeclaringType(), buf);
			} else if (showType()) {
				buf.append(" - ");
				buf.append(Signature.toString(field.getTypeSignature()));
			}
		} catch (JavaModelException x) {
			// dont' show type on exception
		}
	}	
		
	/**
	 * Returns the UI label of a given Java element.
	 */
	public String getTextLabel(IJavaElement element) {
		StringBuffer buf= new StringBuffer();
		renderName(element, buf);
		if (showRoot()) {
			IPackageFragmentRoot root= JavaModelUtility.getPackageFragmentRoot(element);
			if (root != null) {
				buf.append(" - ");
				buf.append(root.getPath().toString());
			}	
		}
		return buf.toString();
	}
	

}