# Copyright 2017 The TensorFlow Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import argparse
import csv
import time

import os
import numpy as np
import tensorflow as tf
from sklearn import metrics
import matplotlib.pyplot as plt

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

def load_graph(model_file):
  graph = tf.Graph()
  graph_def = tf.GraphDef()

  with open(model_file, "rb") as f:
    graph_def.ParseFromString(f.read())
  with graph.as_default():
    tf.import_graph_def(graph_def)

  return graph

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


def read_tensors_from_image_files(file_names, input_height=299, input_width=299, input_mean=0, input_std=255):
   with tf.Graph().as_default():
    input_name = "file_reader"
    output_name = "normalized"
    file_name_placeholder = tf.placeholder(tf.string, shape=[])
    file_reader = tf.read_file(file_name_placeholder, input_name)
    image_reader = tf.image.decode_jpeg(file_reader, channels = 3,
        name='jpeg_reader')
    float_caster = tf.cast(image_reader, tf.float32)
    dims_expander = tf.expand_dims(float_caster, 0)
    resized = tf.image.resize_bilinear(dims_expander, [input_height, input_width])
    normalized = tf.divide(tf.subtract(resized, [input_mean]), [input_std])

    with tf.Session() as sess:
      for file_name in file_names:
        yield sess.run(normalized, {file_name_placeholder: file_name})

def load_labels(label_file):
  label = []
  proto_as_ascii_lines = tf.gfile.GFile(label_file).readlines()
  for l in proto_as_ascii_lines:
    label.append(l.rstrip())
  return label

def evaluate_model(graph_file_path,label_file_path,pos_label,test_dataset_path,architecture):
    start_total = time.time()
    parts = architecture.split('_')
    if parts[0] == 'inception':
        input_height = 299
        input_width = 299
        input_mean = 128
        input_std = 128
        input_layer = "Mul"
    elif parts[0] == 'mobilenet':
        size_string = parts[2]
        input_height = int(size_string)
        input_width = int(size_string)
        input_mean = 127.5
        input_std = 127.5
        input_layer = "input"
    else:
        raise ValueError('Unknown architecture', architecture)

    # fixed params
    output_layer = "final_result"
    thresholds = np.arange(0.1, 1.0, 0.1)

    graph = load_graph(graph_file_path)
    labels = load_labels(label_file_path)
    #determine sky label index!
    pos_index_model = labels.index(pos_label)
    print(labels)
    print("pos index: " + str(pos_index_model))
    gt_file_name = test_dataset_path + "gt.csv"

    sess = tf.Session(graph=graph)

    input_name = "import/" + input_layer
    output_name = "import/" + output_layer
    input_operation = graph.get_operation_by_name(input_name)
    output_operation = graph.get_operation_by_name(output_name)

    counter = 0  # simple line counter in gt file
    y_true = []  # the gt values
    y_score = []  # the predicted scores
    with open(gt_file_name, 'r') as csvfile:
        gt_reader = csv.reader(csvfile, delimiter=',')
        for row in gt_reader:
            if row[1] == '1' or row[1] == '0':
                y_true.append(float(row[1]))
            else:
                raise ValueError(row[1] + ' not expected in annotation file (line:' + str(counter + 1) + ')!')
            # print('Image:' + row[0] + ' - class: ' + image_class)

            start_critical = time.time()
            t = read_tensor_from_image_file(test_dataset_path + row[0].replace("\\", "/"),
                                            input_height=input_height,
                                            input_width=input_width,
                                            input_mean=input_mean,
                                            input_std=input_std)

            end_critical = time.time()
            time_taken_critical = end_critical - start_critical
            results = sess.run(output_operation.outputs[0], {input_operation.outputs[0]: t})
            results = np.squeeze(results)
            # print(results)
            y_score.append(results[pos_index_model])
            counter += 1
    # convert to np arrays needed for metrics calculation
    y_true = np.array(y_true)
    y_score = np.array(y_score)

    time_per_image = (time.time() - start_total) / len(y_true)
    auc = metrics.roc_auc_score(y_true, y_score)

    performance_metrics = []
    print("== Results with different thresholds ==")
    for thres in np.nditer(thresholds):
        y_pred = [] # the predicted values based on threshold
        for score in np.nditer(y_score):
            if score >= thres:
                y_pred.append(1)
            else:
                y_pred.append(0)

        y_pred = np.array(y_pred)

        acc = metrics.accuracy_score(y_true, y_pred)
        prec = metrics.precision_score(y_true, y_pred)
        rec = metrics.recall_score(y_true, y_pred)
        print("Threshold: %.1f -> ACC: %.3f Prec: %.3f Rec: %.3f AUC: %.3f Time: %.3f" % (thres, acc, prec, rec, auc,time_per_image))

        performance_metrics.append([thres, acc, prec,rec, auc, time_per_image])

    return performance_metrics

    # PR curve computation and visualization
    # precision, recall, thresholds = metrics.precision_recall_curve(y_true, y_score)
    #print('Precision: ' + str(precision))
    #print('Recall: ' + str(recall))
    #print('Thresholds: ' + str(thresholds))
    # for x in np.arange(0.0, 1.0, 0.1):
    #     for i in range(0,len(thresholds)):
    #         if abs(thresholds[i]-x)< 0.01:
    #             print('Threshold: ' +str(thresholds[i]) +' Precision: ' + str(precision[i]) + ' Recall: ' + str(recall[i]))
    #             break
    # plt.step(recall, precision, color='b', alpha=0.2,
    #          where='post')
    # plt.fill_between(recall, precision, step='post', alpha=0.2,
    #                  color='b')
    #
    # plt.xlabel('Recall')
    # plt.ylabel('Precision')
    # plt.ylim([0.0, 1.05])
    # plt.xlim([0.0, 1.0])
    # plt.title('2-class Precision-Recall curve: AP={0:0.2f}'.format(ap))
    # plt.savefig(test_dataset_path+'PR_curve.pdf', bbox_inches='tight')


