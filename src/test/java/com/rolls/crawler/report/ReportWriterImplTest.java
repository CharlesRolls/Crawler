package com.rolls.crawler.report;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("javadoc")
public class ReportWriterImplTest
{
   private static String testPath = Paths.get("/JUnit_Test").toFile().getAbsolutePath();
   private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");

   @BeforeEach
   @AfterEach
   private void deleteTestPath() throws IOException
   {
      File pathFile = new File(testPath);
      if (pathFile.exists())
         FileUtils.deleteDirectory(pathFile);
   }

   @Test
   public void testConstructorNull()
   {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
         new ReportWriterImpl(null);
      });
      assertThat(ex.getMessage(), is("Null file."));
   }

   @Test
   public void testSaveNull()
   {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
         new ReportWriterImpl(new File(testPath + "/result.txt")).save(null);
      });
      assertThat(ex.getMessage(), is("Null report details."));
   }

   @Test
   public void testSaveEmptyReportDetails() throws IOException
   {
      // Start with directory NOT existing.
      File filePath = new File(testPath + "/result.txt");
      ReportWriterImpl writer = new ReportWriterImpl(filePath);

      StringBuilder strBld = new StringBuilder();
      strBld.append("Starting URL: null").append(System.lineSeparator());
      strBld.append("Start Time: null").append(System.lineSeparator());
      strBld.append("Duration: 0.00 minutes").append(System.lineSeparator());
      strBld.append(System.lineSeparator());
      strBld.append("No pages found!!!").append(System.lineSeparator());

      writer.save(new ReportDetails());
      String actualRpt = FileUtils.readFileToString(filePath, StandardCharsets.UTF_8);
      assertThat(actualRpt, is(strBld.toString()));
   }

   @Test
   public void testSaveToDirPath()
   {
      File dirPath = new File(testPath + "/Dir");
      dirPath.mkdirs();

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
         new ReportWriterImpl(dirPath).save(new ReportDetails());
      });
      assertThat(ex.getMessage(), is(String.format("Can't write to %s.", dirPath.getAbsoluteFile())));
   }

   @Test
   public void testSaveInvalidFile()
   {
      RuntimeException ex = assertThrows(RuntimeException.class, () -> {
         new ReportWriterImpl(new File(testPath + "/*?/")).save(new ReportDetails());
      });
      assertThat(ex.getMessage(), is("Unable to save."));
   }

   @Test
   public void testSaveNewFile() throws IOException
   {
      // Start with directory existing.
      new File(testPath).mkdirs();

      File filePath = new File(testPath + "/result.txt");
      ReportWriterImpl writer = new ReportWriterImpl(filePath);

      List<ReportPage> pages = new LinkedList<>();
      pages.add(new ReportPage());
      pages.add(createEmptyPage());
      pages.add(createLoadErrorPage());
      pages.add(createValidPage());

      ReportDetails rpt1 = new ReportDetails();
      rpt1.setStartingUrl("http://www.one.com");
      rpt1.setStartTime(new Date());
      rpt1.setDurationMillis(60 * 1000);
      rpt1.setPages(pages);

      StringBuilder strBld = new StringBuilder();
      strBld.append("Starting URL: http://www.one.com").append(System.lineSeparator());
      strBld.append("Start Time: ").append(sdf.format(rpt1.getStartTime())).append(System.lineSeparator());
      strBld.append("Duration: 1.00 minutes").append(System.lineSeparator());
      strBld.append(System.lineSeparator());
      strBld.append("Page: null").append(System.lineSeparator());
      strBld.append(" - Title: null").append(System.lineSeparator());
      strBld.append(" - Internal Links: NONE").append(System.lineSeparator());
      strBld.append(" - External Links: NONE").append(System.lineSeparator());
      strBld.append(" - Content Links: NONE").append(System.lineSeparator());
      strBld.append(System.lineSeparator());
      strBld.append("Page: ").append(System.lineSeparator());
      strBld.append(" - Title: ").append(System.lineSeparator());
      strBld.append(" - Internal Links: NONE").append(System.lineSeparator());
      strBld.append(" - External Links: NONE").append(System.lineSeparator());
      strBld.append(" - Content Links: NONE").append(System.lineSeparator());
      strBld.append(System.lineSeparator());
      strBld.append("Page: http://www.one.com/error.html").append(System.lineSeparator());
      strBld.append(" - Load Error: Unable to load.").append(System.lineSeparator());
      strBld.append(System.lineSeparator());
      strBld.append("Page: http://www.one.com/valid.html").append(System.lineSeparator());
      strBld.append(" - Title: Valid Page").append(System.lineSeparator());
      strBld.append(" - Internal Links:").append(System.lineSeparator());
      strBld.append("     http://www.one.com/pg1.html").append(System.lineSeparator());
      strBld.append("     http://www.one.com/pg2.html").append(System.lineSeparator());
      strBld.append(" - External Links:").append(System.lineSeparator());
      strBld.append("     http://www.two.com/pg1.html").append(System.lineSeparator());
      strBld.append("     http://www.two.com/pg2.html").append(System.lineSeparator());
      strBld.append(" - Content Links:").append(System.lineSeparator());
      strBld.append("     http://www.one.com/img1.jpg").append(System.lineSeparator());
      strBld.append("     http://www.one.com/img2.jpg").append(System.lineSeparator());

      writer.save(rpt1);
      String actualRpt = FileUtils.readFileToString(filePath, StandardCharsets.UTF_8);
      assertThat(actualRpt, is(strBld.toString()));
   }

   private ReportPage createEmptyPage()
   {
      ReportPage page = new ReportPage();
      page.setUrl("");
      page.setLoadError("");
      page.setTitle("");
      page.setInternalLinks(new LinkedList<String>());
      page.setExternalLinks(new LinkedList<String>());
      page.setContentLinks(new LinkedList<String>());
      return page;
   }

   private ReportPage createLoadErrorPage()
   {
      ReportPage page = new ReportPage();
      page.setUrl("http://www.one.com/error.html");
      page.setLoadError("Unable to load.");
      return page;
   }

   private ReportPage createValidPage()
   {
      ReportPage page = new ReportPage();
      page.setUrl("http://www.one.com/valid.html");
      page.setTitle("Valid Page");
      page.setInternalLinks(Arrays.asList("http://www.one.com/pg1.html", "http://www.one.com/pg2.html"));
      page.setExternalLinks(Arrays.asList("http://www.two.com/pg1.html", "http://www.two.com/pg2.html"));
      page.setContentLinks(Arrays.asList("http://www.one.com/img1.jpg", "http://www.one.com/img2.jpg"));
      return page;
   }

   @Test
   public void testSaveAppendFile() throws IOException
   {
      // Start with existing 0 byte file already exists so we can append to it.
      new File(testPath).mkdirs();
      File filePath = new File(testPath + "/result.txt");
      filePath.createNewFile();

      ReportWriterImpl writer = new ReportWriterImpl(filePath);
      long curTime = System.currentTimeMillis();

      // Save report to new file.
      ReportDetails rpt1 = new ReportDetails();
      rpt1.setStartingUrl("http://www.one.com");
      rpt1.setStartTime(new Date(curTime - (10 * 60 * 1000)));
      rpt1.setDurationMillis(15 * 1000);
      rpt1.setCancelled(true);
      rpt1.setPages(new LinkedList<ReportPage>());

      StringBuilder strBld = new StringBuilder();
      strBld.append("Starting URL: http://www.one.com").append(System.lineSeparator());
      strBld.append("Start Time: ").append(sdf.format(rpt1.getStartTime())).append(System.lineSeparator());
      strBld.append("Duration: 0.25 minutes - CANCELLED !!!").append(System.lineSeparator());
      strBld.append(System.lineSeparator());
      strBld.append("No pages found!!!").append(System.lineSeparator());

      writer.save(rpt1);
      String actualRpt = FileUtils.readFileToString(filePath, StandardCharsets.UTF_8);
      assertThat(actualRpt, is(strBld.toString()));

      // Append report to existing file
      ReportDetails rpt2 = new ReportDetails();
      rpt2.setStartingUrl("http://www.two.com");
      rpt2.setStartTime(new Date(curTime - (5 * 60 * 1000)));
      rpt2.setDurationMillis(150 * 1000);
      rpt2.setPages(new LinkedList<ReportPage>());

      strBld.append(System.lineSeparator());
      strBld.append(System.lineSeparator());
      strBld.append("--------------------------------------------------------------------------------");
      strBld.append(System.lineSeparator());
      strBld.append(System.lineSeparator());
      strBld.append("Starting URL: http://www.two.com").append(System.lineSeparator());
      strBld.append("Start Time: ").append(sdf.format(rpt2.getStartTime())).append(System.lineSeparator());
      strBld.append("Duration: 2.50 minutes").append(System.lineSeparator());
      strBld.append(System.lineSeparator());
      strBld.append("No pages found!!!").append(System.lineSeparator());

      writer.save(rpt2);
      actualRpt = FileUtils.readFileToString(filePath, StandardCharsets.UTF_8);
      assertThat(actualRpt, is(strBld.toString()));
   }
}
