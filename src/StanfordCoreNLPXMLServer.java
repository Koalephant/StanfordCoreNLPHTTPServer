/*
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

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Query;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

public class StanfordCoreNLPXMLServer implements Container {
    private static StanfordCoreNLP pipeline;
    private static int port = 8080;
    private static final Logger log = Logger.getLogger( StanfordCoreNLPXMLServer.class.getName() );
    private static int total_requests = 0;

    // an interface to the Stanford Core NLP
    public String parse(String s) throws java.io.IOException {
        Annotation annotation = new Annotation(s);
        pipeline.annotate(annotation);
//        try {
//            Thread.sleep(10000);                 //1000 milliseconds is one second.
//        } catch(InterruptedException ex) {
//            Thread.currentThread().interrupt();
//        }
        StringWriter sb = new StringWriter();
        pipeline.xmlPrint(annotation, sb);
        return sb.toString();
    }

    public void handle(Request request, Response response) {
        try {
            int request_number = ++total_requests;
            log.info("Request " + request_number + " from " + request.getClientAddress().getHostName());
            long time = System.currentTimeMillis();
   
            response.setValue("Content-Type", "text/xml");
            response.setValue("Server", "Stanford CoreNLP XML Server/1.0 (Simple 5.1.6)");
            response.setDate("Date", time);
            response.setDate("Last-Modified", time);
   
            // pass "text" POST query to Stanford Core NLP parser
            final String text = request.getQuery().get("text");  
            PrintStream body = response.getPrintStream();

                       
            // Input Request are handled externally, so here single thread executor is fine
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<Object> task = new Callable<Object>() {
               public Object call() throws IOException {
                  return parse(text);
               }
            };
            Future<Object> future = executor.submit(task);
            Object result = null;
            try {
               result = future.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
               // handle the timeout
            	log.log(Level.SEVERE, "TimeoutException", ex.toString());
            } catch (InterruptedException e) {
               // handle the interrupts
            	log.log(Level.SEVERE, "InterruptedException", e.toString());
            } catch (ExecutionException e) {
               // handle other exceptions
            	log.log(Level.SEVERE, "ExecutionException", e.toString());
            } finally {
               future.cancel(true); // may or may not desire this
               executor.shutdownNow();
            }
            body.println(result);
            body.close();
            long time2 = System.currentTimeMillis();
            log.info("Request " + request_number + " done (" + (time2-time) + " ms)");
        } catch(Exception e) {
            log.log(Level.SEVERE, "Exception", e);
        }
    } 

    public static void main(String args[]) throws Exception {
        // use port if given
        try {
            port = Integer.parseInt(args[0]);
        } catch(Exception e) {
            // silently keep port at 8080
        }

        // initialize the Stanford Core NLP
        pipeline = new StanfordCoreNLP();

        // start the server
        Container container = new StanfordCoreNLPXMLServer();
        Server server = new ContainerServer(container);
        Connection connection = new SocketConnection(server);
        SocketAddress address = new InetSocketAddress(port);
        connection.connect(address);

        log.info("Initialized server at port " + port + ".");
    }
}
