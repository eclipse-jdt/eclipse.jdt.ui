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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Links inside Javadoc hovers and Javadoc view.
 * <p>
 * <strong>This is work in progress.</strong>
 * </p>
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

	public static final String OPEN_LINK_SCHEME= "eclipse-open"; //$NON-NLS-1$
	public static final String JAVADOC_SCHEME= "eclipse-javadoc"; //$NON-NLS-1$
	public static final String JAVADOC_VIEW_SCHEME= "eclipse-javadoc-view"; //$NON-NLS-1$
	private static final char LINK_BRACKET_REPLACEMENT= '\u2603';
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

				if (loc.startsWith("about:")) //$NON-NLS-1$
					return; //FIXME: handle relative links, see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=8112

				URI uri;
				try {
					uri= new URI(loc);
				} catch (URISyntaxException e) {
					JavaPlugin.log(e);
					return;
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
	
	public static String createURI(String scheme, IJavaElement element) throws URISyntaxException {
		return createURI(scheme, element, null, null, null);
	}

	/**
	 * @param scheme a scheme
	 * @param element the declaring element
	 * @param refTypeName a (possibly qualified) type name, can be <code>null</code>
	 * @param refMemberName a member name, can be <code>null</code>
	 * @param refParameterTypes a (possibly empty) array of (possibly qualified) parameter type names, can be <code>null</code>
	 * @return an encoded URI, ready to be used as <code>href</code> attribute in an <code>&lt;a&gt;</code> tag
	 * @throws URISyntaxException
	 */
	public static String createURI(String scheme, IJavaElement element, String refTypeName, String refMemberName, String[] refParameterTypes) throws URISyntaxException {
		/*
		 * We use an opaque URI, not ssp and fragments (to work around Safari bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=212527 (wrongly encodes #)).
		 */
		
		StringBuffer ssp= new StringBuffer(60);
		ssp.append(LINK_SEPARATOR); // make sure first character is not a / (would be hierarchical URI)
		
		// replace '[' manually, since URI confuses it for an IPv6 address as per RFC 2732:
		ssp.append(element.getHandleIdentifier().replace('[', LINK_BRACKET_REPLACEMENT)); // segment[1]

		if (refTypeName != null) {
			ssp.append(LINK_SEPARATOR);
			ssp.append(refTypeName); // segment[2]

			if (refMemberName != null) {
				ssp.append(LINK_SEPARATOR);
				ssp.append(refMemberName); // segment[3]

				if (refParameterTypes != null) {
					ssp.append(LINK_SEPARATOR);
					for (int i= 0; i < refParameterTypes.length; i++) {
						ssp.append(refParameterTypes[i]); // segment[4|5|..]
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
			if (element instanceof IMember && !(element instanceof IType)) {
				element= ((IMember) element).getDeclaringType();
			}
			if (element instanceof IType) {
				String refTypeName= segments[2];
				try {
					IType baseType= (IType) element;
					IType type= resolveType(baseType, refTypeName);
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
								if (methods != null)
									return methods[0];
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
}
