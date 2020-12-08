package com.rolls.crawler.test;

import org.apache.commons.lang3.StringUtils;

/**
 * Timer.
 */
public class TestTimer
{
   private long startTime = 0;
   private long durationMillis = 0;

   /**
    * Constructor.
    * @return Test timer.
    */
   public static TestTimer startNewTimer()
   {
      TestTimer timer = new TestTimer();
      timer.start();
      return timer;
   }

   /**
    * Gets the start time.
    * @return The startTime.
    */
   public long getStartTime()
   {
      return startTime;
   }

   /**
    * Gets the duration calculated at the last endTimer call.
    * @return The duration in milliseconds.
    */
   public long getDurationMillis()
   {
      return durationMillis;
   }

   /**
    * Resets the the start time to now and clears the duration.
    */
   public void start()
   {
      startTime = System.currentTimeMillis();
      durationMillis = 0;
   }

   /**
    * Calculates the duration since the last start call.
    * @return The calculated duration.
    */
   public long endTimer()
   {
      return endTimer(null, false);
   }

   /**
    * Calculates the duration since the last start call and prints a duration message to SysOut.
    * @param details Details to append to duration message.
    * @return The calculated duration.
    */
   public long endTimer(String details)
   {
      if (details == null)
         details = "";

      return endTimer(details, false);
   }

   /**
    * Calculates the duration since the last start call and prints a duration message to SysOut.
    * @param details Details to append to duration message.
    * @param showSeconds Prints duration message in seconds (true) or milliseconds (false).
    * @return The calculated duration.
    */
   public long endTimer(String details, boolean showSeconds)
   {
      durationMillis = System.currentTimeMillis() - startTime;

      if (details != null)
      {
         StringBuilder strBld = new StringBuilder();

         if (showSeconds)
            strBld.append((double)durationMillis / (double)1000).append(" seconds");
         else
            strBld.append(durationMillis).append(" milliseconds");

         if (!StringUtils.isEmpty(details))
            strBld.append(": ").append(details);

         System.out.println(strBld.toString());
      }

      return durationMillis;
   }
}
