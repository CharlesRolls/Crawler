package com.rolls.crawler.service;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API to web crawler functionality.
 */
public interface Crawler
{
   /**
    * Starts crawling all pages in the same domain as the starting URL.
    * @param url Starting URL.
    * @param eventHandler Event handler.
    * @throws MalformedURLException If starting URL is malformed.
    */
   public void start(String url, EventHandler eventHandler) throws MalformedURLException;

   /**
    * Waits for crawl to complete.
    * @param timeout Duration to wait for completion.
    * @param unit Time unit of timeout.
    * @return True if complete.  False if not complete.
    */
   public boolean await(long timeout, TimeUnit unit);

   /**
    * Cancels the crawl.
    * with canceled set to false.
    */
   public void cancel();

   /**
    * Gets the start time of the crawl.
    * @return Start time.
    */
   public Date getStartTime();

   /**
    * Gets the duration of the crawl.
    * @return Duration.
    */
   public long getDuration();

   /**
    * Returns whether the crawl has been canceled.
    * @return True if canceled.
    */
   public boolean isCanceled();

   /**
    * Gets the result of the crawl.
    * @return List of pages in the same domain as the starting URL.
    */
   public List<CrawlPage> getResult();
}
