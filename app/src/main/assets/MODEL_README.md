# Model Details: SSD MobileNet V1 (Quantized)

## Architecture
- **Name:** SSD MobileNet V1
- **Format:** TFLite (with Metadata)
- **Quantization:** UINT8 (Quantized)
- **Input Size:** 300x300

## Source
- **Origin:** TensorFlow Examples (COCO-trained)
- **Download Location:** https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_ssd_mobilenet_v1_1_metadata_2.tflite

## Label Map
- **Bird Class Index:** 16 (1-indexed based on typical COCO mapping in TFLite Metadata)
- **Total Classes:** 90 (COCO)

## Known Limitations & False Positives
- **Planes/Kites:** Large moving objects in the sky may be misclassified as birds.
- **Drones:** Often detected as birds due to size and movement.
- **Leaves/Branches:** Moving vegetation in high wind can trigger false detections.
- **Low Light:** Accuracy drops significantly during dusk/dawn.
- **Small Birds:** Birds far away from the camera might not be detected if they occupy too few pixels.

## Integration Notes
- Uses `tensorflow-lite-task-vision` for simplified inference.
- Confidence threshold should be tuned during Phase 5/9.
