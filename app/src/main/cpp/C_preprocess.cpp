#include <iostream>
#include <math.h>
#include <string>
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/core.hpp"
#include <numeric>
#include <random>
#include <algorithm>
#include <time.h>
#include <numeric>
#include "C_preprocess.hpp"
#include <android/log.h>
#include "jni.h"

//#define DEBUG 0
inline int rgb(int red, int green, int blue) {
    return (0xFF << 24) | (red << 16) | (green << 8) | blue;
}

using namespace std;
using namespace cv;
std::vector<cv::Point2f> line_point;

Mat claheGO(Mat src,int _step = 8)
{
    Mat CLAHE_GO = src.clone();
    int block = _step;//pblock
    int width = src.cols;
    int height= src.rows;
    int width_block = width/block;
    int height_block = height/block;
    int tmp2[8*8][256] ={0};
    float C2[8*8][256] = {0.0};
    int total = width_block * height_block;
    for (int i=0;i<block;i++)
    {
        for (int j=0;j<block;j++)
        {
            int start_x = i*width_block;
            int end_x = start_x + width_block;
            int start_y = j*height_block;
            int end_y = start_y + height_block;
            int num = i+block*j;
            for(int ii = start_x ; ii < end_x ; ii++)
            {
                for(int jj = start_y ; jj < end_y ; jj++)
                {
                    int index =src.at<uchar>(jj,ii);
                    tmp2[num][index]++;
                }
            }
            int average = width_block * height_block / 255;
            int LIMIT = 40 * average;
            int steal = 0;
            for(int k = 0 ; k < 256 ; k++)
            {
                if(tmp2[num][k] > LIMIT){
                    steal += tmp2[num][k] - LIMIT;
                    tmp2[num][k] = LIMIT;
                }
            }
            int bonus = steal/256;
            for(int k = 0 ; k < 256 ; k++)
            {
                tmp2[num][k] += bonus;
            }
            for(int k = 0 ; k < 256 ; k++)
            {
                if( k == 0)
                    C2[num][k] = 1.0f * tmp2[num][k] / total;
                else
                    C2[num][k] = C2[num][k-1] + 1.0f * tmp2[num][k] / total;
            }
        }
    }
    for(int  i = 0 ; i < width; i++)
    {
        for(int j = 0 ; j < height; j++)
        {
            //four coners
            if(i <= width_block/2 && j <= height_block/2)
            {
                int num = 0;
                CLAHE_GO.at<uchar>(j,i) = (int)(C2[num][CLAHE_GO.at<uchar>(j,i)] * 255);
            }else if(i <= width_block/2 && j >= ((block-1)*height_block + height_block/2)){
                int num = block*(block-1);
                CLAHE_GO.at<uchar>(j,i) = (int)(C2[num][CLAHE_GO.at<uchar>(j,i)] * 255);
            }else if(i >= ((block-1)*width_block+width_block/2) && j <= height_block/2){
                int num = block-1;
                CLAHE_GO.at<uchar>(j,i) = (int)(C2[num][CLAHE_GO.at<uchar>(j,i)] * 255);
            }else if(i >= ((block-1)*width_block+width_block/2) && j >= ((block-1)*height_block + height_block/2)){
                int num = block*block-1;
                CLAHE_GO.at<uchar>(j,i) = (int)(C2[num][CLAHE_GO.at<uchar>(j,i)] * 255);
            }
                //four edges except coners
            else if( i <= width_block/2 )
            {
                int num_i = 0;
                int num_j = (j - height_block/2)/height_block;
                int num1 = num_j*block + num_i;
                int num2 = num1 + block;
                float p =  (j - (num_j*height_block+height_block/2))/(1.0f*height_block);
                float q = 1-p;
                CLAHE_GO.at<uchar>(j,i) = (int)((q*C2[num1][CLAHE_GO.at<uchar>(j,i)]+ p*C2[num2][CLAHE_GO.at<uchar>(j,i)])* 255);
            }else if( i >= ((block-1)*width_block+width_block/2)){
                int num_i = block-1;
                int num_j = (j - height_block/2)/height_block;
                int num1 = num_j*block + num_i;
                int num2 = num1 + block;
                float p =  (j - (num_j*height_block+height_block/2))/(1.0f*height_block);
                float q = 1-p;
                CLAHE_GO.at<uchar>(j,i) = (int)((q*C2[num1][CLAHE_GO.at<uchar>(j,i)]+ p*C2[num2][CLAHE_GO.at<uchar>(j,i)])* 255);
            }else if( j <= height_block/2 ){
                int num_i = (i - width_block/2)/width_block;
                int num_j = 0;
                int num1 = num_j*block + num_i;
                int num2 = num1 + 1;
                float p =  (i - (num_i*width_block+width_block/2))/(1.0f*width_block);
                float q = 1-p;
                CLAHE_GO.at<uchar>(j,i) = (int)((q*C2[num1][CLAHE_GO.at<uchar>(j,i)]+ p*C2[num2][CLAHE_GO.at<uchar>(j,i)])* 255);
            }else if( j >= ((block-1)*height_block + height_block/2) ){
                int num_i = (i - width_block/2)/width_block;
                int num_j = block-1;
                int num1 = num_j*block + num_i;
                int num2 = num1 + 1;
                float p =  (i - (num_i*width_block+width_block/2))/(1.0f*width_block);
                float q = 1-p;
                CLAHE_GO.at<uchar>(j,i) = (int)((q*C2[num1][CLAHE_GO.at<uchar>(j,i)]+ p*C2[num2][CLAHE_GO.at<uchar>(j,i)])* 255);
            }
            else{
                int num_i = (i - width_block/2)/width_block;
                int num_j = (j - height_block/2)/height_block;
                int num1 = num_j*block + num_i;
                int num2 = num1 + 1;
                int num3 = num1 + block;
                int num4 = num2 + block;
                float u = (i - (num_i*width_block+width_block/2))/(1.0f*width_block);
                float v = (j - (num_j*height_block+height_block/2))/(1.0f*height_block);
                CLAHE_GO.at<uchar>(j,i) = (int)((u*v*C2[num4][CLAHE_GO.at<uchar>(j,i)] +
                                                 (1-v)*(1-u)*C2[num1][CLAHE_GO.at<uchar>(j,i)] +
                                                 u*(1-v)*C2[num2][CLAHE_GO.at<uchar>(j,i)] +
                                                 v*(1-u)*C2[num3][CLAHE_GO.at<uchar>(j,i)]) * 255);
            }
            //smooth
            CLAHE_GO.at<uchar>(j,i) = CLAHE_GO.at<uchar>(j,i) + (CLAHE_GO.at<uchar>(j,i) << 8) + (CLAHE_GO.at<uchar>(j,i) << 16);
        }
    }
    return CLAHE_GO;
}

