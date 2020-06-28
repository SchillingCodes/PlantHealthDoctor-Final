import argparse, os
import numpy as np
import locale

import tensorflow as tf
import keras
from keras import backend as K
from keras.optimizers import Adam
from keras.models import Model
from keras.models import Sequential
from keras.layers import Dense, Dropout, Activation, Flatten, BatchNormalization, Conv2D, MaxPooling2D, Input
from keras.optimizers import SGD
from keras.utils import multi_gpu_model
from keras.preprocessing.image import ImageDataGenerator
import matplotlib.pyplot as plt
from pathlib import Path
from datetime import datetime as dt
from keras import metrics
from keras.callbacks import EarlyStopping

def create_cnn(width, height, depth, filters=(16,32,64), regress=False):
    # initialize the input shape and channel dimension, assuming
    # TensorFlow/channels-last ordering
    inputShape = (height, width, depth)
    chanDim = -1
    
    # define the model input
    inputs = Input(shape=inputShape)
 
    # loop over the number of filters
    for (i, f) in enumerate(filters):
        # if this is the first CONV layer then set the input
        # appropriately
        if i == 0:
            x = inputs
 
        # CONV => RELU => BN => POOL
        x = Conv2D(f, (3, 3), padding="same")(x)
        x = Activation("relu")(x)
        x = BatchNormalization(axis=chanDim)(x)
        x = MaxPooling2D(pool_size=(2, 2))(x)
        x = Dropout(0.5)(x)
        
    # flatten the volume, then FC => RELU => BN => DROPOUT
    x = Flatten()(x)
    x = Dense(16)(x)
    x = Activation("relu")(x)
    x = BatchNormalization(axis=chanDim)(x)
    x = Dropout(0.5)(x)
 
    # apply another FC layer, this one to match the number of nodes
    # coming out of the MLP
    x = Dense(4)(x)
    x = Activation("relu")(x)
 
    # check to see if the regression node should be added
    if regress:
        x = Dense(1, activation="linear")(x)
 
    # construct the CNN
    model = Model(inputs, x)
  
    return model

