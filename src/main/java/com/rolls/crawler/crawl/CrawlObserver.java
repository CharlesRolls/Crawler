package com.rolls.crawler.crawl;

import java.util.Date;

/**
 * Handler of crawl events.
 */
public interface CrawlObserver
{
   /**
    * A page has been processed.
    * @param page Crawl page.
    */
   public void onPage(CrawlPage page);

   /**
    * The crawl has completed.
    * @param startTime Start time of the crawl.
    * @param durationMillis Duration, in milliseconds, of the crawl.
    * @param cancelled True if the crawl was cancelled.
    */
   public void onComplete(Date startTime, long durationMillis, boolean cancelled);
}
