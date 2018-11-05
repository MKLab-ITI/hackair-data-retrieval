# Project Title

The dataset contains geotagged Images in Europe retrieved from two sources; Flickr and webcams. Regarding Flickr images they are user generated images that are publicly available on Flickr and geo-tagged in Europe, while webcam images are extracted from static outdoor webcams located in Europe. It should be noted that webcams are geo-tagged and, consequently the images retrieved are also geo-tagged and time stamped.
The images are collected and analyzed by specialized computer software that detects images with a sky region appropriate for air quality estimation and extracts pixel color statistics (i.e. mean R/G, G/B ratios) from that region. The computed statistics are then given as input to the air quality estimation model, developed within the project, which produces the Aerosol Optical Depth (AOD).
The images are downloaded, downscaled to a maximum size of 500X500 pixels and stored until image analysis is performed (<1 hour). After this process, the images are permanently deleted from our servers. 
The dataset is created by images retrieved from Flickr and webcams starting from June 1018. The number of records from Flickr is 58,752 while the one from webcams is 79,993.

## Dataset Description
The metadata that will be provided for each image are the following:
•	source
•	URL (of the image on Flickr or the webcam)
•	R/G and G/B ratios of sky part of the image
•	Geo-coordinates
•	Timestamp
•	AOD
For the images retrieved from Flickr, the original images can be retrieved from the corresponding URLs. However, for the webcam images the original images can only be retrieved for the webcams for the ones that provide historical data. Thus, in case such historical information the webcams URLs links to the webcam itself and not the image taken at the specific date-time.  


## Format
The dataset is available in text-based machine-readable format (csv) that allow easy parsing and information exchange.

## License

This project is licensed under the Apache License v2.0 - see the [LICENSE.md](https://www.apache.org/licenses/LICENSE-2.0) file for details

## Acknowledgments

E. Spyromitros-Xioufs, A. Moumtzidou, S. Papadopoulos, S. Vrochidis, Y. Kompatsiaris, A. K. Georgoulias, G. Alexandri, K. Kourtidis, “Towards improved air quality monitoring using publicly available sky images”, In Multimedia Technologies for Environmental & Biodiversity Informatics, 2018.

