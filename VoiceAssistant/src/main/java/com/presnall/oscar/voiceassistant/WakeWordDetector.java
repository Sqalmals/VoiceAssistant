package com.presnall.oscar.voiceassistant;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import ai.picovoice.porcupine.Porcupine;

public class WakeWordDetector {
	private static String[] keywordPath = {
			System.getProperty("user.dir") + "\\src\\main\\resources\\wakewords\\bumblebee_windows.ppn" };
	private static String keyword;
	private static float[] sensitivities = { 0.5f };
	private boolean activated = true, running = true;

	AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
	TargetDataLine microphone;

	public void start() {
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

		Porcupine porcupine;

		try {
			keyword = (new File(keywordPath[0])).getName().split("_")[0];

			microphone = (TargetDataLine) AudioSystem.getLine(info); // initialize microphone
			microphone.open(format);
			microphone.start();

			// initialize porcupine engine
			porcupine = new Porcupine.Builder().setLibraryPath(Porcupine.LIBRARY_PATH)
					.setModelPath(Porcupine.MODEL_PATH).setKeywordPaths(keywordPath).setSensitivities(sensitivities)
					.build();

			System.out.print("Listening for {");
			System.out.printf(" %s(%.02f)", keyword, sensitivities[0]);
			System.out.print(" }\n");

			// porcupine code from example

			// buffers for processing audio
			int frameLength = porcupine.getFrameLength();
			ByteBuffer captureBuffer = ByteBuffer.allocate(frameLength * 2);
			captureBuffer.order(ByteOrder.LITTLE_ENDIAN);
			short[] porcupineBuffer = new short[frameLength];

			int numBytesRead;
			while (running) {

				// read a buffer of audio
				numBytesRead = microphone.read(captureBuffer.array(), 0, captureBuffer.capacity());

				// don't pass to porcupine if we don't have a full buffer
				if (numBytesRead != frameLength * 2) {
					continue;
				}

				// copy into 16-bit buffer
				captureBuffer.asShortBuffer().get(porcupineBuffer);

				// process with porcupine
				int result = porcupine.process(porcupineBuffer);
				if (result >= 0) {
					System.out.printf("[%s] Detected '%s'\n",
							LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), keyword);
					microphone.close();
					Main.begin();
					activated = false;
					while (!activated && running) {Thread.sleep(1);}
				}
			}
			microphone.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		running = false;
	}

	public void activate() {
		activated = true;
		try {
			microphone.open(this.format);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			return;
		}
		microphone.start();
	}
}
