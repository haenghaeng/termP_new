package com.example.termp_new.openCV

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.sqrt

class ScanDoc {
    companion object{
        /**
         * 비트맵을 받으면 거기서 문서를 찾고 문서의 꼭지점 4개를 리턴
         * 만약 문서를 인식할 수 없으면 빈 리스트를 리턴함
         */
        fun getOutlineFromSrc(srcBitmap : Bitmap) : ArrayList<Point> {

            var points = ArrayList<Point>()

            // Bitmap을 OpenCV의 Mat으로 변환
            val src = Mat()
            Utils.bitmapToMat(srcBitmap, src)

            // 흑백영상으로 전환
            val graySrc = Mat()
            Imgproc.cvtColor(src, graySrc, Imgproc.COLOR_BGR2GRAY)

            // 이진화
            val binarySrc = Mat()
            Imgproc.threshold(graySrc, binarySrc, 0.0, 255.0, Imgproc.THRESH_OTSU)

            // 윤곽선 찾기
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                binarySrc,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE
            )

            // 가장 면적이 큰 윤곽선 찾기
            var biggestContour: MatOfPoint? = null
            var biggestContourArea = 0.0
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area > biggestContourArea) {
                    biggestContour = contour
                    biggestContourArea = area
                }
            }

            // 외곽선이 있는지 확인
            if (biggestContour == null) {
                return points
            }
            // 외곽선이 너무 작은지 확인
            if (biggestContourArea < 400) {
                return points
            }

            // 근사화 하여 꼭지점 만들기
            val candidate2f = MatOfPoint2f(*biggestContour.toArray())
            val approxCandidate = MatOfPoint2f()
            Imgproc.approxPolyDP(
                candidate2f,
                approxCandidate,
                Imgproc.arcLength(candidate2f, true) * 0.02,
                true
            )

            // 사각형인지 확인
            if (approxCandidate.rows() != 4) {
                return points
            }

            // 컨벡스(볼록한 도형)인지 판별
            if (!Imgproc.isContourConvex(MatOfPoint(*approxCandidate.toArray()))) {
                return points
            }

            // 좌상단부터 시계 반대 방향으로 정점을 정렬
            points = arrayListOf(
                Point(approxCandidate.get(0, 0)[0], approxCandidate.get(0, 0)[1]),
                Point(approxCandidate.get(1, 0)[0], approxCandidate.get(1, 0)[1]),
                Point(approxCandidate.get(2, 0)[0], approxCandidate.get(2, 0)[1]),
                Point(approxCandidate.get(3, 0)[0], approxCandidate.get(3, 0)[1]),
            )
            points.sortBy { it.x } // x좌표 기준으로 먼저 정렬

            if (points[0].y > points[1].y) {
                val temp = points[0]
                points[0] = points[1]
                points[1] = temp
            }

            if (points[2].y < points[3].y) {
                val temp = points[2]
                points[2] = points[3]
                points[3] = temp
            }

            return points
        }


        /**
         * 원본 비트맵과 문서의 꼭지점 4개(ArrayList<Point>)를 입력받음
         * 추출한 문서의 비트맵을 리턴함
         */
        fun makeDocFromImage(srcBitmap: Bitmap, points: ArrayList<Point>): Bitmap{

            // Bitmap을 OpenCV의 Mat으로 변환
            val src = Mat()
            Utils.bitmapToMat(srcBitmap, src)

            // 원본 영상 내 정점들
            val srcQuad = MatOfPoint2f().apply { fromList(points) }

            val maxSize = calculateMaxWidthHeight(
                tl = points[0],
                bl = points[1],
                br = points[2],
                tr = points[3]
            )
            val dw = maxSize.width
            val dh = dw * maxSize.height/maxSize.width
            val dstQuad = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(0.0, dh),
                Point(dw, dh),
                Point(dw, 0.0)
            )
            // 투시변환 매트릭스 구하기
            val perspectiveTransform = Imgproc.getPerspectiveTransform(srcQuad, dstQuad)

            // 투시변환 된 결과 영상 얻기
            val dst = Mat()
            Imgproc.warpPerspective(src, dst, perspectiveTransform, Size(dw, dh))

            // resultBitmap를 dst와 크기가 같은 Bitmap 으로 설정
            val resultBitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);

            // 결과를 Mat에서 Bitmap으로 변경
            Utils.matToBitmap(dst, resultBitmap)

            return resultBitmap
        }

        private fun calculateMaxWidthHeight(tl:Point, tr:Point, br:Point, bl:Point): Size {
            // Calculate width
            val widthA = sqrt((tl.x - tr.x) * (tl.x - tr.x) + (tl.y - tr.y) * (tl.y - tr.y))
            val widthB = sqrt((bl.x - br.x) * (bl.x - br.x) + (bl.y - br.y) * (bl.y - br.y))
            val maxWidth = max(widthA, widthB)
            // Calculate height
            val heightA = sqrt((tl.x - bl.x) * (tl.x - bl.x) + (tl.y - bl.y) * (tl.y - bl.y))
            val heightB = sqrt((tr.x - br.x) * (tr.x - br.x) + (tr.y - br.y) * (tr.y - br.y))
            val maxHeight = max(heightA, heightB)
            return Size(maxWidth, maxHeight)
        }
    }


}