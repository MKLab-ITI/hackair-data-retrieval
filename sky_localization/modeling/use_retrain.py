import time
from argparse import Namespace

import label_many_images
import retrain

# This script uses the retrain.py script (provided by Tensorflow) in order to adapt the full inception model or
# its mobile versions to a new task by retraining only the last layer. After the adaptation is complete, the
# label_many_images script is used to evaluate the performance of the adapted model on a completely independent test set
# and to record various performance metrics. label_many_images is adapted from label_image.py (provided by Tensorflow)
# to label many images simultaneously and to evaluate the total performance based on a ground truth file.

# The following parameters should be appropriately set before each run
tf_workspace = '/home/espyromi/sky detection/tensorflow-for-poets-2/tf_files/'
datasets_dir = '/home/espyromi/sky detection/' # path to where the alternative training sets and test set reside
# names of alternative training set folders inside datasets_dir
#train_datasets = ['sky_nuswide','sky_trecvid','sky_hackair','sky_hackair-new_train','sky_hackair-new']
train_datasets = ['usable_sky_train', 'usable_sky']
model_type = 'usable' # E.g. 'sky' or 'usable', used only for naming purposes (graphs,training_summaries,labels)
pos_label = 'usable' # name of positive the class of interest for performance measurements, e.g. 'sky' or 'usable'
# whether to evaluate best model of each training set (in terms of validation performance) on an independent test set
# this is meaningful only when multiple test sets are used and we want to compare their performance
apply_on_test = True
test_dataset = 'usable_sky_test' # used only if apply_on_test = True
#test_dataset = 'sky_hackair-new_test' # used only if apply_on_test = True
learning_rates = [0.001] # one or more learning rates to use in each training set
#learning_rates = [0.01,0.005,0.001] # one or more learning rates to use in each training set
#architectures = ['mobilenet_1.0_224', 'inception_v3'] # one or more architectures to test
architectures = ['mobilenet_1.0_224'] # one or more architectures to test
train_batch_sizes = [100] # the size of each training batch, default=100
#train_batch_sizes = [16,32,64,100] # the size of each training batch, default=100

# Various parameters related to retraining that are typically FIXED need to be set for all models
model_dir = tf_workspace + "models"
final_tensor_name = 'final_result' # name of the output classification layer in the retrained graph
how_many_training_steps = 4000 # number of training iteration, default=4000
how_many_training_steps = 1000 # number of training iteration, default=4000
# performance on test set can be ignored when using an independent test set
testing_percentage = 1 # the percentage of images reserved for test, default=10
validation_percentage_train = 24 # the percentage of images used in validation set when tuning the model, default=10
validation_percentage_test = 1 # the percentage of images used in validation set when building the final model, default=10
eval_step_interval = 100 # how frequently to update performance metrics, default=10
validation_batch_size = -1  # use all validation samples
test_batch_size = -1  # use all test samples
# transformations
flip_left_right = False
random_crop = False
random_scale = False
random_brightness = False
print_misclassified_test_images = False
intermediate_store_frequency = 0 # how frequently to store intermediate graph, default=0 (i.e. turned off)

# The following is a helper function used to set the FLAGS (various model adaptation parameters and settings)
# in retrain.py based on the values set in this script
def set_retrain_flags():
    FLAGS = Namespace()
    FLAGS.summaries_dir = summaries_dir
    FLAGS.intermediate_store_frequency = intermediate_store_frequency
    FLAGS.architecture = architecture
    FLAGS.model_dir = model_dir
    FLAGS.image_dir = image_dir
    FLAGS.testing_percentage = testing_percentage
    FLAGS.validation_percentage = validation_percentage
    FLAGS.flip_left_right = flip_left_right
    FLAGS.random_crop = random_crop
    FLAGS.random_scale = random_scale
    FLAGS.random_brightness = random_brightness
    FLAGS.bottleneck_dir = bottleneck_dir
    FLAGS.final_tensor_name = final_tensor_name
    FLAGS.learning_rate = learning_rate
    FLAGS.how_many_training_steps = how_many_training_steps
    FLAGS.train_batch_size = batch_size
    FLAGS.eval_step_interval = eval_step_interval
    FLAGS.validation_batch_size = validation_batch_size
    FLAGS.test_batch_size = test_batch_size
    FLAGS.print_misclassified_test_images = print_misclassified_test_images
    FLAGS.output_graph = output_graph
    FLAGS.output_labels = output_labels
    return FLAGS

