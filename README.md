# hackair-data-retrieval
Contains components for air quality data collection, image collection from Flickr and web cams, and image analysis for sky detection and localization.

It also contains datasets with measurements from the collectors mentioned below (see **data** folder).

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
Image Analysis (IA) service coordinates all the operations required for the extraction of Red/Green (R/G) and Green/Blue (G/B) ratios from sky-depicting images. It accepts a HTTP post request, carries out image processing, and returns a JSON with the results of the analysis. The service accepts as input either a set of local paths of images already downloaded (by image collectors Flickr or webcams) or a set of image URLs. 

The pipeline of the IA service is the following: 
 - IA receives an HTTP post request
 - IA sends a request to the concept detection (CD) service
 - IA receives the CD response that indicates the images that most likely depict sky
 - IA sends a request to the sky localization (SL) service
 - IA receives the SL response that provides a mask with the sky part of the image
 - IA calls the ratio computation (RC) component that computes the R/G and G/B ratios of the image
 - IA combines the results of all previous steps to synthesize its response

The IA service consists of 3 components:
 - concept detection
 - sky localization
 - ratio computation

#### Requirements - Dependencies
The **Image Analysis Service** is implemented in [Java EE 7](https://docs.oracle.com/javaee/7/index.html). Additional dependencies are listed below:
* [asm » asm]: ASM, a very small and fast Java bytecode manipulation framework.
* [com.sun.jersey » jersey-bundle]: A bundle containing code of all jar-based modules that provide JAX-RS and Jersey-related features. 
* [org.json » json]: It is a light-weight, language independent, data interchange format. It implements JSON encoders/decoders.
* [com.fasterxml.jackson.core » jackson-core]: Core Jackson processing abstractions implementation for JSON.
* [com.fasterxml.jackson.core » jackson-databind]: General data-binding functionality for Jackson that works on core streaming API.
* [javax.servlet » servlet-api]: Java Servlet API.
The IA service uses internally the CD service and the SL service.

#### Instructions
1. Install Java RE 7+, Mongo 3.x and Tomcat 8.x in your computer.
2. Clone the project **ImageAnalysisService** locally in your computer.
3. Deploy a war file with main class *ImageAnalysisService.java*
4. Compile jar files and create a jar file for each collector.
5. Edit the *ia_settings.xml* and the *mongosettings.xml* that should reside inside the WEB-INF directory. Example settings files are under the main directory. Below, we specify all parameters of both files and provide 2 indicative examples.

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

The parameters and what they represent in *ia_settings.json*.

Parameter | Explanation
:--- | :---
`skyDetectionVersion` | *string* value that should remain intact
`skyThreshold` | *float* value indicating the threshold of concept detection
`usableSkyThreshold` | *float* value indicating the threshold of ratio computation for sky detection
`imagesRoot` | *string* path pointing to the directory where the images are located
`imagesDownload` | *string* path pointing to the folder where the images to be downloaded are located
`detectionEndpoint` | *string* URL of the Concept Detection service
`localizationEndpoint` | *boostringlean* URL of the Sky Localization service
`processUrls` | *boolean* value indicating whether URLs will be processes
`outputMasks` | *boolean* value indicating whether masks will be kept
`SSLValidationOff` | *boolean* value indicating whether SSL validation is off

Example of *ia_settings.json*
```
{  
   "ia_settings":[  
      {  
      	 "skyDetectionVersion":"new",
         "skyThreshold":0.5,
         "usableSkyThreshold":0.3,
         
         "imagesRoot":"C:/data/images/online/",
         "imagesDownload":"download/",
         
         "detectionEndpoint":"_{BASE_URL}/ConceptDetection/post",
         "localizationEndpoint":"_{BASE_URL}/SkyLocalizationFCN/post",
         
         "processUrls":true,
         "outputMasks":true,
         "SSLValidationOff":true
      }
   ]
}
```

6. Service endpoint (post): _{BASE_URL}/ImageAnalysisService-v1/post
7. Sample body of POST call: 
Example of *ia_settings.json*
```
{  
   "images":[  
      {"path":"flickr/2018-02-04/00000.jpg"},
      {"path":"flickr/2018-02-04/11111.jpg"}
   ]
}
```

### Concept detection
A 22-layer GoogLeNet network on 5055 concepts, which are a subset of the 12,988 ImageNet concepts. Then, this network is applied on the TRECVID SIN 2013 development dataset and the output of the last fully-connected layer (5055 dimensions) is used as the input space of SVM classifiers trained on the 346 TRECVID SIN concepts. The Concept Detection (CD) considered only the sky concept.

The CD component returns a score that represent the algorithm’s confidence that the sky concept appears in each image. The threshold considered for deciding whether an image depicts sky or not is set to 0.8 because the goal is to lower the probability of sending non-sky-depicting images for further analysis.


#### Requirements - Dependencies
The **Concept detection Service** is implemented in python. Additional dependencies are listed below:
* [requests]: Requests packages allow to send HTTP/1.1 requests.
* [numpy]: Fundamental package for scientific computing with Python. 
* [json]: It exposes an API familiar to users of the standard library marshal and pickle modules. .
* [urllib2]: Used for fetching URLs.
* [bottle]: Bottle is a fast, simple and lightweight WSGI micro web-framework.

#### Instructions
1. Install python 2.x, and tensorflow-gpy in your computer. For tensorflow-gpu installation instructions see <a href="https://www.tensorflow.org/install/pip" target="_blank">here</a>. It is recommended to create a virtual environment.
2. Activate a tensorflow environment (if aplicable). Depends on the installation method (e.g. “source activate tensorflow”)
3. Clone the folder **sky_detection** locally in your computer.
   - Main class: 'sky_detection/TF_detection_service.py'
   - Model files: 'sky_detection/best models'
4. Adjust paths at the beginning of *TF_detection_service.py* (models_path, imagesDir)
5. Run service for Ubuntu:
> nohup python TF_detection_service.py > detection_log.txt 2>&1

This command redirects stdout and stderr to a log file and allows closing the terminal and leaving the process running.
6. Service endpoint (post): _{BASE_URL}:8083/ConceptDetection/post
7. Sample body of POST call: 
Example of *ia_settings.json*
```
{  
   "images":[  
      {"path":"flickr/2018-02-04/00000.jpg"},
      {"path":"flickr/2018-02-04/11111.jpg"}
   ]
}
```

### Sky Localization
Sky Localization (SL) refers to the detection of all pixels that depict sky in an image. We employ a fully convolutional network (FCN) approach, which draws on recent successes of deep neural networks for image classification and transfer learning.

The SL component is a computationally heavy processing step that can is suggested to be carried out on a GPU for improving the time performance of the module.

#### Requirements - Dependencies
The **Sky Localization Service** is implemented in python. Additional dependencies are listed below:
* [json]: It exposes an API familiar to users of the standard library marshal and pickle modules. .
* [urllib2]: Used for fetching URLs.
* [bottle]: Bottle is a fast, simple and lightweight WSGI micro web-framework.


#### Instructions
1. Install python 2.x, and caffe in your computer. 
2. Clone the folder **sky_localization** locally in your computer.
   - Main class: 'sky_localization/REST_service_FCN_lef_remote.py'
3. Caffe installation steps:
   1. Install latest available caffe version according to official <a href = "http://caffe.berkeleyvision.org/install_apt.html" target="_blank"> instructions  </a>, i.e.:
      - sudo apt-get install libprotobuf-dev libleveldb-dev libsnappy-dev libopencv-dev libhdf5-serial-dev protobuf-compiler
      - sudo apt-get install --no-install-recommends libboost-all-dev
      - sudo apt-get install libatlas-base-dev (** "sudo apt-get install libopenblas-dev" is also required for caffe_future!)
      - install python via <a href="https://www.anaconda.com/download/#linux" target="_blank">Anaconda</a> as suggested in the <a href="https://docs.anaconda.com/anaconda/install/linux" target="_blank">instructions</a> 
   2. Compile caffe according to the instructions found <a href="http://caffe.berkeleyvision.org/installation.html#compilation" target="_blank">here</a>. Make nessesary changes in makefile for anaconda python (** it is probably good to call make clean first!!!)
      cp Makefile.config.example Makefile.config
      
      make all
      
      solve hdf5 problem by trying <a href="https://gist.github.com/wangruohui/679b05fcd1466bb0937f#fix-hdf5-naming-problem" target="_blank">this</a>
      
      Append /usr/include/hdf5/serial/ to INCLUDE_DIRS at line 85 in Makefile.config. (** This worked for both the older and the latest version of caffe! Yey!)
		--- INCLUDE_DIRS := $(PYTHON_INCLUDE) /usr/local/include
		+++ INCLUDE_DIRS := $(PYTHON_INCLUDE) /usr/local/include /usr/include/hdf5/serial/
		--- LIBRARY_DIRS := $(PYTHON_LIB) /usr/local/lib /usr/lib
		+++ LIBRARY_DIRS := $(PYTHON_LIB) /usr/local/lib /usr/lib /usr/lib/x86_64-linux-gnu/hdf5/serial

      - --> make test
      - --> make runtest
      - It is necessary to set cuda related environmental variables as described <a href="http://docs.nvidia.com/cuda/cuda-installation-guide-linux/#environment-setup" target="_blank">here</a>.
   3. Download and unzip caffe-master.zip as the siftflow model file and prototxt from github repository
      - Repeat step 2
   4. make pycaffe 
      - Before executing this ensure that all the anaconda-related lines in the config file are uncommented. And also execute this line in caffe/python: "for req in $(cat requirements.txt); do pip install $req; done" 
   
      - The following environmental variables should be defined as well:
            export LD_LIBRARY_PATH=/usr/local/cuda-8.0/lib64:$LD_LIBRARY_PATH
	    export PATH=/usr/local/cuda-8.0/bin:$PATH
	

4. Adjust paths at the beginning of *REST_service_FCN_lef_remote.py* (modelFile, protoTxt, imagesRootDir) and in the auxiliary file *inferFCN.py* (caffe_root_python)
5. Run service for Ubuntu:
> nohup python REST_service_FCN_lef_remote.py > fcn_log.txt 2>&1

This command redirects stdout and stderr to a log file and allows closing the terminal and leaving the process running.
6. Service endpoint (post): _{BASE_URL}:8084/SkyLocalizationFCN/post
7. Sample body of POST call: 
Example of *ia_settings.json*
```
{  
   "images":[  
      {"path":"flickr/2018-02-04/00000.jpg"},
      {"path":"flickr/2018-02-04/11111.jpg"}
   ]
}
```

### Ratio Computation
The Ration Computation module considers heuristic rules that aim at refining the sky part of the images. The algorithm uses certain criteria involving the pixel color values and the size of color clusters in order to refine the sky mask. The output of the algorithm is a mask containing all pixels that capture the sky and the mean R/G and G/B ratios of the sky part of the images. It should be noted that the heuristic algorithm is rather strict and does not consider clouds as part of the sky. 


#### Requirements - Dependencies
The **Data Collectors** are implemented in [Java EE 7](https://docs.oracle.com/javaee/7/index.html). Additional dependencies are listed below:
* [asm » asm]: ASM, a very small and fast Java bytecode manipulation framework.
* [com.sun.jersey » jersey-bundle]: A bundle containing code of all jar-based modules that provide JAX-RS and Jersey-related features. 
* [org.json » json]: It is a light-weight, language independent, data interchange format. The files in this package implement JSON encoders/decoders in Java.
* [com.fasterxml.jackson.core » jackson-core]: Core Jackson processing abstractions (aka Streaming API), implementation for JSON.
* [com.fasterxml.jackson.core » jackson-databind]: General data-binding functionality for Jackson: works on core streaming API.
* [javax.servlet » servlet-api]: Java Servlet API.


#### Instructions
1. Install Java RE 7+, Mongo 3.x and Tomcat 8.x in your computer.
2. Clone the project **ImageAnalysisService** locally in your computer.
3. Run the main functions for each collector (i.e. Flickr, Web cams, OpenAQ and Luftdaten respectively)
5. Compile jar files and create a jar file for each collector.
6. Run the jar files with a crawl settings file as command line argument. 
e.g.
> java -jar FlickrCollector.jar "crawlsettings.json" > log.txt 2>&1

This service uses internally the above two services (2.1 and 2.2).
Details
●	Source code: https://bitbucket.org/lefman/hackair/src/master/ImageAnalysisService/ 
●	Main class: ImageAnalysisService.java 
●	Compiled war location: [hackair_root]/modules/services/IA_service
●	Currently running at REM (C:\Program Files\Apache Software Foundation\Tomcat 8.5\webapps\ImageAnalysisService-v1)
●	Endpoint (post): https://services.hackair.eu:8083/ImageAnalysisService-v1/post
●	Sample call: {"images":[{"path":"flickr/2018-02-04/25210632307.jpg"}]}
How to set up
●	Tomcat should be installed and running on the server  
●	The war file should then be deployed
●	After the war is deployed, the ia_settings.xml and the mongosettings.xml inside the WEB-INF directory should be edited. Example settings files are given in the compiled war location See the code for more details on the meaning of each settings parameter.


## Citation
E. Spyromitros-Xioufs, A. Moumtzidou, S. Papadopoulos, S. Vrochidis, Y. Kompatsiaris, A. K. Georgoulias, G. Alexandri, K. Kourtidis, “Towards improved air quality monitoring using publicly available sky images”, In Multimedia Technologies for Environmental & Biodiversity Informatics, 2018. 


## Contact
For further details, please contact Anastasia Moumtzidou (moumtzid@iti.gr)


## Credits
The hackAIR Decision Support API was created by <a href="http://mklab.iti.gr/" target="_blank">MKLab group</a> under the scope of <a href="http://www.hackair.eu/" target="_blank">hackAIR</a> EU Horizon 2020 Project.

![mklab logo](https://mklab.iti.gr/img/cube.svg) Information Technologies Institute - Centre for Research and Technology Hellas &nbsp; &nbsp; &nbsp; <img src="./images/hackAir_logo_RGB.png" alt="hackAIR logo" width="125" height="133">
