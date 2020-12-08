package com.rolls.crawler.parse;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.rolls.crawler.parse.PageDetails;
import com.rolls.crawler.parse.Parser;
import com.rolls.crawler.parse.ParserImpl;

@SuppressWarnings("javadoc")
public class ParserImplTest
{
   @Test
   public void testLoadFail()
   {
      Parser parser = new ParserImpl(-1);
      PageDetails details = parser.parse("http://www.notrealsite.org/somepagethatdoesnotexist.html");
      assertThat(details, allOf(
            hasProperty("loadError", startsWith("Unable to load http://www.notrealsite.org/somepagethatdoesnotexist.html.  CAUSE: ")),
            hasProperty("title", anyOf(nullValue(), empty())),
            hasProperty("imports", anyOf(nullValue(), empty())),
            hasProperty("links", anyOf(nullValue(), empty())),
            hasProperty("media", anyOf(nullValue(), empty()))
            ));
   }

   @Test
   public void testValid() throws URISyntaxException, IOException
   {
      String baseUri = "http://www.notrealsite.org/site/";
      String url = baseUri + "ParserTest.html";

      Connection conn = mock(Connection.class);
      when(conn.followRedirects(Mockito.anyBoolean())).thenReturn(conn);
      when(conn.timeout(Mockito.anyInt())).thenReturn(conn);
      when(conn.get()).thenReturn(loadTestPage(baseUri));

      mockStatic(Jsoup.class);
      when(Jsoup.connect(Mockito.anyString())).thenReturn(conn);

      Parser parser = new ParserImpl(10000);
      PageDetails details = parser.parse(url);
      assertThat(details, allOf(
            hasProperty("loadError", nullValue()),
            hasProperty("title", is("Insert title here")),
            hasProperty("imports", containsInAnyOrder(
                  allOf(
                        hasProperty("tag", is("link")),
                        hasProperty("url", is("http://www.notrealsite.org/css/login.css"))
                        ),
                  allOf(
                        hasProperty("tag", is("link")),
                        hasProperty("url", is("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"))
                        )
                  )),
            hasProperty("media", containsInAnyOrder(
                  allOf(
                        hasProperty("tag", is("script")),
                        hasProperty("url", is("http://www.notrealsite.org/site/include/jquery-3.4.0.min.js"))
                        ),
                  allOf(
                        hasProperty("tag", is("script")),
                        hasProperty("url", is("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"))
                        ),
                  allOf(
                        hasProperty("tag", is("img")),
                        hasProperty("url", is("http://www.notrealsite.org/images/login.jpg"))
                        ),
                  allOf(
                        hasProperty("tag", is("source")),
                        hasProperty("url", is("https://www.notrealsite.org/video/company.mp4"))
                        )
                  )),
            hasProperty("links", containsInAnyOrder(
                  allOf(
                        hasProperty("tag", is("a")),
                        hasProperty("url", is("http://www.notrealsite.org/site/admin/schedule"))
                        ),
                  allOf(
                        hasProperty("tag", is("a")),
                        hasProperty("url", is("https://www.google.com"))
                        )
                  ))
            ));
   }

   private Document loadTestPage(String baseUri) throws URISyntaxException, IOException
   {
      File file = new File(Thread.currentThread().getContextClassLoader().getResource("ParserTest.html").toURI());
      Document doc = Jsoup.parse(file, null, baseUri);
      return doc;
   }
}
