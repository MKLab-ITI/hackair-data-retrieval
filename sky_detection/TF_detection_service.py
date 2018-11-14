
from bottle import post, run, request, HTTPError

import json
import time
import tensorflow as tf
from urllib2 import urlopen, URLError, HTTPError
import numpy as np
import sys
import requests
import os

# define a global variable to hold the graph
sky_graph = None
sky_index = None
sky_input_height = None
sky_input_width = None
sky_input_mean = None
sky_input_std = None
sky_input_layer = None

usable_graph = None
usable_index = None
usable_input_height = None
usable_input_width = None
usable_input_mean = None
usable_input_std = None
usable_input_layer = None

# constants inception
input_height_inception = 299
input_width_inception = 299
input_mean_inception = 128
input_std_inception = 128
input_layer_inception = 'Mul'
# constants mobilenet 224!
input_height_mobilenet = 224
input_width_mobilenet = 224
input_mean_mobilenet = 127.5
input_std_mobilenet = 127.5
input_layer_mobilenet = 'input'
# constants both
output_layer = "final_result"


# the local or remote! location where the images are stored
imagesDir = 'https://host:port/images/' # production
#imagesDir = '/home/hackair/images/' # testing

# path to the stored graph file and labels for sky detection
models_path = '/home/xxx/sky_detection/best models/'
sky_graphPath = models_path + 'sky_sky_hackair-new_mobilenet_1.0_224_LR0.001_graph.pb'
sky_labelsPath = models_path + 'sky_sky_hackair-new_labels.txt'
sky_architecture = 'mobilenet_1.0_224'

# path to the stored graph file and labels for usable sky detection
usable_graphPath = models_path + 'usable_usable_sky_mobilenet_1.0_224_LR0.001_graph.pb'
usable_labelsPath = models_path + 'usable_usable_sky_labels.txt'
usable_architecture = 'mobilenet_1.0_224'

# IPs allowed to call the service
authorised_ips = ['XXX.XXX.XXX.XXX', 'XXX.XXX.XXX.XXX'];


@post('/ConceptDetection/post')
def index():
    client_ip = request.environ.get('REMOTE_ADDR')


    #if client_ip in authorised_ips:
    if True:

        startall_time = time.time()


        # load the network and its details the first time
        global sky_graph
        global sky_index
        global sky_input_height
        global sky_input_width
        global sky_input_mean
        global sky_input_std
        global sky_input_layer
        
        global usable_graph
        global usable_index
        global usable_input_height
        global usable_input_width
        global usable_input_mean
        global usable_input_std
        global usable_input_layer
        
        if not sky_graph:
            # Assume that you have 12GB of GPU memory and want to allocate ~4GB:
            start_time = time.time()
            sky_graph = load_graph(sky_graphPath)
            sky_labels = load_labels(sky_labelsPath)
            sky_index = sky_labels.index("sky")
            sky_graph_loading_time = time.time() - start_time
            print("Sky graph loading time: %f sec " % sky_graph_loading_time)
            #print(sky_labels)
            #print("sky index: " + str(sky_index))
            
            start_time = time.time()
            usable_graph = load_graph(usable_graphPath)
            usable_labels = load_labels(usable_labelsPath)
            usable_index = usable_labels.index("usable")
            usable_graph_loading_time = time.time() - start_time
            print("Usable graph loading time: %f sec " % usable_graph_loading_time)
            #print(usable_labels)
            #print("usable index: " + str(usable_index))


            parts = sky_architecture.split('_')
            if parts[0] == 'inception':
                sky_input_height = input_height_inception
                sky_input_width = input_width_inception
                sky_input_mean = input_mean_inception
                sky_input_std = input_std_inception
                sky_input_layer = input_layer_inception
            elif parts[0] == 'mobilenet':
                sky_input_height = input_height_mobilenet
                sky_input_width = input_width_mobilenet
                sky_input_mean = input_mean_mobilenet
                sky_input_std = input_std_mobilenet
                sky_input_layer = input_layer_mobilenet
            else:
                raise ValueError('Unknown architecture', sky_architecture)
            
            parts = usable_architecture.split('_')
            if parts[0] == 'inception':
                usable_input_height = input_height_inception
                usable_input_width = input_width_inception
                usable_input_mean = input_mean_inception
                usable_input_std = input_std_inception
                usable_input_layer = input_layer_inception
            elif parts[0] == 'mobilenet':
                usable_input_height = input_height_mobilenet
                usable_input_width = input_width_mobilenet
                usable_input_mean = input_mean_mobilenet
                usable_input_std = input_std_mobilenet
                usable_input_layer = input_layer_mobilenet
            else:
                raise ValueError('Unknown architecture', usable_architecture)

        #sets upper limit to allocated GPU memory, not sure if needed
        gpu_options = tf.GPUOptions(per_process_gpu_memory_fraction=0.25)

        sky_sess = tf.Session(graph=sky_graph, config=tf.ConfigProto(gpu_options=gpu_options))
        usable_sess = tf.Session(graph=usable_graph, config=tf.ConfigProto(gpu_options=gpu_options))

        sky_input_operation = sky_graph.get_operation_by_name("import/" + sky_input_layer)
        sky_output_operation = sky_graph.get_operation_by_name("import/" + output_layer)
        
        usable_input_operation = usable_graph.get_operation_by_name("import/" + usable_input_layer)
        usable_output_operation = usable_graph.get_operation_by_name("import/" + output_layer)


        print("Reading images from " + imagesDir)
        # images is an array of python dictionaries
        images = request.json["images"]
        #print(request.json)
        #print(images)

        loading_time_total = 0; sky_inference_time_total = 0; usable_inference_time_total = 0
        for image in images:
            try:
                start_time = time.time()

                #print("Original: " + image['path'])
                final_path = image['path']
                # if a relative path, attach root image dir
                if not final_path.startswith('http'):
                    final_path = imagesDir + final_path
                #print("Final: " + final_path)


                # in any case, if final path is a remote one download and store the image in a temporary local file
                # and update the final path so that it always points to a local file (needed by TF!!!)
                if final_path.startswith('http'):
                    extension = final_path[final_path.rfind('.'):] # get the file extension
                    #print("extension: " + extension)
                    resp = urlopen(final_path)

                    # AFTER THE URL HAS BEEN OPENED!
		    #cwd = os.getcwd()
		    #print(cwd)
                    final_path = models_path + ".temp_imagefile" + extension # update the final path

                    with open(final_path, "wb") as local_file:
                        local_file.write(resp.read())




                loading_time_this = time.time() - start_time
                loading_time_total += loading_time_this
                print("Loading time: %f sec " % loading_time_this)

                start_time = time.time()
                sky_t = read_tensor_from_image_file(final_path,
                                                input_height=sky_input_height,
                                                input_width=sky_input_width,
                                                input_mean=sky_input_mean,
                                                input_std=sky_input_std)
                results = sky_sess.run(sky_output_operation.outputs[0], {sky_input_operation.outputs[0]: sky_t})
                results = np.squeeze(results)
                # print(results)
                image['sky_conf'] = results[sky_index].item()
                sky_inference_time_this = time.time() - start_time
                print("Sky inference time: %f sec " % sky_inference_time_this)
                sky_inference_time_total += sky_inference_time_this

                if image['sky_conf'] > 0.5:
                    start_time = time.time()
                    usable_t = read_tensor_from_image_file(final_path,
                                                           input_height=usable_input_height,
                                                           input_width=usable_input_width,
                                                           input_mean=usable_input_mean,
                                                           input_std=usable_input_std)
                    results = usable_sess.run(usable_output_operation.outputs[0], {usable_input_operation.outputs[0]: usable_t})
                    results = np.squeeze(results)
                    # print(results)
                    image['usable_conf'] = results[usable_index].item()
                    usable_inference_time_this = time.time() - start_time
                    print("Usable inference time: %f sec " % usable_inference_time_this)
                    usable_inference_time_total += usable_inference_time_this

            except BaseException as e:
                print(e)
                image['cd_error'] = str(e)


        # append times to the images object
        request.json['loading_time'] = loading_time_total
        request.json['sky_inference_time'] = sky_inference_time_total
        request.json['usable_inference_time'] = usable_inference_time_total
        request.json['total_time'] = time.time() - startall_time

        sys.stdout.flush()

        # return a json representation of the images array
        return json.dumps(request.json, separators=(',',':'))
        #return json.dumps(request.json, indent = 4, separators = (',', ': '))
    else:
        return HTTPError(404,'You are not authorised to access this service')

