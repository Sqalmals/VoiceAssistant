package com.presnall.oscar.voiceassistant;

import java.io.IOException;

public class Main {

	private static String output = "";

	private static IntentTrainer it = new IntentTrainer();
	private static VoiceRecognizer vr = new VoiceRecognizer();

	// processes input and throws out unnecessary data
	public static void process(String in) throws InterruptedException, IOException {
		output = in.substring(in.indexOf(':') + 3, in.length() - 3);
		if (output.equals("stop")) {
			it.stop();
			vr.stop();
		}
		getIntent(output);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		it.start(); // starts the intent processing thread

		vr.start(); // starts the voice recognition thread
	}

	private static void getIntent(String in) throws InterruptedException, IOException {
		// sends input to the intent processor
		System.out.println(in);
		it.sendInput(in);
		Thread.sleep(50);
		// gets the intent
		String[] intentAndArgument = it.getIntent();
		System.out.println(String.format("Intent: %s, Argument: %s", intentAndArgument[0], intentAndArgument[1]));
		// sends intent to command handler
		CommandHandler.runCommandWithIntent(intentAndArgument[0], intentAndArgument[1]);
	}

}
