/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Links inside Javadoc hovers and Javadoc view.
 *
 * @since 3.4
 */
public class JavaElementLinks {

	/**
	 * A handler is asked to handle links to targets.
	 *
	 * @see JavaElementLinks#createLocationListener(JavaElementLinks.ILinkHandler)
	 */
	public interface ILinkHandler {

		/**
		 * Handle normal kind of link to given target.
		 *
		 * @param target the target to show
		 */
		void handleInlineJavadocLink(IJavaElement target);

		/**
		 * Handle link to given target to open in javadoc view.
		 *
		 * @param target the target to show
		 */
		void handleJavadocViewLink(IJavaElement target);

		/**
		 * Handle link to given target to open its declaration
		 *
		 * @param target the target to show
		 */
		void handleDeclarationLink(IJavaElement target);

		/**
		 * Handle link to given link to open in external browser
		 *
		 * @param url the url to show
		 * @param display the current display
		 * @return <code>true</code> if the handler could open the link
		 *         <code>false</code> if the browser should follow the link
		 */
		boolean handleExternalLink(URL url, Display display);

		/**
		 * Informs the handler that the text of the browser was set.
		 */
		void handleTextSet();
	}

	private static final class JavaElementLinkedLabelComposer extends JavaElementLabelComposer {
		private final IJavaElement fElement;

		public JavaElementLinkedLabelComposer(IJavaElement member, StringBuffer buf) {
			super(buf);
			fElement= member;
		}

		public String getElementName(IJavaElement element) {
			String elementName= element.getElementName();
			if (fElement.equals(element)) { // linking to the member itself would be a no-op
				return elementName;
			}
			if (elementName.length() == 0) { // anonymous
				return elementName;
			}
			try {
				String uri= createURI(JAVADOC_SCHEME, element);
				return createLink(uri, elementName);
			} catch (URISyntaxException e) {
				JavaPlugin.log(e);
				return elementName;
			}
		}

