/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for Bug 403917 - [1.8] Render TYPE_USE annotations in Javadoc hover/view
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;


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
		 * Handle link to given URL to open in browser.
		 *
		 * @param url the url to show
		 * @param display the current display
		 * @return <code>true</code> if the handler could open the link <code>false</code> if the
		 *         browser should follow the link
		 */
		boolean handleExternalLink(URL url, Display display);

		/**
		 * Informs the handler that the text of the browser was set.
		 */
		void handleTextSet();
	}

	static class JavaElementLinkedLabelComposer extends JavaElementLabelComposer {
		private final IJavaElement fElement;

		public JavaElementLinkedLabelComposer(IJavaElement member, StringBuffer buf) {
			super(buf);
			if (member instanceof IPackageDeclaration) {
				fElement= member.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			} else {
				fElement= member;
			}
		}

		@Override
		public String getElementName(IJavaElement element) {
			if (element instanceof IPackageFragment || element instanceof IPackageDeclaration) {
				return getPackageFragmentElementName(element);
			}

			String elementName= element.getElementName();
			return getElementName(element, elementName);
		}

		private String getElementName(IJavaElement element, String elementName) {
			if (element.equals(fElement)) { // linking to the member itself would be a no-op
				return elementName;
			}
			if (elementName.length() == 0) { // anonymous or lambda
				return elementName;
			}
			try {
				String uri= createURI(JAVADOC_SCHEME, element);
				return createHeaderLink(uri, elementName);
			} catch (URISyntaxException e) {
				JavaPlugin.log(e);
				return elementName;
			}
		}

		private String getPackageFragmentElementName(IJavaElement javaElement) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			String javaElementName= javaElement.getElementName();
			String packageName= null;
			StringBuilder strBuffer= new StringBuilder();

			for (String lastSegmentName : javaElementName.split("\\.")) { //$NON-NLS-1$
				if (packageName != null) {
					strBuffer.append('.');
					packageName= packageName + '.' + lastSegmentName;
				} else {
					packageName= lastSegmentName;
				}
				IPackageFragment subFragment= root.getPackageFragment(packageName);
				strBuffer.append(getElementName(subFragment, lastSegmentName));
			}

			return strBuffer.toString();
		}

		@Override
		protected String getGT() {
			return "&gt;"; //$NON-NLS-1$
		}

		@Override
		protected String getLT() {
			return "&lt;"; //$NON-NLS-1$
		}

		@Override
		protected String getSimpleTypeName(IJavaElement enclosingElement, String typeSig) {
			String typeName= super.getSimpleTypeName(enclosingElement, typeSig);

			String title= ""; //$NON-NLS-1$
			String qualifiedName= Signature.toString(Signature.getTypeErasure(typeSig));
			int qualifierLength= qualifiedName.length() - typeName.length() - 1;
			if (qualifierLength > 0) {
				if (qualifiedName.endsWith(typeName)) {
					title= qualifiedName.substring(0, qualifierLength);
					title= Messages.format(JavaUIMessages.JavaElementLinks_title, title);
				} else {
					title= qualifiedName; // Not expected. Just show the whole qualifiedName.
				}
			}

			try {
				String uri= createURI(JAVADOC_SCHEME, enclosingElement, qualifiedName, null, null);
				return createHeaderLink(uri, typeName, title);
			} catch (URISyntaxException e) {
				JavaPlugin.log(e);
				return typeName;
			}
		}

		@Override
		protected String getMemberName(IJavaElement enclosingElement, String typeName, String memberName) {
			try {
				String uri= createURI(JAVADOC_SCHEME, enclosingElement, typeName, memberName, null);
				return createHeaderLink(uri, memberName);
			} catch (URISyntaxException e) {
				JavaPlugin.log(e);
				return memberName;
			}
		}

		@Override
		protected void appendAnnotationLabels(IAnnotation[] annotations, long flags) throws JavaModelException {
			fBuffer.append("<span style='font-weight:normal;'>"); //$NON-NLS-1$
			super.appendAnnotationLabels(annotations, flags);
			fBuffer.append("</span>"); //$NON-NLS-1$
		}
	}

	public static final String OPEN_LINK_SCHEME= CoreJavaElementLinks.OPEN_LINK_SCHEME;
	public static final String JAVADOC_SCHEME= CoreJavaElementLinks.JAVADOC_SCHEME;
	public static final String JAVADOC_VIEW_SCHEME= CoreJavaElementLinks.JAVADOC_VIEW_SCHEME;

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
			@Override
			public void changing(LocationEvent event) {
				String loc= event.location;

				if ("about:blank".equals(loc) || loc.startsWith("data:")) { //$NON-NLS-1$ //$NON-NLS-2$
					/*
					 * Using the Browser.setText API triggers a location change to "about:blank".
					 * XXX: remove this code once https://bugs.eclipse.org/bugs/show_bug.cgi?id=130314 is fixed
					 */
					// The check for "data:" is due to Edge browser issuing a location change with a URL using the data: protocol
					// that contains the Base64 encoded version of the text whenever setText is called on the browser.
					// See issue: https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/248

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

				URI uri= null;
				try {
					uri= new URI(loc);
				} catch (URISyntaxException e) {
					JavaPlugin.log(e); // log bad URL, but proceed in the hope that handleExternalLink(..) can deal with it
				}

				String scheme= uri == null ? null : uri.getScheme();
				boolean nomatch= false;
				if (scheme != null) switch (scheme) {
				case JavaElementLinks.JAVADOC_VIEW_SCHEME:
					{
						IJavaElement linkTarget= JavaElementLinks.parseURI(uri);
						if (linkTarget == null)
							return;
						handler.handleJavadocViewLink(linkTarget);
						break;
					}
				case JavaElementLinks.JAVADOC_SCHEME:
					{
						IJavaElement linkTarget= JavaElementLinks.parseURI(uri);
						if (linkTarget == null)
							return;
						handler.handleInlineJavadocLink(linkTarget);
						break;
					}
				case JavaElementLinks.OPEN_LINK_SCHEME:
					{
						IJavaElement linkTarget= JavaElementLinks.parseURI(uri);
						if (linkTarget == null)
							return;
						handler.handleDeclarationLink(linkTarget);
						break;
					}
				default:
					nomatch= true;
					break;
				}
				if (nomatch) {
					try {
						if (!(loc.startsWith("data:")) && handler.handleExternalLink(new URL(loc), event.display)) //$NON-NLS-1$
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
		return CoreJavaElementLinks.createURI(scheme, element);
	}

	/**
	 * Creates an {@link URI} with the given scheme based on the given element.
	 * The additional arguments specify a member referenced from the given element.
	 *
	 * @param scheme a scheme
	 * @param element the declaring element
	 * @param refTypeName a (possibly qualified) type or package name, can be <code>null</code>
	 * @param refMemberName a member name, can be <code>null</code>
	 * @param refParameterTypes a (possibly empty) array of (possibly qualified) parameter type
	 *            names, can be <code>null</code>
	 * @return an {@link URI}, encoded as {@link URI#toASCIIString() ASCII} string, ready to be used
	 *         as <code>href</code> attribute in an <code>&lt;a&gt;</code> tag
	 * @throws URISyntaxException if the arguments were invalid
	 */
	public static String createURI(String scheme, IJavaElement element, String refTypeName, String refMemberName, String[] refParameterTypes) throws URISyntaxException {
		return CoreJavaElementLinks.createURI(scheme, element, refTypeName, refMemberName, refParameterTypes);
	}

	public static IJavaElement parseURI(URI uri) {
		return CoreJavaElementLinks.parseURI(uri);
	}

	/**
	 * Creates a link with the given URI and label text.
	 *
	 * @param uri the URI
	 * @param label the label
	 * @return the HTML link
	 * @since 3.6
	 */
	public static String createLink(String uri, String label) {
		return CoreJavaElementLinks.createLink(uri, label);
	}

	/**
	 * Creates a header link with the given URI and label text.
	 *
	 * @param uri the URI
	 * @param label the label
	 * @return the HTML link
	 * @since 3.6
	 */
	public static String createHeaderLink(String uri, String label) {
		return CoreJavaElementLinks.createHeaderLink(uri, label);
	}

	/**
	 * Creates a link with the given URI, label and title text.
	 *
	 * @param uri the URI
	 * @param label the label
	 * @param title the title to be displayed while hovering over the link (can be empty)
	 * @return the HTML link
	 * @since 3.10
	 */
	public static String createHeaderLink(String uri, String label, String title) {
		return CoreJavaElementLinks.createHeaderLink(uri, label, title);
	}

	/**
	 * Returns the label for a Java element with the flags as defined by {@link JavaElementLabels}.
	 * Referenced element names in the label (except the given element's name) are rendered as
	 * header links.
	 *
	 * @param element the element to render
	 * @param flags the rendering flags
	 * @return the label of the Java element
	 * @since 3.5
	 */
	public static String getElementLabel(IJavaElement element, long flags) {
		return getElementLabel(element, flags, false);
	}

	/**
	 * Returns the label for a Java element with the flags as defined by {@link JavaElementLabels}.
	 * Referenced element names in the label are rendered as header links.
	 * If <code>linkAllNames</code> is <code>false</code>, don't link the name of the given element
	 *
	 * @param element the element to render
	 * @param flags the rendering flags
	 * @param linkAllNames if <code>true</code>, link all names; if <code>false</code>, link all names except original element's name
	 * @return the label of the Java element
	 * @since 3.6
	 */
	public static String getElementLabel(IJavaElement element, long flags, boolean linkAllNames) {
		StringBuffer buf= new StringBuffer();

		if (!Strings.USE_TEXT_PROCESSOR) {
			new JavaElementLinkedLabelComposer(linkAllNames ? null : element, buf).appendElementLabel(element, flags);
			return Strings.markJavaElementLabelLTR(buf.toString());
		} else {
			String label= JavaElementLabels.getElementLabel(element, flags);
			return label.replace("<", "&lt;").replace(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	/**
	 * Returns the label for a binding with the flags as defined by {@link JavaElementLabels}.
	 * Referenced element names in the label are rendered as header links.
	 *
	 * @param binding the binding to render
	 * @param element the corresponding Java element, used for javadoc hyperlinks
	 * @param flags the rendering flags
	 * @param haveSource true when looking at an ICompilationUnit which enables the use of short type names
	 * @return the label of the binding
	 * @since 3.11
	 */
	public static String getBindingLabel(IBinding binding, IJavaElement element, long flags, boolean haveSource) {
		StringBuffer buf= new StringBuffer();

		if (!Strings.USE_TEXT_PROCESSOR) {
			new BindingLinkedLabelComposer(element, buf, haveSource).appendBindingLabel(binding, flags);
			return Strings.markJavaElementLabelLTR(buf.toString());
		} else {
			String label= JavaElementLabels.getElementLabel(element, flags);
			return label.replace("<", "&lt;").replace(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}
}
