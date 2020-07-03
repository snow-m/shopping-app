/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huohoubrowser.zxing.client.android;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.huohoubrowser.zxing.ResultPoint;
import com.huohoubrowser.zxing.client.android.camera.CameraManager;
import com.taobao.R;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

	private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
	private static final long ANIMATION_DELAY = 80L;
	private static final int CURRENT_POINT_OPACITY = 0xA0;
	private static final int MAX_RESULT_POINTS = 20;
	private static final int POINT_SIZE = 6;
	private float density;
	private CameraManager cameraManager;
	private final Paint paint;
	private Bitmap resultBitmap;
	private final int maskColor;
	private final int resultColor;
	private final int laserColor;
	private final int resultPointColor;
	private int scannerAlpha;
	private List<ResultPoint> possibleResultPoints;
	private List<ResultPoint> lastPossibleResultPoints;
	private int ScreenRate ;
	private int  sDistance;
	private boolean isFirst = false;  
	/** 
     * 字体大小 
     */  
    private static final int TEXT_SIZE = 16;  
	/** 
	 * 四个绿色边角对应的宽度 
	 */  
	private static final int CORNER_WIDTH = 10;  
	/** 
	 * 扫描框中的中间线的宽度 
	 */  
	private static final int MIDDLE_LINE_WIDTH = 6;  

	/** 
	 * 扫描框中的中间线的与扫描框左右的间隙 
	 */  
	private static final int MIDDLE_LINE_PADDING = 5;  
	   /** 
     * 字体距离扫描框下面的距离 
     */  
    private static final int TEXT_PADDING_TOP = 10;  
	   /** 
     * 中间滑动线的最顶端位置 
     */  
    private int slideTop;  
      
    /** 
     * 中间滑动线的最底端位置 
     */  
    private int slideBottom;  
    //扫描线
	private static final int SPEEN_DISTANCE = 30;  
	// This constructor is used when the class is built from an XML resource.
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		density = context.getResources().getDisplayMetrics().density;  
		//将像素转换成dp  
		ScreenRate = (int)(20 * density);  
		sDistance = (int)(10 * density);  
		// Initialize these once for performance rather than calling them every time in onDraw().
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);
		laserColor = resources.getColor(R.color.viewfinder_laser);
		resultPointColor = resources.getColor(R.color.possible_result_points);
		scannerAlpha = 0;
		possibleResultPoints = new ArrayList<ResultPoint>(5);
		lastPossibleResultPoints = null;
	}

	public void setCameraManager(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (cameraManager == null) {
			return; // not ready yet, early draw before done configuring
		}
		Rect frame = cameraManager.getFramingRect();
		if (frame == null) {
			return;
		}
		//初始化中间线滑动的最上边和最下边  
		if(!isFirst){  
			isFirst = true;  
			slideTop = frame.top;  
			slideBottom = frame.bottom;  
		}  
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		// Draw the exterior (i.e. outside the framing rect) darkened
		paint.setColor(resultBitmap != null ? resultColor : maskColor);
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);

		if (resultBitmap != null) {
			// Draw the opaque result bitmap over the scanning rectangle
			paint.setAlpha(CURRENT_POINT_OPACITY);
			canvas.drawBitmap(resultBitmap, null, frame, paint);
		} else {

			//画扫描框边上的角，总共8个部分  
			paint.setColor(getResources().getColor(R.color.scan_line_bg));  
			canvas.drawRect(frame.left-sDistance, frame.top-sDistance, frame.left-sDistance + ScreenRate,  
					frame.top-sDistance + CORNER_WIDTH, paint);  
			canvas.drawRect(frame.left-sDistance, frame.top-sDistance, frame.left-sDistance + CORNER_WIDTH, frame.top-sDistance  
					+ ScreenRate, paint);  
			canvas.drawRect(frame.right+sDistance - ScreenRate, frame.top-sDistance, frame.right+sDistance,  
					frame.top-sDistance + CORNER_WIDTH, paint);  
			canvas.drawRect(frame.right+sDistance - CORNER_WIDTH, frame.top-sDistance, frame.right+sDistance, frame.top-sDistance  
					+ ScreenRate, paint);  
			canvas.drawRect(frame.left-sDistance, frame.bottom+sDistance - CORNER_WIDTH, frame.left-sDistance  
					+ ScreenRate, frame.bottom+sDistance, paint);  
			canvas.drawRect(frame.left-sDistance, frame.bottom+sDistance - ScreenRate,  
					frame.left-sDistance + CORNER_WIDTH, frame.bottom+sDistance, paint);  
			canvas.drawRect(frame.right+sDistance - ScreenRate, frame.bottom +sDistance - CORNER_WIDTH,  
					frame.right+sDistance, frame.bottom+sDistance, paint);  
			canvas.drawRect(frame.right+sDistance - CORNER_WIDTH, frame.bottom+sDistance - ScreenRate,  
					frame.right+sDistance, frame.bottom+sDistance, paint);  


			//绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE  
			slideTop += SPEEN_DISTANCE;  
			if(slideTop >= frame.bottom){  
				slideTop = frame.top;  
			}  
			canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH/2, frame.right - MIDDLE_LINE_PADDING,slideTop + MIDDLE_LINE_WIDTH/2, paint);  


			//画扫描框下面的字  
			paint.setColor(Color.WHITE);    
			paint.setTextSize(TEXT_SIZE * density);    
			paint.setTypeface(Typeface.DEFAULT_BOLD);   
			String text = getResources().getString(R.string.msg_default_status);  
			float textWidth = paint.measureText(text);  
			canvas.drawText(text, (width - textWidth)/2, (float) (frame.top - (float)TEXT_PADDING_TOP *density), paint);

			// Draw a red "laser scanner" line through the middle to show decoding is active