int PreProcess::linear_equation(float a1, float b1, float c1, float a2, float b2, float c2, cv::Point2f *_point) {

    double determinant = a1*b2 - a2 *b1;
    if(determinant != 0) {
        _point->x = (c1*b2 - b1*c2)/determinant;
        _point->y = (a1*c2 - c1*a2)/determinant;
        return 1;
    } else
        return  0;

}

double area_triangle(double a, double b, double c) {
    double s = (a + b + c)/2;
    s = sqrt(s * (s - a) * (s - b) * (s - c));
    return s;
}


//int PreProcess::take_action() {
//    return action;
//}

//cv::Point2f* PreProcess::take_point() {
//    return this->point ;
//}

// Camera Adjustment Direction
void PreProcess::EdgeProcess(){
    float point_value;
    cv::Point2f top_left, top_right, bottom_left, bottom_right;
    double top, left, right, bottom, diagonal;
    if (numofEdge < 4) {
        action = Action::nang_len;
        return;
    }
    else {
        // The 4 intersections of the 4 detected edges
        linear_equation(cos(original[1]), sin(original[1]), original[0], cos(perpendicular1[1]), sin(perpendicular1[1]), perpendicular1[0], &point[0]);
        linear_equation(cos(original[1]), sin(original[1]), original[0], cos(perpendicular2[1]), sin(perpendicular2[1]), perpendicular2[0], &point[1]);
        linear_equation(cos(parallel[1]), sin(parallel[1]), parallel[0], cos(perpendicular1[1]), sin(perpendicular1[1]), perpendicular1[0], &point[2]);
        linear_equation(cos(parallel[1]), sin(parallel[1]), parallel[0], cos(perpendicular2[1]), sin(perpendicular2[1]), perpendicular2[0], &point[3]);

        sort(point, point + 4,comp);
        top_left = point[0];
        top_right = point[1];
        bottom_left = point[2];
        bottom_right = point[3];
        line_point.push_back(point[0]);
        line_point.push_back(point[1]);
        line_point.push_back(point[2]);
        line_point.push_back(point[3]);
        // Distance
        top = cv::norm(top_right - top_left);
        right = cv::norm(top_right - bottom_right);
        left = cv::norm(top_left - bottom_left);
        bottom = cv::norm(bottom_right - bottom_left);
        diagonal = cv::norm(top_left - bottom_right);

        // Area of the image
        double area = area_triangle(top, right, diagonal) + area_triangle(left, bottom, diagonal);
        double image_area = (double) image.size().height * image.size().width;

        if (top > bottom * 1.25) {
            PreProcess::action = Action::nghieng_len;
            return;
        } else if (bottom > top *  1.25) {
            PreProcess::action = Action::nghieng_xuong;
            return;
        } else if (right > left * 1.15) {
            PreProcess::action = Action::nghieng_phai;
            return;
        } else if (left > right * 1.15) {
            PreProcess::action = Action::nghieng_trai;
            return;
        }
        if (area > 0.4 * image_area) {
            PreProcess::action = Action::chup_anh;
            return;
        }
        if (std::max(top_left.y, top_right.y) < 50 ) {
            PreProcess::action = Action::len_tren;
            return;
        }

        if (std::max(bottom_left.y, bottom_right.y) > image.size().height - 50) {
            PreProcess::action = Action::xuong_duoi;
            return;
        }

        if (std::max(bottom_left.x, top_left.x) < 50) {
            PreProcess::action = Action::sang_trai;
            return;
        }

        if (std::max(bottom_right.x, top_right.x) < 50) {
            PreProcess::action = Action::sang_phai;
            return;
        }

        else {
            PreProcess::action = Action::ha_xuong;
            return;
        }

    }

}

