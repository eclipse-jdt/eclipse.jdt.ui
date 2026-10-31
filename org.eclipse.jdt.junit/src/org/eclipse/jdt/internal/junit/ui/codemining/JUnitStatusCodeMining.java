/*******************************************************************************
 * Copyright (c) 2017, 2026 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation,
 *         originally in https://github.com/angelozerr/jdt-codemining (EPL)
 *     Kai Winter <kaiwinter@gmx.de> - JUnit 5 ParameterizedTest support in jdt-codemining
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui.codemining;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.junit.model.ITestElement.Result;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

import org.eclipse.jdt.internal.ui.javaeditor.codemining.AbstractJavaElementLineHeaderCodeMining;

/**
 * Code mining shown above a test class or test method, which displays the result of its last run
 * as an icon.
 */
public class JUnitStatusCodeMining extends AbstractJavaElementLineHeaderCodeMining {

	private static final int ICON_LABEL_GAP= 4;

	// the images of each display, by name, only used by draw() in the UI thread; the display is
	// forgotten when it is disposed, and its images with the shared resource manager
	private static final Map<Display, Map<String, Image>> IMAGES= new HashMap<>();

	private final JUnitStatusRegistry.Results fResults;

	private volatile Result fResult;

	/**
	 * Creates the code mining.
	 *
	 * @param element the test class or test method
	 * @param results the results of the class of the element
	 * @param document the document of the editor
	 * @param provider the provider of the code mining
	 * @throws JavaModelException if the position of the element cannot be computed
	 * @throws BadLocationException if the position of the element cannot be computed
	 */
	public JUnitStatusCodeMining(IJavaElement element, JUnitStatusRegistry.Results results, IDocument document, ICodeMiningProvider provider)
			throws JavaModelException, BadLocationException {
		super(element, document, provider, null);
		fResults= results;
	}

	@Override
	protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor) {
		fResult= null; // not run, if there are no results or they cannot be read
		try {
			IJavaElement element= getElement();
			// there are no results if the class was not found when the code minings were computed
			if (fResults != null) {
				if (element instanceof IType) {
					fResult= fResults.findTypeResult(monitor);
				} else if (element instanceof IMethod method) {
					fResult= fResults.findResult(method);
				}
			}
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		} catch (OperationCanceledException e) {
			// the code minings are being computed again: do not show "Not run" for a result that was not resolved
			CompletableFuture<Void> cancelled= new CompletableFuture<>();
			cancelled.cancel(false);
			return cancelled;
		}
		setLabel(getLabel(fResult));
		return CompletableFuture.completedFuture(null);
	}

	private static String getLabel(Result result) {
		if (result == Result.OK) {
			return JUnitMessages.JUnitCodeMining_status_ok;
		} else if (result == Result.ERROR) {
			return JUnitMessages.JUnitCodeMining_status_error;
		} else if (result == Result.FAILURE) {
			return JUnitMessages.JUnitCodeMining_status_failure;
		} else if (result == Result.IGNORED) {
			return JUnitMessages.JUnitCodeMining_status_ignored;
		}
		return JUnitMessages.JUnitCodeMining_status_notRun;
	}

	@Override
	public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
		String label= getLabel();
		if (label == null) {
			return new Point(0, 0);
		}
		// the label is drawn at y like the default draw() of the other code minings, so that
		// it is aligned with them; the icon is centered on the text line
		Point extent= gc.textExtent(label);
		int width= 0;
		int height= extent.y;
		Image image= getImage(fResult, textWidget.getDisplay());
		if (image != null) {
			Rectangle bounds= image.getBounds();
			gc.drawImage(image, x, y + (extent.y - bounds.height) / 2);
			width= bounds.width + ICON_LABEL_GAP;
			height= Math.max(height, bounds.height);
		}
		// the label is also drawn if the icon is missing, so that the code mining is never empty
		gc.setForeground(color);
		gc.drawText(label, x + width, y, true);
		return new Point(width + extent.x, height);
	}

	private static Image getImage(Result result, Display display) {
		String name= "test.svg"; //$NON-NLS-1$
		if (result == Result.OK) {
			name= "testok.svg"; //$NON-NLS-1$
		} else if (result == Result.ERROR) {
			name= "testerr.svg"; //$NON-NLS-1$
		} else if (result == Result.FAILURE) {
			name= "testfail.svg"; //$NON-NLS-1$
		} else if (result == Result.IGNORED) {
			name= "testignored.svg"; //$NON-NLS-1$
		}
		try {
			Map<String, Image> images= IMAGES.get(display);
			if (images == null) {
				images= new HashMap<>();
				IMAGES.put(display, images);
				display.disposeExec(() -> IMAGES.remove(display));
			}
			Image image= images.get(name);
			if (image == null) {
				image= JFaceResources.getResources(display).create(JUnitPlugin.getImageDescriptor("obj16/" + name)); //$NON-NLS-1$
				images.put(name, image);
			}
			return image;
		} catch (DeviceResourceException e) {
			JUnitPlugin.log(e);
			return null;
		}
	}
}
