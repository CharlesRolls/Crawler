package com.rolls.crawler.test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.rolls.crawler.parse.LinkDetails;
import com.rolls.crawler.parse.PageDetails;
import com.rolls.crawler.parse.Parser;

/**
 * Test parser that simulates a set of web pages on a site.
 */
public class TestParser implements Parser
{
   /**
    * Base URL of the site (HTTP).
    */
   public static final String baseUrl = "http://www.notrealsite.org";

   /**
    * HTTPS version of the base URL.
    */
   public static final String baseUrlSecure = "https://www.notrealsite.org";

   private final int parseDurationMillis;
   private final Map<String, PageDetails> pages;

   private int parseInterrupts = 0;

   /**
    * Constructor.
    * @param parseDurationMillis How long the parse method will take.
    */
   public TestParser(int parseDurationMillis)
   {
      this.parseDurationMillis = parseDurationMillis;
      this.pages = initPageDetails();
   }

   /**
    * Initializes the pages of the site.
    * @return Map of url to page details.
    */
   private static Map<String, PageDetails> initPageDetails()
   {
      Map<String, PageDetails> pageMap = new HashMap<>();
      pageMap.put(baseUrl, getHomePage());
      pageMap.put(baseUrl + "/login.html", getLoginPage());
      pageMap.put(baseUrl + "/about.html", getAboutPage());
      pageMap.put(baseUrl + "/admin/one.html", getAdminOnePage());
      pageMap.put(baseUrl + "/admin/two.html", getAdminTwoPage());
      pageMap.put(baseUrl + "/admin/three.html", getAdminThreePage());
      return pageMap;
   }

   /**
    * Creates the home page.
    * @return Home page details.
    */
   private static PageDetails getHomePage()
   {
      Set<LinkDetails> imports = new LinkedHashSet<>();
      imports.add(new LinkDetails("link", "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"));

      Set<LinkDetails> media = new LinkedHashSet<>();
      media.add(new LinkDetails("img", baseUrl + "/img/home.jpg"));
      media.add(new LinkDetails("script", baseUrl + "/js/home.js"));

      Set<LinkDetails> links = new LinkedHashSet<>();
      links.add(new LinkDetails("a", baseUrl + "/login.html"));
      links.add(new LinkDetails("a", baseUrl + "/about.html"));
      links.add(new LinkDetails("a", "http://www.google.com"));

      PageDetails details = new PageDetails();
      details.setTitle("Home");
      details.setImports(imports);
      details.setMedia(media);
      details.setLinks(links);
      return details;
   }

   /**
    * Creates the login page.
    * @return Login page details.
    */
   private static PageDetails getLoginPage()
   {
      Set<LinkDetails> imports = new LinkedHashSet<>();
      imports.add(new LinkDetails("link", "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"));

      Set<LinkDetails> media = new LinkedHashSet<>();
      media.add(new LinkDetails("img", baseUrl + "/img/login.jpg"));
      media.add(new LinkDetails("script", baseUrl + "/js/login.js"));

      Set<LinkDetails> links = new LinkedHashSet<>();
      links.add(new LinkDetails("a", baseUrl));
      links.add(new LinkDetails("a", baseUrl + "/admin/one.html"));
      links.add(new LinkDetails("a", "http://www.microsoft.com/somepage.html"));

      PageDetails details = new PageDetails();
      details.setTitle("Login");
      details.setImports(imports);
      details.setMedia(media);
      details.setLinks(links);
      return details;
   }

   /**
    * Creates the about page.
    * @return About page details.
    */
   private static PageDetails getAboutPage()
   {
      Set<LinkDetails> imports = new LinkedHashSet<>();
      imports.add(new LinkDetails("link", "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"));

      Set<LinkDetails> media = new LinkedHashSet<>();
      media.add(new LinkDetails("img", baseUrl + "/img/about.jpg"));
      media.add(new LinkDetails("script", baseUrl + "/js/about.js"));

      Set<LinkDetails> links = new LinkedHashSet<>();
      links.add(new LinkDetails("a", baseUrl + "/"));

      PageDetails details = new PageDetails();
      details.setTitle("About");
      details.setImports(imports);
      details.setMedia(media);
      details.setLinks(links);
      return details;
   }

   /**
    * Creates the first admin page.
    * @return First admin page details.
    */
   private static PageDetails getAdminOnePage()
   {
      Set<LinkDetails> imports = new LinkedHashSet<>();
      imports.add(new LinkDetails("link", "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"));

      Set<LinkDetails> media = new LinkedHashSet<>();
      media.add(new LinkDetails("img", baseUrl + "/img/admin/one.jpg"));
      media.add(new LinkDetails("script", baseUrl + "/js/admin/one.js"));

      Set<LinkDetails> links = new LinkedHashSet<>();
      links.add(new LinkDetails("a", baseUrl + "/"));
      links.add(new LinkDetails("a", baseUrl + "/about.html"));
      links.add(new LinkDetails("a", baseUrl + "/admin/two.html"));
      links.add(new LinkDetails("a", baseUrl + "/admin/three.html"));
      links.add(new LinkDetails("a", baseUrlSecure + "/admin/four.html"));

      PageDetails details = new PageDetails();
      details.setTitle("Admin One");
      details.setImports(imports);
      details.setMedia(media);
      details.setLinks(links);
      return details;
   }

   /**
    * Creates the second admin page.
    * @return Second admin page details.
    */
   private static PageDetails getAdminTwoPage()
   {
      return new PageDetails();
   }

   /**
    * Creates the third admin page.
    * @return Third admin page details.
    */
   private static PageDetails getAdminThreePage()
   {
      PageDetails details = new PageDetails();
      details.setLoadError("Unable to load page.");
      return details;
   }

   @Override
   public PageDetails parse(String url)
   {
      // NOTE: Uncomment this to show timing of page parsing to understand the progress calls test case.
//      System.out.println(String.format("%s: %d", url, System.currentTimeMillis()));

      try
      {
         Thread.sleep(parseDurationMillis);
      }
      catch (InterruptedException e)
      {
         ++parseInterrupts;
      }

      PageDetails details = pages.get(url);
      if (details == null)
      {
         details = new PageDetails();
         details.setLoadError(String.format("No details for %s", url));
      }

      return details;
   }

   /**
    * Returns the number of times the parse method was interrupted.
    * @return Number of interrupts.
    */
   public int getParseInterrupts()
   {
      return parseInterrupts;
   }
}