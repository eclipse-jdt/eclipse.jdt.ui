package org.eclipse.jdt.internal.corext.javadoc;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavaDocLocations {
	
	private static final QualifiedName QUALIFIED_NAME= new QualifiedName(JavaUI.ID_PLUGIN, "jdoclocation"); //$NON-NLS-1$
	
	private static final String NODE_ROOT= "javadoclocation"; //$NON-NLS-1$
	private static final String NODE_ENTRY= "location_01"; //$NON-NLS-1$
	private static final String NODE_PATH= "path"; //$NON-NLS-1$
	private static final String NODE_URL= "url"; //$NON-NLS-1$
	
	private static final boolean IS_CASE_SENSITIVE = !new File("Temp").equals(new File("temp")); //$NON-NLS-1$ //$NON-NLS-2$

	
	private static Map fgJavadocLocations= new HashMap(5);
	

	private static IPath canonicalizedPath(IPath externalPath) {
		if (externalPath == null || IS_CASE_SENSITIVE)
			return externalPath;

		if (ResourcesPlugin.getWorkspace().getRoot().findMember(externalPath) != null) {
			return externalPath;
		}

		try {
			return new Path(externalPath.toFile().getCanonicalPath());
		} catch (IOException e) {
		}
		return externalPath;
	}

	private static void setJavadocBaseLocation(IPath path, URL url) {
		if (url == null) {
			fgJavadocLocations.remove(path);
		} else {
			fgJavadocLocations.put(path, url);
		}
	}
	
	/**
	 * Gets the Javadoc location for an archive with the given path.
	 */
	private static URL getJavadocBaseLocation(IPath path) {
		return (URL) fgJavadocLocations.get(path);
	}		
	
	/**
	 * Sets the Javadoc location for an archive with the given path.
	 */
	public static void setLibraryJavadocLocation(IPath archivePath, URL url) {
		setJavadocBaseLocation(canonicalizedPath(archivePath), url);
	}
	
	/**
	 * Sets the Javadoc location for an archive with the given path.
	 */
	public static void setProjectJavadocLocation(IJavaProject project, URL url) {
		setJavadocBaseLocation(project.getProject().getFullPath(), url);
	}
	
	public static URL getProjectJavadocLocation(IJavaProject project) {
		return getJavadocBaseLocation(project.getProject().getFullPath());
	}

	public static URL getLibraryJavadocLocation(IPath archivePath) {
		return getJavadocBaseLocation(canonicalizedPath(archivePath));
	}

	public static URL getJavadocBaseLocation(IJavaElement element) throws JavaModelException {	
		if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
			return getProjectJavadocLocation((IJavaProject) element);
		}
		
		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root == null) {
			return null;
		}

		if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
			return getLibraryJavadocLocation(root.getPath());
		} else {
			return getProjectJavadocLocation(root.getJavaProject());
		}	
	}
		
	private static String getLocationsAsXMLString() throws CoreException {
		Document document = new DocumentImpl();
		Element rootElement = document.createElement(NODE_ROOT);
		document.appendChild(rootElement);

		Iterator iter= fgJavadocLocations.keySet().iterator();
		
		while (iter.hasNext()) {
			IPath path= (IPath) iter.next();
			URL url= getJavadocBaseLocation(path);
		
			Element varElement= document.createElement(NODE_ENTRY);
			varElement.setAttribute(NODE_PATH, path.toString());
			varElement.setAttribute(NODE_URL, url.toExternalForm());
			rootElement.appendChild(varElement);
		}

		// produce a String output
		StringWriter writer = new StringWriter();
		try {
			OutputFormat format = new OutputFormat();
			format.setIndenting(true);
			Serializer serializer = SerializerFactory.getSerializerFactory(Method.XML).makeSerializer(writer, format);
			serializer.asDOMSerializer().serialize(document);
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, "Error", e); //$NON-NLS-1$
			throw new CoreException(status);			
		}
		return writer.toString();			
	}
	
	private static void readVariables(String xmlString) throws IOException {
		StringReader reader = new StringReader(xmlString);
		Element cpElement;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			cpElement = parser.parse(new InputSource(reader)).getDocumentElement();
		} catch (SAXException e) {
			return;
		} catch (ParserConfigurationException e) {
			return;
		} finally {
			reader.close();
		}
		if (cpElement == null) return;
		if (!cpElement.getNodeName().equalsIgnoreCase(NODE_ROOT)) {
			return;
		}
		NodeList list= cpElement.getChildNodes();
		int length= list.getLength();
		for (int i= 0; i < length; ++i) {
			Node node= list.item(i);
			short type= node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element element= (Element) node;
				if (element.getNodeName().equalsIgnoreCase(NODE_ENTRY)) {
					String varPath = element.getAttribute(NODE_PATH);
					String varURL = element.getAttribute(NODE_URL);
					setJavadocBaseLocation(new Path(varPath), new URL(varURL));
				}
			}
		}
	}	
	
	
	public static void saveJavadocLocations() throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		String xmlString= getLocationsAsXMLString();
		root.setPersistentProperty(QUALIFIED_NAME, xmlString);
	}
	
	public static void loadJavadocLocations() throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		String xmlString= root.getPersistentProperty(QUALIFIED_NAME); 
		if (xmlString != null) {
			try {
				readVariables(xmlString);
			} catch (IOException e) {
				IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, "Error", e); //$NON-NLS-1$
				throw new CoreException(status);
			}
		}
	}
	
	public static URL getJavadocLocation(IJavaElement element, boolean includeMemberReference) throws JavaModelException {
		URL baseLocation= getJavadocBaseLocation(element);
		if (baseLocation == null) {
			return null;
		}

		String urlString= baseLocation.toExternalForm();

		StringBuffer pathBuffer= new StringBuffer(urlString);
		if (!urlString.endsWith("/")) { //$NON-NLS-1$
			pathBuffer.append('/');
		}

		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				appendPackageSummaryPath((IPackageFragment) element, pathBuffer);
				break;
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.PACKAGE_FRAGMENT_ROOT :
				appendIndexPath(pathBuffer);
				break;
			case IJavaElement.IMPORT_CONTAINER :
				element= element.getParent();
				// fall through
			case IJavaElement.COMPILATION_UNIT :
				IType mainType= ((ICompilationUnit) element).findPrimaryType();
				if (mainType == null) {
					return null;
				}
				appendTypePath(mainType, pathBuffer);
				break;
			case IJavaElement.CLASS_FILE :
				appendTypePath(((IClassFile) element).getType(), pathBuffer);
				break;
			case IJavaElement.TYPE :
				appendTypePath((IType) element, pathBuffer);
				break;
			case IJavaElement.FIELD :
				IField field= (IField) element;
				appendTypePath(field.getDeclaringType(), pathBuffer);
				if (includeMemberReference) {
					appendFieldReference(field, pathBuffer);
				}
				break;
			case IJavaElement.METHOD :
				IMethod method= (IMethod) element;
				appendTypePath(method.getDeclaringType(), pathBuffer);
				if (includeMemberReference) {
					appendMethodReference(method, pathBuffer);
				}
				break;
			case IJavaElement.INITIALIZER :
				appendTypePath(((IMember) element).getDeclaringType(), pathBuffer);
				break;
			case IJavaElement.IMPORT_DECLARATION :
				IImportDeclaration decl= (IImportDeclaration) element;

				if (decl.isOnDemand()) {
					IJavaElement cont= JavaModelUtil.findTypeContainer(element.getJavaProject(), Signature.getQualifier(decl.getElementName()));
					if (cont instanceof IType) {
						appendTypePath((IType) cont, pathBuffer);
					} else if (cont instanceof IPackageFragment) {
						appendPackageSummaryPath((IPackageFragment) cont, pathBuffer);
					}
				} else {
					IType imp= element.getJavaProject().findType(decl.getElementName());
					appendTypePath((IType) imp, pathBuffer);
				}
				break;
			case IJavaElement.PACKAGE_DECLARATION :
				IJavaElement pack= element.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				if (pack != null) {
					appendPackageSummaryPath((IPackageFragment) pack, pathBuffer);
				} else {
					return null;
				}
				break;
			default :
				return null;
		}

		try {
			return new URL(pathBuffer.toString());
		} catch (MalformedURLException e) {
			JavaPlugin.log(e);
		}
		return null;
	}	
		
	private static void appendPackageSummaryPath(IPackageFragment pack, StringBuffer buf) {
		String packPath= pack.getElementName().replace('.', '/');
		buf.append(packPath);
		buf.append("/package-summary.html"); //$NON-NLS-1$
	}
	
	private static void appendIndexPath(StringBuffer buf) {
		buf.append("index.html"); //$NON-NLS-1$
	}	
	
	private static boolean doesExist(URL url) {
		if (url.getProtocol().equals("file")) { //$NON-NLS-1$
			File file= new File(url.getFile());
			return file.isFile();
		}	
		return true;
	}
	
	
	private static void appendTypePath(IType type, StringBuffer buf) {
		IPackageFragment pack= type.getPackageFragment();
		String packPath= pack.getElementName().replace('.', '/');
		String typePath= JavaModelUtil.getTypeQualifiedName(type);
		buf.append(packPath);
		buf.append('/');
		buf.append(typePath);
		buf.append(".html"); //$NON-NLS-1$
	}		
		
	private static void appendFieldReference(IField field, StringBuffer buf) throws JavaModelException {
		buf.append('#');
		buf.append(field.getElementName());
	}
	
	private static void appendMethodReference(IMethod meth, StringBuffer buf) throws JavaModelException {
		buf.append('#');
		buf.append(meth.getElementName());	
		
		buf.append('(');
		String[] params= meth.getParameterTypes();
		IType declaringType= meth.getDeclaringType();
		for (int i= 0; i < params.length; i++) {
			if (i != 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			String curr= params[i];
			String fullName= JavaModelUtil.getResolvedTypeName(curr, declaringType);
			if (fullName != null) {
				buf.append(fullName);
				int dim= Signature.getArrayCount(curr);
				while (dim > 0) {
					buf.append("[]"); //$NON-NLS-1$
					dim--;
				}
			}
		}
		buf.append(')');
	}
}
