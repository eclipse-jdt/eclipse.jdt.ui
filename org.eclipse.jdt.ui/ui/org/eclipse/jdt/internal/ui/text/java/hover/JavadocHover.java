/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genady Beryozkin <eclipse@genady.org> - [hovering] tooltip for constant string does not show constant value - https://bugs.eclipse.org/bugs/show_bug.cgi?id=85382
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.internal.text.html.BrowserInformationControlInput;
import org.eclipse.jface.internal.text.html.HTMLPrinter;

import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension4;
import org.eclipse.jface.text.IInputChangedListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.SharedASTProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.jdt.internal.ui.infoviews.JavadocView;
import org.eclipse.jdt.internal.ui.text.javadoc.JavadocContentAccess2;
import org.eclipse.jdt.internal.ui.viewsupport.ImagesOnFileSystemRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;


/**
 * Provides Javadoc as hover info for Java elements.
 *
 * @since 2.1
 */
public class JavadocHover extends AbstractJavaEditorTextHover {
	
	/* FIXME:
	 * Links and button tooltips should include link target ('Back to ...')
	 */
	
	/**
	 * Action to go back to the previous input in the hover control.
	 * 
	 * @since 3.4
	 */
	private static final class BackAction extends Action {
		private final BrowserInformationControl fInfoControl;

		public BackAction(BrowserInformationControl infoControl) {
			fInfoControl= infoControl;
			setText(JavaHoverMessages.JavadocHover_back);
			ISharedImages images= PlatformUI.getWorkbench().getSharedImages();
			setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_BACK));
			setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_BACK_DISABLED));
		}
		
		public void run() {
			BrowserInformationControlInput previous= fInfoControl.getInput().getPrevious();
			if (previous != null) {
				fInfoControl.setInput(previous);
			}
		}
	}

	/**
	 * Action to go forward to the next input in the hover control.
	 * 
	 * @since 3.4
	 */
	private static final class ForwardAction extends Action {
		private final BrowserInformationControl fInfoControl;

		public ForwardAction(BrowserInformationControl infoControl) {
			fInfoControl= infoControl;
			setText(JavaHoverMessages.JavadocHover_forward);
			ISharedImages images= PlatformUI.getWorkbench().getSharedImages();
			setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
			setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD_DISABLED));
		}
		
		public void run() {
			BrowserInformationControlInput next= fInfoControl.getInput().getNext();
			if (next != null) {
				fInfoControl.setInput(next);
			}
		}
	}

	/**
	 * Action that shows the current hover contents in the Javadoc view.
	 * 
	 * @since 3.4
	 */
	private static final class ShowInJavadocViewAction extends Action {
		private final BrowserInformationControl fInfoControl;

		public ShowInJavadocViewAction(BrowserInformationControl infoControl) {
			fInfoControl= infoControl;
			setText(JavaHoverMessages.JavadocHover_showInJavadoc);
			setImageDescriptor(JavaPluginImages.DESC_OBJS_JAVADOCTAG); //TODO: better image
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			JavadocBrowserInformationControlInput infoInput= (JavadocBrowserInformationControlInput) fInfoControl.getInput(); //TODO: check cast
			fInfoControl.notifyDelayedInputChange(null);
			fInfoControl.dispose(); //FIXME: should have protocol to hide, rather than dispose
			try {
				JavadocView view= (JavadocView) JavaPlugin.getActivePage().showView(JavaUI.ID_JAVADOC_VIEW);
				view.setInput(infoInput.getElement()); //TODO: should set infoInput to retain history
			} catch (PartInitException e) {
				JavaPlugin.log(e);
			}
		}
	}
	

