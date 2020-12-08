package com.rolls.crawler.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of a report writer that saves crawl results to a file.
 * If the file exists, it is appended to.
 */
public class ReportWriterImpl implements ReportWriter
{
   private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");

   private final File file;

   /**
    * Constructs an new report writer.
    * @param file File to write to.
    */
   public ReportWriterImpl(File file)
   {
      if (file == null)
         throw new IllegalArgumentException("Null file.");

      this.file = file;
   }

   @Override
   public void save(ReportDetails reportDetails)
   {
      if (reportDetails == null)
         throw new IllegalArgumentException("Null report details.");

      boolean append = false;
      if (file.exists())
      {
         if (!file.isFile())
            throw new IllegalArgumentException(String.format("Can't write to %s.", file.getAbsoluteFile()));

         append = file.length() > 0;
      }
      else
      {
         File dir = file.getParentFile();
         if (!file.exists())
            dir.mkdirs();
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true)))
      {
         if (append)
            writeSeparator(writer);

         writeHeader(writer, reportDetails);
         writePages(writer, reportDetails.getPages());
      }
      catch (Exception ex)
      {
         throw new RuntimeException("Unable to save.", ex);
      }
   }

   /**
    * Writes a report separator to the file.
    * @param writer File writer.
    * @throws IOException If write fails.
    */
   private void writeSeparator(BufferedWriter writer) throws IOException
   {
      writer.newLine();
      writer.newLine();
      writer.write("--------------------------------------------------------------------------------");
      writer.newLine();
      writer.newLine();
   }

   /**
    * Writes a report header to the file.
    * @param writer File writer.
    * @param reportDetails Report details to write.
    * @throws IOException If write fails.
    */
   private void writeHeader(BufferedWriter writer, ReportDetails reportDetails) throws IOException
   {
      writer.write(String.format("Starting URL: %s", reportDetails.getStartingUrl()));
      writer.newLine();

      Date startTime = reportDetails.getStartTime();
      writer.write(String.format("Start Time: %s", (startTime != null ? sdf.format(startTime) : "null")));
      writer.newLine();

      writer.write(String.format("Duration: %.2f minutes", (double)reportDetails.getDurationMillis() / (double)1000 / (double)60));
      if (reportDetails.isCancelled())
         writer.write(" - CANCELLED !!!");
      writer.newLine();
   }

   /**
    * Writes a report header to the file.
    * @param writer File writer.
    * @param reportPages Report pages to write.
    * @throws IOException If write fails.
    */
   private void writePages(BufferedWriter writer, List<ReportPage> pages) throws IOException
   {
      if (pages == null || pages.isEmpty())
      {
         writer.newLine();

         writer.write("No pages found!!!");
         writer.newLine();
         return;
      }

      for (ReportPage page : pages)
      {
         writer.newLine();

         writer.write(String.format("Page: %s", page.getUrl()));
         writer.newLine();

         String loadErr = page.getLoadError();
         if (!StringUtils.isEmpty(loadErr))
         {
            writer.write(String.format(" - Load Error: %s", loadErr));
            writer.newLine();
         }
         else
         {
            writer.write(String.format(" - Title: %s", page.getTitle()));
            writer.newLine();

            writeLinks(writer, "Internal Links", page.getInternalLinks());
            writeLinks(writer, "External Links", page.getExternalLinks());
            writeLinks(writer, "Content Links", page.getContentLinks());
         }
      }
   }

   /**
    * Writes a list of links to the file.
    * @param writer File writer.
    * @param linkType Type of links.
    * @param links Links to write.
    * @throws IOException If write fails.
    */
   private void writeLinks(BufferedWriter writer, String linkType, List<String> links) throws IOException
   {
      boolean haveLinks = (links != null && !links.isEmpty());

      writer.write(String.format(" - %s:", linkType));
      if (!haveLinks)
         writer.write(" NONE");
      writer.newLine();

      if (haveLinks)
      {
         for (String link : links)
         {
            writer.write(String.format("     %s", link));
            writer.newLine();
         }
      }
   }
}
