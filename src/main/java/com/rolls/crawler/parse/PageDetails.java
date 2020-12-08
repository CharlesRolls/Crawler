package com.rolls.crawler.parse;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Information about a web pages content.
 */
@Getter
@Setter
public class PageDetails
{
   private String loadError;
   private String title;
   private Set<LinkDetails> imports;
   private Set<LinkDetails> media;
   private Set<LinkDetails> links;
}
