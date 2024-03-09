/*******************************************************************************
 * Copyright (c) 2008, 2024 IBM Corporation and others.
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
 *     Jozef Tomek - add styling enhancements (issue 1073)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.browser.HoverPreferenceStylingInBrowserAction.StylingPreference;
import org.eclipse.jdt.internal.ui.viewsupport.javadoc.JavadocStylingMessages;


/**
 * Links inside Javadoc hovers and Javadoc view.
 *
 * @since 3.4
 */
public class JavaElementLinks {

	/**
	 * ID of the checkbox in generated HTML content that toggles formatting inside element labels.
	 */
	public static final String CHECKBOX_ID_FORMATTIG= "formattingSwitch"; //$NON-NLS-1$
	/**
	 * ID of the checkbox in generated HTML content that toggles wrapping inside element labels.
	 */
	public static final String CHECKBOX_ID_WRAPPING= "wrappingSwitch"; //$NON-NLS-1$
	/**
	 * ID of the checkbox in generated HTML content that toggles type parameters coloring inside element labels.
	 */
	public static final String CHECKBOX_ID_TYPE_PARAMETERS_REFERENCES_COLORING= "typeParamsRefsColoringSwitch"; //$NON-NLS-1$
	/**
	 * ID of the checkbox in generated HTML content that toggles type parameters levels coloring inside element labels.
	 */
	public static final String CHECKBOX_ID_TYPE_PARAMETERS_LEVELS_COLORING= "typeParamsLevelsColoringSwitch"; //$NON-NLS-1$
	/**
	 * ID of the checkbox in generated HTML content that toggles overlay when previewing styling.
	 */
	public static final String CHECKBOX_ID_PREVIEW= "previewSwitch"; //$NON-NLS-1$

	private static final String PREFERENCE_KEY_POSTFIX_FORMATTING= "formatting"; //$NON-NLS-1$
	private static final String PREFERENCE_KEY_POSTFIX_WRAPPING= "wrapping"; //$NON-NLS-1$
	private static final String PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_REFERENCES_COLORING= "typeParamsReferencesColoring"; //$NON-NLS-1$
	private static final String PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_LEVELS_COLORING= "typeParamsLevelsColoring"; //$NON-NLS-1$

	private static final String PREFERENCE_KEY_ENABLED= "javadocElementsStyling.enabled"; //$NON-NLS-1$
	private static final String PREFERENCE_KEY_DARK_MODE_DEFAULT_COLORS= "javadocElementsStyling.darkModeDefaultColors"; //$NON-NLS-1$
	// both use 1-based indexing
	private static final String PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR= "javadocElementsStyling.typesParamsReference_"; //$NON-NLS-1$
	private static final String PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR= "javadocElementsStyling.typesParamsLevel_"; //$NON-NLS-1$
	private static final String PREFERENCE_KEY_POSTFIX_COLOR= ".color"; //$NON-NLS-1$
	/**
	 * Maximum number of type parameters references / levels for which we support setting custom color
	 */
	private static final int MAX_COLOR_INDEX= 16;


	private static final String CSS_CLASS_SWITCH_PARENT= "styleSwitchParent"; //$NON-NLS-1$
	// both use 1-based indexing
	private static final String CSS_CLASS_TYPE_PARAMETERS_REFERENCE_PREFIX= "typeParamsReference typeParamsReferenceNo"; //$NON-NLS-1$
	private static final String CSS_CLASS_TYPE_PARAMETERS_LEVEL_PREFIX= "typeParamsLevel typeParamsLevelNo"; //$NON-NLS-1$

	private static final String CSS_SECTION_START_TYPE_PARAMETERS_REFERENCES= "/* Start of dynamic type parameters references styling section (do not edit this line) */"; //$NON-NLS-1$
	private static final String CSS_SECTION_START_TYPE_PARAMETERS_LEVELS= "/* Start of dynamic type parameters levels styling section (do not edit this line) */"; //$NON-NLS-1$
	private static final String CSS_SECTION_END_TYPE_PARAMETERS_REFERENCES= "/* End of dynamic type parameters references styling section (do not edit this line) */"; //$NON-NLS-1$
	private static final String CSS_SECTION_END_TYPE_PARAMETERS_LEVELS= "/* End of dynamic type parameters levels styling section (do not edit this line) */"; //$NON-NLS-1$
	private static final String CSS_PLACEHOLDER_INDEX= "-INDEX-"; //$NON-NLS-1$
	private static final String CSS_PLACEHOLDER_COLOR= "-COLOR-"; //$NON-NLS-1$


