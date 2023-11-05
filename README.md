# TensorFlow Lite vs PyTorch Mobile ObjectDetectionApp

Sample app to test and compare live object detection using TensorFlow Lite and Pytorch Mobile.

## Quickstart
1) Clone the repo on Android Studio
```bash
git clone https://github.com/federicopuy/ObjectDetectionApp.git

```
2) Run the app!

Tested on Android Studio Giraffe | 2022.3.1

## Features
The app will use the device camera to analyze the images captured, detect objects in them, and perform inference to determine the type of objects in each image. A bounding box will be drawn surrounding the object and its type and confidence score will be shown.
By default its using TensorFlow Lite + NNAPI + mobilenetv1.tflite model, but you can easily change to Pytorch Mobile and yolov5s model by switching the Object detector in ObjectDetectionViewModel (see TODO).

## 3P Dependencies 
- Compose for UI
- TensorFlow Lite and PyTorch Mobile for Object Detection
- Jetpack ViewModel for keeping state and handling business and presentation logic
- Hilt for Dependency Injection
  

## Architecture

![ObjectDetectionApp (1)](https://github.com/federicopuy/ObjectDetectionApp/assets/12384264/71403a1e-69d3-489a-9de3-f48fa0f0b30d)
