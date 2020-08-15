package junit.awtui;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import java.awt.*;
import java.awt.image.*;
import java.net.URL;

import junit.runner.BaseTestRunner;

public class Logo extends Canvas {
	private Image fImage;
	private int fWidth;
	private int fHeight;

	public Logo() {
		fImage = loadImage("logo.gif");
		MediaTracker tracker = new MediaTracker(this);
		tracker.addImage(fImage, 0);
		try {
			tracker.waitForAll();
		} catch (Exception e) {
		}

		if (fImage != null) {
			fWidth = fImage.getWidth(this);
			fHeight = fImage.getHeight(this);
		} else {
			fWidth = 20;
			fHeight = 20;
		}
		setSize(fWidth, fHeight);
	}

	public Image loadImage(String name) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		try {
			URL url = BaseTestRunner.class.getResource(name);
			return toolkit.createImage((ImageProducer) url.getContent());
		} catch (Exception ex) {
		}
		return null;
	}

	public void paint(Graphics g) {
		paintBackground(g);
		if (fImage != null)
			g.drawImage(fImage, 0, 0, fWidth, fHeight, this);
	}

	public void paintBackground(java.awt.Graphics g) {
		g.setColor(SystemColor.control);
		g.fillRect(0, 0, getBounds().width, getBounds().height);
	}
}
