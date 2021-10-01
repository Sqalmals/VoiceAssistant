package com.presnall.oscar.voiceassistant;

import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.namefind.*;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class IntentTrainer implements Runnable {

	private String intent, arg, input;
	private Thread t;
	private boolean running;

	@Override
	public void run() {
		String[] args = { "example/weather/train", "arg" };

		File trainingDirectory = new File(args[0]);
		String[] slots = new String[0];
		if (args.length > 1) {
			slots = args[1].split(",");
		}

		if (!trainingDirectory.isDirectory()) {
			throw new IllegalArgumentException(
					"TrainingDirectory is not a directory: " + trainingDirectory.getAbsolutePath());
		}

		List<ObjectStream<DocumentSample>> categoryStreams = new ArrayList<ObjectStream<DocumentSample>>();
		for (File trainingFile : trainingDirectory.listFiles()) {
			try {
				String intent = trainingFile.getName().replaceFirst("[.][^.]+$", "");
				ObjectStream<String> lineStream = new PlainTextByLineStream(
						new MarkableFileInputStreamFactory(trainingFile), "UTF-8");
				ObjectStream<DocumentSample> documentSampleStream = new IntentDocumentSampleStream(intent, lineStream);
				categoryStreams.add(documentSampleStream);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

		}

		ObjectStream<DocumentSample> combinedDocumentSampleStream = ObjectStreamUtils
				.concatenateObjectStream(categoryStreams);

		TrainingParameters trainingParams = new TrainingParameters();
		trainingParams.put(TrainingParameters.ITERATIONS_PARAM, 10);
		trainingParams.put(TrainingParameters.CUTOFF_PARAM, 0);

		DoccatModel doccatModel;
		try {
			doccatModel = DocumentCategorizerME.train("en", combinedDocumentSampleStream, trainingParams,
					new DoccatFactory());
			combinedDocumentSampleStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		List<TokenNameFinderModel> tokenNameFinderModels = new ArrayList<TokenNameFinderModel>();

		try {
			for (String slot : slots) {
				List<ObjectStream<NameSample>> nameStreams = new ArrayList<ObjectStream<NameSample>>();
				for (File trainingFile : trainingDirectory.listFiles()) {
					ObjectStream<String> lineStream = new PlainTextByLineStream(
							new MarkableFileInputStreamFactory(trainingFile), "UTF-8");
					ObjectStream<NameSample> nameSampleStream = new NameSampleDataStream(lineStream);
					nameStreams.add(nameSampleStream);
				}
				ObjectStream<NameSample> combinedNameSampleStream = ObjectStreamUtils
						.concatenateObjectStream(nameStreams);

				TokenNameFinderModel tokenNameFinderModel = NameFinderME.train("en", slot, combinedNameSampleStream,
						trainingParams, new TokenNameFinderFactory());
				combinedNameSampleStream.close();
				tokenNameFinderModels.add(tokenNameFinderModel);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		DocumentCategorizerME categorizer = new DocumentCategorizerME(doccatModel);
		NameFinderME[] nameFinderMEs = new NameFinderME[tokenNameFinderModels.size()];
		for (int i = 0; i < tokenNameFinderModels.size(); i++) {
			nameFinderMEs[i] = new NameFinderME(tokenNameFinderModels.get(i));
		}

		System.out.println("Training complete. Ready.");
		System.out.print(">");

		InputStream modelIn;
		TokenizerModel model;
		try {
			modelIn = new FileInputStream("./models/en-token.bin");
			model = new TokenizerModel(modelIn);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		Tokenizer tokenizer = new TokenizerME(model);

		while (running) {
			if (input != null) {
				double[] outcome = categorizer.categorize(tokenizer.tokenize(input));

				String[] tokens = tokenizer.tokenize(input);
				String arguments = "";
				for (NameFinderME nameFinderME : nameFinderMEs) {
					Span[] spans = nameFinderME.find(tokens);
					String[] names = Span.spansToStrings(spans, tokens);
					for (int i = 0; i < spans.length; i++) {
						if (i > 0) {
							arguments += ", ";
						}
						arguments += names[i] + " ";
					}
				}

				if(arguments.length() > 0)
					this.setIntent(categorizer.getBestCategory(outcome), arguments.substring(0,arguments.length() - 1));
				else
					this.setIntent(categorizer.getBestCategory(outcome), arguments);
			} else {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
		}

	}

	public void sendInput(String input) {
		this.input = input;
	}

	private void setIntent(String intent, String arguments) {
		this.intent = intent;
		this.arg = arguments;
	}

	public String[] getIntent() {
		String[] output = { intent, arg };
		return output;
	}

	public void start() {
		if (t == null) {
			t = new Thread(this,"IntentTrainer");
			running = true;
			t.start();
		}
	}
	
	public void stop() {
		running = false;
	}

}
