package com.rolls.crawler.parse;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Implementation of web page parser.  Reads imports (HTML link tag), links (HTML a tag),
 * and media (HTML tags that contain src attribute).
 */
public class ParserImpl implements Parser
{
   private final int timeoutMillis;

   /**
    * Constructs a new parser.
    * @param timeoutMillis Timeout for parsing.  Value <= 0 means no timeout.
    */
   public ParserImpl(int timeoutMillis)
   {
      this.timeoutMillis = (timeoutMillis >= 0 ? timeoutMillis: 0);
   }

   @Override
   public PageDetails parse(String url)
   {
      PageDetails details = new PageDetails();

      Document doc;
      try
      {
         doc = Jsoup.connect(url).followRedirects(true).timeout(timeoutMillis).get();
      }
      catch (Exception ex)
      {
         details.setLoadError(String.format("Unable to load %s.  CAUSE: %s", url, ex.toString()));
         return details;
      }

      details.setTitle(doc.title());
      details.setImports(processLinks(doc.select("link[href]"), "abs:href"));
      details.setMedia(processLinks(doc.select("[src]"), "abs:src"));
      details.setLinks(processLinks(doc.select("a[href]"), "abs:href"));

      return details;
   }

   /**
    * Gets link details for a set of elements.
    * @param elements Elements containing links.
    * @param attrName Element attribute that contains the link.
    * @return Set of links.
    */
   private Set<LinkDetails> processLinks(Elements elements, String attrName)
   {
      Set<LinkDetails> links = new LinkedHashSet<>(elements.size());
      for (Element element : elements)
         links.add(new LinkDetails(element.tagName(), element.attr(attrName)));
      return links;
   }
}