if __name__ == '__main__':
        
    parser = argparse.ArgumentParser()

    parser.add_argument('--epochs', type=int, default=10)
    parser.add_argument('--learning-rate', type=float, default=0.01)
    parser.add_argument('--batch-size', type=int, default=128)
    parser.add_argument('--gpu-count', type=int, default=os.environ['SM_NUM_GPUS'])
    parser.add_argument('--model-dir', type=str, default=os.environ['SM_MODEL_DIR'])
    parser.add_argument('--training', type=str, default=os.environ['SM_CHANNEL_TRAINING'])
    parser.add_argument('--validation', type=str, default=os.environ['SM_CHANNEL_VALIDATION'])
    parser.add_argument('--output-dir', default=os.getenv('SM_OUTPUT_DATA_DIR', 'outputs/'))
    
    args, _ = parser.parse_known_args()
    
    epochs     = args.epochs
    lr         = args.learning_rate
    batch_size = args.batch_size
    gpu_count  = args.gpu_count
    model_dir  = args.model_dir
    training_dir   = args.training
    validation_dir = args.validation
    
    is_sagemaker = 'SM_CHANNEL_DATASET' in os.environ
    
    # output directory
    output_dir = Path(args.output_dir)
    if is_sagemaker:
        model_dir = args.model_dir
    else:
        output_dir /= dt.now().strftime('%Y-%m-%d-%H-%M')
        output_dir.mkdir(parents=True)
        model_dir = str(output_dir / args.model_dir)
    
    x_train = np.load(os.path.join(training_dir, 'training.npz'))['image']
    y_train = np.load(os.path.join(training_dir, 'training.npz'))['label']
    x_val  = np.load(os.path.join(validation_dir, 'validation.npz'))['image']
    y_val  = np.load(os.path.join(validation_dir, 'validation.npz'))['label']
    
    # input image dimensions
    img_rows, img_cols = 64, 64

    # Tensorflow needs image channels last, e.g. (batch size, width, height, channels)
    K.set_image_data_format('channels_last')  
    print(K.image_data_format())

    if K.image_data_format() == 'channels_first':
        print("Incorrect configuration: Tensorflow needs channels_last")
    else:
        # channels last
        #x_train = x_train.reshape(x_train.shape[0], img_rows, img_cols, 1)
        #x_val = x_val.reshape(x_val.shape[0], img_rows, img_cols, 1)
        input_shape = (img_rows, img_cols, 3)
        batch_norm_axis=-1

    print('x_train shape:', x_train.shape)
    print(x_train.shape[0], 'train samples')
    print(x_val.shape[0], 'test samples')
    
    # Normalize pixel values
    x_train  = x_train.astype('float32')
    x_val    = x_val.astype('float32')
    y_train  = y_train.astype('float32')
    y_val    = y_val.astype('float32')
    x_train /= 255
    x_val   /= 255
    
    # Convert class vectors to binary class matrices
    #num_classes = 10
    #y_train = keras.utils.to_categorical(y_train, num_classes)
    #y_val   = keras.utils.to_categorical(y_val, num_classes)
    
    model = create_cnn(64, 64, 3, regress=True)
    
    print(model.summary())
    
    aug = ImageDataGenerator(horizontal_flip=True,
                             zoom_range=[0.9,1.1],
                             fill_mode="nearest",
                             brightness_range=[0.5,1.25],
                             width_shift_range=[-8,8],
                             height_shift_range=[-8,8])

    if gpu_count > 1:
        model = multi_gpu_model(model, gpus=gpu_count)
        
    model.compile(loss="mean_absolute_error",
                  optimizer=Adam(lr=1e-3, decay=1e-4))
    
    # simple early stopping
    es = EarlyStopping(monitor='val_loss', mode='min', verbose=1, patience=50)
    
    history = model.fit_generator(aug.flow(x_train, y_train, batch_size=batch_size),
                  validation_data=aug.flow(x_val, y_val, batch_size=batch_size),
                  validation_steps=len(x_val)/batch_size,
                  steps_per_epoch=len(x_train)/batch_size,
                  epochs=epochs,
                  verbose=2,
                  callbacks=[es])
    
    # make predictions on the testing data
    print("[INFO] predicting pH levels...")
    preds = model.predict(x_val)

    # compute the difference between the *predicted* house prices and the
    # *actual* house prices, then compute the percentage difference and
    # the absolute percentage difference
    diff = preds.flatten() - y_val
    percentDiff = (diff / y_val) * 100
    absPercentDiff = np.abs(percentDiff)

    # compute the mean and standard deviation of the absolute percentage
    # difference
    mean = np.mean(absPercentDiff)
    std = np.std(absPercentDiff)

    # finally, show some statistics on our model
    #locale.setlocale(locale.LC_ALL, "en_US.UTF-8")
    #print("[INFO] avg. house price: {}, std house price: {}".format(
        #locale.currency(df["price"].mean(), grouping=True),
        #locale.currency(df["price"].std(), grouping=True)))
    print("[INFO] mean: {:.2f}%, std: {:.2f}%".format(mean, std))
    print(preds)
    print(y_val)
    
    # save Keras model for Tensorflow Serving
    sess = K.get_session()
    tf.saved_model.simple_save(
        sess,
        os.path.join(model_dir, 'model/1'),
        inputs={'inputs': model.input},
        outputs={t.name: t for t in model.outputs})
    
    print(history.history.keys())
    
    # summarize history for loss
    plt.plot(history.history['loss'])
    plt.plot(history.history['val_loss'])
    plt.title('model loss')
    plt.ylabel('loss')
    plt.xlabel('epoch')
    plt.legend(['train', 'test'], loc='upper left')
    loss_file = output_dir / 'loss.png'
    plt.savefig(str(loss_file))
    plt.clf()