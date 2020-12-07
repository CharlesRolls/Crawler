package com.rolls.crawler.parser;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Information about a link.
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class LinkDetails
{
   private String tag;
   private String url;
}