//	/**
//	 * Action that opens the external Javadoc for the current element.
//	 *
//	 * @since 3.4
//	 */
//	private static final class ShowExternalJavadocAction extends Action {
//		private final BrowserInformationControl fInfoControl;
//		private final Shell fParent;
//
//		public ShowExternalJavadocAction(BrowserInformationControl infoControl, Shell parent) {
//			fInfoControl= infoControl;
//			fParent= parent;
//			setText("Show External Javadoc");
//			setImageDescriptor(JavaPluginImages.DESC_OBJS_JAVADOC_LOCATION_ATTRIB); //TODO: better image
//		}
//
//		/*
//		 * @see org.eclipse.jface.action.Action#run()
//		 */
//		public void run() {
//			JavadocBrowserInformationContolInput infoInput= (JavadocBrowserInformationContolInput) fInfoControl.getInput(); //TODO: check cast
//			fInfoControl.notifyDelayedInputChange(null);
//			fInfoControl.dispose(); //FIXME: should have protocol to hide, rather than dispose
////			new OpenExternalJavadocAction(infoInput.getElement(), fShell) //TODO: split up reusable parts into non-api class
//		}
//	}
	
	/**
	 * Action that opens the current hover input element.
	 * 
	 * @since 3.4
	 */
	private static final class OpenDeclarationAction extends Action {
		private final BrowserInformationControl fInfoControl;

		public OpenDeclarationAction(BrowserInformationControl infoControl) {
			fInfoControl= infoControl;
			setText(JavaHoverMessages.JavadocHover_openDeclaration);
			JavaPluginImages.setLocalImageDescriptors(this, "goto_input.gif"); //$NON-NLS-1$ //TODO: better images
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			JavadocBrowserInformationControlInput infoInput= (JavadocBrowserInformationControlInput) fInfoControl.getInput(); //TODO: check cast
			fInfoControl.notifyDelayedInputChange(null);
			fInfoControl.dispose(); //FIXME: should have protocol to hide, rather than dispose

			try {
				//FIXME: add hover location to editor navigation history?
				JavaUI.openInEditor(infoInput.getElement());
			} catch (PartInitException e) {
				JavaPlugin.log(e);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
	}
	

	/**
	 * Presenter control creator.
	 * 
	 * @since 3.3
	 */
	private static final class PresenterControlCreator extends AbstractReusableInformationControlCreator {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl doCreateInformationControl(Shell parent) {
			int shellStyle= SWT.RESIZE | SWT.TOOL;
			int style= SWT.V_SCROLL | SWT.H_SCROLL;
			if (BrowserInformationControl.isAvailable(parent)) {
				ToolBarManager tbm= new ToolBarManager(SWT.FLAT);
				String font= PreferenceConstants.APPEARANCE_JAVADOC_FONT;
				BrowserInformationControl iControl= new BrowserInformationControl(parent, shellStyle, style, font, tbm);
				
				final BackAction backAction= new BackAction(iControl);
				backAction.setEnabled(false);
				tbm.add(backAction);
				final ForwardAction forwardAction= new ForwardAction(iControl);
				tbm.add(forwardAction);
				forwardAction.setEnabled(false);
				IInputChangedListener inputChangeListener= new IInputChangedListener() {
					public void inputChanged(Object newInput) {
						if (newInput == null) {
							backAction.setEnabled(false);
							forwardAction.setEnabled(false);
						} else {
							JavadocBrowserInformationControlInput javaInput= (JavadocBrowserInformationControlInput) newInput;
							backAction.setEnabled(javaInput.getPrevious() != null);
							forwardAction.setEnabled(javaInput.getNext() != null);
						}
					}
				};
				iControl.addInputChangeListener(inputChangeListener);
				
				tbm.add(new ShowInJavadocViewAction(iControl));
//				tbm.add(new ShowExternalJavadocAction(iControl, parent));
				tbm.add(new OpenDeclarationAction(iControl));
				tbm.update(true);
				
				addLinkListener(iControl);
				return iControl;
				
			} else {
				return new DefaultInformationControl(parent, true);
			}
		}
	}

	
	/**
	 * Hover control creator.
	 * 
	 * @since 3.3
	 */
	private static final class HoverControlCreator extends AbstractReusableInformationControlCreator {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl doCreateInformationControl(Shell parent) {
			int shellStyle= SWT.TOOL | SWT.NO_TRIM;
			int style= SWT.NONE;
			if (BrowserInformationControl.isAvailable(parent)) {
				String font= PreferenceConstants.APPEARANCE_JAVADOC_FONT;
				BrowserInformationControl iControl= new BrowserInformationControl(parent, shellStyle, style, font, EditorsUI.getTooltipAffordanceString());
				addLinkListener(iControl);
				return iControl;
			} else {
				return new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString());
			}
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#canReuse(org.eclipse.jface.text.IInformationControl)
		 */
		public boolean canReuse(IInformationControl control) {
			if (!super.canReuse(control))
				return false;
			
			if (control instanceof IInformationControlExtension4)
				((IInformationControlExtension4)control).setStatusText(EditorsUI.getTooltipAffordanceString());
			
			return true;
		}
	}

	private static final long LABEL_FLAGS=  JavaElementLabels.ALL_FULLY_QUALIFIED
		| JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_EXCEPTIONS
		| JavaElementLabels.F_PRE_TYPE_SIGNATURE | JavaElementLabels.M_PRE_TYPE_PARAMETERS | JavaElementLabels.T_TYPE_PARAMETERS
		| JavaElementLabels.USE_RESOLVED;
	private static final long LOCAL_VARIABLE_FLAGS= LABEL_FLAGS & ~JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.F_POST_QUALIFIED;

	
	/**
	 * The hover control creator.
	 * 
	 * @since 3.2
	 */
	private IInformationControlCreator fHoverControlCreator;
	/**
	 * The presentation control creator.
	 * 
	 * @since 3.2
	 */
	private IInformationControlCreator fPresenterControlCreator;

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension2#getInformationPresenterControlCreator()
	 * @since 3.1
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fPresenterControlCreator == null)
			fPresenterControlCreator= new PresenterControlCreator();
		return fPresenterControlCreator;
	}

	/*
	 * @see ITextHoverExtension#getHoverControlCreator()
	 * @since 3.2
	 */
	public IInformationControlCreator getHoverControlCreator() {
		if (fHoverControlCreator == null)
			fHoverControlCreator= new HoverControlCreator();
		return fHoverControlCreator;
	}

	private static void addLinkListener(final BrowserInformationControl control) {
		control.addLocationListener(new LocationAdapter() {
			public void changing(LocationEvent event) {
				String loc= event.location;
//				System.out.println("JavadocHover: changing location:" + loc);
				URI uri;
				try {
					uri= new URI(loc);
				} catch (URISyntaxException e) {
					JavaPlugin.log(e);
					return;
				}
				
				String scheme= uri.getScheme();
				if (JavaElementLinks.JAVADOC_VIEW_SCHEME.equals(scheme)) {
					handleJavadocViewLink(uri);
				} else if (JavaElementLinks.JAVADOC_SCHEME.equals(scheme)) {
					handleInlineJavadocLink(uri);
				} else if (JavaElementLinks.OPEN_LINK_SCHEME.equals(scheme)) {
					handleDeclarationLink(uri);
				} else if (!"about:blank".equals(loc)) { //$NON-NLS-1$
					/*
					 * Using the Browser.setText API triggers a location change to "about:blank".
					 * XXX: remove this code once https://bugs.eclipse.org/bugs/show_bug.cgi?id=130314 is fixed
					 */
					if (loc.startsWith("about:")) //$NON-NLS-1$
						return; //FIXME: handle relative links
					
					control.notifyDelayedInputChange(null);
					control.dispose(); //FIXME: should have protocol to hide, rather than dispose
					try {
						// open external links in real browser:
						OpenBrowserUtil.open(new URL(loc), event.display, ""); //$NON-NLS-1$
					} catch (MalformedURLException e) {
						JavaPlugin.log(e);
					}
				}
			}

			private void handleJavadocViewLink(URI uri) {
				IJavaElement linkTarget= JavaElementLinks.parseURI(uri);
				if (linkTarget == null)
					return;

				control.notifyDelayedInputChange(null);
				control.dispose(); //FIXME: should have protocol to hide, rather than dispose
				try {
					JavadocView view= (JavadocView) JavaPlugin.getActivePage().showView(JavaUI.ID_JAVADOC_VIEW);
					view.setInput(linkTarget);
				} catch (PartInitException e) {
					JavaPlugin.log(e);
				}
			}

			private void handleInlineJavadocLink(URI uri) {
				IJavaElement linkTarget= JavaElementLinks.parseURI(uri);
				if (linkTarget == null)
					return;

				JavadocBrowserInformationControlInput hoverInfo= getHoverInfo(new IJavaElement[] { linkTarget }, null, (JavadocBrowserInformationControlInput) control.getInput());
				if (control.hasDelayedInputChangeListener())
					control.notifyDelayedInputChange(hoverInfo);
				else
					control.setInput(hoverInfo);
			}

			private void handleDeclarationLink(URI uri) {
				IJavaElement linkTarget= JavaElementLinks.parseURI(uri);
				if (linkTarget == null)
					return;

				control.notifyDelayedInputChange(null);
				control.dispose(); //FIXME: should have protocol to hide, rather than dispose
				try {
					//FIXME: add hover location to editor navigation history?
					JavaUI.openInEditor(linkTarget);
				} catch (PartInitException e) {
					JavaPlugin.log(e);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		});
	}
	
	/*
	 * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		return ((JavadocBrowserInformationControlInput) getHoverInfo2(textViewer, hoverRegion)).getHtml();
	}

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension2#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		return internalGetHoverInfo(textViewer, hoverRegion);
	}

	private JavadocBrowserInformationControlInput internalGetHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		IJavaElement[] elements= getJavaElementsAt(textViewer, hoverRegion);
		if (elements == null || elements.length == 0)
			return null;
		
		String constantValue;
		if (elements.length == 1 && elements[0].getElementType() == IJavaElement.FIELD) {
			constantValue= getConstantValue((IField) elements[0], hoverRegion);
		} else {
			constantValue= null;
		}
		
		return getHoverInfo(elements, constantValue, null);
	}

	/**
	 * Computes the hover info.
	 * 
	 * @param elements the resolved elements
	 * @param constantValue a constant value iff result contains exactly 1 constant field, or <code>null</code>
	 * @param previousInput the previous input, or <code>null</code>
	 * @return the HTML hover info for the given element(s) or <code>null</code> if no information is available
	 * @since 3.4
	 */
	private static JavadocBrowserInformationControlInput getHoverInfo(IJavaElement[] elements, String constantValue, JavadocBrowserInformationControlInput previousInput) {
		int nResults= elements.length;
		StringBuffer buffer= new StringBuffer();
		boolean hasContents= false;
		String base= null;
		IJavaElement element= null;
		
		int leadingImageWidth= 0;
		
		if (nResults > 1) {

			for (int i= 0; i < elements.length; i++) {
				HTMLPrinter.startBulletList(buffer);
				IJavaElement curr= elements[i];
				if (curr instanceof IMember || curr.getElementType() == IJavaElement.LOCAL_VARIABLE) {
					//FIXME: provide links
					HTMLPrinter.addBullet(buffer, getInfoText(curr, constantValue, false));
					hasContents= true;
				}
				HTMLPrinter.endBulletList(buffer);
			}

		} else {

			element= elements[0];
			if (element instanceof IMember) {
				IMember member= (IMember) element;
				HTMLPrinter.addSmallHeader(buffer, getInfoText(member, constantValue, true));
				Reader reader;
				try {
//					reader= JavadocContentAccess.getHTMLContentReader(member, true, true);
					String content= JavadocContentAccess2.getHTMLContent(member, true, true);
					reader= content == null ? null : new StringReader(content);
					
					// Provide hint why there's no Javadoc
					if (reader == null && member.isBinary()) {
						boolean hasAttachedJavadoc= JavaDocLocations.getJavadocBaseLocation(member) != null;
						IPackageFragmentRoot root= (IPackageFragmentRoot)member.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
						boolean hasAttachedSource= root != null && root.getSourceAttachmentPath() != null;
						IOpenable openable= member.getOpenable();
						boolean hasSource= openable.getBuffer() != null;

						if (!hasAttachedSource && !hasAttachedJavadoc)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noAttachments);
						else if (!hasAttachedJavadoc && !hasSource)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noAttachedJavadoc);
						else if (!hasAttachedSource)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noAttachedSource);
						else if (!hasSource)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noInformation);
					}
					
				} catch (JavaModelException ex) {
					reader= new StringReader(JavaHoverMessages.JavadocHover_error_gettingJavadoc);
					JavaPlugin.log(ex);
				}
				
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, reader);
				}
				hasContents= true;
				try {
					base= JavaElementLinks.createURI(JavaElementLinks.JAVADOC_SCHEME, member);
				} catch (URISyntaxException e) {
					JavaPlugin.log(e);
				}
			} else if (element.getElementType() == IJavaElement.LOCAL_VARIABLE || element.getElementType() == IJavaElement.TYPE_PARAMETER) {
				HTMLPrinter.addSmallHeader(buffer, getInfoText(element, constantValue, true));
				hasContents= true;
			}
			leadingImageWidth= 20;
		}
		
		if (!hasContents)
			return null;

		if (buffer.length() > 0) {
			HTMLPrinter.insertPageProlog(buffer, 0, getStyleSheet());
			if (base != null) {
				//TODO: base URI only makes sense if URI is hierarchical
//				int endHeadIdx= buffer.indexOf("</head>"); //$NON-NLS-1$
//				buffer.insert(endHeadIdx, "\n<base href='" + base + "'>\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			HTMLPrinter.addPageEpilog(buffer);
			return new JavadocBrowserInformationControlInput(previousInput, element, buffer.toString(), leadingImageWidth);
		}

		return null;
	}

	private static String getInfoText(IJavaElement member, String constantValue, boolean allowImage) {
		long flags= member.getElementType() == IJavaElement.LOCAL_VARIABLE ? LOCAL_VARIABLE_FLAGS : LABEL_FLAGS;
		StringBuffer label= new StringBuffer(JavaElementLabels.getElementLabel(member, flags));
		if (member.getElementType() == IJavaElement.FIELD) {
			if (constantValue != null) {
				IJavaProject javaProject= member.getJavaProject();
				if (JavaCore.INSERT.equals(javaProject.getOption(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, true)))
					label.append(' ');
				label.append('=');
				if (JavaCore.INSERT.equals(javaProject.getOption(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR, true)))
					label.append(' ');
				label.append(constantValue);
			}
		}
		
		StringBuffer buf= new StringBuffer();
		String divStyleAddition= ""; //$NON-NLS-1$
		
		if (allowImage) {
			ImagesOnFileSystemRegistry store= JavaPlugin.getDefault().getImagesOnFSRegistry();
			URL imageUrl= store.getImageURL(member);
			
			if (imageUrl != null) {
				// the image, with absolute placement
				buf.append("<img style='width: 16px; height: 16px; position: absolute; top: 2px; left: 2px;' src='").append(imageUrl.toExternalForm()).append("'/>"); //$NON-NLS-1$ //$NON-NLS-2$
				// add margin top the rest
				divStyleAddition= "margin-left: 20px; margin-top: 2px;"; //$NON-NLS-1$
			}
		}

		buf.append("<div style='word-wrap:break-word;"); // qualified names can become quite long -> allow wrapping inside word (CSS3) //$NON-NLS-1$
		buf.append(divStyleAddition).append("'>"); //$NON-NLS-1$

		for (int i= 0; i < label.length(); i++) {
			char ch= label.charAt(i);
			if (ch == '<') {
				buf.append("&lt;"); //$NON-NLS-1$
			} else if (ch == '>') {
				buf.append("&gt;"); //$NON-NLS-1$
			} else {
				buf.append(ch);
			}
		}
		buf.append("</div>"); //$NON-NLS-1$
		return buf.toString();
	}

	/*
	 * @since 3.4
	 */
	private static boolean isStaticFinal(IField field) {
		try {
			return JdtFlags.isFinal(field) && JdtFlags.isStatic(field);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}
	}

	/**
	 * Returns the constant value for the given field.
	 * 
	 * @param field the field
	 * @param hoverRegion the hover region
	 * @return the constant value for the given field or <code>null</code> if none
	 * @since 3.4
	 */
	private String getConstantValue(IField field, IRegion hoverRegion) {
		if (!isStaticFinal(field))
			return null;
		
		ITypeRoot typeRoot= getEditorInputJavaElement();
		if (typeRoot == null)
			return null;
		
		Object constantValue= null;
		
		CompilationUnit unit= SharedASTProvider.getAST(typeRoot, SharedASTProvider.WAIT_ACTIVE_ONLY, null);
		if (unit == null)
			return null;
		
		ASTNode node= NodeFinder.perform(unit, hoverRegion.getOffset(),	hoverRegion.getLength());
		if (node != null && node.getNodeType() == ASTNode.SIMPLE_NAME) {
			IBinding binding= ((SimpleName)node).resolveBinding();
			if (binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding= (IVariableBinding)binding;
				if (field.equals(variableBinding.getJavaElement())) {
					constantValue= variableBinding.getConstantValue();
				}
			}
		}
		if (constantValue == null)
			return null;
		
		if (constantValue instanceof String) {
			StringBuffer result= new StringBuffer();
			result.append('"');
			String stringConstant= (String)constantValue;
			if (stringConstant.length() > 80) {
				result.append(stringConstant.substring(0, 80));
				result.append(JavaElementLabels.ELLIPSIS_STRING);
			} else {
				result.append(stringConstant);
			}
			result.append('"');
			return result.toString();
			
		} else if (constantValue instanceof Character) {
			String constantResult= '\'' + constantValue.toString() + '\'';
			
			char charValue= ((Character) constantValue).charValue();
			String hexString= Integer.toHexString(charValue);
			StringBuffer hexResult= new StringBuffer("\\u"); //$NON-NLS-1$
			for (int i= hexString.length(); i < 4; i++) {
				hexResult.append('0');
			}
			hexResult.append(hexString);
			return formatWithHexValue(constantResult, hexResult.toString());
			
		} else if (constantValue instanceof Byte) {
			int byteValue= ((Byte) constantValue).intValue() & 0xFF;
			return formatWithHexValue(constantValue, "0x" + Integer.toHexString(byteValue)); //$NON-NLS-1$
			
		} else if (constantValue instanceof Short) {
			int shortValue= ((Short) constantValue).shortValue() & 0xFFFF;
			return formatWithHexValue(constantValue, "0x" + Integer.toHexString(shortValue)); //$NON-NLS-1$
			
		} else if (constantValue instanceof Integer) {
			int intValue= ((Integer) constantValue).intValue();
			return formatWithHexValue(constantValue, "0x" + Integer.toHexString(intValue)); //$NON-NLS-1$
			
		} else if (constantValue instanceof Long) {
			long longValue= ((Long) constantValue).longValue();
			return formatWithHexValue(constantValue, "0x" + Long.toHexString(longValue)); //$NON-NLS-1$
			
		} else {
			return constantValue.toString();
		}
	}

	/**
	 * Creates and returns the a formatted message for the given
	 * constant with its hex value.
	 * 
	 * @param constantValue
	 * @param hexValue
	 * @return a formatted string with constant and hex values
	 * @since 3.4
	 */
	private static String formatWithHexValue(Object constantValue, String hexValue) {
		return Messages.format(JavaHoverMessages.JavadocHover_constantValue_hexValue, new String[] { constantValue.toString(), hexValue });
	}
}
