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
	
	private static final QualifiedName QUALIFIED_NAME= new QualifiedName(JavaUI.ID_PLUGIN, "jdoclocation");
	
	private static final String NODE_ROOT= "javadoclocation";
	private static final String NODE_ENTRY= "location_01";
	private static final String NODE_PATH= "path";
	private static final String NODE_URL= "url";
	
	private static Map fgJavadocLocations= new HashMap(5);
	
	
	/**
	 * Gets the Javadoc location for an archive with the given path.
	 */
	public static URL getJavadocLocation(IPath path) {
		return (URL) fgJavadocLocations.get(path);
	}

	/**
	 * Sets the Javadoc location for an archive with the given path.
	 */
	public static void setJavadocLocation(IPath path, URL url) {
		if (url == null) {
			fgJavadocLocations.remove(url);
		} else {
			fgJavadocLocations.put(path, url);
		}
	}	
		
	private static String getLocationsAsXMLString() throws CoreException {
		Document document = new DocumentImpl();
		Element rootElement = document.createElement(NODE_ROOT);
		document.appendChild(rootElement);

		Iterator iter= fgJavadocLocations.keySet().iterator();
		
		while (iter.hasNext()) {
			IPath path= (IPath) iter.next();
			URL url= getJavadocLocation(path);
		
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
			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, "Error", e);
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
					setJavadocLocation(new Path(varPath), new URL(varURL));
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
				IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, "Error", e);
				throw new CoreException(status);
			}
		}
	}
	
	public static URL getJavaDocLocation(IJavaElement element) throws CoreException {
		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root == null) {
			return null;
		}
		URL baseLocation= getJavadocLocation(root.getPath());
		if (baseLocation == null) {
			return null;
		}
		try {
			String urlString= baseLocation.toExternalForm();
			
			StringBuffer pathBuffer= new StringBuffer(urlString);
			if (!urlString.endsWith("/")) {
				pathBuffer.append('/');
			}
			
			switch (element.getElementType()) {
				case IJavaElement.PACKAGE_FRAGMENT:
					return getPackageLocation((IPackageFragment) element, pathBuffer);
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					return getOverviewLocation(pathBuffer);
				case IJavaElement.IMPORT_CONTAINER:
					element= element.getParent();
					// fall through
				case IJavaElement.COMPILATION_UNIT:
					IType mainType= JavaModelUtil.findPrimaryType((ICompilationUnit) element);
					if (mainType == null) {
						return null;
					}
					return getTypeLocation(mainType, pathBuffer);
				case IJavaElement.CLASS_FILE:
					return getTypeLocation(((IClassFile) element).getType(), pathBuffer);				
				case IJavaElement.TYPE:
					return getTypeLocation((IType) element, pathBuffer);
				case IJavaElement.FIELD:
					return getFieldLocation((IField) element, pathBuffer);		
				case IJavaElement.METHOD:
					return getMethodLocation((IMethod) element, pathBuffer);
				case IJavaElement.INITIALIZER:
					return getTypeLocation(((IMember) element).getDeclaringType(), pathBuffer);			
				case IJavaElement.IMPORT_DECLARATION:
					IImportDeclaration decl= (IImportDeclaration) element;
					
					if (decl.isOnDemand()) {
						IJavaElement cont= JavaModelUtil.findTypeContainer(element.getJavaProject(), Signature.getQualifier(decl.getElementName()));
						if (cont instanceof IType) {
							return getTypeLocation((IType) cont, pathBuffer);
						} else if (cont instanceof IPackageFragment) {
							return getPackageLocation((IPackageFragment) cont, pathBuffer);
						}
						return null;
					} else {
						IType imp= JavaModelUtil.findType(element.getJavaProject(), decl.getElementName());
						return getTypeLocation((IType) imp, pathBuffer);
					}
				case IJavaElement.PACKAGE_DECLARATION:
					IPackageFragment pack= (IPackageFragment) JavaModelUtil.findElementOfKind(element, IJavaElement.PACKAGE_FRAGMENT);
					if (pack != null) {
						return getPackageLocation((IPackageFragment) pack, pathBuffer);
					}
					return null;	
				default:
					return null;
			}
		} catch (MalformedURLException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	
	
	private static URL getTypeLocation(IType type, StringBuffer buf) throws MalformedURLException {
		appendTypePath(type, buf);
		URL url= new URL(buf.toString());
		if (doesExist(url)) {
			return url;
		}
		return null;
	}
	
	private static URL getPackageLocation(IPackageFragment pack, StringBuffer buf) throws MalformedURLException {
		String packPath= pack.getElementName().replace('.', '/');
		buf.append(packPath);
		buf.append("/package-summary.html");
		URL url= new URL(buf.toString());
		if (doesExist(url)) {
			return url;
		}
		return null;
	}
	
	private static URL getOverviewLocation(StringBuffer buf) throws MalformedURLException {
		buf.append("overview-summary.html");
		URL url= new URL(buf.toString());
		if (doesExist(url)) {
			return url;
		}
		return null;		
	}	
	
	private static boolean doesExist(URL url) {
		if (url.getProtocol().equals("file")) {
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
		buf.append(".html");
	}		
		
	private static URL getFieldLocation(IField field, StringBuffer buf) throws JavaModelException, MalformedURLException {
		URL typeURL= getTypeLocation(field.getDeclaringType(), buf);
		if (typeURL == null) {
			return null;
		}
		buf.append('#');
		buf.append(field.getElementName());
		return new URL(buf.toString());
	}
	
	private static URL getMethodLocation(IMethod meth, StringBuffer buf) throws JavaModelException, MalformedURLException {
		IType declaringType= meth.getDeclaringType();
		
		URL typeURL= getTypeLocation(declaringType, buf);
		if (typeURL == null) {
			return null;
		}
		
		buf.append('#');
		buf.append(meth.getElementName());	
		
		buf.append('(');
		String[] params= meth.getParameterTypes();
		for (int i= 0; i < params.length; i++) {
			if (i != 0) {
				buf.append(",%20");
			}
			String curr= params[i];
			String fullName= JavaModelUtil.getResolvedTypeName(curr, declaringType);
			if (fullName != null) {
				buf.append(fullName);
				int dim= Signature.getArrayCount(curr);
				while (dim > 0) {
					buf.append("[]");
					dim--;
				}
			}
		}
		buf.append(')');
		
		return new URL(buf.toString());
	}
}
