package com.rolls.crawler.report;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Information to be saved to the report file.
 */
@Getter
@Setter
public class ReportDetails
{
   private String startingUrl;
   private Date startTime;
   private long durationMillis;
   private boolean canceled;
   private List<ReportPage> pages;
}