PreProcess::PreProcess(cv::Mat image, float height_threshold, float width_threshold) {
    this->image = image.clone();
    this->height_threshold = height_threshold;
    this->width_threshold = width_threshold;
    original[0] = parallel[0] = perpendicular1[0] = perpendicular2[0] = -1;
};

//int PreProcess::CharSize(char *image){
//    return charSize;
//}

//float PreProcess::morphological(int charSize){
//    int kerSize = int (charSize/2);
//    if (DEBUG)
//        printf("%d\n", kerSize);
//    char kernel[charSize][kerSize];
//
//}

void PreProcess::detectEdges(vector<cv::Vec2f> lines) {
    if (lines.size() == 0) {
        status = 0;
        numofEdge = 0;
        return;
    }
    numofEdge = 1;
    this->original = lines[0]; // original lay la canh dau tien
    if (lines.size() == 1){
    }


//    float rho0 = this->original[0];
//    float theta0 = this->original[1];
    for (int i = 1; i < lines.size(); i++)
    {
        if (numofEdge == 4)
            break;
        float rho = lines[i][0], theta = lines[i][1];
        float rho0 = this->original[0];
        float theta0 = this->original[1];
        float delta = abs(theta - theta0);
        if (this->parallel[0] == -1) {
            if ((theta + theta0 - 2 *M_PI) < angle_threshold && rho*rho0 < 0)
                if (abs(abs(rho0) - abs(rho)) < 30)
                    continue;
            if (abs(rho - rho0) > this->width_threshold)
                if (delta < this->angle_threshold || abs(delta - M_PI) < this->angle_threshold || abs(delta - 2 * M_PI) < this->angle_threshold)
                {
                    this->parallel = lines[i];
                    numofEdge += 1;
                    continue;
                }
        }
        if (abs(delta - M_PI / 2) < this->angle_threshold) {
            if (this->perpendicular1[0] == -1)
            {
                this->perpendicular1 = lines[i];
                numofEdge += 1;
            }

            else if (this->perpendicular2[0] == -1)
            {
                if (abs(perpendicular1[1] + theta - 2 * M_PI) < angle_threshold && rho*this->perpendicular1[0] < 0)
                    if (abs(abs(rho) - abs(this->perpendicular1[0])) < 30)
                        continue;
                float height = abs(rho - this->perpendicular1[0]);
                if (height > height_threshold)
                {

                    this->perpendicular2 = lines[i];
                    numofEdge += 1;
                }

            }
        }
    }
    rec_lines.push_back(original);
    rec_lines.push_back(parallel);
    rec_lines.push_back(perpendicular1);
    rec_lines.push_back(perpendicular2);
}