if __name__ == "__main__":

    for train_dataset in train_datasets: # for every training dataset
        #print("Evaluation on dataset: " + train_dataset)
        image_dir = datasets_dir + train_dataset
        # keep the bottlenecks and labels of each training dataset separately to avoid overwriting
        bottleneck_dir = tf_workspace + 'bottlenecks/' + model_type + '/' + train_dataset
        output_labels = tf_workspace + 'retrained/graphs/' + model_type + '_' + train_dataset + '_labels.txt'

        max_validation_accuracy = 0
        best_learning_rate = 0
        best_architecture = ''
        best_batch_size = 0
        best_test_accuracy = 0
        best_graph = ''

        # tunable parameters
        for architecture in architectures:
            #print("Evaluation with architecture: " + architecture)
            for batch_size in train_batch_sizes:
                # print("Evaluation with batch size: " + batch_size)
                for learning_rate in learning_rates:
                    #print("Evaluation with learning rate: %f" % (learning_rate))
                    print("Evaluation - dataset: " + train_dataset + " - architecture: " + architecture + " - batch size: " + str(batch_size) + " - lr: " + str(learning_rate))

                    summaries_dir = tf_workspace + 'training_summaries/' + model_type + '_' + train_dataset + "_" + architecture + "_" + str(batch_size) + "_LR" + str(learning_rate)
                    output_graph = tf_workspace + 'retrained/graphs/' + model_type + '_' + train_dataset + "_" + architecture + "_" + str(batch_size)+ "_LR" + str(learning_rate) + '_graph.pb'

                    validation_percentage = validation_percentage_train
                    # set all FLAGS before calling retrain
                    retrain.FLAGS = set_retrain_flags()
                    # obtain validation accuracy and test accuracy
                    validation_accuracy, test_accuracy = retrain.main("")

                    print("Validation accuracy: %f" % (validation_accuracy))

                    if validation_accuracy > max_validation_accuracy:
                        max_validation_accuracy = validation_accuracy
                        best_learning_rate = learning_rate
                        best_architecture = architecture
                        best_batch_size = batch_size
                        best_test_accuracy = test_accuracy
                        best_graph = retrain.FLAGS.output_graph


        # now that the best parameters have been identified based on the validation accuracy,
        # print the test accuracy of this model and retrain it on a larger part of the training set
        print("Validation accuracy of best model: %f" % (max_validation_accuracy))
        print("Test accuracy of best model: %f" % (best_test_accuracy))
        print("Best setup: LR=" + str(best_learning_rate) + " Architecture=" + best_architecture + " Batch size=" + str(best_batch_size))

        if apply_on_test:
            print("Retraining best model on larger portion of the training dataset")
            # we don't want to update the existing summaries, give the default value
            summaries_dir = '/tmp/retrain_logs'
            learning_rate = best_learning_rate
            architecture = best_architecture
            batch_size = best_batch_size
            validation_percentage = validation_percentage_test
            output_graph = best_graph
            retrain.FLAGS = set_retrain_flags()
            retrain.main("")

            test_dataset_path = datasets_dir + test_dataset + '/'

            # use the saved graph of this final model to make predictions/evaluations on the final test set
            # using the label_many_images.py
            print("Evaluating best model on test set")
            performance_metrics = label_many_images.evaluate_model(best_graph, retrain.FLAGS.output_labels, pos_label, test_dataset_path, best_architecture)

            # write main results in a csv file!
            results_file_path = test_dataset_path + 'results_' + train_dataset + '_' + str(time.time()) + '.txt'
            results_file = open(results_file_path, 'w', 0)
            header = 'train_dataset,train_acc_validation,train_acc_test,threshold,test_acc,test_prec,test_recall,test_auc,time,model'
            results_file.write(header + '\n')

            for performance_metric in performance_metrics: # for every threshold basically
                results_file.write(train_dataset + ',')
                results_file.write("%.3f," % max_validation_accuracy)
                results_file.write("%.3f," % best_test_accuracy)
                for metric in performance_metric:
                    results_file.write("%.3f," % metric)
                results_file.write(best_graph + '\n')

            results_file.close()
