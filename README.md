# hackair-data-retrieval
Contains components for air quality data collection, image collection from Flickr and web cams, and image analysis for sky detection and localization.

## Air quality data collector from open sources
Two sources are involved: a) OpenAQ  platform and b) Luftdaten. The measurements from both sources are stored in a MongoDB.


### OpenAQ

#### Description
<a href="https://openaq.org/" target="_blank">OpenAQ</a> is an open data platform that aggregates and shares air quality data from multiple official sources around the world. The data offered by the platform is of high quality as they mainly come from official, usually government-level organizations. The platform offers the data as they are received from their originating sources, without performing any kind of transformations. 

The OpenAQ system checks each data source for updates information every 10 minutes. In most cases, the data source is the European Environmental Agency (EEA) but additional official-level data sources are included (e.g. DEFRA in the United Kingdom).  

The */latest* endpoint (https://docs.openaq.org/#api-Latest) of the API is used, which provides the latest value of each available parameter (pollutant, i.e. NO2, PM10, PM2.5, SO2, CO, O3, BC) for every location in the system. The service receives as parameters, the pollutant and the region (which can be defined either as country name, city or by using coordinates)

#### Instructions
1. Install Java EE 7 and GlassFish 4.1.1 in your computer.
2. Clone the project locally in your computer.
3. Run Glassfish server and deploy [hackAIR_project.war](hackAIR_project/target) application.
4. Submit POST requests in relevant web-services, as described [here](https://github.com/MKLab-ITI/hackair-decision-support-api#web-services)

### Luftdaten

#### Description
<a href="http://luftdaten.info/" target="_blank">Luftdaten</a> is another source of air quality measurements. It offers data coming from  from low-cost sensors. The Luftdaten API can be accessed by following the instruction in https://github.com/opendata-stuttgart/meta/wiki/APIs.

The data are organized by the OK Lab Stuttgart which is dedicated to the fine dust measurement through the Citizen Science project luftdaten.info. The measurements are provided by citizens that install self-built sensors on the outside their home. Then, Luftdaten.info generates a continuously updated particular matter map from the transmitted data.

#### Instructions
1. Install Java EE 7 and GlassFish 4.1.1 in your computer.
2. Clone the project locally in your computer.
3. Run Glassfish server and deploy [hackAIR_project.war](hackAIR_project/target) application.
4. Submit POST requests in relevant web-services, as described [here](https://github.com/MKLab-ITI/hackair-decision-support-api#web-services)

## Image collection from Flickr and web cams
Two sources are involved: a) Flickr and b) Webcams-travel. The metadata from both sources are stored in a MongoDB.

### Flickr collector

#### Description
The Flickr collector retrieves the URLs and necessary metadata of images captured and recently (within the last 24 hours) around the locations of interest. This information is retrieved by periodically calling the Flickr API. The metadata of each image is stored in a MongoDB and the URLs are used to download the images and store them until image analysis for supporting air quality estimation is performed.

In order to collect images the flickr.photos.search endpoint was used. For determining the geographical coverage of the query the *woe_id* parameter was used. This parameter allows geographical queries based on a WOEID (Where on Earth Identifier), a 32-bit identifier that uniquely identifies spatial entities and is assigned by Flickr to all geotagged images. Furthermore, in order to retrieve only photos taken within the last 24 hours, the *min/max_date_taken* parameters of the *flickr.photos.search* endpoint are used. These parameters operate on Flickr’s ‘taken’ date field which is extracted, if available, from the image’s Exif metadata. However, the value of this field is not always accurate as explained in Flickr API’s documentation.
 
An idiosyncrasy of the Flickr API that should be considered is that whenever the number of results for any given search query is larger than 4,000, only the pages corresponding to the first 4,000 results will contain unique images and subsequent pages will contain duplicates of the first 4,000 results. To tackle this issue, a recursive algorithm was implemented, that splits the query’s date taken interval in two or more and creates new queries that are submitted to the API. This mechanism offers robustness against data bursts.

### Webcams collector

#### Description
<a href="https://developers.webcams.travel/" target="_blank">Webcams.travel</a> is a very large outdoor webcams directory that currently contains 64,475 landscape webcams worldwide. Webcams.travel provides access to webcam data through a comprehensive and well-documented free API. The provided API is RESTful, i.e. the request format is REST and the responses are formatted in JSON and is available only via <a href="https://www.mashape.com/" target="_blank"> Mashape</a>. The collector implemented uses the webcams.travel API to collect data from European webcams. The endpoint exploited  is the */webcams/list/* and apart from the continent modifier that narrows down the complete list of webcams to contain only webcams from specific continent, two other modifiers are used: a) *orderby* and b) *limit*. The orderby modifier has the purpose of enforcing an explicit ordering of the returned webcams in order to ensure as possible that the same webcams. The limit modifier is used to slice the list of webcams by limit and offset given that the maximum number of results that can be returned with a single query is 50. 




## Image analysis for sky detection and localization
Image Analysis (IA) involves all the operations required for the extraction of Red/Green (R/G) and Green/Blue (G/B) ratios from sky-depicting images. IA accepts a HTTP post request, carries out image processing, and returns a JSON with the results of the analysis. The service accepts as input either a set of local paths of images already downloaded (by image collectors Flickr or webcams) or a set of image URLs. 

The IA service consists of 3 components:
 - concept detection
 - sky localization
 - ratio computation

### Concept detection
In the employed framework, we train a 22-layer GoogLeNet [41] network on 5055
concepts, which are a subset of the 12,988 ImageNet concepts. Then, this network
is applied on the TRECVID SIN 2013 development dataset and the output of the
last fully-connected layer (5055 dimensions) is used as the input space of SVM
classiers trained on the 346 TRECVID SIN concepts. Among these classiers, we
use the one trained on the sky concept.
In order to evaluate the accuracy of the employed sky detection framework,
we manually annotated (for the sky concept) 23,000 Instagram images (collected
during preliminary past data collection activities) that were captured in the city of
Berlin during the time period between 01/01/2016 to 15/04/2016. Sky detection
was then applied on each image and the generated condence scores were recorded
in order to facilitate the selection of a decision threshold that provides a good
trade-o between precision and recall. Based on this analysis, we opted for a 0.6
threshold (i.e. the sky concept is considered present if the condence score is 0.6)
which led to 91.2% precision and 80.0% recall.



Sky localization is an important computer vision problem which refers to the
detection of all pixels that depict sky in an image. In this section, we rst present
the state of the art in sky localization (section 5.2.1) and then describe the adopted
sky localization approach which consists of the fusion of two diverse approaches,
a deep learning-based one (section 5.2.2) and one based on a set of heuristic rules
(section 5.2.3), that were found to work in a complementary manner (section 5.2.4).

5.2.2 FCN for sky localization
In the proposed framework, we employ the fully convolutional network (FCN) ap-
proach [22], which draws on recent successes of deep neural networks for image
classication (e.g. [19]) and transfer learning. Transfer learning was rst demon-
strated on various visual recognition tasks (e.g. [7]), then on detection, and on
both instance and semantic segmentation in hybrid proposal classier models [11{
13]. The work in [22] was the rst to adapt deep classication architectures for
image segmentation by using networks pre-trained for image classication and
ne-tuned fully convolutionally on whole image inputs and per pixel ground truth
labels. Importantly, it was shown [22] that the FCN approach achieves state-of-
the-art segmentation performance in a number of standard benchmarks, including
the SIFT Flow dataset where the FCN-16 variant achieved a pixel precision of
94.3% on the set of geometric classes, which include sky.
To measure the performance of the approach specically on the task of sky
localization, we used the SUN Database19 [43], a comprehensive collection of an-
notated images covering a large variety of environmental scenes, places and the ob-
jects within. More specically, we used the pre-trained (on the SIFT Flow dataset)
FCN-16 model made available20 by [22], to predict the sky region of the 2,030 SUN
images for which the polygons capturing the sky part are provided. We measured
a pixel precision of 91.77% and a pixel recall of 94.25%. It should be noted, that
we are interested mainly in the precision of the approach given that what is re-
quired by the air quality estimation approach presented in section 6 is recognizing
accurately even a small part of the sky inside the image.

5.2.3 Sky localization using heuristic rules
The second approach for sky localization is based on heuristic rules that aim at
recognizing the sky part of the images. The algorithm is based on identifying
whether the pixels meet certain criteria involving their color values and the size
of color clusters they belong to. The output of the algorithm is a mask containing
all pixels that capture the sky. Fig. 4 presents the pseudocode of the proposed
method. It should be noted that the heuristic algorithm is far stricter than the
FCN-based since sun and clouds are not considered part of the sky. Similarly to the
FCN-based, the heuristic rule-based method was evaluated on the SUN database
obtaining a mean precision of 82.45% and a mean recall of 59.22%.

When an IA request is received, the IA service
first sends a request to the concept detection (CD) component (step 1) which implements the concept detection
framework described in D3.1. The CD component applies concept detection on each image of the request and returns
a set of scores that represent the algorithm’s confidence that the sky concept appears in each image. When a response
is received by the CD component, the IA service parses it to check which images are the most likely to depict sky based
on the confidence scores calculated by the CD component (step 2). A relatively high (0.8) threshold is used to lower
the probability of sending non-sky-depicting images for subsequent analysis. At step 3, the IA service sends a request
to the sky localization (SL) component which implements the FCN-based sky localization framework described in D3.1.
This is a computationally heavy processing step that is carried out on the GPU of the IA server. The response of the SL
component is the sky mask of each image of the request. To minimize the time required for sending the masks to the
IA service, a compression algorithm is first applied to reduce the size of the masks. Then, the IA service receives the
response from the SL component (step 4) and sends a request to the ratio computation (RC) component. The
RC component takes the sky masks computed by the FCN approach as input, refines them by applying the heuristic
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

Parameter | JSON Type | Mandatory(M) / Optional(O) | Accepted values
:--- | :---: | :---: | :---
`username` | object | M | any *string* value
`gender` | object | O | One of the following: *male*, *female*, *other*
`age` | object | M | any *integer* value
`locationCity` | object | M | any *string* value
`locationCountry` | object | M | any *string* value
`isPregnant` | object | O | any *boolean* value
`isSensitiveTo` | array | O | One or more of the following: *Asthma*, *Allergy*, *Cardiovascular*, *GeneralHealthProblem*
`isOutdoorJobUser` | object | O | any *boolean* value
`preferredActivities` | object | O | `preferredOutdoorActivities`
`preferredOutdoorActivities` | array | O | One or more of the following: *picnic*, *running*, *walking*, *outdoor job*, *biking*, *playing in park*, *general activity*
`airPollutant` | object | M | Both: `airPollutantName`, `airPollutantValue`
`airPollutantName` | object | M | One of the following: *PM_AOD*, *PM10*, *PM2_5*, *PM_fused*
`airPollutantValue` | object | M | any *double* value
`preferredLanguageCode` | object | O | One of the following: *en*, *de*, *no*
`relatedProfiles` | array | O | One or more JSON objects, each of which includes the aforementioned mandatory/optional fields.


### Example JSON object

#### With primary and secondary profile description, in one single request

```
{
  "username": "Helen_Hall",
  "age":"32", 
  "locationCity": "Berlin",
  "locationCountry": "Germany",
  "isPregnant": false,
  "isSensitiveTo": ["Asthma"],
  "preferredLanguageCode": "de",
  "airPollutant": {
    "airPollutantName": "PM_fused",
    "airPollutantValue": "3.5",
  }
  "preferredActivities": {
    "preferredOutdoorActivities": ["picnic","running"]
  },
  "relatedProfiles": [{
    "username": "Helen_Hall_secondary_profile",
    "gender":"female",
    "age":"1", 
    "locationCity": "Berlin",
    "locationCountry": "Germany",
    "preferredLanguageCode": "de",
    "airPollutant": {
      "airPollutantName": "PM_fused",
      "airPollutantValue": "3.5"
    }
  }]
}
```


## Requirements - Dependencies
The hackAIR DS API is implemented in [Java EE 7](https://docs.oracle.com/javaee/7/index.html) with the adoption of [JAX-RS](http://docs.oracle.com/javaee/6/api/javax/ws/rs/package-summary.html) library. Additional dependencies are listed below:
* [Apache Jena](https://jena.apache.org/): a free and open-source Java framework for building Semantic Web and Linked Data applications.
* [SPIN API](http://topbraid.org/spin/api/): an open source Java API to enable the adoption of SPIN rules and the handling of the implemented rule-based reasoning mechanism. 
* [GlassFish Server 4.1.1](http://www.oracle.com/technetwork/middleware/glassfish/overview/index.html): an open-source application server for the Java EE platform, utilised for handling HTTP queries to the RESTful API.
* [json-simple](https://github.com/fangyidong/json-simple): a well-known java toolkit for parsing (encoding/decoding) JSON text.
* [hackAIR Knowledge Base (KB) and Reasoning Framework](https://mklab.iti.gr/results/hackair-ontologies/): this regards the implemented ontological representation of the domain of discourse that handles both the semantic integration and reasoning of environmental and user-specific data, in order to provide recommendations to the hackAIR users, with respect to: (i) personal health and user preferences (activities, daily routine, etc.), and (ii) current AQ conditions of the location of interest. The hackAIR DS module utilises the sources of the hackAIR KB and reasoning framework as a background resource of information, from which it acquires the necessary semantic relations and information in order to support relevant recommendations’ provision to the users upon request for decision support. 


## Instructions
1. Install Java EE 7 and GlassFish 4.1.1 in your computer.
2. Clone the project locally in your computer.
3. Run Glassfish server and deploy [hackAIR_project.war](hackAIR_project/target) application.
4. Submit POST requests in relevant web-services, as described [here](https://github.com/MKLab-ITI/hackair-decision-support-api#web-services)


## Citation
Riga M., Kontopoulos E., Karatzas K., Vrochidis S. and Kompatsiaris I. (2018), An Ontology-based Decision Support Framework for Personalised Quality of Life Recommendations. In: Dargam F., Delias P., Linden I., Mareschal B. (eds) Decision Support Systems VIII: Sustainable Data-Driven and Evidence-Based Decision Support. 4th International Conference on Decision Support System Technology (ICDSST 2018). Lecture Notes in Business Information Processing (LNBIP), Volume 313, Springer, Cham. doi: [https://doi.org/10.1007/978-3-319-90315-6_4](https://doi.org/10.1007/978-3-319-90315-6_4).


## Contact
For further details, please contact Marina Riga (mriga@iti.gr)


## Credits
The hackAIR Decision Support API was created by <a href="http://mklab.iti.gr/" target="_blank">MKLab group</a> under the scope of <a href="http://www.hackair.eu/" target="_blank">hackAIR</a> EU Horizon 2020 Project.



