/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.jeview.JEViewPlugin;

public class JavaElementProperties implements IPropertySource {
	
	private static HashMap<String, Property> fgIdToProperty= new HashMap<String, Property>();
	private static LinkedHashMap<Class<?>, List<Property>> fgTypeToProperty= new LinkedHashMap<Class<?>, List<Property>>();
	
	
	private static abstract class Property {
		private final Class<?> fType;
		private final String fName;
		private final String fId;
		private final PropertyDescriptor fDescriptor;
		
		public Property(Class<?> type, String name) {
			fType= type;
			fName= name;
			fId= "org.eclipse.jdt.jeview." + type.getSimpleName() + "." + name;
			fDescriptor= new PropertyDescriptor(fId, fName);
			fDescriptor.setAlwaysIncompatible(true);
			fDescriptor.setCategory(type.getSimpleName());
		}

		public abstract Object compute(IJavaElement javaElement) throws JavaModelException;

		
		public Class<?> getType() {
			return fType;
		}
		
		
		public String getName() {
			return fName;
		}
		
		public String getId() {
			return fId;
		}
		
		public PropertyDescriptor getDescriptor() {
			return fDescriptor;
		}
		
	}
	
	static {
		addJavaElementProperties();
		addClassFileProperties();
		addCompilationUnitProperties();
		addImportDeclarationProperties();
		addJavaProjectProperties();
		addLocalVariableProperties();
		addMemberProperties();
		addFieldProperties();
		addMethodProperties();
		addTypeProperties();
		addPackageFragmentProperties();
		addPackageFragmentRootProperties();
		addTypeParammeterProperties();
		addParentProperties();
		addSourceReferenceProperties();
		addOpenableProperties();
	}