def load_graph(model_file):
  graph = tf.Graph()
  graph_def = tf.GraphDef()

  with open(model_file, "rb") as f:
    graph_def.ParseFromString(f.read())
  with graph.as_default():
    tf.import_graph_def(graph_def)

  return graph

def load_labels(label_file):
  label = []
  proto_as_ascii_lines = tf.gfile.GFile(label_file).readlines()
  for l in proto_as_ascii_lines:
    label.append(l.rstrip())
  return label

def read_tensor_from_image_file(file_name, input_height=299, input_width=299,
				input_mean=0, input_std=255):
    with tf.Graph().as_default():
      input_name = "file_reader"
      output_name = "normalized"
      file_reader = tf.read_file(file_name, input_name)
      if file_name.endswith(".png"):
        image_reader = tf.image.decode_png(file_reader, channels = 3,
                                           name='png_reader')
      elif file_name.endswith(".gif"):
        image_reader = tf.squeeze(tf.image.decode_gif(file_reader,
                                                      name='gif_reader'))
      elif file_name.endswith(".bmp"):
        image_reader = tf.image.decode_bmp(file_reader, name='bmp_reader')
      else:
        image_reader = tf.image.decode_jpeg(file_reader, channels = 3,
                                            name='jpeg_reader')
      float_caster = tf.cast(image_reader, tf.float32)
      dims_expander = tf.expand_dims(float_caster, 0);
      resized = tf.image.resize_bilinear(dims_expander, [input_height, input_width])
      normalized = tf.divide(tf.subtract(resized, [input_mean]), [input_std])
      sess = tf.Session()
      result = sess.run(normalized)
      return result

def exists(path):
    r = requests.head(path)
    return r.status_code == requests.codes.ok

def dlfile(url):
    # Open the url
    try:
        f = urlopen(url)
        print "downloading " + url

        # Open our local file for writing
        with open(os.path.basename(url), "wb") as local_file:
            local_file.write(f.read())

    #handle errors
    except HTTPError, e:
        print "HTTP Error:", e.code, url
    except URLError, e:
        print "URL Error:", e.reason, url

run(host='0.0.0.0', port=8083)