//			paint.setColor(laserColor);
//			paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
//			scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
//			int middle = frame.height() / 2 + frame.top;
//			canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);

			Rect previewFrame = cameraManager.getFramingRectInPreview();
			float scaleX = frame.width() / (float) previewFrame.width();
			float scaleY = frame.height() / (float) previewFrame.height();

			List<ResultPoint> currentPossible = possibleResultPoints;
			List<ResultPoint> currentLast = lastPossibleResultPoints;
			int frameLeft = frame.left;
			int frameTop = frame.top;
			if (currentPossible.isEmpty()) {
				lastPossibleResultPoints = null;
			} else {
				possibleResultPoints = new ArrayList<ResultPoint>(5);
				lastPossibleResultPoints = currentPossible;
				paint.setAlpha(CURRENT_POINT_OPACITY);
				paint.setColor(resultPointColor);
				synchronized (currentPossible) {
					for (ResultPoint point : currentPossible) {
						canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
								frameTop + (int) (point.getY() * scaleY),
								POINT_SIZE, paint);
					}
				}
			}
			if (currentLast != null) {
				paint.setAlpha(CURRENT_POINT_OPACITY / 2);
				paint.setColor(resultPointColor);
				synchronized (currentLast) {
					float radius = POINT_SIZE / 2.0f;
					for (ResultPoint point : currentLast) {
						canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
								frameTop + (int) (point.getY() * scaleY),
								radius, paint);
					}
				}
			}

			// Request another update at the animation interval, but only repaint the laser line,
			// not the entire viewfinder mask.
			postInvalidateDelayed(ANIMATION_DELAY,
					frame.left - POINT_SIZE,
					frame.top - POINT_SIZE,
					frame.right + POINT_SIZE,
					frame.bottom + POINT_SIZE);
		}
	}

	public void drawViewfinder() {
		Bitmap resultBitmap = this.resultBitmap;
		this.resultBitmap = null;
		if (resultBitmap != null) {
			resultBitmap.recycle();
		}
		invalidate();
	}

	/**
	 * Draw a bitmap with the result points highlighted instead of the live scanning display.
	 *
	 * @param barcode An image of the decoded barcode.
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		List<ResultPoint> points = possibleResultPoints;
		synchronized (points) {
			points.add(point);
			int size = points.size();
			if (size > MAX_RESULT_POINTS) {
				// trim it
				points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
			}
		}
	}

}
