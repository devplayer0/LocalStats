package com.jackos2500.localstats.ui;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardBase;
import com.jackos2500.localstats.MainActivity;
import com.jackos2500.localstats.R;

@SuppressWarnings("rawtypes")
public class AnimatedCardAdapter extends CardAdapter {
	private boolean animationLeft;
	private Set<Integer> alreadyAnimated;
	public AnimatedCardAdapter(Context context) {
		super(context);
		alreadyAnimated = new HashSet<Integer>();
	}
	@SuppressWarnings("unchecked")
	@Override
	public View onViewCreated(int index, View recycled, CardBase item) {
		//if (!item.isHeader()) {
			int[] location = new int[2];
			recycled.getLocationOnScreen(location);
			
			if (location[1] < MainActivity.deviceHeight && !alreadyAnimated.contains(index)) {
				if (!animationLeft) {
					Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up_left);
					animation.setAnimationListener(new InvalidateOnCompleteListener(recycled));
					recycled.startAnimation(animation);
				} else {
					Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up_right);
					animation.setAnimationListener(new InvalidateOnCompleteListener(recycled));
					recycled.startAnimation(animation);
				}
				recycled.getRootView().invalidate();
				alreadyAnimated.add(index);
				animationLeft = !animationLeft;
			}
		//}
		
		return super.onViewCreated(index, recycled, item);
	}
	private static class InvalidateOnCompleteListener implements AnimationListener {
		private View view;
		public InvalidateOnCompleteListener(View view) {
			this.view = view;
		}
		@Override
		public void onAnimationEnd(Animation animation) {
			view.getRootView().invalidate();
		}
		@Override
		public void onAnimationRepeat(Animation animation) {}
		@Override
		public void onAnimationStart(Animation animation) {
			view.getRootView().invalidate();
		}
	}
}
