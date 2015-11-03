/*
	Stanford CoreNLP HTTP Server
	Copyright (C) 2015 Koalephant Co., Ltd

	Original Project:

	Stanford CoreNLP XML Server
	Copyright (C) 2013 Niels Lohmann

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.koalephant.nlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.DocDateAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StanfordCoreNLPHTTPServer implements Container {

	private static final String serverName = "Stanford CoreNLP HTTP Server/2.0 (Simple 5.1.6)";

	private static final Logger log = Logger.getLogger(StanfordCoreNLPHTTPServer.class.getName());
	private static int total_requests = 0;
	private static String host = "127.0.0.1";
	private static int port = 8080;
	private StanfordCoreNLP pipeline;
	private static MediaType defaultType = MediaType.APPLICATION_JSON;
	private static DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();

	/**
	 * Constructor
	 * @param pipeline the StanfordCoreNLP pipeline
	 */
	StanfordCoreNLPHTTPServer(StanfordCoreNLP pipeline) {
		this.pipeline = pipeline;
	}

	/**
	 * Start the Server
	 *
	 * @param args command line arguments
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		Properties props = new Properties();
		// Load properties from the command line
		if (args.length > 0) {
			log.info("Reading Opts...");
			props = StringUtils.argsToProperties(args);
		}

		// use -host if given
		host = props.getProperty("host", host);
		props.remove("host");

		String portOpt = props.getProperty("port");
		// use -port if given
		if (portOpt != null) {
			try {
				props.remove("port");
				port = Integer.parseInt(portOpt);
			} catch (Exception e) {
				System.err.println("Invalid port specified: " + portOpt);
				System.exit(1);
			}
		}

		String defaultTypeStr = props.getProperty("defaultType");
		if (defaultTypeStr != null) {
			try {
				defaultType = MediaType.getFromType(defaultTypeStr, defaultType);
			}
			catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}
		}

		// start the server
		Container container = new StanfordCoreNLPHTTPServer(new StanfordCoreNLP(props));

		log.info("Attempting to listen on " + host + ":" + port + ".");

		Server server = new ContainerServer(container);
		Connection connection = new SocketConnection(server);
		SocketAddress address = new InetSocketAddress(host, port);
		connection.connect(address);

		log.info("Initialized server at " + host + ":" + port + ".");
	}

	// an interface to the Stanford Core NLP
	public String parse(String s, MediaType mediaType) throws IOException {
		Annotation annotation = new Annotation(s);

		DateTime now = new DateTime();

		annotation.set(DocDateAnnotation.class, now.toString(dateTimeFormatter));
		pipeline.annotate(annotation);
		StringWriter sb = new StringWriter();

		switch (mediaType) {
			case TEXT_XML:
			case APPLICATION_XML:
				pipeline.xmlPrint(annotation, sb);
				break;

			case APPLICATION_JSON:
			case TEXT_JSON:
				pipeline.jsonPrint(annotation, sb);
				break;
		}

		return sb.toString();
	}

	public void handle(Request request, Response response) {
		int request_number = ++total_requests;
		long started = System.currentTimeMillis();

		PrintStream body = null;
		try {
			body = response.getPrintStream();
			if (request.getMethod().equals("HEAD")) {
				response.setStatus(Status.getStatus(200));
				body.close();
				return;
			}
			log.info("Request " + request_number + " from " + request.getClientAddress().getHostName());

			final MediaType mediaType = MediaType.getFromType(request.getValue("Accept"), defaultType);

			final String text = request.getQuery().get("text");

			response.setContentType(mediaType.toString());
			response.setValue("Server", serverName);
			response.setDate("Date", started);
			response.setDate("Last-Modified", started);

			// Input Request are handled externally, so here single thread executor is fine
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Callable<Object> task = () -> parse(text, mediaType);
			Future<Object> future = executor.submit(task);
			Object result = null;
			try {
				result = future.get(60, TimeUnit.SECONDS);
			} catch (TimeoutException ex) {
				// handle the timeout
				log.log(Level.SEVERE, "TimeoutException", ex);
				log.info("Request " + request_number + ", raised exception for " + text);
			} catch (InterruptedException e) {
				// handle the interrupts
				log.log(Level.SEVERE, "InterruptedException", e);
				log.info("Request " + request_number + ", raised exception for " + text);
			} catch (ExecutionException e) {
				// handle other exceptions
				log.log(Level.SEVERE, "ExecutionException", e);
				log.info("Request " + request_number + ", raised exception for " + text);
			} finally {
				future.cancel(true); // may or may not desire this
				executor.shutdownNow();
			}
			long finished = System.currentTimeMillis();
			response.setValue("Processing-Time", String.valueOf(finished - started));
			body.println(result);

			log.info("Request " + request_number + " done (" + (finished - started) + " ms)");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception", e);
			log.info("Request " + request_number + ", raised exception");
		}
		finally {
			if (body != null) {
				body.close();
			}
		}
	}
}
