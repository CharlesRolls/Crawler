package com.rolls.crawler;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.rolls.crawler.parser.ParserImpl;
import com.rolls.crawler.report.ReportDetails;
import com.rolls.crawler.report.ReportPage;
import com.rolls.crawler.report.ReportWriter;
import com.rolls.crawler.report.ReportWriterImpl;
import com.rolls.crawler.service.Crawler;
import com.rolls.crawler.service.CrawlerImpl;
import com.rolls.crawler.service.EventHandler;

/**
 * Crawls the web according to the crawler properties and saves the results to a file.
 */
@Component
public class WebCrawler
{
   private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

   @Autowired
   private CrawlerProperties appProps;

   private EventHandlerImpl eventHandler = new EventHandlerImpl();

   /**
    * Internal class to capture events from the web crawler.
    */
   private class EventHandlerImpl implements EventHandler
   {
      @Override
      public void onProgress(int pagesProcessed)
      {
         System.out.print(".");
      }

      @Override
      public void onComplete(int pagesProcessed, boolean canceled)
      {
         System.out.println();
         System.out.println(String.format("Pages Processed: %d ==> %s", pagesProcessed, (canceled ? "CANCELED" : "COMPLETE")));
      }
   }

   /**
    * Runs the crawl.
    */
   public void run()
   {
      try
      {
         if (!checkProperties())
            return;

         System.out.println(String.format("Crawling URL: %s", appProps.getStartingUrl()));

         Crawler crawler = new CrawlerImpl(
               appProps.getNumThreads(),
               appProps.getProgressIntervalMillis(),
               new ParserImpl(appProps.getParseTimeoutMillis())
               );

         crawler.start(appProps.getStartingUrl(), eventHandler);
         if (!crawler.await(appProps.getCrawlTimeoutSeconds(), TimeUnit.SECONDS))
            crawler.cancel();

         saveResult(crawler);
      }
      catch (Exception ex)
      {
         logger.debug("Crawl failed.", ex);
      }
   }

   /**
    * Checks if the properties are valid.
    * @return Whether properties are valid.
    */
   private boolean checkProperties()
   {
      List<String> errs = appProps.validate();
      if (errs.isEmpty())
         return true;

      System.out.println("Unable to run crawler:");
      for (String err : errs)
         System.out.println(" - " + err);

      System.out.println(System.lineSeparator() + "Please update the application.yml file and try again.");

      return false;
   }

   /**
    * Saves the crawl result to a file.
    * @param crawler Crawler that holds the result.
    */
   private void saveResult(Crawler crawler)
   {
      ModelMapper mapper = new ModelMapper();

      ReportDetails reportDetails = new ReportDetails();
      reportDetails.setStartingUrl(appProps.getStartingUrl());
      reportDetails.setStartTime(crawler.getStartTime());
      reportDetails.setDurationMillis(crawler.getDuration());
      reportDetails.setCanceled(crawler.isCanceled());
      reportDetails.setPages(mapper.map(crawler.getResult(), new TypeToken<List<ReportPage>>(){}.getType()));

      File file = Paths.get(appProps.getOutputPath(), appProps.getResultFile()).toFile();
      ReportWriter writer = new ReportWriterImpl(file);
      writer.save(reportDetails);

      System.out.println(String.format("Result stored in %s", file.getAbsoluteFile()));
   }
}