	private static String[] CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_REFERENCES= new String[4];
	private static String[] CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_LEVELS= new String[4];
	private static final ReentrantLock CSS_FRAGMENTS_CACHE_LOCK= new ReentrantLock();
	private static final IPropertyChangeListener COLOR_PROPERTIES_CHANGE_LISTENER= JavaElementLinks::cssFragmentsCacheResetListener;
	private static final ListenerList<IStylingConfigurationListener> configListener = new ListenerList<>();

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
		private final boolean noEnhancements;
		private final boolean enableWrapping;
		private final boolean enableFormatting;
		private final boolean enableTypeParamsColoring;
		private final boolean enableTypeLevelsColoring;

		private boolean appendHoverParent= true;
		private int nextNestingLevel= 1;
		private Map<String, Integer> typesIds= new TreeMap<>();
		private int nextTypeNo= 1;
		private int nextParamNo= 1;
		private boolean appendingMethodQualification= false;
		private boolean typeStyleClassApplied= false;
		private boolean inBoundedTypeParam= false;

		public JavaElementLinkedLabelComposer(IJavaElement member, StringBuffer buf) {
			this(member, buf, null);
		}

		public JavaElementLinkedLabelComposer(IJavaElement member, StringBuffer buf, String stylingPreferenceKeysPrefix) {
			super(buf);
			if (member instanceof IPackageDeclaration) {
				fElement= member.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			} else {
				fElement= member;
			}
			if (getStylingEnabledPreference() && stylingPreferenceKeysPrefix != null) {
				noEnhancements= false;
				enableWrapping= isStylingPreferenceAlways(stylingPreferenceKeysPrefix + PREFERENCE_KEY_POSTFIX_WRAPPING);
				enableFormatting= isStylingPreferenceAlways(stylingPreferenceKeysPrefix + PREFERENCE_KEY_POSTFIX_FORMATTING);
				enableTypeParamsColoring= isStylingPreferenceAlways(stylingPreferenceKeysPrefix + PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_REFERENCES_COLORING);
				enableTypeLevelsColoring= isStylingPreferenceAlways(stylingPreferenceKeysPrefix + PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_LEVELS_COLORING);
			} else {
				noEnhancements= true;
				enableWrapping= enableFormatting= enableTypeParamsColoring= enableTypeLevelsColoring= false;
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
		protected void appendGT() {
			if (noEnhancements) {
				fBuffer.append("&gt;"); //$NON-NLS-1$
			} else {
				fBuffer.append("</span>"); //$NON-NLS-1$
				fBuffer.append("<span class='typeBrackets typeBracketsEnd typeLevel"); //$NON-NLS-1$
				fBuffer.append(String.valueOf(--nextNestingLevel));
				fBuffer.append("'>&gt;</span>"); //$NON-NLS-1$
			}

		}

		@Override
		protected void appendLT() {
			if (noEnhancements) {
				fBuffer.append("&lt;"); //$NON-NLS-1$
			} else {
				fBuffer.append("<span class='typeBrackets typeBracketsStart typeLevel"); //$NON-NLS-1$
				fBuffer.append(String.valueOf(nextNestingLevel));
				fBuffer.append("'>&lt;</span>"); //$NON-NLS-1$

				fBuffer.append("<span class='"); //$NON-NLS-1$
				fBuffer.append(CSS_CLASS_TYPE_PARAMETERS_LEVEL_PREFIX);
				fBuffer.append(String.valueOf(nextNestingLevel++));
				fBuffer.append("'>"); //$NON-NLS-1$
			}
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

			String retVal= typeName;
			try {
				String uri= createURI(JAVADOC_SCHEME, enclosingElement, qualifiedName, null, null);
				retVal= createHeaderLink(uri, typeName, title);
			} catch (URISyntaxException e) {
				JavaPlugin.log(e);
			}

			if (!noEnhancements && !inBoundedTypeParam) {
				if ((Signature.getTypeSignatureKind(typeSig) == Signature.TYPE_VARIABLE_SIGNATURE && !typeStyleClassApplied)
						|| (Signature.getTypeSignatureKind(typeSig) == Signature.CLASS_TYPE_SIGNATURE && nextNestingLevel > 1)) {
					return wrapWithTypeClass(typeName, retVal);
				} else {
					return retVal;
				}
			} else {
				return retVal;
			}
		}

		private String wrapWithTypeClass(String typeName, String value) {
			return "<span class='" //$NON-NLS-1$
					+ getTypeStylingClass(typeName) + "'>" //$NON-NLS-1$
					+ value
					+ "</span>"; //$NON-NLS-1$
		}

		private String getTypeStylingClass(String typeName) {
			Integer typeId;
			if ((typeId= typesIds.putIfAbsent(typeName, nextTypeNo)) == null) {
				typeId= nextTypeNo++;
			}
			return CSS_CLASS_TYPE_PARAMETERS_REFERENCE_PREFIX + typeId;
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

		@Override
		public void appendElementLabel(IJavaElement element, long flags) {
			if (noEnhancements) {
				super.appendElementLabel(element, flags);
				return;
			}
			if (appendingMethodQualification) {
				// method label contains nested method label (eg. lambdas), we need to end method qualification <span> if started
				fBuffer.append("</span>"); //$NON-NLS-1$
				appendingMethodQualification= false;
			}
			if (appendHoverParent) {
				appendHoverParent= false;

				// styling preview checkbox
				fBuffer.append("<input type='checkbox' id='" + CHECKBOX_ID_PREVIEW + //$NON-NLS-1$
							"' style='position: absolute; top: 18px; left: -23px;'/>"); //$NON-NLS-1$

				// wrapping checkbox
				fBuffer.append("<input type='checkbox' id='" + CHECKBOX_ID_WRAPPING + "' "); //$NON-NLS-1$ //$NON-NLS-2$
				if (enableWrapping) {
					fBuffer.append("checked=true "); //$NON-NLS-1$
				}
				fBuffer.append("style='position: absolute; top: 32px; left: -23px;'/>"); //$NON-NLS-1$

				// formatting checkbox
				fBuffer.append("<input type='checkbox' id='" + CHECKBOX_ID_FORMATTIG + "' "); //$NON-NLS-1$ //$NON-NLS-2$
				if (enableFormatting) {
					fBuffer.append("checked=true "); //$NON-NLS-1$
				}
				fBuffer.append("style='position: absolute; top: 46px; left: -23px;'/>"); //$NON-NLS-1$

				// typeLevelsColoring checkbox
				fBuffer.append("<input type='checkbox' id='" + CHECKBOX_ID_TYPE_PARAMETERS_LEVELS_COLORING + "' "); //$NON-NLS-1$ //$NON-NLS-2$
				if (enableTypeLevelsColoring) {
					fBuffer.append("checked=true "); //$NON-NLS-1$
				}
				fBuffer.append("style='position: absolute; top: 60px; left: -23px;'/>"); //$NON-NLS-1$

				// typeParametersColoring checkbox
				fBuffer.append("<input type='checkbox' id='" + CHECKBOX_ID_TYPE_PARAMETERS_REFERENCES_COLORING + "' "); //$NON-NLS-1$ //$NON-NLS-2$
				if (enableTypeParamsColoring) {
					fBuffer.append("checked=true "); //$NON-NLS-1$
				}
				fBuffer.append("style='position: absolute; top: 74px; left: -23px;'/>"); //$NON-NLS-1$

				// encompassing <span> for everything styled based on checkboxes checked state
				fBuffer.append("<span class='" + CSS_CLASS_SWITCH_PARENT + "'>"); //$NON-NLS-1$ //$NON-NLS-2$

				// actual signature content
				super.appendElementLabel(element, flags);

				// preview watermarks
				fBuffer.append("<div id='previewWatermark'>" + JavadocStylingMessages.JavadocStyling_stylingPreview_watermark + " - "); //$NON-NLS-1$ //$NON-NLS-2$
				fBuffer.append("<div id='previewTypeParamsRefsColoring'>" //$NON-NLS-1$
						+ JavadocStylingMessages.JavadocStyling_stylingPreview_typeParamsReferencesColoring + "</div>"); //$NON-NLS-1$
				fBuffer.append("<div id='previewTypeParamsLevelsColoring'>" //$NON-NLS-1$
						+ JavadocStylingMessages.JavadocStyling_stylingPreview_typeParamsLevelsColoring + "</div>"); //$NON-NLS-1$
				fBuffer.append("<div id='previewFormatting'>" //$NON-NLS-1$
						+ JavadocStylingMessages.JavadocStyling_stylingPreview_formatting + "</div>"); //$NON-NLS-1$
				fBuffer.append("<div id='previewWrapping'>" //$NON-NLS-1$
						+ JavadocStylingMessages.JavadocStyling_stylingPreview_wrapping + "</div>"); //$NON-NLS-1$
				fBuffer.append("</div>"); //$NON-NLS-1$
				fBuffer.append("</span>"); //$NON-NLS-1$
				appendHoverParent= true;
			} else {
				super.appendElementLabel(element, flags);
			}
		}

		@Override
		protected void appendMethodPrependedTypeParams(IMethod method, long flags, BindingKey resolvedKey, String resolvedSignature) throws JavaModelException {
			if (noEnhancements) {
				super.appendMethodPrependedTypeParams(method, flags, resolvedKey, resolvedSignature);
			} else {
				fBuffer.append("<span class='methodPrependTypeParams'>"); //$NON-NLS-1$
				super.appendMethodPrependedTypeParams(method, flags, resolvedKey, resolvedSignature);
				fBuffer.append("</span>"); //$NON-NLS-1$
			}
		}

		@Override
		protected void appendMethodPrependedReturnType(IMethod method, long flags, String resolvedSignature) throws JavaModelException {
			if (noEnhancements) {
				super.appendMethodPrependedReturnType(method, flags, resolvedSignature);
			} else {
				fBuffer.append("<span class='methodReturn'>"); //$NON-NLS-1$
				super.appendMethodPrependedReturnType(method, flags, resolvedSignature);
				fBuffer.append("</span>"); //$NON-NLS-1$
			}
		}

		@Override
		protected void appendMethodQualification(IMethod method, long flags) {
			if (noEnhancements) {
				super.appendMethodQualification(method, flags);
			} else {
				appendingMethodQualification= true;
				fBuffer.append("<span class='methodQualifier'>"); //$NON-NLS-1$
				super.appendMethodQualification(method, flags);
				if (appendingMethodQualification) {
					fBuffer.append("</span>"); //$NON-NLS-1$
					appendingMethodQualification= false;
				}
			}
		}

		@Override
		protected void appendMethodName(IMethod method) {
			if (noEnhancements) {
				super.appendMethodName(method);
			} else {
				fBuffer.append("<span class='methodName'>"); //$NON-NLS-1$
				super.appendMethodName(method);
				fBuffer.append("</span>"); //$NON-NLS-1$
			}
		}

		@Override
		protected void appendMethodParams(IMethod method, long flags, String resolvedSignature) throws JavaModelException {
			if (noEnhancements) {
				super.appendMethodParams(method, flags, resolvedSignature);
			} else {
				fBuffer.append("<span class='methodParams'>"); //$NON-NLS-1$
				super.appendMethodParams(method, flags, resolvedSignature);
				fBuffer.append("</span>"); //$NON-NLS-1$
				nextParamNo= 1;
			}
		}

		@Override
		protected void appendMethodParam(IMethod method, long flags, IAnnotation[] annotations, String paramSignature, String name, boolean renderVarargs, boolean isLast) throws JavaModelException {
			if (noEnhancements) {
				super.appendMethodParam(method, flags, annotations, paramSignature, name, renderVarargs, isLast);
			} else {
				fBuffer.append("<span class='methodParam'>"); //$NON-NLS-1$
				super.appendMethodParam(method, flags, annotations, paramSignature, name, renderVarargs, isLast);
				fBuffer.append("</span>"); //$NON-NLS-1$
			}
		}

		@Override
		protected void appendMethodParamName(String name) {
			if (noEnhancements) {
				super.appendMethodParamName(name);
			} else {
				fBuffer.append("<span class='methodParamName methodParamNo"); //$NON-NLS-1$
				fBuffer.append(String.valueOf(nextParamNo++));
				fBuffer.append("'>"); //$NON-NLS-1$
				super.appendMethodParamName(name);
				fBuffer.append("</span>"); //$NON-NLS-1$
			}
		}

		@Override
		protected void appendTypeParameterWithBounds(ITypeParameter typeParameter, long flags) throws JavaModelException {
			if (noEnhancements) {
				super.appendTypeParameterWithBounds(typeParameter, flags);
			} else {
				fBuffer.append("<span class='"); //$NON-NLS-1$
				fBuffer.append(getTypeStylingClass(typeParameter.getElementName()));
				fBuffer.append("'>"); //$NON-NLS-1$
				inBoundedTypeParam= true;
				super.appendTypeParameterWithBounds(typeParameter, flags);
				inBoundedTypeParam= false;
				fBuffer.append("</span>"); //$NON-NLS-1$
			}
		}

		@Override
		protected void appendWildcardTypeSignature(String prefix, IJavaElement enclosingElement, String typeSignature, long flags) {
			if (noEnhancements) {
				super.appendWildcardTypeSignature(prefix, enclosingElement, typeSignature, flags);
			} else {
				int sigKind= Signature.getTypeSignatureKind(typeSignature);
				if (sigKind == Signature.TYPE_VARIABLE_SIGNATURE || sigKind == Signature.CLASS_TYPE_SIGNATURE) {
					typeStyleClassApplied= true;
					String typeName= super.getSimpleTypeName(enclosingElement, typeSignature);
					fBuffer.append("<span class='"); //$NON-NLS-1$
					fBuffer.append(getTypeStylingClass(typeName));
					fBuffer.append("'>"); //$NON-NLS-1$
				}
				super.appendWildcardTypeSignature(prefix, enclosingElement, typeSignature, flags);
				if (typeStyleClassApplied) {
					fBuffer.append("</span>"); //$NON-NLS-1$
					typeStyleClassApplied= false;
				}
			}
		}

		@Override
		protected void appendTypeArgumentSignaturesLabel(IJavaElement enclosingElement, String[] typeArgsSig, long flags) {
			if (inBoundedTypeParam) {
				inBoundedTypeParam= false;
				super.appendTypeArgumentSignaturesLabel(enclosingElement, typeArgsSig, flags);
				inBoundedTypeParam= true;
			} else {
				super.appendTypeArgumentSignaturesLabel(enclosingElement, typeArgsSig, flags);
			}
		}
	}

	public static final String OPEN_LINK_SCHEME= CoreJavaElementLinks.OPEN_LINK_SCHEME;
	public static final String JAVADOC_SCHEME= CoreJavaElementLinks.JAVADOC_SCHEME;
	public static final String JAVADOC_VIEW_SCHEME= CoreJavaElementLinks.JAVADOC_VIEW_SCHEME;

	public static void initDefaultPreferences(IPreferenceStore store) {
		initDefaultColors(store);
		store.setDefault(PREFERENCE_KEY_ENABLED, true);
		store.addPropertyChangeListener(COLOR_PROPERTIES_CHANGE_LISTENER);
		// taking advantage of PREFERENCE_KEY_DARK_MODE_DEFAULT_COLORS change instead of more complicated OSGi event listener
		store.addPropertyChangeListener(JavaElementLinks::propertyChanged);
	}

	public static void initDefaultColors(IPreferenceStore store) {
		if (store.getBoolean(PREFERENCE_KEY_DARK_MODE_DEFAULT_COLORS)) {
			var color= new RGB(177, 102, 218); // semanticHighlighting.typeArgument.color in css\e4-dark_jdt_syntaxhighlighting.css
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, 1), color);
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, 1), color);

