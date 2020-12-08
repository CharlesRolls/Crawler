package com.rolls.crawler.service;

import java.io.File;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.rolls.crawler.crawl.CrawlObserver;
import com.rolls.crawler.crawl.CrawlPage;
import com.rolls.crawler.crawl.CrawlerImpl;
import com.rolls.crawler.parse.ParserImpl;
import com.rolls.crawler.report.ReportDetails;
import com.rolls.crawler.report.ReportPage;
import com.rolls.crawler.report.ReportWriter;
import com.rolls.crawler.report.ReportWriterImpl;

/**
 * Implementation of a web crawler that finds pages of a single domain
 * and saves crawl details to a report file.
 */
@Component
public class CrawlReportServiceImpl implements CrawlService
{
   private final static Logger logger = LoggerFactory.getLogger(CrawlReportServiceImpl.class);

   private final Queue<CrawlServiceObserver> observers = new ConcurrentLinkedQueue<>();

   @Autowired
   private CrawlReportConfig config;

   private CrawlObserverImpl crawlObserver = null;

   /**
    * Internal class to capture events from the web crawler.
    */
   private class CrawlObserverImpl implements CrawlObserver
   {
      private final Queue<CrawlPage> pages = new ConcurrentLinkedQueue<>();
      private AtomicInteger pagesProcessed = new AtomicInteger();

      private Date startTime = null;
      private long durationMillis = 0;
      private boolean cancelled = false;

      @Override
      public void onPage(CrawlPage page)
      {
         pages.add(page);
         pagesProcessed.incrementAndGet();
      }

      @Override
      public void onComplete(Date startTime, long durationMillis, boolean cancelled)
      {
         this.startTime = startTime;
         this.durationMillis = durationMillis;
         this.cancelled = cancelled;
     }
   }

   @Override
   public void addObserver(CrawlServiceObserver observer)
   {
      if (observer == null)
         throw new IllegalArgumentException("Null observer.");

      observers.add(observer);
   }

   @Override
   public void run()
   {
      synchronized (this)
      {
         if (crawlObserver != null)
            throw new IllegalStateException("The crawler is already started.");

         crawlObserver = new CrawlObserverImpl();
      }

      try
      {
         if (!checkProperties())
            return;

         observers.stream().forEach((observer) -> observer.onStart(config.getStartingUrl()));

         CrawlerImpl crawler = new CrawlerImpl(config.getNumThreads(),
               new ParserImpl(config.getParseTimeoutMillis()));

         crawler.addObserver(crawlObserver);
         crawler.start(config.getStartingUrl());

         boolean complete = false;
         long crawlTimeoutMillis = config.getCrawlTimeoutSeconds() * 1000;
         long progressIntervalMillis = config.getProgressIntervalMillis();
         long startMillis = System.currentTimeMillis();
         long lastProgressMillis = startMillis;
         do
         {
            long curTime = System.currentTimeMillis();
            long intervalMiilis = curTime - lastProgressMillis;
            if (intervalMiilis >= progressIntervalMillis)
            {
               observers.stream().forEach((observer) -> observer.onProgress(crawlObserver.pagesProcessed.get()));
               lastProgressMillis = curTime;
            }

            complete = crawler.await(progressIntervalMillis, TimeUnit.MILLISECONDS);
         }
         while (!complete && System.currentTimeMillis() - startMillis < crawlTimeoutMillis);

         if (!complete)
         {
            if (!crawler.cancel())
               logger.error("Cancel after crawl timeout failed.");
         }

         File file = Paths.get(config.getOutputPath(), config.getResultFile()).toFile();
         saveResult(file);

         observers.stream().forEach((observer) -> observer.onComplete(
               crawlObserver.pagesProcessed.get(),
               crawlObserver.cancelled,
               file.getAbsolutePath()
               ));
      }
      catch (Exception ex)
      {
         logger.debug("Crawl failed.", ex);
      }
      finally
      {
         crawlObserver = null;
      }
   }

   /**
    * Checks if the properties are valid.
    * @return Whether properties are valid.
    */
   private boolean checkProperties()
   {
      List<String> errs = config.validate();
      if (errs.isEmpty())
         return true;

      observers.stream().forEach((observer) -> observer.onError(errs));
      return false;
   }

   /**
    * Saves the crawl result to a file.
    */
   private void saveResult(File file)
   {
      ModelMapper mapper = new ModelMapper();

      ReportDetails reportDetails = new ReportDetails();
      reportDetails.setStartingUrl(config.getStartingUrl());
      reportDetails.setStartTime(crawlObserver.startTime);
      reportDetails.setDurationMillis(crawlObserver.durationMillis);
      reportDetails.setCancelled(crawlObserver.cancelled);
      reportDetails.setPages(mapper.map(crawlObserver.pages, new TypeToken<List<ReportPage>>(){}.getType()));

      ReportWriter writer = new ReportWriterImpl(file);
      writer.save(reportDetails);
   }
}
