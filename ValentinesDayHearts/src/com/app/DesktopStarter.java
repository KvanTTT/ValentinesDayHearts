package com.app;

import com.badlogic.gdx.backends.jogl.JoglApplication;

public class DesktopStarter {
	public static void main(String[] args) {
		new JoglApplication(new ValentinesDayHearts(),
				"Valentine's Day Hearts", 800, 480, false);
	}
}
