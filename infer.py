import cv2
import numpy as np
from openvino.runtime import Core
from pprint import pprint
import os
import time
import openvino
import shutil
def preprocess(image, img_h, img_w):
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    scale = max(image.shape[0] / img_h, image.shape[1] / img_w)
    image = cv2.resize(image, (int(image.shape[1] / scale), int(image.shape[0] / scale)))
   
    wpad = img_w - image.shape[1]
    hpad = img_h - image.shape[0]
    image_pad = np.ones((image.shape[0]+hpad, image.shape[1]+wpad, 3)) * 114.0
    image_pad[:image.shape[0], :image.shape[1], :] = image
    image_array = image_pad
    
    image_array = image_array / 255.0
    image_array = image_array.transpose((2, 0, 1))
    image_array = image_array.astype(np.float32)
    input_array = np.ascontiguousarray(np.expand_dims(image_array, 0))
    return input_array, scale, image.shape[0], image.shape[1]
def postprocess(pred, conf_thres, iou_thres, img_w, img_h):
    pred = np.squeeze(pred).transpose((1, 0))
    conf = np.max(pred[..., 4:], axis=-1)
    mask = conf >= conf_thres
    box = pred[mask][..., :4]
    confidences = conf[mask]
    clsid = np.argmax(pred[mask][..., 4:], axis=1)  
    bounding_boxes = np.zeros_like(box)
    bounding_boxes[:, 0] = (box[:, 0] - box[:, 2] / 2) + clsid * img_w
    bounding_boxes[:, 1] = (box[:, 1] - box[:, 3] / 2) + clsid * img_h
    bounding_boxes[:, 2] = box[:, 2]
    bounding_boxes[:, 3] = box[:, 3]
    bounding_boxes[:, 2] += bounding_boxes[:, 0]
    bounding_boxes[:, 3] += bounding_boxes[:, 1]
    if bounding_boxes.shape[0] == 0:
        return []
    bounding_boxes, confidences = bounding_boxes.astype(np.float32), np.array(confidences)
    x1, y1, x2, y2 = bounding_boxes[:, 0], bounding_boxes[:, 1], bounding_boxes[:, 2], bounding_boxes[:, 3]
    areas = (x2 - x1 + 1) * (y2 - y1 + 1)
    idxs = np.argsort(confidences)
    pick = []
    while len(idxs) > 0:
        last_idx = len(idxs) - 1
        max_value_idx = idxs[last_idx]
        pick.append(max_value_idx)
        xx1 = np.maximum(x1[max_value_idx], x1[idxs[: last_idx]])
        yy1 = np.maximum(y1[max_value_idx], y1[idxs[: last_idx]])
        xx2 = np.minimum(x2[max_value_idx], x2[idxs[: last_idx]])
        yy2 = np.minimum(y2[max_value_idx], y2[idxs[: last_idx]])
        w, h = np.maximum(0, xx2 - xx1 + 1), np.maximum(0, yy2 - yy1 + 1)
        iou = w * h / areas[idxs[: last_idx]]
        idxs = np.delete(idxs, np.concatenate(([last_idx], np.where(iou > iou_thres)[0])))
    out = np.concatenate([box[pick], confidences[pick].reshape(-1, 1), clsid[pick].reshape(-1, 1)], axis=1)
    return out
def draw(img, xscale, yscale, pred, color=(255, 0, 0), tmp=True):
    img_ = img.copy()
    result = []
    if len(pred):
        for detect in pred:
            caption = str('{:.2f}_{}'.format(detect[4], int(detect[5])))
            detect_ = [int((detect[0] - detect[2] / 2) * xscale), int((detect[1] - detect[3] / 2) * yscale),
                      int((detect[0] + detect[2] / 2) * xscale), int((detect[1] + detect[3] / 2) * yscale)]
            w = detect_[2]-detect_[0]
            h = detect_[3]-detect_[1]
            result.append([int(detect[5])+10, (detect_[0]+w/2)/img.shape[1], (detect_[1]+h/2)/img.shape[0], w/img.shape[1], h/img.shape[0]])
            img_ = cv2.rectangle(img, (detect_[0], detect_[1]), (detect_[2], detect_[3]), color, 2)
            img_ = cv2.circle(img_, (int(result[-1][1]), int(result[-1][2])), 5, (0, 0, 255), -1)
            if tmp:
                cv2.putText(img, caption, (detect_[0], detect_[1] - 5), 0, 1, color, 2, 16)
            
    return img_, result
class OpenvinoInference(object):
    def __init__(self, onnx_path):
        self.onnx_path = onnx_path
        ie = Core()
        self.model_onnx = ie.read_model(model=self.onnx_path)
        self.compiled_model_onnx = ie.compile_model(model=self.model_onnx, device_name="GPU", config={openvino.properties.intel_gpu.disable_winograd_convolution:True})
        self.output_layer_onnx = self.compiled_model_onnx.output(0)
    def predict(self, datas):
        predict_data = self.compiled_model_onnx([datas])[self.output_layer_onnx]
        return predict_data
if __name__ == '__main__':
    model = OpenvinoInference('ammunition-box_v8s_0613.xml')
    for root, dirs, files in os.walk("./images"):
        for file in files:
            if file.endswith(".jpg"):
                file_name = os.path.splitext(file)[0]
                print(file_name)
                # infer
                img = cv2.imread(os.path.join("./images/", file))
                data, scale, img_w, img_h = preprocess(img, 640, 640)
                pred = model.predict(data)
                result = postprocess(pred, 0.5, 0.45, img_w, img_h)
                ret_img, result = draw(img, scale, scale, result, color=(0, 255, 0), tmp=True)
                cv2.imwrite(os.path.join("./result/", file), ret_img)
                
                txt_file_path = os.path.join("./label/", file_name + ".txt")
                with open(txt_file_path, 'w') as f:
                    lines = []
                    for i in result:
                        data_str = [str(item) for item in i]
                        data_line = ' '.join(data_str)
                        lines.append(data_line)
                    f.write('\n'.join(lines))
