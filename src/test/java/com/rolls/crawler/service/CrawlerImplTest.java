package com.rolls.crawler.service;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import com.rolls.crawler.parse.ParserImpl;

@SuppressWarnings("javadoc")
public class CrawlerImplTest
{
   private class TestEventHandler implements EventHandler
   {
      private List<Integer> onProgressCalls = new LinkedList<>();
      private int pagesProcessed = 0;
      private boolean canceled = false;

      @Override
      public void onProgress(int pagesProcessed)
      {
         onProgressCalls.add(pagesProcessed);
      }

      @Override
      public void onComplete(int pagesProcessed, boolean canceled)
      {
         this.pagesProcessed = pagesProcessed;
         this.canceled = canceled;
      }
   }

   @Test
   public void testConstructorNull()
   {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
         new CrawlerImpl(0, 0, null);
      });
      assertThat(ex.getMessage(), is("Null parser."));
   }

   @Test
   public void testAwaitIllegalState()
   {
      CrawlerImpl crawler = new CrawlerImpl(0, 0, new ParserImpl(500));
      IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
         crawler.await(10, TimeUnit.SECONDS);
      });
      assertThat(ex.getMessage(), is("The crawler has not started."));
   }

   @Test
   public void testCancelIllegalState()
   {
      CrawlerImpl crawler = new CrawlerImpl(0, 0, new ParserImpl(500));
      IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
         crawler.cancel();
      });
      assertThat(ex.getMessage(), is("The crawler has not started."));
   }

   @Test
   public void testGetResultIllegalState()
   {
      CrawlerImpl crawler = new CrawlerImpl(0, 0, new ParserImpl(500));
      IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
         crawler.getResult();
      });
      assertThat(ex.getMessage(), is("The crawler has not started."));
   }

   @Test
   public void testStartInvalidUrl()
   {
      CrawlerImpl crawler = new CrawlerImpl(2, 0, new ParserImpl(500));

      assertThrows(MalformedURLException.class, () -> {
         crawler.start(null, null);
      });

      assertThrows(MalformedURLException.class, () -> {
         crawler.start("", null);
      });

      assertThrows(MalformedURLException.class, () -> {
         crawler.start("not a real url", null);
      });
   }

   @Test
   public void testStartedIllegalState() throws MalformedURLException
   {
      TestEventHandler eventHandler = new TestEventHandler();
      TestParser parser = new TestParser(30 * 1000);

      CrawlerImpl crawler = new CrawlerImpl(2, 0, parser);
      crawler.start(TestParser.baseUrl, eventHandler);

      IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
         crawler.start("http://www.notrealsite.org", eventHandler);
      });
      assertThat(ex.getMessage(), is("The crawler is already started."));

      ex = assertThrows(IllegalStateException.class, () -> {
         crawler.getResult();
      });
      assertThat(ex.getMessage(), is("The crawler is running."));

      assertThat(crawler.getDuration(), is(0L));

      long startTime = System.currentTimeMillis();
      crawler.cancel();
      long duration = System.currentTimeMillis() - startTime;
      assertThat(duration, lessThan(250L));

      assertThat(System.currentTimeMillis() - crawler.getStartTime().getTime(), lessThan(250L));
      assertThat(crawler.getDuration(), lessThan(250L));
      assertThat(crawler.isCanceled(), is(true));

      assertThat(parser.getParseInterrupts(), is(1));
      assertThat(eventHandler.onProgressCalls, is(empty()));
      assertThat(eventHandler.pagesProcessed, is(0));
      assertThat(eventHandler.canceled, is(true));
   }

   @Test
   public void testFullCrawl() throws MalformedURLException
   {
      TestEventHandler eventHandler = new TestEventHandler();
      TestParser parser = new TestParser(100);

      CrawlerImpl crawler = new CrawlerImpl(2, 100, parser);
      crawler.start(TestParser.baseUrl, eventHandler);

      if (!crawler.await(3, TimeUnit.SECONDS))
      {
         crawler.cancel();
         fail("Did not complete in time.");
      }

      assertThat(System.currentTimeMillis() - crawler.getStartTime().getTime(), lessThan(3000L));
      assertThat(crawler.getDuration(), lessThan(1000L));
      assertThat(crawler.isCanceled(), is(false));

      assertThat(parser.getParseInterrupts(), is(0));
      assertThat(eventHandler.onProgressCalls.get(0), is(1));        // After 100 millis should only have done first page
      assertThat(eventHandler.onProgressCalls.size(), lessThan(5));  // Timing of using 2 threads and the way pages link together, checking max progress calls
      assertThat(eventHandler.pagesProcessed, is(6));
      assertThat(eventHandler.canceled, is(false));

      List<CrawlPage> crawlPages = crawler.getResult();
      assertThat(crawlPages, containsInAnyOrder(
            allOf(
                  hasProperty("url", is(TestParser.baseUrl)),
                  hasProperty("loadError", anyOf(nullValue(), empty())),
                  hasProperty("title", is("Home")),
                  hasProperty("internalLinks", containsInAnyOrder(
                        TestParser.baseUrl + "/login.html",
                        TestParser.baseUrl + "/about.html"
                        )),
                  hasProperty("externalLinks", contains("http://www.google.com")),
                  hasProperty("contentLinks", containsInAnyOrder(
                        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css",
                        TestParser.baseUrl + "/img/home.jpg",
                        TestParser.baseUrl + "/js/home.js"
                        ))
                  ),
            allOf(
                  hasProperty("url", is(TestParser.baseUrl + "/login.html")),
                  hasProperty("loadError", anyOf(nullValue(), empty())),
                  hasProperty("title", is("Login")),
                  hasProperty("internalLinks", containsInAnyOrder(
                        TestParser.baseUrl,
                        TestParser.baseUrl + "/admin/one.html"
                        )),
                  hasProperty("externalLinks", contains("http://www.microsoft.com/somepage.html")),
                  hasProperty("contentLinks", containsInAnyOrder(
                        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css",
                        TestParser.baseUrl + "/img/login.jpg",
                        TestParser.baseUrl + "/js/login.js"
                        ))
                  ),
            allOf(
                  hasProperty("url", is(TestParser.baseUrl + "/about.html")),
                  hasProperty("loadError", anyOf(nullValue(), empty())),
                  hasProperty("title", is("About")),
                  hasProperty("internalLinks", contains(TestParser.baseUrl)),
                  hasProperty("externalLinks", anyOf(nullValue(), empty())),
                  hasProperty("contentLinks", containsInAnyOrder(
                        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css",
                        TestParser.baseUrl + "/img/about.jpg",
                        TestParser.baseUrl + "/js/about.js"
                        ))
                  ),
            allOf(
                  hasProperty("url", is(TestParser.baseUrl + "/admin/one.html")),
                  hasProperty("loadError", anyOf(nullValue(), empty())),
                  hasProperty("title", is("Admin One")),
                  hasProperty("internalLinks", containsInAnyOrder(
                        TestParser.baseUrl,
                        TestParser.baseUrl + "/about.html",
                        TestParser.baseUrl + "/admin/two.html",
                        TestParser.baseUrl + "/admin/three.html"
                        )),
                  hasProperty("externalLinks", contains(TestParser.baseUrlSecure + "/admin/four.html")),
                  hasProperty("contentLinks", containsInAnyOrder(
                        "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css",
                        TestParser.baseUrl + "/img/admin/one.jpg",
                        TestParser.baseUrl + "/js/admin/one.js"
                        ))
                  ),
            allOf(
                  hasProperty("url", is(TestParser.baseUrl + "/admin/two.html")),
                  hasProperty("loadError", anyOf(nullValue(), empty())),
                  hasProperty("title", anyOf(nullValue(), empty())),
                  hasProperty("internalLinks", anyOf(nullValue(), empty())),
                  hasProperty("externalLinks", anyOf(nullValue(), empty())),
                  hasProperty("contentLinks", anyOf(nullValue(), empty()))
                  ),
            allOf(
                  hasProperty("url", is(TestParser.baseUrl + "/admin/three.html")),
                  hasProperty("loadError", is("Unable to load page.")),
                  hasProperty("title", anyOf(nullValue(), empty())),
                  hasProperty("internalLinks", anyOf(nullValue(), empty())),
                  hasProperty("externalLinks", anyOf(nullValue(), empty())),
                  hasProperty("contentLinks", anyOf(nullValue(), empty()))
                  )
            ));
   }
}
