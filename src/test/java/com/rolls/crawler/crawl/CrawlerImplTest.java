package com.rolls.crawler.crawl;

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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import com.rolls.crawler.parse.ParserImpl;
import com.rolls.crawler.test.TestParser;
import com.rolls.crawler.test.TestTimer;

@SuppressWarnings("javadoc")
public class CrawlerImplTest
{
   private class TestObserver implements CrawlObserver
   {
      private List<CrawlPage> pages = new LinkedList<>();
      private Date startTime = null;
      private long durationMillis = 0;
      private boolean cancelled = false;

      @Override
      public void onPage(CrawlPage page)
      {
         pages.add(page);
      }

      @Override
      public void onComplete(Date startTime, long durationMillis, boolean cancelled)
      {
         this.startTime = startTime;
         this.durationMillis = durationMillis;
         this.cancelled = cancelled;
      }
   }

   @Test
   public void testConstructorNull()
   {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
         new CrawlerImpl(0, null);
      });
      assertThat(ex.getMessage(), is("Null parser."));
   }

   @Test
   public void testAddObserverNull()
   {
      CrawlerImpl crawler = new CrawlerImpl(0, new ParserImpl(500));
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
         crawler.addObserver(null);
      });
      assertThat(ex.getMessage(), is("Null observer."));
   }

   @Test
   public void testAwaitCancelNotStarted()
   {
      CrawlerImpl crawler = new CrawlerImpl(CrawlerImpl.MIN_THREADS - 1, new ParserImpl(500));

      // Check await()
      TestTimer testTimer = TestTimer.startNewTimer();
      boolean ret = crawler.await(10, TimeUnit.SECONDS);

      assertThat(testTimer.endTimer(), lessThan(10L));
      assertThat(ret, is(true));

      // Check cancel()
      testTimer.start();;
      ret = crawler.cancel();

      assertThat(testTimer.endTimer(), lessThan(10L));
      assertThat(ret, is(true));
   }

   @Test
   public void testStartInvalidUrl()
   {
      CrawlerImpl crawler = new CrawlerImpl(CrawlerImpl.MIN_THREADS, new ParserImpl(500));

      assertThrows(MalformedURLException.class, () -> {
         crawler.start(null);
      });

      assertThrows(MalformedURLException.class, () -> {
         crawler.start("");
      });

      assertThrows(MalformedURLException.class, () -> {
         crawler.start("not a real url");
      });
   }

   @Test
   public void testSecondStartAndCancel() throws MalformedURLException
   {
      TestParser parser = new TestParser(30 * 1000);
      TestObserver observer = new TestObserver();

      CrawlerImpl crawler = new CrawlerImpl(2, parser);
      crawler.addObserver(observer);
      crawler.start(TestParser.baseUrl);

      IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
         crawler.start("http://www.notrealsite.org");
      });
      assertThat(ex.getMessage(), is("The crawler is already started."));

      TestTimer testTimer = TestTimer.startNewTimer();
      crawler.cancel();

      assertThat(testTimer.endTimer(), lessThan(100L));
      assertThat(parser.getParseInterrupts(), is(1));

      assertThat(System.currentTimeMillis() - observer.startTime.getTime(), lessThan(100L));
      assertThat(observer.durationMillis, lessThan(100L));
      assertThat(observer.cancelled, is(true));
      assertThat(observer.pages, is(empty()));
   }

   @Test
   public void testFullCrawl() throws MalformedURLException
   {
      TestParser parser = new TestParser(100);
      TestObserver observer = new TestObserver();

      CrawlerImpl crawler = new CrawlerImpl(2, parser);
      crawler.addObserver(observer);
      crawler.start(TestParser.baseUrl);

      if (!crawler.await(3, TimeUnit.SECONDS))
      {
         crawler.cancel();
         fail("Did not complete in time.");
      }

      assertThat(parser.getParseInterrupts(), is(0));

      assertThat(System.currentTimeMillis() - observer.startTime.getTime(), lessThan(3000L));
      assertThat(observer.durationMillis, lessThan(1000L));
      assertThat(observer.cancelled, is(false));

      assertThat(observer.pages, containsInAnyOrder(
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
                  hasProperty("internalLinks", contains(TestParser.baseUrl + "/")),
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
                        TestParser.baseUrl + "/",
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
