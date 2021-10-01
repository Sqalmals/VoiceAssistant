package com.presnall.oscar.voiceassistant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;

public class Main {

	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;

	private static boolean isCurrent = false;
	private static String output;

	private static boolean connected = false;

	public void startConnection(String ip, int port) throws UnknownHostException, IOException {
		clientSocket = new Socket(ip, port);
		out = new PrintWriter(clientSocket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}

	public void sendMessage(String msg) throws IOException {
		out.println(msg);
	}

	public void stopConnection() throws IOException {
		in.close();
		out.close();
		clientSocket.close();
	}

	public String getInput() throws IOException {
		String input = in.readLine();

		if (input != null && input.contains("}{"))
			return "";
		else
			return input;
	}

	// Filters input and throws out unnecessary data
	public static void filter(String in) {
		if (in.contains("\"text\"")) {
			isCurrent = true;
			output = in.substring(in.indexOf(':') + 3, in.length() - 1);
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		Main client = new Main();
		System.out.println("Starting python voice recognition server...");
		Runtime.getRuntime().exec(
				"cmd.exe /c cd \"" + Paths.get("").toAbsolutePath().toString() + "\\src\\main\\resources\\\" & start cmd.exe /k \"python Recognizer.py\"");
		for (int i = 0; i < 100000; i++) {
			try {
				client.startConnection("127.0.0.1", 25567);
				System.out.println("Connected!");
				connected = true;
				break;
			} catch (Exception e) {
			}
		}

		if (!connected) {
			System.out.println("Could not connect to voice recognition server");
			return;
		}

		IntentTrainer it = new IntentTrainer();
		it.start();

		while (true) {

			String input = client.getInput();
			if (input == null)
				break;

			filter(input);

			if (isCurrent) {
				System.out.println(output);
				it.sendInput(output);
				Thread.sleep(50);
				String[] intentAndArgument = it.getIntent();
				System.out.println(String.format("Intent: %s, Argument: %s", intentAndArgument[0], intentAndArgument[1]));
				CommandHandler.runCommandWithIntent(intentAndArgument[0], intentAndArgument[1]);
				isCurrent = false;
			}
		}

		client.stopConnection();
		it.stop();

	}

}
