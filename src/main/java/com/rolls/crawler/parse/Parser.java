package com.rolls.crawler.parse;

/**
 * API to web page parser functionality.
 */
public interface Parser
{
   /**
    * Loads a web page and parses it for links.
    * @param url URL to the page.
    * @return Page details.  Never null.
    */
   public PageDetails parse(String url);
}
