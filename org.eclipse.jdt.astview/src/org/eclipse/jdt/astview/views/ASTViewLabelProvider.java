/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

package org.eclipse.jdt.astview.views;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;

public class ASTViewLabelProvider extends LabelProvider implements IColorProvider, IFontProvider {
	private int fSelectionStart;
	private int fSelectionLength;

	private final Color fBlue, fRed, fDarkGray, fDarkGreen, fDarkRed;
	private final Font fBold;

	//to dispose:
	private final Font fAllocatedBoldItalic;
	private final Color fLightRed;
	private Color fSelectedElemBGColor;

	public ASTViewLabelProvider() {
		fSelectionStart= -1;
		fSelectionLength= -1;

		Display display= Display.getCurrent();

		fRed= display.getSystemColor(SWT.COLOR_RED);
		fDarkGray= display.getSystemColor(SWT.COLOR_DARK_GRAY);
		fBlue= display.getSystemColor(SWT.COLOR_DARK_BLUE);
		fDarkGreen= display.getSystemColor(SWT.COLOR_DARK_GREEN);
		fDarkRed= display.getSystemColor(SWT.COLOR_DARK_RED);

		fSelectedElemBGColor= new Color(display, 232, 242, 254);
		String currLineColor= EditorsUI.getPreferenceStore().getString(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR);
		String[] rgb= currLineColor.split(","); //$NON-NLS-1$
		if (rgb.length == 3) {
			try {
				fSelectedElemBGColor= new Color(display, Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
			} catch (NumberFormatException e) {
				// do nothing, colour would remain the backup value
			}
		}
		fLightRed= new Color(display, 255, 190, 190);

		fBold= PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		FontData[] fontData= fBold.getFontData();
		for (FontData fd : fontData) {
			fd.setStyle(fd.getStyle() | SWT.ITALIC);
		}
		fAllocatedBoldItalic= new Font(display, fontData);
	}

	public void setSelectedRange(int start, int length) {
		fSelectionStart= start;
		fSelectionLength= length;
		 // could be made more efficient by only updating selected node and parents (of old and new selection)
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}

	@Override
	public String getText(Object obj) {
		StringBuffer buf= new StringBuffer();
		if (obj instanceof ASTNode) {
			getNodeType((ASTNode) obj, buf);
		} else if (obj instanceof ASTAttribute) {
			buf.append(((ASTAttribute) obj).getLabel());
		}
		return buf.toString();
	}

	private void getNodeType(ASTNode node, StringBuffer buf) {
		buf.append(Signature.getSimpleName(node.getClass().getName()));
		buf.append(" ["); //$NON-NLS-1$
		buf.append(node.getStartPosition());
		buf.append("+"); //$NON-NLS-1$
		buf.append(node.getLength());
		buf.append(']');
		if ((node.getFlags() & ASTNode.MALFORMED) != 0) {
			buf.append(" (malformed)"); //$NON-NLS-1$
		}
		if ((node.getFlags() & ASTNode.RECOVERED) != 0) {
			buf.append(" (recovered)"); //$NON-NLS-1$
		}
	}


	@Override
	public Image getImage(Object obj) {
		if (obj instanceof ASTNode) {
			return null;
		} else if (obj instanceof ASTAttribute) {
			return ((ASTAttribute) obj).getImage();
		}

		return null;
//		String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
//		if (obj instanceof ASTNode) {
//			imageKey = ISharedImages.IMG_OBJ_FOLDER;
//		}
//		return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
	}

	@Override
	public Color getForeground(Object element) {
		if ((element instanceof Error))
			return fRed;
		if ((element instanceof ExceptionAttribute) && ((ExceptionAttribute) element).getException() != null)
			return fRed;

		if (element instanceof ASTNode) {
			ASTNode node= (ASTNode) element;
			if ((node.getFlags() & ASTNode.MALFORMED) != 0) {
				return fRed;
			}
			return fDarkGray;
		} else if (element instanceof Binding) {
			Binding binding= (Binding) element;
			if (!binding.isRelevant())
				return fDarkGray;
			return fBlue;
		} else if (element instanceof NodeProperty) {
			return null; // normal color
		} else if (element instanceof BindingProperty) {
			BindingProperty binding= (BindingProperty) element;
			if (!binding.isRelevant())
				return fDarkGray;
			return fBlue;
		} else if (element instanceof JavaElement) {
			JavaElement javaElement= (JavaElement) element;
			if (javaElement.getJavaElement() == null || ! javaElement.getJavaElement().exists()) {
				return fRed;
			}
			return fDarkGreen;
		}
		return fDarkRed; // all extra properties
	}

	@Override
	public Color getBackground(Object element) {
		if (isNotProperlyNested(element)) {
			return fLightRed;
		}
		if (fSelectionStart != -1 && isInside(element)) {
			return fSelectedElemBGColor;
		}
		return null;
	}

	private boolean isNotProperlyNested(Object element) {
		if (element instanceof ASTNode) {
			ASTNode node= (ASTNode) element;
			int start= node.getStartPosition();
			int end= start + node.getLength();

			ASTNode parent= node.getParent();
			if (parent != null) {
				int parentstart= parent.getStartPosition();
				int parentend= parentstart + parent.getLength();

				if (start < parentstart || end > parentend) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isInsideNode(ASTNode node) {
		int start= node.getStartPosition();
		int end= start + node.getLength();
		if (start <= fSelectionStart && (fSelectionStart + fSelectionLength) <= end) {
			return true;
		}
		return false;
	}

	private boolean isInside(Object element) {
		if (element instanceof ASTNode) {
			return isInsideNode((ASTNode) element);
		} else if (element instanceof NodeProperty) {
			NodeProperty property= (NodeProperty) element;
			Object object= property.getNode();
			if (object instanceof ASTNode) {
				return isInsideNode((ASTNode) object);
			} else if (object instanceof List) {
				for (Object child : (List<?>) object) {
					if (isInside(child)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public Font getFont(Object element) {
		if (element instanceof ASTNode) {
			ASTNode node= (ASTNode) element;
			if ((node.getFlags() & ASTNode.RECOVERED) != 0)
				return fAllocatedBoldItalic;
			else
				return fBold;
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		fAllocatedBoldItalic.dispose();
	}

}
