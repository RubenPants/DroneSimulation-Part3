 package main;

import control.AppManager;
import interfaces.AutopilotModule;

public class MainLoopStack {
	
	public static void main(String[] args) {
		
		AppManager.createApp();
		AppManager.setModule(new AutopilotModule());
		
		while (!AppManager.closeRequested()){
			AppManager.updateApp();
		}
		AppManager.closeApp();
		System.exit(0);
	}
}
