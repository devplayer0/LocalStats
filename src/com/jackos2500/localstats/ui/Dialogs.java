package com.jackos2500.localstats.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.jackos2500.localstats.R;
import com.jackos2500.localstats.card.RadiusCard;
import com.jackos2500.localstats.fragment.QueryFragment;

public class Dialogs {
	public static class EditTextDialogFragment extends DialogFragment {
		private IEditTextDialog listener;
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View dialogView = inflater.inflate(R.layout.dialog_edit_text, null);
			
			TextView content = (TextView)dialogView.findViewById(android.R.id.content);
			content.setText(args.getInt("content"));
			
			final EditText editText = (EditText)dialogView.findViewById(R.id.editor);
			editText.setText(args.getString("initialText"), TextView.BufferType.EDITABLE);
			
			builder.setTitle(args.getInt("title"));
			builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					listener.onSave(editText.getText().toString());
				}
			});
			builder.setNegativeButton(R.string.cancel, null);
			
			builder.setView(dialogView);
			
			final AlertDialog dialog = builder.create();
			dialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialogInterface) {
					dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(listener.canSave(editText.getText().toString()));
					if (editText.getText().length() == 0) {
						dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					}
					
					editText.addTextChangedListener(new TextWatcher() {
						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
							if (s.length() == 0) {
								dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
								return;
							}
							dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(listener.canSave(s.toString()));
						}
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
						@Override
						public void afterTextChanged(Editable s) {}
					});
				}
			});
			return dialog;
		}
		public static EditTextDialogFragment newInstance(int title, int content, String initialText, IEditTextDialog listener) {
			EditTextDialogFragment f = new EditTextDialogFragment();
			
			Bundle args = new Bundle();
			args.putInt("title", title);
			args.putInt("content", content);
			args.putString("initialText", initialText);
			
			f.setArguments(args);
			
			f.listener = listener;
			
			return f;
		}
		public static interface IEditTextDialog {
			public boolean canSave(String text);
			public void onSave(String text);
		}
	}
	public static class SetRadiusDialogFragment extends DialogFragment {
		private RadiusCard radiusCard;
		private QueryFragment queryFragment;
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.pick_radius);
			
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View dialog = inflater.inflate(R.layout.dialog_set_radius, null);
			
			final NumberPicker picker = (NumberPicker)dialog.findViewById(R.id.radius_picker);
			picker.setMinValue(1);
			picker.setMaxValue(10);
			picker.setValue(radiusCard.getRadius() != 0 ? radiusCard.getRadius() : 1);
			builder.setNegativeButton(R.string.cancel, null);
			builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					radiusCard.setRadius(picker.getValue());
					//queryFragment.loadElectoralDivisions(radiusCard.getRadius());
				}
			});
			
			builder.setView(dialog);
			
			return builder.create();
		}
		public static SetRadiusDialogFragment newInstance(RadiusCard radiusCard, QueryFragment queryFragment) {
			SetRadiusDialogFragment f = new SetRadiusDialogFragment();
			
			f.radiusCard = radiusCard;
			f.queryFragment = queryFragment;
			
			return f;
		}
	}
	public static class EnableGPSDialogFragment extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.gps_required_title);
			builder.setMessage(R.string.gps_required);
			builder.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(intent);
				}
			});
			builder.setNegativeButton(R.string.cancel, null);
			return builder.create();
		}
		@Override
		public void onDismiss(DialogInterface dialog) {
			getActivity().finish();
		}
	}
	public static class LoadingDialogFragment extends DialogFragment {
		private DialogInterface.OnClickListener cancelListener;
		
		private TextView loadingTextView;
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			if (args.getInt("titleText") != -1) {
				builder.setTitle(args.getInt("titleText"));
			}
			if (args.getBoolean("cancelable")) {
				builder.setNegativeButton(R.string.cancel, cancelListener);
			}
			
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View dialog = inflater.inflate(R.layout.dialog_load, null);
			
			loadingTextView = (TextView)dialog.findViewById(R.id.dialog_load_text);
			loadingTextView.setText(args.getInt("loadingText"));
			
			builder.setView(dialog);
			
			return builder.create();
		}
		public TextView getLoadingTextView() {
			return loadingTextView;
		}
		public static LoadingDialogFragment newInstance(int titleText, int loadingText, boolean cancelable, DialogInterface.OnClickListener cancelListener) {
			LoadingDialogFragment f = new LoadingDialogFragment();
			
			f.cancelListener = cancelListener;
			
			Bundle args = new Bundle();
			args.putInt("titleText", titleText);
			args.putInt("loadingText", loadingText);
			args.putBoolean("cancelable", cancelable);
			f.setArguments(args);
			
			return f;
		}
	}
}