if __name__ == "__main__":
  # test set selection!
  #images_folder = "/home/espyromi/Public/nuswide/small test dataset/"
  #gt_file_name = "/home/espyromi/Public/nuswide/small test dataset/gt.txt"
  train_dataset_name = 'hackair-new_combined'
  eval_dataset_name = 'nuswide'
  models_dir = "/home/espyromi/Desktop/tensorflow-for-poets-2/tf_files/retrained/"
  images_folder = "/home/espyromi/Public/sky_photos_"+eval_dataset_name + "/"
  gt_file_name = images_folder + "gt.csv"
  predictions_file_name = images_folder + train_dataset_name + '_preds.txt'
  num_preds = 1000

  # model selection!
  model_file = models_dir + "/graphs/sky_" + train_dataset_name + "_graph.pb"
  label_file = models_dir + "/labels/sky_" + train_dataset_name + "_labels.txt"

  sky_input_height = 224
  sky_input_width = 224
  sky_input_mean = 128
  sky_input_std = 128
  sky_input_layer = "input"
  output_layer = "final_result"

  parser = argparse.ArgumentParser()
  parser.add_argument("--gt", help="gt to be processed")
  parser.add_argument("--graph", help="graph/model to be executed")
  parser.add_argument("--labels", help="name of file containing labels")
  parser.add_argument("--input_height", type=int, help="input height")
  parser.add_argument("--input_width", type=int, help="input width")
  parser.add_argument("--input_mean", type=int, help="input mean")
  parser.add_argument("--input_std", type=int, help="input std")
  parser.add_argument("--input_layer", help="name of input layer")
  parser.add_argument("--output_layer", help="name of output layer")
  args = parser.parse_args()

  if args.graph:
    model_file = args.graph
  if args.gt:
    gt_file_name = args.gt
  if args.labels:
    label_file = args.labels
  if args.input_height:
    sky_input_height = args.input_height
  if args.input_width:
    sky_input_width = args.input_width
  if args.input_mean:
    sky_input_mean = args.input_mean
  if args.input_std:
    sky_input_std = args.input_std
  if args.input_layer:
    sky_input_layer = args.input_layer
  if args.output_layer:
    output_layer = args.output_layer

  sky_graph = load_graph(model_file)
  labels = load_labels(label_file)

  sess = tf.Session(graph=sky_graph)
  # sess2 = tf.Session()

  input_name = "import/" + sky_input_layer
  output_name = "import/" + output_layer
  input_operation = sky_graph.get_operation_by_name(input_name);
  output_operation = sky_graph.get_operation_by_name(output_name);

  print(labels)
  counter = 0
  tp = 0
  tn = 0
  fp = 0
  fn = 0
  # create predictions file and write labels order in header line
  preds_file = open(predictions_file_name, 'w')
  str_labels = ','.join(labels)
  preds_file.write(str_labels + "\n")

  with open(gt_file_name, 'rb') as csvfile:
      gt_reader = csv.reader(csvfile, delimiter=',')
      for row in gt_reader:
          start_total = time.time()
          counter += 1
          image_class = '';
          if row[1] == '1':
              image_class = 'sky'
          elif row[1] == '0':
              image_class = 'nosky'
          else:
              raise ValueError(row[1] + ' not expected in annotation file (line:' + str(counter) + ')!')

          #print('Image:' + row[0] + ' - class: ' + image_class)


          start_critical = time.time()
          t = read_tensor_from_image_file(images_folder + row[0].replace("\\", "/"),
                                          input_height=sky_input_height,
                                          input_width=sky_input_width,
                                          input_mean=sky_input_mean,
                                          input_std=sky_input_std)



          # with tf.Session(graph=graph) as sess:
          #  results = sess.run(output_operation.outputs[0],
          #                {input_operation.outputs[0]: t})
          end_critical = time.time()
          time_taken_critical = end_critical - start_critical

          results = sess.run(output_operation.outputs[0],
                             {input_operation.outputs[0]: t})
          results = np.squeeze(results)



          results_string = ','.join(['%.5f' % num for num in results])

          #print(results)
          thres = 0.5

          if results[1] >= thres:
              pred = 'sky'
          else:
              pred = 'nosky'

          #top_k = results.argsort()[-5:][::-1]

          if image_class == 'sky':
              if pred == image_class:
                  tp+=1
              else:
                  fn+=1
          else:
              if pred == image_class:
                  tn+=1
              else:
                  fp+=1

          #if image_class == labels[top_k[0]]:
          #    acc += 1
          #for i in top_k:
            #print(labels[i], results[i])

          if counter % 100 == 0:
             print('Images predicted: ' + str(counter))
             print("==Metrics==")
             print('Acc: ' + format(float(tp+tn) / (tp+tn+fp+fn),'.3f')+ "\t")
             print('Prec: ' + format(float(tp) / (tp+fp),'.3f')+ "\t")
             recall = None
             if tp+fn > 0:
                recall = float(tp) / (tp+fn)
                print('Recall: ' + format(recall,'.3f')+ "\n")
             else:
                print("Recall: Undefined")
          if counter == num_preds:
             break

          end_total = time.time()
          time_taken_total = end_total - start_total

          remaining_time = time_taken_total-time_taken_critical

          preds_file.write(','.join(row))
          preds_file.write(',' + results_string + ',')
          #preds_file.write(format(time_taken_critical,'.3f') + "\t")
          preds_file.write(format(time_taken_total,'.3f') + "\n")
          #preds_file.write(format(remaining_time,'.3f') + "\n")

  preds_file.close()

  print("==Metrics==\n")
  print('Acc: ' + format(float(tp + tn) / (tp + tn + fp + fn), '.3f') + "\t")
  print('Prec: ' + format(float(tp) / (tp + fp), '.3f') + "\t")
  print('Recall: ' + format(float(tp) / (tp + fn), '.3f') + "\n")