package com.rolls.crawler.service;

import java.util.List;

/**
 *  Handler of crawl service events.
 */
public interface CrawlServiceObserver
{
   /**
    * Indicates the crawl is starting.
    * @param startingUrl Starting URL.
    */
   public void onStart(String startingUrl);

   /**
    * Indicates errors while running the crawl.
    * @param errors List of errors.
    */
   public void onError(List<String> errors);

   /**
    * Indicates the current progress of the crawler.
    * @param pagesProcessed Number of pages that have been processed.
    */
   public void onProgress(int pagesProcessed);

   /**
    * Indicates the crawler is complete.
    * @param pagesProcessed Number of pages that have been processed.
    * @param cancelled Whether crawl was cancelled.
    * @param reportPath Location of report file.
    */
   public void onComplete(int pagesProcessed, boolean cancelled, String reportPath);
}
