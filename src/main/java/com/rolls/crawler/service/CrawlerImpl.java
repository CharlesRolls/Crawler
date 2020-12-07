package com.rolls.crawler.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rolls.crawler.parser.LinkDetails;
import com.rolls.crawler.parser.PageDetails;
import com.rolls.crawler.parser.Parser;

/**
 * Implementation of a web crawler that finds pages of a single domain.
 */
public class CrawlerImpl implements Crawler
{
   /**
    * Minimum number of threads.
    */
   public final static int MIN_THREADS = 1;

   private final static Logger logger = LoggerFactory.getLogger(CrawlerImpl.class);

   private final int numThreads;
   private final int progressIntervalMillis;
   private final Parser parser;
   private final ConcurrentSkipListMap<String, Result> results = new ConcurrentSkipListMap<>();
   private final AtomicBoolean canceled = new AtomicBoolean();

   private String hostUrl = null;
   private ExecutorService executor = null;
   private EventHandler eventHandler = null;
   private Object eventLock = new Object();
   private Date startTime = null;
   private long lastProgressTime = 0;
   private long duration = 0;
   private AtomicInteger pagesProcessed = new AtomicInteger();

   /**
    * Internal class to hold parse results for a URL.
    */
   private class Result
   {
      PageDetails pageDetails;
   }

   /**
    * Constructs a new crawler.
    * @param numThreads Number of threads to use.  Value < MIN_THREADS defaults to MIN_THREADS.
    * @param progressIntervalMillis Interval, in milliseconds, to raise progress event.  Value <= 0 means disable.
    * @param parser Web page parser to use.
    */
   public CrawlerImpl(int numThreads, int progressIntervalMillis, Parser parser)
   {
      if (parser == null)
         throw new IllegalArgumentException("Null parser.");

      this.numThreads = (numThreads >= MIN_THREADS ? numThreads: MIN_THREADS);
      this.progressIntervalMillis = progressIntervalMillis;
      this.parser = parser;
   }

   @Override
   public void start(String url, EventHandler eventHandler) throws MalformedURLException
   {
      if (executor != null)
         throw new IllegalStateException("The crawler is already started.");

      if (url != null)
         url = url.trim().toLowerCase();

      URL startingUrl = new URL(url);
      hostUrl = new URL(startingUrl.getProtocol(), startingUrl.getHost(), startingUrl.getPort(), "").toString();

      executor = Executors.newFixedThreadPool(numThreads, new BasicThreadFactory.Builder().namingPattern("Crawler-%d").daemon(true).build());
      this.eventHandler = eventHandler;
      startTime = new Date();
      lastProgressTime = this.startTime.getTime();
      duration = 0;

     crawl(url);
   }

   @Override
   public boolean await(long timeout, TimeUnit unit)
   {
      if (executor == null)
         throw new IllegalStateException("The crawler has not started.");

      try
      {
         return executor.awaitTermination(timeout, unit);
      }
      catch (InterruptedException ex)
      {
         Thread.currentThread().interrupt(); // Preserve interrupted status
         return false;
      }
   }

   @Override
   public void cancel()
   {
      if (executor == null)
         throw new IllegalStateException("The crawler has not started.");

      canceled.set(true);
      executor.shutdownNow();

      try
      {
         if (!executor.awaitTermination(10, TimeUnit.SECONDS))
            throw new RuntimeException("Unable to cancel.");
      }
      catch (InterruptedException ex)
      {
         Thread.currentThread().interrupt(); // Preserve interrupted status
      }

      complete();
   }

   @Override
   public Date getStartTime()
   {
      return startTime;
   }

   @Override
   public long getDuration()
   {
      return duration;
   }

   @Override
   public boolean isCanceled()
   {
      return canceled.get();
   }