			color= new RGB(255, 140, 0); // CSS 'DarkOrange'
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, 2), color);
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, 2), color);

			color= new RGB(144, 238, 144); // CSS 'LightGreen'
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, 3), color);
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, 3), color);

			color= new RGB(0, 191, 255); // CSS 'DeepSkyBlue'
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, 4), color);
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, 4), color);
		} else {
			// slightly brighter than SemanticHighlightings.TypeArgumentHighlighting's default color to work better on yellow-ish background
			var color= new RGB(60, 179, 113); // CSS 'MediumSeaGreen'
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, 1), color);
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, 1), color);

			color= new RGB(255, 140, 0); // CSS 'DarkOrange'
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, 2), color);
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, 2), color);

			color= new RGB(153, 50, 204); // CSS 'DarkOrchid'
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, 3), color);
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, 3), color);

			color= new RGB(65, 105, 225); // CSS 'RoyalBlue'
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, 4), color);
			PreferenceConverter.setDefault(store, getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, 4), color);
		}
	}

	public static void initDefaultPreferences(IPreferenceStore store, String keyPrefix) {
		store.setDefault(keyPrefix + PREFERENCE_KEY_POSTFIX_FORMATTING, StylingPreference.ALWAYS.name());
		store.setDefault(keyPrefix + PREFERENCE_KEY_POSTFIX_WRAPPING, StylingPreference.ALWAYS.name());
		store.setDefault(keyPrefix + PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_REFERENCES_COLORING, StylingPreference.HOVER.name());
		store.setDefault(keyPrefix + PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_LEVELS_COLORING, StylingPreference.OFF.name());
	}

	private static void propertyChanged(PropertyChangeEvent event) {
		if (PREFERENCE_KEY_DARK_MODE_DEFAULT_COLORS.equals(event.getProperty())) {
			initDefaultColors(preferenceStore());
			cssFragmentsCacheResetListener(null);
		} else if (PREFERENCE_KEY_ENABLED.equals(event.getProperty())) {
			configListener.forEach(l -> l.stylingStateChanged((Boolean) event.getNewValue()));
		}
	}

	private static void cssFragmentsCacheResetListener(PropertyChangeEvent event) {
		var changeOfTypeLevelsColor= false;
		var changeOfTypeParamsColor= false;
		if (event == null) {
			changeOfTypeLevelsColor= changeOfTypeParamsColor= true;
		} else {
			changeOfTypeLevelsColor= event.getProperty().startsWith(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR);
			changeOfTypeParamsColor= event.getProperty().startsWith(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR);
		}
		if (changeOfTypeLevelsColor || changeOfTypeParamsColor) {
			try {
				if (CSS_FRAGMENTS_CACHE_LOCK.tryLock(500, TimeUnit.MILLISECONDS)) {
					try {
						if (changeOfTypeLevelsColor) {
							CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_LEVELS= new String[4];
						}
						if (changeOfTypeParamsColor) {
							CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_REFERENCES= new String[4];
						}
					} finally {
						CSS_FRAGMENTS_CACHE_LOCK.unlock()	;
					}
				}
			} catch (InterruptedException e1) {
				JavaPlugin.logErrorMessage("Interrupted while waiting for CSS fragments cache lock, cache reset unsuccessful"); //$NON-NLS-1$
			}
		}
	}

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
		return getElementLabel(element, flags, linkAllNames, null);
	}

	/**
	 * Returns the label for a Java element with the flags as defined by {@link JavaElementLabels}.
	 * Referenced element names in the label are rendered as header links.
	 * If <code>linkAllNames</code> is <code>false</code>, don't link the name of the given element
	 *
	 * @param element the element to render
	 * @param flags the rendering flags
	 * @param linkAllNames if <code>true</code>, link all names; if <code>false</code>, link all names except original element's name
	 * @param stylingPreferenceKeysPrefix prefix for preference keys related to styling of HTML content for element labels,
	 * <code>null</code> means no enhanced styling
	 * @return the label of the Java element
	 * @since 3.6
	 */
	public static String getElementLabel(IJavaElement element, long flags, boolean linkAllNames, String stylingPreferenceKeysPrefix) {
		StringBuffer buf= new StringBuffer();

		if (!Strings.USE_TEXT_PROCESSOR) {
			new JavaElementLinkedLabelComposer(linkAllNames ? null : element, buf, stylingPreferenceKeysPrefix).appendElementLabel(element, flags);
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
		return getBindingLabel(binding, element, flags, haveSource, null);
	}

	/**
	 * Returns the label for a binding with the flags as defined by {@link JavaElementLabels}.
	 * Referenced element names in the label are rendered as header links.
	 *
	 * @param binding the binding to render
	 * @param element the corresponding Java element, used for javadoc hyperlinks
	 * @param flags the rendering flags
	 * @param haveSource true when looking at an ICompilationUnit which enables the use of short type names
	 * @param stylingPreferenceKeysPrefix prefix for preference keys related to styling of HTML content for element labels,
	 * <code>null</code> means no enhanced styling
	 * @return the label of the binding
	 * @since 3.11
	 */
	public static String getBindingLabel(IBinding binding, IJavaElement element, long flags, boolean haveSource, String stylingPreferenceKeysPrefix) {
		StringBuffer buf= new StringBuffer();

		if (!Strings.USE_TEXT_PROCESSOR) {
			new BindingLinkedLabelComposer(element, buf, haveSource, stylingPreferenceKeysPrefix).appendBindingLabel(binding, flags);
			return Strings.markJavaElementLabelLTR(buf.toString());
		} else {
			String label= JavaElementLabels.getElementLabel(element, flags);
			return label.replace("<", "&lt;").replace(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	private static boolean isStylingPreferenceAlways(String key) {
		return StylingPreference.ALWAYS == getPreference(key);
	}

	public static boolean getStylingEnabledPreference() {
		return preferenceStore().getBoolean(PREFERENCE_KEY_ENABLED);
	}

	public static StylingPreference getPreferenceForFormatting(String keyPrefix) {
		return getPreference(keyPrefix + PREFERENCE_KEY_POSTFIX_FORMATTING);
	}

	public static StylingPreference getPreferenceForWrapping(String keyPrefix) {
		return getPreference(keyPrefix + PREFERENCE_KEY_POSTFIX_WRAPPING);
	}

	public static StylingPreference getPreferenceForTypeParamsReferencesColoring(String keyPrefix) {
		return getPreference(keyPrefix + PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_REFERENCES_COLORING);
	}

	public static StylingPreference getPreferenceForTypeParamsLevelsColoring(String keyPrefix) {
		return getPreference(keyPrefix + PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_LEVELS_COLORING);
	}

	private static StylingPreference getPreference(String key) {
		return StylingPreference.valueOf(preferenceStore().getString(key));
	}

	public static void setStylingEnabledPreference(boolean value) {
		preferenceStore().setValue(PREFERENCE_KEY_ENABLED, value);
	}

	public static void setPreferenceForFormatting(String keyPrefix, StylingPreference value) {
		setPreference(keyPrefix + PREFERENCE_KEY_POSTFIX_FORMATTING, value);
	}

	public static void setPreferenceForWrapping(String keyPrefix, StylingPreference value) {
		setPreference(keyPrefix + PREFERENCE_KEY_POSTFIX_WRAPPING, value);
	}

	public static void setPreferenceForTypeParamsReferencesColoring(String keyPrefix, StylingPreference value) {
		setPreference(keyPrefix + PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_REFERENCES_COLORING, value);
	}

	public static void setPreferenceForTypeParamsLevelsColoring(String keyPrefix, StylingPreference value) {
		setPreference(keyPrefix + PREFERENCE_KEY_POSTFIX_TYPE_PARAMETERS_LEVELS_COLORING, value);
	}

	private static void setPreference(String key, StylingPreference value) {
		preferenceStore().setValue(key, value.name());
	}

	public static RGB getColorPreferenceForTypeParamsReference(int referenceIndex) {
		return getColorPreference(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, referenceIndex);
	}

	public static RGB getColorPreferenceForTypeParamsLevel(int levelIndex) {
		return getColorPreference(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, levelIndex);
	}

	private static RGB getColorPreference(String keyPrefix, int index) {
		var color= PreferenceConverter.getColor(preferenceStore(), getColorPreferenceKey(keyPrefix, index));
		if (PreferenceConverter.COLOR_DEFAULT_DEFAULT == color) {
			// for unconfigured color indexes alternate between first 4 colors
			return PreferenceConverter.getColor(preferenceStore(), getColorPreferenceKey(keyPrefix, 1 + ((index + 3) % 4)));
		}
		return color;
	}

	public static void setColorPreferenceForTypeParamsReference(int referenceIndex, RGB color) {
		setColorPreference(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, referenceIndex, color);
	}

	public static void setColorPreferenceForTypeParamsLevel(int levelIndex, RGB color) {
		setColorPreference(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, levelIndex, color);
	}

	private static void setColorPreference(String keyPrefix, int index, RGB color) {
		PreferenceConverter.setValue(preferenceStore(), getColorPreferenceKey(keyPrefix, index), color);
	}

	public static String modifyCssStyleSheet(String css, StringBuilder buffer) {
		int startPos= buffer.indexOf(CSS_CLASS_SWITCH_PARENT);
		if (startPos < 0) {
			return css;
		}
		StringBuilder cssContent= new StringBuilder();

		int maxTypeParamNo= getMaxIndexOfStyle(buffer, StringBuilder::indexOf, CSS_CLASS_TYPE_PARAMETERS_REFERENCE_PREFIX);
		int maxTypeLevelNo= getMaxIndexOfStyle(buffer, StringBuilder::indexOf, CSS_CLASS_TYPE_PARAMETERS_LEVEL_PREFIX);

		var locked= false;
		try {
			locked= CSS_FRAGMENTS_CACHE_LOCK.tryLock(100, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			JavaPlugin.logErrorMessage("Interrupted while waiting for CSS fragments cache lock, proceeding without using cache"); //$NON-NLS-1$
		}
		try {
			if (locked) {
				if (CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_LEVELS.length < maxTypeLevelNo) {
					CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_LEVELS= Arrays.copyOf(CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_LEVELS, maxTypeLevelNo);
				}
				if (CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_REFERENCES.length < maxTypeParamNo) {
					CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_REFERENCES= Arrays.copyOf(CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_REFERENCES, maxTypeParamNo);
				}
			}
			var processedUntil= processSection(css, cssContent, 0, CSS_SECTION_START_TYPE_PARAMETERS_LEVELS, CSS_SECTION_END_TYPE_PARAMETERS_LEVELS,
					PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, maxTypeLevelNo,
					(locked ? CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_LEVELS : null), true);
			processedUntil= processSection(css, cssContent, processedUntil, CSS_SECTION_START_TYPE_PARAMETERS_REFERENCES, CSS_SECTION_END_TYPE_PARAMETERS_REFERENCES,
					PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, maxTypeParamNo,
					(locked ? CSS_FRAGMENTS_CACHE_TYPE_PARAMETERS_REFERENCES : null), false);
			cssContent.append(css, processedUntil, css.length());
			return cssContent.toString();
		} catch (Exception e) {
			JavaPlugin.log(e);
			return css;
		} finally {
			if (locked) {
				CSS_FRAGMENTS_CACHE_LOCK.unlock();
			}
		}
	}

	private static int processSection(String cssTemplate, StringBuilder outputCss, int previousEnd,
			String sectionStartLine, String sectionEndLine, String preferenceKeyPrefix,
			int iterations, String[] fragmentsCache, boolean upwards) {
		var sectionStart= cssTemplate.indexOf(sectionStartLine);
		outputCss.append(cssTemplate, previousEnd, sectionStart);

		sectionStart += sectionStartLine.length();
		var sectionEnd= cssTemplate.indexOf(sectionEndLine, sectionStart);
		for (int i= upwards ? 0 : iterations - 1; upwards ? i < iterations : i >= 0 ;  i = i + (upwards ? 1 : -1)) {
			if (fragmentsCache != null && fragmentsCache.length > i && fragmentsCache[i] != null) {
				// re-use cached fragment
				outputCss.append(fragmentsCache[i]);
			} else {
				var section= cssTemplate.substring(sectionStart, sectionEnd);
				var sectionBuf= new StringBuilder(section);
				int pos;
				int index = i + 1; // color styles in CSS and preference keys are 1-based
				while ((pos= sectionBuf.indexOf(CSS_PLACEHOLDER_INDEX)) != -1) {
					sectionBuf.replace(pos, pos + CSS_PLACEHOLDER_INDEX.length(), String.valueOf(index));
				}
				pos= sectionBuf.indexOf(CSS_PLACEHOLDER_COLOR);
				sectionBuf.replace(pos, pos + CSS_PLACEHOLDER_COLOR.length(), getCssColor(getColorPreference(preferenceKeyPrefix, index)));
				if (fragmentsCache != null && fragmentsCache.length > i) { // cache fragment if possible
					fragmentsCache[i]= sectionBuf.toString();
				}
				outputCss.append(sectionBuf);
			}
		}
		return sectionEnd + sectionEndLine.length();
	}

	private static String getCssColor(RGB color) {
		return "rgb(" + color.red + ", " + color.green + ", " + color.blue + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public static int getNumberOfTypeParamsReferences(String content) {
		return getMaxIndexOfStyle(content, String::indexOf, CSS_CLASS_TYPE_PARAMETERS_REFERENCE_PREFIX);
	}

	public static int getNumberOfTypeParamsLevels(String content) {
		return getMaxIndexOfStyle(content, String::indexOf, CSS_CLASS_TYPE_PARAMETERS_LEVEL_PREFIX);
	}

	private static <T extends CharSequence> int getMaxIndexOfStyle(T content, BiFunction<T, String, Integer> indexOfGetter, String stylePrefix) {
		int i= 0;
		while (indexOfGetter.apply(content, stylePrefix + ++i) != -1) { /* no-op */ }
		return i - 1;
	}

	public static Integer[] getColorPreferencesIndicesForTypeParamsReference() {
		return getColorPreferencesIndices(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR);
	}

	public static Integer[] getColorPreferencesIndicesForTypeParamsLevel() {
		return getColorPreferencesIndices(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR);
	}

	private static Integer[] getColorPreferencesIndices(String keyPrefix) {
		List<Integer> retVal= new ArrayList<>(MAX_COLOR_INDEX);
		for (int i= 1; i <= MAX_COLOR_INDEX; i++) {
			if (i <= 4) {
				// pretend first 4 colors are always set (since we have defaults for them)
				retVal.add(i);
			} else {
				String key= getColorPreferenceKey(keyPrefix, i);
				if (preferenceStore().contains(key)) {
					retVal.add(i);
				}
			}
		}
		return retVal.toArray(Integer[]::new);
	}

	public static void resetAllColorPreferencesToDefaults() {
		var store= preferenceStore();
		store.removePropertyChangeListener(COLOR_PROPERTIES_CHANGE_LISTENER);
		try {
			StringBuffer logMessage= new StringBuffer("Following custom color preferences were removed:"); //$NON-NLS-1$
			RGB customColor= null;
			for (int i= 1; i <= MAX_COLOR_INDEX; i++) {
				String key= getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_REFERENCE_COLOR, i);
				if (!store.isDefault(key)) {
					customColor= PreferenceConverter.getColor(store, key);
					logMessage.append("\n\t").append(key).append(" = ").append(customColor); //$NON-NLS-1$ //$NON-NLS-2$
					store.setToDefault(key);
				}
			}
			for (int i= 1; i <= MAX_COLOR_INDEX; i++) {
				String key= getColorPreferenceKey(PREFERENCE_KEY_PREFIX_TYPE_PARAMETERS_LEVEL_COLOR, i);
				if (!store.isDefault(key)) {
					customColor= PreferenceConverter.getColor(store, key);
					logMessage.append("\n\t").append(key).append(" = ").append(customColor); //$NON-NLS-1$ //$NON-NLS-2$
					store.setToDefault(key);
				}
			}
			cssFragmentsCacheResetListener(null);
			if (customColor != null) {
				JavaPlugin.log(new Status(IStatus.INFO, JavaPlugin.getPluginId(), logMessage.toString()));
			}
		} finally {
			store.addPropertyChangeListener(COLOR_PROPERTIES_CHANGE_LISTENER);
		}
	}

	private static String getColorPreferenceKey(String prefix, int index) {
		return prefix + index + PREFERENCE_KEY_POSTFIX_COLOR;
	}

	private static IPreferenceStore preferenceStore() {
		return PreferenceConstants.getPreferenceStore();
	}

	public static void addStylingConfigurationListener(IStylingConfigurationListener listener) {
		configListener.add(listener);
	}

	public static void removeStylingConfigurationListener(IStylingConfigurationListener listener) {
		configListener.remove(listener);
	}

	/**
	 * Styling configuration listener is notified when Javadoc styling enhancements are switched on or off via preference.
	 */
	public static interface IStylingConfigurationListener {
		/**
		 * Called when Javadoc styling enhancements have been toggled.
		 * @param isEnabled whether styling enhancements were turned on or off
		 */
		void stylingStateChanged(boolean isEnabled);
	}

}
