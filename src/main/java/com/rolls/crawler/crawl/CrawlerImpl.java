package com.rolls.crawler.crawl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rolls.crawler.parse.LinkDetails;
import com.rolls.crawler.parse.PageDetails;
import com.rolls.crawler.parse.Parser;

/**
 * Implementation of a web crawler that finds pages of a single domain.
 */
public class CrawlerImpl implements Crawler
{
   /**
    * Minimum number of threads.
    */
   public final static int MIN_THREADS = 1;

   final static Logger logger = LoggerFactory.getLogger(CrawlerImpl.class);

   private final Queue<CrawlObserver> observers = new ConcurrentLinkedQueue<>();
   private final int numThreads;
   private final Parser parser;

   private Worker worker = null;

   /**
    * Worker that handles the crawling.  This object removes itself from the outer
    * object when done.  This structure allows for minimal synchronization.
    */
   private class Worker
   {
      private final String hostUrl;
      private final ThreadPoolExecutor executor;
      private final Date startTime;

      private final Set<String> urls = new HashSet<>();
      private int urlsRequested = 0;
      private int urlsProcessed = 0;
      private boolean canceled = false;
      private boolean complete = false;

      public Worker(String hostUrl)
      {
         this.hostUrl = hostUrl;
         this.executor = new ThreadPoolExecutor(numThreads, numThreads,
               0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
               new BasicThreadFactory.Builder().namingPattern("Crawler-%d").daemon(true).build()
               );
         this.startTime = new Date();
      }

      /**
       * Waits for executor to terminate.
       * @param timeout Duration to wait for completion.
       * @param unit Time unit of timeout.
       * @return True if terminated.
       */
      public boolean awaitTermination(long timeout, TimeUnit unit)
      {
         try
         {
            return executor.awaitTermination(timeout, unit);
         }
         catch (InterruptedException e)
         {
            Thread.currentThread().interrupt(); // Preserve interrupted status
            return true;
         }
      }

      /**
       * Cancels the crawl.
       * @return True if the cancel was successful.
       */
      public boolean cancel()
      {
         canceled = true;
         executor.shutdownNow();

         try
         {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS))
               return false;
         }
         catch (InterruptedException ex)
         {
            Thread.currentThread().interrupt(); // Preserve interrupted status
            return false;
         }

