package com.rolls.crawler.crawl;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Information about a web page.
 */
@Getter
@Setter
public class CrawlPage
{
   private String url;
   private String loadError;
   private String title;
   private List<String> internalLinks;
   private List<String> externalLinks;
   private List<String> contentLinks;
}
