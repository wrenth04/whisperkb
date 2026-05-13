package app.whisperkb.provider

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

class ProviderSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val current = CloudProviderStore.load(this)
        val nameInput = EditText(this).apply {
            hint = "Provider name"
            setText(current?.name.orEmpty())
        }
        val endpointInput = EditText(this).apply {
            hint = "API endpoint"
            setText(current?.endpoint.orEmpty())
        }
        val apiKeyInput = EditText(this).apply {
            hint = "API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(current?.apiKey.orEmpty())
        }
        val modelInput = EditText(this).apply {
            hint = "Model"
            setText(current?.model.orEmpty())
        }
        val summaryView = TextView(this)

        fun refreshSummary() {
            val saved = CloudProviderStore.load(this)
            summaryView.text = if (saved == null) {
                "Saved provider: none"
            } else {
                "Saved provider: ${saved.name} | ${saved.endpoint} | ${saved.model}"
            }
        }

        setContentView(
            ScrollView(this).apply {
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(32, 32, 32, 32)
                        addView(TextView(context).apply { text = "whisperkb provider settings" })
                        addView(summaryView)
                        addView(nameInput)
                        addView(endpointInput)
                        addView(apiKeyInput)
                        addView(modelInput)
                        addView(Button(context).apply {
                            text = "Save provider"
                            setOnClickListener {
                                CloudProviderStore.save(
                                    this@ProviderSettingsActivity,
                                    CloudProviderConfig(
                                        name = nameInput.text.toString().trim(),
                                        endpoint = endpointInput.text.toString().trim(),
                                        apiKey = apiKeyInput.text.toString().trim(),
                                        model = modelInput.text.toString().trim(),
                                    )
                                )
                                refreshSummary()
                            }
                        })
                        addView(Button(context).apply {
                            text = "Clear provider"
                            setOnClickListener {
                                CloudProviderStore.save(
                                    this@ProviderSettingsActivity,
                                    CloudProviderConfig("", "", "", "")
                                )
                                nameInput.setText("")
                                endpointInput.setText("")
                                apiKeyInput.setText("")
                                modelInput.setText("")
                                refreshSummary()
                            }
                        })
                    }
                )
            }
        )

        refreshSummary()
    }
}
