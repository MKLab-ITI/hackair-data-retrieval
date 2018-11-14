# hackair-data-retrieval
Contains components for air quality data collection, image collection from Flickr and web cams, and image analysis for sky detection and localization.

## Data Collectors

### Air quality data collector from open sources
Two sources are involved: a) OpenAQ  platform and b) Luftdaten. The measurements from both sources are stored in a MongoDB.

#### OpenAQ
<a href="https://openaq.org/" target="_blank">OpenAQ</a> is an open data platform that aggregates and shares air quality data from multiple official sources around the world. The data offered is of high quality as they come from official, usually government-level organizations. The platform offers the data without performing any kind of transformations. 

The OpenAQ system checks each data source for updates information every 10 minutes. In most cases, the data source is the European Environmental Agency (EEA) but additional official-level data sources are included (e.g. DEFRA in the United Kingdom).  

The */latest* endpoint (https://docs.openaq.org/#api-Latest) of the API is used, which provides the latest value of each available parameter (pollutant, i.e. NO2, PM10, PM2.5, SO2, CO, O3, BC) for every location in the system. The service receives as parameters, the pollutant and the region (which can be defined either as country name, city or by using coordinates)

#### Luftdaten
<a href="http://luftdaten.info/" target="_blank">Luftdaten</a> is another source of air quality measurements. It offers data coming from  from low-cost sensors. The Luftdaten API can be accessed by following the instruction in https://github.com/opendata-stuttgart/meta/wiki/APIs.

The data are organized by the OK Lab Stuttgart which is dedicated to the fine dust measurement through the Citizen Science project luftdaten.info. The measurements are provided by citizens that install self-built sensors on the outside their home. Then, Luftdaten.info generates a continuously updated particular matter map from the transmitted data.


### Image collection from Flickr and web cams
Two sources are involved: a) Flickr and b) Webcams-travel. The metadata from both sources are stored in a MongoDB.

#### Flickr collector
The Flickr collector retrieves the URLs and necessary metadata of images captured and recently (within the last 24 hours) around the locations of interest. This information is retrieved by periodically calling the Flickr API. The metadata of each image is stored in a MongoDB and the URLs are used to download the images and store them until image analysis for supporting air quality estimation is performed.

In order to collect images the *flickr.photos.search* endpoint was used. For determining the geographical coverage of the query the *woe_id* parameter was used. This parameter allows geographical queries based on a WOEID (Where on Earth Identifier), a 32-bit identifier that uniquely identifies spatial entities and is assigned by Flickr to all geotagged images. Furthermore, in order to retrieve only photos taken within the last 24 hours, the *min/max_date_taken* parameters are used, which operate on Flickr’s ‘taken’ date field. It should be noted that the value of this field is not always accurate as explained in Flickr API’s documentation.
 
An idiosyncrasy of the Flickr API that should be considered is that whenever the number of results for any given search query is larger than 4,000, only the pages corresponding to the first 4,000 results will contain unique images and subsequent pages will contain duplicates of the first 4,000 results. To tackle this issue, a recursive algorithm was implemented, that splits the query’s date taken interval in two or more and creates new queries that are submitted to the API. This mechanism offers robustness against data bursts.

#### Webcams collector
<a href="https://developers.webcams.travel/" target="_blank">Webcams.travel</a> is a very large outdoor webcams directory that currently contains 64,475 landscape webcams worldwide. Webcams.travel provides access to webcam data through a free API. The provided API is RESTful, i.e. the request format is REST and the responses are formatted in JSON and is available only via <a href="https://www.mashape.com/" target="_blank"> Mashape</a>. 

