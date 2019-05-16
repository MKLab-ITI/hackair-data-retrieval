import sys
import time
import requests
import os
import urllib #, cStringIO
import numpy as np
from io import BytesIO
from PIL import Image

caffe_root_python = '/home/hackair/FCN/caffe_future/python'
sys.path.insert(0, caffe_root_python)
import caffe

caffe.set_device(0)
caffe.set_mode_gpu()

#globals
# Pallete of colours
pallete = [ 0,0,0,
	0, 255, 255,
	0, 102, 153,
	0, 51, 204,
	51, 51, 153,
	0, 102, 102,
	0, 153, 204,
	0, 0, 255,
	102, 102, 153,
	51, 153, 102,
	0, 255, 204,
	102, 153, 255,
	102, 0, 255,
	102, 0, 204,
	0, 204, 102,
	153, 204, 255,
	153, 51, 255,
	0, 153, 51,
	255, 255, 255,
	255, 153, 255,
	255, 0, 255,
	102, 0, 102,
	255, 153, 204,
	255, 255, 204,
	51, 51, 0,
	153, 204, 0,
	255, 102, 102,
	255, 153, 102,
	204, 102, 153,
	102, 0, 51,
	255, 102, 0,
	153, 153, 102,
	255, 51, 0,
	255, 0, 0,
	204, 153, 0,
	204, 153, 0,
	128, 0, 0,
	153, 51, 0]

# Newer MAIN PROGRAM
def getSkyMask(image, net):

    start_time = time.time()
    print("Image: " + image)
    img = loadImage(image)
    out = runImageThroughNetwork(img, net)
    print("Processing time: %s seconds " % (time.time() - start_time))

    start_time = time.time() # this takes about 0.15s per image
    mask = encodeMaskToString(out)
    print("Mask encoding time: %s seconds " % (time.time() - start_time))

    start_time = time.time() # this takes about 0.03s per image
    #riteImagePngMaskSky(out, image, '/hackair_on_388/Masks_FCN');
    print("Png mask writing time: %s seconds " % (time.time() - start_time))

    #start_time = time.time()  # this takes about 0.17s per image
    #writeImageTxtMaskSky(out, image, '/hackair_on_388/Masks_FCN');
    #print("Txt mask writing time: %s seconds " % (time.time() - start_time))

    return mask


# FUNCTIONS

# load the network
def loadNetwork(protoTxt, modelFile):
    # Load net
    start_time = time.time()
    net = caffe.Net(protoTxt, modelFile, caffe.TEST)
    print("Network loading time: %s seconds " % (time.time() - start_time))
    return net

# Pass image through network
def runImageThroughNetwork(img, net):
  # shape for input (data blob is N x C x H x W), set data
  net.blobs['data'].reshape(1, *img.shape)
  net.blobs['data'].data[...] = img

  # run net and take argmax for prediction
  net.forward()
  out = net.blobs

  return out;

# load image, switch to BGR, subtract mean, and make dims C x H x W for Caffe
def loadImage(imageFile):
  im = Image.open(imageFile)
  total_size = im.size[0]*im.size[1];
  if total_size > 500*500:
    #image is too large to be processed, return null
    #TODO downscale image to max num pixels (but then the mask will not be valid for the original image)
    raise ValueError('Image is larger than expected!')
  in_ = np.array(im, dtype=np.float32)
  in_ = in_[:,:,::-1]
  in_ -= np.array((104.00698793,116.66876762,122.67891434))
  in_ = in_.transpose((2,0,1))
  return in_;

# load image, switch to BGR, subtract mean, and make dims C x H x W for Caffe
def loadImageRemote(URL):
  imageFile = BytesIO(urllib.urlopen(URL).read())
  im = Image.open(imageFile)
  total_size = im.size[0]*im.size[1];
  if total_size > 500*500:
    #image is too large to be processed, return null
    #TODO downscale image to max num pixels (but then the mask will not be valid for the original image)
    raise ValueError('Image is larger than expected!')
  in_ = np.array(im, dtype=np.float32)
  in_ = in_[:,:,::-1]
  in_ -= np.array((104.00698793,116.66876762,122.67891434))
  in_ = in_.transpose((2,0,1))
  return in_;

