package com.presnall.oscar.voiceassistant;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class TextToSpeech {
	
	public static void speak(String speech) {
		System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");// cmu_time_awb.AlanVoiceDirectory

		Voice voice;

		voice = VoiceManager.getInstance().getVoice("kevin");

		if (voice != null) {
			// the Voice class allocate() method allocates this voice
			voice.allocate();
		}
		try {
			// sets the rate (words per minute i.e. 190) of the speech
			voice.setRate(150);
			// sets the baseline pitch (150) in hertz
			voice.setPitch(50);
			// sets the volume (10) of the voice
			voice.setVolume(10);
			// the speak() method speaks the specified text
			voice.speak(speech);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
