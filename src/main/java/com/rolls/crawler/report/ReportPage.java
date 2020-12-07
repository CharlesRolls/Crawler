package com.rolls.crawler.report;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Information about a web page.
 */
@Getter
@Setter
public class ReportPage
{
   private String url;
   private String loadError;
   private String title;
   private List<String> internalLinks;
   private List<String> externalLinks;
   private List<String> contentLinks;
}
