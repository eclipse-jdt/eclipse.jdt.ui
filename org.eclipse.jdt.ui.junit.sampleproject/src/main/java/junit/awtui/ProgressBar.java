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

public class ProgressBar extends Canvas {
	public boolean fError = false;
	public int fTotal = 0;
	public int fProgress = 0;
	public int fProgressX = 0;

	public ProgressBar() {
		super();
		setSize(20, 30);
	}

	private Color getStatusColor() {
		if (fError)
			return Color.red;
		return Color.green;
	}

	public void paint(Graphics g) {
		paintBackground(g);
		paintStatus(g);
	}

	public void paintBackground(Graphics g) {
		g.setColor(SystemColor.control);
		Rectangle r = getBounds();
		g.fillRect(0, 0, r.width, r.height);
		g.setColor(Color.darkGray);
		g.drawLine(0, 0, r.width - 1, 0);
		g.drawLine(0, 0, 0, r.height - 1);
		g.setColor(Color.white);
		g.drawLine(r.width - 1, 0, r.width - 1, r.height - 1);
		g.drawLine(0, r.height - 1, r.width - 1, r.height - 1);
	}

	public void paintStatus(Graphics g) {
		g.setColor(getStatusColor());
		Rectangle r = new Rectangle(0, 0, fProgressX, getBounds().height);
		g.fillRect(1, 1, r.width - 1, r.height - 2);
	}

	private void paintStep(int startX, int endX) {
		repaint(startX, 1, endX - startX, getBounds().height - 2);
	}

	public void reset() {
		fProgressX = 1;
		fProgress = 0;
		fError = false;
		paint(getGraphics());
	}

	public int scale(int value) {
		if (fTotal > 0)
			return Math.max(1, value * (getBounds().width - 1) / fTotal);
		return value;
	}

	public void setBounds(int x, int y, int w, int h) {
		super.setBounds(x, y, w, h);
		fProgressX = scale(fProgress);
	}

	public void start(int total) {
		fTotal = total;
		reset();
	}

	public void step(boolean successful) {
		fProgress++;
		int x = fProgressX;

		fProgressX = scale(fProgress);

		if (!fError && !successful) {
			fError = true;
			x = 1;
		}
		paintStep(x, fProgressX);
	}
}
