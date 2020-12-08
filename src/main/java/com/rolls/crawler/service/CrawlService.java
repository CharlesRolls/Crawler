package com.rolls.crawler.service;

/**
 * API to a crawl service.
 */
public interface CrawlService
{
   /**
    * Adds an handler for crawl service events.
    * @param observer Object to handle the events.
    */
   public void addObserver(CrawlServiceObserver observer);

   /**
    * Runs the crawl and blocks until the crawl is completed.
    */
   public void run();
}
