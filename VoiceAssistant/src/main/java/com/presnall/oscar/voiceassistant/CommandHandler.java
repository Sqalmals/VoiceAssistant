package com.presnall.oscar.voiceassistant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

public class CommandHandler {

	private static ArrayList<String[]> programs;

	public static void runCommandWithIntent(String intent, String argument) throws IOException {
		if(intent == null) {
			return;
		}
		switch (intent) {
		case "open-program":
			if (programs == null)
				programs = readFileByLine("src/main/resources/programs.txt");
			for (int i = 0; i < programs.size(); i++) {
				if (programs.get(i)[0].equals(argument) && programs.get(i).length < 4) {
					Runtime.getRuntime().exec("cmd.exe /c start \"\" \"" + programs.get(i)[1] + "\"");
					// System.out.println("cmd.exe /c start \"\" \"" + programs.get(i)[1] + "\"");
					TextToSpeech.speak("Opened " + argument);
					break;
				} else if (programs.get(i).length == 4
						&& (programs.get(i)[0].equals(argument) || programs.get(i)[1].equals(argument))) {
					Runtime.getRuntime().exec("cmd.exe /c start \"\" \"" + programs.get(i)[2] + "\"");
					TextToSpeech.speak("Opened " + argument);
					break;
				}
			}
			break;
		case "close-program":
			if (argument.length() < 3)
				return;
			if (programs == null)
				programs = readFileByLine("src/main/resources/programs.txt");
			for (int i = 0; i < programs.size(); i++) {
				String processName = programs.get(i)[programs.get(i).length - 1];
				if (programs.get(i)[0].equals(argument) && programs.get(i).length < 4) {
					Runtime.getRuntime().exec("cmd.exe /c TASKKILL /IM \"" + processName);
					TextToSpeech.speak("Closed " + argument);
					break;
				} else if (programs.get(i).length == 4 && (programs.get(i)[0].equals(argument) || programs.get(i)[1].equals(argument))) {
					Runtime.getRuntime().exec("cmd.exe /c TASKKILL /IM \"" + processName);
					TextToSpeech.speak("Closed " + argument);
					break;
				}
			}
			break;
		case "current-weather":
			if(argument.length() <= 0 || argument.equals("now") || argument.equals("today")) {
				String[] weather = WeatherHandler.getWeather("Mobile%2C%20AL", LocalDate.now().toString(), LocalDate.now().toString(), "us", "SE9N9CDNS9TH62N5TYJEWXZAR").split(";");
				System.out.printf("In %s, the high for today is %s and the low is %s\n", "Mobile", weather[1], weather[2]);
				TextToSpeech.speak(String.format("In %s, the high for today is %s and the low is %s\n", "Mobile", weather[1], weather[2]));
			}
			break;
		case "say":
			TextToSpeech.speak(argument);
			break;
		}
		
	}

	private static ArrayList<String[]> readFileByLine(String fileName) {
		ArrayList<String[]> output = new ArrayList<String[]>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = reader.readLine();
			while (line != null) {
				String[] temp = new String[3];
				temp = line.split(";");
				if (temp[0].contains("/")) {
					String[] temp2 = { temp[0].split("/")[0], temp[0].split("/")[1], temp[1], temp[2] };
					temp = temp2;
				}
				output.add(temp);
				line = reader.readLine();
			}
			reader.close();
			return output;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Read Error");
			return null;
		}

	}
}
