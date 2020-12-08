package com.rolls.crawler;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.rolls.crawler.service.CrawlReportServiceImpl;
import com.rolls.crawler.service.CrawlServiceObserver;

/**
 * Spring CLI wrapper for the application.
 */
@SpringBootApplication
public class CLIApplication implements CommandLineRunner
{
   private CrawlServiceObserverImpl observer = new CrawlServiceObserverImpl();

   @Autowired
   private CrawlReportServiceImpl reportCrawler;

   private class CrawlServiceObserverImpl implements CrawlServiceObserver
   {
      @Override
      public void onStart(String startingUrl)
      {
         System.out.println(String.format("Crawling URL: %s", startingUrl));
      }

      @Override
      public void onError(List<String> errors)
      {
         System.out.println("Unable to run crawler:");
         for (String error : errors)
            System.out.println(" - " + error);

         System.out.println(System.lineSeparator() + "Please update the application.yml file and try again.");
      }

      @Override
      public void onProgress(int pagesProcessed)
      {
         System.out.print(".");
      }

      @Override
      public void onComplete(int pagesProcessed, boolean cancelled, String reportPath)
      {
         System.out.println();
         System.out.println(String.format("Pages Processed: %d ==> %s", pagesProcessed, (cancelled ? "CANCELLED" : "COMPLETE")));
         System.out.println(String.format("Result stored in %s", reportPath));
      }
   }

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
      reportCrawler.addObserver(observer);
      reportCrawler.run();
   }
}