         complete();
         return true;
      }

      /**
       * Submits a task to parse a web page if the URL
       * has not already been queued for parsing.
       * @param url URL to the page.
       */
      public synchronized void crawlPage(String url)
      {
         url = (!url.endsWith("/") ? url : url.substring(0, url.length() - 1));
         if (!urls.contains(url))
         {
            urls.add(url);
            ++urlsRequested;

            executor.submit(new PageParser(url, this));
         }
      }

      /**
       * Returns if the crawl has been canceled.
       * @return True if canceled.
       */
      public boolean isCanceled()
      {
         return canceled;
      }

      /**
       * Returns if a URL is a domain URL.
       * @param url URL to check.
       * @return True if domain URL.
       */
      boolean isDomainUrl(String url)
      {
         return (url.toLowerCase().startsWith(hostUrl));
      }

      /**
       * Marks a page as processed and returns if the crawl has completed.
       * @return True if completed.
       */
      public synchronized boolean pageProcessed()
      {
         ++urlsProcessed;
         return (urlsProcessed == urlsRequested);
      }

      /**
       * Marks the crawl as complete, notifies observers of the completion,
       * cleans up, and removes itself from the outer class.
       */
      public synchronized void complete()
      {
         if (complete)
            return;

         long durationMillis = System.currentTimeMillis() - startTime.getTime();

         complete = true;
         executor.shutdown();

         observers.stream().forEach((observer) -> observer.onComplete(startTime, durationMillis, canceled));

         // Delete the current worker from the outer class.
         worker = null;
      }
   }

   /**
    * Task that runs in the thread pool to process each web page.
    */
   private class PageParser implements Runnable
   {
      private final String url;
      private final Worker worker;

      public PageParser(String url, Worker worker)
      {
         this.url = url;
         this.worker = worker;
      }

      /**
       * Parses the web page, notifies observers of the page, add domain links
       * to queue for parsing, and determines if the crawl is complete.
       */
      @Override
      public void run()
      {
         try
         {
            PageDetails pageDetails = parser.parse(url);
            if (worker.isCanceled())
               return;

            CrawlPage crawlPage = buildCrawlPage(url, pageDetails);
            observers.stream().forEach((observer) -> observer.onPage(crawlPage));

            Set<LinkDetails> links = pageDetails.getLinks();
            if (links != null && !links.isEmpty())
            {
               for (LinkDetails link : links)
               {
                  String linkUrl = link.getUrl();
                  if (worker.isDomainUrl(linkUrl))
                     worker.crawlPage(linkUrl);
               }
            }

            if (worker.pageProcessed())
               worker.complete();
         }
         catch (Exception ex)
         {
            logger.error(String.format("Error parsing %s.", url), ex);
         }
      }

      /**
       * Creates a crawl page for a parsed page.
       * @param url URL that was parsed.
       * @param pageDetails Page details.
       * @return
       */
      private CrawlPage buildCrawlPage(String url, PageDetails pageDetails)
      {
         CrawlPage crawlPage = new CrawlPage();

         crawlPage.setUrl(url);
         crawlPage.setLoadError(pageDetails.getLoadError());
         crawlPage.setTitle(pageDetails.getTitle());

         // Split page links into internal and external links
         List<String> internalLinks = new LinkedList<>();
         List<String> externalLinks = new LinkedList<>();
         parseLinks(pageDetails.getLinks(), (link) -> {
            String linkUrl = link.getUrl();
            if (worker.isDomainUrl(linkUrl))
               internalLinks.add(linkUrl);
            else
               externalLinks.add(linkUrl);
         });

         if (!internalLinks.isEmpty())
            crawlPage.setInternalLinks(internalLinks);

         if (!externalLinks.isEmpty())
            crawlPage.setExternalLinks(externalLinks);

         // Add all page imports and media as content links
         List<String> contentLinks = new LinkedList<>();
         parseLinks(pageDetails.getImports(), (link) -> {
            contentLinks.add(link.getUrl());
         });

         parseLinks(pageDetails.getMedia(), (link) -> {
            contentLinks.add(link.getUrl());
         });

         if (!contentLinks.isEmpty())
            crawlPage.setContentLinks(contentLinks);

         return crawlPage;
      }
   }

   /**
    * Performs an action on every link in a list.
    * @param links List of links.
    * @param action Action to perform.
    */
   private static void parseLinks(Set<LinkDetails> links, Consumer<? super LinkDetails> action)
   {
      if (links != null && !links.isEmpty())
         links.forEach(action);
   }

   /**
    * Constructs a new web crawler.
    * @param numThreads Number of threads to use.  Value < MIN_THREADS defaults to MIN_THREADS.
    * @param parser Web page parser to use.
    */
   public CrawlerImpl(int numThreads, Parser parser)
   {
      if (parser == null)
         throw new IllegalArgumentException("Null parser.");

      this.numThreads = (numThreads >= MIN_THREADS ? numThreads : MIN_THREADS);
      this.parser = parser;
   }

   public void addObserver(CrawlObserver observer)
   {
      if (observer == null)
         throw new IllegalArgumentException("Null observer.");

      observers.add(observer);
   }

   @Override
   public synchronized void start(String startingUrl) throws MalformedURLException
   {
      if (worker != null)
         throw new IllegalStateException("The crawler is already started.");

      if (startingUrl != null)
         startingUrl = startingUrl.trim().toLowerCase();

      URL url = new URL(startingUrl);
      URL hostUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "");

      this.worker = new Worker(hostUrl.toString());
      this.worker.crawlPage(url.toString());
   }

   @Override
   public boolean await(long timeout, TimeUnit unit)
   {
      Worker activeCrawl = worker;
      if (activeCrawl != null)
         return activeCrawl.awaitTermination(timeout, unit);
      else
         return true;
   }

   @Override
   public boolean cancel()
   {
      Worker activeCrawl = worker;
      if (activeCrawl != null)
         return activeCrawl.cancel();
      else
         return true;
   }
}
