package perf.http;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class KillAB {

    enum HTTPMethods{
        GET,POST,PUT,DELETE
    }
    private static Options options = new Options();
    private static Logger logger;

    static {
        logger = Logger.getLogger(KillAB.class);
        options.addOption(new Option("u", "url", true, "URL on which http operation would be done. Mandatory"));
        options.addOption(new Option("m", "method", true, "POST,PUT,GET,DELETE. GET is default"));
        options.addOption(new Option("t", "threads", true, "Number of parallel clients. Default is 1"));
        options.addOption(new Option("r", "requests", true, "number of requests. Default is 1"));
        options.addOption(new Option("cpu", "free-cpu", true, "Free CPUs. Default is 1"));
        options.addOption(new Option("f", "post-file", true, "File to be posted. Method should be POST/PUT"));
        options.addOption(new Option("c", "content-type", true, "Like application/json"));
        options.addOption(new Option("h", "help", false, "help"));
    }


    public static void main(String[] args) throws InterruptedException {
        CommandLineParser parser = new GnuParser();

        try {
            CommandLine line = parser.parse(options, args);
            if(line.hasOption('h')) {
                help();
                return;
            }

            doTheRun(line);

       }
        catch( ParseException exp ) {
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            help();
        }
    }

    private static void doTheRun(CommandLine line) throws InterruptedException, FileNotFoundException, IOException {

        String url = getURL(line);//"http://localhost:8888/fk-alert-service/alerts/meta";
        final int requests = getRequests(line);
        int concurrency = getThreads(line);
        int freeCPUCores = getFreeCPU(line);
        int requestQueueSize = requests;
        String httpMethod = getHttpMethod(line);
        String dataFile = getPostFile(line);
        String contentType = getContentType(line);

        final List<HttpClient> clients = buildClients(concurrency, url, HTTPMethods.valueOf(httpMethod), dataFile, contentType);

       // ClientQueue.initialize(clients);

        //RequestQueue.initialize(requestQueueSize);

       // raiseRequests(requests);

        List<ClientExecutorThread> threads = startClientExecutorThreads(requests, freeCPUCores);
        int threadCnt=0, clientCnt = 0;
        while(clientCnt< clients.size()){
        	threads.get(threadCnt % threads.size()).addClient(clients.get(clientCnt));
        	threadCnt++;
        	clientCnt++;
        }
        for (ClientExecutorThread thread : threads) thread.start();

        waitForClientExecutorThreads(threads);
        waitForRequestsToEnd(clients, requests);

        long endTime = System.currentTimeMillis();
        float totalWasteTime = getWasteTime(threads);
        System.out.println("Time Wasted : "+totalWasteTime);
        System.out.println("Throughtput : "+(requests/((endTime - threads.get(0).getStartTime())/1000f)));
        System.out.println("Time : "+((endTime - threads.get(0).getStartTime())/1000f));
                        //ClientQueue.clearClients();
        printPercentile(clients);

    }

    private static void printPercentile(List<HttpClient> clients) {
        List<Long> requestsTimes = new ArrayList<Long>();
        for(HttpClient c : clients)
            requestsTimes.addAll(c.getRequestTimes());

        Collections.sort(requestsTimes);

        for(float per : new float[]{0.50f,0.75f,0.90f,0.95f,0.98f,0.99f}) {
            System.out.println(((int)(per * 100))+"th :"+requestsTimes.get((int)(per * requestsTimes.size())));
        }
        System.out.println("Longest :"+requestsTimes.get(requestsTimes.size()-4));
    }

    private static float getWasteTime(List<ClientExecutorThread> threads) {
        long timeWasted = 0;
        for(ClientExecutorThread t : threads)
            timeWasted += t.getTimeWasted();
        return timeWasted/(float)threads.size();
    }


    private static List<HttpClient> buildClients (int concurrency, String url, HTTPMethods httpMethod, String dataFile, String contentType) throws FileNotFoundException, IOException{
        List<HttpClient> clients = new ArrayList<HttpClient>();
        for(int i=1; i<= concurrency; i++)
            clients.add(new HttpClient(url, httpMethod, dataFile, contentType));

        return clients;
    }

//    private static void raiseRequests(int requests) throws InterruptedException {
//        for(long i=1; i<= requests; i++) {
//            RequestQueue.raiseRequest(i);
//        }
//
//        System.out.println("Requests Raised");
//        System.out.println("Requests started");
//    }

    private static void waitForClientExecutorThreads(List<ClientExecutorThread> threads) throws InterruptedException {
        for(ClientExecutorThread t : threads)
            t.join();
    }

    private static List<ClientExecutorThread> startClientExecutorThreads(int requests, int freeCPUCores) throws InterruptedException{
        List<ClientExecutorThread> threads = new ArrayList<ClientExecutorThread>();
        int requestsPerCore = requests/freeCPUCores;
        int requestLeftToBeAssinged = requests;

        for(int i = 1; i <= freeCPUCores; i++) {
            ClientExecutorThread t = null;
            if(i == freeCPUCores) {
                t = new ClientExecutorThread(requestLeftToBeAssinged);
            	for (long j=0; j<requestLeftToBeAssinged; j++){
            		t.addRequest(j);
            	}
            } else {
                t = new ClientExecutorThread(requestsPerCore);
                for (long j=0; j<requestsPerCore; j++){
            		t.addRequest(j);
            	}
            }
            //t.start();
            threads.add(t);
            requestLeftToBeAssinged -= requestsPerCore;
        }
        return threads;
    }

    private static void waitForRequestsToEnd(final List<HttpClient> clients, final int requests) throws InterruptedException {
        Thread t  = new Thread() {
            public void run() {
                int completed = 0;
                while(completed < requests) {
                    completed = 0;
                    for(HttpClient client : clients) {
                        completed += client.getRequestTimes().size();
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }

            }
        };
        t.start();
        t.join();
        System.out.println("Requests Ended");
    }

    private static void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "KillAB", options );
    }

    private static HashMap<String, String> getHeaders(CommandLine line) {
        String contentType = line.getOptionValue("c");
        HashMap<String,String> headers = new HashMap<String,String>();
        if(contentType != null)
            headers.put("content-type",contentType);
        return headers;
    }

    private static String getContentType(CommandLine line) {
        return line.getOptionValue("c");
    }

    private static int getThreads(CommandLine line) {
        String threadsStr = line.getOptionValue("t");
        if(threadsStr == null)
            threadsStr = "1";
        logger.info("Threads :"+threadsStr);

        return Integer.parseInt(threadsStr);
    }

    private static int getRequests(CommandLine line) {
        String requestsStr = line.getOptionValue("r");
        if(requestsStr == null)
            requestsStr = "1";
        logger.info("Requests :"+requestsStr);

        return Integer.parseInt(requestsStr);
    }

    private static int getFreeCPU(CommandLine line) {
        String freeCPUsStr = line.getOptionValue("cpu");
        if(freeCPUsStr == null)
            freeCPUsStr = "1";
        logger.info("Requests :"+freeCPUsStr);

        return Integer.parseInt(freeCPUsStr);
    }


    private static String getHttpMethod(CommandLine line) {
        String method = line.getOptionValue("m");
        if(method == null)
            method = "GET";
        logger.info("HTTP Method :"+method);

        return method;
    }

    private static String getURL(CommandLine line){
        String url = line.getOptionValue("u");
        logger.info("URL :"+url);

        if(url == null)
            throw new RuntimeException("option -u is mandatory");

        return url;
    }

    private static String getPostData(CommandLine line) {
        return line.getOptionValue("d");
    }

    private static String getPostFile(CommandLine line) {
        return line.getOptionValue("f");
    }
}
