package com.chiranjeevankumar.aura

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * AURA v0.9 — Native AI Chat Tool
 *
 * Design reference is recreated with native Android views.
 * No screenshot/reference image is embedded.
 *
 * AI requests use the existing AURA provider architecture:
 *
 * AuraChatActivity
 *        ↓
 * AuraAIProvider
 *        ↓
 * GeminiAIProvider
 *        ↓
 * Existing AURA backend
 */
class AuraChatActivity : Activity() {

    private val cyan = Color.rgb(0, 229, 255)
    private val cyanSoft = Color.rgb(70, 190, 205)
    private val background = Color.rgb(3, 8, 13)
    private val panel = Color.rgb(7, 15, 22)
    private val panel2 = Color.rgb(10, 21, 29)
    private val bubble = Color.rgb(12, 30, 38)
    private val white = Color.rgb(235, 250, 255)
    private val muted = Color.rgb(125, 160, 170)

    private lateinit var messageInput: EditText
    private lateinit var messagesContainer: LinearLayout
    private lateinit var messageScroll: ScrollView

    private val geminiProvider: AuraAIProvider by lazy {
        GeminiAIProvider(
            backendUrl =
                "https://experiments-featured-full-cole.trycloudflare.com",
            userId = null
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation =
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.statusBarColor = background
        window.navigationBarColor = background

        setContentView(createPage())
    }

    // =========================================================
    // PAGE
    // =========================================================

    private fun createPage(): View {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(this@AuraChatActivity.background)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(
            createSidebar(),
            LinearLayout.LayoutParams(
                dp(235),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            createMainChat(),
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        return root
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private fun createSidebar(): View {

        val sidebar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(26), dp(18), dp(20))
            setBackgroundColor(panel)
        }

        // Native futuristic avatar — no external/reference image.
        val avatar = TextView(this).apply {
            text = "◈"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            background = roundedBackground(
                Color.rgb(5, 24, 31),
                cyan,
                22
            )
        }

        sidebar.addView(
            avatar,
            LinearLayout.LayoutParams(
                dp(82),
                dp(82)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        sidebar.addView(space(18))

        val title = label(
            "AI ASSISTANT",
            15f,
            white
        )

        sidebar.addView(title)

        val online = label(
            "●  ONLINE",
            12f,
            cyan
        )

        sidebar.addView(
            online,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
            }
        )

        sidebar.addView(space(28))

        sidebar.addView(sidebarButton("＋  NEW CHAT") {
            clearConversation()
        })

        sidebar.addView(sidebarButton("▣  CHATS") { })

        sidebar.addView(sidebarButton("◷  HISTORY") { })

        sidebar.addView(sidebarButton("☆  FAVORITES") { })

        sidebar.addView(space(10))

        sidebar.addView(sidebarButton("⚙  SETTINGS") {
            Toast.makeText(
                this,
                "AURA settings",
                Toast.LENGTH_SHORT
            ).show()
        })

        sidebar.addView(sidebarButton("?  HELP") {
            Toast.makeText(
                this,
                "Ask AURA anything.",
                Toast.LENGTH_SHORT
            ).show()
        })

        sidebar.addView(
            Space(this),
            LinearLayout.LayoutParams(
                1,
                0,
                1f
            )
        )

        val version = label(
            "AURA  v0.9",
            11f,
            muted
        )

        sidebar.addView(version)

        return sidebar
    }

    private fun sidebarButton(
        textValue: String,
        action: () -> Unit
    ): TextView {

        return TextView(this).apply {
            text = textValue
            textSize = 13f
            setTextColor(white)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(8), 0)

            background = roundedBackground(
                panel2,
                Color.TRANSPARENT,
                14
            )

            setOnClickListener {
                action()
            }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                bottomMargin = dp(8)
            }
        }
    }

    // =========================================================
    // MAIN CHAT
    // =========================================================

    private fun createMainChat(): View {

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(this@AuraChatActivity.background)
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }

        main.addView(createHeader())

        messageScroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        messagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(8),
                dp(20),
                dp(8),
                dp(20)
            )
        }

        messageScroll.addView(
            messagesContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        main.addView(
            messageScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        addAssistantMessage(
            "Hello. I'm AURA.\n\nHow can I assist you today?"
        )

        main.addView(createComposer())

        return main
    }

    private fun createHeader(): View {

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(8),
                dp(4),
                dp(8),
                dp(14)
            )
        }

        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        identity.addView(
            label(
                "AI ASSISTANT",
                19f,
                white
            )
        )

        identity.addView(
            label(
                "●  ONLINE",
                11f,
                cyan
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
        )

        header.addView(
            identity,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val status = label(
            "AURA CORE",
            11f,
            muted
        )

        header.addView(status)

        return header
    }

    // =========================================================
    // MESSAGES
    // =========================================================

    private fun addAssistantMessage(message: String) {

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }

        val avatar = TextView(this).apply {
            text = "◈"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            background = roundedBackground(
                Color.rgb(5, 25, 32),
                cyan,
                16
            )
        }

        row.addView(
            avatar,
            LinearLayout.LayoutParams(
                dp(42),
                dp(42)
            ).apply {
                rightMargin = dp(10)
            }
        )

        val card = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(white)
            setPadding(
                dp(16),
                dp(13),
                dp(16),
                dp(13)
            )
            background = roundedBackground(
                bubble,
                Color.rgb(25, 75, 85),
                18
            )
        }

        row.addView(
            card,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        messagesContainer.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        )
    }

    private fun addUserMessage(message: String) {

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }

        val spacer = Space(this)

        row.addView(
            spacer,
            LinearLayout.LayoutParams(
                0,
                1,
                0.15f
            )
        )

        val card = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(white)
            setPadding(
                dp(16),
                dp(13),
                dp(16),
                dp(13)
            )
            background = roundedBackground(
                Color.rgb(8, 38, 48),
                cyanSoft,
                18
            )
        }

        row.addView(
            card,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0.85f
            )
        )

        messagesContainer.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        )
    }

    // =========================================================
    // COMPOSER
    // =========================================================

    private fun createComposer(): View {

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )

            background = roundedBackground(
                panel2,
                Color.rgb(22, 65, 74),
                22
            )
        }

        val attach = iconButton("＋") {
            Toast.makeText(
                this,
                "Attachment support",
                Toast.LENGTH_SHORT
            ).show()
        }

        outer.addView(
            attach,
            LinearLayout.LayoutParams(
                dp(44),
                dp(44)
            )
        )

        messageInput = EditText(this).apply {
            hint = "Type a message..."
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 16f
            setSingleLine(true)
            background = null
            setPadding(
                dp(8),
                0,
                dp(8),
                0
            )

            setOnEditorActionListener { _, _, _ ->
                sendCurrentMessage()
                true
            }
        }

        outer.addView(
            messageInput,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            )
        )

        val mic = iconButton("◉") {
            Toast.makeText(
                this,
                "Voice input",
                Toast.LENGTH_SHORT
            ).show()
        }

        outer.addView(
            mic,
            LinearLayout.LayoutParams(
                dp(44),
                dp(44)
            )
        )

        val send = iconButton("➤") {
            sendCurrentMessage()
        }

        outer.addView(
            send,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        return outer
    }

    private fun iconButton(
        symbol: String,
        action: () -> Unit
    ): TextView {

        return TextView(this).apply {
            text = symbol
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            background = roundedBackground(
                Color.rgb(6, 25, 32),
                Color.rgb(25, 80, 90),
                18
            )

            setOnClickListener {
                action()
            }
        }
    }

    // =========================================================
    // REAL AURA AI CONNECTION
    // =========================================================

    private fun sendCurrentMessage() {

        val message = messageInput.text.toString().trim()

        if (message.isEmpty()) {
            return
        }

        addUserMessage(message)

        messageInput.text.clear()

        addAssistantMessage("Thinking...")

        scrollToBottom()

        geminiProvider.sendMessage(message) { result ->

            runOnUiThread {

                removeLastAssistantMessage()

                result.fold(
                    onSuccess = { response ->
                        addAssistantMessage(response)
                    },

                    onFailure = { error ->
                        addAssistantMessage(
                            "AI error:\n" +
                                (
                                    error.message
                                        ?: error.javaClass.simpleName
                                )
                        )
                    }
                )

                scrollToBottom()
            }
        }
    }

    private fun removeLastAssistantMessage() {

        if (messagesContainer.childCount == 0) {
            return
        }

        messagesContainer.removeViewAt(
            messagesContainer.childCount - 1
        )
    }

    private fun clearConversation() {

        messagesContainer.removeAllViews()

        addAssistantMessage(
            "New conversation ready.\n\nHow can I assist you?"
        )

        scrollToBottom()
    }

    private fun scrollToBottom() {

        messageScroll.post {
            messageScroll.fullScroll(
                View.FOCUS_DOWN
            )
        }
    }

    // =========================================================
    // UI HELPERS
    // =========================================================

    private fun label(
        textValue: String,
        size: Float,
        color: Int
    ): TextView {

        return TextView(this).apply {
            text = textValue
            textSize = size
            setTextColor(color)
        }
    }

    private fun space(height: Int): Space {

        return Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        }
    }

    private fun roundedBackground(
        fill: Int,
        stroke: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()

            if (stroke != Color.TRANSPARENT) {
                setStroke(dp(1), stroke)
            }
        }
    }

    private fun dp(value: Int): Int {

        return (value * resources.displayMetrics.density)
            .toInt()
    }
}
