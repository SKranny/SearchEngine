package main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.concurrent.ForkJoinPool;

@SpringBootApplication
public class Application {

    public static String URL = "http://www.playback.ru";
    public static Parser parser = new Parser(URL);

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(Application.class, args);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(parser);
    }
}
