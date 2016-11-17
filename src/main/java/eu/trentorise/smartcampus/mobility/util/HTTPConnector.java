/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.mobility.util;

import java.io.InputStream;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

public class HTTPConnector {
	
	public static String doGet(String address, String req, String accept, String contentType, String encoding) throws Exception {
		HttpResponse<String> response = Unirest.get(address + ((req != null) ? ("?" + req) : "")).header("Accept", accept).header("Content-Type", contentType).asString();
		if (response.getStatus() != 200) {
			throw new ConnectorException("Failed : HTTP error code : " + response.getStatus(), response.getStatus());
		}		
		return response.getBody();
	}
	
	public static InputStream doStreamGet(String address, String req, String accept, String contentType) throws Exception {
		HttpResponse<InputStream> response = Unirest.get(address + ((req != null) ? ("?" + req) : "")).header("Accept", accept).header("Content-Type", contentType).asBinary();
		if (response.getStatus() != 200) {
			throw new ConnectorException("Failed : HTTP error code : " + response.getStatus(), response.getStatus());
		}		
		return response.getBody();
	}
	
	public static String doAuthenticatedPost(String address, String req, String accept, String contentType, String user, String password) throws Exception {
		HttpResponse<String> response = Unirest.post(address).header("Accept", accept).header("Content-Type", contentType).basicAuth(user, password).body(req).asString();
		if (response.getStatus() != 200) {
			throw new ConnectorException("Failed : HTTP error code : " + response.getStatus(), response.getStatus());
		}		
		return response.getBody();		
	}
	
	public static String doPost(String address, String req, String accept, String contentType) throws Exception {
		HttpResponse<String> response = Unirest.post(address).header("Accept", accept).header("Content-Type", contentType).body(req).asString();
		if (response.getStatus() != 200) {
			throw new ConnectorException("Failed : HTTP error code : " + response.getStatus(), response.getStatus());
		}		
		return response.getBody();		
	}
	
	/*
	public static String doGet(String address, String req, String accept, String contentType, String encoding) throws Exception {

		StringBuffer response = new StringBuffer();

		URL url = new URL(address + ((req != null) ? ("?" + req) : ""));

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setDoOutput(true);
		conn.setDoInput(true);

		if (accept != null) {
			conn.setRequestProperty("Accept", accept);
		}
		if (contentType != null) {
			conn.setRequestProperty("Content-Type", contentType);
		}
		if (conn.getResponseCode() != 200) {
			throw new ConnectorException("Failed : HTTP error code : " + conn.getResponseCode(), conn.getResponseCode());
		}

		BufferedReader br;
		if (encoding != null) {
			br = new BufferedReader(new InputStreamReader((conn.getInputStream()), encoding));
		} else {
			br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		}

		String output = null;
		while ((output = br.readLine()) != null) {
			response.append(output);
		}

		conn.disconnect();
		return response.toString();
	}

	public static InputStream doStreamGet(String address, String req, String accept, String contentType) throws Exception {

		StringBuffer response = new StringBuffer();

		URL url = new URL(address + ((req != null) ? ("?" + req) : ""));

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setDoOutput(true);
		conn.setDoInput(true);

		if (accept != null) {
			conn.setRequestProperty("Accept", accept);
		}
		if (contentType != null) {
			conn.setRequestProperty("Content-Type", contentType);
		}
		if (conn.getResponseCode() != 200) {
			throw new ConnectorException("Failed : HTTP error code : " + conn.getResponseCode(), conn.getResponseCode());
		}

		return conn.getInputStream();
	}
	
	public static String doAuthenticatedPost(String address, String req, String accept, String contentType, String user, String password) throws Exception {
		StringBuffer response = new StringBuffer();

		URL url = new URL(address);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		
		String authString = user + ":" + password;
		byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
		String authStringEnc = new String(authEncBytes);
		conn.setRequestProperty("Authorization", "Basic " + authStringEnc);

		if (accept != null) {
			conn.setRequestProperty("Accept", accept);
		}
		if (contentType != null) {
			conn.setRequestProperty("Content-Type", contentType);
		}		
		
		OutputStream out = conn.getOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		writer.write(req);
		writer.close();
		out.close();

		if (conn.getResponseCode() < 200 || conn.getResponseCode() > 299) {
			throw new ConnectorException("Failed : HTTP error code : " + conn.getResponseCode(), conn.getResponseCode());
		}
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		String output = null;
		while ((output = br.readLine()) != null) {
			response.append(output);
		}

		conn.disconnect();

		return response.toString();
	}

	public static String doPost(String address, String req, String accept, String contentType) throws Exception {

		StringBuffer response = new StringBuffer();

		URL url = new URL(address);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);

		if (accept != null) {
			conn.setRequestProperty("Accept", accept);
		}
		if (contentType != null) {
			conn.setRequestProperty("Content-Type", contentType);
		}

		OutputStream out = conn.getOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		writer.write(req);
		writer.close();
		out.close();

		if (conn.getResponseCode() < 200 || conn.getResponseCode() > 299) {
			throw new ConnectorException("Failed : HTTP error code : " + conn.getResponseCode(), conn.getResponseCode());
		}
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		String output = null;
		while ((output = br.readLine()) != null) {
			response.append(output);
		}

		conn.disconnect();

		return response.toString();
	}
	*/

}
