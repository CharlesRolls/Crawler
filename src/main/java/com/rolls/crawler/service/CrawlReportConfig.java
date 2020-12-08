package com.rolls.crawler.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import com.rolls.crawler.crawl.CrawlerImpl;
import lombok.Getter;
import lombok.Setter;

/**
 * Crawl report service configuration.
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "crawler")
@Getter
@Setter
public class CrawlReportConfig
{
   /**
    * Minimum timeout, in seconds, for crawl to complete.
    */
   public final static int MIN_CRAWL_TIMEOUT_SECONDS = 5;

   private String startingUrl;         // Starting URL.
   private String outputPath;          // Output path for logs and result file.
   private String resultFile;          // Filename to place results in.
   private int numThreads;             // Number of threads to use.
   private int progressIntervalMillis; // Interval, in milliseconds, to raise progress event.  Value <= 0 means disable.
   private int parseTimeoutMillis;     // Timeout, in milliseconds, for a page to load and parse.  Value <= 0 means no timeout.
   private int crawlTimeoutSeconds;    // Timeout, in seconds, for entire crawl to complete.

   /**
    * Checks if the properties are valid and creates the outputPath folder.
    * If crawlTimeoutSeconds, numThreads, or parseTimeoutMillis are less than
    * their minimums, the values are set to the minimum with no error.
    * @return List of errors.
    */
   public List<String> validate()
   {
      List<String> errs = new ArrayList<String>();

      if (StringUtils.isEmpty(startingUrl))
         errs.add("Missing starting URL.");
      else
      {
         String[] schemes = {"http", "https"};
         UrlValidator urlValidator = new UrlValidator(schemes);
         if (!urlValidator.isValid(startingUrl))
            errs.add("Invalid starting URL.");
      }

      if (StringUtils.isEmpty(outputPath))
         errs.add("Missing output path.");
      else
      {
         try
         {
            Path path = Paths.get(outputPath);
            File dir =  path.toFile();
            if (!dir.exists())
               dir.mkdirs();
            else if (!dir.isDirectory())
               errs.add("Output path exists but is not a directory.");
         }
         catch (Exception ex)
         {
            errs.add("Invalid output path or unable to create the directory.");
         }
      }

      if (StringUtils.isEmpty(resultFile))
         errs.add("Missing result file.");
      else if (errs.isEmpty())
      {
         // Only do this check if the output path is valid.
         try
         {
            Paths.get(outputPath, resultFile);
         }
         catch (Exception ex)
         {
            errs.add("Invalid result file.");
         }
      }

      if (numThreads < CrawlerImpl.MIN_THREADS)
         numThreads = CrawlerImpl.MIN_THREADS;

      if (crawlTimeoutSeconds < MIN_CRAWL_TIMEOUT_SECONDS)
         crawlTimeoutSeconds = MIN_CRAWL_TIMEOUT_SECONDS;

      return errs;
   }
}
