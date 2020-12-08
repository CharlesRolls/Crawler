package com.rolls.crawler.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.rolls.crawler.test.TestTimer;

@SuppressWarnings("javadoc")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CrawlReportServiceImpl.class, CrawlReportConfig.class})
public class CrawlReportServiceImplTest
{
   private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

   @Autowired
   private CrawlReportConfig config;

   @Autowired
   private CrawlReportServiceImpl reportCrawler;

   private class TestObserver implements CrawlServiceObserver
   {
      private String startingUrl = null;
      private List<String> errors = new ArrayList<>();
      private List<Integer> onProgressCalls = new LinkedList<>();
      private boolean completeCalled = false;
      private int pagesProcessed = 0;
      private boolean cancelled = false;
      private String reportPath = null;

      @Override
      public void onStart(String startingUrl)
      {
         this.startingUrl = startingUrl;
      }
      @Override
      public void onError(List<String> errors)
      {
         this.errors.addAll(errors);
      }

      @Override
      public void onProgress(int pagesProcessed)
      {
         onProgressCalls.add(pagesProcessed);
      }

      @Override
      public void onComplete(int pagesProcessed, boolean cancelled, String reportPath)
      {
         this.completeCalled = true;
         this.pagesProcessed = pagesProcessed;
         this.cancelled = cancelled;
         this.reportPath = reportPath;
      }
   }

   @BeforeEach
   @AfterEach
   private void deleteReportFile() throws IOException
   {
      if (config.getOutputPath() != null && config.getResultFile() != null)
      {
         File file = Paths.get(config.getOutputPath(), config.getResultFile()).toFile();
         if (file.exists())
            file.delete();
      }
   }

   @Test
   public void testAddObserverNull()
   {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
         reportCrawler.addObserver(null);
      });
      assertThat(ex.getMessage(), is("Null observer."));
   }

   @Test
   @DirtiesContext
   public void testRunErrors() throws MalformedURLException
   {
      TestObserver observer = new TestObserver();

      config.setStartingUrl(null);
      config.setOutputPath(null);
      config.setResultFile(null);

      reportCrawler.addObserver(observer);

      TestTimer testTimer = TestTimer.startNewTimer();
      reportCrawler.run();

      assertThat(testTimer.endTimer(), lessThan(100L));

      assertThat(observer.startingUrl, is(nullValue()));
      assertThat(observer.errors, containsInAnyOrder(
            "Missing starting URL.",
            "Missing output path.",
            "Missing result file."
            ));
      assertThat(observer.onProgressCalls, is(empty()));
      assertThat(observer.completeCalled, is(false));
      assertThat(observer.pagesProcessed, is(0));
      assertThat(observer.cancelled, is(false));
      assertThat(observer.reportPath, is(nullValue()));
   }

   @Test
   @DirtiesContext
   public void testRunNonExitingUrl() throws IOException
   {
      TestObserver observer = new TestObserver();
      File file = Paths.get(config.getOutputPath(), config.getResultFile()).toFile();

      config.setStartingUrl("http://www.notrealsite.org/site/");

      reportCrawler.addObserver(observer);

      TestTimer testTimer = TestTimer.startNewTimer();
      reportCrawler.run();

      assertThat(testTimer.endTimer(), lessThan((long)(config.getParseTimeoutMillis() + 100)));

      assertThat(observer.startingUrl, is("http://www.notrealsite.org/site/"));
      assertThat(observer.errors, is(empty()));
      assertThat(observer.onProgressCalls, is(empty()));
      assertThat(observer.completeCalled, is(true));
      assertThat(observer.pagesProcessed, is(1));
      assertThat(observer.cancelled, is(false));
      assertThat(observer.reportPath, is(file.getAbsolutePath()));

      String actualRpt = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
      String[] lines = actualRpt.split(System.lineSeparator());

      assertThat(lines.length, greaterThanOrEqualTo(6));
      assertThat(lines[0], is("Starting URL: http://www.notrealsite.org/site/"));
      assertThat(lines[1], startsWith("Start Time: " + sdf.format(new Date())));
      assertThat(lines[2], startsWith("Duration: "));
      assertThat(lines[3], is(""));
      assertThat(lines[4], is("Page: http://www.notrealsite.org/site"));
      assertThat(lines[5], startsWith(" - Load Error: Unable to load http://www.notrealsite.org/site.  CAUSE:"));
   }

   @Test
   @DirtiesContext
   public void testRunWithProgressAndTimeout() throws IOException
   {
      TestObserver observer = new TestObserver();
      File file = Paths.get(config.getOutputPath(), config.getResultFile()).toFile();

      reportCrawler.addObserver(observer);

      TestTimer testTimer = TestTimer.startNewTimer();
      reportCrawler.run();

      long durationMillis = testTimer.endTimer();
      long crawlTimeoutMillis = config.getCrawlTimeoutSeconds() * 1000;
      assertThat(durationMillis, greaterThanOrEqualTo(crawlTimeoutMillis));
      assertThat(durationMillis, lessThan(crawlTimeoutMillis + 1000L));

      assertThat(observer.startingUrl, is(config.getStartingUrl()));
      assertThat(observer.errors, is(empty()));
      assertThat(observer.onProgressCalls.size(), greaterThan((int)((crawlTimeoutMillis / config.getParseTimeoutMillis()) * 0.9)));
      assertThat(observer.completeCalled, is(true));
      assertThat(observer.pagesProcessed, greaterThan(40));
      assertThat(observer.cancelled, is(true));
      assertThat(observer.reportPath, is(file.getAbsolutePath()));

      assertThat(file.exists(), is(true));
      assertThat(file.length(), greaterThan(10000L));
   }
}
