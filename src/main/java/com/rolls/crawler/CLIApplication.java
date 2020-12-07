package com.rolls.crawler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring CLI wrapper for the application.
 */
@SpringBootApplication
public class CLIApplication implements CommandLineRunner
{
   @Autowired
   WebCrawler webCrawler;

   /**
    * Program entry.
    * @param args Application arguments.
    */
   public static void main(String[] args)
   {
      SpringApplication app = new SpringApplication(CLIApplication.class);
      app.run(args);
   }

   @Override
   public void run(String... args) throws Exception
   {
      webCrawler.run();
   }
}
