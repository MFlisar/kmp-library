package com.michaelflisar.kmplibrary.setups

class WasmSetup(
    val moduleName: String = DEFAULT_WASM_MODULE_NAME,
    val outputFileName: String = DEFAULT_WASM_OUTPUT_FILENAME,
) {
    companion object {
        val DEFAULT_WASM_MODULE_NAME: String = "app"
        val DEFAULT_WASM_OUTPUT_FILENAME: String = "app.js"
    }

}