	private static void addJavaElementProperties() {
		addProperty(new Property(IJavaElement.class, "elementName") {
			@Override public Object compute(IJavaElement element) {
				return element.getElementName();
			}
		});
		addProperty(new Property(IJavaElement.class, "elementType") {
			@Override public Object compute(IJavaElement element) {
				return getElementTypeString(element.getElementType());
			}
		});
		addProperty(new Property(IJavaElement.class, "exists") {
			@Override public Object compute(IJavaElement element) {
				return element.exists();
			}
		});
		addProperty(new Property(IJavaElement.class, "isReadOnly") {
			@Override public Object compute(IJavaElement element) {
				return element.isReadOnly();
			}
		});
		addProperty(new Property(IJavaElement.class, "isStructureKnown") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return element.isStructureKnown();
			}
		});
		addProperty(new Property(IJavaElement.class, "handleIdentifier") {
			@Override public Object compute(IJavaElement element) {
				return element.getHandleIdentifier();
			}
		});
		addProperty(new Property(IJavaElement.class, "path") {
			@Override public Object compute(IJavaElement element) {
				return element.getPath();
			}
		});
		addProperty(new Property(IJavaElement.class, "schedulingRule") {
			@Override public Object compute(IJavaElement element) {
				return getSchedulingRuleString(element.getSchedulingRule());
			}
		});
	}

	private static void addClassFileProperties() {
		addProperty(new Property(IClassFile.class, "isClass") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IClassFile) element).isClass();
			}
		});
		addProperty(new Property(IClassFile.class, "isInterface") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IClassFile) element).isInterface();
			}
		});
	}

	private static void addCompilationUnitProperties() {
		addProperty(new Property(ICompilationUnit.class, "hasResourceChanged") {
			@Override public Object compute(IJavaElement element) {
				return ((ICompilationUnit) element).hasResourceChanged();
			}
		});
		addProperty(new Property(ICompilationUnit.class, "isWorkingCopy") {
			@Override public Object compute(IJavaElement element) {
				return ((ICompilationUnit) element).isWorkingCopy();
			}
		});
	}

	private static void addImportDeclarationProperties() {
		addProperty(new Property(IImportDeclaration.class, "flags") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return getFlagsString(((IImportDeclaration) element).getFlags(), IImportDeclaration.class);
			}
		});
		addProperty(new Property(IImportDeclaration.class, "isOnDemand") {
			@Override public Object compute(IJavaElement element) {
				return ((IImportDeclaration) element).isOnDemand();
			}
		});
	}

	private static void addJavaProjectProperties() {
		addProperty(new Property(IJavaProject.class, "hasBuildState") {
			@Override public Object compute(IJavaElement element) {
				return ((IJavaProject) element).hasBuildState();
			}
		});
		addProperty(new Property(IJavaProject.class, "getOutputLocation") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IJavaProject) element).getOutputLocation();
			}
		});
		addProperty(new Property(IJavaProject.class, "readOutputLocation") {
			@Override public Object compute(IJavaElement element) {
				return ((IJavaProject) element).readOutputLocation();
			}
		});
	}

	private static void addLocalVariableProperties() {
		addProperty(new Property(ILocalVariable.class, "nameRange") {
			@Override public Object compute(IJavaElement element) {
				return getSourceRangeString(((ILocalVariable) element).getNameRange());
			}
		});
		addProperty(new Property(ILocalVariable.class, "typeSignature") {
			@Override public Object compute(IJavaElement element) {
				return ((ILocalVariable) element).getTypeSignature();
			}
		});
	}

	private static void addMemberProperties() {
		addProperty(new Property(IMember.class, "nameRange") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return getSourceRangeString(((IMember) element).getNameRange());
			}
		});
		addProperty(new Property(IMember.class, "javadocRange") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return getSourceRangeString(((IMember) element).getJavadocRange());
			}
		});
		addProperty(new Property(IMember.class, "flags") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return getFlagsString(((IMember) element).getFlags(), element.getClass());
			}
		});
		addProperty(new Property(IMember.class, "isBinary") {
			@Override public Object compute(IJavaElement element) {
				return ((IMember) element).isBinary();
			}
		});
		addProperty(new Property(IMember.class, "occurrenceCount") {
			@Override public Object compute(IJavaElement element) {
				return ((IMember) element).getOccurrenceCount();
			}
		});
	}

	private static void addFieldProperties() {
		addProperty(new Property(IField.class, "isResolved") {
			@Override public Object compute(IJavaElement element) {
				return ((IField) element).isResolved();
			}
		});
		addProperty(new Property(IField.class, "key") {
			@Override public Object compute(IJavaElement element) {
				return ((IField) element).getKey();
			}
		});
		addProperty(new Property(IField.class, "typeSignature") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IField) element).getTypeSignature();
			}
		});
		addProperty(new Property(IField.class, "constant") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IField) element).getConstant();
			}
		});
		addProperty(new Property(IField.class, "isEnumConstant") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IField) element).isEnumConstant();
			}
		});
	}

	private static void addMethodProperties() {
		addProperty(new Property(IMethod.class, "isResolved") {
			@Override public Object compute(IJavaElement element) {
				return ((IMethod) element).isResolved();
			}
		});
		addProperty(new Property(IMethod.class, "key") {
			@Override public Object compute(IJavaElement element) {
				return ((IMethod) element).getKey();
			}
		});
		addProperty(new Property(IMethod.class, "key - signature") {
			@Override public Object compute(IJavaElement element) {
				String key= ((IMethod) element).getKey();
				if (key != null)
					return new BindingKey(key).toSignature();
				else
					return "<no key>";
			}
		});
		addProperty(new Property(IMethod.class, "numberOfParameters") {
			@Override public Object compute(IJavaElement element) {
				return ((IMethod) element).getNumberOfParameters();
			}
		});
		addProperty(new Property(IMethod.class, "signature") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IMethod) element).getSignature();
			}
		});
		addProperty(new Property(IMethod.class, "returnType") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IMethod) element).getReturnType();
			}
		});
		addProperty(new Property(IMethod.class, "isConstructor") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IMethod) element).isConstructor();
			}
		});
		addProperty(new Property(IMethod.class, "isMainMethod") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IMethod) element).isMainMethod();
			}
		});
	}

	private static void addTypeProperties() {
		addProperty(new Property(IType.class, "isResolved") {
			@Override public Object compute(IJavaElement element) {
				return ((IType) element).isResolved();
			}
		});
		addProperty(new Property(IType.class, "key") {
			@Override public Object compute(IJavaElement element) {
				return ((IType) element).getKey();
			}
		});
		addProperty(new Property(IType.class, "fullyQualifiedName") {
			@Override public Object compute(IJavaElement element) {
				return ((IType) element).getFullyQualifiedName();
			}
		});
		addProperty(new Property(IType.class, "fullyQualifiedName('*')") {
			@Override public Object compute(IJavaElement element) {
				return ((IType) element).getFullyQualifiedName('*');
			}
		});
		addProperty(new Property(IType.class, "fullyQualifiedParameterizedName") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IType) element).getFullyQualifiedParameterizedName();
			}
		});
		addProperty(new Property(IType.class, "typeQualifiedName") {
			@Override public Object compute(IJavaElement element) {
				return ((IType) element).getTypeQualifiedName();
			}
		});
		addProperty(new Property(IType.class, "typeQualifiedName('*')") {
			@Override public Object compute(IJavaElement element) {
				return ((IType) element).getTypeQualifiedName('*');
			}
		});
		addProperty(new Property(IType.class, "isAnnotation") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IType) element).isAnnotation();
			}
		});
		addProperty(new Property(IType.class, "isClass") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IType) element).isClass();
			}
		});
		addProperty(new Property(IType.class, "isEnum") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IType) element).isEnum();
			}
		});
		addProperty(new Property(IType.class, "isInterface") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IType) element).isInterface();
			}
		});
		addProperty(new Property(IType.class, "isAnonymous") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IType) element).isAnonymous();
			}
		});
		addProperty(new Property(IType.class, "isLocal") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IType) element).isLocal();
			}
		});
		addProperty(new Property(IType.class, "isMember") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IType) element).isMember();
			}
		});
	}

	private static void addPackageFragmentProperties() {
		addProperty(new Property(IPackageFragment.class, "containsJavaResources") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IPackageFragment) element).containsJavaResources();
			}
		});
		addProperty(new Property(IPackageFragment.class, "kind") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return getPFRKindString(((IPackageFragment) element).getKind());
			}
		});
		addProperty(new Property(IPackageFragment.class, "hasSubpackages") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IPackageFragment) element).hasSubpackages();
			}
		});
		addProperty(new Property(IPackageFragment.class, "isDefaultPackage") {
			@Override public Object compute(IJavaElement element) {
				return ((IPackageFragment) element).isDefaultPackage();
			}
		});
	}

	private static void addPackageFragmentRootProperties() {
		addProperty(new Property(IPackageFragmentRoot.class, "kind") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return getPFRKindString(((IPackageFragmentRoot) element).getKind());
			}
		});
		addProperty(new Property(IPackageFragmentRoot.class, "sourceAttachmentPath") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IPackageFragmentRoot) element).getSourceAttachmentPath();
			}
		});
		addProperty(new Property(IPackageFragmentRoot.class, "sourceAttachmentRootPath") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IPackageFragmentRoot) element).getSourceAttachmentRootPath();
			}
		});
		addProperty(new Property(IPackageFragmentRoot.class, "isArchive") {
			@Override public Object compute(IJavaElement element) {
				return ((IPackageFragmentRoot) element).isArchive();
			}
		});
		addProperty(new Property(IPackageFragmentRoot.class, "isExternal") {
			@Override public Object compute(IJavaElement element) {
				return ((IPackageFragmentRoot) element).isExternal();
			}
		});
	}

	private static void addTypeParammeterProperties() {
		addProperty(new Property(ITypeParameter.class, "nameRange") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return getSourceRangeString(((ITypeParameter) element).getNameRange());
			}
		});
	}

	private static void addParentProperties() {
		addProperty(new Property(IParent.class, "hasChildren") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IParent) element).hasChildren();
			}
		});
	}

	private static void addSourceReferenceProperties() {
		addProperty(new Property(ISourceReference.class, "sourceRange") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return getSourceRangeString(((ISourceReference) element).getSourceRange());
			}
		});
	}

	private static void addOpenableProperties() {
		addProperty(new Property(IOpenable.class, "hasUnsavedChanges") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IOpenable) element).hasUnsavedChanges();
			}
		});
		addProperty(new Property(IOpenable.class, "isConsistent") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				return ((IOpenable) element).isConsistent();
			}
		});
		addProperty(new Property(IOpenable.class, "isOpen") {
			@Override public Object compute(IJavaElement element) {
				return ((IOpenable) element).isOpen();
			}
		});
		addProperty(new Property(IOpenable.class, "findRecommendedLineSeparator") {
			@Override public Object compute(IJavaElement element) throws JavaModelException {
				String lineSeparator= ((IOpenable) element).findRecommendedLineSeparator();
				lineSeparator= lineSeparator.replace("\r", "\\r").replace("\n", "\\n");
				return lineSeparator;
			}
		});
	}

	private static void addProperty(Property property) {
		fgIdToProperty.put(property.getId(), property);
		List<Property> properties= fgTypeToProperty.get(property.getType());
		if (properties == null) {
			properties= new ArrayList<Property>();
			fgTypeToProperty.put(property.getType(), properties);
		}
		properties.add(property);
	}
	
	protected IJavaElement fJavaElement;

	public JavaElementProperties(IJavaElement javaElement) {
		fJavaElement= javaElement;
	}
	
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> result= new ArrayList<IPropertyDescriptor>();
		for (Entry<Class<?>, List<Property>> entry : fgTypeToProperty.entrySet()) {
			if (entry.getKey().isAssignableFrom(fJavaElement.getClass())) {
				for (Property property : entry.getValue()) {
					result.add(property.getDescriptor());
				}
			}
		}
		return result.toArray(new IPropertyDescriptor[result.size()]);
	}
	
	public Object getPropertyValue(Object id) {
		Property property= fgIdToProperty.get(id);
		if (property == null) {
			return null;
		} else {
			try {
				return property.compute(fJavaElement);
			} catch (JavaModelException e) {
				if (e.isDoesNotExist()) {
					return "JavaModelException: " + e.getLocalizedMessage();
				} else {
					JEViewPlugin.log("error calculating property '" + property.getType().getSimpleName() + '#' + property.getName() + '\'', e);
					return "Error: " + e.getLocalizedMessage();
				}
			}
		}
	}

	static String getElementTypeString(int elementType) {
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
	
	static String getSourceRangeString(ISourceRange range) {
		return range == null ? "null" : range.getOffset() + " + " + range.getLength();
	}

	static String getFlagsString(int flags, Class<? extends IJavaElement> clazz) {
		StringBuffer sb = new StringBuffer().append("0x").append(Integer.toHexString(flags)).append(" (");
		int prologLen= sb.length();
		int rest= flags;
		
		rest&= ~ appendFlag(sb, flags, Flags.AccPublic, "public ");
		rest&= ~ appendFlag(sb, flags, Flags.AccPrivate, "private ");
		rest&= ~ appendFlag(sb, flags, Flags.AccProtected, "protected ");
		rest&= ~ appendFlag(sb, flags, Flags.AccStatic, "static ");
		rest&= ~ appendFlag(sb, flags, Flags.AccFinal, "final ");
		if (IMethod.class.isAssignableFrom(clazz)) {
			rest&= ~ appendFlag(sb, flags, Flags.AccSynchronized, "synchronized ");
			rest&= ~ appendFlag(sb, flags, Flags.AccBridge, "bridge ");
			rest&= ~ appendFlag(sb, flags, Flags.AccVarargs, "varargs ");
		} else {
			rest&= ~ appendFlag(sb, flags, Flags.AccSuper, "super ");
			rest&= ~ appendFlag(sb, flags, Flags.AccVolatile, "volatile ");
			rest&= ~ appendFlag(sb, flags, Flags.AccTransient, "transient ");
		}
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
		if (len != prologLen)
			sb.setLength(len - 1);
		sb.append(")");
		return sb.toString();
	}
	
	private static int appendFlag(StringBuffer sb, int flags, int flag, String name) {
		if ((flags & flag) != 0) {
			sb.append(name);
			return flag;
		} else {
			return 0;
		}
	}

	static String getPFRKindString(int kind) {
		StringBuffer sb = new StringBuffer().append("0x").append(Integer.toHexString(kind)).append(" (");
		int prologLen= sb.length();
		int rest= kind;
		
		rest&= ~ appendFlag(sb, kind, IPackageFragmentRoot.K_BINARY, "binary ");
		rest&= ~ appendFlag(sb, kind, IPackageFragmentRoot.K_SOURCE, "source ");
		
		if (rest != 0)
			sb.append("unknown:0x").append(Integer.toHexString(rest));
		int len = sb.length();
		if (len != prologLen)
			sb.setLength(len - 1);
		sb.append(")");
		return sb.toString();
	}
	
	static String getSchedulingRuleString(ISchedulingRule schedulingRule) {
		if (schedulingRule == null)
			return null;
		else
			return schedulingRule.getClass().getSimpleName() + ": " + schedulingRule.toString();
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