   @Override
   public List<CrawlPage> getResult()
   {
      if (executor == null)
         throw new IllegalStateException("The crawler has not started.");

      if (!executor.isTerminated())
         throw new IllegalStateException("The crawler is running.");

      List<CrawlPage> pages = new LinkedList<>();
      for (Entry<String, Result> entry : results.entrySet())
      {
         PageDetails pageDetails = entry.getValue().pageDetails;

         CrawlPage page = new CrawlPage();
         page.setUrl(entry.getKey());

         if (pageDetails == null)
            page.setLoadError("Not Parsed.");
         else
         {
            page.setLoadError(pageDetails.getLoadError());
            page.setTitle(pageDetails.getTitle());

            // Split page links into internal and external links
            List<String> internalLinks = new LinkedList<>();
            List<String> externalLinks = new LinkedList<>();
            parseLinks(pageDetails.getLinks(), (link) -> {
               String linkUrl = link.getUrl();
               if (isDomainUrl(linkUrl))
                  internalLinks.add(linkUrl);
               else
                  externalLinks.add(linkUrl);
            });

            if (!internalLinks.isEmpty())
               page.setInternalLinks(internalLinks);

            if (!externalLinks.isEmpty())
               page.setExternalLinks(externalLinks);

            // Add all page imports and media as content links
            List<String> contentLinks = new LinkedList<>();
            parseLinks(pageDetails.getImports(), (link) -> {
               contentLinks.add(link.getUrl());
            });

            parseLinks(pageDetails.getMedia(), (link) -> {
               contentLinks.add(link.getUrl());
            });

            if (!contentLinks.isEmpty())
               page.setContentLinks(contentLinks);
         }

         pages.add(page);
      }

      return pages;
   }

   /**
    * Performs and action on every link in a list.
    * @param links List of links.
    * @param action Action to perform.
    */
   private void parseLinks(Set<LinkDetails> links, Consumer<? super LinkDetails> action)
   {
      if (links != null && !links.isEmpty())
         links.forEach(action);
   }

   /**
    * Parses a web page for links and crawls pages in the same domain.
    * If the URL has already been processed or is already queued for
    * processing, it is ignored.
    * @param url URL to the page.
    * @return True if new URL to crawl
    */
   private boolean crawl(String url)
   {
      String finalUrl = (!url.endsWith("/") ? url : url.substring(0, url.length() - 1));
      Result result = results.putIfAbsent(finalUrl, new Result());
      if (result == null)
      {
         executor.execute(() -> processPage(finalUrl));
         return true;
      }
      return false;
   }

   /**
    * This is the method that runs in the thread pool to process each page.
    * It parses the web page, stores the result, and crawls pages in the
    * same domain.
    * @param url URL to the page.
    */
   private void processPage(String url)
   {
      try
      {
         Result result = results.get(url);
         if (result == null)
            throw new RuntimeException("URL not in result.");
         if (result.pageDetails != null)
            throw new RuntimeException("URL already processed.");

         result.pageDetails = parser.parse(url);

         if (canceled.get())
            return;

         int newLinks = 0;
         Set<LinkDetails> links = result.pageDetails.getLinks();
         if (links != null && !links.isEmpty())
         {
            for (LinkDetails link : links)
            {
               String linkUrl = link.getUrl();
               if (isDomainUrl(linkUrl) && crawl(linkUrl))
                  ++newLinks;
            }
         }

         int numProcessed = pagesProcessed.incrementAndGet();

         logger.trace(String.format("Processed %s - NumProcessed: %d, NewLinks: %d, ResultSize: %d", url, numProcessed, newLinks, results.size()));

         boolean done = (numProcessed == results.size());
         if (done)
            complete();
         else
            notifyProgress();
      }
      catch (Exception ex)
      {
         logger.error(String.format("Error parsing %s.", url), ex);
      }
   }

   /**
    * Returns whether a URL is a domain URL.
    * @param url URL to check.
    * @return True if domain URL.
    */
   private boolean isDomainUrl(String url)
   {
      return (url.toLowerCase().startsWith(hostUrl));
   }

   /**
    * Raises complete event and signals the await method to return.
    */
   private void complete()
   {
      synchronized (eventLock)
      {
         if (duration == 0)
         {
            duration = System.currentTimeMillis() - startTime.getTime();

            if (eventHandler != null)
               eventHandler.onComplete(pagesProcessed.get(), canceled.get());
         }
      }

      executor.shutdown();
   }

   /**
    * Raises progress event if progress interval has been reached.
    */
   private void notifyProgress()
   {
      if (eventHandler != null && progressIntervalMillis > 0)
      {
         synchronized (eventLock)
         {
            long curTime = System.currentTimeMillis();
            long intervalMiilis = curTime - lastProgressTime;
            if (intervalMiilis > progressIntervalMillis)
            {
               eventHandler.onProgress(pagesProcessed.get());
               lastProgressTime = curTime;
            }
         }
      }
   }
}
