package com.michaelflisar.kmplibrary.classes

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlTaggedNode
import java.io.File

class YamlValue(
    val value: String,
    val path: String
) {
    companion object {

        fun readYaml(file: File): List<YamlValue> {
            return try {
                val content = file.readText(Charsets.UTF_8)
                val yaml = Yaml.default.parseToYamlNode(content)
                collectYamlValues(yaml)
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("Failed to read setup from path '${file.path}'", e)
            }
        }

        private fun collectYamlValues(
            node: YamlNode,
            result: MutableList<YamlValue> = mutableListOf(),
        ): List<YamlValue> {
            when (node) {
                is YamlMap -> {
                    for (value in node.entries.values) {
                        collectYamlValues(value, result)
                    }
                }

                is YamlList -> {
                    val path = node.path.toHumanReadableString()
                    val value = node.items.joinToString(", ") { it.toString() }
                    result.add(YamlValue(value, path))
                }

                is YamlScalar -> {
                    val path = node.path.toHumanReadableString()
                    val value = node.content
                    result.add(YamlValue(value, path))
                }

                is YamlNull -> {}
                is YamlTaggedNode -> {}
            }
            return result
        }
    }
}