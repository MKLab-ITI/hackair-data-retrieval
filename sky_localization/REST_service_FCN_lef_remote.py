from __future__ import print_function
from bottle import post, run, request
from urllib2 import HTTPError

import json
import os
import time
import inferFCN as infer


# -- The following variables should be correctly initialized before starting the service! --
# paths to the model's file and prototxt
modelFile = '/home/hackair/FCN/fcn-16s-sift-flow.caffemodel'
protoTxt = '/home/hackair/FCN/fcn-16s-sift-flow-deploy.prototxt'
# All sky localization requests contain relative image paths which are appended to the following
# root path. The path may not be local to the server but a remote URL instead!
imagesRootDir = 'https://services.hackair.eu:8083/images/'
imagesRootDir = '/home/hackair/data/images/served_online/' # local path
# URL subpath for the service
route = '/SkyLocalizationFCN/post'
# list of IPs that the service will respond to
authorised_ips = ['160.40.51.77', '160.40.50.236', '160.40.51.145', '160.40.50.230', '173.212.212.242', '127.0.0.1','localhost'];

# this global variable holds the deep neural network in memory
net = None

import socket


@post(route)
def index():
    # get the IP of the caller
    client_ip = request.environ.get('REMOTE_ADDR')

    # return HTTP error 404 if caller's IP is not authorized
    if client_ip not in authorised_ips:
        return HTTPError(404, 'You are not authorised to access this service!')

    # To disable warning related to SSL certificates
    #urllib3.disable_warnings()

    startall_time = time.time()

    print("Reading images from " + imagesRootDir)

    # load the network if not previously loaded
    global net
    if not net:
        net = infer.loadNetwork(protoTxt, modelFile)

    # images is an array of python dictionaries
    images = request.json["images"]
    #print(images)

    # check if images exist and handle appropriately
    loading_time = 0; localization_time = 0; encoding_time = 0; pngmask_time = 0;
    for image in images:
        image['mask'] = ""
        # transform relative path to full path
        fullpath = imagesRootDir + image['path']
        print("Image: " + fullpath)

        img = None
        
        # Check whether file exists (either locally 
        if fullpath.startswith('http'):
            existsFlag = infer.exists(fullpath)
        else:
            existsFlag = os.path.isfile(fullpath)


        if existsFlag:
            #image['found'] = True

            try:
                # append a mask entry to the dictionary
                #endcodedMaskString = infer.getSkyMask(fullpath, net)
                #image['mask'] = endcodedMaskString
                start_time = time.time()

                if fullpath.startswith('http'):
                    img = loadImage(fullpath)
                else:
                    img = infer.loadImageRemote(fullpath)

                this_time = time.time() - start_time
                loading_time += this_time
                print("Loading time: %s seconds " % this_time)

                start_time = time.time()
                out = infer.runImageThroughNetwork(img, net) # this takes about 0.15s per image
                this_time = time.time() - start_time
                localization_time += this_time
                print("Localization time: %s seconds " % this_time)

                start_time = time.time()
                image['mask'] = infer.encodeMaskToString(out)
                this_time = time.time() - start_time
                encoding_time += this_time
                print("Mask encoding time: %s seconds " % this_time)

                #start_time = time.time()  # this takes about 0.03s per image
                #infer.writeImageTxtMaskSky(out, fullpath, imagesDir + 'masks');
                #this_time = time.time() - start_time
                #pngmask_time += this_time
                #print("Png mask writing time: %s seconds " % this_time)

            except IndexError:
                image['sl_error'] = "image is grayscale"
            except BaseException as e:
                image['sl_error'] = str(e)
        else:
            print("Not found!")
            image['sl_error'] = "image not found"
            #image['found'] = False

    # append times to the images object
    request.json['loading_time'] = loading_time
    request.json['localization_time'] = localization_time
    request.json['encoding_time'] = encoding_time
    #request.json['pngmask_time'] = pngmask_time
    request.json['total_time'] = time.time() - startall_time

    # return a json representation of the images array
    return json.dumps(request.json, separators=(',',':'))

run(host='0.0.0.0', port=8084)
