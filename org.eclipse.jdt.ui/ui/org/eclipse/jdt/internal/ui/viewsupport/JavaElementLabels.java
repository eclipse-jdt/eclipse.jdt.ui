package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

public class JavaElementLabels {
	
	/**
	 * Method names contain parameter types.
	 * e.g. <code>foo(int)</code>
	 */
	public final static int M_PARAMETER_TYPES= 1 << 0;
	
	/**
	 * Method names contain parameter names.
	 * e.g. <code>foo(index)</code>
	 */
	public final static int M_PARAMETER_NAMES= 1 << 1;	
	
	/**
	 * Method names contain thrown exceptions.
	 * e.g. <code>foo throws IOException</code>
	 */
	public final static int M_EXCEPTIONS= 1 << 2;
	
	/**
	 * Method names contain return type (appended)
	 * e.g. <code>foo int</code>
	 */
	public final static int M_APP_RETURNTYPE= 1 << 3;
	
	/**
	 * Method names contain return type (appended)
	 * e.g. <code>int foo</code>
	 */
	public final static int M_PRE_RETURNTYPE= 1 << 4;	

	/**
	 * Method names are fully qualified.
	 * e.g. <code>java.util.Vector.size</code>
	 */
	public final static int M_FULLY_QUALIFIED= 1 << 5;
	
	/**
	 * Method names are post qualified.
	 * e.g. <code>size - java.util.Vector</code>
	 */
	public final static int M_POST_QUALIFIED= 1 << 6;
	
	/**
	 * Initializer names are fully qualified.
	 * e.g. <code>java.util.Vector.{ ... }</code>
	 */
	public final static int I_FULLY_QUALIFIED= 1 << 7;
	
	/**
	 * Type names are post qualified.
	 * e.g. <code>{ ... } - java.util.Map</code>
	 */
	public final static int I_POST_QUALIFIED= 1 << 8;		
	
	/**
	 * Field names contain the declared type (appended)
	 * e.g. <code>int fHello</code>
	 */
	public final static int F_APP_TYPE_SIGNATURE= 1 << 9;
	
	/**
	 * Field names contain the declared type (prepended)
	 * e.g. <code>fHello:int</code>
	 */
	public final static int F_PRE_TYPE_SIGNATURE= 1 << 10;	

	/**
	 * Fields names are fully qualified.
	 * e.g. <code>java.lang.System.out</code>
	 */
	public final static int F_FULLY_QUALIFIED= 1 << 11;
	
	/**
	 * Fields names are post qualified.
	 * e.g. <code>out - java.lang.System</code>
	 */
	public final static int F_POST_QUALIFIED= 1 << 12;	
	
	/**
	 * Type names are fully qualified.
	 * e.g. <code>java.util.Map.MapEntry</code>
	 */
	public final static int T_FULLY_QUALIFIED= 1 << 13;
	
	/**
	 * Type names are type container qualified.
	 * e.g. <code>Map.MapEntry</code>
	 */
	public final static int T_CONTAINER_QUALIFIED= 1 << 14;
	
	/**
	 * Type names are post qualified.
	 * e.g. <code>MapEntry - java.util.Map</code>
	 */
	public final static int T_POST_QUALIFIED= 1 << 15;
	
	/**
	 * Declarations (import container / declarartion, package declarartion) are qualified.
	 * e.g. <code>java.util.Vector.class/import container</code>
	 */	
	public final static int D_QUALIFIED= 1 << 16;
	
	/**
	 * Declarations (import container / declarartion, package declarartion) are post qualified.
	 * e.g. <code>import container - java.util.Vector.class</code>
	 */	
	public final static int D_POST_QUALIFIED= 1 << 17;	

	/**
	 * Class file names are fully qualified.
	 * e.g. <code>java.util.Vector.class</code>
	 */	
	public final static int CF_QUALIFIED= 1 << 18;
	
	/**
	 * Class file names are fully qualified.
	 * e.g. <code>java.util.Vector.class</code>
	 */	
	public final static int CF_POST_QUALIFIED= 1 << 19;
	
	/**
	 * Compilation unit names are fully qualified.
	 * e.g. <code>java.util.Vector.java</code>
	 */	
	public final static int CU_QUALIFIED= 1 << 20;
	
