package com.presnall.oscar.voiceassistant;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

public class VoiceRecognizer implements Runnable {

	private Thread t;
	private boolean running, activated = false;
	AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 60000, 16, 2, 4, 44100, false);
	DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
	TargetDataLine microphone;

	@Override
	public void run() {
		LibVosk.setLogLevel(LogLevel.DEBUG);

		try (Model model = new Model("src\\main\\resources\\model");
				Recognizer recognizer = new Recognizer(model, 120000)) {
			try {

				int numBytesRead;
				int CHUNK_SIZE = 1024;

				byte[] b = new byte[4096];

				System.out.println("System ready.");

				while (running) {
					Thread.sleep(1);

					if (activated) {
						numBytesRead = microphone.read(b, 0, CHUNK_SIZE);

						if (recognizer.acceptWaveForm(b, numBytesRead)) {
							microphone.close();
							Main.process(recognizer.getResult());
							activated = false;
						} /*
							 * else { System.out.println(recognizer.getPartialResult()); }
							 */
					}

				}
				microphone.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void activate() {
		try {
			microphone = (TargetDataLine) AudioSystem.getLine(info);
			microphone.open(this.format);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			return;
		}
		
		microphone.start();
		activated = true;
	}

	public void start() {
		if (t == null) {
			t = new Thread(this, "VoiceRecognizer");
			running = true;
			t.start();
		}
	}

	public void stop() {
		running = false;
	}

}