		private String createLink(String uri, String elementName) {
			return "<a class='header' href='" + uri + ("'>" + elementName + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		protected String getGT() {
			return "&gt;"; //$NON-NLS-1$
		}

		protected String getLT() {
			return "&lt;"; //$NON-NLS-1$
		}

		protected String getSimpleTypeName(IJavaElement enclosingElement, String typeSig) {
			String typeName= super.getSimpleTypeName(enclosingElement, typeSig);
			try {
				String uri= createURI(JAVADOC_SCHEME, enclosingElement, typeName, null, null);
				return createLink(uri, typeName);
			} catch (URISyntaxException e) {
				JavaPlugin.log(e);
				return typeName;
			}
		}
	}

	public static final String OPEN_LINK_SCHEME= "eclipse-open"; //$NON-NLS-1$
	public static final String JAVADOC_SCHEME= "eclipse-javadoc"; //$NON-NLS-1$
	public static final String JAVADOC_VIEW_SCHEME= "eclipse-javadoc-view"; //$NON-NLS-1$
	private static final char LINK_BRACKET_REPLACEMENT= '\u2603';

	/**
	 * The link is composed of a number of segments, separated by LINK_SEPARATOR:
	 * <p>
	 * segments[0]: ""<br>
	 * segments[1]: baseElementHandle<br>
	 * segments[2]: typeName<br>
	 * segments[3]: memberName<br>
	 * segments[4...]: parameterTypeName (optional)
	 */
	private static final char LINK_SEPARATOR= '\u2602';

	private JavaElementLinks() {
		// static only
	}

	/**
	 * Creates a location listener which uses the given handler
	 * to handle java element links.
	 *
	 * The location listener can be attached to a {@link Browser}
	 *
	 * @param handler the handler to use to handle links
	 * @return a new {@link LocationListener}
	 */
	public static LocationListener createLocationListener(final ILinkHandler handler) {
		return new LocationAdapter() {
			public void changing(LocationEvent event) {
				String loc= event.location;

				if ("about:blank".equals(loc)) { //$NON-NLS-1$
					/*
					 * Using the Browser.setText API triggers a location change to "about:blank".
					 * XXX: remove this code once https://bugs.eclipse.org/bugs/show_bug.cgi?id=130314 is fixed
					 */
					//input set with setText
					handler.handleTextSet();
					return;
				}

				event.doit= false;

				if (loc.startsWith("about:")) { //$NON-NLS-1$
					// Relative links should be handled via head > base tag.
					// If no base is available, links just won't work.
					return;
				}

				URI uri;
				try {
					uri= new URI(loc);
				} catch (URISyntaxException e) {
					// try it with a file (workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=237903 ):
					File file= new File(loc);
					if (! file.exists()) {
						JavaPlugin.log(e);
						return;
					}
					uri= file.toURI();
					loc= uri.toASCIIString();
				}

				String scheme= uri.getScheme();
				if (JavaElementLinks.JAVADOC_VIEW_SCHEME.equals(scheme)) {
					IJavaElement linkTarget= JavaElementLinks.parseURI(uri);
					if (linkTarget == null)
						return;

					handler.handleJavadocViewLink(linkTarget);
				} else if (JavaElementLinks.JAVADOC_SCHEME.equals(scheme)) {
					IJavaElement linkTarget= JavaElementLinks.parseURI(uri);
					if (linkTarget == null)
						return;

					handler.handleInlineJavadocLink(linkTarget);
				} else if (JavaElementLinks.OPEN_LINK_SCHEME.equals(scheme)) {
					IJavaElement linkTarget= JavaElementLinks.parseURI(uri);
					if (linkTarget == null)
						return;

					handler.handleDeclarationLink(linkTarget);
				} else {
					try {
						if (handler.handleExternalLink(new URL(loc), event.display))
							return;

						event.doit= true;
					} catch (MalformedURLException e) {
						JavaPlugin.log(e);
					}
				}
			}
		};
	}

	/**
	 * Creates an {@link URI} with the given scheme for the given element.
	 *
	 * @param scheme the scheme
	 * @param element the element
	 * @return an {@link URI}, encoded as {@link URI#toASCIIString() ASCII} string, ready to be used
	 *         as <code>href</code> attribute in an <code>&lt;a&gt;</code> tag
	 * @throws URISyntaxException if the arguments were invalid
	 */
	public static String createURI(String scheme, IJavaElement element) throws URISyntaxException {
		return createURI(scheme, element, null, null, null);
	}

	/**
	 * Creates an {@link URI} with the given scheme based on the given element.
	 * The additional arguments specify a member referenced from the given element.
	 *
	 * @param scheme a scheme
	 * @param element the declaring element
	 * @param refTypeName a (possibly qualified) type name, can be <code>null</code>
	 * @param refMemberName a member name, can be <code>null</code>
	 * @param refParameterTypes a (possibly empty) array of (possibly qualified) parameter type
	 *            names, can be <code>null</code>
	 * @return an {@link URI}, encoded as {@link URI#toASCIIString() ASCII} string, ready to be used
	 *         as <code>href</code> attribute in an <code>&lt;a&gt;</code> tag
	 * @throws URISyntaxException if the arguments were invalid
	 */
	public static String createURI(String scheme, IJavaElement element, String refTypeName, String refMemberName, String[] refParameterTypes) throws URISyntaxException {
		/*
		 * We use an opaque URI, not ssp and fragments (to work around Safari bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=212527 (wrongly encodes #)).
		 */

		StringBuffer ssp= new StringBuffer(60);
		ssp.append(LINK_SEPARATOR); // make sure first character is not a / (would be hierarchical URI)

		// replace '[' manually, since URI confuses it for an IPv6 address as per RFC 2732:
		ssp.append(element.getHandleIdentifier().replace('[', LINK_BRACKET_REPLACEMENT)); // segments[1]

		if (refTypeName != null) {
			ssp.append(LINK_SEPARATOR);
			ssp.append(refTypeName); // segments[2]

			if (refMemberName != null) {
				ssp.append(LINK_SEPARATOR);
				ssp.append(refMemberName); // segments[3]

				if (refParameterTypes != null) {
					ssp.append(LINK_SEPARATOR);
					for (int i= 0; i < refParameterTypes.length; i++) {
						ssp.append(refParameterTypes[i]); // segments[4|5|..]
						if (i != refParameterTypes.length - 1) {
							ssp.append(LINK_SEPARATOR);
						}
					}
				}
			}
		}
		return new URI(scheme, ssp.toString(), null).toASCIIString();
	}

	public static IJavaElement parseURI(URI uri) {
		String ssp= uri.getSchemeSpecificPart();
		String[] segments= ssp.split(String.valueOf(LINK_SEPARATOR), -1);

		// replace '[' manually, since URI confuses it for an IPv6 address as per RFC 2732:
		IJavaElement element= JavaCore.create(segments[1].replace(LINK_BRACKET_REPLACEMENT, '['));

		if (segments.length > 2) {
			String refTypeName= segments[2];
			if (refTypeName.indexOf('.') == -1) {
				try {
					IJavaElement resolvedTypeVariable= resolveTypeVariable(element, refTypeName);
					if (resolvedTypeVariable != null)
						return resolvedTypeVariable;
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}

			if (element instanceof ILocalVariable) {
				element= element.getAncestor(IJavaElement.TYPE);
			} else if (element instanceof ITypeParameter) {
				element= ((ITypeParameter)element).getDeclaringMember();
			}
			if (element instanceof IMember && !(element instanceof IType)) {
				element= ((IMember) element).getDeclaringType();
			}
			if (element instanceof IType) {
				try {
					IType type= (IType) element;
					if (refTypeName.length() > 0) {
						type= resolveType(type, refTypeName);
					}
					if (type != null) {
						element= type;
						if (segments.length > 3) {
							String refMemberName= segments[3];
							if (segments.length > 4) {
								String[] paramSignatures= new String[segments[4].length() == 0 ? 0 : segments.length - 4];
								for (int i= 0; i < paramSignatures.length; i++) {
									paramSignatures[i]= Signature.createTypeSignature(segments[i + 4], false);
								}
								IMethod method= type.getMethod(refMemberName, paramSignatures);
								IMethod[] methods= type.findMethods(method);
								if (methods != null) {
									return methods[0];
								} else {
									//TODO: methods whose signature contains type parameters can not be found
									// easily, since the Javadoc references are erasures
									//TODO: reference can also point to method from supertype

									//Shortcut: only check name and parameter count:
									methods= type.getMethods();
									for (int i= 0; i < methods.length; i++) {
										method= methods[i];
										if (method.getElementName().equals(refMemberName) && method.getNumberOfParameters() == paramSignatures.length)
											return method;
									}

								}
							} else {
								IField field= type.getField(refMemberName);
								if (field.exists()) {
									return field;
								} else {
									IMethod[] methods= type.getMethods();
									for (int i= 0; i < methods.length; i++) {
										IMethod method= methods[i];
										if (method.getElementName().equals(refMemberName))
											return method;
									}
								}
							}

						}
					} else {
						// FIXME: either remove or show dialog
//						JavaPlugin.logErrorMessage("JavaElementLinks could not resolve " + uri); //$NON-NLS-1$
					}
					return type;
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return element;
	}

	private static IJavaElement resolveTypeVariable(IJavaElement baseElement, String typeVariableName) throws JavaModelException {
		while (baseElement != null) {
			switch (baseElement.getElementType()) {
				case IJavaElement.METHOD:
					IMethod method= (IMethod)baseElement;
					ITypeParameter[] typeParameters= method.getTypeParameters();
					for (int i= 0; i < typeParameters.length; i++) {
						ITypeParameter typeParameter= typeParameters[i];
						if (typeParameter.getElementName().equals(typeVariableName)) {
							return typeParameter;
						}
					}
					break;

				case IJavaElement.TYPE:
					IType type= (IType)baseElement;
					typeParameters= type.getTypeParameters();
					for (int i= 0; i < typeParameters.length; i++) {
						ITypeParameter typeParameter= typeParameters[i];
						if (typeParameter.getElementName().equals(typeVariableName)) {
							return typeParameter;
						}
					}
					break;

				case IJavaElement.JAVA_MODEL:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:

				case IJavaElement.CLASS_FILE:
				case IJavaElement.COMPILATION_UNIT:

				case IJavaElement.PACKAGE_DECLARATION:
				case IJavaElement.IMPORT_CONTAINER:
				case IJavaElement.IMPORT_DECLARATION:
					return null;

				default:
					break;
			}
			// look for type parameters in enclosing members:
			baseElement= baseElement.getParent();
		}
		return null;
	}

	private static IType resolveType(IType baseType, String refTypeName) throws JavaModelException {
		if (refTypeName.length() == 0)
			return baseType;

		String[][] resolvedNames= baseType.resolveType(refTypeName);
		if (resolvedNames != null && resolvedNames.length > 0) {
			return baseType.getJavaProject().findType(resolvedNames[0][0], resolvedNames[0][1].replace('$', '.'), (IProgressMonitor) null);

		} else if (baseType.isBinary()) {
			// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=206597
			IType type= baseType.getJavaProject().findType(refTypeName, (IProgressMonitor) null);
			if (type == null) {
				// could be unqualified reference:
				type= baseType.getJavaProject().findType(baseType.getPackageFragment().getElementName() + '.' + refTypeName, (IProgressMonitor) null);
			}
			return type;

		} else {
			return null;
		}
	}

	/**
	 * Returns the label for a Java element with the flags as defined by {@link JavaElementLabels}.
	 * Referenced element names in the label (except the given element's name) are rendered as Javadoc links.
	 *
	 * @param element the element to render
	 * @param flags the rendering flags
	 * @return the label of the Java element
	 * @since 3.5
	 */
	public static String getElementLabel(IJavaElement element, long flags) {
		StringBuffer buf= new StringBuffer();

		new JavaElementLinkedLabelComposer(element, buf).appendElementLabel(element, flags);
		return Strings.markLTR(buf.toString(), JavaElementLabelComposer.ADDITIONAL_DELIMITERS);

//		new JavaElementLabelComposer(buf).appendElementLabel(element, flags);
//		String string= buf.toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
//		return Strings.markLTR(string, JavaElementLabelComposer.ADDITIONAL_DELIMITERS);
	}
}