	/**
	 * Compilation unit names are fully qualified.
	 * e.g. <code>java.util.Vector.java</code>
	 */	
	public final static int CU_POST_QUALIFIED= 1 << 21;

	/**
	 * Package names are post qualified.
	 * e.g. <code>MyProject/src/java.util</code>
	 */	
	public final static int P_QUALIFIED= 1 << 22;
	
	/**
	 * Package names are post qualified.
	 * e.g. <code>java.util - MyProject/src</code>
	 */	
	public final static int P_POST_QUALIFIED= 1 << 23;
	
	/**
	 * Package Fragment Roots contain variable name if from a variable.
	 * e.g. <code>JRE_LIB - c:\java\lib\rt.jar</code>
	 */
	public final static int ROOT_VARIABLE= 1 << 24;
	
	/**
	 * Package Fragment Roots contain the project name if not an archive (prepended).
	 * e.g. <code>MyProject/src</code>
	 */
	public final static int ROOT_QUALIFIED= 1 << 25;
	
	/**
	 * Package Fragment Roots contain the project name if not an archive (appended).
	 * e.g. <code>src - MyProject</code>
	 */
	public final static int ROOT_POST_QUALIFIED= 1 << 26;	
	
	/**
	 * Add root path to all elements except roots and Java projects.
	 * e.g. <code>java.lang.Vector - c:\java\lib\rt.jar</code>
	 * Option only applies to getElementLabel
	 */
	public final static int APPEND_ROOT_PATH= 1 << 27;
	
	
	public final static int ALL_FULLY_QUALIFIED= F_FULLY_QUALIFIED | M_FULLY_QUALIFIED | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED | P_QUALIFIED | ROOT_QUALIFIED;
	public final static int ALL_POST_QUALIFIED= F_POST_QUALIFIED | M_POST_QUALIFIED | I_POST_QUALIFIED | T_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED | CU_POST_QUALIFIED | P_POST_QUALIFIED | ROOT_POST_QUALIFIED;
	public final static int ALL_DEFAULT= M_PARAMETER_TYPES;
	public final static int DEFAULT_QUALIFIED= F_FULLY_QUALIFIED | M_FULLY_QUALIFIED | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED;
	public final static int DEFAULT_POST_QUALIFIED= F_POST_QUALIFIED | M_POST_QUALIFIED | I_POST_QUALIFIED | T_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED | CU_POST_QUALIFIED;




	private final static String CONCAT_STRING= JavaUIMessages.getString("JavaElementLabels.concat_string"); // " - ";
	private final static String COMMA_STRING= JavaUIMessages.getString("JavaElementLabels.comma_string"); // ", ";
	private final static String SPACER_STRING= JavaUIMessages.getString("JavaElementLabels.spacer_string"); // "  "; // use for return type

	private JavaElementLabels() {
	}

	private static boolean getFlag(int flags, int flag) {
		return (flags & flag) != 0;
	}
	
	
	public static String getElementLabel(IJavaElement element, int flags) {
		StringBuffer buf= new StringBuffer();
		boolean addRootPath= getFlag(flags, APPEND_ROOT_PATH);
		switch (element.getElementType()) {
			case IJavaElement.METHOD:
				getMethodLabel((IMethod) element, flags, buf);
				break;
			case IJavaElement.FIELD: 
				getFieldLabel((IField) element, flags, buf);
				break;
			case IJavaElement.INITIALIZER:
				getInitializerLabel((IInitializer) element, flags, buf);
				break;				
			case IJavaElement.TYPE: 
				getTypeLabel((IType) element, flags, buf);
				break;
			case IJavaElement.CLASS_FILE: 
				getClassFileLabel((IClassFile) element, flags, buf);
				break;					
			case IJavaElement.COMPILATION_UNIT: 
				getCompilationUnitLabel((ICompilationUnit) element, flags, buf);
				break;	
			case IJavaElement.PACKAGE_FRAGMENT: 
				getPackageFragmentLabel((IPackageFragment) element, flags, buf);
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT: 
				getPackageFragmentRootLabel((IPackageFragmentRoot) element, flags, buf);
				addRootPath= false;
				break;
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.IMPORT_DECLARATION:
			case IJavaElement.PACKAGE_DECLARATION:
				getDeclararionLabel(element, flags, buf);
				break;
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.JAVA_MODEL:
				addRootPath= false;
				buf.append(element.getElementName());
				break;
			default:
				buf.append(element.getElementName());
		}
		if (addRootPath) {
			IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
			if (root != null) {
				buf.append(CONCAT_STRING);
				getPackageFragmentRootLabel(root, ROOT_QUALIFIED, buf);
			}
		}
	
		return buf.toString();
	}
		
