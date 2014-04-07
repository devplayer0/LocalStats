/*
 * Copyright 2013 Ken Yang
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.jackos2500.localstats.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class PieChartView extends View {
	private Paint piePaint;
	private Paint borderPaint;
	private Paint textPaint;

	private int shift			= 0;
	private int margin 			= 0;  // margin to left and right, used to get the radius
	
	private RectF box	 		= null;

	private float density 		= 0f;
	
	private int countColor = Color.BLUE;
	private int remainderColor = Color.RED;
	
	private int count;
	private int remainder;
	
	private float countAngle;
	private float remainderAngle;

	public PieChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		getDisplayMetrics(context);
		shift 	= (int)getRealPxFromDp(8);
		margin = (int)getRealPxFromDp(13);
		
		// used for pie
		piePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		piePaint.setStyle(Paint.Style.FILL);

		// used for border
		borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(getRealPxFromDp(3));
		borderPaint.setColor(Color.WHITE);
		
		// for text
		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setColor(Color.WHITE);
		textPaint.setTextSize(getRealPxFromDp(16));
	}
	
	private void drawValue(Canvas canvas, int value, float startAngle, float angle) {
		float midAngle = startAngle + angle / 2;
		
		float x = (float)((canvas.getWidth() / 2) + (canvas.getWidth() / 4) * Math.cos(midAngle * (Math.PI / 180)));
		float y = (float)((canvas.getHeight() / 2) + (canvas.getHeight() / 4) * Math.sin(midAngle * (Math.PI / 180)));
		canvas.drawText(""+value, x, y, textPaint);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		piePaint.setColor(remainderColor);
		canvas.drawArc(box, 0f, remainderAngle, true, piePaint);
		
		drawValue(canvas, remainder, 0f, remainderAngle);
		
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		float midAngle = remainderAngle + countAngle / 2;
		double dxRadius = Math.toRadians((midAngle + 360f) % 360f);
		float fY = (float) Math.sin(dxRadius);
		float fX = (float) Math.cos(dxRadius);
		canvas.translate(fX * shift, fY * shift);
		
		piePaint.setColor(countColor);
		canvas.drawArc(box, remainderAngle, countAngle, true, piePaint);
		//canvas.drawArc(box, remainderAngle, countAngle, true, borderPaint);
		
		drawValue(canvas, count, remainderAngle, countAngle);
		
		canvas.restore();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		// get screen size
		int displayWidth = MeasureSpec.getSize(widthMeasureSpec);
		int displayHeight = MeasureSpec.getSize(heightMeasureSpec);
		
		if (displayWidth > displayHeight){
			displayWidth = displayHeight;
		}

		 // determine the rectangle size
		int centerWidth = displayWidth / 2; 
		int iR = centerWidth-margin;
		if (box == null) {
			box = new RectF(centerWidth-iR,  // top
					centerWidth-iR,  		// left
					centerWidth+iR,  		// right
					centerWidth+iR); 		// bottom
		}
		setMeasuredDimension(displayWidth, displayWidth);
	}

	private void getDisplayMetrics(Context cxt){
		final DisplayMetrics dm = cxt.getResources().getDisplayMetrics();
		density = dm.density;
	}
	private float getRealPxFromDp(float fDp){
		return (density!=1.0f) ? density*fDp : fDp;
	}

	public void setValue(int count, int total) {
		this.count = count;
		this.remainder = total - count;
		
		countAngle = ((float)count / (float)total) * 360;
		remainderAngle = 360f - countAngle;
	}
}
