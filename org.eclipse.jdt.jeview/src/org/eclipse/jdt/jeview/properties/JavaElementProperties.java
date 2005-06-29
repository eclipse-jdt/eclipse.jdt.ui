/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.jeview.properties;

import java.util.ArrayList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.jeview.JEViewPlugin;

public class JavaElementProperties implements IPropertySource {
	
	private static final String C_JAVAELEMENT= "IJavaElement";
	
	private static final String P_ELEMENT_NAME= "org.eclipse.jdt.jeview.elementName";
	private static final String P_ELEMENT_TYPE= "org.eclipse.jdt.jeview.elementType";
	private static final String P_EXISTS= "org.eclipse.jdt.jeview.exists";
	private static final String P_READ_ONLY= "org.eclipse.jdt.jeview.readOnly";
	private static final String P_STRUCTURE_KNOWN= "org.eclipse.jdt.jeview.structureKnown";
	private static final String P_HANDLE_IDENTIFIER= "org.eclipse.jdt.jeview.handleIdentifier";
	private static final String P_PATH= "org.eclipse.jdt.jeview.path";
	
	private static final String C_MEMBER= "IMember";
	
	private static final String P_FLAGS= "org.eclipse.jdt.jeview.flags";
	private static final String P_NAME_RANGE= "org.eclipse.jdt.jeview.nameRange";
	private static final String P_BINARY= "org.eclipse.jdt.jeview.binary";
	
	private static final String C_PARENT= "IParent";
	
	private static final String P_HAS_CHILDREN= "org.eclipse.jdt.jeview.hasChildren";
	
	protected IJavaElement fJavaElement;
	
	private static final ArrayList<IPropertyDescriptor> JAVA_ELEMENT_PROPERTY_DESCRIPTORS= new ArrayList<IPropertyDescriptor>();
	static {
		addJavaElementDescriptor(new PropertyDescriptor(P_ELEMENT_NAME, "elementName"));
		addJavaElementDescriptor(new PropertyDescriptor(P_ELEMENT_TYPE, "elementType"));
		addJavaElementDescriptor(new PropertyDescriptor(P_EXISTS, "exists"));
		addJavaElementDescriptor(new PropertyDescriptor(P_READ_ONLY, "readOnly"));
		addJavaElementDescriptor(new PropertyDescriptor(P_STRUCTURE_KNOWN, "structureKnown"));
		addJavaElementDescriptor(new PropertyDescriptor(P_HANDLE_IDENTIFIER, "handleIdentifier"));
		addJavaElementDescriptor(new PropertyDescriptor(P_PATH, "path"));
	}

	private static void addJavaElementDescriptor(PropertyDescriptor descriptor) {
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(C_JAVAELEMENT);
		JAVA_ELEMENT_PROPERTY_DESCRIPTORS.add(descriptor);
	}
	
	private static final ArrayList<IPropertyDescriptor> MEMBER_PROPERTY_DESCRIPTORS= new ArrayList<IPropertyDescriptor>();
	static {
		addMemberDescriptor(new PropertyDescriptor(P_FLAGS, "flags"));
		addMemberDescriptor(new PropertyDescriptor(P_NAME_RANGE, "nameRange"));
		addMemberDescriptor(new PropertyDescriptor(P_BINARY, "binary"));
	}
	
	private static void addMemberDescriptor(PropertyDescriptor descriptor) {
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(C_MEMBER);
		MEMBER_PROPERTY_DESCRIPTORS.add(descriptor);
	}
	
	private static final ArrayList<IPropertyDescriptor> PARENT_PROPERTY_DESCRIPTORS= new ArrayList<IPropertyDescriptor>();
	static {
		addParentDescriptor(new PropertyDescriptor(P_HAS_CHILDREN, "hasChildren"));
	}
	
	private static void addParentDescriptor(PropertyDescriptor descriptor) {
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(C_PARENT);
		PARENT_PROPERTY_DESCRIPTORS.add(descriptor);
	}

	public JavaElementProperties(IJavaElement javaElement) {
		fJavaElement= javaElement;
	}
	
	public IPropertyDescriptor[] getPropertyDescriptors() {
		ArrayList<IPropertyDescriptor> result= new ArrayList<IPropertyDescriptor>(JAVA_ELEMENT_PROPERTY_DESCRIPTORS);
		if (fJavaElement instanceof IMember)
			result.addAll(MEMBER_PROPERTY_DESCRIPTORS);
		if (fJavaElement instanceof IParent)
			result.addAll(PARENT_PROPERTY_DESCRIPTORS);
		
		return result.toArray(new IPropertyDescriptor[result.size()]);
	}
	
	public Object getPropertyValue(Object name) {
		if (name.equals(P_ELEMENT_NAME)) {
			return fJavaElement.getElementName();
		} else 	if (name.equals(P_ELEMENT_TYPE)) {
			return getElementTypeString(fJavaElement.getElementType());
		} else 	if (name.equals(P_EXISTS)) {
			return fJavaElement.exists();
		} else 	if (name.equals(P_READ_ONLY)) {
			return fJavaElement.isReadOnly();
		} else 	if (name.equals(P_STRUCTURE_KNOWN)) {
			return computeisStructureKnown();
		} else 	if (name.equals(P_HANDLE_IDENTIFIER)) {
			return fJavaElement.getHandleIdentifier();
		} else 	if (name.equals(P_PATH)) {
			return fJavaElement.getPath();
		}
		
		if (fJavaElement instanceof IMember) {
			IMember member= (IMember) fJavaElement;
			if (name.equals(P_FLAGS)) {
				return computeFlags(member);
			} else if (name.equals(P_NAME_RANGE)) {
				return computeNameRange(member);
			} else if (name.equals(P_BINARY)) {
				return member.isBinary();
			}
		}
		
		if (fJavaElement instanceof IParent) {
			IParent parent= (IParent) fJavaElement;
			if (name.equals(P_HAS_CHILDREN)) {
				return computeHasChildren(parent);
			}
		}
		return null;
	}