	public static void getMethodLabel(IMethod method, int flags, StringBuffer buf) {
		try {
			boolean isConstructor= method.isConstructor();
			
			// return type
			if (getFlag(flags, M_PRE_RETURNTYPE) && !isConstructor) {
				buf.append(Signature.getSimpleName(Signature.toString(method.getReturnType())));
				buf.append(' ');
			}

			// qualification
			if (getFlag(flags, M_FULLY_QUALIFIED)) {
				getTypeLabel(method.getDeclaringType(), T_FULLY_QUALIFIED, buf);
				buf.append('.');
			}
				
			buf.append(method.getElementName());
			
			// parameters
			if (getFlag(flags, M_PARAMETER_TYPES | M_PARAMETER_NAMES)) {
				buf.append('(');
				
				String[] types= getFlag(flags, M_PARAMETER_TYPES) ? method.getParameterTypes() : null;
				String[] names= getFlag(flags, M_PARAMETER_NAMES) ? method.getParameterNames() : null;
				int nParams= types != null ? types.length : names.length;
				
				for (int i= 0; i < nParams; i++) {
					if (i > 0) {
						buf.append(COMMA_STRING); //$NON-NLS-1$
					}
					if (types != null) {
						buf.append(Signature.getSimpleName(Signature.toString(types[i])));
					}
					if (names != null) {
						if (types != null) {
							buf.append(' ');
						}
						buf.append(names[i]);
					}
				}
				buf.append(')');
			}
					
			if (getFlag(flags, M_EXCEPTIONS)) {
				String[] types= method.getExceptionTypes();
				if (types.length > 0) {
					buf.append(" throws "); //$NON-NLS-1$
					for (int i= 0; i < types.length; i++) {
						if (i > 0) {
							buf.append(COMMA_STRING); //$NON-NLS-1$
						}
						buf.append(Signature.getSimpleName(Signature.toString(types[i])));
					}
				}
			}
			
			if (getFlag(flags, M_APP_RETURNTYPE) && !isConstructor) {
				buf.append(SPACER_STRING);
				buf.append(Signature.getSimpleName(Signature.toString(method.getReturnType())));	
			}			
			
			// post qualification
			if (getFlag(flags, M_POST_QUALIFIED)) {
				buf.append(CONCAT_STRING);
				getTypeLabel(method.getDeclaringType(), T_FULLY_QUALIFIED, buf);
			}			
			
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

	public static void getFieldLabel(IField field, int flags, StringBuffer buf) {
		try {
			if (getFlag(flags, F_PRE_TYPE_SIGNATURE)) {
				buf.append(Signature.toString(field.getTypeSignature()));
				buf.append(' '); //$NON-NLS-1$
			}
			
			// qualification
			if (getFlag(flags, F_FULLY_QUALIFIED)) {
				getTypeLabel(field.getDeclaringType(), T_FULLY_QUALIFIED, buf);
				buf.append('.');
			}
			buf.append(field.getElementName());
			
			if (getFlag(flags, F_APP_TYPE_SIGNATURE)) {
				buf.append(SPACER_STRING); //$NON-NLS-1$
				buf.append(Signature.toString(field.getTypeSignature()));
			}
			
			// post qualification
			if (getFlag(flags, F_POST_QUALIFIED)) {
				buf.append(CONCAT_STRING);
				getTypeLabel(field.getDeclaringType(), T_FULLY_QUALIFIED, buf);
			}
			
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}			
	}

	public static void getInitializerLabel(IInitializer initializer, int flags, StringBuffer buf) {
		// qualification
		if (getFlag(flags, I_FULLY_QUALIFIED)) {
			getTypeLabel(initializer.getDeclaringType(), T_FULLY_QUALIFIED, buf);
			buf.append('.');
		}
		buf.append(JavaUIMessages.getString("JavaElementLabels.initializer")); //$NON-NLS-1$

		// post qualification
		if (getFlag(flags, I_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getTypeLabel(initializer.getDeclaringType(), T_FULLY_QUALIFIED, buf);
		}
	}

	
	public static void getTypeLabel(IType type, int flags, StringBuffer buf) {
		if (getFlag(flags, T_FULLY_QUALIFIED)) {
			buf.append(JavaModelUtil.getFullyQualifiedName(type));
		} else if (getFlag(flags, T_CONTAINER_QUALIFIED)) {
			buf.append(JavaModelUtil.getTypeQualifiedName(type));
		} else {
			buf.append(type.getElementName());
		}
		// post qualification
		if (getFlag(flags, T_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				buf.append(JavaModelUtil.getFullyQualifiedName(declaringType));
			} else {
				getPackageFragmentLabel(type.getPackageFragment(), 0, buf);
			}
		}
	}
	
	public static void getDeclararionLabel(IJavaElement declaration, int flags, StringBuffer buf) {
		if (getFlag(flags, D_QUALIFIED)) {
			IJavaElement openable= (IJavaElement) JavaModelUtil.getOpenable(declaration);
			if (openable != null) {
				buf.append(getElementLabel(openable, CF_QUALIFIED | CU_QUALIFIED));
				buf.append('/');
			}	
		}
		if (declaration.getElementType() == IJavaElement.IMPORT_CONTAINER) {
			buf.append(JavaUIMessages.getString("JavaElementLabels.import_container")); //$NON-NLS-1$
		} else {
			buf.append(declaration.getElementName());
		}
		// post qualification
		if (getFlag(flags, D_POST_QUALIFIED)) {
			IJavaElement openable= (IJavaElement) JavaModelUtil.getOpenable(declaration);
			if (openable != null) {
				buf.append(CONCAT_STRING);
				buf.append(getElementLabel(openable, CF_QUALIFIED | CU_QUALIFIED));
			}				
		}
	}	
	

	public static void getClassFileLabel(IClassFile classFile, int flags, StringBuffer buf) {
		if (getFlag(flags, CF_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) classFile.getParent();
			if (!pack.isDefaultPackage()) {
				buf.append(pack.getElementName());
				buf.append('.');
			}
		}
		buf.append(classFile.getElementName());
		
		if (getFlag(flags, CF_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) classFile.getParent(), 0, buf);
		}
	}

	public static void getCompilationUnitLabel(ICompilationUnit cu, int flags, StringBuffer buf) {
		if (getFlag(flags, CU_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			if (!pack.isDefaultPackage()) {
				buf.append(pack.getElementName());
				buf.append('.');
			}
		}
		buf.append(cu.getElementName());
		
		if (getFlag(flags, CU_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) cu.getParent(), 0, buf);
		}		
	}
	
	public static void getPackageFragmentLabel(IPackageFragment pack, int flags, StringBuffer buf) {
		if (getFlag(flags, P_QUALIFIED)) {
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), ROOT_QUALIFIED, buf);
			buf.append('/');
		}
		if (pack.isDefaultPackage()) {
			buf.append(JavaUIMessages.getString("JavaElementLabels.default_package")); //$NON-NLS-1$
		} else {
			buf.append(pack.getElementName());
		}
		if (getFlag(flags, P_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), ROOT_QUALIFIED, buf);
		}
	}

	public static void getPackageFragmentRootLabel(IPackageFragmentRoot root, int flags, StringBuffer buf) {
		String name= root.getElementName();
		if (root.isArchive() && getFlag(flags, ROOT_VARIABLE)) {
			try {
				IClasspathEntry rawEntry= JavaModelUtil.getRawClasspathEntry(root);
				if (rawEntry != null) {
					if (rawEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
						buf.append(rawEntry.getPath().makeRelative());
						buf.append(CONCAT_STRING);
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		if (root.isExternal()) {
			// external jars have path == name
			// no qualification for roots
			buf.append(root.getPath().toOSString());
		} else {
			if (getFlag(flags, ROOT_QUALIFIED)) {
				buf.append(root.getPath().makeRelative().toString());
			} else {
				buf.append(root.getElementName());
			}
			if (getFlag(flags, ROOT_POST_QUALIFIED)) {
				buf.append(CONCAT_STRING);
				buf.append(root.getParent().getElementName());
			}
		}		
	}	
	


}

