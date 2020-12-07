package com.rolls.crawler.report;

/**
 * API to report writer functionality.
 */
public interface ReportWriter
{
   /**
    * Saves crawl results.
    * @param reportDetails Details to save.
    */
   void save(ReportDetails reportDetails);
}