void PreProcess::process() {
    cv::Mat candy_img, dilation_dst, gray, gray_in, dst;
    vector<cv::Vec2f> lines1;
    vector<Vec4i> lines;
    boundingbox(image, lines1);
    if(this->charSize != 0) {
        cv::Mat kernel = getStructuringElement(cv::MORPH_RECT,
                                               cv::Size(charSize, charSize));
        cv::cvtColor(image, gray_in, cv::COLOR_BGR2GRAY);
        dilate(gray_in, dilation_dst, kernel, cv::Point(-1, -1), 1);
//    if (DEBUG)
////    {
////        cv::Mat debug;
////        cv::namedWindow("Dilation window", cv::WINDOW_NORMAL);
////        cv::resize(dilation_dst, debug, cv::Size(), 0.25, 0.25);
////        cv::imshow("Dilation window", debug);
////        cv::waitKey(0);
////    }

        enforceContrast(dilation_dst, dst, "local");
        smoothImage(dilation_dst, PreProcess::charSize, &dst);
        cv::Canny(dst, candy_img, 20, 50, 3, true);
//    if (DEBUG)
//    {
//        cv::Mat candy;
//        cv::resize(candy_img, candy, cv::Size(), 0.5, 0.5);
//        cv::namedWindow("Display Candy", cv::WINDOW_NORMAL);
//        cv::imshow("Display Candy", candy);
//        cv::waitKey(0);
//    }
        cv::HoughLines(candy_img, lines1, 1, M_PI / 180, 60);
        detectEdges(lines1);
        if (1)
        {
            showImageWithLine();
        }

        EdgeProcess();
    } else{
        PreProcess::action = Action::nang_len;
    }
}

void PreProcess::showImageWithLine() {
    color_dst = image.clone();
    for( size_t i = 0; i < rec_lines.size(); i++ ) {
        float rho = rec_lines[i][0];
        float theta = rec_lines[i][1];
        if (rho == -1)
            continue;
        double a = cos(theta), b = sin(theta);
        double x0 = a*rho, y0 = b*rho;
        cv::Point pt1(cvRound(x0 + 1000*(-b)),
                      cvRound(y0 + 1000*(a)));
        cv::Point pt2(cvRound(x0 - 1000*(-b)),
                      cvRound(y0 - 1000*(a)));
        cv::line(color_dst, pt1, pt2, cv::Scalar(0,0,255), 3, 8);
    }

}

void PreProcess::boundingbox(cv::Mat src, vector <cv::Vec2f> lines){

    vector<vector<cv::Point> > contours;
    RNG rng(12345);
    vector<cv::Vec4i> hierarchy;
    cv::Mat candy_img, gray;
    cv::cvtColor(src, gray, cv::COLOR_BGR2GRAY, 1);
    cv::Canny(gray, candy_img, 50, 100, 3, true);
    cv::findContours(candy_img, contours, hierarchy, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);
    vector<vector<Point> > contours_poly( contours.size() );
    vector<Rect> boundRect( contours.size() );
    std::vector<double> height_list;
    for( int i = 0; i < contours.size(); i++ ) {
        approxPolyDP( Mat(contours[i]), contours_poly[i], 3, true );
        boundRect[i] = boundingRect( Mat(contours_poly[i]) );
    }


    for (int i = 0; i < boundRect.size(); i++) {
        int x = boundRect[i].x;
        int y = boundRect[i].y;
        int width = boundRect[i].width;
        int height = boundRect[i].height;
        if (height < 25)
            height_list.push_back(height);

    }
    double sum = 0.0;
    for(auto i = 0; i<height_list.size(); i++)
        sum += height_list.at(i);
    double mean = sum / height_list.size();
    double accum = 0.0;
    std::for_each (std::begin(height_list), std::end(height_list), [&](const double d) {
        accum += (d - mean) * (d - mean);
    });

    double stdev = sqrt(accum / (height_list.size()-1));
    int count = 0;
    int char_size = 0;
    cv::Vec2i point;
    for (int i = 0; i < boundRect.size(); i++) {
        if (boundRect[i].height > (mean - 0.2 * stdev) && (boundRect[i].height < (mean + 0.2 * stdev))) {
            char_size += boundRect[i].height;
            count += 1;
            point[0] = boundRect[i].x;
            point[1] = boundRect[i].y;
            point_list.push_back(point);

        }
        else {
            boundRect[i].height = 0;
            boundRect[i].width = 0;
        }
    }

    if (count != 0) {
        char_size = (int)char_size / count;
        this->charSize = (int)char_size;
        Mat drawing = Mat::zeros( src.size(), CV_8UC3 );
        for( int i = 0; i< contours.size(); i++ ) {
            Scalar color = Scalar( rng.uniform(0, 255), rng.uniform(0,255), rng.uniform(0,255) );
            drawContours( drawing, contours_poly, i, color, 1, 8, vector<Vec4i>(), 0, Point() );
            if (boundRect[i].height == 0)
                continue;
            rectangle( drawing, boundRect[i].tl(), boundRect[i].br(), color, 2, 8, 0 );
        }
    }


//    if (DEBUG) {
//        namedWindow( "Contours", WINDOW_AUTOSIZE );
//        cv::resize(drawing, drawing, cv::Size(), 0.25, 0.25);
//        imshow( "Contours", drawing );
//        waitKey(0);
//    }
}

