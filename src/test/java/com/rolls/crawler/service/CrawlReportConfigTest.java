package com.rolls.crawler.service;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.rolls.crawler.crawl.CrawlerImpl;

@SuppressWarnings("javadoc")
public class CrawlReportConfigTest
{
   private static String testPath = Paths.get("/JUnit_Test").toFile().getAbsolutePath();
   private static String testUrl = "http://www.google.com";

   @BeforeEach
   @AfterEach
   private void deletePTestPath() throws IOException
   {
      File pathFile = new File(testPath);
      if (pathFile.exists())
         FileUtils.deleteDirectory(pathFile);
   }

   @Test
   public void testNull()
   {
      CrawlReportConfig props = new CrawlReportConfig();
      List<String> errs = props.validate();
      assertThat(errs, containsInAnyOrder(
            "Missing starting URL.",
            "Missing output path.",
            "Missing result file."
            ));
      assertThat(props, allOf(
            hasProperty("startingUrl", is(nullValue())),
            hasProperty("outputPath", is(nullValue())),
            hasProperty("resultFile", is(nullValue())),
            hasProperty("numThreads", is(CrawlerImpl.MIN_THREADS)),
            hasProperty("progressIntervalMillis", is(0)),
            hasProperty("parseTimeoutMillis", is(0)),
            hasProperty("crawlTimeoutSeconds", is(CrawlReportConfig.MIN_CRAWL_TIMEOUT_SECONDS))
            ));
   }

   @Test
   public void testInvalidAll()
   {
      CrawlReportConfig props = new CrawlReportConfig();
      props.setStartingUrl("not a url!");
      props.setOutputPath("*?/");
      props.setResultFile("*?/");
      props.setNumThreads(CrawlerImpl.MIN_THREADS - 1);
      props.setProgressIntervalMillis(-1);
      props.setParseTimeoutMillis(-1);
      props.setCrawlTimeoutSeconds(CrawlReportConfig.MIN_CRAWL_TIMEOUT_SECONDS - 1);

      List<String> errs = props.validate();
      assertThat(errs, containsInAnyOrder(
            "Invalid starting URL.",
            "Invalid output path or unable to create the directory."
            ));
      assertThat(props, allOf(
            hasProperty("startingUrl", is("not a url!")),
            hasProperty("outputPath", is("*?/")),
            hasProperty("resultFile", is("*?/")),
            hasProperty("numThreads", is(CrawlerImpl.MIN_THREADS)),
            hasProperty("progressIntervalMillis", is(-1)),
            hasProperty("parseTimeoutMillis", is(-1)),
            hasProperty("crawlTimeoutSeconds", is(CrawlReportConfig.MIN_CRAWL_TIMEOUT_SECONDS))
            ));
   }

   @Test
   public void testOutputPathExistNonDir() throws IOException
   {
      new File(testPath).mkdirs();
      File nonDir = new File(testPath + "/NonDir");
      nonDir.createNewFile();

      CrawlReportConfig props = new CrawlReportConfig();
      props.setStartingUrl(testUrl);
      props.setOutputPath(nonDir.getAbsolutePath());
      props.setResultFile("Result.txt");
      props.setNumThreads(CrawlerImpl.MIN_THREADS);
      props.setProgressIntervalMillis(1000);
      props.setParseTimeoutMillis(5000);
      props.setCrawlTimeoutSeconds(CrawlReportConfig.MIN_CRAWL_TIMEOUT_SECONDS);

      List<String> errs = props.validate();
      assertThat(errs, contains(
            "Output path exists but is not a directory."
            ));
      assertThat(props, allOf(
            hasProperty("startingUrl", is(testUrl)),
            hasProperty("outputPath", is(nonDir.getAbsolutePath())),
            hasProperty("resultFile", is("Result.txt")),
            hasProperty("numThreads", is(CrawlerImpl.MIN_THREADS)),
            hasProperty("progressIntervalMillis", is(1000)),
            hasProperty("parseTimeoutMillis", is(5000)),
            hasProperty("crawlTimeoutSeconds", is(CrawlReportConfig.MIN_CRAWL_TIMEOUT_SECONDS))
            ));
   }

   @Test
   public void testInvalidResultFile()
   {
      CrawlReportConfig props = new CrawlReportConfig();
      props.setStartingUrl(testUrl);
      props.setOutputPath(testPath);
      props.setResultFile("*?/");
      props.setNumThreads(10);
      props.setProgressIntervalMillis(1000);
      props.setParseTimeoutMillis(5000);
      props.setCrawlTimeoutSeconds(30);

      List<String> errs = props.validate();
      assertThat(errs, contains(
            "Invalid result file."
            ));
      assertThat(props, allOf(
            hasProperty("startingUrl", is(testUrl)),
            hasProperty("outputPath", is(testPath)),
            hasProperty("resultFile", is("*?/")),
            hasProperty("numThreads", is(10)),
            hasProperty("progressIntervalMillis", is(1000)),
            hasProperty("parseTimeoutMillis", is(5000)),
            hasProperty("crawlTimeoutSeconds", is(30))
            ));
   }

   @Test
   public void testValid()
   {
      CrawlReportConfig props = new CrawlReportConfig();
      props.setStartingUrl(testUrl);
      props.setOutputPath(testPath);
      props.setResultFile("Result.txt");
      props.setNumThreads(4);
      props.setProgressIntervalMillis(1000);
      props.setParseTimeoutMillis(5000);
      props.setCrawlTimeoutSeconds(30);

      List<String> errs = props.validate();
      assertThat(errs, is(empty()));
      assertThat(props, allOf(
            hasProperty("startingUrl", is(testUrl)),
            hasProperty("outputPath", is(testPath)),
            hasProperty("resultFile", is("Result.txt")),
            hasProperty("numThreads", is(4)),
            hasProperty("progressIntervalMillis", is(1000)),
            hasProperty("parseTimeoutMillis", is(5000)),
            hasProperty("crawlTimeoutSeconds", is(30))
            ));
   }
}
