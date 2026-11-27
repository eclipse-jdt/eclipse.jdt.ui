/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.IJavaThemeConstants;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * Provides the labels for the Package Explorer.
 * <p>
 * It provides labels for the packages in hierarchical layout and in all
 * other cases delegates it to its super class.
 * </p>
 * @since 2.1
 */
public class PackageExplorerLabelProvider extends AppearanceAwareLabelProvider {

	private PackageExplorerContentProvider fContentProvider;
	private Map<ImageDescriptor, Image> fWorkingSetImages;

	private boolean fIsFlatLayout;
	private PackageExplorerProblemsDecorator fProblemDecorator;

	private final Set<String> fHighlightedPaths = new HashSet<>();

	private static final StyledString.Styler HIGHLIGHT_STYLER = new StyledString.Styler() {
		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.font = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
			textStyle.foreground = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
					.getColorRegistry().get(IJavaThemeConstants.PACKAGE_EXPLORER_HIGHLIGHT_COLOR);
		}
	};

	private final IPropertyChangeListener fColorChangeListener = event -> {
		if (IJavaThemeConstants.PACKAGE_EXPLORER_HIGHLIGHT_COLOR.equals(event.getProperty())) {
			notifyChanged();
		}
	};

	public PackageExplorerLabelProvider(PackageExplorerContentProvider cp) {
		super(DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED | JavaElementLabels.ALL_CATEGORY2, DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS);

		fProblemDecorator= new PackageExplorerProblemsDecorator();
		addLabelDecorator(fProblemDecorator);
		Assert.isNotNull(cp);
		fContentProvider= cp;
		fWorkingSetImages= null;
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
				.getColorRegistry().addListener(fColorChangeListener);
	}

	@Override
	public StyledString getStyledText(Object element) {
		String text= getSpecificText(element);
		StyledString result;
		if (text != null) {
			result= new StyledString(decorateText(text, element));
		} else {
			result= super.getStyledText(element);
		}
		IPath path = getResourcePath(element);
		if (isHighlighted(path) && isOpenableFile(element)) {
			result.setStyle(0, result.length(), HIGHLIGHT_STYLER);
		}
		return result;
	}

	private String getSpecificText(Object element) {
		if (!fIsFlatLayout && element instanceof IPackageFragment) {
			IPackageFragment fragment = (IPackageFragment) element;
			Object parent= fContentProvider.getHierarchicalPackageParent(fragment);
			if (parent instanceof IPackageFragment) {
				return getNameDelta((IPackageFragment) parent, fragment);
			} else if (parent instanceof IFolder) { // bug 152735
				return getNameDelta((IFolder) parent, fragment);
			}
		} else if (element instanceof IWorkingSet) {
			return ((IWorkingSet) element).getLabel();
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		String text= getSpecificText(element);
		if (text != null) {
			return decorateText(text, element);
		}
		return super.getText(element);
	}

	private String getNameDelta(IPackageFragment parent, IPackageFragment fragment) {
		String prefix= parent.getElementName() + '.';
		String fullName= fragment.getElementName();
		if (fullName.startsWith(prefix)) {
			return fullName.substring(prefix.length());
		}
		return fullName;
	}

	private String getNameDelta(IFolder parent, IPackageFragment fragment) {
		IPath prefix= parent.getFullPath();
		IPath fullPath= fragment.getPath();
		if (prefix.isPrefixOf(fullPath)) {
			StringBuilder buf= new StringBuilder();
			for (int i= prefix.segmentCount(); i < fullPath.segmentCount(); i++) {
				if (buf.length() > 0)
					buf.append('.');
				buf.append(fullPath.segment(i));
			}
			return buf.toString();
		}
		return fragment.getElementName();
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof IWorkingSet) {
			ImageDescriptor image= ((IWorkingSet)element).getImageDescriptor();
			if (image == null) {
				return null;
			}
			if (fWorkingSetImages == null) {
				fWorkingSetImages= new HashMap<>();
			}

			Image result= fWorkingSetImages.get(image);
			if (result == null) {
				result= image.createImage();
				fWorkingSetImages.put(image, result);
			}
			return decorateImage(result, element);
		}
		return super.getImage(element);
	}

	void toggleHighlight(IPath path) {
		if (path == null)
			return;
		String key = path.toString();
		if (!fHighlightedPaths.remove(key))
			fHighlightedPaths.add(key);
	}

	boolean isHighlighted(IPath path) {
		return path != null && fHighlightedPaths.contains(path.toString());
	}

	void notifyChanged() {
		fireLabelProviderChanged(new LabelProviderChangedEvent(this, null));
	}

	static boolean isOpenableFile(Object element) {
		if (element instanceof IFile)
			return true;
		if (element instanceof IJavaElement) {
			IResource resource = ((IJavaElement) element).getResource();
			return resource instanceof IFile;
		}
		return false;
	}

	private IPath getResourcePath(Object element) {
		if (element instanceof IResource) {
			return ((IResource) element).getFullPath();
		}
		if (element instanceof IJavaElement) {
			IResource resource = ((IJavaElement) element).getResource();
			if (resource != null)
				return resource.getFullPath();
		}
		return null;
	}

	public void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
		fProblemDecorator.setIsFlatLayout(state);
	}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
				.getColorRegistry().removeListener(fColorChangeListener);
		if (fWorkingSetImages != null) {
			for (Image image : fWorkingSetImages.values()) {
				image.dispose();
			}
		}
		super.dispose();
	}
}
