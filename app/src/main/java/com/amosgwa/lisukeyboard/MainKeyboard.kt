package com.amosgwa.lisukeyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.MotionEvent
import com.amosgwa.lisukeyboard.keyboard.GwaKeyboard
import com.amosgwa.lisukeyboard.keyboard.GwaKeyboardView
import com.amosgwa.lisukeyboard.keyboard.OnLanguageSelectionListener
import android.content.res.TypedArray
import android.view.ViewGroup
import com.amosgwa.lisukeyboard.data.KeyboardPreferences
import com.amosgwa.lisukeyboard.view.CustomKeyboardView


class MainKeyboard : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: GwaKeyboardView? = null
    private var keyboardNormal: GwaKeyboard? = null
    private var keyboardShift: GwaKeyboard? = null
    private var keyboardSymbol: GwaKeyboard? = null

    private var languageNames: MutableList<String> = mutableListOf()
    private var languageXmlRes: MutableList<Int> = mutableListOf()
    private var languageShiftXmlRes: MutableList<Int> = mutableListOf()
    private var languageSymbolXmlRes: MutableList<Int> = mutableListOf()

    private var preferences: KeyboardPreferences? = null
    private var lastSavedLanguageIdx: Int = 0
    private var currentLanguageIdx: Int = 0
        set(value) {
            field = value
            preferences?.putInt(KeyboardPreferences.KEY_CURRENT_LANGUAGE, value)
            keyboardNormal = GwaKeyboard(this@MainKeyboard, languageXmlRes[value], TYPE_NORMAL)
            keyboardShift = GwaKeyboard(this@MainKeyboard, languageShiftXmlRes[value], TYPE_SHIFT)
            keyboardSymbol = GwaKeyboard(this@MainKeyboard, languageSymbolXmlRes[value], TYPE_SYMBOL)
            keyboardView?.keyboard = keyboardNormal
            keyboardView?.currentLanguage = languageNames[value]
            keyboardView?.invalidateAllKeys()
        }

    override fun onCreate() {
        super.onCreate()
        loadKeyCodes()
        loadLanguages()
    }

    private fun loadSharedPreferences() {
        preferences = KeyboardPreferences(applicationContext)
        lastSavedLanguageIdx = preferences?.getInt(KeyboardPreferences.KEY_CURRENT_LANGUAGE, 0)
                ?: 0
    }

    override fun onCreateCandidatesView(): View? {
        return null
    }

    override fun onCreateInputView(): View? {
        loadSharedPreferences()
        keyboardNormal = GwaKeyboard(this, languageXmlRes[lastSavedLanguageIdx], TYPE_NORMAL)
        keyboardShift = GwaKeyboard(this, languageShiftXmlRes[lastSavedLanguageIdx], TYPE_SHIFT)
        keyboardSymbol = GwaKeyboard(this, languageSymbolXmlRes[lastSavedLanguageIdx], TYPE_SYMBOL)
        initialSetupKeyboardView()
//        return keyboardView
        return CustomKeyboardView(applicationContext)
    }

    override fun setInputView(view: View?) {
        super.setInputView(view)
        view?.let {
            disableParentClipping(view)
        }
    }

    private fun disableParentClipping(v: View) {
        if (v is ViewGroup) {
            v.clipToPadding = false
            v.clipChildren = false
        }
        val parent = v.parent ?: return
        if (parent is View) {
            disableParentClipping(parent)
        }
    }

    private fun initialSetupKeyboardView() {
        // Setup keyboard view.
        keyboardView = layoutInflater.inflate(R.layout.keyboard, null) as GwaKeyboardView?
        keyboardView?.languages = languageNames
        keyboardView?.currentLanguage = languageNames[lastSavedLanguageIdx]
        keyboardView?.onLanguageSelectionListener = object : OnLanguageSelectionListener {
            override fun onLanguageSelected(languageIdx: Int) {
                currentInputConnection.deleteSurroundingText(1, 0)
                currentLanguageIdx = languageIdx
            }
        }
        // Disable preview for keyboard
        keyboardView?.isPreviewEnabled = false
        keyboardView?.setOnKeyboardActionListener(this)
        // Set the default view to keyboard normal.
        keyboardView?.keyboard = keyboardNormal
    }

    private fun loadLanguages() {
        val languagesArray = resources.obtainTypedArray(R.array.languages)
        var eachLanguageTypedArray: TypedArray? = null
        for (i in 0 until languagesArray.length()) {
            val id = languagesArray.getResourceId(i, -1)
            if (id == -1) {
                throw IllegalStateException("Invalid language array resource")
            }
            eachLanguageTypedArray = resources.obtainTypedArray(id)
            eachLanguageTypedArray?.let {
                val nameIdx = 0

                val languageName = it.getString(nameIdx)
                val xmlRes = it.getResourceId(RES_IDX, -1)
                val shiftXmlRes = it.getResourceId(SHIFT_IDX, -1)
                val symbolXmlRes = it.getResourceId(SYM_IDX, -1)

                if (languageName == null || xmlRes == -1 || shiftXmlRes == -1 || symbolXmlRes == -1) {
                    throw IllegalStateException("Make sure the arrays resources contain name, xml, and shift xml")
                }

                languageNames.add(languageName)
                languageXmlRes.add(xmlRes)
                languageShiftXmlRes.add(shiftXmlRes)
                languageSymbolXmlRes.add(symbolXmlRes)
            }
        }
        if (eachLanguageTypedArray != null) {
            eachLanguageTypedArray.recycle()
        }
        languagesArray.recycle()
    }

    override fun swipeRight() {
    }

    override fun onPress(primaryCode: Int) {
    }

    override fun onRelease(primaryCode: Int) {
    }

    override fun swipeLeft() {
    }

    override fun swipeUp() {
    }

    override fun swipeDown() {
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val inputConnection = currentInputConnection
        playClick(primaryCode)
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                val selectedText: CharSequence? = inputConnection.getSelectedText(0)
                if (selectedText == null) {
                    inputConnection.deleteSurroundingText(1, 0)
                } else {
                    if (selectedText.isEmpty()) {
                        inputConnection.deleteSurroundingText(1, 0)
                    } else {
                        inputConnection.commitText("", 1)
                    }
                }
            }
            KEYCODE_ABC -> {
                keyboardView?.keyboard = keyboardNormal
                keyboardView?.invalidateAllKeys()
                return
            }
            Keyboard.KEYCODE_SHIFT -> {
                keyboardView?.keyboard = keyboardShift
                keyboardView?.invalidateAllKeys()
                return
            }
            KEYCODE_UNSHIFT -> {
                keyboardView?.keyboard = keyboardNormal
                keyboardView?.invalidateAllKeys()
                return
            }
            KEYCODE_123 -> {
                keyboardView?.keyboard = keyboardSymbol
                keyboardView?.invalidateAllKeys()
                return
            }
            KEYCODE_123 -> {
                keyboardView?.keyboard = keyboardSymbol
                keyboardView?.invalidateAllKeys()
                return
            }
            KEYCODE_MYA_TI_MYA_NA -> {
                val output = KEYCODE_MYA_TI.toChar().toString() + KEYCODE_MYA_NA.toChar()
                inputConnection.commitText(output, 2)
            }
            KEYCODE_NA_PO_MYA_NA -> {
                val output = KEYCODE_NA_PO.toChar().toString() + KEYCODE_MYA_NA.toChar()
                inputConnection.commitText(output, 2)
            }
            KEYCODE_LANGUAGE -> {
                val mgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                mgr?.showInputMethodPicker()
            }
            Keyboard.KEYCODE_DONE -> {
                val event = (KeyEvent(0, 0, MotionEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_ENTER, 0, 0, 0, 0,
                        KeyEvent.FLAG_SOFT_KEYBOARD))
                inputConnection.sendKeyEvent(event)
            }
            else -> {
                inputConnection.commitText(primaryCode.toChar().toString(), 1)
            }
        }
        // Switch back to normal if the selected page type is shift.
        val keyboard = keyboardView?.keyboard as GwaKeyboard
        if (keyboard.type == TYPE_SHIFT) {
            keyboardView?.keyboard = keyboardNormal
            keyboardView?.invalidateAllKeys()
        }
    }

    private fun playClick(keyCode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (keyCode) {
            32 -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR)
            Keyboard.KEYCODE_DONE, 10 -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN)
            Keyboard.KEYCODE_DELETE -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE)
            else -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        }
    }

    private fun loadKeyCodes() {
        KEYCODE_UNSHIFT = resources.getInteger(R.integer.keycode_unshift)
        KEYCODE_ABC = resources.getInteger(R.integer.keycode_abc)
        KEYCODE_123 = resources.getInteger(R.integer.keycode_sym)
        KEYCODE_SPACE = resources.getInteger(R.integer.keycode_space)
        KEYCODE_LANGUAGE = resources.getInteger(R.integer.keycode_switch_next_keyboard)
        KEYCODE_NA_PO_MYA_NA = resources.getInteger(R.integer.keycode_na_po_mya_na)
        KEYCODE_MYA_TI_MYA_NA = resources.getInteger(R.integer.keycode_mya_ti_mya_na)
        KEYCODE_MYA_TI = resources.getInteger(R.integer.keycode_mya_ti)
        KEYCODE_MYA_NA = resources.getInteger(R.integer.keycode_mya_na)
        KEYCODE_NA_PO = resources.getInteger(R.integer.keycode_na_po)
    }

    override fun onText(text: CharSequence?) {
    }

    companion object {
        var KEYCODE_NONE = -777
        var KEYCODE_UNSHIFT = KEYCODE_NONE
        var KEYCODE_ABC = KEYCODE_NONE
        var KEYCODE_123 = KEYCODE_NONE
        var KEYCODE_SPACE = KEYCODE_NONE
        var KEYCODE_NA_PO_MYA_NA = KEYCODE_NONE
        var KEYCODE_MYA_TI_MYA_NA = KEYCODE_NONE
        var KEYCODE_LANGUAGE = KEYCODE_NONE
        var KEYCODE_NA_PO = KEYCODE_NONE
        var KEYCODE_MYA_NA = KEYCODE_NONE
        var KEYCODE_MYA_TI = KEYCODE_NONE

        const val TYPE_NORMAL = 0
        const val TYPE_SHIFT = 1
        const val TYPE_SYMBOL = 2


        const val RES_IDX = 1
        const val SHIFT_IDX = 2
        const val SYM_IDX = 3
    }
}
