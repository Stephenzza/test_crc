#include "Sample.hpp"
#include <fstream>
#include <iostream>
#include <opencv2/core.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <string>
int main(int argc, char **argv) {
    uint32_t width = 3840;
    uint32_t height = 576;
    uint32_t dataSize = width * height * 3 / 2;
    uint8_t *input = new uint8_t[dataSize];
    opencv_static_sample::Sample sample;
    for (size_t i = 0; i < width * height * 3; i++) {
        input[i] = i % 255;
    }
    std::string inputFile("/home/thundersoft/code/opencv_test/test/img1.yuv");
    std::ifstream in(inputFile, std::ifstream::binary);
    if (!in.is_open() || !in.good()) {
        std::cerr << "Failed to open input file: " << inputFile << "\n";
    }
    if (!in.read(reinterpret_cast<char *>(input), dataSize)) {
        std::cerr << "Failed to read the contents of: " << inputFile << "\n";
    }
    cv::Mat srcImg(height * 3 / 2, width, CV_8UC1, input);
    cv::Mat img;
    cv::cvtColor(srcImg, img, cv::COLOR_YUV2BGR_NV12);
    //  Define the region of interest (ROI) for cropping
    int x = 100;  // x coordinate of the top-left corner of the ROI
    int y = 100;  // y coordinate of the top-left corner of the ROI
    int w = 200;  // width of the ROI
    int h = 150;  // height of the ROI
    // Create a cropped image (ROI)
    cv::Mat croppedImage = img(cv::Rect(x, y, w, h));
    cv::Mat copiedImg = croppedImage.clone();
    //  Define the target width and height
    int targetWidth = 1920;  // Example: target width
    int targetHeight = 288;  // Example: target height
    // Create a new, scaled image to the specified width and height
    cv::Mat scaledImage;
    cv::resize(img, scaledImage, cv::Size(w, h), 0, 0, cv::INTER_LINEAR);
    cv::rectangle(img, cv::Rect(x + 5, y + 5, w - 10, h - 10), cv::Scalar(0, 0, 255), 1,
                  cv::LINE_4);
    //  垂直连接图像
    cv::Mat verticalConcatenated;
    cv::vconcat(scaledImage, croppedImage, verticalConcatenated);
    cv::imshow("img", img);
    cv::imshow("croppedImage", croppedImage);
    cv::imshow("copiedImg", copiedImg);
    cv::imshow("scaledImage", scaledImage);
    cv::imshow("verticalConcatenated", verticalConcatenated);
    cv::waitKey(0);
    delete[] input;
    return 0;
}