void resize_to_screen1(cv::Mat src, cv::Mat *dst, int max_width = 1280, int max_height = 700)
{
    int width = src.size().width;
    int height = src.size().height;

    double scale_x = double(width) / max_width;
    double scale_y = double(height) / max_height;

    int scale = (int)ceil(scale_x > scale_y ? scale_x : scale_y);

    if (scale > 1)
    {
        double invert_scale = 1 / (double)scale;
        cv::resize(src, *dst, cv::Size(0, 0), invert_scale, invert_scale);
    }
    else
    {
        *dst = src.clone();
    }
}


void imageresize (cv::Mat image_in, cv::Mat *image_out) {
    int height = image_in.size().height;
    int width = image_in.size().width;

    if (height > width)
        cv::resize(image_in, *image_out, cv::Size(1000, 1500));
    else
        cv::resize(image_in, *image_out, cv::Size(1500, 1000));
}


void enforceContrast(cv::Mat image, cv::Mat &dst, string option) {
    dst = claheGO(image, 1);
    return;
}

void smoothImage(cv::Mat image, int kerSize, cv::Mat *dst, string option) {
    string str = "Average";
    if (kerSize % 2 == 0)
        kerSize = kerSize - 1;
    if (option == str)
        cv::blur(image, *dst, cv::Size(kerSize, kerSize));
    else
        cv::GaussianBlur(image, *dst, cv::Size(kerSize, kerSize), 2);
}

extern "C"{
JNIEXPORT jlong JNICALL Java_com_example_builddewarp_CaptureImage_getLines
        (JNIEnv*, jobject, jlong inpAddr, jlong outAddr){
    Mat& image = *(Mat*) inpAddr;
    Mat& dst2 = *(Mat*) outAddr;
    cv::Mat dst;
    cv::Mat image_resize;
    Action ac;
    Mat* mat2;
    float width_threshold = 200;
    float height_threshold = 300;
    std::vector<cv::Point2f> point;
    resize_to_screen1(image, &image_resize);
    PreProcess image_process(image_resize, width_threshold, height_threshold);
    image_process.process();
    ac = image_process.action;
    dst2 = image_process.color_dst.clone();
    mat2 = &dst2;
    return (jlong) mat2;
};
JNIEXPORT jint JNICALL Java_com_example_readingassistant2020_CaptureImageActivity_00024ImageSaver_getAction
        (JNIEnv*, jobject, jlong inpAddr){
    Mat& image = *(Mat*) inpAddr;
    cv::Mat dst;
    cv::Mat image_resize;
    Action ac;
    float width_threshold = 200;
    float height_threshold = 300;
    std::vector<cv::Point2f> point;
    resize_to_screen1(image, &image_resize);
    PreProcess image_process(image_resize, width_threshold, height_threshold);
    image_process.process();
    ac = image_process.action;
    return ac;
};
}

