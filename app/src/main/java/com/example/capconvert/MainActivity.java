package com.example.capconvert;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.capconvert.databinding.ActivityMainBinding;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    private EditText nanoET, picoET, microET, milliET;
    private boolean isUpdating = false; // Flag to prevent infinite loops

    private Locale userLocale;
    private char decimalSeparator;
    private char groupingSeparator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the user's locale and decimal separator
        userLocale = Locale.getDefault();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(userLocale);
        decimalSeparator = symbols.getDecimalSeparator();
        groupingSeparator = symbols.getGroupingSeparator();

        nanoET = binding.nfET;
        picoET = binding.pfET;
        microET = binding.ufET;
        milliET = binding.mfET;

        nanoET.addTextChangedListener(new UnitTextWatcher(nanoET));
        picoET.addTextChangedListener(new UnitTextWatcher(picoET));
        microET.addTextChangedListener(new UnitTextWatcher(microET));
        milliET.addTextChangedListener(new UnitTextWatcher(milliET));
    }

    private class UnitTextWatcher implements TextWatcher {
        private EditText source;

        public UnitTextWatcher(EditText source) {
            this.source = source;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (isUpdating) return; // Prevent recursion
            isUpdating = true;

            try {
                double value = 0;
                String inputText = s.toString();

                // Remove grouping separators for parsing
                inputText = inputText.replace(String.valueOf(groupingSeparator), "");

                // Special case: if the input ends with a decimal separator or is just a decimal separator
                // don't try to parse it yet, just update other fields with value 0
                if (inputText.endsWith(".") || inputText.endsWith(",") || inputText.equals(".") || inputText.equals(",")) {
                    // Just update with 0 for now
                    if (source == nanoET) {
                        updateFields(0, "nF");
                    } else if (source == picoET) {
                        updateFields(0, "pF");
                    } else if (source == microET) {
                        updateFields(0, "uF");
                    } else if (source == milliET) {
                        updateFields(0, "mF");
                    }

                    isUpdating = false;
                    return;
                }

                if (!inputText.isEmpty()) {
                    try {
                        // Normalize decimal separator for parsing
                        String normalizedInput;
                        if (decimalSeparator == ',') {
                            // If locale uses comma, convert periods to commas for parsing
                            normalizedInput = inputText.replace('.', ',');
                        } else {
                            // If locale uses period, convert commas to periods for parsing
                            normalizedInput = inputText.replace(',', '.');
                        }

                        // Try parsing with the locale's number format
                        NumberFormat numberFormat = NumberFormat.getNumberInstance(userLocale);
                        value = numberFormat.parse(normalizedInput).doubleValue();
                    } catch (ParseException e) {
                        // Fallback to direct parsing with dot as decimal separator
                        value = Double.parseDouble(inputText.replace(',', '.'));
                    }
                }

                if (source == nanoET) {
                    updateFields(value, "nF");
                } else if (source == picoET) {
                    updateFields(value, "pF");
                } else if (source == microET) {
                    updateFields(value, "uF");
                } else if (source == milliET) {
                    updateFields(value, "mF");
                }
            } catch (NumberFormatException e) {
                // Handle invalid input gracefully - do nothing to allow typing
            }

            isUpdating = false;
        }
    }

    private void updateFields(double value, String unit) {
        double nano, pico, micro, milli;

        switch (unit) {
            case "nF":
                nano = value;
                pico = value * 1000;
                micro = value / 1000;
                milli = value / 1_000_000;
                break;
            case "pF":
                nano = value / 1000;
                pico = value;
                micro = value / 1_000_000;
                milli = value / 1_000_000_000;
                break;
            case "uF":
                nano = value * 1000;
                pico = value * 1_000_000;
                micro = value;
                milli = value / 1000;
                break;
            case "mF":
                nano = value * 1_000_000;
                pico = value * 1_000_000_000;
                micro = value * 1000;
                milli = value;
                break;
            default:
                return;
        }

        // Only update fields that are not the source
        if (!unit.equals("nF")) updateEditText(nanoET, nano, false);
        if (!unit.equals("pF")) updateEditText(picoET, pico, false);
        if (!unit.equals("uF")) updateEditText(microET, micro, false);
        if (!unit.equals("mF")) updateEditText(milliET, milli, false);
    }

    private void updateEditText(EditText editText, double value, boolean isSource) {
        // Use the user's locale for formatting
        DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(userLocale);
        decimalFormat.setMaximumFractionDigits(6); // Limit to 6 decimal places
        decimalFormat.setGroupingUsed(true); // Disable grouping to avoid confusing separators

        // Format the number according to locale
        String newText = decimalFormat.format(value);

        if (!editText.getText().toString().equals(newText)) {
            int cursorPosition = editText.getSelectionStart(); // Save cursor position
            editText.setText(newText);
            if (isSource) {
                editText.setSelection(Math.min(cursorPosition, newText.length())); // Restore cursor
            }
        }
    }
}