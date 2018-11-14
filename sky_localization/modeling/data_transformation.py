annotation_file = '/media/espyromi/Backup/data/sky detection trecvid dataset/412_TRECVID108_SKY_GT.txt'
images_folder = '/media/espyromi/Backup/data/sky detection trecvid dataset/sky_images/'
new_folder = '/home/espyromi/Desktop/tensorflow-for-poets-2/tf_files/sky_photos/'

import csv
import os
from shutil import copyfile


counter = 0
with open(annotation_file, 'rb') as csvfile:
    gt_reader = csv.reader(csvfile, delimiter=' ')
    for row in gt_reader:
        counter += 1
        if counter%100==0: 
            print 'Processsing row ' + str(counter)
	image_class = '';
	if row[1] == '0':
		continue # unsure class, go to next images
	elif row[1] == '1':
		image_class = 'sky'
	elif row[1] == '-1':
		image_class = 'no_sky'
	else:	
		raise ValueError(row[1] + ' not expected in annotation file (line:'+ counter +')!')
	#print 'Image class: ' + image_class

	subpath = row[0].replace("\\","/");
	new_image_name = subpath.replace("/","_")
		 
        src = images_folder + subpath
        dst = new_folder + image_class + '/' + new_image_name
        #print "Source " + src
        #print "Dest " + dst
        copyfile(src, dst)
        # print ', '.join(row)


#file_to_folder = dict()
#counter = 0
#folders = [fo for fo in os.listdir(images_folder)]
#
#for fo in folders:
#    files = [fi for fi in os.listdir(images_folder+fo)]
#    for fi in files:
#        id = fi.split('.')[0]
#        file_to_folder[id]=fo
#        counter += 1
#        if counter%1000==0: 
#            print 'Processsing file ' + str(counter)


