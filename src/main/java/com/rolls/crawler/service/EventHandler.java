package com.rolls.crawler.service;

/**
 * Events raised by crawler.
 */
public interface EventHandler
{
   /**
    * Indicates the current progress of the crawler.
    * @param pagesProcessed Number of pages that have been processed.
    */
   public void onProgress(int pagesProcessed);

   /**
    * Indicates the crawler is complete.
    * @param pagesProcessed Number of pages that have been processed.
    * @param canceled Whether crawl was canceled.
    */
   public void onComplete(int pagesProcessed, boolean canceled);
}