# saves the sky mask as a b/w png image
def writeImagePngMaskSky(out, imageFile, pathForMaskOutFiles):
    index = imageFile.rfind('/') + 1
    imageName = imageFile[index:]

    out_img = np.uint8(out['upscore'].data[0].argmax(axis=0))
    out_img[out_img != 27] = 0
    out_img[out_img != 27] = 255
    out_img = Image.fromarray(out_img.astype('uint8'))
    seg = pathForMaskOutFiles + "/" + imageName.replace(".jpg", ".png")
    out_img.save(seg)
    return

# saves the sky mask as a txt file
def writeImageTxtMaskSky(out, fullImageName, textFilePath):
  index = fullImageName.rfind('/') + 1
  imageName = fullImageName[index:]
  output = out['upscore'].data[0].argmax(axis=0)
  output[output != 27] = 0
  output[27 == output] = 1
  numrows = len(output)
  numcols = len(output[0])

  # Open and write file
  np.savetxt(textFilePath+"/"+imageName + ".txt", output, fmt='%d', delimiter=' ', newline='\n', header='', footer='', comments='# ')
  
  return

# returns the sky mask as a compressed string, new lines are marked with R
def encodeMaskToString(out):
    # convert to 0/1 matrix
    mask = out['upscore'].data[0].argmax(axis=0)

    maskString = []
    rows = mask.shape[0]
    cols = mask.shape[1]
    rowSegments = []
    for i in range(0,rows):
        conseq = []
        for j in range(0, cols):

            if mask[i,j] == 27:
                conseq.append(j)

            if mask[i,j] != 27 or (mask[i,j] == 27 and j == cols-1):
                if len(conseq)>0:
                    segment = str(conseq[0])
                    if len(conseq) > 1:
                        segment += "-" + str(conseq[-1])
                    rowSegments.append(segment)
                    del conseq[:]

        rowMaskString = ','.join(rowSegments)
        del rowSegments[:]

        # append row to mask
        #print("Row mask:" + rowMaskString)
        maskString.append(rowMaskString)

    return str(rows) + "X" + str(cols) + "|" + "R".join(maskString) + "R"

# NOT USED FUNCTION FOLLOW!

# caclulates RG ration for sky region (NOT USED)
def calculatingRGratioForSkyRegion(fullImageName, textFilePath):
  index = fullImageName.rfind('/') + 1
  imageName = fullImageName[index:]
  
  # Open image file
  im = Image.open(fullImageName) 
  pix = im.load()
  
  # Open txt mask file
  var = []
  with open(textFilePath+"/"+imageName + ".txt") as file:
     array2d = [[int(digit) for digit in line.strip().split(' ')] for line in file]
  
  numrows = len(array2d)
  numcols = len(array2d[0])
  
  
  # Get RGB values for pixesl with label = 27 and get sum of R and G
  sumR = 0
  sumG = 0
  for i in range(0, numrows):
    for j in range(0, numcols):
      if int(array2d[i][j]) == 27:
        sumR = sumR + pix[i,j][0]
        sumG = sumG + pix[i,j][1]
  #print im.size
    
  ration = float(sumR)/sumG
    
  return ration



# saves the network's output as a png image mask (NOT USED)
def saveImagePngMask(out, imageFile, pathForMaskOutFiles):
    index = imageFile.rfind('/') + 1
    imageName = imageFile[index:]

    # out_img = np.uint8(out['score'].data[0].argmax(axis=0))
    out_img = np.uint8(out['upscore'].data[0].argmax(axis=0))
    out_img = Image.fromarray(out_img)

    global pallete
    out_img.putpalette(pallete)

    seg = pathForMaskOutFiles + "/" + imageName.replace(".jpg", ".png")
    out_img.save(seg)
    return


# saves the network's output as a txt image mask (NOT USED)
def saveImageTextMask(out, fullImageName, textFilePath):
    index = fullImageName.rfind('/') + 1
    imageName = fullImageName[index:]
    output = out['upscore'].data[0].argmax(axis=0)
    numrows = len(output)
    numcols = len(output[0])

    # Open and write file
    np.savetxt(textFilePath + "/" + imageName + ".txt", output, fmt='%d', delimiter=' ', newline='\n', header='',
               footer='', comments='# ')

    return

def exists(path):
    r = requests.head(path)
    return r.status_code == requests.codes.ok