The collector implemented uses the *webcams.travel* API to collect data from European webcams. The endpoint exploited is the */webcams/list/* and the following modifiers are used for filtering webcams: a) *continent* for specifying the continent where the web cams are located; b) *orderby* for enforcing an explicit ordering of the returned webcams in order to ensure as possible that the same webcams are returned every time; and c) *limit* for slicing the list of webcams given that the maximum number of results that can be returned per query is 50. 

### Data Collectors parameters
All the aforementioned collectors have two json file as input; the *mongosettings.json* and the *crawlsettings.json*.
The first is common for all collectors and is used for defining the MongoDB parameters while the second slightly differs among the collectors and is used for defining the crawl settings.

Below, we specify all parameters of both files and provide 2 indicative examples.

The parameters and what they represent in *mongosettings.json*.

Parameter | Explanation
:--- | :---
`username` | MongoDB username *string* value
`password` | MongoDB password *string* value
`host` | *string* with the IP of the computer or the *localhost* value
`port` | *integer* value with the MongoDB port
`authMechanism` | *string* value indicating the authentication mechanism, i.e. *MONGODB-CR*, *SCRAM-SHA-1* or *""* if none is used
`databaseName` | *string* value of the db name
`collectionName` | *string* value of the collection name

Example of *mongosettings.json*
```
{  
   "mongo_settings":[  
      {  
         "username":"XXXXXX",
         "password":"xxxxx",
         "host":"",
         "port":27017,
         "authMechanism":"SCRAM-SHA-1",
         "databaseName":"test",
         "collectionName":"sensors"
      }
   ]
}
```

The parameters and what they represent in *crawlsettings.json*.

Parameter | Explanation
:--- | :---
`mongoSettingsFile` | *string* value indicating the name of the json file with the MongoDB settings
`crawlStartString` | *string* value setting the crawl start date with the following format "dd-MM-yyyy HH:mm:ss"
`crawlEndString` | *string* value setting the crawl end date with the following format 
`crawlIntervalSecs` | *integer* value indating the interval in seconds between two crawling procedures. The procedure does not end.
`verbose` | *boolean* value indicating whether the output is written

Example of *crawlsettings.json*
```
{
	"crawl_settings": [
		{
		"mongoSettingsFile": "mongosettings.json",
		"crawlStartString": "",
		"crawlEndString": "",
		"crawlIntervalSecs": 10800,
		"verbose": true
		}
	]
}
```


### Requirements - Dependencies
The **Data Collectors** are implemented in [Java EE 7](https://docs.oracle.com/javaee/7/index.html). Additional dependencies are listed below:
* [com.javadocmd » simplelatlng]: Simple Java implementation of common latitude and longitude calculations.
* [org.jsoup » jsoup]: Java library for working with real-world HTML. It provides a very convenient API for extracting and manipulating data, using the best of DOM, CSS, and jquery-like methods. 
* [com.google.code.gson » gson]: Java library for serializing and deserializing Java objects to (and from) JSON.
* [org.mongodb » mongo-java-driver]: The MongoDB Java Driver uber-artifact, containing the legacy driver, the mongodb-driver, mongodb-driver-core, and bson
* [junit » junit]: Unit is a unit testing framework for Java, created by Erich Gamma and Kent Beck.
* [org.json » json]: It is a light-weight, language independent, data interchange format. The files in this package implement JSON encoders/decoders in Java.
* [org.apache.commons » commons-collections4]: It contains types that extend and augment the Java Collections Framework.
* [org.jongo » jongo]: Query in Java as in Mongo shell
* [org.apache.httpcomponents » httpclient]: Apache HttpComponents Client


### Instructions
1. Install Java RE 7+ and Mongo 3.x in your computer.
2. Clone the project **hackAIRDataCollectors** locally in your computer.
3. Edit the mongosetting.json and crawlsetting.json files.
4. Run the main functions for each collector (i.e. Flickr, Web cams, OpenAQ and Luftdaten respectively)
   - hackAIRDataCollectors/src/main/java/gr/iti/mklab/flickr/FlickrCollector.java
   - hackAIRDataCollectors/src/main/java/gr/iti/mklab/webcams/travel/WebcamsTravelCollectionJob.java
   - hackAIRDataCollectors/src/main/java/gr/iti/mklab/openaq/OpenAQCollector.java 
   - hackAIRDataCollectors/src/main/java/gr/iti/mklab/luftdaten/LuftDatenCollector.java 
5. Compile jar files and create a jar file for each collector.
6. Run the jar files with a crawl settings file as command line argument. 
e.g.
> java -jar FlickrCollector.jar "crawlsettings.json" > log.txt 2>&1




## Image analysis for sky detection and localization
Image Analysis (IA) involves all the operations required for the extraction of Red/Green (R/G) and Green/Blue (G/B) ratios from sky-depicting images. IA accepts a HTTP post request, carries out image processing, and returns a JSON with the results of the analysis. The service accepts as input either a set of local paths of images already downloaded (by image collectors Flickr or webcams) or a set of image URLs. 

The IA service consists of 3 components:
 - concept detection
 - sky localization
 - ratio computation

### Concept detection
A 22-layer GoogLeNet network on 5055 concepts, which are a subset of the 12,988 ImageNet concepts. Then, this network is applied on the TRECVID SIN 2013 development dataset and the output of the last fully-connected layer (5055 dimensions) is used as the input space of SVM classifiers trained on the 346 TRECVID SIN concepts. The Concept Detection (CD) considered only the sky concept.

The CD component returns a score that represent the algorithm’s confidence that the sky concept appears in each image. The threshold considered for deciding whether an image depicts sky or not is set to 0.8 because the goal is to lower the probability of sending non-sky-depicting images for further analysis.


### Sky Localization
Sky Localization (SL) refers to the detection of all pixels that depict sky in an image. We employ a fully convolutional network (FCN) approach, which draws on recent successes of deep neural networks for image classification and transfer learning.

The SL component is a computationally heavy processing step that can is suggested to be carried out on a GPU for improving the time performance of the module.

### Ratio Computation
The Ration Computation module considers heuristic rules that aim at refining the sky part of the images. The algorithm uses certain criteria involving the pixel color values and the size of color clusters in order to refine the sky mask. The output of the algorithm is a mask containing all pixels that capture the sky and the mean R/G and G/B ratios of the sky part of the images. It should be noted that the heuristic algorithm is rather strict and does not consider clouds as part of the sky. 

When an IA request is received, the IA service first sends a request to the concept detection (CD) component (step 1) which implements the concept detection framework. The CD component applies concept detection on each image of the request and returns a set of scores that represent the algorithm’s confidence that the sky concept appears in each image. When a response is received by the CD component, the IA service parses it to check which images are the most likely to depict sky based on the confidence scores calculated by the CD component. Then, the IA service receives the response from the SL component (step 4) and sends a request to the ratio computation (RC) component. The RC component takes the sky masks computed by the FCN approach as input, refines them by applying the heuristic
approach on top of them and computes the R/G and G/B ratios of each image (in case all checks of
the heuristic approach are successfully passed). Finally, the IA service parses the response of the RC component (step
6) and combines the results of all processing steps to synthesize the IA response.


nohup python TF_detection_service.py > detection_log.txt 2>&1




# hackAIR Decision Support API

The involved web-services were created with the adoption of state-of-the-art technologies: RESTful communication, exchange of information on the basis of JSON objects, etc. The hackAIR DS API is publicly available and may run both as an independent service or as an integrated service on the hackAIR app/platform. 


## Web-Services
Up to now, the hackAIR DS API offers the following web services through POST requests:
* _{BASE_URL}/hackAIR_project/api/dynamicPopulation_: performs the dynamic population of involved data (user profile and enviromnental data) in the hackAIR KB for further manipulation.
* _{BASE_URL}/hackAIR_project/api/requestRecommendation_: performs a step-by-step process, i.e. (i) receives a JSON object in pre-defined format, through a POST request to the service of discourse, (ii) converts the JSON data to a hackAIR-compatible ontology-based problem description language for populating new instances (user profile details and environmental related data) in the knowledge base; (iii) triggers the hackAIR reasoning mechanism for handling the available data and rules and for inferencing new knowledge, i.e. provide relevant recommendations to the users. 


### JSON parameters
Below, we specify all the mandatory and optional JSON parameters that are accepted in the POST request:



### Example JSON object

```

```


## Requirements - Dependencies

## Instructions
1. Install Python Java EE 7 and GlassFish 4.1.1 in your computer.
2. Clone the project locally in your computer.
3. Run Tomcat server and deploy [hackAIR_project.war](hackAIR_project/target) application.
4. Submit POST requests in relevant web-services, as described [here](https://github.com/MKLab-ITI/hackair-decision-support-api#web-services)


## Citation
E. Spyromitros-Xioufs, A. Moumtzidou, S. Papadopoulos, S. Vrochidis, Y. Kompatsiaris, A. K. Georgoulias, G. Alexandri, K. Kourtidis, “Towards improved air quality monitoring using publicly available sky images”, In Multimedia Technologies for Environmental & Biodiversity Informatics, 2018. 


## Contact
For further details, please contact Anastasia Moumtzidou (moumtzid@iti.gr)


## Credits
The hackAIR Decision Support API was created by <a href="http://mklab.iti.gr/" target="_blank">MKLab group</a> under the scope of <a href="http://www.hackair.eu/" target="_blank">hackAIR</a> EU Horizon 2020 Project.



