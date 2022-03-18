package com.presnall.oscar.voiceassistant;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherHandler {
	public static String getWeather(String location, String startDate, String endDate, String unitGroup,
			String apiKey) {

		String apiEndPoint = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/";

		// Build the URL pieces
		StringBuilder requestBuilder = new StringBuilder(apiEndPoint);
		requestBuilder.append(location);

		if (startDate != null && !startDate.isEmpty()) {
			requestBuilder.append("/").append(startDate);
			if (endDate != null && !endDate.isEmpty()) {
				requestBuilder.append("/").append(endDate);
			}
		}

		// Build the parameters to send via GET or POST
		URIBuilder builder;
		try {
			builder = new URIBuilder(requestBuilder.toString());
			builder.setParameter("unitGroup", unitGroup).setParameter("key", apiKey);
			
			//System.out.println(builder.toString());

			HttpGet get = new HttpGet(builder.build());

			CloseableHttpClient httpclient = HttpClients.createDefault();

			CloseableHttpResponse response = httpclient.execute(get);

			String rawResult = null;
			try {
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					System.out.printf("Bad response status code:%d%n", response.getStatusLine().getStatusCode());
					return null;
				}
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					rawResult = EntityUtils.toString(entity, Charset.forName("utf-8"));
				}

			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				response.close();
			}

			JSONObject timelineResponse = new JSONObject(rawResult);

			ZoneId zoneId = ZoneId.of(timelineResponse.getString("timezone"));

			JSONArray values = timelineResponse.getJSONArray("days");

			for (int i = 0; i < values.length(); i++) {
				JSONObject dayValue = values.getJSONObject(i);

				ZonedDateTime datetime = ZonedDateTime
						.ofInstant(Instant.ofEpochSecond(dayValue.getLong("datetimeEpoch")), zoneId);

				double maxtemp = dayValue.getDouble("tempmax");
				double mintemp = dayValue.getDouble("tempmin");
				double pop = dayValue.getDouble("precip");
				String source = dayValue.getString("source");
				double temp = dayValue.getDouble("temp");
				return String.format("%s;%.1f;%.1f;%.1f;%s%n;%.1f", datetime.format(DateTimeFormatter.ISO_LOCAL_DATE),
						maxtemp, mintemp, pop, source, temp);
			}
		} catch (UnknownHostException e) {
			return "Connection Error";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
