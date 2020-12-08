# Crawler
Crawler is a simple Java based CLI application that scans a single domain for web pages
and saves a report about the internal and external links for those pages to a file.

## Building

 * ```clone project form git```
 * ```cd <project location>```
 * ```mvn clean package spring-boot:repackage```

This will build a Spring Boot Executable JAR.  An already built version is in the release folder along with
a bat file to run it and an application.yml file that hold the configuration properties for the execution.

## Testing

 * ```cd <project location>```
 * ```mvn test```

## Running

 * ```cd <project location>/release```
 * ```Update application.yml with the desired setting```
 * ```Run Crawl.bat or java -jar crawler-<version>.jar``'  (Assumes java location in your path)

## Notes
1. The domain is the base URL of the initially scanned page.  It will NOT cross protocol boundaries.
For instance, if initial URL is http://www.company.com, https://www.company.com/* will be an external link.
To make this production ready, more could be done to determine if a page is part of the same domain.<br/><br/>

2. The Jsoup library that does the page loading and parsing for HTML tags does a very good job at returning
absolute paths.  I have done basic testing in this area but more test cases could be added to prove this
functionality truly works as desired.  Also, the same library handles redirects but more testing in this
area could be done as well.<br/><br/>

3. The application is packaged into a single Spring Boot Executable JAR for the purposes of its intended use
as a sample application.  The internal class implementations and packaging follows normal separation of
code principles allowing for easy repackaging into individual artifacts.<br/><br/>

4. There are timing tests that could fail depending on the site that is used and network speeds.
The limits set in the tests are very strict about expected durations.  Duration limits may need
to be adjusted accordingly.

## Interesting Points
1. The threading and synchronization in the CrawlImpl class is of note.  It uses minimal synchronization
to handle multi-threading and has a nice design of encapsulation.<br/><br/>

2. Almost all code has been unit tested.  The CLIApplication is the only class that does not have a unit
test because it is a minimal wrapper class to call the main functionality in the service.  This could be
tested as well, but it is not worth the time for this sample.  Code coverage is around 95% for each package.
The only things that have NOT been covered are some Lombok internals and some almost unreachable code.

