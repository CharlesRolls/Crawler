package com.rolls.crawler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import java.io.File;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SuppressWarnings("javadoc")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {WebCrawler.class, CrawlerProperties.class})
public class WebCrawlerTest
{
   @Autowired
   private CrawlerProperties appProps;

   @Autowired
   WebCrawler webCrawler;

   @Test
   public void testAppRun() throws Exception
   {
      File file = Paths.get(appProps.getOutputPath(), appProps.getResultFile()).toFile();
      if (file.exists())
         file.delete();

      webCrawler.run();
      assertThat(file.length(), greaterThan(10000L));
   }
}
