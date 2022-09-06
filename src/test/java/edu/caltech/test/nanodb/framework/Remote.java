package edu.caltech.test.nanodb.framework;


import java.io.IOException;


public class Remote {

    public static Process startServer() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("./ndbs");
        return builder.start();
    }


    public static Process startClient() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("./ndbc");
        return builder.start();
    }

}
