package junit.swingui;

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

import java.awt.Color;

import javax.swing.JProgressBar;

/**
 * A progress bar showing the green/red status
 */
class ProgressBar extends JProgressBar {
	boolean fError = false;

	public ProgressBar() {
		super();
		setForeground(getStatusColor());
	}

	private Color getStatusColor() {
		if (fError)
			return Color.red;
		return Color.green;
	}

	public void reset() {
		fError = false;
		setForeground(getStatusColor());
		setValue(0);
	}

	public void start(int total) {
		setMaximum(total);
		reset();
	}

	public void step(int value, boolean successful) {
		setValue(value);
		if (!fError && !successful) {
			fError = true;
			setForeground(getStatusColor());
		}
	}
}
