How does the crawler work

1. The user specifies a file that contains the queries. See input/queries.csv for an example. Several types of geographical (Flickr) queries are supported. Currently we use the woeid query type that just needs the woeid of the location of interest (currently the whole Europe).
2. The user also specifies a file that contains the mongo db settings. See input/mongosetting.json for an example. This file the details for connecting to mongo db and the specific databased where the crawl results will be written.
3. Finally, the user specifies a file that contains the setting of the crawl. See input/crawlsetting.json for and example.


How date and time settings work

-In the crawl settings file, the user can specify when he wants the crawl to start/end. The format is like this: "31-12-2010 24:00:00" and the CET time zone is assumed. If the user leaves the crawl start/end string empty, then the crawl starts immediately and end in 365 days from now.
-If the crawl should not start now, the main thread sleeps for the required amount of time and then enters a while loop that keeps running until the time when the next crawl should start becomes larger than the time when the crawl should end.
-The crawl is repeated every crawlIntervalSecs, provided that the crawlEndTime has not been reached.
-Every time the crawl is repeated, it search for photos with max_taken_date equal to now (approximately) and min_taken_date equal to now-crawlTimeSpanSecs.
-In both cases, a unix time stamp is used to denote the date. This means that we do not have to care about time zones when posing the query.
-The only "problem" is that according to "https://www.flickr.com/services/api/misc.dates.html", we cannot be sure about the time zone of a taken date in Flickr. This means that we may request a photo that is taken now but the date taken string can be larger than the date taken string in our current timezone. 

How to perform a past crawl
1. Set the crawlstartstring to the last date that is valid for your crawl.
2. Set the crawlendstring to ""
3. Set the crawktimespan to then number of days before crawlstartstring that are valid for your crawl. 