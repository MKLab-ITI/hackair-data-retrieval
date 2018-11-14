import os
import numpy as np
from shutil import copyfile


# A script that randomly splits a dataset into train/test
# Assume that the root folder of the dataset has one subfolder for each class that
# contains all the images that belong to that class

# the following variables should be set appropriately before running the script
datasets_folder_path = '/home/espyromi/sky detection/'
dataset_root_folder = 'usable_sky' # or
pos_class_subfolder = 'usable'
neg_class_subfolder = 'unusable'
train_pct = 0.9 # % of images to go in training set
random_seed = 0 # random seed to use for sampling


dataset_path = datasets_folder_path + dataset_root_folder
train_folder =  datasets_folder_path + dataset_root_folder + '_train/'
test_folder =  datasets_folder_path + dataset_root_folder + '_test/'

# create the new dirs
if not os.path.exists(train_folder):
    os.makedirs(train_folder)
    os.makedirs(train_folder + pos_class_subfolder)
    os.makedirs(train_folder + neg_class_subfolder)

if not os.path.exists(test_folder):
    os.makedirs(test_folder)
    os.makedirs(test_folder + pos_class_subfolder)
    os.makedirs(test_folder + neg_class_subfolder)



# list all files in each dir and add the folder (sky/nosky) in the beginning
pos_image_files = [pos_class_subfolder + '/' + fi for fi in os.listdir(dataset_path + '/' + pos_class_subfolder)]
neg_image_files = [neg_class_subfolder + '/' + fi for fi in os.listdir(dataset_path + '/' + neg_class_subfolder)]

#merge and shuffle the two lists
all_images = pos_image_files + neg_image_files;
# print first 10 images before shuffling
print len(all_images),": ",all_images[:10]
np.random.seed(random_seed)
np.random.shuffle(all_images)
# print first 10 images after shuffling
print len(all_images),": ",all_images[:10]

counter_train = 0
for image in all_images:
    target_folder = ''
    val = np.random.uniform()
    if val <train_pct: #include in training set
        target_folder = train_folder
        counter_train += 1
    else: #include in test set
        target_folder = test_folder
    src = dataset_path + '/' + image
    dst = target_folder + image
    #print "Source: " + src
    #print "Dest: " + dst
    copyfile(src, dst)

print "# items in train: " + str(counter_train)
print "# items in test: "  + str(len(all_images) - counter_train)

