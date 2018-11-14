import os
import random

dataset_folder_root = '/home/espyromi/sky detection/usable_sky_test/'
#pos_folder = 'sky/'
#neg_folder = 'nosky/'
pos_folder = 'usable/'
neg_folder = 'unusable/'
gt_file_path = dataset_folder_root + 'gt.csv'

# list all files in each dir and add the folder (sky/nosky) in the beginning
pos_image_files = [pos_folder + fi for fi in os.listdir(dataset_folder_root + pos_folder)]
neg_image_files = [neg_folder + fi for fi in os.listdir(dataset_folder_root + neg_folder)]

#merge and shuffle the two lists
all_images = pos_image_files + neg_image_files;
print len(all_images),": ",all_images[:10]
random.seed()
random.shuffle(all_images)
print len(all_images),": ",all_images[:10]

#now create the gt file
gt_file = open(gt_file_path, 'w')

for image in all_images:
    label = ''
    if image.startswith(pos_folder):
        label = '1'
    else:
        label = '0'

    gt_file.write(image + ',' + label + "\n")

gt_file.close()