	private String getElementTypeString(int elementType) {
		String name;
		switch (elementType) {
			case IJavaElement.JAVA_MODEL :
				name= "IJavaModel";
				break;
			case IJavaElement.JAVA_PROJECT :
				name= "IJavaProject";
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT :
				name= "IPackageFragmentRoot";
				break;
			case IJavaElement.PACKAGE_FRAGMENT :
				name= "IPackageFragment";
				break;
			case IJavaElement.COMPILATION_UNIT :
				name= "ICompilationUnit";
				break;
			case IJavaElement.CLASS_FILE :
				name= "IClassFile";
				break;
			case IJavaElement.TYPE :
				name= "IType";
				break;
			case IJavaElement.FIELD :
				name= "IField";
				break;
			case IJavaElement.METHOD :
				name= "IMethod";
				break;
			case IJavaElement.INITIALIZER :
				name= "IInitializer";
				break;
			case IJavaElement.PACKAGE_DECLARATION :
				name= "IPackageDeclaration";
				break;
			case IJavaElement.IMPORT_CONTAINER :
				name= "IImportContainer";
				break;
			case IJavaElement.IMPORT_DECLARATION :
				name= "IImportDeclaration";
				break;
			case IJavaElement.LOCAL_VARIABLE :
				name= "ILocalVariable";
				break;
			case IJavaElement.TYPE_PARAMETER :
				name= "ITypeParameter";
				break;
			default :
				name= "UNKNOWN";
				break;
		}
		return elementType + " (" + name + ")";
	}
	
	private Object computeisStructureKnown() {
		try {
			return fJavaElement.isStructureKnown();
		} catch (JavaModelException e) {
			JEViewPlugin.log("error calculating property", e);
			return "Error: " + e.getLocalizedMessage();
		}
	}
	
	private Object computeNameRange(IMember member) {
		try {
			ISourceRange nameRange= member.getNameRange();
			return nameRange.getOffset() + " + " + nameRange.getLength();
		} catch (JavaModelException e) {
			JEViewPlugin.log("error calculating property", e);
			return "Error: " + e.getLocalizedMessage();
		}
	}

	private Object computeFlags(IMember member) {
		int flags;
		try {
			flags= member.getFlags();
		} catch (JavaModelException e) {
			JEViewPlugin.log("error calculating property", e);
			return "Error: " + e.getLocalizedMessage();
		}
		return "0x" + Integer.toHexString(flags) + " (" + getFlagsString(flags) + ")";
	}

	private String getFlagsString(int flags) {
		StringBuffer sb = new StringBuffer();
		int rest= flags;
		
		rest&= ~ appendFlag(sb, flags, Flags.AccPublic, "public ");
		rest&= ~ appendFlag(sb, flags, Flags.AccPrivate, "private ");
		rest&= ~ appendFlag(sb, flags, Flags.AccProtected, "protected ");
		rest&= ~ appendFlag(sb, flags, Flags.AccStatic, "static ");
		rest&= ~ appendFlag(sb, flags, Flags.AccFinal, "final ");
		rest&= ~ appendFlag(sb, flags, Flags.AccSynchronized, "synchronized/super ");
		rest&= ~ appendFlag(sb, flags, Flags.AccVolatile, "volatile/bridge ");
		rest&= ~ appendFlag(sb, flags, Flags.AccTransient, "transient/varargs ");
		rest&= ~ appendFlag(sb, flags, Flags.AccNative, "native ");
		rest&= ~ appendFlag(sb, flags, Flags.AccInterface, "interface ");
		rest&= ~ appendFlag(sb, flags, Flags.AccAbstract, "abstract ");
		rest&= ~ appendFlag(sb, flags, Flags.AccStrictfp, "strictfp ");
		rest&= ~ appendFlag(sb, flags, Flags.AccSynthetic, "synthetic ");
		rest&= ~ appendFlag(sb, flags, Flags.AccAnnotation, "annotation ");
		rest&= ~ appendFlag(sb, flags, Flags.AccEnum, "enum ");
		rest&= ~ appendFlag(sb, flags, Flags.AccDeprecated, "deprecated ");
		
		if (rest != 0)
			sb.append("unknown:0x").append(Integer.toHexString(rest));
		
		int len = sb.length();
		if (len == 0)
			return ""; //$NON-NLS-1$
		sb.setLength(len - 1);
		return sb.toString();
	}
	
	private int appendFlag(StringBuffer sb, int flags, int flag, String name) {
		if ((flags & flag) != 0) {
			sb.append(name);
			return flag;
		} else {
			return 0;
		}
	}

	private Object computeHasChildren(IParent member) {
		try {
			return member.hasChildren();
		} catch (JavaModelException e) {
			JEViewPlugin.log("error calculating property", e);
			return "Error: " + e.getLocalizedMessage();
		}
	}
	
	public void setPropertyValue(Object name, Object value) {
		// do nothing
	}
	
	public Object getEditableValue() {
		return this;
	}
	
	public boolean isPropertySet(Object property) {
		return false;
	}
	
	public void resetPropertyValue(Object property) {
		// do nothing
	}
}
