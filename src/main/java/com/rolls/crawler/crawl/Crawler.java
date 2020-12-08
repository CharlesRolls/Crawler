package com.rolls.crawler.crawl;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

/**
 * API to web crawler functionality.
 */
public interface Crawler
{
   /**
    * Adds an handler for crawl events.
    * @param observer Object to handle the events.
    */
   public void addObserver(CrawlObserver observer);

   /**
    * Starts crawling all pages in the same domain as the starting URL.
    * @param startingUrl URL to start crawling.
    * @throws MalformedURLException If starting URL is malformed.
    */
   public void start(String startingUrl) throws MalformedURLException;

   /**
    * Waits for crawl to complete.
    * @param timeout Duration to wait for completion.
    * @param unit Time unit of timeout.
    * @return True if complete or a crawl was not running.
    */
   public boolean await(long timeout, TimeUnit unit);

   /**
    * Cancels the crawl.
    * @return True if the cancel was successful or a crawl was not running.
    */
   public boolean cancel();
}
